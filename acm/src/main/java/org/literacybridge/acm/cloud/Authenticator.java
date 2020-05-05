package org.literacybridge.acm.cloud;

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
import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONObject;
import org.literacybridge.acm.cloud.AuthenticationDialog.WelcomeDialog;
import org.literacybridge.acm.cloud.IdentityPersistence.SigninDetails;
import org.literacybridge.acm.cloud.cognito.AuthenticationHelper;
import org.literacybridge.acm.cloud.cognito.CognitoHelper;
import org.literacybridge.acm.cloud.cognito.CognitoJWTParser;
import org.literacybridge.acm.config.AccessControl;
import org.literacybridge.acm.config.HttpUtility;

import java.awt.Window;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;

/**
 * Helper class to provide a simpler interface to cognito authentication.
 */
public class Authenticator {
    private static Authenticator instance;
    private SigninResult signinResult = SigninResult.NONE;

    public static synchronized Authenticator getInstance() {
        if (instance == null) {
            instance = new Authenticator();
        }
        return instance;
    }

    // Provides access to Cognito helpers
    CognitoInterface cognitoInterface = new CognitoInterface();

    // Provides access to authenticated AWS calls.
    AwsInterface awsInterface = new AwsInterface();

    public AwsInterface getAwsInterface() { return awsInterface; }

    private AuthenticationHelper.AuthenticationResult authenticationResult;
    private final CognitoHelper cognitoHelper;
    // A map of {String:String} with the info provided by the auth process.
    private Map<String, String> authenticationInfo;
    // We need these to make authenticated calls to, eg, S3, or Lambda.
    private Credentials credentials;

    private String userName;
    private String userEmail;
    // { program : roles-string }
    private Map<String, String> userPrograms = new HashMap<>();
    private String userProgram;
    private boolean sandboxSelected = false;

    // Cached helpers. Ones not used internally are lazy allocated.
    private final IdentityPersistence identityPersistence;
    private TbSrnHelper tbSrnHelper = null;
    private ProjectsHelper projectsHelper = null;

    // Programs available in local storage.
    private List<String> locallyAvailablePrograms = new LinkedList<>();

    private Authenticator() {
        this.identityPersistence = new IdentityPersistence();
        cognitoHelper = new CognitoHelper();
    }

    public List<String> getLocallyAvailablePrograms() {
        return locallyAvailablePrograms;
    }

    public void setLocallyAvailablePrograms(Collection<String> locallyAvailablePrograms) {
        this.locallyAvailablePrograms = new ArrayList<>(locallyAvailablePrograms);
    }

