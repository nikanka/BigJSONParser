package com.bigjsonviewer.parser;

import static org.junit.Assert.*;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;


public class LazyByteStreamParserTest {
	
	public static void main(String[] args) throws IOException{
		String fileName = "testFiles/SmallTest1.json";
		
		byte[] bytes = Files.readAllBytes(Paths.get(fileName));
		JSONObject topExp = new JSONObject(new String(bytes));
		
		
	}
	
	private void verifyObject(JSONObject nodeExp, LazyByteStreamParser parser, JSONNode node)throws IOException{
		// load children dynamically
		List<JSONNode> children = parser.loadChildrenAtPosition(node.getStartFilePosition());
		// compare number of children
		assertEquals(nodeExp.length(), children.size());
		// compare each child
		System.out.println("Children of Object:");
		
		for(JSONNode child: children){
			System.out.println("\t"+child.getName()+" : "+child.getValue());
			Object childExp = null;
			try{
				childExp = nodeExp.get(child.getName());
			}catch(JSONException e){
				e.printStackTrace();
			}
			// verify name
			assertNotNull(childExp);
			switch(child.getType()){
				case JSONNode.TYPE_ARRAY:
					assertTrue(childExp instanceof JSONArray);
					//verifySubTree()
					break;
				case JSONNode.TYPE_OBJECT:
					assertTrue(childExp instanceof JSONObject);
					verifyObject((JSONObject)childExp, parser, child);
					break;
				case JSONNode.TYPE_KEYWORD://TODO: handle null value
					assertEquals(childExp.toString(), child.getValue());
					break;
				case JSONNode.TYPE_NUMBER:
					assertEquals(childExp.toString().toLowerCase(), child.getValue().toLowerCase());
					break;
				case JSONNode.TYPE_STRING:
					assertEquals(childExp.toString(), child.getValue());
					break;
			}
		}
	}

	@Test
	public void shouldParseSmallJSONFile() throws IOException{
		String fileName = "testFiles/SmallTest1.json";
		LazyByteStreamParser parser = new LazyByteStreamParser(fileName, 1000);
		JSONNode top = parser.parseTopLevel();
		
		// read the whole file in a String
		byte[] bytes = Files.readAllBytes(Paths.get(fileName));
		JSONObject topExp = new JSONObject(new String(bytes));
		
		verifyObject(topExp, parser, top);
		
	}
}
