package org.literacybridge.acm.utils;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.literacybridge.acm.tbloader.TBLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MoveStats {
  private static final Logger logger = LoggerFactory.getLogger(MoveStats.class);
  private static final String TALKING_BOOK_DATA = "TalkingBookData";

  public static void main(String[] args) throws IOException {

    if (args.length != 2) {
      printUsage();
      System.exit(1);
    }
    File sourceDir = new File(args[0]);
    File targetDir = new File(args[1]);
    if (!(sourceDir.exists() && targetDir.exists())) {
      printUsage();
      System.exit(1);
    }

    File[] subdirs = sourceDir.listFiles(new FilenameFilter() {
      @Override
      public boolean accept(File dir, String name) {
        // Filters the 'sourceDir' for subdirectories with contents like:
        // collected-data/<project>/talkingbookdata
        // <project> is like uwr or CARE, one or more in the collected-data
        // directory
        boolean good = false;
        // Get non-hidden directories only
        if (!name.startsWith(".")) {
          File subdir = new File(dir, name);
          if (subdir.isDirectory()) {
            File collecteddata = FileNC(subdir,
                TBLoader.COLLECTED_DATA_SUBDIR_NAME);
            if (collecteddata.exists()) {
              // Check that every project directory has a talkingbookdata
              // subdirectory.
              File[] projects = collecteddata.listFiles();
              boolean everyProjectHasTBData = false; // must be at least one
                                                     // project with TBData
              for (File project : projects) {
                if (!project.isDirectory() || project.isHidden()
                    || project.getName().startsWith(".")) {
                  continue;
                }
                File tbdata = FileNC(project, TALKING_BOOK_DATA);
                if (!tbdata.exists() || !tbdata.isDirectory()) {
                  logger.warn(String.format(
                      "The 'project' directory %s has no expected %s subdirectory.",
                      project.getName(), TALKING_BOOK_DATA));
                  everyProjectHasTBData = false; // if any does not exist then
                                                 // false and exit
                  break;
                } else {
                  everyProjectHasTBData = true; // found at least with TBData
                }
              }
              if (everyProjectHasTBData) {
                good = true;
              }
            } else {
              logger.warn(String.format(
                  "The 'laptop' directory %s has no expected %s subdirectory.",
                  name, TBLoader.COLLECTED_DATA_SUBDIR_NAME));
            }
          }
        }
        return good;
      }
    });
    if (subdirs.length > 0) {
      String timeStamp = TBLoader.getDateTime();
      File targetCollection = new File(targetDir, timeStamp);
      targetCollection.mkdir();

      for (File subdir : subdirs) {
        logger.info("Zipping " + subdir + " and moving to "
            + targetCollection.getAbsolutePath());
        ZipUnzip.zip(subdir,
            new File(targetCollection, subdir.getName() + ".zip"), true);
        FileUtils.cleanDirectory(subdir);
      }
      System.out.println(targetCollection.getAbsolutePath());
    } else {
      System.out.println(
          "no directories found in target (other than possibly empty or hidden ones)");
    }
  }

  private static void printUsage() {
    System.err.println(
        "java -cp acm.jar:lib/* org.literacybridge.acm.utils.MoveStats source target");
  }

  /**
   * Copied from the lb-core-api project. There should be, obviously, only one,
   * and this entire util-let should probably not be in the ACM project. But,
   * there are dependencies on the TBLoader that also need to be refactored as
   * part of splitting it out.
   * 
   * @param parent
   *          A File representing a directory that may contain a child file or
   *          directory.
   * @param child
   *          The name of a possible child file or directory, but not
   *          necessarily with the correct casing, for instance,
   *          TALKING_BOOK_DATA vs talkingbookdata.
   * @return A File representing the child.
   */
  private static final File FileNC(File parent, final String child) {
    File retval = new File(parent, child);
    // Check for name that matches, ignoring case.
    if (!retval.exists()) {
      File[] candidates = parent.listFiles(new FilenameFilter() {
        boolean found = false;

        @Override
        public boolean accept(File dir, String name) {
          // Accept the first file that matches case insenstively.
          if (!found && name.equalsIgnoreCase(child)) {
            found = true;
            return true;
          }
          return false;
        }
      });
      // If candidates contains a file, we know it exists, so use it.
      if (candidates.length == 1) {
        retval = candidates[0];
      }
    }
    return retval;
  }

}
