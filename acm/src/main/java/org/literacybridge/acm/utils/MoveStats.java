package org.literacybridge.acm.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.FileExistsException;
import org.apache.commons.io.FileUtils;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.literacybridge.core.fs.ZipUnzip;
import org.literacybridge.core.tbloader.TBLoaderConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.literacybridge.core.tbloader.TBLoaderConstants.TALKING_BOOK_DATA;
import static org.literacybridge.core.tbloader.TBLoaderConstants.USER_RECORDINGS;

/**
 * Moves stats and user feedback to a directory for processing.
 * The arguments are two paths, an input directory, an output directory, and a timestamp. Any
 * output will be written to the output directory in a subdirectory named with the timestamp. If
 * no stats or user feedback is found, the timestamp directory will not be created.
 * <p>
 * The input is a directory, like ~/Dropbox/stats, containing one or more subdirectories
 * for tbloaders. The tbloader directories then contains a 'collected-data' directory,
 * and those contain directories for projects. The project directories may contain
 * userrecordings and talkingbookdata subdirectories, which are the actual directories
 * of interest.
 * <p>
 * The directories are processed as follows:
 * given a directory with these subdirectories:
 * {TBLOADER-ID} / collected-data / {PROJECT} / talkingbookdata / --- files ---
 * {TBLOADER-ID} / collected-data / {PROJECT} / operationaldata / --- files ---
 * {TBLOADER-ID} / collected-data / {PROJECT} / userrecordings / {DEPLOYMENT} / --- files ---
 * the files in userrecordings are moved to
 * {TARGET} / {TIMESTAMP} / userrecordings / {PROJECT} / {DEPLOYMENT} / ...
 * and the files in talkingbookdata, operationaldata, and any additional (possibly extraneous
 * files and/or directories) are moved to a .zip file
 * {TARGET} / {TIMESTAMP} / {TBLOADER-ID}.zip
 */
public class MoveStats {
    private static final Logger logger = LoggerFactory.getLogger(MoveStats.class);

    private final Params params;
    private Set<String> whitelistedIds = null;
    private Set<String> blacklistedIds = null;

    public static void main(String[] args) throws IOException, CmdLineException {
        Params params = new Params();
        CmdLineParser parser = new CmdLineParser(params);

        MoveStats mover = new MoveStats(params);
        if (!mover.validateCommandLineArgs(parser, args)) {
            printUsage(parser);
            System.exit(100);
        }

        int rc = mover.move();
        System.exit(rc);
    }

    private MoveStats(Params params) {
        this.params = params;
    }

    /**
     * Checks that required directories exist, and that optional report is not a directory.
     *
     * @return true if args are OK, false otherwise.
     */
    private boolean validateCommandLineArgs(CmdLineParser parser, String[] args) {
        boolean filesOk = true;
        try {
            parser.parseArgument(args);
        } catch (CmdLineException e) {
            System.err.println(String.format("Error: %s", e.getMessage()));
            return false;
        }
        if (!params.sourceDir.isDirectory()) {
            System.err.println(
                    String.format("'%s' is not a directory", params.sourceDir.getName()));
            filesOk = false;
        } else if (!params.sourceDir.exists()) {
            System.err.println(String.format("'%s' does not exist", params.sourceDir.getName()));
            filesOk = false;
        }
        if (!params.targetDir.isDirectory()) {
            System.err.println(
                    String.format("'%s' is not a directory", params.targetDir.getName()));
            filesOk = false;
        } else if (!params.targetDir.exists()) {
            System.err.println(String.format("'%s' does not exist", params.targetDir.getName()));
            filesOk = false;
        }

        if (params.report != null) {
            if (params.report.exists() && params.report.isDirectory()) {
                System.err.println(String.format("'%s' is a directory.", params.report.getName()));
                filesOk = false;
            }
        }

        if (params.whiteListFile != null) {
            whitelistedIds = new HashSet<>();
            try {
                IOUtils.readLines(params.whiteListFile, whitelistedIds);
                whitelistedIds = whitelistedIds.stream().map(String::toLowerCase).collect(Collectors.toSet());
            } catch (IOException e) {
                System.err.println(String.format("Can't read whitelist file: %s", e.getMessage()));
                filesOk = false;
            }

        }

        if (params.blackListFile != null) {
            blacklistedIds = new HashSet<>();
            try  {
                IOUtils.readLines(params.blackListFile, blacklistedIds);
                blacklistedIds = blacklistedIds.stream().map(String::toLowerCase).collect(Collectors.toSet());
            } catch (IOException e) {
                System.err.println(String.format("Can't read blacklist file: %s", e.getMessage()));
                filesOk = false;
            }

        }

        return filesOk;
    }

