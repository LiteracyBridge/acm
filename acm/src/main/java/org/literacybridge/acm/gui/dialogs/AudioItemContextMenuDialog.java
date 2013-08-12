package org.literacybridge.acm.gui.dialogs;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.SystemColor;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

import org.literacybridge.acm.api.IDataRequestResult;
import org.literacybridge.acm.config.Configuration;
import org.literacybridge.acm.config.ControlAccess;
import org.literacybridge.acm.content.AudioItem;
import org.literacybridge.acm.content.LocalizedAudioItem;
import org.literacybridge.acm.db.PersistentAudioItem;
import org.literacybridge.acm.db.PersistentTag;
import org.literacybridge.acm.db.PersistentTagOrdering;
import org.literacybridge.acm.gui.Application;
import org.literacybridge.acm.gui.UIConstants;
import org.literacybridge.acm.gui.ResourceView.audioItems.AudioItemView;
import org.literacybridge.acm.gui.dialogs.audioItemPropertiesDialog.AudioItemPropertiesDialog;
import org.literacybridge.acm.gui.resourcebundle.LabelProvider;
import org.literacybridge.acm.gui.util.language.LanguageUtil;
import org.literacybridge.acm.metadata.MetadataSpecification;

// TODO: deal with localized audio items when languages are fully implemented
public class AudioItemContextMenuDialog extends JDialog implements WindowListener {
	private static final Logger LOG = Logger.getLogger(AudioItemContextMenuDialog.class.getName());
	
	public AudioItemContextMenuDialog(final JFrame parent, final AudioItem clickedAudioItem, final AudioItem[] selectedAudioItems, 
			final AudioItemView audioItemView, final IDataRequestResult data) {
		super(parent, "", false);
		
		final boolean readOnly = ControlAccess.isACMReadOnly();
		
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
		
		final PersistentTag selectedTag = Application.getFilterState().getSelectedTag();
		
		String labelPostfix;	    
	    final FlatButton deleteButton;
		
		if (selectedTag == null) {
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
			
			deleteButton = new FlatButton(String.format(LabelProvider.getLabel("AUDIO_ITEM_CONTEXT_MENU_DIALOG_DELETE", LanguageUtil.getUILanguage())
					, labelPostfix)
					, deleteImageIcon
					, backgroundColor
					, highlightedColor) {
				@Override public void click() {
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
							try {
								a.destroy();
								// it's okay to delete from DB but cannot delete the .a18 file since that's in the shared (dropbox) repository
								if (!ControlAccess.isACMReadOnly() && !ControlAccess.isSandbox())
									Configuration.getRepository().delete(a);
							} catch (Exception e) {
								LOG.log(Level.WARNING, "Unable to delete audioitem id=" + a.getUuid(), e);
							}
						}
						Application.getFilterState().updateResult(true);
					}

				}				
			};
		} else {
			if (selectedAudioItems.length > 1) {
				labelPostfix = String.format(LabelProvider.getLabel("AUDIO_ITEM_CONTEXT_MENU_DIALOG_LABEL_POSTFIX", LanguageUtil.getUILanguage())
												, selectedAudioItems.length);
			} else {
				labelPostfix = selectedTitle;
			}			
			
			deleteButton = new FlatButton(String.format(LabelProvider.getLabel("AUDIO_ITEM_CONTEXT_MENU_DIALOG_REMOVE_TAG", LanguageUtil.getUILanguage())
					, labelPostfix, selectedTag.getName())
					, deleteImageIcon
					, backgroundColor
					, highlightedColor) {
				@Override public void click() {
					AudioItemContextMenuDialog.this.setVisible(false);
					
					for (AudioItem a : selectedAudioItems) {
						try {
							int position = PersistentTagOrdering.getFromDatabase(a.getPersistentAudioItem(), selectedTag).getPosition();
							a.removeTag(selectedTag);
							a.commit();
							for (PersistentAudioItem item : selectedTag.getPersistentAudioItemList()) {
								PersistentTagOrdering ordering = PersistentTagOrdering.getFromDatabase(item, selectedTag);
								if (ordering.getPosition() > position) {
									ordering.setPosition(ordering.getPosition() - 1);
									ordering.commit();
								}
							}
						} catch (Exception e) {
							LOG.log(Level.WARNING, "Unable to remove audioitem id=" + a.getUuid() + " from tag " + selectedTag.getName(), e);
						}
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
				AudioItemContextMenuDialog.this.setVisible(false);
				AudioItemPropertiesDialog dlg = new AudioItemPropertiesDialog(Application.getApplication()
					, audioItemView
					, data.getAudioItems()
					, clickedAudioItem, readOnly);
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
		
		if (ControlAccess.isACMReadOnly()) {
			deleteButton.setEnabled(false);
		}
		
		setLayout(grid);
		
		editButton.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		deleteButton.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		exportButton.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		add(editButton);
		add(exportButton);
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
