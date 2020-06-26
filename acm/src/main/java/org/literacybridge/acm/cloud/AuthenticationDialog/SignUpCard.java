package org.literacybridge.acm.cloud.AuthenticationDialog;

import org.literacybridge.acm.gui.Assistant.FlexTextField;
import org.literacybridge.acm.gui.Assistant.GBC;
import org.literacybridge.acm.gui.Assistant.PanelButton;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;

import static org.literacybridge.acm.gui.Assistant.AssistantPage.getGBC;

public class SignUpCard extends CardContent {
    private static final String DIALOG_TITLE = "Create User ID";
    protected static final int CARD_HEIGHT = 635;

    private final FlexTextField usernameField;
    private final FlexTextField emailField;
    private final FlexTextField phoneNumberField;
    private final FlexTextField passwordField;
    private final PanelButton createAccount;
    private final PanelButton haveCode;

    public SignUpCard(WelcomeDialog welcomeDialog, WelcomeDialog.Cards panel) {
        super(welcomeDialog, DIALOG_TITLE, panel);
        JPanel dialogPanel = this;
        // The GUI
        dialogPanel.setLayout(new GridBagLayout());
        GBC gbc = new GBC(getGBC());
        gbc.insets.bottom = 5; // tighter bottom spacing.

        // Amplio logo
        JLabel logoLabel = new JLabel(getScaledLogo());
        dialogPanel.add(logoLabel, gbc);

        dialogPanel.add(new JLabel("<html>Pick a user name, enter your email address, and optional phone number " +
            "(format +18885551212), and a password, and click Create User ID. You will be taken to a new screen in which to " +
            "enter a verification code that you will receive by email. If you already have the code, click Have Code."), gbc);

        // User name
        usernameField = new FlexTextField();
        usernameField.setFont(getTextFont());
        usernameField.setIcon(getPersonIcon());
        usernameField.setPlaceholder("Desired user name");
        usernameField.getDocument().addDocumentListener(passwordDocListener);
        dialogPanel.add(usernameField, gbc);

        // Email name
        emailField = new FlexTextField();
        emailField.setFont(getTextFont());
        emailField.setPlaceholder("Email Address");
        emailField.getDocument().addDocumentListener(passwordDocListener);
        dialogPanel.add(emailField, gbc);

        // Phone number
        phoneNumberField = new FlexTextField();
        phoneNumberField.setFont(getTextFont());
        phoneNumberField.setPlaceholder("Phone number (optional)");
        phoneNumberField.getDocument().addDocumentListener(passwordDocListener);
        dialogPanel.add(phoneNumberField, gbc);

        // Password
        passwordField = new FlexTextField();
        passwordField.setFont(getTextFont());
        passwordField.setPlaceholder("Password");
        passwordField.setIsPassword(true).setRevealPasswordEnabled(true);
        passwordField.getDocument().addDocumentListener(passwordDocListener);
        dialogPanel.add(passwordField, gbc);
        gbc.insets.bottom = 12;

        JLabel rules = new JLabel(PASSWORD_RULES_FORMATTED);
        dialogPanel.add(rules, gbc);

        // Consume all vertical space here.
        dialogPanel.add(new JLabel(""), gbc.withWeighty(1.0));

        // Buttons.
        Box hBox = Box.createHorizontalBox();
        createAccount = new PanelButton("Create User ID");
        createAccount.setFont(getTextFont());
        Insets padding = createAccount.getPadding();
        padding.left = padding.right = 8;
        createAccount.setPadding(padding);
        createAccount.setBgColorPalette(AMPLIO_GREEN);
        createAccount.addActionListener(this::onCreate);
        createAccount.setEnabled(false);
        hBox.add(createAccount);

        hBox.add(Box.createHorizontalStrut(15));
        hBox.add(Box.createHorizontalGlue());
        haveCode = new PanelButton("Have Code");
        haveCode.setFont(getTextFont());
        haveCode.setPadding(padding);
        haveCode.setBgColorPalette(AMPLIO_GREEN);
        haveCode.addActionListener(this::haveCode);
        haveCode.setEnabled(false);
        hBox.add(haveCode);

        hBox.add(Box.createHorizontalGlue());
        hBox.add(Box.createHorizontalStrut(15));
        PanelButton cancel = new PanelButton("Cancel");
        cancel.setFont(getTextFont());
        cancel.setPadding(padding);
        cancel.setBgColorPalette(AMPLIO_GREEN);
        cancel.addActionListener(this::onCancel);
        hBox.add(cancel);

        dialogPanel.add(hBox, gbc);

        addComponentListener(componentAdapter);
    }

    @Override
    void onEnter() {
        if (createAccount.isEnabled()) onCreate(null);
    }

    private void onCreate(ActionEvent actionEvent) {
        welcomeDialog.clearMessage();
        String signUpResult = welcomeDialog.cognitoInterface.signUpUser(usernameField.getText(),
            passwordField.getText(),
            emailField.getText(),
            phoneNumberField.getText());

        if (signUpResult != null) {
            welcomeDialog.setMessage(signUpResult);
            return;
        }
        welcomeDialog.setUsername(usernameField.getText());
        welcomeDialog.setPassword(passwordField.getText());
        welcomeDialog.gotoConfirmationCard();
    }

    /**
     * User clicked "Have code", meaning that they
     *
     * @param actionEvent is unused
     */
    private void haveCode(ActionEvent actionEvent) {
        welcomeDialog.gotoConfirmationCard();
    }

    void onCancel(ActionEvent actionEvent) {
        cancel();
    }

    /**
     * Handles any actions that need to be taken when the panel is shown or hidden.
     */
    @Override
    void onShown() {
        super.onShown();
        usernameField.setText(null);
        passwordField.setText(null);
        passwordField.setRevealPasswordEnabled(true).setPasswordRevealed(false);
        emailField.setText(null);
        phoneNumberField.setText(null);
        usernameField.setRequestFocusEnabled(true);
        usernameField.requestFocusInWindow();
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
            String email = emailField.getText();
            boolean pValid = PASSWORD_PATTERN.matcher(p1).matches();
            createAccount.setEnabled(name.length() > 0 && pValid  && email.length() > 5);
            haveCode.setEnabled(name.length() > 0);
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
