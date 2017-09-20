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

import static org.literacybridge.core.tbloader.TBLoaderConstants.ISO8601;

/**
 * Log operations of the applications. Uploaded to server, to extract app metrics, usage, and updates.
 */

public class OperationLogImpl implements OperationLog.Implementation{
    // This log is, of course, the application log, where we log debugging information about the
    // operation log (this class' function).
    private static final Logger LOG = Logger.getLogger(OperationLogImpl.class.getName());

    private File logDir;
    private File logFile;
    private DateFormat filenameFormat = ISO8601;

    OperationLogImpl(File logDir) {
        this.logDir = logDir;
        if (!logDir.exists()) {
            if (!logDir.mkdirs()) {
                LOG.log(Level.WARNING, String.format("Failed to create directory %s", logDir.getAbsolutePath()));
            }
        }
    }

    private synchronized File getLogFile() {
        if (logFile == null) {
            String logTimestamp = filenameFormat.format(new Date());
            logFile = new File(logDir, logTimestamp + ".log");
        }
        return logFile;
    }

    public synchronized void logEvent(OperationLog.Operation operation) {
        String logData = operation.formatLog();
        File outFile = getLogFile();
        try (OutputStream fos = new FileOutputStream(outFile, true)) {
            fos.write(logData.getBytes());
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
