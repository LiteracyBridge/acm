package org.literacybridge.androidtbloader.content;

import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import org.apache.commons.lang3.StringUtils;
import org.literacybridge.androidtbloader.TBLoaderAppContext;
import org.literacybridge.androidtbloader.checkin.KnownLocations;
import org.literacybridge.androidtbloader.community.CommunityInfo;
import org.literacybridge.androidtbloader.util.PathsProvider;
import org.literacybridge.core.fs.OperationLog;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Description of a Deployment.
 */
public class ContentInfo {
    private static final String TAG = "TBL!:" + "ContentInfo";

    public enum DownloadStatus {
        NEVER_DOWNLOADED,
        WAITING,
        DOWNLOADING,
        PROCESSING,
        DOWNLOADED,
        DOWNLOAD_FAILED,

        NONE,
    }
    private String mFriendlyName;

    // Which S3 bucket (and path)?
    //  deplid=${programid}-${year}-${depl#}-${rev}
    //  "acm-content-updates"
    //     with /projects/${programid}/content-${deplid}.zip
    //                          . . . /${deplid}.current
    // - or -
    //  "amplio-program-content"
    //     with /${programid}/TB-Loaders/published/${deplid}/content-${deplid}.zip
    //                                      . . . /${deplid}.rev
    private String mBucket;
    private String mKey;

    // Like "UWR"
    private String mProgramId;

    // Like "TEST-19-1"
    private String mDeployment;

    // Like "ab"
    private String mRevision;

    // Like "DEMO-2017-2-a"
//    private String mVersion;

    // Like "content-TEST-19-1-ab.zip"
    private String mFilename;

    // Size of the download, the "content-DEMO-2016-2.zip" file
    private long mSize;

    // Track the size and progress of unzipping.
    private long mUnzipTotal = 0;
    private long mUnzipProgress = 0;

    private DownloadStatus mDownloadStatus;

    // If currently downloading, will be non-null
    private ContentDownloader mContentDownloader = null;
    // A client that wants to listen to download state.
    private ContentDownloader.DownloadListener mListener = null;
    // Logger for the download perf.
    private OperationLog.Operation mOpLog;

    // Community list built from the communities in the actual Deployment
    private Map<String, CommunityInfo> mCommunitiesCache = null;

    ContentInfo(String programId) {
        this.mDownloadStatus = DownloadStatus.NONE;
        this.mFriendlyName = null;
        this.mProgramId = programId;
        this.mDeployment = "";
        this.mRevision = "";
//        this.mVersion = "";
    }

//    ContentInfo withVersion(String version) {
//        this.mVersion = version;
//        return this;
//    }

    ContentInfo withFriendlyName(String friendlyName) {
        this.mFriendlyName = friendlyName;
        return this;
    }

    ContentInfo withBucketName(String bucketName) {
        this.mBucket = bucketName;
        return this;
    }

    ContentInfo withDeployment(String deployment) {
        this.mDeployment = deployment;
        return this;
    }

    ContentInfo withRevision(String revision) {
        this.mRevision = revision;
        return this;
    }

    public boolean isNewerRevisionThan(ContentInfo previous) {
        return mRevision.compareToIgnoreCase(previous.getRevision()) > 0;
    }

    ContentInfo withFilename(String filename) {
        this.mFilename = filename;
        return this;
    }

    void setKey(String key) {
        this.mKey = key;
    }

    void setFilename(String filename) {
        this.mFilename = filename;
    }

    void setSize(long size) {
        this.mSize = size;
    }

    ContentInfo withStatus(ContentInfo.DownloadStatus status) {
        this.mDownloadStatus = status;
        return this;
    }

    @Override
    public String toString() {
        return String.format("%s: %s (%d)", mProgramId, mFilename, mSize);
    }

    String getBucketName() {
        return mBucket;
    }

    String getKey() {
        return mKey;
    }

    String getProgramId() {
        return mProgramId;
    }

    String getFriendlyName() {
        if (mFriendlyName == null) {
            mFriendlyName = TBLoaderAppContext.getInstance().getConfig().getFriendlyName(mProgramId);
        }
        return mFriendlyName;
    }


//    public String getVersion() {
//        return mVersion;
//    }

    public String getDeployment() {
        return mDeployment;
    }

    public String getRevision() {
        return mRevision;
    }

    public String getVersionedDeployment() {
        String result = mDeployment;
        if (StringUtils.isNotBlank(mRevision))
            result += "-" + mRevision;
        return result;
    }

    public String getFilename() {
        return mFilename;
    }

    public long getSize() {
        return mSize;
    }

    public boolean isDownloaded() {
        return mDownloadStatus == DownloadStatus.DOWNLOADED;
    }

    DownloadStatus getDownloadStatus() {
        return mDownloadStatus;
    }

    void setDownloadStatus(DownloadStatus status) {
        this.mDownloadStatus = status;
    }

