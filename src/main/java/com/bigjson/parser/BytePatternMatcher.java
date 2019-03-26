package com.bigjson.parser;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

class BytePatternMatcher {
	private static final boolean DEBUG = false;
	private byte[] targetBytes;
	private int targetHash = 0;
	// TODO: use a different class for singly linked list? The own one?
	private LinkedList<Byte> curBytes;
	private int curHash = 0;
	private int base = 256;
	private int modulus = 101;
	private int leftBaseOffset = 1;
	private boolean caseSensitive;
	/**
	 * String reading state machine for the tail of the scanning window, it
	 * contains the state of the first (left-most byte) of the window
	 */
	private StringReadingStateMachine startStateMachine;
	// private boolean startIsInString;

	public static void main(String[] args) throws IOException {
		try (JSONSearch search = new JSONSearch(new File("testFiles", "test.txt"))) {
			String str = "abc'\n','\r','\t','\f','\b','\"','	',投,ネ,'\\', \\\\\uD83D\uDE00\u007f\u0080";
			// \\u6295, \\u30cd
			String newStr = new String(
					createPatternsForString(str, StandardCharsets.UTF_8, false, true).get(1).getTargetBytes(),
					StandardCharsets.UTF_8);
			System.out.println(str + " -> " + newStr);
			for (int i = 0; i < newStr.length(); i++) {
				System.out.println(i + " : \\u" + Integer.toHexString(Character.codePointAt(newStr, i)) + ", "
						+ Integer.toHexString(newStr.charAt(i)));
			}
			System.out.println(Arrays.toString(str.getBytes(StandardCharsets.UTF_8)));
		}
	}

	BytePatternMatcher(byte[] bytesToSearch, boolean caseSensitive) {
		this.caseSensitive = caseSensitive;
		this.targetBytes = bytesToSearch;
		if(DEBUG){
			System.out.print("Pattern: ");
			for(byte b: targetBytes){
				System.out.print((char)b);
			}
			System.out.println();
		}
		
		if (!caseSensitive) {
			for (int i = 0; i < targetBytes.length; i++) {
				targetBytes[i] = toLowerCase(targetBytes[i]);
			}
		}
		for (byte b : targetBytes) {
			targetHash = addByteToHash(targetHash, b);
		}
		calculateOffset();
		this.curBytes = new LinkedList<Byte>();
		this.startStateMachine = new StringReadingStateMachine(StringReadingStateMachine.MODE_CHECK_ASCII);
		// startIsInString = false;
	}

	private void calculateOffset() {
		leftBaseOffset = 1;
		// [base^(length-1)]%modulus
		for (int i = 1; i < targetBytes.length; i++) {
			// do it in a loop like this to avoid integer overflow
			leftBaseOffset = (leftBaseOffset * base) % modulus;
		}
	}

	/**
	 * The length of the pattern
	 * 
	 * @return
	 */
	public int length() {
		return targetBytes.length;
	}

	byte[] getTargetBytes() {
		return targetBytes;
	}

	/**
	 * 
	 * @param b
	 * @return true if after adding the byte the substring matches the pattern
	 */
	boolean addNewByteAndCompare(byte b, boolean isInString) throws IllegalFormatException {
		if (curBytes.size() < targetBytes.length) {
			// the substring is too short -> append a new byte
			curHash = addByteToHash(curHash, b);
			addByteToCurBytes(b);
			// if substring is still too short enough -> no match here
			if (curBytes.size() < targetBytes.length) {
				return false;
			}
		} else {
			// substring is of pattern length -> need to shift/roll it
			rollHash(b, isInString);
		}
		// now compare the pattern and the new substring
		if (curHash == targetHash && !startStateMachine.isInEscapedSequence()) {
			return compareBytes();
		}
		return false;
	}

	private void addByteToCurBytes(byte b) {
		if (!caseSensitive) {
			b = toLowerCase(b);
		}
		curBytes.add(b);
	}

	private boolean compareBytes() {
		int ind = 0;
		for (Byte observed : curBytes) {
			if (observed != targetBytes[ind]) {
				return false;
			}
			ind++;
		}
		return true;
	}

	/**
	 * Clear the observed bytes and hash value. If endOfToken is true, also
	 * reset the start state.<br>
	 * If the reset is called because a match was found, the start state is not
	 * reset in order to start the next search properly (makes sense if we are
	 * in a string/name token)
	 */
	void reset(boolean endOfTocken) {
		curBytes.clear();
		curHash = 0;
		if (endOfTocken) {
			startStateMachine.reset();
		}
	}

	/**
	 * Update curHash and curBytes with newByte
	 * 
	 * @param newByte
	 */
	private void rollHash(byte newByte, boolean isInString) throws IllegalFormatException {
		// remove the first byte and update the start state
		byte passedByte = curBytes.removeFirst();
		// if(passedByte == '"' && !startIsInString){
		// startIsInString = true;
		// }
		// if(startIsInString){
		// if(passedByte == '"' && !startStateMachine.isInEscapedSequence()){
		// startIsInString = false;
		// } else {
		// isInString shows is the head (newly observed) byte is in string
		// but as the pattern cannot span across the tokens, if the head is in
		// string
		// then the tail (last byte) should be in the same string
		if (isInString) {
			startStateMachine.pushByte(passedByte);
		}
		if(DEBUG){
			System.out.println("Roll (len=" + length() + "): " + (char) passedByte + " >> " + (char) newByte + "(inStr = "
				+ isInString + ", escaped = " + startStateMachine.isInEscapedSequence() + ")");
		}
		// }
		// }
		// subtract the number for the first byte in previous window
		curHash = curHash + modulus - (byteToInt(passedByte) * leftBaseOffset) % modulus;
		// add new byte
		curHash = addByteToHash(curHash, newByte);
		// curBytes.add(newByte);
		addByteToCurBytes(newByte);
	}

