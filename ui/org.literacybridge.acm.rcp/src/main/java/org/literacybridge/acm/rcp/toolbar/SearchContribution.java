package org.literacybridge.acm.rcp.toolbar;


import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.menus.WorkbenchWindowControlContribution;

public class SearchContribution extends WorkbenchWindowControlContribution {

	private Combo searchCB = null;
	
	public SearchContribution() {
	}

	public SearchContribution(String id) {
		super(id);
	}

	@Override
	protected Control createControl(Composite parent) {
		searchCB = new Combo(parent, SWT.CENTER);
		return searchCB;
	}

	@Override
	protected int computeWidth(Control control) {
		return 150;	// fix width for the search box (otherwise the width would be near 0).
	}
}
