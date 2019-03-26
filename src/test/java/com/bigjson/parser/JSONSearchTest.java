package com.bigjson.parser;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import org.junit.Assert;
import org.junit.Test;
import static org.junit.Assert.*;

public class JSONSearchTest {
//{"number" : -12E-1, "keywords": {"true" : true, "false": false, "null":null}, "string keywords": ["true", "null", "false"] }	
	@Test
	public void shouldSearchInASCIIFile() throws IOException, IllegalFormatException{
		String json = "{\"number\" : -12E-1, \"keywords\": {\"true\" : true, \"false\": false, "
				+ "\"null\":null}, \"string keywords\": [\"true\", \"null\", \"false\"] }";
		try (JSONSearch search = createSearch(json)) {
			searchAndCompare(search, "true", json, false, false);
			searchAndCompare(search, "TRUE", json, false, false);
			searchAndCompare(search, "tru", json, true, false);
			searchAndCompare(search, "ru", json, true, false);
			searchAndCompare(search, "u", json, true, false);
			searchAndCompare(search, "U", json, false, false);
			searchAndCompare(search, "-12E-1", json, true, false);
			searchAndCompare(search, "-12e-1", json, false, false);
			searchAndCompare(search, "-12e-1", json, true, false);
			searchAndCompare(search, "E-1", json, true, false);
			searchAndCompare(search, "keY", json, true, false);
			searchAndCompare(search, "-1", json, true, false);
			searchAndCompare(search, "12", json, true, false);
			searchAndCompare(search, "e", json, true, false);
			
			shouldNotFind(search, "false", 1, 30, true, false);
			shouldNotFind(search, " -1", 0, json.length(), true, false);
			shouldNotFind(search, "\"false\"", 0, json.length(), true, false);
			shouldNotFind(search, "se\"", 0, json.length(), true, false);
			shouldNotFind(search, ":", 1, 30, true, false);
			shouldNotFind(search, "13", 0, json.length(), true, false);
			shouldNotFind(search, "\"", 0, json.length(), true, false);
			shouldNotFind(search, ",", 0, json.length(), true, false);
			shouldNotFind(search, "{", 0, json.length(), true, false);
			shouldNotFind(search, "]", 0, json.length(), true, false);
			shouldNotFind(search, "False", 0, json.length(), true, false);	
			shouldNotFind(search, "E-1, \"key", 0, json.length(), true, false);	
			shouldNotFind(search, "E-1key", 0, json.length(), true, false);	

			shouldFindAtPos(search, " ", 85, 0, json.length(), true, false);
			shouldFindAtPos(search, " ", 85, 0, json.length(), false, false);
			
			
		}
		json = "{\"assembly_ref\":\"15754/23/1\"}";
		try (JSONSearch search = createSearch(json)) {
			searchAndCompare(search, "1", json, false, false);
		}
	}
	
	@Test
	public void shouldSearchEscapeSequences() throws IOException, IllegalFormatException{
		String json = "[\"abc\\n\", \"\\r','\\t','\\f','\\b'\" ,\"abc \", \"'\",\"投, ネ\",\"\\\\', \\\\\uD83D\uDE00\", \"\\u007f\\u0080\"]";
		try (JSONSearch search = createSearch(json)) {
			searchAndCompare(search, "\\'", "\\\\'", json, true, true);
			searchAndCompare(search, "abc\n", "abc\\n", json, true, true);
			searchAndCompare(search, "ABC\n", "abc\\n", json, false, true);
			searchAndCompare(search, "\n", "\\n", json, true, false);
			searchAndCompare(search, "\r", "\\r", json, true, true);
			searchAndCompare(search, "\t", "\\t", json, false, false);
			searchAndCompare(search, "\f", "\\f", json, false, true);
			searchAndCompare(search, "\b", "\\b", json, true, true);
			searchAndCompare(search, "\\", "\\\\", json, true, true);
			searchAndCompare(search, "\\'", "\\\\'", json, true, true);

			shouldNotFind(search, "\\", 0, 20, true, true);
			shouldNotFind(search, "n", 0, json.length(), true, true);
			shouldNotFind(search, "uD83D", 0, json.length(), true, true);
			shouldNotFind(search, "D", 0, json.length(), true, true);
			shouldNotFind(search, "007f", 0, json.length(), true, true);
			shouldNotFind(search, "0", 0, json.length(), true, true);
		}
		json = "[\"\\\\\", \"\\\\\"]";
		try (JSONSearch search = createSearch(json)) {	
			searchAndCompare(search, "\\", "\\\\", json, true, true);
		}
	}
	
