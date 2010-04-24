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
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import org.literacybridge.acm.content.AudioItem;
import org.literacybridge.acm.metadata.Metadata;
import org.literacybridge.acm.metadata.MetadataSpecification;
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
			Metadata metaData = audioItem.getLocalizedAudioItem(LanguageUtil.getUserChoosenLanguage()).getMetadata();
			
			Field[] fields = MetadataSpecification.class.getFields();
			
			Container c = new Container();
			c.setLayout(new GridLayout(fields.length, 2));
				
			// Create for every metadata a control to edit it
			for (int i = 0; i < fields.length; i++) {
				Field f = fields[i];
				Label text = new Label(f.getName() + ": ");
				c.add(text);
				c.add(new TextField("my value is coming soon"));
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
