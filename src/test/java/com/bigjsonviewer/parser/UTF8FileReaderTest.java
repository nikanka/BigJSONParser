package com.bigjsonviewer.parser;

import static org.junit.Assert.*;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.bigjsonviewer.parser.UTF8FileReader;

public class UTF8FileReaderTest {
	

//	public static void main(String[] args)throws IOException {
//		new UTF8FileReaderTest().shouldReadStringStartingAtBufferSize();
//	}
	
	
	
	@Test
	public  void shouldReadStringWithEscapedQuote()throws IOException {
		// prepare file
		String fileName = "UTF8FileReaderTest1.txt";
		FileWriter fw = new FileWriter(fileName);
		fw.write("\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\\\"bbbbbbbbb\"");
		fw.close();
		List<String> result = readAllStringsFromFile(fileName);
		assertEquals(1, result.size());
		assertEquals("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\\\"bbbbbbbbb", result.get(0));
	}
	
	@Test
	public void shouldReadStringWithQuoteAtByteBufferEdge()throws IOException {
		// prepare file
		String fileName = "UTF8FileReaderTest2.txt";
		FileWriter fw = new FileWriter(fileName);
		char[] prefix = new char[UTF8FileReader.bufferSize-1];
		Arrays.fill(prefix, 's');
		String str = new String(prefix);
		fw.write("\"");
		fw.write(str);
		fw.write("\"aaaaaaaaaa");// 10 'a'
		fw.close();
		List<String> result = readAllStringsFromFile(fileName);
		assertEquals(1, result.size());
		assertEquals(str, result.get(0));
	}
	@Test
	public void shouldReadStringWithQuoteAtCharBufferEdge()throws IOException {
		// prepare file
		String fileName = "UTF8FileReaderTest2.txt";
		FileWriter fw = new FileWriter(fileName);
		char[] prefix = new char[UTF8FileReader.bufferSize];
		Arrays.fill(prefix, 's');
		String str = new String(prefix);
		fw.write("prefix\"");
		fw.write(str);
		fw.write("\"aaaaaaaaaa");// 10 'a'
		fw.close();
		List<String> result = readAllStringsFromFile(fileName);
		assertEquals(1, result.size());
		assertEquals(str, result.get(0));
	}
	@Test
	public void shouldReadStringWithEscapedQuoteAtByteBufferEdge()throws IOException {
		// prepare file
		String fileName = "UTF8FileReaderTest2.txt";
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
		List<String> result = readAllStringsFromFile(fileName);
		assertEquals(1, result.size());
		assertEquals(str, result.get(0));
	}
	@Test
	public void shouldReadStringWithEscapedQuoteAtCharBufferEdge()throws IOException {
		// prepare file
		String fileName = "UTF8FileReaderTest3.txt";
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
		List<String> result = readAllStringsFromFile(fileName);
		assertEquals(1, result.size());
		assertEquals(str, result.get(0));
	}
	
	@Test
	public void shouldReadStringInRussian()throws IOException {
		// prepare file
		String fileName = "UTF8FileReaderTest4.txt";
		OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(fileName), 
				Charset.forName("UTF-8").newEncoder());
		String str = "Строчка\\\"\nЕще одна �?трочка";
		System.out.println(str);
		writer.write("\"");
		writer.write(str);
		writer.write("\" aaaaaaaaaa");// 10 'a'
		writer.close();
		List<String> result = readAllStringsFromFile(fileName);
		assertEquals(1, result.size());
		assertEquals(str, result.get(0));
	}
	
	private List<String> readAllStringsFromFile(String fileName)throws IOException{
		UTF8FileReader reader = new UTF8FileReader(fileName);
		List<String> result = new ArrayList<String>();
		boolean readingString = false;
		char prevChar = Character.MIN_VALUE;
		StringBuilder sb = new StringBuilder();
		while(reader.hasNext()){
			char ch = reader.getNextChar();
			if(ch == '"'){
				if(!readingString || readingString && prevChar != '\\'){
					readingString = !readingString;
					
					if(!readingString){
						result.add(sb.toString());
						sb.setLength(0);
					}
					continue;
				}
			}
			if(readingString){
				sb.append(ch);
			}
			prevChar = ch;
		}
		return result;
	}
	
	
	@Test
	public void shouldReadAllStrings()throws IOException{
		String fileName = "UTF8CharFileReaderTest1.txt";
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
		List<String> result = readAllStringsFromFile(fileName);
		assertEquals(3, result.size());
		assertEquals(str1, result.get(0));
		assertEquals(str2, result.get(1));
		assertEquals(str3, result.get(2));
		
	}


	

}
