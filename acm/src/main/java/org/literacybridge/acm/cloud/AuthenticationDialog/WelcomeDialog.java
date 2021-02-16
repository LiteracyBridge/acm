package org.literacybridge.acm.cloud.AuthenticationDialog;

import org.apache.commons.lang3.StringUtils;
import org.literacybridge.acm.cloud.Authenticator;
import org.literacybridge.acm.gui.Assistant.RoundedLineBorder;
import org.literacybridge.acm.gui.util.UIUtils;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

import static org.literacybridge.acm.cloud.AuthenticationDialog.WelcomeDialog.Cards.ConfirmCard;
import static org.literacybridge.acm.cloud.AuthenticationDialog.WelcomeDialog.Cards.EmailCard;
import static org.literacybridge.acm.cloud.AuthenticationDialog.WelcomeDialog.Cards.ForgotPasswordCard;
import static org.literacybridge.acm.cloud.AuthenticationDialog.WelcomeDialog.Cards.ModCard;
import static org.literacybridge.acm.cloud.AuthenticationDialog.WelcomeDialog.Cards.ProgramCard;
import static org.literacybridge.acm.cloud.AuthenticationDialog.WelcomeDialog.Cards.ResetCard;
import static org.literacybridge.acm.cloud.AuthenticationDialog.WelcomeDialog.Cards.NewPasswordRequiredCard;
import static org.literacybridge.acm.cloud.AuthenticationDialog.WelcomeDialog.Cards.LoginCard;
import static org.literacybridge.acm.cloud.AuthenticationDialog.WelcomeDialog.Cards.SignUpCard;
import static org.literacybridge.acm.gui.util.UIUtils.UiOptions.TOP_THIRD;

public class WelcomeDialog extends JDialog {
    private String email;
    private String password;
    private boolean isSavedPassword;
    private String program;
    private boolean isSandboxSelected;

    private boolean success = false;

    public boolean isSuccess() {
        return success;
    }

    public String getEmail() {
        return email;
    }

    void setEmail(String email) {
        this.email = email;
    }

    /**
     * Returns email.
     * @return the best identity we have for the user.
     */
    public String getIdentity() {
        return this.email;
    }

    public String getProgram() {
        return program;
    }

    void setProgram(String program) {
        this.program = program;
    }

    void setPassword(String password) {
        this.password = password;
        this.isSavedPassword = false;
    }

    public boolean isSavedPassword() {
        return isSavedPassword;
    }

    public boolean isSandboxSelected() {
        return isSandboxSelected;
    }
    void setSandboxSelected(boolean sandboxSelected) {
        isSandboxSelected = sandboxSelected;
    }

    enum Cards {
        NullCard(100, CardContent::new),
        LoginCard(org.literacybridge.acm.cloud.AuthenticationDialog.LoginCard.CARD_HEIGHT, LoginCard::new),
        SignUpCard(RegisterCard.CARD_HEIGHT, RegisterCard::new),
        ForgotPasswordCard(org.literacybridge.acm.cloud.AuthenticationDialog.ForgotPasswordCard.CARD_HEIGHT, ForgotPasswordCard::new),
        ResetCard(org.literacybridge.acm.cloud.AuthenticationDialog.ResetCard.CARD_HEIGHT, ResetCard::new),
        NewPasswordRequiredCard(org.literacybridge.acm.cloud.AuthenticationDialog.NewPasswordRequiredCard.CARD_HEIGHT, NewPasswordRequiredCard::new),
        ConfirmCard(org.literacybridge.acm.cloud.AuthenticationDialog.ConfirmCard.CARD_HEIGHT, ConfirmCard::new),
        EmailCard(org.literacybridge.acm.cloud.AuthenticationDialog.EmailCard.CARD_HEIGHT, EmailCard::new),
        ProgramCard(org.literacybridge.acm.cloud.AuthenticationDialog.ProgramCard.CARD_HEIGHT, ProgramCard::new),
        ModCard(org.literacybridge.acm.cloud.AuthenticationDialog.ModCard.CARD_HEIGHT, ModCard::new);

        int minimumHeight;
        BiFunction<WelcomeDialog, Cards, CardContent> ctor;

        Cards(int minimumHeight, BiFunction<WelcomeDialog, Cards, CardContent> ctor) {
            this.minimumHeight = minimumHeight;
            this.ctor = ctor;
        }
    }

    private final CardLayout cardLayout;
    private final JPanel cardsContainer;

