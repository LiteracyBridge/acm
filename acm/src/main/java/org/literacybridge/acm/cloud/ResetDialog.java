package org.literacybridge.acm.cloud;

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
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;

import static org.literacybridge.acm.gui.Assistant.AssistantPage.getGBC;
import static org.literacybridge.acm.gui.util.UIUtils.UiOptions.SHIFT_DOWN;
import static org.literacybridge.acm.gui.util.UIUtils.UiOptions.TOP_THIRD;

/**
 * A dialog class to assist in resetting a cognito based password. User is prompted to enter
 * their new password, typing it twice, and the reset code from the server, sent via email.
 */
public class ResetDialog extends JDialog {

    private final PlaceholderTextField usernameField;
    private final PlaceholderTextField passwordField1;
    private final PlaceholderTextField passwordField2;
    private final JCheckBox showPassword;
    private final JLabel mismatchWarning;
    private final PlaceholderTextField resetCode;
    private final JButton changePassword;

    private boolean didOk = false;

    private ResetDialog(Window owner, String username) {
        super(owner, "Reset Password", ModalityType.DOCUMENT_MODAL);

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
        usernameField = new PlaceholderTextField(username);
        usernameField.setEnabled(false);
//        usernameField.setPlaceholder("User Name or Email Address");
        usernameField.getDocument().addDocumentListener(passwordDocListener);
        dialogPanel.add(usernameField, gbc);

        // Password
        gbc.insets.bottom = 5; // tighter bottom spacing.
        passwordField1 = new PlaceholderTextField();
        passwordField1.setPlaceholder("New Password");
        passwordField1.setMaskChar('*');
        passwordField1.getDocument().addDocumentListener(passwordDocListener);
        dialogPanel.add(passwordField1, gbc);

        // Password, again
        passwordField2 = new PlaceholderTextField();
        passwordField2.setPlaceholder("Repeat password");
        passwordField2.setMaskChar('*');
        passwordField2.getDocument().addDocumentListener(passwordDocListener);
        dialogPanel.add(passwordField2, gbc);

        // Option checkboxes, and Password mismatch warning.
        Box hBox = Box.createHorizontalBox();
        showPassword = new JCheckBox("Show password");
        showPassword.addActionListener(this::onShowPassword);
        hBox.add(showPassword);
        hBox.add(Box.createHorizontalStrut(10));

        mismatchWarning = new JLabel("Passwords don't match.");
        mismatchWarning.setForeground(Color.RED);
        Font font = mismatchWarning.getFont();
        font = new Font(font.getName(), font.getStyle()|Font.ITALIC, font.getSize());
        mismatchWarning.setFont(font);
        mismatchWarning.setVisible(false);
        hBox.add(mismatchWarning);
        hBox.add(Box.createHorizontalGlue());
        gbc.insets.bottom = 12; // tighter bottom spacing.
        dialogPanel.add(hBox, gbc);

        // Reset code from server
        resetCode = new PlaceholderTextField();
        resetCode.setPlaceholder("Enter reset code");
        resetCode.getDocument().addDocumentListener(passwordDocListener);
        dialogPanel.add(resetCode, gbc);

        // Consume all vertical space here.
        gbc.weighty = 1.0;
        dialogPanel.add(new JLabel(""), gbc);
        gbc.weighty = 0;

        // Sign In button and Sign Up link.
        hBox = Box.createHorizontalBox();
        changePassword = new JButton("Change Password");
        changePassword.addActionListener(this::onOk);
        changePassword.setEnabled(false);
        hBox.add(changePassword);
        hBox.add(Box.createHorizontalStrut(20));
        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(this::onCancel);
        hBox.add(cancel);
        hBox.add(Box.createHorizontalGlue());

        gbc.insets.bottom = 0; // no bottom spacing.
        dialogPanel.add(hBox, gbc);

        SwingUtils.addEscapeListener(this);
        setMinimumSize(new Dimension(450, 280));

        // Center horizontally and in the top 2/3 of screen.
        UIUtils.centerWindow(this, TOP_THIRD, SHIFT_DOWN);
    }

    private void onCancel(ActionEvent actionEvent) {
        this.setVisible(false);
    }

    private void onOk(ActionEvent actionEvent) {
        // Unfortunately, cognito doesn't return any success/failure status on this call.
        Authenticator.getInstance().updatePassword(usernameField.getText(), passwordField1.getText(), resetCode.getText());
        didOk = true;
        this.setVisible(false);
    }

    /**
     * Retrieve the new password.
     * @return the new password, if the user clicked OK. Null otherwise.
     */
    String getNewPassword() {
        return didOk ? passwordField1.getText() : null;
    }

    /**
     * As the user types into various text boxes, sets the mismatch warning and enables/disables
     * the "Change" button as appropriate.
     */
    @SuppressWarnings("FieldCanBeLocal")
    private DocumentListener passwordDocListener = new DocumentListener() {
        private void check() {
            String name = usernameField.getText();
            String p1 = passwordField1.getText();
            String p2 = passwordField2.getText();
            String pin = resetCode.getText();
            mismatchWarning.setVisible(p1.length() > 0 && p2.length() > 0 && !p1.equals(p2));
            changePassword.setEnabled(name.length() > 0 && p1.length() > 0 && p1.equals(p2) && pin.length() > 5);
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


    private void onShowPassword(ActionEvent actionEvent) {
        passwordField1.setMaskChar(showPassword.isSelected() ? (char)0 : '*');
        passwordField2.setMaskChar(showPassword.isSelected() ? (char)0 : '*');
    }

    public static String showDialog(Window parent, String username) {
        ResetDialog dialog = new ResetDialog(parent, username);
        dialog.setVisible(true);
        return dialog.getNewPassword();
    }
}
