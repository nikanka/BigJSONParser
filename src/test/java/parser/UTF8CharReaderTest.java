package test.java.parser;

import static org.junit.Assert.*;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import main.java.parser.UTF8CharFileReader;

public class UTF8CharReaderTest {
	
	@Test
	public void shouldReadAllStrings()throws IOException{
		String fileName = "UTF8CharFileReaderTest1.txt";
		OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(fileName), 
				Charset.forName("UTF-8").newEncoder());
		// Empty string
		String str1 = "";
		// long string
		char[] arr = new char[UTF8CharFileReader.bufferSize + 100];
		Arrays.fill(arr, 's');
		String str2 = new String(arr);
		// string in Russian
		String str3 = "Строчка\\\"\nЕще одна строчка";
				
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


	
	private List<String> readAllStringsFromFile(String fileName)throws IOException{
		UTF8CharFileReader reader = new UTF8CharFileReader(fileName);
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
}

