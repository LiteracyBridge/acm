package org.literacybridge.acm.cloud.AuthenticationDialog;

import org.literacybridge.acm.gui.Assistant.GBC;
import org.literacybridge.acm.gui.UIConstants;
import org.literacybridge.acm.gui.util.UIUtils;
import org.literacybridge.acm.utils.SwingUtils;

import javax.swing.*;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.regex.Pattern;

import static java.awt.GridBagConstraints.CENTER;
import static org.literacybridge.acm.Constants.AMPLIO_GREEN_B;
import static org.literacybridge.acm.Constants.AMPLIO_GREEN_G;
import static org.literacybridge.acm.Constants.AMPLIO_GREEN_R;
import static org.literacybridge.acm.gui.Assistant.AssistantPage.getGBC;

class CardContent extends JPanel {
    public static final Color AMPLIO_GREEN = new Color(AMPLIO_GREEN_R, AMPLIO_GREEN_G, AMPLIO_GREEN_B);

    static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])\\S{8,99}$");
    static final String PASSWORD_RULES = "At least 8 characters, with lowercase, UPPERCASE, and digit(s).";
    static final String PASSWORD_RULES_FORMATTED = "<html><span style='font-size:0.9em;'><i>" + PASSWORD_RULES + "</i></span></html>";

    final WelcomeDialog welcomeDialog;
    final WelcomeDialog.Cards panel;
    private final String dialogTitle;

    private static Font textFont;
    Font getTextFont() {
        if (textFont == null) {
            Font uFont = new JTextField().getFont();
            textFont = new Font(uFont.getName(), uFont.getStyle(),uFont.getSize()+2 );
        }
        return textFont;
    }

    private static ImageIcon personIcon;
    ImageIcon getPersonIcon() {
        if (personIcon == null) {
            personIcon = new ImageIcon(UIConstants.getResource("person_256.png"));
        }
        return personIcon;
    }

    public static int logoHeight = 0;
    private static final int iconSize = 350;
    private static ImageIcon scaledIcon;
    private static final GBC gbc = new GBC(getGBC()).withAnchor(CENTER).withInsets(new Insets(10,0,20,0));

    ImageIcon getScaledLogo() {
        if (scaledIcon == null) {
//            logoIcon = new ImageIcon(UIConstants.getResource("Amplio-Logo-NoTagline-FullColor-Square.png"));
            scaledIcon = SwingUtils.getScaledImage("Amplio_horiz_color_HiRes.png", iconSize, -1);
            logoHeight = scaledIcon.getIconHeight() + gbc.insets.top + gbc.insets.bottom;
        }
        return scaledIcon;
    }

    void addScaledLogo() {
        JLabel logoLabel = new JLabel(getScaledLogo());
        this.add(logoLabel, gbc);
    }

    CardContent(WelcomeDialog welcomeDialog,
        String dialogTitle,
        WelcomeDialog.Cards panel) {
        super();
        this.welcomeDialog = welcomeDialog;
        this.dialogTitle = dialogTitle;
        this.panel = panel;

        addComponentListener(componentAdapter);
    }

    CardContent(WelcomeDialog welcomeDialog, WelcomeDialog.Cards panel) {
        this(welcomeDialog, "Authentication", panel);
    }

    /**
     * Handles any actions that need to be taken when the panel is shown or hidden.
     */
    ComponentAdapter componentAdapter = new ComponentAdapter() {
        @Override
        public void componentShown(ComponentEvent evt) {
            welcomeDialog.setTitle(dialogTitle);
//            onShown();
        }
    };

    /**
     * Called when the card is shown.
     * @param actionEvent is unused
     */
    void onShown(ActionEvent actionEvent) {
        welcomeDialog.setTitle(dialogTitle);
        // Override as needed
    }

    void onEnter() {
        // Override as needed
    }

    void onCancel(ActionEvent e) {
        // Override as needed
    }

    void ok() {
        welcomeDialog.ok(this);
    }
    void cancel() {
        welcomeDialog.cancel(this);
    }

    void resizeForWidth(int widest) {
        Dimension d = welcomeDialog.getMinimumSize();
        d.width = Math.max(d.width, widest + 108);
        welcomeDialog.setMinimumSize(d);
        UIUtils.centerWindow(welcomeDialog, UIUtils.UiOptions.HORIZONTAL_ONLY);
    }


}
