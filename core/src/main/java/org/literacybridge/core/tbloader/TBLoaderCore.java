package org.literacybridge.core.tbloader;

import static org.literacybridge.core.tbloader.TBLoaderConstants.COLLECTED_DATA_SUBDIR_NAME;

import static org.literacybridge.core.tbloader.TBLoaderConstants.COLLECTION_SUBDIR;
import static org.literacybridge.core.tbloader.TBLoaderConstants.DEFAULT_GROUP_LABEL;
import static org.literacybridge.core.tbloader.TBLoaderConstants.GROUP_FILE_EXTENSION;

import static org.literacybridge.core.tbloader.TBLoaderConstants.NEED_SERIAL_NUMBER;
import static org.literacybridge.core.tbloader.TBLoaderConstants.NO_SERIAL_NUMBER;
import static org.literacybridge.core.tbloader.TBLoaderConstants.PROJECT_FILE_EXTENSION;
import static org.literacybridge.core.tbloader.TBLoaderConstants.STARTING_SERIALNUMBER;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;

import org.literacybridge.core.OSChecker;
import org.literacybridge.core.ProgressListener;
import org.literacybridge.core.fs.RelativePath;
import org.literacybridge.core.fs.TBFileSystem;
import org.literacybridge.core.fs.ZipUnzip;

// @TODO: Clean up this steaming pile... None of the paths are remotely documented, most are named
// straight out of a obfuscated C contest...

public class TBLoaderCore {
  private static final Logger LOG = Logger.getLogger(TBLoaderCore.class.getName());

  private final TBLoaderConfig tbLoaderConfig;
  private final TBDeviceInfo device;
  private TBInfo tbStats;
  private String lastSynchDir;
  private final RelativePath dropboxCollectedDataPath;
  private final RelativePath copyToPath;
  private final RelativePath pathOperationalData;
  private long startTime;
  private int durationSeconds;

  public TBLoaderCore(TBLoaderConfig tbLoaderConfig, TBDeviceInfo device) {
    this.device = device;
    this.tbLoaderConfig = tbLoaderConfig;

    RelativePath copyTo = tbLoaderConfig.getCollectedDataPath().clone();
    // Like "/Users/mike/Dropbox (Literacy Bridge)/tbcd1234/collected-data"
    copyTo = new RelativePath(copyTo, COLLECTED_DATA_SUBDIR_NAME);
    dropboxCollectedDataPath = copyTo.clone();
    // Like "/Users/mike/Dropbox (Literacy Bridge)/tbcd1234/collected-data/XYZ"
    copyTo = new RelativePath(copyTo, tbLoaderConfig.getProject());
    // Like "/Users/mike/Dropbox (Literacy Bridge)/tbcd1234/collected-data/XYZ/OperationalData/1234"
    pathOperationalData = new RelativePath(copyTo, "OperationalData", tbLoaderConfig.getDeviceID());

    this.copyToPath = copyTo;
    LOG.log(Level.INFO, "copyTo:" + copyTo);
  }

  @Deprecated
  public String getCopyToFolder() {
    return new File(tbLoaderConfig.getDropboxFileSystem().getRootPath(), copyToPath.toString()).getAbsolutePath();
  }

  @Deprecated
  public RelativePath getDropboxCollectedDataPath() {
    return dropboxCollectedDataPath;
  }

  public TBDeviceInfo getDevice() {
    return device;
  }

  public void loadTBStats() throws IOException {
    if (this.tbStats == null) {
      this.tbStats = getStatsFromDrive(device);
    }
  }

  public TBInfo getTBStats() {
    return tbStats;
  }

  public boolean checkConnection(boolean checkHasSystem) throws IOException {
    TBFileSystem fs = device.getFileSystem();
    if (fs == null) {
      return false;
    }

    String[] files = fs.list(RelativePath.EMPTY);
    if (files == null) {
      return false;
    }

    if (!checkHasSystem) {
      return true;
    }

    if (files.length == 0) {
      return false;
    }

    return fs.fileExists(TBLoaderConstants.TB_SYSTEM_PATH)
        && fs.fileExists(TBLoaderConstants.TB_LANGUAGES_PATH)
        && fs.fileExists(TBLoaderConstants.TB_LISTS_PATH)
        && fs.fileExists(TBLoaderConstants.TB_AUDIO_PATH);
  }

  private static TBInfo getStatsFromDrive(TBDeviceInfo device) throws IOException {
    if (device.getFileSystem() == null) {
      return null;
    }

    TBFileSystem fileSystem = device.getFileSystem();
    TBInfo tbInfo;

    if (fileSystem.fileExists(TBLoaderConstants.BINARY_STATS_PATH)) {
      tbInfo = new TBInfo(fileSystem, TBLoaderConstants.BINARY_STATS_PATH);
    } else if (fileSystem.fileExists(TBLoaderConstants.BINARY_STATS_ALTERNATIVE_PATH)) {
      tbInfo = new TBInfo(fileSystem, TBLoaderConstants.BINARY_STATS_ALTERNATIVE_PATH);
    } else {
      throw new IOException();
    }

    if (tbInfo.getCountReflashes() == -1) {
      tbInfo = null;
    }

    return tbInfo;
  }


  /**
   * Looks in the TalkingBook's system directory for any files with a ".srn" extension who's name does not begin
   * with "-erase". If any such files are found, returns the name of the first one found (ie, one selected at random),
   * without the extension.
   *
   * @return The file's name found (minus extension), or "UNKNOWN"
   */
  public static String getSerialNumberFromSystem(TBFileSystem fs) throws IOException {
    String sn = NO_SERIAL_NUMBER;
    String[] files;

    if (fs.fileExists(TBLoaderConstants.TB_SYSTEM_PATH)) {
      files = fs.list(TBLoaderConstants.TB_SYSTEM_PATH, new TBFileSystem.FilenameFilter() {
        @Override
        public boolean accept(TBFileSystem fs, RelativePath dir, String name) {
          String lowercase = name.toLowerCase();
          return lowercase.endsWith(".srn") && !lowercase.startsWith("-erase");
        }
      });
      if (files.length > 0) {
        String tsnFileName = files[0];
        sn = tsnFileName.substring(0, tsnFileName.length() - 4);
      }
      if (sn.equals("")) { // Can only happen if file is named ".srn"
        sn = NO_SERIAL_NUMBER;
      }
      if (!sn.equals(NO_SERIAL_NUMBER)) {
        LOG.log(Level.INFO, "No stats SRN. Found *.srn file:" + sn);
      } else {
        LOG.log(Level.INFO, "No stats SRN and no good *.srn file found.");
      }
    }

    return sn;
  }

  /**
   * Tests whether a string is a valid "serial number" for this TB-Loader (sensitive to whether the TB-Loader
   * is being run in "old TB mode" or "new TB mode").
   *
   * @param srn - the string to check
   * @return TRUE if the string could be a serial number, FALSE if not.
   */
  public boolean isSerialNumberFormatGood(String srn) {
    boolean isGood;
    if (srn == null)
      isGood = false;
    else if (srn.toLowerCase().startsWith(tbLoaderConfig.getSrnPrefix().toLowerCase())
        && srn.length() == 10)
      isGood = true;
    else {
      isGood = false;
      LOG.log(Level.INFO, "***Incorrect Serial Number Format:" + srn + "***");
    }
    return isGood;
  }

