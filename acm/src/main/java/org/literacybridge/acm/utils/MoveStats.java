package org.literacybridge.acm.utils;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

import org.apache.commons.io.FileExistsException;
import org.apache.commons.io.FileUtils;
import org.literacybridge.acm.tbloader.TBLoader;
import org.literacybridge.core.tbloader.TBLoaderConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Moves stats and user feedback to a directory for processing.
 * The arguments are two paths, an input directory, an output directory, and a timestamp. Any
 * output will be written to the output directory in a subdirectory named with the timestamp. If
 * no stats or user feedback is found, the timestamp directory will not be created.
 *
 * The input is a directory, like ~/Dropbox/stats, containing one or more subdirectories
 * for tbloaders. The tbloader directories then contains a 'collected-data' directory,
 * and those contain directories for projects. The project directories may contain
 * userrecordings and talkingbookdata subdirectories, which are the actual directories
 * of interest.
 *
 * The directories are processed as follows:
 * given a directory with these subdirectories:
 *   {TBLOADER-ID} / collected-data / {PROJECT} / talkingbookdata / --- files ---
 *   {TBLOADER-ID} / collected-data / {PROJECT} / userrecordings / {UPDATE-ID} / --- files ---
 * the files in userrecordings are moved to
 *   {TARGET} / {TIMESTAMP} / userrecordings / {PROJECT} / {UPDATE-ID} / ...
 * and the files in talkingbookdata are moved to a .zip file
 *   {TARGET} / {TIMESTAMP} / {TBLOADER-ID}.zip
 *
 */
public class MoveStats {
  private static final Logger logger = LoggerFactory.getLogger(MoveStats.class);
  private static final String TALKING_BOOK_DATA = "TalkingBookData";
  private static final String USER_RECORDINGS = "userrecordings";

  private File targetCollection;
  private File targetUserRecordingsCollection;

  public static void main(String[] args) throws IOException {
    int rc = new MoveStats().move(args);
    System.exit(rc);
  }

  private int move(String[] args) throws IOException {

    if (args.length != 2) {
      printUsage();
      return 1;
    }
    // Generally, ~/Dropbox/stats or ~Dropbox/outbox/stats
    File sourceDir = new File(args[0]);
    // Generally, ~/Dropbox/collected-data-processed
    File targetDir = new File(args[1]);
    if (!(sourceDir.exists() && targetDir.exists())) {
      printUsage();
      return 1;
    }
    String timeStamp = args[2];
    targetCollection = new File(targetDir, timeStamp);
    targetUserRecordingsCollection = new File(targetCollection, USER_RECORDINGS);

    File[] feedbackDirs = sourceDir.listFiles(new UserRecordingsFilter());
    moveUserRecordings(feedbackDirs);

    File[] statsDirs = sourceDir.listFiles(new TalkingBookDataFilter());
    if (statsDirs.length > 0) {
      moveStats(statsDirs);
      System.out.println(targetCollection.getAbsolutePath());
      return 0;
    } else {
      System.err.println(
              "no directories found in target (other than possibly empty or hidden ones)");
      return 2;
    }

  }

  /**
   * Move userrecordings out of the statistics and into their own subdirectory. Don't bother
   * zipping them because the audio files are compressed, and mostly do not compress at all.
   * @param feedbackDirs An array of tbloader dirs, each of which contains at least one
   *                     userrecordings dir.
   * @throws IOException
   */
  private void moveUserRecordings(File[] feedbackDirs) throws IOException {
    // Iterate over the tbloaders reporting feedback...
    for (File tbLoaderDir : feedbackDirs) {
      logger.info(String.format("Processing user feedback in %s", tbLoaderDir.getAbsolutePath()));

      // Get the contained collected-data subdirectory, and enumerate the project subdirectories.
      File collectedData = IOUtils.FileIgnoreCase(tbLoaderDir, TBLoaderConstants.COLLECTED_DATA_SUBDIR_NAME);
      File[] projectDirs = collectedData.listFiles((fn) -> fn.isDirectory() && !fn.isHidden() && !fn.getName().startsWith("."));
      // Iterate over the projects...
      for (File projectDir : projectDirs) {

        // See if this project has userrecordings.
        File userrecordings = IOUtils.FileIgnoreCase(projectDir, USER_RECORDINGS);
        if (!userrecordings.exists()) continue;

        // Target directory for any {UPDATE} found within this {PROJECT}
        File targetProjDir = new File(targetUserRecordingsCollection, projectDir.getName().toLowerCase());
        logger.info(String.format("  processing %s into %s", projectDir.getName(), targetProjDir.getAbsolutePath()));

        // We have a userrecordings subdirectory, within some project subdirectory.
        // Enumerate the contained {UPDATE} directories.
        File[] updateDirs = userrecordings.listFiles((fn) -> fn.isDirectory() && !fn.isHidden() && !fn.getName().startsWith("."));
        for (File updateDir : updateDirs) {
          logger.info(String.format("    moving %s", updateDir.getName()));

          // Move the {UPDATE} directory to the target
          try {
            FileUtils.moveDirectoryToDirectory(updateDir, targetProjDir, true);
          } catch (FileExistsException e) {
            // Already exists (from a different tbloader's feedback), so copy & delete.
            FileUtils.copyDirectory( updateDir, targetProjDir );
            FileUtils.deleteDirectory( updateDir );
          }
        }
      }
    }
  }

