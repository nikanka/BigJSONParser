package com.bigjson.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.JTree;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EtchedBorder;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import com.bigjson.parser.IllegalFormatException;
import com.bigjson.parser.JSONLoader;
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
//	private static final String loadFileStr = "Load tree from JSON file";
	private static final int stringDisplayLength = 100;
	private static final String defaultFileFolder = "C:/Users/nikanka/JSONViewer/JSONFiles";
	
	private DefaultTreeModel treeModel;
	private JSONLoader backend;
	
	private JScrollPane scrollPane;
	private JTree treeView;
	private PopupMenu nodeMenu;
	private MouseListener nodeMenuMouseListener;
	private JLabel fileInfoField;
	private JTextArea nodeValueTextArea;
	private JButton loadFullStringBtn;

	public static void main(String[] args) throws IOException {
		createAndShowGUI();
	}
	
	private static void createAndShowGUI() {
		JFrame frame = new JFrame("JSON Tree View");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
        //Create and set up the content pane
		JSONTreeViewPanel treeViewer = new JSONTreeViewPanel();
		// TODO make several tabs so can load several files 
        frame.setJMenuBar(treeViewer.createMenuBar());
        frame.setContentPane(treeViewer);
 
        //Display the window.
        frame.setSize(1000, 800);
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH); 
        frame.setVisible(true);
    }
	

	public JSONTreeViewPanel() {
		super(new BorderLayout());
		init();
	}
	private JMenuBar createMenuBar(){
		JMenuBar menuBar = new JMenuBar();
		JMenu fileMenu = new JMenu("File");
		JMenu loadFileMenu = new JMenu("Load tree from JSON file");
		JMenuItem justLoadItem = new JMenuItem("Just load");
		JMenuItem validateItem = new JMenuItem("Load and vaidate (can take more time)");
		loadFileMenu.add(justLoadItem);
		loadFileMenu.add(validateItem);
		fileMenu.add(loadFileMenu);
		menuBar.add(fileMenu);
		JFileChooser fileChooser = new JFileChooser(defaultFileFolder);
		justLoadItem.addActionListener(e -> {
			int returnVal = fileChooser.showOpenDialog(JSONTreeViewPanel.this);
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				File file = fileChooser.getSelectedFile();
				loadTreeFromFile(file, false);
			}
		});
		validateItem.addActionListener(e -> {
			int returnVal = fileChooser.showOpenDialog(JSONTreeViewPanel.this);
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				File file = fileChooser.getSelectedFile();
				loadTreeFromFile(file, true);
			}
		});
		return menuBar;
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
			JSONTreeNode treeNode = (JSONTreeNode) treeView.getLastSelectedPathComponent();
			if (treeNode != null) {
				loadFullStringForNode(treeNode);
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
				Object selComp = selPath.getLastPathComponent();
				if (selComp instanceof JSONTreeNode) {
					JSONTreeNode selNode = (JSONTreeNode) selComp;
//					if (!selNode.getJSONNode().isFullyLoaded() && selNode.getJSONNode().getType() == JSONNode.TYPE_STRING) {
						nodeMenu.setClickedNode(selNode);
						nodeMenu.show(e.getComponent(), e.getX(), e.getY());
//					}
				}
			}

		};
	}

	private void loadTreeFromFile(File file, boolean validate){
		// close previous loader
		if(backend != null){
			try{
				backend.close();
			}catch(IOException e){
				showDialog("An IOException occured while closing the previous file reader: " + e.getMessage());
				return;
			}
		}
		// create a loader for a given file
		try{
			backend = new JSONLoader(file, stringDisplayLength);
			long t1 = System.currentTimeMillis();
			JSONTreeNode rootNode = new JSONTreeNode(validate ? backend.getRootAndValidate() : backend.getRoot());
			System.out.println("Load root and children: "+(System.currentTimeMillis() - t1)/1000 + " s");
//			treeModel = new DefaultTreeModel(rootNode, true);
			initTreeModel(rootNode);
			loadChildrenForNode(rootNode);
		}catch(IOException e){
			showDialog("An IOException occured while reading new file: "+e.getMessage());
			return;
		}catch(IllegalFormatException e){
			showDialog("An IllegalFormatException occured while loading the file: "+e.getMessage());
			return;
		}
		fileInfoField.setText(file.getName());
		fileInfoField.setToolTipText(file.getAbsolutePath());
		if(validate){
			showDialog("File " + file.getName() + " has been successfully validated");
		}
	}
	
	private void initTreeModel(JSONTreeNode root){
		treeModel = new DefaultTreeModel(root, true);
		treeView = new JTree(treeModel);
		treeView.collapseRow(0);
		treeView.addTreeWillExpandListener(this);
		treeView.addMouseListener(nodeMenuMouseListener);
		treeView.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		treeView.addTreeSelectionListener((TreeSelectionEvent e) -> {
			JSONTreeNode treeNode = (JSONTreeNode) treeView.getLastSelectedPathComponent();
			if (treeNode == null || !treeNode.getJSONNode().isLeaf()) {
				nodeValueTextArea.setText("");
				loadFullStringBtn.setEnabled(false);
				return;
			}
			JSONNode node = treeNode.getJSONNode();
			nodeValueTextArea.setText(node.getValue() + (node.isFullyLoaded() ? "" : "..."));
			loadFullStringBtn.setEnabled(!node.isFullyLoaded());
		});
		scrollPane.setViewportView(treeView);	
	}
	
	
	
	private void showDialog(String message){
		JOptionPane.showMessageDialog(null, message);
	}

	@Override
	public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {
		JSONTreeNode parent = (JSONTreeNode) event.getPath().getLastPathComponent();
		try{
			loadChildrenForNode(parent);
		} catch(IOException e){
			showDialog("IOException occured while loading children nodes: " + e.getMessage());
		}catch(IllegalFormatException e){
			showDialog("IllegalFormatException occured while loading children nodes: " + e.getMessage());
		}
	}

	private void loadChildrenForNode(JSONTreeNode parent) throws IOException, IllegalFormatException{
		if(!parent.getAllowsChildren() || parent.childrenAreLoaded()){
			return;
		}
		long time = System.currentTimeMillis();
		List<JSONNode> children = backend.loadChildren(parent.getJSONNode());
		for(JSONNode child: children){
			treeModel.insertNodeInto(new JSONTreeNode(child), parent, parent.getChildCount());
		}
		parent.setChildrenAreLoaded(true);
		System.out.println("Load children for node " + parent + ": " + 
							(System.currentTimeMillis() - time) / 1000 + " s");
	}
	
	@Override
	public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException {
		// do nothing
	}
	
	private String loadFullStringForNode(JSONTreeNode treeNode){
		JSONNode node = treeNode.getJSONNode();
		try {
			node = backend.loadNodeWithFullString(node);	
		} catch (IOException e) {
			showDialog(
					"IOException occured while loading full string for " + treeNode.getUserObject() + ": " + e.getMessage());

		} catch (IllegalFormatException e) {
			showDialog("IllegalFormatException occured while loading full string for " + treeNode.getUserObject() + ": "
					+ e.getMessage());
		}
		String str = node.getValue();
		treeNode.setJSONNode(node);
		treeModel.nodeChanged(treeNode);
		nodeValueTextArea.setText(str);
		loadFullStringBtn.setEnabled(false);
		return str;
	}

	private class PopupMenu extends JPopupMenu implements ActionListener{
		private JSONTreeNode clickedNode = null;
		private JMenuItem loadStrItem;
		private JMenuItem validateItem;
	    public PopupMenu(){
	    	validateItem = new JMenuItem("Validate");
	    	loadStrItem = new JMenuItem("Load Full String");
			validateItem.addActionListener(e -> {
				try {
					IllegalFormatException ex = backend.validateNode(clickedNode.getJSONNode());
					if (ex != null) {
						showDialog("A problem with the node format was detected: " + ex.getMessage());
					} else {
						showDialog("The format of the node was succesfully validated.");
					}
				} catch (Exception ex) {
					showDialog(ex.getClass() + " occured while validating the node: " + ex.getMessage());
				}
			});
	    	loadStrItem.addActionListener(e -> loadFullStringForNode(clickedNode));
	        this.add(validateItem);
	        this.add(loadStrItem);
	    }
	    
	    void setClickedNode(JSONTreeNode node){
	    	this.clickedNode = node;
	    	loadStrItem.setVisible(!clickedNode.getJSONNode().isFullyLoaded() && clickedNode.getJSONNode().getType() == JSONNode.TYPE_STRING);
	    }
		@Override
		public void actionPerformed(ActionEvent e) {
			loadFullStringForNode(clickedNode);
		}
	}
	
