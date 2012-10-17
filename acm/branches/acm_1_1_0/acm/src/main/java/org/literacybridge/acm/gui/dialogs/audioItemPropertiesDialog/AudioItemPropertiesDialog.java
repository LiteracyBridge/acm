package org.literacybridge.acm.gui.dialogs.audioItemPropertiesDialog;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.jdesktop.swingx.decorator.HighlighterFactory;
import org.literacybridge.acm.content.AudioItem;
import org.literacybridge.acm.metadata.Metadata;
import org.literacybridge.acm.gui.resourcebundle.LabelProvider;
import org.literacybridge.acm.gui.Application;
import org.literacybridge.acm.gui.ResourceView.ResourceView;
import org.literacybridge.acm.gui.ResourceView.audioItems.AudioItemView;
import org.literacybridge.acm.gui.messages.RequestAndSelectAudioItemMessage;
import org.literacybridge.acm.gui.messages.RequestAudioItemMessage;
import org.literacybridge.acm.gui.messages.RequestedAudioItemMessage;
import org.literacybridge.acm.gui.util.FocusTraversalOnArray;
import org.literacybridge.acm.gui.util.language.LanguageUtil;
import javax.swing.BoxLayout;
import java.awt.Component;
import javax.swing.Box;

public class AudioItemPropertiesDialog extends JDialog implements Observer {

	private static final long serialVersionUID = -3854016276035587383L;
	private AudioItemPropertiesTable propertiesTable = null;

	private JButton backBtn;
	private JButton nextBtn;
	private JButton btnClose;
	
	private final boolean readOnly;

	public AudioItemPropertiesDialog(JFrame parent, AudioItemView view,
			List<AudioItem> audioItemList, AudioItem showItem,
			final boolean readOnly) {
		super(parent, LabelProvider.getLabel("AUDIO_ITEM_PROPERTIES",
				LanguageUtil.getUILanguage()), true);
		this.readOnly = readOnly;
		addToMessageService();
		createControlsForAvailableProperties(readOnly);
		setMinimumSize(new Dimension(500, 500));
		setSize(500, 500);
		pack();
		setFocusTraversalPolicy(new FocusTraversalOnArray(new Component[]{backBtn, nextBtn, btnClose}));

		// show current item first
		RequestAndSelectAudioItemMessage msg = new RequestAndSelectAudioItemMessage(RequestAudioItemMessage.RequestType.Current);
		Application.getMessageService().pumpMessage(msg);
		
		enableControls();
	}

	protected void addToMessageService() {
		Application.getMessageService().addObserver(this);
	}
	
	private void showAudioItem(AudioItem item) {
		if (item == null)
			return; // JTBD
		Metadata metadata = item.getLocalizedAudioItem(
				LanguageUtil.getUserChoosenLanguage()).getMetadata();
		showMetadata(item, metadata);


	}

	private void getNextItem() {

		RequestAndSelectAudioItemMessage msg = new RequestAndSelectAudioItemMessage(RequestAudioItemMessage.RequestType.Next);
		Application.getMessageService().pumpMessage(msg);
	}

	private void getPrevItem() {
		RequestAndSelectAudioItemMessage msg = new RequestAndSelectAudioItemMessage(RequestAudioItemMessage.RequestType.Previews);
		Application.getMessageService().pumpMessage(msg);
	}

	private void enableControls() {
		// always true, if no audio items available ... no action
		nextBtn.setEnabled(true);
		backBtn.setEnabled(true);
	}

	private void createControlsForAvailableProperties(final boolean readOnly) {
		// add navigation buttons
		JPanel p = new JPanel();
		
		backBtn = new JButton(LabelProvider.getLabel("GOTO_PREV_AUDIO_ITEM", LanguageUtil.getUILanguage()));
		backBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				getPrevItem();
				enableControls();
			}
		});
		p.add(backBtn);
		
		nextBtn = new JButton(LabelProvider.getLabel("GOTO_NEXT_AUDIO_ITEM", LanguageUtil.getUILanguage()));
		nextBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				getNextItem();
				enableControls();
			}
		});
		p.add(nextBtn);
		
		getContentPane().add(p, BorderLayout.NORTH);
		
		p.setBorder(BorderFactory.createEmptyBorder());
	
		// Show properties table
		JScrollPane theScrollPane = new JScrollPane();
		propertiesTable = new AudioItemPropertiesTable();
		propertiesTable.setShowGrid(false, false);
		// use fixed color; there seems to be a bug in some plaf
		// implementations that cause strange rendering
		propertiesTable.addHighlighter(HighlighterFactory
				.createAlternateStriping(Color.white, new Color(237, 243,
						254)));
		theScrollPane.setViewportView(propertiesTable);
		getContentPane().add(theScrollPane);
		
		JPanel panel = new JPanel();
		getContentPane().add(panel, BorderLayout.SOUTH);
		panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
		
		Component horizontalGlue = Box.createHorizontalGlue();
		panel.add(horizontalGlue);
		
		btnClose = new JButton(LabelProvider.getLabel("CLOSE", LanguageUtil.getUILanguage()));
		btnClose.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				// update IRequestResult
				ResourceView.updateDataRequestResult();
				Application.getFilterState().updateResult();
				setVisible(false);
			}
		});
		panel.add(btnClose);
	}

	private void showMetadata(AudioItem audioItem, Metadata metadata) {
		propertiesTable.setModel(new AudioItemPropertiesModel(audioItem, metadata, this.readOnly));
	}

	@Override
	public void update(Observable o, Object arg) {
		if (arg instanceof RequestedAudioItemMessage) {
			RequestedAudioItemMessage msg = (RequestedAudioItemMessage) arg;
			showAudioItem(msg.getAudioItem());
		}
		
	}
}
