package org.literacybridge.core.tbloader;

/**
 * Interface definition for a callback to be invoked as the TB-Loader makes progress.
 */

public abstract class ProgressListener {
    public enum Steps {
        ready("Ready"),
        starting("Starting"),
        checkDisk("Checking SD card"),
        fixupDirectories("Fixing device directory names"),
        listDeviceFiles("Listing device files"),
        gatherDeviceFiles("Gathering device files", true),
        gatherUserRecordings("Gathering user recordings", true),
        clearStats("Clearing statistics"),
        clearUserRecordings("Clearing user recordings"),
        clearFeedbackCategories("Clearing feedback categories"),
        copyStatsAndFiles("Zipping statistics and files"),
        reformatting("Reformatting SD card"),
        relabelling("Relabelling SD card"),
        clearSystem("Clearing old TB system files", true),
        updateSystem("Updating TB system files", true),
        updateContent("Updating TB content", true),
        updateCommunity("Updating community content", true),
        listDeviceFiles2("Listing device files after update"),
        delay("Finalizing", false),
        finishing("Finished");

        public final String description;
        public final boolean hasFiles;
        Steps(String description) {
            this(description, false);
        }
        Steps(String description, boolean hasFiles) {
            this.description = description;
            this.hasFiles = hasFiles;
        }

        public String description() {
            return description;
        }

        public int count() {
            return values().length;
        }
    }

    /**
     * Called when the TB-Loader begins a new step.
     * @param step The step being started.
     */
    public abstract void step(Steps step);

    /**
     * Called with some detailed value, such as a file name.
     * @param value being reported.
     */
    public abstract void detail(String value);

    /**
     * Called to add a value to the log.
     * @param value to be logged.
     */
    public abstract void log(String value);

    /**
     * Called to add a value to the log, possibly appending to the most recent line.
     * @param append If true, append to most recent log line; otherwise log to a new line.
     * @param value to be added to the log.
     */
    public abstract void log(boolean append, String value);

    /**
     * Call to log an error value. Implementors can override to provide error UI.
     * @param value in error.
     */
    public void error(String value) {log(value);}
}
