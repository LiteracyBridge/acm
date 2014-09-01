package org.literacybridge.acm.gui.dialogs;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.ButtonGroup;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;

import org.literacybridge.acm.content.LocalizedAudioItem;
import org.literacybridge.acm.gui.Application;
import org.literacybridge.acm.gui.resourcebundle.LabelProvider;
import org.literacybridge.acm.gui.util.UIUtils;
import org.literacybridge.acm.gui.util.language.LanguageUtil;
import org.literacybridge.acm.importexport.CSVExporter;
import org.literacybridge.acm.importexport.FileSystemExporter;
import org.literacybridge.acm.repository.AudioItemRepository.AudioFormat;

public class ExportDialog extends JDialog implements ActionListener {
	private static final Logger LOG = Logger.getLogger(ExportDialog.class.getName());
	
	private JFileChooser exportDirectoryChooser;
	private JRadioButton mp3Button;
	private JRadioButton wavButton;
	private JRadioButton a18Button;
	private JRadioButton csvButton;
	private JRadioButton idOnlyButton;
	private JRadioButton titleOnlyButton;
	private JRadioButton title_idButton;
	
	private final LocalizedAudioItem[] selectedAudioItems;
	
	public ExportDialog(LocalizedAudioItem[] selectedAudioItems) {
		setTitle(LabelProvider.getLabel("EXPORT", LanguageUtil.getUILanguage()));
		this.selectedAudioItems = selectedAudioItems;
		
		exportDirectoryChooser = new JFileChooser();
		exportDirectoryChooser.setControlButtonsAreShown(true);
		exportDirectoryChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		exportDirectoryChooser.setApproveButtonText(LabelProvider.getLabel("EXPORT", LanguageUtil.getUILanguage()));

		// remove file type combo box
		Container c1 = (Container) exportDirectoryChooser.getComponent(3); 
		Container c2 = (Container) c1.getComponent(2); 
		Container c3 = (Container) c1.getComponent(3); 
		c2.remove(0);
		c2.remove(0);
		
		c2.add(new JLabel(LabelProvider.getLabel("EXPORT_FORMAT", LanguageUtil.getUILanguage())));
		c2.add(new JSeparator());
		
		ButtonGroup formatGroup = new ButtonGroup();
		
		mp3Button = new JRadioButton("mp3");
		wavButton = new JRadioButton("wav");
		a18Button = new JRadioButton("a18");
		csvButton = new JRadioButton("csv");
		
		formatGroup.add(mp3Button);
		formatGroup.add(wavButton);
		formatGroup.add(a18Button);
		formatGroup.add(csvButton);

		ButtonGroup filenameGroup = new ButtonGroup();
		titleOnlyButton = new JRadioButton("title");
		idOnlyButton = new JRadioButton("id");
		title_idButton = new JRadioButton("title+id");
		filenameGroup.add(idOnlyButton);
		filenameGroup.add(titleOnlyButton);
		filenameGroup.add(title_idButton);
		
		ActionListener buttonListener = new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) {
				if (csvButton.isSelected()) {
					exportDirectoryChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
				} else {
					exportDirectoryChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				}
			}
		};
		
		mp3Button.addActionListener(buttonListener);
		wavButton.addActionListener(buttonListener);
		a18Button.addActionListener(buttonListener);
		csvButton.addActionListener(buttonListener);
		
		mp3Button.setPreferredSize(new Dimension(70,20));
		wavButton.setPreferredSize(new Dimension(70,20));
		a18Button.setPreferredSize(new Dimension(70,20));
		csvButton.setPreferredSize(new Dimension(70,20));
		
		idOnlyButton.setPreferredSize(new Dimension(70,20));
		titleOnlyButton.setPreferredSize(new Dimension(70,20));
		title_idButton.setPreferredSize(new Dimension(70,20));

		c2.add(mp3Button);
		c2.add(wavButton);
		c2.add(a18Button);
		c2.add(csvButton);

		c3.add(titleOnlyButton);
		c3.add(idOnlyButton);
		c3.add(title_idButton);
		
		// select a18 by default
		a18Button.setSelected(true);

		// select filename=title__id by default
		title_idButton.setSelected(true);

		add(exportDirectoryChooser);
		setSize(600, 400);
		
		exportDirectoryChooser.addActionListener(this);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals(JFileChooser.APPROVE_SELECTION)) {
			this.setVisible(false);
			export(exportDirectoryChooser.getSelectedFile());
		} else if (e.getActionCommand().equals(JFileChooser.CANCEL_SELECTION)) {
			this.setVisible(false);
		}
	}
	
	private void export(final File target) {
		final Runnable job;
		
		if (csvButton.isSelected()) {
			job = new Runnable() {
				@Override public void run() {
					try {
						CSVExporter.export(ExportDialog.this.selectedAudioItems, target);
					} catch (IOException e) {
						LOG.log(Level.WARNING, "Exporting audio items failed", e);
					}
				}
			};
		} else {			
			final AudioFormat targetFormat;
			final boolean idInFilename = idOnlyButton.isSelected() || title_idButton.isSelected();
			final boolean titleInFilename = titleOnlyButton.isSelected() || title_idButton.isSelected();
			// TODO export dialog that let's you modify audio settings
			if (mp3Button.isSelected()) {
				targetFormat = AudioFormat.MP3;
			} else if (wavButton.isSelected()) {
				targetFormat = AudioFormat.WAV;
			} else {
				targetFormat = AudioFormat.A18;
			}
			
			// don't piggyback on the UI thread
			job = new Runnable() {
				@Override public void run() {
					Application app = Application.getApplication();
					// TODO proper label
					Container dialog = UIUtils.showDialog(app, new BusyDialog(LabelProvider.getLabel("EXPORTING_TO_TALKINGBOOK", LanguageUtil.getUILanguage()), app));
					try {
						FileSystemExporter.export(ExportDialog.this.selectedAudioItems, target, targetFormat,titleInFilename,idInFilename);
					} catch (IOException e) {
						LOG.log(Level.WARNING, "Exporting audio items failed", e);
					} finally {
						UIUtils.hideDialog(dialog);
					}
				}
			};
		}
		
		new Thread(job).start();
	}
}
