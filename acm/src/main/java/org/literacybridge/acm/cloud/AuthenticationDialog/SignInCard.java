package org.literacybridge.acm.cloud.AuthenticationDialog;

import org.apache.commons.lang3.StringUtils;
import org.literacybridge.acm.cloud.ActionLabel;
import org.literacybridge.acm.gui.Assistant.FlexTextField;
import org.literacybridge.acm.gui.Assistant.GBC;
import org.literacybridge.acm.gui.Assistant.PanelButton;
import org.literacybridge.acm.gui.util.UIUtils;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import static java.awt.GridBagConstraints.CENTER;
import static java.awt.GridBagConstraints.EAST;
import static java.awt.GridBagConstraints.NONE;
import static org.literacybridge.acm.gui.Assistant.AssistantPage.getGBC;
import static org.literacybridge.acm.gui.util.UIUtils.UiOptions.TOP_THIRD;

public class SignInCard extends CardContent {
    private static final String DIALOG_TITLE = "Amplio Sign In";

    private final PanelButton signIn;
    private final FlexTextField usernameField;
    private final FlexTextField passwordField;
    private final JCheckBox rememberMe;

    public SignInCard(WelcomeDialog welcomeDialog, WelcomeDialog.Cards panel) {
        super(welcomeDialog, DIALOG_TITLE, panel);
        JPanel dialogPanel = this;
        // The GUI
        dialogPanel.setLayout(new GridBagLayout());
        GBC gbc = new GBC(getGBC()).withAnchor(CENTER);
        gbc.insets.bottom = 12; // tighter bottom spacing.

        // Amplio logo
        JLabel logoLabel = new JLabel(getScaledLogo());
        dialogPanel.add(logoLabel, gbc);

        // User name
        usernameField = new FlexTextField();
        usernameField.setFont(getTextFont());
        usernameField.setIcon(getPersonIcon());
        usernameField.setPlaceholder("User Name or Email Address");
        usernameField.addKeyListener(textKeyListener);
        usernameField.getDocument().addDocumentListener(textDocumentListener);
        dialogPanel.add(usernameField, gbc);

        // Password
        passwordField = new FlexTextField();
        passwordField.setFont(getTextFont());
        passwordField.setIsPassword(true);
        passwordField.setPlaceholder("Password");
        passwordField.addKeyListener(textKeyListener);
        passwordField.getDocument().addDocumentListener(textDocumentListener);
        dialogPanel.add(passwordField, gbc);

        // Option checkboxes, and forgot password link.
        rememberMe = new JCheckBox("Remember me", true);

        ActionLabel forgotPassword = new ActionLabel("Forgot your password?");
        forgotPassword.addActionListener(this::onForgotPassword);
        dialogPanel.add(forgotPassword, gbc.withAnchor(EAST).withFill(NONE));

        // Consume all vertical space here.
        dialogPanel.add(new JLabel(""), gbc.withWeighty(1.0));

        // Sign In button.
        signIn = new PanelButton("Sign In");
        signIn.setFont(getTextFont());
        signIn.setBgColorPalette(AMPLIO_GREEN);
        signIn.addActionListener(this::onSignin);
        signIn.setEnabled(false);
        dialogPanel.add(signIn, gbc.withFill(NONE));

        // Sign-up link.
        ActionLabel signUp = new ActionLabel("No user id? Click here!");
        signUp.addActionListener(this::onSignUp);
        dialogPanel.add(signUp, gbc.withFill(NONE));

        addComponentListener(componentAdapter);
    }

    @Override
    void onShown() {
        super.onShown();
        usernameField.setText(welcomeDialog.getUsername());
        passwordField.setText(welcomeDialog.getPassword());
        passwordField.setPasswordRevealed(false);
        passwordField.setText(welcomeDialog.getPassword());
        passwordField.setRevealPasswordEnabled(!welcomeDialog.isSavedPassword());
        usernameField.setRequestFocusEnabled(true);
        usernameField.requestFocusInWindow();
    }

    public boolean isRememberMeSelected() {
        return rememberMe.isSelected();
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
        welcomeDialog.gotoSignUpCard();
    }

    /**
     * User clicked on the "Forgot password" link.
     * @param actionEvent is ignored.
     */
    private void onForgotPassword(ActionEvent actionEvent) {
        if (StringUtils.isEmpty(usernameField.getText())) {
            welcomeDialog.setMessage("Please enter the user id or email for which to reset the password.");
            return;
        }
        welcomeDialog.setUsername(usernameField.getText());
        welcomeDialog.clearMessage();
        // Comment out next line to NOT reset the password, to test the GUI aspect of the reset dialog.
        welcomeDialog.cognitoInterface.resetPassword(usernameField.getText());

        welcomeDialog.gotoResetCard();
    }

    /**
     * User clicked "Sign in" pressed enter.
     * @param actionEvent is ignored.
     */
    private void onSignin(ActionEvent actionEvent) {
        UIUtils.runWithWaitSpinner(welcomeDialog,
            () -> welcomeDialog.cognitoInterface.authenticate(usernameField.getText(), passwordField.getText()),
            this::onSigninReturned,
            TOP_THIRD);
    }

    /**
     * Called after the "authenticate" call returns.
     */
    private void onSigninReturned() {
        // ok and cancel do the same thing, but one succeeded and one failed, so it is best to
        // keep them separate, in case this semantic changes in the future.
        if (welcomeDialog.cognitoInterface.isAuthenticated()) {
            // Authenticated with Cognito.
            if (rememberMe.isSelected()) {
                welcomeDialog.setPassword(passwordField.getText());
            }
            ok();
        } else if(welcomeDialog.cognitoInterface.isSdkClientException()) {
            // No connectivity. Can't sign in with Cognito.
            welcomeDialog.SdkClientException(this);
        } else {
            // Probably bad user / password. Inform user, let them try again.
            welcomeDialog.setMessage(welcomeDialog.cognitoInterface.getAuthMessage());
        }
    }

    /**
     * Sets the enabled state of controls, based on which other controls have contents.
     */
    private void enableControls() {
        boolean enableSignIn = (usernameField.getText().length() > 0 && passwordField.getText().length() > 0);
        signIn.setEnabled(enableSignIn);
        if (passwordField.getText().length() == 0) {
            passwordField.setRevealPasswordEnabled(true);
        }
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
     * We don't enable "showPassword" for saved passwords, so when a saved password is used,
     * the control is disabled. If the old password is deleted, we re-enable the control.
     *
     * Also used to enable the sign-in button if a user id or password is pasted into the
     * corresponding field (because we're not listening to that key, we'd otherwise miss the
     * presence of the user id or password).
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
