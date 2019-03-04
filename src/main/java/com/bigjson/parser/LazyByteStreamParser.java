package com.bigjson.parser;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Parse a JSON text (provided by a file reader) upon request. I.e. it can parse a 
 * top level node (assuming the file cursor is at the beginning of this node) and
 * it can parse child nodes starting at a given position without parsing their 
 * child nodes. In case of a String node with a long text, load only first 
 * <code>stringDisplayLength</code> symbols.
 * <br>
 * Text is supposed to be in UTF-8 format, i.e. String node value can contain 
 * several-byte chars.
 * <br><br>
 * Grammar in EBNF:
 * <br>
 * object	: '{' { STRING : value [, STRING : value] } '}'<br>
 * array	: '['  {value [, value] } ']'<br>
 * value	: STRING | NUMBER | object | array | 'true' | 'false' | 'null'<br>
 * STRING	: '"' {}   '"' <br>
 * NUMBER	:<br>
 * @author nikanka
 *
 */
public class LazyByteStreamParser implements Closeable{
	private static final String KEYWORD_TRUE = "true";
	private static final String KEYWORD_FALSE = "false";
	private static final String KEYWORD_NULL = "null";
	private static final String defaultTopLevelName = "JSON";
	
	private static final boolean DEBUG = false; 
	
	private int stringDisplayLength = 4;
	private UTF8FileReader reader;
	/**
	 * Contains last read byte outside strings. 
	 * When reading a string it contains a quote (last symbol read before entering a string). 
	 */
	private byte curByte;
	private Pattern patternNumber = Pattern.compile("^-?(0|([1-9][0-9]*))(.[0-9]*)?([eE][+-]?[0-9]+)?$");

	/**
	 * Create a parser for a given file.
	 * 
	 * @param fileName
	 * @param stringDisplayLimit
	 *            how many first characters of a string to view. If
	 *            stringDisplayLimit < 0 full strings are loaded
	 * @throws IOException
	 *             if file is not found or empty or if an I/O error occurs while
	 *             reading bytes from the file
	 */
	protected LazyByteStreamParser(String fileName, int stringDisplayLimit) throws IOException{
		reader = new UTF8FileReader(fileName);
		this.stringDisplayLength = stringDisplayLimit;
	}
	@Override
	public void close() throws IOException {
		reader.close();
	}

	byte getCurByte(){
		return curByte;
	}
	private static char byteToChar(byte b){
		return (char)(b & 0xFF);
	}
	
