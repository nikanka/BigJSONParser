package com.bigjson.gui;

import java.io.IOException;
import java.util.List;

import javax.swing.DefaultComboBoxModel;

import com.bigjson.parser.IllegalFormatException;
import com.bigjson.parser.JSONLoader;
import com.bigjson.parser.StringSearchInfo;

public class SearchModel {
	private JSONTreeViewPanel treeViewPanel;
	private JSONLoader loader;
	private long from = -1;
	private long to = -1;
	private StringSearchInfo currentSearch = null;
	private int curMatchInd = -1;
	private DefaultComboBoxModel<StringSearchInfo> cachedSearches = new DefaultComboBoxModel<StringSearchInfo>();

	public SearchModel(JSONTreeViewPanel treeViewPanel, long from, long to){
		this.treeViewPanel = treeViewPanel;
		this.loader = treeViewPanel.getTreeViewModel().getJSONLoader();
		this.from = from;
		this.to = to;
	}
	
	/**
	 * Check whether cached searches contain search with the same parameters. If
	 * yes, set it as a current search, else create a new search and set it as a current
	 * search.
	 * 
	 * @param toSearch
	 * @param caseSensitive
	 * @param searchForAltUnicode
	 * @return
	 */
	public void setNewSearch(String toSearch, boolean caseSensitive, boolean searchForAltUnicode){
		StringSearchInfo search = loader.createNewSearch(toSearch, from, to, caseSensitive, searchForAltUnicode);
		int ind = cachedSearches.getIndexOf(search);
		if(ind < 0){
			System.out.println("Search created");
			cachedSearches.addElement(search);
		} else {
			search = cachedSearches.getElementAt(ind);
			System.out.println("This search is cached");
		}
		setNewSearch(search);
//		return currentSearch;
	}

	public void setNewSearch(StringSearchInfo search){
		cachedSearches.setSelectedItem(search);
		currentSearch = search;
		curMatchInd = -1;
	}
	
	private boolean findNextMatch(){
		try{
			return loader.findNextMatch(currentSearch);
		}catch(IOException ex){
			MainFrame.showDialog("An IOException occured while searching: "+ex.getMessage());
			return false;
		}catch(IllegalFormatException ex){
			MainFrame.showDialog("An IllegalFormatException occured while searching: "+ex.getMessage());
			return false;
		}
	}

	
	/**
	 * @return true if successfully moved to the next match
	 * @throws RuntimeException
	 *             if the current search is finished and the current match is
	 *             the last one for this search
	 */
	public boolean goToTheNextMatch(){
		List<Long> matches = currentSearch.getMatchPositions(); 
		if(curMatchInd + 1 == matches.size()){
			if(currentSearch.isFinished() || !findNextMatch()){
				return false;
			}
		} 
		curMatchInd++;
		treeViewPanel.expandLastNodeContainingPos(matches.get(curMatchInd));
		return true;
	}
	/**
	 * throw RuntimeException if current match index is 0. 
	 */
	public void goToThePrevMatch() {
		if (curMatchInd == 0) {
			throw new RuntimeException(
					"Current match is the first one for this search: cannot go to the previous match");
		}
		curMatchInd--;
		treeViewPanel.expandLastNodeContainingPos(currentSearch.getMatchPositions().get(curMatchInd));
	}

	public boolean mayHaveNextMatch(){
		return curMatchInd < currentSearch.getMatchPositions().size() - 1 || !currentSearch.isFinished();
	}
	
	public boolean hasPreviousMatch(){
		return curMatchInd > 0;
	}
	public DefaultComboBoxModel<StringSearchInfo> getCachedSearches() {
		return cachedSearches;
	}

	public StringSearchInfo getCurrentSearch() {
		return currentSearch;
	}

	public void resetCurrentSearch() {
		currentSearch = null;
		curMatchInd = -1;
		cachedSearches.setSelectedItem(null);
	}

	public long getCurrentMatchPos() {
		if(currentSearch == null){
			return -1;
		}
		return currentSearch.getMatchPositions().get(curMatchInd);
	}


}
