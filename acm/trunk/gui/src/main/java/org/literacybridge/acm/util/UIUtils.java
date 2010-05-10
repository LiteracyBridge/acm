package org.literacybridge.acm.util;

import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;

import javax.swing.SwingUtilities;

public class UIUtils {
	public static Dialog showDialog(Frame parent, final Dialog dialog) {
	    final Dimension frameSize = parent.getSize();
	    final int x = (frameSize.width - dialog.getWidth()) / 2;
	    final int y = (frameSize.height - dialog.getHeight()) / 2;
	    dialog.setLocation(x, y);

       if(!SwingUtilities.isEventDispatchThread()){ 
            SwingUtilities.invokeLater(new Runnable(){ 
                public void run(){ 
                	dialog.setVisible(true);
                } 
            }); 
        } else {
        	dialog.setVisible(true);
        }

	    return dialog;
	}
}
