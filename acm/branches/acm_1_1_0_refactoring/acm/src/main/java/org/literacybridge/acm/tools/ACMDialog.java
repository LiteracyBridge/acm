package org.literacybridge.acm.tools;

import java.awt.Color;
import java.awt.Frame;

import javax.swing.JDialog;

public class ACMDialog extends JDialog {
	private Color backgroundColor;
	
	public ACMDialog(Frame owner, String title, boolean modal) {
		super(owner, title, modal);
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
