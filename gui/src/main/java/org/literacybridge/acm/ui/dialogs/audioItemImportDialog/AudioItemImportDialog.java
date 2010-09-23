package org.literacybridge.acm.ui.dialogs.audioItemImportDialog;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;

import org.literacybridge.acm.api.IDataRequestResult;
import org.literacybridge.acm.content.AudioItem;

public class AudioItemImportDialog extends JDialog {

	private AudioItemImportView childDialog;
	private JButton okBtn = new JButton("OK");
	private JButton cancelBtn = new JButton("Cancel");
	
	public AudioItemImportDialog() {
		setTitle("Import AudioItems from device");
		createControls();
		
		setSize(800, 500);
	}
	
	private void createControls() {
		setLayout(new BorderLayout());
		childDialog = new AudioItemImportView();
		add(childDialog, BorderLayout.CENTER);
		
		JPanel panel = new JPanel();
		panel.setLayout(new GridLayout(1, 2));
		okBtn = new JButton("OK");
		okBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setVisible(false);
			}
		});
		
		cancelBtn = new JButton("Cancel");
		cancelBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setVisible(false);
			}
		});
		panel.add(okBtn);
		panel.add(cancelBtn);
		
		add(panel, BorderLayout.SOUTH);
	}

	public void setData(IDataRequestResult data) {
		childDialog.setData(data);
	}
	
	public List<AudioItem> getAudioItemsForImport() {
		return childDialog.getAudioItemsForImport();
	}
}
