package com.bigjson.parser;

public interface JSONNode {
	public static final int TYPE_OBJECT = 1;
	public static final int TYPE_ARRAY = 2;
	public static final int TYPE_STRING = 3;
	public static final int TYPE_NUMBER = 4;
	public static final int TYPE_KEYWORD = 5;

	/**
	 * Create a copy of a node with a fully loaded value (isFullyLoaded becomes true)
	 * @param newVal
	 * @return a copy of a current node with new value and isFullyLoaded == true
	 */
	public JSONNode createNodeCopyWithFullyLoadedValue(String newVal);
		
	public int getType();
	
	public String getName();

	public String getValue();

	public long getStartFilePosition();
	
	public long getValueFilePosition();
	
	public long getEndFilePosition();

	public boolean isFullyLoaded();
	
	public boolean isLeaf();
}
