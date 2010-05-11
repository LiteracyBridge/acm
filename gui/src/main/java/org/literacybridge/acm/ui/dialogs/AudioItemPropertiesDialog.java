package org.literacybridge.acm.ui.dialogs;

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
import java.awt.Container;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.ScrollPane;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import org.literacybridge.acm.content.AudioItem;
import org.literacybridge.acm.metadata.LBMetadataIDs;
import org.literacybridge.acm.metadata.Metadata;
import org.literacybridge.acm.metadata.MetadataField;
import org.literacybridge.acm.metadata.MetadataValue;
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
	

	private Label DC_TITLE_L = new Label();
	private Label DC_CREATOR_L = new Label();
	private Label DC_SUBJECT_L = new Label();
	private Label DC_DESCRIPTION_L = new Label();
	private Label DC_PUBLISHER_L = new Label();
	private Label DC_CONTRIBUTOR_L = new Label();
//	private Label DC_DATE_L = new Label();
	private Label DC_TYPE_L = new Label();
	private Label DC_FORMAT_L = new Label();
	private Label DC_SOURCE_L = new Label();
	//private Label DC_LANGUAGE_L = new Label();
	private Label DC_RELATION_L = new Label();
	private Label DC_COVERAGE_L = new Label();
	private Label DC_RIGHTS_L = new Label();
	
	private TextField DC_TITLE_TF = new TextField();
	private TextField DC_CREATOR_TF = new TextField();
	private TextField DC_SUBJECT_TF = new TextField();
	private TextField DC_DESCRIPTION_TF = new TextField();
	private TextField DC_PUBLISHER_TF = new TextField();
	private TextField DC_CONTRIBUTOR_TF = new TextField();
//	private TextField DC_DATE_TF = new TextField();
	private TextField DC_TYPE_TF = new TextField();
	private TextField DC_FORMAT_TF = new TextField();
	private TextField DC_SOURCE_TF = new TextField();
	//private TextField DC_LANGUAGE_TF = new TextField();
	private TextField DC_RELATION_TF = new TextField();
	private TextField DC_COVERAGE_TF = new TextField();
	private TextField DC_RIGHTS_TF = new TextField();
	
	private JButton backBtn = null;
	private JButton nextBtn = null;
	
	public AudioItemPropertiesDialog(JFrame parent, AudioItemView view, List<AudioItem> audioItemList, AudioItem showItem) {
		super(parent, "AudioItem Properties", true);
		this.audioItemList = audioItemList;
		currIndex = getIndexOfAudioItem(showItem);
		this.audioItemView = view;
		
		createControlsForAvailableProperties();
		pack();
		setSize(500, 500);
		
		AudioItem audioItem = audioItemList.get(currIndex);
		showAudioItem(audioItem);
		enableControls();
	}
	
	private int getIndexOfAudioItem(AudioItem item) {
		for(int i=0; i<audioItemList.size(); ++i) {
			AudioItem audioItem = audioItemList.get(i);
			if (audioItem.equals(item)) {
				return i;
			}
		}
		
		return 0;
	}
	
	private void showAudioItem(AudioItem item) {
		if (item == null) return; // JTBD
		Metadata metadata = item.getLocalizedAudioItem(LanguageUtil.getUserChoosenLanguage()).getMetadata();
		showMetadata(metadata);
		
		// select it in underlying view if available
		if (audioItemView != null) {
			audioItemView.selectAudioItem(item);
		}
	}
	
	private AudioItem getNextItem() {
		commitChanges();
		
		if (audioItemList.size() > currIndex+1) {
			return audioItemList.get(++currIndex);			
		}
		
		return null;
	}
	
	private AudioItem getPrevItem() {
		commitChanges();
		
		if (currIndex-1 > -1) {
			return audioItemList.get(--currIndex);
		}
		
		return null;
	}
	
	private void enableControls() {
		nextBtn.setEnabled(currIndex != audioItemList.size()-1);
		backBtn.setEnabled(currIndex != 0);
	}
	
	private void createControlsForAvailableProperties() {

		if (audioItemList != null && audioItemList.size() > 0) {
			// add navigation buttons
			JPanel p = new JPanel();
			backBtn = new JButton("Goto previous AudioItem");
			backBtn.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					showAudioItem(getPrevItem());
					enableControls();
				}
			});
			p.add(backBtn);
			nextBtn = new JButton("Goto next AudioItem");
			nextBtn.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					showAudioItem(getNextItem());
					enableControls();
				}
			});
			p.add(nextBtn);
			add(p, BorderLayout.BEFORE_FIRST_LINE);
			p.setBorder(new TitledBorder("Navigate to"));
			
			

			Container c = new Container();
			c.setLayout(new GridLayout(LBMetadataIDs.FieldToIDMap.size(), 2));

			DC_TITLE_L.setText("DC_TITLE");
			DC_CREATOR_L.setText("DC_CREATOR");
			DC_SUBJECT_L.setText("DC_SUBJECT");
			DC_DESCRIPTION_L.setText("DC_DESCRIPTION");
			DC_PUBLISHER_L.setText("DC_PUBLISHER");
			DC_CONTRIBUTOR_L.setText("DC_CONTRIBUTOR");
