package test.java.parser;

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

import org.junit.Test;

import main.java.parser.UTF8FileReader;

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
		String str = "Строчка\\\"\nЕще одна строчка";
		System.out.println(str);
		writer.write("\"");
		writer.write(str);
		writer.write("\" aaaaaaaaaa");// 10 'a'
		writer.close();
		List<String> result = readAllStringsFromFile(fileName);
		assertEquals(1, result.size());
		assertEquals(str, result.get(0));
	}

	public List<String> readAllStringsFromFile(String fileName)throws IOException{
		UTF8FileReader reader = new UTF8FileReader(fileName);
		List<String> result = new ArrayList<String>();
		while(reader.hasNext()){
			byte b = reader.getNextByte();
			if(b == '"' ){
				String str = reader.readString();
				result.add(str);
				System.out.println(str);
				b = reader.getNextByte();
				assert b=='"': (char)b;
			}
		}
		return result;
	}
}
