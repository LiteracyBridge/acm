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
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoDevice;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserPool;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserSession;
import com.amazonaws.regions.Region;
import com.amazonaws.services.cognitoidentityprovider.AmazonCognitoIdentityProvider;
import com.amazonaws.services.cognitoidentityprovider.AmazonCognitoIdentityProviderClient;
import com.amazonaws.services.cognitoidentityprovider.model.GetUserRequest;
import com.amazonaws.services.cognitoidentityprovider.model.GetUserResult;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.literacybridge.androidtbloader.util.Constants;

import java.io.UnsupportedEncodingException;
import java.security.InvalidParameterException;
import java.util.*;

import static android.util.Base64.DEFAULT;

public class UserHelper {
    private static final String TAG= "TBL!:" + "UserHelper";
    // App settings

    private static List<String> attributeDisplaySeq;
    private static Map<String, String> signUpFieldsC2O;
    private static Map<String, String> signUpFieldsO2C;

    private static UserHelper appHelper;
    private static CognitoUserPool userPool;
    /**
     * This is the actual, unique, non-alias user id in the Cognito User Pool. Their official
     * term for this value is "username".
     */
    private static String username;
    private static String user;

    private static List<ItemToDisplay> firstTimeLogInDetails;
    private static Map<String, String> firstTimeLogInUserAttributes;
    private static List<String> firstTimeLogInRequiredAttributes;
    private static int firstTimeLogInItemsCount;
    private static Map<String, String> firstTimeLogInUpDatedAttributes;
    private static String firstTimeLoginNewPassword;

    // Change the next three lines of code to run this demo on your user pool

    // The session describes this signon.
    private static CognitoUserSession currSession;

    private static AmazonCognitoIdentityProvider cipClient;
    private static CognitoCredentialsProvider credentialsProvider;

    // User details to display - they are the current values, including any local modification
    private static boolean phoneVerified;
    private static boolean emailVerified;

    private static Map<String,String> mAuthenticationPayload;

    public static void init(Context context) {
        setData();

        if (appHelper != null && userPool != null) {
            return;
        }

        if (appHelper == null) {
            appHelper = new UserHelper();
        }

        if (userPool == null) {

            // Create a user pool with default ClientConfiguration
            //userPool = new CognitoUserPool(context, Constants.COGNITO_USER_POOL_ID, Constants.COGNITO_APP_CLIENT_ID, Constants.COGNITO_APP_SECRET, Constants.COGNITO_REGION);

            // This will also work
            //*
            ClientConfiguration clientConfiguration = new ClientConfiguration();
            cipClient = new AmazonCognitoIdentityProviderClient(new AnonymousAWSCredentials(), clientConfiguration);
            cipClient.setRegion(Region.getRegion(Constants.COGNITO_REGION));
            userPool = new CognitoUserPool(context, Constants.COGNITO_USER_POOL_ID, Constants.COGNITO_APP_CLIENT_ID, Constants.COGNITO_APP_SECRET, cipClient);
            // */

        }

        phoneVerified = false;
        emailVerified = false;

        Set<String> currUserAttributes = new HashSet<>();
        List<ItemToDisplay> trustedDevices = new ArrayList<>();
        firstTimeLogInDetails = new ArrayList<>();
        firstTimeLogInUpDatedAttributes= new HashMap<>();

        CognitoDevice thisDevice = null;
        boolean thisDeviceTrustState = false;
    }

    public static CognitoCredentialsProvider getCredentialsProvider(Context context) {
        if (credentialsProvider == null) {
            credentialsProvider = new CognitoCachingCredentialsProvider(
                    context.getApplicationContext(),
                    Constants.COGNITO_IDENTITY_POOL_ID,
                    Constants.COGNITO_REGION);
        }
        return credentialsProvider;
    }

    public static CognitoUserPool getPool() {
        return userPool;
    }

    public static Map<String, String> getSignUpFieldsC2O() {
        return signUpFieldsC2O;
    }