    /**
     * Given a source directory, a target directory, and a default timestamp:
     * .  For every tbcd9999 subdirectory of the source directory
     * .    For every child of the tbcd9999 directory
     * .      If it is a directory named 'collected-data'
     * .        process from src / tbcd9999 / collected-data to target / default-timestamp / ...
     * .        write 'target / default-timestamp' to stdout
     * .      else if it is a file named {ISO 8601}.zip
     * .        unzip it
     * .        process from src / tbcd9999 / {ISO 8601} to target / {ISO 8601}
     * .        write 'target / {ISO 8601}' to stdout
     * <p>
     * This function enumerates the tbcd9999 directories and processes them.
     *
     * @return the exit code for the process.
     */
    private int move() {
        MoveResults result = new MoveResults(), tmpResult;

        // Generally, sourceDir is like ~/Dropbox/stats or ~Dropbox/outbox/stats
        // Generally, targetDir is like ~/Dropbox/collected-data-processed

        // Process any non-hidden subdirectories.
        File children[] = params.sourceDir.listFiles();
        if (children != null) {
            for (File tbLoaderDir : children) {
                if (isGoodTbLoaderDir(tbLoaderDir)) {
                    result.tbcdProcessed(tbLoaderDir.getName());
                    tmpResult = processOneTbLoaderDir(tbLoaderDir, params.targetDir,
                                                      params.defaultTimeStamp);
                    result.add(tmpResult);
                } else {
                    result.tbcdSkipped(tbLoaderDir.getName());
                    FileUtils.deleteQuietly(tbLoaderDir);
                }
            }
        }

        // Let people know what's happened.
        // Make an overall report, on stdout.
        if (params.report != null) {
            result.makeReport(params.report);
        } else {
            result.makeReport(System.out);
        }

        return result.getExitCode();
    }

    private boolean isGoodTbLoaderDir(File tbLoaderDir) {
        if (!tbLoaderDir.isDirectory() || tbLoaderDir.isHidden()) {
            return false;
        }
        String tbcdIdName = tbLoaderDir.getName().toLowerCase();
        if (tbcdIdName.startsWith(".")) {
            return false;
        }
        // blacklisted?
        if (blacklistedIds != null && blacklistedIds.contains(tbcdIdName)) {
            return false;
        }
        // not whitelisted?
        if (whitelistedIds != null && ! whitelistedIds.contains(tbcdIdName)) {
            return false;
        }
        return true;
    }

    /**
     * Process one tbloader directory into the target collection directory. If the tbloader
     * doesn't contain a timestamp, use the default timestamp.
     *
     * @param tbLoaderDir One TB-Loader directory like ...stats / tbde{tbcd id}
     * @param targetDir Destination for stats, like Dropbox / collected-data-processed
     * @param defaultTimeStamp The timestamp under which to collect things with no other timestamp.
     */
    private MoveResults processOneTbLoaderDir(File tbLoaderDir, File targetDir,
                                              String defaultTimeStamp) {
        MoveResults result = new MoveResults(), tmpResult;
        // Process 'collected-data' subdirectories or {ISO 8601}.zip file children
        File children[] = tbLoaderDir.listFiles();
        if (children != null) {
            // First look for an actual 'collected-data' directory, and process it. Then process
            // the timestamped directories; each will be processed as 'collected-data' (which is
            // why we need to get the real 'collected-data' done first.
            for (File child : children) {
                if (child.isDirectory() && child.getName().equalsIgnoreCase(
                        TBLoaderConstants.COLLECTED_DATA_SUBDIR_NAME)) {
                    tmpResult = processCollectedData(child, targetDir, defaultTimeStamp, defaultTimeStamp);
                    result.add(tmpResult);
                }
            }
            for (File child : children) {
                if (!(child.isDirectory() && child.getName().equalsIgnoreCase(
                        TBLoaderConstants.COLLECTED_DATA_SUBDIR_NAME))) {
                    tmpResult = process8601Zip(child, targetDir, defaultTimeStamp);
                    result.add(tmpResult);
                }
            }
        }
        tbLoaderDir.delete();
        return result;
    }