	/**
	 * Parse the file and return the top level node of JSON tree.
	 * 
	 * @param topLevelName
	 *            specifies the name of the top-level node. If topLevelName is
	 *            null the default name ("JSON") will be used instead.
	 * @return top level node of the JSON tree
	 * @throws IOException
	 * @throws IllegalFormatException
	 */
	protected JSONNode parseTopLevel(String topLevelName) throws IOException, IllegalFormatException{
		if(topLevelName == null){
			topLevelName = defaultTopLevelName;
		}
		debug("Entered parseTopLevel ('" + topLevelName+ "'). File pos = " + reader.getFilePosition());	
		reader.getToPosition(0);
		moveToNextNonspaceByte();
		JSONNode root = parseValue(topLevelName);
		debug("Done with parseTopLevel: "+reader.getFilePosition()+", '"+byteToChar(curByte)+"'");
		// check that nothing else left in the file
		
		while(reader.hasNext()){
			moveToNextByte();
			if(!curByteIsWhitespace()){
				throwIllegalFormatExceptionWithFilePos(
						"The JSON structure contains non-space symbols outside the root: " + (char)curByte);
			}
		}
		
		return root;
	}
	protected List<JSONNode> loadChildrenAtPosition(long filePos) throws IOException, IllegalFormatException{
		if(!reader.getToPosition(filePos)){
			throw new IllegalArgumentException("Trying to set the cursor to a position that is bigger "
					+ "than the file's size: " + filePos);
		}
		try{
			moveToNextByte();
		} catch(IOException e){
			throw new IllegalFormatException(
					e.getMessage() + " while looking for an opening bracket for an object or array");
		}
		if(curByte == '['){
			try {
				return parseArray();
			}catch(IOException e){
				throw new IllegalArgumentException(e.getMessage() + " while reading an array");
			}		
		} else if(curByte == '{'){
			try{
				return parseObject();
			}catch(IOException e){
				throw new IllegalArgumentException(e.getMessage() + " while reading an object");
			}
		} 
		throwIllegalFormatExceptionWithFilePos("Lazy load children: was expecting '[' or '{'");
		return null;
	}
	/**
	 * Load full String at a given position. 
	 * @param openingQuotePos file position of an opening quote
	 * @param closingQuotePos file position of a closing quote
	 * @return the string that was read
	 * @throws IOException
	 * @throws IllegalFormatException if end of file is reached before the string is read or 
	 * string does not start or end with a quote 
	 */
	protected String loadStringAtPosition(long openingQuotePos, long closingQuotePos) throws IOException, IllegalFormatException{
		if(!reader.getToPosition(openingQuotePos)){
			throw new IllegalArgumentException("Trying to set the cursor to a position that is bigger "
					+ "than the file's size: " + openingQuotePos);
		}
		try{
			moveToNextByte(); // read opening quote
		} catch(IllegalFormatException e){
			throw new IllegalFormatException(e.getMessage() + " while reading an opening quote for a String");
		}
		StringWithCoords str = parseString(false);
		// just a sanity check
		if(str.getOpeningQuotePos() != openingQuotePos ||
				str.getClosingQuotePos() != closingQuotePos){
			throw new RuntimeException("The coordinates of the loaded String "
					+ "are not equal to the original ones: "
					+ "got [" + str.getOpeningQuotePos() + ", " + str.getClosingQuotePos() + "] "
					+ "expected [" + openingQuotePos + ", " + closingQuotePos + "]");
		}
		return str.getString();
	}

	/**
	 * Parse an array: '[' { value [, value] } ']' and return a list of array nodes.
	 * In the beginning of the method the cursor should be at '['. 
	 * After this method the cursor should be at the position following the ']' 
	 * (']' was just read into curByte). 
	 * 
	 * @return
	 * @throws IOException
	 * @throws IllegalFormatException
	 */
	private List<JSONNode> parseArray() throws IOException, IllegalFormatException{
		debug("Entered parseArray");
		if(curByte != '['){
			throwIllegalFormatExceptionWithFilePos("Array should start with '['");
		}
		List<JSONNode> nodeList = new ArrayList<JSONNode>();
		moveToNextNonspaceByte();
		while(curByte != ']'){
			JSONNode child = parseValue(""+nodeList.size());
			nodeList.add(child);
			moveToNextNonspaceByte();
			if(curByte == ']'){
				break;
			}
			if(curByte == ','){
				moveToNextNonspaceByte();
			} else {
				throwIllegalFormatExceptionWithFilePos("Unexpected symbol after "
						+ "an element in an array: '" + byteToChar(curByte) + "'");
			}
		}
		debug("Done with parseArray: "+reader.getFilePosition()+", '"+byteToChar(curByte)+"'");
		return nodeList;
	}

//	private JSONNode parseObject(String name, boolean lazy) throws IOException{
//		JSONNode objNode = new JSONNode(JSONNode.TYPE_OBJECT, name, level);
//		return parseObject(objNode, lazy);
//	}
	/**
	 * Parse an object: '{' { string:value [, string:value] } '}'
	 * In the beginning of the method the cursor should be at '{'.
	 * After this method the cursor should be at the position following the '}'
	 * ('}' was just read into curByte). 
	 * @return a list of named JSON nodes that are contained in this object
	 * @throws IOException
	 * @throws IllegalFormatException
	 */
	private List<JSONNode> parseObject() throws IOException, IllegalFormatException{
		debug("Entered parseObject at pos " + reader.getFilePosition());
		if(curByte != '{'){
			throwIllegalFormatExceptionWithFilePos("Object should start with '{'");
		}
		List<JSONNode> nodeList = new ArrayList<JSONNode>();
		moveToNextNonspaceByte();
		while(curByte != '}'){
			JSONNode node = parseNameValuePair();
			nodeList.add(node);
			moveToNextNonspaceByte();
			if(curByte == '}'){
				break;
			}
			if(curByte == ','){
				moveToNextNonspaceByte();
			} else {
				throwIllegalFormatExceptionWithFilePos("Unexpected symbol after "
						+ "a name-value pair in an object: "+ (char)curByte);
			}
		}
		debug("Done with parseObject: "+reader.getFilePosition()+", '"+byteToChar(curByte)+"'");
		return nodeList;
	}

