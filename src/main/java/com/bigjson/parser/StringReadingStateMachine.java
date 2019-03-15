package com.bigjson.parser;

public class StringReadingStateMachine {
	private int mode = MODE_READ_UTF8;
	private int state = STATE_DEFAULT;
	private char charToReturn;
	private char[] unicodeDigits = new char[4];
	private static final int hex_radix = 16;
	
	public static final int MODE_CHECK_ASCII = 0;
	public static final int MODE_CHECK_UTF8 = 1;
	public static final int MODE_READ_UTF8 = 2;
	public static final int STATE_DEFAULT = 0;
	public static final int STATE_ESCAPE = 1;
	public static final int STATE_UNICODE_U = 2;
	public static final int STATE_UNICODE_0 = 3;
	public static final int STATE_UNICODE_1 = 4;
	public static final int STATE_UNICODE_2 = 5;
	
	public StringReadingStateMachine(int mode){
		reset(mode);
	}
	
	/**
	 * Reset the state to the default, set new mode.
	 */
	public void reset(int mode){
		state = STATE_DEFAULT;
		changeMode(mode);
	}
	
	/**
	 * Reset the state to the default, maintain the mode.
	 */
	public void reset(){
		state = STATE_DEFAULT;
	}
	
	/**
	 * Change the mode, maintain the state
	 * @param newMode
	 */
	public void changeMode(int newMode){
		if(mode != MODE_CHECK_ASCII && mode != MODE_CHECK_UTF8 && mode != MODE_READ_UTF8){
			throw new IllegalArgumentException("Unknow mode value: " + mode);
		}
		this.mode = newMode;
	}
	
	public boolean isInFinalState(){
		return state == STATE_DEFAULT;
	}
	
	public boolean isInEscapedSequence(){
		return state != STATE_DEFAULT;
	}
	
	public char getChar(){
		if(mode != MODE_READ_UTF8){
			throw new RuntimeException("The current mode is not a reading mode (mode = " + mode + ")");
		}
		if(state != STATE_DEFAULT){
			throw new RuntimeException("This is not a final state, unable to return a valid character");
		}
		return charToReturn;
	}
	
	/**
	 * Take in next ASCII byte. Ignore bytes that are not in ASCII range ([0,
	 * 128)) and not in escape sequence.<br>
	 * <br>
	 * IMPORTANT: should be used only in the checking ASCII mode - will throw a
	 * RuntimeException otherwise. <br>
	 * IMPORTANT: does not check if the string is over
	 * 
	 * @param b
	 * @throws IllegalFormatException
	 *             <ul>
	 *             <li>if <code>ch</code> is a control character (code < 32)
	 *             <li>if <code>ch</code> is escaped and not one of these: ", \,
	 *             /, b, f, n, r, t, u
	 *             <li>if <code>ch</code> is one of the four chars after \\u and
	 *             is not a hex digit
	 *             </ul>
	 */
	public void pushByte(byte b)  throws IllegalFormatException {
		if(mode != MODE_CHECK_ASCII){
			throw new RuntimeException(
					"Cannot accept bytes when not in checking ASCII mode (current mode = " + mode + ")");
		}
		if(b < 0){ 
			// byte is not in ASCII range and we are not in escape sequence: just skip it
			if(isInFinalState()){
				return;
			}
			throw new IllegalFormatException("Unexpected byte in an escaped sequece: " + b);
		}
		pushChar((char)b);
	}

	/**
	 * Take in the next char and update the reading state. <br>
	 * If the state is final and the mode == MODE_READ the processed char can be
	 * obtained via getChar() method<br>
	 * <br>
	 * IMPORTANT: does not check if the string is over
	 * 
	 * @param ch
	 * @return true if the current state is final and a char is ready to be
	 *         returned
	 * @throws IllegalFormatException
	 *             <ul>
	 *             <li>if <code>ch</code> is a control character (code < 32)
	 *             <li>if <code>ch</code> is an quote and the machine is not in
	 *             ESCAPE state
	 *             <li>if <code>ch</code> is escaped (machine is in ESCAPE
	 *             state) and not one of these: ", \, /, b, f, n, r, t, u
	 *             <li>if <code>ch</code> is one of the four chars after \\u and
	 *             is not a hex digit
	 *             </ul>
	 */
	public boolean pushChar(char ch) throws IllegalFormatException{
//		System.out.println("push char '" + ch + "', mode = " + mode);
		if(ch < 32){// < 20 in hex
			throw new IllegalFormatException("Got control character in a string (code = " + (int)ch + ")");
		}
		if(state == STATE_DEFAULT){
			if(ch == '\\'){
				state = STATE_ESCAPE;
				return false; // is not a terminal state: should read a symbol after '\'
			}
			if(ch == '"'){
				throw new IllegalFormatException("Got an unmasked quote in a string");
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
			if(mode == MODE_READ_UTF8){
				unicodeToChar();
			}
			state = STATE_DEFAULT;
			return true;
		}
		throw new RuntimeException("Invalid state value: "+state);
	}
	
	/**
	 * 
	 * @param ch
	 * @return
	 * @throws IllegalFormatException
	 *             if get an illegal escaped char (should be one of these: ", \,
	 *             /, b, f, n, r, t, u)
	 */
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
	/**
	 * 
	 * @param ch
	 * @return
	 * @throws IllegalFormatException if <code>ch</code> is not a hex digit (0-9, A-F, a-f)
	 */
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
