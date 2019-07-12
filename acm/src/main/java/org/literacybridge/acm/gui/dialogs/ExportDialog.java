package org.literacybridge.acm.gui.dialogs;

import com.google.common.collect.Lists;
import org.literacybridge.acm.gui.Application;
import org.literacybridge.acm.gui.resourcebundle.LabelProvider;
import org.literacybridge.acm.gui.util.UIUtils;
import org.literacybridge.acm.importexport.AudioExporter;
import org.literacybridge.acm.importexport.CSVExporter;
import org.literacybridge.acm.repository.AudioItemRepository.AudioFormat;
import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.utils.OsUtils;
import org.literacybridge.acm.utils.SwingUtils;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.plaf.FileChooserUI;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.literacybridge.acm.repository.AudioItemRepository.audioFormatForExtension;

public class ExportDialog extends JDialog {
    private static final Logger LOG = Logger.getLogger(ExportDialog.class.getName());
    // For a hack to work around a Swing bug:
    private Color backgroundColor = getBackground();

    private TYPE exportType;

    // To remember the last directory to which the user navigated.
    private static File recentDirectory = new File(System.getProperty("user.home"));

    private File csvFile = new File("metadata.csv");

    private JFileChooser fileChooser;
    private String chosenFiletype;

    // For selecting how the file names are constructed.
    private JComboBox<String> nameFormatCombo = null;
    private final String titleOnly = LabelProvider.getLabel("TITLE_ONLY");
    private final String idOnly = LabelProvider.getLabel("ID_ONLY");
    private final String titlePlusId = LabelProvider.getLabel("TITLE_PLUS_ID");

    private final AudioItem[] selectedAudioItems;

    /**
     * Creates a new Export dialog, with a JFileChooser to do the actual file selection work.
     * @param selectedAudioItems The audio items to be exported.
     * @param type Whether TYPE.Audio or TYPE.Metadata is to be exported.
     */
    ExportDialog(AudioItem[] selectedAudioItems, TYPE type) {
        this.selectedAudioItems = selectedAudioItems;
        this.exportType = type;

        JPanel dialogPanel = new JPanel();
        dialogPanel.setLayout(new BoxLayout(dialogPanel, BoxLayout.Y_AXIS));
        add(dialogPanel);

        fileChooser = new JFileChooser(recentDirectory);
        fileChooser.addPropertyChangeListener(fileTypeListener);
        fileChooser.addActionListener(chooserActionListener);

        // Move the JFileChooser's border to the dialog.
        Border fileChooserBorder = fileChooser.getBorder();
        Border emptyBorder = new EmptyBorder(0, 0, 0, 0);
        fileChooser.setBorder(emptyBorder);
        dialogPanel.setBorder(fileChooserBorder);

        // Tweak some labels. If the tweaks don't work, the dialog still functions properly. (So
        // it is presumably localization safe.)
        List<JLabel> labels = SwingUtils.getDescendantsOfType(JLabel.class, fileChooser);
        for (JLabel label : labels) {
            type.applyTweaks(label);
        }

        // Some controls and set properties, based on the type of export.
        String titleName;
        if (type == TYPE.Audio) {
            titleName = (selectedAudioItems.length > 1) ? "EXPORT_AUDIO" : "EXPORT_ONE_AUDIO";

            // For audio export, only choosing the directory into which to store files.
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

            // Set up the file type choosers.
            fileChooser.setAcceptAllFileFilterUsed(false);
            String labelFormat = LabelProvider.getLabel("X_AUDIO_FILES");
            for (AudioFormat format : AudioFormat.exportables()) {
                String extension = format.getFileExtension().trim().toLowerCase();
                String label = String.format(labelFormat, extension.toUpperCase());
                FileNameExtensionFilter filter = new FileNameExtensionFilter(label, extension);
                fileChooser.addChoosableFileFilter(filter);
            }
        } else {
            titleName = (selectedAudioItems.length > 1) ? "EXPORT_METADATA" : "EXPORT_ONE_METADATA";

            // Saving metadata to a single file only.
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fileChooser.setSelectedFile(csvFile);
        }
        setTitle(LabelProvider.getLabel(titleName));

        // Add the file chooser to the dialog.
        dialogPanel.add(fileChooser);

        // Add other controls to the dialog.
        if (type == TYPE.Audio) {
            // Add the combo for selecting the file name format.
            dialogPanel.add(Box.createVerticalStrut(6));
            Box nameFormatPanel = Box.createHorizontalBox();
            JLabel nameFormatLabel = new JLabel(LabelProvider.getLabel("EXPORT_FORMAT"));
            nameFormatPanel.add(nameFormatLabel);
            nameFormatPanel.add(Box.createHorizontalStrut(6));
            nameFormatCombo = new JComboBox<>();
            nameFormatCombo.addItem(titlePlusId);
            nameFormatCombo.addItem(titleOnly);
            nameFormatCombo.addItem(idOnly);
            nameFormatPanel.add(nameFormatCombo);
            dialogPanel.add(nameFormatPanel);
        }

        // We will provide our own Export and Cancel buttons
        fileChooser.setControlButtonsAreShown(false);
        dialogPanel.add(Box.createVerticalStrut(20));
        Box buttonBox = Box.createHorizontalBox();
        buttonBox.add(Box.createHorizontalGlue());
        JButton exportButton = new JButton(LabelProvider.getLabel("EXPORT"));
        exportButton.addActionListener(approvalButtonPusher);
        //exportButton.setEnabled(false);
        buttonBox.add(exportButton);
        buttonBox.add(Box.createHorizontalStrut(6));
        JButton cancelButton = new JButton(LabelProvider.getLabel("CANCEL"));
        cancelButton.addActionListener(e -> fileChooser.cancelSelection());
        buttonBox.add(cancelButton);
        dialogPanel.add(buttonBox);

        setSize(600, 400);
        setAlwaysOnTop(true);
    }

