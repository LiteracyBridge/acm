package org.literacybridge.acm.rcp.toolbar;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.ui.menus.WorkbenchWindowControlContribution;

public class PlayerDetailsContribution extends
		WorkbenchWindowControlContribution {

	public PlayerDetailsContribution() {
	}

	public PlayerDetailsContribution(String id) {
		super(id);
	}

	@Override
	protected Control createControl(Composite parent) {
		Scale positionSlider = new Scale(parent, SWT.CENTER);
		positionSlider.setMaximum(100);
		return positionSlider;
	}
}
