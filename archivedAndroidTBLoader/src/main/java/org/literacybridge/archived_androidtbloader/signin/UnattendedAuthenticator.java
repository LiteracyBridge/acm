package org.literacybridge.archived_androidtbloader.signin;

import android.util.Log;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoDevice;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUser;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserSession;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.AuthenticationContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.ChallengeContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.MultiFactorAuthenticationContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.AuthenticationHandler;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.GenericHandler;
import org.literacybridge.archived_androidtbloader.TBLoaderAppContext;
import org.literacybridge.archived_androidtbloader.util.Constants;

public class UnattendedAuthenticator {
    private static final String TAG = "TBL!:" + UnattendedAuthenticator.class.getName();

    private static final TBLoaderAppContext sTbLoaderAppContext = TBLoaderAppContext.getInstance();
    private final GenericHandler handler;

    public UnattendedAuthenticator(GenericHandler handler) {
        this.handler = handler;
        Log.d(TAG, "ctor, initializing user helper");
        UserHelper.initInstance(sTbLoaderAppContext, Constants.cognitoConfig);
        Log.d(TAG, "ctor, user helper initialized");
    }

    // Login if a user is already present
    public void authenticate() {
        Log.d(TAG, "authenticate: getting current user");
        CognitoUser user = UserHelper.getInstance().getPool().getCurrentUser();
        if (user.getUserId() == null) {
            Log.e(TAG, "authenticate: no current user");
            handler.onFailure(new IllegalStateException("No cached current user"));
        } else if (!sTbLoaderAppContext.isCurrentlyConnected()) {
            Log.d(TAG, "authenticate: not connected");
            handler.onFailure(new IllegalStateException("Not currently connected"));
        } else {
            Log.d(TAG, String.format("authenticate: calling getSessionInBackground for user %s", user.getUserId()));
            UserHelper.getInstance().setUserId(user.getUserId());
            // TODO: Handle fallback
            user.getSessionInBackground(authenticationHandler);
        }
    }

    AuthenticationHandler authenticationHandler = new AuthenticationHandler() {
        @Override
        public void onSuccess(CognitoUserSession cognitoUserSession, final CognitoDevice device) {
            Log.d(TAG, "authenticationHandler: Success");
            UserHelper.getInstance().setCurrSession(sTbLoaderAppContext, cognitoUserSession, new Runnable() {
                @Override
                public void run() {
                    handler.onSuccess();
                }
            });
        }

        @Override
        public void getAuthenticationDetails(AuthenticationContinuation authenticationContinuation, String username) {
            Log.e(TAG, "getAuthenticationDetails called: not supported");
            handler.onFailure(new IllegalStateException("getAuthenticationDetails not supported"));
        }

        @Override
        public void getMFACode(MultiFactorAuthenticationContinuation multiFactorAuthenticationContinuation) {
            Log.e(TAG, "getMFACode called: not supported");
            handler.onFailure(new IllegalStateException("getMFACode not supported"));
        }

        @Override
        public void onFailure(Exception e) {
            Log.e(TAG, "Authentication failed.", e);
            handler.onFailure(e);
        }

        @Override
        public void authenticationChallenge(ChallengeContinuation continuation) {
            Log.e(TAG, "authenticationChallenge called: not supported");
            handler.onFailure(new IllegalStateException("authenticationChallenge not supported"));
        }
    };


}