  /**
   * Tests whether a string is a valid new-style "serial number".
   *
   * @param srn
   * @return
   */
  public boolean isSerialNumberFormatGood2(String srn) {
    boolean isGood = false;
    try {
      if (srn != null && srn.length() == 10 && srn.substring(1, 2).equals("-")
          && (srn.substring(0, 1).equalsIgnoreCase("A") || srn.substring(0, 1)
          .equalsIgnoreCase("B"))) {
        int highBytes = Integer.parseInt(srn.substring(2, 6), 0x10);
        // TODO: What was this trying to do? It appears to try to guess that if the "device number" part of a srn is
        // in the range 0-N, then the SRN must be > X, where N was "0x10" and X was "0x200" (it was the case that if
        // a number was assigned, the device number would have been smaller than 0x10 for most devices, and the
        // serial numbers were arbitrarily started at 0x200 for each device). I (Bill) *think* that this just
        // missed getting updated as the device number(s) increased; an ordinary bug. Does show the value of
        // burned-in serial numbers.
        //
        // The number below needs to be greater than the highest assigned "device" (TB Laptop). As of 23-May-16,
        // that highest assigned device number is 0x14. Opening up the range eases maintenance, but lets more
        // corrupted srns get through. 0x2f is somewhere in the middle.
        if (highBytes < 0x2f) {
          int lowBytes = Integer.parseInt(srn.substring(6), 0x10);
          if (lowBytes >= STARTING_SERIALNUMBER) {
            isGood = true;
          }
        }
      }
    } catch (NumberFormatException e) {
      isGood = false;
    }
    return isGood;
  }

  /**
   * Populates the values in the right-hand side, the "previous deployment" side of the main screen.
   *
   * @throws Exception
   */
  public DeploymentInfo loadDeploymentInfoFromDevice() throws IOException {
    String sn = NO_SERIAL_NUMBER; // "UNKNOWN"
    DeploymentInfo deploymentInfo = null;

    if (device.getFileSystem() == null) {
      return null;
    }
    RelativePath systemPath = TBLoaderConstants.TB_SYSTEM_PATH;

    try {
      String community;
      if (getTBStats() != null && getTBStats().getLocation() != null && !getTBStats().getLocation().equals(
          "")) {
        community = getTBStats().getLocation();
      } else {
        community = getCommunityFromDevice();
      }

      sn = getSerialNumberFromDevice();
      String rev = getFirmwareRevisionFromSystem(systemPath);

      // Previous image or package. Displays as "Content"
      // TODO: package vs image - consistency in nomenclature.
      String pkg = getPkgNameFromSystem(systemPath);

      // Previous deployment name. Displays as "Update"
      String depl = getDeploymentNameFromSystem(systemPath);

      // Last updated date. Displays as "First Rotation Date".
      lastSynchDir = readLastSynchDirFromSystem(systemPath);
      String lastUpdate = getLastUpdateDate(lastSynchDir);

      // This doesn't display anywhere.
      String oldProject = getProjectFromSystem(systemPath);

      deploymentInfo = new DeploymentInfo(sn, oldProject, pkg, depl, lastUpdate, rev, community);

      sn = sn.toUpperCase();
      if (!isSerialNumberFormatGood2(sn)) {
        // We will allocate a new-style serial number before we update the device.
        sn = NEED_SERIAL_NUMBER;
      }
      device.setSerialNumber(sn);
    } catch (Exception ignore) {
      LOG.log(Level.WARNING, "exception - ignore and keep going with empty strings", ignore);
    }

    return deploymentInfo;
  }

  /**
   * Looks in the TalkingBook's system directory for any files with a ".srn" extension who's name does not begin
   * with "-erase". If any such files are found, returns the name of the first one found (ie, one selected at random),
   * without the extension.
   *
   * @return The file's name found (minus extension), or null if no file found.
   */
  public String getSerialNumberFromDevice() throws IOException {
    String sn = NO_SERIAL_NUMBER;

    if (getTBStats() != null && isSerialNumberFormatGood(getTBStats().getSerialNumber())
        && isSerialNumberFormatGood2(getTBStats().getSerialNumber())) {
      sn = getTBStats().getSerialNumber();
    } else {
      sn = getSerialNumberFromSystem(device.getFileSystem());
    }

    if (!isSerialNumberFormatGood(sn)) {
      if (sn.substring(1, 2).equals("-")) {
        // TODO: This code probably only works in "the TB Loader for new TBs", because "a-0".compareTo("a-") is NOT < 0, but .compareTo("b-") is.
        if (sn.compareToIgnoreCase(tbLoaderConfig.getSrnPrefix()) < 0)
          JOptionPane.showMessageDialog(null,
              "This appears to be an OLD TB.  If so, please close this program and open the TB Loader for old TBs.",
              "OLD TB!", JOptionPane.WARNING_MESSAGE);
        else if (sn.compareToIgnoreCase(tbLoaderConfig.getSrnPrefix()) > 0)
          JOptionPane.showMessageDialog(null,
              "This appears to be a NEW TB.  If so, please close this program and open the TB Loader for new TBs.",
              "NEW TB!", JOptionPane.WARNING_MESSAGE);
      }
    }

    return sn;
  }

  /**
   * Look in the TalkingBook's system directory for any files with a ".prj" extension. If any such
   * files are found, return the name of the first one found (ie, selected at random), without the
   * extension.
   *
   * If no .prj file is found, use the value of the new project. Since Talking Books
   * don't move between projects very often, this is usually correct. Not always, though.
   *
   * @param systemPath A file representing the TalkingBook's system directory.
   * @return The file's name found (minus extension), or the value of newProject if none is
   * found.
   */
  public String getProjectFromSystem(RelativePath systemPath) throws IOException {
    // If no value found, just assume the new project name.
    String project = tbLoaderConfig.getProject();
    String [] prjFiles;
    TBFileSystem fs = device.getFileSystem();
    if (fs.fileExists(systemPath)) {
      prjFiles = fs.list(systemPath, new TBFileSystem.FilenameFilter() {
        @Override
        public boolean accept(TBFileSystem unused, RelativePath dir, String name) {
          return name.toLowerCase().endsWith(PROJECT_FILE_EXTENSION);
        }
      });
      // Pick one at random, get the name, drop the extension.
      if (prjFiles.length > 0) {
        String fn = prjFiles[0];
        project = fn.substring(0, fn.length()-PROJECT_FILE_EXTENSION.length());
      }
    }
    return project;
  }

  /**
   * Looks in the TalkingBook's system directory for any files with a ".rev" or ".img" extension. If any such files
   * are found, returns the name of the first one found (ie, one selected at random), without the extension.
   *
   * @param systemPath The TalkingBook's system directory.
   * @return The file's name found (minus extension), or "UNKNOWN" if no file found, or if the file name consists
   * only of the extension (eg, a file named ".img" will return "UNKNOWN").
   */
  public String getFirmwareRevisionFromSystem(RelativePath systemPath) throws IOException {
    String rev = "UNKNOWN";
    String[] files;

    TBFileSystem fs = device.getFileSystem();
    if (fs.fileExists(systemPath)) {
      // get firmware revision number from .rev or .img file
      files = fs.list(systemPath, new TBFileSystem.FilenameFilter() {
        @Override
        public boolean accept(TBFileSystem fs, RelativePath dir, String name) {
          String lowercase = name.toLowerCase();
          return lowercase.endsWith(".img") || lowercase.endsWith(".rev");
        }
      });

      for (int i = 0; i < files.length; i++) {
        String revFileName = files[i];
        revFileName = revFileName.substring(0, revFileName.length() - 4);
        if (i == 0)
          rev = revFileName;
      }
      if (rev.length() == 0)
        rev = "UNKNOWN";  // eliminate problem of zero length filenames being inserted into batch statements
    }

    return rev;
  }