//			DC_DATE_L.setText("DC_DATE");
			DC_TYPE_L.setText("DC_TYPE");
			DC_FORMAT_L.setText("DC_FORMAT");
			DC_SOURCE_L.setText("DC_SOURCE");
			//DC_LANGUAGE_L.setText("DC_LANGUAGE");
			DC_RELATION_L.setText("DC_RELATION");
			DC_COVERAGE_L.setText("DC_COVERAGE");
			DC_RIGHTS_L.setText("DC_RIGHTS");
			
			c.add(DC_TITLE_L);
			c.add(DC_TITLE_TF);
			
			c.add(DC_CREATOR_L);
			c.add(DC_CREATOR_TF);
			
			c.add(DC_SUBJECT_L);
			c.add(DC_SUBJECT_TF);
			
			c.add(DC_DESCRIPTION_L);
			c.add(DC_DESCRIPTION_TF);
			
			c.add(DC_PUBLISHER_L);
			c.add(DC_PUBLISHER_TF);
			
			c.add(DC_CONTRIBUTOR_L);
			c.add(DC_CONTRIBUTOR_TF);
			
//			c.add(DC_DATE_L);
//			c.add(DC_DATE_TF);
			
			c.add(DC_TYPE_L);
			c.add(DC_TYPE_TF);
			
			c.add(DC_FORMAT_L);
			c.add(DC_FORMAT_TF);
						
			c.add(DC_SOURCE_L);
			c.add(DC_SOURCE_TF);
			
//			c.add(DC_LANGUAGE_L);
//			c.add(DC_LANGUAGE_TF);
			
			c.add(DC_RELATION_L);
			c.add(DC_RELATION_TF);
			
			c.add(DC_COVERAGE_L);
			c.add(DC_COVERAGE_TF);
			
			c.add(DC_RIGHTS_L);
			c.add(DC_RIGHTS_TF);
			
			// create scrollable conainer
			ScrollPane sp = new ScrollPane();
			add(sp, BorderLayout.CENTER);
			sp.add(c);
		}
		
		// add bottom buttons
		JButton okBtn = new JButton("Close");
		okBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				commitChanges();
				setVisible(false);
			}
		});
		add(okBtn, BorderLayout.SOUTH);
	}	

	private void showMetadata(Metadata metadata) {
		DC_TITLE_TF.setText(Metadata.getCommaSeparatedList(metadata, DC_TITLE));
		DC_CREATOR_TF.setText(Metadata.getCommaSeparatedList(metadata, DC_CREATOR));
		DC_SUBJECT_TF.setText(Metadata.getCommaSeparatedList(metadata, DC_SUBJECT));
		DC_DESCRIPTION_TF.setText(Metadata.getCommaSeparatedList(metadata, DC_DESCRIPTION));
		DC_PUBLISHER_TF.setText(Metadata.getCommaSeparatedList(metadata, DC_PUBLISHER));
		DC_CONTRIBUTOR_TF.setText(Metadata.getCommaSeparatedList(metadata, DC_CONTRIBUTOR));
//		DC_DATE_TF.setText(Metadata.getCommaSeparatedList(metadata, DC_DATE));
		DC_TYPE_TF.setText(Metadata.getCommaSeparatedList(metadata, DC_TYPE));
		DC_FORMAT_TF.setText(Metadata.getCommaSeparatedList(metadata, DC_FORMAT));
//		DC_LANGUAGE_TF.setText(Metadata.getCommaSeparatedList(metadata, DC_LANGUAGE));
		DC_RELATION_TF.setText(Metadata.getCommaSeparatedList(metadata, DC_RELATION));
		DC_SOURCE_TF.setText(Metadata.getCommaSeparatedList(metadata, DC_SOURCE));
		DC_COVERAGE_TF.setText(Metadata.getCommaSeparatedList(metadata, DC_COVERAGE));
		DC_RIGHTS_TF.setText(Metadata.getCommaSeparatedList(metadata, DC_RIGHTS));
	}	

	private void commitChanges() {
		if (currIndex == -1) {
			return;
		}

		AudioItem audioItem = audioItemList.get(currIndex);
		Metadata metadata = audioItem.getLocalizedAudioItem(LanguageUtil.getUserChoosenLanguage()).getMetadata();
		
		setValues(DC_TITLE, metadata, DC_TITLE_TF.getText());
		setValues(DC_CREATOR, metadata, DC_CREATOR_TF.getText());
		setValues(DC_SUBJECT, metadata, DC_SUBJECT_TF.getText());
		setValues(DC_DESCRIPTION, metadata, DC_DESCRIPTION_TF.getText());
		setValues(DC_PUBLISHER, metadata, DC_PUBLISHER_TF.getText());
		setValues(DC_CONTRIBUTOR, metadata, DC_CONTRIBUTOR_TF.getText());
		//setValues(DC_DATE, metadata, DC_DATE_TF.getText());
		setValues(DC_TYPE, metadata, DC_TYPE_TF.getText());
		setValues(DC_FORMAT, metadata, DC_FORMAT_TF.getText());
		//setValues(DC_LANGUAGE, metadata, DC_LANGUAGE_TF.getText());
		setValues(DC_RELATION, metadata, DC_RELATION_TF.getText());
		setValues(DC_SOURCE, metadata, DC_SOURCE_TF.getText());
		setValues(DC_COVERAGE, metadata, DC_COVERAGE_TF.getText());
		setValues(DC_RIGHTS, metadata, DC_RIGHTS_TF.getText());

		metadata.commit();
	}	
	
	private void setValues(MetadataField<String> field, Metadata metadata, String commaSeparatedListValue) {
		StringTokenizer t = new StringTokenizer(commaSeparatedListValue, ",");
		
		List<MetadataValue<String>> existingValues = metadata.getMetadataValues(field);
		Iterator<MetadataValue<String>> it = null;
		
		if (existingValues != null) {
			it = existingValues.iterator();
		}
		
		while (t.hasMoreTokens()) {
			String value = t.nextToken().trim();
			if (it != null && it.hasNext()) {
				it.next().setValue(value);
			} else {
				metadata.addMetadataField(field, new MetadataValue<String>(value));
			}
		}
	}
	
}
