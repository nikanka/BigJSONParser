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
	private LazyByteStreamParser parser;
	
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
		parser = new LazyByteStreamParser(file, topLevelName, stringDisplayLimit);
//		long t1 = System.currentTimeMillis();
//		root = parser.getRoot();
//		System.out.println("LoadTime for the root: "+(System.currentTimeMillis() - t1)/1000 + " s");
	}
	@Override
	public void close() throws IOException {
		parser.close();
	}
	
	public JSONNode getRoot() throws IOException, IllegalFormatException{
		return parser.getRoot();
	}
	
	public File getFile(){
		return parser.getFile();
	}
		
//	public List<JSONNode> loadChildren(long pos) throws IOException, IllegalFormatException{
//		return parser.loadChildrenAtPosition(pos);
//	}
	/**
	 * 
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
		return parser.loadChildrenAtPosition(node.getStartFilePosition());
	}
	/**
	 * Return new JSONNode object which is the same as <code>node</code>
	 * but with fully loaded string
	 * @param node
	 * @return
	 */
	public JSONNode loadNodeWithFullString(JSONNode node) throws IOException, IllegalFormatException{
		String str = loadFullString(node.getStartFilePosition(), node.getEndFilePosition());
		return node.createNodeCopyWithFullyLoadedValue(str);
	}
	
	public String loadFullString(long openingQuotePos, long closingQuotePos) throws IOException, IllegalFormatException{
		return parser.loadStringAtPosition(openingQuotePos, closingQuotePos);
	}	

}