	private int addByteToHash(int hash, byte b) {
		return (hash * base + byteToInt(b)) % modulus;
	}

	private static int byteToInt(byte b) {
		return b & 0xFF;
	}

	private static byte toLowerCase(byte b) {
		if (b > 90 || b < 65) {
			return b;
		}
		return (byte) (b | 0b100000);
	}

	/**
	 * Convert given string into one or two arrays of bytes using given
	 * <code>charset</code>. Second array will be created if
	 * <code>searchForAltUnicode</code> is true an the string contains non-ASCII
	 * chars. In this case non-ASCII chars are substituted by one or two (if
	 * symbol is not in BMP) \\uhhhh sequences.<br>
	 * All control characters as well as quote and backslash symbols are
	 * substituted by a special combo (\", \\, \n, \b, \f, \t, \r) or a
	 * corresponding \\uhhhh sequence. <br>
	 * The list is reverse-sorted by array length, i.e. if there are two arrays
	 * the one with the non-ASCII symbols substituted by \\uhhhh will be the
	 * first one.
	 * 
	 * @param str
	 * @return
	 */
	public static List<BytePatternMatcher> createPatternsForString(String str, Charset charset, boolean caseSensitive,
			boolean searchForAltUnicode) {
		// TODO: create several arrays for strings with backslashes
		// and one array for strings without backslashes
		List<BytePatternMatcher> ret = new ArrayList<BytePatternMatcher>(2);
		// check if some bytes are control, a backslash or a quote:
		// convert them to backslash combo
		// or \\uhhhh (for those that do not have a special backslash combo)
		byte[] bytes = processControllChars(str.getBytes(charset));
		if (searchForAltUnicode) {
			byte[] uBytes = substituteNonASCIByHex(bytes, charset);
			if (uBytes != null) {
				ret.add(new BytePatternMatcher(uBytes, caseSensitive));
			}
		}
		ret.add(new BytePatternMatcher(bytes, caseSensitive));
		return ret;
	}

	private static byte[] substituteNonASCIByHex(byte[] initBytes, Charset charset) {
		String str = new String(initBytes, charset);
		/*
		 * it is easier to do it via String because String in java are encoded
		 * in UTF-16, so if code point of a symbol <= 0xFFFF (BMP) it is coded
		 * with one char and correspond to one \\uhhhh combo, but if it is
		 * larger it is coded with two chars, each corresponding to one \\uhhhh
		 * combo, as JSON format suggest to do.
		 */
		StringBuffer sb = new StringBuffer(str.length() * 3);
		int nonAsciNum = 0;
		for (int i = 0; i < str.length(); i++) {
			char ch = str.charAt(i);
			if (ch > 0x7F) { 
				nonAsciNum++;
				String chHex = Integer.toHexString(ch);
				sb.append("\\u");
				if (chHex.length() < 4) {
					sb.append('0');
				}
				if (chHex.length() < 3) {
					sb.append('0');
				}
				// chHex cannot be shorter than 2 because ch > 0x7f
				sb.append(chHex);
			} else {
				sb.append(ch);
			}
		}
		if (nonAsciNum == 0) {
			return null;
		}
		return sb.toString().getBytes(charset);
	}

	private static byte[] processControllChars(byte[] initBytes) {
		int addForControlChars = 0;
		for (byte b : initBytes) {
			if (b > 7 && b < 11 || // backspace (8), horizontal tab (9), newline
									// (10)
					b == 12 || b == 13 || // formfeed (12), carriage return (13)
					b == '"' || b == '\\') {
				addForControlChars++; // substitute one byte by two
			} else if (b >= 0 && b < 32) {
				System.out.println("met byte " + b + "  -> need to sub with unicode");
				addForControlChars += 5; // we substitute one byte with six
			}
		}
		byte[] bytes;
		if (addForControlChars == 0) {
			return initBytes;
		}
		bytes = new byte[initBytes.length + addForControlChars];
		for (int i = 0, j = 0; i < initBytes.length; i++) {
			if (initBytes[i] == '"' || initBytes[i] == '\\' || initBytes[i] >= 0 && initBytes[i] < 32) {
				j += insertEscapeCombo(initBytes[i], bytes, j);
			} else {
				bytes[j] = initBytes[i];
				j++;
			}
		}

		return bytes;
	}

	private static int insertEscapeCombo(byte b, byte[] target, int pos) {
		if (b == '"') {
			target[pos + 1] = '"';
		} else if (b == '\\') {
			target[pos + 1] = '\\';
		} else if (b == 8) {
			target[pos + 1] = 'b';
		} else if (b == 9) {
			target[pos + 1] = 't';
		} else if (b == 10) {
			target[pos + 1] = 'n';
		} else if (b == 12) {
			target[pos + 1] = 'f';
		} else if (b == 13) {
			target[pos + 1] = 'r';
		} else if (b >= 0 && b < 32) {
			target[pos] = '\\';
			target[pos + 1] = 'u';
			target[pos + 2] = '0';
			target[pos + 3] = '0';
			target[pos + 4] = (byte) (b < 16 ? '0' : '1');
			target[pos + 5] = (byte) (b % 16 < 10 ? b + 48 : b + 87);
			return 6;
		} else {
			throw new IllegalArgumentException(
					"Byte '" + b + "' is not in control characters range [0, 32) and not a quote or a backslash");
		}
		target[pos] = '\\';
		return 2;
	}
}