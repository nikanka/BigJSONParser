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
import org.junit.Test;

import com.bigjson.parser.JSONNode;
import com.bigjson.parser.LazyByteStreamParser;
import com.bigjson.parser.UTF8FileReader;
import com.bigjson.parser.LazyByteStreamParser.StringWithCoords;


public class LazyByteStreamParserTest {

	@Test
	public void shouldParseSmallJSONFile() throws IOException{
		String fileName = "testFiles/SmallTest1.json";
		parseAndCompare(fileName, 10000);
		parseAndCompare(fileName, 10);
		fileName = "testFiles/SmallTest2.json";
		parseAndCompare(fileName, 10000);
		parseAndCompare(fileName, 10);
		
		
	}
	private void parseAndCompare(String fileName, int stringLenngth) throws IOException{
		// this parser vs...
		LazyByteStreamParser parser = new LazyByteStreamParser(fileName, stringLenngth);
		JSONNode top = parser.parseTopLevel(null);
		
		// org.json parser
		byte[] bytes = Files.readAllBytes(Paths.get(fileName));
		JSONObject topExp = new JSONObject(new String(bytes));
		
		verifyNode(topExp, parser, top, stringLenngth);	
	}
	
	/**
	 * Verifies the type and value of the node. It is assumed that the name has been already verified.
	 * @param nodeExp
	 * @param parser
	 * @param node
	 */
	private static void verifyNode(Object nodeExp, LazyByteStreamParser parser, JSONNode node, int stringLength)
			throws IOException {
			System.out.println("Verify "+ node.getName()+" : "+node.getValue());
		switch(node.getType()){
			case JSONNode.TYPE_ARRAY:
				assertTrue(nodeExp instanceof JSONArray);
				verifyArrayChildren((JSONArray)nodeExp, parser, node, stringLength);
				break;
			case JSONNode.TYPE_OBJECT:
				assertTrue(nodeExp instanceof JSONObject); 
				verifyObjectChildren((JSONObject)nodeExp, parser, node, stringLength);
				break;
			case JSONNode.TYPE_KEYWORD://TODO: handle null value
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
	private static void verifyObjectChildren(JSONObject nodeExp, LazyByteStreamParser parser, JSONNode node, int stringLength)throws IOException{
		// load children dynamically
		List<JSONNode> children = parser.loadChildrenAtPosition(node.getStartFilePosition());
		// compare number of children
		assertEquals(nodeExp.length(), children.size());
		// compare each child
		System.out.println("Children of an Object:");
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
	private static void verifyArrayChildren(JSONArray nodeExp, LazyByteStreamParser parser, JSONNode node, int stringLength)throws IOException{
		// load children dynamically
		List<JSONNode> children = parser.loadChildrenAtPosition(node.getStartFilePosition());
		// compare number of children
		assertEquals(nodeExp.length(), children.size());
		// compare each child
		System.out.println("Children of an Array:");
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
	public  void shouldReadStringWithEscapedQuote()throws IOException {
		// prepare file
		String fileName = UTF8FileReaderTest.getGeneratedFilePath("UTF8FileReaderTest1.txt");
		FileWriter fw = new FileWriter(fileName);
		fw.write("\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\\\"bbbbbbbbb\"");
		fw.close();
		List<String> result = readAllStringsFromFile(fileName, -1);
		assertEquals(1, result.size());
		assertEquals("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\\\"bbbbbbbbb", result.get(0));
	}
	
	@Test
	public void shouldReadStringInRussian()throws IOException {
		// prepare file
		String fileName = UTF8FileReaderTest.getGeneratedFilePath("UTF8FileReaderTest4.txt");
		OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(fileName), 
				Charset.forName("UTF-8").newEncoder());
		String str = "Строчка\\\"\nЕще одна строчка";
		System.out.println(str);
		writer.write("\"");
		writer.write(str);
		writer.write("\" aaaaaaaaaa");// 10 'a'
		writer.close();
		List<String> result = readAllStringsFromFile(fileName, -1);
		assertEquals(1, result.size());
		System.out.println(Arrays.toString(str.getBytes("UTF8")));
		System.out.println(Arrays.toString(result.get(0).getBytes("UTF8")));
		assertEquals(str, result.get(0));

	}
	
	@Test
	public void shouldReadAllStrings()throws IOException{
		String fileName = UTF8FileReaderTest.getGeneratedFilePath("UTF8FileReaderTest5.txt");
		OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(fileName), 
				Charset.forName("UTF-8").newEncoder());
		// Empty string
		String str1 = "f";
		// long string
		char[] arr = new char[UTF8FileReader.bufferSize + 100];
		Arrays.fill(arr, 's');
		String str2 = new String(arr);
		// string in Russian
		String str3 = "Строчка\\\"\nЕще одна �?трочка";
				
		writer.write("String #1 = \""+str1+"\"; ");
		writer.write("String #2 = \""+str2+"\"; ");
		writer.write("String #3 = \""+str3+"\"; ");
		writer.close();
		List<String> result = readAllStringsFromFile(fileName, -1);
		assertEquals(3, result.size());
		assertEquals(str1, result.get(0));
		assertEquals(str2, result.get(1));
		assertEquals(str3, result.get(2));
		
	}

	
	@Test
	public void shouldReadStringWithQuoteAtByteBufferEdge()throws IOException {
		// prepare file
		String fileName = UTF8FileReaderTest.getGeneratedFilePath("UTF8FileReaderTest2.txt");
		FileWriter fw = new FileWriter(fileName);
		char[] prefix = new char[UTF8FileReader.bufferSize-1];
		Arrays.fill(prefix, 's');
		String str = new String(prefix);
		fw.write("\"");
		fw.write(str);
		fw.write("\"aaaaaaaaaa");// 10 'a'
		fw.close();
		List<String> result = readAllStringsFromFile(fileName, -1);
		assertEquals(1, result.size());
		assertEquals(str, result.get(0));
	}
	@Test
	public void shouldReadStringWithQuoteAtCharBufferEdge()throws IOException {
		// prepare file
		String fileName = UTF8FileReaderTest.getGeneratedFilePath("UTF8FileReaderTest2.txt");
		FileWriter fw = new FileWriter(fileName);
		char[] prefix = new char[UTF8FileReader.bufferSize];
		Arrays.fill(prefix, 's');
		String str = new String(prefix);
		fw.write("prefix\"");
		fw.write(str);
		fw.write("\"aaaaaaaaaa");// 10 'a'
		fw.close();
		List<String> result = readAllStringsFromFile(fileName, -1);
		assertEquals(1, result.size());
		assertEquals(str, result.get(0));
	}
	@Test
	public void shouldReadStringWithEscapedQuoteAtByteBufferEdge()throws IOException {
		// prepare file
		String fileName = UTF8FileReaderTest.getGeneratedFilePath("UTF8FileReaderTest2.txt");
		FileWriter fw = new FileWriter(fileName);
		char[] prefix = new char[UTF8FileReader.bufferSize-2];
		
		Arrays.fill(prefix, 's');
		String str = new String(prefix);
		str = str + "\\\"StringEnd";
		System.out.println(str.substring(8191));
		fw.write("\"");
		fw.write(str);
		fw.write("\"aaaaaaaaaa");// 10 'a'
		fw.close();
		List<String> result = readAllStringsFromFile(fileName, -1);
		assertEquals(1, result.size());
		assertEquals(str, result.get(0));
	}
	@Test
	public void shouldReadStringWithEscapedQuoteAtCharBufferEdge()throws IOException {
		// prepare file
		String fileName = UTF8FileReaderTest.getGeneratedFilePath("UTF8FileReaderTest3.txt");
		FileWriter fw = new FileWriter(fileName);
		char[] prefix = new char[UTF8FileReader.bufferSize-1];
		
		Arrays.fill(prefix, 's');
		String str = new String(prefix);
		str = str + "\\\"StringEnd";
		System.out.println(str.substring(8191));
		fw.write("\"");
		fw.write(str);
		fw.write("\"aaaaaaaaaa");// 10 'a'
		fw.close();
		List<String> result = readAllStringsFromFile(fileName, -1);
		assertEquals(1, result.size());
		assertEquals(str, result.get(0));
	}


	private List<String> readAllStringsFromFile(String fileName, int strMaxLen)throws IOException{
		LazyByteStreamParser parser = new LazyByteStreamParser(fileName, strMaxLen);
		List<String> result = new ArrayList<String>();
		while(parser.hasNext()){
			parser.moveToNextChar();
			// we suppose that backslash means nothing outside of a string 
			// and that it cannot be seen outside of a  string in a real correct JSON file
			if(parser.getCurChar() == '"'){
				StringWithCoords str = parser.parseString(strMaxLen >= 0);
				result.add(str.getString());
			}
		}
		return result;
	}
	
}
