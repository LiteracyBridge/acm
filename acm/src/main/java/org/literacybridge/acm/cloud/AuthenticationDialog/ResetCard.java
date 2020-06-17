package org.literacybridge.acm.cloud.AuthenticationDialog;

import org.literacybridge.acm.gui.Assistant.FlexTextField;
import org.literacybridge.acm.gui.Assistant.GBC;
import org.literacybridge.acm.gui.Assistant.PanelButton;
import org.literacybridge.acm.gui.Assistant.RoundedLineBorder;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.Color;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;

import static java.awt.GridBagConstraints.CENTER;
import static org.literacybridge.acm.gui.Assistant.AssistantPage.getGBC;

public class ResetCard extends CardContent {
    private final static String DIALOG_TITLE = "Reset Password";
    protected static final int CARD_HEIGHT = 580;

    private final JLabel usernameField;
    private final FlexTextField passwordField;
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

        dialogPanel.add(new JLabel("<html>Check email for <span style='font-size:1.1em'>\"Your Amplio verification code\"</span>. Enter your " +
            "new password and the verification code, then click Change Password. If you remember your password, " +
            "simply click Cancel to return the sign-in screen."), gbc);

        // User name
//        usernameField = new FlexTextField();
//        usernameField.setFont(getTextFont());
//        usernameField.setIcon(getPersonIcon());
//        usernameField.setEnabled(true);
//        usernameField.getDocument().addDocumentListener(passwordDocListener);
//        dialogPanel.add(usernameField, gbc);

        usernameField = new JLabel();
        int iconsize = 16;
        ImageIcon person = new ImageIcon(getPersonIcon().getImage().getScaledInstance(iconsize, iconsize, Image.SCALE_SMOOTH));
        usernameField.setIcon(person);
        Border nameBorder = new CompoundBorder(new RoundedLineBorder(Color.DARK_GRAY, 1, 8), new EmptyBorder(3,3,2,2));
        usernameField.setBorder(nameBorder);
        dialogPanel.add(usernameField, gbc);

        // Password
        gbc.insets.bottom = 5; // tighter bottom spacing.
        passwordField = new FlexTextField();
        passwordField.setFont(getTextFont());
        passwordField.setPlaceholder("Your chosen new password");
        passwordField.setIsPassword(true).setRevealPasswordEnabled(true);
        passwordField.getDocument().addDocumentListener(passwordDocListener);
        gbc.insets.bottom = 4; // tighter bottom spacing.
        dialogPanel.add(passwordField, gbc);
        gbc.insets.bottom = 12;

        JLabel rules = new JLabel(PASSWORD_RULES_FORMATTED);
        dialogPanel.add(rules, gbc);


        Box hBox = Box.createHorizontalBox();

        // Reset code from server
        resetCode = new FlexTextField();
        resetCode.setFont(getTextFont());
        resetCode.setPlaceholder("Verification code from email");
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
        welcomeDialog.cognitoInterface.updatePassword(usernameField.getText(), passwordField.getText(), resetCode.getText());
        welcomeDialog.setPassword(passwordField.getText());
        ok();
    }

    /**
     * Handles any actions that need to be taken when the panel is shown or hidden.
     */
    @Override
    void onShown() {
        super.onShown();
        usernameField.setText(welcomeDialog.getEmail());
        passwordField.setText(null);
        passwordField.setRevealPasswordEnabled(true).setPasswordRevealed(false);
        passwordField.setRequestFocusEnabled(true);
        passwordField.requestFocusInWindow();
    }

   /**
     * As the user types into various text boxes, sets the mismatch warning and enables/disables
     * the "Change" button as appropriate.
     */
    @SuppressWarnings("FieldCanBeLocal")
    private final DocumentListener passwordDocListener = new DocumentListener() {
        private void check() {
            String name = usernameField.getText();
            String p1 = passwordField.getText();
            String pin = resetCode.getText();
            boolean pValid = PASSWORD_PATTERN.matcher(p1).matches();
            changePassword.setEnabled(name.length() > 0 && pValid && pin.length() > 5);
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
