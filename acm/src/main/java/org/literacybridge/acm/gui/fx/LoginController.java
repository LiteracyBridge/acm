package org.literacybridge.acm.gui.fx;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.cognitoidentity.model.Credentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
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
import org.json.simple.JSONValue;
import org.literacybridge.acm.cloud.Authenticator;
import org.literacybridge.acm.cloud.IdentityPersistence;
import org.literacybridge.acm.cloud.cognito.AuthenticationHelper;
import org.literacybridge.acm.cloud.cognito.CognitoHelper;
import org.literacybridge.acm.cloud.cognito.CognitoJWTParser;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.config.AccessControl;
import org.literacybridge.acm.config.AmplioHome;
import org.literacybridge.acm.config.HttpUtility;
import org.literacybridge.acm.gui.ApplicationV2;
import org.literacybridge.acm.gui.CommandLineParams;
import org.literacybridge.acm.gui.resourcebundle.LabelProvider;

import javax.swing.*;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import static org.literacybridge.acm.cloud.Authenticator.LoginOptions.OFFLINE_EMAIL_CHOICE;
import static org.literacybridge.acm.cloud.Authenticator.PROGRAMS_INFO_API;

public class LoginController {

    private final String applicationName;
    private String computerName;
    private final IdentityPersistence identityPersistence;
    private final CognitoHelper cognitoHelper;
    private Runnable onLogin;
    private Handler handler;
    private ChangeListener<String> savedPasswordListener;
    private Map<String, String> authenticationInfo;
    private Credentials credentials;
    private AuthenticationHelper.AuthenticationResult authenticationResult;

    public static final String PROGRAMS_INFO_FILE = "programs.info";

    private final AwsInterface awsInterface = new AwsInterface();

    CognitoInterface cognitoInterface = new CognitoInterface();


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
        authenticationResult = cognitoHelper.ValidateUser(email, password, userMetadata);

