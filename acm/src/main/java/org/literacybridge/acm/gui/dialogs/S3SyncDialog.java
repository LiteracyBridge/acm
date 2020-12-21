package org.literacybridge.acm.gui.dialogs;

import org.amplio.CloudSync;
import org.literacybridge.acm.gui.Assistant.GBC;
import org.literacybridge.acm.gui.util.UIUtils;
import org.literacybridge.acm.utils.SwingUtils;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import static org.literacybridge.acm.gui.util.UIUtils.UiOptions.TOP_THIRD;

public class S3SyncDialog extends JDialog {

    private JProgressBar progressBar;
    private JLabel messageText;
    private JLabel exceptionText;

    public enum SYNC_STYLE {
        DOWNLOAD,   // Initial download from S3.
        SYNC        // Sync with S3.
    }

    /**
     * A description of a single field that we receive from the synchronizer.
     */
    private static class Field {
        // How the field is named when we receive it.
        public final String key;
        // How the field will be labelled in the dialog.
        public final String label;
        // If the field is only applicable to certain SYNC_STYLEs, list those
        // here. If empty or null, applicable to all.
        public final List<SYNC_STYLE> appliesTo;
        Field(String key, String label, SYNC_STYLE... appliesTo) {
            this.key = key;
            this.label = label;
            this.appliesTo = Arrays.asList(appliesTo);
        }
    }

    /**
     * The fields that will be displayed during a sync operation.
     */
    static final Field[] ALL_SCHEMA = new Field[]{
//            new Field("durationMS", "Duration"),
            new Field("filesUp", "Files Uploaded", SYNC_STYLE.SYNC),
            new Field("bytesUp", "Bytes Uploaded", SYNC_STYLE.SYNC),
            new Field("filesPendingUp", "Files Remaining to Upload", SYNC_STYLE.SYNC),
            new Field("bytesPendingUp", "Bytes Remaining to Upload", SYNC_STYLE.SYNC),
            new Field("objectsDown", "Files Downloaded"),
            new Field("bytesDown", "Bytes Downloaded"),
            new Field("objectsPendingDown", "Files Remaining to Download"),
            new Field("bytesPendingDown", "Bytes Remaining to Download"),
            new Field("filesDeleted", "Files Deleted from Computer", SYNC_STYLE.SYNC),
            new Field("objectsDeleted", "Files Deleted from Cloud", SYNC_STYLE.SYNC),
    };
    private final List<Field> SCHEMA;

    private final String program;
    private final SYNC_STYLE syncStyle;

    private Timer timer;
    private final JButton closeButton;
    private final JCheckBox autoClose;
    private JLabel[] values;

    // How to have a synchronized boolean.
    private boolean syncDone;
    private Throwable syncError;
    private synchronized void setSyncDone() {
        syncDone = true;
    }
    private synchronized void setSyncError(Throwable syncError) {
        this.syncError = syncError;
    }
    private synchronized boolean isSyncDone() {
        return syncDone;
    }
    public boolean hasSyncError() {
        return syncError != null;
    }

    /**
     * Constructor for the Exceptions display.
     *
     * @param owner window, for positioning this window.
     * @param program being sync'd.
     * @param syncStyle controls which fields are shown.
     */
    public S3SyncDialog(Frame owner, String program, SYNC_STYLE syncStyle) {
        super(owner, makeTitle(program, syncStyle), ModalityType.APPLICATION_MODAL);
        this.program = program;
        this.syncStyle = syncStyle;

        SCHEMA = Arrays.stream(ALL_SCHEMA)
                .filter((f) -> f.appliesTo.size()==0 || f.appliesTo.contains(syncStyle))
                .collect(Collectors.toList());

        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(new EmptyBorder(5, 15, 5, 15));

        JLabel messageLabel = new JLabel("Synchronization progress:");
        panel.add(messageLabel, BorderLayout.NORTH);

        panel.add(buildStatusPanel(), BorderLayout.CENTER);
        refresh(new HashMap<>());

        Box hbox = Box.createHorizontalBox();
        hbox.add(Box.createHorizontalStrut(5));
        autoClose = new JCheckBox("Close when sync finishes.", true);
        hbox.add(autoClose);

        hbox.add(Box.createHorizontalGlue());

        closeButton = new JButton("Close");
        closeButton.setEnabled(false);
        closeButton.addActionListener(e -> setVisible(false));
        hbox.add(closeButton);
        hbox.add(Box.createHorizontalStrut(5));

        panel.add(hbox, BorderLayout.SOUTH);
        add(panel);
        setSize(800, 500);

        SwingUtils.addEscapeListener(this);
        UIUtils.centerWindow(this, TOP_THIRD);
    }

    private static String makeTitle(String program, SYNC_STYLE syncStyle) {
        if (syncStyle == SYNC_STYLE.DOWNLOAD) {
            return String.format("Download %s from S3", program.toUpperCase(Locale.ROOT));
        }
        return String.format("Synchronize %s with S3", program.toUpperCase(Locale.ROOT));
    }

    /**
     * Starts the sync process.
     */
    public void go() {
        CloudSync.start(true);
        timer = new Timer(10, this::startListener);
        timer.start();
        System.out.println("Timer started: "+timer.toString());
        setVisible(true);
    }

    int peekcount = 0;
    /**
     * Checks whether the synchronizer has started; if so, starts the actual sync.
     * @param actionEvent is not used.
     */
    private void startListener(ActionEvent actionEvent) {
        if (CloudSync.ping()) {
            System.out.println("Synchronizer has started. Preparing for sync.");
            timer.stop();
            SyncWorker worker = new SyncWorker();
            worker.execute();
            timer = new Timer(500, this::syncListener);
            timer.start();
        } else {
            if (++peekcount % 500 == 0) {
                System.out.println("Still waiting for synchronizer...");
            }
            // TODO: Timeout and abort at 15 seconds.
        }
    }

