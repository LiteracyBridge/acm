package org.literacybridge.acm.gui.util;

import java.awt.Color;
import java.awt.Container;

public class ACMContainer extends Container {
	private Color backgroundColor;
	
	public ACMContainer() {
		super();
		this.backgroundColor = getBackground();
	}
	
	@Override
	public void setBackground(Color bgColor) {
		// Workaround for weird bug in seaglass look&feel that causes a
		// java.awt.IllegalComponentStateException when e.g. a combo box
		// in this dialog is clicked on
		if (bgColor.getAlpha() == 0) {
			super.setBackground(backgroundColor);
		} else {
			super.setBackground(bgColor);
			backgroundColor = bgColor;
		}
	}

}
