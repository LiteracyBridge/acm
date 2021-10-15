package org.literacybridge.acm.tbloader;

import org.literacybridge.acm.config.AmplioHome;
import org.literacybridge.acm.gui.Assistant.GBC;
import org.literacybridge.acm.gui.util.UIUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.literacybridge.acm.gui.util.UIUtils.UiOptions.TOP_THIRD;

/**
 * Class to perform a DFU firmware update on a TBv2.
 * <p>
 * - Prompts the user to press and hold the pot, then press and release the tree and table.
 * - Waits to see the device enter DFU mode.
 * - Invokes the "cube programmer" utility to erase and reprogram the device.
 * -- Starts the TB application, but when startted in this way, the app can't enter USB mode,
 * and soon hangs. The fix is TBD.
 * - Displays the progress of the erase and update.
 * - Automatically closes a second after the update finishes.
 */
public class Tb2FirmwareUpdater extends JDialog {
    private static final Logger LOG = Logger.getLogger(Tb2FirmwareUpdater.class.getName());
    private static final Pattern CUBE_VERSION_MATCHER = Pattern.compile("(?i)^\\s*STM32CubeProgrammer\\s*(\\S*).*$");
    private static final Pattern DFU_MATCHER = Pattern.compile("(?i)^\\W*DFU Interface\\W*$");
    private static final Pattern NO_DFU_DEVICE = Pattern.compile("(?i)^.*No STM32 device.*$");
    private static final Pattern DFU_DEVICE = Pattern.compile("(?i)^.*Device Index\\s*:\\s*(\\w*)$");
    private static final Pattern DOWNLOAD_STARTED = Pattern.compile("(?i)^.*Download in Progress:.*$");
    private static final Pattern DOWNLOAD_FINISHED = Pattern.compile("(?i)^.*File download complete.*$");
    private static final Pattern DOWNLOAD_DURATION = Pattern.compile(
        "(?i)^Time elapsed during download operation: (?:(\\d{2}):){3}\\.(\\d{3})$");

    private final Timer timer;
    private static boolean dfuVersionLogged = false; // Once per TB-Loader session.

    private final File firmare;
    private JTextComponent prompt;
    private JProgressBar progressBar;
    private JButton closeButton;

    private String connectedDeviceName;
    private long timeToClose = Long.MAX_VALUE;
    private boolean updateOk = false;

    private enum State {
        WAITING,      // plain panel, no instructions.
        WAITING_POT("Press and hold the POT."),
        WAITING_TPT("While holding the POT, press both the TREE and the TABLE."),
        WAITING_POT2("Keep holding the POT, and release the TREE and the TABLE."),

        READY("Resetting Talking Book memory."),
        ERASED("Updating Talking Book firmware."),
        UPDATING,       // update in progress
        FINISHED("The Talking Book firmware has been updated.");

        State nextState = null;
        final String text;

        static {
            WAITING.nextState = WAITING_POT;
            WAITING_POT.nextState = WAITING_TPT;
            WAITING_TPT.nextState = WAITING_POT2;
            WAITING_POT2.nextState = WAITING;       // Loop back to start
            READY.nextState = ERASED;
        }

        State(String text) {this.text = text;}

        State() {this.text = "";}

        State getNextState() {return nextState != null ? nextState : this;}

        boolean isWaiting() {return ordinal() <= WAITING_POT2.ordinal();}
    }

    private State state = State.WAITING_POT;


    /**
     * Constructor.
     *
     * @param owner    window.
     * @param firmware to be installed onto a v2 Talking Book.
     */
    public Tb2FirmwareUpdater(Frame owner, File firmware) {
        super(owner);
        setTitle("Load Firmware on V2 Talking Book");
        this.firmare = firmware;

        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(new EmptyBorder(5, 15, 5, 15));

        setLayout(new BorderLayout());

        panel.add(buildPromptPanel(), BorderLayout.CENTER);

        Box buttonsBox = createNavigationButtons();
        panel.add(buttonsBox, BorderLayout.SOUTH);

        timer = new Timer(2000, this::timerListener);
        timer.start();

        add(panel);
        setSize(500, 300);
        UIUtils.centerWindow(this, TOP_THIRD);
    }

    /**
     * This is a hack to kill the timer if the window closes. The windowstatelistener is
     * supposed to do that, but isn't working.
     *
     * @param visible Should the window be visible?
     */
    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        // If we're closing the window, and there's a timer, stop it.
        if (!visible && timer != null) {
            timer.stop();
        }
    }

//    public boolean useTextArea = true;

    /**
     * Builds the main prompt and status panel.
     *
     * @return a component containing the prompt and status.
     */
    private Component buildPromptPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        GBC gbc = new GBC()
            .setGridx(0)
            .setWeightx(1.0)
            .setGridy(GridBagConstraints.RELATIVE)
            .setFill(GridBagConstraints.HORIZONTAL)
            .setAnchor(GridBagConstraints.FIRST_LINE_START)
            .setIpady(10);

        panel.add(new JLabel("Instructions:"), gbc);

