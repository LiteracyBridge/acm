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
import java.util.regex.Pattern;

/**
 * Log operations of the applications. Uploaded to server, to extract app metrics, usage, and updates.
 */

public class OperationLogImpl implements OperationLog.Implementation{
    // This log is, of course, the application log, where we log debugging information about the
    // operation log (this class' function).
    private static final Logger LOG = Logger.getLogger(OperationLogImpl.class.getName());

    private static final Pattern NEWLINE = Pattern.compile("\n");
    private static final Pattern COMMA = Pattern.compile(",");

    private File logDir;
    private File logFile;
    private DateFormat filenameFormat;
    private DateFormat timestampFormat;

    OperationLogImpl(File logDir) {
        this.logDir = logDir;
        if (!logDir.exists()) {
            if (!logDir.mkdirs()) {
                LOG.log(Level.WARNING, String.format("Failed to create directory %s", logDir.getAbsolutePath()));
            }
        }
        TimeZone UTC = TimeZone.getTimeZone("UTC");
        filenameFormat = new SimpleDateFormat("yyyyMMdd'T'HHmmss.SSS'Z'", Locale.US); // Quoted "Z" to indicate UTC, no timezone offset
        filenameFormat.setTimeZone(UTC);
        timestampFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US);
        timestampFormat.setTimeZone(UTC);
    }

    private synchronized File getLogFile() {
        if (logFile == null) {
            String logTimestamp = filenameFormat.format(new Date());
            logFile = new File(logDir, logTimestamp + ".log");
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

    public synchronized void logEvent(OperationLog.Operation operation) {
        String name = operation.getName();
        Map<String, String> info = operation.getInfo();
        StringBuilder builder = new StringBuilder();
        builder.append(String.format("%s,%s", timestampFormat.format(new Date()), name));
        if (info != null) {
            for (Map.Entry<String, String> entry : info.entrySet()) {
                builder.append(',').append(entry.getKey()).append(':');
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
            logFile = null;
        }
    }

}
