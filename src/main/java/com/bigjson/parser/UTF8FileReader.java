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

import javax.management.RuntimeErrorException;

public class UTF8FileReader {
	public static final int bufferSize = 8192;
	private static final int MODE_READING_ASCII_CHARS = 0;
	private static final int MODE_READING_UTF8_CHARS = 1;
	private static final int MODE_READING_CLOSING_QUOTE = 2;
	
	private int currentMode = MODE_READING_ASCII_CHARS;
	boolean DEBUG = false;
	
	private long filePos = 0;
	private String fileName;
	private FileInputStream input;
	private FileChannel fileChannel;
	private ByteBuffer byteBuffer;
	private CharBuffer charBuffer;
	private StringReadingStateMachine stringReadingState = new StringReadingStateMachine(StringReadingStateMachine.MODE_READ);
	private boolean hasNext = true;
	private boolean reachedClosingQuote = false;
//	private boolean prevCharIsBackslash = false;
	private CharsetDecoder decoder = Charset.forName("UTF-8").newDecoder()
			         .onMalformedInput(CodingErrorAction.REPORT)
			         .onUnmappableCharacter(CodingErrorAction.REPORT);

		
	public UTF8FileReader(String fileName) throws IOException {
		this.fileName = fileName;
		input = new FileInputStream(fileName);
		fileChannel = input.getChannel();
		charBuffer = CharBuffer.allocate(bufferSize);
		byteBuffer = ByteBuffer.allocate(bufferSize);
		byteBuffer.flip();
		charBuffer.flip();
		hasNext = readBytes() >= 0;
		if(!hasNext){
			throw new IOException("It looks like file " + fileName + " is empty");
		}
	}
	
	void closeFileInputStream() throws IOException{
		input.close();
	}
	
	public int getReadingMode(){
		return currentMode;
	}

	/**
	 * Return file position in bytes that is going to be read next.
	 * So, the position of curChar that has been just read is file position - 1.
	 * <br><br>
	 * IMPORTANT: be aware that the exact position can be obtained only if the reader
	 * is in ASCII reading mode, i.e. outside strings. While reading strings filePosition is 
	 * somewhere between the start and the end of the string 
	 *  
	 * 
	 * @return
	 */
	public long getFilePosition(){
		return filePos;
	}
	
	public String getFileName(){
		return fileName;
	}
	
	/**
	 * Set this file channel to a position <code>pos</code>, so that the next byte that is read
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
	 * @throws IOException - if an I/O error occurs
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
//			hasNext = false;
			byteBuffer.limit(pos);
			//return read;
		} else {
			byteBuffer.limit(pos + read);
		}
//		if(read == 0) {
//			System.err.println(byteBuffer.array().length);
//			System.err.println(fileChannel.position());
//			throw new IOException("Input stream returned 0 bytes");
//		}
//		byteBuffer.position(pos+read);
//		byteBuffer.flip();
		
		if(DEBUG) {
			System.out.println("Bytes read from file: "+read+ " bytes");
			System.out.println("Byte buffer after reading: "+byteBuffer);
		}
		
		return read;//byteBuffer.remaining();
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
	 * Return the next byte from the file and increments current position
	 * (current position is equal to the position of byte to be read next).
	 * If the read byte was the last byte in byte buffer, the buffer is refilled.
	 * 
	 * @return
	 * @throws IOException
	 *             if an I/O error occurs
	 * @throws IllegalFormatException
	 *             if the file has no bytes to read
	 */
	protected byte getNextByte() throws IOException, IllegalFormatException{
		if(!hasNext()){
			throw new IllegalFormatException("Unexpected end of stream at pos " + filePos + " of file " + fileName);
		}
		if(currentMode != MODE_READING_ASCII_CHARS){
			throw new RuntimeException("Cannot read bytes in current mode: "+currentMode);
		}
		byte ret = byteBuffer.get();
		filePos++;
		if(!byteBuffer.hasRemaining()){
			reloadByteBuffer();
		}
		return ret;
	}
	private void reloadByteBuffer()throws IOException {
		if(DEBUG) System.out.println("GetNextByte buffer reload");
		hasNext = readBytes()>=0;
	}
	
