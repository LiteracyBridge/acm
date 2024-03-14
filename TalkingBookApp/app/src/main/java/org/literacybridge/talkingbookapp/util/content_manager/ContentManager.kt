package org.literacybridge.talkingbookapp.util.content_manager

//import com.amazonaws.services.s3.model.ListObjectsV2Request

//import com.amazonaws.services.s3.model.ListObjectsV2Result
import android.content.Intent
import android.os.AsyncTask
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import aws.sdk.kotlin.services.s3.model.ListObjectsV2Request
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState
import com.amazonaws.services.s3.model.ListObjectsV2Result
import com.amazonaws.services.s3.model.S3ObjectSummary
import org.literacybridge.core.fs.OperationLog
import org.literacybridge.talkingbookapp.App
import org.literacybridge.talkingbookapp.util.CONTENT_BUCKET_NAME
import org.literacybridge.talkingbookapp.util.DEPLOYMENTS_BUCKET_NAME
import org.literacybridge.talkingbookapp.util.PathsProvider
import org.literacybridge.talkingbookapp.util.content_manager.S3Helper.ListObjectsListener
import org.literacybridge.talkingbookapp.util.content_manager.S3Helper.listObjects
import java.io.File
import java.util.regex.Pattern

/**
 * Manages lists of available Deployments for projects.
 */
class ContentManager(private val applicationContext: App) {
    enum class Flags {
        ForUser,

        // Only for the current user (generally the default)
        ForAll,

        // For all users
        Local,

        // Only local content (exclude cloud)
        Cloud,

        // Only cloud content (exclude local)
        All;

        // All
        fun inList(flags: Array<Flags>): Boolean {
            for (f in flags) {
                if (f == this) return true
            }
            return false
        }
    }

    private abstract class ListContentListener {
        abstract fun onSuccess(projects: List<ContentInfo>)
        open fun onFailure(ex: Exception?) {}
    }

    // A list of all the projects found in s3
    private val mProjects: MutableMap<String, ContentInfo> = HashMap()
    private val mContentList: MutableList<ContentInfo> = ArrayList()
    private var mContentListTime: Long = 0 // To determine freshness.

    // A cache of community names for projects, as contained in the Deployments.
    private var mProjectCommunitiesCache: Map<String, Map<String, CommunityInfo>>? = null
    fun onSignOut() {
        mContentList.clear()
        mContentListTime = 0
    }

    fun startDownload(info: ContentInfo, listener: ContentDownloader.DownloadListener) {
        info.startDownload(applicationContext, object : ContentDownloader.DownloadListener {
            override fun onUnzipProgress(id: Int, current: Long, total: Long) {
                listener.onUnzipProgress(id, current, total)
            }

            override fun onStateChanged(id: Int, state: TransferState) {
                mProjectCommunitiesCache = null
                listener.onStateChanged(id, state)
            }

            override fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long) {
                listener.onProgressChanged(id, bytesCurrent, bytesTotal)
            }

            override fun onError(id: Int, ex: Exception) {
                listener.onError(id, ex)
            }
        })
    }

    /**
     * Gets the names of the known projects. Can be filtered to "local" or to "cloud". By
     * default is filtered to this user's projects, but can be expanded to all.
     *
     * @param flags Flags to control the filtering.
     * @return A Set&lt;String&gt; of file names.
     */
    fun getProjectNames(flags: Array<Flags>): Set<String> {
        // By default include both local and cloud. If _either_ flag is set, include what the flags say.
        // Include local content if the local flag is set, or if neither local nor cloud is set.
        val includeLocal = Flags.Local.inList(flags) || !Flags.Cloud.inList(flags)
        // Similarly for cloud content.
        val includeCloud = Flags.Cloud.inList(flags) || !Flags.Local.inList(flags)

        // Default is only for the user. If ForAll is set, include all content.
        val includeForAllUsers = Flags.ForAll.inList(flags)
        val result: MutableSet<String> = HashSet()
        for ((key, value) in mProjects) {
            if (includeForAllUsers || App().config!!.isProgramIdForUser(
                    key
                )
            ) {
                if (value.downloadStatus == ContentInfo.DownloadStatus.DOWNLOADED) {
                    if (includeLocal) {
                        result.add(key)
                    }
                } else {
                    if (includeCloud) {
                        result.add(key)
                    }
                }
            }
        }
        return result
    }

    /**
     * Is there content info?
     *
     * @return true if so.
     */
    fun haveContentInfo(): Boolean {
        return mContentList.size > 0
    }

    fun haveContentInfoForUser(): Boolean {
        val config = App().config
        return mContentList.stream()
            .anyMatch { ci: ContentInfo -> ci.isDownloaded && config!!.isProgramIdForUser(ci.programId) }
    }

    val contentList: List<ContentInfo>
        get() = mContentList

    // Add (Flags... flags) argument, and the following allows getting projects for this user,
