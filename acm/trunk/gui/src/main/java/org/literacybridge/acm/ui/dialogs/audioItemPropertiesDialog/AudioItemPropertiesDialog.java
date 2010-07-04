package org.literacybridge.acm.ui.dialogs.audioItemPropertiesDialog;

import static org.literacybridge.acm.metadata.MetadataSpecification.DC_CONTRIBUTOR;
import static org.literacybridge.acm.metadata.MetadataSpecification.DC_COVERAGE;
import static org.literacybridge.acm.metadata.MetadataSpecification.DC_CREATOR;
import static org.literacybridge.acm.metadata.MetadataSpecification.DC_DESCRIPTION;
import static org.literacybridge.acm.metadata.MetadataSpecification.DC_FORMAT;
import static org.literacybridge.acm.metadata.MetadataSpecification.DC_PUBLISHER;
import static org.literacybridge.acm.metadata.MetadataSpecification.DC_RELATION;
import static org.literacybridge.acm.metadata.MetadataSpecification.DC_RIGHTS;
import static org.literacybridge.acm.metadata.MetadataSpecification.DC_SOURCE;
import static org.literacybridge.acm.metadata.MetadataSpecification.DC_SUBJECT;
import static org.literacybridge.acm.metadata.MetadataSpecification.DC_TITLE;
import static org.literacybridge.acm.metadata.MetadataSpecification.DC_TYPE;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.ScrollPane;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.TitledBorder;
import javax.swing.event.TableColumnModelListener;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import org.jdesktop.swingx.JXLabel;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.JXTableHeader;
import org.jdesktop.swingx.decorator.HighlighterFactory;

import org.literacybridge.acm.content.AudioItem;
import org.literacybridge.acm.metadata.LBMetadataIDs;
import org.literacybridge.acm.metadata.Metadata;
import org.literacybridge.acm.metadata.MetadataField;
import org.literacybridge.acm.metadata.MetadataValue;
import org.literacybridge.acm.resourcebundle.LabelProvider;
import org.literacybridge.acm.ui.ResourceView.audioItems.AudioItemTableModel;
import org.literacybridge.acm.ui.ResourceView.audioItems.AudioItemView;
import org.literacybridge.acm.util.ClosingDialogAdapter;
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
				setVisible(false);
			}
		});
		add(okBtn, BorderLayout.SOUTH);
	}

	private void showMetadata(Metadata metadata) {
		propertiesTable.setModel(new AudioItemPropertiesModel(metadata));
	}
}