	/**
	 * Provide the next char from a file. Keep track of the current reading mode: reading 
	 * String (may contain UTF-8 multy-byte characters), reading closing quote (end of reading
	 * String) and reading everything else that should be in ASCII, i.e. one byte per character.
	 * @return next char (either single- or multy-byte depending on the reading mode)
	 * @throws IOException
	 */
//	public char getNextChar() throws IllegalFormatException{
//		if(!hasNext){
//			throw new IllegalFormatException("Unexpected end of stream");
//		}
//		char ret = 1;	
//		if(currentMode == MODE_READING_CLOSING_QUOTE){
//			try{
//				ret = (char)(getNextByte() & 0xFF);// safe transition from singed to unsigned byte
//				if(ret != '"'){
//					throw new RuntimeException("We should not have got here if the nextByte is not a quote (pos = "
//							+ filePos + " in " + fileName + ")");
//				}
//			} catch(IOException e){
//				throw new IOException(e.getMessage() + " while reading a closing quote of a String ");
//			}
//			assert ret == '\"': "'"+ret+"'";
//			currentMode = MODE_READING_ASCII_CHARS;
//			if (DEBUG) System.out.println("CHANGE MODE TO READING ASCII");
//		} else if(currentMode == MODE_READING_ASCII_CHARS){
//			ret = (char)getNextByte();
//			if(ret == '\"'){
//				// checking for the backslash at the prev pos is not necessary as 
//				// it has an escape meaning only within Strings
////				if (DEBUG) System.out.println("GetNextChar  char buffer initial load");
////				int n = fillCharBufferUpToQuote(false);
////				if(n==0){
////					currentMode = MODE_READING_CLOSING_QUOTE;
////					if (DEBUG) System.out.println("CHANGE MODE TO READING CLOSING QUOTE");
////				}
////				if(n > 0){
////					currentMode = MODE_READING_UTF8_CHARS;
////					if (DEBUG) System.out.println("CHANGE MODE TO READING UTF8: "+
////							new String(Arrays.copyOfRange(charBuffer.array(), charBuffer.position(), charBuffer.limit())));
////				}
//				currentMode = MODE_READING_UTF8_CHARS;
//				if (DEBUG){
//					System.out.println("CHANGE MODE TO READING UTF8: "
//							+ "opening quote at pos " + (getFilePosition() - 1));
//				}
// 			}
//		} else if(currentMode == MODE_READING_UTF8_CHARS){
//			// char buffer is empty
//			if(charBuffer.position() == charBuffer.limit()){
//				int n = fillCharBufferUpToQuote();
//				if (DEBUG){
//					System.out.println("GetNextChar char buffer load: " + new String(
//							Arrays.copyOfRange(charBuffer.array(), charBuffer.position(), charBuffer.limit())));
//				}
//				if(n == 0){// end of string, i.e. it's an empty string
//					currentMode = MODE_READING_CLOSING_QUOTE;
////					prevCharIsBackslash = false;
//					if(DEBUG)System.out.println("CHANGE MODE TO READING CLOSING QUOTE");
//					return getNextChar();
//				}
//			}
//			// TODO: handle a situation when there are bytes left to read but they do not 
//			// give any meaningful character (in case of multi-byte chars)
//			ret = charBuffer.get();
////			prevCharIsBackslash = ret == '\\';
////			if(charBuffer.position() == charBuffer.limit()){
////				if (DEBUG) System.out.println("GetNextChar  char buffer reload");
////				int n = fillCharBufferUpToQuote(ret == '\\');
////				if(n == 0){// end of string
////					currentMode = MODE_READING_CLOSING_QUOTE;
////					if(DEBUG)System.out.println("CHANGE MODE TO READING CLOSING QUOTE");
////				}
////			}
//		} else {
//			throw new RuntimeException("Invalid reading mode: " + currentMode);
//		}
//		if(DEBUG) System.out.println("Char just read: " + ret);
//		return ret;
//	}	
	/**
	 * Reads the next char within a string. If the end of string is reached the
	 * mode is changed to reading ASCII (=> should use getNextByte() to get next
	 * symbol which expected to be a quote). If a string contains an escaped
	 * sequence (like \t or \" or \u0041), all chars within this sequence are
	 * read and the parsed char is returned (i.e. 't' or '"' or 'A')
	 * 
	 * @return nest char in a string
	 * @throws IOException
	 *             if I/O error occurs
	 * @throws IllegalFormatException
	 *             if the end of file is reached before the closing quote is met
	 */
	protected char getNextChar() throws IOException, IllegalFormatException{
		if(!isReadingString()){
			throw new RuntimeException("Cannot read chars outside a string (current mode = " + currentMode + ")");
		}
		char curChar = charBuffer.get();
		// if char buffer is empty - fill it
		if (!charBuffer.hasRemaining()){
			if (reachedClosingQuote) {
				// if we are here the string reading state should be final and last read char should not be a backslash
				if (!stringReadingState.isInFinalState() || curChar == '\\') {
					throw new RuntimeException(
							"We should not have got here being in the middle of reading escaped sequence");
				}
//				// and the first byte in byteBuffer should be a quote
//				byte b = getNextByte();
//				if (b != '"') {
//					throw new RuntimeException(
//							"We should not have got here if the current byte is not a quote, got " + b + " instead");
//				}
				reachedClosingQuote = false;
				currentMode = MODE_READING_ASCII_CHARS;
				return curChar;
			} else {
				reachedClosingQuote = fillCharBufferUpToQuote();
				if (DEBUG) {
					System.out.println("GetNextChar char buffer load: " + new String(
							Arrays.copyOfRange(charBuffer.array(), charBuffer.position(), charBuffer.limit())));
				}
			}
		}
		if(curChar == '\\'){
			while(!stringReadingState.pushChar(curChar)){
				curChar = getNextChar();
			}	
			return stringReadingState.getChar();
		}
		
		return curChar;
	}	

