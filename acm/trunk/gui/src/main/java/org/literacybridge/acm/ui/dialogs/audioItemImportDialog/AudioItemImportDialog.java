package org.literacybridge.acm.ui.dialogs.audioItemImportDialog;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.literacybridge.acm.device.DeviceContents;
import org.literacybridge.acm.device.DeviceInfo;
import org.literacybridge.acm.importexport.FileImporter;
import org.literacybridge.acm.resourcebundle.LabelProvider;
import org.literacybridge.acm.ui.Application;
import org.literacybridge.acm.ui.dialogs.BusyDialog;
import org.literacybridge.acm.util.UIUtils;
import org.literacybridge.acm.util.language.LanguageUtil;

@SuppressWarnings("serial")
public class AudioItemImportDialog extends JDialog {

	private AudioItemImportView childDialog;
	
	public AudioItemImportDialog(JFrame parent, DeviceInfo deviceInfo) {
		super(parent, "Import AudioItems from device", ModalityType.APPLICATION_MODAL);
		createControls();
		
		setSize(800, 500);
		
		try {
			final DeviceContents device = deviceInfo.getDeviceContents();
			final List<File> audioItems = device.loadAudioItems();
			
			childDialog.setData(audioItems);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void createControls() {
		setLayout(new BorderLayout());
		childDialog = new AudioItemImportView();
		add(childDialog, BorderLayout.CENTER);
		
		JPanel panel = new JPanel();
		panel.setLayout(new GridLayout(1, 5));
		
		JButton selectAllBtn = new JButton("Select All");
		selectAllBtn.addActionListener(getSelectActionListener(true));
		
		JButton selectNoneBtn = new JButton("Select None");
		selectNoneBtn.addActionListener(getSelectActionListener(false));
		
		
		JButton okBtn = new JButton("Import");
		okBtn.addActionListener(getImportActionListener());
		
		JButton cancelBtn = new JButton("Cancel");
		cancelBtn.addActionListener(getCancelActionListener());
		
		panel.add(selectAllBtn);
		panel.add(selectNoneBtn);
		panel.add(new JLabel()); // dummy place holder
		panel.add(okBtn);
		panel.add(cancelBtn);
		
		add(panel, BorderLayout.SOUTH);
	}

	private ActionListener getSelectActionListener(final boolean selectAll) {
		return new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				childDialog.setCheckSetForAllItems(selectAll);
				
			}
		};
	}
	
	private ActionListener getCancelActionListener() {
		return new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				UIUtils.hideDialog(AudioItemImportDialog.this);
			}
		};
	}
	
	private ActionListener getImportActionListener() {
		return new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				UIUtils.hideDialog(AudioItemImportDialog.this);
				
				final List<File> files = getAudioItemsForImport();
				
				// don't piggyback on the drag&drop thread
				Runnable job = new Runnable() {

					@Override
					public void run() {
						Application parent = Application.getApplication();
						final Container busy = UIUtils.showDialog(parent, new BusyDialog(LabelProvider.getLabel("IMPORTING_FILES", LanguageUtil.getUserChoosenLanguage()), parent));
						try {
							for (File f : files) {
								FileImporter.getInstance().importFile(null, f);
							}
						} catch (IOException e) {
							e.printStackTrace();
						} finally {
							if (!SwingUtilities.isEventDispatchThread()) {
								SwingUtilities.invokeLater(new Runnable() {
									
									@Override
									public void run() {
										UIUtils.hideDialog(busy);
										Application.getFilterState().updateResult();	
									}
								});
							}
						}
					}
				};
				
				new Thread(job).start();
			}
		};
	}
	
	public List<File> getAudioItemsForImport() {
		return childDialog.getAudioItemsForImport();
	}
}
