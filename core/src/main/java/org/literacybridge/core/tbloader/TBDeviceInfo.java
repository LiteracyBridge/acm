package org.literacybridge.core.tbloader;

import org.literacybridge.core.fs.TbFile;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.literacybridge.core.tbloader.TBLoaderConstants.COMMUNITY_PROPERTY;
import static org.literacybridge.core.tbloader.TBLoaderConstants.DEPLOYMENT_PROPERTIES_NAME;
import static org.literacybridge.core.tbloader.TBLoaderConstants.DEPLOYMENT_PROPERTY;
import static org.literacybridge.core.tbloader.TBLoaderConstants.PACKAGE_PROPERTY;
import static org.literacybridge.core.tbloader.TBLoaderConstants.PROJECT_FILE_EXTENSION;
import static org.literacybridge.core.tbloader.TBLoaderConstants.PROJECT_PROPERTY;
import static org.literacybridge.core.tbloader.TBLoaderConstants.TALKING_BOOK_ID_PROPERTY;
import static org.literacybridge.core.tbloader.TBLoaderConstants.TEST_DEPLOYMENT_PROPERTY;
import static org.literacybridge.core.tbloader.TBLoaderConstants.UNKNOWN;
import static org.literacybridge.core.tbloader.TBLoaderUtils.isSerialNumberFormatGood;
import static org.literacybridge.core.tbloader.TBLoaderUtils.isSerialNumberFormatGood2;

public final class TBDeviceInfo {
    private static final Logger LOG = Logger.getLogger(TBDeviceInfo.class.getName());

    private final TbFile tbRoot;
    private final String label;
    private final String tbPrefix;

    // Computed or cached properties.
    private final TbFile tbSystem;
    private TbFlashData tbFlashData;
    private Properties tbDeploymentProperties;
    private boolean corrupted = false;

    // Properties that are read from the Talking Book and provided to clients of the class.
    private String serialNumber = null;
    private boolean needNewSerialNumber = false;
    private Boolean testDeployment = null;
    private String projectName = null;
    private String deploymentName = null;
    private String packageName = null;
    private String communityName = null;


    public static TBDeviceInfo getNullDeviceInfo() {
        return new TBDeviceInfo(TBLoaderConstants.NO_DRIVE, null);
    }

    public TBDeviceInfo(TbFile tbRoot, String label, String prefix) {
        if (tbRoot == null) {
            throw new IllegalArgumentException("Root may not be null");
        }
        this.tbRoot = tbRoot;
        this.label = label.trim();
        tbPrefix = prefix;
        tbSystem = tbRoot.open(TBLoaderConstants.TB_SYSTEM_PATH);
        tbFlashData = loadFlashData(tbRoot);
        loadDeploymentProperties();
        loadSerialNumber();
    }

