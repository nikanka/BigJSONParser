package com.bigjson.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import com.bigjson.parser.JSONNode;

/**
 * SWING panel for rendering a dynamically loaded JSON tree. Uses entities of
 * the <code>JSONTreeNode</code> class to represent tree nodes.
 * 
 * @author nikanka
 *
 */
@SuppressWarnings("serial")
public class JSONTreeViewPanel extends JPanel implements TreeWillExpandListener{	
	
	private JSONTreeViewModel treeViewModel;
	
	private JScrollPane scrollPane;
	private JTree treeView;
	private PopupMenu nodeMenu;
	private MouseListener nodeMenuMouseListener;
	private JLabel fileInfoField;
	private JTextArea nodeValueTextArea;
	private JButton loadFullStringBtn;
	
	private JDialog createSearchDialog(JSONNode node) {
		if (node == null){
			node = ((JSONTreeNode)treeViewModel.getTreeModel().getRoot()).getJSONNode();
		}
		System.out.println("Open search dialog for node " + node.getName() + " (" + node.getStartFilePosition() + ", "
		+ node.getEndFilePosition() + ")");
		
		return new SearchDialog(this, node.getStartFilePosition(), node.getEndFilePosition(), treeViewModel);
	}
	
	public void expandNode(JSONTreeNode treeNode) {
		TreePath path = new TreePath(treeNode.getPath());
		treeView.expandPath(path);
		treeView.setSelectionPath(path);
		treeView.scrollPathToVisible(path);
	}
	
	public void expandLastNodeContainingPos(long pos){
		expandNode(treeViewModel.openLastNodeContainingPos(pos));
	}

	public JSONTreeViewPanel(File file, boolean validate) {
		super(new BorderLayout());
		init();
		treeViewModel = new JSONTreeViewModel(file, validate);
		initTreeModel();
		fileInfoField.setText(file.getName());
		fileInfoField.setToolTipText(file.getAbsolutePath());

	}
	
	public JSONTreeViewModel getTreeViewModel(){
		return treeViewModel;
	}

	private void init(){
		// text field on top: which file is viewed
		JPanel topPanel = new JPanel();
		JLabel fileLabel = new JLabel("JSON file: ");
		fileInfoField = new JLabel("To choose a file go to File menu");
		topPanel.add(fileLabel);
		topPanel.add(fileInfoField);
		add(topPanel, BorderLayout.NORTH);
		
		// center area: render a tree
		scrollPane = new JScrollPane();
		scrollPane.setPreferredSize(new Dimension(500, 400));
		
		// bottom panel: view node value
		// Create a text pane.
        nodeValueTextArea = new JTextArea();
        nodeValueTextArea.setEditable(false);
        nodeValueTextArea.setLineWrap(true);
        JScrollPane nodeValueScrollPane = new JScrollPane(nodeValueTextArea);
        nodeValueScrollPane.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Node value"),
                BorderFactory.createEmptyBorder(5,5,5,5)));
        nodeValueScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        nodeValueScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        JPanel nodeValuePanel = new JPanel();
        nodeValuePanel.setLayout(new BorderLayout());
        loadFullStringBtn = new JButton("Load Full String");
		loadFullStringBtn.addActionListener(e -> {
			TreePath selPath = treeView.getSelectionPath();
			JSONTreeNode treeNode = (JSONTreeNode) selPath.getLastPathComponent();
			if (treeNode != null) {
				treeViewModel.loadFullStringForNode(treeNode);
				updateNodeValueTextArea(treeNode);
			}
		});
		loadFullStringBtn.setEnabled(false);
        JPanel borderPanel = new JPanel(new BorderLayout());
		borderPanel.setBorder(BorderFactory.createEmptyBorder(8, 5, 2, 5));
		nodeValuePanel.add(nodeValueScrollPane, BorderLayout.CENTER);
		borderPanel.add(loadFullStringBtn, BorderLayout.CENTER);
		nodeValuePanel.add(borderPanel, BorderLayout.EAST);
		JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scrollPane, nodeValuePanel);
		splitPane.setDividerLocation(0.9);
        splitPane.setResizeWeight(1.0);
        add(splitPane, BorderLayout.CENTER);
		
		// pop-up menu for nodes in the tree
		nodeMenu = new PopupMenu();
		nodeMenuMouseListener = new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if (!SwingUtilities.isRightMouseButton(e)) {
					return;
				}
				TreePath selPath = treeView.getPathForLocation(e.getX(), e.getY());
				if (selPath == null) {
					return;
				}
				treeView.setSelectionPath(selPath);
				Object selComp = selPath.getLastPathComponent();
				if (selComp instanceof JSONTreeNode) {
					JSONTreeNode selNode = (JSONTreeNode) selComp;
					nodeMenu.setClickedNode(selNode);
					nodeMenu.show(e.getComponent(), e.getX(), e.getY());
				}
			}

		};
	}

	private void initTreeModel(){
		treeView = new JTree(treeViewModel.getTreeModel());
		treeView.collapseRow(0);
		treeView.addTreeWillExpandListener(this);
		treeView.addMouseListener(nodeMenuMouseListener);
		treeView.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		treeView.addTreeSelectionListener((TreeSelectionEvent e) -> updateNodeValueTextArea(
				(JSONTreeNode) treeView.getLastSelectedPathComponent()));
		scrollPane.setViewportView(treeView);	
	}
	
	private void updateNodeValueTextArea(JSONTreeNode treeNode){
		if (treeNode == null || !treeNode.getJSONNode().isLeaf()) {
			nodeValueTextArea.setText("");
			loadFullStringBtn.setEnabled(false);
			return;
		}
		JSONNode node = treeNode.getJSONNode();
		nodeValueTextArea.setText(node.getValue() + (node.isFullyLoaded() ? "" : "..."));
		loadFullStringBtn.setEnabled(!node.isFullyLoaded());		
	}

	@Override
	public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {
		JSONTreeNode parent = (JSONTreeNode) event.getPath().getLastPathComponent();
		treeViewModel.loadChildrenForNode(parent);
		
	}
	
	@Override
	public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException {
		// do nothing
	}

	private class PopupMenu extends JPopupMenu{
		private JSONTreeNode clickedNode = null;
		private JMenuItem loadStrItem;
		private JMenuItem searchItem;
		private JMenuItem validateItem;
		
		public PopupMenu(){
	    	loadStrItem = new JMenuItem("Load Full String");
	    	searchItem = new JMenuItem("Search...");
	    	validateItem = new JMenuItem("Validate");
	    	
	    	searchItem.addActionListener(e -> {
	    		// open search dialog
	    		JDialog searchDialog = createSearchDialog(clickedNode.getJSONNode());
	    	    searchDialog.setVisible(true);
	    	});
	    	loadStrItem.addActionListener(e -> {
	    		treeViewModel.loadFullStringForNode(clickedNode);
	    		updateNodeValueTextArea(clickedNode);
	    	});
	        validateItem.addActionListener(e -> treeViewModel.validateNode(clickedNode));
	    	this.add(loadStrItem);
	    	this.add(searchItem);
	    	this.add(validateItem);
	    }
	    
	    void setClickedNode(JSONTreeNode node){
	    	this.clickedNode = node;
	    	loadStrItem.setVisible(!clickedNode.getJSONNode().isFullyLoaded() && clickedNode.getJSONNode().getType() == JSONNode.TYPE_STRING);
	    }
	}
	
}
