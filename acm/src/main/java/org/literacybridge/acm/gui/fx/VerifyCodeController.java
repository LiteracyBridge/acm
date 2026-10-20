package org.literacybridge.acm.gui.fx;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;

import java.util.List;

public class VerifyCodeController {
    @FXML private TextField code1, code2, code3, code4, code5, code6;
    @FXML private Label errorLabel;
    @FXML private Button verifyButton;
    @FXML private ProgressIndicator spinner;

    private List<TextField> boxes;

    @FXML
    private void initialize() {
        boxes = List.of(code1, code2, code3, code4, code5, code6);

        for (int i = 0; i < boxes.size(); i++) {
            final int idx = i;
            TextField tf = boxes.get(i);

            tf.textProperty().addListener((obs, old, val) -> {
                String digits = val.replaceAll("\\D", "");
                if (digits.length() >= 6) {            // pasted full code
                    fill(digits);
                } else if (digits.length() > 1) {      // typed over existing digit
                    tf.setText(digits.substring(digits.length() - 1));
                } else if (!digits.equals(val)) {      // non-digit typed
                    tf.setText(digits);
                } else if (digits.length() == 1 && idx < 5) {
                    boxes.get(idx + 1).requestFocus();
                }
            });

            tf.setOnKeyPressed(e -> {
                if (e.getCode() == KeyCode.BACK_SPACE && tf.getText().isEmpty() && idx > 0) {
                    TextField prev = boxes.get(idx - 1);
                    prev.clear();
                    prev.requestFocus();
                }
            });
        }
        Platform.runLater(code1::requestFocus);
    }

    private void fill(String digits) {
        for (int i = 0; i < 6; i++) {
            boxes.get(i).setText(String.valueOf(digits.charAt(i)));
        }
        boxes.get(5).requestFocus();
    }

    private String getCode() {
        StringBuilder sb = new StringBuilder();
        boxes.forEach(b -> sb.append(b.getText()));
        return sb.toString();
    }

    @FXML
    private void handleVerify() {
        String code = getCode();
        if (code.length() < 6) {
            errorLabel.setText("Enter the 6-digit code.");
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
            return;
        }
        // TODO: verify code with backend, on success show ResetPasswordView
    }

    @FXML
    private void handleResend() {
        // TODO: resend code
    }
}