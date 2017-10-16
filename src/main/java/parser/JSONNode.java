package main.java.parser;

public class JSONNode {
	static final int TYPE_OBJECT = 1;
	static final int TYPE_ARRAY = 2;
	static final int TYPE_STRING = 3;
	static final int TYPE_NUMBER = 4;
	static final int TYPE_KEYWORD = 5;

	private int type;
	private String value;
	private String name;
	private long startPos;
	private boolean isFullyLoaded = true;
	
	JSONNode(int type, String name, String value){
		this(type, name);
		this.setValue(value);
	}
	JSONNode(int type, String name){
		this.type = type;
		this.name = name;
	}
		
	long getFilePosition(){
		return startPos;
	}
	
	void setFilePosition(long pos){
		this.startPos = pos;
	}
	
	public boolean isFullyLoaded(){
		return isFullyLoaded;
	}
	
	void setIsFullyLoaded(boolean isFullyLoaded){
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
	public void setValue(String value) {
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