    private TBDeviceInfo(String label, String prefix) {
        this.tbRoot = null;
        this.tbPrefix = prefix;
        this.label = label;
        tbSystem = null;
        tbFlashData = null;
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

    void setCorrupted() {
        this.corrupted = true;
    }

    boolean isCorrupted() {
        return corrupted;
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

    private void loadSerialNumber() {
        getSerialNumber();
        // See if we need to allocate a new serial number. If we do, just mark it, don't actually
        // allocate one until we're sure we'll use it.
        if (!TBLoaderUtils.isSerialNumberFormatGood(tbPrefix, serialNumber) ||
                !TBLoaderUtils.isSerialNumberFormatGood2(serialNumber)) {
            needNewSerialNumber = true;
            // We will allocate a new-style serial number before we update the tbDeviceInfo.
            serialNumber = TBLoaderConstants.NEED_SERIAL_NUMBER;
        }
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
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
    public String getSerialNumber() {
        if (serialNumber == null) {
            serialNumber = getProperty(TALKING_BOOK_ID_PROPERTY, UNKNOWN);
            String src = "properties";

            // If we didn't have it in properties, look for the flash data or marker file(s).
            if (serialNumber.equalsIgnoreCase(UNKNOWN)) {
                if (getFlashData() != null
                    && isSerialNumberFormatGood(tbPrefix, getFlashData().getSerialNumber())
                    && isSerialNumberFormatGood2(getFlashData().getSerialNumber())) {
                    serialNumber = getFlashData().getSerialNumber();
                    src = "flash";
                } else {
                    serialNumber = getSerialNumberFromFileSystem(tbRoot);
                    src = "marker";
                }
            }
            serialNumber = serialNumber.toUpperCase();
            LOG.log(Level.INFO, String.format("TBL!: Got serial number (%s) from %s.", serialNumber, src));
        }
        return serialNumber;
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
     * @return The file's name found (minus extension), or UNKNOWN
     */
    public static String getSerialNumberFromFileSystem(TbFile tbRoot) {
        String sn = UNKNOWN;
        if (tbRoot == null) {
            return sn;
        }
        TbFile tbSystem = tbRoot.open(TBLoaderConstants.TB_SYSTEM_PATH);
        Properties props = loadDeploymentProperties(tbSystem);

        sn = props.getProperty(TALKING_BOOK_ID_PROPERTY, UNKNOWN);

        if (sn.equalsIgnoreCase(UNKNOWN)) {
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
                    sn = UNKNOWN;
                }
                if (!sn.equals(UNKNOWN)) {
                    LOG.log(Level.INFO, "TBL!: No stats SRN. Found *.srn file:" + sn);
                } else {
                    LOG.log(Level.INFO, "TBL!: No stats SRN and no good *.srn file found.");
                }
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
        if (projectName == null) {
            projectName = getProperty(PROJECT_PROPERTY, UNKNOWN);
            String src = "properties";

            // If we didn't have it in properties, look for marker file(s).
            if (projectName.equalsIgnoreCase(UNKNOWN)) {

                String[] prjFiles;
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
                        projectName = fn.substring(0, fn.length() - PROJECT_FILE_EXTENSION.length());
                        src = "marker";
                    }
                }
            }
            LOG.log(Level.INFO, String.format("TBL!: Got project name (%s) from %s.", projectName, src));
        }
        return projectName;
    }

    /**
     * Looks in the TalkingBook's system directory for any files with a ".rev" or ".img" extension. If any such files
     * are found, returns the name (without extension) of the first .rev file found, else the name of the first .img file
     * found (ie, chosen somewhat at random.).
     *
     * @return The file's name found (minus extension), or UNKNOWN if no file found, or if the file name consists
     * only of the extension (eg, a file named ".img" will return UNKNOWN).
     */
    private String getFirmwareVersion() {
        String rev = UNKNOWN;
        String revFileName = null;
        String imgFileName = null;
        String[] files;

        if (tbSystem.exists()) {
            files = tbSystem.list(new TbFile.FilenameFilter() {
                @Override
                public boolean accept(TbFile parent, String name) {
                    String lowercase = name.toLowerCase();
                    return lowercase.length() > 4 && (lowercase.endsWith(".img") || lowercase.endsWith(".rev"));
                }
            });

            for (String file : files) {
                String fn = file;
                fn = fn.substring(0, fn.length() - 4);
                String ext = file.substring(fn.length());
                if (ext.equalsIgnoreCase(".img") && imgFileName == null) {
                    imgFileName = fn;
                } else if (revFileName == null) {
                    revFileName = fn;
                }
            }
            // Found .rev? .img?
            rev = (revFileName!=null) ? revFileName : ((imgFileName!=null) ? imgFileName : rev);
        }

        return rev;
    }

    /**
     * Checks in tbstats structure for image name. If not found, will then
     * look in the TalkingBook's system directory for a file with a ".pkg" extension. If there is exactly
     * one such file, returns the file's name, sans extension.
     *
     * @return The file's name found (minus extension), or UNKNOWN if no file found.
     */
    private String getPackageName() {
        if (packageName == null) {
            packageName = getProperty(PACKAGE_PROPERTY, UNKNOWN);
            String src = "properties";

            // If we didn't have it in properties, look for the flash data or marker file(s).
            if (packageName.equalsIgnoreCase(UNKNOWN)) {
                String[] files;

                if (getFlashData() != null && getFlashData().getImageName() != null
                    && !getFlashData().getImageName().equals("")) {
                    packageName = getFlashData().getImageName();
                    src = "flash";
                } else if (tbSystem.exists()) {
                    files = tbSystem.list(new TbFile.FilenameFilter() {
                        @Override
                        public boolean accept(TbFile parent, String name) {
                            String lowercase = name.toLowerCase();
                            return lowercase.endsWith(".pkg");
                        }
                    });

                    if (files.length == 1) {
                        packageName = files[0].substring(0, files[0].length() - 4);
                        src = "marker";
                    }
                }
            }
            LOG.log(Level.INFO, String.format("TBL!: Got package name (%s) from %s.", packageName, src));
        }
        return packageName;
    }

    /**
     * Look in the tbstats structure for a deployment name (called "deploymentNumber", but really a string). If none,
     * look in the TalkingBook's system directory for a file with a ".dep" extension. If there is exactly
     * one such file, returns the file's name, sans extension.
     *
     * @return The file's name found (minus extension), or UNKNOWN if no file found.
     */
    private String getDeploymentName() {
        if (deploymentName == null) {
            deploymentName = getProperty(DEPLOYMENT_PROPERTY, UNKNOWN);
            String src = "properties";

            // If we didn't have it in properties, look for the flash data or marker file(s).
            if (deploymentName.equalsIgnoreCase(UNKNOWN)) {
                String[] files;

                if (getFlashData() != null && getFlashData().getDeploymentNumber() != null
                    && !getFlashData().getDeploymentNumber().equals("")) {
                    deploymentName = getFlashData().getDeploymentNumber();
                    src = "flash";
                } else if (tbSystem.exists()) {
                    files = tbSystem.list(new TbFile.FilenameFilter() {
                        @Override
                        public boolean accept(TbFile parent, String name) {
                            String lowercase = name.toLowerCase();
                            return lowercase.endsWith(".dep");
                        }
                    });
                    if (files.length == 1) {
                        deploymentName = files[0].substring(0, files[0].length() - 4);
                        src = "marker";
                    }
                }
            }
            LOG.log(Level.INFO, String.format("TBL!: Got deployment name (%s) from %s.", deploymentName, src));
        }
        return deploymentName;
    }

    /**
     * Looks in the TalkingBook's system directory for a file named "last_updated.txt",
     * and reads the first line from it. Stores any value so read into TBLoader.lastSynchDir.
     *
     * @return The "last synch dir", which has the update date and time encoded into it.
     */
    private String getSynchDir() {
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
     * @return The community name, or UNKNOWN if it can not be determined.
     */
    public String getCommunityName() {
        if (communityName == null) {
            communityName = getProperty(COMMUNITY_PROPERTY, UNKNOWN);
            String src = "properties";

            // If we didn't have it in properties, look for the flash data or marker file(s).
            if (communityName.equalsIgnoreCase(UNKNOWN)) {
                if (getFlashData() != null && getFlashData().getCommunity() != null
                    && !getFlashData().getCommunity().equals(
                    "")) {
                    communityName = getFlashData().getCommunity();
                    src = "flash";
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
                            LOG.log(Level.INFO,
                                "TBL!: This does not look like a TB: " + tbRoot.toString());

                        } else if (files.length == 1) {
                            String locFileName = files[0];
                            communityName = locFileName.substring(0, locFileName.length() - 4);
                            src = "root marker";
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
                                src = "system marker";
                            }
                        }
                    } catch (Exception ignore) {
                        LOG.log(Level.WARNING, "TBL!: Exception while reading community", ignore);
                        // ignore and keep going with empty string
                        src = "nowhere";
                    }
                }
            }
            LOG.log(Level.INFO, String.format("TBL!: Got community name (%s) from %s.", communityName, src));
        }
        return communityName;
    }

