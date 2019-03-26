package com.bigjson.gui;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.tree.DefaultTreeModel;

import com.bigjson.parser.IllegalFormatException;
import com.bigjson.parser.JSONLoader;
import com.bigjson.parser.JSONNode;

public class JSONTreeViewModel {
	static int stringDisplayLength = 100;
	
	private DefaultTreeModel treeModel;
	private JSONLoader loader;	 
	
	public JSONTreeViewModel(File file, boolean validate) {
		// create a loader for a given file
		try {
			loader = new JSONLoader(file, stringDisplayLength);
			long t1 = System.currentTimeMillis();
			JSONTreeNode rootNode = new JSONTreeNode(validate ? loader.getRootAndValidate() : loader.getRoot());
			System.out.println("Load root and children: " + (System.currentTimeMillis() - t1) / 1000 + " s");
			treeModel = new DefaultTreeModel(rootNode, true);
			loadChildrenForNode(rootNode);
		} catch (IOException e) {
			MainFrame.showDialog("An IOException occured while reading new file: " + e.getMessage());
		} catch (IllegalFormatException e) {
			MainFrame.showDialog("An IllegalFormatException occured while loading the file: " + e.getMessage());
		}
		if (validate) {
			MainFrame.showDialog("File " + file.getName() + " has been successfully validated");
		}
	}

	public DefaultTreeModel getTreeModel(){
		return treeModel;
	}
	
	public JSONLoader getJSONLoader(){
		return loader;
	}
	
	public void dispose(){
		try{
			loader.close();
		}catch(IOException e){
			MainFrame.showDialog("An IOException occured while closing the previous file reader: " + e.getMessage());
			return;
		}
		// TODO: how release resources properly?
		loader = null;
		treeModel = null;
	}
	
	
	public void loadFullStringForNode(JSONTreeNode treeNode){
		JSONNode node = treeNode.getJSONNode();
		try {
			node = loader.loadNodeWithFullString(node);	
		} catch (IOException e) {
			MainFrame.showDialog(
					"IOException occured while loading full string for " + treeNode.getUserObject() + ": " + e.getMessage());

		} catch (IllegalFormatException e) {
			MainFrame.showDialog("IllegalFormatException occured while loading full string for " + treeNode.getUserObject() + ": "
					+ e.getMessage());
		}
		treeNode.setJSONNode(node);
		treeModel.nodeChanged(treeNode);
	}
	

	
	
	private JSONTreeNode getLastNodeContainingPos(JSONTreeNode treeNode, long pos){
		JSONNode node = treeNode.getJSONNode();
		if(pos < node.getStartFilePosition() || pos > node.getEndFilePosition()){
			return null;
		}
		// if it is a leaf or the pos is outside of a value (e.g. in a name), open this node
		if(node.isLeaf() || pos < node.getValueFilePosition()){
			// if in string - load the string
			if(node.getType() == JSONNode.TYPE_STRING && pos > node.getValueFilePosition()){
				loadFullStringForNode(treeNode);
			}
			return treeNode;
		}
		int n = treeNode.getChildCount();
		if(n == 0){
			loadChildrenForNode(treeNode);
		}
		n = treeNode.getChildCount();
		if(n == 0){
			return treeNode;
		}
		for(int i = 0; i < n; i++){
			JSONTreeNode child = getLastNodeContainingPos((JSONTreeNode)treeNode.getChildAt(i), pos);
			if(child != null){
				return child;
			}	
		}
		return treeNode;
	}
	public JSONTreeNode openLastNodeContainingPos(long pos){
		JSONTreeNode node = (JSONTreeNode)treeModel.getRoot();
		if(node == null){
			return null;
		}
		node = getLastNodeContainingPos(node, pos);
		if(node == null){
			// make showDialog universal, maybe just switch to JOptionPane.showMessageDialog(null, "msg");
			MainFrame.showDialog("Position " + pos + " is outside the root");
		}
		return node;
	}

	
	public void loadChildrenForNode(JSONTreeNode parent){
		if(!parent.getAllowsChildren() || parent.childrenAreLoaded()){
			return;
		}
		long time = System.currentTimeMillis();
		try{
			List<JSONNode> children = loader.loadChildren(parent.getJSONNode());
			for(JSONNode child: children){
				treeModel.insertNodeInto(new JSONTreeNode(child), parent, parent.getChildCount());
			}
		} catch(IOException e){
			JOptionPane.showMessageDialog(null, "IOException occured while loading children nodes: " + e.getMessage());
		} catch(IllegalFormatException e){
			JOptionPane.showMessageDialog(null, "IllegalFormatException occured while loading children nodes: " + e.getMessage());
		}
		parent.setChildrenAreLoaded(true);
		System.out.println("Load children for node " + parent + ": " + 
							(System.currentTimeMillis() - time) / 1000 + " s");
	}

	public void validateNode(JSONTreeNode clickedNode) {
		try {
			IllegalFormatException ex = loader.validateNode(clickedNode.getJSONNode());
			if (ex != null) {
				JOptionPane.showMessageDialog(null, "A problem with the node format was detected: " + ex.getMessage());
			} else {
				JOptionPane.showMessageDialog(null, "The format of the node was succesfully validated.");
			}
		} catch (Exception ex) {
			JOptionPane.showMessageDialog(null, ex.getClass() + " occured while validating the node: " + ex.getMessage());
		}
	}


}
