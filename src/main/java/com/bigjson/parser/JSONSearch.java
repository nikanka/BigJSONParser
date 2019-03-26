package com.bigjson.parser;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

public class JSONSearch implements Closeable{
	
	private enum SearchableTokenType{
		STRING, NONSTRING
	}
	private UTF8FileReader reader;
	private byte curByte;
	private SearchableTokenType curToken = null;
	/**
	 * String state machine for the head of the scanning window(s), it contains
	 * state for the next (to-be-read) byte.
	 */
	private StringReadingStateMachine headStateMachine;
	
	public JSONSearch(File file) throws IOException{
		this(new UTF8FileReader(file));
	}
	
	public JSONSearch(UTF8FileReader reader) throws IOException{
		this.reader = reader;
		headStateMachine = new StringReadingStateMachine(StringReadingStateMachine.MODE_CHECK_ASCII);
	}
	
	@Override
	public void close() throws IOException {
		reader.close();
	}
	
	public UTF8FileReader getReader(){
		return reader;
	}
	
	public Charset getCharset(){
		return reader.getCharset();
	}
	/**
	 * 
	 * @param searchInfo
	 * @return true is the string to search can fit into the remaining search range
	 * @throws IOException
	 * @throws IllegalArgumentException
	 *             if the search is already finished
	 *             (searchInfo.searchIsFinished() is true)

	 */
	private void initSearch(StringSearchInfo searchInfo) throws IOException{
		if(searchInfo.isFinished()){
			throw new IllegalArgumentException("The search is already finished");
		}
		if(!searchInfo.getCharset().equals(reader.getCharset())){
			
		}
		reader.getToPosition(searchInfo.getCurSearchStartPos());
		if(searchInfo.getLastMatchPos() < 0){
			// if it is the first search it should not be inside of a searchable token
			curToken = null;
		} else {
			curToken = searchInfo.isLastMatchWitninString() ? SearchableTokenType.STRING
					: SearchableTokenType.NONSTRING;
		}
	}
	
	public StringSearchInfo createNewSearch(String stringToSearch, long searchStartPos, long searchEndPos,
			boolean caseSensitive, boolean searchForAltUnicode) {
		return StringSearchInfo.createNewSearch(stringToSearch, searchStartPos, searchEndPos, caseSensitive,
				searchForAltUnicode, reader.getCharset());
	}
	
	/**
	 * Search for the next match for a given string and search range (are specified in the
	 * <code>searchInfo</code>). The search starts from the position just after the previous match
	 * (or from the beginning of the search range if this is the first search).<br>
	 * Update the <code>searchInfo</code> object with a new match or no match.
	 * @return true if the next match was found, false otherwise
	 * @param searchInfo
	 * @throws IOException
	 * @throws IllegalFormatException
	 * @throws IllegalArgumentException
	 *             if the search is already finished
	 *             (searchInfo.searchIsFinished() is true)
	 */
	public boolean findNextMatch(StringSearchInfo searchInfo) throws IOException, IllegalFormatException{
		initSearch(searchInfo);
		// go along file positions keeping track of the state (within or out of a string)
		// and recalculate the sum(s)
		List<BytePatternMatcher> patterns = searchInfo.getPatterns();
		boolean prevStateIsReadable = false;
		boolean inReadableState;
		while(reader.hasNext() && reader.getFilePosition() < searchInfo.getSearchEndPos()){
			try{
				inReadableState = moveToNextByte();
				if(inReadableState){ 
					// if we are in the search-able token, update the match results
					for(BytePatternMatcher pattern: patterns){
						if(pattern.addNewByteAndCompare(curByte, curToken == SearchableTokenType.STRING)){
							// found a match!
							resetPatterns(patterns, false);
							searchInfo.addNewSearchResult(reader.getFilePosition() - pattern.length(), pattern.length(),
									curToken == SearchableTokenType.STRING);
//							System.out.println("Found at pos "+searchInfo.getLastMatchPos());
							// check if the shortest pattern fit into the remaining searching range
							if (patterns.get(patterns.size() - 1).length() > searchInfo.getSearchEndPos()
									-  searchInfo.getCurSearchStartPos()) {
								searchInfo.isFinished();
							}
							return true;
						}
					}
				} else if(prevStateIsReadable){
					resetPatterns(patterns, true);
				}
			}catch(IllegalFormatException e){
				throw new IllegalFormatException(e.getMessage() + " at pos " + reader.getFilePosition() + " of file "
						+ reader.getFile().getName());
			}
			prevStateIsReadable = inReadableState;
		}		
		// we reached up to the end of the search range and found nothing
		resetPatterns(patterns, true);
		searchInfo.addNewSearchResult(-1, -1, searchInfo.isLastMatchWitninString());
		return false;
	}
	
	private static void resetPatterns(List<BytePatternMatcher> patterns, boolean endOfToken){
		for(BytePatternMatcher pattern: patterns){
			pattern.reset(endOfToken);
		}
	}
	/**
	 * Move to the next byte and update the current state (if we are within a searcable token 
	 * and if it is a string). If we exit the searchable token, reset the pattern matches.
	 * @return true if returned byte is within searchable token
	 * @throws IllegalFormatException
	 * @throws IOException
	 */
	private boolean moveToNextByte() throws IllegalFormatException, IOException{
		curByte = reader.getNextByte();
		if(curToken == null){
			if(curByte == '"'){ // start a string
				headStateMachine.reset();
				curToken = SearchableTokenType.STRING;
				// the string is started but the current byte is not part of it
				return false;
			} 
			if(isNonStringTokenStart(curByte)){
				curToken = SearchableTokenType.NONSTRING;
				// the token is started and the current byte is its first byte 
				return true;
			}
			return false;
		}
		if(curToken == SearchableTokenType.STRING){
			if(curByte == '"' && !headStateMachine.isInEscapedSequence()){
				// string is over
				curToken = null;
//				resetPatternMatchs();
				return false;
			}
			headStateMachine.pushByte(curByte);
			return true;
		}
		if(curToken == SearchableTokenType.NONSTRING){
			if(isTokenEnd(curByte)){
				curToken = null;
//				resetPatternMatchs();
				return false;
			}
			return true;
		}
		throw new RuntimeException("Should not have got here");
	}
	
	private boolean isTokenEnd(byte b){
		return isWhitespace(b) || b == ',' || b == '}' || b == ']';
	}
	private boolean isWhitespace(byte b) {
		return b == 9|| b == 10 || b == 13 || b == 32;
	}
	/**
	 * Check if the given byte is a first byte of a true, false, null or a number token.
	 * @param b
	 * @return
	 */
	private boolean isNonStringTokenStart(byte b){
		return b == 't' || b == 'f' || b == 'n' || b == '-' || (b >= 48 && b <= 57); 
	}

}
