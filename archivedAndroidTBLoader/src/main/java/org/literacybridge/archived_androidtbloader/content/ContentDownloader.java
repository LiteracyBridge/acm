package org.literacybridge.archived_androidtbloader.content;

import android.os.AsyncTask;
import android.util.Log;

import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.GenericHandler;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;

import org.literacybridge.archived_androidtbloader.signin.UnattendedAuthenticator;
import org.literacybridge.archived_androidtbloader.util.PathsProvider;
import org.literacybridge.archived_androidtbloader.util.S3Helper;
import org.literacybridge.core.fs.ZipUnzip;

import java.io.File;
import java.io.IOException;
import java.util.function.Supplier;

/**
 * Performs download of a Deployment
 */
class ContentDownloader {
    private static final String TAG= "TBL!:" + "ContentDownloader";

    private ContentInfo mContentInfo;
    private DownloadListener mDownloadListener;

    private TransferObserver mObserver;
    private File mProjectDir;

    private volatile boolean mCancelRequested = false;

    /**
     * Like TransferListener, but with onUnzipProgress added.
     */
    public interface DownloadListener extends TransferListener {
        public void onUnzipProgress(int id, long current, long total);
    }

    ContentDownloader(ContentInfo contentInfo, DownloadListener downloadListener) {
        this.mContentInfo = contentInfo;
        this.mDownloadListener = downloadListener;
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
     * Cancels any active download.
     */
    public void cancel() {
        mCancelRequested = true;
        S3Helper.getTransferUtility().cancel(mObserver.getId());
    }

    /**
     * Starts downloading the Deployment, and listens for status from S3.
     */
    void start() {
        new UnattendedAuthenticator(new GenericHandler() {
            @Override
            public void onSuccess() {
                start2();
            }

            @Override
            public void onFailure(Exception exception) {
                mDownloadListener.onStateChanged(0, TransferState.FAILED);
            }
        }).authenticate();
    }

    private void start2() {
        mProjectDir = PathsProvider.getLocalContentProjectDirectory(mContentInfo.getProgramId());
        Log.d(TAG, "Starting download of content");

        TransferListener listener = new TransferListener() {
            @Override
            public void onStateChanged(int id, TransferState state) {
                if (state == TransferState.COMPLETED) {
                    S3Helper.getTransferUtility().deleteTransferRecord(id);
                    onDownloadCompleted(id, state);
                } else if (state == TransferState.FAILED || state == TransferState.CANCELED) {
                    S3Helper.getTransferUtility().deleteTransferRecord(id);
                    mDownloadListener.onStateChanged(id, state);
                } else {
                    mDownloadListener.onStateChanged(id, state);
                }
            }

            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                mDownloadListener.onProgressChanged(id, bytesCurrent, mContentInfo.getSize());
            }

            @Override
            public void onError(int id, Exception ex) {
                // After we return from this onError call, we'll get another call, to
                // onStateChanged with TransferState.FAILED.

                // TODO: If ex is AmazonClientException, with cause IOException, with cause
                // ErrnoException, with errno 28, ENOSPC, then we need a different error code,
                // because retry is pointless.

                mDownloadListener.onError(id, ex);
            }
        };

        // Where the file will be downloaded
        File file = fileForDownload("content");
        file.getParentFile().mkdirs();
        // Initiate the download
        Log.d(TAG, String.format("Starting download of content, b: %s, k: %s", mContentInfo.getBucketName(), mContentInfo.getKey()));
        mObserver = S3Helper.getTransferUtility().download(mContentInfo.getBucketName(), mContentInfo.getKey(), file);
        mObserver.setTransferListener(listener);
    }

    private static class BackgroundDownloader extends AsyncTask<Void, Void, Void> {
        private final int id;
        private final TransferState state;
        private final DownloadListener downloadListener;
        private final ContentInfo contentInfo;
        private final File downloadedZip;
        private final File projectDir;
        private final Supplier<Boolean> isCancelRequested;
        final long t1 = System.currentTimeMillis();
        private Exception thrown = null;

