package org.literacybridge.acm.cloud;

import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.config.AmplioHome;
import org.literacybridge.acm.gui.dialogs.PopUp;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import java.awt.Desktop;
import java.awt.Dimension;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;

public class UpdatePrompter {
    private static final String NOW = "Update Now";
    private static final String LATER = "Update Later";
    private static final String UPDATE_URL = "https://downloads.amplio.org/software/index.html";

    private static final Calendar PROMPT_EVERY_DAY_AFTER = new Calendar.Builder().setDate(2021, 11, 15).build();
    private static final Calendar PROMPT_EVERY_TIME_AFTER = new Calendar.Builder().setDate(2022,0,1).build();
    private static final long ONE_DAY_MILLIS = 24 * 60 * 60 * 1000;
    private static final long THREE_DAYS_MILLIS = 3 * 24 * 60 * 60 * 1000;


    public static void go() {
        new UpdatePrompter().checkForUpdate();
    }

    /**
     * Prompts the user to upgrade to the setup program, if they haven't already. As some yet-to-be-defined
     * cutoff date approaches, can prompt more vigorously.
     */
    private void checkForUpdate() {
        if (shouldShowPrompt()) {
            JComponent edPane = buildUpdateText();
            int selected = new PopUp.Builder()
                .withMessageType(JOptionPane.WARNING_MESSAGE)
                .withTitle("Update Required")
                .withContents(edPane)
                .withSize(new Dimension(475, 250))
                .withOptions(new Object[]{NOW})
                .go();
            if (selected == 0 && openUpdateLink()) {
                System.exit(0);
            }
            ACMConfiguration.getInstance().setLatestUpdateSetupPromptDate();
        }
    }

    /**
     * Should the user be prompted to update? Based on time since last prompt, and time until forced udpate.
     *
     * @return true if the user should be prompted.
     */
    private boolean shouldShowPrompt() {
        if (AmplioHome.isOldStyleSetup() || ACMConfiguration.getInstance().alwaysPromptForUpdate()) {
            Calendar now = new Calendar.Builder().setInstant(System.currentTimeMillis()).build();
            if (now.after(PROMPT_EVERY_TIME_AFTER) ) { // || ACMConfiguration.getInstance().alwaysPromptForUpdate()) {
                return true;
            }
            long millisSincePrompt = System.currentTimeMillis() - ACMConfiguration.getInstance().getLatestUpdateSetupPromptDate();
            long noPromptWait = (now.before(PROMPT_EVERY_DAY_AFTER)) ?
                               THREE_DAYS_MILLIS :
                               ONE_DAY_MILLIS;
            return millisSincePrompt > noPromptWait;
        }
        return false;
    }

    /**
     * Builds the text to be shown to the user, prompting them to update.
     *
     * @return A JComponent containing the text.
     */
    private JComponent buildUpdateText() {
        return new JLabel("<html>Updates are required to continue running Amplio's software. " +
            "Everyone must install these updates. <br/><br/>" +
            "Click \"Update Now\" to be taken to a web page where you can perform the update.");
    }

    /**
     * Tries to open the update web page.
     *
     * @return true if the browser could be launched, false otherwise. Also returns false if
     * the value of UPDATE.URL is a malformed URL.
     */
    private boolean openUpdateLink() {
        try {
            URL url = new URL(UpdatePrompter.UPDATE_URL);
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
