package com.bigjson.parser;

public class QuoteSearchingStateMachine {
	private int state = STATE_DEFAULT;
	
	public static final int STATE_DEFAULT = 0;
	public static final int STATE_ESCAPE = 1;

	public void reset(){
		state = STATE_DEFAULT;
	}
	
	public boolean isInFinalState(){
		return state == STATE_DEFAULT;
	}
	
	public boolean isInEscapedState(){
		return state == STATE_ESCAPE;
	}
	
	/**
	 * Take in the next byte and update the reading state
	 * 
	 * @param ch
	 * @return true if the current state is final and the machine is not waiting
	 *         for the next byte
	 * @throws IllegalFormatException
	 */
	public boolean pushByte(byte b) throws IllegalFormatException{
		if(state == STATE_DEFAULT){
			if(b == '\\'){
				state = STATE_ESCAPE;
				return false; // is not a terminal state: should read a symbol after '\'
			}
			return true;
		} 
		if(state == STATE_ESCAPE){
			state = STATE_DEFAULT;
			return true;
		}
		throw new RuntimeException("Invalid state value: "+state);
	}
	
}
