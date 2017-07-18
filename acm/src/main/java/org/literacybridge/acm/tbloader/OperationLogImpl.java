package org.literacybridge.acm.tbloader;

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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Log operations of the applications. Uploaded to server, to extract app metrics, usage, and updates.
 */

public class OperationLogImpl implements OperationLog.Implementation{
    // This log is, of course, the application log, where we log debugging information about the
    // operation log (this class' function).
    private static final Logger LOG = Logger.getLogger(OperationLogImpl.class.getName());
    private static final String TAG = OperationLogImpl.class.getSimpleName();

    private static int ZIP_THRESHOLD = 20000;
    private static int UPLOAD_WAIT_TIME = 10 * 60 * 1000; // 10 minutes in ms

    private String tbcdId;
    private File logDir;
    private File logFile;
    private DateFormat filenameFormat;
    private DateFormat logFormat;
    public OperationLogImpl(File logDir, String tbcdId) {
        this.logDir = logDir;
        this.tbcdId = tbcdId;
        if (!logDir.exists()) {
            logDir.mkdirs();
        }
        uploadExistingLogs();
        TimeZone UTC = TimeZone.getTimeZone("UTC");
        filenameFormat = new SimpleDateFormat("yyyyMMdd'T'HHmmss.SSS'Z'", Locale.US); // Quoted "Z" to indicate UTC, no timezone offset
        filenameFormat.setTimeZone(UTC);
        logFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US);
        logFormat.setTimeZone(UTC);
    }

    private synchronized File getLogFile() {
        if (logFile == null) {
            String logTimestamp = filenameFormat.format(new Date());
            logFile = new File(logDir, logTimestamp + ".log");
        }
        return logFile;
    }

    private String enquote(String rawString) {
        if (rawString.indexOf(',') >= 0) {
            return String.format("\"%s\"", rawString);
        }
        return rawString;
    }

    public synchronized void logEvent(String name, Map<String, String> info) {
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
            LOG.log(Level.INFO, String.format("Exception writing to log file: %s", logFile), e);
        }
    }

    public synchronized void closeLogFile() {
        if (logFile != null) {
            try {
                uploadLog(logFile);
            } catch (Exception e) {
                LOG.log(Level.INFO, String.format("Exception closing log file: %s", logFile), e);
                // Not much we can do about this...
            } finally {
                logFile = null;
            }
        }
    }

    private void uploadExistingLogs() {
        File [] existingLogs = logDir.listFiles();
        for (File logFile : existingLogs) {
            uploadLog(logFile);
        }
    }

    private void uploadLog(File logFile) {
        //LOG.log(Level.INFO, String.format("Upload log file %s", logFile));
        // Like "log/tbcd000c/20170718T110600.000Z"
        String logName = "log/tbcd" + tbcdId + "/" + logFile.getName();
        if (logFile.length() > ZIP_THRESHOLD) {
            //TODO Zip it.
        }
        //TBLoaderAppContext.getInstance().getUploadManager().uploadFileAsName(logFile, logName);
    }

}
