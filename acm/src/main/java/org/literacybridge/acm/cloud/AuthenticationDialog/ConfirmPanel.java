package org.literacybridge.acm.cloud.AuthenticationDialog;

import org.literacybridge.acm.cloud.Authenticator;
import org.literacybridge.acm.gui.Assistant.PlaceholderTextField;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import static org.literacybridge.acm.gui.Assistant.AssistantPage.getGBC;

public class ConfirmPanel extends DialogPanel {
    private static final String DIALOG_TITLE = "Create User ID";

    private String username;
    private final PlaceholderTextField confirmationField;
    private final JButton confirm;

    public ConfirmPanel(DialogController dialogController) {
        super(dialogController, DIALOG_TITLE);
        JPanel dialogPanel = this;

        // The GUI
        dialogPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = getGBC();
        gbc.insets.bottom = 12; // tighter bottom spacing.

        // User name
        confirmationField = new PlaceholderTextField();
        confirmationField.setPlaceholder("Confirmation code");
        confirmationField.getDocument().addDocumentListener(passwordDocListener);
        dialogPanel.add(confirmationField, gbc);

        // Option checkboxes, and Password mismatch warning.
        Box hBox;

        // Consume all vertical space here.
        gbc.weighty = 1.0;
        dialogPanel.add(new JLabel(""), gbc);
        gbc.weighty = 0;

        // Sign In button and Sign Up link.
        hBox = Box.createHorizontalBox();
        confirm = new JButton("Confirm");
        confirm.addActionListener(this::onOk);
        confirm.setEnabled(false);
        hBox.add(confirm);

        hBox.add(Box.createHorizontalStrut(20));
        JButton resend = new JButton("Resend Code");
        resend.addActionListener(this::onResend);
        resend.setEnabled(true);
        hBox.add(resend);

        hBox.add(Box.createHorizontalStrut(20));
        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(this::onCancel);
        hBox.add(cancel);
        hBox.add(Box.createHorizontalGlue());

        gbc.insets.bottom = 0; // no bottom spacing.
        dialogPanel.add(hBox, gbc);

        addComponentListener(componentAdapter);
    }

    private void onOk(ActionEvent actionEvent) {
        // Unfortunately, cognito doesn't return any success/failure status on this call.
        @SuppressWarnings("unused")
        String result = Authenticator.getInstance()
            .verifyAccessCode(username, confirmationField.getText());
        dialogController.ok(this);
    }

    private void onResend(@SuppressWarnings("unused") ActionEvent actionEvent) {
        Authenticator.getInstance().resendAccessCode(username);
    }

    void onCancel(ActionEvent actionEvent) {
        dialogController.cancel(this);
    }

    /**
     * Handles any actions that need to be taken when the panel is shown or hidden.
     */
    @Override
    void onShown() {
        ConfirmPanel.this.username = dialogController.getNewUsername();
        confirmationField.setText(null);
    }


    /**
     * As the user types into various text boxes, sets the mismatch warning and enables/disables
     * the "Change" button as appropriate.
     */
    @SuppressWarnings("FieldCanBeLocal")
    private DocumentListener passwordDocListener = new DocumentListener() {
        private void check() {
            String code = confirmationField.getText();
            confirm.setEnabled(code.length() > 0);
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
