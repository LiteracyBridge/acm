package org.literacybridge.acm.cloud.AuthenticationDialog;

import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;
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

public class ResetPanel extends DialogPanel {
    private final static String DIALOG_TITLE = "Reset Password";

    private final PlaceholderTextField usernameField;
    private final PlaceholderTextField passwordField1;
    private final PlaceholderTextField passwordField2;
    private final JCheckBox showPassword;
    private final JLabel mismatchWarning;
    private final PlaceholderTextField resetCode;
    private final JButton changePassword;

    public ResetPanel(DialogController dialogController) {
        super(dialogController, DIALOG_TITLE);
        JPanel dialogPanel = this;

        // The GUI
        dialogPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = getGBC();
        gbc.insets.bottom = 12; // tighter bottom spacing.

        // User name
        usernameField = new PlaceholderTextField();
        usernameField.setEnabled(false);
//        usernameField.setPlaceholder("User Name or Email Address");
        usernameField.getDocument().addDocumentListener(passwordDocListener);
        dialogPanel.add(usernameField, gbc);

        // Password
        gbc.insets.bottom = 5; // tighter bottom spacing.
        passwordField1 = new PlaceholderTextField();
        passwordField1.setPlaceholder("New Password");
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

        // Reset code from server
        resetCode = new PlaceholderTextField();
        resetCode.setPlaceholder("Enter reset code");
        resetCode.getDocument().addDocumentListener(passwordDocListener);
        dialogPanel.add(resetCode, gbc);

        // Consume all vertical space here.
        gbc.weighty = 1.0;
        dialogPanel.add(new JLabel(""), gbc);
        gbc.weighty = 0;

        // Sign In button and Sign Up link.
        hBox = Box.createHorizontalBox();
        changePassword = new JButton("Change Password");
        changePassword.addActionListener(this::onOk);
        changePassword.setEnabled(false);
        hBox.add(changePassword);
        hBox.add(Box.createHorizontalStrut(20));
        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(this::onCancel);
        hBox.add(cancel);
        hBox.add(Box.createHorizontalGlue());

        gbc.insets.bottom = 0; // no bottom spacing.
        dialogPanel.add(hBox, gbc);

        addComponentListener(componentAdapter);
    }

    /**
     * Gets the password and the state of "show password".
     * @return a Triple of password, allow show, do show.
     */
    Triple<String,Boolean,Boolean> getPassword() {
        return new ImmutableTriple<>(passwordField1.getText(),
            showPassword.isEnabled(), showPassword.isSelected());
    }


    void onCancel(ActionEvent actionEvent) {
        dialogController.cancel(this);
    }

    private void onOk(ActionEvent actionEvent) {
        // Unfortunately, cognito doesn't return any success/failure status on this call.
        dialogController.cognitoInterface.updatePassword(usernameField.getText(), passwordField1.getText(), resetCode.getText());
        dialogController.ok(this);
    }

    /**
     * Handles any actions that need to be taken when the panel is shown or hidden.
     */
    @Override
    void onShown() {
        String username = dialogController.getUsername();
        usernameField.setText(username);
        passwordField1.setText(null);
        passwordField2.setText(null);
        showPassword.setSelected(false);
        onShowPassword(null);
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


    private void onShowPassword(ActionEvent actionEvent) {
        passwordField1.setMaskChar(showPassword.isSelected() ? (char)0 : '*');
        passwordField2.setMaskChar(showPassword.isSelected() ? (char)0 : '*');
    }

}
