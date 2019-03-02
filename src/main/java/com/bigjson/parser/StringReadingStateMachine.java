package com.bigjson.parser;

public class StringReadingStateMachine {
	private int mode = MODE_READ;
	private int state = STATE_DEFAULT;
	private char charToReturn;
	private char[] unicodeDigits = new char[4];
	private static final int hex_radix = 16;
	
	public static final int MODE_CHECK_FORMAT = 2;
	public static final int MODE_READ = 2;
	public static final int STATE_DEFAULT = 0;
	public static final int STATE_ESCAPE = 1;
	public static final int STATE_UNICODE_U = 2;
	public static final int STATE_UNICODE_0 = 3;
	public static final int STATE_UNICODE_1 = 4;
	public static final int STATE_UNICODE_2 = 5;
	
	StringReadingStateMachine(int mode){
		reset(mode);
	}
	
	void reset(int mode){
		state = STATE_DEFAULT;
		this.mode = mode;
	}
	void reset(){
		state = STATE_DEFAULT;
	}
	
	boolean isInFinalState(){
		return state == STATE_DEFAULT;
	}
	
	boolean isInEscapedState(){
		return state == STATE_ESCAPE;
	}
	
	char getChar(){
		if(mode != MODE_READ){
			throw new RuntimeException("The current mode is not a reading mode (mode = " + mode + ")");
		}
		if(state != STATE_DEFAULT){
			throw new RuntimeException("This is not a final state, unable to return a valid character");
		}
		return charToReturn;
	}
	boolean pushByte(byte b) throws IllegalFormatException{
		return pushChar((char)(b & 0xFF));
	}
	/**
	 * Take in the next char and update the reading state. If the state is final and
	 * the mode == MODE_READ the processed char can be obtained via getChar() method
	 * @param ch
	 * @return true if the current state is final and a char is ready to be returned
	 * @throws IllegalFormatException
	 */
	boolean pushChar(char ch) throws IllegalFormatException{
		if(state == STATE_DEFAULT){
			if(ch == '\\'){
				state = STATE_ESCAPE;
				return false; // is not a terminal state: should read a symbol after '\'
			}
			charToReturn = ch;
			return true;
		} 
		if(state == STATE_ESCAPE){
			return escapeStatePush(ch);
		}
		if(state == STATE_UNICODE_U){
			unicodeDigits[0] = charToDigit(ch);
			state = STATE_UNICODE_0;
			return false;
		}
		if(state == STATE_UNICODE_0){
			unicodeDigits[1] = charToDigit(ch);
			state = STATE_UNICODE_1;
			return false;
		}
		if(state == STATE_UNICODE_1){
			unicodeDigits[2] = charToDigit(ch);
			state = STATE_UNICODE_2;
			return false;
		}
		if(state == STATE_UNICODE_2){
			unicodeDigits[3] = charToDigit(ch);
			unicodeToChar();
			state = STATE_DEFAULT;
			return true;
		}
		throw new RuntimeException("Invalid state value: "+state);
	}
	
	private boolean escapeStatePush(char ch) throws IllegalFormatException {
		if(ch == 'u'){
			state = STATE_UNICODE_U;
			return false;
		}else if(ch == '\\' || ch == '/' || ch == '"'){
			charToReturn = ch;
		}else if(ch == 'b'){
			charToReturn = '\b';
		}else if(ch == 'f'){
			charToReturn = '\f';
		}else if(ch == 'n'){
			charToReturn = '\n';
		}else if(ch == 'r'){
			charToReturn = '\r';
		}else if(ch == 't'){
			charToReturn = '\t';
		}else {
			throw new IllegalFormatException("Unexped char after a backslash in a String: '" + ch
					+ "'. Should be one of these: \", \\, /, b, f, n, r, t, u");
		}
		state = STATE_DEFAULT;
		return true;
	}

	private char charToDigit(char ch)throws IllegalFormatException{
		int d = Character.digit(ch, hex_radix);
		if(d < 0){
			throw new IllegalFormatException(
					"Unexpected symbol in a four hex digit unicode character representation. Expected a hex digit, got: '"
							+ ch + "'");
		}
		return (char)d;
	}
	
	private void unicodeToChar() {
		charToReturn = 0;
		for(int i = 0; i < unicodeDigits.length; i++){
			// since we have only 4 hex digits we do not expect the result number to be bigger than Character.MAX_VALUE
			charToReturn *= hex_radix;
			charToReturn += unicodeDigits[i];
		}
	}
}