  /**
   * Checks in tbstats structure for image name. If not found, will then
   * look in the TalkingBook's system directory for a file with a ".pkg" extension. If there is exactly
   * one such file, returns the file's name, sans extension.
   *
   * @param systemPath The TalkingBook's system directory.
   * @return The file's name found (minus extension), or "UNKNOWN" if no file found.
   */
  public String getPkgNameFromSystem(RelativePath systemPath) throws IOException {
    String pkg = "UNKNOWN";
    String[] files;

    TBFileSystem fs = device.getFileSystem();

    if (getTBStats() != null && getTBStats().getImageName() != null
        && !getTBStats().getImageName().equals("")) {
      pkg = getTBStats().getImageName();
    } else if (fs.fileExists(systemPath)) {
      // get package name from .pkg file
      files = fs.list(systemPath, new TBFileSystem.FilenameFilter() {
        @Override
        public boolean accept(TBFileSystem fs, RelativePath dir, String name) {
          String lowercase = name.toLowerCase();
          return lowercase.endsWith(".pkg");
        }
      });
      if (files.length == 1) {
        pkg = files[0].substring(0, files[0].length() - 4);
      }
    }

    return pkg;
  }

  /**
   * Look in the tbstats structure for a deployment name (called "deploymentNumber", but really a string). If none,
   * look in the TalkingBook's system directory for a file with a ".dep" extension. If there is exactly
   * one such file, returns the file's name, sans extension.
   *
   * @param systemPath The TalkingBook's system directory.
   * @return The file's name found (minus extension), or "UNKNOWN" if no file found.
   */
  public String getDeploymentNameFromSystem(RelativePath systemPath) throws IOException {
    String depl = "UNKNOWN";
    String[] files;

    TBFileSystem fs = device.getFileSystem();

    if (getTBStats() != null && getTBStats().getDeploymentNumber() != null
        && !getTBStats().getDeploymentNumber().equals("")) {
      depl = getTBStats().getDeploymentNumber();
    } else if (fs.fileExists(systemPath)) {
      // get deployment name from .dep file
      files = fs.list(systemPath, new TBFileSystem.FilenameFilter() {
        @Override
        public boolean accept(TBFileSystem fs, RelativePath dir, String name) {
          String lowercase = name.toLowerCase();
          return lowercase.endsWith(".dep");
        }
      });
      if (files.length == 1) {
        depl = files[0].substring(0, files[0].length() - 4);
      }
    }

    return depl;
  }

  /**
   * Look in the tbstats structure for updateDate. If none, parse the lastSynchDir, if there is one.
   *
   * @return The last sync date, or "UNKNOWN" if not available.
   */
  public String getLastUpdateDate(String lastSynchDir) {
    String lastUpdate = "UNKNOWN";

    if (getTBStats() != null && getTBStats().getUpdateDate() != -1)
      lastUpdate = getTBStats().getUpdateYear() + "/" + getTBStats().getUpdateMonth() + "/"
          + getTBStats().getUpdateDate();
    else {
      String strLine = lastSynchDir; // 1111y11m11d
      if (strLine != null) {
        int y = strLine.indexOf('y');
        int m = strLine.indexOf('m');
        int d = strLine.indexOf('d');
        lastUpdate =
            strLine.substring(0, y) + "/" + strLine.substring(y + 1, m) + "/"
                + strLine.substring(m + 1, d);
      }
    }

    return lastUpdate;
  }

  /**
   * Looks in the TalkingBook's system directory for a file named "last_updated.txt",
   * and reads the first line from it. Stores any value so read into TBLoader.lastSynchDir.
   *
   * @param systemPath The TalkingBook's system directory.
   */
  public String readLastSynchDirFromSystem(RelativePath systemPath) throws IOException {
    String[] files;
    TBFileSystem fs = device.getFileSystem();
    String lastSynchDir = null;

    files = fs.list(systemPath, new TBFileSystem.FilenameFilter() {
      @Override
      public boolean accept(TBFileSystem fs, RelativePath dir, String name) {
        String lowercase = name.toLowerCase();
        return lowercase.equals("last_updated.txt");
      }
    });
    if (files.length == 1) {
      try (FileInputStream fstream = new FileInputStream(files[0]);
          DataInputStream in = new DataInputStream(fstream);
          BufferedReader br = new BufferedReader(new InputStreamReader(in))
      ) {
        String strLine;
        if ((strLine = br.readLine()) != null) {
          lastSynchDir = strLine;
        }
        in.close();
      } catch (Exception e) { //Catch and ignore exception if any
        System.err.println("Ignoring error: " + e.getMessage());
      }
    }

    return lastSynchDir;
  }

  public String getCommunityFromDevice() {
    String communityName = "UNKNOWN";
    if (device.getFileSystem() == null) {
      return communityName;
    }
    TBFileSystem fs = device.getFileSystem();
    RelativePath systemPath = RelativePath.parse("system");
    try {
      String[] files;
      // get Location file info
      // check root first, in case device was just assigned a new community (e.g. from this app)
      files = fs.list(RelativePath.EMPTY, new TBFileSystem.FilenameFilter() {
        @Override
        public boolean accept(TBFileSystem fs, RelativePath dir, String name) {
          String lowercase = name.toLowerCase();
          return lowercase.endsWith(".loc");
        }
      });
      if (files == null) {
        LOG.log(Level.INFO, "This does not look like a TB: " + fs.getRootPath());

      } else if (files.length == 1) {
        String locFileName = files[0];
        communityName = locFileName.substring(0, locFileName.length() - 4);
      } else if (files.length == 0 && fs.fileExists(systemPath)) {
        // get Location file info
        files = fs.list(systemPath, new TBFileSystem.FilenameFilter() {
          @Override
          public boolean accept(TBFileSystem fs, RelativePath dir, String name) {
            String lowercase = name.toLowerCase();
            return lowercase.endsWith(".loc");
          }
        });
        if (files.length == 1) {
          String locFileName = files[0];
          communityName = locFileName.substring(0, locFileName.length() - 4);
        }
      }
    } catch (Exception ignore) {
      LOG.log(Level.WARNING, "Exception while reading community", ignore);
      // ignore and keep going with empty string
    }
    LOG.log(Level.INFO, "TB's current community name is " + communityName);
    return communityName;
  }

  public static String getFirmwareRevisionNumbers(TBFileSystem fs) {
    String revision = "(No firmware)";

    try {
      // get Package
      String[] files = fs.list(TBLoaderConstants.CONTENT_BASIC_SUBDIR, new TBFileSystem.FilenameFilter() {
        @Override
        public boolean accept(TBFileSystem fs, RelativePath dir, String name) {
          String lowercase = name.toLowerCase();
          return lowercase.endsWith(".img");
        }
      });
      if (files.length > 1) {
        revision = "(Multiple Firmwares!)";
      } else if (files.length == 1) {
        revision = files[0].substring(0, files[0].length() - 4);
      }
    } catch (Exception ignore) {
      LOG.log(Level.WARNING, "exception - ignore and keep going with default string", ignore);
    }
    return revision;
  }

