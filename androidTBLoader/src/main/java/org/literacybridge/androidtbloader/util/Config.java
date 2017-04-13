package org.literacybridge.androidtbloader.util;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.S3ObjectSummary;

import org.literacybridge.androidtbloader.TBLoaderAppContext;
import org.literacybridge.androidtbloader.signin.UserHelper;
import org.literacybridge.core.fs.OperationLog;

import java.io.File;
import java.io.FileInputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Configuration for the current user.
 */

public class Config {
    private static final String TAG = Config.class.getSimpleName();

    public static final String DEVICE_ID_KEY = "tbcd";
    public static final String PROJECTS_FILTER_KEY = "projects";
    public static final String ADVANCED_FLAG_KEY = "advanced";
    public static final String USERNAME_KEY = "username";
    public static final String ETAG_KEY = "etag";

    public static final String DEVICE_ID_DEFAULT = "";
    public static final String PROJECTS_FILTER_DEFAULT = ".*";
    public static final Boolean ADVANCED_FLAG_DEFAULT = false;
    public static final String USERNAME_DEFAULT = null;
    public static final String ETAG_DEFAULT = "";

    public interface Listener {
        void onSuccess();
        void onError();
    }

    private boolean mIsAdvanced = false;
    private String mTbcdId;
    private String mProjectFilter;
    private Pattern mProjectPattern;
    private String mUsername;

    public String getTbcdid() {
        return mTbcdId;
    }

    public String getProjectFilter() {
        return mProjectFilter;
    }

    public boolean isAdvanced() { return mIsAdvanced; }

    public String getUsername() { return mUsername; }

    public boolean haveCachedConfig() {
        SharedPreferences userPrefs = PreferenceManager.getDefaultSharedPreferences(
                TBLoaderAppContext.getInstance());
        String etag = userPrefs.getString(ETAG_KEY, null);
        String username = userPrefs.getString(USERNAME_KEY, null);
        return (etag != null && username != null);
    }

    /**
     * This should be called to clear any user-specific cached settings.
     */
    public void onSignOut() {
        SharedPreferences userPrefs = PreferenceManager.getDefaultSharedPreferences(TBLoaderAppContext.getInstance());
        SharedPreferences.Editor editor = userPrefs.edit();
        editor.clear().commit();
        mIsAdvanced = false;
        mTbcdId = mProjectFilter = mUsername = null;
        mProjectPattern = null;
    }

    /**
     * Determines whether the current user is assigned to a given project. Returns true if the
     * project filter has not yet been initialized.
     * @param projectName The project of interest.
     * @return true if the current user should see the project, false otherwise.
     */
    public boolean isUsersProject(String projectName) {
        if (mProjectFilter == null) return true;
        if (mProjectPattern == null) {
            mProjectPattern = Pattern.compile("(?i)" + mProjectFilter);
        }
        Matcher m = mProjectPattern.matcher(projectName);
        return m.matches();
    }


    private TBLoaderAppContext mApplicationContext;

    public Config(TBLoaderAppContext appContext) {
        mApplicationContext = appContext;
        SharedPreferences userPrefs = PreferenceManager.getDefaultSharedPreferences(appContext);
        gotUserSettings(userPrefs, null);
    }

    private void gotUserSettings(SharedPreferences prefs, final Listener listener) {
        mTbcdId = prefs.getString(DEVICE_ID_KEY, DEVICE_ID_DEFAULT);
        // Default project filter is "match anything"
        mProjectFilter = prefs.getString(PROJECTS_FILTER_KEY, PROJECTS_FILTER_DEFAULT);
        mIsAdvanced = Boolean.parseBoolean(prefs.getString(ADVANCED_FLAG_KEY, ADVANCED_FLAG_DEFAULT.toString()));
        String newName = prefs.getString(USERNAME_KEY, USERNAME_DEFAULT);
        if (mUsername == null) {
            mUsername = newName;
        } else if (newName!=null && !mUsername.equals(newName)) {
            // It is unexpected that the name should change while logged in.
            OperationLog.log("UserNameChanged").put("from", mUsername).put("to", newName).save();
            mUsername = newName;
        }
        if (listener != null) {
            listener.onSuccess();
        }
    }

