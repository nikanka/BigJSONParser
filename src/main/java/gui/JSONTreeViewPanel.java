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

public class JSONTreeViewPanel extends JPanel implements TreeWillExpandListener {
	DefaultTreeModel treeModel;
	
	public static void main(String[] args) throws IOException{
		String fileName = "SmallTest2.json";
		ByteStreamParser parser = new ByteStreamParser(fileName);
		TreeNode root = parser.parseIntoFullTree();
		
		JFrame frame = new JFrame("Tree View");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.add(new JSONTreeViewPanel(root));
		frame.pack();
		frame.setVisible(true);
	}
	public JSONTreeViewPanel(TreeNode root){
		super();
//		DefaultMutableTreeNode root = new DefaultMutableTreeNode("Root");
//		DefaultMutableTreeNode child1 = new DefaultMutableTreeNode("Child1");
//		DefaultMutableTreeNode child2 = new DefaultMutableTreeNode("Child2");
//		root.add(child1);
//		root.add(child2);
//		child1.add(new DefaultMutableTreeNode("mockNode"));
//		child2.add(new DefaultMutableTreeNode("mockNode"));
		treeModel = new DefaultTreeModel(root, true);
		JTree treeView = new JTree(treeModel);
		treeView.addTreeWillExpandListener(this);
//		treeView.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

		JScrollPane scrollPane = new JScrollPane(treeView);
		scrollPane.setPreferredSize(new Dimension(500, 400));
		add(scrollPane);
	}
	
	private void addNodes(	){
		
	}
	@Override
	public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {
		
//		DefaultMutableTreeNode parent = (DefaultMutableTreeNode)event.getPath().getLastPathComponent();
//		treeModel.insertNodeInto(new DefaultMutableTreeNode("Test"), parent, 0);
//		
		
	}
	@Override
	public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException {
		// TODO Auto-generated method stub
		
	}
}
