package org.literacybridge.core.tbloader;

import org.literacybridge.core.fs.RelativePath;
import org.literacybridge.core.fs.TbFile;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.literacybridge.core.tbloader.TBLoaderConstants.NO_SERIAL_NUMBER;
import static org.literacybridge.core.tbloader.TBLoaderConstants.PROJECT_FILE_EXTENSION;
import static org.literacybridge.core.tbloader.TBLoaderUtils.isSerialNumberFormatGood;
import static org.literacybridge.core.tbloader.TBLoaderUtils.isSerialNumberFormatGood2;

public final class TBDeviceInfo {
    private static final Logger LOG = Logger.getLogger(TBDeviceInfo.class.getName());

    private final String tbPrefix;

    private final TbFile tbRoot;
    private final TbFile tbSystem;
    private final String label;
    private String serialNumber;
    private boolean needNewSerialNumber;
    private boolean corrupted;
    private TbFlashData tbFlashData;

    public TBDeviceInfo(TbFile tbRoot, String label, String prefix) {
        this.tbRoot = tbRoot;
        this.label = label.trim();
        this.corrupted = false;
        this.serialNumber = "";
        if (tbRoot != null) {
            tbSystem = tbRoot.open(TBLoaderConstants.TB_SYSTEM_PATH);
            tbFlashData = loadFlashData(tbRoot);
        } else {
            tbSystem = null;
            tbFlashData = null;
        }
        tbPrefix = prefix;
    }

    public TbFile getRootFile() {
        return tbRoot;
    }

    public String getLabel() {
        return label;
    }

    public String getLabelWithoutDriveLetter() {
        String labelNoDriveLetter = this.label;
        int index = labelNoDriveLetter.lastIndexOf('(');
        if (index > 0) {
            labelNoDriveLetter = labelNoDriveLetter.substring(0, index - 1);
        }
        return labelNoDriveLetter;
    }

    void setCorrupted(boolean corrupted) {
        this.corrupted = corrupted;
    }

