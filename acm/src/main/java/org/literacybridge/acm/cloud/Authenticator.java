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
import org.json.simple.JSONValue;
import org.literacybridge.acm.Constants;
import org.literacybridge.acm.cloud.AuthenticationDialog.WelcomeDialog;
import org.literacybridge.acm.cloud.cognito.AuthenticationHelper;
import org.literacybridge.acm.cloud.cognito.CognitoHelper;
import org.literacybridge.acm.cloud.cognito.CognitoJWTParser;
import org.literacybridge.acm.config.AccessControl;
import org.literacybridge.acm.config.AmplioHome;
import org.literacybridge.acm.config.HttpUtility;
import org.literacybridge.acm.gui.Application;

import java.awt.Window;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Helper class to provide a simpler interface to cognito authentication.
 */
public class Authenticator {
    private static final Logger LOG = Logger.getLogger(Authenticator.class.getName());

    public static final List<String> ALL_ACM_ROLES = Arrays.asList("manage_deployment", "manage_playlist", "manage_prompt", "manage_content", "manage_checkout", "deploy_content");
    public static final String FIELD_OFFICER_ROLE_STRING = "FO";
    public final static Set<String> UPDATING_ROLES = new HashSet<>(ALL_ACM_ROLES);
    public final static Set<String> ALL_USER_ROLES = new HashSet<>(ALL_ACM_ROLES);

    public static final String ACCESS_CONTROL_API = Constants.API_URL + "/acm";
    // public static final String TBL_HELPER_API= "https://1rhce42l9a.execute-api.us-west-2.amazonaws.com/prod";
    public static final String PROGRAMS_INFO_API = Constants.API_URL +"/programs?for-acm=true";
    public static final String UF_KEYS_HELPER_API = "https://l9gt6xbtm5.execute-api.us-west-2.amazonaws.com/prod";

    public static final String PROGRAMS_INFO_FILE = "programs.info";


    private static Authenticator instance;
    private LoginResult loginResult = LoginResult.NONE;
    private String defaultProgram;
    private Set<LoginOptions> loginOptions;
    private String applicationName;
    private String computerName;

    public static synchronized Authenticator getInstance() {
        if (instance == null) {
            instance = new Authenticator();
        }
        return instance;
    }

