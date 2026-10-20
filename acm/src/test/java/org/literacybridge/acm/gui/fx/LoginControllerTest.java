package org.literacybridge.acm.gui.fx;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LoginControllerTest {
    private static boolean toolkitReady;

    @BeforeClass
    public static void startToolkit() {
        try {
            new JFXPanel();
            Platform.setImplicitExit(false);
            toolkitReady = true;
        } catch (Throwable ignored) {
            // JavaFX tests are skipped when the toolkit cannot start (for example, headless).
        }
    }

    private static class Harness implements LoginController.Handler {
        LoginController controller;
        Parent root;
        boolean loggedIn;
        boolean forgot;
        boolean signedUp;
        String email;
        String password;

        @Override
        public void onLogin(String email, String password) {
            loggedIn = true;
            this.email = email;
            this.password = password;
        }

        @Override
        public void onForgotPassword(String email) {
            forgot = true;
            this.email = email;
        }

        @Override
        public void onSignUp() {
            signedUp = true;
        }
    }

    private interface ScreenBody {
        void run(Harness harness) throws Exception;
    }

    private void withScreen(final ScreenBody body) throws Exception {
        Assume.assumeTrue("JavaFX toolkit unavailable", toolkitReady);
        final AtomicReference<Throwable> failure = new AtomicReference<>();
        final CountDownLatch done = new CountDownLatch(1);

        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/literacybridge/acm/gui/loginscreen.fxml"));
                    Harness harness = new Harness();
                    harness.root = loader.load();
                    harness.controller = loader.getController();
                    harness.controller.setHandler(harness);
                    new Scene(harness.root, 1280, 760);
                    harness.root.applyCss();
                    harness.root.layout();
                    body.run(harness);
                } catch (Throwable t) {
                    failure.set(t);
                } finally {
                    done.countDown();
                }
            }
        });

        assertTrue("JavaFX thread timed out", done.await(20, TimeUnit.SECONDS));
        if (failure.get() != null) {
            throw new AssertionError(failure.get());
        }
    }

    @Test
    public void loginButtonIsEnabledOnlyWhenBothFieldsHaveText() throws Exception {
        withScreen(h -> {
            Button login = (Button) h.root.lookup("#loginButton");
            assertTrue(login.isDisabled());
            h.controller.setEmail("a@b.org");
            assertTrue(login.isDisabled());
            h.controller.setPassword("Passw0rd");
            assertFalse(login.isDisabled());
        });
    }

    @Test
    public void loginActionForwardsCredentialsToHandler() throws Exception {
        withScreen(h -> {
            h.controller.setEmail("a@b.org");
            h.controller.setPassword("Passw0rd");
            ((Button) h.root.lookup("#loginButton")).fire();
            assertTrue(h.loggedIn);
            assertEquals("a@b.org", h.email);
            assertEquals("Passw0rd", h.password);
        });
    }

    @Test
    public void forgotPasswordAndSignUpActionsReachHandler() throws Exception {
        withScreen(h -> {
            h.controller.setEmail("a@b.org");
            ((Hyperlink) h.root.lookup("#forgotPasswordLink")).fire();
            assertTrue(h.forgot);
            assertEquals("a@b.org", h.email);

            ((Hyperlink) h.root.lookup("#signUpLink")).fire();
            assertTrue(h.signedUp);
        });
    }

    @Test
    public void passwordVisibilityToggleKeepsTheSameValue() throws Exception {
        withScreen(h -> {
            PasswordField password = (PasswordField) h.root.lookup("#passwordField");
            TextField visiblePassword = (TextField) h.root.lookup("#visiblePasswordField");
            ToggleButton visibility = (ToggleButton) h.root.lookup("#passwordVisibilityButton");
            h.controller.setPassword("Passw0rd");

            assertTrue(password.isVisible());
            assertFalse(visiblePassword.isVisible());
            visibility.fire();
            assertTrue(visiblePassword.isVisible());
            assertFalse(password.isVisible());
            assertEquals("Passw0rd", visiblePassword.getText());
            visibility.fire();
            assertTrue(password.isVisible());
            assertFalse(visiblePassword.isVisible());
        });
    }

    @Test
    public void savedPasswordCanNotBeRevealedUntilItIsCleared() throws Exception {
        withScreen(h -> {
            ToggleButton visibility = (ToggleButton) h.root.lookup("#passwordVisibilityButton");
            h.controller.setPassword("fromDisk");
            h.controller.setRevealPasswordEnabled(false);
            assertTrue(visibility.isDisabled());
            h.controller.setPassword("");
            assertFalse(visibility.isDisabled());
        });
    }

    @Test
    public void rememberMeRemainsSelectedForExistingAuthenticationFlow() throws Exception {
        withScreen(h -> assertTrue(h.controller.isRememberMeSelected()));
    }
}
