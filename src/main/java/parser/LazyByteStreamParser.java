package main.java.parser;

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
	
	private int stringDisplayLength = 4;
	private UTF8FileReader reader;
	private char curChar;
	
	private Pattern patternNumber = Pattern.compile("^-?(0|([1-9][0-9]*))(.[0-9]+)?([eE][+-]?[0-9]+)?$");

	public static void main(String[] args) throws IOException{
//		char ch = '\"';
//		int chInt = 0x0022;
//		System.out.println(ch+(ch==chInt?" == ":" != ")+chInt);
//		StringBuilder sb = new StringBuilder();
//		//sb.appendCodePoint(chInt);
//		//sb.appendCodePoint((int)'й');
//		sb.append("\uD100");
//		sb.append("\uD100");
//		sb.append("\\u005C");
//		System.out.println(sb.toString());
//		System.out.println(sb.toString().length());
		
		String fileName = "SmallTest2.json";
		LazyByteStreamParser parser = new LazyByteStreamParser(fileName);
//		parser.reader.getToPosition(11);
//		System.out.println("pos "+parser.reader.getFilePosition()+": '"+(char)parser.reader.getNextByte()+"'");
//		System.out.println("pos "+parser.reader.getFilePosition()+": '"+(char)parser.reader.getNextByte()+"'");
//		System.out.println("pos "+parser.reader.getFilePosition()+": '"+(char)parser.reader.getNextByte()+"'");
		JSONNode root = parser.parseTopLevel();
		for(JSONNode node: parser.loadChildrenAtPosition(root.getStartFilePosition())){
			System.out.println(node.getName());
		}
//		root.print(System.out);
//		System.out.flush();
		
		
	}
	protected LazyByteStreamParser(String fileName) throws IOException{
		reader = new UTF8FileReader(fileName);
	}
	protected JSONNode parseTopLevel() throws IOException{
		if(DEBUG){
			System.out.println("Entered parseTopLevel. File pos = " + reader.getFilePosition());	
		}
		// TODO: check that the file cursor is at the beginning of the file?
		moveToNextNonspaceChar();
		JSONNode root = parseValue(null);		
		if(DEBUG){
			System.out.println("Done with parseTopLevel: "+reader.getFilePosition()+", '"+curChar+"'");
		}
		
		return root;
	}
	protected List<JSONNode> loadChildrenAtPosition(long filePos) throws IOException{
		reader.getToPosition(filePos);
		moveToNextChar();
		if(curChar == '['){
			return parseArray();
		} else if(curChar == '{'){
			return parseObject();
		} 
		throwUnexpectedSymbolException("Lazy load children: was expecting '[' or '{'");
		return null;
		// TODO: full loading for Strings
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
			throwUnexpectedSymbolException("Array should start with '['");
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
			System.out.println("Entered parseObject");
		}
		
		if(curChar != '{'){
			throwUnexpectedSymbolException("Object should start with '{'");
		}
////		JSONTreeNode objNode = new JSONTreeNode(JSONTreeNode.TYPE_OBJECT, name, level);
//		objNode.setIsFullyLoaded(!lazy);
//		if(lazy){
//			objNode.setFilePosition(reader.getFilePosition()-1);// -1 since we've already read '{'
//			moveToTheEndOfToken('{', '}');
//			if(DEBUG){
//				System.out.println(
//						removeTab() + "Finished lazy loading of an object. File pos = " + reader.getFilePosition());
//			}
//			return objNode;
//		}
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
		System.out.println("End Of Object");
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
	 * The cursor should be at (i.e. just before) the opening symbol in the beginning of the method.
	 * @param openingChar
	 * @param closingChar
	 * @return file position where the cursor is
	 * @throws IOException
	 */
