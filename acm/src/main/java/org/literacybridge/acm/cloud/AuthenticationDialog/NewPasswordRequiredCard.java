package org.literacybridge.acm.cloud.AuthenticationDialog;

import org.literacybridge.acm.gui.Assistant.FlexTextField;
import org.literacybridge.acm.gui.Assistant.GBC;
import org.literacybridge.acm.gui.Assistant.PanelButton;
import org.literacybridge.acm.gui.Assistant.RoundedLineBorder;

import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.Color;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;

import static java.awt.GridBagConstraints.CENTER;
import static org.literacybridge.acm.gui.Assistant.AssistantPage.getGBC;

public class NewPasswordRequiredCard extends CardContent {
    private final static String DIALOG_TITLE = "New Password Required";
    protected static final int CARD_HEIGHT = 520;

    private final JLabel emailField;
    private final FlexTextField passwordField;
    private final PanelButton changePassword;

    public NewPasswordRequiredCard(WelcomeDialog welcomeDialog, WelcomeDialog.Cards panel) {
        super(welcomeDialog, DIALOG_TITLE, panel);
        JPanel dialogPanel = this;

        // The GUI
        dialogPanel.setLayout(new GridBagLayout());
        GBC gbc = new GBC(getGBC()).setAnchor(CENTER);
        gbc.insets.bottom = 12; // tighter bottom spacing.

        // Amplio logo
        JLabel logoLabel = new JLabel(getScaledLogo());
        dialogPanel.add(logoLabel, gbc);

        dialogPanel.add(new JLabel("<html>A new password is required for your account.\nPlease choose a new password, then click Change Password."), gbc);

        // User name
        emailField = new JLabel();
        int iconsize = 16;
        ImageIcon person = new ImageIcon(getPersonIcon().getImage().getScaledInstance(iconsize, iconsize, Image.SCALE_SMOOTH));
        emailField.setIcon(person);
        Border nameBorder = new CompoundBorder(new RoundedLineBorder(Color.DARK_GRAY, 1, 8), new EmptyBorder(3,7,2,2));
        emailField.setBorder(nameBorder);
        dialogPanel.add(emailField, gbc);

        // Password
        gbc.insets.bottom = 5; // tighter bottom spacing.
        passwordField = new FlexTextField();
        passwordField.setFont(getTextFont());
        passwordField.setPlaceholder("Your chosen new password");
        passwordField.setIsPassword(true).setRevealPasswordEnabled(true);
        passwordField.getDocument().addDocumentListener(passwordDocListener);
        gbc.insets.bottom = 4; // tighter bottom spacing.
        dialogPanel.add(passwordField, gbc);
        gbc.insets.bottom = 12;

        JLabel rules = new JLabel(PASSWORD_RULES_FORMATTED);
        dialogPanel.add(rules, gbc);

        // Consume all vertical space here.
        dialogPanel.add(new JLabel(""), gbc.withWeighty(1.0));

        // Buttons
        Box hBox = Box.createHorizontalBox();
        hBox.add(Box.createHorizontalGlue());
        changePassword = new PanelButton("Change Password");
        changePassword.setFont(getTextFont());
        changePassword.setBgColorPalette(AMPLIO_GREEN);
        changePassword.addActionListener(this::onOk);
        changePassword.setEnabled(false);
        hBox.add(changePassword);
        hBox.add(Box.createHorizontalStrut(20));
        PanelButton cancel = new PanelButton("Cancel");
        cancel.setFont(getTextFont());
        cancel.setBgColorPalette(AMPLIO_GREEN);
        cancel.addActionListener(this::onCancel);
        hBox.add(cancel);
        hBox.add(Box.createHorizontalGlue());

        dialogPanel.add(hBox, gbc);

        addComponentListener(componentAdapter);
    }

    void onCancel(ActionEvent actionEvent) {
        cancel();
    }

    private void onOk(ActionEvent actionEvent) {
        welcomeDialog.cognitoInterface.provideNewPassword(emailField.getText(), passwordField.getText());
        welcomeDialog.setPassword(passwordField.getText());
        ok();
    }

    /**
     * Handles any actions that need to be taken when the panel is shown or hidden.
     */
    @Override
    void onShown() {
        super.onShown();
        emailField.setText(welcomeDialog.getIdentity());
        passwordField.setText(null);
        passwordField.setRevealPasswordEnabled(true).setPasswordRevealed(false);
        passwordField.setRequestFocusEnabled(true);
        passwordField.requestFocusInWindow();
    }

   /**
     * As the user types into various text boxes, sets the mismatch warning and enables/disables
     * the "Change" button as appropriate.
     */
    @SuppressWarnings("FieldCanBeLocal")
    private final DocumentListener passwordDocListener = new DocumentListener() {
        private void check() {
            String name = emailField.getText();
            boolean pValid = PASSWORD_PATTERN.matcher(passwordField.getText()).matches();
            changePassword.setEnabled(name.length() > 0 && pValid);
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
