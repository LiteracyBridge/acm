package org.literacybridge.acm.gui.dialogs.audioItemImportDialog;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.literacybridge.acm.device.DeviceContents;
import org.literacybridge.acm.device.DeviceInfo;
import org.literacybridge.acm.gui.Application;
import org.literacybridge.acm.gui.dialogs.BusyDialog;
import org.literacybridge.acm.gui.resourcebundle.LabelProvider;
import org.literacybridge.acm.gui.util.UIUtils;
import org.literacybridge.acm.gui.util.language.LanguageUtil;
import org.literacybridge.acm.importexport.AudioImporter;

@SuppressWarnings("serial")
public class AudioItemImportDialog extends JDialog {
  private static final Logger LOG = Logger.getLogger(AudioItemImportDialog.class.getName());

  private AudioItemImportView childDialog;
  private DeviceContents device;

  public AudioItemImportDialog(JFrame parent, DeviceInfo deviceInfo) {
    super(parent, LabelProvider.getLabel("AUDIO_ITEM_IMPORT_DIALOG_TITLE"), ModalityType.APPLICATION_MODAL);
    createControls();

    setSize(800, 500);

    try {
      device = deviceInfo.getDeviceContents();

      initialize();
    } catch (IOException e) {
      LOG.log(Level.WARNING, "Loading from device failed.", e);
    }
  }

  private void initialize() {
    // TB file system can be very slow -- use a thread to read it.
    Runnable job = new Runnable() {

      @Override
      public void run() {
        try {
          final List<File> audioItems = device.loadAudioFiles();
          childDialog.setData(audioItems);
        } catch (IOException e) {
          LOG.log(Level.WARNING, "Examining files on device failed.", e);
        } finally {
          setCursor(Cursor.getDefaultCursor());
        }
      }
    };

    setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    new Thread(job).start();

  }

  private void createControls() {
    setLayout(new BorderLayout());
    childDialog = new AudioItemImportView();
    add(childDialog, BorderLayout.CENTER);

    JPanel panel = new JPanel();
    panel.setLayout(new GridLayout(1, 5));

    JButton selectAllBtn = new JButton(LabelProvider.getLabel(
        "AUDIO_ITEM_IMPORT_DIALOG_SELECT_ALL", LanguageUtil.getUILanguage()));
    selectAllBtn.addActionListener(getSelectActionListener(true));

    JButton selectNoneBtn = new JButton(LabelProvider.getLabel(
        "AUDIO_ITEM_IMPORT_DIALOG_SELECT_NONE", LanguageUtil.getUILanguage()));
    selectNoneBtn.addActionListener(getSelectActionListener(false));

    JButton okBtn = new JButton(
        LabelProvider.getLabel("IMPORT", LanguageUtil.getUILanguage()));
    okBtn.addActionListener(getImportActionListener());

    JButton cancelBtn = new JButton(
        LabelProvider.getLabel("CANCEL", LanguageUtil.getUILanguage()));
    cancelBtn.addActionListener(getCancelActionListener());

    panel.add(selectAllBtn);
    panel.add(selectNoneBtn);
    panel.add(new JLabel()); // dummy place holder
    panel.add(okBtn);
    panel.add(cancelBtn);

    add(panel, BorderLayout.SOUTH);
  }

  private ActionListener getSelectActionListener(final boolean selectAll) {
    return new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        childDialog.setCheckSetForAllItems(selectAll);

      }
    };
  }

  private ActionListener getCancelActionListener() {
    return new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        Application.getFilterState().updateResult();
        UIUtils.hideDialog(AudioItemImportDialog.this);
      }
    };
  }

  private ActionListener getImportActionListener() {
    return new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        UIUtils.hideDialog(AudioItemImportDialog.this);

        final List<File> files = getAudioItemsForImport();

        // don't piggyback on the drag&drop thread
        Runnable job = new Runnable() {

          private boolean onProgress(Integer n, Integer m) {
            // Exported n of m...
            dialog.update(String.format(template, n, m));
            return !dialog.isStopRequested();
          }
          String template = LabelProvider.getLabel("IMPORTED_N_OF_M");

          BusyDialog dialog;
          @Override
          public void run() {
            Application parent = Application.getApplication();
            dialog = UIUtils.showDialog(parent,
                new BusyDialog(LabelProvider.getLabel("IMPORTING_FILES"), parent, true));
            try {
              int count = 0;
              for (File file : files) {
                try {
                  AudioImporter.getInstance().importOrUpdateAudioItemFromFile(file, null);
                } catch (Exception e) {
                  LOG.log(Level.WARNING, "Importing file '" + file + "' failed.", e);
                }
                if (!onProgress(++count, files.size())) {
                  break;
                }
              }
            } finally {
              UIUtils.hideDialog(dialog);
            }
          }
        };

        new Thread(job).start();
      }
    };
  }

  public List<File> getAudioItemsForImport() {
    return childDialog.getAudioItemsForImport();
  }
}
