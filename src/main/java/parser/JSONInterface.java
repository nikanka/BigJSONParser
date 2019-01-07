package main.java.parser;

import java.io.IOException;
import java.util.List;

public class JSONInterface {
	private LazyByteStreamParser parser;
	private JSONNode root;
	public JSONInterface(String fileName){
		try{
			parser = new LazyByteStreamParser(fileName);
			root = parser.parseTopLevel();//TODO: should check format at this point
		}catch(IOException e){
			//TODO
			throw new RuntimeException(e);
		}
	}
	
	public JSONNode getRoot(){
		return root;
	}
	
	public boolean isLeaf(JSONNode node){
		return node.getType() != JSONNode.TYPE_ARRAY && node.getType() != JSONNode.TYPE_OBJECT;
	}
	
	
	public List<JSONNode> loadChildren(long pos){
		try{
			return parser.loadChildrenAtPosition(pos);
		}catch(IOException e){
			throw new RuntimeException(e);//TODO handle exception
		}
	}
	
	public String loadFullString(long pos){
		//TODO
		return null;
	}
	

}
