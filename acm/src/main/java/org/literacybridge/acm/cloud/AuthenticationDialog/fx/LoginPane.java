package org.literacybridge.acm.cloud.AuthenticationDialog.fx;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.InputStream;

/**
 * The JavaFX login form. Pure UI: it collects an email and a password and reports gestures to a
 * {@link Handler}. It performs no authentication itself, so the Cognito code behind it is
 * untouched by the Swing-to-JavaFX migration.
 *
 * <p>Every method here runs on the JavaFX application thread. Callers coming from Swing must
 * hop threads first -- see {@code FxBridge.onFx}.
 */
public class LoginPane extends VBox {
    /** Gestures the hosting card reacts to. All are raised on the JavaFX thread. */
    public interface Handler {
        void onLogin(String email, String password);
        void onForgotPassword(String email);
        void onSignUp();
    }

    private static final double FORM_WIDTH = 340;
    private static final double LOGO_SIZE = 120;

    private final Handler handler;

    private final TextField emailField = new TextField();
    private final PasswordField passwordField = new PasswordField();
    /** Mirrors passwordField, shown in its place when the eye toggle reveals the password. */
    private final TextField passwordReveal = new TextField();
    private final ToggleButton revealButton = new ToggleButton();
    private final CheckBox rememberMe = new CheckBox("Remember me");
    private final Button loginButton = new Button("Login");

    public LoginPane(String applicationName, Handler handler) {
        this.handler = handler;

        getStyleClass().add("login-pane");
        setAlignment(Pos.TOP_CENTER);
        getStylesheets().add(getClass().getResource("/fx/login.css").toExternalForm());

        getChildren().addAll(
            buildLogo(),
            buildTitle(applicationName),
            buildForm(),
            buildLoginButton(),
            buildSignUpRow());
    }

    private ImageView buildLogo() {
        ImageView logo = new ImageView();
        InputStream png = getClass().getResourceAsStream("/Amplio-Logo-NoTagline-FullColor-Square.png");
        if (png != null) {
            logo.setImage(new Image(png, LOGO_SIZE, LOGO_SIZE, true, true));
        }
        logo.getStyleClass().add("logo");
        return logo;
    }

    private Label buildTitle(String applicationName) {
        Label title = new Label(String.format("Login to %s", applicationName));
        title.getStyleClass().add("title");
        return title;
    }

    private VBox buildForm() {
        emailField.setPromptText("Enter email");
        emailField.getStyleClass().add("field");
        // Enter anywhere in the form submits, matching the old Swing dialog's global Enter key.
        emailField.setOnAction(e -> triggerLogin());

        passwordField.setPromptText("Enter password");
        passwordField.getStyleClass().add("field");
        passwordField.setOnAction(e -> triggerLogin());

        VBox form = new VBox(
            label("Email"), emailField,
            label("Password"), buildPasswordRow(),
            buildOptionsRow());
        form.getStyleClass().add("form");
        form.setMaxWidth(FORM_WIDTH);
        return form;
    }

    private Label label(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("field-label");
        return label;
    }

    /** The password field with the reveal ("eye") toggle overlaid on its right edge. */
    private StackPane buildPasswordRow() {
        passwordReveal.setPromptText("Enter password");
        passwordReveal.getStyleClass().add("field");
        // One text value, two views of it: whichever is hidden is also un-managed, so it takes
        // no space and the row does not change height when the password is revealed.
        passwordReveal.textProperty().bindBidirectional(passwordField.textProperty());
        passwordReveal.visibleProperty().bind(revealButton.selectedProperty());
        passwordReveal.managedProperty().bind(revealButton.selectedProperty());
        passwordField.visibleProperty().bind(revealButton.selectedProperty().not());
        passwordField.managedProperty().bind(revealButton.selectedProperty().not());
        passwordReveal.setOnAction(e -> triggerLogin());

        revealButton.setGraphic(eyeIcon("/no-eye_256.png"));
        revealButton.getStyleClass().add("reveal-button");
        revealButton.selectedProperty().addListener((obs, was, revealed) ->
            revealButton.setGraphic(eyeIcon(revealed ? "/eye_256.png" : "/no-eye_256.png")));

        StackPane row = new StackPane(passwordField, passwordReveal, revealButton);
        StackPane.setAlignment(revealButton, Pos.CENTER_RIGHT);
        return row;
    }

    private ImageView eyeIcon(String resource) {
        InputStream png = getClass().getResourceAsStream(resource);
        if (png == null) return null;
        ImageView icon = new ImageView(new Image(png, 18, 18, true, true));
        icon.getStyleClass().add("eye-icon");
        return icon;
    }

    /**
     * "Remember me" on the left, "Forgot Password" on the right. The Figma design omits the
     * checkbox, but {@code Authenticator} still reads it to decide whether to store the
     * password, so it is kept rather than silently hard-coding that choice.
     */
    private HBox buildOptionsRow() {
        rememberMe.setSelected(true);
        rememberMe.getStyleClass().add("remember-me");

        Hyperlink forgotPassword = new Hyperlink("Forgot Password");
        forgotPassword.getStyleClass().add("link");
        forgotPassword.setOnAction(e -> handler.onForgotPassword(getEmail()));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox row = new HBox(rememberMe, spacer, forgotPassword);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("options-row");
        return row;
    }

    private Button buildLoginButton() {
        loginButton.getStyleClass().add("login-button");
        // Replaces the old DocumentListener/KeyListener pair: the button follows the fields.
        loginButton.disableProperty().bind(
            emailField.textProperty().isEmpty().or(passwordField.textProperty().isEmpty()));
        loginButton.setOnAction(e -> triggerLogin());
        return loginButton;
    }

    private HBox buildSignUpRow() {
        Label prompt = new Label("Don't have an account yet?");
        prompt.getStyleClass().add("signup-prompt");

        Hyperlink signUp = new Hyperlink("Sign Up");
        signUp.getStyleClass().add("link");
        signUp.setOnAction(e -> handler.onSignUp());

        HBox row = new HBox(prompt, signUp);
        row.setAlignment(Pos.CENTER);
        row.getStyleClass().add("signup-row");
        return row;
    }

    // ---- API used by LoginCard. Every one of these runs on the JavaFX thread. ----

    public String getEmail() {
        return emailField.getText();
    }

    public String getPassword() {
        return passwordField.getText();
    }

    public void setEmail(String email) {
        emailField.setText(email == null ? "" : email);
    }

    public void setPassword(String password) {
        passwordField.setText(password == null ? "" : password);
    }

    public boolean isRememberMeSelected() {
        return rememberMe.isSelected();
    }

    /**
     * A password restored from disk is never revealed, so the eye is switched off and disabled.
     * Clearing the field re-enables it -- at that point there is nothing saved left to expose.
     */
    public void setRevealPasswordEnabled(boolean enabled) {
        revealButton.setSelected(false);
        revealButton.setDisable(!enabled);
        if (!enabled) {
            passwordField.textProperty().addListener(new ChangeListener<String>() {
                @Override
                public void changed(ObservableValue<? extends String> obs, String was, String now) {
                    if (now == null || now.isEmpty()) {
                        revealButton.setDisable(false);
                        passwordField.textProperty().removeListener(this);
                    }
                }
            });
        }
    }

    public void focusEmail() {
        emailField.requestFocus();
    }

    public boolean isLoginEnabled() {
        return !loginButton.isDisabled();
    }

    /** Submits the form, if it is complete. Safe to call from any gesture. */
    public void triggerLogin() {
        if (isLoginEnabled()) {
            handler.onLogin(getEmail(), getPassword());
        }
    }
}