	/**
	 * Search for the closing symbol matching the opening one (that was just
	 * read). Move the file cursor to the end of the current token (after
	 * closing symbol), i.e. the last read byte is the closing symbol of this
	 * token <br>
	 * Ignore opening and closing symbols within Strings. <br>
	 * In the beginning of the method the cursor should be at the symbol
	 * following the opening symbol (i.e. opening symbol has just been read and
	 * curChar==openingSymbol, filePos = filePos-of-openingSymbol + 1).
	 * 
	 * @param openingSymbol
	 * @param closingSymbol
	 * @return a position of the closing symbol for this node
	 * @throws IOException
	 *             if I/O error occur
	 * @throws IllegalFormatException
	 *             if the end of file is reached before the matching closing
	 *             symbol is met or if a non-ASCII character is met outside of
	 *             strings
	 */
	private long moveToTheEndOfToken(byte openingSymbol, byte closingSymbol)throws IOException, IllegalFormatException{
		long startPos = reader.getFilePosition() - 1;
		int opened = 1;
		try {
			while (reader.hasNext()) {
				moveToNextNonspaceByte();
				if (curByte == '"') {// String starts
					debug("Skip the string with opening quote at pos " + (reader.getFilePosition() - 1));
					reader.skipTheString();
					moveToNextByte();
					if (curByte != '"') {
						throwIllegalFormatExceptionWithFilePos(
								"Expected a quote at the end of a string, got: '" + byteToChar(curByte) + "'");
					}
					continue;
				}
				if (curByte == closingSymbol) {
					opened--;
				} else if (curByte == openingSymbol) {
					opened++;
				}
				if (opened == 0) {
					long closingSymbolPos = reader.getFilePosition() - 1;// since we've already read the closingSymbol
					return closingSymbolPos;
				}
			}
			if (opened > 0) {
				throw new IllegalFormatException("Failed to find closing symbol ");
			}
		} catch (IllegalFormatException e) {
			throwIllegalFormatExceptionWithFilePos(e.getMessage() + " while searching for the closing bracket of an "
					+ (openingSymbol == '{' ? "object" : "array") + " node (opening bracket was at pos " + startPos
					+ ")");
		}
		return startPos;
	}
	
	/**
	 * Parse "name":value pair. <br>
	 * In the beginning of the method the cursor should be at the first quote of the name <br>
	 * After this method the cursor should be at the next symbol after the
	 * value, i.e. the last symbol of the value was just read and is stored in
	 * curByte.
	 * @return
	 */
	private JSONNode parseNameValuePair()throws IOException, IllegalFormatException{
		debug("Entered parseNameValuePair at pos " + reader.getFilePosition());
		String name = null;
		try{
			name = parseString(false).getString();
		}catch(IOException e){
			throw new IOException(e.getMessage()+" while parsing name in a name-value pair");
		}
		moveToNextNonspaceByte();
		if(curByte != ':'){
			throwIllegalFormatExceptionWithFilePos("Invalid separator between "
					+ "name and value in an object");
		}
		moveToNextNonspaceByte();
		JSONNode ret = parseValue(name);
		debug("Done with parseNameValuePair: next pos = " + reader.getFilePosition() + ", last read byte = '"
				+ (char) curByte + "'");
		return ret;
	}
	
