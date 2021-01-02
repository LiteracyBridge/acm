/*
 *  Copyright 2013-2016 Amazon.com,
 *  Inc. or its affiliates. All Rights Reserved.
 *
 *  Licensed under the Amazon Software License (the "License").
 *  You may not use this file except in compliance with the
 *  License. A copy of the License is located at
 *
 *      http://aws.amazon.com/asl/
 *
 *  or in the "license" file accompanying this file. This file is
 *  distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 *  CONDITIONS OF ANY KIND, express or implied. See the License
 *  for the specific language governing permissions and
 *  limitations under the License.
 */

package org.literacybridge.androidtbloader.signin;

import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AnonymousAWSCredentials;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.auth.CognitoCredentialsProvider;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserPool;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserSession;
import com.amazonaws.regions.Region;
import com.amazonaws.services.cognitoidentityprovider.AmazonCognitoIdentityProvider;
import com.amazonaws.services.cognitoidentityprovider.AmazonCognitoIdentityProviderClient;
import com.amazonaws.services.cognitoidentityprovider.model.GetUserRequest;
import com.amazonaws.services.cognitoidentityprovider.model.GetUserResult;

import org.json.JSONException;
import org.json.JSONObject;
import org.literacybridge.androidtbloader.util.Constants;

import java.nio.charset.StandardCharsets;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static android.util.Base64.DEFAULT;

public class UserHelper {
    private static final String TAG= "TBL!:" + "UserHelper";

    // App settings

    private static UserHelper instance;
    private boolean isFallbackInstance = false;
    public static UserHelper getInstance() {
        if (instance == null) {
            throw new IllegalStateException("User Helper has not been initialized.");
        }
        return instance;
    }
    public static synchronized UserHelper initInstance(Context context, Constants.CognitoConfig config) {
        if (instance == null) {
            instance = new UserHelper(config);
            instance.init(context);
        }
        return instance;
    }
    public static synchronized void setFallbackInstance(UserHelper fallbackInstance) {
        fallbackInstance.isFallbackInstance = true;
        instance = fallbackInstance;
    }
    public static synchronized UserHelper createInstance(Context context, Constants.CognitoConfig config) {
        UserHelper trialInstance = new UserHelper(config);
        trialInstance.init(context);
        return trialInstance;
    }

    private final Constants.CognitoConfig config;
    public UserHelper(Constants.CognitoConfig config) {
        this.config = config;
    }
    
    private static List<String> attributeDisplaySeq;
    private static Map<String, String> signUpFieldsC2O;
    private static Map<String, String> signUpFieldsO2C;

    private CognitoUserPool userPool;
    /**
     * This is the actual, unique, non-alias user id in the Cognito User Pool. Their official
     * term for this value is "username".
     */
    private String username;
    private String user;

    private List<ItemToDisplay> firstTimeLogInDetails;
    private Map<String, String> firstTimeLogInUserAttributes;
    private List<String> firstTimeLogInRequiredAttributes;
    private int firstTimeLogInItemsCount;
    private Map<String, String> firstTimeLogInUpDatedAttributes;
    private String firstTimeLoginNewPassword;

    // Change the next three lines of code to run this demo on your user pool

    // The session describes this signon.
    private CognitoUserSession currSession;

    private AmazonCognitoIdentityProvider cipClient;
    private CognitoCredentialsProvider credentialsProvider;

    // User details to display - they are the current values, including any local modification
    private boolean phoneVerified;
    private boolean emailVerified;

    private Map<String,String> mAuthenticationPayload;

    private void init(Context context) {
        if (attributeDisplaySeq == null) {
            setData();
        }

        // Create a user pool with default ClientConfiguration
        //userPool = new CognitoUserPool(context, config.COGNITO_USER_POOL_ID, config.COGNITO_APP_CLIENT_ID, config.COGNITO_APP_SECRET, config.COGNITO_REGION);

        // This will also work
        //*
        ClientConfiguration clientConfiguration = new ClientConfiguration();
        cipClient = new AmazonCognitoIdentityProviderClient(new AnonymousAWSCredentials(), clientConfiguration);
        cipClient.setRegion(Region.getRegion(config.COGNITO_REGION));
        userPool = new CognitoUserPool(context, config.COGNITO_USER_POOL_ID, config.COGNITO_APP_CLIENT_ID, config.COGNITO_APP_SECRET, cipClient);
        // */

        phoneVerified = false;
        emailVerified = false;

        firstTimeLogInDetails = new ArrayList<>();
        firstTimeLogInUpDatedAttributes= new HashMap<>();

    }

