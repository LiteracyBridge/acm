package org.literacybridge.androidtbloader.dropbox;

import android.accounts.AuthenticatorException;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;
import com.dropbox.client2.session.Session;
import com.dropbox.client2.session.TokenPair;

public class DropboxConnection {
    private static final String TAG = DropboxConnection.class.getSimpleName();

    private static final String APP_KEY = "oxb43a3l17014fp";
    private static final String APP_SECRET = "i29dz23rv342wtp";

    final static private String ACCOUNT_PREFS_NAME = "prefs";
    final static private String ACCESS_KEY_NAME = "ACCESS_KEY";
    final static private String ACCESS_SECRET_NAME = "ACCESS_SECRET";


    // If you'd like to change the access type to the full Dropbox instead of
    // an app folder, change this value.
    private static final Session.AccessType ACCESS_TYPE = Session.AccessType.DROPBOX;

    private DropboxAPI<AndroidAuthSession> mApi;

    private final Context mAppContext;

    public DropboxConnection(Context context) {
        mAppContext = context.getApplicationContext();
        // We create a new AuthSession so that we can use the Dropbox API.
        AndroidAuthSession session = buildSession();
        mApi = new DropboxAPI<AndroidAuthSession>(session);
    }

    public DropboxAPI<AndroidAuthSession> getApi() {
        return mApi;
    }

    public boolean isAuthenticated() {
        return mApi.getSession().getAccessTokenPair() != null;
    }

    public void connect(Context parent) {
        if (!isAuthenticated()) {
            mApi.getSession().startAuthentication(parent);
            mApi.getSession().isLinked();
        }
    }

    public boolean isConnected() {
        return isAuthenticated() && mApi.getSession().isLinked();
    }

    public void resumeAuthentication() throws DropboxException {
        AndroidAuthSession session = mApi.getSession();
        if (session.authenticationSuccessful()) {
            try {
                // Mandatory call to complete the auth
                session.finishAuthentication();

                // Store it locally in our app for later use
                TokenPair tokens = session.getAccessTokenPair();
                storeKeys(tokens.key, tokens.secret);
                Log.d(TAG, "key=" + tokens.key + ", secret=" + tokens.secret);
            } catch (IllegalStateException e) {
                throw new DropboxException(e);
            }
        }
    }

    public void clearAuthentication() {
        clearKeys();
        mApi.getSession().unlink();
        AndroidAuthSession session = buildSession();
        mApi = new DropboxAPI<AndroidAuthSession>(session);
    }

    public DropboxAPI.Account getAccountInfo() {
        try {
            return mApi.accountInfo();
        } catch (DropboxException e) {
            return null;
        }
    }

    private AndroidAuthSession buildSession() {
        AppKeyPair appKeyPair = new AppKeyPair(APP_KEY, APP_SECRET);
        AndroidAuthSession session;

        String[] stored = getKeys();
        if (stored != null) {
            AccessTokenPair accessToken = new AccessTokenPair(stored[0], stored[1]);
            session = new AndroidAuthSession(appKeyPair, ACCESS_TYPE, accessToken);
        } else {
            session = new AndroidAuthSession(appKeyPair, ACCESS_TYPE);
        }

        return session;
    }

    /**
     * @return Array of [access_key, access_secret], or null if none stored
     */
    private String[] getKeys() {
        SharedPreferences prefs = mAppContext.getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
        String key = prefs.getString(ACCESS_KEY_NAME, null);
        String secret = prefs.getString(ACCESS_SECRET_NAME, null);
        if (key != null && secret != null) {
            String[] ret = new String[2];
            ret[0] = key;
            ret[1] = secret;
            return ret;
        } else {
            return null;
        }
    }

    private void storeKeys(String key, String secret) {
        // Save the access key for later
        SharedPreferences prefs = mAppContext.getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putString(ACCESS_KEY_NAME, key);
        edit.putString(ACCESS_SECRET_NAME, secret);
        edit.commit();
    }

    private void clearKeys() {
        SharedPreferences prefs = mAppContext.getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
        SharedPreferences.Editor edit = prefs.edit();
        edit.clear();
        edit.commit();
    }
}
