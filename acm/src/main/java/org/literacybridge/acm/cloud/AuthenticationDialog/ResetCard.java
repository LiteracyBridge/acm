package org.literacybridge.acm.cloud.AuthenticationDialog;

import org.literacybridge.acm.gui.Assistant.FlexTextField;
import org.literacybridge.acm.gui.Assistant.GBC;
import org.literacybridge.acm.gui.Assistant.PanelButton;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;

import static java.awt.GridBagConstraints.CENTER;
import static org.literacybridge.acm.gui.Assistant.AssistantPage.getGBC;

public class ResetCard extends CardContent {
    private final static String DIALOG_TITLE = "Reset Password";

    private final FlexTextField usernameField;
    private final FlexTextField passwordField1;
    private final FlexTextField passwordField2;
    private final JLabel mismatchWarning;
    private final FlexTextField resetCode;
    private final PanelButton changePassword;

    public ResetCard(WelcomeDialog welcomeDialog, WelcomeDialog.Cards panel) {
        super(welcomeDialog, DIALOG_TITLE, panel);
        JPanel dialogPanel = this;

        // The GUI
        dialogPanel.setLayout(new GridBagLayout());
        GBC gbc = new GBC(getGBC()).setAnchor(CENTER);
        gbc.insets.bottom = 12; // tighter bottom spacing.

        // Amplio logo
        JLabel logoLabel = new JLabel(getScaledLogo());
        dialogPanel.add(logoLabel, gbc);

        // User name
        usernameField = new FlexTextField();
        usernameField.setFont(getTextFont());
        usernameField.setIcon(getPersonIcon());
        usernameField.setEnabled(false);
        usernameField.getDocument().addDocumentListener(passwordDocListener);
        dialogPanel.add(usernameField, gbc);

        // Password
        gbc.insets.bottom = 5; // tighter bottom spacing.
        passwordField1 = new FlexTextField();
        passwordField1.setFont(getTextFont());
        passwordField1.setPlaceholder("Password");
        passwordField1.setIsPassword(true).setRevealPasswordEnabled(true);
        passwordField1.getDocument().addDocumentListener(passwordDocListener);
        dialogPanel.add(passwordField1, gbc);

        // Password, again
        passwordField2 = new FlexTextField();
        passwordField2.setFont(getTextFont());
        passwordField2.setPlaceholder("Repeat password");
        passwordField2.setIsPassword(true).setRevealPasswordEnabled(true);
        passwordField2.getDocument().addDocumentListener(passwordDocListener);
        dialogPanel.add(passwordField2, gbc);

        // Option checkboxes, and Password mismatch warning.
        Box hBox = Box.createHorizontalBox();
        mismatchWarning = new JLabel("Passwords don't match.");
        mismatchWarning.setForeground(Color.RED);
        Font font = mismatchWarning.getFont();
        font = new Font(font.getName(), font.getStyle()|Font.ITALIC, font.getSize());
        mismatchWarning.setFont(font);
        mismatchWarning.setVisible(false);
        hBox.add(mismatchWarning);
        hBox.add(Box.createHorizontalGlue());
        gbc.insets.bottom = 12;
        dialogPanel.add(hBox, gbc);

        // Reset code from server
        resetCode = new FlexTextField();
        resetCode.setFont(getTextFont());
        resetCode.setPlaceholder("Enter reset code");
        resetCode.getDocument().addDocumentListener(passwordDocListener);
        dialogPanel.add(resetCode, gbc);

        // Consume all vertical space here.
        dialogPanel.add(new JLabel(""), gbc.withWeighty(1.0));

        // Buttons
        hBox = Box.createHorizontalBox();
        changePassword = new PanelButton("Change Password");
        changePassword.setFont(getTextFont());
        changePassword.setBgColorPalette(AMPLIO_GREEN);
        changePassword.addActionListener(this::onOk);
        changePassword.setEnabled(false);
        hBox.add(changePassword);
        hBox.add(Box.createHorizontalStrut(20));
        PanelButton cancel = new PanelButton("Cancel");
        cancel.setFont(getTextFont());
        cancel.setBgColorPalette(AMPLIO_GREEN);
        cancel.addActionListener(this::onCancel);
        hBox.add(cancel);
        hBox.add(Box.createHorizontalGlue());

        dialogPanel.add(hBox, gbc);

        addComponentListener(componentAdapter);
    }

    void onCancel(ActionEvent actionEvent) {
        cancel();
    }

    private void onOk(ActionEvent actionEvent) {
        // Unfortunately, cognito doesn't return any success/failure status on this call.
        welcomeDialog.cognitoInterface.updatePassword(usernameField.getText(), passwordField1.getText(), resetCode.getText());
        welcomeDialog.setPassword(passwordField1.getText());
        ok();
    }

    /**
     * Handles any actions that need to be taken when the panel is shown or hidden.
     */
    @Override
    void onShown() {
        usernameField.setText(welcomeDialog.getUsername());
        passwordField1.setText(null);
        passwordField2.setText(null);
        passwordField1.setRevealPasswordEnabled(true).setPasswordRevealed(false);
        passwordField2.setRevealPasswordEnabled(true).setPasswordRevealed(false);
        passwordField1.setRequestFocusEnabled(true);
        passwordField1.requestFocusInWindow();
    }

   /**
     * As the user types into various text boxes, sets the mismatch warning and enables/disables
     * the "Change" button as appropriate.
     */
    @SuppressWarnings("FieldCanBeLocal")
    private final DocumentListener passwordDocListener = new DocumentListener() {
        private void check() {
            String name = usernameField.getText();
            String p1 = passwordField1.getText();
            String p2 = passwordField2.getText();
            String pin = resetCode.getText();
            mismatchWarning.setVisible(p1.length() > 0 && p2.length() > 0 && !p1.equals(p2));
            changePassword.setEnabled(name.length() > 0 && p1.length() > 0 && p1.equals(p2) && pin.length() > 5);
        }
        @Override
        public void insertUpdate(DocumentEvent e) {
            check();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            check();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            check();
        }
    };

}
