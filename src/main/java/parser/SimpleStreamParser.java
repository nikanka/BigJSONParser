package main.java.parser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
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
public class SimpleStreamParser {
	
	public static final String KEYWORD_TRUE = "true";
	public static final String KEYWORD_FALSE = "false";
	public static final String KEYWORD_NULL = "null";
	
	private static final boolean DEBUG = true; 
	private String tabString = "";
	
	private Reader reader;
	private char curChar;
	private Pattern patternUnicodeNumbers = Pattern.compile("[\\da-fA-F]{4}");
	private Pattern patternNumber = Pattern.compile("^-?(0|([1-9][0-9]*))(.[0-9]+)?([eE][+-]?[0-9]+)?$");
	
	public static void main(String[] args) throws IOException{
//		char ch = '\"';
//		int chInt = 0x0022;
//		System.out.println(ch+(ch==chInt?" == ":" != ")+chInt);
		StringBuilder sb = new StringBuilder();
//		//sb.appendCodePoint(chInt);
//		//sb.appendCodePoint((int)'й');
//		sb.append("\uD100");
//		sb.append("\uD100");
//		sb.append("\\u005C");
//		System.out.println(sb.toString());
//		System.out.println(sb.toString().length());
		
		String fileName = "SmallTest2.json";
		SimpleStreamParser parser = new SimpleStreamParser(new File(fileName));
		parser.parseIntoFullTree();
		
	}
	public SimpleStreamParser(String input){
		reader = new Reader(input);
	}
	
	public SimpleStreamParser(File file) throws IOException{
		reader = new Reader(file);
	}
	
