package com.bigjson.parser;

import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.List;

public class StringSearchInfo {
	
	private static final int CACHED_MATCHES_MAX_NUM = 100;

	private String stringToSearch;
	/**
	 * inclusive
	 */
	private long searchStartPos;
	/**
	 * exclusive
	 */
	private long searchEndPos;
	
	private boolean isLastMatchWitninString = false;
	
	private LinkedList<Long> matchPositions;
	
	private boolean searchIsFinished = false;
	
	private boolean caseSensitive;
	private boolean searchForAltUnicode;
	private Charset charset;
	private List<BytePatternMatcher> patterns;

	private long nextSearchShift = 1;
	

	private StringSearchInfo(String stringToSearch, long searchStartPos, long searchEndPos, boolean caseSensitive,
			boolean searchForAltUnicode, Charset charset) {
		if(stringToSearch.length() == 0){
			throw new IllegalArgumentException("Cannot search for an empty string");
		}
		if(searchEndPos <= searchStartPos){
			throw new IllegalArgumentException("Search end pos should be bigger than search start pos (got ["
					+ searchStartPos + ", " + searchEndPos + "))");
		}
		this.stringToSearch = stringToSearch;
		this.searchStartPos = searchStartPos;
		this.searchEndPos = searchEndPos;
		this.matchPositions = new LinkedList<Long>();
		this.caseSensitive = caseSensitive;
		this.searchForAltUnicode = searchForAltUnicode;
		this.charset = charset;
		this.patterns = BytePatternMatcher.createPatternsForString(stringToSearch, charset, caseSensitive, searchForAltUnicode);
		/*
		 * Check if the first byte of the first pattern if a backslash.
		 * If so, the next byte is an escaped one and has no meaning by itself, 
		 * so we can skip it. This will help with the situations like this:
		 * user searches for a backslash, in file it looks like two backslashes. After
		 * first find the searcher starts to search starting from the second backslash (if
		 * start searching form the next byte) and thinks this is the first backslash, so 
		 * bytes that are not allowed after the backslash may cause an exception or a 
		 * closing quote may be taken for an escaped quote
		 */
		// TODO: maybe just not use string state machine?
		if(patterns.get(0).getTargetBytes()[0] == '\\'){
			nextSearchShift = 2;
		}
	}
	
	/**
	 * IMPORTANT: the start position of a search should not be inside of a string, a keyword
	 * (null, false, true) or a number.
	 * @param stringToSearch
	 * @param searchStartPos
	 * @param searchEndPos
	 * @param caseSensitive
	 * @param searchForAltUnicode
	 * 
	 * @return
	 * @throws IllegalArgumentException
	 *             if the string to search is empty or if search end pos is not
	 *             bigger than search start pos
	 */
	public static StringSearchInfo createNewSearch(String stringToSearch, long searchStartPos, long searchEndPos,
			boolean caseSensitive, boolean searchForAltUnicode, Charset charset) {
		return new StringSearchInfo(stringToSearch, searchStartPos, searchEndPos, caseSensitive, searchForAltUnicode, charset);
	}

	public String getStringToSearch() {
		return stringToSearch;
	}

	public boolean isCaseSensitive(){
		return caseSensitive;
	}
	
	public boolean searchForAltUnicode(){
		return searchForAltUnicode;
	}
	
	public Charset getCharset(){
		return charset;
	}
	
	public long getSearchStartPos() {
		return searchStartPos;
	}

	/**
	 * Exclusive
	 * @return
	 */
	public long getSearchEndPos() {
		return searchEndPos;
	}

	public boolean isLastMatchWitninString() {
		return isLastMatchWitninString;
	}

	public List<Long> getMatchPositions() {
		return matchPositions;
	}
	
	public long getLastMatchPos(){
		if(matchPositions.size() == 0){
			return -1;
		}
		return matchPositions.get(matchPositions.size() - 1);
	}
	
	public long getCurSearchStartPos(){
		return Math.max(searchStartPos, getLastMatchPos() + nextSearchShift );
	}
	
	/**
	 * Is this search is finished, i.e. last search failed to find a match.
	 * 
	 * @return true if no more matches can be found within the specified search
	 *         range
	 */
	public boolean searchIsFinished() {
		return searchIsFinished;
	}
	
	public static int getCachedMatchesMaxNum() {
		return CACHED_MATCHES_MAX_NUM;
	}
	
	/**
	 * Add a new search result, witch is either a new match or no match
	 * (indicated by a negative match position).
	 * 
	 * @param matchPos
	 *            a position of a new match or -1 if no match can be found
	 * @param matchLength
	 *            the length of the new match (can differ from length of the
	 *            <code>stringToMatch</code>)
	 * @param isWithinString
	 *            true if the start position of the new match is within a
	 *            string, false otherwise
	 * @throws IllegalArgumentException
	 *             if match position is outside of the search range, which
	 *             starts from the position following previous match position
	 *             (or from <code>searchStartPos</code> if it is the first
	 *             match) and ends at <code>searchEndPos - matchLength</code>
	 */
	public void addNewSearchResult(long matchPos, int matchLength, boolean isWithinString){
		if(matchPos < 0){
			searchIsFinished = true;
			return;
		}
		if (matchPos < getCurSearchStartPos() || matchPos > searchEndPos - matchLength) {
			throw new IllegalArgumentException("The start position of the match is out of search range: got " + matchPos
					+ ", should be within [" + getCurSearchStartPos() + ", " + getSearchEndPos() + ")");
		}
		matchPositions.add(matchPos);
		if(matchPositions.size() > CACHED_MATCHES_MAX_NUM){
			matchPositions.removeFirst();
		}
		isLastMatchWitninString = isWithinString;
	}

	public List<BytePatternMatcher> getPatterns() {
		return patterns;
	}
}
