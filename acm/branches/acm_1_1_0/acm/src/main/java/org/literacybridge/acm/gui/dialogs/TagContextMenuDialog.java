package org.literacybridge.acm.gui.dialogs;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.SystemColor;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

import org.literacybridge.acm.api.IDataRequestResult;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.config.ControlAccess;
import org.literacybridge.acm.db.PersistentTag;
import org.literacybridge.acm.gui.Application;
import org.literacybridge.acm.gui.UIConstants;
import org.literacybridge.acm.gui.resourcebundle.LabelProvider;
import org.literacybridge.acm.gui.util.language.LanguageUtil;
//import org.literacybridge.acm.content.AudioItem;
//import org.literacybridge.acm.content.LocalizedAudioItem;
//import org.literacybridge.acm.gui.ResourceView.audioItems.AudioItemView;
//import org.literacybridge.acm.gui.dialogs.audioItemPropertiesDialog.AudioItemPropertiesDialog;

public class TagContextMenuDialog extends JDialog implements WindowListener {
	private static final Logger LOG = Logger.getLogger(TagContextMenuDialog.class.getName());
	
	public TagContextMenuDialog(final JFrame parent, final PersistentTag clickedTag, 
			final IDataRequestResult data) {
		super(parent, "", false);
		
		final boolean readOnly = ACMConfiguration.getCurrentDB().getControlAccess().isACMReadOnly();
		
		setResizable(false);
		setUndecorated(true);		
		
		ImageIcon editImageIcon = new ImageIcon(UIConstants.getResource(UIConstants.ICON_EDIT_16_PX));
		ImageIcon deleteImageIcon = new ImageIcon(UIConstants.getResource(UIConstants.ICON_DELETE_16_PX));
		
		Color backgroundColor = parent.getBackground();
		Color highlightedColor = SystemColor.textHighlight;
		
		GridLayout grid = new GridLayout(3, 1);
		
		
		final PersistentTag selectedTag = Application.getFilterState().getSelectedTag();
		final String selectedTitle = selectedTag.getName();
		
		String labelPostfix;	    
	    final FlatButton deleteButton;
		
		if (selectedTag == null) {
			final String deleteMessage;
			
			labelPostfix = selectedTitle;
			deleteMessage = String.format(LabelProvider.getLabel("AUDIO_ITEM_CONTEXT_MENU_DIALOG_DELETE_TITLE", LanguageUtil.getUILanguage())
												, selectedTitle);
			
			deleteButton = new FlatButton(String.format(LabelProvider.getLabel("AUDIO_ITEM_CONTEXT_MENU_DIALOG_DELETE", LanguageUtil.getUILanguage())
					, labelPostfix)
					, deleteImageIcon
					, backgroundColor
					, highlightedColor) {
				@Override public void click() {
					TagContextMenuDialog.this.setVisible(false);
					
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
							try {
								//a.destroy();
								//Configuration.getRepository().delete(a);
							} catch (Exception e) {
//								LOG.log(Level.WARNING, "Unable to delete audioitem id=" + a.getUuid(), e);
							}
						Application.getFilterState().updateResult(true);
					}
				}				
			};
		} else {
			labelPostfix = selectedTitle;
			
			deleteButton = new FlatButton(String.format(LabelProvider.getLabel("AUDIO_ITEM_CONTEXT_MENU_DIALOG_REMOVE_TAG", LanguageUtil.getUILanguage())
					, labelPostfix, selectedTag.getName())
					, deleteImageIcon
					, backgroundColor
					, highlightedColor) {
				@Override public void click() {
					TagContextMenuDialog.this.setVisible(false);
					
						try {
//							a.removeTag(selectedTag);
//							a.commit();
						} catch (Exception e) {
//							LOG.log(Level.WARNING, "Unable to remove audioitem id=" + a.getUuid() + " from tag " + selectedTag.getName(), e);
						}
					Application.getFilterState().updateResult(true);

				}				
			};

		}
		
		final String editButtonLabel = readOnly
						? LabelProvider.getLabel("AUDIO_ITEM_CONTEXT_MENU_DIALOG_VIEW_PROPS_TITLE", LanguageUtil.getUILanguage())
						: LabelProvider.getLabel("AUDIO_ITEM_CONTEXT_MENU_DIALOG_EDIT_TITLE", LanguageUtil.getUILanguage());


		FlatButton editButton = new FlatButton(String.format(editButtonLabel, selectedTitle)
									, editImageIcon
									, backgroundColor
									, highlightedColor) {
			@Override
			public void click() {
				TagContextMenuDialog.this.setVisible(false);
				// Call add new playlist dialog but relabel it and use it for edit
				// Also populate the text field with current name
			}
		};
		
		if (ACMConfiguration.getCurrentDB().getControlAccess().isACMReadOnly()) {
			deleteButton.setEnabled(false);
		}
		
		setLayout(grid);
		
		editButton.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		deleteButton.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		add(editButton);
		add(deleteButton);
		
		addWindowListener(this);
		setAlwaysOnTop(true);
		setSize(new Dimension(450, 100));
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
			if (isEnabled()) {
				click();
			}
		}

		@Override
		public void mouseExited(MouseEvent e) {
			if (isEnabled()) {
				setBackground(backgroundColor);
			}
		}
		
		@Override
		public void mouseEntered(MouseEvent e) {
			if (isEnabled()) {
				setBackground(highlightedColor);
			}
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
