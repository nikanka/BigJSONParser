package com.bigjson.parser;

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
	

	private StringSearchInfo(String stringToSearch, long searchStartPos, long searchEndPos){
		this.stringToSearch = stringToSearch;
		this.searchStartPos = searchStartPos;
		this.searchEndPos = searchEndPos;
		this.matchPositions = new LinkedList<Long>();
	}
	
	public static StringSearchInfo createNewSearch(String stringToSearch, long searchStartPos, long searchEndPos){
		return new StringSearchInfo(stringToSearch, searchStartPos, searchEndPos);
	}

	public String getStringToSearch() {
		return stringToSearch;
	}

	public long getSearchStartPos() {
		return searchStartPos;
	}

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
		return Math.max(searchStartPos, getLastMatchPos() + 1);
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
}
