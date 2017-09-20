package org.literacybridge.androidtbloader.uploader;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserSession;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.GenericHandler;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.services.s3.model.ObjectMetadata;
import org.literacybridge.androidtbloader.R;
import org.literacybridge.androidtbloader.TBLoaderAppContext;
import org.literacybridge.androidtbloader.signin.UnattendedAuthenticator;
import org.literacybridge.androidtbloader.signin.UserHelper;
import org.literacybridge.androidtbloader.util.OperationLogImpl;
import org.literacybridge.androidtbloader.util.PathsProvider;
import org.literacybridge.androidtbloader.util.S3Helper;
import org.literacybridge.androidtbloader.util.Util;
import org.literacybridge.core.fs.OperationLog;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.regex.Pattern;

import static android.support.v4.app.NotificationCompat.PRIORITY_HIGH;
import static org.literacybridge.androidtbloader.util.Constants.COLLECTED_DATA_BUCKET_NAME;

/**
 * Manages uploading files to S3.
 *
 * Files pending upload are moved to an "upload directory". When we're able to upload, we
 * copy files from that directory to the S3 bucket named by COLLECTED_DATA_BUCKET_NAME, or
 * "acm-stats".
 *
 */

public class UploadService extends JobService {
    private static final String TAG = "TBL!:" + UploadService.class.getSimpleName();

    private static final int UPLOADER_JOB_ID = 42;
    private static final int NOTIFICATION_ID = 43;
    public static final String UPLOADER_STATUS_EVENT = "uploader_status_change";

    private static final int PERIOD = 10 * 60 * 1000;   // 10 minutes between polls.
    // Allow only at most this many consecutive transfer errors before waiting for the next job.
    private static final int MAX_CONSECUTIVE_ERRORS = 2;

    // If we have any current error / warning messages we'd like to display in the service.
    private static String sErrorMessage;

    private static File sUploadDirectory = PathsProvider.getUploadDirectory();
    private static PriorityQueue<UploadItem> sUploadQueue;
    private static List<UploadItem> sUploadHistory = new LinkedList<>();

    private static final Pattern TEXTFILE = Pattern.compile("(?i).*(.srn|.log)$");

    private JobParameters params;
    private boolean mSuccess = true;
    private long mActiveDownloadSize = 0;
    private long mActiveUploadProgress = 0;
    private boolean mCancelRequested = false;
    private int mTransferId;
    private int mConsecutiveErrors;
    private boolean mAnySuccessThisJob;

    private String mPendingUploadName = "";
    private int mPendingUploadCount = 0;
    private long mPendingUploadSize = 0;
    private Toast mToast = null;

    /**
     * Starts the upload service. Files will be uploaded when we're connected, smallest first.
     */
    public static synchronized void startUploadService() {
        int period = TBLoaderAppContext.getInstance().isDebug() ? 6 * 1000 : PERIOD;
        Log.d(TAG, String.format("startUploadService: scheduling job with period %d", period));
        JobScheduler jobScheduler = (JobScheduler) TBLoaderAppContext.getInstance().getSystemService(Context.JOB_SCHEDULER_SERVICE);
        // Always just start the job. It appears that we don't get duplicates, and trying to avoid
        // extra jobs has proven unreliable.
        JobInfo jobInfo = new JobInfo.Builder(UPLOADER_JOB_ID,
                                              new ComponentName(
                                                  TBLoaderAppContext.getInstance(),
                                                  UploadService.class))
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            .setPersisted(true)
            .setPeriodic(period)
            .build();
        jobScheduler.schedule(jobInfo);
    }

    private static synchronized void cancelUploadService() {
        Log.d(TAG, "Cancelling upload service");
        JobScheduler jobScheduler =
                (JobScheduler) TBLoaderAppContext.getInstance().getSystemService(Context.JOB_SCHEDULER_SERVICE);
        jobScheduler.cancel(UPLOADER_JOB_ID);
    }

    /**
     * Override this method with the callback logic for your job. Any such logic needs to be
     * performed on a separate thread, as this function is executed on your application's main
     * thread.
     *
     * @param params Parameters specifying info about this job, including the extras bundle you
     *               optionally provided at job-creation time.
     * @return True if your service needs to process the work (on a separate thread). False if
     * there's no more work to be done for this job.
     */
    @Override
    public boolean onStartJob(JobParameters params) {
        Log.d(TAG, String.format("onStartJob called, thread [%d]", Thread.currentThread().getId()));
        this.params = params;

        if (checkForUploads() == 0) {
            Log.d(TAG, "onStartJob: no uploads, cancelling service.");
            updateStatus();
            cancelUploadService();
            return false;
        }

        if (!TBLoaderAppContext.getInstance().isCurrentlyConnected()) {
            Log.d(TAG, "onStartJob: not currently connected");
            updateStatus();
            return false;
        }

        startUploads();
        return true;
    }

