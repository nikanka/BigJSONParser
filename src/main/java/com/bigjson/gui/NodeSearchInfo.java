package com.bigjson.gui;

import javax.swing.tree.TreePath;

import com.bigjson.parser.StringSearchInfo;

class NodeSearchInfo {
	StringSearchInfo search;
	TreePath nodePath;
	NodeSearchInfo(StringSearchInfo search, TreePath nodePath){
		this.nodePath = nodePath;
		this.search = search;
	}
	TreePath getNodePath(){
		return nodePath;
	}
	StringSearchInfo getSearchInfo(){
		return search;
	}
	String getStringToSearch(){
		return search.getStringToSearch();
	}
}