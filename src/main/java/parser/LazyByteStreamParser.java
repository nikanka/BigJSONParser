package main.java.parser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Grammar in EBNF:
 * 
 * object	: '{' { STRING : value [, STRING : value] } '}'
 * array	: '['  {value [, value] } ']'
 * value	: STRING | NUMBER | object | array | 'true' | 'false' | 'null'
 * STRING	: '"' {}   '"' 
 * NUMBER	:
 * @author nikanka
 *
 */
public class LazyByteStreamParser {
	public static final String KEYWORD_TRUE = "true";
	public static final String KEYWORD_FALSE = "false";
	public static final String KEYWORD_NULL = "null";
	public final static int STRING_DISPLAY_LENGTH = 20;

	
	private static final boolean DEBUG = false; 
	
	private UTF8CharFileReader reader;
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
//		root.print(System.out);
//		System.out.flush();
		
		
	}
	public LazyByteStreamParser(String fileName) throws IOException{
		reader = new UTF8CharFileReader(fileName);
	}
	public JSONNode parseTopLevel() throws IOException{
		if(DEBUG){
			System.out.println("Entered parseIntoFullTree");
			
		}
		moveToNextNonspaceChar();
		JSONNode root = parseValue(null);
//		String rootName = "JSON";
//		if(curChar == '{'){
//			root = parseObject(rootName);
//		} else if(curChar == '['){
//			root = parseArray(rootName);
//		} else {
//			throwUnexpectedSymbolException("Unexpected symbol: should be '{' or '['");
//		}
		
		if(DEBUG){
			System.out.println("Done with parseIntoFullTree: "+reader.getFilePosition()+", '"+curChar+"'");
		}
		
		return root;
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
	 * Parse an array: '[' { value [, value] } ']' and put the data into provided 
	 * <code>arrayNode</code>. If <code>lazy</code> is <code>true</code> does not parse array content.
	 * In the beginning of the method the cursor should be at '['. 
	 * After this method the cursor should be at the char following the ']' if any. 
	 * @return
	 */
	private List<JSONNode> parseArray(JSONNode arrayNode, boolean lazy) throws IOException{
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
						+ "values in an array");
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
	 * @return
	 */
	private List<JSONNode> parseObject(JSONNode objNode, boolean lazy) throws IOException{
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
	 * If there are no more symbols, leaves it at the closing symbol. Ignores opening anf closing 
	 * symbols within Strings. 
	 * @param openingChar
	 * @param closingChar
	 * @return file position where the cursor is
	 * @throws IOException
	 */
	private long moveToTheEndOfToken(char openingChar, char closingChar)throws IOException{
		int opened = 1;
		boolean inString = false;
		char prevChar = ' ';
		while(reader.hasNext()){
			char ch = reader.getNextChar();
			if(ch == '\"'){
				if(!inString){
					inString = true;
				} else if(prevChar != '\\'){
					inString = false;
				}
			}
			if(!inString){
				if(ch == closingChar){
					opened--;
				} else if(ch == openingChar){
					opened++;
				}
			}
			if(opened == 0){
				if(reader.hasNext()){
					moveToNextChar();
				}
				return reader.getFilePosition();
			}
			prevChar = ch;
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
		String name = parseString(false);
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
	
	public void loadNodeChildren(JSONNode node) throws IOException{
		reader.getToPosition(node.getFilePosition());
		moveToNextChar();
		if(node.getType() == JSONNode.TYPE_ARRAY){
			parseArray(node, false);
		} else if(node.getType() == JSONNode.TYPE_OBJECT){
			parseObject(node, false);
		}
		// TODO: full loading for Strings
	}
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
			ret.setFilePosition(reader.getFilePosition()-1);// -1 since we've already read '{'
			moveToTheEndOfToken('{', '}');
		} else if(curChar == '['){
			ret = new JSONNode(JSONNode.TYPE_ARRAY, name);
			ret.setIsFullyLoaded(false);
			ret.setFilePosition(reader.getFilePosition()-1);// -1 since we've already read '{'
			moveToTheEndOfToken('[', ']');
		} else if(curChar == '\"'){
			long strPos = reader.getFilePosition();
			ret = new JSONNode(JSONNode.TYPE_STRING, name, parseString(true));
			ret.setFilePosition(strPos-1);
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
	 * Checks the validity of the string. If there is a not allowed symbol, throws IllegalArgumaentException.
	 *  
	 * @return Parsed string truncated so that its length <= <code>length</code>. If <code>length</code> < 0, 
	 * returns the whole string 
	 */
	private String parseString(boolean lazy) throws IOException{
		if(DEBUG){
			System.out.println("Entered parseString");
		}
		if(curChar != '"'){
			throwUnexpectedSymbolException("String does not start with '\"'");
		}
		StringBuilder sb = new StringBuilder();
		moveToNextChar();
		for(int i = 0; i < STRING_DISPLAY_LENGTH; i++){
			if(reader.getReadingMode() != UTF8CharFileReader.MODE_READING_UTF8_CHARS){
				break;
			}
			sb.append(curChar);
			moveToNextChar();
		}
		while(reader.getReadingMode() != UTF8CharFileReader.MODE_READING_ASCII_CHARS){
			moveToNextChar();
		}
			
		
//		while(true){
//			if(Character.getType(curChar)==Character.CONTROL){
//				throwUnexpectedSymbolException("Control symbol within a string");
//			}
//			if(curChar == '\\'){
//				sb.append(curChar);
//				moveToNextChar();
//				if(curChar == 'u'){
//					sb.append(curChar);
//					for(int i = 0; i < 4; i++){
//						moveToNextChar();
//						if(!Character.isDigit(curChar) && 
//								(Character.toLowerCase(curChar) > 'f' ||
//										Character.toLowerCase(curChar) < 'a')){
//							throwUnexpectedSymbolException("Non-hexadecimal "
//									+ "digit after \\u");
//						}
//						sb.append(curChar);
//					}
//				} else if (curChar == '\"' || curChar == '\\' || curChar == '/' || curChar == 'b'
//						|| curChar == 'f' || curChar == 'n' || curChar == 'r' || curChar == 't') {
//					sb.append(curChar);
//				} else{
//					throwUnexpectedSymbolException("Illegal symbol after '\\' in a string");
//				}
//			} 
//			if(curChar == '\"' && prevChar == '\\'){
//				break;
//			}
//			sb.append(curChar);
//			moveToNextChar();
//		}	
		if(reader.hasNext()){
			moveToNextChar();
		}
		if(DEBUG){
			System.out.println("Done with parseString (lazy = " + lazy + "): " + reader.getFilePosition()
					+ ", '" + curChar + "'");
		}
		

		return sb.toString();
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