    /**
     * Returns the current progress of any current download, as a percentage.
     * @return The percentage, as an integer.
     */
    int getProgress() {
        int progress = 0;
        switch (mDownloadStatus) {
        case PROCESSING:
            progress = (int) ((double) mUnzipProgress * 100 / mUnzipTotal);
            break;
        case DOWNLOADING:
            long bytesProgress = mContentDownloader.getBytesTransferred();
            progress = (int) ((double) bytesProgress * 100 / mSize);
            break;
        case DOWNLOADED:
            progress = 100;
            break;
        default:
            // leave it at zero
            break;
        }
        return progress;
    }

    /**
     * Is a download currently in progress?
     * @return true if so
     */
    boolean isDownloading() {
        return mContentDownloader != null;
    }

    boolean isUpdateAvailable() {
        // If we want to allow the user to manually choose when to download updates,
        // implement this.
        return false;
    }

    /**
     * Cancels any active download.
     */
    public void cancel() {
        if (mContentDownloader != null) {
            mContentDownloader.cancel();
        }
    }

    /**
     * Starts a download of this Deployment
     * @param applicationContext The application's context
     * @param listener Listener on s3 progress
     * @return true if a download was started, false if one was already in progress
     */
    boolean startDownload(TBLoaderAppContext applicationContext, ContentDownloader.DownloadListener listener) {
        if (mContentDownloader != null) return false;
        mListener = listener;
        mOpLog = OperationLog.startOperation("DownloadContent")
            .put("projectname", getProgramId())
            .put("version", getVersionedDeployment())
            .put("filename", getFilename())
            .put("bytesToDownload", getSize());
        mContentDownloader = new ContentDownloader(this, myDownloadListener);
        mContentDownloader.start();
        mDownloadStatus = DownloadStatus.WAITING;
        return true;
    }

    void setTransferListener(ContentDownloader.DownloadListener downloadListener) {
        mListener = downloadListener;
    }

    private ContentDownloader.DownloadListener myDownloadListener = new ContentDownloader.DownloadListener() {
        @Override
        public void onUnzipProgress(int id, long current, long total) {
            mDownloadStatus = DownloadStatus.PROCESSING;
            mUnzipTotal = total;
            mUnzipProgress = current;
            if (mListener != null) mListener.onUnzipProgress(id, current, total);
        }

        @Override
        public void onStateChanged(int id, TransferState state) {
            if (state == TransferState.COMPLETED ||
                    state == TransferState.CANCELED ||
                    state == TransferState.FAILED) {
                mOpLog.put("endState", state)
                    .finish();
                mOpLog = null;
                mContentDownloader = null;
                if (state == TransferState.COMPLETED) {
                    mDownloadStatus = DownloadStatus.DOWNLOADED;
                } else if (state == TransferState.CANCELED) {
                    mDownloadStatus = DownloadStatus.NEVER_DOWNLOADED;
                } else {
                    mDownloadStatus = DownloadStatus.DOWNLOAD_FAILED;
                }
            } else if (state == TransferState.IN_PROGRESS) {
                mDownloadStatus = DownloadStatus.DOWNLOADING;
            } else if (state == TransferState.WAITING_FOR_NETWORK) {
                mDownloadStatus = DownloadStatus.WAITING;
            }
            if (mListener != null) mListener.onStateChanged(id, state);
        }

        @Override
        public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
            if (mListener != null) mListener.onProgressChanged(id, bytesCurrent, bytesTotal);
        }

        @Override
        public void onError(int id, Exception ex) {
            if (mOpLog != null) mOpLog.put("exception", ex.getMessage());
            if (mListener != null) mListener.onError(id, ex);
        }
    };

    /**
     * Gets a list of the communities in the Deployment. Look for directories, each of which is
     * a community, like this:
     * {project}/content/{Deployment}/communities/{COMMUNITY...}
     * @return A Set of CommunityInfo.
     */
    public Map<String, CommunityInfo> getCommunities() {
        KnownLocations.loadLocationsForProjects(Arrays.asList(getProgramId().toUpperCase()));
        if (mCommunitiesCache == null) {
            Map<String, CommunityInfo> result = new HashMap<>();
            File projectDir = PathsProvider.getLocalContentProjectDirectory(mProgramId);
            File contentDir = new File(projectDir, "content");
            File[] deploymentsDirs = contentDir.listFiles();
            if (deploymentsDirs != null && deploymentsDirs.length == 1) {
                File communitiesDir = new File(deploymentsDirs[0], "communities");
                File[] communities = communitiesDir.listFiles();
                if (communities != null) {
                    for (File community : communities) {
                        String communityName = community.getName().toUpperCase();
                        CommunityInfo ci = KnownLocations.findCommunity(communityName,
                                getProgramId().toUpperCase());
                        if (ci == null) {
                            ci = new CommunityInfo(communityName, getProgramId());
                        }
                        result.put(community.getName(), ci);
                    }
                }
            }
            mCommunitiesCache = result;
        }
        return mCommunitiesCache;
    }

}
