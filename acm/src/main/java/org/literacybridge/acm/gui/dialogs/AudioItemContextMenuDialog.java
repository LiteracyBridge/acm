package org.literacybridge.acm.gui.dialogs;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.SystemColor;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.*;
import javax.swing.border.Border;

import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.gui.Application;
import org.literacybridge.acm.gui.UIConstants;
import org.literacybridge.acm.gui.MainWindow.audioItems.AudioItemView;
import org.literacybridge.acm.gui.dialogs.audioItemPropertiesDialog.AudioItemPropertiesDialog;
import org.literacybridge.acm.gui.dialogs.audioItemPropertiesDialog.LanguageComboBoxModel;
import org.literacybridge.acm.gui.resourcebundle.LabelProvider;
import org.literacybridge.acm.store.*;

import static org.literacybridge.acm.store.MetadataSpecification.DC_LANGUAGE;

// TODO: deal with localized audio items when languages are fully implemented
public class AudioItemContextMenuDialog extends JDialog {
  private static final Logger LOG = Logger
      .getLogger(AudioItemContextMenuDialog.class.getName());

  private String getMetadataTitle(final AudioItem clickedAudioItem) {
    Metadata md = clickedAudioItem.getMetadata();
    Object obj = md.getMetadataValue(MetadataSpecification.DC_TITLE);
    if (obj != null) {
      return obj.toString();
    }
    LOG.log(Level.SEVERE, "Unexpected null value for metadata value for title.");
    return "--";
  }

  public AudioItemContextMenuDialog(final JFrame parent,
      final AudioItem clickedAudioItem, final AudioItem[] selectedAudioItems,
      final AudioItemView audioItemView, final SearchResult data) {
    super(parent, "", false);

    setResizable(false);
    setUndecorated(true);

    // Any number of rows, 1 column.
    GridLayout grid = new GridLayout(0, 1);

    final String labelPostfix = getPostfixLabel(selectedAudioItems);

    FlatButton renameButton = makeRenameButton(selectedAudioItems, labelPostfix);
    FlatButton editButton = makeEditButton(selectedAudioItems);
    FlatButton removeButton = makeRemoveButton(selectedAudioItems, labelPostfix);
    FlatButton exportAudioButton = makeExportButton(selectedAudioItems, labelPostfix, ExportDialog.EXPORT_DATA_TYPE.Audio);
    FlatButton exportMetadataButton = makeExportButton(selectedAudioItems, labelPostfix, ExportDialog.EXPORT_DATA_TYPE.Metadata);
    FlatButton deleteButton = makeDeleteButton(selectedAudioItems, labelPostfix);
    FlatButton languageButton = makeLanguageButton(selectedAudioItems, labelPostfix);

    setLayout(grid);
    Border border = BorderFactory.createEmptyBorder(10, 10, 10, 10);

    renameButton.setBorder(border);
    editButton.setBorder(border);
    if (removeButton != null) {
        removeButton.setBorder(border);
    }
    exportAudioButton.setBorder(border);
    exportMetadataButton.setBorder(border);
    deleteButton.setBorder(border);
    languageButton.setBorder(border);

    add(renameButton);
    add(editButton);
    if (removeButton != null) {
        add(removeButton);
    }
    add(exportAudioButton);
    add(exportMetadataButton);
    add(deleteButton);
    add(languageButton);

    addWindowListener(windowListener);
    addKeyListener(keyListener);
    setAlwaysOnTop(true);
    setSize(new Dimension(450, 175));
  }

  /**
   * Helper to get the string "N Audio Items" or <audio item name>
   * @param selectedAudioItems Build a string for this many items.
   * @return The string to be displayed
   */
  private String getPostfixLabel(final AudioItem[] selectedAudioItems) {
    String labelPostfix;
    int numSelected = selectedAudioItems.length;

    if (numSelected > 1) {
      labelPostfix = String.format(LabelProvider.getLabel("AUDIO_ITEM_CONTEXT_MENU_DIALOG__LABEL_POSTFIX"), numSelected);
    } else {
      labelPostfix =  getMetadataTitle(selectedAudioItems[0]);
    }

    return labelPostfix;
  }

  /**
   * Makes a delete button.
   * TODO: address usability
   * @param selectedAudioItems Audio item(s) to delete.
   * @param labelPostfix String to label the item(s)
   * @return The button
   */
  private FlatButton makeDeleteButton(final AudioItem[] selectedAudioItems, final String labelPostfix) {
      return makeDeleteAudioItemsButton(selectedAudioItems, labelPostfix);
  }

