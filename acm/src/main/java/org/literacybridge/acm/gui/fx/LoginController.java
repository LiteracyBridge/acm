package org.literacybridge.acm.gui.fx;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;

public class LoginController {

    public interface Handler {
        void onLogin(String email, String password);
        void onForgotPassword(String email);
        void onSignUp();
    }

    @FXML
    private TextField emailField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private TextField visiblePasswordField;
    @FXML
    private ToggleButton passwordVisibilityButton;
    @FXML
    private Button loginButton;
    @FXML
    private Hyperlink signUpLink;
    @FXML
    private CheckBox rememberMeCheckBox;
    @FXML
    private Label messageLabel;

    private Runnable onLogin;
    private Handler handler;
    private ChangeListener<String> savedPasswordListener;

    public void setEmail(String email) {
        emailField.setText(email == null ? "" : email);
    }

    public void setPassword(String password) {
        passwordField.setText(password == null ? "" : password);
    }

    public void setRevealPasswordEnabled(boolean enabled) {
        passwordVisibilityButton.setSelected(false);
        passwordVisibilityButton.setDisable(!enabled);
        if (savedPasswordListener != null) {
            passwordField.textProperty().removeListener(savedPasswordListener);
            savedPasswordListener = null;
        }
        if (!enabled) {
            savedPasswordListener = new ChangeListener<String>() {
                @Override
                public void changed(ObservableValue<? extends String> observable,
                                    String oldValue, String newValue) {
                    if (newValue == null || newValue.isEmpty()) {
                        passwordVisibilityButton.setDisable(false);
                        passwordField.textProperty().removeListener(this);
                        savedPasswordListener = null;
                    }
                }
            };
            passwordField.textProperty().addListener(savedPasswordListener);
        }
    }

    public void focusEmail() {
        emailField.requestFocus();
    }

    public void triggerLogin() {
        handleLogin();
    }

    // Kept for the standalone login preview.
    public void setOnLogin(Runnable onLogin) {
        this.onLogin = onLogin;
    }

    // Set by the application host when this screen is used in the login flow.
    public void setHandler(Handler handler) {
        this.handler = handler;
    }

    @FXML
    private void handleLogin() {
        String email = getEmail();
        String password = getPassword();

        if (email.isEmpty() || password.isEmpty()) {
            messageLabel.setText("Please fill out all the fields");
            return;
        }

        messageLabel.setText("");
        if (handler != null) {
            handler.onLogin(email, password);
        } else if (onLogin != null) {
            onLogin.run();
        }
    }

    @FXML
    private void handleForgotPassword() {
        if (handler != null) {
            handler.onForgotPassword(getEmail());
        }
    }

    @FXML
    private void handleSignUp() {
        if (handler != null) {
            handler.onSignUp();
        }
    }

    @FXML
    private void initialize() {
        // Keep the masked and visible password fields synchronized.
        visiblePasswordField.textProperty().bindBidirectional(passwordField.textProperty());
        visiblePasswordField.visibleProperty().bind(passwordVisibilityButton.selectedProperty());
        visiblePasswordField.managedProperty().bind(visiblePasswordField.visibleProperty());
        passwordField.visibleProperty().bind(passwordVisibilityButton.selectedProperty().not());
        passwordField.managedProperty().bind(passwordField.visibleProperty());

        loginButton.disableProperty().bind(
                emailField.textProperty().isEmpty()
                        .or(passwordField.textProperty().isEmpty())
        );
    }

    @FXML
    private void handlePasswordVisibility() {
        // Move focus to the active field and keep the caret at the end.
        if (passwordVisibilityButton.isSelected()) {
            visiblePasswordField.requestFocus();
            visiblePasswordField.positionCaret(visiblePasswordField.getText().length());
        } else {
            passwordField.requestFocus();
            passwordField.positionCaret(passwordField.getText().length());
        }
    }

    public String getEmail() {
        return emailField.getText().trim();
    }

    public String getPassword() {
        return passwordField.getText();
    }

    public boolean isRememberMeSelected() {
        return rememberMeCheckBox.isSelected();
    }
}