//	private long moveToTheEndOfToken(byte openingSymbol, byte closingSymbol)throws IOException{
//		int opened = 1;
//		boolean inString = false;
//		byte prevByte = ' ';
//		while(reader.hasNext()){
//			byte b = reader.getNextByte();
//			if(b == '\"'){
//				if(!inString){
//					inString = true;
//				} else if(prevByte != '\\'){
//					inString = false;
//				}
//			}
//			if(!inString){
//				if(b == closingSymbol){
//					opened--;
//				} else if(b == openingSymbol){
//					opened++;
//				}
//			}
//			if(opened == 0){
//				if(reader.hasNext()){
//					moveToNextChar();
//				}
//				return reader.getFilePosition();
//			}
//			prevByte = b;
//		}
//		return -1;
//	}
//	
	
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
				if(reader.hasNext()){
					moveToNextChar();
				}
				return reader.getFilePosition();
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
			System.out.println("Entered parseNameValuePair");
		}
		String name = parseString(false).getString();
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
		JSONNode ret = null;
		if(curChar == '{'){
			ret = new JSONNode(JSONNode.TYPE_OBJECT, name);
			ret.setIsFullyLoaded(false);
			ret.setStartFilePosition(reader.getFilePosition()-1);// -1 since we've already read '{'
			moveToTheEndOfToken('{', '}');
		} else if(curChar == '['){
			ret = new JSONNode(JSONNode.TYPE_ARRAY, name);
			ret.setIsFullyLoaded(false);
			ret.setStartFilePosition(reader.getFilePosition()-1);// -1 since we've already read '{'
			moveToTheEndOfToken('[', ']');
		} else if(curChar == '\"'){
			StringWithCoords str = parseString(true);
			ret = new JSONNode(JSONNode.TYPE_STRING, name, str.getString());
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
	private JSONNode parseNumber(String name) throws IOException{
		if(DEBUG){
			System.out.println("Entered parseNumber");
		}
		StringBuilder sb = new StringBuilder();
		while(curChar != ',' && curChar != '}' && curChar != ']' && !Character.isWhitespace(curChar)){
			sb.append((char)curChar);
			moveToNextChar();
		}
		String number = sb.toString();
		if(patternNumber.matcher(number).matches()){
			if(DEBUG){
				System.out.println("Done with parseNumber: "+reader.getFilePosition()+", '"+curChar+"'");
			}
			return new JSONNode(JSONNode.TYPE_NUMBER, name, number);
		}
		throwUnexpectedSymbolException(number + " at pos "+
		(reader.getFilePosition()-number.length())+" is not a number");
		return null;
	}
	private JSONNode parseKeyword(String name, String keyword)throws IOException{
		if(DEBUG){
			System.out.println("Entered parseKeyword");
		}
		for(int i=0; i< keyword.length(); i++){
			if(curChar != keyword.charAt(i)){
				throwUnexpectedSymbolException("Unexpected keyword: should be '"+keyword+"'");
			}
			moveToNextChar();
		}
		if(DEBUG){
			System.out.println("Done with parseKeyword: "+reader.getFilePosition()+", '"+curChar+"'");
		}
		return new JSONNode(JSONNode.TYPE_KEYWORD, name, keyword);
	}
	
	private void throwUnexpectedSymbolException(String msg){
		throw new IllegalArgumentException(msg +
				" (got char '"+curChar+"' at "+ reader.getFilePosition()+")");
	}

	/**
	 * Read and parse string in "". The file is supposed to be in UTF-8 format.
	 * In the beginning of the method the cursor should be at the first '"'
	 * After this method the cursor should be at the char following the closing '"' if any.
	 * 
	 * <br><br>
	 * ????(how?)Checks the validity of the string. If there is a not allowed symbol, throws IllegalArgumaentException.
	 *  
	 * @return Parsed string truncated so that its length <= <code>length</code>. If <code>length</code> < 0, 
	 * returns the whole string 
	 */
	//TODO: does this method indeed check the validity of the string?
	private StringWithCoords parseString(boolean lazy) throws IOException{
		if(DEBUG){
			System.out.println("Entered parseString");
		}
		if(curChar != '"'){
			throwUnexpectedSymbolException("String does not start with '\"'");
		}
		long openingQuotePos = reader.getFilePosition() - 1;// since we've already read an opening quote
		StringBuilder sb = new StringBuilder();
		moveToNextChar();
		while(true){
			sb.append(curChar);
			if(reader.getReadingMode() == UTF8FileReader.MODE_READING_CLOSING_QUOTE){
				moveToNextChar();// reading closing quote will change to reading ASCII
				break;
			}
			if(lazy && sb.length() == stringDisplayLength){
				// if we are here, we stopped reading but did not reach the closing quote 
				// (due to lazy reading), so skip the rest of the string and read the closing quote
				reader.skipTheString();
				break;
			}
			moveToNextChar();
		}
		long closingQuotePos = reader.getFilePosition() - 1;// since we've already read a closing quote
//		if(reader.hasNext()){
//			moveToNextChar();
//		}
		if(DEBUG){
			System.out.println("Done with parseString (lazy = " + lazy + "): " + reader.getFilePosition()
					+ ", '" + curChar + "'");
		}
		
		return new StringWithCoords(sb.toString(), openingQuotePos, closingQuotePos);
	}

	private void moveToNextChar() throws IOException{
		curChar = reader.getNextChar();
		if(DEBUG){
			System.out.println("Char = '"+curChar+"'");
		}
	}
	/**
	 * Moves the cursor to the next non-space char. Does not check if there are characters 
	 * left to be read from file, so should be used only when there should be some non-space
	 * characters to read.
	 * @throws IOException
	 */
	private void moveToNextNonspaceChar() throws IOException{
		curChar = reader.getNextChar();
		while(Character.isWhitespace(curChar)){
			curChar = reader.getNextChar();	
		}
		if(DEBUG) System.out.println("Got non-space char: '"+curChar+"'");
	}
	
	private class StringWithCoords{
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