	protected void prepareForReadingAString(){
		currentMode = MODE_READING_UTF8_CHARS;
		stringReadingState.reset(StringReadingStateMachine.MODE_READ);
	}
	protected boolean isReadingString(){
		return currentMode == MODE_READING_UTF8_CHARS;
	}

//	/**
//	 * Read and return a whole String starting at a given file position and up to an 
//	 * unmasked quote. It is assumed the the opening quote has already been read.
//	 * The file cursor is at the closing quote after this method.<br>
//	 * This method should be used when the entire String is to be read.
//	 * @return a String that was read (without quotes)
//	 * @throws IOException
//	 */
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
	 * Move the cursor to the end of a String, i.e. to the closing quote. It is
	 * assumed that before entering the method the cursor is within a String.
	 * <br>
	 * At the end of this method the cursor is at the closing quote (so the next
	 * byte should be a quote) and the mode is set to reading ASCII.
	 * 
	 * @return current file position (i.e. position of the closing quote)
	 * @throws IOException
	 *             if an I/O error occur
	 * @throws IllegalFormatException
	 *             if end of file is reached before closing quote is met
	 */
	protected long skipTheString() throws IOException, IllegalFormatException{
		if(!stringReadingState.isInFinalState()){
			throw new RuntimeException(
					"We should not have got here if the stateCheck is in non-final state (i.e. in the middle of a backslash sequence)");
		}
		if(reachedClosingQuote){
			// if we reached the quote while previous scan it should be the
			// first byte in the byte buffer
			currentMode = MODE_READING_ASCII_CHARS;
			reachedClosingQuote = false;
			// imitate reading char buffer up to the end - for consistency
			charBuffer.position(charBuffer.limit());
//			byte b = getNextByte();
//			if(b != '"'){
//				throw new RuntimeException(
//						"We should not have got here if the current byte is not a quote, got " + b + " instead");
//			}
			return filePos;
		}
		// if the char buffer was not read up to its limit - read it, but keep track of reding state 
		while(charBuffer.hasRemaining()){
			char ch = charBuffer.get();
			stringReadingState.pushChar(ch);
			// we should not meet any closing quotes here
			// because we already scanned for them before filling the char buffer
		}
		// now switch to bytes:
		// search for the first unmasked quote in the byte buffer 
		currentMode = MODE_READING_ASCII_CHARS;
		int quotePos = -1;
		while(quotePos < 0){
			quotePos = scanForClosingQuote();
		}
		byteBuffer.position(quotePos);
//		// now we do not care about all string reading states - we just need to know if it is escaped or not
//		boolean escaped = stringReadingState.isInEscapedState();
//		while(true){
//			byte b = getNextByte();
//			if(b == '"' && !escaped){
//				stringReadingState.reset();
//				break;
//			} 
//			if(escaped){
//				escaped = false;
//			}else if(b == '\\'){
//				escaped = true;
//			}
//		}
		// TODO: maybe return position of closing quote?
		return filePos;
	}