    /**
     * Retrieve one of the bits of information returned by the sign-in.
     *
     * @param propertyName to be returned.
     * @param defaultValue if there is no such value.
     * @return the value associated with propertyName, or defaultValue.
     */
    public String getUserProperty(String propertyName, String defaultValue) {
        if (authenticationInfo == null) return defaultValue;
        return authenticationInfo.getOrDefault(propertyName, defaultValue);
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
     * @return true if we can access amplio.org, false otherwise.
     */
    public boolean isOnline() {
        return AccessControl.isOnline();
    }

    public String getUserName() {
        return userName;
    }

    public String getuserEmail() {
        return userEmail;
    }

    public String getUserProgram() {
        return userProgram;
    }

    /**
     * Gets the list of roles, if any, that are defined for the user in the selected program.
     * This will be empty if offline.
     * @return the set of roles.
     */
    public Set<String> getUserRoles() {
        Set<String> result = new HashSet<>();
        String roleStr = userPrograms.get(userProgram);
        if (roleStr != null) {
            String[] roles = roleStr.split(",");
            result.addAll(Arrays.asList(roles));
        }
        return result;
    }

    public boolean isSandboxSelected() {
        return this.sandboxSelected;
    }
    void setSandboxSelected(boolean isSelected) {
        this.sandboxSelected = isSelected;
    }

    public TbSrnHelper getTbSrnHelper() {
        if (tbSrnHelper == null) {
            if (!signinResult.signedIn()) {
                throw new IllegalStateException("Must sign in first.");
            }
            tbSrnHelper = new TbSrnHelper(userEmail);
        }
        return tbSrnHelper;
    }

    /**
     * The ProjectsHelper can list and retrieve the Deployment(s) for a project.
     *
     * @return the helper.
     */
    public ProjectsHelper getProjectsHelper() {
        if (projectsHelper == null) {
            projectsHelper = new ProjectsHelper(identityPersistence);
        }
        return projectsHelper;
    }

    public enum SigninResult {
        NONE, FAILURE, SUCCESS, CACHED_OFFLINE, OFFLINE;

        boolean signedIn() {
            return this == SUCCESS || this == CACHED_OFFLINE || this == OFFLINE;
        }
    }

    public enum SigninOptions {OFFLINE_EMAIL_CHOICE, CHOOSE_PROGRAM, LOCAL_DATA_ONLY, OFFER_DEMO_MODE}

    /**
     * Determine who the user is. If we can access an authentication server, the user must
     * authenticate. (We need the user to be authenticated in order to check for new content,
     * or to download any new content found.) If we can not, we simply ask for their email
     * address, defaulting to the email of the last user who successfully authenticated.
     *
     * @param parent      window for the dialog.
     * @param signinFlags options for the sign-in.
     * @return a SignInResult from the process.
     */
    public SigninResult getUserIdentity(Window parent, String defaultProgram, SigninOptions... signinFlags) {
        Set<SigninOptions> options = new HashSet<>(Arrays.asList(signinFlags));
        SigninDetails savedSignInDetails = identityPersistence.retrieveSignInDetails();
        signinResult = SigninResult.NONE;
        boolean onlineAuthentication = isOnline();

        WelcomeDialog dialog = new WelcomeDialog(parent, defaultProgram, options, cognitoInterface);
        if (savedSignInDetails != null) {
            dialog.setSavedCredentials(savedSignInDetails.identity,
                savedSignInDetails.email,
                savedSignInDetails.secret);
        }
        dialog.setVisible(true);

        if (dialog.isSuccess()) {
            if (isAuthenticated()) {
                // Authenticated with Cognito.
                String password = dialog.isRememberMeSelected() ? dialog.getPassword() : null;
                // These are the values that we get from the Cognito signin. As of 2019-12-24
                // [sub, exp, iat, token_use, event_id, aud, iss, phone_number_verified, auth_time # Cognito values
                // custom:greeting # what the user asked to be called.
                // email_verified # will always be true if they're here; means they responded to their email.
                // edit # regex of projects the user can edit.
                // admin # if true, user is an admin in their projects.
                // cognito:username # sign-in user name. These are unique, but are chosen by the user.
                // view # regex of projects the user can view.
                // phone_number # user's phone number, if they provided one.
                // email # user's email. We use this for unique identity.
                // ]
                Map<String, String> props = new HashMap<>();
                authenticationInfo.forEach(props::put);
                identityPersistence.saveSignInDetails(userName, userEmail, password, props);
                userProgram = dialog.getProgram();
                sandboxSelected = dialog.isSandboxSelected();
                signinResult = SigninResult.SUCCESS;
            } else {
                // Couldn't get to Cognito; only have user's email.
                String email = dialog.getEmail();
                if (StringUtils.isEmpty(email)) {
                    // And if we don't even have an email, declare failure.
                    signinResult = SigninResult.FAILURE;
                } else if (savedSignInDetails != null && savedSignInDetails.email.equalsIgnoreCase(
                    // If the email is the same as the recently saved email, use those saved details.
                    email)) {
                    userName = savedSignInDetails.identity;
                    userEmail = savedSignInDetails.email;
                    authenticationInfo = identityPersistence.getExtraProperties();
                    signinResult = SigninResult.CACHED_OFFLINE;
                    if (authenticationInfo.containsKey("programs")) {
                        userPrograms = parseProgramList(authenticationInfo.get("programs"));
                    }
                    userProgram = dialog.getProgram();
                    sandboxSelected = dialog.isSandboxSelected();
                    signinResult = SigninResult.SUCCESS;
                } else {
                    // If some new email with no saved details, use what we have.
                    userName = email;
                    userEmail = email;
                    userProgram = dialog.getProgram();
                    sandboxSelected = dialog.isSandboxSelected();
                    signinResult = SigninResult.OFFLINE;
                }
            }
        } else {
            // User cancelled the dialog.
            identityPersistence.clearSignInDetails();
            signinResult = SigninResult.FAILURE;
        }

        return signinResult;
    }

    private void onAuthenticated(String jwtToken) {
        authenticationInfo = new HashMap<>();
        JSONObject payload = CognitoJWTParser.getPayload(jwtToken);
        for (Object k : payload.keySet()) {
            authenticationInfo.put(k.toString(), payload.get(k).toString());
        }
        String provider = authenticationInfo.get("iss").replace("https://", "");
        credentials = cognitoHelper.GetCredentials(provider, jwtToken);

        userName = authenticationInfo.get("cognito:username");
        userEmail = authenticationInfo.get("email");
        if (authenticationInfo.containsKey("programs")) {
            userPrograms = parseProgramList(authenticationInfo.get("programs"));
        }
    }

    private Map<String, String> parseProgramList(String list) {
        Map<String, String> result;
        String[] programs = list.split(";");
        result = stream(programs).map(s -> s.split(":"))
            .collect(Collectors.toMap(e -> e[0], e -> e[1]));
        return result;
    }

    /**
     * Signs out. Not used.
     */
    @SuppressWarnings("unused")
    public void doSignoutAndForgetUser() {
        // Since "signing in" merely obtains credentials for use in future calls, signing out
        // is a matter of forgetting the credentials.
        credentials = null;
        authenticationInfo = null;
        userEmail = null;
        userName = null;
        identityPersistence.clearSignInDetails();
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
            BiConsumer<Long, Long> progressHandler)
        {
            if (!isAuthenticated()) return false;
            AmazonS3 s3Client = getS3Client();
            long bytesExpected, bytesDownloaded = 0;

            try (S3Object s3Object = s3Client.getObject(new GetObjectRequest(bucket, key));
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

        /**
         * Make a REST call with the current signed-in credentials. We use this to make Lambda calls.
         *
         * @param requestURL URL to request.
         * @return result in a JSON object.
         */
        public JSONObject authenticatedRestCall(String requestURL) {
            if (!isAuthenticated()) return null;
            checkSession();
            Map<String,String> headers = new LinkedHashMap<>();
            headers.put("Accept", "application/json");
            headers.put("Content-Type", "text/plain");
            String idToken = authenticationResult.getIdToken();
            headers.put("Authorization", idToken);

            HttpUtility httpUtility = new HttpUtility();
            JSONObject jsonResponse = null;
            try {
                httpUtility.sendGetRequest(requestURL, headers);
                jsonResponse = httpUtility.readJSONObject();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            httpUtility.disconnect();
            return jsonResponse;
        }

    }

    /**
     * Wrapper class to encapsulate the Cognito helpers. These are provided to the sign-in /
     * sign-up dialog only.
     */
    public class CognitoInterface {
        public boolean isOnline() {
            return Authenticator.this.isOnline();
        }

        /**
         * Given a username (or email) and password, attempt to sign-in to Cognito. Sets members
         * authenticationResult with the results of the sign-in attempt.
         * authenticationInfo with the info returned by a successful sign-in.
         * credentials with the credentials returned by a successful sign-in.
         *
         * @param username or email address.
         * @param password of the user id.
         */
        public void authenticate(String username, String password) {
            AuthenticationHelper.AuthenticationResult validationResult = cognitoHelper.ValidateUser(username, password);
            parseAuthenticationResult(validationResult);
        }

        private void parseAuthenticationResult(AuthenticationHelper.AuthenticationResult authenticationResult) {
            Authenticator.this.authenticationResult = authenticationResult;
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
         * @param username that needs a password reset.
         */
        public void resetPassword(String username) {
            cognitoHelper.ResetPassword(username);
        }

        /**
         * Completes a password reset. Given a user id and the reset code sent to the user id's
         * associated email address, and a new password, sets the password to the new value (if
         * it is a valid password, of course).
         *
         * @param username of the password to reset.
         * @param password the new password.
         * @param pin      reset code sent via email.
         */
        public void updatePassword(String username, String password, String pin) {
            cognitoHelper.UpdatePassword(username, password, pin);
        }

        public String signUpUser(String username,
            String password,
            String email,
            String phonenumber)
        {
            return cognitoHelper.SignUpUser(username, password, email, phonenumber);
        }

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

        public boolean isSdkClientException() {
            return authenticationResult != null && authenticationResult.isSdkClientException();
        }

        public Map<String, String> getPrograms() {
            return userPrograms;
        }

        /**
         * @return "Authenticated" or the failure message from authentication.
         */
        public String getAuthMessage() {
            if (authenticationResult == null) return null;
            return authenticationResult.getMessage();
        }

    }
}