	/**
	 * Parse a value, whatever it is.<br>
	 * At the beginning of this method cursor should be at the first non-space
	 * byte of the value to parse<br>
	 * After this method the cursor should be at the next symbol after the
	 * value, i.e. the last symbol of the value was just read and is stored in
	 * curByte.
	 * 
	 * @param name
	 * @return a JSON node with name <code>name</code> and a parsed value
	 * @throws IOException
	 * @throws IllegalFormatException
	 *             if the end of file is reached before the value is read, a
	 *             non-ASCII symbol is met outside of a string or if the value
	 *             does not match the format
	 */
	private JSONNode parseValue(String name) throws IOException, IllegalFormatException{
		debug("Entered parseValue ('" + name + "'): " + (reader.getFilePosition() - 1));
		if(curByteIsWhitespace()){
			throw new RuntimeException("Current byte should be the first non-space byte of the value to parse");
		}
		JSONNonTreeNode ret = null;
		if(curByte == '{'){
			ret = new JSONNonTreeNode(JSONNode.TYPE_OBJECT, name);
			ret.setStartFilePosition(reader.getFilePosition()-1);// -1 since we've already read '{'
			ret.setEndFilePosition(moveToTheEndOfToken((byte)'{', (byte)'}'));
		} else if(curByte == '['){
			ret = new JSONNonTreeNode(JSONNode.TYPE_ARRAY, name);
			ret.setStartFilePosition(reader.getFilePosition()-1);// -1 since we've already read '['
			ret.setEndFilePosition(moveToTheEndOfToken((byte)'[', (byte)']'));
		} else if(curByte == '"'){
			StringWithCoords str = parseString(stringDisplayLength >= 0);
			ret = new JSONNonTreeNode(JSONNode.TYPE_STRING, name, str.isFullyRead(), str.getString());
			ret.setStartFilePosition(str.openingQuotePos);
			ret.setEndFilePosition(str.closingQuotePos);
		} else if(curByte == 't'){
			ret = parseKeyword(name, KEYWORD_TRUE);
		} else if(curByte == 'f'){
			ret = parseKeyword(name, KEYWORD_FALSE);
		} else if(curByte == 'n'){
			ret = parseKeyword(name, KEYWORD_NULL);
		} else {
				ret = parseNumber(name);
		} 
//		if(reader.hasNext()){
//			moveToNextByte();
//		}
		debug("Done with parseValue ('" + name + "'): "+reader.getFilePosition()+", '"+(char)curByte+"'");
		return ret;
	}

	/**
	 * Read a number and create a JSON node with the provided name. The number
	 * should match the pattern (patternNumber) The cursor after this method is
	 * at the symbol just after the number, i.e. the last symbol of the number
	 * was just read.
	 * 
	 * @param name
	 *            the name of the created JSON node
	 * @return
	 * @throws IOException
	 * @throws IllegalFormatException
	 *             if the end of file is reached before the number is read, a
	 *             non-ASCII symbol is met or if the number does not match the
	 *             format
	 */
	private JSONNonTreeNode parseNumber(String name) throws IOException, IllegalFormatException{
		debug("Entered parseNumber");
		if (curByte == ',' || curByte == '}' || curByte == ']') {
			throwIllegalFormatExceptionWithFilePos("No value for name " + name + " is found");
		}
		StringBuilder sb = new StringBuilder();
		sb.append((char)curByte);
		try {
			while (reader.hasNext()){
				// we can cast nextByte to int (in isWhitespace()) and curByte
				// to char directly here as they should be in ASCII range (i.e. byte >= 0)
				byte nextByte = reader.peekNextByte();				
				if (nextByte == ',' || nextByte == '}' || nextByte == ']' || Character.isWhitespace(nextByte)) {
					break;
				}
				moveToNextByte();
				sb.append((char) curByte);
			}
		} catch (IllegalFormatException e) {
			throwIllegalFormatExceptionWithFilePos(e.getMessage() + " while reading a number");
		}
		String number = sb.toString();
		if (!patternNumber.matcher(number).matches()) {
			throwIllegalFormatExceptionWithFilePos("Number '" + number + "' does not match the format");
		}
		debug("Done with parseNumber: " + reader.getFilePosition() + ", '" + (char) curByte + "'");
		
		return new JSONNonTreeNode(JSONNode.TYPE_NUMBER, name, number);
	}

