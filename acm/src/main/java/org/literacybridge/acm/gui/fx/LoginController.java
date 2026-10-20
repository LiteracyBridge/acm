package org.literacybridge.acm.gui.fx;

import com.amazonaws.services.cognitoidentity.model.Credentials;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONObject;
import org.literacybridge.acm.cloud.Authenticator;
import org.literacybridge.acm.cloud.IdentityPersistence;
import org.literacybridge.acm.cloud.cognito.AuthenticationHelper;
import org.literacybridge.acm.cloud.cognito.CognitoHelper;
import org.literacybridge.acm.cloud.cognito.CognitoJWTParser;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.config.AccessControl;
import org.literacybridge.acm.config.AmplioHome;
import org.literacybridge.acm.gui.ApplicationV2;
import org.literacybridge.acm.gui.CommandLineParams;
import org.literacybridge.acm.gui.resourcebundle.LabelProvider;

import javax.swing.*;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.literacybridge.acm.cloud.Authenticator.LoginOptions.*;
import static org.literacybridge.acm.cloud.Authenticator.PROGRAMS_INFO_API;

public class LoginController {

    private final String applicationName;
    private String computerName;
    private final IdentityPersistence identityPersistence;
    private final CognitoHelper cognitoHelper;
//    Authenticator.CognitoInterface cognitoInterface = new Authenticator.CognitoInterface();


    @FXML
    private Pane rootPane;

    @FXML
    private TextField emailField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private TextField visiblePasswordField;
    @FXML
    private ToggleButton passwordVisibilityButton;
    @FXML
    private Button loginButton;
    @FXML
    private Hyperlink signUpLink;
    @FXML
    private CheckBox rememberMeCheckBox;
    @FXML
    private Label messageLabel;

    private Runnable onLogin;
    private Handler handler;
    private ChangeListener<String> savedPasswordListener;
    private Map<String, String> authenticationInfo;
    private Credentials credentials;
    private Authenticator.AwsInterface awsInterface;

    public LoginController() {
        try {
            computerName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException ignored) {
        }
        this.identityPersistence = new IdentityPersistence(AmplioHome.getDirectory());
        applicationName = ACMConfiguration.cannonicalProjectName(ApplicationV2.cliParams.sharedACM);
        cognitoHelper = new CognitoHelper();
    }

    public interface Handler {
        void onLogin(String email, String password);

        void onForgotPassword(String email);

        void onSignUp();
    }

    public void setEmail(String email) {
        emailField.setText(email == null ? "" : email);
    }

    public void setPassword(String password) {
        passwordField.setText(password == null ? "" : password);
    }

    public void setRevealPasswordEnabled(boolean enabled) {
        passwordVisibilityButton.setSelected(false);
        passwordVisibilityButton.setDisable(!enabled);
        if (savedPasswordListener != null) {
            passwordField.textProperty().removeListener(savedPasswordListener);
            savedPasswordListener = null;
        }
        if (!enabled) {
            savedPasswordListener = new ChangeListener<String>() {
                @Override
                public void changed(ObservableValue<? extends String> observable,
                                    String oldValue, String newValue) {
                    if (newValue == null || newValue.isEmpty()) {
                        passwordVisibilityButton.setDisable(false);
                        passwordField.textProperty().removeListener(this);
                        savedPasswordListener = null;
                    }
                }
            };
            passwordField.textProperty().addListener(savedPasswordListener);
        }
    }

    public void focusEmail() {
        emailField.requestFocus();
    }

    public void triggerLogin() {
        handleLogin();
    }

    // Kept for the standalone login preview.
    public void setOnLogin(Runnable onLogin) {
        this.onLogin = onLogin;
    }

    // Set by the application host when this screen is used in the login flow.
    public void setHandler(Handler handler) {
        this.handler = handler;
    }

