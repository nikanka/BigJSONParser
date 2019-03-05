package com.bigjson.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

public class JSONLoaderTest {

	private static boolean DEBUG = true;
	
	private static void debug(String toPrint){
		if(DEBUG){
			System.out.println("TEST: " + toPrint);
		}
	}

	@Test
	public void shouldParseSmallJSONFile() throws IOException, IllegalFormatException{
		String fileName = "testFiles/SmallTest0.json";
		parseAndCompare(fileName, 10000);
		parseAndCompare(fileName, 10);
		fileName = "testFiles/SmallTest1.json";
		parseAndCompare(fileName, 10000);
		parseAndCompare(fileName, 10);
		fileName = "testFiles/SmallTest2.json";
		parseAndCompare(fileName, 10000);
		parseAndCompare(fileName, 10);
		fileName = "testFiles/SmallTest3.json";
		parseAndCompare(fileName, 10000);
		parseAndCompare(fileName, 10);
	}
	
	private void parseAndCompare(String fileName, int stringLenngth) throws IOException, IllegalFormatException{
		try(JSONLoader loader = new JSONLoader(fileName, stringLenngth)){
			// this parser vs...
			JSONNode top = loader.getRoot();
			// org.json parser
			byte[] bytes = Files.readAllBytes(Paths.get(fileName));
			JSONObject topExp = new JSONObject(new String(bytes));
			
			verifyNode(topExp, loader, top, stringLenngth);
		}
	}
	
	/**
	 * Verifies the type and value of the node. It is assumed that the name has been already verified.
	 * @param nodeExp
	 * @param parser
	 * @param node
	 */
	private static void verifyNode(Object nodeExp, JSONLoader loader, JSONNode node, int stringLength)
			throws IOException, IllegalFormatException {
		debug("Verify "+ node.getName()+" : "+node.getValue());
		switch(node.getType()){
			case JSONNode.TYPE_ARRAY:
				assertTrue(nodeExp instanceof JSONArray);
				verifyArrayChildren((JSONArray)nodeExp, loader, node, stringLength);
				break;
			case JSONNode.TYPE_OBJECT:
				assertTrue(nodeExp instanceof JSONObject); 
				verifyObjectChildren((JSONObject)nodeExp, loader, node, stringLength);
				break;
			case JSONNode.TYPE_KEYWORD:
				assertEquals(nodeExp.toString(), node.getValue());
				break;
			case JSONNode.TYPE_NUMBER:
				try{
					NumberFormat f = NumberFormat.getInstance(); 
					assertEquals(f.parse(nodeExp.toString().toUpperCase()), 
							f.parse(node.getValue().toUpperCase()));
				} catch(ParseException e){
					throw new RuntimeException(e);
				}
				break;
			case JSONNode.TYPE_STRING:
				String strExp = nodeExp.toString();
				assertEquals(strExp.substring(0, Math.min(strExp.length(), stringLength)), node.getValue());
				break;
		}
	}

	/**
	 * Verifies children of an object node: their number and names.
	 * @param nodeExp
	 * @param parser
	 * @param node
	 * @throws IOException
	 */
	private static void verifyObjectChildren(JSONObject nodeExp, JSONLoader loader, JSONNode node,
			int stringLength) throws IOException, IllegalFormatException {
		// load children dynamically
		List<JSONNode> children = loader.loadChildren(node);
		// compare number of children
		assertEquals(nodeExp.length(), children.size());
		// compare each child
		debug("Children of an Object:");
		for(JSONNode child: children){
			Object childExp = null;
			try{
				childExp = nodeExp.get(child.getName());
			}catch(JSONException e){
				e.printStackTrace();
			}
			// verify name
			assertNotNull(childExp);
			verifyNode(childExp, loader, child, stringLength);
		}
	}
	
	/**
	 * Verifies children of an object node: their number and names.
	 * @param nodeExp
	 * @param parser
	 * @param node
	 * @throws IOException
	 */
	private static void verifyArrayChildren(JSONArray nodeExp, JSONLoader loader, JSONNode node,
			int stringLength) throws IOException, IllegalFormatException {
		// load children dynamically
		List<JSONNode> children = loader.loadChildren(node);
		// compare number of children
		assertEquals(nodeExp.length(), children.size());
		// compare each child
		debug("Children of an Array:");
		for(int i = 0; i < children.size(); i++){
			Object childExp = null;
			try{
				childExp = nodeExp.get(i);
			}catch(JSONException e){
				e.printStackTrace();
			}
			assertNotNull(childExp);
			JSONNode child = children.get(i);
			verifyNode(childExp, loader, child, stringLength);
		}
	}


}