	@Test
	public void shouldSearchInNonASCIIFile() throws IOException, IllegalFormatException{
		String json = "[\"abc\\n\", \"\\r','\\t','\\f','\\b'\" ,\"abc \", \"'\",\"投, ネ\",\"\\\\', \\\\\uD83D\uDE00\", \"\\u007e\\u0080\"]";
		JSONSearch search = createSearch(json);	
		searchAndCompare(search, "a", json, true, true);
		searchAndCompare(search, "abc", json, true, true);
		searchAndCompare(search, "c", json, true, true);
		searchAndCompare(search, "投", json, true, true);
		searchAndCompare(search, "ネ", json, true, true);
		searchAndCompare(search, "投, ネ", json, true, true);
		searchAndCompare(search, "~\u0080", "\\u007f\\u0080", json, false, true);
		searchAndCompare(search, "\u0080", "\\u0080", json, false, true);
			
		shouldNotFind(search, "\u007f\u0080", 0, json.length(), false, true);
		shouldNotFind(search, "\u0080", 0, json.length(), false, false);
		shouldNotFind(search, "\\u0080", 0, json.length(), false, true);
		shouldNotFind(search, "\\u007f", 0, json.length(), false, true);
		
	}
	private void shouldNotFind(JSONSearch search, String toSearch, int from, int to, boolean caseSensitive,
			boolean searchForAltUnicode) throws IllegalFormatException, IOException {
		StringSearchInfo searchInfo = search.createNewSearch(toSearch, from, to, caseSensitive,
				searchForAltUnicode);
		search.findNextMatch(searchInfo);
		assertTrue(searchInfo.isFinished());
	}

	private void shouldFindAtPos(JSONSearch search, String toSearch, int matchPos, int from, int to,
			boolean caseSensitive, boolean searchForAltUnicode) throws IllegalFormatException, IOException {
		StringSearchInfo searchInfo = search.createNewSearch(toSearch, from, to, caseSensitive,
				searchForAltUnicode);
		search.findNextMatch(searchInfo);
		assertEquals(matchPos, searchInfo.getLastMatchPos());
		search.findNextMatch(searchInfo);
		assertTrue(searchInfo.isFinished());
	}

	private void searchAndCompare(JSONSearch search, String toSearch, String json, boolean caseSensitive,
			boolean searchForAltUnicode) throws IllegalFormatException, IOException {
		searchAndCompare(search, toSearch, toSearch, json, caseSensitive, searchForAltUnicode);
	}
	private void searchAndCompare(JSONSearch search, String toSearchUser, String toSearch, String json, boolean caseSensitive,
			boolean searchForAltUnicode) throws IllegalFormatException, IOException {
		StringSearchInfo searchInfo = search.createNewSearch(toSearchUser, 0, json.getBytes(search.getCharset()).length, caseSensitive,
				searchForAltUnicode);
		int prevMatchPos = -1;
		if(!caseSensitive){
			toSearch = toSearch.toLowerCase();
		}
		while (true) {
			search.findNextMatch(searchInfo);
			prevMatchPos = json.indexOf(toSearch, prevMatchPos + 1);
			if (searchInfo.isFinished()) {
				assertTrue("Search should not be finished, expected prevMatchPos = " + prevMatchPos, prevMatchPos < 0);
				break;
			}
			if(prevMatchPos < 0){
				Assert.fail("Search should be finished");
			}
			// prevMatchPos this is in chars but I need in bytes
			int byteMatchPos = json.substring(0, prevMatchPos).getBytes(searchInfo.getCharset()).length;
			assertEquals(byteMatchPos, searchInfo.getLastMatchPos());
		}
	}

	private JSONSearch createSearch(String json) throws IOException{
		File f = TestUtils.getGeneratedTestFile();
		try(PrintWriter pw = new PrintWriter(new FileWriter(f))){
			pw.write(json);
		}
		return new JSONSearch(f);
	}

}
