package org.literacybridge.androidtbloader.uploader;

import android.util.Log;

import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferType;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;

import org.literacybridge.androidtbloader.TBLoaderAppContext;
import org.literacybridge.androidtbloader.util.PathsProvider;
import org.literacybridge.androidtbloader.util.S3Helper;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.literacybridge.androidtbloader.util.Constants.COLLECTED_DATA_BUCKET_NAME;

/**
 * Manages uploading files to S3.
 *
 * Files pending upload are moved to an "upload directory". When we're able to upload, we
 * copy files from that directory to the S3 bucket named by COLLECTED_DATA_BUCKET_NAME, or
 * "acm-stats".
 */

public class UploadManager {
    private static final String TAG = "UploadManager";

    private class QueuedItem {
        File file;
        long size;

        QueuedItem(File file) {
            this.file = file;
            this.size = file.length();
        }
    }

    public interface UploadListener {
        void onUploadActivity(int fileCount, long bytesRemaining);
    }
    private UploadListener mUploadListener;

    private File mUploadDirectory;
    
    private Map<String, QueuedItem> mUploadQueue;
    private String mActive;
    private long mActiveDownloaded;

    public UploadManager() {
        mUploadDirectory = PathsProvider.getUploadDirectory();
        mUploadQueue = new LinkedHashMap<>();
    }

    /**
     * Submits a single file to be uploaded to S3. The file will be moved to the upload directory.
     * If the file isn't uploaded while the app is running, it will have another chance the next
     * time that the app runs.
     * @param file The file to be uploaded.
     * @param objectName The name the object should have once uploaded.
     * @return True if the file was moved into the upload directory.
     */
    public synchronized boolean uploadFileAsName(File file, String objectName) {
        File uploadFile = new File(mUploadDirectory, objectName);
        File parent = uploadFile.getParentFile();
        if (!parent.exists()) {
            parent.mkdirs();
        }
        if (!file.renameTo(uploadFile)) {
            Log.d(TAG, String.format("Unable to rename %s to %s", file, uploadFile));
            return false;
        }
        upload(uploadFile);
        return true;
    }

    /**
     * Submits a single file to be uploaded to S3. Only one transfer is performed at a time. The
     * file must exist in the upload directory. The relative path of the file within the upload
     * directory is the name of the object in the S3 bucket.
     * @param file The file to be uploaded.
     */
    private synchronized void upload(File file) {
        enqueueUpload(file);
        checkQueue();
    }

    /**
     * Checks the "upload directory" and re-submits any files found.
     */
    public synchronized void restartUploads() {
        Log.d(TAG, "Looking for uploads to restart");
        restart(mUploadDirectory);
        checkQueue();
    }

    /**
     * Gets a count of the files waiting to be uploaded.
     * @return the count of files waiting.
     */
    public int getCountQueuedFiles() {
        return mUploadQueue.size();
    }

    /**
     * Gets the size of the files waiting to be uploaded.
     * @return the sum of the sizes of the files.
     */
    public long getSizeQueuedFiles() {
        long size = 0;
        for (QueuedItem i : mUploadQueue.values()) {
            size += i.size;
        }
        return size - mActiveDownloaded;
    }

    public void setUpdateListener(UploadListener listener) {
        mUploadListener = listener;
    }

    private void onUploadActivity() {
        if (mUploadListener != null)
            mUploadListener.onUploadActivity(getCountQueuedFiles(), getSizeQueuedFiles());
    }

    /**
     * Starts uploading the given file.
     * @param file to be uploaded.
     */
    private void startUpload(final File file) {
        // Build a key from the file's relative position in the upload directory.
        String key = mUploadDirectory.toURI().relativize(file.toURI()).getPath();

        mActiveDownloaded = 0;
        final TransferUtility transferUtility = S3Helper.getTransferUtility();
        TransferObserver observer = transferUtility.upload(COLLECTED_DATA_BUCKET_NAME, key, file);
        Log.d(TAG, String.format("Uploading file %s to key %s, id: %s", file.getName(), key, observer.getId()));

        observer.setTransferListener(new TransferListener() {
            @Override
            public void onStateChanged(int id, TransferState state) {
                Log.d(TAG, String.format("Transfer state changed for %d: %s", id, state.toString()));
                if (state == TransferState.COMPLETED) {
                    transferUtility.deleteTransferRecord(id);
                    file.delete();
                    mActive = null;
                    dequeueUploadAndCheck(file);
                } else if (state == TransferState.FAILED) {
                    transferUtility.deleteTransferRecord(id);
                    mActive = null;
                    dequeueUploadAndCheck(file);
                }
            }

            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                Log.d(TAG, String.format("Transfer progress for %d: %3.1f", id, 100.0*bytesCurrent/Math.max(1.0, bytesTotal)));
                mActiveDownloaded = bytesCurrent;
                onUploadActivity();
            }

            @Override
            public void onError(int id, Exception ex) {
                Log.d(TAG, String.format("Transfer exception for %d:", id), ex);
            }
        });
    }

    /**
     * Removes the given file from the upload queue, and checks the queue for more files to upload.
     * @param file to be removed from the queue.
     */
    private synchronized void dequeueUploadAndCheck(File file) {
        String relativeName = mUploadDirectory.toURI().relativize(file.toURI()).getPath();
        if (mUploadQueue.containsKey(relativeName)) {
            mUploadQueue.remove(relativeName);
            onUploadActivity();
        }
        checkQueue();
    }

    /**
     * Adds a file to the upload queue.
     * @param file to be added.
     */
    private void enqueueUpload(File file) {
        String relativeName = mUploadDirectory.toURI().relativize(file.toURI()).getPath();
        if (!mUploadQueue.containsKey(relativeName)) {
            Log.d(TAG, String.format("Enqueuing upload for key %s", relativeName));
            mUploadQueue.put(relativeName, new QueuedItem(file));
            onUploadActivity();
        }
    }

    /**
     * If there is no upload in progress, and files waiting to upload, starts the next upload.
     */
    private synchronized void checkQueue() {
        // If nothing is active, start the first item on the queue.
        if (mActive == null) {
            for (Map.Entry<String, QueuedItem> e : mUploadQueue.entrySet()) {
                mActive = e.getKey();
                startUpload(e.getValue().file);
                break;
            }
        }
    }

    /**
     * Restarts (re-enqueues) the given file, or files in the given directory.
     * @param file to be restarted.
     */
    private void restart(File file) {
        if (file.isDirectory()) {
            File [] files = file.listFiles();
            for (File f : files)
                restart(f);
        } else {
            enqueueUpload(file);
        }
    }


}
