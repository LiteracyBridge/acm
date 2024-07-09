package org.literacybridge.archived_androidtbloader.util;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.util.Log;
import org.literacybridge.archived_androidtbloader.TBLoaderAppContext;
import org.literacybridge.core.fs.OperationLog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;

import static org.literacybridge.archived_androidtbloader.util.Constants.ISO8601;

/**
 * Log operations of the applications. Uploaded to server, to extract app metrics, usage, and updates.
 */

public class OperationLogImpl implements OperationLog.Implementation{
    private static final String TAG = "TBL!:" + OperationLogImpl.class.getSimpleName();

    // This option can be applied to operation logs that shouldn't trigger a quick upload. These
    // operation logs can accumulate until a higher priority event happens.
    public static final String OPTION_NOTRIGGER = "notrigger";

    private static final int UPLOAD_WAIT_TIME = 10 * 60 * 1000; // 10 minutes in ms
    private static final int UPLOAD_WAIT_TIME_DEBUG = 30 * 1000;

    private static int getUploadWaitTime() {
        return TBLoaderAppContext.getInstance().isDebug() ? UPLOAD_WAIT_TIME_DEBUG : UPLOAD_WAIT_TIME;
    }

    private static OperationLogImpl sInstance = null;
    public static synchronized OperationLogImpl getInstance() {
        if (sInstance == null) {
            sInstance = new OperationLogImpl();
        }
        return sInstance;
    }

    private Handler handler = new Handler();
    private Runnable runnable;

    private File logFile;
    private DateFormat filenameFormat = ISO8601;

    private Queue<File> mPendingFiles = null;

    @SuppressLint("SimpleDateFormat")
    private OperationLogImpl() {
        File logDir = PathsProvider.getLogDirectory();
        if (!logDir.exists()) {
            if (!logDir.mkdirs()) {
                Log.d(TAG, String.format("Failed to create directory %s", logDir.getAbsolutePath()));
            }
        }
        handleExistingLogFiles();

        runnable = new Runnable() {
            @Override
            public void run() {
                closeLogFile();
            }
        };
    }

    /**
     * At startup we find any log files lying around. Take the latest one and append to it,
     * and schedule any others for uploading as soon as we start uploading.
     */
    private void handleExistingLogFiles() {
        File [] existing = PathsProvider.getLogDirectory().listFiles();
        if (existing != null && existing.length > 0) {
            // Find the newest. We'll append to it. The rest, we'll schedule for upload.
            File newestFile = existing[0];
            long newestTime = newestFile.lastModified();
            for (File f : existing) {
                if (f.lastModified() > newestTime) {
                    newestTime = f.lastModified();
                    newestFile = f;
                }
            }
            logFile = newestFile;
            if (existing.length > 1) {
                // To have existing log files, we should generally have our tbcdid. But, in case
                // not, queue them, and upload when we know we have it.
                mPendingFiles = new LinkedList<>();
                for (File f : existing) {
                    if (f != newestFile) {
                        mPendingFiles.add(f);
                    }
                }
            }
        }
    }


    private synchronized File getLogFile() {
        if (logFile == null) {
            String logTimestamp = filenameFormat.format(new Date());
            logFile = new File(PathsProvider.getLogDirectory(), logTimestamp + ".log");
        }
        return logFile;
    }

    public void logEvent(OperationLog.Operation operation) {
        String logData = operation.formatLog();
        File outFile = getLogFile();
        try (OutputStream fos = new FileOutputStream(outFile, true)) {
            fos.write(logData.getBytes());
        } catch (IOException e) {
            Log.d(TAG, String.format("Exception writing to log file: %s", logFile), e);
        }
        if (!operation.hasOption(OPTION_NOTRIGGER)) {
            Log.d(TAG, "Delaying upload of logs");
            handler.removeCallbacks(runnable);
            handler.postDelayed(runnable, getUploadWaitTime());
        }
    }

    public synchronized void closeLogFile() {
        handler.removeCallbacks(runnable);
        // If we don't yet have our tbcd id, reschedule.
        if (TBLoaderAppContext.getInstance().getConfig().getTbcdid() == null) {
            Log.d(TAG, "No tbcd id, rescheduling log file upload");
            handler.postDelayed(runnable, getUploadWaitTime());
            return;
        }
        File prevLogFile = logFile;
        if (logFile != null) {
            try {
                logFile = null;
                uploadLog(prevLogFile);
                uploadPendingLogs();
            } catch (Exception e) {
                Log.d(TAG, String.format("Exception closing log file: %s", prevLogFile), e);
                // Not much we can do about this...
            }
        }
    }

    private void uploadPendingLogs() {
        // If there are any pending files, send them now. This will be rare.
        if (mPendingFiles != null) {
            File f = mPendingFiles.poll();
            while (f != null) {
                Log.d(TAG, String.format("Uploading orphaned log file: %s", f.getName()));
                uploadLog(f);
                f = mPendingFiles.poll();
            }
            mPendingFiles = null;
        }
    }

    private void uploadLog(File logFile) {
        Log.d(TAG, String.format("Upload log file %s", logFile));
        String logName = "log/tbcd" + TBLoaderAppContext.getInstance().getConfig().getTbcdid() + "/" + logFile.getName();
        // Consider zipping it, to save upload time. Makes consolidation and searching more difficult.
        TBLoaderAppContext.getInstance().getUploadService().uploadFileAsName(logFile, logName);
    }

}