    /**
     * Gets a count of the files waiting to be uploaded.
     * @return the count of files waiting.
     */
    public int getUploadCount() {
        return mPendingUploadCount;
    }

    /**
     * Gets the size of the files waiting to be uploaded.
     * @return the sum of the sizes of the files.
     */
    public long getUploadSize() {
        return mPendingUploadSize;
    }

    /**
     * Submits a single file to be uploaded to S3. The file will be moved to the upload directory.
     * If the file isn't uploaded while the app is running, it will have another chance the next
     * time that the app runs.
     * @param file The file to be uploaded.
     * @param objectName The name the object should have once uploaded to s3.
     * @param removeConflicting If true, and a file of the same name already exists, replace it
     *                          with this one.
     * @return True if the file was moved into the upload directory.
     */
    public synchronized boolean uploadFileAsName(File file, String objectName, boolean removeConflicting) {
        File uploadFile = new File(sUploadDirectory, objectName);
        File parent = uploadFile.getParentFile();
        if (!parent.exists()) {
            parent.mkdirs();
        }
        if (removeConflicting && uploadFile.exists()) {
            uploadFile.delete();
        }
        if (!file.renameTo(uploadFile)) {
            Log.d(TAG, String.format("Unable to rename %s to %s", file, uploadFile));
            return false;
        }
        startUploadService();
        return true;
    }

    /**
     * Submits a single file to be uploaded to S3. The file will be moved to the upload directory.
     * If the file isn't uploaded while the app is running, it will have another chance the next
     * time that the app runs.
     * @param file The file to be uploaded.
     * @param objectName The name the object should have once uploaded.
     * @return True if the file was moved into the upload directory.
     */
    public boolean uploadFileAsName(File file, String objectName) {
        return uploadFileAsName(file, objectName, false);
    }


    /**
     * Updates the notification area based on count of uploads, and whether there is an error
     * message.
     */
    private void updateNotification() {
        Log.d(TAG, String.format("updateNotification, count:%d, size:%d, name:%s", mPendingUploadCount, mPendingUploadSize, mPendingUploadName));
        NotificationManager mNotifyMgr = (NotificationManager) getSystemService(
                NOTIFICATION_SERVICE);
        if (mPendingUploadCount > 0 || sErrorMessage != null) {
            int icon = R.drawable.talking_book_outline;
            String title;
            String content;
            // If there is an error message, add it, and turn the icon red.
            if (sErrorMessage != null) {
                title = sErrorMessage;
                content = String.format("%s in %d files waiting to upload.", Util.getBytesString(mPendingUploadSize), mPendingUploadCount);
                icon = R.drawable.talking_book_outline_red;
            } else {
                title = "Pending Statistics Uploads";
                content = String.format("%s in %d files to be uploaded.", Util.getBytesString(mPendingUploadSize), mPendingUploadCount);
            }

            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(icon)
                .setPriority(PRIORITY_HIGH)
                .setContentTitle(title)
                .setContentText(content);

            Intent statusActivityIntent = new Intent(this, UploadStatusActivity.class);
            statusActivityIntent.putExtra("hide_configure_button", true);

            PendingIntent notificationIntent = PendingIntent.getActivities(this, 0,
                new Intent[] { statusActivityIntent }, PendingIntent.FLAG_UPDATE_CURRENT);
            mBuilder.setContentIntent(notificationIntent);

            Notification notification = mBuilder.build();
            notification.flags |= Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;

            mNotifyMgr.notify(NOTIFICATION_ID, notification);
        } else {
            mNotifyMgr.cancel(NOTIFICATION_ID);
        }
    }

    /**
     * Updates progress to notification area and UploadManager.
     */
    private void updateStatus() {
        long size = 0;
        int count = 0;
        String name = "";
        if (sUploadQueue != null) {
            count = sUploadQueue.size();
            for (UploadItem item : sUploadQueue)
                size += item.file.length();
            UploadItem item = sUploadQueue.peek();
            name = item == null ? "None" : item.file.getName();
            // Account for any currently active transfers.
            size += mActiveDownloadSize - mActiveUploadProgress;
        }

        if (!name.equalsIgnoreCase(mPendingUploadName) || count != mPendingUploadCount || size != mPendingUploadSize) {
            mPendingUploadName = name;
            mPendingUploadCount = count;
            mPendingUploadSize = size;
            Log.d(TAG, String.format("updateStatus, count:%d, size:%d, name:%s", mPendingUploadCount, mPendingUploadSize, mPendingUploadName));

            Intent intent = new Intent(UPLOADER_STATUS_EVENT);
            intent.putExtra("count", mPendingUploadCount);
            intent.putExtra("size", mPendingUploadSize);
            intent.putExtra("name", mPendingUploadName);
            LocalBroadcastManager.getInstance(TBLoaderAppContext.getInstance()).sendBroadcast(intent);

            updateNotification();
        }
    }