    /**
     * Given a collected-data subdirectory (may have some other name), copy any userrecordings,
     * and talkingbookdata & operationaldata to the targetDir / timestamp / tbcd9999
     * (tbcd9999 is the name of the parent of collectedDataDir)
     *
     * @param collectedDataDir {tbcd id} / collected-data or {tbcd id} / {timestamp}, with
     *                         subdirectories like / {PROJ1},  / {PROJ2},  ...
     * @param targetDir output directory from arguments, like Dropbox/collected-data-processed/2018/04/18
     * @param statsTimeStamp timestamp under which to collect statistics, 2017y06m19d13h20m20s or
     *                       20170619T132020.123
     * @param userFeedbackTimeStamp timestamp under which to collect user feedback, 2017y06m19d13h20m20s
     */
    private MoveResults processCollectedData(File collectedDataDir, File targetDir,
                                             String statsTimeStamp, String userFeedbackTimeStamp) {
        boolean hasTbData = hasTalkingBookData(collectedDataDir);
        boolean hasUfData = hasUserRecordings(collectedDataDir);

        String tbcdId = collectedDataDir.getParentFile().getName();
        // Get non-hidden subdirectories, convert to Strings, then collect in a List<String>
        File projectDirs[] = collectedDataDir.listFiles(
                (fn) -> fn.isDirectory() && !fn.isHidden() && !fn.getName().startsWith("."));
        List<String> projects = Arrays.stream(projectDirs != null ? projectDirs : new File[0])
                .map(File::getName)
                .collect(Collectors.toList());
        MoveResults result = new MoveResults();

        if (hasTbData || hasUfData) {
            if (hasUfData) {
                //File targetUfParentDir = new File(targetDir, userFeedbackTimeStamp);
                //File targetUfDataDir = new File(targetUfParentDir, USER_RECORDINGS_PATH);
                File targetUfDataDir = new File(targetDir, USER_RECORDINGS);
                try {
                    moveUserRecordings(collectedDataDir, targetUfDataDir);
                    result.recordingsMoved(tbcdId, statsTimeStamp, projects);
                } catch (IOException e) {
                    // Report, but otherwise ignore.
                    result.recordingsFailedToMove(tbcdId, statsTimeStamp, projects);
                }
            }
            if (hasTbData) {
                File targetTbDataDir = new File(targetDir, statsTimeStamp);
                try {
                    moveStats(collectedDataDir, targetTbDataDir);
                    result.statsMoved(tbcdId, statsTimeStamp, projects);
                } catch (IOException e) {
                    // Report, but otherwise ignore.
                    result.statsFailedToMove(tbcdId, statsTimeStamp, projects);
                }
            }
            try {
                FileUtils.cleanDirectory(collectedDataDir);
                collectedDataDir.delete();
            } catch (Exception e) {
                // Report, but otherwise ignore.
            }
        }
        return result;
    }

    /**
     * Given a file, see if it is named {ISO 8601}.zip. If so, unzip it, and process the contents as
     * collected-data. Use {ISO 8601} as the timestamp of the file.
     *
     * @param zipFile A file, possibly named like {iso 8601 time}.zip.
     * @param targetDir Destination for an iso8601 named file.
     * @param userFeedbackTimeStamp The timestamp where we're collecting user feedback.
     */
    private MoveResults process8601Zip(File zipFile, File targetDir, String userFeedbackTimeStamp) {
        MoveResults result = new MoveResults();
        String iso8601 = get8601(zipFile.getName());
        if (iso8601 == null) {
            return result;
        }
        // The zip file contains a directory with the name in unzipDir, so we unzip it directly
        // into the same directory as the zip file. However, we need the unzipped directory to be
        // named 'collected-data', so we'll rename it. Then, when we re-zip the files (without the
        // user recordings), the directory name will be 'collected-data', as the stats importer
        // requires.
        File unzipDir = new File(zipFile.getParentFile(), iso8601);
        File collectedDataDir = new File(zipFile.getParentFile(),
                                         TBLoaderConstants.COLLECTED_DATA_SUBDIR_NAME);
        try {
            ZipUnzip.unzip(zipFile, zipFile.getParentFile());
            unzipDir.renameTo(collectedDataDir);
            result = processCollectedData(collectedDataDir, targetDir, iso8601, userFeedbackTimeStamp);
            // Only if processed successfully, remove the .zip file.
            zipFile.delete();
        } catch (IOException e) {
            // Just ignore. Clean up later.
        } finally {
            // Clean up.
            try {
                FileUtils.cleanDirectory(collectedDataDir);
                collectedDataDir.delete();
            } catch (Exception e1) {
                // we tried...
            }
        }
        return result;
    }

