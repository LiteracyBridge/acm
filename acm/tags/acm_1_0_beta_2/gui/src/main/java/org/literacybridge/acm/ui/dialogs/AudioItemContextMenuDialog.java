package org.literacybridge.acm.ui.dialogs;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.SystemColor;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

import org.literacybridge.acm.api.IDataRequestResult;
import org.literacybridge.acm.content.AudioItem;
import org.literacybridge.acm.content.LocalizedAudioItem;
import org.literacybridge.acm.metadata.MetadataSpecification;
import org.literacybridge.acm.resourcebundle.LabelProvider;
import org.literacybridge.acm.ui.Application;
import org.literacybridge.acm.ui.UIConstants;
import org.literacybridge.acm.ui.ResourceView.audioItems.AudioItemView;
import org.literacybridge.acm.ui.dialogs.audioItemPropertiesDialog.AudioItemPropertiesDialog;
import org.literacybridge.acm.util.language.LanguageUtil;

// TODO: deal with localized audio items when languages are fully implemented
public class AudioItemContextMenuDialog extends JDialog implements WindowListener {
	public AudioItemContextMenuDialog(final JFrame parent, final AudioItem clickedAudioItem, final AudioItem[] selectedAudioItems, 
			final AudioItemView audioItemView, final IDataRequestResult data) {
		super(parent, "", false);
		
		setResizable(false);
		setUndecorated(true);		
		
		ImageIcon editImageIcon = new ImageIcon(UIConstants.getResource(UIConstants.ICON_EDIT_16_PX));
		ImageIcon deleteImageIcon = new ImageIcon(UIConstants.getResource(UIConstants.ICON_DELETE_16_PX));
		ImageIcon exportImageIcon = new ImageIcon(UIConstants.getResource(UIConstants.ICON_EXPORT_16_PX));
		
		Color backgroundColor = parent.getBackground();
		Color highlightedColor = SystemColor.textHighlight;
		
		GridLayout grid = new GridLayout(3, 1);
		
		final String selectedTitle = clickedAudioItem.getLocalizedAudioItem(
				LanguageUtil.getUserChoosenLanguage()).getMetadata().getMetadataValues(MetadataSpecification.DC_TITLE).get(0).toString();
		
		String labelPostfix;
		final String deleteMessage;
		if (selectedAudioItems.length > 1) {
			labelPostfix = String.format(LabelProvider.getLabel("AUDIO_ITEM_CONTEXT_MENU_DIALOG_LABEL_POSTFIX", LanguageUtil.getUILanguage())
											, selectedAudioItems.length);
			deleteMessage = String.format(LabelProvider.getLabel("AUDIO_ITEM_CONTEXT_MENU_DIALOG_DELETE_ITEMS", LanguageUtil.getUILanguage())
											, selectedAudioItems.length);
		} else {
			labelPostfix = selectedTitle;
			deleteMessage = String.format(LabelProvider.getLabel("AUDIO_ITEM_CONTEXT_MENU_DIALOG_DELETE_TITLE", LanguageUtil.getUILanguage())
											, selectedTitle);
		}
		
		FlatButton deleteButton = new FlatButton(String.format(LabelProvider.getLabel("AUDIO_ITEM_CONTEXT_MENU_DIALOG_DELETE", LanguageUtil.getUILanguage())
																	, labelPostfix)
											, deleteImageIcon
											, backgroundColor
											, highlightedColor) {
			@Override
			public void click() {
				AudioItemContextMenuDialog.this.setVisible(false);
				
				Object[] options = {LabelProvider.getLabel("CANCEL", LanguageUtil.getUILanguage())
									, LabelProvider.getLabel("DELETE", LanguageUtil.getUILanguage())};
				int n = JOptionPane.showOptionDialog(Application.getApplication(),
				    deleteMessage,
				    LabelProvider.getLabel("CONFRIM_DELETE", LanguageUtil.getUILanguage()),
				    JOptionPane.OK_CANCEL_OPTION,
				    JOptionPane.QUESTION_MESSAGE,
				    null,
				    options,
				    options[0]);

				if (n == 1) {
					for (AudioItem a : selectedAudioItems) {
						a.destroy();
					}
					Application.getFilterState().updateResult();
				}

			}
			
		};

		FlatButton editButton = new FlatButton(String.format(LabelProvider.getLabel("AUDIO_ITEM_CONTEXT_MENU_DIALOG_EDIT_TITLE", LanguageUtil.getUILanguage()), selectedTitle)
									, editImageIcon
									, backgroundColor
									, highlightedColor) {
			@Override
			public void click() {
				AudioItemContextMenuDialog.this.setVisible(false);
				AudioItemPropertiesDialog dlg = new AudioItemPropertiesDialog(Application.getApplication()
					, audioItemView
					, data.getAudioItems()
					, clickedAudioItem);
				dlg.setVisible(true);				
			}
		};

		FlatButton exportButton = new FlatButton(String.format(LabelProvider.getLabel("AUDIO_ITEM_CONTEXT_MENU_DIALOG_EXPORT_TITLE", LanguageUtil.getUILanguage()), labelPostfix)
									, exportImageIcon
									, backgroundColor
									, highlightedColor) {
			@Override
			public void click() {
				AudioItemContextMenuDialog.this.setVisible(false);
				LocalizedAudioItem[] localizedItems = new LocalizedAudioItem[selectedAudioItems.length];
				for (int i = 0; i < selectedAudioItems.length; i++) {
					localizedItems[i] = selectedAudioItems[i].getLocalizedAudioItem(
							LanguageUtil.getUserChoosenLanguage());
				}
				ExportDialog export = new ExportDialog(localizedItems);
				export.setVisible(true);
			}
		};
		
		setLayout(grid);
		
		editButton.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		deleteButton.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		exportButton.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		add(editButton);
		add(exportButton);
		add(deleteButton);
		
		addWindowListener(this);
		setAlwaysOnTop(true);
		setSize(new Dimension(300, 100));
	}
	
	@Override
	public void windowDeactivated(WindowEvent e) {
		setVisible(false);
	}
	
	public abstract static class FlatButton extends JLabel implements MouseListener {
		private Color backgroundColor;
		private Color highlightedColor;

		public FlatButton(String label, Color backgroundColor, Color highlightedColor) {
			super(label);
			init(backgroundColor, highlightedColor);
		}
		
		public FlatButton(String label, Icon icon, Color backgroundColor, Color highlightedColor) {
			super(label, icon, JLabel.LEFT);
			init(backgroundColor, highlightedColor);
		}

		private final void init(Color backgroundColor, Color highlightedColor) {
			this.backgroundColor = backgroundColor;
			this.highlightedColor = highlightedColor;
			setOpaque(true);
			addMouseListener(this);
		}
		
		public abstract void click();
		
		@Override
		public void mouseClicked(MouseEvent e) {
			click();
		}

		@Override
		public void mouseExited(MouseEvent e) {
			setBackground(backgroundColor);
		}
		
		@Override
		public void mouseEntered(MouseEvent e) {
			setBackground(highlightedColor);
		}

		@Override
		public void mousePressed(MouseEvent e) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			// TODO Auto-generated method stub
			
		}
		
		
	}

	@Override public void windowActivated(WindowEvent e) {}
	@Override public void windowClosed(WindowEvent e) {}
	@Override public void windowClosing(WindowEvent e) {}
	@Override public void windowDeiconified(WindowEvent e) {}
	@Override public void windowIconified(WindowEvent e) {}
	@Override public void windowOpened(WindowEvent e) {}
}