	public Node parseIntoFullTree(){
		if(DEBUG){
			System.out.println("Entered parseIntoFullTree");
			
		}
		moveToNextNonspaceChar();
		Node root = null;
		if(curChar == '{'){
			root = parseObject();
		} else if(curChar == '['){
			root = parseArray();
		} else {
			throwUnexpectedSymbolException("Unexpected symbol: should be '{' or '['");
		}
		if(DEBUG){
			System.out.println("Done with parseIntoFullTree: "+reader.curPos+", '"+curChar+"'");
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
	 * In the beginning of the method the cursor should be at '['
	 * After this method the cursor should be at the first non-space symbol after ']' 
	 * @return
	 */
	private Node parseArray() {
		if(DEBUG){
			System.out.println(addTab()+"Entered parseArray");
		}
		
		if(curChar != '['){
			throwUnexpectedSymbolException("Array should start with '['");
		}
		Node arrayNode = new Node(Node.TYPE_ARRAY);
		moveToNextNonspaceChar();
		
		while(curChar != ']'){
			Node child = parseValue();
			arrayNode.addChild(child);
			
			if(curChar == ']'){
				break;
			}
			
			if(curChar == ','){
				moveToNextNonspaceChar();
			} else {
				throwUnexpectedSymbolException("Unexpected symbol between "
						+ "values in an array");
			}
		}
		if(reader.hasNext()){	
			moveToNextNonspaceChar();
		}
		if(DEBUG){
			System.out.println(removeTab()+"Done with parseArray: "+reader.curPos+", '"+curChar+"'");
		}

		return arrayNode;
	}

	/**
	 * Parse an object: '{' { string:value [, string:value] } '}'
	 * In the beginning of the method the cursor should be at '{'
	 * After this method the cursor should be at the first non-space symbol after '}' 
	 * @return
	 */
	private Node parseObject(){
		if(DEBUG){
			System.out.println(addTab()+"Entered parseObject");
		}

		if(curChar != '{'){
			throwUnexpectedSymbolException("Object should start with '{'");
		}
		Node objNode = new Node(Node.TYPE_OBJECT);
		moveToNextNonspaceChar();
		
		while(curChar != '}'){
			Node child = parseNameValuePair();
			objNode.addChild(child);
			
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
			moveToNextNonspaceChar();
		}
		if(DEBUG){
			System.out.println(removeTab()+"Done with parseObject: "+reader.curPos+", '"+curChar+"'");
		}
		return objNode;
	}
	/**
	 * Parse "string":value
	 * In the beginning of the method the cursor should be at the first '"'
	 * After this method the cursor should be at the next non-space symbol (should be ',' or '}')
	 * @return
	 */
	private Node parseNameValuePair(){
		if(DEBUG){
			System.out.println(addTab() + "Entered parseNameValuePair");
		}
		String name = parseString();
		if(curChar != ':'){
			throwUnexpectedSymbolException("Invalid separator between "
					+ "name and value in an object");
		}
		moveToNextNonspaceChar();
		Node ret = parseValue();
		ret.name = name;
		if(DEBUG){
			System.out.println(removeTab() + "Done with parseNameValuePair: "+reader.curPos+", '"+curChar+"'");
		}
		return ret;
	}
	/**
	 * Parse a value, whatever it is
	 * After this method the cursor should be at the next non-space symbol 
	 * @return
	 */

	private Node parseValue() {
		if(DEBUG){
			System.out.println(addTab() + "Entered parseValue");
		}
		Node ret = null;
		if(curChar == '{'){
			ret = parseObject();
		} else if(curChar == '['){
			ret = parseArray();
		} else if(curChar == '\"'){
			ret = new Node(Node.TYPE_STRING, parseString());
		} else if(curChar == 't'){
			ret = parseKeyword(KEYWORD_TRUE);
		} else if(curChar == 'f'){
			ret = parseKeyword(KEYWORD_FALSE);
		} else if(curChar == 'n'){
			ret = parseKeyword(KEYWORD_NULL);
		} else {
			ret = parseNumber();
		} 
		if(DEBUG){
			System.out.println(removeTab() + "Done with parseValue: "+reader.curPos+", '"+curChar+"'");
		}
		return ret;
	}
	private Node parseNumber(){
		if(DEBUG){
			System.out.println(addTab() + "Entered parseNumber");
		}
		StringBuilder sb = new StringBuilder();
		while(curChar != ',' && curChar != '}' && curChar != ']'){
			sb.append(curChar);
			moveToNextChar();
		}
		String number = sb.toString();
		if(patternNumber.matcher(number).matches()){
			if(DEBUG){
				System.out.println(removeTab() + "Done with parseNumber: "+reader.curPos+", '"+curChar+"'");
			}
			return new Node(Node.TYPE_NUMBER, number);
		}
		throwUnexpectedSymbolException(number + " at pos "+
		(reader.curPos-number.length())+" is not a number");
		return null;
	}
	private Node parseKeyword(String keyword){
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
			System.out.println(removeTab() + "Done with parseKeyword: "+reader.curPos+", '"+curChar+"'");
		}
		return new Node(Node.TYPE_KEYWORD, keyword);
	}
	
	private void throwUnexpectedSymbolException(String msg){
		throw new IllegalArgumentException(msg +
				" (got char '"+curChar+"' at "+ reader.curPos+")");
	}

	/**
	 * Parse string in ""
	 * In the beginning of the method the cursor should be at the first '"'
	 * After this method the cursor should be at the next non-space symbol 
	 * @return
	 */
	private String parseString() {
		if(DEBUG){
			System.out.println(addTab() + "Entered parseString");
		}
		if(curChar != '"'){
			throwUnexpectedSymbolException("String does not start with '\"'");
		}
		StringBuilder sb = new StringBuilder();
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
		moveToNextNonspaceChar();
		if(DEBUG){
			System.out.println(removeTab() + "Done with parseString: "+reader.curPos+", '"+curChar+"'");
		}
		

		return sb.toString();
	}

	private void moveToNextChar(){
		curChar = reader.nextChar();
		if(DEBUG){
			System.out.println("Char = '"+curChar+"'");
		}
	}
	private void moveToNextNonspaceChar(){
		curChar = reader.nextNonspaceChar();
	}
	
	private static class Node {
		static final int TYPE_OBJECT = 1;
		static final int TYPE_ARRAY = 2;
		static final int TYPE_STRING = 3;
		static final int TYPE_NUMBER = 4;
		static final int TYPE_KEYWORD = 5;
		
		int type;
		String value;
		String name;
		List<Node> children;
		
		Node(int type, String value){
			this(type);
			this.value = value;
		}
		Node(int type){
			this.type = type;
			this.children = new ArrayList<Node>();
		}
		
		void addChild(Node child){
			children.add(child);
		}
		
		
	}
	
//	private static class Reader{
//		String input;
//		long curPos = -1;
//		int nextChar = -1;
////		char prevChar;
////		char curChar;
//		
//		Reader(String input){
//			this.input = input;
//			if(input.length()>0){				
//				nextChar = input.charAt(0);
//			}
//		}
//		
//		Reader(File file)throws IOException{	
//			this(new String(Files.readAllBytes(Paths.get(file.getAbsolutePath())),"UTF-8"));
//		}
//		
//		char nextChar(){
////			prevChar = curChar;
//			if(!hasNext()){
//				throw new IllegalArgumentException("Unexpected end of text "
//						+ "at position "+curPos);
//			}
//			return input.charAt((int)++curPos);
//		}
//		
//		char nextNonspaceChar(){
//			char ch = nextChar();
//			while(Character.isWhitespace(ch)){
//				ch = nextChar();
//			}
//			return ch;
//		}
//		
//		void setToPos(long pos){
//			curPos = pos;
//		}
//		
////		char getPreviousChar(){
////			return prevChar;
////		}
//		
//		boolean hasNext(){
//			return curPos < input.length()-1;
//		}
//		
//	}
	private static class Reader{
		String input;
		long curPos = -1;
		int nextChar = -1;
//		char prevChar;
//		char curChar;
		
		Reader(String input){
			this.input = input;
			if(input.length()>0){				
				nextChar = input.charAt(0);
			}
		}
		
		Reader(File file)throws IOException{	
			this(new String(Files.readAllBytes(Paths.get(file.getAbsolutePath())),"UTF-8"));
		}
		
		char nextChar(){
//			prevChar = curChar;
			if(!hasNext()){
				throw new IllegalArgumentException("Unexpected end of text "
						+ "at position "+curPos);
			}
			return input.charAt((int)++curPos);
		}
		
		char nextNonspaceChar(){
			char ch = nextChar();
			while(Character.isWhitespace(ch)){
				ch = nextChar();
			}
			return ch;
		}
		
		void setToPos(long pos){
			curPos = pos;
		}
		
//		char getPreviousChar(){
//			return prevChar;
//		}
		
		boolean hasNext(){
			return curPos < input.length()-1;
		}
		
	}

}
