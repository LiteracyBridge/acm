package org.literacybridge.acm.gui.dialogs;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.SystemColor;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.*;

import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.gui.Application;
import org.literacybridge.acm.gui.UIConstants;
import org.literacybridge.acm.gui.ResourceView.audioItems.AudioItemView;
import org.literacybridge.acm.gui.dialogs.audioItemPropertiesDialog.AudioItemPropertiesDialog;
import org.literacybridge.acm.gui.dialogs.audioItemPropertiesDialog.LanguageComboBoxModel;
import org.literacybridge.acm.gui.resourcebundle.LabelProvider;
import org.literacybridge.acm.gui.util.language.LanguageUtil;
import org.literacybridge.acm.store.*;

import static org.literacybridge.acm.store.MetadataSpecification.DC_LANGUAGE;

// TODO: deal with localized audio items when languages are fully implemented
public class AudioItemContextMenuDialog extends JDialog
    implements WindowListener {
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

    ImageIcon editImageIcon = new ImageIcon(
        UIConstants.getResource(UIConstants.ICON_EDIT_16_PX));
    ImageIcon deleteImageIcon = new ImageIcon(
        UIConstants.getResource(UIConstants.ICON_DELETE_16_PX));
    ImageIcon exportImageIcon = new ImageIcon(
            UIConstants.getResource(UIConstants.ICON_EXPORT_16_PX));

    Color backgroundColor = parent.getBackground();
    Color highlightedColor = SystemColor.textHighlight;

    GridLayout grid = new GridLayout(4, 1);

    final String selectedTitle = getMetadataTitle(clickedAudioItem);

    final Playlist selectedTag = Application.getFilterState()
        .getSelectedPlaylist();

    String labelPostfix;
    final FlatButton deleteButton;

    if (selectedTag == null) {
      final String deleteMessage;

      if (selectedAudioItems.length > 1) {
        labelPostfix = String
            .format(
                LabelProvider.getLabel(
                    "AUDIO_ITEM_CONTEXT_MENU_DIALOG_LABEL_POSTFIX",
                    LanguageUtil.getUILanguage()),
                selectedAudioItems.length);
        deleteMessage = String
            .format(
                LabelProvider.getLabel(
                    "AUDIO_ITEM_CONTEXT_MENU_DIALOG_DELETE_ITEMS",
                    LanguageUtil.getUILanguage()),
                selectedAudioItems.length);
      } else {
        labelPostfix = selectedTitle;
        deleteMessage = String.format(LabelProvider.getLabel(
            "AUDIO_ITEM_CONTEXT_MENU_DIALOG_DELETE_TITLE",
            LanguageUtil.getUILanguage()), selectedTitle);
      }

      deleteButton = new FlatButton(
          String.format(
              LabelProvider.getLabel("AUDIO_ITEM_CONTEXT_MENU_DIALOG_DELETE",
                  LanguageUtil.getUILanguage()),
              labelPostfix),
          deleteImageIcon, backgroundColor, highlightedColor) {
        @Override
        public void click() {
          AudioItemContextMenuDialog.this.setVisible(false);

          Object[] options = {
              LabelProvider.getLabel("CANCEL", LanguageUtil.getUILanguage()),
              LabelProvider.getLabel("DELETE", LanguageUtil.getUILanguage()) };
          int n = JOptionPane.showOptionDialog(Application.getApplication(),
              deleteMessage,
              LabelProvider.getLabel("CONFRIM_DELETE",
                  LanguageUtil.getUILanguage()),
              JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null,
              options, options[0]);

          if (n == 1) {
            for (AudioItem a : selectedAudioItems) {
              try {
                ACMConfiguration.getInstance().getCurrentDB().getMetadataStore()
                    .deleteAudioItem(a.getUuid());
                ACMConfiguration.getInstance().getCurrentDB().getMetadataStore()
                    .commit(a);
                // it's okay to delete from DB but cannot delete the .a18 file
                // since that's in the shared (dropbox) repository
                if (!ACMConfiguration.getInstance().getCurrentDB()
                    .getControlAccess().isSandbox())
                  ACMConfiguration.getInstance().getCurrentDB().getRepository()
                      .delete(a);
              } catch (Exception e) {
                LOG.log(Level.WARNING,
                    "Unable to delete audioitem id=" + a.getUuid(), e);
              }
            }
            Application.getFilterState().updateResult(true);
          }

        }
      };
    } else {
      if (selectedAudioItems.length > 1) {
        labelPostfix = String
            .format(
                LabelProvider.getLabel(
                    "AUDIO_ITEM_CONTEXT_MENU_DIALOG_LABEL_POSTFIX",
                    LanguageUtil.getUILanguage()),
                selectedAudioItems.length);
      } else {
        labelPostfix = selectedTitle;
      }

      deleteButton = new FlatButton(
          String.format(
              LabelProvider.getLabel(
                  "AUDIO_ITEM_CONTEXT_MENU_DIALOG_REMOVE_TAG",
                  LanguageUtil.getUILanguage()),
              labelPostfix, selectedTag.getName()),
          deleteImageIcon, backgroundColor, highlightedColor) {
        @Override
        public void click() {
          AudioItemContextMenuDialog.this.setVisible(false);

          for (AudioItem a : selectedAudioItems) {
            try {
              a.removePlaylist(selectedTag);
              selectedTag.removeAudioItem(a.getUuid());
              ACMConfiguration.getInstance().getCurrentDB().getMetadataStore()
                  .commit(a);
            } catch (Exception e) {
              LOG.log(Level.WARNING, "Unable to remove audioitem id="
                  + a.getUuid() + " from tag " + selectedTag.getName(), e);
            }
          }
          Application.getFilterState().updateResult(true);

        }
      };

    }

    final String editButtonLabel = LabelProvider.getLabel(
        "AUDIO_ITEM_CONTEXT_MENU_DIALOG_EDIT_TITLE",
        LanguageUtil.getUILanguage());

    FlatButton editButton = new FlatButton(
        String.format(editButtonLabel, selectedTitle), editImageIcon,
        backgroundColor, highlightedColor) {
      @Override
      public void click() {
        AudioItemContextMenuDialog.this.setVisible(false);
        AudioItemPropertiesDialog dlg = new AudioItemPropertiesDialog(
            Application.getApplication(), audioItemView, data.getAudioItems(),
            clickedAudioItem);
        dlg.setVisible(true);
      }
    };

    FlatButton exportButton = new FlatButton(
        String.format(LabelProvider.getLabel(
            "AUDIO_ITEM_CONTEXT_MENU_DIALOG_EXPORT_TITLE",
            LanguageUtil.getUILanguage()), labelPostfix),
        exportImageIcon, backgroundColor, highlightedColor) {
      @Override
      public void click() {
        AudioItemContextMenuDialog.this.setVisible(false);
        ExportDialog export = new ExportDialog(selectedAudioItems);
        export.setVisible(true);
      }
    };

    FlatButton languageButton = makeLanguageButton(selectedAudioItems);

    setLayout(grid);

    editButton.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    deleteButton.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    exportButton.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    languageButton.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    add(editButton);
    add(exportButton);
    add(deleteButton);
    add(languageButton);

    addWindowListener(this);
    setAlwaysOnTop(true);
    setSize(new Dimension(450, 125));
  }

  @Override
  public void windowDeactivated(WindowEvent e) {
    setVisible(false);
  }

  /**
   * Creates the button ta handle setting the language on one or more audio items.
   * @param selectedAudioItems
   * @return
   */
  private FlatButton makeLanguageButton(final AudioItem[] selectedAudioItems) {
    ImageIcon setLanguageImageIcon = new ImageIcon(
            UIConstants.getResource(UIConstants.ICON_LANGUAGE_24_PX));
    Color backgroundColor = Application.getApplication().getBackground();
    Color highlightedColor = SystemColor.textHighlight;

    String setLanguageForWhat = (selectedAudioItems.length > 1) ?
            String.format(LabelProvider.getLabel("AUDIO_ITEM_CONTEXT_MENU_DIALOG_LABEL_POSTFIX"), selectedAudioItems.length) :
            getMetadataTitle(selectedAudioItems[0]);
    String setLanguageLabel = String.format(
            LabelProvider.getLabel("AUDIO_ITEM_CONTEXT_MENU_DIALOG_SET_LANGUAGE"),
            setLanguageForWhat);
    FlatButton languageButton = new FlatButton(setLanguageLabel, setLanguageImageIcon, backgroundColor, highlightedColor) {
      @Override
      public void click() {
        String dialogTitle = LabelProvider.getLabel("AUDIO_ITEM_LANGUAGE_MENU_SELECT_LANGUAGE");
        JComboBox languageBox = new JComboBox();
        LanguageComboBoxModel languageComboBoxModel = new LanguageComboBoxModel();
        languageBox.setModel(languageComboBoxModel);
        languageBox.setSelectedIndex(0);

        AudioItemContextMenuDialog.this.setVisible(false);
        int result = JOptionPane.showOptionDialog(null, languageBox, dialogTitle, JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, null, null);

        if (result == JOptionPane.OK_OPTION ) {
          int index = languageBox.getSelectedIndex();
          Locale locale = languageComboBoxModel.getLocalForIndex(index);
          String languageCode = locale.getLanguage();
          RFC3066LanguageCode abstractLanguageCode = new RFC3066LanguageCode(languageCode);
          MetadataValue<RFC3066LanguageCode> abstractMetadataLanguageCode = new MetadataValue(abstractLanguageCode);
          MetadataStore store = ACMConfiguration.getInstance().getCurrentDB().getMetadataStore();

          for (AudioItem audioItem : selectedAudioItems) {
            audioItem.getMetadata()
                    .setMetadataField(DC_LANGUAGE, abstractMetadataLanguageCode);
            try {
              store.commit(audioItem);
            } catch (IOException e1) {
              e1.printStackTrace();
            }
          }
        }

      }
    };

    return languageButton;
  }


  public abstract static class FlatButton extends JLabel
      implements MouseListener {
    private Color backgroundColor;
    private Color highlightedColor;

    public FlatButton(String label, Color backgroundColor,
        Color highlightedColor) {
      super(label);
      init(backgroundColor, highlightedColor);
    }

    public FlatButton(String label, Icon icon, Color backgroundColor,
        Color highlightedColor) {
      super(label, icon, SwingConstants.LEFT);
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

  @Override
  public void windowActivated(WindowEvent e) {
  }

  @Override
  public void windowClosed(WindowEvent e) {
  }

  @Override
  public void windowClosing(WindowEvent e) {
  }

  @Override
  public void windowDeiconified(WindowEvent e) {
  }

  @Override
  public void windowIconified(WindowEvent e) {
  }

  @Override
  public void windowOpened(WindowEvent e) {
  }
}
