package com.bigjson.parser;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.List;

import org.json.JSONException;

/**
 * This class serves as an interface for the library, allowing to initialize 
 * parser, load the root of the JSON tree and children for a given node on demand. 
 * It also allows to load full String for a given String node (at a given
 * position in file). 
 * @author nikanka
 *
 */
public class JSONLoader implements Closeable {
	private LazyJSONParser parser; 
	private JSONSearch search;
	
	public JSONLoader(File file, int stringDisplayLimit) throws IOException, IllegalFormatException{
		this(file, null, stringDisplayLimit);
	}
	
	/**
	 * Creates a loader for a given JSON file.
	 * 
	 * @param file
	 * @param topLevelName
	 *            what name should be given (for display purposes) to the
	 *            top-level node. If topLevelName == null, the default name
	 *            ("JSON") will be used 
	 * @param stringDisplayLimit
	 *            how many first characters of a string to view. If
	 *            stringDisplayLimit < 0 full strings are loaded

	 * @throws IOException
	 *             if file is not found or empty or if an I/O error occurs while
	 *             reading bytes from the file
	 * @throws IllegalFormatException
	 *             if the format of the data is not as expected
	 */
	public JSONLoader(File file, String topLevelName, int stringDisplayLimit) throws IOException, IllegalFormatException{
		parser = new LazyJSONParser(file, topLevelName, stringDisplayLimit);
		search = new JSONSearch(parser.getReader());
//		long t1 = System.currentTimeMillis();
//		root = parser.getRoot();
//		System.out.println("LoadTime for the root: "+(System.currentTimeMillis() - t1)/1000 + " s");
	}
	@Override
	public void close() throws IOException {
		parser.close();
	}
	
	public JSONNode getRoot() throws IOException, IllegalFormatException{
		return parser.getRoot(false);
	}
	
	public JSONNode getRootAndValidate() throws IOException, IllegalFormatException{
		return parser.getRoot(true);
	}
	
	public File getFile(){
		return parser.getFile();
	}
	
	/**
	 * Search for the next occurrence of the string specified in
	 * <code>searchInfo</code> within the search range (also specified in
	 * <code>searchInfo</code>), if the search is not finished yet (
	 * <code>searchInfo.searchIsFinished() == false</code>). <br>
	 * Save new search result in the <code>searchInfo</code> object.<br><br>
	 * See more info in the doc for {@link #createNewSearch(String, long, long, boolean, boolean) startNewSearch}
	 * 
	 * @param searchInfo
	 *            object containing information about search of specified string
	 *            within the specified region of the file, including all
	 *            previous matches and the current match (if it was found). To
	 *            check whether a new match was found use
	 *            <code>searchInfo.searchIsFinished()</code>.
	 * @throws IOException
 	 * @throws IllegalArgumentException
	 *             if the search is already finished
	 *             (searchInfo.searchIsFinished() is true)
	 */
	public void findNextMatch(StringSearchInfo searchInfo) throws IOException, IllegalFormatException{
		search.findNextMatch(searchInfo);
	}
	
