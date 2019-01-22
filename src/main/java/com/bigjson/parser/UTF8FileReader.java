package com.bigjson.parser;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.util.Arrays;

public class UTF8FileReader {
	public static final int bufferSize = 8192;
	public static final int MODE_READING_ASCII_CHARS = 0;
	public static final int MODE_READING_UTF8_CHARS = 1;
	public static final int MODE_READING_CLOSING_QUOTE = 2;
	
	private int currentMode = MODE_READING_ASCII_CHARS;
	boolean DEBUG = false;
	
	private long filePos = 0;
	private FileInputStream input;
	private FileChannel fileChannel;
	private ByteBuffer byteBuffer;
	private CharBuffer charBuffer;
	private boolean hasNext = true;
	private boolean prevCharIsBackslash = false;
	private CharsetDecoder decoder = Charset.forName("UTF-8").newDecoder()
			         .onMalformedInput(CodingErrorAction.REPORT)
			         .onUnmappableCharacter(CodingErrorAction.REPORT);

		
	public UTF8FileReader(String fileName) throws IOException {
		input = new FileInputStream(fileName);
		fileChannel = input.getChannel();
		charBuffer = CharBuffer.allocate(bufferSize);
		byteBuffer = ByteBuffer.allocate(bufferSize);
		byteBuffer.flip();
		charBuffer.flip();
		readBytes();
	}
	
	
	public int getReadingMode(){
		return currentMode;
	}

	/**
	 * Return file position in bytes that is going to be read next.
	 * <br><br>
	 * So, the position of curChar that has been just read is file position - 1.
	 * @return
	 */
	public long getFilePosition(){
		return filePos;
	}
	
	/**
	 * Set this file channel to a position prior to <code>pos</code>, so that the next byte that is read
	 * is at the position <code>pos</code> of the file.
	 * @param pos
	 * @return
	 * @throws IOException
	 */
	boolean getToPosition(long pos)throws IOException{
		if(DEBUG) System.out.println("get to pos "+pos);
		filePos = pos;
		fileChannel.position(pos);
//		nextByte = input.read();
//		return nextByte >= 0;
		// reset buffer
		byteBuffer.limit(byteBuffer.capacity());
		// set position  to limit, so that compact() in readBytes() set position to 0
		// without copying any bytes from the buffer
		byteBuffer.position(byteBuffer.limit());
		hasNext = readBytes()>=0;
		return hasNext;
	}

	/**
	 * Read bytes from file into byte buffer.
	 * @return
	 * @throws IOException
	 */
	private int readBytes() throws IOException{
		byteBuffer.compact();
		int pos = byteBuffer.position();
		int rem = byteBuffer.remaining();
		assert(rem >0): rem;
		
		if(DEBUG) System.out.println("Start read from file pos "+fileChannel.position()+
				" ("+filePos+")");
		int read = input.read(byteBuffer.array(), byteBuffer.arrayOffset() + pos, rem);
		if(read < 0){
			hasNext = false;
			return read;
		}
		if(read == 0) {
			System.err.println(byteBuffer.array().length);
			System.err.println(fileChannel.position());
			throw new IOException("Input stream returned 0 bytes");
		}
		byteBuffer.position(pos+read);
		byteBuffer.flip();
		if(DEBUG) {
			System.out.println("Bytes read from file: "+read+ " bytes");
			System.out.println("Byte buffer after reading: "+byteBuffer);
		}
		
		return byteBuffer.remaining();
	}
	
	/**
	 * Check if there are bytes left to read
	 * @return
	 */
	public boolean hasNext(){
//		return byteBuffer.position()<= byteBuffer;
		return hasNext; 
	}

	/**
	 * Return the next byte from the file
	 * @return
	 * @throws IOException
	 */
	private byte getNextByte() throws IOException{
		if(!hasNext()){
			throw new IOException("Unexpected end of stream at pos " + filePos);
		}
		byte ret = byteBuffer.get();
		filePos++;
		if(byteBuffer.position() == byteBuffer.limit()){
			if(DEBUG) System.out.println("GetNextByte buffer reload");
			readBytes();
		}
		return ret;
	}
	
