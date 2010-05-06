package org.literacybridge.acm.ui.dialogs;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import org.jdesktop.swingx.JXBusyLabel;
import org.literacybridge.acm.ui.Application;

public class BusyDialog extends JDialog {
	JLabel description;
	JXBusyLabel busyLabel;
	
	private BusyDialog(String description, JFrame parent) {
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
		
		setSize(new Dimension(200, 50));
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE); 

		
	}
	
	public static BusyDialog show(String description) {
		JFrame frame = Application.getApplication();
	    final BusyDialog busy = new BusyDialog(description, frame);
	    final Dimension frameSize = frame.getSize();
	    final int x = (frameSize.width - busy.getWidth()) / 2;
	    final int y = (frameSize.height - busy.getHeight()) / 2;
	    busy.setLocation(x, y);

       if(!SwingUtilities.isEventDispatchThread()){ 
            SwingUtilities.invokeLater(new Runnable(){ 
                public void run(){ 
                    busy.setVisible(true);
                } 
            }); 
        } else {
        	busy.setVisible(true);
        }

	    return busy;
	}
}
