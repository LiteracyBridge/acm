package org.literacybridge.acm.gui.fx;

import javafx.fxml.FXML;
import javafx.scene.control.*;

public class ResetPasswordController {
    @FXML private PasswordField newPasswordField, confirmPasswordField;
    @FXML private Label errorLabel;
    @FXML private Button resetButton;
    @FXML private ProgressIndicator spinner;

    @FXML
    private void handleReset() {
        String pw = newPasswordField.getText();
        String confirm = confirmPasswordField.getText();

        if (pw.length() < 8) {
            showError("Password must be at least 8 characters.");
        } else if (!pw.equals(confirm)) {
            showError("Passwords do not match.");
        } else {
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
            // TODO: send new password to backend, then go back to login
        }
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }
}