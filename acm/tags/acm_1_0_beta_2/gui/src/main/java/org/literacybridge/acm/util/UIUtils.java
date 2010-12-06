package org.literacybridge.acm.util;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;

import javax.swing.SwingUtilities;

public class UIUtils {
	public static Container showDialog(Frame parent, final Container dialog) {
		final Dimension frameSize = parent.getSize();
		final int x = (frameSize.width - dialog.getWidth()) / 2;
		final int y = (frameSize.height - dialog.getHeight()) / 2;
		return showDialog(dialog, x, y);
	}

	public static Container showDialog(final Container dialog, int x,
			int y) {

		dialog.setLocation(x, y);
		setVisible(dialog, true);
		return dialog;
	}
	
	public static void hideDialog(final Container dialog) {
		setVisible(dialog, false);
	}

	
	private static void setVisible(final Container dialog, final boolean visible) {
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
