package org.literacybridge.acm.gui.dialogs.audioItemImportDialog;

import java.util.logging.Level;
import java.util.logging.Logger;

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

import org.literacybridge.acm.config.ControlAccess;
import org.literacybridge.acm.device.DeviceContents;
import org.literacybridge.acm.device.DeviceInfo;
import org.literacybridge.acm.importexport.FileImporter;
import org.literacybridge.acm.gui.resourcebundle.LabelProvider;
import org.literacybridge.acm.gui.Application;
import org.literacybridge.acm.gui.dialogs.BusyDialog;
import org.literacybridge.acm.gui.util.UIUtils;
import org.literacybridge.acm.gui.util.language.LanguageUtil;

@SuppressWarnings("serial")
public class AudioItemImportDialog extends JDialog {
	private static final Logger LOG = Logger.getLogger(AudioItemImportDialog.class.getName());

	private AudioItemImportView childDialog;
	private DeviceContents device;
	
	public AudioItemImportDialog(JFrame parent, DeviceInfo deviceInfo) {
		super(parent
				, LabelProvider.getLabel("AUDIO_ITEM_IMPORT_DIALOG_TITLE",	LanguageUtil.getUILanguage())
				, ModalityType.APPLICATION_MODAL);
		createControls();
		
		setSize(800, 500);
		
		try {
			device = deviceInfo.getDeviceContents();
			
			initialize();
		} catch (IOException e) {
			LOG.log(Level.WARNING, "Loading from device failed.", e);
		}
	}
	
	private void initialize() {
		// don't piggyback on the UI thread
		Runnable job = new Runnable() {

			@Override
			public void run() {
				Application parent = Application.getApplication();
				// TODO: show "Import statistics" instead of "Import files" in busy dialog
				final Container busy = UIUtils.showDialog(parent, new BusyDialog(LabelProvider.getLabel("IMPORTING_FILES", LanguageUtil.getUILanguage()), parent));
				try {
					final List<File> audioItems = device.loadAudioItems();
					// load statistics
					//device.importStats();
					//device.importOtherDeviceStats();
					childDialog.setData(audioItems);
				} catch (IOException e) {
					LOG.log(Level.WARNING, "Importing stats from device failed.", e);
				} finally {
					if (!SwingUtilities.isEventDispatchThread()) {
						SwingUtilities.invokeLater(new Runnable() {
							
							@Override
							public void run() {
								UIUtils.hideDialog(busy);
							}
						});
					}
				}
			}
		};
		
		new Thread(job).start();

	}
	
	private void createControls() {
		setLayout(new BorderLayout());
		childDialog = new AudioItemImportView();
		add(childDialog, BorderLayout.CENTER);
		
		JPanel panel = new JPanel();
		panel.setLayout(new GridLayout(1, 5));
		
		JButton selectAllBtn = new JButton(LabelProvider.getLabel("AUDIO_ITEM_IMPORT_DIALOG_SELECT_ALL", LanguageUtil.getUILanguage()));
		selectAllBtn.addActionListener(getSelectActionListener(true));
		
		JButton selectNoneBtn = new JButton(LabelProvider.getLabel("AUDIO_ITEM_IMPORT_DIALOG_SELECT_NONE", LanguageUtil.getUILanguage()));
		selectNoneBtn.addActionListener(getSelectActionListener(false));
		
		
		JButton okBtn = new JButton(LabelProvider.getLabel("IMPORT", LanguageUtil.getUILanguage()));
		if (ControlAccess.isACMReadOnly()) {
			okBtn.setEnabled(false);
			selectAllBtn.setEnabled(false);
			selectNoneBtn.setEnabled(false);
		}
		okBtn.addActionListener(getImportActionListener());
		
		JButton cancelBtn = new JButton(LabelProvider.getLabel("CANCEL", LanguageUtil.getUILanguage()));
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
				Application.getFilterState().updateResult();
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
						final Container busy = UIUtils.showDialog(parent, new BusyDialog(LabelProvider.getLabel("IMPORTING_FILES", LanguageUtil.getUILanguage()), parent));
						try {
							for (File f : files) {
								try {
									FileImporter.getInstance().importFile(null, f);
								} catch (Exception e) {
									LOG.log(Level.WARNING, "Importing file '" + f + "' failed.", e);
								}
							}
							// also refresh all statistics
							// device.importStats();
							// device.importOtherDeviceStats();
						} catch (Exception e) {
							LOG.log(Level.WARNING, "Importing stats from device failed.", e);
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