    public boolean isTestDeployment() {
        if (testDeployment == null) {
            testDeployment = false;
            Properties properties = loadDeploymentProperties();
            if (properties != null) {
                String testDeploymentStr = properties.getProperty(TEST_DEPLOYMENT_PROPERTY, Boolean.FALSE.toString());
                testDeployment = Boolean.parseBoolean(testDeploymentStr);
                LOG.log(Level.INFO, String.format("TBL!: isTestDeployment: %b (%s)", testDeployment, testDeploymentStr));
            } else {
                LOG.log(Level.INFO, String.format("TBL!: isTestDeployment: %b (no deployment.properties)", testDeployment));
            }
        }
        return testDeployment;
    }

    /**
     * If there is a deployment.properties file (already loaded), and it has a value for the
     * desired property, return that value. Otherwise return the default value.
     * @param name of the desired property.
     * @param defaultValue if there is no properties file, or if it does not contain the property.
     * @return the property, or the default value.
     */
    private String getProperty(String name, String defaultValue) {
        if (tbDeploymentProperties != null) {
            return tbDeploymentProperties.getProperty(name, defaultValue);
        }
        return defaultValue;
    }

    /**
     * If there is a "deployment.properties" file in the system directory, loads it.
     * @return The Properties, or null if not found or unreadable.
     */
    private Properties loadDeploymentProperties() {
        if (tbDeploymentProperties == null) {
            tbDeploymentProperties = loadDeploymentProperties(tbSystem);
        }
        return tbDeploymentProperties;
    }

