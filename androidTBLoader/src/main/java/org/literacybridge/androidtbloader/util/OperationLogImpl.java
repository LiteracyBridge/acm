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
import java.util.regex.Pattern;

import static org.literacybridge.androidtbloader.util.Constants.ISO8601;
import static org.literacybridge.androidtbloader.util.Constants.UTC;

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
    private static final Pattern NEWLINE = Pattern.compile("\n");
    private static final Pattern COMMA = Pattern.compile(",");

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
    private DateFormat filenameFormat;
    private DateFormat logFormat;
    @SuppressLint("SimpleDateFormat")
    private OperationLogImpl() {
        File logDir = PathsProvider.getLogDirectory();
        if (!logDir.exists()) {
            if (!logDir.mkdirs()) {
                Log.d(TAG, String.format("Failed to create directory %s", logDir.getAbsolutePath()));
            }
        }
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

    /**
     * Replaces newlines with spaces and commas with semicolons.
     * @param rawString String that may have problematic characters.
     * @return String with those characters removed.↵
     */
    private String enquote(String rawString) {
        rawString = NEWLINE.matcher(rawString).replaceAll("↵");
        rawString = COMMA.matcher(rawString).replaceAll(";");
        return rawString;
    }

    public void logEvent(OperationLog.Operation operation) {
        StringBuilder builder = new StringBuilder();
        builder.append(String.format("%s,%s", logFormat.format(new Date()), operation.getName()));
        Map<String, String> info = operation.getInfo();
        // Convert Map<String,String> to string of key:value,key:value.
        // Note that \n is munged to ↵ and , to ;
        for (Map.Entry<String, String> entry : info.entrySet()) {
            builder.append(',').append(entry.getKey()).append(':');
            builder.append(enquote(entry.getValue()));
        }
        builder.append("\n");
        File outFile = getLogFile();
        try (OutputStream fos = new FileOutputStream(outFile, true)) {
            fos.write(builder.toString().getBytes());
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
            } catch (Exception e) {
                Log.d(TAG, String.format("Exception closing log file: %s", prevLogFile), e);
                // Not much we can do about this...
            }
        }
    }

    private void uploadLog(File logFile) {
        Log.d(TAG, String.format("Upload log file %s", logFile));
        String logName = "log/tbcd" + TBLoaderAppContext.getInstance().getConfig().getTbcdid() + "/" + logFile.getName();
        // Consider zipping it, to save upload time. Makes consolidation and searching more difficult.
        TBLoaderAppContext.getInstance().getUploadService().uploadFileAsName(logFile, logName);
    }

}
