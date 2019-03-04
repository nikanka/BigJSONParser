package com.bigjson.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
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
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreePath;

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
	private static String loadFileStr = "Load tree from JSON file";
	
	private DefaultTreeModel treeModel;
	private JSONLoader backend;
	
	private JScrollPane scrollPane;
	private JTree treeView;
	private PopupMenu nodeMenu;
	private MouseListener nodeMenuMouseListener;
	private JLabel fileInfoField;

	public static void main(String[] args) throws IOException {
//		String fileName = "testFiles/SmallTest2.json";
		createAndShowGUI();
//		frame.add(new JSONTreeViewPanel(fileName));
//		frame.pack();
//		frame.setVisible(true);
	}
	
	private static void createAndShowGUI() {
		JFrame frame = new JFrame("JSON Tree View");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
        //Create and set up the content pane
		JSONTreeViewPanel treeViewer = new JSONTreeViewPanel();
        frame.setJMenuBar(treeViewer.createMenuBar());
        frame.setContentPane(treeViewer);
 
        //Display the window.
        frame.setSize(1000, 800);
        frame.setVisible(true);
    }
	

	public JSONTreeViewPanel() {
		super(new BorderLayout());
		init();
	}
	private JMenuBar createMenuBar(){
		JMenuBar menuBar = new JMenuBar();
		JMenu menu = new JMenu("File");
		JMenuItem menuItem = new JMenuItem(loadFileStr);
		menu.add(menuItem);
		menuBar.add(menu);
		JFileChooser fileChooser = new JFileChooser("C:/Users/nikanka/JSONViewer/JSONFiles");
		menuItem.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				int returnVal = fileChooser.showOpenDialog(JSONTreeViewPanel.this);

		        if (returnVal == JFileChooser.APPROVE_OPTION) {
		            File file = fileChooser.getSelectedFile();
		            loadTreeFromFile(file);
		        } 
				
			}
		});
		return menuBar;
	}
	private void init(){
		// text field on top: which file is viewed
		JPanel topPanel = new JPanel();
		JLabel fileLabel = new JLabel("JSON file: ");
		fileInfoField = new JLabel("To choose a file go to File menu");
		//fileInfoField.setHorizontalAlignment(alignment);
		//fileInfoField.setEnabled(false);
		topPanel.add(fileLabel);
		topPanel.add(fileInfoField);
		add(topPanel, BorderLayout.NORTH);
		
		scrollPane = new JScrollPane();
		scrollPane.setPreferredSize(new Dimension(500, 400));
		add(scrollPane, BorderLayout.CENTER);
	
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
					if (!selNode.getJSONNode().isFullyLoaded() && selNode.getJSONNode().getType() == JSONNode.TYPE_STRING) {
						nodeMenu.setClickedNode(selNode);
						nodeMenu.show(e.getComponent(), e.getX(), e.getY());
					}
				}
			}

		};
	}

	private void loadTreeFromFile(File file){
		if(backend != null){
			try{
				backend.close();
			}catch(IOException e){
				showDialog("An IOException occured while closing the previous file reader: " + e.getMessage());
				return;
			}
		}
		try{
			backend = new JSONLoader(file.getAbsolutePath());
			JSONTreeNode rootNode = new JSONTreeNode(backend.getRoot());
			treeModel = new DefaultTreeModel(rootNode, true);
			treeView = new JTree(treeModel);
			treeView.collapseRow(0);
			loadChildrenForNode(rootNode);
		}catch(IOException e){
			showDialog("An IOException occured while reading new file: "+e.getMessage());
			return;
		}catch(IllegalFormatException e){
			showDialog("An IllegalFormatException occured while parsing the file: "+e.getMessage());
			return;
		}
		fileInfoField.setText(file.getName());
		fileInfoField.setToolTipText(file.getAbsolutePath());
		scrollPane.setViewportView(treeView);
		treeView.addTreeWillExpandListener(this);
		treeView.addMouseListener(nodeMenuMouseListener);
		
	}
	
	
	
	private void showDialog(String message){
		JOptionPane.showMessageDialog(null, message);
	}

	@Override
	public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {
		JSONTreeNode parent = (JSONTreeNode) event.getPath().getLastPathComponent();
//		System.out.println("Expanding node "+ parent);
//		if (parent.isFullyLoaded()) {
//			return;
//		}
		long loadTime = System.currentTimeMillis();
		try{
			//List<JSONNode> children = backend.loadChildren(parent.getJSONNode().getStartFilePosition());
			for(int i=0; i < parent.getChildCount(); i++){
			//for (JSONNode child : children) {
				JSONTreeNode childNode = (JSONTreeNode)parent.getChildAt(i);
				loadChildrenForNode(childNode);
//				List<JSONNode> grandChildren = backend.loadChildren(childNode.getJSONNode().getStartFilePosition());
//				//treeModel.insertNodeInto(childNode, parent, parent.getChildCount());
//				for(JSONNode grandChild: grandChildren){
//					treeModel.insertNodeInto(new JSONTreeNode(grandChild), childNode, childNode.getChildCount());
//				}
			}
		} catch(IOException e){
			showDialog("IOException occured while loading children nodes: " + e.getMessage());
		}catch(IllegalFormatException e){
			showDialog("IllegalFormatException occured while loading children nodes: " + e.getMessage());
		}
		//parent.setIsFullyLoaded(true);
		loadTime = System.currentTimeMillis() - loadTime;
		System.out.println("LoadTime for parent node " + parent + ": "+loadTime/1000 +" s");
	}

	private void loadChildrenForNode(JSONTreeNode parent) throws IOException, IllegalFormatException{
		if(!parent.getAllowsChildren() || parent.childrenAreLoaded()){
//			System.out.println("\t Node "+parent+" is not allower to have children or it's children are already loaded");
			return;
		}
		//System.out.println("\tLoading children for "+parent +" :\n");
		List<JSONNode> children = backend.loadChildren(parent.getJSONNode().getStartFilePosition());
		//treeModel.insertNodeInto(childNode, parent, parent.getChildCount());
		for(JSONNode child: children){
			//System.out.println("\t\t"+child.getName()+" : "+child.getValue());
			treeModel.insertNodeInto(new JSONTreeNode(child), parent, parent.getChildCount());
		}
		parent.setChildrenAreLoaded(true);
//		System.out.println("Parent is "+treeView.isExpanded(parent.getLevel())+" expanded");
	}
	
	@Override
	public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException {
		// do nothing
	}

	private class PopupMenu extends JPopupMenu implements ActionListener{
		JSONTreeNode clickedNode = null;
	    public PopupMenu(){
	    	JMenuItem loadStr = new JMenuItem("Load Full String");
	    	loadStr.addActionListener(this);
	        this.add(loadStr);
	    }
	    
	    void setClickedNode(JSONTreeNode node){
	    	this.clickedNode = node;
	    }
		@Override
		public void actionPerformed(ActionEvent e) {
			String str = clickedNode.loadFullString();
			System.out.println(str);
		}
	}
	/**
	 * A wrapper around a JSONNode object to render it in a JSONTreeViewPanel
	 * 
	 * @author nikanka
	 *
	 */
	private class JSONTreeNode extends DefaultMutableTreeNode {

		private boolean childrenAreLoaded = false;
		private JSONNode node;
		private DecimalFormat decimalFormat = new DecimalFormat("#,###,###,##0.00" );
		

		private JSONTreeNode(JSONNode node) {
			super();//getNodeStringWithSize(node),//getNodeStringWithSize(),
					//node.getType() == JSONNode.TYPE_ARRAY || node.getType() == JSONNode.TYPE_OBJECT);
//			this.node = node;// TODO
			this.setUserObject(node);
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
				sb.append(node.getValue());
				if(node.getType() == JSONNode.TYPE_STRING){
					if(!node.isFullyLoaded()){
						sb.append("...");
					}
					sb.append("\"");
				}
			}
			if(node.getType() == JSONNode.TYPE_ARRAY || node.getType() == JSONNode.TYPE_OBJECT ||
					(node.getType() == JSONNode.TYPE_STRING && !node.isFullyLoaded())){
				sb.append("  [");
				sb.append(decimalFormat.format((node.getEndFilePosition() - node.getStartFilePosition())/1024.));
				sb.append("Kb]");
				
			}
			return sb.toString();
		}
		private boolean childrenAreLoaded() {
			return childrenAreLoaded;
		}

		private void setChildrenAreLoaded(boolean newVal) {
			this.childrenAreLoaded = newVal;
		}
		@Override
		public void setUserObject(Object userObject) {
			if(!(userObject instanceof JSONNode)){
				throw new IllegalArgumentException("userObject should be an instance of JSONNode class");
			}
			this.node = (JSONNode)userObject;
			super.setUserObject(createNodeString());
		}
		private String loadFullString(){
			String str = null;
			try {
				node = backend.loadNodeWithFullString(node);
				setUserObject(node);
				treeModel.nodeChanged(this);
			} catch (IOException e) {
				showDialog(
						"IOException occured while loading full string for " + getUserObject() + ": " + e.getMessage());

			} catch (IllegalFormatException e) {
				showDialog("IllegalFormatException occured while loading full string for " + getUserObject() + ": "
						+ e.getMessage());
			}
			return str;
		}

		private JSONNode getJSONNode() {
			return node;
		}
	}
}
