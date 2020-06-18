package org.literacybridge.acm.cloud.AuthenticationDialog;

import org.literacybridge.acm.cloud.ActionLabel;
import org.literacybridge.acm.gui.Assistant.FlexTextField;
import org.literacybridge.acm.gui.Assistant.GBC;
import org.literacybridge.acm.gui.Assistant.PanelButton;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;

import static java.awt.GridBagConstraints.CENTER;
import static java.awt.GridBagConstraints.NONE;
import static org.literacybridge.acm.gui.Assistant.AssistantPage.getGBC;

public class ForgotPasswordCard extends CardContent {
    private final static String DIALOG_TITLE = "Forgot Password";
    protected static final int CARD_HEIGHT = 515;

    private final FlexTextField usernameField;
    private final PanelButton resetPassword;

    public ForgotPasswordCard(WelcomeDialog welcomeDialog, WelcomeDialog.Cards panel) {
        super(welcomeDialog, DIALOG_TITLE, panel);
        JPanel dialogPanel = this;

        // The GUI
        dialogPanel.setLayout(new GridBagLayout());
        GBC gbc = new GBC(getGBC()).setAnchor(CENTER);
        gbc.insets.bottom = 12; // tighter bottom spacing.

        // Amplio logo
        JLabel logoLabel = new JLabel(getScaledLogo());
        dialogPanel.add(logoLabel, gbc);

        dialogPanel.add(new JLabel("<html>Enter your email address and click 'Send password reset email'. On the next panel, enter the code which you will receive in your email."), gbc);

        // User name
        usernameField = new FlexTextField();
        usernameField.setFont(getTextFont());
        usernameField.setIcon(getPersonIcon());
        usernameField.setEnabled(true);
        usernameField.setPlaceholder("Enter your email address");
        usernameField.getDocument().addDocumentListener(passwordDocListener);
        dialogPanel.add(usernameField, gbc);

        gbc.insets.bottom = 12;

        // Consume all vertical space here.
        dialogPanel.add(new JLabel(""), gbc.withWeighty(1.0));

        // Reset button
        resetPassword = new PanelButton("Send password reset email");
        resetPassword.setFont(getTextFont());
        resetPassword.setBgColorPalette(AMPLIO_GREEN);
        resetPassword.addActionListener(this::onOk);
        resetPassword.setEnabled(false);
        dialogPanel.add(resetPassword, gbc.withFill(NONE));

        Box hBox = Box.createHorizontalBox();
        hBox.add(new JLabel("Already have login and password? "));
        ActionLabel signIn = new ActionLabel("Sign in");
        signIn.addActionListener(this::onCancel);
        hBox.add(signIn);
        dialogPanel.add(hBox, gbc.withFill(NONE));

        addComponentListener(componentAdapter);
    }

    void onCancel(ActionEvent actionEvent) {
        cancel();
    }

    private void onOk(ActionEvent actionEvent) {
        welcomeDialog.setUsername(usernameField.getText());
        welcomeDialog.clearMessage();
        // Comment out next line to NOT reset the password, to test the GUI aspect of the reset dialog.
        welcomeDialog.cognitoInterface.resetPassword(usernameField.getText());

        ok();
    }

    /**
     * Handles any actions that need to be taken when the panel is shown or hidden.
     */
    @Override
    void onShown() {
        super.onShown();
        usernameField.setText(welcomeDialog.getEmail());
    }

   /**
     * As the user types into various text boxes, sets the mismatch warning and enables/disables
     * the "Change" button as appropriate.
     */
    @SuppressWarnings("FieldCanBeLocal")
    private final DocumentListener passwordDocListener = new DocumentListener() {
        private void check() {
            String name = usernameField.getText();
            resetPassword.setEnabled(name.length() > 0);
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
