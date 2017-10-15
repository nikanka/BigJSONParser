package main.java.gui;

import java.awt.Dimension;
import java.io.IOException;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreeNode;

import main.java.parser.ByteStreamParser;
import main.java.parser.JSONTreeNode;
import main.java.parser.LazyByteStreamParser;

public class JSONTreeViewPanel extends JPanel implements TreeWillExpandListener {
	DefaultTreeModel treeModel;
	LazyByteStreamParser parser;
	
	public static void main(String[] args) throws IOException{
		String fileName = "SmallTest2.json";
		
		JFrame frame = new JFrame("Tree View");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.add(new JSONTreeViewPanel(fileName));
		frame.pack();
		frame.setVisible(true);
	}
	public JSONTreeViewPanel(String fileName)throws IOException{
		super();
//		DefaultMutableTreeNode root = new DefaultMutableTreeNode("Root");
//		DefaultMutableTreeNode child1 = new DefaultMutableTreeNode("Child1");
//		DefaultMutableTreeNode child2 = new DefaultMutableTreeNode("Child2");
//		root.add(child1);
//		root.add(child2);
//		child1.add(new DefaultMutableTreeNode("mockNode"));
//		child2.add(new DefaultMutableTreeNode("mockNode"));
		parser = new LazyByteStreamParser(fileName, true);
		TreeNode root = parser.parseTopLevel();
		
		treeModel = new DefaultTreeModel(root, true);
		JTree treeView = new JTree(treeModel);
		treeView.addTreeWillExpandListener(this);
//		treeView.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

		JScrollPane scrollPane = new JScrollPane(treeView);
		scrollPane.setPreferredSize(new Dimension(500, 400));
		add(scrollPane);
	}
	
	@Override
	public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {
		JSONTreeNode parent = (JSONTreeNode)event.getPath().getLastPathComponent();
		if(parent.isFullyLoaded()){
			return;
		}
		try{
			parser.loadNodeChildren(parent);
		}catch(IOException e){
			throw new RuntimeException(e);
		}
//		for(JSONTreeNode child: parent.children()){
//			
//		}
//		treeModel.insertNodeInto(new DefaultMutableTreeNode("Test"), parent, 0);
//		
		
	}
	@Override
	public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException {
		// TODO Auto-generated method stub
		
	}
}
