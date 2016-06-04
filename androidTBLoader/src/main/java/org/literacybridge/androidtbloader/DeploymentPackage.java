package org.literacybridge.androidtbloader;

import org.literacybridge.androidtbloader.dropbox.IOHandler;
import org.literacybridge.core.fs.RelativePath;

import java.util.Collections;
import java.util.Date;
import java.util.Set;

public class DeploymentPackage {
    public enum DownloadStatus {
        NEVER_DOWNLOADED,
        DOWNLOADED,
        DOWNLOAD_FAILED,
    }

    private final String mProjectName;

    private String mRevision;
    private Date mExpiration;
    private Set<String> mCommunitiesFilter;
    private DownloadStatus mDownloadStatus;

    private boolean mUpdateAvailable;
    private boolean isCurrentlyDownloading;

    private final RelativePath mRemotePath;
    private final RelativePath mLocalPath;
    private final RelativePath mLocalUnzipPath;

    public DeploymentPackage(String projectName, String revision, Date expiration, Set<String> communitiesFilter,
                             DownloadStatus downloadStatus) {
        mProjectName = projectName;
        mRevision = revision;
        mExpiration = expiration;
        mCommunitiesFilter = Collections.unmodifiableSet(communitiesFilter);
        mDownloadStatus = downloadStatus;

        RelativePath projectPath = RelativePath.parse(mProjectName);
        RelativePath filePath = new RelativePath(mRevision, "content-" + mRevision + ".zip");
        RelativePath unzipDirPath = new RelativePath(mRevision, "content-" + mRevision);
        mRemotePath = RelativePath.concat(
                projectPath,
                IOHandler.PUBLISHED_SUB_PATH,
                filePath
        );
        mLocalPath = RelativePath.concat(projectPath, filePath);
        mLocalUnzipPath = RelativePath.concat(projectPath, unzipDirPath);
    }

    public synchronized boolean isCurrentlyDownloading() {
        return isCurrentlyDownloading;
    }

    public synchronized void setCurrentlyDownloading(boolean currentlyDownloading) {
        isCurrentlyDownloading = currentlyDownloading;
    }

    public RelativePath getRemotePath() {
        return mRemotePath;
    }

    public RelativePath getLocalPath() {
        return mLocalPath;
    }

    public RelativePath getLocalUnzipPath() {
        return mLocalUnzipPath;
    }

    public synchronized void setRevision(String revision) {
        mRevision = revision;
    }

    public synchronized void setExpiration(Date expiration) {
        mExpiration = expiration;
    }

    public synchronized void setCommunitiesFilter(Set<String> communitiesFilter) {
        mCommunitiesFilter = communitiesFilter;
    }

    public synchronized boolean isUpdateAvailable() {
        return mUpdateAvailable;
    }

    public synchronized void setUpdateAvailable(boolean updateAvailable) {
        mUpdateAvailable = updateAvailable;
    }

    public String getProjectName() {
        return mProjectName;
    }

    public synchronized String getRevision() {
        return mRevision;
    }

    public synchronized Date getExpiration() {
        return mExpiration;
    }

    public synchronized boolean hasExpiration() {
        return mExpiration != null && mExpiration.getTime() != 0;
    }

    public synchronized DownloadStatus getDownloadStatus() {
        return mDownloadStatus;
    }

    public synchronized Set<String> getCommunitiesFilter() {
        return mCommunitiesFilter;
    }

    public synchronized void setDownloadStatus(DownloadStatus downloadStatus) {
        mDownloadStatus = downloadStatus;
    }

    @Override
    public synchronized String toString() {
        return String.format("Project: %s\nRevision: %s\nExpiration: %s\nCommunitiesFilter: %s\nDownloadStatus: %s",
                mProjectName, mRevision, mExpiration, mCommunitiesFilter.toString(), mDownloadStatus.name());
    }
}
