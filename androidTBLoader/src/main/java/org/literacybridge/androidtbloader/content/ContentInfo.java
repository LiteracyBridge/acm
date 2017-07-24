package org.literacybridge.androidtbloader.content;

import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;

import org.literacybridge.androidtbloader.TBLoaderAppContext;
import org.literacybridge.androidtbloader.checkin.KnownLocations;
import org.literacybridge.androidtbloader.community.CommunityInfo;
import org.literacybridge.androidtbloader.util.PathsProvider;
import org.literacybridge.core.fs.OperationLog;

import java.io.File;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Description of a Content Update.
 */
public class ContentInfo {
    private static final String TAG = "TBL!:" + "ContentInfo";

    public enum DownloadStatus {
        NEVER_DOWNLOADED,
        DOWNLOADED,
        DOWNLOAD_FAILED,

        NONE,
    }
    // Like "UWR"
    private String mProjectName;

    // Like "DEMO-2017-2-a"
    private String mVersion;

    // Date that the Content Update expires, if any
    private Date mExpiration;

    // Size of the download, the "content-DEMO-2016-2.zip" file
    private long mSize;

    private DownloadStatus mDownloadStatus;

    // If currently downloading, will be non-null
    private ContentDownloader mContentDownloader = null;
    // A client that wants to listen to download state.
    private TransferListener mListener = null;
    // Logger for the download perf.
    private OperationLog.Operation mOpLog;

    // Community list built from the communities in the actual content update
    private Map<String, CommunityInfo> mCommunitiesCache = null;

    ContentInfo(String projectName) {
        this.mProjectName = projectName;
        this.mVersion = "";
    }

    ContentInfo withVersion(String version) {
        this.mVersion = version;
        return this;
    }

    public ContentInfo withExpiration(Date expiration) {
        this.mExpiration = expiration;
        return this;
    }

    public ContentInfo withSize(long size) {
        this.mSize = size;
        return this;
    }

    ContentInfo withStatus(ContentInfo.DownloadStatus status) {
        this.mDownloadStatus = status;
        return this;
    }

    @Override
    public String toString() {
        return String.format("%s: %s (%d)", mProjectName, mVersion, mSize);
    }

    public String getProjectName() {
        return mProjectName;
    }

    public String getVersion() {
        return mVersion;
    }

    public Date getExpiration() {
        return mExpiration;
    }
    public boolean hasExpiration() {
        return mExpiration != null && mExpiration.getTime() != 0;
    }

    public long getSize() {
        return mSize;
    }

    long addToSize(long size) {
        this.mSize += size;
        return this.mSize;
    }

    public DownloadStatus getDownloadStatus() {
        return mDownloadStatus;
    }

    void setDownloadStatus(DownloadStatus status) {
        this.mDownloadStatus = status;
    }

    public void setDownloader(ContentDownloader contentDownloader) {
        this.mContentDownloader = contentDownloader;
    }

    /**
     * Returns the current progress of any current download, as a percentage.
     * @return The percentage, as an integer.
     */
    public int getProgress() {
        int progress = 0;
        if (mContentDownloader != null && mSize != 0) {
            long bytesProgress = mContentDownloader.getBytesTransferred();
            progress = (int) ((double) bytesProgress * 100 / mSize);
        }
        return progress;
    }

    /**
     * Is a download currently in progress?
     * @return true if so
     */
    public boolean isDownloading() {
        return mContentDownloader != null;
    }

    public boolean isUpdateAvailable() {
        // If we want to allow the user to manually choose when to download updates,
        // implement this.
        return false;
    }

    /**
     * Starts a download of this Content Update
     * @param applicationContext The application's context
     * @param listener Listener on s3 progress
     * @return true if a download was started, false if one was already in progress
     */
    boolean startDownload(TBLoaderAppContext applicationContext, TransferListener listener) {
        if (mContentDownloader != null) return false;
        mListener = listener;
        mOpLog = OperationLog.startOperation("DownloadContent")
            .put("projectname", getProjectName())
            .put("version", getVersion())
            .put("bytesToDownload", getSize());
        mContentDownloader = new ContentDownloader(this, myTransferListener);
        mContentDownloader.start();
        return true;
    }

    public void setTransferListener(TransferListener transferListener) {
        mListener = transferListener;
    }

    private TransferListener myTransferListener = new TransferListener() {
        @Override
        public void onStateChanged(int id, TransferState state) {
            if (state == TransferState.COMPLETED ||
                    state == TransferState.CANCELED ||
                    state == TransferState.FAILED) {
                mOpLog.put("endState", state)
                    .finish();
                mOpLog = null;
                mContentDownloader = null;
            }
            if (mListener != null) mListener.onStateChanged(id, state);
        }

        @Override
        public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
            if (mListener != null) mListener.onProgressChanged(id, bytesCurrent, bytesTotal);
        }

        @Override
        public void onError(int id, Exception ex) {
            if (mListener != null) mListener.onError(id, ex);
        }
    };

    /**
     * Gets a list of the communities in the content update.
     * @return A Set of CommunityInfo.
     */
    public Map<String, CommunityInfo> getCommunities() {
        KnownLocations.loadLocationsForProjects(Arrays.asList(getProjectName().toUpperCase()));
        if (mCommunitiesCache == null) {
            Map<String, CommunityInfo> result = new HashMap<>();
            File projectDir = PathsProvider.getLocalContentProjectDirectory(mProjectName);
            File contentDir = new File(projectDir, "content");
            File[] contentUpdates = contentDir.listFiles();
            if (contentUpdates != null && contentUpdates.length == 1) {
                File communitiesDir = new File(contentUpdates[0], "communities");
                File[] communities = communitiesDir.listFiles();
                if (communities != null) {
                    for (File community : communities) {
                        String communityName = community.getName().toUpperCase();
                        CommunityInfo ci = KnownLocations.findCommunity(communityName,
                                getProjectName().toUpperCase());
                        if (ci == null) {
                            ci = new CommunityInfo(communityName, getProjectName());
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