        private BackgroundDownloader(int id,
            TransferState state,
            DownloadListener downloadListener,
            ContentInfo contentInfo,
            File downloadedZip,
            File projectDir,
            Supplier<Boolean> isCancelRequested)
        {
            this.id = id;
            this.state = state;
            this.downloadListener = downloadListener;
            this.contentInfo = contentInfo;
            this.downloadedZip = downloadedZip;
            this.projectDir = projectDir;
            this.isCancelRequested = isCancelRequested;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            Log.d(TAG, "Unzipping file");
            try {
                ZipUnzip.unzip(downloadedZip, downloadedZip.getParentFile(), new ZipUnzip.UnzipListener() {
                    @Override
                    public boolean progress(long current, long total) {
                        downloadListener.onUnzipProgress(id, current, total);
                        return !isCancelRequested.get(); // true: continue unzipping
                    }
                });
            } catch (Exception ex) {
                thrown = ex;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (thrown != null) {
                Log.d(TAG, String.format("Exception unzipping %s", downloadedZip.getName()), thrown);
                // Simulate an S3 exception, by onError() then onStateChanged().
                downloadListener.onError(id, thrown);
                downloadListener.onStateChanged(id, TransferState.FAILED);
                return;
            }

            if (isCancelRequested.get()) {
                // Simulate download cancelled. We can only be here if the actual last state was
                // COMPLETED. We know that the transfer has already been deleted.
                downloadListener.onStateChanged(id, TransferState.CANCELED);
                return;
            }

            Log.d(TAG,
                String.format("Unzipped %s, %dms",
                    downloadedZip.getName(),
                    System.currentTimeMillis() - t1));
            // Create the mVersion marker. Like DEMO-2017-2-a.current
            String filename = contentInfo.getVersionedDeployment() + ".current";
            File marker = new File(projectDir, filename);
            try {
                marker.createNewFile();
                downloadedZip.delete();
            } catch (IOException e) {
                // This really should never happen. But if it does, we won't recognize this downloaded content.
                // If we leave it, we'll clean it up next time. Return the error, and leave the mess 'til then.
                downloadListener.onError(id, e);
                return;
            }
            downloadListener.onStateChanged(id, state);
        }
    }

    /**
     * Helper to unzip the downloaded content on a background thread. Creates a file like
     * "DEMO-2017-2-a.current" on success.
     * @param id The id of the completed transfer.
     * @param state The new download state. Always TransferState.COMPLETED
     */
    private void onDownloadCompleted(final int id, final TransferState state) {
        final long t1 = System.currentTimeMillis();

        // The file is like content-2016-03-c.zip, and contains a top level directory "content".
        final File downloadedZip = fileForDownload("content");

        new BackgroundDownloader(id, state, mDownloadListener, mContentInfo, downloadedZip, mProjectDir, ()->mCancelRequested).execute();
        // Unzip the content. This is lengthy, so use an async thread.
//        new AsyncTask<Void, Void, Void>() {
//            Exception thrown = null;
//
//            @Override
//            protected Void doInBackground(Void... params) {
//                Log.d(TAG, "Unzipping file");
//                try {
//                    ZipUnzip.unzip(downloadedZip, downloadedZip.getParentFile(), new ZipUnzip.UnzipListener() {
//                        @Override
//                        public boolean progress(long current, long total) {
//                            mDownloadListener.onUnzipProgress(id, current, total);
//                            return !mCancelRequested; // true: continue unzipping
//                        }
//                    });
//                } catch (Exception ex) {
//                    thrown = ex;
//                }
//                return null;
//            }
//
//            @Override
//            protected void onPostExecute(Void result) {
//                if (thrown != null) {
//                    Log.d(TAG, String.format("Exception unzipping %s", downloadedZip.getName()), thrown);
//                    // Simulate an S3 exception, by onError() then onStateChanged().
//                    mDownloadListener.onError(id, thrown);
//                    mDownloadListener.onStateChanged(id, TransferState.FAILED);
//                    return;
//                }
//
//                if (mCancelRequested) {
//                    // Simulate download cancelled. We can only be here if the actual last state was
//                    // COMPLETED. We know that the transfer has already been deleted.
//                    mDownloadListener.onStateChanged(id, TransferState.CANCELED);
//                    return;
//                }
//
//                Log.d(TAG,
//                    String.format("Unzipped %s, %dms",
//                        downloadedZip.getName(),
//                        System.currentTimeMillis() - t1));
//                // Create the mVersion marker. Like DEMO-2017-2-a.current
//                String filename = mContentInfo.getVersionedDeployment() + ".current";
//                File marker = new File(mProjectDir, filename);
//                try {
//                    marker.createNewFile();
//                    // TODO: Delete the .zip file!
//                } catch (IOException e) {
//                    // This really should never happen. But if it does, we won't recognize this downloaded content.
//                    // If we leave it, we'll clean it up next time. Return the error, and leave the mess 'til then.
//                    mDownloadListener.onError(id, e);
//                    return;
//                }
//                mDownloadListener.onStateChanged(id, state);
//            }
//        }.execute();

    }

    /**
     *  Helper to generate the filename for a download component.
     * @param component The component being downloaded; currently "content"
     * @return The name of the object in s3, like "content-DEMO-2017-2-a"
     */
    private File fileForDownload(String component) {
        String filename = component + "-" + mContentInfo.getVersionedDeployment() + ".zip";
        return new File(mProjectDir, filename);
    }

}
