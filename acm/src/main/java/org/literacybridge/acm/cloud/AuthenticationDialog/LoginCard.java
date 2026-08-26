package org.literacybridge.acm.cloud.AuthenticationDialog;

import org.literacybridge.acm.cloud.AuthenticationDialog.fx.LoginPane;
import org.literacybridge.acm.gui.fx.FxBridge;
import org.literacybridge.acm.gui.resourcebundle.LabelProvider;
import org.literacybridge.acm.gui.util.UIUtils;

import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;

import static org.literacybridge.acm.cloud.Authenticator.LoginOptions.NO_WAIT;
import static org.literacybridge.acm.gui.util.UIUtils.UiOptions.TOP_THIRD;

/**
 * The login card. The form itself is JavaFX ({@link LoginPane}); this class is the Swing shell
 * that hosts it and owns everything that talks to the rest of the dialog.
 *
 * <p>It is still a JPanel with the same public methods as the Swing version it replaced, so
 * {@code WelcomeDialog}, {@code CardContent} and {@code Authenticator} are unchanged.
 *
 * <p>Threading: constructor, {@code onShown} and {@code onEnter} arrive on the Swing EDT; the
 * {@link LoginPane.Handler} callbacks arrive on the JavaFX thread. Each hops to the other side
 * through {@link FxBridge} rather than touching foreign controls directly.
 */
public class LoginCard extends CardContent implements LoginPane.Handler {
    private static final String DIALOG_TITLE = "Login to %s";

    /**
     * Height of this card excluding the shared logo, which WelcomeDialog adds separately as
     * CardContent.logoHeight. The JavaFX form draws its own (circular) logo, so this covers the
     * whole FX scene; see the getScaledLogo() call in the constructor.
     */
    protected static final int CARD_HEIGHT = 470;

    /** Built on, and only ever touched from, the JavaFX thread. */
    private volatile LoginPane loginPane;

    /**
     * Mirror of the JavaFX checkbox. Authenticator reads isRememberMeSelected() from the EDT
     * after the dialog closes, where the FX control must not be touched, so the value is copied
     * across at the moment the user submits.
     */
    private volatile boolean rememberMeSelected = true;

    public LoginCard(WelcomeDialog welcomeDialog, WelcomeDialog.Cards panel) {
        super(welcomeDialog, String.format(DIALOG_TITLE, welcomeDialog.applicationName), panel);

        // WelcomeDialog sizes itself as (CARD_HEIGHT + CardContent.logoHeight), and logoHeight is
        // only populated as a side effect of loading the shared logo. We do not display that logo,
        // but the other (still Swing) cards rely on the value, so prime it here.
        getScaledLogo();

        JFXPanel host = FxBridge.createHostPanel();
        host.setBackground(Color.WHITE);
        setLayout(new BorderLayout());
        add(host, BorderLayout.CENTER);

        // Scene and controls are built on the JavaFX thread. Anything queued afterwards (onShown,
        // onEnter) runs on that same thread, so it is guaranteed to see a constructed loginPane.
        final String applicationName = welcomeDialog.applicationName;
        FxBridge.onFx(() -> {
            loginPane = new LoginPane(applicationName, this);
            host.setScene(new Scene(loginPane));
        });

        addComponentListener(componentAdapter);
    }

    // ---- Called by WelcomeDialog, on the EDT ----

    @Override
    void onShown(ActionEvent actionEvent) {
        super.onShown(actionEvent);
        final String email = welcomeDialog.getEmail();
        final String password = welcomeDialog.getPassword();
        final boolean savedPassword = welcomeDialog.isSavedPassword();
        final boolean noWait = welcomeDialog.options.contains(NO_WAIT);

        FxBridge.onFx(() -> {
            loginPane.setEmail(email);
            loginPane.setPassword(password);
            loginPane.setRevealPasswordEnabled(!savedPassword);
            loginPane.focusEmail();
            // If no_wait is specified, and we have everything we need, go!
            if (noWait) loginPane.triggerLogin();
        });
    }

    @Override
    void onEnter() {
        FxBridge.onFx(() -> loginPane.triggerLogin());
    }

    public boolean isRememberMeSelected() {
        return rememberMeSelected;
    }

    // ---- LoginPane.Handler, called on the JavaFX thread ----

    @Override
    public void onLogin(String email, String password) {
        // Read the checkbox while we are still on the FX thread.
        rememberMeSelected = loginPane.isRememberMeSelected();
        FxBridge.onSwing(() -> doLogin(email, password));
    }

    @Override
    public void onForgotPassword(String email) {
        FxBridge.onSwing(() -> {
            welcomeDialog.setEmail(email);
            welcomeDialog.clearMessage();
            welcomeDialog.gotoForgotPasswordCard();
        });
    }

    @Override
    public void onSignUp() {
        FxBridge.onSwing(() -> welcomeDialog.gotoSignUpCard(null));
    }

    // ---- Authentication, on the EDT ----

    private void doLogin(String email, String password) {
        welcomeDialog.clearMessage();
        String text = LabelProvider.getLabel("Logging In");
        UIUtils.runWithWaitSpinner(text, welcomeDialog,
            () -> welcomeDialog.cognitoInterface.authenticate(email, password),
            () -> onLoginReturned(email, password),
            TOP_THIRD);
    }

    /**
     * Called on the EDT after the "authenticate" call returns. Unchanged in behaviour from the
     * Swing version; the email and password are passed in rather than read back out of the form,
     * because the form now lives on another thread.
     */
    private void onLoginReturned(String email, String password) {
        // ok and cancel do the same thing, but one succeeded and one failed, so it is best to
        // keep them separate, in case this semantic changes in the future.
        if (welcomeDialog.cognitoInterface.isAuthenticated()) {
            // Authenticated with Cognito.
            if (rememberMeSelected) {
                welcomeDialog.setPassword(password);
            }
            ok();
        } else if (welcomeDialog.cognitoInterface.isPasswordResetRequired()) {
            // The password has been reset. Prompt user for new password.
            welcomeDialog.setEmail(email);
            welcomeDialog.gotoResetCard();
            welcomeDialog.setMessage("Your password has been reset. Please choose a new password.");
        } else if (welcomeDialog.cognitoInterface.isNotAuthorizedException()) {
            // Probably bad user / password. Inform user, let them try again.
            welcomeDialog.setMessage(welcomeDialog.cognitoInterface.getAuthMessage());
        } else if (welcomeDialog.cognitoInterface.isSdkClientException()) {
            // No connectivity. Can't login with Cognito.
            welcomeDialog.SdkClientException(this);
        } else if (welcomeDialog.cognitoInterface.isNewPasswordRequired()) {
            // Server requires user to reset password.
            welcomeDialog.setEmail(email);
            welcomeDialog.gotoNewPasswordRequiredCard();
        } else {
            // Probably bad user / password. Inform user, let them try again.
            welcomeDialog.setMessage(welcomeDialog.cognitoInterface.getAuthMessage());
        }
    }
}
