package org.literacybridge.acm.ui.dialogs.audioItemPropertiesDialog;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.decorator.HighlighterFactory;
import org.literacybridge.acm.content.AudioItem;
import org.literacybridge.acm.metadata.Metadata;
import org.literacybridge.acm.resourcebundle.LabelProvider;
import org.literacybridge.acm.ui.ResourceView.ResourceView;
import org.literacybridge.acm.ui.ResourceView.audioItems.AudioItemView;
import org.literacybridge.acm.util.language.LanguageUtil;

public class AudioItemPropertiesDialog extends JDialog {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3854016276035587383L;
	private List<AudioItem> audioItemList = null;
	private int currIndex = 0;
	private AudioItemView audioItemView = null;

	private JXTable propertiesTable = null;

	private JButton backBtn = null;
	private JButton nextBtn = null;

	public AudioItemPropertiesDialog(JFrame parent, AudioItemView view,
			List<AudioItem> audioItemList, AudioItem showItem) {
		super(parent, LabelProvider.getLabel("AUDIO_ITEM_PROPERTIES",
				LanguageUtil.getUserChoosenLanguage()), true);
		this.audioItemList = audioItemList;
		currIndex = getIndexOfAudioItem(showItem);
		this.audioItemView = view;

		createControlsForAvailableProperties();

		setSize(500, 500);
		pack();

		AudioItem audioItem = audioItemList.get(currIndex);
		showAudioItem(audioItem);
		enableControls();
	}

	private int getIndexOfAudioItem(AudioItem item) {
		for (int i = 0; i < audioItemList.size(); ++i) {
			AudioItem audioItem = audioItemList.get(i);
			if (audioItem.equals(item)) {
				return i;
			}
		}

		return 0;
	}

	private void showAudioItem(AudioItem item) {
		if (item == null)
			return; // JTBD
		Metadata metadata = item.getLocalizedAudioItem(
				LanguageUtil.getUserChoosenLanguage()).getMetadata();
		showMetadata(metadata);

		// select it in underlying view if available
		if (audioItemView != null) {
			audioItemView.selectAudioItem(item);
		}
	}

	private AudioItem getNextItem() {

		if (audioItemList.size() > currIndex + 1) {
			return audioItemList.get(++currIndex);
		}

		return null;
	}

	private AudioItem getPrevItem() {

		if (currIndex - 1 > -1) {
			return audioItemList.get(--currIndex);
		}

		return null;
	}

	private void enableControls() {
		nextBtn.setEnabled(currIndex != audioItemList.size() - 1);
		backBtn.setEnabled(currIndex != 0);
	}

	private void createControlsForAvailableProperties() {

		if (audioItemList != null && audioItemList.size() > 0) {
			// add navigation buttons
			JPanel p = new JPanel();
			backBtn = new JButton(LabelProvider.getLabel("GOTO_PREV_AUDIO_ITEM", LanguageUtil.getUserChoosenLanguage()));
			backBtn.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					showAudioItem(getPrevItem());
					enableControls();
				}
			});
			p.add(backBtn);
			nextBtn = new JButton(LabelProvider.getLabel("GOTO_NEXT_AUDIO_ITEM", LanguageUtil.getUserChoosenLanguage()));
			nextBtn.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					showAudioItem(getNextItem());
					enableControls();
				}
			});
			p.add(nextBtn);
			add(p, BorderLayout.NORTH);
			p.setBorder(BorderFactory.createEmptyBorder());

			// Show properties table
			JScrollPane theScrollPane = new JScrollPane();
			propertiesTable = new JXTable();
			propertiesTable.setShowGrid(false, false);
			// use fixed color; there seems to be a bug in some plaf
			// implementations that cause strange rendering
			propertiesTable.addHighlighter(HighlighterFactory
					.createAlternateStriping(Color.white, new Color(237, 243,
							254)));

			theScrollPane.getViewport().add(propertiesTable, null);
			add(theScrollPane);
		}

		// add bottom buttons
		JButton okBtn = new JButton(LabelProvider.getLabel("CLOSE", LanguageUtil.getUserChoosenLanguage()));
		okBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// update IRequestResult
				ResourceView.updateDataRequestResult();
				setVisible(false);
			}
		});
		add(okBtn, BorderLayout.SOUTH);
	}

	private void showMetadata(Metadata metadata) {
		propertiesTable.setModel(new AudioItemPropertiesModel(metadata));
	}
}
