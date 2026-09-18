package org.literacybridge.acm.cloud.AuthenticationDialog;

import org.literacybridge.acm.cloud.AuthenticationDialog.fx.LoginPane;
import org.literacybridge.acm.gui.fx.FxBridge;
import org.literacybridge.acm.gui.resourcebundle.LabelProvider;
import org.literacybridge.acm.gui.util.UIUtils;

import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import javax.swing.SwingUtilities;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import static org.literacybridge.acm.cloud.Authenticator.LoginOptions.NO_WAIT;
import static org.literacybridge.acm.gui.util.UIUtils.UiOptions.TOP_THIRD;

/**
 * The login card. The form itself is JavaFX ({@link LoginPane}); this class is the Swing shell
 * that positions it and owns everything that talks to the rest of the dialog.
 *
 * <p>Unlike the JFXPanel-based version this replaced, the JavaFX form is NOT embedded inside a
 * Swing component. {@code javafx.swing} (JFXPanel/SwingNode) unconditionally requires the
 * {@code jdk.unsupported.desktop} JDK module (JDK-8210092 removed the old implementation that
 * didn't), and some deployed JRE builds don't ship it. To avoid that dependency, this card shows
 * the JavaFX form as a separate, undecorated, always-on-top {@link Stage} that is manually kept
 * positioned directly over this (otherwise invisible) placeholder panel, tracking the host
 * dialog's position, size, and visibility.
 *
 * <p>It is still a JPanel with the same public methods as the JFXPanel version it replaced, so
 * {@code WelcomeDialog}, {@code CardContent} and {@code Authenticator} are unchanged.
 *
 * <p>Threading: constructor, {@code onShown} and {@code onEnter} arrive on the Swing EDT; the
 * {@link LoginPane.Handler} callbacks arrive on the JavaFX thread. Each hops to the other side
 * through {@link FxBridge} rather than touching foreign controls directly.
 */
public class LoginCard extends CardContent implements LoginPane.Handler {
    private static final String DIALOG_TITLE = "Login to %s";

    protected static final int CARD_HEIGHT = 470;

    /** Built on, and only ever touched from, the JavaFX thread. */
    private volatile LoginPane loginPane;

    /** The overlay window hosting {@link #loginPane}. Only ever touched from the FX thread. */
    private volatile Stage stage;

    private volatile boolean rememberMeSelected = true;

    /** The dialog (or other top-level window) this card is showing inside. */
    private Window hostWindow;

    public LoginCard(WelcomeDialog welcomeDialog, WelcomeDialog.Cards panel) {
        super(welcomeDialog, String.format(DIALOG_TITLE, welcomeDialog.applicationName), panel);

        getScaledLogo();

        // Empty placeholder: reserves the same layout space the JFXPanel host used to, so
        // WelcomeDialog's existing sizing/layout logic is unaffected. It is never painted --
        // the overlay Stage sits directly on top of it and does all the drawing.
        setOpaque(false);
        setPreferredSize(new Dimension(0, CARD_HEIGHT));

        final String applicationName = welcomeDialog.applicationName;
        FxBridge.startToolkit();
        FxBridge.onFx(() -> {
            loginPane = new LoginPane(applicationName, this);
            stage = new Stage(StageStyle.TRANSPARENT);
            stage.setScene(new Scene(loginPane, Color.TRANSPARENT));
            // No AWT Window can "own" an FX Stage, so there is no built-in way to keep this
            // layered above WelcomeDialog the way an owned window would be. Always-on-top is a
            // blunt substitute for that -- note it floats above *all* windows, not just this
            // app's, while visible.
            stage.setAlwaysOnTop(true);
        });

        addComponentListener(cardVisibilityAdapter);
    }

    @Override
    public void addNotify() {
        super.addNotify();
        hostWindow = SwingUtilities.getWindowAncestor(this);
        if (hostWindow != null) {
            hostWindow.addComponentListener(hostWindowAdapter);
            hostWindow.addWindowListener(hostWindowStateAdapter);
        }
    }

