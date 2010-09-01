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
		return showDialog(dialog, x, y);
	}

	public static Dialog showDialog(final Dialog dialog, int x,
			int y) {

		dialog.setLocation(x, y);
		setVisible(dialog, true);
		return dialog;
	}
	
	public static void hideDialog(final Dialog dialog) {
		setVisible(dialog, false);
	}

	
	private static void setVisible(final Dialog dialog, final boolean visible) {
		if (!SwingUtilities.isEventDispatchThread()) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					dialog.setVisible(visible);
				}
			});
		} else {
			dialog.setVisible(visible);
		}
	}

}