  /**
   * Makes a remove items from playlist button. If there is a playlist selected, returns null;
   * TODO: address usability
   * @param selectedAudioItems Audio item(s) to delete.
   * @param labelPostfix String to label the item(s)
   * @return The button
   */
  private FlatButton makeRemoveButton(final AudioItem[] selectedAudioItems, final String labelPostfix) {
    if (Application.getFilterState().getSelectedPlaylist() != null) {
      return makeRemoveFromPlaylistButton(selectedAudioItems, labelPostfix);
    } else
      return null;
  }

  /**
   * Makes a button to remove audio items from a playlist.
   * @param selectedAudioItems Audio item(s) to remove from playlist
   * @param labelPostfix String to label the item(s)
   * @return The button.
   */
  private FlatButton makeRemoveFromPlaylistButton(final AudioItem[] selectedAudioItems, final String labelPostfix) {
    Color backgroundColor = Application.getApplication().getBackground();
    Color highlightedColor = SystemColor.textHighlight;
    ImageIcon deleteImageIcon = new ImageIcon(
            UIConstants.getResource(UIConstants.ICON_DELETE_16_PX));
    final String selectedTitle = getMetadataTitle(selectedAudioItems[0]);
    final Playlist selectedPlaylist = Application.getFilterState().getSelectedPlaylist();
    String buttonLabel = String.format(LabelProvider.getLabel(
        "AUDIO_ITEM_CONTEXT_MENU_DIALOG__REMOVE_PLAYLIST"),
            labelPostfix, selectedPlaylist.getName());

    FlatButton removeFromPlaylistButton = new FlatButton(buttonLabel, deleteImageIcon, backgroundColor, highlightedColor) {
      @Override
      public void click() {
        AudioItemContextMenuDialog.this.setVisible(false);

        for (AudioItem a : selectedAudioItems) {
          try {
            a.removePlaylist(selectedPlaylist);
            selectedPlaylist.removeAudioItem(a.getId());
            ACMConfiguration.getInstance().getCurrentDB().getMetadataStore()
                    .commit(a);
          } catch (Exception e) {
            LOG.log(Level.WARNING, "Unable to remove audioitem id="
                    + a.getId() + " from playlist " + selectedPlaylist.getName(), e);
          }
        }
        Application.getFilterState().updateResult(true);

      }
    };

    return removeFromPlaylistButton;
  }

  /**
   * Makes a button to delete audio items from the database.
   * @param selectedAudioItems The audio item(s) to be deleted.
   * @param labelPostfix String to label the item(s)
   * @return The button
   */
  private FlatButton makeDeleteAudioItemsButton(final AudioItem[] selectedAudioItems, final String labelPostfix) {
    Color backgroundColor = Application.getApplication().getBackground();
    Color highlightedColor = SystemColor.textHighlight;
    ImageIcon deleteImageIcon = new ImageIcon(
            UIConstants.getResource(UIConstants.ICON_TRASH_16_PX));
    final String selectedTitle = getMetadataTitle(selectedAudioItems[0]);
    String buttonLabel = String.format(LabelProvider.getLabel("AUDIO_ITEM_CONTEXT_MENU_DIALOG__DELETE"), labelPostfix);

    FlatButton deleteButton = new FlatButton(buttonLabel, deleteImageIcon, backgroundColor, highlightedColor) {
      @Override
      public void click() {
        deleteSelectedAudioItems();
      }
      private void deleteSelectedAudioItems() {
        final String deleteMessage;
        int numSelected = selectedAudioItems.length;
        if (numSelected > 1) {
          deleteMessage = String.format(LabelProvider.getLabel("AUDIO_ITEM_CONTEXT_MENU_DIALOG__DELETE_ITEMS"), numSelected);
        } else {
          deleteMessage = String.format(LabelProvider.getLabel("AUDIO_ITEM_CONTEXT_MENU_DIALOG__DELETE_TITLE"), selectedTitle);
        }
        Object[] options = { LabelProvider.getLabel("CANCEL"),
                LabelProvider.getLabel("DELETE") };

        AudioItemContextMenuDialog.this.setVisible(false);
        int n = JOptionPane.showOptionDialog(Application.getApplication(),
                deleteMessage, LabelProvider.getLabel("CONFRIM_DELETE"),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, options, options[0]);

        if (n == 1) {
          MetadataStore store = ACMConfiguration.getInstance().getCurrentDB().getMetadataStore();
          Transaction transaction = store.newTransaction();
          try {
            for (AudioItem item : selectedAudioItems) {
              try {
                // Remove the audio item from any playlists.
                // Copy to a new collection because we will modify the existing collection.
                Collection<Playlist> playlists = new ArrayList<>(item.getPlaylists());
                playlists.forEach(pl -> {
                  item.removePlaylist(pl);
                  pl.removeAudioItem(item.getId());
                  transaction.add(pl);
                });
                // Then remove the item itself.
                item.delete();
                transaction.add(item);

                // TODO: It is NOT OKAY to not delete from the file system. It is simply harder
                // to do it right. But, unless we delete the file system, we accumulate obsolete
                // cruft forever. However, we would need to defer the actual deletions until the
                // db is committed. So, let them accumulate.

                // it's okay to delete from DB but cannot delete the .a18 file
                // since that's in the shared repository
              } catch (Exception e) {
                // TODO: fix all of these silently ignored exceptions
                LOG.log(Level.WARNING, "Unable to delete audioitem id=" + item.getId(), e);
              }
            }
            transaction.commit();
          } catch (IOException ex) {
            ex.printStackTrace();
          }
          Application.getFilterState().updateResult(true);
        }

      }
    };

    return deleteButton;
  }