    /**
     * See if the given name is {ISO 8601}.zip.
     *
     * @param name Possible {ISO 8601}.zip file name.
     * @return the {ISO 8601} part, or null if not a valid string.
     */
    private String get8601(String name) {
        String result = null;
        // @TODO: We may not need to drop the ".zip" just for parsing.
        if (name.toLowerCase().endsWith(".zip")) {
            name = name.substring(0, name.length() - 4);
        }
        // SimpleDateFormat can't deal with 'Z' suffix.
        DateFormat df1 = new SimpleDateFormat("yyyyMMdd'T'HHmmss.SSS");
        try {
            Date result1 = df1.parse(name);
            result = name;
        } catch (ParseException e) {
            // Not a valid iso8601 string. Do nothing, and return null.
        }
        return result;
    }

    /**
     * Move userrecordings out of the statistics and into their own subdirectory. Don't bother
     * zipping them because the audio files are compressed, and mostly do not compress at all.
     * Files are moved from: collected-data / {project} / UserRecordings / {deployment} / --- files ---
     * to: {timestamp} / UserRecordings / {project} / {deployment} / --- files ---
     *
     * @param collectedDataDir Directory with user recordings.
     * @param targetUserRecordingsDir Directory where user recordings should go.
     * @throws IOException if the recordings can be neither moved nor copied & deleted.
     */
    private void moveUserRecordings(File collectedDataDir, File targetUserRecordingsDir)
            throws IOException {
        logger.info(String.format("Processing user feedback in %s",
                                  collectedDataDir.getAbsolutePath()));

        File[] projectDirs = collectedDataDir.listFiles(
                (fn) -> fn.isDirectory() && !fn.isHidden() && !fn.getName().startsWith("."));
        // Iterate over the projects...
        for (File projectDir : projectDirs) {

            // See if this project has userrecordings.
            File userrecordings = IOUtils.FileIgnoreCase(projectDir, USER_RECORDINGS);
            if (!userrecordings.exists())
                continue;

            // Target directory for any {DEPLOYMENT} found within this {PROJECT}
            File targetProjDir = new File(targetUserRecordingsDir,
                                          projectDir.getName().toLowerCase());
            logger.info(String.format("  processing %s into %s", projectDir.getName(),
                                      targetProjDir.getAbsolutePath()));

            // We have a userrecordings subdirectory, within some project subdirectory.
            // Enumerate the contained {DEPLOYMENT} directories.
            File[] updateDirs = userrecordings.listFiles(
                    (fn) -> fn.isDirectory() && !fn.isHidden() && !fn.getName().startsWith("."));
            for (File updateDir : updateDirs) {
                logger.info(String.format("    moving %s", updateDir.getName()));

                // Move the {DEPLOYMENT} directory to the target
                try {
                    FileUtils.moveDirectoryToDirectory(updateDir, targetProjDir, true);
                } catch (FileExistsException e) {
                    logger.info(
                            String.format("    -> move failed, copying %s", updateDir.getName()));
                    // Already exists (from a different tbloader's feedback), so copy contents & delete.
                    File targetUpdateDir = new File(targetProjDir, updateDir.getName());
                    FileUtils.copyDirectory(updateDir, targetUpdateDir);
                    FileUtils.deleteDirectory(updateDir);
                }
            }
        }
    }

    private void moveStats(File collectedDataDir, File targetTbDataDir) throws IOException {
        targetTbDataDir.mkdir();
        File tbLoaderDir = collectedDataDir.getParentFile();
        String zipDirs[] = new String[1];
        zipDirs[0] = collectedDataDir.getName();

        // Zip the contents of one tbloader's reported stats directory
        logger.info(String.format("Zipping %s and moving to %s", tbLoaderDir,
                                  targetTbDataDir.getAbsolutePath()));
        File zipFile = new File(targetTbDataDir, tbLoaderDir.getName() + ".zip");
        ZipUnzip.zip(tbLoaderDir, zipFile, true /* include base dir */, zipDirs);
    }

