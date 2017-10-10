package main.java.parser;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Pattern;

import javax.swing.tree.TreeNode;

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
public class ByteStreamParser {
	public static final String KEYWORD_TRUE = "true";
	public static final String KEYWORD_FALSE = "false";
	public static final String KEYWORD_NULL = "null";
	
	private static final boolean DEBUG = false; 
	private String tabString = "";
	
	private UTF8CharFileReader reader;
	private char curChar;
	private int level = 0;
	
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
		ByteStreamParser parser = new ByteStreamParser(fileName);
		JSONTreeNode root = parser.parseIntoFullTree();
		root.print(System.out);
		System.out.flush();
		
		
	}
	public ByteStreamParser(String fileName) throws IOException{
		reader = new UTF8CharFileReader(fileName);
	}
	public JSONTreeNode parseIntoFullTree() throws IOException{
		if(DEBUG){
			System.out.println("Entered parseIntoFullTree");
			
		}
		moveToNextNonspaceChar();
		JSONTreeNode root = parseValue(null);
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
	private String addTab(){
		tabString = tabString+" ";
		return tabString;
	}
	private String removeTab(){
		String ret = tabString;
		tabString = tabString.substring(1);
		return ret;
	}
	/**
	 * Parse an array: '[' { value [, value] } ']'
	 * In the beginning of the method the cursor should be at '['. 
	 * After this method the cursor should be at the char following the ']' if any. 
	 * @return
	 */
	private JSONTreeNode parseArray(String name) throws IOException{
		if(DEBUG){
			System.out.println(addTab()+"Entered parseArray");
		}
		if(curChar != '['){
			throwUnexpectedSymbolException("Array should start with '['");
		}
		JSONTreeNode arrayNode = new JSONTreeNode(JSONTreeNode.TYPE_ARRAY, name, level);
		moveToNextNonspaceChar();
		level++;
		int ind = 0;
		while(curChar != ']'){
			JSONTreeNode child = parseValue(""+ind);
			arrayNode.addChild(child);
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
		level--;
		if(reader.hasNext()){	
			moveToNextChar();
		}
		if(DEBUG){
			System.out.println(removeTab()+"Done with parseArray: "+reader.getFilePosition()+", '"+curChar+"'");
		}
		return arrayNode;
	}

	/**
	 * Parse an object: '{' { string:value [, string:value] } '}'
	 * In the beginning of the method the cursor should be at '{'.
	 * After this method the cursor should be at the char following the '}' if any. 
	 * @return
	 */
	private JSONTreeNode parseObject(String name) throws IOException{
		if(DEBUG){
			System.out.println(addTab()+"Entered parseObject");
		}
		
		if(curChar != '{'){
			throwUnexpectedSymbolException("Object should start with '{'");
		}
		JSONTreeNode objNode = new JSONTreeNode(JSONTreeNode.TYPE_OBJECT, name, level);
		moveToNextNonspaceChar();
		level++;
		while(curChar != '}'){
			JSONTreeNode child = parseNameValuePair();
			objNode.addChild(child);
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
		level--;
		System.out.println("End Of Object");
		if(reader.hasNext()){
			moveToNextChar();
		}
		if(DEBUG){
			System.out.println(removeTab()+"Done with parseObject: "+reader.getFilePosition()+", '"+curChar+"'");
		}
		return objNode;
	}
	/**
	 * Parse "string":value
	 * In the beginning of the method the cursor should be at the first '"'
	 * After this method the cursor should be at the next non-space symbol (should be ',' or '}')
	 * @return
	 */
	private JSONTreeNode parseNameValuePair()throws IOException{
		if(DEBUG){
			System.out.println(addTab() + "Entered parseNameValuePair");
		}
		String name = parseString();
		if(curChar != ':'){
			throwUnexpectedSymbolException("Invalid separator between "
					+ "name and value in an object");
		}
		moveToNextNonspaceChar();
		JSONTreeNode ret = parseValue(name);
		if(DEBUG){
			System.out.println(removeTab() + "Done with parseNameValuePair: "+reader.getFilePosition()+", '"+curChar+"'");
		}
		return ret;
	}
	/**
	 * Parse a value, whatever it is
	 * After this method the cursor should be at the next non-space symbol 
	 * @return
	 */

	private JSONTreeNode parseValue(String name) throws IOException{
		if(DEBUG){
			System.out.println(addTab() + "Entered parseValue");
		}
		JSONTreeNode ret = null;
		if(curChar == '{'){
			ret = parseObject(name);
		} else if(curChar == '['){
			ret = parseArray(name);
		} else if(curChar == '\"'){
			ret = new JSONTreeNode(JSONTreeNode.TYPE_STRING, name, parseString(), level);
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
			System.out.println(removeTab() + "Done with parseValue: "+reader.getFilePosition()+", '"+curChar+"'");
		}
		return ret;
	}
	private JSONTreeNode parseNumber(String name) throws IOException{
		if(DEBUG){
			System.out.println(addTab() + "Entered parseNumber");
		}
		StringBuilder sb = new StringBuilder();
		while(curChar != ',' && curChar != '}' && curChar != ']' && !Character.isWhitespace(curChar)){
			sb.append((char)curChar);
			moveToNextChar();
		}
		String number = sb.toString();
		if(patternNumber.matcher(number).matches()){
			if(DEBUG){
				System.out.println(removeTab() + "Done with parseNumber: "+reader.getFilePosition()+", '"+curChar+"'");
			}
			return new JSONTreeNode(JSONTreeNode.TYPE_NUMBER, name, number, level);
		}
		throwUnexpectedSymbolException(number + " at pos "+
		(reader.getFilePosition()-number.length())+" is not a number");
		return null;
	}
	private JSONTreeNode parseKeyword(String name, String keyword)throws IOException{
		if(DEBUG){
			System.out.println(addTab() + "Entered parseKeyword");
		}
		for(int i=0; i< keyword.length(); i++){
			if(curChar != keyword.charAt(i)){
				throwUnexpectedSymbolException("Unexpected keyword: should be '"+keyword+"'");
			}
			moveToNextChar();
		}
		if(DEBUG){
			System.out.println(removeTab() + "Done with parseKeyword: "+reader.getFilePosition()+", '"+curChar+"'");
		}
		return new JSONTreeNode(JSONTreeNode.TYPE_KEYWORD, name, keyword, level);
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
	private String parseString() throws IOException{
		if(DEBUG){
			System.out.println(addTab() + "Entered parseString");
		}
		if(curChar != '"'){
			throwUnexpectedSymbolException("String does not start with '\"'");
		}
		StringBuilder sb = new StringBuilder();
//		byte[] stringBytes = new  
		moveToNextChar();
		while(true){
			if(Character.getType(curChar)==Character.CONTROL){
				throwUnexpectedSymbolException("Control symbol within a string");
			}
			if(curChar == '\\'){
				sb.append(curChar);
				moveToNextChar();
				if(curChar == 'u'){
					sb.append(curChar);
					for(int i = 0; i < 4; i++){
						moveToNextChar();
						if(!Character.isDigit(curChar) && 
								(Character.toLowerCase(curChar) > 'f' ||
										Character.toLowerCase(curChar) < 'a')){
							throwUnexpectedSymbolException("Non-hexadecimal "
									+ "digit after \\u");
						}
						sb.append(curChar);
					}
				} else if (curChar == '\"' || curChar == '\\' || curChar == '/' || curChar == 'b'
						|| curChar == 'f' || curChar == 'n' || curChar == 'r' || curChar == 't') {
					sb.append(curChar);
				} else{
					throwUnexpectedSymbolException("Illegal symbol after '\\' in a string");
				}
			} 
			if(curChar == '\"'){
				break;
			}
			sb.append(curChar);
			moveToNextChar();
		}	
		if(reader.hasNext()){
			moveToNextChar();
		}
		if(DEBUG){
			System.out.println(removeTab() + "Done with parseString: "+reader.getFilePosition()+", '"+curChar+"'");
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
