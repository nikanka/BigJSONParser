package com.bigjson.parser;

import java.io.Closeable;
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

public class UTF8FileReader implements Closeable{
	public static final int bufferSize = 8192;
	private static final int MODE_READING_ASCII_CHARS = 0;
	private static final int MODE_READING_UTF8_CHARS = 1;
	
	private int currentMode = MODE_READING_ASCII_CHARS;
	private boolean DEBUG = false;
	
	private long filePos = 0;
	private String fileName;
	private FileInputStream input;
	private FileChannel fileChannel;
	private ByteBuffer byteBuffer;
	private CharBuffer charBuffer;
	private StringReadingStateMachine stringReadingState = new StringReadingStateMachine(StringReadingStateMachine.MODE_READ);
	private boolean hasNext = true;
	private ClosingQuoteScanResult quoteScanResult = new ClosingQuoteScanResult();
	private CharsetDecoder decoder = Charset.forName("UTF-8").newDecoder()
			         .onMalformedInput(CodingErrorAction.REPORT)
			         .onUnmappableCharacter(CodingErrorAction.REPORT);

	/**
	 * Creates a reader for a given file
	 * 
	 * @param fileName
	 * @throws IOException
	 *             if file is not found or empty or if an I/O error occurs while
	 *             reading bytes from the file
	 */
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
	
//	void closeFileInputStream() throws IOException{
//		input.close();
//	}
	
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
	
	@Override
	public void close() throws IOException {
		fileChannel.close();
		input.close();
	}
	
	private void debug(String msg){
		if(DEBUG)System.out.println("DEBUG: " + msg);
	}
	