// all users, downloaded, cloud, or all.
//        List<ContentInfo> result = new ArrayList<>();
//        for (String projectName : getProjectNames(flags)) {
//            result.add(mProjects.get(projectName));
//        }
//        return result;
    private val communitiesForProjects: Map<String, Map<String, CommunityInfo>>?
        private get() {
            // TODO: read for datastore/user view model
//            if (mProjectCommunitiesCache == null) {
//                val projectCommunities: MutableMap<String, Map<String, CommunityInfo>> = HashMap()
//                for (info in mContentList) {
//                    if (info.downloadStatus == ContentInfo.DownloadStatus.DOWNLOADED) {
//                        projectCommunities[info.programId] = info.communities
//                    }
//                }
//                mProjectCommunitiesCache = projectCommunities
//            }
            return mProjectCommunitiesCache
        }

    fun getCommunitiesForProjects(projects: List<String>): Map<String, Map<String, CommunityInfo>> {
        val pc = communitiesForProjects
        val resultSet: MutableMap<String, Map<String, CommunityInfo>> = HashMap()
        for (project in projects) {
            val communities = pc!![project]
            if (communities != null) {
                resultSet[project] = communities
            }
        }
        return resultSet
    }

    fun getContentInfo(project: String?): ContentInfo? {
        for (info in mContentList) {
            if (info.programId.equals(project, ignoreCase = true)) return info
        }
        return null
    }

    fun removeLocalContent(info: ContentInfo) {
        val toClear: List<String> = ArrayList()
        val toRemove: MutableList<String> = ArrayList()
        toRemove.add(info.programId)
        removeContent(toClear, toRemove) { // Move from cloud list to local list.
            info.downloadStatus =
                ContentInfo.DownloadStatus.NEVER_DOWNLOADED
            mProjectCommunitiesCache = null
            onContentListChanged()
        }
    }

    private fun onContentListChanged() {
        mContentList.sortWith(
            Comparator.comparing(
                { obj: ContentInfo -> obj.friendlyName }, java.lang.String.CASE_INSENSITIVE_ORDER
            )
        )
        mContentListTime = System.currentTimeMillis()
        val intent = Intent(CONTENT_LIST_CHANGED_EVENT)
        LocalBroadcastManager.getInstance(App()).sendBroadcast(intent)
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
    fun fetchContentList() {
        authenticateAndFetchContentList(false)
    }

    fun refreshContentList() {
        authenticateAndFetchContentList(true)
    }

    private fun authenticateAndFetchContentList(force: Boolean) {
//        CognitoUserSession session = UserHelper.getInstance().getCurrSession();
//        if (session == null || !session.isValid()) {
//            Log.d(TAG, String.format("authenticateAndFetchContentList: %s session, trying to authenticate", session == null ? "no" : "invalid"));
//
//            new UnattendedAuthenticator(new GenericHandler() {
//                @Override
//                public void onSuccess() {
//                    Log.d(TAG, "authenticateAndFetchContentList: authentication success");
//                    fetchContentList(force);
//                }
//
//                @Override
//                public void onFailure(Exception exception) {
//                    Log.d(TAG, "authenticateAndFetchContentList: authentication failure");
//                    if (force) {
//                        Toast mToast = Toast.makeText(TBLoaderAppContext.getInstance(),
//                                "Authentication Failure", Toast.LENGTH_SHORT);
//                        mToast.show();
//                    }
//                    fetchContentList(force);
//                }
//            }).authenticate();
//        } else {
//            fetchContentList(force);
//        }
        fetchContentList(force)
    }

    /**
     * Implementation of fetchContentList
     *
     * @param forceUpdate If true refresh even if the data is "fresh".
     */
    private fun fetchContentList(forceUpdate: Boolean) {
        val opLog = OperationLog.startOperation("RefreshContentList")
        val localVersions = findLocalContent()

        // If the data is already "fresh", we may avoid some work.
        if (!forceUpdate && System.currentTimeMillis() <= mContentListTime + CONTENT_UPDATE_FRESH) {
            onContentListChanged()
            return
        }

        // Reconcile with cloud (if we can).
        findCloudContent(object : ListContentListener() {
            fun addProjectsToLog() {
                val local: MutableList<String> = ArrayList()
                val cloud: MutableList<String> = ArrayList()
                for ((key, value) in mProjects) {
                    if (value.downloadStatus == ContentInfo.DownloadStatus.DOWNLOADED) {
                        local.add(key)
                    } else {
                        cloud.add(key)
                    }
                }
                opLog.put<List<String>>("local", local)
                    .put<List<String>>("cloud", cloud)
            }

            override fun onSuccess(cloudInfo: List<ContentInfo>) {
                reconcileCloudVersions(localVersions, cloudInfo) {
                    addProjectsToLog()
                    opLog.finish()
                    onContentListChanged()
                }
            }

            override fun onFailure(ex: Exception?) {
                Log.d(TAG, "Can't get cloud content")
                // No cloud data to invalidate local data, so just take the local values.
                mContentList.clear()
                for (info in localVersions.values) {
                    if (info.downloadStatus == ContentInfo.DownloadStatus.DOWNLOADED) {
                        mProjects[info.programId] = info
                        mContentList.add(info)
                    }
                }
                mProjectCommunitiesCache = null
                addProjectsToLog()
                opLog.put("exception", ex)
                    .finish()
                onContentListChanged()
            }
        })
    }

    private fun reconcileCloudVersions(
        localVersions: Map<String, ContentInfo>,
        cloudInfo: List<ContentInfo>,
        onFinished: Runnable
    ) {
        val projectsToRemove: MutableList<String> = ArrayList()
        val projectsToClear: MutableList<String> = ArrayList()
        val cloudVersions: MutableMap<String, ContentInfo> = HashMap()
        for (info in cloudInfo) {
            cloudVersions[info.programId] = info
        }
        mContentList.clear()
        mProjectCommunitiesCache = null

        // Look for local projects no longer in the cloud.
        for (project in localVersions.keys) {
            if (!cloudVersions.containsKey(project)) {
                projectsToRemove.add(project)
            }
        }

        // Look at cloud info; find stale local versions.
        for ((project, cloudItem) in cloudVersions) {
            // Local version, if we have one.
            val localItem = localVersions[project]
            val localVersion = localItem?.versionedDeployment
            if (localVersion == null || localVersion == NO_LOCAL_VERSION) {
                // No local version, so a cloud item.
//                if (DEBUG && (cloudItem.getDownloadStatus() != ContentInfo.DownloadStatus.NEVER_DOWNLOADED)) {
//                    throw new AssertionError("download status is not NEVER_DOWNLOADED");
//                }
                mContentList.add(cloudItem)
                mProjects[cloudItem.programId] = cloudItem
            } else if (!cloudItem.versionedDeployment.equals(localVersion, ignoreCase = true)) {
                // Stale local version, or unknown local version. Remove it; a cloud item.
                projectsToClear.add(project)
                cloudItem.downloadStatus = ContentInfo.DownloadStatus.NEVER_DOWNLOADED
                mContentList.add(cloudItem)
                mProjects[cloudItem.programId] = cloudItem
            } else {
                // have a local, up-to-date version
                cloudItem.downloadStatus = ContentInfo.DownloadStatus.DOWNLOADED
                mContentList.add(cloudItem)
                mProjects[cloudItem.programId] = cloudItem
            }
        }
        // Entries from cloudInfo were copied into cloudVersions, edited from there, updating the entries in cloudInfo.
        removeContent(projectsToClear, projectsToRemove, onFinished)
    }

    private class BackgroundCleaner(
        val projectsToClear: List<String>,
        val projectsToRemove: List<String>,
        val onFinished: Runnable,
        val opLog: OperationLog.Operation
    ) : AsyncTask<Void?, Void?, Void?>() {
        protected override fun doInBackground(vararg params: Void?): Void? {
            Log.i(TAG, "Removing obsolete projects and content")
            val projectsDir = PathsProvider.getLocalContentDirectory()
            try {
                // Projects
                for (project in projectsToRemove) {
                    Log.d(TAG, String.format("Completely removing %s", project))
                    val projectDir = File(projectsDir, project)
                    if (projectDir.exists()) {
                        deleteDirectory(projectDir)
                    }
                }
                // Content only
                for (project in projectsToClear) {
                    Log.d(TAG, String.format("Removing content from %s", project))
                    val projectDir = File(projectsDir, project)
                    if (projectDir.exists()) {
                        emptyDirectory(projectDir)
                    }
                }
            } catch (e: Exception) {
                opLog.put("exception", e)
                Log.d(TAG, "Exception removing files", e)
            }
            return null
        }

        override fun onPostExecute(aVoid: Void?) {
            opLog.finish()
            onFinished.run()
        }

    }

    private fun removeContent(
        projectsToClear: List<String>,
        projectsToRemove: List<String>,
        onFinished: Runnable
    ) {
        val opLog = OperationLog.startOperation("RemoveLocalContent")
        if (projectsToClear.size > 0) {
            opLog.put("toClear", projectsToClear)
        }
        if (projectsToRemove.size > 0) {
            opLog.put("toRemove", projectsToRemove)
        }
        BackgroundCleaner(projectsToClear, projectsToRemove, onFinished, opLog).execute()
    }

    // Matches deploymentName-suffix.current or .rev. Like TEST-19-2-ab.rev
    // group(0) is entire string, like 'TEST-19-2-ab.rev'
    // group(1) is deployment + revision, like 'TEST-19-2-ab'
    // group(2) is deployment, like 'TEST-19-2'
    // group(3) is revision, like 'ab'
    private val markerPattern =
        Pattern.compile("((\\w+(?:-\\w+)*)-(\\w+))\\.(current|rev)", Pattern.CASE_INSENSITIVE)

    // group(0) is entire string, like 'content-TEST-19-2-ab.zip' or 'TEST-19-2-ab.zip'
    // group(1) is deployment + revision, like 'TEST-19-2-ab'
    // group(2) is the deployment, like 'TEST-19-2'
    // group(3) is the deployment, like 'ab'
    private val zipfilePattern =
        Pattern.compile("((?:content-)?(\\w+(?:-\\w+)*)-(\\w+))\\.zip", Pattern.CASE_INSENSITIVE)

    /**
     * Retrieves a list of the projects and current Deployments, from the S3 bucket.
     *
     * @param projectsListener Callback to receive results.
     */
    private fun findCloudContent(projectsListener: ListContentListener) {
        val request = ListObjectsV2Request {
            bucket = DEPLOYMENTS_BUCKET_NAME
            prefix = "projects/"
        }
//            .withBucketName(DEPLOYMENTS_BUCKET_NAME)
//            .withPrefix("projects/")

        // Get the list of objects
        listObjects(request, object : ListObjectsListener {
            override fun onSuccess(result: ListObjectsV2Result?) {
                val projects: MutableMap<String, ContentInfo> = HashMap()
                val s3ObjectSummaries = result!!.objectSummaries

                // Examine all the objects, and figure out the projects, their sizes, and current update.
                findDeploymentsAndSizes(s3ObjectSummaries, projects, PUBLISH_FORMAT.DEPLOYMENT)
                // Examine the S3 hosted (Smart Sync) programs for the user.
                findCloudContent2(projectsListener, projects)
            }

            override fun onFailure(ex: Exception?) {
                projectsListener.onFailure(ex)
            }
        })
    }

    private enum class PUBLISH_FORMAT {
        DEPLOYMENT,

        // Only the deployment information is in the bucket. (from dropobx)
        PROGRAM // The entire program database is in the bucket. (from smartsync)
    }

    /**
     * Given a list of s3ObjectSummaries and a map of program id to revision, find the latest
     * deployments from that list, and add or update the list of revisions.
     *
     * @param s3ObjectSummaries The list of S3 objects.
     * @param projects          The map of programs to revisions.
     * @param publishFormat     The style of bucket being examined.
     */
    private fun findDeploymentsAndSizes(
        s3ObjectSummaries: List<S3ObjectSummary>,
        projects: MutableMap<String, ContentInfo>,
        publishFormat: PUBLISH_FORMAT
    ) {
        val revKeyLength = if (publishFormat == PUBLISH_FORMAT.DEPLOYMENT) 3 else 4
        val zipKeyLength = if (publishFormat == PUBLISH_FORMAT.DEPLOYMENT) 3 else 5
        val programidIx = if (publishFormat == PUBLISH_FORMAT.DEPLOYMENT) 1 else 0
        // First, look for all of the ".current" (or ".rev") files. This will let us know what versions
        // we are current in s3.
        for (summary in s3ObjectSummaries) {
            val parts = summary.key.split("/".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()
            if (parts.size == revKeyLength) {
                val matcher = markerPattern.matcher(parts[revKeyLength - 1])
                if (matcher.matches()) {
                    // The key is like projects/${programid}/TEST-19-2-ab.rev
                    // or ${programid}/TB-Loaders/published/${deplid}.rev
                    val programId = parts[programidIx]
                    val deployment = matcher.group(2)
                    val revision = matcher.group(3)
                    Log.i(
                        TAG,
                        String.format(
                            "Found deployment revision %s %s %s",
                            programId,
                            deployment,
                            revision
                        )
                    )
                    if (applicationContext.config!!.isProgramIdForUser(programId)) {
                        val info = ContentInfo(programId)
                            .withBucketName(summary.bucketName)
                            .withDeployment(deployment)
                            .withRevision(revision)
                            .withStatus(ContentInfo.DownloadStatus.NEVER_DOWNLOADED)
                        val previous = projects[programId]
                        // If we've already found a marker file. See if this one is
                        // newer, and if so, replace the previous one.
                        if (previous == null || info.isNewerRevisionThan(previous)) {
                            projects[programId] = info
                        }
                    }
                }
            }
        }

        // Knowing the current Deployments (in s3), gather their sizes.
        for (summary in s3ObjectSummaries) {

            // The S3 key of the .zip is like projects/${programid}/content-TEST-19-2-ab.zip or projects/TEST/TEST-19-2-ab.zip
            // or like ${programid}/TB-Loaders/published/${deplid}/content-${deplid}.zip
            val parts = summary.key.split("/".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()
            if (parts.size == zipKeyLength) {
                val matcher = zipfilePattern.matcher(parts[zipKeyLength - 1])
                if (matcher.matches()) {
                    Log.i(TAG, String.format("project zip item (%d) %s", summary.size, summary.key))
                    val filename = parts[zipKeyLength - 1]
                    val project = parts[programidIx]
                    val deployment = matcher.group(2)
                    val revision = matcher.group(3)
                    val info = projects[project]
                    // If this zip file matches the revision marker, get the size and key.
                    if (info != null && info.deployment
                            .equals(deployment, ignoreCase = true) && info.revision
                            .equals(revision, ignoreCase = true)
                    ) {
                        info.size = summary.size
                        info.filename = filename
                        info.key = summary.key
                    }
                }
            }
        }
    }

    /**
     * A ListObjectsListener class that can chain through several S3 queries. When all have
     * been completed, the success callback is called. Any error triggers the failure callback.
     *
     *
     * This exists to look for deployed content in Smart Sync programs. Generally a user will have
     * at most one or two programs, so the performance is OK. For HQ folks, who have many programs
     * there can be a significant delay examining all of the programs.
     */
    private inner class ChainingListener(
        private val projectsListener: ListContentListener,
        private val programids: List<String>,
        private val projects: MutableMap<String, ContentInfo>
    ) : ListObjectsListener {
        val request = arrayOfNulls<ListObjectsV2Request>(1)
        private var continuationToken: String? = null
        private var requestWasTruncated = false
        private var programid: String? = null
        override fun onSuccess(result: ListObjectsV2Result?) {
            requestWasTruncated = result!!.isTruncated
            continuationToken = result.nextContinuationToken
            val s3ObjectSummaries = result.objectSummaries
            findDeploymentsAndSizes(s3ObjectSummaries, projects, PUBLISH_FORMAT.PROGRAM)
            next()
        }

        override fun onFailure(ex: Exception?) {
            projectsListener.onFailure(ex)
        }

        /**
         * Called to initiate or continue searching for deployments.
         */
        operator fun next() {
            if (!requestWasTruncated) {
                // The last ListObjects request (if any) has completed. If there are no
                // more program ids in the list, we're done.
                if (programids.size == 0) {
                    Log.i(TAG, "Done searching S3 programs")

                    // Give the results to the caller
                    val projectList: List<ContentInfo> = ArrayList(projects.values)
                    projectsListener.onSuccess(projectList)
                    return
                }
                // next (or first) program id to examiune.
                programid = programids.elementAt(0)
            }
            Log.i(TAG, String.format("Searching S3 program %s", programid))
            request[0] = ListObjectsV2Request {
                bucket = CONTENT_BUCKET_NAME
                prefix = "$programid/TB-Loaders/published/"
            }
//                ListObjectsV2Request.
//                .withBucketName(CONTENT_BUCKET_NAME)
//                .withPrefix("$programid/TB-Loaders/published/")
//            if (continuationToken != null) {
//                request[0]?.continuationToken = continuationToken
//            }
            listObjects(request[0], this)
        }
    }

    /**
     * Examines the deployments of Smart Sync projects, looking for new deployments.
     *
     * @param projectsListener to be called when the process completes or fails.
     * @param partialResult    what was found from Dropbox hosted programs.
     */
    private fun findCloudContent2(
        projectsListener: ListContentListener,
        partialResult: MutableMap<String, ContentInfo>
    ) {
        val programids = applicationContext.config!!.programIdsForUser
        val listener = ChainingListener(projectsListener, programids, partialResult)
        listener.next()
    }

    /**
     * Finds local content in the "External Storage Directory", in the "/tbloader/projects" directory.
     * In each project, looks for a .current file to capture what version the data thinks it is.
     *
     * @return A map of project names to local version strings (or "" if the version couldn't be
     * determined. In that case, we'll need to consider the content stale.
     */
    private fun findLocalContent(): Map<String, ContentInfo> {
        // Projects are kept in /tbloader/projects
        val projectsDir = PathsProvider.getLocalContentDirectory()
        val localVersions: MutableMap<String, ContentInfo> = HashMap()
        if (projectsDir.exists() && projectsDir.isDirectory) {
            val projects = projectsDir.listFiles()
            // Iterate over the projects, and find local versions...
            for (project in projects) {
                if (project.isDirectory) {
                    val programId = project.name
                    // Look for a file named *.current. The '*' part will be the version.
                    val versionFile = project.listFiles { dir, filename ->
                        markerPattern.matcher(filename)
                            .matches() //filename.toLowerCase().endsWith(".current");
                    }
                    if (versionFile.size == 1) {
                        // Exactly one .current file, so it's the one.
                        val fileName = versionFile[0].name
                        val matcher = markerPattern.matcher(fileName)
                        matcher.matches()
                        val deployment = matcher.group(2)
                        val revision = matcher.group(3)
                        val info = ContentInfo(programId)
                            .withDeployment(deployment)
                            .withRevision(revision)
                            .withStatus(ContentInfo.DownloadStatus.DOWNLOADED)
                            .withFilename(fileName)
                        localVersions[programId] = info
                        Log.i(
                            TAG,
                            String.format(
                                "Found version data for %s: %s",
                                project,
                                info.versionedDeployment
                            )
                        )
                        // We'll keep this one even if we can't connect to the network.
                        mProjects[programId] = info
                    } else if (versionFile.size == 0) {
                        localVersions[programId] = ContentInfo(programId).withDeployment(
                            NO_LOCAL_VERSION
                        ).withStatus(ContentInfo.DownloadStatus.NEVER_DOWNLOADED)
                        Log.i(TAG, String.format("No content, but empty directory for %s", project))
                    } else {
                        // Too many: can't determine version.
                        localVersions[programId] = ContentInfo(programId).withDeployment(
                            UNKNOWN_LOCAL_VERSION
                        ).withStatus(ContentInfo.DownloadStatus.DOWNLOAD_FAILED)
                        Log.d(
                            TAG,
                            String.format("Found content, but no version data for %s", project)
                        )
                    }
                }
            }
        }
        return localVersions
    }

    companion object {
        private const val TAG = "TBL!:" + "ContentManager"
        private const val UNKNOWN_LOCAL_VERSION = "--"
        private const val NO_LOCAL_VERSION = ""
        private const val CONTENT_UPDATE_FRESH = (60 * 1000 // Fresh if newer than 60 seconds.
                ).toLong()
        const val CONTENT_LIST_CHANGED_EVENT = "ContentListChanged"

        /**
         * Helper to remove the contents of a directory.
         *
         * @param dir File object representing the directory to be emptied.
         */
        private fun emptyDirectory(dir: File) {
            if (!dir.exists() || !dir.isDirectory) return
            val contents = dir.listFiles()
            for (c in contents) deleteDirectory(c)
        }

        /**
         * Helper to remove a file or a directory and all of its contents.
         *
         * @param dir File object representing the directory to be removed.
         */
        private fun deleteDirectory(dir: File) {
            if (!dir.exists()) return
            if (!dir.isDirectory) {
                dir.delete()
                return
            }
            val contents = dir.listFiles()
            for (c in contents) deleteDirectory(c)
            dir.delete()
        }
    }
}
