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

import java.io.File;
import java.io.FileInputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.content.Context.MODE_PRIVATE;
import static org.literacybridge.androidtbloader.util.Constants.LOCATION_FILE_EXTENSION;

/**
 * Configuration for the current user.
 */

public class Config {
    private static final String TAG = Config.class.getSimpleName();

    public interface ConfigHandler {
        void gotConfig(SharedPreferences prefs);
        void noConfig();
    }

    private static String mTbcdId;
    private static String mProjectFilter;
    private static Pattern mProjectPattern;

    public static String getTbcdid() {
        return mTbcdId;
    }

    public static String getProjectFilter() {
        return mProjectFilter;
    }

    /**
     * Determines whether the current user is assigned to a given project. Returns true if the
     * project filter has not yet been initialized.
     * @param projectName The project of interest.
     * @return true if the current user should see the project, false otherwise.
     */
    public static boolean isUsersProject(String projectName) {
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
    }

    public void getUserConfig(final ConfigHandler handler) {
        // Load any persisted settings.
//        final SharedPreferences userPrefs = mApplicationContext.getSharedPreferences(UserHelper.getUserId(), MODE_PRIVATE);
        final SharedPreferences userPrefs = PreferenceManager.getDefaultSharedPreferences(
                mApplicationContext);
        final String etag = userPrefs.getString("etag", "");
        // Query settings from s3.
        ListObjectsV2Request request = new ListObjectsV2Request()
                .withBucketName(Constants.CONTENT_UPDATES_BUCKET_NAME)
                .withPrefix("users/" + UserHelper.getUserId() + ".config");

        Log.d(TAG, String.format("Fetching config file info for %s", UserHelper.getUserId()));
        S3Helper.listObjects(request, new S3Helper.ListObjectsListener() {
            @Override
            public void onSuccess(ListObjectsV2Result result) {
                List<S3ObjectSummary> s3Users = result.getObjectSummaries();
                // Should be only one
                if (s3Users.size() != 1) {
                    handler.noConfig();
                    return;
                }
                S3ObjectSummary summary = s3Users.get(0);
                Log.d(TAG, String.format("User key: %s, etag: %s, saved etag: %s", summary.getKey(), summary.getETag(), etag));
                String s3etag = summary.getETag();
                if (s3etag.equalsIgnoreCase(etag)) {
                    Log.d(TAG, String.format("Already have current config file info for %s", UserHelper.getUserId()));
                    gotUserSettings(userPrefs, handler);
                    return;
                }
                fetchUserSettings(summary, handler);
            }

            @Override
            public void onFailure(Exception ex) {
                if (!etag.equals("")) {
                    Log.d(TAG, String.format("Could not fetch config file info for %s, but have saved settings", UserHelper.getUserId()));
                    gotUserSettings(userPrefs, handler);
                    return;
                }
                handler.noConfig();
            }
        });

    }

    private void gotUserSettings(SharedPreferences prefs, final ConfigHandler handler) {
        mTbcdId = prefs.getString(Constants.TBLOADER_DEVICE_PREF_NAME, "");
        // Default project filter is "match anything"
        mProjectFilter = prefs.getString("projects", ".*");
        handler.gotConfig(prefs);
    }

    private void fetchUserSettings(final S3ObjectSummary summary, final ConfigHandler handler) {
        Log.d(TAG, String.format("Fetching config file info for %s", UserHelper.getUserId()));
        String filename = UserHelper.getUserId() + ".config";
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
                        handler.noConfig();
                        return;
                    }

                    // Update user preferences based on download.
//                    SharedPreferences userPrefs = mApplicationContext.getSharedPreferences(UserHelper.getUserId(), MODE_PRIVATE);
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
                    prefsEditor.putString("etag", summary.getETag());
                    // Add rest of signin config to prefs
                    for (Properties.Entry e : newSettings.entrySet()) {
                        prefsEditor.putString((String)e.getKey(), (String)e.getValue());
                    }
                    prefsEditor.apply();

