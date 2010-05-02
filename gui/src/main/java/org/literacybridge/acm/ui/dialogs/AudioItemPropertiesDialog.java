package org.literacybridge.acm.ui.dialogs;

import static org.literacybridge.acm.metadata.MetadataSpecification.DC_CONTRIBUTOR;
import static org.literacybridge.acm.metadata.MetadataSpecification.DC_COVERAGE;
import static org.literacybridge.acm.metadata.MetadataSpecification.DC_CREATOR;
import static org.literacybridge.acm.metadata.MetadataSpecification.DC_DATE;
import static org.literacybridge.acm.metadata.MetadataSpecification.DC_DESCRIPTION;
import static org.literacybridge.acm.metadata.MetadataSpecification.DC_FORMAT;
import static org.literacybridge.acm.metadata.MetadataSpecification.DC_IDENTIFIER;
import static org.literacybridge.acm.metadata.MetadataSpecification.DC_LANGUAGE;
import static org.literacybridge.acm.metadata.MetadataSpecification.DC_PUBLISHER;
import static org.literacybridge.acm.metadata.MetadataSpecification.DC_RELATION;
import static org.literacybridge.acm.metadata.MetadataSpecification.DC_RIGHTS;
import static org.literacybridge.acm.metadata.MetadataSpecification.DC_SOURCE;
import static org.literacybridge.acm.metadata.MetadataSpecification.DC_SUBJECT;
import static org.literacybridge.acm.metadata.MetadataSpecification.DC_TITLE;
import static org.literacybridge.acm.metadata.MetadataSpecification.DC_TYPE;
import static org.literacybridge.acm.metadata.MetadataSpecification.DTB_REVISION;
import static org.literacybridge.acm.metadata.MetadataSpecification.DTB_REVISION_DATE;
import static org.literacybridge.acm.metadata.MetadataSpecification.DTB_REVISION_DESCRIPTION;
import static org.literacybridge.acm.metadata.MetadataSpecification.LB_COPY_COUNT;
import static org.literacybridge.acm.metadata.MetadataSpecification.LB_PLAY_COUNT;
import static org.literacybridge.acm.metadata.MetadataSpecification.LB_RATING;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.ScrollPane;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import org.literacybridge.acm.content.AudioItem;
import org.literacybridge.acm.metadata.LBMetadataIDs;
import org.literacybridge.acm.metadata.Metadata;
import org.literacybridge.acm.resourcebundle.LabelProvider;
import org.literacybridge.acm.util.ClosingDialogAdapter;
import org.literacybridge.acm.util.LanguageUtil;

public class AudioItemPropertiesDialog extends JDialog {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3854016276035587383L;
	private List<AudioItem> audioItemList = null;
	private int currIndex = 0;
	

	private Label DC_TITLE_L = new Label();
	private Label DC_CREATOR_L = new Label();
	private Label DC_SUBJECT_L = new Label();
	private Label DC_DESCRIPTION_L = new Label();
	private Label DC_PUBLISHER_L = new Label();
	private Label DC_CONTRIBUTOR_L = new Label();
	private Label DC_DATE_L = new Label();
	private Label DC_TYPE_L = new Label();
	private Label DC_FORMAT_L = new Label();
	private Label DC_IDENTIFIER_L = new Label();
	private Label DC_SOURCE_L = new Label();
	private Label DC_LANGUAGE_L = new Label();
	private Label DC_RELATION_L = new Label();
	private Label DC_COVERAGE_L = new Label();
	private Label DC_RIGHTS_L = new Label();
	private Label DTB_REVISION_L = new Label();
	private Label DTB_REVISION_DATE_L = new Label();
	private Label DTB_REVISION_DESCRIPTION_L = new Label();
	private Label LB_COPY_COUNT_L = new Label();
	private Label LB_PLAY_COUNT_L = new Label();
	private Label LB_RATING_L = new Label();
	
