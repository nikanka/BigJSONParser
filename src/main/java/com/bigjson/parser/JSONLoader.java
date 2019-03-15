package com.bigjson.parser;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.List;

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
	 * <code>searchInfo.searchIsFinished() == false</code>). The match should
	 * fit completely in the search range <br>
	 * Save new search result in the <code>searchInfo</code> object
	 * 
	 * @param searchInfo
	 *            object containing information about search of specified string
	 *            within the specified region of the file, including all
	 *            previous matches and the current match (if it was found). To
	 *            check whether a new match was found use
	 *            <code>searchInfo.searchIsFinished()</code>.
	 * @throws IOException
	 */
	public void findNextMatch(StringSearchInfo searchInfo) throws IOException{
		if(!searchInfo.searchIsFinished()){
			parser.getReader().findNextMatch(searchInfo);
		}
	}
	
	/**
	 * Start a new search of <code>stringToSearch</code> within range
	 * <code>[searchStartPos, searchEndPos)</code> of a file. The match should
	 * fit completely in this range.<br>
	 * <br>
	 * IMPORTANT: the start position of a search (<code>searchStartPos</code>) should  
	 * be outside of a string. 
	 * TODO: A match should be completely within a value or a name.
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
	 * @return an object containing information about the search including the
	 *         first match (if any). To check whether a match was found use
	 *         <code>searchInfo.searchIsFinished()</code>.
	 * @throws IOException
	 */
	public StringSearchInfo startNewSearch(String stringToSearch, long searchStartPos, long searchEndPos) throws IOException{
		StringSearchInfo searchInfo = StringSearchInfo.createNewSearch(stringToSearch, searchStartPos, searchEndPos);
		findNextMatch(searchInfo);
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
	 * @return an object containing information about the search
	 * @throws IOException
	 */
	public StringSearchInfo startNewSearchWithinANode(String stringToSearch, JSONNode node)throws IOException{
		return startNewSearch(stringToSearch, node.getStartFilePosition(), node.getEndFilePosition() + 1);
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