	/**
	 * Provide the next char from a file. Keep track of the current reading mode: reading 
	 * String (may contain UTF-8 multy-byte characters), reading closing quote (end of reading
	 * String) and reading everything else that should be in ASCII, i.e. one byte per character.
	 * @return next char (either single- or multy-byte depending on the reading mode)
	 * @throws IOException
	 */
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
//				if (DEBUG) System.out.println("GetNextChar  char buffer initial load");
//				int n = fillCharBufferUpToQuote(false);
//				if(n==0){
//					currentMode = MODE_READING_CLOSING_QUOTE;
//					if (DEBUG) System.out.println("CHANGE MODE TO READING CLOSING QUOTE");
//				}
//				if(n > 0){
//					currentMode = MODE_READING_UTF8_CHARS;
//					if (DEBUG) System.out.println("CHANGE MODE TO READING UTF8: "+
//							new String(Arrays.copyOfRange(charBuffer.array(), charBuffer.position(), charBuffer.limit())));
//				}
				currentMode = MODE_READING_UTF8_CHARS;
				if (DEBUG){
					System.out.println("CHANGE MODE TO READING UTF8: "
							+ "opening quote at pos " + (getFilePosition() - 1));
				}
 			}
		} else if(currentMode == MODE_READING_UTF8_CHARS){
			// char buffer is empty
			if(charBuffer.position() == charBuffer.limit()){
				int n = fillCharBufferUpToQuote(prevCharIsBackslash);
				if (DEBUG){
					System.out.println("GetNextChar char buffer load: " + new String(
							Arrays.copyOfRange(charBuffer.array(), charBuffer.position(), charBuffer.limit())));
				}
				if(n == 0){// end of string, i.e. it's an empty string
					currentMode = MODE_READING_CLOSING_QUOTE;
					prevCharIsBackslash = false;
					if(DEBUG)System.out.println("CHANGE MODE TO READING CLOSING QUOTE");
					return getNextChar();
				}
			}
			// TODO: handle a situation when there are bytes left to read but they do not 
			// give any meaningful character (in case of multi-byte chars)
			ret = charBuffer.get();
			prevCharIsBackslash = ret == '\\';
//			if(charBuffer.position() == charBuffer.limit()){
//				if (DEBUG) System.out.println("GetNextChar  char buffer reload");
//				int n = fillCharBufferUpToQuote(ret == '\\');
//				if(n == 0){// end of string
//					currentMode = MODE_READING_CLOSING_QUOTE;
//					if(DEBUG)System.out.println("CHANGE MODE TO READING CLOSING QUOTE");
//				}
//			}
		} else {
			throw new RuntimeException("Invalid reading mode: " + currentMode);
		}
		if(DEBUG) System.out.println("Char just read: " + ret);
		return ret;
	}	


	/**
	 * Read and return a whole String starting at a given file position and up to an 
	 * unmasked quote. It is assumed the the opening quote has already been read.
	 * The file cursor is at the closing quote after this method.<br>
	 * This method should be used when the entire String is to be read.
	 * @return a String that was read (without quotes)
	 * @throws IOException
	 */