	/**
	 * Fill the char buffer with chars starting from current position and up to
	 * an unmasked quote (or the end of the char buffer). The position of the
	 * buffer is set to 0 and the limit is set to the number of characters that
	 * were read into the buffer. <br>
	 * <br>
	 * The file position is set to number of bytes read so far, which means it
	 * is set to the position of the closing quote of the string that is being
	 * read or to some position within a string if the quote has not been
	 * reached and the char buffer is full.
	 * 
	 * @param escaped
	 *            true if the reading state is escaped (i.e. the last read
	 *            character is a an unmasked backslash)
	 * @return true if an unmasked quote has been meet
	 * @throws IOException
	 *             if I/O error occurs
	 * @throws IllegalFormatException
	 *             if the end of file is reached while scanning for closing
	 *             quote
	 */
	private boolean fillCharBufferUpToQuote() throws IOException, IllegalFormatException{
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
		//boolean escaped = prevCharWasBackSlash;
		for (;;) {
			posBeforeDecoding = byteBuffer.position();
			if(DEBUG)System.out.println("Scaning for closing quote starting from pos "
					+ posBeforeDecoding + "...");
			int quotePos = scanForClosingQuote();
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
//				readingState.reset();
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
//				if(byteBuffer.position() > 0){
//					escaped = byteBuffer.get(byteBuffer.position()-1) == '\\';
//				}
//				if (DEBUG) System.out.println("\t\tRead chars: byte reload (escaped = "+escaped+")");
				int n = readBytes();
				if (n < 0) {
					throw new IllegalFormatException("Unexpected end of file while reading String");
					// eof = true;// TODO it seems that this eof value does not affect anything
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
			 // flush decoder
			 decoder.reset();
		 }
		if (charBuffer.position() == 0 && !eof) {
			throw new RuntimeException("No chars were decoded from byte buffer");
		}
		charBuffer.flip();
		
		return eof;//charBuffer.remaining();
		
	}
	
	/** 
	 * Search for an unmasked quote in the current byteBuffer without refilling it.
	 * The buffer position is not changed after this method.
	 * @return
	 */
	private int scanForClosingQuote(){// throws IOException {
//		if(byteBuffer.remaining() == 0){
//			return -1;
//		}
//		assert byteBuffer.arrayOffset()==0: byteBuffer.arrayOffset();
//		byte[] arr = byteBuffer.array();
//		int start = byteBuffer.position();
//		if(!escaped && arr[start] == '"'){
//			return start;
//		}
//		int limit = byteBuffer.limit();
//		for(int i = start + 1; i < limit; i++){
//			if(arr[i] == '"' && arr[i-1] != '\\'){
//				return i;
//			}
//		}
		int quotePos = -1;
		boolean escaped = stringReadingState.isInEscapedState();
		byteBuffer.mark();// remember the start position
		// scan for an unmasked quote
		while(byteBuffer.hasRemaining()){
			byte b = byteBuffer.get();
			if(b == '"' && !escaped){
				quotePos = byteBuffer.position();
				break;
			} 
			if(escaped){
				escaped = false;
			}else if(b == '\\'){
				escaped = true;
			}
		}
		// reset the original position of the buffer
		byteBuffer.reset();
		
		return quotePos;
	}
	
	

}