    private static Properties loadDeploymentProperties(TbFile tbSystem) {
        Properties props = new Properties();
        if (tbSystem != null && tbSystem.exists()) {
            TbFile propsFile = tbSystem.open(DEPLOYMENT_PROPERTIES_NAME);
            if (propsFile.exists()) {
                try (InputStream istream = propsFile.openFileInputStream();
                ) {
                    props.load(istream);
                    istream.close();
                } catch (Exception e) { //Catch and ignore exception if any
                    System.err.println("Ignoring error: " + e.getMessage());
                }
            }
        }

        return props;
    }

    /**
     * Loads information about the previous deployment from the Talking Book's file system.
     * If the project name can't be determined, use the fallback name.
     *
     */
    public DeploymentInfo createDeploymentInfo(String fallbackProjectName) {
        DeploymentInfo deploymentInfo = null;

        if (tbRoot == null) {
            return null;
        }

        try {
            String  community = getCommunityName();

            String firmwareVersion = getFirmwareVersion();

            // Previous image or package. Displays as "Content"
            // TODO: package vs image - consistency in nomenclature.
            String pkg = getPackageName();

            // Previous deployment name. Displays as "Update"
            String depl = getDeploymentName();

            // Last updated date. Displays as "First Rotation Date".
            String lastSynchDir = getSynchDir();
            String lastUpdate = getLastUpdateDate(lastSynchDir);

            // Project previously deployed to Talking Book, if known.
            String oldProject = getProjectName();

            DeploymentInfo.DeploymentInfoBuilder builder = new DeploymentInfo.DeploymentInfoBuilder()
                    .withSerialNumber(serialNumber)
                    .withProjectName(oldProject)
                    .withDeploymentName(depl)
                    .withPackageName(pkg)
                    .withUpdateDirectory(lastSynchDir)
                    .withUpdateTimestamp(lastUpdate)
                    .withFirmwareRevision(firmwareVersion)
                    .withCommunity(community)
                    .withFallbackProjectName(fallbackProjectName)
                    .asTestDeployment(isTestDeployment());
            deploymentInfo = builder.build();

        } catch (Exception ignore) {
            LOG.log(Level.WARNING, "TBL!: exception - ignore and keep going with empty strings", ignore);
        }

        return deploymentInfo;
    }

    /**
     * Look in the tbstats structure for updateDate. If none, parse the synchDir, if there is one.
     *
     * @return The last sync date, or UNKNOWN if not available.
     */
    private String getLastUpdateDate(String synchDir) {
        String lastUpdate = UNKNOWN;

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

    public String getDescription() {
        DeploymentInfo info = createDeploymentInfo("");
        if (info != null) {
            return info.toString();
        }
        if (label.isEmpty()) {
            return tbRoot.toString();
        }
        return label;
    }
}
