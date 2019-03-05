package com.bigjson.parser;

import static org.junit.Assert.*;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.bigjson.parser.LazyByteStreamParser;
import com.bigjson.parser.UTF8FileReader;
import com.bigjson.parser.LazyByteStreamParser.StringWithCoords;


public class LazyByteStreamParserTest {
	
	private static boolean DEBUG = true;
	
	private static void debug(String toPrint){
		if(DEBUG){
			System.out.println("TEST: " + toPrint);
		}
	}
	
	@Rule
    public ExpectedException thrown = ExpectedException.none();


	@Test
	public void shouldParseSmallJSONFile() throws IOException, IllegalFormatException{
		String fileName = "testFiles/SmallTest0.json";
		parseAndCompare(fileName, 10000);
		parseAndCompare(fileName, 10);
		fileName = "testFiles/SmallTest1.json";
		parseAndCompare(fileName, 10000);
		parseAndCompare(fileName, 10);
		fileName = "testFiles/SmallTest2.json";
		parseAndCompare(fileName, 10000);
		parseAndCompare(fileName, 10);
		fileName = "testFiles/SmallTest3.json";
		parseAndCompare(fileName, 10000);
		parseAndCompare(fileName, 10);
	}
	
	private void parseAndCompare(String fileName, int stringLenngth) throws IOException, IllegalFormatException{
		try(LazyByteStreamParser parser = new LazyByteStreamParser(fileName, stringLenngth)){
			// this parser vs...
			JSONNode top = parser.parseTopLevel(null);
			// org.json parser
			byte[] bytes = Files.readAllBytes(Paths.get(fileName));
			JSONObject topExp = new JSONObject(new String(bytes));
			
			verifyNode(topExp, parser, top, stringLenngth);
		}
	}
	
	/**
	 * Verifies the type and value of the node. It is assumed that the name has been already verified.
	 * @param nodeExp
	 * @param parser
	 * @param node
	 */
	private static void verifyNode(Object nodeExp, LazyByteStreamParser parser, JSONNode node, int stringLength)
			throws IOException, IllegalFormatException {
		debug("Verify "+ node.getName()+" : "+node.getValue());
		switch(node.getType()){
			case JSONNode.TYPE_ARRAY:
				assertTrue(nodeExp instanceof JSONArray);
				verifyArrayChildren((JSONArray)nodeExp, parser, node, stringLength);
				break;
			case JSONNode.TYPE_OBJECT:
				assertTrue(nodeExp instanceof JSONObject); 
				verifyObjectChildren((JSONObject)nodeExp, parser, node, stringLength);
				break;
			case JSONNode.TYPE_KEYWORD:
				assertEquals(nodeExp.toString(), node.getValue());
				break;
			case JSONNode.TYPE_NUMBER:
				try{
					NumberFormat f = NumberFormat.getInstance(); 
					assertEquals(f.parse(nodeExp.toString().toUpperCase()), 
							f.parse(node.getValue().toUpperCase()));
				} catch(ParseException e){
					throw new RuntimeException(e);
				}
				break;
			case JSONNode.TYPE_STRING:
				String strExp = nodeExp.toString();
				assertEquals(strExp.substring(0, Math.min(strExp.length(), stringLength)), node.getValue());
				break;
		}
	}

	/**
	 * Verifies children of an object node: their number and names.
	 * @param nodeExp
	 * @param parser
	 * @param node
	 * @throws IOException
	 */
	private static void verifyObjectChildren(JSONObject nodeExp, LazyByteStreamParser parser, JSONNode node,
			int stringLength) throws IOException, IllegalFormatException {
		// load children dynamically
		List<JSONNode> children = parser.loadChildrenAtPosition(node.getStartFilePosition());
		// compare number of children
		assertEquals(nodeExp.length(), children.size());
		// compare each child
		debug("Children of an Object:");
		for(JSONNode child: children){
			Object childExp = null;
			try{
				childExp = nodeExp.get(child.getName());
			}catch(JSONException e){
				e.printStackTrace();
			}
			// verify name
			assertNotNull(childExp);
			verifyNode(childExp, parser, child, stringLength);
		}
	}
	