    /**
     * Get the user config from server. If no connectivity, and have a cached config use that.
     * @param username The user's sign-in id, corresponding to their server-side config file.
     * @param listener A Listener to accept the results.
     */
    public void getServerSideUserConfig(final String username, final Listener listener) {
        // Load any persisted settings.
        final SharedPreferences userPrefs = PreferenceManager.getDefaultSharedPreferences(
                mApplicationContext);
        final String etag = userPrefs.getString(ETAG_KEY, ETAG_DEFAULT);

        String cachedUsername = userPrefs.getString(USERNAME_KEY, USERNAME_DEFAULT);
        if (cachedUsername == null || !cachedUsername.equals(username)) {
            SharedPreferences.Editor prefsEditor = userPrefs.edit();
            prefsEditor.putString(USERNAME_KEY, username);
            prefsEditor.apply();
            if (mUsername==null) {                                                       
                mUsername = username;
            }
        }

        // Query settings from s3.
        ListObjectsV2Request request = new ListObjectsV2Request()
                .withBucketName(Constants.CONTENT_UPDATES_BUCKET_NAME)
                .withPrefix("users/" + username + ".config");

        Log.d(TAG, String.format("Fetching config file info for %s", UserHelper.getUserId()));
        S3Helper.listObjects(request, new S3Helper.ListObjectsListener() {
            @Override
            public void onSuccess(ListObjectsV2Result result) {
                List<S3ObjectSummary> s3Users = result.getObjectSummaries();
                // Should be only one
                if (s3Users.size() != 1) {
                    listener.onError();
                    return;
                }
                S3ObjectSummary summary = s3Users.get(0);
                Log.d(TAG, String.format("User key: %s, etag: %s, saved etag: %s", summary.getKey(), summary.getETag(), etag));
                String s3etag = summary.getETag();
                if (s3etag.equalsIgnoreCase(etag)) {
                    Log.d(TAG, String.format("Already have current config file info for %s", UserHelper.getUserId()));
                    gotUserSettings(userPrefs, listener);
                    return;
                }
                fetchUserSettings(username, summary, listener);
            }

            @Override
            public void onFailure(Exception ex) {
                if (!etag.equals("")) {
                    Log.d(TAG, String.format("Could not fetch config file info for %s, but have saved settings", UserHelper.getUserId()));
                    gotUserSettings(userPrefs, listener);
                    return;
                }
                listener.onError();
            }
        });

    }


    private void fetchUserSettings(final String username, final S3ObjectSummary summary, final Listener listener) {
        Log.d(TAG, String.format("Fetching config file info for %s", UserHelper.getUserId()));
        String filename = UserHelper.getUsername() + ".config";
        String key = "users/" + filename;
        final File file = new File(PathsProvider.getLocalTempDirectory(), filename);

        // Initiate the download
        Log.d(TAG, "about to call transferUtility.download");
        final TransferUtility transferUtility = S3Helper.getTransferUtility();
        final TransferObserver observer = transferUtility.download(Constants.CONTENT_UPDATES_BUCKET_NAME, key, file);
        Log.d(TAG, "back from call to transferUtility.download");

        observer.setTransferListener(new TransferListener() {
            @Override
            public void onStateChanged(int id, TransferState state) {
                Log.d(TAG, String.format("transfer state changed: %s", state.toString()));
                if (state == TransferState.COMPLETED) {
                    // This doesn't seem to be automatic, so do it manually.
                    transferUtility.deleteTransferRecord(observer.getId());

                    // Load new settings from downloaded file.
                    Properties newSettings = new Properties();
                    try {
                        newSettings.load(new FileInputStream(file));
                    } catch (Exception ex) {
                        Log.d(TAG, "Exception loading user config", ex);
                    }
                    file.delete();
                    if (newSettings.getProperty("tbcd") == null) {
                        listener.onError();
                        return;
                    }

                    // Update user preferences based on download.
                    SharedPreferences userPrefs = PreferenceManager.getDefaultSharedPreferences(
                            mApplicationContext);
                    SharedPreferences.Editor prefsEditor = userPrefs.edit();

                    // Add update time to prefs
                    TimeZone tz = TimeZone.getTimeZone("UTC");
                    DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'"); // Quoted "Z" to indicate UTC, no timezone offset
                    df.setTimeZone(tz);
                    String nowAsIso = df.format(new Date());
                    prefsEditor.putString("downloaded", nowAsIso);
                    Log.d(TAG, String.format("Setting time as %s", nowAsIso));
                    // Add etag to prefs
                    prefsEditor.putString(ETAG_KEY, summary.getETag());
                    // Add rest of signin config to prefs
                    for (Properties.Entry e : newSettings.entrySet()) {
                        prefsEditor.putString((String) e.getKey(), (String) e.getValue());
                    }
                    prefsEditor.apply();

                    gotUserSettings(userPrefs, listener);

                }
            }

            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                Log.d(TAG, String.format("transfer progress: %d of %d", bytesCurrent, bytesTotal));
            }

            @Override
            public void onError(int id, Exception ex) {
                Log.d(TAG, "transfer error", ex);
                listener.onError();
            }
        });

    }

}
