package org.literacybridge.androidtbloader.content;

import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserSession;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.GenericHandler;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import org.literacybridge.androidtbloader.BuildConfig;
import org.literacybridge.androidtbloader.TBLoaderAppContext;
import org.literacybridge.androidtbloader.community.CommunityInfo;
import org.literacybridge.androidtbloader.signin.UnattendedAuthenticator;
import org.literacybridge.androidtbloader.signin.UserHelper;
import org.literacybridge.androidtbloader.util.Constants;
import org.literacybridge.androidtbloader.util.PathsProvider;
import org.literacybridge.androidtbloader.util.S3Helper;
import org.literacybridge.core.fs.OperationLog;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Manages lists of available Deployments for projects.
 */

public class ContentManager {
    private static final String TAG = "TBL!:" + "ContentManager";
    private static final String UNKNOWN_LOCAL_VERSION = "--";
    private static final String NO_LOCAL_VERSION = "";
    private static final long CONTENT_UPDATE_FRESH = 60 * 1000; // Fresh if newer than 60 seconds.

    public static final String CONTENT_LIST_CHANGED_EVENT = "ContentListChanged";


    public enum Flags {
        ForUser,    // Only for the current user (generally the default)
        ForAll,     // For all users
        Local,      // Only local content (exclude cloud)
        Cloud,      // Only cloud content (exclude local)
        All;        // All

        boolean inList(Flags[] flags) {
            for (Flags f : flags) {
                if (f == this) return true;
            }
            return false;
        }
    }


    private static abstract class ListContentListener {
        public abstract void onSuccess(List<ContentInfo> projects);
        public void onFailure(Exception ex) {}
    }

    private TBLoaderAppContext applicationContext;

    // A list of all the projects found in s3
    private Map<String, ContentInfo> mProjects = new HashMap<>();

    private List<ContentInfo> mContentList = new ArrayList<>();
    private long mContentListTime = 0;  // To determine freshness.

    // A cache of community names for projects, as contained in the Deployments.
    private Map<String, Map<String, CommunityInfo>> mProjectCommunitiesCache = null;

    public ContentManager(TBLoaderAppContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    void startDownload(ContentInfo info, final ContentDownloader.DownloadListener listener) {
        info.startDownload(applicationContext, new ContentDownloader.DownloadListener() {
            @Override
            public void onUnzipProgress(int id, long current, long total) {
                listener.onUnzipProgress(id, current, total);
            }

            @Override
            public void onStateChanged(int id, TransferState state) {
                mProjectCommunitiesCache = null;
                listener.onStateChanged(id, state);
            }

            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                listener.onProgressChanged(id, bytesCurrent, bytesTotal);
            }

            @Override
            public void onError(int id, Exception ex) {
                 listener.onError(id, ex);
            }
        });
    }

    /**
     * Gets the names of the known projects. Can be filtered to "local" or to "cloud". By
     * default is filtered to this user's projects, but can be expanded to all.
     * @param flags Flags to control the filtering.
     * @return A Set&lt;String&gt; of file names.
     */
    public Set<String> getProjectNames(Flags... flags) {
        // By default include both local and cloud. If _either_ flag is set, include what the flags say.
        // Include local content if the local flag is set, or if neither local nor cloud is set.
        boolean includeLocal = Flags.Local.inList(flags) || !Flags.Cloud.inList(flags);
        // Similarly for cloud content.
        boolean includeCloud = Flags.Cloud.inList(flags) || !Flags.Local.inList(flags);

        // Default is only for the user. If ForAll is set, include all content.
        boolean includeForAllUsers = Flags.ForAll.inList(flags);

        Set<String> result = new HashSet<>();
        for (Map.Entry<String, ContentInfo> entry : mProjects.entrySet()) {
            if (includeForAllUsers || TBLoaderAppContext.getInstance().getConfig().isUsersProject(entry.getKey())) {
                if (entry.getValue().getDownloadStatus() == ContentInfo.DownloadStatus.DOWNLOADED) {
                    if (includeLocal) {
                        result.add(entry.getKey());
                    }
                } else {
                    if (includeCloud) {
                        result.add(entry.getKey());
                    }
                }
            }
        }
        return result;
    }

