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
import static org.literacybridge.acm.cloud.AuthenticationDialog.WelcomeDialog.Cards.ProgramCard;
import static org.literacybridge.acm.cloud.AuthenticationDialog.WelcomeDialog.Cards.ResetCard;
import static org.literacybridge.acm.cloud.AuthenticationDialog.WelcomeDialog.Cards.SignInCard;
import static org.literacybridge.acm.cloud.AuthenticationDialog.WelcomeDialog.Cards.SignUpCard;
import static org.literacybridge.acm.gui.util.UIUtils.UiOptions.TOP_THIRD;

public class WelcomeDialog extends JDialog {
    private String username;
    private String email;
    private String password;
    private boolean isSavedPassword;
    private String program;
    private boolean isSandboxSelected;

    private boolean success = false;

    public boolean isSuccess() {
        return success;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public String getProgram() {
        return program;
    }

    void setEmail(String email) {
        this.email = email;
    }

    void setPassword(String password) {
        this.password = password;
        this.isSavedPassword = false;
    }

    void setProgram(String program) {
        this.program = program;
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
        SignInCard(555, SignInCard::new),
        SignUpCard(550, SignUpCard::new),
        ResetCard(555, ResetCard::new),
        ConfirmCard(410, ConfirmCard::new),
        EmailCard(415, EmailCard::new),
        ProgramCard(580, ProgramCard::new);

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
    final Set<Authenticator.SigninOptions> options;
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
     * The Welcome dialog. Prompts the user to sign in when online, or enter their email when
     * offline. If there are multiple programs available, prompts the user to choose the desired
     * program.
     * @param owner Owner window.
     * @param defaultProgram The program to be selected by default (if it is an available program).
     * @param options From Authenticator.SignInOptions
     * @param cognitoInterface a private interface inside Authenticator, by which the dialog can
     *                         query and set values in the Authenticator.
     */
    public WelcomeDialog(Window owner,
        String defaultProgram,
        Set<Authenticator.SigninOptions> options,
        Authenticator.CognitoInterface cognitoInterface) {
        super(owner, "Amplio Sign In", ModalityType.DOCUMENT_MODAL);
        this.cognitoInterface = cognitoInterface;
        this.options = options;
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
            makeCard(SignInCard);
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
//        addComponentListener(new ComponentAdapter() {
//            @Override
//            public void componentResized(ComponentEvent e) {
//                System.out.println(String.format("Size %dx%d", WelcomeDialog.this.getWidth(), WelcomeDialog.this.getHeight()));
//            }
//        });
    }

    private void dialogShown() {
        currentCard.onShown();
    }



    private org.literacybridge.acm.cloud.AuthenticationDialog.SignInCard signInCard() {
        return ((org.literacybridge.acm.cloud.AuthenticationDialog.SignInCard) cardMap.get(SignInCard));
    }

    public boolean isRememberMeSelected() {
        return signInCard().isRememberMeSelected();
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
     * @param username The saved user name.
     * @param email The saved email associated with the saved user name.
     * @param password The saved password.
     */
    public void setSavedCredentials(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.isSavedPassword = true;
    }

    /**
     * Switches to a card. If the card has not previously been shown, it will be created now.
     * @param newCard The enum Cards value to be shown.
     */
    private void activateCard(Cards newCard) {
        if (!cardMap.containsKey(newCard)) {
            makeCard(newCard);
        }
        int deltaFromNominal = getHeight()- currentCard.panel.minimumHeight;
        System.out.printf("transition card %s -> %s, cur height: %d, cur min: %d, new min: %d, delta: %d\n",
            currentCard.panel.name(), newCard.name(),
            getHeight(),
            currentCard.panel.minimumHeight,
            newCard.minimumHeight,
            deltaFromNominal);
        if (getHeight() != newCard.minimumHeight+deltaFromNominal) {
            setMinimumSize(new Dimension(getWidth(), newCard.minimumHeight));
            setSize(new Dimension(getWidth(), newCard.minimumHeight+deltaFromNominal));
        }
        currentCard = cardMap.get(newCard);
        clearMessage();
        cardLayout.show(cardsContainer, newCard.name());
        currentCard.onShown();
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

    /**
     * Navigate to the sign up card, in response to "No user id? Click here!"
     */
    void gotoSignUpCard() {
        activateCard(SignUpCard);
    }

    /**
     * Navigates to the password reset card, in response to "Forgot password?"
     */
    void gotoResetCard() {
        activateCard(ResetCard);
    }

    /**
     * Navigates to the new account confirmation card.
     */
    void gotoConfirmationCard() {
        activateCard(ConfirmCard);
    }

    /**
     * Navigates to the program selection card, after signing in or entering email address.
     */
    void gotoProgramSelection() {
        if (options.contains(Authenticator.SigninOptions.CHOOSE_PROGRAM)) {
            activateCard(ProgramCard);
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
        case SignInCard:
        case EmailCard:
            gotoProgramSelection();
            break;

        case ResetCard:
        case ConfirmCard:
            activateCard(SignInCard);
            break;

        case ProgramCard:
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
        case SignInCard:

        case EmailCard:
        case ProgramCard:
            setVisible(false);
            break;

        case ConfirmCard:
            activateCard(SignUpCard);
            break;

        case SignUpCard:
        case ResetCard:
            activateCard(SignInCard);
            break;
        }
    }

    /**
     * Called when the signin card detects an SdkClientException. This is interpreted to mean
     * that we are offline.
     * @param senderCard The card that detected SdkClientException. Only the sign in card is valid.
     */
    void SdkClientException(CardContent senderCard) {
        if (senderCard.panel == SignInCard) {
            if (options.contains(Authenticator.SigninOptions.OFFLINE_EMAIL_CHOICE)) {
                activateCard(EmailCard);
            } else if (StringUtils.isNotBlank(email)) {
                activateCard(ProgramCard);
            } else {
                // Ends the dialog, with failure.
                setVisible(false);
            }
        }
    }
}
