package org.literacybridge.androidtbloader.checkin;

import android.content.SharedPreferences;
import android.location.Location;

import android.util.Log;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import org.literacybridge.androidtbloader.TBLoaderAppContext;
import org.literacybridge.androidtbloader.community.CommunityInfo;
import org.literacybridge.androidtbloader.signin.UserHelper;
import org.literacybridge.androidtbloader.util.Config;
import org.literacybridge.androidtbloader.util.Constants;
import org.literacybridge.androidtbloader.util.PathsProvider;
import org.literacybridge.androidtbloader.util.S3Helper;
import org.literacybridge.core.fs.OperationLog;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.content.Context.MODE_PRIVATE;
import static org.literacybridge.androidtbloader.util.Constants.LOCATION_FILE_EXTENSION;

/**
 * Manages the community locations that we know.
 */

public class KnownLocations {
    private static final String TAG = "TBL!:" + KnownLocations.class.getSimpleName();
    //private static final String TAG = KnownLocations.class.getSimpleName();

    // Cached list of {project name => {community name => community info} }
    private static Map<String, Map<String, CommunityInfo>> allProjects = new HashMap<>();

    /**
     * Tries to find the project / community combination in all projects. Returns null if not found.
     * @param community Community of interest.
     * @param project Project of interest.
     * @return The CommunityInfo, if found, otherwise null.
     */
    public static CommunityInfo findCommunity(String community, String project) {
        Map<String, CommunityInfo> communities = allProjects.get(project);
        if (communities != null) {
            return communities.get(community);
        }
        return null;
    }

    private List<String> projects = new ArrayList<>();
    private Map<String, Map<String, CommunityInfo>> communities = new HashMap<>();

    KnownLocations(List<String> projects) {
        for (String p : projects) {
            this.projects.add(p.toUpperCase());
        }
        loadLocationsForProjects(this.projects);
        for (String p : this.projects) {
            this.communities.put(p, allProjects.get(p));
        }
    }

    /**
     * Finds the communities "near" a location, where near currently means 1/2 km.
     * @param location The location.
     * @return List of nearby communities, sorted by ascending distance.
     */
    List<CommunityInfo> findCommunitiesNear(Location location) {
        List<SR> near = findByDistance(location, 0, 500);
        Collections.sort(near, new Comparator<SR>() {
            @Override
            public int compare(SR lhs, SR rhs) {
                if (lhs.distance < rhs.distance) return -1;
                if (lhs.distance > rhs.distance) return 1;
                return lhs.community.getName().compareToIgnoreCase(rhs.community.getName());
            }
        });
        List<CommunityInfo> result = new ArrayList<>();
        for (SR sr : near) {
            result.add(sr.community);
        }
        return result;
    }

    /**
     * Helper to find communities with known locations within a given distance.
     * @param target The location for which the list of nearby communities is desired.
     * @param minDist The closest desired distance.
     * @param maxDist The farthest desired distance.
     * @return A list of objects with communities within the desired range.
     */
    private List<SR> findByDistance(Location target, float minDist, float maxDist) {
        List<SR> result = new ArrayList<>();
        for (String project : projects) {
            // There may or may not be community infos for a given project.
            Map<String, CommunityInfo> infos = communities.get(project);
            if (infos != null) {
                for (CommunityInfo community : infos.values()) {
                    float distance = community.getLocation().distanceTo(target);
                    if (distance >= minDist && distance < maxDist) {
                        result.add(new SR(distance, community));
                    }
                }
            }
        }
        return result;
    }

    /**
     * Given a gps location and a community, mark the community as at that location.
     * @param mGpsLocation The GPS coordinates.
     * @param community A community at those coordinates.
     */
    static void setLocationInfoFor(Location mGpsLocation, CommunityInfo community) {
        // We can use this location to improve our locations. Send to the server; let them handle it.
        Map<String,String> logInfo = new HashMap<>();
        logInfo.put("community", community.getName());
        logInfo.put("project", community.getProject());
        logInfo.put("longitude", String.format("%+3.5f", mGpsLocation.getLongitude()));
        logInfo.put("latitude", String.format("%+3.5f", mGpsLocation.getLatitude()));
        OperationLog.logEvent("SetLocation", logInfo);

        // Also record it locally.
        // Find the collection of communities associated with the given community's project.
        Map<String, CommunityInfo> projCommunities = allProjects.get(community.getProject());
        // If there is no such collection, create a new empty one.
        if (projCommunities == null) {
            projCommunities = new HashMap<>();
            allProjects.put(community.getProject(), projCommunities);
        }
        // Find the entry for this community in the collection of communities.
        CommunityInfo info = projCommunities.get(community.getName());
        // If there is no such entity, create a new one.
        if (info == null) {
            info = new CommunityInfo(community.getName(), community.getProject());
            projCommunities.put(community.getName(), info);
        }
        info.setLocation(mGpsLocation);
    }

    /**
     * A helper object to hold the distance from the "current" location to a given project.
     */
    private static class SR {
        double distance;
        CommunityInfo community;