    /**
     * When using JFileChooser.setControlButtonsAreShown(false) to control the
     * buttons, there's no good way to actually click "OK", or simulate it.
     * This hack simulates it (using the Look & Feel framework). This code
     * assumes that the L&F has a default button, and that it is "OK".
     */
    private ActionListener approvalButtonPusher = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            FileChooserUI ui = fileChooser.getUI();
            JButton defaultButton = ui.getDefaultButton(fileChooser);
            for (ActionListener a : defaultButton.getActionListeners()) {
                a.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, null) {
                    //Nothing need go here, the actionPerformed method (with the above arguments) will trigger the respective listener
                });
            }
        }
    };

    /**
     * Watches for property changes in the file chooser dialog. The ones we care about are
     * changes to the file filter property. Keep track of the last file extension chosen.
     */
    private PropertyChangeListener fileTypeListener = new PropertyChangeListener() {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
//            System.out.printf("Property change %s, selection: %s\n", evt.getPropertyName(), fileChooser
//                .getSelectedFile());

            if (evt.getPropertyName().equals(JFileChooser.FILE_FILTER_CHANGED_PROPERTY)) {
                FileNameExtensionFilter newFilter = (FileNameExtensionFilter) evt.getNewValue();
                if (newFilter != null) {
                    chosenFiletype = newFilter.getExtensions()[0]; // There can be only one.
                }
//            } else {
//                System.out.printf("%s: %s\n", evt.getPropertyName(), evt);
            }
        }
    };

    /**
     * This is called when the dialog is accepted or cancelled.
     */
    private ActionListener chooserActionListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (OsUtils.WINDOWS) {
                // The current directory in the fileChooser doesn't work on Windows. Get it from the
                // selection.
                File chosen = fileChooser.getSelectedFile();
                if (chosen != null && chosen.isFile()) {
                    recentDirectory = chosen.getParentFile();
                } else if (chosen != null && chosen.isDirectory()) {
                    recentDirectory = chosen;
                }
            } else {
                recentDirectory = fileChooser.getCurrentDirectory();
            }
            if (e.getActionCommand().equals(JFileChooser.APPROVE_SELECTION)) {
                if (exportType == TYPE.Audio) {
                    exportAudio(fileChooser.getSelectedFile());
                } else {
                    exportMetadata(fileChooser.getSelectedFile());
                }
            }
            ExportDialog.this.setVisible(false);
        }
    };

    /**
     * Performs the export.
     *
     * @param selectedFile The selection from the file chooser
     */
    private void exportMetadata(final File selectedFile) {
        final int numItems = ExportDialog.this.selectedAudioItems.length;

        final Runnable job = new Runnable() {
            @Override
            public void run() {
                try {
                    Writer targetWriter = new FileWriter(selectedFile);
                    CSVExporter.exportMessages(Lists.newArrayList(ExportDialog.this.selectedAudioItems),
                        targetWriter);
                    Application.getApplication()
                        .setStatusMessage(String.format("%d Item(s) exported.", numItems));
                } catch (IOException e) {
                    Application.getApplication()
                        .setStatusMessage(String.format("Exporting %d audio item(s) failed.",
                            numItems));
                    LOG.log(Level.WARNING, "Exporting audio items failed", e);
                }
            }
        };

        new Thread(job).start();
    }

    /**
     * Exports the provided items to the chosen directory.
     *
     * @param selectedDirectory The selection from the file chooser.
     */
    private void exportAudio(final File selectedDirectory) {
        final int numItems = ExportDialog.this.selectedAudioItems.length;

        final AudioFormat targetFormat = audioFormatForExtension(chosenFiletype);
        final String nameFormat = (String) nameFormatCombo.getSelectedItem();
        final boolean idInFilename = nameFormat.equals(idOnly) || nameFormat.equals(titlePlusId);
        final boolean titleInFilename =
            nameFormat.equals(titleOnly) || nameFormat.equals(titlePlusId);
        if (targetFormat == null) {
            throw new IllegalStateException("Unknown audio format in ExportDialog.");
        }

        // don't piggyback on the UI thread
        final Runnable job = new Runnable() {
            private boolean onProgress(Integer n, Integer m) {
                // Exported n of m...
                numExported = n;
                dialog.update(String.format(template, n, m));
                return !dialog.isStopRequested();
            }
            String template = LabelProvider.getLabel("EXPORTED_N_OF_M");
            int numExported = 0;

            BusyDialog dialog;
            @Override
            public void run() {
                Application app = Application.getApplication();
                dialog = UIUtils.showDialog(app,
                    new BusyDialog(LabelProvider.getLabel("EXPORTING_MESSAGES"), app, true));
                try {
                    AudioExporter exporter = AudioExporter.getInstance();
                    exporter.export(Arrays.asList(ExportDialog.this.selectedAudioItems),
                        selectedDirectory,
                        targetFormat,
                        titleInFilename,
                        idInFilename,
                        this::onProgress);

                    Application.getApplication().setStatusMessage(String.format("%d Item(s) exported.", numExported));
                } catch (IOException e) {
                    LOG.log(Level.WARNING, "Exporting audio items failed", e);
                    Application.getApplication()
                        .setStatusMessage(String.format("Exporting %d audio item(s) failed.", numItems));
                } finally {
                    UIUtils.hideDialog(dialog);
                }
            }
        };

        new Thread(job).start();
    }

    /**
     * Workaround for weird bug in seaglass look&feel that causes a
     * java.awt.IllegalComponentStateException when e.g. a combo box
     * in this dialog is clicked on
     *
     * @param bgColor The desired new background color.
     */
    @Override
    public void setBackground(Color bgColor) {
        if (bgColor.getAlpha() == 0) {
            super.setBackground(backgroundColor);
        } else {
            super.setBackground(bgColor);
            backgroundColor = bgColor;
        }
    }

    /**
     * Support for tweaking the UI. If we find a label of "from", apply the tweak.
     */
    private static abstract class Tweak {
        String label;

        Tweak(String label) {this.label = label;}

        abstract void process(JLabel jLabel);
    }

    private static class RemoveParent extends Tweak {
        RemoveParent(String label) {super(label);}

        void process(JLabel jLabel) {jLabel.getParent().setVisible(false);}
    }

    private static class FromTo extends Tweak {
        String newLabel;

        FromTo(String label, String newLabel) {
            super(label);
            this.newLabel = newLabel;
        }

        void process(JLabel jLabel) {
            jLabel.setText(newLabel);
        }
    }

    public enum TYPE {
        // @formatter:off
        Audio(new Tweak[] {
            new FromTo("Look in:", "Save in:"),
            new FromTo("Files of type:", "Audio format:")
        }),
        Metadata(new Tweak[] {
            new FromTo("Look in:", "Save in:"),
            new RemoveParent("Files of type:")
        });
        // @formatter:on

        TYPE(Tweak[] tweaks) {
            for (Tweak tweak : tweaks) {
                this.tweaks.put(tweak.label.replaceAll("[^a-zA-Z ]", "").toLowerCase(), tweak);
            }
        }

        void applyTweaks(JLabel jLabel) {
            Tweak tweak = tweaks.get(jLabel.getText().replaceAll("[^a-zA-Z ]", "").toLowerCase());
            if (tweak != null) tweak.process(jLabel);
        }

        final Map<String, Tweak> tweaks = new HashMap<>();
    }
}