    /**
     * Helper to show Toast messages. Cancels any existing or queued toasts, to prevent a long
     * stack of Toasts.
     * @param text to show
     */
    private void showToast(CharSequence text) {
        Context context = TBLoaderAppContext.getInstance();
        int duration = Toast.LENGTH_SHORT;
        if (mToast != null) {
            mToast.cancel();
        }
        mToast = Toast.makeText(context, text, duration);
        mToast.show();
    }

    /**
     * Starts the process of uploading statistics, user feedback, and log files.
     */
    private void startUploads() {
        // Uncomment for debugging.
//        CharSequence text = "Starting Upload Service.";
//        showToast(text);
        mConsecutiveErrors = 0;
        mAnySuccessThisJob = false; // so far

        CognitoUserSession session = UserHelper.getCurrSession();
        if (session == null || !session.isValid()) {
            Log.d(TAG, String.format("startUploads: %s session, trying to authenticate", session==null?"no":"invalid"));

            new UnattendedAuthenticator(new GenericHandler() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "startUploads: authentication success");
                    transferNext();
                }

                @Override
                public void onFailure(Exception exception) {
                    Log.d(TAG, "startUploads: authentication failure");
                    showToast("Authentication Failure!");
                    sErrorMessage = "Authentication Failure!";
                }
            }).authenticate();
        } else {
            Log.d(TAG, "startUploads: have existing session");
            transferNext();
        }
    }

    /**
     * Transfers the next file. If no next file, stops the service.
     *
     * Priority is "smallest first".
     */
    private void transferNext() {
        // No transfer active.
        mActiveDownloadSize = 0;
        mActiveUploadProgress = 0;
        if (mCancelRequested) {
            return;
        }

        // For testing, to let uploads accumulate.
        SharedPreferences userPrefs = PreferenceManager.getDefaultSharedPreferences(
            TBLoaderAppContext.getInstance());
        boolean disableUploads = userPrefs.getBoolean("pref_disable_uploads", false);
        if (disableUploads) {
            Log.d(TAG, "transferNext: uploads disabled");
            updateStatus();
            return;
        }

        // Too many errors? Quit for a while.
        if (mConsecutiveErrors >= MAX_CONSECUTIVE_ERRORS) {
            return;
        }
        // Empty work queue, and no progress? Quit for a while
        if (sUploadQueue.size() == 0 && !mAnySuccessThisJob) {
            return;
        }

        if (sUploadQueue.size() > 0 || checkForUploads() > 0) {
            updateStatus();
            transferOne(sUploadQueue.peek());
        } else {
            updateStatus();
            jobFinished(params, !mSuccess);
            cancelUploadService();
        }
    }
    private void transferDone(UploadItem previous) {
        if (previous != null) {
            if (previous.success) {
                mConsecutiveErrors = 0;
                mAnySuccessThisJob = true;
            } else {
                mConsecutiveErrors++;
            }
            sUploadQueue.remove(previous);
            sUploadHistory.add(0, previous);
        }
        transferNext();
    }


    /**
     * Transfers one file to s3. When the transfer is complete, will check for more.
     * @param queuedItem with file to be uploaded.
     */
    private void transferOne(final UploadItem queuedItem) {
        final File file = queuedItem.file;
        mActiveDownloadSize = file.length();
        // Build a key from the file's relative position in the upload directory.
        String key = sUploadDirectory.toURI().relativize(file.toURI()).getPath();
        ObjectMetadata metadata = new ObjectMetadata();

        // If the file is a ".srn" or a ".log", set its type as text/plain
        if (TEXTFILE.matcher(file.getName()).matches()) {
            metadata.setContentType("text/plain");
        }

        // We don't want the act of uploading a log file to create a log record that triggers
        // another upload of a log file. So we set the option NOTRIGGER, so that this log operation
        // won't trigger an upload.
        final OperationLog.Operation opLog = OperationLog.startOperation("uploadfle")
                .option(OperationLogImpl.OPTION_NOTRIGGER, Boolean.TRUE)
                .put("name", file.getName())
                .put("size", file.length())
                .put("key", key)
                .put("service", true);

        final TransferUtility transferUtility = S3Helper.getTransferUtility();
        TransferObserver observer = transferUtility.upload(COLLECTED_DATA_BUCKET_NAME, key, file, metadata);
        queuedItem.uploadStarted();
        Log.d(TAG, String.format("Uploading file %s (%d bytes) to key %s, id: %s", file.getName(), file.length(), key, observer.getId()));

        observer.setTransferListener(new TransferListener() {
            @Override
            public void onStateChanged(int id, TransferState state) {
                Log.d(TAG, String.format("Transfer state changed for %d: %s", id, state.toString()));
                mTransferId = id;
                if (state == TransferState.COMPLETED) {
                    transferUtility.deleteTransferRecord(id);
                    boolean deleted = file.delete();
                    opLog.put("status", "success")
                        .put("deleteAfterUpload", deleted?"succeeded":"failed")
                        .finish();
                    sErrorMessage = null;
                    mTransferId = 0;
                    queuedItem.uploadSucceeded();
                    transferDone(queuedItem);
                } else if (state == TransferState.FAILED) {
                    CharSequence text = "Error uploading "+file.getName();
                    showToast(text);

                    mSuccess = false;
                    transferUtility.deleteTransferRecord(id);
                    opLog.put("status", "failure")
                            .finish();
                    sErrorMessage = text.toString();
                    mTransferId = 0;
                    queuedItem.uploadFailed();
                    transferDone(queuedItem);
                } else if (state == TransferState.WAITING_FOR_NETWORK) {
                    sErrorMessage = "Waiting for network";
                    updateStatus();
                }
            }

            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                Log.d(TAG, String.format("Transfer progress for %d: %3.1f", id, 100.0*bytesCurrent/Math.max(1.0, bytesTotal)));
                mActiveUploadProgress = bytesCurrent;
                sErrorMessage = null;
                updateStatus();
            }

            @Override
            public void onError(int id, Exception ex) {
                CognitoUserSession session = UserHelper.getCurrSession();
                Log.d(TAG, String.format("Transfer exception for %d:, session valid: %s", id, session==null?"no session":session.isValid()?"yes":"no"), ex);
            }
        });
    }

    /**
     * This method is called if the system has determined that you must stop execution of your job
     * even before you've had a chance to call {@link #jobFinished(JobParameters, boolean)}.
     * <p>
     * <p>This will happen if the requirements specified at schedule time are no longer met. For
     * example you may have requested WiFi with
     * {@link JobInfo.Builder#setRequiredNetworkType(int)}, yet while your
     * job was executing the user toggled WiFi. Another example is if you had specified
     * {@link JobInfo.Builder#setRequiresDeviceIdle(boolean)}, and the phone left its
     * idle maintenance window. You are solely responsible for the behaviour of your application
     * upon receipt of this message; your app will likely start to misbehave if you ignore it. One
     * immediate repercussion is that the system will cease holding a wakelock for you.</p>
     *
     * @param params Parameters specifying info about this job.
     * @return True to indicate to the JobManager that you'd like to reschedule this job based
     * on the retry criteria provided at job creation-time. False to drop the job. Regardless of
     * the value returned, your job must stop executing.
     */
    @Override
    public boolean onStopJob(JobParameters params) {
        mCancelRequested = true;
        if (mTransferId > 0) {
            final TransferUtility transferUtility = S3Helper.getTransferUtility();
            transferUtility.cancel(mTransferId);
            mTransferId = 0;
        }
        return (checkForUploads() > 0);
    }

    /**
     * If the parameter is a file, adds it to the upload queue; if a directory, adds any
     * files contained within to the upload queue.
     * @return Count of files to be uploaded.
     */
    private static synchronized int checkForUploads() {
        if (sUploadQueue == null) {
            sUploadQueue = new PriorityQueue<>(11, new UploadItem.QueuedUploadItemComparator());
        }
        int maxToShow = 4;
        int numAdded = 0;
        Queue<File> list = new LinkedList<>();
        // Start with the root directory.
        list.add(sUploadDirectory);

        // Go until the list is empty.
        while (list.size() > 0) {
            File file = list.poll();
            // Add the contents of any directories to the list; any files to the upload queue.
            if (file.isDirectory()) {
                list.addAll(Arrays.asList(file.listFiles()));
            } else {
                UploadItem qif = new UploadItem(file);
                if (!sUploadQueue.contains(qif)) {
                    if (numAdded < maxToShow){
                        Log.d(TAG, String.format("Enqueuing upload for file %s",
                                                 file.getAbsolutePath()));
                    }
                    sUploadQueue.add(qif);
                    numAdded++;
                }
            }
        }
        Log.d(TAG, String.format("Queued %d files to upload", numAdded));

        return sUploadQueue.size();
    }

    /**
     * Gets the list of queued uploads. Prevents modification of the real list.
     * @return A List<UploadItem>.
     */
    static List<UploadItem> getUploadQueue() {
        List<UploadItem> result = new ArrayList<>();
        if (sUploadQueue != null && sUploadQueue.size() > 0) {
            result.addAll(sUploadQueue);
        }
        return result;
    }

    static List<UploadItem> getUploadHistory() {
        return sUploadHistory;
    }
    /**
     * Returns any error message, else empty string.
     * @return the latest error message, or empty string.
     */
    static String getErrorMessage() {
        return sErrorMessage == null ? "" : sErrorMessage;
    }
}
