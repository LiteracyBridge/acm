package org.literacybridge.acm.rcp.views.audioItemProperties;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.part.ViewPart;

public class AudioItemProperties extends ViewPart {


	
	@Override
	public void createPartControl(Composite parent) {
		Text text = new Text(parent, SWT.NULL);
		text.setText("Hello");
	}

		
	@Override
	public void setFocus() {

	}

}
