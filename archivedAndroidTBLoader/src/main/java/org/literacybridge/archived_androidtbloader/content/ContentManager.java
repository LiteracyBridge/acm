package org.literacybridge.archived_androidtbloader.content;

import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserSession;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.GenericHandler;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.S3ObjectSummary;

import org.literacybridge.archived_androidtbloader.BuildConfig;
import org.literacybridge.archived_androidtbloader.TBLoaderAppContext;
import org.literacybridge.archived_androidtbloader.community.CommunityInfo;
import org.literacybridge.archived_androidtbloader.signin.UnattendedAuthenticator;
import org.literacybridge.archived_androidtbloader.signin.UserHelper;
import org.literacybridge.archived_androidtbloader.util.Config;
import org.literacybridge.archived_androidtbloader.util.Constants;
import org.literacybridge.archived_androidtbloader.util.PathsProvider;
import org.literacybridge.archived_androidtbloader.util.S3Helper;
import org.literacybridge.core.fs.OperationLog;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private final TBLoaderAppContext applicationContext;

    // A list of all the projects found in s3
    private final Map<String, ContentInfo> mProjects = new HashMap<>();

    private final List<ContentInfo> mContentList = new ArrayList<>();
    private long mContentListTime = 0;  // To determine freshness.

    // A cache of community names for projects, as contained in the Deployments.
    private Map<String, Map<String, CommunityInfo>> mProjectCommunitiesCache = null;

    public ContentManager(TBLoaderAppContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public void onSignOut() {
        mContentList.clear();
        mContentListTime = 0;
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
            if (includeForAllUsers || TBLoaderAppContext.getInstance().getConfig().isProgramIdForUser(entry.getKey())) {
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

    public boolean haveContentInfoForUser() {
        final Config config = TBLoaderAppContext.getInstance().getConfig();
        return mContentList.stream()
            .anyMatch(ci-> {
                return ci.isDownloaded() && config.isProgramIdForUser(ci.getProgramId());
            });
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
                    projectCommunities.put(info.getProgramId(), info.getCommunities());
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
            if (info.getProgramId().equalsIgnoreCase(project))
                return info;
        }
        return null;
    }

    void removeLocalContent(final ContentInfo info) {
        List<String> toClear = new ArrayList<>();
        List<String> toRemove = new ArrayList<>();
        toRemove.add(info.getProgramId());
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
        mContentList.sort(Comparator.comparing(ContentInfo::getFriendlyName, String.CASE_INSENSITIVE_ORDER));
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
        CognitoUserSession session = UserHelper.getInstance().getCurrSession();
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
            onContentListChanged();
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
                        mProjects.put(info.getProgramId(), info);
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
            cloudVersions.put(info.getProgramId(), info);
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
            String localVersion = localItem == null ? null : localItem.getVersionedDeployment();
            if (localVersion == null || localVersion.equals(NO_LOCAL_VERSION)) {
                // No local version, so a cloud item.
                if (BuildConfig.DEBUG && (cloudItem.getDownloadStatus() != ContentInfo.DownloadStatus.NEVER_DOWNLOADED)) {
                    throw new AssertionError("download status is not NEVER_DOWNLOADED");
                }
                mContentList.add(cloudItem);
                mProjects.put(cloudItem.getProgramId(), cloudItem);
            } else if (!cloudItem.getVersionedDeployment().equalsIgnoreCase(localVersion)) {
                // Stale local version, or unknown local version. Remove it; a cloud item.
                projectsToClear.add(project);
                cloudItem.setDownloadStatus(ContentInfo.DownloadStatus.NEVER_DOWNLOADED);
                mContentList.add(cloudItem);
                mProjects.put(cloudItem.getProgramId(), cloudItem);
            } else {
                // have a local, up-to-date version
                cloudItem.setDownloadStatus(ContentInfo.DownloadStatus.DOWNLOADED);
                mContentList.add(cloudItem);
                mProjects.put(cloudItem.getProgramId(), cloudItem);
            }
        }
        // Entries from cloudInfo were copied into cloudVersions, edited from there, updating the entries in cloudInfo.

        removeContent(projectsToClear, projectsToRemove, onFinished);
    }

    private static class BackgroundCleaner extends AsyncTask<Void, Void, Void> {
        final List<String> projectsToClear;
        final List<String> projectsToRemove;
        final Runnable onFinished;
        final OperationLog.Operation opLog;

        public BackgroundCleaner(List<String> projectsToClear,
            List<String> projectsToRemove,
            Runnable onFinished,
            OperationLog.Operation opLog)
        {
            super();
            this.projectsToClear = projectsToClear;
            this.projectsToRemove = projectsToRemove;
            this.onFinished = onFinished;
            this.opLog = opLog;
        }

        @Override
        protected Void doInBackground(Void... voids) {
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
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            opLog.finish();
            onFinished.run();
        }
    }

    private void removeContent(final List<String> projectsToClear, final List<String> projectsToRemove, final Runnable onFinished) {
        final OperationLog.Operation opLog = OperationLog.startOperation("RemoveLocalContent");
        if (projectsToClear.size() > 0) {
            opLog.put("toClear", projectsToClear);
        }
        if (projectsToRemove.size() > 0) {
            opLog.put("toRemove", projectsToRemove);
        }

        new BackgroundCleaner(projectsToClear, projectsToRemove, onFinished, opLog).execute();
    }

    /**
     * Helper to remove the contents of a directory.
     * @param dir File object representing the directory to be emptied.
     */
    private static void emptyDirectory(File dir) {
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
    private static void deleteDirectory(File dir) {
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

    // Matches deploymentName-suffix.current or .rev. Like TEST-19-2-ab.rev
    // group(0) is entire string, like 'TEST-19-2-ab.rev'
    // group(1) is deployment + revision, like 'TEST-19-2-ab'
    // group(2) is deployment, like 'TEST-19-2'
    // group(3) is revision, like 'ab'
    private final Pattern markerPattern = Pattern.compile("((\\w+(?:-\\w+)*)-(\\w+))\\.(current|rev)", Pattern.CASE_INSENSITIVE);

    // group(0) is entire string, like 'content-TEST-19-2-ab.zip' or 'TEST-19-2-ab.zip'
    // group(1) is deployment + revision, like 'TEST-19-2-ab'
    // group(2) is the deployment, like 'TEST-19-2'
    // group(3) is the deployment, like 'ab'
    private final Pattern zipfilePattern = Pattern.compile("((?:content-)?(\\w+(?:-\\w+)*)-(\\w+))\\.zip", Pattern.CASE_INSENSITIVE);

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
                findDeploymentsAndSizes(s3ObjectSummaries, projects, PUBLISH_FORMAT.DEPLOYMENT);
                // Examine the S3 hosted (Smart Sync) programs for the user.
                findCloudContent2(projectsListener, projects);
            }

            @Override
            public void onFailure(Exception ex) {
                projectsListener.onFailure(ex);
            }
        });
    }

    private enum PUBLISH_FORMAT {
        DEPLOYMENT,     // Only the deployment information is in the bucket. (from dropobx)
        PROGRAM         // The entire program database is in the bucket. (from smartsync)
    };

    /**
     * Given a list of s3ObjectSummaries and a map of program id to revision, find the latest
     * deployments from that list, and add or update the list of revisions.
     * @param s3ObjectSummaries The list of S3 objects.
     * @param projects The map of programs to revisions.
     * @param publishFormat The style of bucket being examined.
     */
    private void findDeploymentsAndSizes(List<S3ObjectSummary> s3ObjectSummaries,
                                         Map<String, ContentInfo> projects,
                                         PUBLISH_FORMAT publishFormat) {
        int revKeyLength = publishFormat==PUBLISH_FORMAT.DEPLOYMENT ? 3 : 4;
        int zipKeyLength = publishFormat==PUBLISH_FORMAT.DEPLOYMENT ? 3 : 5;
        int programidIx = publishFormat==PUBLISH_FORMAT.DEPLOYMENT ? 1 : 0;
        // First, look for all of the ".current" (or ".rev") files. This will let us know what versions
        // we are current in s3.
        for (S3ObjectSummary summary : s3ObjectSummaries) {
            String [] parts = summary.getKey().split("/");
            if (parts.length == revKeyLength) {
                Matcher matcher = markerPattern.matcher(parts[revKeyLength-1]);
                if (matcher.matches()) {
                    // The key is like projects/${programid}/TEST-19-2-ab.rev
                    // or ${programid}/TB-Loaders/published/${deplid}.rev
                    String programId = parts[programidIx];
                    String deployment = matcher.group(2);
                    String revision = matcher.group(3);
                    Log.i(TAG, String.format("Found deployment revision %s %s %s", programId, deployment, revision));
                    if (applicationContext.getConfig().isProgramIdForUser(programId)) {
                        ContentInfo info = new ContentInfo(programId)
                            .withBucketName(summary.getBucketName())
                            .withDeployment(deployment)
                            .withRevision(revision)
                            .withStatus(ContentInfo.DownloadStatus.NEVER_DOWNLOADED);
                        ContentInfo previous = projects.get(programId);
                        // If we've already found a marker file. See if this one is
                        // newer, and if so, replace the previous one.
                        if (previous == null || info.isNewerRevisionThan(previous)) {
                            projects.put(programId, info);
                        }
                    }
                }
            }
        }

        // Knowing the current Deployments (in s3), gather their sizes.
        for (S3ObjectSummary summary : s3ObjectSummaries) {

            // The S3 key of the .zip is like projects/${programid}/content-TEST-19-2-ab.zip or projects/TEST/TEST-19-2-ab.zip
            // or like ${programid}/TB-Loaders/published/${deplid}/content-${deplid}.zip
            String [] parts = summary.getKey().split("/");
            if (parts.length == zipKeyLength) {
                Matcher matcher = zipfilePattern.matcher(parts[zipKeyLength-1]);
                if (matcher.matches()) {
                    Log.i(TAG, String.format("project zip item (%d) %s", summary.getSize(), summary.getKey()));
                    String filename = parts[zipKeyLength-1];
                    String project = parts[programidIx];
                    String deployment = matcher.group(2);
                    String revision = matcher.group(3);
                    ContentInfo info = projects.get(project);
                    // If this zip file matches the revision marker, get the size and key.
                    if (info != null && info.getDeployment()
                        .equalsIgnoreCase(deployment) && info.getRevision()
                        .equalsIgnoreCase(revision)) {
                        info.setSize(summary.getSize());
                        info.setFilename(filename);
                        info.setKey(summary.getKey());
                    }
                }
            }
        }
    }

    /**
     * A ListObjectsListener class that can chain through several S3 queries. When all have
     * been completed, the success callback is called. Any error triggers the failure callback.
     *
     * This exists to look for deployed content in Smart Sync programs. Generally a user will have
     * at most one or two programs, so the performance is OK. For HQ folks, who have many programs
     * there can be a significant delay examining all of the programs.
     */
    private class ChainingListener implements S3Helper.ListObjectsListener {
        private final ListContentListener projectsListener;
        private final List<String> programids;
        private final Map<String, ContentInfo> projects;
        final ListObjectsV2Request[] request = new ListObjectsV2Request[1];
        private String continuationToken = null;
        private boolean requestWasTruncated = false;
        private String programid;

        private ChainingListener(ListContentListener projectsListener, List<String> programids, Map<String, ContentInfo> programs) {
            this.projectsListener = projectsListener;
            this.programids = programids;
            this.projects = programs;
        }

        @Override
        public void onSuccess(ListObjectsV2Result result) {
            requestWasTruncated = result.isTruncated();
            continuationToken = result.getNextContinuationToken();
            List<S3ObjectSummary> s3ObjectSummaries = result.getObjectSummaries();
            findDeploymentsAndSizes(s3ObjectSummaries, projects, PUBLISH_FORMAT.PROGRAM);
            next();
        }

        @Override
        public void onFailure(Exception ex) {
            projectsListener.onFailure(ex);
        }

        /**
         * Called to initiate or continue searching for deployments.
         */
        public void next() {
            if (!requestWasTruncated) {
                // The last ListObjects request (if any) has completed. If there are no
                // more program ids in the list, we're done.
                if (programids.size() == 0) {
                    Log.i(TAG, "Done searching S3 programs");

                    // Give the results to the caller
                    List<ContentInfo> projectList = new ArrayList<>(projects.values());
                    projectsListener.onSuccess(projectList);
                    return;
                }
                // next (or first) program id to examiune.
                programid = programids.remove(0);
            }
            Log.i(TAG, String.format("Searching S3 program %s", programid));

            request[0] = new ListObjectsV2Request()
                .withBucketName(Constants.CONTENT_BUCKET_NAME)
                .withPrefix(programid + "/TB-Loaders/published/");
            if (continuationToken != null) {
                request[0].setContinuationToken(continuationToken);
            }

            S3Helper.listObjects(request[0], this);
        }

    }

    /**
     * Examines the deployments of Smart Sync projects, looking for new deployments.
     * @param projectsListener to be called when the process completes or fails.
     * @param partialResult what was found from Dropbox hosted programs.
     */
    private void findCloudContent2(final ListContentListener projectsListener, Map<String, ContentInfo> partialResult) {
        List<String> programids = applicationContext.getConfig().getProgramIdsForUser();

        ChainingListener listener = new ChainingListener(projectsListener, programids, partialResult);
        listener.next();
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
                    String programId = project.getName();
                    // Look for a file named *.current. The '*' part will be the version.
                    File[] versionFile = project.listFiles(new FilenameFilter() {
                        @Override
                        public boolean accept(File dir, String filename) {
                            return markerPattern.matcher(filename).matches();//filename.toLowerCase().endsWith(".current");
                        }
                    });
                    if (versionFile.length == 1) {
                        // Exactly one .current file, so it's the one.
                        String fileName = versionFile[0].getName();
                        Matcher matcher = markerPattern.matcher(fileName);
                        matcher.matches();
                        String deployment = matcher.group(2);
                        String revision = matcher.group(3);
                        ContentInfo info = new ContentInfo(programId)
                            .withDeployment(deployment)
                            .withRevision(revision)
                            .withStatus(ContentInfo.DownloadStatus.DOWNLOADED)
                            .withFilename(fileName);
                        localVersions.put(programId, info);
                        Log.i(TAG, String.format("Found version data for %s: %s", project, info.getVersionedDeployment()));
                        // We'll keep this one even if we can't connect to the network.
                        mProjects.put(programId, info);
                    } else if (versionFile.length == 0) {
                        localVersions.put(programId, new ContentInfo(programId).withDeployment(NO_LOCAL_VERSION).withStatus(ContentInfo.DownloadStatus.NEVER_DOWNLOADED));
                        Log.i(TAG, String.format("No content, but empty directory for %s", project));
                    } else {
                        // Too many: can't determine version.
                        localVersions.put(programId, new ContentInfo(programId).withDeployment(UNKNOWN_LOCAL_VERSION).withStatus(ContentInfo.DownloadStatus.DOWNLOAD_FAILED));
                        Log.d(TAG, String.format("Found content, but no version data for %s", project));
                    }
                }
            }
        }
        return localVersions;
    }


}