	/**
	 * Verifies children of an object node: their number and names.
	 * @param nodeExp
	 * @param parser
	 * @param node
	 * @throws IOException
	 */
	private static void verifyArrayChildren(JSONArray nodeExp, LazyByteStreamParser parser, JSONNode node,
			int stringLength) throws IOException, IllegalFormatException {
		// load children dynamically
		List<JSONNode> children = parser.loadChildrenAtPosition(node.getStartFilePosition());
		// compare number of children
		assertEquals(nodeExp.length(), children.size());
		// compare each child
		debug("Children of an Array:");
		for(int i = 0; i < children.size(); i++){
			Object childExp = null;
			try{
				childExp = nodeExp.get(i);
			}catch(JSONException e){
				e.printStackTrace();
			}
			assertNotNull(childExp);
			JSONNode child = children.get(i);
			verifyNode(childExp, parser, child, stringLength);
		}
	}

	
	@Test
	public void shouldReadStringWithEscapedQuoteAndSlash()throws IOException, IllegalFormatException {
		// prepare file
		String fileName = UTF8FileReaderTest.getGeneratedFileName();
		FileWriter fw = new FileWriter(fileName);
		String str1 = "aa";
		String str2 = "\"bbb";
		// in file: aa\\\"bbb
		fw.write("\""+str1+ "\\\\\\"+str2+"\"");
		fw.close();
		List<String> strings = new ArrayList<String>();
		// should parse into 'aa\"bbb'
		strings.add(str1+"\\"+str2);
		checkAllStringsFromFile(fileName, -1, strings);
		checkAllStringsFromFile(fileName, 3, strings);

	}
	
