package org.literacybridge.androidtbloader.util;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.util.Log;

import org.literacybridge.androidtbloader.TBLoaderAppContext;
import org.literacybridge.core.fs.OperationLog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import static org.literacybridge.androidtbloader.util.Constants.ISO8601;
import static org.literacybridge.androidtbloader.util.Constants.UTC;

/**
 * Log operations of the applications. Uploaded to server, to extract app metrics, usage, and updates.
 */

public class OperationLogImpl implements OperationLog.Implementation{
    private static final String TAG = OperationLogImpl.class.getSimpleName();

    private static int ZIP_THRESHOLD = 20000;
    private static int UPLOAD_WAIT_TIME = 10 * 60 * 1000; // 10 minutes in ms

    private Handler handler = new Handler();
    private Runnable runnable;

    private File logFile;
    private DateFormat filenameFormat;
    private DateFormat logFormat;
    @SuppressLint("SimpleDateFormat")
    public OperationLogImpl() {
        File logDir = PathsProvider.getLogDirectory();
        if (!logDir.exists()) {
            logDir.mkdirs();
        }
        uploadExistingLogs();
        filenameFormat = ISO8601;
        logFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US);
        logFormat.setTimeZone(UTC);

        runnable = new Runnable() {
            @Override
            public void run() {
                closeLogFile();
            }
        };
    }

    private synchronized File getLogFile() {
        if (logFile == null) {
            String logTimestamp = filenameFormat.format(new Date());
            logFile = new File(PathsProvider.getLogDirectory(), logTimestamp + ".log");
        }
        return logFile;
    }

    private String enquote(String rawString) {
        if (rawString.indexOf(',') >= 0) {
            return String.format("\"%s\"", rawString);
        }
        return rawString;
    }

    public void logEvent(String name, Map<String, String> info) {
        StringBuilder builder = new StringBuilder();
        builder.append(String.format("%s,%s", logFormat.format(new Date()), name));
        if (info != null) {
            for (Map.Entry<String, String> entry : info.entrySet()) {
                builder.append(',').append(entry.getKey()).append(':');
                // TODO: check for new lines.
                builder.append(enquote(entry.getValue()));
            }
        }
        builder.append("\n");
        File outFile = getLogFile();
        try (OutputStream fos = new FileOutputStream(outFile, true)) {
            fos.write(builder.toString().getBytes());
        } catch (IOException e) {
            Log.d(TAG, String.format("Exception writing to log file: %s", logFile), e);
        }
        Log.d(TAG, "Delaying upload of logs");
        handler.removeCallbacks(runnable);
        handler.postDelayed(runnable, UPLOAD_WAIT_TIME);
    }

    public synchronized void closeLogFile() {
        handler.removeCallbacks(runnable);
        if (logFile != null) {
            try {
                uploadLog(logFile);
            } catch (Exception e) {
                Log.d(TAG, String.format("Exception closing log file: %s", logFile), e);
                // Not much we can do about this...
            } finally {
                logFile = null;
            }
        }
    }

    private void uploadExistingLogs() {
        File logDir = PathsProvider.getLogDirectory();
        File [] existingLogs = logDir.listFiles();
        for (File logFile : existingLogs) {
            uploadLog(logFile);
        }
    }

    private void uploadLog(File logFile) {
        Log.d(TAG, String.format("Upload log file %s", logFile));
        String logName = "log/tbcd" + TBLoaderAppContext.getInstance().getConfig().getTbcdid() + "/" + logFile.getName();
        if (logFile.length() > ZIP_THRESHOLD) {
            //TODO Zip it.
        }
        TBLoaderAppContext.getInstance().getUploadManager().uploadFileAsName(logFile, logName);
    }

}
