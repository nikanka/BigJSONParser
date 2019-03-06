package com.bigjson.parser;

import static org.junit.Assert.*;

import java.io.File;
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
	public void shouldReadStringWithEscapedQuoteAndSlash()throws IOException, IllegalFormatException {
		// prepare file
		File file = UTF8FileReaderTest.getGeneratedFileName();
		FileWriter fw = new FileWriter(file);
		String str1 = "aa";
		String str2 = "\"bbb";
		// in file: aa\\\"bbb
		fw.write("\""+str1+ "\\\\\\"+str2+"\"");
		fw.close();
		List<String> strings = new ArrayList<String>();
		// should parse into 'aa\"bbb'
		strings.add(str1+"\\"+str2);
		checkAllStringsFromFile(file, -1, strings);
		checkAllStringsFromFile(file, 3, strings);

	}
	
	@Test
	public void shouldReadNonAsciStrings()throws IOException, IllegalFormatException{
		File file = UTF8FileReaderTest.getGeneratedFileName();
		OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), 
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
		checkAllStringsFromFile(file, -1, strings);
		checkAllStringsFromFile(file, 10, strings);		
	}
	@Test
	public void shouldReadEmplyAndLongStrings()throws IOException, IllegalFormatException{
		File file = UTF8FileReaderTest.getGeneratedFileName();
		OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), 
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
		checkAllStringsFromFile(file, -1, strings);		
		checkAllStringsFromFile(file, 10, strings);		
	}

	
	@Test
	public void shouldReadStringWithQuoteAtByteBufferEdge()throws IOException, IllegalFormatException {
		// prepare file
		File file = UTF8FileReaderTest.getGeneratedFileName();
		FileWriter fw = new FileWriter(file);
		char[] prefix = new char[UTF8FileReader.bufferSize-1];
		Arrays.fill(prefix, 's');
		String str = new String(prefix);
		fw.write("\"");
		fw.write(str);
		fw.write("\"aaaaaaaaaa");// 10 'a'
		fw.close();
		List<String> strings = new ArrayList<String>();
		strings.add(str);
		checkAllStringsFromFile(file, 5, strings);
	}
	@Test
	public void shouldReadStringWithQuoteAtCharBufferEdge()throws IOException, IllegalFormatException {
		// prepare file
		File file = UTF8FileReaderTest.getGeneratedFileName();
		FileWriter fw = new FileWriter(file);
		char[] prefix = new char[UTF8FileReader.bufferSize];
		Arrays.fill(prefix, 's');
		String str = new String(prefix);
		fw.write("prefix\"");
		fw.write(str);
		fw.write("\"aaaaaaaaaa");// 10 'a'
		fw.close();
		List<String> strings = new ArrayList<String>();
		strings.add(str);
		checkAllStringsFromFile(file, 5, strings);
	}
	@Test
	public void shouldReadStringWithEscapedQuoteAtByteBufferEdge()throws IOException, IllegalFormatException {
		// prepare file
		File file = UTF8FileReaderTest.getGeneratedFileName();
		FileWriter fw = new FileWriter(file);
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
		checkAllStringsFromFile(file, -1, strings);
		checkAllStringsFromFile(file, 10, strings);
	}
	@Test
	public void shouldReadStringWithEscapedQuoteAtCharBufferEdge()throws IOException, IllegalFormatException {
		// prepare file
		File file = UTF8FileReaderTest.getGeneratedFileName();
		FileWriter fw = new FileWriter(file);
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
		checkAllStringsFromFile(file, -1, strings);
		checkAllStringsFromFile(file, 5, strings);
	}
	@Test
	public void shouldReadStringWithEscapedBackslashAndQuotes()throws IOException, IllegalFormatException{
		File file = UTF8FileReaderTest.getGeneratedFileName();
		FileWriter fw = new FileWriter(file);
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
		checkAllStringsFromFile(file, -1, strings);
	}
	
	@Test
	public void shouldReadSingleString() throws IOException, IllegalFormatException{
		try(LazyByteStreamParser parser = new LazyByteStreamParser(new File("testFiles/SmallTest4.json"), -1)){
			JSONNode root = parser.getRoot();
			debug(root.getValue());
			assertEquals(JSONNode.TYPE_STRING, root.getType());
		}
	}
	@Test
	public void shouldReadSingleKeyword() throws IOException, IllegalFormatException{
		try(LazyByteStreamParser parser = new LazyByteStreamParser(new File("testFiles/SmallTest5.json"), -1)){
			JSONNode root = parser.getRoot();
			debug(root.getValue());
			assertEquals(JSONNode.TYPE_KEYWORD, root.getType());
		}
	}
	@Test
	public void shouldReadSingleNumber() throws IOException, IllegalFormatException{
		try(LazyByteStreamParser parser = new LazyByteStreamParser(new File("testFiles/SmallTest6.json"), -1)){
			JSONNode root = parser.getRoot();
			debug(root.getValue());
			assertEquals(JSONNode.TYPE_NUMBER, root.getType());
		}
	}
	
	@Test
	public void shouldThrowAnExceptionIfSeveralRoots() throws IOException, IllegalFormatException{
		try(LazyByteStreamParser parser = new LazyByteStreamParser(new File("testFiles/SmallTest7.json"), -1)){
			thrown.expect(IllegalFormatException.class);
			parser.getRoot();
		}
	}

	@Test
	public void shouldNotReadOutsideOfFilePosRange() throws IOException, IllegalFormatException{
		File file = UTF8FileReaderTest.getGeneratedFileName();
		FileWriter fw = new FileWriter(file);
		fw.write("a");
		fw.close();
		try(LazyByteStreamParser parser = new LazyByteStreamParser(file, -1)){
			thrown.expect(IllegalArgumentException.class);
			parser.loadStringAtPosition(2, 3);
		}
	}

	@Test
	public void shouldNotReadAtNegativePos() throws IOException, IllegalFormatException{
		File file = UTF8FileReaderTest.getGeneratedFileName();
		FileWriter fw = new FileWriter(file);
		fw.write("a");
		fw.close();
		try(LazyByteStreamParser parser = new LazyByteStreamParser(file, -1)){
			thrown.expect(IllegalArgumentException.class);
			parser.loadStringAtPosition(-1, 1);
		}
	}

	private void checkAllStringsFromFile(File file, int strMaxLen, List<String> expected)
			throws IOException, IllegalFormatException {
		List<StringWithCoords> resultWithCoords = new ArrayList<StringWithCoords>();
		// find all strings
		debug("find all strings with in the file");
		try(LazyByteStreamParser parser = new LazyByteStreamParser(file, strMaxLen)){
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