                    gotUserSettings(userPrefs, handler);

                }
            }

            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                Log.d(TAG, String.format("transfer progress: %d of %d", bytesCurrent, bytesTotal));
            }

            @Override
            public void onError(int id, Exception ex) {
                Log.d(TAG, "transfer error", ex);
                handler.noConfig();
            }
        });

    }

    /**
     * Reads the versions (etags) from S3 for location files. Downloads any that are stale.
     * @param handler A handler to call when the current files have been downloaded, or if
     *                we can't get the list of S3 objects.
     */
    public void refreshCommunityLocations(final Config.ConfigHandler handler) {
        // Load the etags for any location info we already have.
        final SharedPreferences locationPrefs = mApplicationContext.getSharedPreferences("community.locations", MODE_PRIVATE);
        // Query new location info from s3.
        ListObjectsV2Request request = new ListObjectsV2Request()
                .withBucketName(Constants.CONTENT_UPDATES_BUCKET_NAME)
                .withPrefix("locations/");

        Log.d(TAG, String.format("Fetching community location file info"));
        S3Helper.listObjects(request, new S3Helper.ListObjectsListener() {
            // Build list of files to be downloaded here.
            final List<S3ObjectSummary> toFetch = new ArrayList<>();

            @Override
            public void onSuccess(ListObjectsV2Result result) {
                List<S3ObjectSummary> s3LocFiles = result.getObjectSummaries();
                for (S3ObjectSummary summary : s3LocFiles) {
                    String keyFileName = summary.getKey().substring(summary.getKey().indexOf('/')+1).toUpperCase();
                    // For some reason, querying this prefix returns the bare prefix ("locations/"), while querying
                    // the "projects/" prefix does NOT return the bare prefix. The docs are unhelpful...
                    if (keyFileName.length() == 0) { continue; }

                    String project = keyFileName.substring(0, keyFileName.lastIndexOf('.'));
                    // Only concerned with this user's projects.
                    if (!isUsersProject(project)) { continue; }

                    // Version of any saved location info.
                    String etag = locationPrefs.getString(project, "");
                    Log.d(TAG, String.format("Location key: %s, project: %s, etag: %s, saved etag: %s",
                                    summary.getKey(), project, summary.getETag(), etag));
                    // Version of current location info.
                    String s3etag = summary.getETag();
                    if (s3etag.equalsIgnoreCase(etag) && new File(PathsProvider.getLocationsCacheDirectory(), project + LOCATION_FILE_EXTENSION).exists()) {
                        Log.d(TAG, String.format("Already have current location file info for %s", project));
                    } else {
                        Log.d(TAG, String.format("Need to download location file for %s", project));
                        toFetch.add(summary);
                    }
                }
                fetchCommunityLocations();
            }

            /**
             * Downloads the first item in the 'toFetch' list. When the download completes,
             * (asynchronously) recursively calls itself to download the next.
             */
            private void fetchCommunityLocations() {
                if (toFetch.size() == 0) {
                    // Nothing left to do...
                    handler.gotConfig(null);
                    return;
                }
                S3ObjectSummary summary = toFetch.remove(0);
                downloadLocationsFile(summary, new Runnable() {
                    @Override
                    public void run() {
                        fetchCommunityLocations();
                    }
                });
            }

            @Override
            public void onFailure(Exception ex) {
                Log.d(TAG, String.format("Could not fetch community location file info"));
                handler.noConfig();
            }
        });

    }

    /**
     * Downloads a single community locations file from S3.
     * @param summary The S3ObjectSummary that describes the S3 object.
     * @param handler A Runnable for the completion callback.
     */
    private void downloadLocationsFile(final S3ObjectSummary summary, final Runnable handler) {
        Log.d(TAG, String.format("Fetching location info for %s", UserHelper.getUserId()));
        String keyFileName = summary.getKey().substring(summary.getKey().indexOf('/')+1).toUpperCase();
        final String project = keyFileName.substring(0, keyFileName.lastIndexOf('.'));
        final File tempFile = new File(PathsProvider.getLocationsCacheDirectory(), project + LOCATION_FILE_EXTENSION + ".temp");
        final File newFile = new File(PathsProvider.getLocationsCacheDirectory(), project + LOCATION_FILE_EXTENSION);

        // Initiate the download. Make sure there's someplace to put the new file.
        tempFile.mkdirs();
        if (tempFile.exists()) {
            tempFile.delete();
        }
        Log.d(TAG, "about to call transferUtility.download");
        final TransferUtility transferUtility = S3Helper.getTransferUtility();
        final TransferObserver observer = transferUtility.download(Constants.CONTENT_UPDATES_BUCKET_NAME, summary.getKey(), tempFile);
        Log.d(TAG, "back from call to transferUtility.download");

        observer.setTransferListener(new TransferListener() {
            @Override
            public void onStateChanged(int id, TransferState state) {
                Log.d(TAG, String.format("transfer state changed: %s", state.toString()));
                if (state == TransferState.COMPLETED) {
                    Log.d(TAG, String.format("Got new locations file for %s", project));
                    // This doesn't seem to be automatic, so do it manually.
                    transferUtility.deleteTransferRecord(observer.getId());

                    // Swap new file for old.
                    if (newFile.exists()) {
                        newFile.delete();
                    }
                    tempFile.renameTo(newFile);

                    // Remember which version of location info we've just downloaded.
                    final SharedPreferences locationPrefs = mApplicationContext.getSharedPreferences("community.locations", MODE_PRIVATE);
                    SharedPreferences.Editor prefsEditor = locationPrefs.edit();
                    prefsEditor.putString(project, summary.getETag());
                    prefsEditor.apply();

                    handler.run();
                }
            }

            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                Log.d(TAG, String.format("transfer progress: %d of %d", bytesCurrent, bytesTotal));
            }

            @Override
            public void onError(int id, Exception ex) {
                Log.d(TAG, "transfer error", ex);
                handler.run();
            }
        });

    }


}