    /**
     * Is there content info?
     * @return true if so.
     */
    public boolean haveContentInfo() {
        return mContentList.size() > 0;
    }

    List<ContentInfo> getContentList() {
        return mContentList;
// Add (Flags... flags) argument, and the following allows getting projects for this user,
// all users, downloaded, cloud, or all.
//        List<ContentInfo> result = new ArrayList<>();
//        for (String projectName : getProjectNames(flags)) {
//            result.add(mProjects.get(projectName));
//        }
//        return result;
    }

    @Deprecated
    private synchronized Map<String, Map<String, CommunityInfo>> getCommunitiesForProjects() {
        if (mProjectCommunitiesCache == null) {
            Map<String, Map<String, CommunityInfo>> projectCommunities = new HashMap<>();
            for (ContentInfo info : mContentList) {
                if (info.getDownloadStatus() == ContentInfo.DownloadStatus.DOWNLOADED) {
                    projectCommunities.put(info.getProjectName(), info.getCommunities());
                }
            }
            mProjectCommunitiesCache = projectCommunities;
        }
        return mProjectCommunitiesCache;
    }

    @Deprecated
    public Map<String, Map<String, CommunityInfo>> getCommunitiesForProjects(List<String> projects) {
        Map<String, Map<String, CommunityInfo>> pc = getCommunitiesForProjects();
        Map<String, Map<String, CommunityInfo>> resultSet = new HashMap<>();
        for (String project : projects) {
            Map<String, CommunityInfo> communities = pc.get(project);
            if (communities != null) {
                resultSet.put(project, communities);
            }
        }
        return resultSet;
    }

    public ContentInfo getContentInfo(String project) {
        for (ContentInfo info : mContentList) {
            if (info.getProjectName().equalsIgnoreCase(project))
                return info;
        }
        return null;
    }

    void removeLocalContent(final ContentInfo info) {
        List<String> toClear = new ArrayList<>();
        List<String> toRemove = new ArrayList<>();
        toRemove.add(info.getProjectName());
        removeContent(toClear, toRemove, new Runnable() {
            @Override
            public void run() {
                // Move from cloud list to local list.
                info.setDownloadStatus(ContentInfo.DownloadStatus.NEVER_DOWNLOADED);
                mProjectCommunitiesCache = null;
                onContentListChanged();
            }
        });
    }


    private void onContentListChanged() {
        mContentListTime = System.currentTimeMillis();
        Intent intent = new Intent(CONTENT_LIST_CHANGED_EVENT);
        LocalBroadcastManager.getInstance(TBLoaderAppContext.getInstance()).sendBroadcast(intent);
    }


    /**
     * Initialization:
     * - Get list of projects from preferences.
     * - Reconcile with local files by:
     * -- deleting local version for any entries marked local that don't exist on file system.
     * -- updating any versions that mismatch.
     * -- adding any local file system entries not in the list?
     * - Get list of projects from S3.
     * - Reconcile with list by
     * -- adding new projects not previously mentioned.
     * -- removing any projects no longer mentioned (delete projects?).
     * -- updating the cloud version for any that have changed.
     * -- marking as stale any projects not up to date (delete content?)
     */
    public void fetchContentList() {
        authenticateAndFetchContentList(false);
    }
    public void refreshContentList() {
        authenticateAndFetchContentList(true);
    }

