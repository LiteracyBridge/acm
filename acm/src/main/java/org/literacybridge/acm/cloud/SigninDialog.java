package org.literacybridge.acm.cloud;

import org.apache.commons.lang3.StringUtils;
import org.literacybridge.acm.gui.Assistant.PlaceholderTextField;
import org.literacybridge.acm.gui.Assistant.RoundedLineBorder;
import org.literacybridge.acm.gui.util.UIUtils;
import org.literacybridge.acm.utils.SwingUtils;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import static org.literacybridge.acm.gui.Assistant.AssistantPage.getGBC;
import static org.literacybridge.acm.gui.util.UIUtils.UiOptions.TOP_THIRD;

public final class SigninDialog extends JDialog {

    private final JCheckBox showPassword;
    private final PlaceholderTextField passwordField;
    private final JCheckBox rememberMe;
    private final PlaceholderTextField usernameField;
    private JLabel authFailureMessage;
    private final JButton signIn;

    private boolean autoSignIn = false;

    SigninDialog(Window owner, String prompt) {
        super(owner, String.format("%s Sign In", prompt), ModalityType.DOCUMENT_MODAL);

        // Set an empty border on the panel, to give some blank space around the content.
        setLayout(new BorderLayout());

        JPanel borderPanel = new JPanel();
        Border outerBorder = new EmptyBorder(12, 12, 12, 12);
        Border innerBorder = new RoundedLineBorder(Color.GRAY, 1, 6, 2);
        borderPanel.setBorder(new CompoundBorder(outerBorder, innerBorder));
        add(borderPanel, BorderLayout.CENTER);
        borderPanel.setLayout(new BorderLayout());

        JPanel dialogPanel = new JPanel();
        borderPanel.add(dialogPanel, BorderLayout.CENTER);
        dialogPanel.setBorder(new EmptyBorder(6, 6, 6, 6));

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
        rememberMe.addActionListener(this::onRememberMe);
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

        SwingUtils.addEscapeListener(this);

        ActionListener enterListener = (e) -> {
            System.out.println("Enter");
            if (signIn.isEnabled())
                onSignin(null);
        };

        getRootPane().registerKeyboardAction(enterListener,
            KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW);
        getRootPane().setDefaultButton(null);

        // Center horizontally and in the top 2/3 of screen.
        setMinimumSize(new Dimension(450, 250));
        UIUtils.centerWindow(this, TOP_THIRD);
        setAlwaysOnTop(true);
    }

    /**
     * Pre-populate the dialog with saved user id and password. The "show password" feature is
     * disabled for pre-populated passwords.
     * @param user to pre-populate
     * @param password to pre-populate
     */
    void setSavedCredentials(String user, String password) {
        rememberMe.setSelected(true);
        usernameField.setText(user);
        showPassword.setSelected(false);
        showPassword.setEnabled(false);
        passwordField.setMaskChar('*');
        passwordField.setText(password);
    }

    /**
     * Makes the signin dialog visible. If autoSignIn is set, and there is a user name and a
     * password, also tries to actually perform the sign-in operation.
     */
    void doSignin() {
        String user = usernameField.getText();
        String password = passwordField.getText();
        if (autoSignIn && StringUtils.isNotEmpty(user) && StringUtils.isNotEmpty(password)) {
            onSignin(null);
        }
        setVisible(true);
    }

    /**
     * Displays a message to the user when the sign-in fails.
     * @param message to be shown.
     */
    private void setMessage(String message) {
        if (authFailureMessage == null) {
            authFailureMessage = new JLabel();
            authFailureMessage.setBorder(new EmptyBorder(5,10,10, 5));
            add(authFailureMessage, BorderLayout.SOUTH);
        }
        authFailureMessage.setText(message);
    }

    /**
     * When we implement "create user id", that code will go here.
     * @param actionEvent is unused.
     */
    private void onSignUp(ActionEvent actionEvent) {
        JOptionPane.showMessageDialog(this, "Please use the Amplio Dashboard to create a new User Id.",
            "Create User Id", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Called from the "forgot password" link. 
     * @param actionEvent is unused.
     */
    private void onForgotPassword(ActionEvent actionEvent) {
        if (StringUtils.isEmpty(usernameField.getText())) {
            setMessage("Please enter the user id or email for which to reset the password.");
            return;
        }
        setMessage(null);
        // Comment out next line to NOT reset the password, to test the GUI aspect of the reset dialog.
        Authenticator.getInstance().resetPassword(usernameField.getText());
        String newPw = ResetDialog.showDialog(this, usernameField.getText());
        if (newPw != null) {
            // User just entered a new password. Let them view it if desired.
            showPassword.setEnabled(true);
            passwordField.setText(newPw);
        }
    }

    private void onRememberMe(ActionEvent actionEvent) {

    }

    boolean isRememberMeSelected() {
        return rememberMe.isSelected();
    }

    private void onShowPassword(ActionEvent actionEvent) {
        passwordField.setMaskChar(showPassword.isSelected() ? (char)0 : '*');
    }

    String getPasswordText() {
        return passwordField.getText();
    }

    /**
     * If the user made any net changes, commit them.
     *
     * @param e is unused.
     */
    private void onSignin(ActionEvent e) {
        Authenticator authenticator = Authenticator.getInstance();

        UIUtils.runWithWaitSpinner(this,
            () -> authenticator.authenticate(usernameField.getText(), passwordField.getText()),
            this::onSigninReturned,
            TOP_THIRD);
    }

    private void onSigninReturned() {
        Authenticator authenticator = Authenticator.getInstance();
        if (!authenticator.isAuthenticated()) {
            setMessage(authenticator.getAuthMessage());
        } else {
            setVisible(false);
        }
    }

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

}