	private TextField DC_TITLE_TF = new TextField();
	private TextField DC_CREATOR_TF = new TextField();
	private TextField DC_SUBJECT_TF = new TextField();
	private TextField DC_DESCRIPTION_TF = new TextField();
	private TextField DC_PUBLISHER_TF = new TextField();
	private TextField DC_CONTRIBUTOR_TF = new TextField();
	private TextField DC_DATE_TF = new TextField();
	private TextField DC_TYPE_TF = new TextField();
	private TextField DC_FORMAT_TF = new TextField();
	private TextField DC_IDENTIFIER_TF = new TextField();
	private TextField DC_SOURCE_TF = new TextField();
	private TextField DC_LANGUAGE_TF = new TextField();
	private TextField DC_RELATION_TF = new TextField();
	private TextField DC_COVERAGE_TF = new TextField();
	private TextField DC_RIGHTS_TF = new TextField();
	private TextField DTB_REVISION_TF = new TextField();
	private TextField DTB_REVISION_DATE_TF = new TextField();
	private TextField DTB_REVISION_DESCRIPTION_TF = new TextField();
	private TextField LB_COPY_COUNT_TF = new TextField();
	private TextField LB_PLAY_COUNT_TF = new TextField();
	private TextField LB_RATING_TF = new TextField();
	
	private JButton backBtn = null;
	private JButton nextBtn = null;
	
	public AudioItemPropertiesDialog(JFrame parent, List<AudioItem> audioItemList, AudioItem showItem) {
		super(parent, "AudioItem Properties", true);
		this.audioItemList = audioItemList;
		currIndex = getIndexOfAudioItem(showItem);
		
		createControlsForAvailableProperties();
		pack();
		setSize(500, 500);
		
		AudioItem audioItem = audioItemList.get(currIndex);
		showAudioItem(audioItem);
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
	}
	
	private AudioItem getNextItem() {
		if (audioItemList.size() > currIndex+1) {
			return audioItemList.get(++currIndex);			
		}
		
		return null;
	}
	