//        if (useTextArea) {
        prompt = new JTextArea(state.text);
//        } else {
//            prompt = new JTextField(state.text, 90);
//        }
        panel.add(prompt, gbc.withFill(GridBagConstraints.BOTH).withWeighty(1.0));

        progressBar = new JProgressBar(0, 100);
        progressBar.setValue(0);
        panel.add(progressBar, gbc);

        return panel;
    }

    /**
     * Creates the cancel/close button.
     *
     * @return a box with the button.
     */
    private Box createNavigationButtons() {
        Box hbox = Box.createHorizontalBox();
        hbox.setBorder(new EmptyBorder(10, 10, 10, 10));

        closeButton = new JButton("Cancel");

        closeButton.addActionListener(actionEvent -> setVisible(false));
        hbox.add(Box.createHorizontalGlue());
        hbox.add(Box.createHorizontalStrut(15));
        hbox.add(closeButton);

        return hbox;
    }

    /**
     * Listens on a timer. Mostly used to animate the instructions. Also advances through the status
     * notifications.
     *
     * @param actionEvent a timer event.
     */
    private void timerListener(ActionEvent actionEvent) {
        state = state.getNextState();
        // If in a waiting state, should we remain there?
        if (state.isWaiting() && (connectedDeviceName = isConnected()) != null) {
            // No need to keep waiting, the device is ready to be programmed.
            state = State.READY;
        }
        prompt.setText(state.text);
        if (state == State.READY) {
            closeButton.setEnabled(false);
            SwingUtilities.invokeLater(this::go);
        }
        if (state == State.FINISHED && System.currentTimeMillis() > timeToClose) {
            setVisible(false);
        }
    }

    /**
     * Orchestrates the update:
     * - connect to the device
     * - write the firmware
     * - restart the device
     * <p>
     * As the firmware is written, the progress bar is updated. When/if the download completes successfully,
     * re-enables the close button (note that the "X" is never disabled).
     *
     * @return true if everything completed successfully.
     */
    public boolean go() {
        final boolean[] result = new boolean[1]; // final, but contents are mutable.
        String[] command = new String[]{"-c", "port=" + connectedDeviceName, "-e", "[0 5]", "-w", firmare.getAbsolutePath(), "-s"};

        // Run this on a separate thread so that the UI can be responsive to progress updates.
        // We want a boolean result, but the cube command returns an object, which might be null, or might
        // be some other type. This construct interprets a Boolean(true) as true and anything else as false.
        Thread t = new Thread(() -> result[0] = Boolean.TRUE.equals(cube(command, br -> {
            if (waitForDownloadStart(br)) {
                if (watchDownload(br, progress -> Tb2FirmwareUpdater.this.onProgress(progress, false))) {
                    if (waitForDownloadFinish(br)) {
                        onProgress(100, true);
                    }
                    return true;
                }
            }
            return false;
        })));
        t.start();

        return result[0];
    }

    /**
     * Called to update the progress display.
     *
     * @param progress The percentage of completion.
     * @param finished Pass true when progress is complete, to re-enable the close button.
     */
    private void onProgress(Integer progress, boolean finished) {
        try {
            SwingUtilities.invokeAndWait(() -> {
                progressBar.setValue(progress);
                if (finished) {
                    state = State.FINISHED;
                    closeButton.setText("Close");
                    closeButton.setEnabled(true);
                } else if (state == State.READY) {
                    state = State.ERASED;
                }
            });
        } catch (InterruptedException | InvocationTargetException ignored) {
        }
    }

    /**
     * Runs the cube command to see if a device is connected in DFU mode.
     *
     * @return the device port name, if any, or null if no connected device.
     */
    private String isConnected() {
        String[] command = {"-l"};
        return cube(command, br -> {
            if (waitForDfu(br)) {
                return getDfuDevice(br);
            }
            return null;
        });
    }

    /**
     * Runs a STM32CubeProgrammer command.
     *
     * @param cmdarray An array with the commands to run. Does not contain the cube executable itself.
     * @param handler  a callback to be invoked with a BufferedReader of the command output.
     * @param <R>      The generic type returned by the handler, also returned by this method.
     * @return Whatever the handler returned.
     */
    private <R> R cube(String[] cmdarray, Function<BufferedReader, R> handler) {
        try {
            File cubeDir = AmplioHome.getStmUtilsDir();
            List<String> cmdList = new ArrayList<>(Collections.singletonList(cubeDir.getAbsolutePath() + File.separatorChar + "cube.exe"));
            cmdList.addAll(Arrays.asList(cmdarray));
            LOG.log(Level.INFO, "Executing: " + String.join(" ", cmdarray));
            Process proc = new ProcessBuilder(cmdList)
                .directory(cubeDir)
                .redirectErrorStream(true) // merge stderr into stdout
                .start();

            // Stdout is called the "InputStream". Hopefully, someone at Sun was fired for that...
            //  (what it IS is necessary, but what it DOES is useful)
            BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            R result = handler.apply(reader);
            // Wait for the process to terminate.
            proc.waitFor();
            return result;
        } catch (InterruptedException | IOException e) {
            return null;
        }
    }

    /**
     * Waits for a line "=====  DFU Interface  =====". Saves the STM32CubeProgrammer version, if that's seen.
     *
     * @param stream Output from the cube programmer.
     * @return true if the DFU Interface line was found.
     */
    private boolean waitForDfu(BufferedReader stream) {
        String line;
        try {
            while ((line = stream.readLine()) != null) {
                System.out.println(line);
                Matcher matcher;
                if (!dfuVersionLogged) {
                    matcher = CUBE_VERSION_MATCHER.matcher(line);
                    if (matcher.matches()) {
                        String cubeVersion = matcher.group(1);
                        LOG.log(Level.INFO, "STM Cube Programmer version: " + cubeVersion);
                        dfuVersionLogged = true;
                    }
                }
                matcher = DFU_MATCHER.matcher(line);
                if (matcher.matches()) {
                    return true;
                }
            }
        } catch (IOException ignored) {
        }
        return false;
    }

    /**
     * Waits for line like "Device Index    : USB1"
     *
     * @param stream Output from the cube programmer.
     * @return the device name, or null if no device was found.
     */
    private String getDfuDevice(BufferedReader stream) {
        String line;
        try {
            while ((line = stream.readLine()) != null) {
                System.out.println(line);
                if (NO_DFU_DEVICE.matcher(line).matches()) {
                    return null;
                }
                Matcher matcher = DFU_DEVICE.matcher(line);
                if (matcher.matches()) {
                    return matcher.group(1);
                }
            }
        } catch (IOException ignored) {
        }
        return null;
    }

    /**
     * Reads the cube output until the "Download in progress" message.
     *
     * @param stream Output from the cube programmer.
     * @return true if that message was found.
     */
    private boolean waitForDownloadStart(BufferedReader stream) {
        String line;
        try {
            while ((line = stream.readLine()) != null) {
                System.out.println(line);
                Matcher matcher = DOWNLOAD_STARTED.matcher(line);
                if (matcher.matches()) {
                    return true;
                }
            }
        } catch (IOException ignored) {
        }
        return false;
    }

    private void sleep(long ms) {
        try {Thread.sleep(ms);} catch (Exception ignored) {}
    }

    /**
     * Watches the progress of the download, invokes a progress callback.
     *
     * @param stream     Output from the cube programmer.
     * @param onProgress function to be called as the update progresses.
     * @return true if progress made it to 100%, false if the output stopped before 100%.
     */
    private boolean watchDownload(BufferedReader stream, Consumer<Integer> onProgress) {
        char[] cbuf = new char[20];
        StringBuilder numberBuilder = new StringBuilder();
        try {
            int n = 0;
            while (true) {
                int intChar = stream.read();
                if (intChar == -1) return false;
                char ch = (char) intChar;
                if (Character.isDigit(ch)) {
                    numberBuilder.append(ch);
                } else if (ch == '%') {
                    int progress = Integer.parseInt(numberBuilder.toString());
                    numberBuilder.delete(0, numberBuilder.length());
                    System.out.printf("%d \n", progress);
                    onProgress.accept(progress);
                    if (progress == 100) return true;
                }
            }
        } catch (IOException ignored) {
        }
        return false;
    }

    /**
     * After the progress has counted up to 100%, waits for the completion and duration outputs.
     *
     * @param stream Output from the cube programmer.
     * @return true if the download finished successfully. Also sets global 'updateOK = true'.
     */
    private boolean waitForDownloadFinish(BufferedReader stream) {
        String line;
        try {
            while ((line = stream.readLine()) != null) {
                System.out.println(line);
                Matcher matcher = DOWNLOAD_FINISHED.matcher(line);
                if (matcher.matches()) {
                    System.out.printf(
                        "In 'waitForDownloadFinish', download finished, setting time to close to 1000ms, state=%s \n",
                        state);
                    timeToClose = System.currentTimeMillis() + 1000;
                    updateOk = true;
                }
                matcher = DOWNLOAD_DURATION.matcher(line);
                if (matcher.matches()) {
                    // Hours, minutes, seconds, milliseconds
                    long downloadDuration = Integer.parseInt(matcher.group(1));
                    downloadDuration = downloadDuration * 60 + Integer.parseInt(matcher.group(2));
                    downloadDuration = downloadDuration * 60 + Integer.parseInt(matcher.group(3));
                    downloadDuration = downloadDuration * 1000 + Integer.parseInt(matcher.group(4));
                    return true;
                }
            }
        } catch (IOException ignored) {
        }
        return updateOk;
    }

    /**
     * Did the update finish successfully?
     *
     * @return true if the update finished successfully.
     */
    public Boolean isOk() {
        return updateOk;
    }
}