    /**
     * See if a collected-data directory contains Talking Book data.
     *
     * @param collectedDataDir The directory of interest.
     * @return true if a 'talkingbookdata' subdirectory was found.
     */
    private static boolean hasTalkingBookData(File collectedDataDir) {
        // The 'collected-data' subdirectory should contain project (UWR, CARE, ...)
        // subdirectories. There must be at least one such project subdirectory, and ALL
        // of them must contain a 'talkingbookdata' subdirectory:
        // tbcd000c/collected-data/UWR/talkingbookdata
        File[] projects = collectedDataDir.listFiles();
        boolean foundProjectWithTbData = false;  // haven't found anything
        boolean foundProjectWithoutTbData = false;  // yet...
        for (File projectDir : projects) {
            if (!projectDir.isDirectory() || projectDir.isHidden() || projectDir.getName()
                    .startsWith(".")) {
                continue;
            }
            File tbdata = IOUtils.FileIgnoreCase(projectDir, TALKING_BOOK_DATA);
            if (!tbdata.exists() || !tbdata.isDirectory()) {
                logger.warn(
                        String.format("Directory %s has no expected %s subdirectory (in %s).",
                                      projectDir.getName(), TALKING_BOOK_DATA, collectedDataDir.getAbsolutePath()));
                foundProjectWithoutTbData = true;
            } else {
                foundProjectWithTbData = true; // found at least one with TB Data
                break;
            }
        }

        // For many years the code was like this. If any subdirectory existed in the tbcd000c/collected-data
        // directory, and that subdirectory did not itself have a 'talkingbookdata' subdirectory, then
        // the entire tbcd000c/collected-data directory was ignored for purposes of statistics.
        //return (foundProjectWithTbData && !foundProjectWithoutTbData);

        return foundProjectWithTbData;
    }

    /**
     * See if a collected-data directory contains User Recordings.
     *
     * @param collectedDataDir The directory of interest.
     * @return true if a 'userrecordings' subdirectory was found.
     */
    private static boolean hasUserRecordings(File collectedDataDir) {
        // The 'collected-data' subdirectory should contain project (UWR, CARE, ...)
        // subdirectories. There must be at least one such project subdirectory, and at
        // least one of them must contain a 'userrecordings' subdirectory:
        // tbcd000c/collected-data/UWR/userrecordings
        File[] projects = collectedDataDir.listFiles();
        for (File projectDir : projects) {
            if (!projectDir.isDirectory() || projectDir.isHidden() || projectDir.getName()
                    .startsWith(".")) {
                continue;
            }
            File userrecordings = IOUtils.FileIgnoreCase(projectDir, USER_RECORDINGS);
            // If there is a userrecordings directory, are there also audio files?
            if (userrecordings.exists() && userrecordings.isDirectory()) {
                return hasAudioFile(userrecordings);
            }
        }

        return false;
    }