//	public String readStringAtPos(long filePos)throws IOException{
//		StringBuilder sb = new StringBuilder();
//		boolean lastCharIsBackSlash = false;
//		for(;;){
//			int n = fillCharBufferUpToQuote(lastCharIsBackSlash);
//			assert charBuffer.arrayOffset()==0: charBuffer.arrayOffset();
//			char[] charArr = charBuffer.array();
//			sb.append(charArr, 0, n);
//			if(DEBUG) System.out.println(
//					"Read " + n + " chars: CharBuffer [pos=" + charBuffer.position() + " lim=" + charBuffer.limit()
//							+ " cap=" + charBuffer.capacity() + "]\n" + byteBuffer + " (filePos = " + filePos + ")");
//			if(n < charBuffer.capacity()){
//				break;
//			}
//			lastCharIsBackSlash = charArr[n-1]=='\\';
//		}
//		currentMode = MODE_READING_CLOSING_QUOTE;
//		if(DEBUG) System.out.println("End of readStringTest: "+byteBuffer+" (filePos = "+filePos+")");
//		return sb.toString();
//	}
	
	/**
	 * Move the cursor to the end of a String, i.e. to the closing quote. 
	 * It is assumed that before entering the method the cursor is within 
	 * a String.<br> 
	 * At the end of this method the closing quote was just read and the mode is 
	 * set to reading ASCII.
	 * @return current file position (i.e. position immediate after the closing quote)
	 * @throws IOException
	 */
	public long skipTheString() throws IOException{
		// if we read all the chars from the char buffer
		// we just check the class variable to know if the last read char is a backslash
		boolean prevIsBackslash = prevCharIsBackslash;
		// if the char buffer was not read up to its limit 
		// read the last char before the limit to check if its a backslash
		if(charBuffer.position() < charBuffer.limit()){
			charBuffer.position(charBuffer.limit()-1);
			prevIsBackslash = charBuffer.get() == '\\';
			// now buffer is read up to the limit
		}
		// search for the first unmasked quote in the byte buffer 
		currentMode = MODE_READING_ASCII_CHARS;
		while(true){
			byte b = getNextByte();
			if(b == '\"' && !prevIsBackslash){
				break;
			}
			prevIsBackslash = b == '\\';
		}
		// TODO: handle unexpected end of file - before the closing quote is reached
		return filePos;
	}


	/**
	 * Fill the char buffer with chars starting from current position and up
	 * to an unmasked quote (or the end of the char buffer). The position of the 
	 * buffer is set to 0 and the limit is set to the number of characters that were
	 * read into the buffer.
	 * <br><br>
	 * The file position is set to number of bytes read so far, which means it is set
	 * to the position of the closing quote of the string that is being read or to some 
	 * position within a string if the quote has not been reached and the char buffer is full.  
	 * 
	 * @param prevCharWasBackSlash
	 * @return the number characters that were read into the buffer
	 * @throws IOException
	 */
	private int fillCharBufferUpToQuote(boolean prevCharWasBackSlash) throws IOException{
		if(DEBUG) System.out.println("Read chars up to quote from : "+ byteBuffer);
		charBuffer.clear();
		boolean eof = false;
//		int byteOldLimit = byteBuffer.limit();
//		long toDecode = filePosLimit - filePos;
//		if(toDecode <= byteBuffer.remaining()){
//			byteBuffer.limit(byteBuffer.position() + (int)toDecode);
//			eof = true;
//		}
		int posBeforeDecoding;
		boolean escaped = prevCharWasBackSlash;
		for (;;) {
			posBeforeDecoding = byteBuffer.position();
			if(DEBUG)System.out.println("Scaning for closing quote starting from pos "
					+ posBeforeDecoding + "...");
			int quotePos = scanForClosingQuote(escaped);
			if(DEBUG){
				if(quotePos >= 0){
					System.out.println(" found quote at byte buffer pos " + quotePos);
				} else {
					System.out.println(" no quote found.");
				}
			}
			int byteOldLimit = byteBuffer.limit();
			if(quotePos >= 0){
				byteBuffer.limit(quotePos);
				eof = true;
			}
			CoderResult cr = decoder.decode(byteBuffer, charBuffer, eof);
			if (DEBUG) System.out.println("\t\tDecoded "+ (byteBuffer.position() - posBeforeDecoding)+" bytes");
			// update file position based on the number of read and decoded bytes
			filePos += (byteBuffer.position() - posBeforeDecoding); 
			if (cr.isUnderflow()) {
				if (DEBUG) System.out.println("\t\tUnderflow, eof = "+eof);
				if (eof){
					 // restore old byte array limit so that I can read next bytes from it
					 // (without decoding)
					 byteBuffer.limit(byteOldLimit);
					 if (DEBUG) System.out.println("EOF: "+byteBuffer);
					 break;
				}
				assert byteOldLimit == byteBuffer.limit(): byteOldLimit - byteBuffer.limit();
					
				if (!charBuffer.hasRemaining()){
					break;
				}
				// if ((cb.position() > 0) && !inReady())
				// break; // Block at most once
				escaped = byteBuffer.get(byteBuffer.position()-1) == '\\';
				if (DEBUG) System.out.println("\t\tRead chars: byte reload (escaped = "+escaped+")");
				
				int n = readBytes();
				if (n < 0) {
					eof = true;// TODO it seems that this eof value does not affect anything
//					hasNext = false;
//					 if ((charBuffer.position() == 0) && (!byteBuffer.hasRemaining()))
//					 break;
//					decoder.reset();
				}
				 continue;
			} else if (cr.isOverflow()) {
				if (DEBUG) System.out.println("Overflow");
				assert charBuffer.position() > 0;
				// restore old byte array limit 
				// so that I can read next bytes from it when decoding chars into 
				// empty char buffer (and yes, I'll need to search for a quote again)
				// TODO: pass info about closing quote, so I do not to search for it again
				byteBuffer.limit(byteOldLimit);
				break;
			} else {
				 byteBuffer.limit(byteOldLimit);
				 if (DEBUG) System.err.println("Character coding exception at file pos ~ "+filePos);
				 cr.throwException();
			}
		}
		 if (eof) {
		 // ## Need to flush decoder
			 decoder.reset();
		 }
		if (charBuffer.position() == 0 && !eof) {
			throw new RuntimeException("No chars were decoded from byte buffer");
		}
		charBuffer.flip();
		
		return charBuffer.remaining();
		
	}
	
	private int scanForClosingQuote(boolean escaped) throws IOException {
		if(byteBuffer.remaining() == 0){
			return -1;
		}
		assert byteBuffer.arrayOffset()==0: byteBuffer.arrayOffset();
		byte[] arr = byteBuffer.array();
		int start = byteBuffer.position();
		if(!escaped && arr[start] == '"'){
			return start;
		}
		int limit = byteBuffer.limit();
		for(int i = start + 1; i < limit; i++){
			if(arr[i] == '"' && arr[i-1] != '\\'){
				return i;
			}
		}
		return -1;
	}
	

}