    private JLabel authMessage;
    private final Map<Cards, CardContent> cardMap = new HashMap<>();
    private CardContent currentCard;

    final Authenticator.CognitoInterface cognitoInterface;
    final Set<Authenticator.LoginOptions> options;
    final String applicationName;
    final String defaultProgram;

    /**
     * Helper to make a card and add it to the card panel and card map.
     * @param card enum Cards value specifying which card to make.
     */
    private void makeCard(Cards card) {
        CardContent newCard = card.ctor.apply(this, card);
        cardsContainer.add(card.name(), newCard);
        cardMap.put(card, newCard);
    }

    /**
     * The Welcome dialog. Prompts the user to login when online, or enter their email when
     * offline. If there are multiple programs available, prompts the user to choose the desired
     * program.
     * @param owner Owner window.
     * @param defaultProgram The program to be selected by default (if it is an available program).
     * @param options From Authenticator.LoginOptions
     * @param cognitoInterface a private interface inside Authenticator, by which the dialog can
     *                         query and set values in the Authenticator.
     */
    public WelcomeDialog(Window owner,
        String applicationName,
        String defaultProgram,
        Set<Authenticator.LoginOptions> options,
        Authenticator.CognitoInterface cognitoInterface) {
        super(owner, "Amplio Login", ModalityType.DOCUMENT_MODAL);
        this.cognitoInterface = cognitoInterface;
        this.options = options;
        this.applicationName = applicationName;
        this.defaultProgram = defaultProgram;

        // Set an empty border on the panel, to give some blank space around the content.
        setLayout(new BorderLayout());
        JPanel borderPanel = new JPanel();
        Border outerBorder = new EmptyBorder(12, 12, 12, 12);
        Border innerBorder = new RoundedLineBorder(Color.GRAY, 1, 6, 2);
        borderPanel.setBorder(new CompoundBorder(outerBorder, innerBorder));
        add(borderPanel, BorderLayout.CENTER);
        borderPanel.setLayout(new BorderLayout());
        borderPanel.setBackground(Color.white);

        cardLayout = new CardLayout();
        cardsContainer = new JPanel(cardLayout);
        borderPanel.add(cardsContainer, BorderLayout.CENTER);
        cardsContainer.setBorder(new EmptyBorder(6, 26, 6, 26));

        // Create the panels, add them to the cardPanel, and to the dialogPanelMap.
        if (cognitoInterface.isOnline()) {
            makeCard(LoginCard);
        } else {
            makeCard(EmailCard);
        }
        currentCard = new ArrayList<>(cardMap.values()).get(0);

        ActionListener escListener = e -> currentCard.onCancel(e);

        getRootPane().registerKeyboardAction(escListener,
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW);

        ActionListener enterListener = e -> currentCard.onEnter();

        getRootPane().registerKeyboardAction(enterListener,
            KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW);
        getRootPane().setDefaultButton(null);

        // Defer activating the card, so that post construction properties can be set.
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                dialogShown();
            }
        });

        // Center horizontally and in the top 2/3 of screen.
        setMinimumSize(new Dimension(450, currentCard.panel.minimumHeight));
        UIUtils.centerWindow(this, TOP_THIRD);
        setAlwaysOnTop(true);

        // For debugging sizing issues.
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                System.out.printf("Size %dx%d%n", WelcomeDialog.this.getWidth(), WelcomeDialog.this.getHeight());
            }
        });
    }

    private void dialogShown() {
        currentCard.onShown(null);
    }



    private LoginCard loginCard() {
        return ((LoginCard) cardMap.get(LoginCard));
    }

    public boolean isRememberMeSelected() {
        return loginCard().isRememberMeSelected();
    }

    /**
     * The Authenticator calls this to retrieve the password, to "remember me".
     * @return the password.
     */
    public String getPassword() {
        return password;
    }

    /**
     * The Authenticator calls this with any saved credentials, "remember me"
     * @param email The saved email.
     * @param password The saved password.
     */
    public void setSavedCredentials(String email, String password) {
        this.email = email;
        this.password = password;
        this.isSavedPassword = true;
    }

    /**
     * Switches to a card. If the card has not previously been shown, it will be created now.
     * @param newCard The enum Cards value to be shown.
     * @param actionEvent is passed through to the new card.
     */
    @SuppressWarnings("CommentedOutCode")
    private void activateCard(Cards newCard, ActionEvent actionEvent) {
        if (!cardMap.containsKey(newCard)) {
            makeCard(newCard);
        }
        int deltaFromNominal = getHeight()- currentCard.panel.minimumHeight;
        // For debugging card transitions.
//        System.out.printf("transition card %s -> %s, cur height: %d, cur min: %d, new min: %d, delta: %d\n",
//            currentCard.panel.name(), newCard.name(),
//            getHeight(),
//            currentCard.panel.minimumHeight,
//            newCard.minimumHeight,
//            deltaFromNominal);
        if (getHeight() != newCard.minimumHeight+deltaFromNominal) {
            setMinimumSize(new Dimension(getWidth(), newCard.minimumHeight));
            setSize(new Dimension(getWidth(), newCard.minimumHeight+deltaFromNominal));
        }
        currentCard = cardMap.get(newCard);
        clearMessage();
        cardLayout.show(cardsContainer, newCard.name());
        currentCard.onShown(actionEvent);
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
        // Surround with <html> tag so text will wrap if necessary.
        authMessage.setText(String.format("<html>%s</html>", message));
    }

    void clearMessage() {
        if (authMessage != null) {
            UIUtils.setLabelText(authMessage, "");
        }
    }

    /**
     * Navigate to the sign up card, in response to "No user id? Click here!"
     * @param actionEvent is passed through to the new card.
     */
    void gotoSignUpCard(ActionEvent actionEvent) {
        activateCard(SignUpCard, actionEvent);
    }

    /**
     * Navigates to the forgot password card, in response to "Forgot password?"
     */
    void gotoForgotPasswordCard() {
        activateCard(ForgotPasswordCard, null);
    }

    /**
     * Navigates to the new account confirmation card.
     */
    void gotoConfirmationCard() {
        activateCard(ConfirmCard, null);
    }

    /**
     * Navigates to the card to enter a new password. Should also have a reset code.
     */
    void gotoResetCard() {
        activateCard(ResetCard, null);
    }

    /**
     * Navigates to the "NewPassword" card. In response to a password change forced by server.
     */
    public void gotoNewPasswordRequiredCard() {
        activateCard(NewPasswordRequiredCard, null);
    }

    /**
     * Navigates to the program selection card, after logging in or entering email address.
     */
    void gotoProgramSelection() {
        if (options.contains(Authenticator.LoginOptions.CHOOSE_PROGRAM)) {
            activateCard(ProgramCard, null);
        } else {
            success = true;
            setVisible(false);
        }
    }

    /**
     * Called by the sub-panels when user clicks OK on the panel.
     * @param senderCard that clicked OK.
     */
    void ok(CardContent senderCard) {
        switch (senderCard.panel) {
        case LoginCard:
        case EmailCard:
        case NewPasswordRequiredCard:             
            gotoProgramSelection();
            break;

        case ForgotPasswordCard:
            activateCard(ResetCard, null);
            break;

        case ResetCard:
        case ConfirmCard:
            activateCard(LoginCard, null);
            break;

        case ProgramCard:
            // If there's a Message-of-the-Day, show it.
            if (StringUtils.isNotBlank(cognitoInterface.getAuthenticationAttribute("mod"))) {
                activateCard(ModCard, null);
                break;
            }
            // Fall through and close dialog.
        case ModCard:
            success = true;
            setVisible(false);
            break;
        }
    }

    /**
     * Called by the sub-panels when user cancels. The result may just be to
     * switch back to the sign-in panel, or may be to cancel.
     * @param senderCard that clicked cancel.
     */
    void cancel(CardContent senderCard) {
        switch (senderCard.panel) {
        case LoginCard:

        case EmailCard:
        case ProgramCard:
            setVisible(false);
            break;

        case ConfirmCard:
            activateCard(SignUpCard, null);
            break;

        case SignUpCard:
        case ForgotPasswordCard:
        case ResetCard:
            activateCard(LoginCard, null);
            break;
        }
    }

    /**
     * Called when the login card detects an SdkClientException. This is interpreted to mean
     * that we are offline.
     * @param senderCard The card that detected SdkClientException. Only the login card is valid.
     */
    void SdkClientException(CardContent senderCard) {
        if (senderCard.panel == LoginCard) {
            if (options.contains(Authenticator.LoginOptions.OFFLINE_EMAIL_CHOICE)) {
                activateCard(EmailCard, null);
            } else if (StringUtils.isNotBlank(email)) {
                activateCard(ProgramCard, null);
            } else {
                // Ends the dialog, with failure.
                setVisible(false);
            }
        }
    }
}
