package org.literacybridge.acm.cloud.AuthenticationDialog;

import org.literacybridge.acm.gui.Assistant.FlexTextField;
import org.literacybridge.acm.gui.Assistant.GBC;
import org.literacybridge.acm.gui.Assistant.PanelButton;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;

import static org.literacybridge.acm.gui.Assistant.AssistantPage.getGBC;

public class ConfirmCard extends CardContent {
    private static final String DIALOG_TITLE = "Confirm User ID";

    private final FlexTextField confirmationField;
    private final PanelButton confirm;

    public ConfirmCard(WelcomeDialog welcomeDialog, WelcomeDialog.Cards panel) {
        super(welcomeDialog, DIALOG_TITLE, panel);
        JPanel dialogPanel = this;

        // The GUI
        dialogPanel.setLayout(new GridBagLayout());
        GBC gbc = new GBC(getGBC());
        gbc.insets.bottom = 12; // tighter bottom spacing.

        // Amplio logo
        JLabel logoLabel = new JLabel(getScaledLogo());
        dialogPanel.add(logoLabel, gbc);

        // User name
        confirmationField = new FlexTextField();
        confirmationField.setFont(getTextFont());
        confirmationField.setPlaceholder("Confirmation code");
        confirmationField.getDocument().addDocumentListener(codeDocListener);
        dialogPanel.add(confirmationField, gbc);

        // Consume all vertical space here.
        dialogPanel.add(new JLabel(""), gbc.withWeighty(1.0));

        // Buttons.
        double xPad = 1.25; // Squeeze the buttons horizontally, so they'll fit.
        double yPad = 1.5;  // Make the buttons a little shorter. Looks a little better.
        Box hBox = Box.createHorizontalBox();
        confirm = new PanelButton("Confirm");
        confirm.setFont(getTextFont());
        confirm.setPadding(xPad, yPad);
        confirm.setBgColorPalette(AMPLIO_GREEN);
        confirm.addActionListener(this::onOk);
        confirm.setEnabled(false);
        hBox.add(confirm);

        hBox.add(Box.createHorizontalStrut(20));
        PanelButton resend = new PanelButton("Resend Code");
        resend.setFont(getTextFont());
        resend.setPadding(xPad, yPad);
        resend.setBgColorPalette(AMPLIO_GREEN);
        resend.addActionListener(this::onResend);
        resend.setEnabled(true);
        hBox.add(resend);

        hBox.add(Box.createHorizontalStrut(20));
        PanelButton cancel = new PanelButton("Cancel");
        cancel.setFont(getTextFont());
        cancel.setPadding(xPad, yPad);
        cancel.setBgColorPalette(AMPLIO_GREEN);
        cancel.addActionListener(this::onCancel);
        hBox.add(cancel);
        hBox.add(Box.createHorizontalGlue());

        dialogPanel.add(hBox, gbc);

        addComponentListener(componentAdapter);
    }

    private void onOk(ActionEvent actionEvent) {
        // Unfortunately, cognito doesn't return any success/failure status on this call.
        welcomeDialog.cognitoInterface.verifyAccessCode(welcomeDialog.getUsername(), confirmationField.getText());
        ok();
    }

    private void onResend(@SuppressWarnings("unused") ActionEvent actionEvent) {
        welcomeDialog.cognitoInterface.resendAccessCode(welcomeDialog.getUsername());
    }

    void onCancel(ActionEvent actionEvent) {
        cancel();
    }

    /**
     * Handles any actions that need to be taken when the panel is shown or hidden.
     */
    @Override
    void onShown() {
        confirmationField.setText(null);
        confirmationField.setRequestFocusEnabled(true);
        confirmationField.requestFocusInWindow();
    }


    /**
     * As the user types into various text boxes, sets the mismatch warning and enables/disables
     * the "Change" button as appropriate.
     */
    @SuppressWarnings("FieldCanBeLocal")
    private final DocumentListener codeDocListener = new DocumentListener() {
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
