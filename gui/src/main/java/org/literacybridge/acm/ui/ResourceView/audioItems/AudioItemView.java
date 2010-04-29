package org.literacybridge.acm.ui.ResourceView.audioItems;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;

import org.jdesktop.swingx.JXTable;
import org.literacybridge.acm.api.IDataRequestResult;
import org.literacybridge.acm.ui.Application;
import org.literacybridge.acm.ui.dialogs.AudioItemPropertiesDialog;

public class AudioItemView extends Container implements Observer {

	private static final long serialVersionUID = -2886958461177831842L;

	// model
	private IDataRequestResult currResult = null;

	// table
	private JXTable audioItemTable = null;
	private JPopupMenu audioItemTablePopupMenu = null;

	public AudioItemView(IDataRequestResult result) {
		setLayout(new BorderLayout());
		createTable();
		createPopupMenu();
		addHandlers();
		Application.getMessageService().addObserver(this);
		this.currResult = result;
	}

	private void createTable() {
		audioItemTable = new JXTable();
		updateTable(); // init empty

		JScrollPane scrollPane = new JScrollPane(audioItemTable);
		add(BorderLayout.CENTER, scrollPane);
	}

	private void updateTable() {
		audioItemTable.setModel(new AudioItemTableModel(currResult));
	}

	private void addHandlers() {
	    MouseListener popupListener = new PopupListener();
	    audioItemTable.addMouseListener(popupListener);
	    audioItemTable.getTableHeader().addMouseListener(popupListener);
	}

	private void createPopupMenu() {
		audioItemTablePopupMenu = new JPopupMenu();
		JMenuItem showPropertiesMenuItem = new JMenuItem("Properties...");
		showPropertiesMenuItem
				.addActionListener(new PopupMenuItemClicked(this));
		audioItemTablePopupMenu.add(showPropertiesMenuItem);
	}

	@Override
	public void update(Observable o, Object arg) {
		if (arg instanceof IDataRequestResult) {
			currResult = (IDataRequestResult) arg;
			updateTable();
		}
	}

	private class PopupListener extends MouseAdapter {
		public void mousePressed(MouseEvent e) {
			showPopup(e);
		}

		public void mouseReleased(MouseEvent e) {
			showPopup(e);
		}

		private void showPopup(MouseEvent e) {
			if (e.isPopupTrigger()) {
				audioItemTablePopupMenu.show(e.getComponent(), e.getX(), e
						.getY());
			}
		}
	}

	// Handler our popup action
	private class PopupMenuItemClicked implements ActionListener {

		private AudioItemView adaptee = null;

		public PopupMenuItemClicked(AudioItemView adaptee) {
			this.adaptee = adaptee;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			int index = adaptee.audioItemTable.getSelectedRow();
			AudioItemPropertiesDialog dlg = new AudioItemPropertiesDialog(
					Application.getApplication(), currResult.getAudioItems(),
					index);
			dlg.setVisible(true);
		}
	}
}