    public CognitoCredentialsProvider getCredentialsProvider(Context context) {
        if (credentialsProvider == null) {
            credentialsProvider = new CognitoCachingCredentialsProvider(
                    context.getApplicationContext(),
                    config.COGNITO_IDENTITY_POOL_ID,
                    config.COGNITO_REGION);
        }
        return credentialsProvider;
    }

    public Constants.CognitoConfig getConfig() {
        return config;
    }

    public CognitoUserPool getPool() {
        return userPool;
    }

    public Map<String, String> getSignUpFieldsC2O() {
        return signUpFieldsC2O;
    }

    public void setCurrSession(Context applicationContext, CognitoUserSession session, final Runnable done) {
        currSession = session;
        mAuthenticationPayload = null;
        getAuthenticationPayload();
        credentialsProvider = getCredentialsProvider(applicationContext);

        Map<String, String> logins = new HashMap<>();
        logins.put(config.COGNITO_USER_POOL_LOGIN_STRING, currSession.getIdToken().getJWTToken());
        credentialsProvider.setLogins(logins);

        new CredentialsRefresher(credentialsProvider, ()->new UserNameGetter(this, done).execute()).execute();
    }

    private static class CredentialsRefresher extends AsyncTask<Void, Void, Void> {
        final CognitoCredentialsProvider credentialsProvider;
        final Runnable onDone;

        private CredentialsRefresher(CognitoCredentialsProvider credentialsProvider, Runnable onDone) {
            this.credentialsProvider = credentialsProvider;
            this.onDone = onDone;
        }

        @Override
        protected Void doInBackground(Void... params) {
            Log.d(TAG, "Refreshing credentials in background.");
            try {
                credentialsProvider.refresh();
            } catch (Exception ex) {
                Log.d(TAG, "Caught exception in refresh", ex);
            }
            return null;
        }
        @Override
        protected void onPostExecute(Void result) {
            onDone.run();
        }
    }

    private static class UserNameGetter extends AsyncTask<Void, Void, Void> {
        final UserHelper helper;
        final Runnable done;

        private UserNameGetter(UserHelper helper, Runnable done) {
            this.helper = helper;
            this.done = done;
        }

        @Override
        protected Void doInBackground(Void... params) {
            GetUserRequest getUserRequest = new GetUserRequest();
            getUserRequest.setAccessToken(helper.currSession.getAccessToken().getJWTToken());
            GetUserResult userResult = helper.cipClient.getUser(getUserRequest);
            helper.username = userResult.getUsername();
            return null;
        }
        @Override
        protected void onPostExecute(Void result) {
            Log.d(TAG, String.format("Retrieved user name: %s", helper.username));
            done.run();
        }
    }


    public String getUsername() {
        return username;
    }

    public  CognitoUserSession getCurrSession() {
        return currSession;
    }

    public String getJwtToken() {
        return currSession == null ? null : currSession.getIdToken().getJWTToken();
    }
    @SuppressWarnings("unused")
    public String getAccessToken() {
        return currSession == null ? null : currSession.getAccessToken().getJWTToken();
    }
    @SuppressWarnings("unused")
    public String getRefreshToken() {
        return currSession == null ? null : currSession.getRefreshToken().getToken();
    }

    public String getUserId() {
        return user;
    }

    public void setUserId(String newUser) {
        user = newUser;
    }

    public String formatException(Exception exception) {
        String formattedString = "Internal Error";
        Log.e(TAG, exception.toString());
        Log.getStackTraceString(exception);

        String temp = exception.getMessage();

        if(temp != null && temp.length() > 0) {
            formattedString = temp.split("\\(")[0];
            if(temp != null && temp.length() > 0) {
                return formattedString;
            }
        }

        return  formattedString;
    }

    public int getFirstTimeLogInItemsCount() {
        return  firstTimeLogInItemsCount;
    }

    public ItemToDisplay getUserAttributeForFirstLogInCheck(int position) {
        return firstTimeLogInDetails.get(position);
    }

