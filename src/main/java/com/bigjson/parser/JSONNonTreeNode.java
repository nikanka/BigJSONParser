package com.bigjson.parser;

/**
 * Represent a node of a JSON tree that was read from a file. 
 * Besides the usual node information (type, name and value) contain 
 * a start position in a file (in bytes) and if this node is fully loaded, 
 * i.e. if its immediate children are loaded or if the String value is fully loaded.
 * @author nikanka
 *
 */
public class JSONNonTreeNode implements JSONNode{

	private int type;
	private String value;
	private String name;
	private long startPos;
	private long endPos;
	private boolean isFullyLoaded = true;
	
	/**
	 * In case of String node, value contains String without quotes. 
	 * File position for a node is a byte position of an opening quote 
	 * (in case of String node) or opening bracket (in case of array or object nodes).
	 * End position is specified only for String nodes and contains a byte 
	 * position of a closing quote.
	 * @param type
	 * @param name
	 * @param value
	 */
	protected JSONNonTreeNode(int type, String name, boolean isFullyLoaded, String value){
		this(type, name, isFullyLoaded);
		this.value = value;
	}
	protected JSONNonTreeNode(int type, String name, String value){
		this(type, name, true);
		this.value = value;
	}
	protected JSONNonTreeNode(int type, String name, boolean isFullyLoaded){
		this.type = type;
		this.name = name;
		this.isFullyLoaded = isFullyLoaded;
	}
	/**
	 * 
	 * @param type
	 * @param name
	 */
	protected JSONNonTreeNode(int type, String name){
		this(type, name, true);
	}

	/**
	 * Create a copy of a given node with a new value
	 * @param newVal
	 * @param node
	 */
	public JSONNode createNodeCopyWithFullyLoadedValue(String newVal) {
		JSONNonTreeNode ret = new JSONNonTreeNode(getType(), getName(), true, newVal);
		ret.startPos = this.startPos;
		ret.endPos = this.endPos;
		return ret;
	}
		
	public long getStartFilePosition(){
		return startPos;
	}
	
	public long getEndFilePosition(){
		return endPos;
	}
	
	protected void setStartFilePosition(long pos){
		this.startPos = pos;
	}
	
	protected void setEndFilePosition(long pos){
		this.endPos = pos;
	}
	
	public boolean isFullyLoaded(){
		return isFullyLoaded;
	}
	
	public boolean isLeaf(){
		return getType() != JSONNode.TYPE_ARRAY && getType() != JSONNode.TYPE_OBJECT;
	}
	
	public int getType(){
		return type;
	}
	public String getName() {
		return name;
	}

	public String getValue() {
		return value;
	}
}
