package com.bigjson.parser;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;

import org.junit.Test;
import static org.junit.Assert.*;

import com.bigjson.parser.IllegalFormatException;
import com.bigjson.parser.StringReadingStateMachine;

public class StringReadingStateMachineTest {
	
	private StringReadingStateMachine stringReading = new StringReadingStateMachine(StringReadingStateMachine.MODE_CHECK_UTF8);
	private Charset utf8 = Charset.forName("UTF-8");
	private CharsetDecoder decoder = utf8.newDecoder()
	         .onMalformedInput(CodingErrorAction.REPORT)
	         .onUnmappableCharacter(CodingErrorAction.REPORT);
	
	@Test
	public void shouldReadEscapeSequence()throws IllegalFormatException, CharacterCodingException {
		assertEquals("aaa\"bb", checkFormat("aaa\\\"bb"));// "
		assertEquals("aaa\\bb", checkFormat("aaa\\\\bb"));// \
		assertEquals("aaa/bb",  checkFormat("aaa\\/bb")); // /
		assertEquals("aaa\bbb", checkFormat("aaa\\bbb")); // \b
		assertEquals("aaa\fbb", checkFormat("aaa\\fbb")); // \f
		assertEquals("aaa\nbb", checkFormat("aaa\\nbb")); // \n
		assertEquals("\rbb", checkFormat("\\rbb")); // \r
		assertEquals("aaa\tbb", checkFormat("aaa\\tbb")); // \t
		assertEquals("aaa\u09Fabb", checkFormat("aaa\\u09Fabb")); // unicode char in hex
		assertEquals("\uFFFF", checkFormat("\\uFFFF")); // unicode char in hex
	}
	
	@Test
	public void shouldThrowExceptionForUnfinishedEscapeSeq() throws IllegalFormatException, CharacterCodingException {
		expectIllegalFormatException("aaa\\");
		expectIllegalFormatException("aaa\\u09F"); 		
	}
	@Test
	public void shouldThrowExceptionForUnexpectedSymbolInEscapeSeq() throws IllegalFormatException, CharacterCodingException {
		expectIllegalFormatException("aaa\\zbb");
		expectIllegalFormatException("aaa\\.bb");
		expectIllegalFormatException("aaa\\u0JF1bb");		
	}
	
	@Test
	public void shouldThrowExceptionForNotAllowedSymbol() throws IllegalFormatException, CharacterCodingException {
		expectIllegalFormatException("aaa\nbb");
		expectIllegalFormatException("\r");
		expectIllegalFormatException("\"");	
	}

	private void expectIllegalFormatException(String str) throws IllegalFormatException, CharacterCodingException {
		expectIllegalFormatException(str, StringReadingStateMachine.MODE_CHECK_ASCII);
		expectIllegalFormatException(str, StringReadingStateMachine.MODE_CHECK_UTF8);
		expectIllegalFormatException(str, StringReadingStateMachine.MODE_READ_UTF8);
	}
	
	private void expectIllegalFormatException(String str, int mode)
			throws IllegalFormatException, CharacterCodingException {
		try{
			if(mode == StringReadingStateMachine.MODE_CHECK_ASCII){
				checkFormatASCIIOnly(str);
			} else if(mode == StringReadingStateMachine.MODE_CHECK_UTF8){
				checkFormatUTF8(str);
			} else if(mode == StringReadingStateMachine.MODE_READ_UTF8){
				readString(str);
			}
		}catch(IllegalFormatException e){
			System.err.println(e.getMessage());
			return;
		}
		throw new RuntimeException("The method should have thrown an IllegalFormatException for a string \"" + str
				+ "\" and mode = " + mode);
	}
	
	private String checkFormat(String str) throws IllegalFormatException, CharacterCodingException{
		checkFormatASCIIOnly(str);
		checkFormatUTF8(str);
		return readString(str);
	}
	
	private void checkFormatASCIIOnly(String str) throws IllegalFormatException{
		// byte check
		stringReading.reset(StringReadingStateMachine.MODE_CHECK_ASCII);
		for(byte b: str.getBytes()){
			stringReading.pushByte(b);
		}
		if(!stringReading.isInFinalState()){
			throw new IllegalFormatException(
					"End of string in the middle of an escape sequence while checking ASCII only: \"" + str + "\"");
		}
	}
	private void checkFormatUTF8(String str) throws IllegalFormatException, CharacterCodingException{
		// utf-8 char check
		CharBuffer charBuf = decoder.decode(ByteBuffer.wrap(str.getBytes(utf8)));
		stringReading.reset(StringReadingStateMachine.MODE_CHECK_UTF8);
		while(charBuf.hasRemaining()){
			stringReading.pushChar(charBuf.get());
		}
		if(!stringReading.isInFinalState()){
			throw new IllegalFormatException(
					"End of string in the middle of an escape sequence while checking UTF-8: \"" + str + "\"");
		}
	}
	private String readString(String str) throws IllegalFormatException, CharacterCodingException{
		CharBuffer charBuf = decoder.decode(ByteBuffer.wrap(str.getBytes(utf8)));
		stringReading.reset(StringReadingStateMachine.MODE_READ_UTF8);
		StringBuilder sb = new StringBuilder();
		while(charBuf.hasRemaining()){
			stringReading.pushChar(charBuf.get());
			if(stringReading.isInFinalState()){
				sb.append(stringReading.getChar());
			}
		}
		if(!stringReading.isInFinalState()){
			throw new IllegalFormatException(
					"End of string in the middle of an escape sequence while reading a string: \"" + str + "\"");
		}
		return sb.toString();
	}

}
