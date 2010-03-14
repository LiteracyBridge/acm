package org.literacybridge.acm.rcp.toolbar;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.menus.WorkbenchWindowControlContribution;
import org.literacybridge.acm.rcp.util.FontUtil;

public class SearchLabelContribution extends WorkbenchWindowControlContribution {

	public SearchLabelContribution() {
	}

	public SearchLabelContribution(String id) {
		super(id);
	}

	@Override
	protected Control createControl(Composite parent) {
		Label searchLabel = new Label(parent, SWT.CENTER);
		searchLabel.setText("Search:");	

	    searchLabel.setFont(FontUtil.getToolbarDefaultFont());
		return searchLabel;
	}

}