    public static void setCurrSession(Context applicationContext, CognitoUserSession session, final Runnable done) {
        currSession = session;
        mAuthenticationPayload = null;
        Map<String,String> payload = getAuthenticationPayload();
        credentialsProvider = getCredentialsProvider(applicationContext);

        Map<String, String> logins = new HashMap<>();
        logins.put(Constants.COGNITO_USER_POOL_LOGIN_STRING, currSession.getIdToken().getJWTToken());
        credentialsProvider.setLogins(logins);

        // May need to credentialsProvider.refresh(); use AsyncTask<> to do so.
        new AsyncTask<Void, Void, Void>() {

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
                getUsernameFromSession(done);
            }
        }.execute();
    }

    private static void getUsernameFromSession(final Runnable done) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                GetUserRequest getUserRequest = new GetUserRequest();
                getUserRequest.setAccessToken(currSession.getAccessToken().getJWTToken());
                GetUserResult userResult = cipClient.getUser(getUserRequest);
                username = userResult.getUsername();
                return null;
            }
            @Override
            protected void onPostExecute(Void result) {
                Log.d(TAG, String.format("Retrieved user name: %s", username));
                done.run();
            }
        }.execute();
    }

    public static String getUsername() {
        return username;
    }

    public static  CognitoUserSession getCurrSession() {
        return currSession;
    }

    public static String getJwtToken() {
        return currSession == null ? null : currSession.getIdToken().getJWTToken();
    }
    public static String getAccessToken() {
        return currSession == null ? null : currSession.getAccessToken().getJWTToken();
    }
    public static String getRefreshToken() {
        return currSession == null ? null : currSession.getRefreshToken().getToken();
    }

    public static String getUserId() {
        return user;
    }

    public static void setUserId(String newUser) {
        user = newUser;
    }

    public static String formatException(Exception exception) {
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

    public static int getFirstTimeLogInItemsCount() {
        return  firstTimeLogInItemsCount;
    }

    public static ItemToDisplay getUserAttributeForFirstLogInCheck(int position) {
        return firstTimeLogInDetails.get(position);
    }

    public static void setUserAttributeForDisplayFirstLogIn(Map<String, String> currAttributes, List<String> requiredAttributes) {
        firstTimeLogInUserAttributes = currAttributes;
        firstTimeLogInRequiredAttributes = requiredAttributes;
        firstTimeLogInUpDatedAttributes = new HashMap<>();
        refreshDisplayItemsForFirstTimeLogin();
    }

    public static void setUserAttributeForFirstTimeLogin(String attributeName, String attributeValue) {
        if (firstTimeLogInUserAttributes ==  null) {
            firstTimeLogInUserAttributes = new HashMap<>();
        }
        firstTimeLogInUserAttributes.put(attributeName, attributeValue);
        firstTimeLogInUpDatedAttributes.put(attributeName, attributeValue);
        refreshDisplayItemsForFirstTimeLogin();
    }

    public static Map<String, String> getUserAttributesForFirstTimeLogin() {
        return firstTimeLogInUpDatedAttributes;
    }

    public static void setPasswordForFirstTimeLogin(String password) {
        firstTimeLoginNewPassword = password;
    }

    public static String getPasswordForFirstTimeLogin() {
        return firstTimeLoginNewPassword;
    }

    private static void refreshDisplayItemsForFirstTimeLogin() {
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

    public static void newDevice(CognitoDevice device) {
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
        attributeDisplaySeq.add("custom:greeting");

        signUpFieldsC2O = new HashMap<>();
//        signUpFieldsC2O.put("Given name", "given_name");
//        signUpFieldsC2O.put("Family name", "family_name");
//        signUpFieldsC2O.put("Nick name", "nickname");
        signUpFieldsC2O.put("Phone number", "phone_number");
        signUpFieldsC2O.put("Phone number verified", "phone_number_verified");
        signUpFieldsC2O.put("Email verified", "email_verified");
        signUpFieldsC2O.put("Email","email");
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
//        signUpFieldsO2C.put("middle_name", "Middle name");
        signUpFieldsO2C.put("custom:greeting", "Preferred Greeting");

    }

    private static void refreshWithSync() {
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

            if(attr.getKey().equals("email")) {
            }
            else if(attr.getKey().equals("phone_number")) {
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

    public static synchronized Map<String,String> getAuthenticationPayload() {
        if (mAuthenticationPayload == null) {
            JSONObject jsonPayload = getPayloadFromJwt(currSession.getIdToken().getJWTToken());
            Map<String,String> payload = new HashMap<>();
            Set<Map.Entry> eset = jsonPayload.entrySet();
            for (Map.Entry e : eset) {
                payload.put(e.getKey().toString(), e.getValue().toString());
            }
            if (payload.size() > 0) {
                mAuthenticationPayload = payload;
                refreshWithSync();
            }
        }
        return mAuthenticationPayload;
    }
    public static String getAuthenticationPayload(String key) {
        Map<String, String> props = getAuthenticationPayload();
        return props==null ? null : props.get(key);
    }

    private static final int HEADER = 0;
    private static final int PAYLOAD = 1;
    private static final int SIGNATURE = 2;
    private static final int JWT_PARTS = 3;
    /**
     * Returns payload of a JWT as a JSON object.
     *
     * @param jwt REQUIRED: valid JSON Web Token as String.
     * @return payload as a JSONObject.
     */
    public static JSONObject getPayloadFromJwt(String jwt) {
        try {
            validateJWT(jwt);
            final String payload = jwt.split("\\.")[PAYLOAD];
            final byte[] sectionDecoded = Base64.decode(payload, DEFAULT);
            final String jwtSection = new String(sectionDecoded, "UTF-8");
            return (JSONObject) JSONValue.parse(jwtSection);
        } catch (final UnsupportedEncodingException e) {
            throw new InvalidParameterException(e.getMessage());
        } catch (final Exception e) {
            throw new InvalidParameterException("error in parsing JSON");
        }
    }
    /**
     * Checks if {@code JWT} is a valid JSON Web Token.
     *
     * @param jwt REQUIRED: The JWT as a {@link String}.
     */
    private static void validateJWT(String jwt) {
        // Check if the the JWT has the three parts
        final String[] jwtParts = jwt.split("\\.");
        if (jwtParts.length != JWT_PARTS) {
            throw new InvalidParameterException("not a JSON Web Token");
        }
    }

}

