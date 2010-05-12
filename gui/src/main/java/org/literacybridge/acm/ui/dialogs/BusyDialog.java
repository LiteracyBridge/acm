package org.literacybridge.acm.ui.dialogs;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

import org.jdesktop.swingx.JXBusyLabel;

public class BusyDialog extends JDialog {
	JLabel description;
	JXBusyLabel busyLabel;
	
	public BusyDialog(String description, JFrame parent) {
		super(parent, "", true);
		this.description = new JLabel(description);
		this.busyLabel = new JXBusyLabel();
		this.description.setHorizontalAlignment(SwingConstants.CENTER);
		this.busyLabel.setHorizontalAlignment(SwingConstants.CENTER);
		
		setResizable(false);
		setUndecorated(true);
		busyLabel.setBusy(true);
		
		add(this.description, BorderLayout.NORTH);
		add(this.busyLabel, BorderLayout.CENTER);
		
		setSize(new Dimension(200, 80));
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE); 

		
	}
}
