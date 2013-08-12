package org.literacybridge.acm.gui.util;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JDialog;

public class ClosingDialogAdapter extends MouseAdapter {
	JDialog dlg = null;
	public ClosingDialogAdapter(JDialog dlg) {
		this.dlg = dlg;
	}
	@Override
	public void mouseClicked(MouseEvent e) {
		dlg.setVisible(false);
	}
}
