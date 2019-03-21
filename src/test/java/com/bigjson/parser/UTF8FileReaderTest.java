package com.bigjson.parser;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.Random;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.bigjson.parser.UTF8FileReader;
import com.bigjson.parser.LazyJSONParser.StringWithCoords;

public class UTF8FileReaderTest {
	
	@Rule
    public ExpectedException thrown = ExpectedException.none();
	
	@Test
	public void checkFilePos() throws IOException, IllegalFormatException{
		try(UTF8FileReader reader = new UTF8FileReader(new File(TestUtils.getTestFileDir(), "UTF8FileReaderPositionTest.txt"))){
			while(reader.hasNext()){
				long pos = reader.getFilePosition();
				int ch = Integer.parseInt(""+(char)reader.getNextByte());
				assertEquals(pos, ch);
			}
		}
	}
 
	@Test 
	public void shouldFindClosingQuotes() throws IOException, IllegalFormatException{
		File file = TestUtils.getGeneratedTestFile();
		StringWithCoords[] strings = createFileWithStrings(file, 100, 1000, false);
//		System.out.println(Arrays.toString(strings));
		try(UTF8FileReader reader = new UTF8FileReader(file)){
			for(int i = 0; i < strings.length; i++){
				StringWithCoords s = strings[i];
				System.out.println("String: "+s.getOpeningQuotePos()+".."+s.getClosingQuotePos());
				reader.getToPosition(s.getOpeningQuotePos());
				assertEquals('"', (char)reader.getNextByte());
				reader.skipTheString();
				long closingQuotePos = reader.getFilePosition();
				assertEquals('"', (char)reader.getNextByte());
				assertEquals(s.getClosingQuotePos(), closingQuotePos);
				assertFalse(reader.isReadingString());
			}
		}
	}
	
	@Test 
	public void shouldThrowIllegalFormatExceptionWhenStringIsNotClosed()throws IOException, IllegalFormatException{
		File file= TestUtils.getGeneratedTestFile();
		StringWithCoords[] strings = createFileWithStrings(file, 1, 10000, true);
		try(UTF8FileReader reader = new UTF8FileReader(file)){
			StringWithCoords s = strings[1];// skip the zero-length string
			System.out.println("String: "+s.getOpeningQuotePos()+".."+s.getClosingQuotePos());
			reader.getToPosition(s.getOpeningQuotePos());
			assertEquals('"', (char)reader.getNextByte());
			thrown.expect(IllegalFormatException.class);
			reader.skipTheString();	
		}
	}

	/**
	 * Creates file with random strings in quotes separated by random ascii characters.
	 * Strings do not contain quotes and backslashes. The first string is always zero-length.
	 * @param fileName name of the created file
	 * @param num number of strings to generate in addition to the first zero-length string
	 * @param stringBlockLimit maximum length of a string and a block between strings
	 * @return
	 * @throws IOException
	 */
	private static StringWithCoords[] createFileWithStrings(File file, int num, int stringBlockLimit, boolean doNotCloseLastQuote) throws IOException{
		num++;
		StringWithCoords[] generatedStrings = new StringWithCoords[num];
		try(OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), 
				Charset.forName("UTF-8").newEncoder())){
			Random rnd = new Random(System.currentTimeMillis());
			int[] stringLenghts = new int[generatedStrings.length];
			stringLenghts[0] = 0;
			for(int i = 1; i < stringLenghts.length; i++){
				stringLenghts[i] = rnd.nextInt(stringBlockLimit);
			}
			long curPos = 0;
			for(int i = 0; i < generatedStrings.length; i++){
				// generate ASCII text between unicode strings in quotes
				int[] asciiCodePoints = new Random()
						.ints(rnd.nextInt(stringBlockLimit), 32, 128)//0, 128)
						.filter(value -> value != '"')
						.toArray();
				String betweenStrings = new String(asciiCodePoints, 0, asciiCodePoints.length) + "\"";
				writer.write(betweenStrings);
				curPos += betweenStrings.length(); // it is ASCII, so 1 symbol == 1 byte
				long openQuote = curPos - 1;
				// generate unicode string not containing quotes and backslashes
				int[] wholeRangeCodePoints = new Random()
						// if I use the whole range of code points up to 0X10FFFF (or even only 0xffff - BMP)
						// I sometimes get an exception  java.nio.charset.MalformedInputException when write
						// symbols to a file
						.ints(stringLenghts[i], 0, 0x1fff)//0XFFFF)
						.filter(value -> value != '"' && value != '\\')
						.toArray();
				String unicodeStr = new String(wholeRangeCodePoints, 0, wholeRangeCodePoints.length);
				// possibly add a masked quote in the middle of the unicode string
				if(rnd.nextBoolean()){
					// generate the end of the unicode string after masked quote
					wholeRangeCodePoints = new Random()
							.ints(rnd.nextInt(stringBlockLimit), 0, 0x1fff)//0X10FFFF)
							.filter(value -> value != '"')
							.toArray();
					unicodeStr += "\\\""+new String(wholeRangeCodePoints, 0, wholeRangeCodePoints.length);
				}
				writer.write(unicodeStr);
				curPos += unicodeStr.getBytes("UTF8").length;
				generatedStrings[i] = new StringWithCoords(unicodeStr, openQuote, curPos);
				// close the quote
				if(i == generatedStrings.length - 1 && doNotCloseLastQuote){
					break;
				}
				writer.write("\"");
				curPos++;
			}
		}
		return generatedStrings;
	}	
}