    boolean isCorrupted() {
        return corrupted;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public boolean needNewSerialNumber() { return needNewSerialNumber; }

    /**
     * Tries to determine is a Talking Book is connected. Looks for key files in the TB file system.
     * @param checkHasSystem If true, look for additional specific directories expected to exist.
     * @return True if probably is a Talking Book, false if maybe not.
     */
    public boolean checkConnection(boolean checkHasSystem) {

        if (tbRoot == null || !tbRoot.exists() || !tbRoot.isDirectory()) {
            return false;
        }

        String[] files = tbRoot.list();
        if (files == null) {
            return false;
        }

        if (!checkHasSystem) {
            return true;
        }

        if (files.length == 0) {
            return false;
        }

        return tbRoot.open(TBLoaderConstants.TB_SYSTEM_PATH).exists()
                && tbRoot.open(TBLoaderConstants.TB_LANGUAGES_PATH).exists()
                && tbRoot.open(TBLoaderConstants.TB_LISTS_PATH).exists()
                && tbRoot.open(TBLoaderConstants.TB_AUDIO_PATH).exists();
    }

    /**
     * Reads and parses the flashData.bin file from a Talking Book.
     * @return A TbFlashData structure from the Talking Book, or null if one can't be read.
     */
    private static TbFlashData loadFlashData(TbFile tbRoot) {
        TbFlashData tbFlashData = null;
        try {
            TbFile flashDataBin = tbRoot.open(TBLoaderConstants.BINARY_STATS_PATH);
            if (flashDataBin.exists()) {
                tbFlashData = new TbFlashData(flashDataBin);
            } else {
                flashDataBin = tbRoot.open(TBLoaderConstants.BINARY_STATS_ALTERNATIVE_PATH);
                if (flashDataBin.exists()) {
                    tbFlashData = new TbFlashData(flashDataBin);
                }
            }
        }catch (IOException ex) {
            // Ignore exception; no tbinfo, so return null;
        }

        // Weird little bit of logic here. Wonder what it means...
        if (tbFlashData != null && tbFlashData.getCountReflashes() == -1) {
            tbFlashData = null;
        }

        return tbFlashData;
    }

    /**
     * Returns the cached TbInfo, if there is one, but doesn't try to read from the file system.
     * @return The TbInfo, or null if there is one, or it hasn't been loaded yet.
     */
    TbFlashData getFlashData() {
        return tbFlashData;
    }

    /**
     * Looks in the TalkingBook's system directory for any files with a ".srn" extension who's name does not begin
     * with "-erase". If any such files are found, returns the name of the first one found (ie, one selected at random),
     * without the extension.
     *
     * @return The file's name found (minus extension), or null if no file found.
     */
    public String getSerialNumber(String prefix) {
        String sn;

        if (getFlashData() != null && isSerialNumberFormatGood(prefix, getFlashData().getSerialNumber())
                && isSerialNumberFormatGood2(getFlashData().getSerialNumber())) {
            sn = getFlashData().getSerialNumber();
        } else {
            sn = getSerialNumberFromFileSystem(tbRoot);
        }
        return sn;
    }

    /**
     * Looks in the TalkingBook's system directory for any files with a ".srn" extension whose name does not begin
     * with "-erase". If any such files are found, returns the name of the first one found (ie, one selected at random),
     * without the extension.
     *
     * NOTE: this is a static function so that the Talking Book Connection manager can get the device serial
     * number quickly and cheaply (basically, without parsing the flash data).
     * @TODO: Is that really necessary?
     *
     * @param tbRoot The root of the Talking Book file system.
     * @return The file's name found (minus extension), or "UNKNOWN"
     */
    public static String getSerialNumberFromFileSystem(TbFile tbRoot) {
        String sn = NO_SERIAL_NUMBER;
        if (tbRoot == null) {
            return sn;
        }
        TbFile tbSystem = tbRoot.open(TBLoaderConstants.TB_SYSTEM_PATH);
        String[] files;

        if (tbSystem.exists()) {
            files = tbSystem.list(new TbFile.FilenameFilter() {
                @Override
                public boolean accept(TbFile parent, String name) {
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
     * Look in the TalkingBook's system directory for any files with a ".prj" extension. If any such
     * files are found, return the name of the first one found (ie, selected at random), without the
     * extension.
     *
     * @return The file's name found (minus extension), or the value of newProject if none is
     * found.
     */
    public String getProjectName() {
        String project = "UNKNOWN";
        String [] prjFiles;
        if (tbSystem.exists()) {
            prjFiles = tbSystem.list(new TbFile.FilenameFilter() {
                @Override
                public boolean accept(TbFile parent, String name) {
                    String lowercase = name.toLowerCase();
                    return lowercase.endsWith(PROJECT_FILE_EXTENSION);
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
     * @return The file's name found (minus extension), or "UNKNOWN" if no file found, or if the file name consists
     * only of the extension (eg, a file named ".img" will return "UNKNOWN").
     */
    String getFirmwareVersion() {
        String rev = "UNKNOWN";
        String[] files;

        if (tbSystem.exists()) {
            files = tbSystem.list(new TbFile.FilenameFilter() {
                @Override
                public boolean accept(TbFile parent, String name) {
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
     * @return The file's name found (minus extension), or "UNKNOWN" if no file found.
     */
    String getPackageName() {
        String pkg = "UNKNOWN";
        String[] files;

        if (getFlashData() != null && getFlashData().getImageName() != null
                && !getFlashData().getImageName().equals("")) {
            pkg = getFlashData().getImageName();
        } else if (tbSystem.exists()) {
            files = tbSystem.list(new TbFile.FilenameFilter() {
                @Override
                public boolean accept(TbFile parent, String name) {
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
     * @return The file's name found (minus extension), or "UNKNOWN" if no file found.
     */
    String getDeploymentName() {
        String depl = "UNKNOWN";
        String[] files;

        if (getFlashData() != null && getFlashData().getDeploymentNumber() != null
                && !getFlashData().getDeploymentNumber().equals("")) {
            depl = getFlashData().getDeploymentNumber();
        } else if (tbSystem.exists()) {
            files = tbSystem.list(new TbFile.FilenameFilter() {
                @Override
                public boolean accept(TbFile parent, String name) {
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
     * Looks in the TalkingBook's system directory for a file named "last_updated.txt",
     * and reads the first line from it. Stores any value so read into TBLoader.lastSynchDir.
     *
     * @return The "last synch dir", which has the update date and time encoded into it.
     */
    String getSynchDir() {
        String lastSynchDir = null;

        TbFile lastUpdate = tbSystem.open("last_updated.txt");
        if (lastUpdate.exists()) {
            try (InputStream fstream = lastUpdate.openFileInputStream();
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

    /**
     * Checks in tbstats structure for location. If not found, will then
     * look in the TalkingBook's root for a file with a ".loc" extension.
     * If there is exactly one such file, returns the file's name, sans extension,
     * if no such file in the root, checks in the system directory.
     *
     * @return The community name, or "UNKNOWN" if it can not be determined.
     */
    public String getCommunityName() {
        String communityName = "UNKNOWN";
        if (tbRoot == null) {
            return communityName;
        }
        if (getFlashData() != null && getFlashData().getLocation() != null && !getFlashData().getLocation().equals(
                "")) {
            communityName = getFlashData().getLocation();
        } else {
            try {
                String[] files;
                // get Location file info
                // check root first, in case tbDeviceInfo was just assigned a new community (e.g. from this app)
                files = tbRoot.list(new TbFile.FilenameFilter() {
                    @Override
                    public boolean accept(TbFile parent, String name) {
                        String lowercase = name.toLowerCase();
                        return lowercase.endsWith(".loc");
                    }
                });
                if (files == null) {
                    LOG.log(Level.INFO, "This does not look like a TB: " + tbRoot.toString());

                } else if (files.length == 1) {
                    String locFileName = files[0];
                    communityName = locFileName.substring(0, locFileName.length() - 4);
                } else if (files.length == 0 && tbSystem.exists()) {
                    // get Location file info
                    files = tbSystem.list(new TbFile.FilenameFilter() {
                        @Override
                        public boolean accept(TbFile parent, String name) {
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
        }
        LOG.log(Level.INFO, "TB's current community name is " + communityName);
        return communityName;
    }

    /**
     * Loads information about the previous deployment from the Talking Book's file system.
     *
     */
    public DeploymentInfo createDeploymentInfo(String defaultProject) {
        String serialNumber = NO_SERIAL_NUMBER; // "UNKNOWN"
        DeploymentInfo deploymentInfo = null;

        if (tbRoot == null) {
            return null;
        }
        RelativePath systemPath = TBLoaderConstants.TB_SYSTEM_PATH;

        try {
            String  community = getCommunityName();

            serialNumber = getSerialNumber(tbPrefix);

            String firmwareVersion = getFirmwareVersion();

            // Previous image or package. Displays as "Content"
            // TODO: package vs image - consistency in nomenclature.
            String pkg = getPackageName();

            // Previous deployment name. Displays as "Update"
            String depl = getDeploymentName();

            // Last updated date. Displays as "First Rotation Date".
            String lastSynchDir = getSynchDir();
            String lastUpdate = getLastUpdateDate(lastSynchDir);

            // Project previously deployed to Talking Book, if known, else the new project about to be
            // deployed to the Talking Book.
            String oldProject = getProjectName();
            if (oldProject == null || oldProject.equalsIgnoreCase("UNKNOWN")) {
                oldProject = defaultProject;
            }

            // See if we need to allocate a new serial number. If we do, just mark it, don't actually
            // allocate one until we're sure we'll use it.
            serialNumber = serialNumber.toUpperCase();
            if (!TBLoaderUtils.isSerialNumberFormatGood2(serialNumber)) {
                needNewSerialNumber = true;
                // We will allocate a new-style serial number before we update the tbDeviceInfo.
                serialNumber = TBLoaderConstants.NEED_SERIAL_NUMBER;
            }

            deploymentInfo = new DeploymentInfo(serialNumber, oldProject, depl, pkg, lastSynchDir, lastUpdate, firmwareVersion, community);

            setSerialNumber(serialNumber);
        } catch (Exception ignore) {
            LOG.log(Level.WARNING, "exception - ignore and keep going with empty strings", ignore);
        }

        return deploymentInfo;
    }

    /**
     * Look in the tbstats structure for updateDate. If none, parse the synchDir, if there is one.
     *
     * @return The last sync date, or "UNKNOWN" if not available.
     */
    private String getLastUpdateDate(String synchDir) {
        String lastUpdate = "UNKNOWN";

        if (tbFlashData != null && tbFlashData.getUpdateDate() != -1)
            lastUpdate = tbFlashData.getUpdateYear() + "/" + tbFlashData.getUpdateMonth() + "/"
                    + tbFlashData.getUpdateDate();
        else {
            if (synchDir != null) {
                int y = synchDir.indexOf('y');
                int m = synchDir.indexOf('m');
                int d = synchDir.indexOf('d');
                lastUpdate =
                        synchDir.substring(0, y) + "/" + synchDir.substring(y + 1, m) + "/"
                                + synchDir.substring(m + 1, d);
            }
        }

        return lastUpdate;
    }


    @Override
    public String toString() {
        if (label.isEmpty()) {
            return tbRoot.toString();
        }
        return label;
    }
}
