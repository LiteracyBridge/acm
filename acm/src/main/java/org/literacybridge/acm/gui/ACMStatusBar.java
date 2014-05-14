package org.literacybridge.acm.gui;

import java.awt.Dimension;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.JSeparator;

import org.jdesktop.swingx.JXStatusBar;
import org.jdesktop.swingx.plaf.basic.BasicStatusBarUI;

public class ACMStatusBar extends JXStatusBar {
	private JLabel statusLabel;
	
	private JLabel progressLabel;
	private JProgressBar progressBar;
	private JButton cancelButton;
	
	public ACMStatusBar() {
		putClientProperty(BasicStatusBarUI.AUTO_ADD_SEPARATOR, false);
		setPreferredSize(new Dimension(1600, 25));
		
		statusLabel = new JLabel();
		add(statusLabel, new JXStatusBar.Constraint(400));
		
		add(new JSeparator(JSeparator.VERTICAL));
		
		progressLabel = new JLabel("Updating wav cache");
		add(progressLabel, new JXStatusBar.Constraint(300));
		
		progressBar = new JProgressBar();
		add(progressBar, new JXStatusBar.Constraint(200));
		
		cancelButton = new JButton("Cancel");
		add(cancelButton);
	}

	public void setStatusMessage(String message) {
		statusLabel.setText(message);
	}
	
	public JButton getCancelButton() {
		return cancelButton;
	}
	
	public JProgressBar getProgressBar() {
		return progressBar;
	}
	
	public JLabel getProgressLabel() {
		return progressLabel;
	}
	
	public void startTask(String label) {
		progressLabel.setText(label);
		progressBar.setValue(0);
		progressBar.setVisible(true);
		progressLabel.setVisible(true);
		cancelButton.setVisible(true);
	}
	
	public void endTask() {
		progressBar.setVisible(false);
		progressLabel.setVisible(false);
		cancelButton.setVisible(false);		
	}
}