	/**
	 * Set this file channel to a position <code>pos</code>, so that the next byte that is read
	 * is at the position <code>pos</code> of the file.
	 * @param pos
	 * @return
	 * @throws IOException
	 */
	boolean getToPosition(long pos)throws IOException{
		debug("get to pos "+pos);
		if(pos < 0){
			throw new IllegalArgumentException("File position should not be negative (got " + pos + ")");
		}
		if(filePos == pos){
			return hasNext;
		}
		//check if this position is within the loaded byte buffer
		if((pos > filePos && pos-filePos < byteBuffer.remaining()) || 
			(pos < filePos && filePos - pos <= byteBuffer.position())){
			byteBuffer.position((int)(byteBuffer.position() + pos - filePos));
			debug("Jumped to byte buffer pos " + byteBuffer.position());
		} else {
			// reset buffer
			byteBuffer.limit(byteBuffer.capacity());
			// set position  to limit, so that compact() in readBytes() set position to 0
			// without copying any bytes from the buffer
			byteBuffer.position(byteBuffer.limit());
			// read bytes from new pos
			fileChannel.position(pos);
			hasNext = readBytes()>=0;
		}
		filePos = pos;
//		nextByte = input.read();
//		return nextByte >= 0;
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
		
		debug("Start read from file pos " + fileChannel.position() + " (" + filePos + ")");
		int read = input.read(byteBuffer.array(), byteBuffer.arrayOffset() + pos, rem);
		if(read < 0){
//			hasNext = false;
			byteBuffer.limit(pos);
			//return read;
		} else {
			byteBuffer.limit(pos + read);
		}
		debug("Bytes read from file: "+read+ " bytes");
		debug("Byte buffer after reading: "+byteBuffer);
		
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
	 * (current position is equal to the position of byte to be read next).<br>
	 * If the read byte was the last byte in byte buffer, the buffer is refilled.
	 * 
	 * IMPORTANT: This method will throw a RuntimeException if the reader is not
	 * in ASCII reading mode. 
	 * 
	 * @return byte that was just read
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
	
	/**
	 * Peek at the next byte (byte at current file position) without moving to
	 * it: neither buffer not file positions are changed.
	 * 
	 * IMPORTANT: This method will throw a RuntimeException if the reader is not
	 * in ASCII reading mode.
	 * 
	 * @return next byte (byte at current file position)
	 * @throws IllegalFormatException
	 *             if the file has no bytes to read
	 */
	protected byte peekNextByte() throws IllegalFormatException{
		if(!hasNext()){
			throw new IllegalFormatException("Unexpected end of file at pos " + filePos + " of file " + fileName);
		}
		if(currentMode != MODE_READING_ASCII_CHARS){
			throw new RuntimeException("Cannot read bytes in current mode: "+currentMode);
		}
		// next position should always be < limit if hasNext==false
		// because byte buffer is reloaded automatically when next byte is read
		// and buffer has nothing to read
		return byteBuffer.get(byteBuffer.position());

	}
	/**
	 * Reloads byte buffer. If there is nothing to read left in the file
	 * hasNext will become false;
	 * @throws IOException
	 */
	private void reloadByteBuffer()throws IOException {
		debug("Byte buffer reload (file pos = "+filePos+"): ");
		hasNext = readBytes()>=0;
		quoteScanResult.setScanned(false);
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
	 * Reads the next char within a string as it is. Does not check for any
	 * escape sequences.<br>
	 * If the end of string is reached the mode is changed to reading ASCII (=>
	 * should use getNextByte() to get next symbol which expected to be a
	 * quote).<br>
	 * <br>
	 * IMPORTANT: This method will throw a RuntimeException if the reader is not
	 * in UTF8 reading mode. It has to be in this mode only when a string is
	 * being read, i.e. after an opening quote and up to a closing quote.<br>
	 * To prepare the reader for the string-reading mode when an opening quote
	 * is met use <code>prepareForReadingAString()</code> method. <br>
	 * The reader automatically returns to ASCII-reading mode when an unmasked
	 * quote is met. To check if the reader in a string(UTF8)-reading mode use
	 * <code>isReadingString()</code>
	 * 
	 * @return nest char in a string
	 * @throws IOException
	 *             if I/O error occurs
	 * @throws IllegalFormatException
	 *             if the end of file is reached before the closing quote is met
	 */
	private char getNextChar() throws IOException, IllegalFormatException{
		if(!isReadingString()){
			throw new RuntimeException("Cannot read chars outside a string (current mode = " + currentMode + ")");
		}
		char curChar = charBuffer.get();
		// if char buffer is empty - fill it
		if (!charBuffer.hasRemaining()){
			// if byte buffer was read (decoded) up to quote we do not need to
			// reload char buffer (alternatively byte buffer can be decoded up to
			// position before the quote if char buffer did not have enough
			// space (overflow))
			if (quoteScanResult.reachedClosingQuote()
					&& byteBuffer.position() == quoteScanResult.getClosingQuotePos()) {
				//				reachedClosingQuote = false;
				quoteScanResult.reset();
				currentMode = MODE_READING_ASCII_CHARS;
				return curChar;
			} else {
				fillCharBufferUpToQuote();
				debug("GetNextChar char buffer load: " + new String(
						Arrays.copyOfRange(charBuffer.array(), charBuffer.position(), charBuffer.limit())));
			}
		}
		return curChar;
	}	

	/**
	 * Reads the next char within a string. If a string contains an escaped
	 * sequence (like \t or \" or \u0041), all chars within this sequence are
	 * read and the processed char is returned (i.e. 't' or '"' or 'A')<br>
	 * <br>
	 * If the end of string is reached the mode is changed to reading ASCII (=>
	 * should use getNextByte() to get next symbol which expected to be a
	 * quote).<br>
	 * <br>
	 * IMPORTANT: a RuntimeException will be thrown if the reader is not
	 * in UTF8 reading mode. It has to be in this mode only when a string is
	 * being read, i.e. after an opening quote and up to a closing quote.<br>
	 * To prepare the reader for the string-reading mode when an opening quote
	 * is met use <code>prepareForReadingAString()</code> method. <br>
	 * The reader automatically returns to ASCII-reading mode when an unmasked
	 * quote is met. To check if the reader in a string(UTF8)-reading mode use
	 * <code>isReadingString()</code>
	 * 
	 * @return next processed char in a string
	 * @throws IOException
	 *             if I/O error occurs
	 * @throws IllegalFormatException
	 *             if the end of file is reached before the closing quote is met
	 *             or non-valid escape sequence is found
	 */
	protected char getNextProcessedChar() throws IOException, IllegalFormatException{
		char curChar = getNextChar();
		if(curChar == '\\'){
			while(!stringReadingState.pushChar(curChar)){
				if(!isReadingString()){
					throw new IllegalFormatException(
							"End of string is reached in the middle of reading escaped sequence");
				}
				curChar = getNextChar();
			}	
			return stringReadingState.getChar();
		}
		return curChar;
	}

	protected void prepareForReadingAString()throws IOException, IllegalFormatException{
		currentMode = MODE_READING_UTF8_CHARS;
		stringReadingState.reset(StringReadingStateMachine.MODE_READ);
		quoteScanResult.reset();
		debug("Initial char buffer load: ");
		fillCharBufferUpToQuote();
		if (!charBuffer.hasRemaining()){
			// if byte buffer was read (decoded) up to quote we do not need to
			// reload char buffer (alternatively byte buffer can be decoded up to
			// position before the quote if char buffer did not have enough
			// space (overflow))
			if (quoteScanResult.reachedClosingQuote()
					&& byteBuffer.position() == quoteScanResult.getClosingQuotePos()) {
				quoteScanResult.reset();
				currentMode = MODE_READING_ASCII_CHARS;
				
			}
		}
		debug(charBuffer.limit() + " chars decoded: "
				+ new String(Arrays.copyOfRange(charBuffer.array(), charBuffer.position(), charBuffer.limit())));
	}
	protected boolean isReadingString(){
		return currentMode == MODE_READING_UTF8_CHARS;
	}
	
	/**
	 * Move the cursor to the end of a String, i.e. to the closing quote. It is
	 * assumed that before entering the method the cursor is within a String (i.e.
	 * an opening quote and possibly other symbols of the string were already read).
	 * <br>
	 * At the end of this method the cursor is at the closing quote (so the next
	 * byte should be a quote) and the mode is set to reading ASCII. <br>
	 * The reader can be in any reading mode before entering the method. It will be
	 * in the ASCII reading mode after the method
	 * 
	 * @return current file position (i.e. position of the closing quote)
	 * @throws IOException
	 *             if an I/O error occur
	 * @throws IllegalFormatException
	 *             if end of file is reached before closing quote is met
	 */
//	protected long skipTheString() throws IOException, IllegalFormatException{
//		if(!stringReadingState.isInFinalState()){
//			throw new RuntimeException(
//					"We should not have got here if the stateCheck is in non-final state (i.e. in the middle of a backslash sequence)");
//		}
//		if(reachedClosingQuote){
//			// if we reached the quote while previous scan it should be the
//			// first byte in the byte buffer
//			currentMode = MODE_READING_ASCII_CHARS;
//			reachedClosingQuote = false;
//			// imitate reading char buffer up to the end - for consistency
//			charBuffer.position(charBuffer.limit());
////			byte b = getNextByte();
////			if(b != '"'){
////				throw new RuntimeException(
////						"We should not have got here if the current byte is not a quote, got " + b + " instead");
////			}
//			return filePos;
//		}
//		// if the char buffer was not read up to its limit - read it, but keep track of reding state 
//		while(charBuffer.hasRemaining()){
//			char ch = charBuffer.get();
//			stringReadingState.pushChar(ch);
//			// we should not meet any closing quotes here
//			// because we already scanned for them before filling the char buffer
//		}
//		// now switch to bytes:
//		// search for the first unmasked quote in the byte buffer 
//		currentMode = MODE_READING_ASCII_CHARS;
//		int quotePos = -1;
//		while(hasNext() && quotePos < 0){
//			if(!byteBuffer.hasRemaining()){
//				reloadByteBuffer();
//			}
//			quotePos = scanForClosingQuote();
//			if(quotePos < 0){
//				filePos += byteBuffer.remaining();
//				byteBuffer.position(byteBuffer.limit());
//			}
//		}
//		if(quotePos < 0){
//			throw new IllegalFormatException("The end of file is reached before finding the closing quote");
//		}
//		// move filePosition and the buffer position to the quote pos
//		filePos += (quotePos - byteBuffer.position());
//		byteBuffer.position(quotePos);
//		// reset string reading state since we finished with this string
//		stringReadingState.reset();
//		return filePos;
//	}

	protected void skipTheString() throws IOException, IllegalFormatException{
		if(!stringReadingState.isInFinalState()){
			throw new RuntimeException(
					"We should not have got here if the stateCheck is in non-final state (i.e. in the middle of a backslash sequence)");
		}
		// imitate reading char buffer up to the end - for consistency
		charBuffer.position(charBuffer.limit());
		currentMode = MODE_READING_ASCII_CHARS;	
		// if we have not found a quote during scan the current byte buffer
		// we need to search in the next chunk(s) of bytes
		while (!quoteScanResult.reachedClosingQuote()) {
			// if we already scanned current buffer we do need to do it again
			if (quoteScanResult.wasScanned()) {
				// imitate reading byte buffer up to the end
				filePos += byteBuffer.remaining();
				byteBuffer.position(byteBuffer.limit());
				reloadByteBuffer();
				if(!hasNext()){
					throw new IllegalFormatException("The end of file is reached before finding the closing quote");
				}
			}
			// method uses quoteScanResult to maintain escaped state between
			// byte buffer reloads
			searchForClosingQuoteInCurrentByteBuffer();
		}
		// we found the quote - jump directly to closing quote
		filePos += quoteScanResult.getClosingQuotePos() - byteBuffer.position();
		byteBuffer.position(quoteScanResult.getClosingQuotePos());
		debug("Jump to the closing quote pos: in file " + filePos + ", in byteBuffer "
				+ quoteScanResult.getClosingQuotePos() + " (" + byteBuffer + ")");
		// reset string reading state and closing quote scan result since we
		// finished with this string
		quoteScanResult.reset();
		stringReadingState.reset();
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
	 * @return true if an unmasked quote has been meet
	 * @throws IOException
	 *             if I/O error occurs
	 * @throws IllegalFormatException
	 *             if the end of file is reached while scanning for closing
	 *             quote
	 */
	private void fillCharBufferUpToQuote() throws IOException, IllegalFormatException{
		debug("Read chars up to quote from : "+ byteBuffer);
		charBuffer.clear();
//		boolean quoteReached = false;
		while(true) {
			int posBeforeDecoding = byteBuffer.position();
//			int quotePos = 
			if(!quoteScanResult.reachedClosingQuote()){
				debug("Scaning for closing quote starting from pos " + posBeforeDecoding + "...");
				searchForClosingQuoteInCurrentByteBuffer();
			}
//			int quotePos = quoteScanResult.getClosingQuotePos();
			debug(quoteScanResult.reachedClosingQuote()?
					" quote is at byte buffer pos " + quoteScanResult.getClosingQuotePos() : 
						" no quote found.");
			int byteOldLimit = byteBuffer.limit();
			if(quoteScanResult.reachedClosingQuote()){
				byteBuffer.limit(quoteScanResult.getClosingQuotePos());
//				quoteReached = true;
			}
			CoderResult cr = decoder.decode(byteBuffer, charBuffer, quoteScanResult.reachedClosingQuote());
			debug("\tDecoded "+ (byteBuffer.position() - posBeforeDecoding)+" bytes");
			// update file position based on the number of read and decoded bytes
			filePos += (byteBuffer.position() - posBeforeDecoding); 
			 // restore old byte array limit  
			 byteBuffer.limit(byteOldLimit);
			 
			if (cr.isUnderflow()) {
				debug("\tUnderflow, closing quote pos = "+quoteScanResult.getClosingQuotePos() + ": "+byteBuffer);
				if (quoteScanResult.reachedClosingQuote()){
					 break;
				}
//				assert byteOldLimit == byteBuffer.limit(): byteOldLimit - byteBuffer.limit();	
				if (!charBuffer.hasRemaining()){
					break;
				}
				// if ((cb.position() > 0) && !inReady())
				// break; // Block at most once
//				if(byteBuffer.position() > 0){
//					escaped = byteBuffer.get(byteBuffer.position()-1) == '\\';
//				}
//				if (DEBUG) System.out.println("\t\tRead chars: byte reload (escaped = "+escaped+")");
//				int n = readBytes();
				reloadByteBuffer();
				if (!hasNext) {
					throw new IllegalFormatException("Unexpected end of file while reading String");
				}
				continue;
			} else if (cr.isOverflow()) {
				debug("Overflow");
				break;
			} else {
				 cr.throwException();
			}
		}
		 if (quoteScanResult.reachedClosingQuote()) {
			 // reset decoder 
			 // TODO: should I reset it only when closing quote is reached?
			 decoder.reset();
		 }
		if (charBuffer.position() == 0 && !quoteScanResult.reachedClosingQuote()) {
			throw new RuntimeException("No chars were decoded from byte buffer and the closing quote is not reached");
		}
		// made just decoded chars available for reading
		charBuffer.flip();
		
//		return quoteReached;//charBuffer.remaining();
		
	}
	
	/**
	 * Search for an unmasked quote in the current byteBuffer without refilling
	 * the buffer and without updating buffer or file position. <br>
	 * Results of the scan are saved in <code>quoteScanResult</code>, which
	 * contains an unmasked quote position (or -1 if no quote was found)
	 * relative to the buffer (not a file position!) and an escaped state at the
	 * end of the buffer (make sense only when quote is not met) <br>
	 * <br>
	 * IMPORTANT: everything stays the same after this method: the buffer
	 * position as well as the file position and the state of stringReadingState
	 * stay the same as they were before the method. The only thing that is
	 * updated is quoteScanResult. Also, a new mark in the byte buffer is set.
	 */
	private void searchForClosingQuoteInCurrentByteBuffer(){
		if(!byteBuffer.hasRemaining()){
			return;
		}
		int quotePos = -1;
		boolean escaped = quoteScanResult.isEscaped();//stringReadingState.isInEscapedState();
		// remember the start position
		byteBuffer.mark();
		// scan for an unmasked quote
		while(byteBuffer.hasRemaining()){
			byte b = byteBuffer.get();
			if(b == '"' && !escaped){
				quotePos = byteBuffer.position() - 1;// since we've already read the quote
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
		// save scan result
		quoteScanResult.setReachedClosingQuotePos(quotePos);
		quoteScanResult.setEscaped(escaped);
		quoteScanResult.setScanned(true);
	//	return quotePos;
	}
	
	private static class ClosingQuoteScanResult{
		boolean scanned = false;
		private boolean escaped = false;
		private int closingQuoteBufferPos = -1;
		
		void reset(){
			closingQuoteBufferPos = -1;
			escaped = false;
			scanned = false;
		}
		boolean reachedClosingQuote(){
			return closingQuoteBufferPos >= 0;
		}
		
		void setReachedClosingQuotePos(int pos){
			this.closingQuoteBufferPos = pos;
		}
		
		void setEscaped(boolean escaped){
			this.escaped = escaped;
		}
		boolean isEscaped(){
			return escaped;
		}
		int getClosingQuotePos(){
			return closingQuoteBufferPos;
		}
		boolean wasScanned(){
			return scanned;
		}
		void setScanned(boolean wasScanned){
			scanned = wasScanned;
		}
	}

}