        SR(double distance, CommunityInfo community) {
            this.distance = distance;
            this.community = community;
        }
    }

    /**
     * Given a list of project names, load the locations files for those projects. Cache
     * the location files, so subsequent calls don't need to read a file.
     * @param projects A list of projects of interest.
     */
    public static void loadLocationsForProjects(List<String> projects) {
        for (String proj : projects) {
            String project = proj.toUpperCase();
            if (!allProjects.containsKey(project)) {
                File locFile = new File(PathsProvider.getLocationsCacheDirectory(),
                        project + LOCATION_FILE_EXTENSION);
                if (locFile.exists()) {
                    LocationsCsvFile csv = new LocationsCsvFile(locFile);
                    Map<String, CommunityInfo> infos = csv.read();
                    allProjects.put(project, infos);
                }
            }
        }
    }

    /**
     * Reads the versions (etags) from S3 for location files. Downloads any that are stale.
     * @param listener A listener to call when the current files have been downloaded, or if
     *                we can't get the list of S3 objects.
     */
    public static void refreshCommunityLocations(final Config.Listener listener) {
        final OperationLog.Operation opLog = OperationLog.startOperation("RefreshCommunityLocations");
        // Load the etags for any location info we already have.
        final SharedPreferences locationPrefs = TBLoaderAppContext.getInstance().getSharedPreferences("community.locations", MODE_PRIVATE);
        // Query new location info from s3.
        ListObjectsV2Request request = new ListObjectsV2Request()
                .withBucketName(Constants.DEPLOYMENTS_BUCKET_NAME)
                .withPrefix("locations/");

        Log.d(TAG, String.format("Fetching community location file info"));
        S3Helper.listObjects(request, new S3Helper.ListObjectsListener() {
            // Build list of files to be downloaded here.
            final List<S3ObjectSummary> toFetch = new ArrayList<>();

            @Override
            public void onSuccess(ListObjectsV2Result result) {
                List<String> cached = new ArrayList<>();
                List<String> missing = new ArrayList<>();
                List<String> stale = new ArrayList<>();
                List<S3ObjectSummary> s3LocFiles = result.getObjectSummaries();
                for (S3ObjectSummary summary : s3LocFiles) {
                    String keyFileName = summary.getKey().substring(summary.getKey().indexOf('/')+1).toUpperCase();
                    // For some reason, querying this prefix returns the bare prefix ("locations/"), while querying
                    // the "projects/" prefix does NOT return the bare prefix. The docs are unhelpful...
                    if (keyFileName.length() == 0) { continue; }

                    String project = keyFileName.substring(0, keyFileName.lastIndexOf('.'));
                    // Only concerned with this user's projects.
                    if (!TBLoaderAppContext.getInstance().getConfig().isProgramIdForUser(project)) { continue; }

                    // Version of any saved location info.
                    String etag = locationPrefs.getString(project, "");
                    Log.d(TAG, String.format("Location key: %s, project: %s, etag: %s, saved etag: %s",
                                             summary.getKey(), project, summary.getETag(), etag));
                    // Version of current location info.
                    String s3etag = summary.getETag();
                    File locationFile = new File(PathsProvider.getLocationsCacheDirectory(), project + LOCATION_FILE_EXTENSION);
                    if (s3etag.equalsIgnoreCase(etag) && locationFile.exists()) {
                        cached.add(project);
                        Log.d(TAG, String.format("Already have current location file info for %s", project));
                    } else {
                        if (!locationFile.exists()) {
                            missing.add(project);
                        } else {
                            stale.add(project);
                        }
                        Log.d(TAG, String.format("Need to download location file for %s", project));
                        toFetch.add(summary);
                    }
                }
                if (cached.size() > 0) opLog.put("cached", cached);
                if (stale.size() > 0) opLog.put("stale", stale);
                if (missing.size() > 0) opLog.put("missing", missing);
                fetchCommunityLocations();
            }

            /**
             * Downloads the first item in the 'toFetch' list. When the download completes,
             * (asynchronously) recursively calls itself to download the next.
             */
            private void fetchCommunityLocations() {
                if (toFetch.size() == 0) {
                    // Nothing left to do...
                    opLog.finish();
                    listener.onSuccess();
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
                opLog.put("exception", ex)
                        .finish();
                Log.d(TAG, String.format("Could not fetch community location file info"));
                listener.onError();
            }
        });

    }

    /**
     * Downloads a single community locations file from S3.
     * @param summary The S3ObjectSummary that describes the S3 object.
     * @param handler A Runnable for the completion callback.
     */
    private static void downloadLocationsFile(final S3ObjectSummary summary, final Runnable handler) {
        Log.d(TAG, String.format("Fetching location info for %s", UserHelper.getInstance().getUserId()));
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
        final TransferObserver observer = transferUtility.download(Constants.DEPLOYMENTS_BUCKET_NAME, summary.getKey(), tempFile);
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
                    final SharedPreferences locationPrefs = TBLoaderAppContext.getInstance().getSharedPreferences("community.locations", MODE_PRIVATE);
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