	/**
	 * Read a keyword and create a JSON node with the provided name. The keyword
	 * should be equal to the provided one. The cursor after this method is at
	 * the symbol just after the keyword, i.e. the last symbol of the keyword
	 * was just read.
	 * 
	 * @param name
	 * @param keyword
	 * @return
	 * @throws IOException
	 * @throws IllegalFormatException if the end of file is reached before the keyword is read, a
	 *             non-ASCII symbol is met or if the number does not match the
	 *             format
	 */
	private JSONNonTreeNode parseKeyword(String name, String keyword)throws IOException, IllegalFormatException{
		debug("Entered parseKeyword '" + keyword + "'");
		int pos = 0;
		while(true) {
			if (curByte != keyword.charAt(pos)) {
				throwIllegalFormatExceptionWithFilePos("Unexpected keyword: should be '" + keyword + "', got '"
						+ (char) curByte + "' at keyword pos " + pos);
			}
			pos++;
			if(pos >= keyword.length()){
				break;
			}
			moveToNextByte();
		}
		debug("Done with parseKeyword: next pos = "+reader.getFilePosition()+", last read byte = '"+(char)curByte+"'");
		return new JSONNonTreeNode(JSONNode.TYPE_KEYWORD, name, keyword);
	}
	
	private void throwIllegalFormatExceptionWithFilePos(String msg) throws IllegalFormatException{
		throw new IllegalFormatException(
				msg + "' at " + (reader.getFilePosition() - 1) + " of file " + reader.getFileName() + ")");
	}
	
	private void debug(String msg){
		if(DEBUG)System.out.println("DEBUG: " + msg);
	}

	/**
	 * Read and parse string in "". The file is supposed to be in UTF-8 format.
	 * In the beginning of the method the cursor should be at the opening quote.
	 * <br>
	 * After this method the cursor should be at the byte just after the closing
	 * quote, i.e. the closing quote has been just read (stored in curByte) and
	 * the file position is set to the index of the next (to-be-read) byte
	 * 
	 * <br>
	 * <br>
	 * ????(how?)Checks the validity of the string. If there is a not allowed
	 * symbol, throws IllegalArgumaentException.
	 * 
	 * @param lazy
	 * @return Parsed string truncated so that its length <= <code>length</code>
	 *         . If <code>length</code> < 0, returns the whole string
	 * @throws IOException
	 *             if an I/O error occurs
	 * @throws IllegalFormatException
	 *             if end of file is reached before the closing quote is met or
	 *             string does not start or end with a quote
	 */
	StringWithCoords parseString(boolean lazy) throws IOException, IllegalFormatException{
		debug("Entered parseString at pos " + reader.getFilePosition());
		if(curByte != '"'){
			throwIllegalFormatExceptionWithFilePos("String does not start with '\"'");
		}
		long openingQuotePos = reader.getFilePosition() - 1;// since we've already read an opening quote
		StringBuilder sb = new StringBuilder();
		// if string is not empty - read it
		if (reader.peekNextByte() != '"') {
			try{
				// prepare reader for reading a string
				reader.prepareForReadingAString();
				while(reader.isReadingString()){		
					if(lazy && sb.length() >= stringDisplayLength){
						// if we are here, we stopped reading but did not reach the closing quote 
						// (due to lazy reading), so skip the rest of the string and read the closing quote
						debug("Skip the string with opening quote at pos "+ openingQuotePos);
						reader.skipTheString(); 
						break;
					}
					sb.append(reader.getNextProcessedChar());
				}
			}catch(IllegalFormatException e){
				throwIllegalFormatExceptionWithFilePos(
						e.getMessage() + " while reading a String (opening quote at pos " + openingQuotePos + ")");
			}
		}
		long closingQuotePos = reader.getFilePosition();
		moveToNextByte();
		if(curByte != '"'){
			throwIllegalFormatExceptionWithFilePos(
					"Expected a quote at the end of a string, got: '" + byteToChar(curByte) + "'");
		}
		debug("Done with parseString (lazy = " + lazy + "): " + reader.getFilePosition() + ", '" + (char) curByte
				+ "'");

		return new StringWithCoords(sb.toString(), openingQuotePos, closingQuotePos);
	}