    /**
     * Checks whether the sync operation has finished. Updates progress.
     * @param actionEvent is not used.
     */
    private void syncListener(ActionEvent actionEvent) {
        if (isSyncDone()) {
            System.out.printf("Sync has finished for program '%s'\n", program);
            timer.stop();
            closeButton.setEnabled(true);
            if (autoClose.isSelected()) {
                setVisible(false);
            }
        } else {
            try {
                CloudSync.RemoteResponse response = CloudSync.status(program+"_DB");
                Map<String, Object> status = response.responseData;
                refresh(status);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Builds the actual data display. Specific values depend on the nature of the sync: general, or sync from S3.
     * @return the component that contains the display.
     */
    private Component buildStatusPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());

        GBC gbcLeft = new GBC()
                .setGridx(0)
                .setFill(GridBagConstraints.NONE)
                .setAnchor(GridBagConstraints.FIRST_LINE_END)
                .setIpady(10);
        GBC gbcRight = new GBC()
                .setGridx(1)
                .setFill(GridBagConstraints.HORIZONTAL)
                .setAnchor(GridBagConstraints.LINE_START)
                .setIpady(10)
                .setWeightx(0.5f);

        values = new JLabel[SCHEMA.size()];
        for (int i=0; i<SCHEMA.size(); i++) {
            JLabel label = new JLabel(SCHEMA.get(i).label + ": ");
            panel.add(label, gbcLeft);
            values[i] = new JLabel();
            panel.add(values[i], gbcRight);
        }

        // Add one to the progress max, for the "send email" step.
        progressBar = new JProgressBar(0, 1);
        progressBar.setValue(0);
        panel.add(progressBar, gbcLeft.withGridwidth(2).withFill(GridBagConstraints.HORIZONTAL));

        messageText = new JLabel();
        panel.add(messageText, gbcLeft.withGridwidth(2).withFill(GridBagConstraints.HORIZONTAL));

        exceptionText = new JLabel();
        panel.add(exceptionText, gbcLeft.withGridwidth(2).withFill(GridBagConstraints.HORIZONTAL));

        // Absorb any additional space.
        panel.add(new JLabel(), gbcLeft.withWeighty(1.0f));

        return panel;
    }

    /**
     * Refreshes the display with latest status values from the synchronizer.
     * @param newValues to be shown.
     */
    private void refresh(Map<String, Object> newValues) {
        for (int ix=0; ix<SCHEMA.size(); ix++) {
            String key = SCHEMA.get(ix).key;
            Object value = newValues.getOrDefault(key, "n/a");
            if (value instanceof Long) {
                value = String.format("%,d", value);
            } else if (value instanceof Integer) {
                value = String.format("%d", value);
            }
            values[ix].setText(value.toString());
        }
        long bytesDown = (long) newValues.getOrDefault("bytesDown", 0L);
        long bytesLeft = (long) newValues.getOrDefault("bytesPendingDown", 0L);
        long bytesTotal = bytesDown + bytesLeft;
        if (bytesTotal > 0) {
            progressBar.setMaximum((int)bytesTotal);
            progressBar.setValue((int)bytesDown);
        }
    }

    /**
     * A class to encapsulate running the sync in the background.
     */
    class SyncWorker extends SwingWorker<Void, Void> {
        @Override
        protected Void doInBackground() throws Exception {
            if (syncStyle == SYNC_STYLE.DOWNLOAD) {
                String programConfig = String.format("[%1$s_DB]\nbucket:${CONTENT_BUCKET}\n" +
                        "prefix:%1$s\n" +
                        "path:${PROGRAMS_DIR}/%1$s\n" +
                        "status_file:%1$s_DB\n" +
                        "delay_start: 15 seconds\n" +
                        "sync_interval:55 seconds\n" +
                        "\n" +
                        "[%1$s_PROGSPEC]\nbucket:${PROGSPECS_BUCKET}\n" +
                        "prefix:%1$s\n" +
                        "path:${PROGSPECS_DIR}/%1$s\n" +
                        "status_file:%1$s_PROGSPEC\n" +
                        "policy:MIRROR_CLOUD\n" +
                        "delay_start:5 seconds\n" +
                        "sync_interval:2 minutes\n", program);
                System.out.printf("Adding config for program '%s':\n%s", program, programConfig);
                CloudSync.addConfig(program, programConfig);
            }
            System.out.printf("Starting sync for program '%s'\n", program);
            try {
                CloudSync.RemoteResponse response;
                response = CloudSync.sync(program + "_PROGSPEC");
                if (response.responseHasError) {
                    setSyncError(new Exception("Exception synchronizing "+program+"_PROGSPEC"));
                    return null;
                }
                response = CloudSync.sync(program + "_DB");
                if (response.responseHasError) {
                    setSyncError(new Exception("Exception synchronizing "+program+"_DB"));
                    return null;
                }
            } catch (Throwable whatever) {
                whatever.printStackTrace();
                setSyncError(whatever);
            }
            return null;
        }

        @Override
        protected void done() {
            setSyncDone();
            closeButton.setText("Close");
            if (hasSyncError()) {
                messageText.setText("An error occurred while synchronizing the program content.");
                exceptionText.setText(syncError.toString());
                autoClose.setSelected(false);
            }
        }
    }

}