	@Test
	public void shouldReadNonAsciStrings()throws IOException, IllegalFormatException{
		String fileName = UTF8FileReaderTest.getGeneratedFileName();
		OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(fileName), 
				Charset.forName("UTF-8").newEncoder());
		List<String> strings = new ArrayList<String>();
		// string in Russian
		strings.add("Строчка\"\nЕще одна cтрочка");
		writer.write("String in Russian: \"Строчка\\\"\\nЕще одна cтрочка\"; ");		
		// string in German
		strings.add("Äpfel, \"Männer\", Bänke, Hände");
		writer.write("String in German: \"Äpfel, \\\"Männer\\\", Bänke, Hände\"; ");
		// string in Japan	
		strings.add("投ネノ対仲講ーしさ  た米掲ルヌセ解更みフ まけ疎断 でさぽざ  \"宗度年テ参授ラ設50多海こざび語記");
		writer.write("String in Japan: \"投ネノ対仲講ーしさ  た米掲ルヌセ解更みフ まけ疎断 でさぽざ  \\\"宗度年テ参授ラ設50多海こざび語記\".");
		writer.close();
		checkAllStringsFromFile(fileName, -1, strings);
		checkAllStringsFromFile(fileName, 10, strings);		
	}
	@Test
	public void shouldReadEmplyAndLongStrings()throws IOException, IllegalFormatException{
		String fileName = UTF8FileReaderTest.getGeneratedFileName();
		OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(fileName), 
				Charset.forName("UTF-8").newEncoder());
		List<String> strings = new ArrayList<String>();
		// Empty string
		strings.add("");
		// long string
		char[] arr = new char[UTF8FileReader.bufferSize + 100];
		Arrays.fill(arr, 's');
		strings.add(new String(arr));
		for(int i = 0; i < strings.size(); i++){
			writer.write("String #"+i+" = \""+strings.get(i)+"\"; ");		
		}
		writer.close();
		checkAllStringsFromFile(fileName, -1, strings);		
		checkAllStringsFromFile(fileName, 10, strings);		
	}

	
	@Test
	public void shouldReadStringWithQuoteAtByteBufferEdge()throws IOException, IllegalFormatException {
		// prepare file
		String fileName = UTF8FileReaderTest.getGeneratedFileName();
		FileWriter fw = new FileWriter(fileName);
		char[] prefix = new char[UTF8FileReader.bufferSize-1];
		Arrays.fill(prefix, 's');
		String str = new String(prefix);
		fw.write("\"");
		fw.write(str);
		fw.write("\"aaaaaaaaaa");// 10 'a'
		fw.close();
		List<String> strings = new ArrayList<String>();
		strings.add(str);
		checkAllStringsFromFile(fileName, 5, strings);
	}
	@Test
	public void shouldReadStringWithQuoteAtCharBufferEdge()throws IOException, IllegalFormatException {
		// prepare file
		String fileName = UTF8FileReaderTest.getGeneratedFileName();
		FileWriter fw = new FileWriter(fileName);
		char[] prefix = new char[UTF8FileReader.bufferSize];
		Arrays.fill(prefix, 's');
		String str = new String(prefix);
		fw.write("prefix\"");
		fw.write(str);
		fw.write("\"aaaaaaaaaa");// 10 'a'
		fw.close();
		List<String> strings = new ArrayList<String>();
		strings.add(str);
		checkAllStringsFromFile(fileName, 5, strings);
	}
	@Test
	public void shouldReadStringWithEscapedQuoteAtByteBufferEdge()throws IOException, IllegalFormatException {
		// prepare file
		String fileName = UTF8FileReaderTest.getGeneratedFileName();
		FileWriter fw = new FileWriter(fileName);
		char[] prefix = new char[UTF8FileReader.bufferSize-2];
		
		Arrays.fill(prefix, 's');
		String str = new String(prefix);
		// in file: "ss...ss\"StringEnd"aaaaaaaaaa
		fw.write("\"" + str + "\\\"StringEnd" + "\"aaaaaaaaaa");
		fw.close();
		// should read as 'ss...ss"StringEnd'
		str = str + "\"StringEnd";
		List<String> strings = new ArrayList<String>();
		strings.add(str);
		checkAllStringsFromFile(fileName, -1, strings);
		checkAllStringsFromFile(fileName, 10, strings);
	}
	@Test
	public void shouldReadStringWithEscapedQuoteAtCharBufferEdge()throws IOException, IllegalFormatException {
		// prepare file
		String fileName = UTF8FileReaderTest.getGeneratedFileName();
		FileWriter fw = new FileWriter(fileName);
		char[] prefix = new char[UTF8FileReader.bufferSize-1];
		Arrays.fill(prefix, 's');
		String str = new String(prefix);
		// in file: "ss...ss\"StringEnd"aaaaaaaaaa
		fw.write("\"" + str + "\\\"StringEnd" + "\"aaaaaaaaaa");
		fw.close();
		// should read as 'ss...ss"StringEnd'
		str = str + "\"StringEnd";
		List<String> strings = new ArrayList<String>();
		strings.add(str);
		checkAllStringsFromFile(fileName, -1, strings);
		checkAllStringsFromFile(fileName, 5, strings);
	}
	@Test
	public void shouldReadStringWithEscapedBackslashAndQuotes()throws IOException, IllegalFormatException{
		String fileName = UTF8FileReaderTest.getGeneratedFileName();
		FileWriter fw = new FileWriter(fileName);
		List<String> strings = new ArrayList<String>();
		// will be ..."aaa\\aaa"... in file -> should read as 'aaa\aaa'
		fw.write("string with one backslash: \"aaa\\\\aaa\"; ");
		strings.add("aaa\\aaa");
		// will be ..."bbb\\"... in file -> should read as 'bbb\'
		fw.write("string with two backslashes: \"bbb\\\\\"; "); 
		strings.add("bbb\\");
		// will be ..."bbb\\\"string continues"... in file -> should read as 'bbb\"string continues'
		fw.write("string with backslash and quote: \"bbb\\\\\\\"string continues\""); 
		strings.add("bbb\\\"string continues");
		fw.close();
		checkAllStringsFromFile(fileName, -1, strings);
	}
	
	@Test
	public void shouldReadSingleString() throws IOException, IllegalFormatException{
		try(LazyByteStreamParser parser = new LazyByteStreamParser("testFiles/SmallTest4.json", -1)){
			JSONNode root = parser.parseTopLevel(null);
			debug(root.getValue());
			assertEquals(JSONNode.TYPE_STRING, root.getType());
		}
	}
	@Test
	public void shouldReadSingleKeyword() throws IOException, IllegalFormatException{
		try(LazyByteStreamParser parser = new LazyByteStreamParser("testFiles/SmallTest5.json", -1)){
			JSONNode root = parser.parseTopLevel(null);
			debug(root.getValue());
			assertEquals(JSONNode.TYPE_KEYWORD, root.getType());
		}
	}
	@Test
	public void shouldReadSingleNumber() throws IOException, IllegalFormatException{
		try(LazyByteStreamParser parser = new LazyByteStreamParser("testFiles/SmallTest6.json", -1)){
			JSONNode root = parser.parseTopLevel(null);
			debug(root.getValue());
			assertEquals(JSONNode.TYPE_NUMBER, root.getType());
		}
	}
	
	@Test
	public void shouldThrowAnExceptionIfSeveralRoots() throws IOException, IllegalFormatException{
		try(LazyByteStreamParser parser = new LazyByteStreamParser("testFiles/SmallTest7.json", -1)){
			thrown.expect(IllegalFormatException.class);
			parser.parseTopLevel(null);
		}
	}

	@Test
	public void shouldNotReadOutsideOfFilePosRange() throws IOException, IllegalFormatException{
		String fileName = UTF8FileReaderTest.getGeneratedFileName();
		FileWriter fw = new FileWriter(fileName);
		fw.write("a");
		fw.close();
		try(LazyByteStreamParser parser = new LazyByteStreamParser(fileName, -1)){
			thrown.expect(IllegalArgumentException.class);
			parser.loadStringAtPosition(2, 3);
		}
	}

	@Test
	public void shouldNotReadAtNegativePos() throws IOException, IllegalFormatException{
		String fileName = UTF8FileReaderTest.getGeneratedFileName();
		FileWriter fw = new FileWriter(fileName);
		fw.write("a");
		fw.close();
		try(LazyByteStreamParser parser = new LazyByteStreamParser(fileName, -1)){
			thrown.expect(IllegalArgumentException.class);
			parser.loadStringAtPosition(-1, 1);
		}
	}

	private void checkAllStringsFromFile(String fileName, int strMaxLen, List<String> expected)
			throws IOException, IllegalFormatException {
		List<StringWithCoords> resultWithCoords = new ArrayList<StringWithCoords>();
		// find all strings
		debug("find all strings with in the file");
		try(LazyByteStreamParser parser = new LazyByteStreamParser(fileName, strMaxLen)){
			while(parser.hasNext()){
				parser.moveToNextByte();
				// we suppose that backslash means nothing outside of a string 
				// and that it cannot be seen outside of a  string in a real correct JSON file
				if(parser.getCurByte() == '"'){
					StringWithCoords str = parser.parseString(strMaxLen >= 0);
					resultWithCoords.add(str);
					debug("Parsed String: '"+str.getString()+"'");
				}
			}
			assertEquals(expected.size(), resultWithCoords.size());
			// load Strings by coords
			debug("load all strings by coodrs");
			for(int i = 0; i < expected.size(); i++){
				StringWithCoords str = resultWithCoords.get(i);
				String exp = expected.get(i);
				if(strMaxLen >= 0){
					assertEquals(exp.substring(0, Math.min(exp.length(), strMaxLen)), str.getString());
				}
				String s = parser.loadStringAtPosition(str.getOpeningQuotePos(), str.getClosingQuotePos());
				debug("Parsed String: '"+s+"'");
				assertEquals(exp, s);
			}
		}
	}
}
