package org.literacybridge.acm.rcp.views.category.helpers;

import java.util.List;

import org.eclipse.jface.viewers.ISelection;
import org.literacybridge.acm.rcp.views.adapters.ICategoryResultContainer;

public class CheckedTreeNodeSelection implements ISelection {

	private List<ICategoryResultContainer> checkedTreeLeafs = null;
	
	public CheckedTreeNodeSelection(List<ICategoryResultContainer> selectedTreeLeafs) {
		this.checkedTreeLeafs = selectedTreeLeafs;
	}
	
	@Override
	public boolean isEmpty() {
		return checkedTreeLeafs != null && checkedTreeLeafs.size() > 0 ? false : true;
	}

	// @returns list of check tree !!leafs!! not the parent nodes at the moment.
	public List<ICategoryResultContainer> getCheckTreeLeafs() {
		return checkedTreeLeafs;
	}
	
}
