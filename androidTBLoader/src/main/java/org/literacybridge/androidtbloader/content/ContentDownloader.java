package org.literacybridge.androidtbloader.content;

import android.util.Log;

import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;

import org.literacybridge.androidtbloader.util.Constants;
import org.literacybridge.androidtbloader.util.PathsProvider;
import org.literacybridge.androidtbloader.util.S3Helper;
import org.literacybridge.core.fs.ZipUnzip;

import java.io.File;
import java.io.IOException;

/**
 * Performs download of a Content Update
 */
class ContentDownloader {
    private static final String TAG="ContentDownloader";

    private ContentInfo mContentInfo;
    private TransferListener mTransferListener;

    private TransferObserver mObserver;
    private File mProjectDir;

    ContentDownloader(ContentInfo contentInfo, TransferListener transferListener) {
        this.mContentInfo = contentInfo;
        this.mTransferListener = transferListener;
    }

    /**
     * Returns the number of bytes transferred so far. If the transfer hasn't started, or failed to
     * start, returns 0.
     * @return The number of bytes transferred.
     */
    long getBytesTransferred() {
        if (mObserver == null) return 0;
        // Only deals with a single part transfer.
        return mObserver.getBytesTransferred();
    }

    /**
     * Starts downloading a Content Update. The files are in two parts, software- and content-.
     * Start with the content-, because it is probably larger.
     */
    void start() {
        mProjectDir = PathsProvider.getLocalContentProjectDirectory(mContentInfo.getProjectName());
        startDownloadPart1();
    }

    /**
     * Starts downloading the Content Update, and listens for status from S3.
     */
    private void startDownloadPart1() {
        // Start two transfers...
        Log.d(TAG, "Starting download of content");
        downloadPart("content", new TransferListener() {

            @Override
            public void onStateChanged(int id, TransferState state) {
                if (state == TransferState.COMPLETED) {
                    S3Helper.getTransferUtility().deleteTransferRecord(id);
                    // unzip the content.
                    File p1 = fileForDownload("content");
                    try {
                        // The file is like content-2016-03-c.zip, and contains a top level directory "content".
                        long t1 = System.currentTimeMillis();
                        ZipUnzip.unzip(p1, p1.getParentFile());
                        Log.d(TAG,
                                String.format("Unzipped %s, %dms",
                                        p1.getName(),
                                        System.currentTimeMillis() - t1));
                    } catch (IOException e) {
                        Log.d(TAG, String.format("Exception unzipping %s", p1.getName()), e);
                        mContentInfo.setDownloadStatus(ContentInfo.DownloadStatus.DOWNLOAD_FAILED);
                        mTransferListener.onError(id, e);
                    }
                    // Create the mVersion marker.
                    String filename = mContentInfo.getVersion() + ".current";
                    File marker = new File(mProjectDir, filename);
                    try {
                        marker.createNewFile();
                    } catch (IOException e) {
                        // This really should never happen. But if it does, we won't recognize this downloaded content.
                        // If we leave it, we'll clean it up next time. Return the error, and leave the mess 'til then.
                        mContentInfo.setDownloadStatus(ContentInfo.DownloadStatus.DOWNLOAD_FAILED);
                        mTransferListener.onError(id, e);
                        return;
                    }
                    mContentInfo.setDownloadStatus(ContentInfo.DownloadStatus.DOWNLOADED);
                    mTransferListener.onStateChanged(id, state);
                } else if (state == TransferState.FAILED || state == TransferState.CANCELED) {
                    mContentInfo.setDownloader(null);
                    mContentInfo.setDownloadStatus(ContentInfo.DownloadStatus.DOWNLOAD_FAILED);
                    S3Helper.getTransferUtility().deleteTransferRecord(id);
                    mTransferListener.onStateChanged(id, state);
                } else {
                    mTransferListener.onStateChanged(id, state);
                }
            }

            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                mTransferListener.onProgressChanged(id, bytesCurrent, mContentInfo.getSize());
            }

            @Override
            public void onError(int id, Exception ex) {
                mContentInfo.setDownloadStatus(ContentInfo.DownloadStatus.DOWNLOAD_FAILED);
                mTransferListener.onError(id, ex);
            }
        });
    }

    /**
     *  Helper to generate the filename for a download component.
     * @param component The component being downloaded; currently "content"
     * @return The name of the object in s3, like "content-DEMO-2017-2-a"
     */
    private File fileForDownload(String component) {
        String filename = component + "-" + mContentInfo.getVersion() + ".zip";
        return new File(mProjectDir, filename);
    }

    /**
     * Starts the download from S3 of one of the content update's components.
     *
     * @param component Currently, only "content"
     * @param listener  A TransferListener.
     */
    private void downloadPart(String component, TransferListener listener) {
        Log.d(TAG,
                String.format("Downloading %s file info for %s:%s",
                        component,
                        mContentInfo.getProjectName(),
                        mContentInfo.getVersion()));

        final File file = fileForDownload(component);
        file.getParentFile().mkdirs();
        String key = "projects/" + mContentInfo.getProjectName() + "/" + file.getName();

        // Initiate the download
        Log.d(TAG, "about to call transferUtility.download");
        TransferUtility transferUtility = S3Helper.getTransferUtility();
        mObserver = transferUtility.download(Constants.CONTENT_UPDATES_BUCKET_NAME, key, file);
        Log.d(TAG, "back from call to transferUtility.download");

        mObserver.setTransferListener(listener);
    }

}