//	private class NodeChangedListener implements TreeModelListener{
//
//		@Override
//		public void treeNodesChanged(TreeModelEvent e) {
//			TreePath selectedNode = 
//                    treeView.getSelectionModel().getSelectionPath();//getLastSelectedPathComponent();
//			if(selectedNode == null){
//				System.out.println("Nothing is selected");
//				return;
//			}
//			
//			JSONTreeNode changedNode = (JSONTreeNode)e.getTreePath().getPathComponent(index);
//			if(changedNode == selectedNode){
//				
//			}
//			
//			
//		}
//
//		public void treeNodesInserted(TreeModelEvent e) {
//		}
//
//		public void treeNodesRemoved(TreeModelEvent e) {
//		}
//
//		public void treeStructureChanged(TreeModelEvent e) {
//		}
//		
//	}
	/**
	 * A wrapper around a JSONNode object to render it in a JSONTreeViewPanel
	 * 
	 * @author nikanka
	 *
	 */
	private class JSONTreeNode extends DefaultMutableTreeNode {

		private boolean childrenAreLoaded = false;
		private JSONNode node;
		private DecimalFormat decimalFormat = new DecimalFormat("#,###,###,##0.0" );
		private DecimalFormat intFormat = new DecimalFormat("#,###,###,###" );
		

		private JSONTreeNode(JSONNode node) {
			super();//getNodeStringWithSize(node),//getNodeStringWithSize(),
					//node.getType() == JSONNode.TYPE_ARRAY || node.getType() == JSONNode.TYPE_OBJECT);
//			this.node = node;// TODO
			this.setJSONNode(node);
			this.setAllowsChildren(node.getType() == JSONNode.TYPE_ARRAY || node.getType() == JSONNode.TYPE_OBJECT);
		}

//		private String getNodeStringWithSize(){
//			if(node.isFullyLoaded()){
//				return node.getNodeString();
//			}
//			StringBuilder sb = new StringBuilder(node.getNodeString());
//			sb.append("  [");
//			sb.append(decimalFormat.format((node.getEndFilePosition() - node.getStartFilePosition())/1024.));
//			sb.append("Kb]");
//			return sb.toString();
//		}
		
		private String createNodeString(){
			StringBuilder sb = new StringBuilder((node.getName()==null?0:node.getName().length()) 
					+ (node.getValue()==null?0:node.getValue().length()) + 40);
			if(node.getName() != null){
				sb.append(node.getName());
			}
			if(node.getValue() != null){
				if(node.getName() != null){
					sb.append(" : ");
				}
				if(node.getType() == JSONNode.TYPE_STRING){
					sb.append("\"");
				}
				String val = node.getValue(); 
				if(node.getType() == JSONNode.TYPE_STRING && 
						(!node.isFullyLoaded() || val.length() > stringDisplayLength)){
					sb.append(val.substring(0, stringDisplayLength));
					sb.append("...");
				}else {
					sb.append(val);
				}
				if(node.getType() == JSONNode.TYPE_STRING){
					sb.append("\"");
				}	
			}
			if(node.getType() == JSONNode.TYPE_ARRAY || node.getType() == JSONNode.TYPE_OBJECT ||
					(node.getType() == JSONNode.TYPE_STRING && !node.isFullyLoaded())){
				sb.append("  [");
				sb.append(decimalFormat.format((node.getEndFilePosition() - node.getStartFilePosition())/1024.));
				sb.append("Kb]");	
			}
			if(node.getType() == JSONNode.TYPE_STRING && node.getValue().length() > stringDisplayLength){
				sb.append("  [length = ");
				sb.append(intFormat.format(node.getValue().length()));
				sb.append("]");
			}
			return sb.toString();
		}
		private boolean childrenAreLoaded() {
			return childrenAreLoaded;
		}

		private void setChildrenAreLoaded(boolean newVal) {
			this.childrenAreLoaded = newVal;
		}
		public void setJSONNode(JSONNode node) {
			this.node = node;
			super.setUserObject(createNodeString());
		}
		

		private JSONNode getJSONNode() {
			return node;
		}
	}
}
