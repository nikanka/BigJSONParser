package com.bigjson.gui;

import java.text.DecimalFormat;

import javax.swing.tree.DefaultMutableTreeNode;

import com.bigjson.parser.JSONNode;

/**
	 * A wrapper around a JSONNode object to render it in a JSONTreeViewPanel
	 * 
	 * @author nikanka
	 *
	 */
	class JSONTreeNode extends DefaultMutableTreeNode {

		private boolean childrenAreLoaded = false;
		private JSONNode node;
		private DecimalFormat decimalFormat = new DecimalFormat("#,###,###,##0.0" );
		private DecimalFormat intFormat = new DecimalFormat("#,###,###,###" );
		

		JSONTreeNode(JSONNode node) {
			super();//getNodeStringWithSize(node),//getNodeStringWithSize(),
					//node.getType() == JSONNode.TYPE_ARRAY || node.getType() == JSONNode.TYPE_OBJECT);
//			this.node = node;// TODO
			this.setJSONNode(node);
			this.setAllowsChildren(node.getType() == JSONNode.TYPE_ARRAY || node.getType() == JSONNode.TYPE_OBJECT);
		}
		
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
						(!node.isFullyLoaded() || val.length() > JSONTreeViewModel.stringDisplayLength)){
					if(val.length() > JSONTreeViewModel.stringDisplayLength){
						val = val.substring(0, JSONTreeViewModel.stringDisplayLength);
					}
					sb.append(val);
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
			if(node.getType() == JSONNode.TYPE_STRING && node.getValue().length() > JSONTreeViewModel.stringDisplayLength){
				sb.append("  [length = ");
				sb.append(intFormat.format(node.getValue().length()));
				sb.append("]");
			}
			return sb.toString();
		}
		boolean childrenAreLoaded() {
			return childrenAreLoaded;
		}

		void setChildrenAreLoaded(boolean newVal) {
			this.childrenAreLoaded = newVal;
		}
		public void setJSONNode(JSONNode node) {
			this.node = node;
			super.setUserObject(createNodeString());
		}
		

		JSONNode getJSONNode() {
			return node;
		}
	}