package com.bigjson.parser;

public interface JSONNode {
	public static final int TYPE_OBJECT = 1;
	public static final int TYPE_ARRAY = 2;
	public static final int TYPE_STRING = 3;
	public static final int TYPE_NUMBER = 4;
	public static final int TYPE_KEYWORD = 5;

	/**
	 * Create a copy of a given node with a new value
	 * @param node
	 * @param newVal
	 */
	public JSONNode createNodeCopyWithNewValue(String newVal, boolean isFullyLoaded);
		
	public int getType();
	
	public String getName();

	public String getValue();

	public long getStartFilePosition();
	
	public long getEndFilePosition();

	public boolean isFullyLoaded();
	
	public boolean isLeaf();
}
