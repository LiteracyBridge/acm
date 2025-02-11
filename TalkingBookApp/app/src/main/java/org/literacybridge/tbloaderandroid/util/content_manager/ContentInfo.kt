package org.literacybridge.tbloaderandroid.util.content_manager
//
//import com.amplifyframework.storage.TransferState
//import org.literacybridge.androidtbloader.App
//import org.literacybridge.androidtbloader.util.LOG_TAG
//
////
////import com.amazonaws.mobileconnectors.s3.transferutility.TransferState
////import org.literacybridge.core.fs.OperationLog
////import org.literacybridge.androidtbloader.App
//
///**
// * Description of a Deployment.
// */
//class ContentInfo internal constructor(// Like "UWR"
//    val programId: String
//) {
//    enum class DownloadStatus {
//        NEVER_DOWNLOADED,
//        WAITING,
//        DOWNLOADING,
//        PROCESSING,
//        DOWNLOADED,
//        DOWNLOAD_FAILED,
//        NONE
//    }
//
//    private var mFriendlyName: String? = null
//
//    // Which S3 bucket (and path)?
//    //  deplid=${programid}-${year}-${depl#}-${rev}
//    //  "acm-content-updates"
//    //     with /projects/${programid}/content-${deplid}.zip
//    //                          . . . /${deplid}.current
//    // - or -
//    //  "amplio-program-content"
//    //     with /${programid}/TB-Loaders/published/${deplid}/content-${deplid}.zip
//    //                                      . . . /${deplid}.rev
//    var bucketName: String? = null
//        private set
//    var key: String? = null
//
//    //    public String getVersion() {
//    //        return mVersion;
//    //    }
//    // Like "TEST-19-1"
//    var deployment = ""
//        private set
//
//    // Like "ab"
//    var revision = ""
//        private set
//
//    // Like "DEMO-2017-2-a"
//    //    private String mVersion;
//    // Like "content-TEST-19-1-ab.zip"
//    var filename: String? = null
//
//    // Size of the download, the "content-DEMO-2016-2.zip" file
//    var size: Long = 0
//
//    // Track the size and progress of unzipping.
//    private var mUnzipTotal: Long = 0
//    private var mUnzipProgress: Long = 0
//    var downloadStatus: DownloadStatus
//
//    // If currently downloading, will be non-null
//    private var mContentDownloader: ContentDownloader? = null
//
//    // A client that wants to listen to download state.
//    private var mListener: ContentDownloader.DownloadListener? = null
//
//    // Logger for the download perf.
//    private var mOpLog: OperationLog.Operation? = null// TODO: implement getCommunities function
////        KnownLocations.loadLocationsForProjects(Arrays.asList(getProgramId().toUpperCase()));
////        if (mCommunitiesCache == null) {
////            Map<String, CommunityInfo> result = new HashMap<>();
////            File projectDir = PathsProvider.getLocalContentProjectDirectory(mProgramId);
////            File contentDir = new File(projectDir, "content");
////            File[] deploymentsDirs = contentDir.listFiles();
////            if (deploymentsDirs != null && deploymentsDirs.length == 1) {
////                File communitiesDir = new File(deploymentsDirs[0], "communities");
////                File[] communities = communitiesDir.listFiles();
////                if (communities != null) {
////                    for (File community : communities) {
////                        String communityName = community.getName().toUpperCase();
////                        CommunityInfo ci = KnownLocations.findCommunity(communityName,
////                                getProgramId().toUpperCase());
////                        if (ci == null) {
////                            ci = new CommunityInfo(communityName, getProgramId());
////                        }
////                        result.put(community.getName(), ci);
////                    }
////                }
////            }
////            mCommunitiesCache = result;
////        }
//    /**
//     * Gets a list of the communities in the Deployment. Look for directories, each of which is
//     * a community, like this:
//     * {project}/content/{Deployment}/communities/{COMMUNITY...}
//     * @return A Set of CommunityInfo.
//     */
//    // Community list built from the communities in the actual Deployment
//    val communities: Map<String, CommunityInfo>? = null
//
//    //    ContentInfo withVersion(String version) {
//    //        this.mVersion = version;
//    //        return this;
//    //    }
//    fun withFriendlyName(friendlyName: String?): ContentInfo {
//        mFriendlyName = friendlyName
//        return this
//    }
//
//    fun withBucketName(bucketName: String?): ContentInfo {
//        this.bucketName = bucketName
//        return this
//    }
//
//    fun withDeployment(deployment: String): ContentInfo {
//        this.deployment = deployment
//        return this
//    }
//
//    fun withRevision(revision: String): ContentInfo {
//        this.revision = revision
//        return this
//    }
//
//    fun isNewerRevisionThan(other: ContentInfo): Boolean {
//        // A longer name is always greater. When the lengths are the same, then we need
//        // to compare the strings.
//        return if (revision.length == other.revision.length) {
//            revision.compareTo(other.revision, ignoreCase = true) > 0
//        } else {
//            revision.length > other.revision.length
//        }
//    }
//
//    fun withFilename(filename: String?): ContentInfo {
//        this.filename = filename
//        return this
//    }
//
//    fun withStatus(status: DownloadStatus): ContentInfo {
//        downloadStatus = status
//        return this
//    }
//
//    override fun toString(): String {
//        return String.format("%s: %s (%d)", programId, filename, size)
//    }
//
//    val friendlyName: String
//        get() {
//            if (mFriendlyName == null) {
//                mFriendlyName = App().config!!.getFriendlyName(
//                    programId
//                )
//            }
//            return mFriendlyName!!
//        }
//    val versionedDeployment: String
//        get() {
//            var result = deployment
//            // TODO: use kotlin checks
////        if (StringUtils.isNotBlank(mRevision))
//            result += "-" + revision
//            return result
//        }
//    val isDownloaded: Boolean
//        get() = downloadStatus == DownloadStatus.DOWNLOADED
//    val progress: Int
//        /**
//         * Returns the current progress of any current download, as a percentage.
//         * @return The percentage, as an integer.
//         */
//        get() {
//            var progress = 0
//            when (downloadStatus) {
//                DownloadStatus.PROCESSING -> progress =
//                    (mUnzipProgress.toDouble() * 100 / mUnzipTotal).toInt()
//
//                DownloadStatus.DOWNLOADING -> {
//                    val bytesProgress = mContentDownloader!!.bytesTransferred
//                    progress = (bytesProgress.toDouble() * 100 / size).toInt()
//                }
//
//                DownloadStatus.DOWNLOADED -> progress = 100
//                else -> {}
//            }
//            return progress
//        }
//    val isDownloading: Boolean
//        /**
//         * Is a download currently in progress?
//         * @return true if so
//         */
//        get() = mContentDownloader != null
//    val isUpdateAvailable: Boolean
//        get() =// If we want to allow the user to manually choose when to download updates,
//            // implement this.
//            false
//
//    /**
//     * Cancels any active download.
//     */
//    fun cancel() {
//        if (mContentDownloader != null) {
//            mContentDownloader!!.cancel()
//        }
//    }
//
//    /**
//     * Starts a download of this Deployment
//     *
//     * @param applicationContext
//     * @param listener           Listener on s3 progress
//     * @return true if a download was started, false if one was already in progress
//     */
//    fun startDownload(
//        listener: ContentDownloader.DownloadListener?
//    ): Boolean {
//        if (mContentDownloader != null) return false
//
//        mListener = listener
//        mOpLog = OperationLog.startOperation("DownloadContent")
//            .put("projectname", programId)
//            .put("version", versionedDeployment)
//            .put("filename", filename)
//            .put("bytesToDownload", size)
//        mContentDownloader = ContentDownloader(this, myDownloadListener)
//        mContentDownloader!!.start()
//        downloadStatus = DownloadStatus.WAITING
//        return true
//    }
//
//    fun setTransferListener(downloadListener: ContentDownloader.DownloadListener?) {
//        mListener = downloadListener
//    }
//
//    private val myDownloadListener: ContentDownloader.DownloadListener =
//        object : ContentDownloader.DownloadListener {
//            override fun onUnzipProgress(id: Int, current: Long, total: Long) {
//                downloadStatus = DownloadStatus.PROCESSING
//                mUnzipTotal = total
//                mUnzipProgress = current
//                if (mListener != null) mListener!!.onUnzipProgress(id, current, total)
//            }
//
//            override fun onStateChanged(id: Int, state: TransferState) {
//                if (state == TransferState.COMPLETED || state == TransferState.CANCELED || state == TransferState.FAILED) {
//                    mOpLog!!.put("endState", state)
//                        .finish()
//                    mOpLog = null
//                    mContentDownloader = null
//                    if (state == TransferState.COMPLETED) {
//                        downloadStatus = DownloadStatus.DOWNLOADED
//                    } else if (state == TransferState.CANCELED) {
//                        downloadStatus = DownloadStatus.NEVER_DOWNLOADED
//                    } else {
//                        downloadStatus = DownloadStatus.DOWNLOAD_FAILED
//                    }
//                } else if (state == TransferState.IN_PROGRESS) {
//                    downloadStatus = DownloadStatus.DOWNLOADING
//                } else if (state == TransferState.WAITING_FOR_NETWORK) {
//                    downloadStatus = DownloadStatus.WAITING
//                }
//                if (mListener != null) mListener!!.onStateChanged(id, state)
//            }
//
//            override fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long) {
//                if (mListener != null) mListener!!.onProgressChanged(id, bytesCurrent, bytesTotal)
//            }
//
//            override fun onError(id: Int, ex: Exception) {
//                if (mOpLog != null) mOpLog!!.put("exception", ex.message)
//                if (mListener != null) mListener!!.onError(id, ex)
//            }
//        }
//
//    init {
//        downloadStatus = DownloadStatus.NONE
//        //        this.mVersion = "";
//    }
//
//    companion object {
//        private val TAG = "$LOG_TAG: ContentInfo"
//    }
//}
