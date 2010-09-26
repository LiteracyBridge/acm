package org.literacybridge.acm.ui.dialogs;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

import javax.swing.ButtonGroup;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;

import org.literacybridge.acm.content.LocalizedAudioItem;
import org.literacybridge.acm.importexport.FileSystemExporter;
import org.literacybridge.acm.resourcebundle.LabelProvider;
import org.literacybridge.acm.ui.Application;
import org.literacybridge.acm.util.UIUtils;
import org.literacybridge.acm.util.language.LanguageUtil;
import org.literacybridge.audioconverter.api.A18Format;
import org.literacybridge.audioconverter.api.AudioConversionFormat;
import org.literacybridge.audioconverter.api.MP3Format;
import org.literacybridge.audioconverter.api.WAVFormat;
import org.literacybridge.audioconverter.api.A18Format.AlgorithmList;
import org.literacybridge.audioconverter.api.A18Format.useHeaderChoice;

public class ExportDialog extends JDialog implements ActionListener {
	private JFileChooser exportDirectoryChooser;
	private JRadioButton mp3Button;
	private JRadioButton wavButton;
	private JRadioButton a18Button;
	
	private final LocalizedAudioItem[] selectedAudioItems;
	
	public ExportDialog(LocalizedAudioItem[] selectedAudioItems) {
		this.selectedAudioItems = selectedAudioItems;
		
		exportDirectoryChooser = new JFileChooser();
		exportDirectoryChooser.setControlButtonsAreShown(true);
		exportDirectoryChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		exportDirectoryChooser.setApproveButtonText("Export");

		// remove file type combo box
		Container c1 = (Container) exportDirectoryChooser.getComponent(3); 
		Container c2 = (Container) c1.getComponent(2); 
		c2.remove(0);
		c2.remove(0);
		
		c2.add(new JLabel("Export format: "));
		c2.add(new JSeparator());
		
		ButtonGroup formatGroup = new ButtonGroup();
		
		mp3Button = new JRadioButton("mp3");
		wavButton = new JRadioButton("wav");
		a18Button = new JRadioButton("a18");
		formatGroup.add(mp3Button);
		formatGroup.add(wavButton);
		formatGroup.add(a18Button);
		
		mp3Button.setPreferredSize(new Dimension(70,20));
		wavButton.setPreferredSize(new Dimension(70,20));
		a18Button.setPreferredSize(new Dimension(70,20));
		c2.add(mp3Button);
		c2.add(wavButton);
		c2.add(a18Button);
		
		// select mp3 by default
		mp3Button.setSelected(true);
		
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
	
	private void export(final File targetDir) {
		final AudioConversionFormat targetFormat;
		// TODO export dialog that let's you modify these default settings
		if (mp3Button.isSelected()) {
			targetFormat = new MP3Format(128, 16000, 1);
		} else if (wavButton.isSelected()) {
			targetFormat = new WAVFormat(128, 16000, 1);
		} else {
			targetFormat = new A18Format(128, 16000, 1, AlgorithmList.A1800, useHeaderChoice.No);
		}
		
		// don't piggyback on the UI thread
		Runnable job = new Runnable() {
			@Override
			public void run() {
				Application app = Application.getApplication();
				// TODO proper label
				Container dialog = UIUtils.showDialog(app, new BusyDialog(LabelProvider.getLabel("EXPORTING_TO_TALKINGBOOK",	LanguageUtil.getUserChoosenLanguage()), app));
				try {
					FileSystemExporter.export(ExportDialog.this.selectedAudioItems, targetDir, targetFormat);
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					UIUtils.hideDialog(dialog);
				}
			}
		};
		
		new Thread(job).start();
	}
}
