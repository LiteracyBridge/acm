package org.literacybridge.androidtbloader.util;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import org.apache.commons.lang3.StringUtils;
import org.literacybridge.androidtbloader.TBLoaderAppContext;
import org.literacybridge.core.fs.OperationLog;

import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Configuration for the current user.
 */

public class Config {
    private static final String TAG = "TBL!:" + Config.class.getSimpleName();

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

    private static final String USERNAME_PROP = "cognito:username";
    private static final String EMAIL_PROP = "email";
    private static final String GREETING_PROP = "custom:greeting";
    private static final String EDIT_PROP = "edit";
    private static final String VIEW_PROP = "view";
    private static final String ADMIN_PROP = "admin";

    private static final String TBCDID_PROP = "tbcd";

    private final SharedPreferences mUserPrefs;
    private final TBLoaderAppContext mAppContext;

    public interface Listener {
        void onSuccess();

        void onError();
    }

    private TbSrnHelper mTbSrnHelper;

    private Pattern mProjectPattern;

    public String getTbcdid() {
        return mUserPrefs.getString(TBCDID_PROP, null);
    }

    public boolean isAdvanced() {
        return getEmail().endsWith("@amplio.org");
    }

    public String getUsername() {
        return mUserPrefs.getString(USERNAME_PROP, null);
    }

    public String getEmail() {
        return mUserPrefs.getString(EMAIL_PROP, null);
    }
    public String getGreeting() { return mUserPrefs.getString(GREETING_PROP, ""); }
    public void updateGreeting(String newGreeting) {
        SharedPreferences.Editor prefsEditor = mUserPrefs.edit();
        prefsEditor.putString(GREETING_PROP, newGreeting);
        prefsEditor.apply();
    }

    public Config(TBLoaderAppContext appContext) {
        mAppContext = appContext;
        mUserPrefs = PreferenceManager.getDefaultSharedPreferences(mAppContext);
    }

    /**
     * This should be called to clear any user-specific cached settings.
     */
    public void onSignOut() {
        SharedPreferences userPrefs = PreferenceManager.getDefaultSharedPreferences(
            TBLoaderAppContext.getInstance());
        SharedPreferences.Editor editor = userPrefs.edit();
        editor.clear().apply();
        mProjectPattern = null;
        mTbSrnHelper = null;
    }

    public void applyUserDetails(Map<String, String> details) {
        String prevEmail = getEmail();
        if (details != null && details.size() > 0) {
            OperationLog.Operation opLog = OperationLog.startOperation("GetUserDetails");
            SharedPreferences.Editor prefsEditor = mUserPrefs.edit();

            for (Map.Entry<String, String> e : details.entrySet()) {
                prefsEditor.putString(e.getKey(), e.getValue());
                opLog.put(e.getKey(), e.getValue());
            }
            prefsEditor.putString("greeting", details.get("custom:greeting"));

            // If the email has changed, then we need to clear the stale TBCD value. It will be re-filled
            // in prepareForSerialNumberAllocation
            if (!mUserPrefs.getString(EMAIL_PROP, "").equalsIgnoreCase(prevEmail)) {
                prefsEditor.remove(TBCDID_PROP);
            }

            prefsEditor.commit();
            opLog.finish();
        }
    }

    public boolean haveCachedConfig() {
        return mUserPrefs.getString(EMAIL_PROP, null) != null;
    }

    /**
     * Determines whether the current user is assigned to a given project. Returns true if the
     * project filter has not yet been initialized.
     *
     * @param projectName The project of interest.
     * @return true if the current user should see the project, false otherwise.
     */
    public boolean isUsersProject(String projectName) {
        if (mProjectPattern == null) {
            String editable = mUserPrefs.getString(EDIT_PROP, "");
            String viewable = mUserPrefs.getString(VIEW_PROP, "");
            // Case insensitive. Match start of string....
            StringBuilder pattern = new StringBuilder().append("(?i)^(");
            if (!StringUtils.isEmpty(editable)) {
                // Match editables...
                pattern.append(editable);
                if (!StringUtils.isEmpty(viewable)) {
                    // or...
                    pattern.append('|');
                }
            }
            if (!StringUtils.isEmpty(viewable)) {
                // ...match vieweables
                pattern.append(viewable);
            }
            // Match end of string.
            pattern.append(")$");

            mProjectPattern = Pattern.compile(pattern.toString());
        }
        Matcher m = mProjectPattern.matcher(projectName);
        return m.matches();
    }

    /**
     * Attempt to allocate the next Talking Book serial number for this TBLoader. If there are
     * no serial numbers available locally, will attempt to call the server to allocate a new
     * block.
     *
     * @param success  A Consumer that accepts an integer of the next serial number.
     * @param failure  A Consumer that accepts an Exception from a failure.
     */
    public void allocateDeviceSerialNumber(Consumer<String> success, Consumer<Exception> failure) {
        mTbSrnHelper.allocateNextSrn(success, failure);
    }

    /**
     * Get the user config from server. If no connectivity, and have a cached config use that.
     *
     * @param listener A Listener to accept the results.
     */
    public void prepareForSerialNumberAllocation(final Listener listener) {
        mTbSrnHelper = new TbSrnHelper(mAppContext);
        String email = getEmail();
        mTbSrnHelper.prepareForAllocation(email, new Listener() {
            @Override
            public void onSuccess() {
                String tbcd = mTbSrnHelper.getTbLoaderIdHex();
                if (!mUserPrefs.getString(TBCDID_PROP, "").equalsIgnoreCase(tbcd)) {
                    SharedPreferences.Editor prefsEditor = mUserPrefs.edit();
                    prefsEditor.putString(TBCDID_PROP, tbcd);
                    prefsEditor.apply();
                }
                listener.onSuccess();
            }

            @Override
            public void onError() {
                listener.onError();
            }
        });
    }

}
