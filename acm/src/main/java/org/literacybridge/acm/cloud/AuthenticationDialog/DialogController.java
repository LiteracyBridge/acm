package org.literacybridge.acm.cloud.AuthenticationDialog;

import org.apache.commons.lang3.tuple.Triple;
import org.literacybridge.acm.cloud.Authenticator;
import org.literacybridge.acm.gui.Assistant.RoundedLineBorder;
import org.literacybridge.acm.gui.util.UIUtils;
import org.literacybridge.acm.utils.SwingUtils;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;

import static org.literacybridge.acm.gui.util.UIUtils.UiOptions.TOP_THIRD;

public class DialogController extends JDialog {
    public static final String SIGNIN_CARD = "signin";
    public static final String SIGNUP_CARD = "signup";
    public static final String RESET_CARD = "reset";
    public static final String CONFIRM_CARD = "confirm";

    private final CardLayout cardLayout;
    private SignInPanel signInPanel;
    private SignUpPanel signUpPanel;
    private ResetPanel resetPanel;
    private ConfirmPanel confirmPanel;
    private final JPanel cardPanel;

    private JLabel authMessage;
    private Map<String,DialogPanel> dialogPanelMap = new HashMap<>();
    private DialogPanel currentPanel = null;

    Authenticator.CognitoInterface cognitoInterface;

    public DialogController(Window owner, Authenticator.CognitoInterface cognitoInterface) {
        super(owner, "Amplio Sign In", ModalityType.DOCUMENT_MODAL);
        this.cognitoInterface = cognitoInterface;

        // Set an empty border on the panel, to give some blank space around the content.
        setLayout(new BorderLayout());

        JPanel borderPanel = new JPanel();
        Border outerBorder = new EmptyBorder(12, 12, 12, 12);
        Border innerBorder = new RoundedLineBorder(Color.GRAY, 1, 6, 2);
        borderPanel.setBorder(new CompoundBorder(outerBorder, innerBorder));
        add(borderPanel, BorderLayout.CENTER);
        borderPanel.setLayout(new BorderLayout());

        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        borderPanel.add(cardPanel, BorderLayout.CENTER);
        cardPanel.setBorder(new EmptyBorder(6, 6, 6, 6));

        signInPanel = new SignInPanel(this);
        signUpPanel = new SignUpPanel(this);
        resetPanel = new ResetPanel(this);
        confirmPanel = new ConfirmPanel(this);
        cardPanel.add(SIGNIN_CARD, signInPanel);
        dialogPanelMap.put(SIGNIN_CARD, signInPanel);
        cardPanel.add(SIGNUP_CARD, signUpPanel);
        dialogPanelMap.put(SIGNUP_CARD, signUpPanel);
        cardPanel.add(RESET_CARD, resetPanel);
        dialogPanelMap.put(RESET_CARD, resetPanel);
        cardPanel.add(CONFIRM_CARD, confirmPanel);
        dialogPanelMap.put(CONFIRM_CARD, confirmPanel);

        ActionListener escListener = e -> currentPanel.onCancel(e);

        getRootPane().registerKeyboardAction(escListener,
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW);

        ActionListener enterListener = e -> currentPanel.onEnter();

        getRootPane().registerKeyboardAction(enterListener,
            KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW);
        getRootPane().setDefaultButton(null);


        activateCard(SIGNIN_CARD);

        // Center horizontally and in the top 2/3 of screen.
        setMinimumSize(new Dimension(450, 250));
        UIUtils.centerWindow(this, TOP_THIRD);
        setAlwaysOnTop(true);

    }

    public boolean isRememberMeSelected() {
        return signInPanel.isRememberMeSelected();
    }

    public String getUsername() {
        return signInPanel.getUsername();
    }

    String getNewUsername() {
        return signUpPanel.getUsername();
    }

    public String getPassword() {
        return signInPanel.getPassword();
    }

    public void setSavedCredentials(String username, String password) {
        signInPanel.setSavedCredentials(username, password);
    }

    private void activateCard(String card) {
        currentPanel = dialogPanelMap.get(card);
        cardLayout.show(cardPanel, card);
    }

    /**
     * Displays a message to the user when the sign-in fails.
     * @param message to be shown.
     */
    void setMessage(String message) {
        if (authMessage == null) {
            authMessage = new JLabel();
            authMessage.setBorder(new EmptyBorder(5,10,10, 5));
            add(authMessage, BorderLayout.SOUTH);
        }
        authMessage.setText(message);
    }

    void clearMessage() {
        if (authMessage != null) {
            UIUtils.setLabelText(authMessage, "");
        }
    }

    void gotoSignUpCard() {
        if (getHeight() < 320) {
            setMinimumSize(new Dimension(getWidth(), 320));
        }
        activateCard(SIGNUP_CARD);
    }

    void gotoResetCard() {
        if (getHeight() < 280) {
            setMinimumSize(new Dimension(getWidth(), 280));
        }
        activateCard(RESET_CARD);
    }

    void gotoConfirmationCard() {
        activateCard(CONFIRM_CARD);
    }

    /**
     * Called by the sub-panels when user clicks OK on the panel.
     * @param panel that clicked OK.
     */
    void ok(JPanel panel) {
        if (panel == signInPanel) {
            setVisible(false);
        } else if (panel == resetPanel) {
            Triple<String,Boolean,Boolean> pwd = resetPanel.getPassword();
            signInPanel.setPassword(pwd);
            activateCard(SIGNIN_CARD);
        } else if (panel == confirmPanel) {
            Triple<String,Boolean,Boolean> pwd = signUpPanel.getPassword();
            signInPanel.setPassword(pwd);
            String username = signUpPanel.getUsername();
            signInPanel.setUsername(username);
            activateCard(SIGNIN_CARD);
        }
    }

    /**
     * Called by the sub-panels when user cancels. The result may just be to
     * switch back to the sign-in panel, or may be to cancel.
     * @param panel that clicked cancel.
     */
    void cancel(JPanel panel) {
        if (panel == signInPanel) {
            setVisible(false);
        }
        activateCard(SIGNIN_CARD);
    }
}