	private AudioItem getPrevItem() {
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
			DC_DATE_L.setText("DC_DATE");
			DC_TYPE_L.setText("DC_TYPE");
			DC_FORMAT_L.setText("DC_FORMAT");
			DC_IDENTIFIER_L.setText("DC_IDENTIFIER");
			DC_SOURCE_L.setText("DC_SOURCE");
			DC_LANGUAGE_L.setText("DC_LANGUAGE");
			DC_RELATION_L.setText("DC_RELATION");
			DC_COVERAGE_L.setText("DC_COVERAGE");
			DC_RIGHTS_L.setText("DC_RIGHTS");
			DTB_REVISION_L.setText("DTB_REVISION");
			DTB_REVISION_DATE_L.setText("DTB_REVISION_DATE");
			DTB_REVISION_DESCRIPTION_L.setText("DTB_REVISION_DESCRIPTION");
			LB_COPY_COUNT_L.setText("LB_COPY_COUNT");
			LB_PLAY_COUNT_L.setText("LB_PLAY_COUNT");
			LB_RATING_L.setText("LB_RATING");
			
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
			
			c.add(DC_DATE_L);
			c.add(DC_DATE_TF);
			
			c.add(DC_TYPE_L);
			c.add(DC_TYPE_TF);
			
			c.add(DC_FORMAT_L);
			c.add(DC_FORMAT_TF);
			
			c.add(DC_IDENTIFIER_L);
			c.add(DC_IDENTIFIER_TF);
			
			c.add(DC_SOURCE_L);
			c.add(DC_SOURCE_TF);
			
			c.add(DC_LANGUAGE_L);
			c.add(DC_LANGUAGE_TF);
			
			c.add(DC_RELATION_L);
			c.add(DC_RELATION_TF);
			
			c.add(DC_COVERAGE_L);
			c.add(DC_COVERAGE_TF);
			
			c.add(DC_RIGHTS_L);
			c.add(DC_RIGHTS_TF);
			
			c.add(DTB_REVISION_L);
			c.add(DTB_REVISION_TF);
			
			c.add(DTB_REVISION_DATE_L);
			c.add(DTB_REVISION_DATE_TF);
			
			c.add(DTB_REVISION_DESCRIPTION_L);
			c.add(DTB_REVISION_DESCRIPTION_TF);
			
			c.add(LB_COPY_COUNT_L);
			c.add(LB_COPY_COUNT_TF);
			
			c.add(LB_PLAY_COUNT_L);
			c.add(LB_PLAY_COUNT_TF);
			
			c.add(LB_RATING_L);
			c.add(LB_RATING_TF);
			
			
			
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

	private void showMetadata(Metadata metadata) {
		setTitle(metadata);
		setCreator(metadata);
		setSubject(metadata);
		setDescription(metadata);
		setPublisher(metadata);
		setContribute(metadata);
		setDate(metadata);
		setType(metadata);
		setFormat(metadata);
		setIdentifier(metadata);
		setLanguage(metadata);
		setRelation(metadata);
		setSource(metadata);
		setCoverage(metadata);
		setRights(metadata);
		setRevision(metadata);
		setRevisionDate(metadata);
		setRevisionDescription(metadata);
		setCopyCount(metadata);
		setPlayCount(metadata);
		setRating(metadata);		
	}
	
	private void setTitle(Metadata metadata) {
		DC_TITLE_TF.setText(Metadata.getCommaSeparatedList(metadata, DC_TITLE));
	}

	private void setCreator(Metadata metadata) {
		DC_CREATOR_TF.setText(Metadata.getCommaSeparatedList(metadata, DC_CREATOR));
	}
	
	private void setSubject(Metadata metadata) {
		DC_SUBJECT_TF.setText(Metadata.getCommaSeparatedList(metadata, DC_SUBJECT));
	}
	
	private void setDescription(Metadata metadata) {
		DC_DESCRIPTION_TF.setText(Metadata.getCommaSeparatedList(metadata, DC_DESCRIPTION));
	}
	
	private void setPublisher(Metadata metadata) {
		DC_PUBLISHER_TF.setText(Metadata.getCommaSeparatedList(metadata, DC_PUBLISHER));
	}
	
	private void setContribute(Metadata metadata) {
		DC_CONTRIBUTOR_TF.setText(Metadata.getCommaSeparatedList(metadata, DC_CONTRIBUTOR));
	}
	
	private void setDate(Metadata metadata) {
		DC_DATE_TF.setText(Metadata.getCommaSeparatedList(metadata, DC_DATE));
	}
	
	private void setType(Metadata metadata) {
		DC_TYPE_TF.setText(Metadata.getCommaSeparatedList(metadata, DC_TYPE));
	}
	
	private void setFormat(Metadata metadata) {
		DC_FORMAT_TF.setText(Metadata.getCommaSeparatedList(metadata, DC_FORMAT));
	}

	private void setIdentifier(Metadata metadata) {
		DC_IDENTIFIER_TF.setText(Metadata.getCommaSeparatedList(metadata, DC_IDENTIFIER));
	}
	
	private void setLanguage(Metadata metadata) {
		DC_LANGUAGE_TF.setText(Metadata.getCommaSeparatedList(metadata, DC_LANGUAGE));
	}
	
	private void setRelation(Metadata metadata) {
		DC_RELATION_TF.setText(Metadata.getCommaSeparatedList(metadata, DC_RELATION));
	}

	private void setSource(Metadata metadata) {
		DC_SOURCE_TF.setText(Metadata.getCommaSeparatedList(metadata, DC_SOURCE));
	}
	
	private void setCoverage(Metadata metadata) {
		DC_COVERAGE_TF.setText(Metadata.getCommaSeparatedList(metadata, DC_COVERAGE));
	}
	
	private void setRights(Metadata metadata) {
		DC_RIGHTS_TF.setText(Metadata.getCommaSeparatedList(metadata, DC_RIGHTS));
	}
	
	private void setRevision(Metadata metadata) {
		DTB_REVISION_TF.setText(Metadata.getCommaSeparatedList(metadata, DTB_REVISION));
	}
	
	private void setRevisionDate(Metadata metadata) {
		DTB_REVISION_DATE_TF.setText(Metadata.getCommaSeparatedList(metadata, DTB_REVISION_DATE));
	}
	
	private void setRevisionDescription(Metadata metadata) {
		DTB_REVISION_DESCRIPTION_TF.setText(Metadata.getCommaSeparatedList(metadata, DTB_REVISION_DESCRIPTION));
	}

	private void setCopyCount(Metadata metadata) {
		LB_COPY_COUNT_TF.setText(Metadata.getCommaSeparatedList(metadata, LB_COPY_COUNT));
	}

	private void setPlayCount(Metadata metadata) {
		LB_PLAY_COUNT_TF.setText(Metadata.getCommaSeparatedList(metadata, LB_PLAY_COUNT));
	}

	private void setRating(Metadata metadata) {
		LB_RATING_TF.setText(Metadata.getCommaSeparatedList(metadata, LB_RATING));
	}	
}