	/**
	 * Create a new search (but does not search yet) of <code>stringToSearch</code> in leaf nodes (string,
	 * number, null, false and true tokens) and names of objects within range
	 * <code>[searchStartPos, searchEndPos)</code> of a file. The match should
	 * fit completely in this range and be completely within a token.<br>
	 * <br>
	 * IMPORTANT: the start position of a search (<code>searchStartPos</code>)
	 * should be outside of a searchable token (i.e. outside of object names and
	 * string, number, null, false and true tokens).
	 * 
	 * @param stringToSearch
	 *            string to be found
	 * @param searchStartPos
	 *            inclusive start position of the search (match position should
	 *            be not less than <code>searchStartPos</code>)
	 * @param searchEndPos
	 *            exclusive end position of the search (position of the last
	 *            byte of a match should be smaller than
	 *            <code>searchEndPos</code>)
	 * @param caseSensitive
	 *            if the search should be case-sensitive
	 * @param searchForAltUnicode
	 *            if true also search for alternative string with non-ASCII
	 *            symbols substituted by one or two \\uhhhh sequences (where h is
	 *            a hex digit).
	 * @return an object containing information about the search including the
	 *         first match (if any). To check whether a match was found use
	 *         <code>searchInfo.searchIsFinished()</code>.
	 */
	// TODO: add some type of ID to connect search with the reader/file 
	public StringSearchInfo createNewSearch(String stringToSearch, long searchStartPos, long searchEndPos,
			boolean caseSensitive, boolean searchForAltUnicode) {
		StringSearchInfo searchInfo = search.createNewSearch(stringToSearch, searchStartPos, searchEndPos,
				caseSensitive, searchForAltUnicode);
		return searchInfo;
	}

	/**
	 * Search for a specified string within a given node. <br>
	 * Does the same as <code>startNewSearch()</code> using node file
	 * coordinates as a search range.
	 * 
	 * @param stringToSearch
	 *            string to be found
	 * @param node
	 *            a node that sets the range of a search
	 * @return an object containing information about the search (parameters and results)
	 */
	public StringSearchInfo startNewSearchWithinANode(String stringToSearch, JSONNode node, boolean caseSensitive,
			boolean searchForAltUnicode) {
		return createNewSearch(stringToSearch, node.getStartFilePosition(), node.getEndFilePosition() + 1,
				caseSensitive, searchForAltUnicode);
	}

	//	public List<JSONNode> loadChildren(long pos) throws IOException, IllegalFormatException{
//		return parser.loadChildrenAtPosition(pos);
//	}
	/**
	 * Load children of a given node. 
	 * @param node
	 * @return a list of children of a given node if it is not null and not a leaf
	 * @throws IOException
	 * @throws IllegalFormatException
	 */
	// TODO: add some type of ID to connect parser with the reader/file
	public List<JSONNode> loadChildren(JSONNode node) throws IOException, IllegalFormatException{
		if(node == null || node.isLeaf()){
			return null;
		}
		// if the node is a root we already have loaded its children
		// (we can use start file position as an id for a node)
		if(node.getStartFilePosition() == getRoot().getStartFilePosition()){
			return parser.getRootChildren();
		}
		return parser.loadChildrenAtPosition(node.getValueFilePosition());
	}
	
	/**
	 * Validate a format of a given node.
	 * 
	 * @param node to be validated
	 * @return an instance of {@link IllegalFormatException} if a problem with
	 *         the node format occurs
	 * @throws IOException
	 */
	public IllegalFormatException validateNode(JSONNode node) throws IOException, IllegalFormatException {
		if (node == null) {
			return null;
		}
		/*
		 * if the node is the root we do not expect non-space symbols just after
		 * it (this is needed only when the root is a number, because a number
		 * does not have a way to determine that it is over except meeting the
		 * symbol just after it, which may legally be a comma or a bracket if
		 * this not is not a root)
		 */
		// TODO: find a way to make it more simple and solve the problem with a
		// number
		return parser.validateNodeAtPosition(node.getValueFilePosition(), node.getEndFilePosition(),
				node.getStartFilePosition() == getRoot().getStartFilePosition());
	}
	/**
	 * Return new JSONNode object which is the same as <code>node</code>
	 * but with fully loaded string
	 * @param node
	 * @return
	 */
	public JSONNode loadNodeWithFullString(JSONNode node) throws IOException, IllegalFormatException{
		String str = loadFullString(node.getValueFilePosition(), node.getEndFilePosition());
		return node.createNodeCopyWithFullyLoadedValue(str);
	}
	
	public String loadFullString(long openingQuotePos, long closingQuotePos) throws IOException, IllegalFormatException{
		return parser.loadStringAtPosition(openingQuotePos, closingQuotePos);
	}	

}
