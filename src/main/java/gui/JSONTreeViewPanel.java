package main.java.gui;

import java.awt.Dimension;
import java.io.IOException;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreeNode;

import main.java.parser.JSONInterface;
import main.java.parser.JSONNode;

/**
 * SWING panel for rendering a dynamically loaded JSON tree. Uses entities of
 * the <code>JSONTreeNode</code> class to represent tree nodes.
 * 
 * @author nikanka
 *
 */
public class JSONTreeViewPanel extends JPanel implements TreeWillExpandListener {
	DefaultTreeModel treeModel;
	JSONInterface backend;

	public static void main(String[] args) throws IOException {
		String fileName = "SmallTest2.json";

		JFrame frame = new JFrame("Tree View");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.add(new JSONTreeViewPanel(fileName));
		frame.pack();
		frame.setVisible(true);
	}

	public JSONTreeViewPanel(String fileName) throws IOException {
		super();
		backend = new JSONInterface(fileName);
		TreeNode root = new JSONTreeNode(backend.getRoot());

		treeModel = new DefaultTreeModel(root, true);
		JTree treeView = new JTree(treeModel);
		treeView.addTreeWillExpandListener(this);
	
		JScrollPane scrollPane = new JScrollPane(treeView);
		scrollPane.setPreferredSize(new Dimension(500, 400));
		add(scrollPane);
	}

	@Override
	public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {
		JSONTreeNode parent = (JSONTreeNode) event.getPath().getLastPathComponent();
		if (parent.isFullyLoaded()) {
			return;
		}
		List<JSONNode> children = backend.loadChildren(parent.getJSONNode().getFilePosition());
		for (JSONNode child : children) {
			treeModel.insertNodeInto(new JSONTreeNode(child), parent, parent.getChildCount());
		}
		parent.setIsFullyLoaded(true);
	}

	@Override
	public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException {
		// TODO Auto-generated method stub

	}

	/**
	 * A wrapper around a JSONNode object to render it in a JSONTreeViewPanel
	 * 
	 * @author nikanka
	 *
	 */
	private class JSONTreeNode extends DefaultMutableTreeNode {

		private boolean isFullyLoaded;
		private JSONNode node;

		private JSONTreeNode(JSONNode node) {
			// TODO: make empty arrays and objects leaves
			super(node.getNodeString(),
					node.getType() == JSONNode.TYPE_ARRAY || node.getType() == JSONNode.TYPE_OBJECT);
			this.node = node;
			this.isFullyLoaded = node.isFullyLoaded();
		}

		private boolean isFullyLoaded() {
			return isFullyLoaded;
		}

		private void setIsFullyLoaded(boolean newVal) {
			this.isFullyLoaded = newVal;
		}

		private JSONNode getJSONNode() {
			return node;
		}

	}
}