  /**
   * Make a button to invoke the rename dialog.
   * @param selectedAudioItems Audio Items that would be renamed.
   * @param labelPostfix A description of the item(s).
   * @return the button.
   */
  private FlatButton makeRenameButton(final AudioItem[] selectedAudioItems, String labelPostfix) {
    Color backgroundColor = Application.getApplication().getBackground();
    Color highlightedColor = SystemColor.textHighlight;
    ImageIcon renameImageIcon = new ImageIcon(
        UIConstants.getResource("rename-16.png"));
    final String selectedTitle = getMetadataTitle(selectedAudioItems[0]);
    String buttonLabel = String.format("Rename '%s'", labelPostfix);

    FlatButton renameButton = new FlatButton(buttonLabel, renameImageIcon, backgroundColor, highlightedColor) {
      @Override
      public void click() {
        AudioItemContextMenuDialog.this.setVisible(false);
        AudioItemRenameDialog dialog = new AudioItemRenameDialog(
            Application.getApplication(),  selectedAudioItems);
        // Place the new dialog within the application frame.
        dialog.setLocation(Application.getApplication().getX()+20, Application.getApplication().getY()+20);
        dialog.setVisible(true);
      }
    };
    return renameButton;
  }

  /**
   * Makes the button to handle editing one audio item
   * @param selectedAudioItems The audio item to be edited. If multiple, just notify that multi-edit
   *                           is not supported.
   * @return the button
   */
  private FlatButton makeEditButton(final AudioItem[] selectedAudioItems) {
    Color backgroundColor = Application.getApplication().getBackground();
    Color highlightedColor = SystemColor.textHighlight;
    ImageIcon editImageIcon = new ImageIcon(
            UIConstants.getResource(UIConstants.ICON_EDIT_16_PX));
    // We don't support editing multiple items, and when multiples, the "clicked" item
    // is always reported as the first item, regardless of what's actually clicked. So,
    // to avoid confusion, only allow editing when only a single item is selected. Of
    // course, always show the button, so the UI doesn't jump around, just disable it
    // when appropriate.
    boolean multiEditAttempted = selectedAudioItems.length > 1;
    final String editButtonLabel = multiEditAttempted ? LabelProvider.getLabel("AUDIO_ITEM_CONTEXT_MENU_DIALOG__CANT_MULTI_EDIT")
                                                      : LabelProvider.getLabel("AUDIO_ITEM_CONTEXT_MENU_DIALOG__EDIT_TITLE");
                                                                  ;
    final String selectedTitle = getMetadataTitle(selectedAudioItems[0]);
    String buttonLabel = String.format(editButtonLabel, selectedTitle);

    FlatButton editButton = new FlatButton(buttonLabel, editImageIcon, backgroundColor, highlightedColor) {
      @Override
      public void click() {
        AudioItemContextMenuDialog.this.setVisible(false);
        AudioItemPropertiesDialog dialog = new AudioItemPropertiesDialog(
                Application.getApplication(), null, null,
                selectedAudioItems[0]);
        // Place the new dialog within the application frame.
        dialog.setLocation(Application.getApplication().getX()+20, Application.getApplication().getY()+20);
        dialog.setVisible(true);
      }
    };
    if (multiEditAttempted) {
      editButton.setEnabled(false);
    }

    return editButton;
  }

