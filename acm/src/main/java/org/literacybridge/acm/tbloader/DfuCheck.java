package org.literacybridge.acm.tbloader;

import org.jdesktop.swingx.VerticalLayout;
import org.literacybridge.acm.config.AmplioHome;
import org.literacybridge.acm.gui.Assistant.RoundedLineBorder;
import org.literacybridge.acm.gui.UIConstants;
import org.literacybridge.acm.gui.dialogs.PopUp;
import org.literacybridge.core.OSChecker;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

public class DfuCheck {
    private static final String NOW = "Install Now";
    private static final String LATER = "Install Later";
    private static final String UPDATE_URL = "https://downloads.amplio.org/software/index.html";

    public static boolean go() {
        return new DfuCheck().checkForDfu();
    }

    /**
     * Checks whether the STM32 DFU bootloader needs to be installed on this computer.
     * @return true if the software needs to be installed, false if it does not (or can not be).
     */
    boolean checkForDfu() {
        if (needUpdate()) {
            JComponent edPane = buildUpdateText();
            int selected = new PopUp.Builder()
                    .withMessageType(JOptionPane.WARNING_MESSAGE)
                    .withTitle("Additional Software Needed")
                    .withContents(edPane)
                    .withSize(new Dimension(600, 300))
                    .withOptions(new Object[]{NOW, LATER})
                    .go();
            if (selected == 0 && openUpdateLink()) {
                System.exit(0);
            }
            return false;
        }
        return true;
    }

    /**
     * Determines if the DFU_Driver is needed for this program, is missing, and we're on Windows
     * (where we can actually install it).
     *
     * @return true if the DFU_Driver needs to be installed, false if no need or can't
     */
    private boolean needUpdate() {
        File dfuDir = new File(AmplioHome.getDirectory(), "DFU_Driver");
        //noinspection UnnecessaryLocalVariable
        boolean needUpdate = TBLoader.getApplication().tbLoaderConfig.hasTbV2Devices() &&
                OSChecker.WINDOWS &&
                !new CommandLineUtils(null).hasDfuDriver();
        return needUpdate;
    }

    /**
     * Builds the text to be shown to the user, prompting them to update.
     *
     * @return A JComponent containing the text.
     */
    private JComponent buildUpdateText() {
        JPanel panel = new JPanel();
        panel.setLayout(new VerticalLayout());

        panel.add(new JLabel("<html>Additional software is needed to support Talking Book V2 devices. " +
            "You can wait until later to install the software but might not be able to load content " +
            "onto some Talking Books.<br/><br/>" +
            "Click \"Install Now\" to be taken to a web page where you can perform the installation. <br/>" + "" +
                "On that page, choose \"Install TBv2 Support\".<br/><br/>"));

        Color darkForestGreen = new Color(0x22, 0x8b, 0x22);
        Dimension size = new Dimension(300, 60);
        ImageIcon buttonIcon = new ImageIcon(UIConstants.getResource("tbv2Support.png"));
        JLabel buttonLabel = new JLabel(buttonIcon);
        buttonLabel.setForeground(darkForestGreen);
        buttonLabel.setPreferredSize(size);
        buttonLabel.setMaximumSize(size);
        buttonLabel.setText("<html><b>&lt;=</b> Choose this to install TBv2 support.");
        buttonLabel.setBorder(new RoundedLineBorder(darkForestGreen, 2, 8));
        buttonLabel.setBackground(new Color(255, 255, 240));
        buttonLabel.setOpaque(true);
        panel.add(buttonLabel);

        panel.add(new JLabel("  "));

        return panel;
    }

    /**
     * Tries to open the update web page.
     *
     * @return true if the browser could be launched, false otherwise. Also returns false if
     * the value of UPDATE.URL is a malformed URL.
     */
    private boolean openUpdateLink() {
        try {
            URL url = new URL(UPDATE_URL);
            return openLink(url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Tries to open a browser for the given URL.
     *
     * @param url to be opened.
     * @return true if the browser could be launched, false otherwise.
     */
    private boolean openLink(URL url) {
        if (Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().browse(url.toURI());
                return true;
            } catch (IOException | URISyntaxException ex) {
                ex.printStackTrace();
            }
        }
        return false;
    }
}