  private void moveStats(File[] statsDirs) throws IOException {
    targetCollection.mkdir();
    // Iterate over the tbloaders reporting stats
    for (File statsDir : statsDirs) {
      // Zip the contents of one tbloader's reported stats directory
      logger.info(String.format("Zipping %s and moving to %s", statsDir, targetCollection.getAbsolutePath()));
      File zipFile = new File(targetCollection, statsDir.getName() + ".zip");
      ZipUnzip.zip(statsDir, zipFile, true /* include base dir */);
      FileUtils.cleanDirectory(statsDir);
    }
  }

  private static void printUsage() {
    System.err.println(
        "java -cp acm.jar:lib/* org.literacybridge.acm.utils.MoveStats source target timestamp");
  }

  /**
   * A FilenameFilter class to accept well-formed collected-data subdirectories
   * of the stats inbox.
   *
   * These well-formed directories look like:
   *  {TB-LOADER-ID} / collected-data / {PROJECT} / talkingbookdata
   * and
   *  {TB-LOADER-ID} / collected-data / {PROJECT} / userrecordings
   * where {TB-LOADER-ID} is like 'tbcd000c' and {PROJECT} is like 'UWR'
   *
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
      if (!subdir.isDirectory()) return false;

      // Only accept non-hidden directories
      if (name.startsWith(".") || subdir.isHidden()) return false;

      // Only accept directories that contain a subdirectory named 'collected-data', like
      // tbcd000c/collected-data
      File collectedData = IOUtils.FileIgnoreCase(subdir, TBLoaderConstants.COLLECTED_DATA_SUBDIR_NAME);
      if (!collectedData.exists() || !collectedData.isDirectory()) {
        logger.warn(String.format(
                "The 'TB-Builder id' directory %s has no expected %s subdirectory.",
                name, TBLoaderConstants.COLLECTED_DATA_SUBDIR_NAME));
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
      // The 'collected-data' subdirectory should contain project (UWR, CARE, ...)
      // subdirectories. There must be at least one such project subdirectory, and ALL
      // of them must contain a 'talkingbookdata' subdirectory:
      // tbcd000c/collected-data/UWR/talkingbookdata
      File[] projects = collectedData.listFiles();
      boolean foundProjectWithTbData = false;  // haven't found anything
      boolean foundProjectWithoutTbData = false;  // yet...
      for (File project : projects) {
        if (!project.isDirectory() || project.isHidden() || project.getName().startsWith(".")) {
          continue;
        }
        File tbdata = IOUtils.FileIgnoreCase(project, TALKING_BOOK_DATA);
        if (!tbdata.exists() || !tbdata.isDirectory()) {
          logger.warn(String.format(
                  "The 'project' directory %s has no expected %s subdirectory.",
                  project.getName(), TALKING_BOOK_DATA));
          foundProjectWithoutTbData = true;
          break;
        } else {
          foundProjectWithTbData = true; // found at least with TBData
        }
      }

      return (foundProjectWithTbData && !foundProjectWithoutTbData);
    }
  }

  static class UserRecordingsFilter extends CollectedDataFilter {

    @Override
    boolean acceptCollectedData(File collectedData) {
      // The 'collected-data' subdirectory should contain project (UWR, CARE, ...)
      // subdirectories. There must be at least one such project subdirectory, and at
      // least one of them must contain a 'userrecordings' subdirectory:
      // tbcd000c/collected-data/UWR/userrecordings
      File[] projects = collectedData.listFiles();
      for (File project : projects) {
        if (!project.isDirectory() || project.isHidden() || project.getName().startsWith(".")) {
          continue;
        }
        File userrecordings = IOUtils.FileIgnoreCase(project, USER_RECORDINGS);
        if (userrecordings.exists() && userrecordings.isDirectory()) {
          return true;
        }
      }

      return false;
    }
  }

}