  /**
   * Creates the button to handle exporting audio items
   * @param selectedAudioItems The audio item(s) to be exported
   * @param labelPostfix String to label the item(s)
   * @param exportDataType Whether Audio or Metadata or CustomGreeting
   * @return The button control
   */
  private FlatButton makeExportButton(final AudioItem[] selectedAudioItems,
      final String labelPostfix,
      ExportDialog.EXPORT_DATA_TYPE exportDataType) {
    Color backgroundColor = Application.getApplication().getBackground();
    Color highlightedColor = SystemColor.textHighlight;
    ImageIcon exportImageIcon = new ImageIcon(UIConstants.getResource((exportDataType == ExportDialog.EXPORT_DATA_TYPE.Audio)
                                ? UIConstants.ICON_EXPORT_16_PX
                                : UIConstants.ICON_GRID_16_PX));
    String title = LabelProvider.getLabel((exportDataType == ExportDialog.EXPORT_DATA_TYPE.Audio)
                  ? "AUDIO_ITEM_CONTEXT_MENU_DIALOG__EXPORT_AUDIO_TITLE"
                  : "AUDIO_ITEM_CONTEXT_MENU_DIALOG__EXPORT_METADATA_TITLE");
    String buttonLabel = String.format(title, labelPostfix);

    FlatButton exportButton = new FlatButton(buttonLabel, exportImageIcon, backgroundColor, highlightedColor) {
      @Override
      public void click() {
        AudioItemContextMenuDialog.this.setVisible(false);
        ExportDialog dialog = new ExportDialog(selectedAudioItems, exportDataType);
        // Place the new dialog within the application frame.
        dialog.setLocation(Application.getApplication().getX()+20, Application.getApplication().getY()+20);
        dialog.setVisible(true);
      }
    };

    return exportButton;
  }

  /**
   * Creates the button ta handle setting the language on one or more audio items.
   * @param selectedAudioItems Audio item(s) for which to set language.
   * @param labelPostfix String to label the item(s)
   * @return the constructed button.
   */
  private FlatButton makeLanguageButton(final AudioItem[] selectedAudioItems,
      final String labelPostfix)
  {
    ImageIcon setLanguageImageIcon = new ImageIcon(UIConstants.getResource(UIConstants.ICON_LANGUAGE_24_PX));
    Color backgroundColor = Application.getApplication().getBackground();
    Color highlightedColor = SystemColor.textHighlight;

    String setLanguageLabel = String.format(LabelProvider.getLabel(
        "AUDIO_ITEM_CONTEXT_MENU_DIALOG__SET_LANGUAGE"), labelPostfix);

    FlatButton languageButton = new FlatButton(setLanguageLabel,
        setLanguageImageIcon,
        backgroundColor,
        highlightedColor) {
      @Override
      public void click() {
        String dialogTitle = LabelProvider.getLabel("AUDIO_ITEM_LANGUAGE_MENU_SELECT_LANGUAGE");
        JComboBox languageBox = new JComboBox();
        LanguageComboBoxModel languageComboBoxModel = new LanguageComboBoxModel();
        languageBox.setModel(languageComboBoxModel);
        languageBox.setSelectedIndex(0);

        AudioItemContextMenuDialog.this.setVisible(false);
        int result = JOptionPane.showOptionDialog(null,
            languageBox,
            dialogTitle,
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            null,
            null);

        if (result == JOptionPane.OK_OPTION) {
          int index = languageBox.getSelectedIndex();
          Locale locale = languageComboBoxModel.getLocalForIndex(index);
          String languageCode = locale.getLanguage();
          RFC3066LanguageCode abstractLanguageCode = new RFC3066LanguageCode(languageCode);
          MetadataValue<RFC3066LanguageCode> abstractMetadataLanguageCode = new MetadataValue(
              abstractLanguageCode);

          Transaction transaction = ACMConfiguration.getInstance()
              .getCurrentDB()
              .getMetadataStore()
              .newTransaction();

          boolean success = false;
          try {
            for (AudioItem audioItem : selectedAudioItems) {
              audioItem.getMetadata().putMetadataField(DC_LANGUAGE, abstractMetadataLanguageCode);
              transaction.add(audioItem);
            }
            transaction.commit();
            success = true;
          } catch (IOException e1) {
            e1.printStackTrace();
          } finally {
            if (!success) {
              try {
                transaction.rollback();
              } catch (IOException e) {
                LOG.log(Level.SEVERE, "Unable to rollback transaction.", e);
              }
            }
            Application.getFilterState().updateResult(true);
          }
        }

      }
    };

    return languageButton;
  }

  private KeyListener keyListener = new KeyAdapter() {
    @Override
    public void keyTyped(KeyEvent e) {
      // escape?
      if (e.getKeyChar() == 0x1b) {
        setVisible(false);
      }
    }
  };

  private WindowListener windowListener = new WindowAdapter() {
    @Override
    public void windowDeactivated(WindowEvent e) {
      setVisible(false);
    }
  };

  public abstract static class FlatButton extends JLabel
      implements MouseListener {
    private Color backgroundColor;
    private Color highlightedColor;

    FlatButton(String label, Icon icon, Color backgroundColor, Color highlightedColor) {
      super(label, icon, SwingConstants.LEFT);
      init(backgroundColor, highlightedColor);
    }

    private void init(Color backgroundColor, Color highlightedColor) {
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


}