    private void authenticateAndFetchContentList(final boolean force) {
        CognitoUserSession session = UserHelper.getCurrSession();
        if (session == null || !session.isValid()) {
            Log.d(TAG, String.format("authenticateAndFetchContentList: %s session, trying to authenticate", session==null?"no":"invalid"));

            new UnattendedAuthenticator(new GenericHandler() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "authenticateAndFetchContentList: authentication success");
                    fetchContentList(force);
                }

                @Override
                public void onFailure(Exception exception) {
                    Log.d(TAG, "authenticateAndFetchContentList: authentication failure");
                    if (force) {
                        Toast mToast = Toast.makeText(TBLoaderAppContext.getInstance(),
                            "Authentication Failure", Toast.LENGTH_SHORT);
                        mToast.show();
                    }
                    fetchContentList(force);
                }
            }).authenticate();
        } else {
            fetchContentList(force);
        }

    }

    /**
     * Implementation of fetchContentList
     * @param forceUpdate If true refresh even if the data is "fresh".
     */
    private void fetchContentList(final boolean forceUpdate) {
        final OperationLog.Operation opLog = OperationLog.startOperation("RefreshContentList");
        final Map<String,ContentInfo> localVersions = findLocalContent();

        // If the data is already "fresh", we may avoid some work.
        if (!forceUpdate && System.currentTimeMillis() <= mContentListTime+CONTENT_UPDATE_FRESH) {
            return;
        }

        // Reconcile with cloud (if we can).
        findCloudContent(new ListContentListener() {
            void addProjectsToLog() {
                List<String> local = new ArrayList<>();
                List<String> cloud = new ArrayList<>();
                for (Map.Entry<String, ContentInfo> entry : mProjects.entrySet()) {
                    if (entry.getValue().getDownloadStatus() == ContentInfo.DownloadStatus.DOWNLOADED) {
                        local.add(entry.getKey());
                    } else {
                        cloud.add(entry.getKey());
                    }
                }
                opLog.put("local", local)
                    .put("cloud", cloud);

            }
            @Override
            public void onSuccess(List<ContentInfo> cloudInfo) {
                reconcileCloudVersions(localVersions, cloudInfo, new Runnable() {
                    @Override
                    public void run() {
                        addProjectsToLog();
                        opLog.finish();
                        onContentListChanged();
                    }
                });
            }
            @Override
            public void onFailure(Exception ex) {
                Log.d(TAG, "Can't get cloud content");
                // No cloud data to invalidate local data, so just take the local values.
                mContentList.clear();
                for (ContentInfo info : localVersions.values()) {
                    if (info.getDownloadStatus() == ContentInfo.DownloadStatus.DOWNLOADED) {
                        mProjects.put(info.getProjectName(), info);
                        mContentList.add(info);
                    }
                }
                mProjectCommunitiesCache = null;
                addProjectsToLog();
                opLog.put("exception", ex)
                    .finish();
                onContentListChanged();
            }
        });
    }

    private void reconcileCloudVersions(Map<String,ContentInfo> localVersions, List<ContentInfo> cloudInfo, final Runnable onFinished) {
        final List<String> projectsToRemove = new ArrayList<>();
        final List<String> projectsToClear = new ArrayList<>();
        Map<String, ContentInfo> cloudVersions = new HashMap<>();
        for (ContentInfo info : cloudInfo) {
            cloudVersions.put(info.getProjectName(), info);
        }
        mContentList.clear();
        mProjectCommunitiesCache = null;

        // Look for local projects no longer in the cloud.
        for (String project : localVersions.keySet()) {
            if (!cloudVersions.containsKey(project)) {
                projectsToRemove.add(project);
            }
        }

        // Look at cloud info; find stale local versions.
        for (Map.Entry<String,ContentInfo> cv : cloudVersions.entrySet()) {
            String project = cv.getKey();
            ContentInfo cloudItem = cv.getValue();
            // Local version, if we have one.
            ContentInfo localItem = localVersions.get(project);
            String localVersion = localItem == null ? null : localItem.getVersion();
            if (localVersion == null || localVersion.equals(NO_LOCAL_VERSION)) {
                // No local version, so a cloud item.
                if (BuildConfig.DEBUG && (cloudItem.getDownloadStatus() != ContentInfo.DownloadStatus.NEVER_DOWNLOADED)) {
                    throw new AssertionError("download status is not NEVER_DOWNLOADED");
                }
                mContentList.add(cloudItem);
                mProjects.put(cloudItem.getProjectName(), cloudItem);
            } else if (!cloudItem.getVersion().equalsIgnoreCase(localVersion)) {
                // Stale local version, or unknown local version. Remove it; a cloud item.
                projectsToClear.add(project);
                cloudItem.setDownloadStatus(ContentInfo.DownloadStatus.NEVER_DOWNLOADED);
                mContentList.add(cloudItem);
                mProjects.put(cloudItem.getProjectName(), cloudItem);
            } else {
                // have a local, up-to-date version
                cloudItem.setDownloadStatus(ContentInfo.DownloadStatus.DOWNLOADED);
                mContentList.add(cloudItem);
                mProjects.put(cloudItem.getProjectName(), cloudItem);
            }
        }
        // Entries from cloudInfo were copied into cloudVersions, edited from there, updating the entries in cloudInfo.

        removeContent(projectsToClear, projectsToRemove, onFinished);
    }

    private void removeContent(final List<String> projectsToClear, final List<String> projectsToRemove, final Runnable onFinished) {
        final OperationLog.Operation opLog = OperationLog.startOperation("RemoveLocalContent");
        if (projectsToClear.size() > 0) {
            opLog.put("toClear", projectsToClear);
        }
        if (projectsToRemove.size() > 0) {
            opLog.put("toRemove", projectsToRemove);
        }
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                Log.i(TAG, "Removing obsolete projects and content");
                File projectsDir = PathsProvider.getLocalContentDirectory();
                try {
                    // Projects
                    for (String project : projectsToRemove) {
                        Log.d(TAG, String.format("Completely removing %s", project));
                        File projectDir = new File(projectsDir, project);
                        if (projectDir.exists()) {
                            deleteDirectory(projectDir);
                        }
                    }
                    // Content only
                    for (String project : projectsToClear) {
                        Log.d(TAG, String.format("Removing content from %s", project));
                        File projectDir = new File(projectsDir, project);
                        if (projectDir.exists()) {
                            emptyDirectory(projectDir);
                        }
                    }
                } catch (Exception e) {
                    opLog.put("exception", e);
                    Log.d(TAG, "Exception removing files", e);
                }
                Log.i(TAG, "Done removing files");
                return null;
            }
            @Override
            protected void onPostExecute(Void result) {
                Log.d(TAG, "Back from deleting files");
                opLog.finish();
                onFinished.run();
            }
        }.execute();
    }

    /**
     * Helper to remove the contents of a directory.
     * @param dir File object representing the directory to be emptied.
     */
    private void emptyDirectory(File dir) {
        if (!dir.exists() || !dir.isDirectory())
            return;
        File[] contents = dir.listFiles();
        for (File c : contents)
            deleteDirectory(c);
    }

    /**
     * Helper to remove a file or a directory and all of its contents.
     * @param dir File object representing the directory to be removed.
     */
    private void deleteDirectory(File dir) {
        if (!dir.exists())
            return;
        if (!dir.isDirectory()) {
            dir.delete();
            return;
        }
        File[] contents = dir.listFiles();
        for (File c : contents)
            deleteDirectory(c);
        dir.delete();
    }

    /**
     * Retrieves a list of the projects and current Deployments, from the S3 bucket.
     * @param projectsListener Callback to receive results.
     */
    private void findCloudContent(final ListContentListener projectsListener) {
        ListObjectsV2Request request = new ListObjectsV2Request()
                .withBucketName(Constants.DEPLOYMENTS_BUCKET_NAME)
                .withPrefix("projects/");

        // Get the list of objects
        S3Helper.listObjects(request, new S3Helper.ListObjectsListener() {
            @Override
            public void onSuccess(ListObjectsV2Result result) {
                Map<String, ContentInfo> projects = new HashMap<>();
                List<S3ObjectSummary> s3ObjectSummaries = result.getObjectSummaries();

                // Examine all the objects, and figure out the projects, their sizes, and current update.

                // First, look for all of the ".current" files. This will let us know what versions
                // we are current in s3.
                for (S3ObjectSummary summary : s3ObjectSummaries) {
                    String [] parts = summary.getKey().split("/");
                    if (parts.length == 3 && parts[2].endsWith(".current")) {
                        // The key is like projects/CARE/2016-4-d.current
                        String project = parts[1];
                        if (applicationContext.getConfig().isUsersProject(project)) {
                            String current = parts[2].substring(0, parts[2].indexOf("."));
                            ContentInfo info = new ContentInfo(project).withVersion(current).withStatus(ContentInfo.DownloadStatus.NEVER_DOWNLOADED);
                            projects.put(project, info);
                        }
                    }
                }

                // Knowing the current Deployments (in s3), gather their sizes.
                for (S3ObjectSummary summary : s3ObjectSummaries) {
                    Log.i(TAG, String.format("project item (%d) %s", summary.getSize(), summary.getKey()));

                    // The S3 key is like projects/CARE/software-2016-4-d.zip
                    String [] parts = summary.getKey().split("/");
                    if (parts.length == 3 && parts[2].endsWith(".zip")) {
                        String project = parts[1];
                        ContentInfo info = projects.get(project);
                        if (info != null) {
                            // What are the .zip files in the Deployment?
                            String contentZip = "content-" + info.getVersion() + ".zip";
                            // Is this file one of them?
                            if (parts[2].equalsIgnoreCase(contentZip)) {
                                info.addToSize(summary.getSize());
                            }
                        }
                    }
                }
                // Give the results to the caller
                List<ContentInfo> projectList = new ArrayList<>(projects.values());
                projectsListener.onSuccess(projectList);
            }

            @Override
            public void onFailure(Exception ex) {
                projectsListener.onFailure(ex);
            }
        });
    }

    /**
     * Finds local content in the "External Storage Directory", in the "/tbloader/projects" directory.
     * In each project, looks for a .current file to capture what version the data thinks it is.
     * @return A map of project names to local version strings (or "" if the version couldn't be
     *   determined. In that case, we'll need to consider the content stale.
     */
    private Map<String,ContentInfo> findLocalContent() {
        // Projects are kept in /tbloader/projects
        File projectsDir = PathsProvider.getLocalContentDirectory();
        Map<String, ContentInfo> localVersions = new HashMap<>();

        if (projectsDir.exists() && projectsDir.isDirectory()) {
            File[] projects = projectsDir.listFiles();
            // Iterate over the projects, and find local versions...
            for (File project : projects) {
                if (project.isDirectory()) {
                    String projectName = project.getName();
                    // Look for a file named *.current. The '*' part will be the version.
                    File[] versionFile = project.listFiles(new FilenameFilter() {
                        @Override
                        public boolean accept(File dir, String filename) {
                            return filename.toLowerCase().endsWith(".current");
                        }
                    });
                    if (versionFile.length == 1) {
                        // Exactly one .current file, so it's the one.
                        String fileName = versionFile[0].getName();
                        String localVersion = fileName.substring(0, fileName.indexOf('.'));
                        ContentInfo info = new ContentInfo(projectName).withVersion(localVersion).withStatus(ContentInfo.DownloadStatus.DOWNLOADED);
                        localVersions.put(projectName, info);
                        Log.i(TAG, String.format("Found version data for %s: %s", project, localVersion));
                        // We'll keep this one even if we can't connect to the network.
                        mProjects.put(projectName, info);
                    } else if (versionFile.length == 0) {
                        localVersions.put(projectName, new ContentInfo(projectName).withVersion(NO_LOCAL_VERSION).withStatus(ContentInfo.DownloadStatus.NEVER_DOWNLOADED));
                        Log.i(TAG, String.format("No content, but empty directory for %s", project));
                    } else {
                        // Too many: can't determine version.
                        localVersions.put(projectName, new ContentInfo(projectName).withVersion(UNKNOWN_LOCAL_VERSION).withStatus(ContentInfo.DownloadStatus.DOWNLOAD_FAILED));
                        Log.d(TAG, String.format("Found content, but no version data for %s", project));
                    }
                }
            }
        }
        return localVersions;
    }


}