    /**
     * Determine if a directory or any of its children contains .a18 or .mp3 files.
     * @param directory The directory of interest.
     * @return True if there are any audio files, false if none found.
     */
    private static boolean hasAudioFile(File directory) {
        File children[] = directory.listFiles();
        if (children != null) {
            for (File child : children) {
                if (child.isDirectory()) {
                    if (hasAudioFile(child)) {
                        return true;
                    }
                } else {
                    String name = child.getName().toLowerCase();
                    if (name.endsWith(".a18") || name.endsWith(".mp3") || name.endsWith(".ogg")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * A FilenameFilter class to accept well-formed collected-data subdirectories
     * of the stats inbox.
     * <p>
     * These well-formed directories look like:
     * {TB-LOADER-ID} / collected-data / {PROJECT} / talkingbookdata
     * and
     * {TB-LOADER-ID} / collected-data / {PROJECT} / userrecordings
     * where {TB-LOADER-ID} is like 'tbcd000c' and {PROJECT} is like 'UWR'
     * <p>
     * This class is intended to be subclassed. This class determines if a
     * collected-data directory exists, and the subclasses determine if the
     * contents of the collected-data subdirectory warrents inclusion of this
     * file.
     */
    private static abstract class CollectedDataFilter implements FilenameFilter {

        /**
         * Tests if a specified file should be included in a file list.
         *
         * @param dir  the directory in which the file was found.
         * @param name the name of the file.
         * @return <code>true</code> if and only if the name should be
         * included in the file list; <code>false</code> otherwise.
         */
        @Override
        public boolean accept(File dir, String name) {
            // Filters the 'sourceDir' for subdirectories with contents like:
            // collected-data/<project>/talkingbookdata
            // <project> is like uwr or CARE, one or more in the collected-data
            // directory

            // Only accept directories
            File subdir = new File(dir, name);
            if (!subdir.isDirectory())
                return false;

            // Only accept non-hidden directories
            if (name.startsWith(".") || subdir.isHidden())
                return false;

            // Only accept directories that contain a subdirectory named 'collected-data', like
            // tbcd000c/collected-data
            File collectedData = IOUtils.FileIgnoreCase(subdir,
                                                        TBLoaderConstants.COLLECTED_DATA_SUBDIR_NAME);
            if (!collectedData.exists() || !collectedData.isDirectory()) {
                logger.warn(String.format(
                        "The 'TB-Builder id' directory %s has no expected %s subdirectory.", name,
                        TBLoaderConstants.COLLECTED_DATA_SUBDIR_NAME));
                return false;
            }

            // If we made it this far, see if the subclass accepts this collected-data directory.
            return acceptCollectedData(collectedData);
        }

        // Subclasses implement this to accept a collected-data subdirectory based on
        // whatever further criteria they choose.
        abstract boolean acceptCollectedData(File collectedData);
    }

    static class TalkingBookDataFilter extends CollectedDataFilter {

        @Override
        boolean acceptCollectedData(File collectedData) {
            return hasTalkingBookData(collectedData);
        }
    }

    static class UserRecordingsFilter extends CollectedDataFilter {

        @Override
        boolean acceptCollectedData(File collectedData) {
            return hasUserRecordings(collectedData);

        }
    }

    /**
     * Helper class for collecting results of the move operations.
     */
    static class MoveResults {
        Set<String> tbcdsProcessed = new HashSet<>();
        Set<String> tbcdsSkipped = new HashSet<>();
        // Keep track of stats moved, failed to move, recordings moved, failed to move.
        // Track by tbcdid / project / timestamp
        Map<String, Map<String, Set<String>>> statsMoved = new HashMap<>();
        Map<String, Map<String, Set<String>>> statsFailedToMove = new HashMap<>();
        Map<String, Map<String, Set<String>>> recordingsMoved = new HashMap<>();
        Map<String, Map<String, Set<String>>> recordingsFailedToMove = new HashMap<>();

        /**
         * Add a single entry to the given map.
         *
         * @param map       The map to update, statsMoved, statsFailedToMove, etc.
         * @param tbcdId    The tbcd id
         * @param projects  A list of one or more projects
         * @param timeStamp The timestamp for the project(s)
         */
        private void addToMap(Map<String, Map<String, Set<String>>> map, String tbcdId,
                              Collection<String> projects, String timeStamp) {
            Map<String, Set<String>> projectsMap = map.computeIfAbsent(tbcdId, k -> new HashMap<>());
            for (String project : projects) {
                Set<String> timeStampSet = projectsMap.computeIfAbsent(project,
                                                                       k -> new HashSet<>());
                timeStampSet.add(timeStamp);
            }
        }

        /**
         * Add all the elements from one map to another. Understands the structure
         * of the data, and does wholesale moves when possible, item-by-item merges otherwise.
         *
         * @param to   here
         * @param from here
         */
        private void addToMap(Map<String, Map<String, Set<String>>> to,
                              Map<String, Map<String, Set<String>>> from) {
            for (String tbcdId : from.keySet()) {
                // If the source tbcd isn't in the target, just add it whole.
                if (!to.containsKey(tbcdId)) {
                    to.put(tbcdId, from.get(tbcdId));
                } else {
                    // Otherwise, add by projects.
                    Map<String, Set<String>> toProjectMap = to.get(tbcdId);
                    Map<String, Set<String>> fromProjectMap = from.get(tbcdId);
                    for (String project : fromProjectMap.keySet()) {
                        // If the source project isn't in the target, add it whole.
                        if (!toProjectMap.containsKey(project)) {
                            toProjectMap.put(project, fromProjectMap.get(project));
                        } else {
                            // Otherwise, add all the timestamps.
                            toProjectMap.get(project).addAll(fromProjectMap.get(project));
                        }
                    }
                }
            }
        }

        void tbcdProcessed(String tbcdId) {
            tbcdsProcessed.add(tbcdId);
        }

        void tbcdSkipped(String tbcdId) {
            tbcdsSkipped.add(tbcdId);
        }

        /**
         * Record the fact that stats were moved.
         *
         * @param tbcdid    That generated the stats.
         * @param timeStamp of the stats.
         * @param projects  with stats.
         */
        void statsMoved(String tbcdid, String timeStamp, Collection<String> projects) {
            addToMap(statsMoved, tbcdid, projects, timeStamp);
        }

        /**
         * Record the fact that stats failed to move.
         *
         * @param tbcdid    That generated the stats.
         * @param timeStamp of the stats.
         * @param projects  with stats.
         */
        void statsFailedToMove(String tbcdid, String timeStamp, Collection<String> projects) {
            addToMap(statsFailedToMove, tbcdid, projects, timeStamp);
        }

        /**
         * Record the fact that user recordings were moved.
         *
         * @param tbcdid    That generated the user recordings.
         * @param timeStamp of the user recordings.
         * @param projects  with user recordings.
         */
        void recordingsMoved(String tbcdid, String timeStamp, Collection<String> projects) {
            addToMap(recordingsMoved, tbcdid, projects, timeStamp);
        }

        /**
         * Record the fact that user recordings failed to move.
         *
         * @param tbcdid    That generated the user recordings.
         * @param timeStamp of the user recordings.
         * @param projects  with user recordings.
         */
        void recordingsFailedToMove(String tbcdid, String timeStamp, Collection<String> projects) {
            addToMap(recordingsFailedToMove, tbcdid, projects, timeStamp);
        }

        /**
         * Merge another set of results into this one. Useful to gather results as we progress.
         *
         * @param moreResults The results to be added.
         * @return this.
         */
        MoveResults add(MoveResults moreResults) {
            addToMap(statsMoved, moreResults.statsMoved);
            addToMap(statsFailedToMove, moreResults.statsFailedToMove);
            addToMap(recordingsMoved, moreResults.recordingsMoved);
            addToMap(recordingsFailedToMove, moreResults.recordingsFailedToMove);
            tbcdsProcessed.addAll(moreResults.tbcdsProcessed);
            tbcdsSkipped.addAll(moreResults.tbcdsSkipped);
            return this;
        }

        /**
         * If any stats or recordings were moved, return 0. They'll need to be imported.
         * @return 0 if anything was moved, 1 otherwise.
         */
        int getExitCode() {
            if (statsMoved.size() > 0 || recordingsMoved.size() > 0) {
                return 0;
            }
            return 1;
        }

        /**
         * Print a report to the given PrintStream.
         *
         * @param ps Where to print.
         */
        void makeReport(PrintStream ps) {
            String report = new Report().generate();
            ps.print(report);
            ps.flush();
        }

        /**
         * Print a report to the given file. If the name ends with ".html", generate html.
         *
         * @param file Where to print the report.
         */
        void makeReport(File file) {
            boolean html = file.getName().toLowerCase().endsWith(".html");
            String report = new Report(html).generate();
            if (report.length() > 0) {
                try {
                    // If the file exists, append a new report to it.
                    FileOutputStream fos = new FileOutputStream(file, true);
                    PrintStream ps = new PrintStream(fos);

                    ps.print(report);
                    ps.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        /**
         * An interface for getting a set of report lines.
         */
        private interface GetLines {
            public Collection<String> getLines();
        }

        /**
         * Helper class to generate a report.
         */
        private class Report {
            boolean html;

            Report() {
                this(false);
            }

            Report(boolean html) {
                this.html = html;
            }

            /**
             * The driver for generating the report.
             *
             * @return the report, as a string.
             */
            private String generate() {
                // List of all tbcd ids seen.
                Set<String> tbcdIds = new HashSet<>();
                tbcdIds.addAll(statsMoved.keySet());
                tbcdIds.addAll(statsFailedToMove.keySet());
                tbcdIds.addAll(recordingsMoved.keySet());
                tbcdIds.addAll(recordingsFailedToMove.keySet());

                StringBuilder result = new StringBuilder();
                if (tbcdsSkipped.size() > 0) {
                    result.append(makeList("Skipped", 2, () -> tbcdsSkipped,
                                           "style=\"font-family:monospace; list-style:none\""));
                }
                if (tbcdsProcessed.size() > 0) {
                    result.append(makeList("Processed", 2, () -> tbcdsProcessed,
                                           "style=\"font-family:monospace; list-style:none\""));
                }
                for (String tbcdId : tbcdIds) {
                    result.append(generate(tbcdId));
                }
                return result.toString();
            }

            /**
             * Report on one tbcd id.
             *
             * @param tbcdId to be reported.
             * @return the report for one tbcd id, as a string.
             */
            private String generate(String tbcdId) {
                StringBuilder result = new StringBuilder(h(2, "Moved collected-data for " + tbcdId));
                result.append(generate(statsMoved.get(tbcdId), "Stats Moved"));
                result.append(generate(statsFailedToMove.get(tbcdId), "Stats Failed To Move"));
                result.append(generate(recordingsMoved.get(tbcdId), "Recordings Moved"));
                result.append(
                        generate(recordingsFailedToMove.get(tbcdId), "Recordings Failed To Move"));
                return result.toString();
            }

            /**
             * Report one set of stats (statsMoved, statsFailedToMove, etc.) for a tbcd id.
             *
             * @param mapByProject The stats to report.
             * @param name         (or heading) of the stats, like "Stats Moved".
             * @return the report for one set of stats, as a string.
             */
            private String generate(Map<String, Set<String>> mapByProject, String name) {
                StringBuilder result = new StringBuilder();
                if (mapByProject != null) {
                    result.append(h(3, name));
                    for (String project : mapByProject.keySet()) {
                        result.append(generate(mapByProject.get(project), project));
                    }
                }
                return result.toString();
            }

            /**
             * Report the timestamps for one project (in one tbcd id).
             *
             * @param setOfTimestamps Timestamps collected for the project
             * @param project to be reported.
             * @return the report of timestamps, as a string.
             */
            private String generate(Set<String> setOfTimestamps, String project) {
                StringBuilder result = new StringBuilder();
                result.append(makeList(project, 4, () -> setOfTimestamps, "style=\"font-family:monospace; list-style:none\""));
                return result.toString();
            }

            private String makeList(String heading, int hlevel, GetLines getter) {
                return makeList(heading, hlevel, getter, "");
            }

            /**
             * Make a list of items under a heading.
             *
             * @param heading   for the set of items.
             * @param hlevel    H1, H2, etc. for the heading.
             * @param getter    returns the line items.
             * @param listStyle optional style to apply to the <ul ...></ul>
             * @return the list portion of the report, as a string.
             */
            private String makeList(String heading, int hlevel, GetLines getter, String listStyle) {
                String ul = html ? "<ul " + listStyle + ">\n" : "";
                StringBuilder result = new StringBuilder(h(hlevel, heading));
                result.append(ul);
                for (String detail : getter.getLines()) {
                    result.append(html ? "<li>" : "      ").append(detail).append(
                            html ? "</li>" : "\n");
                }
                result.append(html ? "</ul>" : "");
                return result.toString();
            }

            /**
             * Generate an <h1></h1>, <h2></h2>, etc.
             *
             * @param level The number for the h
             * @param line  The heading itself
             * @return the line wrapped in <hn></hn>
             */
            private String h(int level, String line) {
                String h = html ? "<h" + level + ">" : (level < 2) ? "" : String.format(
                        "%1$" + (level - 1) * 2 + "s", " ");
                String eh = html ? "</h" + level + ">" : "\n";
                return h + line + eh;
            }

        }
    }

    private static final class Params {
        @Option(name = "--verbose", aliases = "-v", usage = "Give verbose output.")
        boolean verbose;

        @Option(name = "--report", usage = "Generate a report to this file. If *.html, format as HTML.")
        File report;

        @Option(name = "--whitelist", aliases = "-w", usage = "File with whitelisted tbcd ids, one per line. If omitted, all tbcd ids are included (except blacklisted).")
        File whiteListFile;

        @Option(name = "--blacklist", aliases = "-b", metaVar = "blacklist", usage = "File with blacklisted tbcd ids, one per line. Takes precedence over whitelist.")
        File blackListFile;

        @Argument(usage = "Directory containing tbcd{tbcd id} directories.", index = 0, metaVar = "source")
        File sourceDir;

        @Argument(usage = "Target 'collected-data-processed' directory", index = 1, metaVar = "target")
        File targetDir;

        @Argument(usage = "Default timestamp, like 2017y06m19d17h21m12s.", index = 2, metaVar = "timestamp")
        String defaultTimeStamp;
    }

    private static void printUsage(CmdLineParser parser) {
        System.err.println(
                "\nUsage: java -cp acm.jar:lib/* org.literacybridge.acm.utils.MoveStats [--verbose] source target timestamp [--report filename]");
        parser.printUsage(System.err);
    }

}