        String jwtToken = authenticationResult.getJwtToken();
        if (jwtToken != null) {
            onAuthenticated(jwtToken);
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

    /**
     * Writes a JSONObject to the ~/Amplio/programs.info file or ~/LiteracyBridge/programs.info.
     *
     * @param programsInfo to be written.
     */
    private void writeProgramsInfoJSON(JSONObject programsInfo) {
        String resultString = programsInfo.toJSONString();
        JSONObject parsedString = (JSONObject) JSONValue.parse(resultString);
        assert (parsedString.equals(programsInfo));

        File programsInfoFile = new File(AmplioHome.getDirectory(), PROGRAMS_INFO_FILE);
        try (FileWriter fw = new FileWriter(programsInfoFile)) {
            fw.write(resultString);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void onAuthenticated(String jwtToken) {
        JSONObject payload = CognitoJWTParser.getPayload(jwtToken);
        Set<Map.Entry<Object, Object>> payloadEntries = payload.entrySet();
        authenticationInfo = payloadEntries.stream().collect(Collectors.toMap(e -> e.getKey().toString(), e -> e.getValue().toString()));

        String provider = authenticationInfo.get("iss").replace("https://", "");
        credentials = cognitoHelper.GetCredentials(provider, jwtToken);

//        String email = authenticationInfo.get("email");

        JSONObject jsonResponse = awsInterface.authenticatedGetCall(PROGRAMS_INFO_API);
        try {
            JSONObject programsInfo = (JSONObject) jsonResponse.get("result");
//            parseProgramsInfoJSON(programsInfo);
            writeProgramsInfoJSON(programsInfo);

            identityPersistence.saveLoginDetails(emailField.getText(), passwordField.getText(), new HashMap<>(authenticationInfo));

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/literacybridge/acm/gui/programselection.fxml"));
            Stage stage = getState();
            stage.setScene(new Scene(loader.load()));
            stage.show();
        } catch (Exception ex) {
            ex.printStackTrace();
            DailogHelpers.errorDailog(getState(), "Authentication Failure", "Authentication is required to use the ACM.", false);

        }
    }

    /**
     * Authenticated means that we have successfully given sign-in credentials to Cognito. We
     * have a token, and can make authenticated server calls.
     *
     * @return True if the user is authenticated, false otherwise
     */
    public boolean isAuthenticated() {
        return credentials != null;
    }

    /**
     * Encapsulates helpers to make authenticated AWS calls.
     */
    public class AwsInterface {
        private AmazonS3 s3Client = null;

        /**
         * Gets an S3 client object, through which we can upload or download files to S3.
         *
         * @return the S3 client object.
         */
        AmazonS3 getS3Client() {
            checkSession();
            if (s3Client == null) {
                Regions region = cognitoHelper.getRegion();
                BasicSessionCredentials awsCreds = new BasicSessionCredentials(credentials.getAccessKeyId(),
                        credentials.getSecretKey(),
                        credentials.getSessionToken());
                s3Client = AmazonS3ClientBuilder.standard()
                        .withCredentials(new AWSStaticCredentialsProvider(awsCreds))
                        .withRegion(region)
                        .build();
            }
            return s3Client;
        }

        void refreshSession() {
            if (authenticationResult.isSuccess()) {
                String refreshToken = authenticationResult.getRefreshToken();
                AuthenticationHelper.AuthenticationResult refreshResult =
                        cognitoHelper.RefreshSession(refreshToken);
                if (refreshResult.isSuccess()) {
                    cognitoInterface.parseAuthenticationResult(refreshResult);
                    s3Client = null;
                    System.out.println("Refreshed session");
                } else {
                    System.out.println("Failed to refresh.");
                }
            }
        }

        void checkSession() {
            if (authenticationResult.isExpired()) {
                System.out.println("Refreshing session after timeout.");
                refreshSession();
            }
        }

        /**
         * Downloads an object from S3, using the credentials of the current signed-in user.
         *
         * @param bucket          containing the object
         * @param key             of the object
         * @param of              output File
         * @param progressHandler optional handler called periodically with (received, expected) bytes.
         */
        public boolean downloadS3Object(String bucket,
                                        String key,
                                        File of,
                                        BiConsumer<Long, Long> progressHandler) {
            return downloadS3Object(new GetObjectRequest(bucket, key), of, progressHandler);
        }

        public boolean downloadS3Object(GetObjectRequest request, File of, BiConsumer<Long, Long> progressHandler) {
            if (credentials == null) return false;

            AmazonS3 s3Client = getS3Client();
            long bytesExpected, bytesDownloaded = 0;

            try (S3Object s3Object = s3Client.getObject(request);
                 FileOutputStream fos = new FileOutputStream(of);
                 BufferedOutputStream bos = new BufferedOutputStream(fos)) {
                InputStream is = s3Object.getObjectContent();
                bytesExpected = s3Object.getObjectMetadata().getContentLength();

                byte[] buffer = new byte[65536];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) > 0) {
                    bos.write(buffer, 0, bytesRead);
                    bytesDownloaded += bytesRead;
                    if (progressHandler != null) {
                        progressHandler.accept(bytesDownloaded, bytesExpected);
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
            return true;
        }

        public boolean uploadS3Object(String bucket, String key, File inputFile) {
            boolean result = false;
            if (!isAuthenticated()) return false;
            try {
                PutObjectRequest request = new PutObjectRequest(bucket, key, inputFile);
                @SuppressWarnings("unused")
                PutObjectResult putResult = getS3Client().putObject(request);
                result = true;
            } catch (Exception ex) {
                System.out.println("Refreshing session after exception.");
                refreshSession();
                // Ignore and return false
                // ex.printStackTrace();
            }
            return result;
        }

        private JSONObject authenticatedRestCall(String verb, String requestURL, JSONObject body, Map<String, String> headers) {
            if (!isAuthenticated()) return null;
            checkSession();
            Map<String, String> allHeaders = new LinkedHashMap<>();
            if (headers != null) {
                allHeaders.putAll(headers);
            }
            String idToken = authenticationResult.getIdToken();
            allHeaders.put("Authorization", idToken);

            HttpUtility httpUtility = new HttpUtility();
            JSONObject jsonResponse = null;

            try {
                if (verb.equals("POST")) {
                    httpUtility.sendPostRequest(requestURL, body, allHeaders);
                } else if (verb.equals("GET")) {
                    httpUtility.sendGetRequest(requestURL, allHeaders);
                }
                jsonResponse = httpUtility.readJSONObject();
            } catch (IOException ex) {
                // Stack trace & ignore.
                ex.printStackTrace();
            } finally {
                httpUtility.disconnect();
            }
            return jsonResponse;
        }

        /**
         * Make a REST GET call with the current signed-in credentials. We use this to make Lambda calls.
         *
         * @param requestURL URL to request.
         * @return result in a JSON object.
         */
        public JSONObject authenticatedGetCall(String requestURL) {
            Map<String, String> headers = new HashMap<>();
            headers.put("Accept", "application/json");
            return authenticatedRestCall("GET", requestURL, null, headers);
        }

        /**
         * Make a REST POST call with the current signed-in credentials. We use this to make Lambda calls.
         *
         * @param requestURL URL to request.
         * @param body       JSON object to pass as request body.
         * @return result in a JSON object.
         */
        public JSONObject authenticatedPostCall(String requestURL, JSONObject body) {
            return authenticatedRestCall("POST", requestURL, body, null);
        }

    }

    /**
     * Wrapper class to encapsulate the Cognito helpers. These are provided to the sign-in /
     * sign-up dialog only.
     */
    public class CognitoInterface {
        public boolean isOnline() {
            return AccessControl.isOnline();
        }

        /**
         * Given a username (or email) and password, attempt to sign-in to Cognito. Sets members
         * authenticationResult with the results of the sign-in attempt.
         * authenticationInfo with the info returned by a successful sign-in.
         * credentials with the credentials returned by a successful sign-in.
         *
         * @param usernameOrEmail username or email address.
         * @param password        of the user id.
         */
        public void authenticate(String usernameOrEmail, String password) {
            Map<String, String> userMetadata = new HashMap<>();
            userMetadata.put("Application", applicationName);
            if (StringUtils.isNotBlank(computerName)) userMetadata.put("Computer", computerName);
            AuthenticationHelper.AuthenticationResult validationResult = cognitoHelper.ValidateUser(usernameOrEmail, password, userMetadata);

            parseAuthenticationResult(validationResult);
        }

        private void parseAuthenticationResult(AuthenticationHelper.AuthenticationResult authenticationResult) {
            LoginController.this.authenticationResult = authenticationResult;
            String jwtToken = authenticationResult.getJwtToken();
            if (jwtToken != null) {
                onAuthenticated(jwtToken);
            }
        }

        /**
         * Starts a password reset process. Triggers the Cognito server to send a reset code to
         * the email address associated with the given user name.
         * <p>
         * Note: This does not actually reset any passwords, only sends the code to allow the
         * user to reset their password.
         *
         * @param usernameOrEmail username or email that needs a password reset.
         */
        public void resetPassword(String usernameOrEmail) {
            cognitoHelper.ResetPassword(usernameOrEmail);
        }

        /**
         * Completes a password reset. Given a user id and the reset code sent to the user id's
         * associated email address, and a new password, sets the password to the new value (if
         * it is a valid password, of course).
         *
         * @param usernameOrEmail username or email of the password to reset.
         * @param password        the new password.
         * @param pin             reset code sent via email.
         */
        public void updatePassword(String usernameOrEmail, String password, String pin) {
            cognitoHelper.UpdatePassword(usernameOrEmail, password, pin);
        }

        /**
         * Called in response to a NEW_PASSWORD_REQUIRED result. This sets the new password, and completes the login.
         *
         * @param username of user logging in.
         * @param password the new password chosen by the user.
         */
        public void provideNewPassword(String username, String password) {
            // Authentication result from last time.
            if (!authenticationResult.isNewPasswordRequired()) {
                throw new IllegalStateException("Illegal call to provideNewPassword");
            }
            AuthenticationHelper.AuthenticationResult newPasswordResult = cognitoHelper.ProvideNewPassword(authenticationResult, username, password);
            parseAuthenticationResult(newPasswordResult);
        }

        public String signUpUser(String email,
                                 String password,
                                 Map<String, String> attributes) {
            return cognitoHelper.SignUpUser(email, password, email, attributes);
        }

        @SuppressWarnings("UnusedReturnValue")
        public String verifyAccessCode(String username, String code) {
            return cognitoHelper.VerifyAccessCode(username, code);
        }

        public void resendAccessCode(String username) {
            cognitoHelper.ResendAccessCode(username);
        }

        /**
         * Authenticated means that we have successfully given sign-in credentials to Cognito. We
         * have a token, and can make authenticated server calls.
         *
         * @return True if the user is authenticated, false otherwise
         */
        public boolean isAuthenticated() {
            return credentials != null;
        }

        public boolean isNotAuthorizedException() {
            return authenticationResult != null && authenticationResult.isNotAuthorizedException();
        }

        public boolean isPasswordResetRequired() {
            return authenticationResult != null && authenticationResult.isPasswordResetRequired();
        }

        public boolean isSdkClientException() {
            return authenticationResult != null && authenticationResult.isSdkClientException();
        }

        public boolean isNewPasswordRequired() {
            return authenticationResult != null && authenticationResult.isNewPasswordRequired();
        }


//        public Map<String, String> getProgramsAndRolesForUser() {
//            return programsAndRolesForUser;
//        }
//
//        public Map<String, String> getProgramNames() {
//            if (programNames == null) {
//                readProgramsInfo();
//            }
//            return programNames;
//        }

        /**
         * @return "Authenticated" or the failure message from authentication.
         */
        public String getAuthMessage() {
            if (authenticationResult == null) return null;
            return authenticationResult.getMessage();
        }

        public String getAuthenticationAttribute(String attributeName) {
            if (authenticationInfo == null) return null;
            return authenticationInfo.get(attributeName);
        }

    }
}