  public void logTBData(String action,
      DeploymentInfo oldDeployment,
      DeploymentInfo newDeployment,
      String location,
      int durationSeconds,
      TBFileSystem fs) {
    final String VERSION_TBDATA = "v03";
    BufferedWriter bw;
    // Like "/Users/mike/Dropbox (Literacy Bridge)/tbcd1234/collected-data/XYZ/OperationalData/1234/tbData"
    RelativePath tbDataPath = new RelativePath(pathOperationalData, "tbData");
    SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy'y'MM'm'dd'd'");
    String strDate = sdfDate.format(new Date());
    // Like "/Users/mike/Dropbox (Literacy Bridge)/tbcd1234/collected-data/XYZ/OperationalData/1234/tbData/tbData-v03-2020y12m25d-1234.csv"
    RelativePath filePath = new RelativePath(tbDataPath, "tbData-" + VERSION_TBDATA + "-" + strDate + "-"
        + tbLoaderConfig.getDeviceID() + ".csv");

    try {
      fs.mkdirs(tbDataPath);
      boolean isNewFile = !fs.fileExists(filePath);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      bw = new BufferedWriter(new OutputStreamWriter(baos));
      if (bw != null) {
        if (isNewFile) {
          bw.write(
              "PROJECT,UPDATE_DATE_TIME,OUT_SYNCH_DIR,LOCATION,ACTION,DURATION_SEC,");
          bw.write(
              "OUT-SN,OUT-DEPLOYMENT,OUT-IMAGE,OUT-FW-REV,OUT-COMMUNITY,OUT-ROTATION-DATE,");
          bw.write(
              "IN-SN,IN-DEPLOYMENT,IN-IMAGE,IN-FW-REV,IN-COMMUNITY,IN-LAST-UPDATED,IN-SYNCH-DIR,IN-DISK-LABEL,CHKDSK CORRUPTION?,");
          bw.write("FLASH-SN,FLASH-REFLASHES,");
          bw.write(
              "FLASH-DEPLOYMENT,FLASH-IMAGE,FLASH-COMMUNITY,FLASH-LAST-UPDATED,FLASH-CUM-DAYS,FLASH-CORRUPTION-DAY,FLASH-VOLT,FLASH-POWERUPS,FLASH-PERIODS,FLASH-ROTATIONS,");
          bw.write(
              "FLASH-MSGS,FLASH-MINUTES,FLASH-STARTS,FLASH-PARTIAL,FLASH-HALF,FLASH-MOST,FLASH-ALL,FLASH-APPLIED,FLASH-USELESS");
          for (int i = 0; i < 5; i++) {
            bw.write(
                ",FLASH-ROTATION,FLASH-MINUTES-R" + i + ",FLASH-PERIOD-R" + i
                    + ",FLASH-HRS-POST-UPDATE-R" + i + ",FLASH-VOLT-R" + i);
          }
          bw.write("\n");
        }
        bw.write(tbLoaderConfig.getProject().toUpperCase() + ",");
        bw.write(device.getDateTime().toUpperCase() + ",");
        bw.write(device.getDateTime().toUpperCase() + "-" + tbLoaderConfig.getDeviceID().toUpperCase() + ",");
        bw.write(location.toUpperCase() + ",");
        bw.write(action + ",");
        bw.write(Integer.toString(durationSeconds) + ",");
        bw.write(device.getSerialNumber().toUpperCase() + ",");
        bw.write(newDeployment.getDeploymentName().toUpperCase() + ",");
        bw.write(newDeployment.getPackageName().toUpperCase() + ",");
        bw.write(newDeployment.getFirmwareRevision() + ",");
        bw.write(newDeployment.getCommunity().toUpperCase() + ",");
        bw.write(newDeployment.getLastUpdatedText() + ",");
        bw.write(oldDeployment.getSerialNumber().toUpperCase() + ",");
        bw.write(oldDeployment.getDeploymentName().toUpperCase() + ",");
        bw.write(oldDeployment.getPackageName().toUpperCase() + ",");
        bw.write(oldDeployment.getFirmwareRevision() + ",");
        bw.write(oldDeployment.getCommunity().toUpperCase() + ",");
        bw.write(oldDeployment.getLastUpdatedText() + ",");
        bw.write((lastSynchDir != null ? lastSynchDir.toUpperCase() : "") + ",");
        bw.write(device.getLabel() + ",");
        bw.write(device.isCorrupted() + ",");

        if (tbStats != null) {
          bw.write(tbStats.getSerialNumber().toUpperCase() + ",");
          bw.write(tbStats.getCountReflashes() + ",");
          bw.write(tbStats.getDeploymentNumber().toUpperCase() + ",");
          bw.write(tbStats.getImageName().toUpperCase() + ",");
          bw.write(tbStats.getLocation().toUpperCase() + ",");
          bw.write(tbStats.getUpdateYear() + "/" + tbStats.getUpdateMonth() + "/"
              + tbStats.getUpdateDate() + ",");
          bw.write(tbStats.getCumulativeDays() + ",");
          bw.write(tbStats.getCorruptionDay() + ",");
          bw.write(tbStats.getLastInitVoltage() + ",");
          bw.write(tbStats.getPowerups() + ",");
          bw.write(tbStats.getPeriods() + ",");
          bw.write(tbStats.getProfileTotalRotations() + ",");
          bw.write(tbStats.getTotalMessages() + ",");
          int totalSecondsPlayed = 0, countStarted = 0, countQuarter = 0, countHalf = 0, countThreequarters = 0, countCompleted = 0, countApplied = 0, countUseless = 0;
          for (int m = 0; m < tbStats.getTotalMessages(); m++) {
            for (int r = 0; r < (tbStats.getProfileTotalRotations() < 5 ?
                tbStats.getProfileTotalRotations() :
                5); r++) {
              totalSecondsPlayed += tbStats.getStats()[m][r].getTotalSecondsPlayed();
              countStarted += tbStats.getStats()[m][r].getCountStarted();
              countQuarter += tbStats.getStats()[m][r].getCountQuarter();
              countHalf += tbStats.getStats()[m][r].getCountHalf();
              countThreequarters += tbStats.getStats()[m][r].getCountThreequarters();
              countCompleted += tbStats.getStats()[m][r].getCountCompleted();
              countApplied += tbStats.getStats()[m][r].getCountApplied();
              countUseless += tbStats.getStats()[m][r].getCountUseless();
            }
          }
          bw.write(totalSecondsPlayed / 60 + ",");
          bw.write(countStarted + ",");
          bw.write(countQuarter + ",");
          bw.write(countHalf + ",");
          bw.write(countThreequarters + ",");
          bw.write(countCompleted + ",");
          bw.write(countApplied + ",");
          bw.write(String.valueOf(countUseless));
          for (int r = 0; r < (tbStats.getProfileTotalRotations() < 5 ?
              tbStats.getProfileTotalRotations() :
              5); r++) {
            bw.write(
                "," + r + "," + tbStats.totalPlayedSecondsPerRotation(r) / 60
                    + "," + tbStats.getRotations()[r].getStartingPeriod() + ",");
            bw.write(tbStats.getRotations()[r].getHoursAfterLastUpdate() + ","
                + tbStats.getRotations()[r].getInitVoltage());
          }
        }
        bw.write("\n");
        bw.flush();
        bw.close();

        InputStream content = new ByteArrayInputStream(baos.toByteArray());
        fs.createNewFile(filePath, content, true);
        content.close();
        baos.close();
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public class Result {
    public final boolean success;
    public final boolean corrupted;
    public final boolean reformatFailed;
    public final boolean verified;
    public final String duration;

    private Result(boolean success, boolean corrupted, boolean reformatFailed, boolean verified) {
      this.success = success;
      this.corrupted = corrupted;
      this.reformatFailed = reformatFailed;
      this.verified = verified;
      this.duration = getDuration();
    }
  }

  public Result update(DeploymentInfo oldDeploymentInfo,
      DeploymentInfo newDeploymentInfo, String location,
      boolean grabStatsOnly, boolean forceFirmware,
      TBFileSystem sourceImage,
      ProgressListener progressListener) {
    return doUpdate(oldDeploymentInfo, newDeploymentInfo, location,
        grabStatsOnly, forceFirmware, sourceImage, wrap(progressListener));
  }

  public Result doUpdate(DeploymentInfo oldDeploymentInfo,
      DeploymentInfo newDeploymentInfo, String location,
      boolean grabStatsOnly, boolean forceFirmware, TBFileSystem sourceImage,
      ProgressListener progressListener) {

    System.out.println("oldDeploymentInfo:\n" + oldDeploymentInfo.toString());
    System.out.println("newDeploymentInfo:\n" + newDeploymentInfo.toString());
    System.out.println("location: " + location);

    progressListener.updateProgress(0, "Initializing Update ...");

    final RelativePath syncdirPath = new RelativePath(
        TBLoaderConstants.TB_DATA_PATH,
        oldDeploymentInfo.getDeploymentName(),
        tbLoaderConfig.getDeviceID(),
        oldDeploymentInfo.getCommunity(),
        getDevice().getSerialNumber(),
        getDevice().getSyncDir());

    boolean hasCorruption = false;
    boolean goodCard = false;
    boolean success = false;
    try {
      tbLoaderConfig.getDropboxFileSystem().mkdirs(copyToPath); // ensures COLLECTION_SUBDIR exists
      setStartTime();
      goodCard = checkConnection(true);
      if (!goodCard) {
        return new Result(false, false, false, false);
      }
      if (OSChecker.WINDOWS) {
        progressListener.updateProgress(5, "Checking Memory Card");
        hasCorruption = CommandLineUtils.checkDisk(device.getFileSystem().getRootPath());
        if (hasCorruption) {
          device.setCorrupted(true);
          progressListener.addDetail("...Corrupted\nGetting Stats");
          CommandLineUtils.checkDisk(device.getFileSystem().getRootPath(),
              new RelativePath(syncdirPath, "chkdsk-reformat.txt").asString());
        } else {
          progressListener.addDetail("...Good\nGetting Stats");
        }
      } else {
        progressListener.addDetail("Skipping checkDisk; not supported on this OS.");
      }


      // ***************   grab.txt   ***************
      // mkdir "${syncpath}"
      // at the end, this gets zipped up into the copyTo (Dropbox dir)
      tbLoaderConfig.getTempFileSystem().mkdirs(syncdirPath);

      progressListener.addDetail("\nCreating syncdirPath: " + syncdirPath);

      // rem Capturing Full Directory
      // dir ${device_drive} /s > "${syncpath}\dir.txt"
      String[] deviceFiles = device.getFileSystem().list(RelativePath.EMPTY);
      StringBuilder builder = new StringBuilder();
      for (String fn : deviceFiles) {
        builder.append(fn).append("\n");
      }
      RelativePath dirTxt = new RelativePath(syncdirPath, "dir.txt");
      byte[] content = builder.toString().getBytes();
      tbLoaderConfig.getTempFileSystem().createNewFile(dirTxt, new ByteArrayInputStream(content), true);
      System.out.println(new File(tbLoaderConfig.getTempFileSystem().getRootPath(), dirTxt.asString()));

      progressListener.updateProgress(10, "\nCollecting Usage Data ...");
      long t1 = System.currentTimeMillis();
      // rem Collecting Usage Data
      // xcopy ${device_drive} "${syncpath}" /E /Y /EXCLUDE:software\scripts\exclude.txt /C
      //    /e : Copies all subdirectories, even if they are empty. Use /e with the /s and /t command-line options.
      //    /y : Suppresses prompting to confirm that you want to overwrite an existing destination file.
      //    /c : Ignores errors.
      TBFileSystem.copyDir(device.getFileSystem(), RelativePath.EMPTY,
          tbLoaderConfig.getTempFileSystem(), syncdirPath,
          TBLoaderConstants.XCOPY_EXCLUDE_FILTER, TBLoaderConstants.XCOPY_EXCLUDE_FILTER);
      progressListener.addDetail(" " + (System.currentTimeMillis() - t1) + " ms.");

      t1 = System.currentTimeMillis();
      progressListener.updateProgress(20, "\nCollecting User Recordings ...");

      // rem Collecting User Recordings
      // mkdir "${recording_path}"
      // xcopy "${device_drive}\messages\audio\*_9_*.a18" "${recording_path}" /C
      // xcopy "${device_drive}\messages\audio\*_9-0_*.a18" "${recording_path}" /C
      RelativePath recordingPath = new RelativePath(
          copyToPath,
          TBLoaderConstants.USER_RECORDINGS_PATH,
          oldDeploymentInfo.getDeploymentName(),
          tbLoaderConfig.getDeviceID(),
          oldDeploymentInfo.getCommunity());
      tbLoaderConfig.getDropboxFileSystem().mkdirs(recordingPath);

      TBFileSystem.FilenameFilter xCopyFilter = new TBFileSystem.FilenameFilter() {
        @Override
        public boolean accept(TBFileSystem fs, RelativePath dir, String name) {
          return name.endsWith(".a18")
              && (name.contains("_9_") || name.contains("_9-0_"));
        }
      };

      TBFileSystem.copyDir(device.getFileSystem(), TBLoaderConstants.TB_AUDIO_PATH,
          tbLoaderConfig.getDropboxFileSystem(), recordingPath, xCopyFilter, xCopyFilter);


      // copy "${device_drive}\languages\dga\10.a18" "${syncpath}" /Y
      RelativePath dga10File = RelativePath.parse("dga/10.a18");
      tbLoaderConfig.getTempFileSystem().mkdirs(RelativePath.concat(syncdirPath, dga10File).getParent());
      if (device.getFileSystem().fileExists(RelativePath.concat(TBLoaderConstants.TB_LANGUAGES_PATH, dga10File))) {
        TBFileSystem.copy(device.getFileSystem(), RelativePath.concat(TBLoaderConstants.TB_LANGUAGES_PATH, dga10File),
                tbLoaderConfig.getTempFileSystem(), RelativePath.concat(syncdirPath, dga10File));
      }

      progressListener.addDetail(" " + (System.currentTimeMillis() - t1) + " ms.");
      t1 = System.currentTimeMillis();
      progressListener.updateProgress(30, "\nDeleting Usage Statistics ...");


      // rem Deleting Usage Statistics
      // del ${device_drive}\log\*.* /Q
      // del ${device_drive}\log-archive\*.* /S /Q
      // del ${device_drive}\statistics\*.* /S /Q
      // del ${device_drive}\statistics\stats\*.* /S /Q
      //      /s : Deletes specified files from the current directory and all subdirectories. Displays the names of the files as they are being deleted.
      //      /q : Specifies quiet mode. You are not prompted for delete confirmation.
      deleteDirectoryContents(device.getFileSystem(), RelativePath.parse("log"), false);
      deleteDirectoryContents(device.getFileSystem(), RelativePath.parse("log-archive"), false);
      deleteDirectoryContents(device.getFileSystem(), RelativePath.parse("statistics"), false);
      deleteDirectoryContents(device.getFileSystem(), RelativePath.parse("statistics/stats"), false);


      progressListener.addDetail(" " + (System.currentTimeMillis() - t1) + " ms.");
      t1 = System.currentTimeMillis();
      progressListener.updateProgress(40, "\nDeleting User Recordings ...");

      // rem Deleting User Recordings
      // del ${device_drive}\messages\audio\*_9_*.a18 /Q
      // del ${device_drive}\messages\audio\*_9-0_*.a18 /Q
      for (String name : device.getFileSystem().list(
          TBLoaderConstants.TB_AUDIO_PATH, new TBFileSystem.FilenameFilter() {
            @Override
            public boolean accept(TBFileSystem fs, RelativePath dir, String name) {
              return name.endsWith(".a18")
                  && (name.contains("_9_") || name.contains("_9-0_"));
            }
          })) {
        device.getFileSystem().deleteFile(
            new RelativePath(TBLoaderConstants.TB_AUDIO_PATH, name));
      }

      progressListener.addDetail(" " + (System.currentTimeMillis() - t1) + " ms.");
      t1 = System.currentTimeMillis();
      progressListener.addDetail("\nDeleting User Feedback Category...");


      // rem Deleting User Feedback Category
      // for /f %f in ('dir ${device_drive}\messages\lists /b /AD') do del ${device_drive}\messages\lists\%f\9*.txt /Q
      for (String list : device.getFileSystem().list(TBLoaderConstants.TB_LISTS_PATH)) {
        RelativePath listPath = new RelativePath(TBLoaderConstants.TB_LISTS_PATH, list);
        String[] toDelete = device.getFileSystem().list(listPath, new TBFileSystem.FilenameFilter() {
          @Override
          public boolean accept(TBFileSystem fs, RelativePath dir, String name) {
            return name.startsWith("9") && name.endsWith(".txt");
          }
        });
        if (toDelete != null) {
          for (String f : toDelete) {
            device.getFileSystem().deleteFile(new RelativePath(listPath, f));
          }
        }
      }

      progressListener.addDetail(" " + (System.currentTimeMillis() - t1) + " ms.");
      t1 = System.currentTimeMillis();
      progressListener.addDetail("\nLog TB data...");

      logTBData("stats-only", oldDeploymentInfo, newDeploymentInfo,
          location, durationSeconds, tbLoaderConfig.getDropboxFileSystem());
      progressListener.addDetail(" " + (System.currentTimeMillis() - t1) + " ms.");
      success = true;
    } catch (Exception e) {
      e.printStackTrace();
      LOG.log(Level.WARNING, "Unable to get stats:", e);
      progressListener.addDetail(getStackTrace(e));
    }

    boolean verified = false;

    try {
      if (success) {
        if (!OSChecker.MAC_OS) {
          progressListener.addDetail("...Got Stats\nErasing Flash Stats");
          // echo .>${device_drive}\sysdata.txt
          eraseAndOverwriteFile(device.getFileSystem(), TBLoaderConstants.SYS_DATA_TXT, ".");
          progressListener.addDetail("...Erased Flash Stats");
        }

        if (grabStatsOnly && OSChecker.WINDOWS) {
          progressListener.addDetail("Disconnecting");
          LOG.log(Level.INFO, "STATUS:Disconnecting TB");
          CommandLineUtils.disconnectDrive(device.getFileSystem().getRootPath());
          progressListener.addDetail("...Complete");
        }
        success = true;
      } else {
        progressListener.addDetail("...No Stats!\n");
      }
      // zip up stats
      RelativePath targetPath = RelativePath.parse(syncdirPath.asString() + ".zip");
      RelativePath targetFullPath = RelativePath.concat(copyToPath, targetPath);

      progressListener.updateProgress(50, "Copying Usage Statistics ...");
      // TODO(michael): implement zip/unzip for RelativePath
      File sourcePathFile = new File(tbLoaderConfig.getTempFileSystem().getRootPath(), syncdirPath.asString());
      File targetTempFile = new File(tbLoaderConfig.getTempFileSystem().getRootPath(), targetPath.asString());
      ZipUnzip.zip(sourcePathFile, targetTempFile, true);
      TBFileSystem.copy(tbLoaderConfig.getTempFileSystem(), targetPath,
          tbLoaderConfig.getDropboxFileSystem(), targetFullPath);
      System.out.println("Copying to: " + new File(tbLoaderConfig.getDropboxFileSystem().getRootPath(), targetFullPath.asString()));

      TBFileSystem.deleteRecursive(tbLoaderConfig.getTempFileSystem(), syncdirPath);

      if (!grabStatsOnly) {
        if (hasCorruption) {
          progressListener.addDetail("Reformatting");
          LOG.log(Level.INFO, "STATUS:Reformatting");
          if (!OSChecker.WINDOWS) {
            return new Result(false, true, false, false);
          }
          goodCard = CommandLineUtils.formatDisk(device.getFileSystem().getRootPath(), device.getSerialNumber().toUpperCase());
          if (!goodCard) {
            progressListener.addDetail("...Failed\n");
            return new Result(false, true, true, false);
          } else {
            progressListener.addDetail("...Good\n");
            LOG.log(Level.INFO, "STATUS:Format was good");
          }
        } else {
          if (!newDeploymentInfo.getSerialNumber()
              .equalsIgnoreCase(
                  device.getLabelWithoutDriveLetter())) {
            if (!OSChecker.WINDOWS) {
              progressListener.addDetail("Skipping relabeling; not supported on this OS.");
            } else {
              LOG.log(Level.INFO, "STATUS:Relabeling volume");
              progressListener.addDetail("Relabeling\n");
              CommandLineUtils.relabel(device.getFileSystem().getRootPath(), newDeploymentInfo.getSerialNumber());
            }
          }
        }
        progressListener.updateProgress(60, "Updating Talking Book Files ...");
        LOG.log(Level.INFO, "STATUS:Updating TB Files");

        // ***************   update.txt   ***************
        // rem UPDATE
        // rem Copying Basic Files to Talking Book
        // rmdir ${device_drive}\archive
        if (device.getFileSystem().fileExists(RelativePath.parse("archive"))) {
          deleteDirectoryContents(device.getFileSystem(), RelativePath.parse("archive"), true);
        }

        // del ${device_drive}\*.img
        for (String name : device.getFileSystem().list(
            RelativePath.EMPTY, new TBFileSystem.FilenameFilter() {
              @Override
              public boolean accept(TBFileSystem fs, RelativePath dir, String name) {
                return name.endsWith(".img");
              }
            })) {
          device.getFileSystem().deleteFile(
              new RelativePath(TBLoaderConstants.TB_AUDIO_PATH, name));
        }

        // del ${device_drive}\system\*.pkg
        // del ${device_drive}\system\*.dep
        for (String name : device.getFileSystem().list(
            TBLoaderConstants.TB_SYSTEM_PATH, new TBFileSystem.FilenameFilter() {
              @Override
              public boolean accept(TBFileSystem fs, RelativePath dir, String name) {
                return name.endsWith(".pkg") || name.endsWith(".dep");
              }
            })) {
          device.getFileSystem().deleteFile(
              new RelativePath(TBLoaderConstants.TB_SYSTEM_PATH, name));
        }

        // software\robocopy content\${new_deployment}\basic ${device_drive} /E /NP /XD .* /XA:H /XF *.srn *.rev
        //      /E    : Copy subdirectories, including Empty ones.
        //      /NP   : No Progress - don't display percentage copied.
        //      /XD   : eXclude Directories matching given names/paths.
        //      /XA:H : eXclude files with any of the given Attributes set.
        //      /XF   : eXclude Files matching given names/paths/wildcards.

        TBFileSystem.copyDir(sourceImage,
            TBLoaderConstants.CONTENT_BASIC_SUBDIR, device.getFileSystem(),
            RelativePath.EMPTY, new TBFileSystem.FilenameFilter() {
              @Override
              public boolean accept(TBFileSystem fs, RelativePath dir,
                  String name) {
                  return !name.endsWith(".srn") && !name.endsWith(".rev");
                }
              }, new TBFileSystem.FilenameFilter() {
              @Override
              public boolean accept(TBFileSystem fs, RelativePath dir,
                  String name) {
                try {
                  return !fs.isDirectory(dir);
                } catch (IOException e) {
                  return false;
                }
              }
            }, true, false);

        progressListener.updateProgress(70, "Updating Talking Book Files ...");
        // rem removing binary config and control tracks
        // del "${device_drive}\system\config.bin"
        // del "${device_drive}\languages\control.bin"
        // del "${device_drive}\languages\dga\control.bin"
        RelativePath fileToDelete = new RelativePath(TBLoaderConstants.TB_SYSTEM_PATH, "config.bin");
        if (device.getFileSystem().fileExists(fileToDelete)) {
          device.getFileSystem().deleteFile(fileToDelete);
        }
        fileToDelete = new RelativePath(TBLoaderConstants.TB_LANGUAGES_PATH, "control.bin");
        if (device.getFileSystem().fileExists(fileToDelete)) {
          device.getFileSystem().deleteFile(fileToDelete);
        }
        fileToDelete = new RelativePath(TBLoaderConstants.TB_LANGUAGES_PATH, "dga", "config.bin");
        if (device.getFileSystem().fileExists(fileToDelete)) {
          device.getFileSystem().deleteFile(fileToDelete);
        }

        // rem Setting Time and Location
        // echo SRN:${new_srn}> "${device_drive}\sysdata.txt"
        // echo IMAGE:${new_image}>> "${device_drive}\sysdata.txt"
        // echo UPDATE:${new_deployment}>> "${device_drive}\sysdata.txt"
        // echo LOCATION:${new_community}>> "${device_drive}\sysdata.txt"
        // echo YEAR:${year}>> "${device_drive}\sysdata.txt"
        // echo MONTH:${month}>> "${device_drive}\sysdata.txt"
        // echo DATE:${dateInMonth}>> "${device_drive}\sysdata.txt"
        Calendar cal = Calendar.getInstance();
        String month = String.valueOf(cal.get(Calendar.MONTH) + 1);
        String dateInMonth = String.valueOf(cal.get(Calendar.DAY_OF_MONTH));
        String year = String.valueOf(cal.get(Calendar.YEAR));

        StringBuilder sysDataTxtBuilder = new StringBuilder();
        sysDataTxtBuilder.append(String.format("SRN:%s\n", newDeploymentInfo.getSerialNumber()));
        sysDataTxtBuilder.append(String.format("IMAGE:%s\n", newDeploymentInfo.getPackageName()));
        sysDataTxtBuilder.append(String.format("UPDATE:%s\n", newDeploymentInfo.getDeploymentName()));
        sysDataTxtBuilder.append(String.format("LOCATION:%s\n", newDeploymentInfo.getCommunity()));
        sysDataTxtBuilder.append(String.format("YEAR:%s\n", year));
        sysDataTxtBuilder.append(String.format("MONTH:%s\n", month));
        sysDataTxtBuilder.append(String.format("DATE:%s\n", dateInMonth));
        eraseAndOverwriteFile(device.getFileSystem(), TBLoaderConstants.SYS_DATA_TXT,
            sysDataTxtBuilder.toString());

        // del "${device_drive}\system\*.srn"
        // del "${device_drive}\system\*.loc"
        for (String name : device.getFileSystem().list(
            TBLoaderConstants.TB_SYSTEM_PATH, new TBFileSystem.FilenameFilter() {
              @Override
              public boolean accept(TBFileSystem fs, RelativePath dir, String name) {
                return name.endsWith(".srn") || name.endsWith(".loc");
              }
            })) {
          device.getFileSystem().deleteFile(
              new RelativePath(TBLoaderConstants.TB_SYSTEM_PATH, name));
        }

        // echo . > ${device_drive}\system\${new_srn}.srn
        eraseAndOverwriteFile(device.getFileSystem(),
            new RelativePath(TBLoaderConstants.TB_SYSTEM_PATH,
                newDeploymentInfo.getSerialNumber() + ".srn"),
            ".");

        // echo . > ${device_drive}\system\${new_deployment}.dep
        eraseAndOverwriteFile(device.getFileSystem(),
            new RelativePath(TBLoaderConstants.TB_SYSTEM_PATH,
                newDeploymentInfo.getDeploymentName() + ".dep"),
            ".");

        // echo ${new_community}> "${device_drive}\system\${new_community}.loc"
        eraseAndOverwriteFile(device.getFileSystem(),
            new RelativePath(TBLoaderConstants.TB_SYSTEM_PATH,
                newDeploymentInfo.getCommunity() + ".loc"),
            newDeploymentInfo.getCommunity());

        // echo . > "${device_drive}\inspect"
        eraseAndOverwriteFile(device.getFileSystem(),
            new RelativePath(RelativePath.EMPTY, "inspect"), ".");

        // del "${device_drive}\*.rtc"
        for (String name : device.getFileSystem().list(
            RelativePath.EMPTY, new TBFileSystem.FilenameFilter() {
              @Override
              public boolean accept(TBFileSystem fs, RelativePath dir, String name) {
                return name.endsWith(".rtc");
              }
            })) {
          device.getFileSystem().deleteFile(
              new RelativePath(name));
        }

        // echo . > "${device_drive}\0h1m0s.rtc"
        eraseAndOverwriteFile(device.getFileSystem(),
            new RelativePath(RelativePath.EMPTY, "0h1m0s.rtc"), ".");

        // echo ${syncdir}> "${device_drive}\system\last_updated.txt"
        eraseAndOverwriteFile(device.getFileSystem(),
            new RelativePath(TBLoaderConstants.TB_SYSTEM_PATH, "last_updated.txt"),
            getDevice().getSyncDir());


        LOG.log(Level.INFO, "STATUS:Updated");
        LOG.log(Level.INFO, "STATUS:Adding Image Content");
        // ***************   customCommunity.txt   ***************
        // rem Copying Image-Specific Files
        // IF EXIST "content\${new_deployment}\images\${new_image}" software\robocopy "content\${new_deployment}\images\${new_image}" ${device_drive} /NP /XX /E
        //   XX: An "extra" file is present in destination but not source, excluding extras will NOT delete from destination.
        // !IF EXIST "content\${new_deployment}\images\${new_image}" software\robocopy "content\${new_deployment}\images\${new_image}" ${device_drive} /NP /XX /E /NJH /NJS /NDL /L
        RelativePath newImagePath = new RelativePath(TBLoaderConstants.IMAGES_SUBDIR, newDeploymentInfo.getPackageName());
        if (sourceImage.fileExists(newImagePath)) {
          TBFileSystem.copyDir(sourceImage,
              newImagePath,
              device.getFileSystem(), RelativePath.EMPTY, TBFileSystem.ACCEPT_ALL, TBFileSystem.ACCEPT_ALL, true, true);
        }
        progressListener.updateProgress(80, "Updating Talking Book Files ...");

        LOG.log(Level.INFO, "STATUS:Adding communities Content");
        // rem Copying Community-Specific Files
        // IF EXIST "content\${new_deployment}\communities\${new_community}" software\robocopy "content\${new_deployment}\communities\${new_community}" ${device_drive} /NP /XX /E
        //   XX: An "extra" file is present in destination but not source, excluding extras will NOT delete from destination.
        // !IF EXIST "content\${new_deployment}\communities\${new_community}" software\robocopy "content\${new_deployment}\communities\${new_community}" ${device_drive} /NP /XX /E /NJH /NJS /NDL /L
        RelativePath newCommunityPath = new RelativePath(TBLoaderConstants.COMMUNITIES_SUBDIR, newDeploymentInfo.getCommunity());
        if (tbLoaderConfig.getDropboxFileSystem().fileExists(newCommunityPath)) {
          TBFileSystem.copyDir(tbLoaderConfig.getDropboxFileSystem(),
              newImagePath,
              device.getFileSystem(), RelativePath.EMPTY, TBFileSystem.ACCEPT_ALL, TBFileSystem.ACCEPT_ALL, true, true);
        }

        progressListener.updateProgress(90, "Updating Talking Book Files ...");

        LOG.log(Level.INFO, "STATUS:Verifying sys data");
        // rem REVIEW TARGET
        // type "${device_drive}\sysdata.txt"
        // dir ${device_drive} /s
        verified = device.getFileSystem().fileExists(TBLoaderConstants.SYS_DATA_TXT);

        if (forceFirmware) {
          // rename firmware at root to system.img to force TB to update itself
          String rootPath =  device.getFileSystem().getRootPath().substring(0, 2);
          File root = new File(rootPath);
          File firmware = new File(root, newDeploymentInfo.getFirmwareRevision() + ".img");
          firmware.renameTo(new File(root, "system.img"));
          progressListener.addDetail("\nRefreshed firmware...");
          LOG.log(Level.INFO, "STATUS:Forced Firmware Refresh");
        }
        progressListener.addDetail("...Updated\n");
        if (verified) {
          progressListener.addDetail("Updated & Verified");
          LOG.log(Level.INFO, "STATUS:Updated & Verified...Disconnecting TB");
          if (OSChecker.WINDOWS) {
            progressListener.addDetail("Disconnecting TB");
            LOG.log(Level.INFO, "STATUS:Disconnecting TB");
            CommandLineUtils.disconnectDrive(device.getFileSystem().getRootPath());
          }

          progressListener.addDetail("...Complete");
          LOG.log(Level.INFO, "STATUS:Complete");
          success = true;
          if (forceFirmware) {
            logTBData("update-fw", oldDeploymentInfo,
                newDeploymentInfo, location, durationSeconds,
                tbLoaderConfig.getDropboxFileSystem());
          } else {
            logTBData("update", oldDeploymentInfo,
                newDeploymentInfo, location, durationSeconds,
                tbLoaderConfig.getDropboxFileSystem());
          }
        } else {
          logTBData("update-failed verification", oldDeploymentInfo,
              newDeploymentInfo, location, durationSeconds,
              tbLoaderConfig.getDropboxFileSystem());
          success = false;
          progressListener.addDetail("...Failed Verification in "
                  + getDuration() + "\n");
          LOG.log(Level.INFO, "STATUS:Failed Verification");
        }
      }
    } catch (Throwable e) {
      LOG.log(Level.WARNING, "Unable to zip device files:", e);
      e.printStackTrace();
      progressListener.updateProgress(100, "Update failed.");
      progressListener.addDetail(getStackTrace(e));
    }

    if (success && verified) {
      progressListener.updateProgress(100, "Update complete.");
    } else {
      progressListener.updateProgress(100, "Update failed.");
    }

    return new Result(success, hasCorruption, false, verified);
  }


  public static String getStackTrace(Throwable aThrowable) {
    Writer result = new StringWriter();
    PrintWriter printWriter = new PrintWriter(result);
    aThrowable.printStackTrace(printWriter);
    return result.toString();
  }

  private void eraseAndOverwriteFile(TBFileSystem fs, RelativePath path, String content) throws IOException {
    // e.g.: echo .>${device_drive}\sysdata.txt
    fs.createNewFile(path, new ByteArrayInputStream(content.getBytes()), true);
  }

  // delete contents for dir without deleting dir itself
  private void deleteDirectoryContents(TBFileSystem fs, RelativePath dir, boolean deleteDir) throws IOException {
    if (fs.fileExists(dir) && fs.isDirectory(dir)) {
      for (String file : fs.list(dir)) {
        TBFileSystem.deleteRecursive(fs, new RelativePath(dir, file));
      }
    }

    if (deleteDir) {
      fs.rmdir(dir);
    }
  }

  private void setStartTime() {
    startTime = System.nanoTime();
  }

  private String getDuration() {
    String elapsedTime;
    int durationMinutes;
    long durationNanoseconds = System.nanoTime() - startTime;
    durationSeconds = (int) (durationNanoseconds / 1000000000.0);
    if (durationSeconds > 60) {
      durationMinutes = durationSeconds / 60;
      durationSeconds -= durationMinutes * 60;
      elapsedTime = new String(
          Integer.toString(durationMinutes) + " minutes " + Integer.toString(
              durationSeconds) + " seconds");
    } else
      elapsedTime = new String(
          Integer.toString(durationSeconds) + " seconds");
    return elapsedTime;
  }

  public static String getImageFromCommunity(final TBFileSystem sourceImage, String community) throws IOException {
    if (community == null) {
      return null;
    }
    String imageName = "";
    String groupName = "";
    String[] images;

    images = sourceImage.list(TBLoaderConstants.IMAGES_SUBDIR, new TBFileSystem.FilenameFilter() {
      @Override
      public boolean accept(TBFileSystem fs, RelativePath dir, String name) {
        try {
          return fs.isDirectory(dir);
        } catch (IOException e) {
          return false;
        }
      }
    });
    if (images != null && images.length == 1) {
      // grab first image package
      imageName = images[0];
    } else if (images != null) {
      RelativePath fCommunityDir = new RelativePath(TBLoaderConstants.COMMUNITIES_SUBDIR, community, "system");

      if (sourceImage.fileExists(fCommunityDir)) {
        // get groups
        String[] groups = sourceImage.list(fCommunityDir, new TBFileSystem.FilenameFilter() {
          @Override
          public boolean accept(TBFileSystem fs, RelativePath dir, String name) {
            String lowercase = name.toLowerCase();
            return lowercase.endsWith(GROUP_FILE_EXTENSION);
          }
        });
        for (String group : groups) {
          // for every group that the community belongs to look for a match in each of images's group listing
          groupName = group.substring(0, group.length() - 4);
          for (String image : images) {
            RelativePath imageSysFolder = new RelativePath(TBLoaderConstants.IMAGES_SUBDIR, image, "system");
            String[] imageGroups = sourceImage.list(imageSysFolder, new TBFileSystem.FilenameFilter() {
              @Override
              public boolean accept(TBFileSystem fs, RelativePath dir, String name) {
                String lowercase = name.toLowerCase();
                return lowercase.endsWith(GROUP_FILE_EXTENSION);
              }
            });
            for (String imageGroup : imageGroups) {
              String imageGroupName = imageGroup.substring(0, imageGroup.length() - 4);
              if (imageGroupName.equalsIgnoreCase(groupName)) {
                imageName = image;
                break;
              }
            }
            if (!imageName.equals("")) {
              break;
            }
          }
        }
      }
      if (imageName.equals("")) {
        // no match of groups between community and multiple packages
        // Only hope is to find a default package
        for (String image : images) {
          RelativePath imageSysFolder = new RelativePath(TBLoaderConstants.IMAGES_SUBDIR, image, "system");
          String[] imageDefaultGroup = sourceImage.list(imageSysFolder,
              new TBFileSystem.FilenameFilter() {
                @Override
                public boolean accept(TBFileSystem fs, RelativePath dir, String name) {
                  String lowercase = name.toLowerCase();
                  return lowercase.endsWith(
                      DEFAULT_GROUP_LABEL + GROUP_FILE_EXTENSION);
                }
              });
          if (imageDefaultGroup.length == 1) {
            imageName = image;
          }
        }
      }
    }
    if (imageName.equals("")) {
      imageName = "ERROR!  MISSING CONTENT IMAGE!";
    }
    return imageName;
  }


  private static ProgressListener wrap(final ProgressListener listener) {
    return new ProgressListener() {
      @Override public void updateProgress(int progressPercent, String progressUpdate) {
        LOG.log(Level.INFO, "PROGRESS: " + progressPercent + "% (" + progressUpdate + ")");
        if (listener != null) {
          listener.updateProgress(progressPercent, progressUpdate);
        }
      }

      @Override
      public void addDetail(String detail) {
        LOG.log(Level.INFO, "\t" + detail);
        if (listener != null) {
          listener.addDetail(detail);
        }
      }
    };
  }
}
