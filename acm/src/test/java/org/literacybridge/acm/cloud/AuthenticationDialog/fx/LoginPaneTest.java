package org.literacybridge.acm.cloud.AuthenticationDialog.fx;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.event.Event;
import javafx.scene.Scene;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Behaviour of the JavaFX login form. Events are fired directly at the controls, so no real
 * keyboard or mouse input is involved.
 *
 * <p>Skipped (not failed) when JavaFX can not start, which is the case on a headless machine.
 */
public class LoginPaneTest {
    private static boolean toolkitReady = false;

    @BeforeClass
    public static void startToolkit() {
        try {
            new JFXPanel();                 // boots the JavaFX toolkit
            Platform.setImplicitExit(false);
            toolkitReady = true;
        } catch (Throwable ignored) {
            // headless, or no JavaFX: tests below are skipped
        }
    }

    /** A LoginPane plus a record of the gestures it raised. */
    private static class Harness {
        LoginPane pane;
        boolean loggedIn, forgot, signUp;
        String email, password;
    }

    /** Builds a pane on the JavaFX thread and runs {@code body} against it there. */
    private void withPane(final PaneBody body) throws Exception {
        Assume.assumeTrue("JavaFX toolkit unavailable", toolkitReady);
        final AtomicReference<Throwable> failure = new AtomicReference<>();
        final CountDownLatch done = new CountDownLatch(1);
        Platform.runLater(new Runnable() {
            public void run() {
                try {
                    final Harness h = new Harness();
                    h.pane = new LoginPane("Audio Content Manager", new LoginPane.Handler() {
                        public void onLogin(String e, String p) {
                            h.loggedIn = true; h.email = e; h.password = p;
                        }
                        public void onForgotPassword(String e) { h.forgot = true; h.email = e; }
                        public void onSignUp() { h.signUp = true; }
                    });
                    new Scene(h.pane, 418, 485);
                    h.pane.applyCss();
                    h.pane.layout();
                    body.run(h);
                } catch (Throwable t) {
                    failure.set(t);
                } finally {
                    done.countDown();
                }
            }
        });
        assertTrue("JavaFX thread timed out", done.await(20, TimeUnit.SECONDS));
        if (failure.get() != null) throw new AssertionError(failure.get());
    }

    private interface PaneBody { void run(Harness h) throws Exception; }

    @SuppressWarnings("unchecked")
    private static <T> T control(LoginPane pane, String name) throws Exception {
        Field f = LoginPane.class.getDeclaredField(name);
        f.setAccessible(true);
        return (T) f.get(pane);
    }

    private static void pressEnter(Object control) {
        Event.fireEvent((javafx.event.EventTarget) control,
            new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.ENTER, false, false, false, false));
    }

    @Test
    public void loginIsEnabledOnlyWhenBothFieldsHaveText() throws Exception {
        withPane(h -> {
            assertFalse("empty form", h.pane.isLoginEnabled());
            h.pane.setEmail("a@b.org");
            assertFalse("email only", h.pane.isLoginEnabled());
            h.pane.setPassword("Passw0rd");
            assertTrue("both filled", h.pane.isLoginEnabled());
        });
    }

    @Test
    public void enterInEitherFieldSubmitsTheForm() throws Exception {
        withPane(h -> {
            h.pane.setEmail("a@b.org");
            h.pane.setPassword("Passw0rd");

            pressEnter(control(h.pane, "emailField"));
            assertTrue("enter in email submits", h.loggedIn);
            assertEquals("a@b.org", h.email);
            assertEquals("Passw0rd", h.password);

            h.loggedIn = false;
            pressEnter(control(h.pane, "passwordField"));
            assertTrue("enter in password submits", h.loggedIn);
        });
    }

    @Test
    public void incompleteFormDoesNotSubmit() throws Exception {
        withPane(h -> {
            h.pane.setEmail("a@b.org");
            h.pane.triggerLogin();
            assertFalse("no password, no submit", h.loggedIn);
        });
    }

    @Test
    public void eyeToggleSwapsTheVisibleFieldButKeepsOneValue() throws Exception {
        withPane(h -> {
            PasswordField password = control(h.pane, "passwordField");
            TextField reveal = control(h.pane, "passwordReveal");
            ToggleButton eye = control(h.pane, "revealButton");
            h.pane.setPassword("Passw0rd");

            assertTrue("hidden by default", password.isVisible());
            assertFalse(reveal.isVisible());

            eye.setSelected(true);
            assertTrue("revealed field shown", reveal.isVisible());
            assertFalse(password.isVisible());
            assertEquals("shared value", "Passw0rd", reveal.getText());

            eye.setSelected(false);
            assertTrue("hidden again", password.isVisible());
            assertFalse(reveal.isVisible());
        });
    }

    @Test
    public void savedPasswordCanNotBeRevealedUntilItIsCleared() throws Exception {
        withPane(h -> {
            ToggleButton eye = control(h.pane, "revealButton");
            h.pane.setPassword("fromDisk");
            h.pane.setRevealPasswordEnabled(false);
            assertTrue("eye disabled for a saved password", eye.isDisabled());

            h.pane.setPassword("");
            assertFalse("eye re-enabled once cleared", eye.isDisabled());
        });
    }

    @Test
    public void rememberMeDefaultsToSelected() throws Exception {
        // Authenticator relies on this to keep the pre-JavaFX behaviour of saving the password.
        withPane(h -> assertTrue(h.pane.isRememberMeSelected()));
    }
}
