package org.literacybridge.acm.cloud.AuthenticationDialog;

import org.apache.commons.lang3.StringUtils;
import org.literacybridge.acm.gui.Assistant.GBC;
import org.literacybridge.acm.gui.Assistant.PanelButton;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;

import static java.awt.GridBagConstraints.BOTH;
import static java.awt.GridBagConstraints.CENTER;
import static java.awt.GridBagConstraints.NONE;
import static org.literacybridge.acm.gui.Assistant.AssistantPage.getGBC;

public class ModCard extends CardContent {
    private static final String DIALOG_TITLE = "Message of the Day";
    protected static final int CARD_HEIGHT = 315;

    public ModCard(WelcomeDialog welcomeDialog,
            WelcomeDialog.Cards panel) {
        super(welcomeDialog, DIALOG_TITLE, panel);
        JPanel dialogPanel = this;

        // The GUI
        dialogPanel.setLayout(new GridBagLayout());
        GBC gbc = new GBC(getGBC()).withAnchor(CENTER);
        gbc.insets.bottom = 12; // tighter bottom spacing.

        // Amplio logo
        addScaledLogo();

        JLabel promptLabel = new JLabel("<html>A message from Amplio.</html>");
        dialogPanel.add(promptLabel, gbc);

        JTextArea messageArea = new JTextArea();
        dialogPanel.add(messageArea, gbc.withWeighty(1.0).withFill(BOTH));
        String message = welcomeDialog.cognitoInterface.getAuthenticationAttribute("mod");
        messageArea.setText(message);

        String buttonText = welcomeDialog.cognitoInterface.getAuthenticationAttribute("modButton");
        if (StringUtils.isBlank(buttonText)) {
            buttonText = "OK";
        }
        PanelButton okButton = new PanelButton(buttonText);
        okButton.setFont(getTextFont());
        okButton.setBgColorPalette(AMPLIO_GREEN);
        okButton.addActionListener(e -> onOk());
        dialogPanel.add(okButton, gbc.withFill(NONE));

        addComponentListener(componentAdapter);
    }

    @Override
    void onEnter() {
        onOk();
    }

    void onOk() {
        ok();
    }

}
