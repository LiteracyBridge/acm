package org.literacybridge.acm.ui.dialogs;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.ScrollPane;
import java.awt.TextField;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import org.literacybridge.acm.content.AudioItem;
import org.literacybridge.acm.metadata.LBMetadataIDs;
import org.literacybridge.acm.metadata.Metadata;
import org.literacybridge.acm.metadata.MetadataField;
import org.literacybridge.acm.metadata.MetadataSpecification;
import org.literacybridge.acm.resourcebundle.LabelProvider;
import org.literacybridge.acm.resourcebundle.LabelProvider.KeyValuePair;
import org.literacybridge.acm.util.ClosingDialogAdapter;
import org.literacybridge.acm.util.LanguageUtil;

public class AudioItemPropertiesDialog extends JDialog {

	private List<AudioItem> audioItemList = null;
	private int currIndex = 0;
	
	public AudioItemPropertiesDialog(JFrame parent, List<AudioItem> audioItemList, int index) {
		super(parent, "AudioItem Properties", true);
		this.audioItemList = audioItemList;
		currIndex = index;
		
		createControlsForAvailableProperties();
		pack();
		setSize(500, 500);
	}
	
	private void createControlsForAvailableProperties() {

		if (audioItemList != null && audioItemList.size() > 0) {
			// add navigation buttons
			JPanel p = new JPanel();
			JButton backBtn = new JButton("Goto previous AudioItem");
			p.add(backBtn);
			JButton nextBtn = new JButton("Goto next AudioItem");
			p.add(nextBtn);
			add(p, BorderLayout.BEFORE_FIRST_LINE);
			p.setBorder(new TitledBorder("Navigate to"));
			
			
			AudioItem audioItem = audioItemList.get(0);
			Metadata metadata = audioItem.getLocalizedAudioItem(LanguageUtil.getUserChoosenLanguage()).getMetadata();

			// TODOs
			Locale locale = Locale.ENGLISH;

			Container c = new Container();
			c.setLayout(new GridLayout(LBMetadataIDs.FieldToIDMap.size(), 2));
			
			Iterator<KeyValuePair<MetadataField<?>, String>> fieldsIterator = LabelProvider.getMetaFieldLabelsIterator(locale);
			while (fieldsIterator.hasNext()) {
				KeyValuePair<MetadataField<?>, String> field = fieldsIterator.next();
				String valueList = Metadata.getCommaSeparatedList(metadata, field.getKey());
				Label text = new Label(field.getValue() + ": ");
				c.add(text);
				c.add(new TextField(valueList));
			}

			// create scrollable conainer
			ScrollPane sp = new ScrollPane();
			add(sp, BorderLayout.CENTER);
			sp.add(c);
		}
		
		// add bottom buttons
		JButton okBtn = new JButton("Close");
		okBtn.addMouseListener(new ClosingDialogAdapter(this));
		add(okBtn, BorderLayout.SOUTH);
	}	
}
