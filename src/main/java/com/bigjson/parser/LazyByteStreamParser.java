package com.bigjson.parser;

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
public class LazyByteStreamParser {
	private static final String KEYWORD_TRUE = "true";
	private static final String KEYWORD_FALSE = "false";
	private static final String KEYWORD_NULL = "null";
	private static final boolean DEBUG = false; 
	private static final String defaultTopLevelName = "JSON";
	
	private int stringDisplayLength = 4;
	private UTF8FileReader reader;
	private char curChar;
	
	private Pattern patternNumber = Pattern.compile("^-?(0|([1-9][0-9]*))(.[0-9]*)?([eE][+-]?[0-9]+)?$");

	protected LazyByteStreamParser(String fileName, int stringDisplayLimit) throws IOException{
		reader = new UTF8FileReader(fileName);
		this.stringDisplayLength = stringDisplayLimit;
	}
	
	void destroy() throws IOException{
		reader.closeFileInputStream();
	}
	char getCurChar(){
		return curChar;
	} 
	/**
	 * Parse the file and return the top level node of JSON tree.  
	 * @param topLevelName specifies the name of the top-level node. If topLevelName is null 
	 * the default name ("JSON") will be used instead.
	 * @return top level node of the JSON tree
	 * @throws IOException
	 */
	protected JSONNode parseTopLevel(String topLevelName) throws IOException{
		if(DEBUG){
			System.out.println("Entered parseTopLevel. File pos = " + reader.getFilePosition());	
		}
		if(topLevelName == null){
			topLevelName = defaultTopLevelName;
		}
		// TODO: check that the file cursor is at the beginning of the file?
		moveToNextNonspaceChar();
		JSONNode root = parseValue(topLevelName);	 
		if(DEBUG){
			System.out.println("Done with parseTopLevel: "+reader.getFilePosition()+", '"+curChar+"'");
		}
		
		return root;
	}
	protected List<JSONNode> loadChildrenAtPosition(long filePos) throws IOException{
		if(!reader.getToPosition(filePos)){
			throw new IOException("Trying to set the cursor to a position that is bigger "
					+ "than the file's size: " + filePos);
		}
		try{
			moveToNextChar();
		} catch(IOException e){
			throw new IllegalArgumentException(
					e.getMessage() + " while looking for an opening bracket for an object or array");
		}
		if(curChar == '['){
			try {
				return parseArray();
			}catch(IOException e){
				throw new IllegalArgumentException(e.getMessage() + " while reading an array");
			}		
		} else if(curChar == '{'){
			try{
				return parseObject();
			}catch(IOException e){
				throw new IllegalArgumentException(e.getMessage() + " while reading an object");
			}
		} 
		throwUnexpectedSymbolException("Lazy load children: was expecting '[' or '{'");
		return null;
	}
	protected String loadStringAtPosition(long openingQuotePos, long closingQuotePos) throws IOException{
		if(!reader.getToPosition(openingQuotePos)){
			throw new IOException("Trying to set the cursor to a position that is bigger "
					+ "than the file's size: " + openingQuotePos);
		}
		try{
			moveToNextChar(); // read opening quote
		} catch(IOException e){
			throw new IllegalArgumentException(e.getMessage() + " while reading an opening quote for a String");
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
	 * Parse an array: '[' { value [, value] } ']' and create a new tree node.
	 * If <code>lazy</code> is <code>true</code> does not parse array content.
	 * In the beginning of the method the cursor should be at '['. 
	 * After this method the cursor should be at the char following the ']' if any. 
	 * @return
	 */
//	private JSONNode parseArray(String name, boolean lazy) throws IOException{
//		JSONNode arrayNode = new JSONNode(JSONNode.TYPE_ARRAY, name, level);
//		return parseArray(arrayNode, lazy);
//	}
	/**
	 * Parse an array: '[' { value [, value] } ']' and return a list of array nodes.
	 * In the beginning of the method the cursor should be at '['. 
	 * After this method the cursor should be at the char following the ']' if any. 
	 * @return
	 */
	private List<JSONNode> parseArray() throws IOException{
		if(DEBUG){
			System.out.println("Entered parseArray");
		}
		if(curChar != '['){
			throwUnexpectedSymbolException("Array should start with '[' (pos " + reader.getFilePosition() + " of file "
					+ reader.getFileName() + ")");
		}
//		JSONTreeNode arrayNode = new JSONTreeNode(JSONTreeNode.TYPE_ARRAY, name, level);
//		arrayNode.setIsFullyLoaded(!lazy);
//		if(lazy){
//			arrayNode.setFilePosition(reader.getFilePosition()-1);// -1 since we've already read '['
//			moveToTheEndOfToken('[', ']');
//			if(DEBUG){
//				System.out.println(
//						removeTab() + "Finished lazy loading of an array. File pos = " + reader.getFilePosition());
//			}
//			return arrayNode;
//		}
		List<JSONNode> nodeList = new ArrayList<JSONNode>();
		moveToNextNonspaceChar();
		int ind = 0;
		while(curChar != ']'){
			JSONNode child = parseValue(""+ind);
			nodeList.add(child);
			if(Character.isWhitespace(curChar)){
				moveToNextNonspaceChar();
			}
			if(curChar == ']'){
				break;
			}
			
			if(curChar == ','){
				moveToNextNonspaceChar();
			} else {
				throwUnexpectedSymbolException("Unexpected symbol between "
						+ "values in an array: '" + curChar + "'");
			}
			ind++;
		}
		if(reader.hasNext()){	
			moveToNextChar();
		}
		if(DEBUG){
			System.out.println("Done with parseArray: "+reader.getFilePosition()+", '"+curChar+"'");
		}
		return nodeList;
	}

//	private JSONNode parseObject(String name, boolean lazy) throws IOException{
//		JSONNode objNode = new JSONNode(JSONNode.TYPE_OBJECT, name, level);
//		return parseObject(objNode, lazy);
//	}
	/**
	 * Parse an object: '{' { string:value [, string:value] } '}'
	 * In the beginning of the method the cursor should be at '{'.
	 * After this method the cursor should be at the char following the '}' if any. 
	 * @return a list of named JSON nodes that are contained in this object
	 */
	private List<JSONNode> parseObject() throws IOException{
		if(DEBUG){
			System.out.println("Entered parseObject at pos " + reader.getFilePosition());
		}
		
		if(curChar != '{'){
			throwUnexpectedSymbolException("Object should start with '{'");
		}
		List<JSONNode> nodeList = new ArrayList<JSONNode>();
		moveToNextNonspaceChar();
		while(curChar != '}'){
			JSONNode node = parseNameValuePair(true);
			nodeList.add(node);
			if(Character.isWhitespace(curChar)){
				moveToNextNonspaceChar();
			}
			if(curChar == '}'){
				break;
			}
			if(curChar == ','){
				moveToNextNonspaceChar();
			} else {
				throwUnexpectedSymbolException("Unexpected symbol between "
						+ "name-value pairs");
			}
		}
		if(reader.hasNext()){
			moveToNextChar();
		}
		if(DEBUG){
			System.out.println("Done with parseObject: "+reader.getFilePosition()+", '"+curChar+"'");
		}
		return nodeList;
	}
	/**
	 * Moves the file cursor to the symbol just after the current token (after closing symbol),
	 * If there are no more symbols, leaves it at the closing symbol. Ignores opening and closing 
	 * symbols within Strings. 
	 * The cursor should be at the symbol following the opening symbol in the beginning of the method 
	 * (i.e. opening symbol has just been read and curChar==openingSymbol, filePos = filePos-of-openingSymbol + 1).
	 * @param openingChar
	 * @param closingChar
	 * @return file position of the closing symbol of the current token
	 * @throws IOException
	 */
	private long moveToTheEndOfToken(char openingSymbol, char closingSymbol)throws IOException{
		int opened = 1;
		while(reader.hasNext()){
			char ch = reader.getNextChar();
			if(ch == '\"'){// String starts
				reader.skipTheString();
				continue;
			}
			if(ch == closingSymbol){
				opened--;
			} else if(ch == openingSymbol){
				opened++;
			}
			
			if(opened == 0){
				long closingSymbolPos = reader.getFilePosition() - 1;// since we've already read the closingSymbol
				if(reader.hasNext()){
					moveToNextChar();
				}
				return closingSymbolPos;
			}
		}
		return -1;
	}
	
	/**
	 * Parse "string":value
	 * In the beginning of the method the cursor should be at the first '"'
	 * After this method the cursor should be at the next space symbol, if any
	 * @return
	 */
	private JSONNode parseNameValuePair(boolean lazy)throws IOException{
		if(DEBUG){
			System.out.println("Entered parseNameValuePair at pos " + reader.getFilePosition());
		}
		String name = parseString(false).getString();
		if(Character.isWhitespace(curChar)) {
			moveToNextNonspaceChar();
		}
		if(curChar != ':'){
			throwUnexpectedSymbolException("Invalid separator between "
					+ "name and value in an object");
		}
		moveToNextNonspaceChar();
		JSONNode ret = parseValue(name);
		if(DEBUG){
			System.out.println("Done with parseNameValuePair: "+reader.getFilePosition()+", '"+curChar+"'");
		}
		return ret;
	}
//	public void loadNodeChildren(JSONNode node) throws IOException{
//		
//	}
	/**
	 * Parse a value, whatever it is.
	 * After this method the cursor should be at the next symbol, if any 
	 * @return
	 */
	private JSONNode parseValue(String name) throws IOException{
		if(DEBUG){
			System.out.println("Entered parseValue");
		}
		JSONNonTreeNode ret = null;
		if(curChar == '{'){
			ret = new JSONNonTreeNode(JSONNode.TYPE_OBJECT, name);
			ret.setStartFilePosition(reader.getFilePosition()-1);// -1 since we've already read '{'
			try{
				ret.setEndFilePosition(moveToTheEndOfToken('{', '}'));
			} catch(IOException e){
				throw new IllegalArgumentException(e.getMessage()
						+ " while searching for the closing bracket of an object node starting at position "
						+ ret.getStartFilePosition());
			}
		} else if(curChar == '['){
			ret = new JSONNonTreeNode(JSONNode.TYPE_ARRAY, name);
			ret.setStartFilePosition(reader.getFilePosition()-1);// -1 since we've already read '['
			try{
				ret.setEndFilePosition(moveToTheEndOfToken('[', ']'));
			} catch(IOException e){
				throw new IllegalArgumentException(e.getMessage()
						+ " while searching for the closing bracket of an array node starting at position "
						+ ret.getStartFilePosition());
			}
		} else if(curChar == '\"'){
			StringWithCoords str = parseString(true);
			ret = new JSONNonTreeNode(JSONNode.TYPE_STRING, name, str.isFullyRead(), str.getString());
			ret.setStartFilePosition(str.openingQuotePos);
			ret.setEndFilePosition(str.closingQuotePos);
		} else if(curChar == 't'){
			ret = parseKeyword(name, KEYWORD_TRUE);
		} else if(curChar == 'f'){
			ret = parseKeyword(name, KEYWORD_FALSE);
		} else if(curChar == 'n'){
			ret = parseKeyword(name, KEYWORD_NULL);
		} else {
			ret = parseNumber(name);
		} 
//		if(Character.isWhitespace(curChar)){
//			moveToNextNonspaceChar();
//		}
		if(DEBUG){
			System.out.println("Done with parseValue: "+reader.getFilePosition()+", '"+curChar+"'");
		}
		return ret;
	}
	private JSONNonTreeNode parseNumber(String name) throws IOException{
		if(DEBUG){
			System.out.println("Entered parseNumber");
		}
		StringBuilder sb = new StringBuilder();
		while(curChar != ',' && curChar != '}' && curChar != ']' && !Character.isWhitespace(curChar)){
			sb.append((char)curChar);
			try{
				moveToNextChar();
			} catch(IOException e){
				throw new IllegalArgumentException(e.getMessage() + " while reading a number");
			}
		}
		String number = sb.toString();
		if(patternNumber.matcher(number).matches()){
			if(DEBUG){
				System.out.println("Done with parseNumber: "+reader.getFilePosition()+", '"+curChar+"'");
			}
			return new JSONNonTreeNode(JSONNode.TYPE_NUMBER, name, number);
		}
		throwUnexpectedSymbolException(number + " at pos "+
		(reader.getFilePosition()-number.length())+" is not a number");
		return null;
	}
	private JSONNonTreeNode parseKeyword(String name, String keyword)throws IOException{
		if(DEBUG){
			System.out.println("Entered parseKeyword");
		}
		for(int i=0; i< keyword.length(); i++){
			if(curChar != keyword.charAt(i)){
				throwUnexpectedSymbolException("Unexpected keyword: should be '"+keyword+"'");
			}
			try{
				moveToNextChar();
			} catch(IOException e){
				throw new IllegalArgumentException(e.getMessage() + " while reading a keyword '" + keyword + "'");
			}
		}
		if(DEBUG){
			System.out.println("Done with parseKeyword: "+reader.getFilePosition()+", '"+curChar+"'");
		}
		return new JSONNonTreeNode(JSONNode.TYPE_KEYWORD, name, keyword);
	}
	
	private void throwUnexpectedSymbolException(String msg){
		throw new IllegalArgumentException(msg +
				" (got char '"+curChar+"' at "+ (reader.getFilePosition()-1)+")");
	}

	/**
	 * Read and parse string in "". The file is supposed to be in UTF-8 format.
	 * In the beginning of the method the cursor should be at the first '"'
	 * After this method the cursor should be at the char following the closing '"' if any,
	 * meaning that the byte following closing quote has been just read (stored in curChar)
	 * and the file position is set to the index of the next (to-be-read) byte 
	 * 
	 * <br><br>
	 * ????(how?)Checks the validity of the string. If there is a not allowed symbol, throws IllegalArgumaentException.
	 *  
	 * @return Parsed string truncated so that its length <= <code>length</code>. If <code>length</code> < 0, 
	 * returns the whole string 
	 */
	StringWithCoords parseString(boolean lazy) throws IOException{
		if(DEBUG){
			System.out.println("Entered parseString at pos " + reader.getFilePosition());
		}
		if(curChar != '"'){
			throwUnexpectedSymbolException("String does not start with '\"'");
		}
		long openingQuotePos = reader.getFilePosition() - 1;// since we've already read an opening quote
		StringBuilder sb = new StringBuilder();
		try{
			moveToNextChar();
		}catch(IOException e){
			throw new IOException(e.getMessage() + " while reading the first char of a String");
		}
		while(true){
			if(reader.getReadingMode() == UTF8FileReader.MODE_READING_ASCII_CHARS){
//				moveToNextChar();// move to the symbol immediate after closing quote
				break;
			}
			if(lazy && sb.length() == stringDisplayLength){
				// if we are here, we stopped reading but did not reach the closing quote 
				// (due to lazy reading), so skip the rest of the string and read the closing quote
				reader.skipTheString();
				break;
			}
			sb.append(curChar);
			moveToNextChar();
		}
		long closingQuotePos = reader.getFilePosition() - 1;// since we've already read a closing quote
		if(reader.hasNext()){
			moveToNextChar();
		}
		if(DEBUG){
			System.out.println("Done with parseString (lazy = " + lazy + "): " + reader.getFilePosition()
					+ ", '" + curChar + "'");
		}
		
		return new StringWithCoords(sb.toString(), openingQuotePos, closingQuotePos);
	}

	void moveToNextChar() throws IOException{
		curChar = reader.getNextChar();
//		if(DEBUG){
//			System.out.println("Char = '"+curChar+"'");
//		}
	}
	boolean hasNext(){
		return reader.hasNext();
	}
	/**
	 * Moves the cursor to the next non-space char. Does not check if there are characters 
	 * left to be read from file.
	 * @throws IOException
	 */
	private void moveToNextNonspaceChar() throws IOException{
		curChar = reader.getNextChar();
		while(Character.isWhitespace(curChar)){
			curChar = reader.getNextChar();	
		}
		if(DEBUG) System.out.println("Got non-space char: '"+curChar+"'");
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
//	private static class Node implements TreeNode{
//		static final int TYPE_OBJECT = 1;
//		static final int TYPE_ARRAY = 2;
//		static final int TYPE_STRING = 3;
//		static final int TYPE_NUMBER = 4;
//		static final int TYPE_KEYWORD = 5;
//		
//		int type;
//		String value;
//		String name;
//		List<Node> children;
//		Node parent;
//		long startPos;
//		long endPos;
//		int level;
//		
//		Node(int type, String value, int level){
//			this(type, level);
//			this.value = value;
//		}
//		Node(int type, int level){
//			this.type = type;
//			this.level = level;
//			this.children = new ArrayList<Node>();
//			Arrays.fill(spaces,(byte)' ');
//		}
//		
//		void addChild(Node child){
//			children.add(child);
//			child.parent = this;
//		}
//		byte[] objStart = "{\n".getBytes();
//		byte[] arrayStart = "[\n".getBytes();
//		byte[] comma = ",\n".getBytes();
//		byte[] colon = ": ".getBytes();
//		byte[] spaces = new byte[500];
//		
//		void print(OutputStream stream)throws IOException{
//			stream.write(spaces, 0, level*2);
//			if(name != null){
//				stream.write('\"');
//				stream.write(name.getBytes());
//				stream.write('\"');
//				stream.write(colon);
//			}
//			if(type == TYPE_STRING){
//				stream.write('\"');
//				stream.write(value.getBytes(Charset.forName("UTF-8")));
//				stream.write('\"');
//			} else if(type == TYPE_KEYWORD || type == TYPE_NUMBER){
//				stream.write(value.getBytes());
//			} else if(type == TYPE_OBJECT){
//				stream.write(objStart);
//				for(int i = 0; i < children.size(); i++){
//					Node child = children.get(i);
//					child.print(stream);
//					if(i < children.size()-1){
//						stream.write(comma);
//					}
//				}
//				stream.write('\n');
//				stream.write(spaces, 0, level*2);
//				stream.write('}');
//			} else if(type == TYPE_ARRAY){
//				stream.write(arrayStart);
//				for(int i = 0; i < children.size(); i++){
//					Node child = children.get(i);
//					child.print(stream);
//					if(i < children.size()-1){
//						stream.write(comma);
//					}
//				}
//				stream.write('\n');
//				stream.write(spaces, 0, level*2);
//				stream.write(']');
//			}
//		}
//		 
//		/////// TreeNode methods ///////////////////////
//		@Override
//		public TreeNode getChildAt(int childIndex) {
//			if(childIndex < 0 || childIndex >= children.size()){
//				throw new IndexOutOfBoundsException("Asks for child #"+ childIndex +
//						" (have "+children.size()+" children)");
//			}
//			return children.get(childIndex);
//		}
//		@Override
//		public int getChildCount() {
//			return children.size();
//		}
//		@Override
//		public TreeNode getParent() {
//			return parent;
//		}
//		@Override
//		public int getIndex(TreeNode node) {
//			if(node == null) {
//				return -1;
//			}
//			for(int i = 0; i < children.size(); i++){
//				if(node.equals(children.get(i))){
//					return i;
//				}
//			}
//			return -1;
//		}
//		@Override
//		public boolean getAllowsChildren() {
//			return type == TYPE_ARRAY || type == TYPE_OBJECT;
//		}
//		@Override
//		public boolean isLeaf() {
//			return children.size() == 0;
//		}
//		@Override
//		public Enumeration children() {
//			return Collections.enumeration(children);
//		}
//		/////// End of TreeNode methods ///////////////////////
//		
//		
//	}

	
}
