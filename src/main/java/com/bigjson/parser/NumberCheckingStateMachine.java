package com.bigjson.parser;

public class NumberCheckingStateMachine {
	
	public static enum State {
		SIGN, FIRST_DIDIT, AFTER_FIRST, DIGIT, FRAC_FIRST, FRAC, EXP_SIGN, EXP_DIGIT_FIRST, EXP_DIGIT
	}
	
	private State state;
	
	public NumberCheckingStateMachine(){
		reset();
	}
	public void reset(){
		// every number must start with SIGN state ('' or '-')
		state = State.SIGN;
	}
	
	public boolean numberIsFinished(){
		return state == State.AFTER_FIRST || state == State.DIGIT || 
				state == State.FRAC || state == State.EXP_DIGIT;
	}
		
	/**
	 * Push a byte into a number-reading state machine. <br>
	 * <br>
	 * IMPORTANT: the byte should belong to this number, i.e. it cannot be a
	 * whitespace, comma, or a bracket.
	 * 
	 * @return
	 * @throws IllegalFormatException
	 */
	public void push(byte b) throws IllegalFormatException {
		if (b == '.') {
			if (state == State.AFTER_FIRST || state == State.DIGIT) {
				state = State.FRAC_FIRST;
				return;
			} else {
				throw new IllegalFormatException(
						"Got fraction sign ('.') not after int part of a number (state = " + state + ") ");
			}
		}
		if (b == 'E' || b == 'e') {
			if (state == State.AFTER_FIRST || state == State.DIGIT || state == State.FRAC) {
				state = State.EXP_SIGN;
				return;
			} else {
				throw new IllegalFormatException(
						"Got exp sign ('e' or 'E') not after INT or FRAC part of a number (state = " + state + ")");
			}
		}
		if (state == State.SIGN) {
			state = State.FIRST_DIDIT;
			if (b != '-') {
				push(b);
			}
		} else if (state == State.FIRST_DIDIT) {
			if (b == '0') {
				state = State.AFTER_FIRST;
			} else if (b > 48 && b < 58) {
				state = State.DIGIT;
			} else {
				throw new IllegalFormatException(
						"Unexpected byte in the beginning of a number: " + b + " (expected a digit)");
			}
		} else if (state == State.DIGIT) {
			if (isNotADigit(b)) {
				throw new IllegalFormatException(
						"Unexpected byte in the INT part of a number: " + b + " (expected a digit)");
			}
			// we could also get 'e', 'E' or '.' in DIGIT state, but we should
			// have entered earlier if statement than
		} else if (state == State.FRAC_FIRST) {
			if (isNotADigit(b)) {
				throw new IllegalFormatException(
						"Unexpected byte in the FRAC part of a number: " + b + " (expected a digit)");
			}
			state = State.FRAC;
		} else if (state == State.FRAC) {
			if (isNotADigit(b)) {
				throw new IllegalFormatException(
						"Unexpected byte in the FRAC part of a number: " + b + " (expected a digit)");
			}
		} else if (state == State.EXP_SIGN) {
			state = State.EXP_DIGIT_FIRST;
			if (b != '+' && b != '-') {
				push(b);
			}
		} else if (state == State.EXP_DIGIT_FIRST) {
			if (isNotADigit(b)) {
				throw new IllegalFormatException(
						"Unexpected byte in the EXP part of a number: " + b + " (expected a digit)");
			}
			state = State.EXP_DIGIT;
		} else if (state == State.EXP_DIGIT) {
			if (isNotADigit(b)) {
				throw new IllegalFormatException(
						"Unexpected byte in the EXP part of a number: " + b + " (expected a digit)");
			}
		} else {
			// we cannot be in AFTER_FIRST_ZERO state legally here because the
			// only four ways out of there are 'E', 'e', '.' and end of number
			// (see if statements in the beginning)
			throw new RuntimeException("Unknwon state value: " + state.toString());
		}
	}

	private boolean isNotADigit(byte b) {
		return b > 57 || b < 48;
	}
}
