package org.literacybridge.acm.gui.util;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Toolkit;
import java.lang.reflect.InvocationTargetException;

import javax.swing.SwingUtilities;

public class UIUtils {
	
	public static void centerOnScreen(final Component c, final boolean absolute) {
		final int width = c.getWidth();
		final int height = c.getHeight();
		final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int x = (screenSize.width / 2) - (width / 2);
		int y = (screenSize.height / 2) - (height / 2);
		if (!absolute) {
			x /= 2;
			y /= 2;
		}
		
		c.setLocation(x, y);
	}
		
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

	public static void invokeAndWait(final Runnable runnable) {
		try {
			if (SwingUtilities.isEventDispatchThread()) {
				runnable.run();
			} else {
				SwingUtilities.invokeAndWait(runnable);
			}
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}
}