    public void setUserAttributeForDisplayFirstLogIn(Map<String, String> currAttributes, List<String> requiredAttributes) {
        firstTimeLogInUserAttributes = currAttributes;
        firstTimeLogInRequiredAttributes = requiredAttributes;
        firstTimeLogInUpDatedAttributes = new HashMap<>();
        refreshDisplayItemsForFirstTimeLogin();
    }

    public void setUserAttributeForFirstTimeLogin(String attributeName, String attributeValue) {
        if (firstTimeLogInUserAttributes ==  null) {
            firstTimeLogInUserAttributes = new HashMap<>();
        }
        firstTimeLogInUserAttributes.put(attributeName, attributeValue);
        firstTimeLogInUpDatedAttributes.put(attributeName, attributeValue);
        refreshDisplayItemsForFirstTimeLogin();
    }

    public Map<String, String> getUserAttributesForFirstTimeLogin() {
        return firstTimeLogInUpDatedAttributes;
    }

    public void setPasswordForFirstTimeLogin(String password) {
        firstTimeLoginNewPassword = password;
    }

    public String getPasswordForFirstTimeLogin() {
        return firstTimeLoginNewPassword;
    }

    private void refreshDisplayItemsForFirstTimeLogin() {
        firstTimeLogInItemsCount = 0;
        firstTimeLogInDetails = new ArrayList<>();

        for(Map.Entry<String, String> attr: firstTimeLogInUserAttributes.entrySet()) {
            if ("phone_number_verified".equals(attr.getKey()) || "email_verified".equals(attr.getKey())) {
                continue;
            }
            String message = "";
            if ((firstTimeLogInRequiredAttributes != null) && (firstTimeLogInRequiredAttributes.contains(attr.getKey()))) {
                message = "Required";
            }
            ItemToDisplay item = new ItemToDisplay(attr.getKey(), attr.getValue(), message, Color.BLACK, Color.DKGRAY, Color.parseColor("#329AD6"), 0, null);
            firstTimeLogInDetails.add(item);
            firstTimeLogInRequiredAttributes.size();
            firstTimeLogInItemsCount++;
        }

        for (String attr: firstTimeLogInRequiredAttributes) {
            if (!firstTimeLogInUserAttributes.containsKey(attr)) {
                ItemToDisplay item = new ItemToDisplay(attr, "", "Required", Color.BLACK, Color.DKGRAY, Color.parseColor("#329AD6"), 0, null);
                firstTimeLogInDetails.add(item);
                firstTimeLogInItemsCount++;
            }
        }
    }

    public void newDevice() {
    }


    private void refreshWithSync() {
        // This will refresh the current items to display list with the attributes fetched from service
        List<String> tempKeys = new ArrayList<>();
        List<String> tempValues = new ArrayList<>();

        emailVerified = false;
        phoneVerified = false;

        for(Map.Entry<String, String> attr: mAuthenticationPayload.entrySet()) {

            tempKeys.add(attr.getKey());
            tempValues.add(attr.getValue());

            if(attr.getKey().contains("email_verified")) {
                emailVerified = attr.getValue().contains("true");
            }
            else if(attr.getKey().contains("phone_number_verified")) {
                phoneVerified = attr.getValue().contains("true");
            }
        }

        // Arrange the input attributes per the display sequence
        Set<String> keySet = new HashSet<>(tempKeys);
        for(String det: attributeDisplaySeq) {
            if(keySet.contains(det)) {
                // Adding items to display list in the required sequence

                ItemToDisplay item = new ItemToDisplay(signUpFieldsO2C.get(det), tempValues.get(tempKeys.indexOf(det)), "",
                        Color.BLACK, Color.DKGRAY, Color.parseColor("#37A51C"),
                        0, null);

                if(det.contains("email")) {
                    if(emailVerified) {
                        item.setDataDrawable("checked");
                        item.setMessageText("Email verified");
                    }
                    else {
                        item.setDataDrawable("not_checked");
                        item.setMessageText("Email not verified");
                        item.setMessageColor(Color.parseColor("#E94700"));
                    }
                }

                if(det.contains("phone_number")) {
                    if(phoneVerified) {
                        item.setDataDrawable("checked");
                        item.setMessageText("Phone number verified");
                    }
                    else {
                        item.setDataDrawable("not_checked");
                        item.setMessageText("Phone number not verified");
                        item.setMessageColor(Color.parseColor("#E94700"));
                    }
                }
            }
        }
    }