	/**
	 * Use this method to read next byte. Primary use is to read characters
	 * outside of strings where we expect only ASCII characters.
	 * 
	 * @throws IOException
	 *             if an I/O error occurs
	 * @throws IllegalFormatException
	 *             if there are no bytes to read from file or the read byte does
	 *             not code an ASCII char
	 */
	void moveToNextByte() throws IOException, IllegalFormatException{
		curByte = reader.getNextByte();
		if(curByte < 0){
			// we expect only ASCII symbols outside of strings
			throwIllegalFormatExceptionWithFilePos("Unexpected byte outside a string: " + curByte);
		}
	}
	boolean hasNext(){
		return reader.hasNext();
	}
	/**
	 * Moves the cursor to the next non-space byte. Should be use outside of strings only! Does not check if there are characters 
	 * left to be read from file.
	 * 
	 * @throws IOException if an I/O error occurs
	 * @throws IllegalFormatException if there are no bytes to read or the read byte does
	 *             not code an ASCII char
	 */
	private void moveToNextNonspaceByte() throws IOException, IllegalFormatException{
//		curByte = reader.getNextByte();
		moveToNextByte();
		while(curByteIsWhitespace()){
//			curByte = reader.getNextByte();
			moveToNextByte();
		}
//		if(DEBUG) System.out.println("Got non-space char: '"+byteToChar(curByte)+"'");
	}
	
	/**
	 * Checks if the current byte encodes a whitespace character. We expect only
	 * ASCII characters here ( we check curByte value while reading it), so no
	 * need to be worry about converting signed byte into int
	 * 
	 * @return true if curByte encodes a whitespace character, false otherwise
	 */
	private boolean curByteIsWhitespace(){
		return Character.isWhitespace(curByte);
	}
	
	static class StringWithCoords{
		private String string;
		private long openingQuotePos;
		private long closingQuotePos;
		
		public StringWithCoords(String string, long openingQuotePos, long closingQuotePos) {
			this.string = string;
			this.openingQuotePos = openingQuotePos;
			this.closingQuotePos = closingQuotePos;
		}
		
		public String getString() {
			return string;
		}

		public long getOpeningQuotePos() {
			return openingQuotePos;
		}

		public long getClosingQuotePos() {
			return closingQuotePos;
		}
		
		public boolean isFullyRead(){
			//TODO: wrong way to estimate if the string is fully loaded:
			// length of string not always == number of bytes
			return string.length() == closingQuotePos - openingQuotePos - 1;
		}
		
		public String toString() {
			StringBuilder sb = new StringBuilder(string.length() + 100);
			sb.append('(').append(openingQuotePos).append(", ").append(closingQuotePos).append("): \"") ;
			sb.append(string).append('"');
			return sb.toString();
		}
	}
}
