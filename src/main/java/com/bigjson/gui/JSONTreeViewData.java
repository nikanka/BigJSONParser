package com.bigjson.gui;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JOptionPane;
import javax.swing.tree.DefaultTreeModel;
import com.bigjson.parser.IllegalFormatException;
import com.bigjson.parser.JSONLoader;
import com.bigjson.parser.JSONNode;
import com.bigjson.parser.StringSearchInfo;

public class JSONTreeViewData {
	static int stringDisplayLength = 100;
	
	private DefaultTreeModel treeModel;
	private JSONLoader backend;
	private StringSearchInfo currentSearch = null;
	private DefaultComboBoxModel<StringSearchInfo> cachedSearches = new DefaultComboBoxModel<StringSearchInfo>();
	 
	
	public JSONTreeViewData(File file, boolean validate) {
		// create a loader for a given file
		try {
			backend = new JSONLoader(file, stringDisplayLength);
			long t1 = System.currentTimeMillis();
			JSONTreeNode rootNode = new JSONTreeNode(validate ? backend.getRootAndValidate() : backend.getRoot());
			System.out.println("Load root and children: " + (System.currentTimeMillis() - t1) / 1000 + " s");
			 treeModel = new DefaultTreeModel(rootNode, true);
//			initTreeModel(rootNode);
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
	public void dispose(){
		try{
			backend.close();
		}catch(IOException e){
			MainFrame.showDialog("An IOException occured while closing the previous file reader: " + e.getMessage());
			return;
		}
		// TODO: how release resources properly?
		backend = null;
		cachedSearches = null;
		treeModel = null;
	}
//	private void loadTreeFromFile(File file, boolean validate){
//		// close previous loader
//		if(backend != null){
//			try{
//				backend.close();
//			}catch(IOException e){
//				showDialog("An IOException occured while closing the previous file reader: " + e.getMessage());
//				return;
//			}
//		}
//		// create a loader for a given file
//		try{
//			backend = new JSONLoader(file, stringDisplayLength);
//			long t1 = System.currentTimeMillis();
//			JSONTreeNode rootNode = new JSONTreeNode(validate ? backend.getRootAndValidate() : backend.getRoot());
//			System.out.println("Load root and children: "+(System.currentTimeMillis() - t1)/1000 + " s");
////			treeModel = new DefaultTreeModel(rootNode, true);
//			initTreeModel(rootNode);
//			loadChildrenForNode(rootNode);
//		}catch(IOException e){
//			showDialog("An IOException occured while reading new file: "+e.getMessage());
//			return;
//		}catch(IllegalFormatException e){
//			showDialog("An IllegalFormatException occured while loading the file: "+e.getMessage());
//			return;
//		}
//		fileInfoField.setText(file.getName());
//		fileInfoField.setToolTipText(file.getAbsolutePath());
//		if(validate){
//			showDialog("File " + file.getName() + " has been successfully validated");
//		}
//	}
	
	public StringSearchInfo createNewSearch(String toSearch, long from, long to, boolean caseSensitive, boolean searchForAltUnicode){
		 StringSearchInfo search =  backend.createNewSearch(toSearch, from, to,
				caseSensitive, searchForAltUnicode);
		System.out.println("Search created");
		cachedSearches.addElement(search);
		return search;
	}

	
	public void loadFullStringForNode(JSONTreeNode treeNode){
		JSONNode node = treeNode.getJSONNode();
		try {
			node = backend.loadNodeWithFullString(node);	
		} catch (IOException e) {
			MainFrame.showDialog(
					"IOException occured while loading full string for " + treeNode.getUserObject() + ": " + e.getMessage());

		} catch (IllegalFormatException e) {
			MainFrame.showDialog("IllegalFormatException occured while loading full string for " + treeNode.getUserObject() + ": "
					+ e.getMessage());
		}
		String str = node.getValue();
		treeNode.setJSONNode(node);
		treeModel.nodeChanged(treeNode);

//		nodeValueTextArea.setText(str);
//		loadFullStringBtn.setEnabled(false);
//		return str;
	}
	
	/**
	 * Return true if search is not finished
	 * @return
	 */
	private boolean findNextMatch(){
		try{
			backend.findNextMatch(currentSearch);
		}catch(IOException ex){
			MainFrame.showDialog("An IOException occured while searching: "+ex.getMessage());
			return false;
		}catch(IllegalFormatException ex){
			MainFrame.showDialog("An IllegalFormatException occured while searching: "+ex.getMessage());
			return false;
		}
		long pos = currentSearch.getLastMatchPos();
		if(pos >= 0){
			openLastNodeContainingPos(pos);
		}
		return !currentSearch.searchIsFinished();
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
	private void openLastNodeContainingPos(long pos){
		JSONTreeNode node = (JSONTreeNode)treeModel.getRoot();
		if(node == null){
			return;
		}
		// TODO: need to actually open  node (maybe make it selected and then 
		// treeView will notice it and expand it)
		if(getLastNodeContainingPos(node, pos) == null){
			// make showDialog universal, maybe just switch to JOptionPane.showMessageDialog(null, "msg");
			MainFrame.showDialog("Position " + pos + " is outside the root");
		}
	}

	
	public void loadChildrenForNode(JSONTreeNode parent){
		if(!parent.getAllowsChildren() || parent.childrenAreLoaded()){
			return;
		}
		long time = System.currentTimeMillis();
		try{
			List<JSONNode> children = backend.loadChildren(parent.getJSONNode());
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
			IllegalFormatException ex = backend.validateNode(clickedNode.getJSONNode());
			if (ex != null) {
				JOptionPane.showMessageDialog(null, "A problem with the node format was detected: " + ex.getMessage());
			} else {
				JOptionPane.showMessageDialog(null, "The format of the node was succesfully validated.");
			}
		} catch (Exception ex) {
			JOptionPane.showMessageDialog(null, ex.getClass() + " occured while validating the node: " + ex.getMessage());
		}
	}

	public DefaultComboBoxModel<StringSearchInfo> getCashedSearches() {
		return cachedSearches;
	}

}
