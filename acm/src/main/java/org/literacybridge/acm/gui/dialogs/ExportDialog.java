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
import org.literacybridge.acm.importexport.FileSystemExporter;
import org.literacybridge.acm.repository.AudioItemRepository.AudioFormat;

public class ExportDialog extends JDialog implements ActionListener {
	private static final Logger LOG = Logger.getLogger(ExportDialog.class.getName());
	
	private JFileChooser exportDirectoryChooser;
	private JRadioButton mp3Button;
	private JRadioButton wavButton;
	private JRadioButton a18Button;
	
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
		c2.remove(0);
		c2.remove(0);
		
		c2.add(new JLabel(LabelProvider.getLabel("EXPORT_FORMAT", LanguageUtil.getUILanguage())));
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
		final AudioFormat targetFormat;
		// TODO export dialog that let's you modify audio settings
		if (mp3Button.isSelected()) {
			targetFormat = AudioFormat.MP3;
		} else if (wavButton.isSelected()) {
			targetFormat = AudioFormat.WAV;
		} else {
			targetFormat = AudioFormat.A18;
		}
		
		// don't piggyback on the UI thread
		Runnable job = new Runnable() {
			@Override
			public void run() {
				Application app = Application.getApplication();
				// TODO proper label
				Container dialog = UIUtils.showDialog(app, new BusyDialog(LabelProvider.getLabel("EXPORTING_TO_TALKINGBOOK", LanguageUtil.getUILanguage()), app));
				try {
					FileSystemExporter.export(ExportDialog.this.selectedAudioItems, targetDir, targetFormat);
				} catch (IOException e) {
					LOG.log(Level.WARNING, "Exporting audio items failed", e);
				} finally {
					UIUtils.hideDialog(dialog);
				}
			}
		};
		
		new Thread(job).start();
	}
}
