package main.java.parser;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import javax.swing.tree.TreeNode;


public class JSONTreeNode  implements TreeNode{
	static final int TYPE_OBJECT = 1;
	static final int TYPE_ARRAY = 2;
	static final int TYPE_STRING = 3;
	static final int TYPE_NUMBER = 4;
	static final int TYPE_KEYWORD = 5;

	private int type;
	private String value;
	private String name;
	private List<JSONTreeNode> children;
	private JSONTreeNode parent;
	private long startPos;
	private long endPos;
	private int level;
	private boolean isFullyLoaded = true;
	
	JSONTreeNode(int type, String name, String value, int level){
		this(type, name, level);
		this.value = value;
	}
	JSONTreeNode(int type, String name, int level){
		this.type = type;
		this.level = level;
		this.name = name;
		this.children = new ArrayList<JSONTreeNode>();
		Arrays.fill(spaces,(byte)' ');
	}
	
	void addChild(JSONTreeNode child){
		children.add(child);
		child.parent = this;
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
	byte[] objStart = "{\n".getBytes();
	byte[] arrayStart = "[\n".getBytes();
	byte[] comma = ",\n".getBytes();
	byte[] colon = ": ".getBytes();
	byte[] spaces = new byte[500];
	
	void print(OutputStream stream)throws IOException{
		stream.write(spaces, 0, level*2);
		if(name != null){
			stream.write('\"');
			stream.write(name.getBytes());
			stream.write('\"');
			stream.write(colon);
		}
		if(type == TYPE_STRING){
			stream.write('\"');
			stream.write(value.getBytes(Charset.forName("UTF-8")));
			stream.write('\"');
		} else if(type == TYPE_KEYWORD || type == TYPE_NUMBER){
			stream.write(value.getBytes());
		} else if(type == TYPE_OBJECT){
			stream.write(objStart);
			for(int i = 0; i < children.size(); i++){
				JSONTreeNode child = children.get(i);
				child.print(stream);
				if(i < children.size()-1){
					stream.write(comma);
				}
			}
			stream.write('\n');
			stream.write(spaces, 0, level*2);
			stream.write('}');
		} else if(type == TYPE_ARRAY){
			stream.write(arrayStart);
			for(int i = 0; i < children.size(); i++){
				JSONTreeNode child = children.get(i);
				child.print(stream);
				if(i < children.size()-1){
					stream.write(comma);
				}
			}
			stream.write('\n');
			stream.write(spaces, 0, level*2);
			stream.write(']');
		}
	}
	
	public int getType(){
		return type;
	}
	
	public int getLevel(){
		return level;
	}
	 
	@Override
	public String toString() {
		if(type == TYPE_KEYWORD || type == TYPE_NUMBER){
			return (name!=null?(name+" : "):"") + value;
		}
		if(type == TYPE_STRING){
			return (name!=null?(name+" : "):"")+"\""+value+"\"";
		}
		if(type == TYPE_ARRAY){
			if(name == null)return "[]";
			return "[] "+name;
		}
		if(type == TYPE_OBJECT){
			if(name == null)return "{}";
			return "{} "+name;
		}
		return super.toString();
	}
	
	/////// TreeNode methods ///////////////////////
	@Override
	public TreeNode getChildAt(int childIndex) {
		if(childIndex < 0 || childIndex >= children.size()){
			throw new IndexOutOfBoundsException("Asks for child #"+ childIndex +
					" (have "+children.size()+" children)");
		}
		return children.get(childIndex);
	}
	@Override
	public int getChildCount() {
		return children.size();
	}
	@Override
	public TreeNode getParent() {
		return parent;
	}
	@Override
	public int getIndex(TreeNode node) {
		if(node == null) {
			return -1;
		}
		for(int i = 0; i < children.size(); i++){
			if(node.equals(children.get(i))){
				return i;
			}
		}
		return -1;
	}
	@Override
	public boolean getAllowsChildren() {
		return type == TYPE_ARRAY || type == TYPE_OBJECT;
	}
	@Override
	public boolean isLeaf() {
		return children.size() == 0;
	}
	@Override
	public Enumeration children() {
		return Collections.enumeration(children);
	}
	/////// End of TreeNode methods ///////////////////////

}
