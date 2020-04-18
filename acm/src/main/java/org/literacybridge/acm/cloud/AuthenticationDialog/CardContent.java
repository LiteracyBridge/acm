package org.literacybridge.acm.cloud.AuthenticationDialog;

import org.literacybridge.acm.gui.UIConstants;

import javax.swing.*;
import java.awt.Color;
import java.awt.Font;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import static org.literacybridge.acm.Constants.AMPLIO_GREEN_B;
import static org.literacybridge.acm.Constants.AMPLIO_GREEN_G;
import static org.literacybridge.acm.Constants.AMPLIO_GREEN_R;

class CardContent extends JPanel {
    public static final Color AMPLIO_GREEN = new Color(AMPLIO_GREEN_R, AMPLIO_GREEN_G, AMPLIO_GREEN_B);

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

    private static final int iconSize = 256;
    private static ImageIcon logoIcon;
    private static ImageIcon scaledIcon;
    ImageIcon getScaledLogo() {
        if (logoIcon == null) {
            logoIcon = new ImageIcon(UIConstants.getResource("Amplio-Logo-NoTagline-FullColor-Square.png"));
            scaledIcon = new ImageIcon(logoIcon.getImage().getScaledInstance(iconSize, iconSize, Image.SCALE_SMOOTH));
        }
        return scaledIcon;
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
     */
    void onShown() {
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


}
