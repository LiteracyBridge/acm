package org.literacybridge.acm.cloud.AuthenticationDialog;

import org.literacybridge.acm.gui.Assistant.FlexTextField;
import org.literacybridge.acm.gui.Assistant.GBC;
import org.literacybridge.acm.gui.Assistant.PanelButton;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import static java.awt.GridBagConstraints.CENTER;
import static java.awt.GridBagConstraints.NONE;
import static org.literacybridge.acm.cloud.Authenticator.LoginOptions.NO_WAIT;
import static org.literacybridge.acm.gui.Assistant.AssistantPage.getGBC;

public class EmailCard extends CardContent {
    private static final String DIALOG_TITLE = "Login to %s";
    protected static final int CARD_HEIGHT = 150;

    private final PanelButton okButton;
    private final FlexTextField emailField;

    public EmailCard(WelcomeDialog welcomeDialog,
        WelcomeDialog.Cards panel)
    {
        super(welcomeDialog, String.format(DIALOG_TITLE, welcomeDialog.applicationName), panel);
        JPanel dialogPanel = this;
        // The GUI
        dialogPanel.setLayout(new GridBagLayout());
        GBC gbc = new GBC(getGBC()).setAnchor(CENTER);
        gbc.insets.bottom = 12; // tighter bottom spacing.

        // Amplio logo
        addScaledLogo();

        // User name
        emailField = new FlexTextField();
        emailField.setFont(getTextFont());
        emailField.setPlaceholder("Enter your email address");
        emailField.addKeyListener(textKeyListener);
        emailField.getDocument().addDocumentListener(textDocumentListener);
        dialogPanel.add(emailField, gbc);

        // Consume all vertical space here.
        dialogPanel.add(new JLabel(""), gbc.withWeighty(1.0));

        // Login button and Sign Up link.
        okButton = new PanelButton("OK");
        okButton.setFont(getTextFont());
        okButton.setBgColorPalette(AMPLIO_GREEN);
        okButton.addActionListener(this::onOk);
        okButton.setEnabled(false);

        dialogPanel.add(okButton, gbc.withFill(NONE));

        addComponentListener(componentAdapter);
    }

    @Override
    void onEnter() {
        onOk(null);
    }

    @Override
    void onShown(ActionEvent actionEvent) {
        super.onShown(actionEvent);
        emailField.setText(welcomeDialog.getEmail());
        emailField.setRequestFocusEnabled(true);
        emailField.requestFocusInWindow();

        // If no_wait is specified, and we have everything we need, go!
        if (welcomeDialog.options.contains(NO_WAIT) && okButton.isEnabled()) onOk(null);
    }

    /**
     * User clicked "Login" or pressed enter.
     * @param actionEvent is ignored.
     */
    private void onOk(ActionEvent actionEvent) {
        welcomeDialog.setEmail(emailField.getText());
        ok();
    }

    /**
     * Sets the enabled state of controls, based on which other controls have contents.
     */
    private void enableControls() {
        okButton.setEnabled(emailField.getText().length() > 0);
    }

    @SuppressWarnings("FieldCanBeLocal")
    private final KeyListener textKeyListener = new KeyAdapter() {
        @Override
        public void keyTyped(KeyEvent e) {
            super.keyTyped(e);
            enableControls();
        }
    };

    /**
     * Used to enable the ok button if an email address is pasted into the email field (because
     * we're not listening to that key, we'd otherwise miss the presence of the email address).
     */
    @SuppressWarnings("FieldCanBeLocal")
    private final DocumentListener textDocumentListener = new DocumentListener() {
        @Override
        public void insertUpdate(DocumentEvent e) {
            enableControls();
        }
        @Override
        public void removeUpdate(DocumentEvent e) {
            enableControls();
        }
        @Override
        public void changedUpdate(DocumentEvent e) {
            enableControls();
        }
    };

}
