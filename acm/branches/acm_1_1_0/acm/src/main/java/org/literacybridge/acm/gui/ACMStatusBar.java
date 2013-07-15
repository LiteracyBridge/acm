package org.literacybridge.acm.gui;

import java.awt.Dimension;

import javax.swing.JLabel;

import org.jdesktop.swingx.JXStatusBar;
import org.jdesktop.swingx.plaf.basic.BasicStatusBarUI;

public class ACMStatusBar extends JXStatusBar {
	private JLabel statusLabel;
	
	public ACMStatusBar() {
		putClientProperty(BasicStatusBarUI.AUTO_ADD_SEPARATOR, false);
		setPreferredSize(new Dimension(1600, 25));
		statusLabel = new JLabel();
		statusLabel.setPreferredSize(new Dimension(500, 20));
		add(statusLabel);		
	}

	public void setStatusMessage(String message) {
		statusLabel.setText(message);
	}
}
