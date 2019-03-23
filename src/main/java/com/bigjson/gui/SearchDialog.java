package com.bigjson.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ItemEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.plaf.basic.BasicComboBoxEditor;

import com.bigjson.parser.JSONNode;
import com.bigjson.parser.StringSearchInfo;

public class SearchDialog extends JDialog{
//	private DefaultComboBoxModel<StringSearchInfo> cachedSearches;
	private JSONTreeViewData treeViewData;
	private long from;
	private long to;
	public SearchDialog(Frame parent, long from, long to, JSONTreeViewData treeViewData){
		super(parent, "Search");
		this.treeViewData = treeViewData;
		this.from = from;
		this.to = to;
		init();
	}

	private void init() {
		setLocation(400, 300);
		Container cp = getContentPane();
		// find panel
		JPanel pFind = new JPanel();
		pFind.setLayout(new BoxLayout(pFind, BoxLayout.Y_AXIS));
		// TODO: remember previous searches 
		JComboBox<StringSearchInfo> comboFind = new JComboBox<StringSearchInfo>(treeViewData.getCashedSearches());
		comboFind.setRenderer(new SearchCellRenderer());
		comboFind.setEditor(new SearchCellEditor());
		comboFind.addItemListener(e -> {
			if(e.getStateChange() == ItemEvent.SELECTED){
				System.out.println("Selected " + e.getItem().getClass());
				System.out.println("Editor: " + comboFind.getEditor().getItem());
				if(e.getItem() instanceof StringSearchInfo){
					
				}
//				cachedSearches.addElement((StringSearchInfo)e.getItem());
//				StringSearchInfo selSearch = (StringSearchInfo)e.getItem();//(StringSearchInfo)((JComboBox)e.getSource()).getModel().getSelectedItem();
//				finalizeCurrentSearch();
//				currentSearch = selSearch;
			}
		});
		
		comboFind.setEditable(true);
		comboFind.setMaximumSize(new Dimension(500, 30));
		JLabel lFind = new JLabel("Find: ");
		JLabel lFound = new JLabel("");
		lFind.setAlignmentX(0);
        pFind.add(lFind);
        pFind.setAlignmentX(0);
        pFind.add(comboFind);
        pFind.add(lFound);
        // options panel
        JPanel pOptions = new JPanel(new GridLayout(2, 1));
        pOptions.setBorder(BorderFactory.createTitledBorder("Options"));
        JCheckBox caseSensChBox = new JCheckBox("Case sensitive");
        JCheckBox altUnicodeChBox = new JCheckBox("Search for alternative non-ASCII encoding");
        pOptions.add(caseSensChBox);
        pOptions.add(altUnicodeChBox);
        // buttons
        JPanel pButtons = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton btnPrev = new JButton("Go To Previous Match");
        btnPrev.setEnabled(false);
        JButton btnFind = new JButton("Find");
        JButton btnStart = new JButton("Start New Search");
        btnStart.setEnabled(false);
        JButton btnClose = new JButton("Close");
        pButtons.add(btnPrev);
        pButtons.add(btnFind);
        pButtons.add(btnStart);
        pButtons.add(btnClose);
        
        btnFind.addActionListener(e -> {
        	System.out.println("FIND, selected: " + comboFind.getSelectedIndex());
        	if(comboFind.getSelectedIndex() < 0){
        		treeViewData.createNewSearch((String)comboFind.getEditor().getItem(), from, to, caseSensChBox.isSelected(), altUnicodeChBox.isSelected());
        	}
//        	System.out.println("FIND, selected item: " + cachedSearches.getSelectedItem());
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
        });
        btnStart.addActionListener(e -> {
        	caseSensChBox.setEnabled(true);
        	altUnicodeChBox.setEnabled(true);
        	comboFind.setEnabled(true);     
        	btnPrev.setEnabled(false);
        	btnFind.setEnabled(true);
        	lFound.setText("");
        	comboFind.setSelectedIndex(-1);
//        	currentSearch = null;
        });
        btnClose.addActionListener(e -> {
//        	finalizeCurrentSearch();
//        	searchDialog.dispose();
        });
        // center panel: find + options
        JPanel pCenter = new JPanel(new GridLayout(2, 1));
        pCenter.add(pFind);
        pCenter.add(pOptions);
        // south panel: buttons
        cp.add(pCenter, BorderLayout.CENTER);
        cp.add(pButtons, BorderLayout.SOUTH);
        
        pack();
        
        setDefaultCloseOperation(
			    JDialog.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
			    public void windowClosing(WindowEvent e) {
//			    	finalizeCurrentSearch();
			    	e.getWindow().dispose();
			    }
			});		
	}
	
	private class SearchCellRenderer extends DefaultListCellRenderer {
	    public Component getListCellRendererComponent(
	                                   JList list,
	                                   Object value,
	                                   int index,
	                                   boolean isSelected,
	                                   boolean cellHasFocus) {
	        if (value instanceof NodeSearchInfo) {
	            value = ((NodeSearchInfo)value).getStringToSearch();
	        }
	        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
	        return this;
	    }
	}
	private class SearchCellEditor extends BasicComboBoxEditor {
		public void setItem(Object anObject) {
			if (anObject instanceof NodeSearchInfo) {
				anObject = ((NodeSearchInfo) anObject).getStringToSearch();
			}
			super.setItem(anObject);
		}
		
		protected JTextField createEditorComponent() {
	        JTextField editor = new JTextField();
	        return editor;
	    }
	}
}
