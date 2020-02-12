package org.literacybridge.acm.cloud.AuthenticationDialog;

import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;
import org.literacybridge.acm.cloud.Authenticator;
import org.literacybridge.acm.gui.Assistant.PlaceholderTextField;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;

import static org.literacybridge.acm.gui.Assistant.AssistantPage.getGBC;

public class SignUpPanel extends DialogPanel {
    private static final String DIALOG_TITLE = "Create User ID";

    private final PlaceholderTextField usernameField;
    private final PlaceholderTextField emailField;
    private final PlaceholderTextField phoneNumberField;
    private final PlaceholderTextField passwordField1;
    private final PlaceholderTextField passwordField2;
    private final JCheckBox showPassword;
    private final JLabel mismatchWarning;
    private final JButton createAccount;
    private final JButton haveCode;

    public SignUpPanel(DialogController dialogController) {
        super(dialogController, DIALOG_TITLE);
        JPanel dialogPanel = this;
        // The GUI
        dialogPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = getGBC();
        gbc.insets.bottom = 5; // tighter bottom spacing.

        // User name
        usernameField = new PlaceholderTextField();
        usernameField.setPlaceholder("Desired user name");
        usernameField.getDocument().addDocumentListener(passwordDocListener);
        dialogPanel.add(usernameField, gbc);

        // Email name
        emailField = new PlaceholderTextField();
        emailField.setPlaceholder("Email Address");
        emailField.getDocument().addDocumentListener(passwordDocListener);
        dialogPanel.add(emailField, gbc);

        // Phone number
        phoneNumberField = new PlaceholderTextField();
        phoneNumberField.setPlaceholder("Phone number (optional)");
        phoneNumberField.getDocument().addDocumentListener(passwordDocListener);
        dialogPanel.add(phoneNumberField, gbc);

        // Password
        passwordField1 = new PlaceholderTextField();
        passwordField1.setPlaceholder("Password");
        passwordField1.setMaskChar('*');
        passwordField1.getDocument().addDocumentListener(passwordDocListener);
        dialogPanel.add(passwordField1, gbc);

        // Password, again
        passwordField2 = new PlaceholderTextField();
        passwordField2.setPlaceholder("Repeat password");
        passwordField2.setMaskChar('*');
        passwordField2.getDocument().addDocumentListener(passwordDocListener);
        dialogPanel.add(passwordField2, gbc);

        // Option checkboxes, and Password mismatch warning.
        Box hBox = Box.createHorizontalBox();
        showPassword = new JCheckBox("Show password");
        showPassword.addActionListener(this::onShowPassword);
        hBox.add(showPassword);
        hBox.add(Box.createHorizontalStrut(10));

        mismatchWarning = new JLabel("Passwords don't match.");
        mismatchWarning.setForeground(Color.RED);
        Font font = mismatchWarning.getFont();
        font = new Font(font.getName(), font.getStyle()|Font.ITALIC, font.getSize());
        mismatchWarning.setFont(font);
        mismatchWarning.setVisible(false);
        hBox.add(mismatchWarning);
        hBox.add(Box.createHorizontalGlue());
        gbc.insets.bottom = 12; // tighter bottom spacing.
        dialogPanel.add(hBox, gbc);

        // Consume all vertical space here.
        gbc.weighty = 1.0;
        dialogPanel.add(new JLabel(""), gbc);
        gbc.weighty = 0;

        // Sign In button and Sign Up link.
        hBox = Box.createHorizontalBox();
        createAccount = new JButton("Create User ID");
        createAccount.addActionListener(this::onCreate);
        createAccount.setEnabled(false);
        hBox.add(createAccount);

        hBox.add(Box.createHorizontalStrut(20));
        haveCode = new JButton("Have Code");
        haveCode.addActionListener(this::haveCode);
        haveCode.setEnabled(false);
        hBox.add(haveCode);

        hBox.add(Box.createHorizontalStrut(20));
        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(this::onCancel);
        hBox.add(cancel);
        hBox.add(Box.createHorizontalGlue());

        gbc.insets.bottom = 0; // no bottom spacing.
        dialogPanel.add(hBox, gbc);

        addComponentListener(componentAdapter);
    }

    @Override
    void onEnter() {
        if (createAccount.isEnabled()) onCreate(null);
    }

    String getUsername() {
        return usernameField.getText();
    }
    /**
     * Gets the password and the state of "show password".
     * @return a Triple of password, allow show, do show.
     */
    Triple<String,Boolean,Boolean> getPassword() {
        return new ImmutableTriple<>(passwordField1.getText(),
            showPassword.isEnabled(), showPassword.isSelected());
    }

    private void onCreate(ActionEvent actionEvent) {
        dialogController.clearMessage();
        String signUpResult = Authenticator.getInstance()
            .signUpUser(usernameField.getText(),
                passwordField1.getText(),
                emailField.getText(),
                phoneNumberField.getText());

        if (signUpResult != null) {
            dialogController.setMessage(signUpResult);
            return;
        }

        dialogController.gotoConfirmationCard();
    }

    /**
     * User clicked "Have code", meaning that they
     * @param actionEvent
     */
    private void haveCode(ActionEvent actionEvent) {
        dialogController.gotoConfirmationCard();
    }

    void onCancel(ActionEvent actionEvent) {
        dialogController.cancel(this);
    }

    /**
     * Handles any actions that need to be taken when the panel is shown or hidden.
     */
    @Override
    void onShown() {
        usernameField.setText(null);
        passwordField1.setText(null);
        passwordField2.setText(null);
        showPassword.setSelected(false);
        onShowPassword(null);
        emailField.setText(null);
        phoneNumberField.setText(null);
    }


    /**
     * As the user types into various text boxes, sets the mismatch warning and enables/disables
     * the "Change" button as appropriate.
     */
    @SuppressWarnings("FieldCanBeLocal")
    private DocumentListener passwordDocListener = new DocumentListener() {
        private void check() {
            String name = usernameField.getText();
            String p1 = passwordField1.getText();
            String p2 = passwordField2.getText();
            String email = emailField.getText();
            mismatchWarning.setVisible(p1.length() > 0 && p2.length() > 0 && !p1.equals(p2));
            createAccount.setEnabled(name.length() > 0 && p1.length() > 0 && p1.equals(p2) && email.length() > 5);
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


    private void onShowPassword(ActionEvent actionEvent) {
        passwordField1.setMaskChar(showPassword.isSelected() ? (char)0 : '*');
        passwordField2.setMaskChar(showPassword.isSelected() ? (char)0 : '*');
    }


}
