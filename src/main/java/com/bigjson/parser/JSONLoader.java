package com.bigjson.parser;

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
public class JSONLoader {
	private LazyByteStreamParser parser;
	private JSONNode root;
	private String fileName;
	
	public JSONLoader(String fileName) throws IOException, IllegalFormatException{
		this(fileName, null);
	}
	
	/**
	 * Creates a loader for a given JSON file.
	 * 
	 * @param fileName
	 * @param topLevelName
	 *            what name should be given (for display purposes) to the
	 *            top-level node. If topLevelName == null, the default name
	 *            ("JSON") will be used 
	 * @throws IOException
	 *             if file is not found or empty or if an I/O error occurs while
	 *             reading bytes from the file
	 * @throws IllegalFormatException
	 *             if the format of the data is not as expected
	 */
	public JSONLoader(String fileName, String topLevelName) throws IOException, IllegalFormatException{
		this.fileName = fileName;
		parser = new LazyByteStreamParser(fileName, 20);
		root = parser.parseTopLevel(topLevelName);//TODO: should check format at this point
	}
	public void destroy() throws IOException{
		parser.destroy();
	}
	public JSONNode getRoot(){
		return root;
	}
	
	public String getFileName(){
		return fileName;
	}
		
	public List<JSONNode> loadChildren(long pos) throws IOException, IllegalFormatException{
		return parser.loadChildrenAtPosition(pos);
	}
	public List<JSONNode> loadChildren(JSONNode node) throws IOException, IllegalFormatException{
		if(node == null){
			return null;
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
		return node.createNodeCopyWithNewValue(str, true);
	}
	
	public String loadFullString(long openingQuotePos, long closingQuotePos) throws IOException, IllegalFormatException{
		return parser.loadStringAtPosition(openingQuotePos, closingQuotePos);
	}
	

}
