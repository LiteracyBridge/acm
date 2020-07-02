package org.literacybridge.acm.cloud.AuthenticationDialog;

import org.apache.commons.lang3.StringUtils;
import org.literacybridge.acm.gui.Assistant.FlexTextField;
import org.literacybridge.acm.gui.Assistant.GBC;
import org.literacybridge.acm.gui.Assistant.PanelButton;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.util.HashMap;
import java.util.Map;

import static org.literacybridge.acm.gui.Assistant.AssistantPage.getGBC;

public class RegisterCard extends CardContent {
    private static final String DIALOG_TITLE = "Register User";
    protected static final int CARD_HEIGHT = 590;

    private final FlexTextField emailField;
    private final FlexTextField passwordField;
    private final FlexTextField nameField;
    private final PanelButton createAccount;
    private final PanelButton haveCode;

    public RegisterCard(WelcomeDialog welcomeDialog, WelcomeDialog.Cards panel) {
        super(welcomeDialog, DIALOG_TITLE, panel);
        JPanel dialogPanel = this;
        // The GUI
        dialogPanel.setLayout(new GridBagLayout());
        GBC gbc = new GBC(getGBC());
        gbc.insets.bottom = 5; // tighter bottom spacing.

        // Amplio logo
        JLabel logoLabel = new JLabel(getScaledLogo());
        dialogPanel.add(logoLabel, gbc);

        dialogPanel.add(new JLabel("<html>Enter your email address and a password, and click \"Register User\". " +
            "You will be taken to a new screen in which to " +
            "enter a confirmation code that you will receive by email."), gbc);

        // Email name
        emailField = new FlexTextField();
        emailField.setFont(getTextFont());
        emailField.setPlaceholder("Enter your email address");
        emailField.getDocument().addDocumentListener(passwordDocListener);
        dialogPanel.add(emailField, gbc);

        // Password
        passwordField = new FlexTextField();
        passwordField.setFont(getTextFont());
        passwordField.setPlaceholder("Your chosen password");
        passwordField.setIsPassword(true).setRevealPasswordEnabled(true);
        passwordField.getDocument().addDocumentListener(passwordDocListener);
        dialogPanel.add(passwordField, gbc);

        nameField = new FlexTextField();
        nameField.setFont(getTextFont());
        nameField.setPlaceholder("Please enter your full name");
        nameField.setIsPassword(false);
        nameField.getDocument().addDocumentListener(passwordDocListener);
        dialogPanel.add(nameField, gbc);
        gbc.insets.bottom = 12;


        JLabel rules = new JLabel(PASSWORD_RULES_FORMATTED);
        dialogPanel.add(rules, gbc);

        // Consume all vertical space here.
        dialogPanel.add(new JLabel(""), gbc.withWeighty(1.0));

        // Buttons.
        Box hBox = Box.createHorizontalBox();
        createAccount = new PanelButton("Register User");
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

        haveCode.setVisible(false);

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
        // Add any Cognito properties here.
        Map<String,String> attributes = new HashMap<>();
        attributes.put("name", nameField.getText());

        String signUpResult = welcomeDialog.cognitoInterface.signUpUser(emailField.getText(),
            passwordField.getText(),
            attributes);

        if (signUpResult != null) {
            welcomeDialog.setMessage(signUpResult);
            return;
        }
        welcomeDialog.setEmail(emailField.getText());
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
     * @param actionEvent is passed to super. Optionally used to show fields controlled by modifier keys.
     */
    @Override
    void onShown(ActionEvent actionEvent) {
        super.onShown(actionEvent);
        // To show a field only if the shift and ctrl keys are pressed, used this:
//        int shiftMask = InputEvent.SHIFT_MASK|InputEvent.CTRL_MASK;
//        usernameField.setVisible((actionEvent.getModifiers() &  shiftMask) == shiftMask);
        passwordField.setText(null);
        passwordField.setRevealPasswordEnabled(true).setPasswordRevealed(false);
        emailField.setText(null);
        emailField.setRequestFocusEnabled(true);
        emailField.requestFocusInWindow();
    }

    /**
     * As the user types into various text boxes, sets the mismatch warning and enables/disables
     * the "Change" button as appropriate.
     */
    @SuppressWarnings("FieldCanBeLocal")
    private final DocumentListener passwordDocListener = new DocumentListener() {
        private void check() {
            String p1 = passwordField.getText();
            String email = emailField.getText();
            boolean pValid = PASSWORD_PATTERN.matcher(p1).matches();
            createAccount.setEnabled(pValid  && email.length() > 5);
            haveCode.setEnabled(email.length() > 0);
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
