package org.literacybridge.acm.cloud.AuthenticationDialog;

import org.literacybridge.acm.cloud.ActionLabel;
import org.literacybridge.acm.gui.Assistant.FlexTextField;
import org.literacybridge.acm.gui.Assistant.GBC;
import org.literacybridge.acm.gui.Assistant.PanelButton;
import org.literacybridge.acm.gui.resourcebundle.LabelProvider;
import org.literacybridge.acm.gui.util.UIUtils;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
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

public class LoginCard extends CardContent {
    private static final String DIALOG_TITLE = "Login to %s";
    protected static final int CARD_HEIGHT = 275;

    private final PanelButton login;
    private final FlexTextField emailField;
    private final FlexTextField passwordField;
    private final JCheckBox rememberMe;

    public LoginCard(WelcomeDialog welcomeDialog, WelcomeDialog.Cards panel) {
        super(welcomeDialog, String.format(DIALOG_TITLE, welcomeDialog.applicationName), panel);
        JPanel dialogPanel = this;
        // The GUI
        dialogPanel.setLayout(new GridBagLayout());
        GBC gbc = new GBC(getGBC()).withAnchor(CENTER);
        gbc.insets.bottom = 12; // tighter bottom spacing.

        // Amplio logo
        addScaledLogo();

        // Email
        emailField = new FlexTextField();
        emailField.setFont(getTextFont());
        emailField.setIcon(getPersonIcon());
        emailField.setPlaceholder("Enter your email address");
        emailField.addKeyListener(textKeyListener);
        emailField.getDocument().addDocumentListener(textDocumentListener);
        dialogPanel.add(emailField, gbc);

        // Password
        passwordField = new FlexTextField();
        passwordField.setFont(getTextFont());
        passwordField.setIsPassword(true);
        passwordField.setPlaceholder("Enter your password");
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

        // Login button.
        login = new PanelButton("Login");
        login.setFont(getTextFont());
        login.setBgColorPalette(AMPLIO_GREEN);
        login.addActionListener(this::onLogin);
        login.setEnabled(false);
        dialogPanel.add(login, gbc.withFill(NONE));

        // Sign-up link.
        ActionLabel signUp = new ActionLabel("Not registered yet? Click here!");
        signUp.addActionListener(this::onSignUp);
        dialogPanel.add(signUp, gbc.withFill(NONE));

        addComponentListener(componentAdapter);
    }

    @Override
    void onShown(ActionEvent actionEvent) {
        super.onShown(actionEvent);
        emailField.setText(welcomeDialog.getEmail());
        passwordField.setText(welcomeDialog.getPassword());
        passwordField.setPasswordRevealed(false);
        passwordField.setText(welcomeDialog.getPassword());
        passwordField.setRevealPasswordEnabled(!welcomeDialog.isSavedPassword());
        emailField.setRequestFocusEnabled(true);
        emailField.requestFocusInWindow();
    }

    public boolean isRememberMeSelected() {
        return rememberMe.isSelected();
    }

    @Override
    void onEnter() {
        if (login.isEnabled()) onLogin(null);
    }

    /**
     * User clicked on the "No user id? Click here!" link.
     * @param actionEvent is ignored.
     */
    private void onSignUp(ActionEvent actionEvent) {
        
        welcomeDialog.gotoSignUpCard(actionEvent);
    }

    /**
     * User clicked on the "Forgot password" link.
     * @param actionEvent is ignored.
     */
    private void onForgotPassword(ActionEvent actionEvent) {
        welcomeDialog.setEmail(emailField.getText());
        welcomeDialog.clearMessage();
        welcomeDialog.gotoForgotPasswordCard();
    }

    /**
     * User clicked "Login" or pressed enter.
     * @param actionEvent is ignored.
     */
    private void onLogin(ActionEvent actionEvent) {
        welcomeDialog.clearMessage();
        String text = LabelProvider.getLabel("Attempting Login");
        UIUtils.runWithWaitSpinner(text, welcomeDialog,
            () -> welcomeDialog.cognitoInterface.authenticate(emailField.getText(), passwordField.getText()),
            this::onLoginReturned,
            TOP_THIRD);
    }

    /**
     * Called after the "authenticate" call returns.
     */
    private void onLoginReturned() {
        // ok and cancel do the same thing, but one succeeded and one failed, so it is best to
        // keep them separate, in case this semantic changes in the future.
        if (welcomeDialog.cognitoInterface.isAuthenticated()) {
            // Authenticated with Cognito.
            if (rememberMe.isSelected()) {
                welcomeDialog.setPassword(passwordField.getText());
            }
            ok();
        } else if(welcomeDialog.cognitoInterface.isPasswordResetRequired()) {
            // The password has been reset. Prompt user for new password.
            welcomeDialog.setEmail(emailField.getText());
            welcomeDialog.gotoResetCard();
            welcomeDialog.setMessage("Your password has been reset. Please choose a new password.");
        } else if(welcomeDialog.cognitoInterface.isNotAuthorizedException()) {
            // Probably bad user / password. Inform user, let them try again.
            welcomeDialog.setMessage(welcomeDialog.cognitoInterface.getAuthMessage());
        } else if(welcomeDialog.cognitoInterface.isSdkClientException()) {
            // No connectivity. Can't login with Cognito.
            welcomeDialog.SdkClientException(this);
        } else if(welcomeDialog.cognitoInterface.isNewPasswordRequired()) {
            // Server requires user to reset password.
            welcomeDialog.setEmail(emailField.getText());
            welcomeDialog.gotoNewPasswordRequiredCard();
        } else {
            // Probably bad user / password. Inform user, let them try again.
            welcomeDialog.setMessage(welcomeDialog.cognitoInterface.getAuthMessage());
        }
    }

    /**
     * Sets the enabled state of controls, based on which other controls have contents.
     */
    private void enableControls() {
        boolean enableLogin = (
            emailField.getText().length() > 0 && passwordField.getText().length() > 0);
        login.setEnabled(enableLogin);
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
