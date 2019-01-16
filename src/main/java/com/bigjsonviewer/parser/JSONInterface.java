package com.bigjsonviewer.parser;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;

public class JSONInterface {
	private LazyByteStreamParser parser;
	private JSONNode root;
	private String fileName;
	
	public JSONInterface(String fileName){
		this.fileName = fileName;
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
	
	public String loadFullString(long openingQuotePos, long closingQuotePos) throws IOException{
		// TODO: make it more effective 
		// TODO: store one reusable reader per user 
		RandomAccessFile raf = new RandomAccessFile(new File(fileName), "r");
		raf.getChannel().position(openingQuotePos + 1);
		// TODO: do we need longer strings then MAX_INT_VALUE?
		byte[] byteBuf = new byte[(int)(closingQuotePos - openingQuotePos - 1)];
		int len = raf.read(byteBuf);
		raf.close();
		// TODO: do we need UTF-16?
		return new String(byteBuf, "UTF-8");
	}
	

}
