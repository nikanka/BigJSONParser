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
	
	public JSONLoader(String fileName) throws IOException{
		this(fileName, null);
	}
	public JSONLoader(String fileName, String topLevelName) throws IOException{
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
		
	public List<JSONNode> loadChildren(long pos) throws IOException{
		return parser.loadChildrenAtPosition(pos);
	}
	public List<JSONNode> loadChildren(JSONNode node) throws IOException{
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
	public JSONNode loadNodeWithFullString(JSONNode node) throws IOException{
		String str = loadFullString(node.getStartFilePosition(), node.getEndFilePosition());
		return node.createNodeCopyWithNewValue(str, true);
	}
	
	public String loadFullString(long openingQuotePos, long closingQuotePos) throws IOException{
		return parser.loadStringAtPosition(openingQuotePos, closingQuotePos);
	}
	

}