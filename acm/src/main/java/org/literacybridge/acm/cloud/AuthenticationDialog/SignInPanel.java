package org.literacybridge.acm.cloud.AuthenticationDialog;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.literacybridge.acm.cloud.ActionLabel;
import org.literacybridge.acm.cloud.Authenticator;
import org.literacybridge.acm.gui.Assistant.PlaceholderTextField;
import org.literacybridge.acm.gui.util.UIUtils;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import static org.literacybridge.acm.gui.Assistant.AssistantPage.getGBC;
import static org.literacybridge.acm.gui.util.UIUtils.UiOptions.TOP_THIRD;

public class SignInPanel extends DialogPanel {
    private static final String DIALOG_TITLE = "Amplio Sign In";

    private final JButton signIn;
    private final PlaceholderTextField usernameField;
    private final PlaceholderTextField passwordField;
    private final JCheckBox showPassword;
    private final JCheckBox rememberMe;

    public SignInPanel(DialogController dialogController) {
        super(dialogController, DIALOG_TITLE);
        JPanel dialogPanel = this;
        // The GUI
        dialogPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = getGBC();
        gbc.insets.bottom = 12; // tighter bottom spacing.

        // User name
        usernameField = new PlaceholderTextField();
        usernameField.setPlaceholder("User Name or Email Address");
        usernameField.addKeyListener(textKeyListener);
        usernameField.getDocument().addDocumentListener(textDocumentListener);
        dialogPanel.add(usernameField, gbc);

        // Password
        passwordField = new PlaceholderTextField();
        passwordField.setPlaceholder("Password");
        passwordField.setMaskChar('*');
        passwordField.addKeyListener(textKeyListener);
        passwordField.getDocument().addDocumentListener(textDocumentListener);
        dialogPanel.add(passwordField, gbc);

        // Option checkboxes, and forgot password link.
        Box hBox = Box.createHorizontalBox();
        Box vBox = Box.createVerticalBox();
        showPassword = new JCheckBox("Show password");
        showPassword.addActionListener(this::onShowPassword);
        vBox.add(showPassword);
        rememberMe = new JCheckBox("Remember me", true);
        vBox.add(rememberMe);
        hBox.add(vBox);
        hBox.add(Box.createHorizontalGlue());

        ActionLabel forgotPassword = new ActionLabel("Forgot password?");
        forgotPassword.addActionListener(this::onForgotPassword);

        hBox.add(forgotPassword);
        hBox.add(Box.createHorizontalStrut(10));

        dialogPanel.add(hBox, gbc);

        // Consume all vertical space here.
        gbc.weighty = 1.0;
        dialogPanel.add(new JLabel(""), gbc);
        gbc.weighty = 0;

        // Sign In button and Sign Up link.
        hBox = Box.createHorizontalBox();
        signIn = new JButton("Sign In");
        signIn.addActionListener(this::onSignin);
        signIn.setEnabled(false);
        hBox.add(signIn);
        hBox.add(Box.createHorizontalGlue());
        ActionLabel signUp = new ActionLabel("No user id? Click here!");
        signUp.addActionListener(this::onSignUp);

        hBox.add(signUp);
        hBox.add(Box.createHorizontalStrut(10));

        gbc.insets.bottom = 0; // no bottom spacing.
        dialogPanel.add(hBox, gbc);

        addComponentListener(componentAdapter);
    }

    public boolean isRememberMeSelected() {
        return rememberMe.isSelected();
    }

    String getUsername() {
        return usernameField.getText();
    }

    String getPassword() {
        return passwordField.getText();
    }

    void setUsername(String username) {
        usernameField.setText(username);
    }

    /**
     * Sets a password as it was entered on a "reset" panel or a "signup" panel. This also
     * includes the "show password" state.
     * @param pwd with "show password" state.
     */
    void setPassword(Triple<String,Boolean,Boolean> pwd) {
        passwordField.setText(pwd.getLeft());
        if (pwd.getMiddle()) {
            showPassword.setEnabled(true);
            showPassword.setSelected(pwd.getRight());
        } else {
            showPassword.setEnabled(false);
            showPassword.setSelected(false);
        }
        onShowPassword(null);
    }

    @Override
    void onEnter() {
        if (signIn.isEnabled()) onSignin(null);
    }

    /**
     * User clicked on the "No user id? Click here!" link.
     * @param actionEvent is ignored.
     */
    private void onSignUp(ActionEvent actionEvent) {
        dialogController.gotoSignUpCard();
    }

    /**
     * User clicked on the "Forgot password" link.
     * @param actionEvent is ignored.
     */
    private void onForgotPassword(ActionEvent actionEvent) {
        if (StringUtils.isEmpty(usernameField.getText())) {
            dialogController.setMessage("Please enter the user id or email for which to reset the password.");
            return;
        }
        dialogController.clearMessage();
        // Comment out next line to NOT reset the password, to test the GUI aspect of the reset dialog.
        Authenticator.getInstance().resetPassword(usernameField.getText());

        dialogController.gotoResetCard();
    }

    /**
     * Show or hide the password. This is disabled unless this user typed in the password.
     * @param actionEvent is ignored.
     */
    private void onShowPassword(ActionEvent actionEvent) {
        passwordField.setMaskChar(showPassword.isSelected() ? (char)0 : '*');
    }

    /**
     * User clicked "Sign in" pressed enter.
     * @param actionEvent is ignored.
     */
    private void onSignin(ActionEvent actionEvent) {
        Authenticator authenticator = Authenticator.getInstance();

        UIUtils.runWithWaitSpinner(dialogController,
            () -> authenticator.authenticate(usernameField.getText(), passwordField.getText()),
            this::onSigninReturned,
            TOP_THIRD);
    }

    /**
     * Called after the "authenticate" call returns.
     */
    private void onSigninReturned() {
        Authenticator authenticator = Authenticator.getInstance();
        if (!authenticator.isAuthenticated()) {
            dialogController.setMessage(authenticator.getAuthMessage());
        } else {
            dialogController.ok(this);
        }
    }

    /**
     * Sets the enabled state of controls, based on which other controls have contents.
     */
    private void enableControls() {
        signIn.setEnabled(usernameField.getText().length() > 0 && passwordField.getText().length() > 0);
        if (passwordField.getText().length() == 0) showPassword.setEnabled(true);
    }

    @SuppressWarnings("FieldCanBeLocal")
    private KeyListener textKeyListener = new KeyAdapter() {
        @Override
        public void keyTyped(KeyEvent e) {
            super.keyTyped(e);
            enableControls();
        }
    };

    /**
     * We don't enable "showPassword" for saved passwords, so when a saved password is used,
     * the control is disabled. If the old password is deleted, we re-enable the control.
     *
     * Also used to enable the sign-in button if a user id or password is pasted into the
     * corresponding field (because we're not listening to that key, we'd otherwise miss the
     * presence of the user id or password).
     */
    @SuppressWarnings("FieldCanBeLocal")
    private DocumentListener textDocumentListener = new DocumentListener() {
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

    /**
     * Sets the user name and password from previously saved credentials. The "show password"
     * field will be de-selected and disabled.
     * @param username to set.
     * @param password for the user name.
     */
    void setSavedCredentials(String username, String password) {
        rememberMe.setSelected(true);
        usernameField.setText(username);
        showPassword.setSelected(false);
        showPassword.setEnabled(false);
        passwordField.setMaskChar('*');
        passwordField.setText(password);
    }
}