    @Override
    public void removeNotify() {
        if (hostWindow != null) {
            hostWindow.removeComponentListener(hostWindowAdapter);
            hostWindow.removeWindowListener(hostWindowStateAdapter);
            hostWindow = null;
        }
        hideOverlay();
        super.removeNotify();
    }

    // ---- Overlay positioning. All Swing-side; hops to FX thread for the actual Stage calls. ----

    private void syncOverlayBounds() {
        if (!isShowing()) return;
        Point loc = getLocationOnScreen();
        int w = getWidth();
        int h = getHeight();
        FxBridge.onFx(() -> {
            if (stage == null) return;
            stage.setX(loc.x);
            stage.setY(loc.y);
            stage.setWidth(w);
            stage.setHeight(h);
        });
    }

    private void showOverlay() {
        syncOverlayBounds();
        FxBridge.onFx(() -> {
            if (stage != null) stage.show();
        });
    }

    private void hideOverlay() {
        FxBridge.onFx(() -> {
            if (stage != null) stage.hide();
        });
    }

    /**
     * Standard java.awt.CardLayout shows/hides cards by calling setVisible() on each child, so
     * this fires exactly when WelcomeDialog switches to or away from this card -- no separate
     * hook into WelcomeDialog/CardContent is needed. If WelcomeDialog does its own card
     * switching some other way (not java.awt.CardLayout), this assumption doesn't hold and
     * these calls will need to move to wherever cards are actually swapped.
     */
    private final ComponentAdapter cardVisibilityAdapter = new ComponentAdapter() {
        @Override public void componentShown(ComponentEvent e) { showOverlay(); }
        @Override public void componentHidden(ComponentEvent e) { hideOverlay(); }
        @Override public void componentResized(ComponentEvent e) { syncOverlayBounds(); }
        @Override public void componentMoved(ComponentEvent e) { syncOverlayBounds(); }
    };

    private final ComponentAdapter hostWindowAdapter = new ComponentAdapter() {
        @Override public void componentMoved(ComponentEvent e) { syncOverlayBounds(); }
        @Override public void componentResized(ComponentEvent e) { syncOverlayBounds(); }
    };

    private final WindowAdapter hostWindowStateAdapter = new WindowAdapter() {
        @Override public void windowIconified(WindowEvent e) { hideOverlay(); }
        @Override public void windowDeiconified(WindowEvent e) { if (isShowing()) showOverlay(); }
        @Override public void windowActivated(WindowEvent e) {
            if (isShowing()) FxBridge.onFx(() -> { if (stage != null) stage.toFront(); });
        }
        @Override public void windowClosing(WindowEvent e) { hideOverlay(); }
    };

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

    private void onLoginReturned(String email, String password) {
        if (welcomeDialog.cognitoInterface.isAuthenticated()) {
            if (rememberMeSelected) {
                welcomeDialog.setPassword(password);
            }
            ok();
        } else if (welcomeDialog.cognitoInterface.isPasswordResetRequired()) {
            welcomeDialog.setEmail(email);
            welcomeDialog.gotoResetCard();
            welcomeDialog.setMessage("Your password has been reset. Please choose a new password.");
        } else if (welcomeDialog.cognitoInterface.isNotAuthorizedException()) {
            welcomeDialog.setMessage(welcomeDialog.cognitoInterface.getAuthMessage());
        } else if (welcomeDialog.cognitoInterface.isSdkClientException()) {
            welcomeDialog.SdkClientException(this);
        } else if (welcomeDialog.cognitoInterface.isNewPasswordRequired()) {
            welcomeDialog.setEmail(email);
            welcomeDialog.gotoNewPasswordRequiredCard();
        } else {
            welcomeDialog.setMessage(welcomeDialog.cognitoInterface.getAuthMessage());
        }
    }
}