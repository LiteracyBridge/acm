package org.literacybridge.acm.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Helper to get around the unfortunate difficulty of configuring logging through config files.
 *
 */
public class LogHelper {
    private static final Logger LOG = Logger.getLogger(org.literacybridge.acm.utils.LogHelper.class.getName());

    private static final String FORMAT_PROPERTY = "java.util.logging.SimpleFormatter.format";
    private static final String DEFAULT_FORMAT = "%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS %4$s %2$s %5$s%6$s%n";
    private static final int MAX_LOGFILE_SIZE = 16 * 1024 * 1024;
    private static final int MAX_LOGFILE_COUNT = 10;
    private static final boolean APPEND = true;
    /**
     * Helper class to generate log name patterns. In particular, the 'absolute' method
     * will generate the relative path from the home directory to the given directory.
     */
    private boolean absolute = false;
    private File directory;
    private String logName = "log";
    private String format = null;

    public LogHelper inDirectory(File directory) {
        this.directory = directory;
        return this;
    }
    public LogHelper inDirectory(String dirName) {
        return inDirectory(new File(dirName));
    }

    /**
     * Causes all of the logs to be in the same directory. Use this when the application will
     * change working directory, but the logs should remain together.
     * @return the same LogHelper
     */
    public LogHelper absolute() {
        this.absolute = true;
        return this;
    }

    public LogHelper withName(String name) {
        this.logName = name;
        return this;
    }

    public LogHelper withFormat(String format) {
        this.format = format;
        return this;
    }

    public void initialize() throws IOException {
        StringBuilder pattern = new StringBuilder();
        if (directory == null) { directory = new File("."); }
        directory.mkdirs();
        if (absolute) {
            // Find the path relative to home, then use that to create the directory pattern.
            Path relativeLogPath = Paths.get(System.getProperty("user.home"))
                .relativize(Paths.get(directory.getAbsolutePath()));
            pattern.append("%h/").append(relativeLogPath.toString()).append("/");
        } else {
            pattern.append(directory.getPath()).append('/');
        }
        pattern.append(logName);

        String logFormat = System.getProperty(LogHelper.FORMAT_PROPERTY);
        if (logFormat == null || logFormat.length() == 0) {
            logFormat = format == null ? DEFAULT_FORMAT : format;
            System.setProperty(LogHelper.FORMAT_PROPERTY, logFormat);
        }

        Logger rootLogger = Logger.getLogger("");
        FileHandler logHandler = new FileHandler(pattern.toString(),
            MAX_LOGFILE_SIZE,
            MAX_LOGFILE_COUNT,
            APPEND);
        logHandler.setFormatter(new SimpleFormatter());
        logHandler.setLevel(Level.INFO);
        rootLogger.removeHandler(rootLogger.getHandlers()[0]);
        rootLogger.setLevel(Level.INFO);
        rootLogger.addHandler(logHandler);
        LOG.info("************************************************************");
    }

}
