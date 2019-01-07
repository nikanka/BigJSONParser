package main.java.parser;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;

public class UTF8FileReader {
	public static final int bufferSize = 8192;
	
	boolean DEBUG = true;
	
	protected long filePos = 0;
	private FileInputStream input;
	protected FileChannel fileChannel;
	protected ByteBuffer byteBuffer;
	protected CharBuffer charBuffer;
	protected boolean hasNext = true;
	private CharsetDecoder decoder = Charset.forName("UTF-8").newDecoder()
			         .onMalformedInput(CodingErrorAction.REPORT)
			         .onUnmappableCharacter(CodingErrorAction.REPORT);

	
	public static void main(String[] args)throws IOException {
		String testFile = "SmallTest2.json";
		UTF8FileReader reader = new UTF8FileReader(testFile);
//		testRead(reader);
//		int n=0;
//		while(reader.hasNext()){
//			reader.getNextByte();
//			n++;
//		}
//		System.out.println("There are total "+n+" bytes in file "+testFile);
		testReadOnlyStrings(reader);
//		System.out.println(reader.readUTF8String(3527, 3527+23));
		
//		StringBuilder sb = new StringBuilder();
//		long firstOpen = -1;
//		long firstClose = -1;
//		while(reader.hasNext()){
//			byte b = reader.getNextByte();
//			if(b=='['){
//				firstOpen = reader.filePos;	
//			} else if(b==']'){
//				firstClose = reader.filePos;
//			}
//		}
//		System.out.println("First '[' pos = "+firstOpen);
//		System.out.println("First ']' pos = "+firstClose);
//		reader.getToPosition(firstOpen);
//		while(reader.hasNext() && reader.filePos < firstClose){
//			sb.append((char)reader.getNextByte());
//		}
//		System.out.print(sb.toString());
	}
	public static void testReadOnlyStrings(UTF8FileReader reader)throws IOException{
//		UTF8FileReader reader = new UTF8FileReader(testFile);
//		scanForClosingQuote(curByte)
//		long openPos = -1;
//		long closePos = -1;
		while(reader.hasNext()){
			byte b = reader.getNextByte();
			System.out.println("next byte : '"+(char)b+"'");
			if(b == '"'){
//				openPos = reader.getFilePosition();
//				closePos = reader.scanForClosingQuote(b);
				System.out.println("file pos = "+reader.filePos+", buff pos = "+reader.byteBuffer.position());
				String str = reader.readString();
				b = reader.getNextByte();
				assert b=='"': (char)b;
				System.out.println(reader.byteBuffer+" (filePos = "+reader.filePos+")");
				System.out.println(str);
			}
		}
	}

	
	public UTF8FileReader(String fileName) throws IOException {
		input = new FileInputStream(fileName);
		fileChannel = input.getChannel();
		charBuffer = CharBuffer.allocate(bufferSize);
		byteBuffer = ByteBuffer.allocate(bufferSize);
//		System.out.println("Init buffer: "+byteBuffer.toString()+" offset = "+byteBuffer.arrayOffset());
		
		byteBuffer.flip();
		charBuffer.flip();
//		System.out.println("After flip: "+byteBuffer.toString()+" offset = "+byteBuffer.arrayOffset());
//		if(readBytes()<0){
//			hasNext = false;
//		}
		readBytes();
	}
	/**
	 * Return current file position in bytes.
	 * @return
	 */
	public long getFilePosition(){
		return filePos;
	}
	protected int readBytes(/*long limitFilePos*/) throws IOException{
//		long toLoad = -1;
//		if(limitFilePos >= 0){
//			if(limitFilePos < filePos){
//				throw new IllegalArgumentException("Limit position ("+limitFilePos+") "
//					+ "cannot be less than current file position ("+filePos+") ");
//			}
//			long toRead = limitFilePos - filePos;
//			toLoad = toRead - byteBuffer.remaining();
//			if(toLoad <=0 ){
//				byteBuffer.limit(byteBuffer.position()+(int)toRead);
//				return byteBuffer.remaining();
//			}
//		}
//		System.out.println("Byte buffer before reading: "+byteBuffer);
		byteBuffer.compact();
//		System.out.println("After compact: "+byteBuffer.toString()+" offset = "+byteBuffer.arrayOffset());
		int pos = byteBuffer.position();
		int rem = byteBuffer.remaining();
//		if(toLoad > 0 && toLoad < rem){
//			byteBuffer.limit(pos + (int)toLoad);
//			rem = byteBuffer.remaining();
//		}
		assert(rem >0): rem;
		
		System.out.println("Start read from file pos "+fileChannel.position()+
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
		System.out.println("Bytes read from file: "+read+ " bytes");
		System.out.println("Byte buffer after reading: "+byteBuffer);
		
		return byteBuffer.remaining();
	}
	

	
//	protected String readUTF8String(long fromFilePos, long toFilePos) throws IOException{
//		if(toFilePos <= fromFilePos){
//			throw new IllegalArgumentException("To ("+toFilePos+") is less or equal to"
//					+ " from ("+fromFilePos+")");
//		}
//		long len = toFilePos - fromFilePos;
//		if(len == 0){
//			return "";
//		}
//		if(len > Integer.MAX_VALUE){
//			throw new RuntimeException("String to be read is too big. File byte coordinates: "
//					+ "["+fromFilePos+"; "+toFilePos+")");
//		}
//		fileChannel.position(fromFilePos);
//		byte[] byteArr = new byte[(int)len];
//		int read = input.read(byteArr);
//		if(read < 0){
//			return null;
//		}
//		if(read == 0) {
//			throw new IOException("Input stream returned 0 bytes");
//		}
//		CharBuffer charBuf = CharBuffer.allocate(read);
//		CoderResult cr = decoder.decode(ByteBuffer.wrap(byteArr, 0, read), charBuf, true);
//		assert cr != CoderResult.OVERFLOW: cr;// each byte can be decoded in AT MOST one char
//		if(cr == CoderResult.UNDERFLOW){
//			return new String(charBuf.array(), 0, charBuf.position());
//		} else {
//			cr.throwException();
//			return null;
//		}
//	}
//
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
//	/**
//	 * Decode characters from byte input stream from current position 
//	 * until a quote is met. The validity of the String is checked
//	 * @return the length of the string
//	 */
//	int initialStringCheck(){
//		//TODO
//		// first scan to find a quote
//		return 0;
//	}
	/**
	 * Decode characters from byte input stream for specified range.
	 * 
	 * @param from start file pos of bytes to decode (including) 
	 * @param to end file pos of bytes to decode (exclusive)
	 * @return the decoded string
	 */
//	String readString(long from, long to)throws IOException{
//		getToPosition(from);
//		StringBuilder sb = new StringBuilder();
//		for(;;){
//			int n = readCharsUpToQuote();
//			sb.append(charBuffer.array(), 0, n);
//			if(n < charBuffer.capacity()){
//				break;
//			}
//		}
//		return sb.toString();
//		return null;
//	}
	/**
	 * Read and return a String starting at current file position and up to an 
	 * unmasked quote. It is assumed the the opening quote has already been read.
	 * The file cursor is at the closing quote after this method.
	 * @return a String that was read (without quotes)
	 * @throws IOException
	 */
	//TODO: which method for loading Strings to use: this or LazyByteStreamParser.parseString()???
	public String readString()throws IOException{
		StringBuilder sb = new StringBuilder();
		boolean lastCharIsBackSlash = false;
		for(;;){
//			System.out.println("byte buff pos before: "+byteBuffer.position());
			int n = fillCharBufferUpToQuote(lastCharIsBackSlash);
			assert charBuffer.arrayOffset()==0: charBuffer.arrayOffset();
			char[] charArr = charBuffer.array();
			sb.append(charArr, 0, n);
			//System.out.println(sb.toString());
//			System.out.println("byte buff pos after: "+byteBuffer.position());
			System.out.println(
					"Read " + n + " chars: CharBuffer [pos=" + charBuffer.position() + " lim=" + charBuffer.limit()
							+ " cap=" + charBuffer.capacity() + "]\n" + byteBuffer + " (filePos = " + filePos + ")");
			if(n < charBuffer.capacity()){
				break;
			}
			lastCharIsBackSlash = charArr[n-1]=='\\';
		}
		System.out.println("End of readStringTest: "+byteBuffer+" (filePos = "+filePos+")");
		return sb.toString();
	}
	

	/**
	 * Fill the char buffer with chars starting from current position and up
	 * to an unmasked quote (or the end of the char buffer).
	 * 
	 * @param prevCharWasBackSlash
	 * @return the number of remaining elements of the char buffer (from 
	 * the current position and up to its limit)
	 * @throws IOException
	 */
	protected int fillCharBufferUpToQuote(boolean prevCharWasBackSlash) throws IOException{
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
			int quotePos = scanForClosingQuote(escaped);
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
					eof = true;
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
	
	
	public byte getNextByte() throws IOException{
		if(!hasNext()){
			throw new RuntimeException("Unexpected end of stream at pos " + filePos);
		}
//		byte ret =  (byte)nextByte;
//		nextByte = input.read();
//		curPos++;
		byte ret = byteBuffer.get();
		filePos++;
		if(byteBuffer.position() == byteBuffer.limit()){
			System.out.println("GetNextByte buffer reload");
			readBytes();
//			if(readBytes()<0){
//				hasNext = false;
//			}
		}
		return ret;
	}
	
	public boolean hasNext(){
//		return byteBuffer.position()<= byteBuffer;
		return hasNext; 
	}

	
//	public char getNextChar() throws IOException{
//		if(!hasNext()){
//			throw new RuntimeException("Unexpected end of stream at pos " + filePos);
//		}
//		
//		char ret = charBuffer.get();
//		
//		return ret;
//	}
	
	
	
	/*public int getNextChar(boolean detectQuote) throws IOException {
		if (haveLeftoverChar) {
			haveLeftoverChar = false;
			return leftoverChar;
		}
		charBuffer.clear();

		boolean eof = false;
		for (;;) {
			int posBeforeDecoding = byteBuffer.position();
			CoderResult cr = decoder.decode(byteBuffer, charBuffer, eof);
			int readByteNum = byteBuffer.position() - posBeforeDecoding; 
			if (cr.isUnderflow()) {
				// if (eof)
				// break;
				if (!charBuffer.hasRemaining())
					break;
				// if ((cb.position() > 0) && !inReady())
				// break; // Block at most once
				int n = readBytes();
				if (n < 0) {
					eof = true;
					// if ((cb.position() == 0) && (!byteBuffer.hasRemaining()))
					// break;
//					decoder.reset();
				}
				 continue;
			} else if (cr.isOverflow()) {
				// assert cb.position() > 0;
				if(detectQuote){
					if(charBuffer.get(0) == '"' && prevChar != '\\'){
						closingQuoteFilePos = filePos;
					}
				}
				break;
			} else {
				cr.throwException();
			}
		}

		 if (eof) {
		 // ## Need to flush decoder
		 decoder.reset();
		 }
		

//		filePos += re
		if (charBuffer.position() == 0) {
			if (eof) {
				return -1;
			} else {
				throw new RuntimeException("No chars were decoded from byte buffer");
			}
		}
		if (charBuffer.position() == 2) {
			leftoverChar = charBuffer.get(1);
			haveLeftoverChar = true;
		}

		return charBuffer.get(0);

	}

    int implRead(char[] cbuf, int off, int end) throws IOException {

        // In order to handle surrogate pairs, this method requires that
        // the invoker attempt to read at least two characters.  Saving the
        // extra character, if any, at a higher level is easier than trying
        // to deal with it here.
        assert (end - off > 1);

        CharBuffer cb = CharBuffer.wrap(cbuf, off, end - off);
        if (cb.position() != 0)
        // Ensure that cb[0] == cbuf[off]
        cb = cb.slice();

        boolean eof = false;
        for (;;) {
        CoderResult cr = decoder.decode(byteBuffer, cb, eof);
        if (cr.isUnderflow()) {
            if (eof)
                break;
            if (!cb.hasRemaining())
                break;
//            if ((cb.position() > 0) && !inReady())
//                break;          // Block at most once
            int n = readBytes();
            if (n < 0) {
                eof = true;
//                if ((cb.position() == 0) && (!byteBuffer.hasRemaining()))
//                    break;
                decoder.reset();
            }
            continue;
        }
        if (cr.isOverflow()) {
//            assert cb.position() > 0;
            break;
        }
        cr.throwException();
        }

//        if (eof) {
//        // ## Need to flush decoder
//        decoder.reset();
//        }

        if (cb.position() == 0) {
            if (eof)
                return -1;
            assert false;
        }
        return cb.position();
    }

    private boolean inReady() {
    	        try {
    	        return ((input != null) && (input.available() > 0));     	        } catch (IOException x) {
    	        return false;
    	        }
    	    }
*/    
//    private int read0() throws IOException {
//             // Return the leftover char, if there is one
//            if (haveLeftoverChar) {
//                haveLeftoverChar = false;
//                return leftoverChar;
//            }
//
//            // Convert more bytes
//            char cb[] = new char[2];
//            int n = read(cb, 0, 2);
//            switch (n) {
//            case -1:
//                return -1;
//            case 2:
//                leftoverChar = cb[1];
//                haveLeftoverChar = true;
//                // FALL THROUGH
//            case 1:
//                return cb[0];
//            default:
//                assert false : n;
//                return -1;
//            }
//        
//    }
	/**
	 * Set this file channel to a position prior to <code>pos</code>, so that the next byte that is read
	 * is at the position <code>pos</code> of the file.
	 * @param pos
	 * @return
	 * @throws IOException
	 */
	public boolean getToPosition(long pos)throws IOException{
		System.out.println("get to pos "+pos);
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
/*	
	public int nextNonspaceByte()throws IOException{
		while(hasNext()){
			byte ret = getNextByte();
			if(!Character.isWhitespace(ret)){
				return ret;
			}
		}
		return -1;
	}
*/	
	/**
	 * Read next char from the file assuming that the encoding is UTF-8
	 * @return
	 */
//	char getNextChar(){
//		InputStreamReader
//	}
	
	
}

