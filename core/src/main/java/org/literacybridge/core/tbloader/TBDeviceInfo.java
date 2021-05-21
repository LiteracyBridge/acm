package org.literacybridge.core.tbloader;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.literacybridge.core.fs.TbFile;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.literacybridge.core.tbloader.TBLoaderConstants.BINARY_STATS_ALTERNATIVE_PATH;
import static org.literacybridge.core.tbloader.TBLoaderConstants.BINARY_STATS_PATH;
import static org.literacybridge.core.tbloader.TBLoaderConstants.COMMUNITY_PROPERTY;
import static org.literacybridge.core.tbloader.TBLoaderConstants.DEPLOYMENT_PROPERTIES_NAME;
import static org.literacybridge.core.tbloader.TBLoaderConstants.DEPLOYMENT_PROPERTY;
import static org.literacybridge.core.tbloader.TBLoaderConstants.DEPLOYMENT_UUID_PROPERTY;
import static org.literacybridge.core.tbloader.TBLoaderConstants.NEED_SERIAL_NUMBER;
import static org.literacybridge.core.tbloader.TBLoaderConstants.NO_DRIVE;
import static org.literacybridge.core.tbloader.TBLoaderConstants.PACKAGE_PROPERTY;
import static org.literacybridge.core.tbloader.TBLoaderConstants.PROJECT_FILE_EXTENSION;
import static org.literacybridge.core.tbloader.TBLoaderConstants.PROJECT_PROPERTY;
import static org.literacybridge.core.tbloader.TBLoaderConstants.RECIPIENTID_PROPERTY;
import static org.literacybridge.core.tbloader.TBLoaderConstants.TALKING_BOOK_ID_PROPERTY;
import static org.literacybridge.core.tbloader.TBLoaderConstants.TBCDID_PROPERTY;
import static org.literacybridge.core.tbloader.TBLoaderConstants.TB_AUDIO_PATH;
import static org.literacybridge.core.tbloader.TBLoaderConstants.TB_LANGUAGES_PATH;
import static org.literacybridge.core.tbloader.TBLoaderConstants.TB_LISTS_PATH;
import static org.literacybridge.core.tbloader.TBLoaderConstants.TB_SYSTEM_PATH;
import static org.literacybridge.core.tbloader.TBLoaderConstants.TEST_DEPLOYMENT_PROPERTY;
import static org.literacybridge.core.tbloader.TBLoaderConstants.UNKNOWN;
import static org.literacybridge.core.tbloader.TBLoaderConstants.USERNAME_PROPERTY;

public final class TBDeviceInfo {
    private static final Logger LOG = Logger.getLogger(TBDeviceInfo.class.getName());

    private final TbFile tbRoot;
    private final String label;
    private final String tbPrefix;

    // Computed or cached properties.
    private final TbFile tbSystem;
    private final TbFlashData tbFlashData;
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


    @SuppressWarnings("unused")
    public static TBDeviceInfo getNullDeviceInfo() {
        return new TBDeviceInfo(NO_DRIVE, null);
    }

    public TBDeviceInfo(TbFile tbRoot, String label, String prefix) {
        if (tbRoot == null) {
            throw new IllegalArgumentException("Root may not be null");
        }
        this.tbRoot = tbRoot;
        this.label = label==null?"":label.trim();
        tbPrefix = prefix;
        tbSystem = tbRoot.open(TB_SYSTEM_PATH);
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
    @SuppressWarnings("unused")
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

        return tbRoot.open(TB_SYSTEM_PATH).exists()
                && tbRoot.open(TB_LANGUAGES_PATH).exists()
                && tbRoot.open(TB_LISTS_PATH).exists()
                && tbRoot.open(TB_AUDIO_PATH).exists();
    }