    @FXML
    private void handleLogin() {
        String email = getEmail();
        String password = getPassword();

        if (email.isEmpty() || password.isEmpty()) {
            messageLabel.setText("Please fill out all the fields");
            return;
        }

        messageLabel.setText("");
        Map<String, String> userMetadata = new HashMap<>();
        userMetadata.put("Application", "applicationName");
        if (StringUtils.isNotBlank(computerName)) {
            userMetadata.put("Computer", computerName);
        }
        AuthenticationHelper.AuthenticationResult validationResult = cognitoHelper.ValidateUser(email, password, userMetadata);

        String jwtToken = validationResult.getJwtToken();
        if (jwtToken != null) {
            JSONObject payload = CognitoJWTParser.getPayload(jwtToken);
            @SuppressWarnings("unchecked")
            Set<Map.Entry<Object, Object>> payloadEntries = payload.entrySet();
            authenticationInfo = payloadEntries.stream().collect(Collectors.toMap(e -> e.getKey().toString(), e -> e.getValue().toString()));

            String provider = authenticationInfo.get("iss").replace("https://", "");
            credentials = cognitoHelper.GetCredentials(provider, jwtToken);


            JSONObject jsonResponse = awsInterface.authenticatedGetCall(PROGRAMS_INFO_API);
            try {
                JSONObject programsInfo = (JSONObject) jsonResponse.get("result");
                System.out.println(programsInfo);
//                parseProgramsInfoJSON(programsInfo);
//                writeProgramsInfoJSON(programsInfo);
            } catch (Exception ex) {
                ex.printStackTrace();
                DailogHelpers.errorDailog(getState(), "Authentication Failure", "Authentication is required to use the ACM.", false);
            }
        }

        if (handler != null) {
            handler.onLogin(email, password);
        } else if (onLogin != null) {
            onLogin.run();
        }
    }

    @FXML
    private void handleForgotPassword() {
        if (handler != null) {
            handler.onForgotPassword(getEmail());
        }
    }

    @FXML
    private void handleSignUp() {
        if (handler != null) {
            handler.onSignUp();
        }
    }

    @FXML
    private void initialize() {
//        try {
//            computerName = InetAddress.getLocalHost().getHostName();
//        } catch (UnknownHostException ignored) {
//        }
//        this.identityPersistence = new IdentityPersistence(AmplioHome.getDirectory());
//        cognitoHelper = new CognitoHelper();


        visiblePasswordField.textProperty().bindBidirectional(passwordField.textProperty());
        visiblePasswordField.visibleProperty().bind(passwordVisibilityButton.selectedProperty());
        visiblePasswordField.managedProperty().bind(visiblePasswordField.visibleProperty());
        passwordField.visibleProperty().bind(passwordVisibilityButton.selectedProperty().not());
        passwordField.managedProperty().bind(passwordField.visibleProperty());

        loginButton.disableProperty().bind(
                emailField.textProperty().isEmpty()
                        .or(passwordField.textProperty().isEmpty())
        );

        IdentityPersistence.LoginDetails savedLoginDetails = identityPersistence.retrieveLoginDetails();
        setPassword(savedLoginDetails.secret);
        setEmail(savedLoginDetails.email);

        if (!AccessControl.isOnline()) {
            passwordField.setDisable(true);
        }

//        CommandLineParams params = ApplicationV2.cliParams;
//        Authenticator authInstance = Authenticator.getInstance();
//        authInstance.setLocallyAvailablePrograms(ACMConfiguration.getInstance().getLocalProgramDbs());
//        Authenticator.LoginOptions okRepoOption = AmplioHome.isOldStyleHomeDirectory() || params.noS3Dbs ? LOCAL_DATA_ONLY : LOCAL_OR_S3;
//        Authenticator.LoginResult result = authInstance.authenticateAndChooseProgram(null,
//                LabelProvider.getLabel("ACM_PROGRAM_NAME"),
//                ACMConfiguration.cannonicalProjectName(params.sharedACM),
//                OFFLINE_EMAIL_CHOICE,
//                CHOOSE_PROGRAM,
//                okRepoOption,
//                params.noFoundDbs ? NOP : INCLUDE_FOUND_ACMS,
//                params.sandbox ? SUGGEST_DEMO_MODE : OFFER_DEMO_MODE, // vs no demo mode at all
//                INCLUDE_FB_ACMS,
//                params.go ? NO_WAIT : NOP);
//        if (result == Authenticator.LoginResult.FAILURE) {
//            JOptionPane.showMessageDialog(null,
//                    "Authentication is required to use the ACM.",
//                    "Authentication Failure",
//                    JOptionPane.ERROR_MESSAGE);
//            System.exit(13);
//        }
    }

    @FXML
    private void handlePasswordVisibility() {
        // Move focus to the active field and keep the caret at the end.
        if (passwordVisibilityButton.isSelected()) {
            visiblePasswordField.requestFocus();
            visiblePasswordField.positionCaret(visiblePasswordField.getText().length());
        } else {
            passwordField.requestFocus();
            passwordField.positionCaret(passwordField.getText().length());
        }
    }

    public String getEmail() {
        return emailField.getText().trim();
    }

    public String getPassword() {
        return passwordField.getText();
    }

    public boolean isRememberMeSelected() {
        return rememberMeCheckBox.isSelected();
    }

    private Stage getState() {
        return (Stage) rootPane.getScene().getWindow();
    }
}