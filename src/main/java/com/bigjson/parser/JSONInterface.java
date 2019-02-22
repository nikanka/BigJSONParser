package com.bigjson.parser;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;

/**
 * This class serves as an interface for the library, allowing to initialize 
 * parser, load the root of the JSON tree and children at a given file position in file
 * on demand. It also allows to load full String for a given String node (at a given
 * position in file). 
 * @author nikanka
 *
 */
public class JSONInterface {
	private LazyByteStreamParser parser;
	private JSONNode root;
	private String fileName;
	
	public JSONInterface(String fileName) throws IOException{
		this(fileName, null);
	}
	public JSONInterface(String fileName, String topLevelName) throws IOException{
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
	
	public boolean isLeaf(JSONNode node){
		return node.getType() != JSONNode.TYPE_ARRAY && node.getType() != JSONNode.TYPE_OBJECT;
	}
	
	public List<JSONNode> loadChildren(long pos) throws IOException{
		return parser.loadChildrenAtPosition(pos);
	}
	
	public void setNodeValue(JSONNode node, String newVal){
		node.setValue(newVal);
	}
	
	public String loadFullString(long openingQuotePos, long closingQuotePos) throws IOException{
		// TODO: make it more effective 
		// TODO: store one reusable reader per user 
//		RandomAccessFile raf = new RandomAccessFile(new File(fileName), "r");
//		raf.getChannel().position(openingQuotePos + 1);
//		// TODO: do we need longer strings then MAX_INT_VALUE?
//		byte[] byteBuf = new byte[(int)(closingQuotePos - openingQuotePos - 1)];
//		int len = raf.read(byteBuf);
//		raf.close();
//		return new String(byteBuf, "UTF-8");
		return parser.loadStringAtPosition(openingQuotePos, closingQuotePos);
	}
	

}