    /**
     * Reads and parses the flashData.bin file from a Talking Book.
     * @return A TbFlashData structure from the Talking Book, or null if one can't be read.
     */
    private static TbFlashData loadFlashData(TbFile tbRoot) {
        TbFlashData tbFlashData = null;
        try {
            TbFile flashDataBin = tbRoot.open(BINARY_STATS_PATH);
            if (flashDataBin.exists()) {
                tbFlashData = new TbFlashData(flashDataBin);
            } else {
                flashDataBin = tbRoot.open(BINARY_STATS_ALTERNATIVE_PATH);
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
        if (!TBLoaderUtils.isSerialNumberFormatGood2(serialNumber)) {
            // We will allocate a new-style serial number before we update the tbDeviceInfo.
            serialNumber = NEED_SERIAL_NUMBER;
        }
        needNewSerialNumber = TBLoaderUtils.newSerialNumberNeeded(tbPrefix, serialNumber);
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
     * Looks in several places for the Talking Book ID, aka Serial Number.
     *
     * Lookes in deployment.properties, in the flash data, and for a file named *.srn.
     *
     * @return The the first serial number found.
     */
    public String getSerialNumber() {
        if (serialNumber == null) {
            serialNumber = getProperty(TALKING_BOOK_ID_PROPERTY);
            String src = "properties";

            // If we didn't have it in properties, look for the flash data or marker file(s).
            if (serialNumber.equalsIgnoreCase(UNKNOWN)) {
                if (getFlashData() != null
                    && TBLoaderUtils.isSerialNumberFormatGood(tbPrefix, getFlashData().getSerialNumber())) {
                    serialNumber = getFlashData().getSerialNumber();
                    src = "flash";
                } else {
                    serialNumber = getSerialNumberFromFileSystem(tbRoot);
                    src = "marker";
                }
            } else {
                if (getFlashData() != null
                    && TBLoaderUtils.isSerialNumberFormatGood(tbPrefix, getFlashData().getSerialNumber())) {
                    String flashSerialNumber = getFlashData().getSerialNumber();
                    if (TBLoaderUtils.isSerialNumberFormatGood2(serialNumber) &&
                        TBLoaderUtils.isSerialNumberFormatGood2(flashSerialNumber) &&
                        !serialNumber.equalsIgnoreCase(flashSerialNumber)) {
                        // Flash and properties mismatch. Do not trust either.
                        serialNumber = NEED_SERIAL_NUMBER;
                    }
                }
            }
            serialNumber = serialNumber.toUpperCase();
            LOG.log(Level.FINE, String.format("TBL!: Got serial number (%s) from %s.", serialNumber, src));
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
     *
     * @param tbRoot The root of the Talking Book file system.
     * @return The file's name found (minus extension), or UNKNOWN
     */
    public static String getSerialNumberFromFileSystem(TbFile tbRoot) {
        String sn = UNKNOWN;
        if (tbRoot == null) {
            return sn;
        }
        TbFile tbSystem = tbRoot.open(TB_SYSTEM_PATH);
        Properties props = loadDeploymentProperties(tbSystem);

        sn = props.getProperty(TALKING_BOOK_ID_PROPERTY, UNKNOWN);

        if (sn.equalsIgnoreCase(UNKNOWN)) {
            String[] files;

            if (tbSystem.exists()) {
                files = tbSystem.list((parent, name) -> {
                    String lowercase = name.toLowerCase();
                    return lowercase.endsWith(".srn") && !lowercase.startsWith("-erase");
                });
                if (files.length > 0) {
                    String tsnFileName = files[0];
                    sn = tsnFileName.substring(0, tsnFileName.length() - 4);
                }
                if (sn.equals("")) { // Can only happen if file is named ".srn"
                    sn = UNKNOWN;
                }
                if (!sn.equals(UNKNOWN)) {
                    LOG.log(Level.FINE, "TBL!: No stats SRN. Found *.srn file:" + sn);
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
            projectName = getProperty(PROJECT_PROPERTY);
            String src = "properties";

            // If we didn't have it in properties, look for marker file(s).
            if (projectName.equalsIgnoreCase(UNKNOWN)) {

                String[] prjFiles;
                if (tbSystem.exists()) {
                    prjFiles = tbSystem.list((parent, name) -> {
                        String lowercase = name.toLowerCase();
                        return lowercase.endsWith(PROJECT_FILE_EXTENSION);
                    });
                    // Pick one at random, get the name, drop the extension.
                    if (prjFiles.length > 0) {
                        String fn = prjFiles[0];
                        projectName = fn.substring(0, fn.length() - PROJECT_FILE_EXTENSION.length());
                        src = "marker";
                    }
                }
            }
            LOG.log(Level.FINE, String.format("TBL!: Got project name (%s) from %s.", projectName, src));
        }
        return projectName;
    }

    /**
     * Looks in the TalkingBook's system directory for files with a ".rev" or ".img" extension. If
     * exactly one *.rev file exists, the basename is returned as the revision. If no .rev files,
     * but there is exactly one *.img file, that basename is returned as the revision.
     *
     * @return The file's name found (minus extension), or UNKNOWN if no file found, or if the file name consists
     * only of the extension (eg, a file named ".img" will return UNKNOWN).
     */
    private String getFirmwareVersion() {
        String rev = UNKNOWN;

        if (tbSystem.exists()) {
            String[] revNames = tbSystem.list((dir, name) -> name.length() > 4 && (name.toLowerCase().endsWith(".rev")));
            if (revNames.length == 1) {
                rev = FilenameUtils.removeExtension(revNames[0]).toLowerCase();
            } else if (revNames.length == 0) {
                String[] imgNames = tbSystem.list((dir, name) -> name.length() > 4 && (name.toLowerCase().endsWith(".img")));
                if (imgNames.length == 1) {
                    rev = FilenameUtils.removeExtension(imgNames[0]).toLowerCase();
                }
            }
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
            packageName = getProperty(PACKAGE_PROPERTY);
            String src = "properties";

            // If we didn't have it in properties, look for the flash data or marker file(s).
            if (packageName.equalsIgnoreCase(UNKNOWN)) {
                String[] files;

                if (getFlashData() != null && getFlashData().getImageName() != null
                    && !getFlashData().getImageName().equals("")) {
                    packageName = getFlashData().getImageName();
                    src = "flash";
                } else if (tbSystem.exists()) {
                    files = tbSystem.list((parent, name) -> {
                        String lowercase = name.toLowerCase();
                        return lowercase.endsWith(".pkg");
                    });

                    if (files.length == 1) {
                        packageName = files[0].substring(0, files[0].length() - 4);
                        src = "marker";
                    }
                }
            }
            LOG.log(Level.FINE, String.format("TBL!: Got package name (%s) from %s.", packageName, src));
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
    public String getDeploymentName() {
        if (deploymentName == null) {
            deploymentName = getProperty(DEPLOYMENT_PROPERTY);
            String src = "properties";

            // If we didn't have it in properties, look for the flash data or marker file(s).
            if (deploymentName.equalsIgnoreCase(UNKNOWN)) {
                String[] files;

                if (getFlashData() != null && getFlashData().getDeploymentNumber() != null
                    && !getFlashData().getDeploymentNumber().equals("")) {
                    deploymentName = getFlashData().getDeploymentNumber();
                    src = "flash";
                } else if (tbSystem.exists()) {
                    files = tbSystem.list((parent, name) -> {
                        String lowercase = name.toLowerCase();
                        return lowercase.endsWith(".dep");
                    });
                    if (files.length == 1) {
                        deploymentName = files[0].substring(0, files[0].length() - 4);
                        src = "marker";
                    }
                }
            }
            LOG.log(Level.FINE, String.format("TBL!: Got deployment name (%s) from %s.", deploymentName, src));
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
            } catch (Exception e) { //Catch and ignore exception if any
                LOG.log(Level.WARNING, "Ignoring error: ", e.getMessage());
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
            communityName = getProperty(COMMUNITY_PROPERTY);
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
                        files = tbRoot.list((parent, name) -> {
                            String lowercase = name.toLowerCase();
                            return lowercase.endsWith(".loc");
                        });
                        if (files == null) {
                            LOG.log(Level.INFO,
                                "TBL!: This does not look like a TB: " + tbRoot);

                        } else if (files.length == 1) {
                            String locFileName = files[0];
                            communityName = locFileName.substring(0, locFileName.length() - 4);
                            src = "root marker";
                        } else if (files.length == 0 && tbSystem.exists()) {
                            // get Location file info
                            files = tbSystem.list((parent, name) -> {
                                String lowercase = name.toLowerCase();
                                return lowercase.endsWith(".loc");
                            });
                            if (files.length == 1) {
                                String locFileName = files[0];
                                communityName = locFileName.substring(0, locFileName.length() - 4);
                                src = "system marker";
                            }
                        }
                    } catch (Exception ex) {
                        LOG.log(Level.WARNING, "TBL!: Exception while reading community", ex);
                        // ignore and keep going with empty string
                        src = "nowhere";
                    }
                }
            }
            LOG.log(Level.FINE, String.format("TBL!: Got community name (%s) from %s.", communityName, src));
        }
        return communityName;
    }

    /**
     * Reads the recipient id from the deployment properties file. There is no other location
     * for this datum.
     * @return The recipient id, or null if it can not be determines.
     */
    public String getRecipientid() {
        String recipientid = null;
        Properties properties = loadDeploymentProperties();
        if (properties != null) {
            recipientid = properties.getProperty(RECIPIENTID_PROPERTY, null);
            LOG.log(Level.FINE, String.format("TBL!: recipientid: %s", recipientid));
        } else {
            LOG.log(Level.FINE, "TBL!: recipientid: (null) (no deployment.properties)");
        }

        return recipientid;
    }

    /**
     * Reads the deployment UUID from the deployment properties file. There is no other location
     * for this datum. Note that this is the deployment UUID assigned at the time of the most
     * recent previous Deployment.
     * @return The deployment UUID, or null if it can not be determines.
     */
    public String getDeploymentUUID() {
        String uuid = null;
        Properties properties = loadDeploymentProperties();
        if (properties != null) {
            uuid = properties.getProperty(DEPLOYMENT_UUID_PROPERTY, null);
            LOG.log(Level.FINE, String.format("TBL!: deployment UUID: %s", uuid));
        } else {
            LOG.log(Level.FINE, "TBL!: deployment UUID: (null) (no deployment.properties)");
        }

        return uuid;
    }

    /**
     * Reads the deploying user's name from the deployment properties file. There is no other
     * location for this datum. Note that this is the user who performed the most
     * recent previous Deployment.
     * @return The deploying user's name, or null if it can not be determines.
     */
    @SuppressWarnings("unused")
    public String getDeploymentUsername() {
        String username = null;
        Properties properties = loadDeploymentProperties();
        if (properties != null) {
            username = properties.getProperty(USERNAME_PROPERTY, null);
            LOG.log(Level.FINE, String.format("TBL!: deployment username: %s", username));
        } else {
            LOG.log(Level.FINE, "TBL!: deployment username: (null) (no deployment.properties)");
        }

        return username;
    }

    /**
     * Reads the deployment tbcdid from the deployment properties file. There is no other location
     * for this datum. Note that this is the tbcd id of the laptop/phone that performed the most
     * recent previous Deployment.
     * @return The deployment tbcd id, or null if it can not be determines.
     */
    @SuppressWarnings("unused")
    public String getDeploymentTbcdid() {
        String tbcdid = null;
        Properties properties = loadDeploymentProperties();
        if (properties != null) {
            tbcdid = properties.getProperty(TBCDID_PROPERTY, null);
            LOG.log(Level.FINE, String.format("TBL!: deployment tbcdid: %s", tbcdid));
        } else {
            LOG.log(Level.FINE, "TBL!: deployment tbcdid: (null) (no deployment.properties)");
        }

        return tbcdid;
    }

    public boolean isTestDeployment() {
        if (testDeployment == null) {
            testDeployment = false;
            Properties properties = loadDeploymentProperties();
            if (properties != null) {
                String testDeploymentStr = properties.getProperty(TEST_DEPLOYMENT_PROPERTY, Boolean.FALSE.toString());
                testDeployment = Boolean.parseBoolean(testDeploymentStr);
                LOG.log(Level.FINE, String.format("TBL!: isTestDeployment: %b (%s)", testDeployment, testDeploymentStr));
            } else {
                LOG.log(Level.FINE, String.format("TBL!: isTestDeployment: %b (no deployment.properties)", testDeployment));
            }
        }
        return testDeployment;
    }

    /**
     * If there is a deployment.properties file (already loaded), and it has a value for the
     * desired property, return that value. Otherwise return the default value.
     * @param name of the desired property.
     * @return the property, or the default value.
     */
    private String getProperty(String name) {
        if (tbDeploymentProperties != null) {
            return tbDeploymentProperties.getProperty(name, TBLoaderConstants.UNKNOWN);
        }
        return TBLoaderConstants.UNKNOWN;
    }

    /**
     * If there is a "deployment.properties" file in the system directory, loads it.
     * @return The Properties, or null if not found or unreadable.
     */
    public Properties loadDeploymentProperties() {
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
                try (InputStream istream = propsFile.openFileInputStream()) {
                    props.load(istream);
                } catch (Exception e) { //Catch and ignore exception if any
                    LOG.log(Level.WARNING, "Ignoring error: ", e.getMessage());
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
            String recipientid = getRecipientid();

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
                    .withRecipientid(recipientid)
                    .withFallbackProjectName(fallbackProjectName)
                    .asTestDeployment(isTestDeployment());
            deploymentInfo = builder.build();

        } catch (Exception ex) {
            LOG.log(Level.WARNING, "TBL!: exception - ignore and keep going with empty strings", ex);
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
