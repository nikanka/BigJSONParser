package main.java.parser;


/**
 * Represent a node of a JSON tree that was read from a file. 
 * Besides the usual node information (type, name and value) contain 
 * a start position in a file (in bytes) and if this node is fully loaded, 
 * i.e. if its immediate children are loaded or if the String value is fully loaded.
 * @author nikanka
 *
 */
public class JSONNode {
	public static final int TYPE_OBJECT = 1;
	public static final int TYPE_ARRAY = 2;
	public static final int TYPE_STRING = 3;
	public static final int TYPE_NUMBER = 4;
	public static final int TYPE_KEYWORD = 5;

	private int type;
	private String value;
	private String name;
	private String nodeString;
	private long startPos;
	private boolean isFullyLoaded = true;
	
	protected JSONNode(int type, String name, String value){
		this.type = type;
		this.name = name;
		this.setValue(value);
		this.nodeString = createNodeString();
	}
	protected JSONNode(int type, String name){
		this(type, name, null);
	}
	
	private String createNodeString(){
		if(getType() == JSONNode.TYPE_ARRAY){
			return "[]" + getName()!=null?getName():"";
		}
		if(getType() == JSONNode.TYPE_OBJECT){
			return "{}" + getName()!=null?getName():"";
		} 
		String ret = getName()!=null?(getName()+" : "):"";
		if(getType() == JSONNode.TYPE_NUMBER || getType() == JSONNode.TYPE_KEYWORD){
			return ret + getValue();
		}
		if(getType() == JSONNode.TYPE_STRING){
			return ret + "\""+getValue() + (isFullyLoaded()?"":"...") + "\"";
		}
		assert false: getType();
		return null;
	}

		
	public long getFilePosition(){
		return startPos;
	}
	
	public String getNodeString(){
		return nodeString;
	}
	
	protected void setFilePosition(long pos){
		this.startPos = pos;
	}
	
	public boolean isFullyLoaded(){
		return isFullyLoaded;
	}
	
	public void setIsFullyLoaded(boolean isFullyLoaded){
		this.isFullyLoaded = isFullyLoaded;
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
	protected void setValue(String value) {
		this.value = value;
	}
	 
//	/////// TreeNode methods ///////////////////////
//	@Override
//	public TreeNode getChildAt(int childIndex) {
//		if(childIndex < 0 || childIndex >= children.size()){
//			throw new IndexOutOfBoundsException("Asks for child #"+ childIndex +
//					" (have "+children.size()+" children)");
//		}
//		return children.get(childIndex);
//	}
//	@Override
//	public int getChildCount() {
//		return children.size();
//	}
//	@Override
//	public TreeNode getParent() {
//		return parent;
//	}
//	@Override
//	public int getIndex(TreeNode node) {
//		if(node == null) {
//			return -1;
//		}
//		for(int i = 0; i < children.size(); i++){
//			if(node.equals(children.get(i))){
//				return i;
//			}
//		}
//		return -1;
//	}
//	@Override
//	public boolean getAllowsChildren() {
//		return type == TYPE_ARRAY || type == TYPE_OBJECT;
//	}
//	@Override
//	public boolean isLeaf() {
//		return children.size() == 0;
//	}
//	@Override
//	public Enumeration children() {
//		return Collections.enumeration(children);
//	}
//	/////// End of TreeNode methods ///////////////////////

}
