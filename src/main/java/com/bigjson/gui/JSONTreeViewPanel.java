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
	private JSONTreeViewData treeViewData;
	
	private JScrollPane scrollPane;
	private JTree treeView;
	private PopupMenu nodeMenu;
	private MouseListener nodeMenuMouseListener;
	private JLabel fileInfoField;
	private JTextArea nodeValueTextArea;
	private JButton loadFullStringBtn;
	
	private JDialog createSearchDialog(JSONNode node) {
		if (node == null){
			node = ((JSONTreeNode)treeViewData.getTreeModel().getRoot()).getJSONNode();
		}
		System.out.println("Open search dialog for node " + node.getName() + " (" + node.getStartFilePosition() + ", "
		+ node.getEndFilePosition() + ")");
		
		return new SearchDialog(null, node.getStartFilePosition(), node.getEndFilePosition() + 1, treeViewData);
		
		
//		JDialog searchDialog = new JDialog(mainFrame, "Search");
//		searchDialog.setLocation(400, 300);
//		Container cp = searchDialog.getContentPane();
//		// find panel
//		JPanel pFind = new JPanel();
//		pFind.setLayout(new BoxLayout(pFind, BoxLayout.Y_AXIS));
//		// TODO: remember previous searches 
//		JComboBox<StringSearchInfo> comboFind = new JComboBox<StringSearchInfo>(cachedSearches);
//		comboFind.addItemListener(e -> {
//			if(e.getStateChange() == ItemEvent.SELECTED){
//				StringSearchInfo selSearch = (StringSearchInfo)e.getItem();//(StringSearchInfo)((JComboBox)e.getSource()).getModel().getSelectedItem();
////				finalizeCurrentSearch();
//				currentSearch = selSearch;
//			}
//		});
//		
//		comboFind.setEditable(true);
//		comboFind.setMaximumSize(new Dimension(500, 30));
//		JLabel lFind = new JLabel("Find: ");
//		JLabel lFound = new JLabel("");
//		lFind.setAlignmentX(0);
//        pFind.add(lFind);
//        pFind.setAlignmentX(0);
//        pFind.add(comboFind);
//        pFind.add(lFound);
//        // options panel
//        JPanel pOptions = new JPanel(new GridLayout(2, 1));
//        pOptions.setBorder(BorderFactory.createTitledBorder("Options"));
//        JCheckBox caseSensChBox = new JCheckBox("Case sensitive");
//        JCheckBox altUnicodeChBox = new JCheckBox("Search for alternative non-ASCII encoding");
//        pOptions.add(caseSensChBox);
//        pOptions.add(altUnicodeChBox);
//        // buttons
//        JPanel pButtons = new JPanel(new FlowLayout(FlowLayout.CENTER));
//        JButton btnPrev = new JButton("Go To Previous Match");
//        btnPrev.setEnabled(false);
//        JButton btnFind = new JButton("Find");
//        JButton btnStart = new JButton("Start New Search");
//        btnStart.setEnabled(false);
//        JButton btnClose = new JButton("Close");
//        pButtons.add(btnPrev);
//        pButtons.add(btnFind);
//        pButtons.add(btnStart);
//        pButtons.add(btnClose);
//        
//        btnFind.addActionListener(e -> {
//        	if(currentSearch == null){
//        		caseSensChBox.setEnabled(false);
//            	altUnicodeChBox.setEnabled(false);
//            	comboFind.setEnabled(false);
////            	String toSearch = (String)comboFind.getSelectedItem();
//            	String toSearch = (String)comboFind.getEditor().getItem();
//            	startNewSearch(toSearch, node, caseSensChBox.isSelected(), altUnicodeChBox.isSelected());
//        	}
//        	btnStart.setEnabled(true);
//        	if(!findNextMatch()){
//        		btnFind.setEnabled(false);
//        		lFound.setText("No more matches found");
//        	} else {
//        		lFound.setText("Found a match at pos " + currentSearch.getLastMatchPos());
//        	}
//        	if(currentSearch.getLastMatchPos() >= 0){
//        		btnPrev.setEnabled(true);
//        	}
//        });
//        btnStart.addActionListener(e -> {
//        	caseSensChBox.setEnabled(true);
//        	altUnicodeChBox.setEnabled(true);
//        	comboFind.setEnabled(true);     
//        	btnPrev.setEnabled(false);
//        	btnFind.setEnabled(true);
//        	lFound.setText("");
//        	currentSearch = null;
//        });
//        btnClose.addActionListener(e -> {
//        	finalizeCurrentSearch();
//        	searchDialog.dispose();
//        });
//        // center panel: find + options
//        JPanel pCenter = new JPanel(new GridLayout(2, 1));
//        pCenter.add(pFind);
//        pCenter.add(pOptions);
//        // south panel: buttons
//        cp.add(pCenter, BorderLayout.CENTER);
//        cp.add(pButtons, BorderLayout.SOUTH);
//        searchDialog.pack();
//        
//        searchDialog.setDefaultCloseOperation(
//			    JDialog.DO_NOTHING_ON_CLOSE);
//        searchDialog.addWindowListener(new WindowAdapter() {
//			    public void windowClosing(WindowEvent e) {
//			    	finalizeCurrentSearch();
//			    	e.getWindow().dispose();
//			    }
//			});
//		return searchDialog;
	}
	
//	private void finalizeCurrentSearch(){
//		// TODO check is this search is already cached
//		cachedSearches.addElement(currentSearch);
//		currentSearch = null;
//	}

//	/**
//	 * Return true if search is not finished
//	 * @return
//	 */
//	private boolean findNextMatch(){
//		try{
//			backend.findNextMatch(currentSearch);
//		}catch(IOException ex){
//			showDialog("An IOException occured while searching: "+ex.getMessage());
//			return false;
//		}catch(IllegalFormatException ex){
//			showDialog("An IllegalFormatException occured while searching: "+ex.getMessage());
//			return false;
//		}
//		long pos = currentSearch.getLastMatchPos();
//		if(pos >= 0){
//			openLastNodeContainingPos(pos);
//		}
//		// TODO : open node with the match
//		return !currentSearch.searchIsFinished();
//	}
//	
	

	private void expandNode(JSONTreeNode treeNode) {
		TreePath path = new TreePath(treeNode.getPath());
		treeView.expandPath(path);
		treeView.setSelectionPath(path);
		treeView.scrollPathToVisible(path);
	}

	

	public JSONTreeViewPanel(File file, boolean validate) {
		super(new BorderLayout());
//		this.mainFrame = frame;
		init();
		treeViewData = new JSONTreeViewData(file, validate);
		initTreeModel();
		fileInfoField.setText(file.getName());
		fileInfoField.setToolTipText(file.getAbsolutePath());

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
				treeViewData.loadFullStringForNode(treeNode);
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
		treeView = new JTree(treeViewData.getTreeModel());
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
		treeViewData.loadChildrenForNode(parent);
		
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
	    		treeViewData.loadFullStringForNode(clickedNode);
	    		updateNodeValueTextArea(clickedNode);
	    	});
	        validateItem.addActionListener(e -> treeViewData.validateNode(clickedNode));
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