    public synchronized Map<String,String> getAuthenticationPayload() {
        if (mAuthenticationPayload == null && currSession != null) {
            JSONObject jsonPayload = getPayloadFromJwt(currSession.getIdToken().getJWTToken());
            Map<String,String> payload = new HashMap<>();
            for (Iterator<String> it = jsonPayload.keys(); it.hasNext(); ) {
                String key = it.next();
                try {
                    String value = jsonPayload.getString(key);
                    payload.put(key, value);
                } catch (JSONException e) {
                    // Ignore the key. Should not happen IRL.
                }
            }
            if (payload.size() > 0) {
                mAuthenticationPayload = payload;
                refreshWithSync();
            }
        }
        return mAuthenticationPayload;
    }
    public String getAuthenticationPayload(String key) {
        Map<String, String> props = getAuthenticationPayload();
        return props==null ? null : props.get(key);
    }

    @SuppressWarnings("unused")
    private final int HEADER = 0;
    @SuppressWarnings("FieldCanBeLocal")
    private final int PAYLOAD = 1;
    @SuppressWarnings("unused")
    private final int SIGNATURE = 2;
    @SuppressWarnings("FieldCanBeLocal")
    private final int JWT_PARTS = 3;
    /**
     * Returns payload of a JWT as a JSON object.
     *
     * @param jwt REQUIRED: valid JSON Web Token as String.
     * @return payload as a JSONObject.
     */
    public JSONObject getPayloadFromJwt(String jwt) {
        try {
            validateJWT(jwt);
            final String payload = jwt.split("\\.")[PAYLOAD];
            final byte[] sectionDecoded = Base64.decode(payload, DEFAULT);
            final String jwtSection = new String(sectionDecoded, StandardCharsets.UTF_8);
            return new JSONObject(jwtSection);
        } catch (final Exception e) {
            throw new InvalidParameterException("error in parsing JSON");
        }
    }
    /**
     * Checks if {@code JWT} is a valid JSON Web Token.
     *
     * @param jwt REQUIRED: The JWT as a {@link String}.
     */
    private void validateJWT(String jwt) {
        // Check if the the JWT has the three parts
        final String[] jwtParts = jwt.split("\\.");
        if (jwtParts.length != JWT_PARTS) {
            throw new InvalidParameterException("not a JSON Web Token");
        }
    }

    private static void setData() {
        // Set attribute display sequence
        attributeDisplaySeq = new ArrayList<>();
//        attributeDisplaySeq.add("given_name");
//        attributeDisplaySeq.add("middle_name");
//        attributeDisplaySeq.add("family_name");
//        attributeDisplaySeq.add("nickname");
        attributeDisplaySeq.add("phone_number");
        attributeDisplaySeq.add("email");
        attributeDisplaySeq.add("name");
        attributeDisplaySeq.add("custom:greeting");

        signUpFieldsC2O = new HashMap<>();
//        signUpFieldsC2O.put("Given name", "given_name");
//        signUpFieldsC2O.put("Family name", "family_name");
//        signUpFieldsC2O.put("Nick name", "nickname");
        signUpFieldsC2O.put("Phone number", "phone_number");
        signUpFieldsC2O.put("Phone number verified", "phone_number_verified");
        signUpFieldsC2O.put("Email verified", "email_verified");
        signUpFieldsC2O.put("Email","email");
        signUpFieldsC2O.put("Name", "name");
//        signUpFieldsC2O.put("Middle name","middle_name");
        signUpFieldsC2O.put("Preferred Greeting","custom:greeting");

        signUpFieldsO2C = new HashMap<>();
//        signUpFieldsO2C.put("given_name", "Given name");
//        signUpFieldsO2C.put("family_name", "Family name");
//        signUpFieldsO2C.put("nickname", "Nick name");
        signUpFieldsO2C.put("phone_number", "Phone number");
        signUpFieldsO2C.put("phone_number_verified", "Phone number verified");
        signUpFieldsO2C.put("email_verified", "Email verified");
        signUpFieldsO2C.put("email", "Email");
        signUpFieldsO2C.put("name", "Name");
//        signUpFieldsO2C.put("middle_name", "Middle name");
        signUpFieldsO2C.put("custom:greeting", "Preferred Greeting");

    }

}

