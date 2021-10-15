package org.literacybridge.acm.tbloader;

import org.literacybridge.acm.gui.util.UIUtils;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.logging.Logger;

import static org.literacybridge.acm.gui.util.UIUtils.UiOptions.TOP_THIRD;

public class Experimental extends JDialog {
    private static final Logger LOG = Logger.getLogger(Tb2FirmwareUpdater.class.getName());

    public final Timer timer;
    public JButton closeButton;
    public String volumeLabel;
    public String createDate;
    public long totalDiskSpace;
    public long availableDiskSpace;
    public boolean noFurtherAction = false;
    public int numHiddenFiles;
    public boolean windowsInsanity;
    public String totalDiskSpaceUnits;
    public String availableDiskSpaceUnits;
    public String totalSizeHiddenFilesunits;
    public long totalSizeHiddenFiles;
    public boolean gotLastLine;

    public static void go(ActionEvent e) {
        new Experimental().setVisible(true);
    }

    public Experimental() {
        super();
        setTitle("Experimental Feature(s)");

        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(new EmptyBorder(5, 15, 5, 15));

        setLayout(new BorderLayout());

//        panel.add(buildPromptPanel(), BorderLayout.CENTER);

        Box buttonsBox = createNavigationButtons();
        panel.add(buttonsBox, BorderLayout.SOUTH);

        timer = new Timer(2000, this::timerListener);
        timer.start();

        add(panel);
        setSize(500, 300);
        UIUtils.centerWindow(this, TOP_THIRD);
    }

    private void timerListener(ActionEvent actionEvent) {
    }

    /**
     * Creates the cancel/close button.
     *
     * @return a box with the button.
     */
    private Box createNavigationButtons() {
        Box hbox = Box.createHorizontalBox();
        hbox.setBorder(new EmptyBorder(10, 10, 10, 10));

        JButton chkdskButton = new JButton("Chkdsk");
        chkdskButton.addActionListener(this::chkdsk);
        hbox.add(chkdskButton);
        hbox.add(Box.createHorizontalStrut(10));

        JButton chkdskFButton = new JButton("Chkdsk /f");
        chkdskFButton.addActionListener(this::chkdskF);
        hbox.add(chkdskFButton);
        hbox.add(Box.createHorizontalStrut(10));

        JButton formatButton = new JButton("Format");
        formatButton.addActionListener(this::format);
        hbox.add(formatButton);
        hbox.add(Box.createHorizontalStrut(10));

        closeButton = new JButton("Cancel");

        closeButton.addActionListener(actionEvent -> setVisible(false));
        hbox.add(Box.createHorizontalGlue());
        hbox.add(Box.createHorizontalStrut(15));
        hbox.add(closeButton);

        return hbox;
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

    private boolean chkdsk(ActionEvent e) {
        try {
            return new CommandLineUtils(null).checkDisk("f:");
        } catch (Exception ignored) {
        } finally {
            setVisible(false);
        }
        return false;
    }

    private boolean chkdskF(ActionEvent e) {
        try {
            return new CommandLineUtils(null).checkDiskAndFix("f:", null);
        } catch (Exception ignored) {
        } finally {
            setVisible(false);
        }
        return false;
    }

    private boolean format(ActionEvent e) {
        try {
            return new CommandLineUtils(null).formatDisk("f:", "A-LABEL");
        } catch (Exception ignored) {
        } finally {
            setVisible(false);
        }
        return false;
    }
}
