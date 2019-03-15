package com.bigjson.parser;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.bigjson.parser.LazyJSONParser.StringWithCoords;


public class LazyJSONParserTest {
	
	private static boolean DEBUG = false;
	
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
		File file = TestUtils.getGeneratedFileName();
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
		File file = TestUtils.getGeneratedFileName();
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
		File file = TestUtils.getGeneratedFileName();
		OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), 
				Charset.forName("UTF-8").newEncoder());
		List<String> strings = new ArrayList<String>();
		// Empty string
		strings.add("");
		// long string
		char[] arr = new char[UTF8FileReader.BUFFER_SIZE + 100];
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
		File file = TestUtils.getGeneratedFileName();
		FileWriter fw = new FileWriter(file);
		char[] prefix = new char[UTF8FileReader.BUFFER_SIZE-1];
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
		File file = TestUtils.getGeneratedFileName();
		FileWriter fw = new FileWriter(file);
		char[] prefix = new char[UTF8FileReader.BUFFER_SIZE];
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
		File file = TestUtils.getGeneratedFileName();
		FileWriter fw = new FileWriter(file);
		char[] prefix = new char[UTF8FileReader.BUFFER_SIZE-2];
		
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
		File file = TestUtils.getGeneratedFileName();
		FileWriter fw = new FileWriter(file);
		char[] prefix = new char[UTF8FileReader.BUFFER_SIZE-1];
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
		File file = TestUtils.getGeneratedFileName();
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
	public void shouldThrowExceptionForInvalidJSONFormat() throws IOException{
		expectIllegalFormatExceptionWhileRootValidation("tru", -1);
		expectIllegalFormatExceptionWhileRootValidation("{{},[]}", -1);
		expectIllegalFormatExceptionWhileRootValidation("[{},[],]", -1);
		expectIllegalFormatExceptionWhileRootValidation("[2 , ,[],]", -1);
		expectIllegalFormatExceptionWhileRootValidation("{, \"1\":[],}", -1);
		expectIllegalFormatExceptionWhileRootValidation("{\"1\":[]]", -1);
		expectIllegalFormatExceptionWhileRootValidation("[[], [{{}}], []]", -1);
		expectIllegalFormatExceptionWhileRootValidation("{\"1\": 1, \"2\": {2}}", -1);
		expectIllegalFormatExceptionWhileRootValidation("[\"aaaaaaa\\k\"]", 2);
		expectIllegalFormatExceptionWhileRootValidation("{\"1\": 1, \"2\": [\"aaaaaaa\\k\"]}", 2);
		expectIllegalFormatExceptionWhileRootValidation("{\"1\": 1, \"2: [\"aaaaaaa\\k\"]}", -1);
		expectIllegalFormatExceptionWhileRootValidation("{\"1\": 1, \"2\": [\"aaaaaaa]}", -1);
		// JSON with only ws
		expectIllegalFormatExceptionWhileRootValidation("  	", -1);
		// non-space symbol outside of the root
		expectIllegalFormatExceptionWhileRootValidation("1,", -1);
		expectIllegalFormatExceptionWhileRootValidation("1 ,", -1);
		expectIllegalFormatExceptionWhileRootValidation("\"aaa\",", -1);
		expectIllegalFormatExceptionWhileRootValidation("\"aaa\" ,", -1);
		expectIllegalFormatExceptionWhileRootValidation("{},", -1);
		expectIllegalFormatExceptionWhileRootValidation("{} ,", -1);
		expectIllegalFormatExceptionWhileRootValidation("true,", -1);
		expectIllegalFormatExceptionWhileRootValidation("false ,", -1);
		expectIllegalFormatExceptionWhileRootValidation(" , 1", -1);
		expectIllegalFormatExceptionWhileRootValidation(",1", -1);
		
		// backslash should be the last character in charBuffer
		char[] longStr = new char[UTF8FileReader.BUFFER_SIZE-1];
		Arrays.fill(longStr, 's'); 
		expectIllegalFormatExceptionWhileRootValidation("[\"" + new String(longStr) + "\\k\"]", 2);
		 // fourth (invalid) hex should be the third byte after char buffer is over
		longStr = new char[UTF8FileReader.BUFFER_SIZE-3];
		Arrays.fill(longStr, 's'); // backslash should be the last character in charBuffer
		expectIllegalFormatExceptionWhileRootValidation("[\"" + new String(longStr) + "\\u123J\"]", 2);

	}
	@Test
	public void shouldValidateCorrectNode() throws IllegalFormatException, IOException{
		validateImmideateRootChildren("[3, \"str\", {}, [], -0.5e+10]", -1);
		validateImmideateRootChildren("[3 , \"str\" , {} , [] , -0.5e+10] ", -1);

	}
	@Test
	public void shouldThrowExceptionForInvalidNodeFormat() throws IOException, IllegalFormatException{
		expectIllegalFormatExceptionWhileNodeValidation("[[], [{{}}], []]", -1, 5, 11);
		expectIllegalFormatExceptionWhileNodeValidation("{\"1\": 1, \"2\": {2}}", -1, 14, 16);
		expectIllegalFormatExceptionWhileNodeValidation("{\"1\": 1, \"2\": \"aaaaaaa\\k\"}", 2, 14, 24);
		expectIllegalFormatExceptionWhileNodeValidation("[\"aaaaaaa\\k\"]", 2, 1, 11);
	}
	
	private void expectIllegalFormatExceptionWhileRootValidation(String json, int strDisplayLen) throws IOException{
		File file = TestUtils.getGeneratedFileName();
		FileWriter fw = new FileWriter(file);
		fw.write(json);
		fw.close();
		try(LazyJSONParser parser = new LazyJSONParser(file, strDisplayLen)){
			try {
				parser.getRoot(true);
			} catch(IllegalFormatException e){
				System.err.println(e.getMessage());
				return;
			}
		}
		throw new RuntimeException("An IllegalFormatException was expected for string \"" + json + "\"");
	}
	
	private void expectIllegalFormatExceptionWhileNodeValidation(String json, int strDisplayLen, int nodeStart, int nodeEnd)
			throws IOException, IllegalFormatException {
		IllegalFormatException e = validateInnerNode(json, strDisplayLen, nodeStart, nodeEnd);
		assertNotNull(e);
		System.err.println(e.getMessage());

	}

	private IllegalFormatException validateInnerNode(String json, int strDisplayLen, int nodeStart, int nodeEnd) throws IOException, IllegalFormatException {
		File file = TestUtils.getGeneratedFileName();
		FileWriter fw = new FileWriter(file);
		fw.write(json);
		fw.close();
		try(LazyJSONParser parser = new LazyJSONParser(file, strDisplayLen)){
			parser.getRoot(false); // do not validate root children
			return parser.validateNodeAtPosition(nodeStart, nodeEnd, false);
		}
	}
	
	private void validateImmideateRootChildren(String json, int strDisplayLen) throws IOException, IllegalFormatException {
		File file = TestUtils.getGeneratedFileName();
		FileWriter fw = new FileWriter(file);
		fw.write(json);
		fw.close();
		try(LazyJSONParser parser = new LazyJSONParser(file, strDisplayLen)){
			parser.getRoot(false); // do not validate root children
			for(JSONNode node: parser.getRootChildren()){
				assertNull(parser.validateNodeAtPosition(node.getValueFilePosition(), node.getEndFilePosition(), false));
			} 
		}
	}
	
	@Test
	public void shouldNotReadOutsideOfFilePosRange() throws IOException, IllegalFormatException{
		readStringAtFilePos("a", 3, 4, IllegalArgumentException.class);
	}
	@Test
	public void shouldNotReadStringWithoutOpeningQuote() throws IOException, IllegalFormatException{
		readStringAtFilePos("a", 1, 2, IllegalFormatException.class);
	}
	@Test
	public void shouldNotReadStringWithWrongClosingPos() throws IOException, IllegalFormatException{
		readStringAtFilePos("a", 0, 1, RuntimeException.class);
	}
	@Test
	public void shouldNotReadAtNegativePos() throws IOException, IllegalFormatException{
		readStringAtFilePos("a", -1, 1, IllegalArgumentException.class);
	}
	
	private void readStringAtFilePos(String str, int from, int to, Class exceptionClass)  throws IOException, IllegalFormatException{
		File file = TestUtils.getGeneratedFileName();
		FileWriter fw = new FileWriter(file);
		fw.write('"');
		fw.write(str);
		fw.write('"');
		fw.close();
		try(LazyJSONParser parser = new LazyJSONParser(file, -1)){
			thrown.expect(exceptionClass);
			parser.loadStringAtPosition(from, to);
		}
	}

	private void checkAllStringsFromFile(File file, int strMaxLen, List<String> expected)
			throws IOException, IllegalFormatException {
		List<StringWithCoords> resultWithCoords = new ArrayList<StringWithCoords>();
		// find all strings
		debug("find all strings with in the file");
		try(LazyJSONParser parser = new LazyJSONParser(file, strMaxLen)){
			while(parser.hasNext()){
				parser.moveToNextByte();
				// we suppose that backslash means nothing outside of a string 
				// and that it cannot be seen outside of a  string in a real correct JSON file
				if(parser.getCurByte() == '"'){
					StringWithCoords str = parser.parseString(strMaxLen >= 0, false);
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
