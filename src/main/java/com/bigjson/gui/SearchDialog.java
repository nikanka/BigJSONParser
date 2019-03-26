package com.bigjson.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.BasicComboBoxEditor;

import com.bigjson.parser.StringSearchInfo;

public class SearchDialog extends JDialog{
	private SearchModel searchModel;
	
	public SearchDialog(JSONTreeViewPanel treePanel, long from, long to, JSONTreeViewModel treeViewModel){
		super(SwingUtilities.getWindowAncestor(treePanel), "Search");
		this.searchModel = new SearchModel(treePanel, from, to);
		init();
	}

	private void init() {
		setLocation(400, 300);
		Container cp = getContentPane();
		// find panel
		JPanel pFind = new JPanel();
		pFind.setLayout(new GridLayout(3, 1));
		JComboBox<StringSearchInfo> comboFind = new JComboBox<StringSearchInfo>(searchModel.getCachedSearches());
		comboFind.setRenderer(new SearchCellRenderer());
		comboFind.setEditor(new SearchCellEditor());
		comboFind.setEditable(true);
		comboFind.setMaximumSize(new Dimension(500, 30));
		JLabel lFind = new JLabel("Find: ");
		JLabel lFound = new JLabel("");
        pFind.add(lFind);
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
        JButton btnPrev = new JButton("Previous Match");
        btnPrev.setEnabled(false);
        JButton btnNext = new JButton("Next Match");
        JButton btnStart = new JButton("Start New Search");
        btnStart.setEnabled(false);
        JButton btnClose = new JButton("Close");
        pButtons.add(btnPrev);
        pButtons.add(btnNext);
        pButtons.add(btnStart);
        pButtons.add(btnClose);
        // center panel: find + options
        JPanel pCenter = new JPanel(new GridLayout(2, 1));
        pCenter.add(pFind);
        pCenter.add(pOptions);
        // south panel: buttons
        cp.add(pCenter, BorderLayout.NORTH);
        cp.add(pButtons, BorderLayout.CENTER);
        pack();
        
        btnNext.addActionListener(e -> {
        	if(searchModel.getCurrentSearch() == null){
        		String toSearch = (String)comboFind.getEditor().getItem();
        		if(toSearch.length() == 0){
        			return;
        		}
        		searchModel.setNewSearch(toSearch, caseSensChBox.isSelected(), altUnicodeChBox.isSelected());
        		comboFind.setEnabled(false);
        		btnStart.setEnabled(true);
        	}
        	if(!searchModel.goToTheNextMatch()){
        		lFound.setText("No more matches found");
        	} else {
            	lFound.setText("Found a match at pos " + searchModel.getCurrentMatchPos());
            	btnPrev.setEnabled(true);
        	}
        	if(!searchModel.mayHaveNextMatch()){
        		btnNext.setEnabled(false);
        	} 
        });
        btnPrev.addActionListener(e -> {
        	System.out.println("Prev, selected: " + comboFind.getSelectedIndex());
        	searchModel.goToThePrevMatch();
        	if(!searchModel.hasPreviousMatch()){
        		btnPrev.setEnabled(false);
        	} else {
        		lFound.setText("Found a match at pos " + searchModel.getCurrentMatchPos());
        	}
        });
        btnStart.addActionListener(e -> {
        	caseSensChBox.setEnabled(true);
        	altUnicodeChBox.setEnabled(true);
        	comboFind.setEnabled(true);     
        	btnPrev.setEnabled(false);
        	btnNext.setEnabled(true);
        	lFound.setText("");
        	searchModel.resetCurrentSearch();
        });
        btnClose.addActionListener(e -> {
        	dispose();
        });
        
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
	        if (value instanceof StringSearchInfo) {
	            value = ((StringSearchInfo)value).getStringToSearch();
	        }
	        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
	        return this;
	    }
	}
	private class SearchCellEditor extends BasicComboBoxEditor {
		public void setItem(Object anObject) {
			if (anObject instanceof StringSearchInfo) {
				anObject = ((StringSearchInfo) anObject).getStringToSearch();
			}
			super.setItem(anObject);
		}
		
		protected JTextField createEditorComponent() {
	        JTextField editor = new JTextField();
	        return editor;
	    }
	}
}
