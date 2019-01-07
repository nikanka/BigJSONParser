package main.java.parser;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.Arrays;

public class UTF8CharFileReader extends UTF8FileReader{

	public static final int MODE_READING_ASCII_CHARS = 0;
	public static final int MODE_READING_UTF8_CHARS = 1;
	public static final int MODE_READING_CLOSING_QUOTE = 2;
	
	private int currentMode = MODE_READING_ASCII_CHARS;

	public static void main(String[] args) throws IOException {
		String testFile = "UTF8FileReaderTest10.txt";//"SmallTest2.json";
		OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(testFile), 
				Charset.forName("UTF-8").newEncoder());
		String str = "start = \"Строчка\\\"\nЕще одна строчка\"; The end";
//		String str = "start = \"Test string\"; The end";
		System.out.println(str);
		writer.write(str);
		writer.close();
		UTF8CharFileReader reader = new UTF8CharFileReader(testFile);
		testRead(reader);
	}
	
	public UTF8CharFileReader(String fileName) throws IOException {
		super(fileName);
	}
	public static void testRead(UTF8CharFileReader reader)throws IOException{
		StringBuilder sb = new StringBuilder();
		
		while(reader.hasNext()){
			char ch = reader.getNextChar();
			char[] arr = new char[1];
			arr[0] = ch;
			sb.append(arr);
			System.out.println(new String(arr) + " " + reader.getFilePosition()+" "+
			reader.byteBuffer.position()+" "+reader.charBuffer.position());
		}
		System.out.println(sb.toString());
	}

	public int getReadingMode(){
		return currentMode;
	}
	/**
	 * Move the cursor to the end of a String, i.e. to the closing quote. 
	 * It is assumed that before entering the method the cursor is within 
	 * a String. 
	 * @throws IOException
	 */
	public void skipTheString() throws IOException{
		currentMode = MODE_READING_ASCII_CHARS;
		byte prevByte = ' ';
		while(true){
			byte b = getNextByte();
			if(b == '\"' && prevByte != '\\'){
				break;
			}
			prevByte = b;
		}
	}
	public char getNextChar() throws IOException{
//		if(!hasNext){
//			throw new RuntimeException("Unexpected end of stream at pos " + filePos);
//		}
		char ret = 1;	
		if(currentMode == MODE_READING_CLOSING_QUOTE){
			ret = (char)getNextByte();
			assert ret == '\"': "'"+ret+"'";
			currentMode = MODE_READING_ASCII_CHARS;
			if (DEBUG) System.out.println("CHANGE MODE TO READING ASCII");
		} else if(currentMode == MODE_READING_ASCII_CHARS){
			ret = (char)getNextByte();
			if(ret == '\"'){
				// checking for the backslash at the prev pos is not necessary as 
				// it has an escape meaning only within Strings
				if (DEBUG) System.out.println("GetNextChar  char buffer initial load");
				int n = fillCharBufferUpToQuote(false);
				if(n==0){
					currentMode = MODE_READING_CLOSING_QUOTE;
					if (DEBUG) System.out.println("CHANGE MODE TO READING CLOSING QUOTE");
				}
				if(n > 0){
					currentMode = MODE_READING_UTF8_CHARS;
					if (DEBUG) System.out.println("CHANGE MODE TO READING UTF8: "+
							new String(Arrays.copyOfRange(charBuffer.array(), charBuffer.position(), charBuffer.limit())));
				}
			}
		} else {
			ret = charBuffer.get();
			if(charBuffer.position() == charBuffer.limit()){
				if (DEBUG) System.out.println("GetNextChar  char buffer reload");
				int n = fillCharBufferUpToQuote(ret == '\\');
				if(n == 0){// end of string
					currentMode = MODE_READING_CLOSING_QUOTE;
					System.out.println("CHANGE MODE TO READING CLOSING QUOTE");
				}
			}
		
		}
		return ret;
	}

}