    public static synchronized void setDebugInstance(Authenticator authenticator) {
        instance = authenticator;
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

    private String userEmail;
    // { program : friendly-name }
    private Map<String, String> programNames;
    // { program : roles-string }
    private Map<String, String> programsAndRolesForUser = new HashMap<>();
    private String selectedProgramid;
    private boolean sandboxSelected = false;

    // Cached helpers. Ones not used internally are lazy allocated.
    private final IdentityPersistence identityPersistence;
    private TbSrnHelper tbSrnHelper = null;
    private ProjectsHelper projectsHelper = null;

    // Programs available locally. This is initially programs *found* locally.
    // Programs available in local storage. A Map of {programid: name} May be backed by s3.
    private Map<String, String> locallyAvailablePrograms = new HashMap<>();
    // Programs configured for cloud storage in S3.
    private List<String> s3RepositoryList;

    protected Authenticator() {
        try {
            computerName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException ignored) {
        }
        this.identityPersistence = new IdentityPersistence(AmplioHome.getDirectory());
        cognitoHelper = new CognitoHelper();
    }

    /**
     * Gets a list of the programs that can be opened locally, ie, with no internet connection.
     * NOTE: Filters out programs that happen to be locally in Dropbox, but have been converted to S3
     * in the cloud.
     * @return a list of programs that can be opened locally.
     */
    public List<String> getLocallyAvailablePrograms() {
        return locallyAvailablePrograms.keySet().stream()
            .filter(this::isProgramS3)
            .collect(Collectors.toList());

    }

    /**
     * Informs the authenticator about locally available programs (ie, those on the local drive).
     * @param locallyAvailablePrograms A map of locally available program ids and their friendly names.
     */
    public void setLocallyAvailablePrograms(Map<String, String> locallyAvailablePrograms) {
        this.locallyAvailablePrograms = locallyAvailablePrograms;
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

    public String getUserContact() {
        String contact = userEmail;
        if (authenticationInfo != null) {
            String ph = authenticationInfo.get("phone_number");
            if (ph != null)
                contact += ", " + ph;
        }
        return contact;
    }
    public String getUserSelfName() {
        String[] SELF_NAME_PROPERTIES = {"custom:greeting", "nickname", "name", "cognito:username"};
        String name = null;
        if (authenticationInfo != null) {
            for (String prop : SELF_NAME_PROPERTIES) {
                name = authenticationInfo.get(prop);
                if (name != null)
                    break;
            }
        }
        return name;
    }

    public String getUserName() {
        return getUserSelfName();
    }

    public String getUserEmail() {
        return userEmail;
    }

    public String getSelectedProgramid() {
        return selectedProgramid;
    }

    /**
     * Gets the list of roles, if any, that are defined for the user in the selected program.
     * This will be empty if offline.
     * @return the set of roles.
     */
    public Set<String> getUserRolesInSelectedProgram() {
        Set<String> result = new HashSet<>();
        String roleStr = programsAndRolesForUser.get(selectedProgramid);
        if (roleStr != null) {
            String[] roles = roleStr.split(",");
            result.addAll(Arrays.asList(roles));
        }
        return result;
    }

    public boolean hasUpdatingRole() {
        // We can't do updates when offline.
        if (!isAuthenticated()) return false;
        Set<String> userRoles = getUserRolesInSelectedProgram();
        userRoles.retainAll(UPDATING_ROLES);
        return !userRoles.isEmpty();
    }

    public boolean isSandboxSelected() {
        return this.sandboxSelected;
    }

    public boolean isProgramS3(String program) {
        return s3RepositoryList != null && s3RepositoryList.contains(program);
    }

    public TbSrnHelper getTbSrnHelper() {
        if (tbSrnHelper == null) {
            if (!loginResult.signedIn()) {
                throw new IllegalStateException("Must login first.");
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

    public enum LoginResult {
        NONE, FAILURE, SUCCESS, CACHED_OFFLINE, OFFLINE;

        boolean signedIn() {
            return this == SUCCESS || this == CACHED_OFFLINE || this == OFFLINE;
        }
    }

    public enum LoginOptions {OFFLINE_EMAIL_CHOICE,
        CHOOSE_PROGRAM,
        LOCAL_DATA_ONLY,
        LOCAL_OR_S3(LOCAL_DATA_ONLY),
        OFFER_DEMO_MODE,
        SUGGEST_DEMO_MODE,
        INCLUDE_FB_ACMS,
        INCLUDE_FOUND_ACMS,
        NO_WAIT,
        NOP; // to alternate with another option: isFoo ? XYZ : NOP

        private List<LoginOptions> incompatible;
        LoginOptions() { }
        LoginOptions(LoginOptions... incompatible) {
            this.incompatible = Arrays.asList(incompatible);
        }
        boolean isCompatible(LoginOptions... toTest) {
            if (incompatible == null) return true;
            for (LoginOptions t : toTest) {
                if (incompatible.contains(t))
                    return false;
            }
            return true;
        }
        static boolean allCompatible(LoginOptions... options) {
            for (LoginOptions so : options) {
                if (!so.isCompatible(options)) {
                    return false;
                }
            }
            return true;
        }
        static String getIncompatibles(LoginOptions... options) {
            List<String> result = new ArrayList<>();
            for (LoginOptions so : options) {
                if (so.incompatible != null) {
                    for (LoginOptions other : options) {
                        if (so.incompatible.contains(other)) {
                            result.add(so.name() + " and " + other.name());
                        }
                    }
                }
            }
            return String.join(", ", result);
        }
    }

    /**
     * Determine who the user is. If we can access an authentication server, the user must
     * authenticate. (We need the user to be authenticated in order to check for new content,
     * or to download any new content found.) If we can not, we simply ask for their email
     * address, defaulting to the email of the last user who successfully authenticated.
     *
     * @param parent      window for the dialog.
     * @param applicationName the name of the application authenticating (for credentials dialog)
     * @param loginFlags options for the sign-in.
     * @return a LoginResult from the process.
     */
    public LoginResult authenticateAndChooseProgram(Window parent, String applicationName, String defaultProgram, LoginOptions... loginFlags) {
        if (!LoginOptions.allCompatible(loginFlags)) {
            throw new IllegalArgumentException("Incompatible options specified: " + LoginOptions.getIncompatibles(loginFlags));
        }
        this.applicationName = applicationName;

        UpdatePrompter.go();

        loginOptions = new HashSet<>(Arrays.asList(loginFlags));
        if (loginOptions.contains(LoginOptions.SUGGEST_DEMO_MODE)) {
            loginOptions.add(LoginOptions.OFFER_DEMO_MODE);
        }
        IdentityPersistence.LoginDetails savedLoginDetails = identityPersistence.retrieveLoginDetails();
        this.defaultProgram = defaultProgram;
        loginResult = LoginResult.NONE;

        WelcomeDialog dialog = new WelcomeDialog(parent, applicationName, defaultProgram, loginOptions, cognitoInterface);
        dialog.setIconImage(Application.getImageIcon());
        if (savedLoginDetails != null) {
            dialog.setSavedCredentials(savedLoginDetails.email,
                savedLoginDetails.secret);
        }
        dialog.setVisible(true);

        if (dialog.isSuccess()) {
            if (isAuthenticated()) {
                // Authenticated with Cognito.
                String password = dialog.isRememberMeSelected() ? dialog.getPassword() : null;
                // These are the values that we get from the Cognito login. As of 2019-12-24
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
                Map<String, String> props = new HashMap<>(authenticationInfo);
                identityPersistence.saveLoginDetails(userEmail, password, props);
                selectedProgramid = dialog.getProgram();
                sandboxSelected = dialog.isSandboxSelected();
                loginResult = LoginResult.SUCCESS;
            } else {
                // Couldn't get to Cognito; only have user's email.
                String email = dialog.getEmail();
                if (StringUtils.isEmpty(email)) {
                    // And if we don't even have an email, declare failure.
                    loginResult = LoginResult.FAILURE;
                } else if (savedLoginDetails != null && savedLoginDetails.email.equalsIgnoreCase(email)) {
                    // If the email is the same as the recently saved email, use those saved details.
                    userEmail = savedLoginDetails.email;
                    authenticationInfo = identityPersistence.getExtraProperties();
                    loginResult = LoginResult.CACHED_OFFLINE;
                    selectedProgramid = dialog.getProgram();
                    readProgramsInfo();
                    sandboxSelected = dialog.isSandboxSelected();
                    loginResult = LoginResult.SUCCESS;
                } else {
                    // If some new email with no saved details, use what we have.
                    userEmail = email;
                    selectedProgramid = dialog.getProgram();
                    readProgramsInfo();
                    sandboxSelected = dialog.isSandboxSelected();
                    loginResult = LoginResult.OFFLINE;
                }
            }
        } else {
            // User cancelled the dialog.
            loginResult = LoginResult.FAILURE;
        }

        return loginResult;
    }

    @SuppressWarnings("unchecked")
    private void onAuthenticated(String jwtToken) {
        JSONObject payload = CognitoJWTParser.getPayload(jwtToken);
        @SuppressWarnings("unchecked")
        Set<Map.Entry<Object,Object>> payloadEntries = payload.entrySet();
        authenticationInfo = payloadEntries.stream().collect(Collectors.toMap(e->e.getKey().toString(), e->e.getValue().toString()));

        String provider = authenticationInfo.get("iss").replace("https://", "");
        credentials = cognitoHelper.GetCredentials(provider, jwtToken);

        userEmail = authenticationInfo.get("email");

        JSONObject jsonResponse = awsInterface.authenticatedGetCall(PROGRAMS_INFO_API);
        try {
            JSONObject programsInfo = (JSONObject) jsonResponse.get("result");
            parseProgramsInfoJSON(programsInfo);
            writeProgramsInfoJSON(programsInfo);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Writes a JSONObject to the ~/Amplio/programs.info file or ~/LiteracyBridge/programs.info.
     * @param programsInfo to be written.
     */
    private void writeProgramsInfoJSON(JSONObject programsInfo) {
        String resultString = programsInfo.toJSONString();
        JSONObject parsedString = (JSONObject)JSONValue.parse(resultString);
        assert(parsedString.equals(programsInfo));

        File programsInfoFile = new File(AmplioHome.getDirectory(), PROGRAMS_INFO_FILE);
        try (FileWriter fw = new FileWriter(programsInfoFile)) {
            fw.write(resultString);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Reads a JSONObject from the ~/Amplio/programs.info file or ~/LiteracyBridge/programs.info. Parses
     * the read JSONObject.
     */
    private void readProgramsInfo() {
        File programsInfoFile = new File(AmplioHome.getDirectory(), PROGRAMS_INFO_FILE);
        try (FileReader fr = new FileReader(programsInfoFile)) {
            JSONObject programsInfo = (JSONObject) JSONValue.parse(fr);
            parseProgramsInfoJSON(programsInfo);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Parses a JSONObject of programs info. Populates 'userProgramsAndRoles', 's3RepositoryList', and 'programNames'
     * @param programsInfo to be parsed.
     */
    @SuppressWarnings("unchecked")
    private void parseProgramsInfoJSON(JSONObject programsInfo) {
        Map<String,Map<String,String>> programs_info = new HashMap<>();
        String implicit_repository = programsInfo.getOrDefault("implicit_repository", "dbx").toString();
        ((JSONObject) programsInfo.get("programs")).forEach((key, value) -> {
            Set<Map.Entry<Object,Object>> valueEntries = ((JSONObject)value).entrySet();
            Map<String,String> program_info = valueEntries.stream()
                .collect(Collectors.toMap(e->e.getKey().toString(), e->e.getValue().toString()));
            programs_info.put(key.toString(), program_info);
        });

        // Build map of {programid: "role,role"}
        programsAndRolesForUser = programs_info.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e->e.getValue().get("roles")));
        // Build a list of [programid] of those programs with s3 as repository.
        s3RepositoryList = programs_info.entrySet().stream()
            .filter(e->e.getValue().getOrDefault("repository", implicit_repository).equalsIgnoreCase("s3"))
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
        // Extract the friendly names as {programid: "name"}
        programNames = programs_info.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e->e.getValue().getOrDefault("name", e.getKey())));

        // If the "defaultProgram" is not an S3 program, then it is strictly local to the user's computer.
        // If the defaultProgram is not configured (on the server) for this user, simulate a FO
        // role in that program, so that it is possible to open (but not update) it.
        if (StringUtils.isNotBlank(defaultProgram) && !programsAndRolesForUser.containsKey(defaultProgram)
                && !s3RepositoryList.contains(defaultProgram)) {
            programsAndRolesForUser.put(defaultProgram, FIELD_OFFICER_ROLE_STRING);
            // We could look in the program's properties.config for the friendly name. But, since the user
            // had to pass the programid on the command line, it is probably slightly more user-friendly to
            // simply show the programid, and it is certainly cheaper.
            programNames.put(defaultProgram, defaultProgram);
        }

        if (loginOptions.contains(LoginOptions.INCLUDE_FOUND_ACMS)) {
            // Add any locally available programs that werern't in the user's list, but
            // add them as read-only.
            locallyAvailablePrograms.keySet().stream()
                .filter(p -> !programsAndRolesForUser.containsKey(p))
                .forEach(p -> {
                    programsAndRolesForUser.put(p, FIELD_OFFICER_ROLE_STRING);
                    programNames.put(p, p);
                });
        }
    }

    /**
     * Signs out. Not used.
     */
    @SuppressWarnings("unused")
    public void doSignoutAndForgetUser() {
        // Since "logging in" merely obtains credentials for use in future calls, logging out
        // is a matter of forgetting the credentials.
        credentials = null;
        authenticationInfo = null;
        userEmail = null;
        identityPersistence.clearLoginDetails();
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
            return downloadS3Object(new GetObjectRequest(bucket, key), of, progressHandler);
        }

        public boolean downloadS3Object(GetObjectRequest request, File of, BiConsumer<Long, Long> progressHandler) {
            if (!isAuthenticated()) return false;
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
                }
                else if (verb.equals("GET")) {
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
         * @param body JSON object to pass as request body.
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
            return Authenticator.this.isOnline();
        }

        /**
         * Given a username (or email) and password, attempt to sign-in to Cognito. Sets members
         * authenticationResult with the results of the sign-in attempt.
         * authenticationInfo with the info returned by a successful sign-in.
         * credentials with the credentials returned by a successful sign-in.
         *
         * @param usernameOrEmail username or email address.
         * @param password of the user id.
         */
        public void authenticate(String usernameOrEmail, String password) {
            Map<String,String> userMetadata = new HashMap<>();
            userMetadata.put("Application", applicationName);
            if (StringUtils.isNotBlank(computerName)) userMetadata.put("Computer", computerName);
            AuthenticationHelper.AuthenticationResult validationResult = cognitoHelper.ValidateUser(usernameOrEmail, password, userMetadata);

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
         * @param password the new password.
         * @param pin      reset code sent via email.
         */
        public void updatePassword(String usernameOrEmail, String password, String pin) {
            cognitoHelper.UpdatePassword(usernameOrEmail, password, pin);
        }

        /**
         * Called in response to a NEW_PASSWORD_REQUIRED result. This sets the new password, and completes the login.
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
            Map<String,String> attributes)
        {
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


        public Map<String, String> getProgramsAndRolesForUser() {
            return programsAndRolesForUser;
        }

        public Map<String, String> getProgramNames() {
            if (programNames == null) {
                readProgramsInfo();
            }
            return programNames;
        }

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
