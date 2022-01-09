package org.literacybridge.core.tbdevice;

import org.literacybridge.core.fs.TbFile;
import org.literacybridge.core.tbloader.DeploymentInfo;
import org.literacybridge.core.tbloader.TBLoaderConstants;
import org.literacybridge.core.tbloader.TbFlashData;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.literacybridge.core.tbloader.TBLoaderConstants.COMMUNITY_PROPERTY;
import static org.literacybridge.core.tbloader.TBLoaderConstants.DEPLOYMENT_PROPERTIES_NAME;
import static org.literacybridge.core.tbloader.TBLoaderConstants.DEPLOYMENT_PROPERTY;
import static org.literacybridge.core.tbloader.TBLoaderConstants.DEPLOYMENT_UUID_PROPERTY;
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
import static org.literacybridge.core.tbloader.TBLoaderConstants.USERNAME_PROPERTY;

public abstract class TbDeviceInfo {
    private static final Logger LOG = Logger.getLogger(TbDeviceInfo.class.getName());

    public enum DEVICE_VERSION {
        NONE,
        UNKNOWN,
        TBv1,
        TBv2
    }

    public static TbDeviceInfo getDeviceInfoFor(TbFile tbRoot,
        String label,
        String prefix) {
        TbDeviceInfo deviceInfo = null;
        switch (getDeviceVersion(tbRoot)) {
            case NONE:
                deviceInfo = new TbDeviceInfoNull();
                break;
            case UNKNOWN:
                deviceInfo = new TbDeviceInfoUnknown();
                break;
            case TBv1:
                deviceInfo = new TbDeviceInfoV1(tbRoot, label, prefix);
                break;
            case TBv2:
                deviceInfo = new TbDeviceInfoV2(tbRoot, label);
                break;
        }
        return deviceInfo;
    }

    protected final TbFile tbRoot;
    protected final String label;

    // Computed or cached properties.
    protected final TbFile tbSystem;
    protected Properties tbDeploymentProperties;
    protected boolean corrupted = false;

    // Properties that are read from the Talking Book and provided to clients of the class.
    protected String serialNumber = null;
    protected Boolean testDeployment = null;
    protected String projectName = null;
    protected String deploymentName = null;
    protected List<String> packageNames = new ArrayList<>();
    protected String communityName = null;


    @SuppressWarnings("unused")
    public static TbDeviceInfo getNullDeviceInfo() {
        return new TbDeviceInfoNull();
    }

    protected TbDeviceInfo(TbFile tbRoot, String label) {
        this.tbRoot = tbRoot;
        if (tbRoot == null) {
            tbSystem = null;
        } else {
            tbSystem = tbRoot.open(TB_SYSTEM_PATH);
            loadDeploymentProperties();
        }
        this.label = label == null ? "" : label.trim();
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

    public void setCorrupted() {
        this.corrupted = true;
    }

    public boolean isCorrupted() {
        return corrupted;
    }

    public abstract boolean isSerialNumberFormatGood(String srn);
    public abstract boolean newSerialNumberNeeded();

    /**
     * Tries to determine is a Talking Book is connected. Looks for key files in the TB file system.
     *
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

    public void setSerialNumber(String serialNumber) {
        throw new IllegalStateException("setSerialNumber not defined on this class.");
    }

    /**
     * Returns the cached TbInfo, if there is one, but doesn't try to read from the file system.
     *
     * @return The TbInfo, or null if there is one, or it hasn't been loaded yet.
     */
    public TbFlashData getFlashData() {
        return null;
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
            if (projectName.equalsIgnoreCase(TBLoaderConstants.UNKNOWN)) {

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
            if (deploymentName.equalsIgnoreCase(TBLoaderConstants.UNKNOWN)) {
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
            if (communityName.equalsIgnoreCase(TBLoaderConstants.UNKNOWN)) {
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
     *
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
     *
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
     *
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
     *
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
                LOG.log(Level.FINE,
                    String.format("TBL!: isTestDeployment: %b (%s)", testDeployment, testDeploymentStr));
            } else {
                LOG.log(Level.FINE,
                    String.format("TBL!: isTestDeployment: %b (no deployment.properties)", testDeployment));
            }
        }
        return testDeployment;
    }

    /**
     * If there is a "deployment.properties" file in the system directory, loads it.
     *
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
     */
    public DeploymentInfo createDeploymentInfo(String fallbackProjectName) {
        DeploymentInfo deploymentInfo = null;

        if (tbRoot == null) {
            return null;
        }

        try {
            String community = getCommunityName();
            String recipientid = getRecipientid();

            String firmwareVersion = getFirmwareVersion();

            // Previous image or package. Displays as "Content"
            // TODO: package vs image - consistency in nomenclature.
            List<String> pkgs = getPackageNames();

            // Previous deployment name. Displays as "Update"
            String depl = getDeploymentName();

            // Last updated date. Displays as "First Rotation Date".
            String lastSynchDir = getSynchDir();
            String lastUpdate = getLastUpdateDate();

            // Project previously deployed to Talking Book, if known.
            String oldProject = getProjectName();

            DeploymentInfo.DeploymentInfoBuilder builder = new DeploymentInfo.DeploymentInfoBuilder()
                .withSerialNumber(getSerialNumber())
                .withProjectName(oldProject)
                .withDeploymentName(depl)
                .withPackageNames(pkgs)
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
     * Tries to determine the names of the content package(s) (images) on this Talking Book.
     * <p>
     * Looks first in the deployment.properties file. If that is not found,
     * checks in tbstats structure for image name. If not found, will then
     * look in the TalkingBook's system directory for a file with a ".pkg" extension. If there is exactly
     * one such file, returns the file's name, sans extension.
     *
     * @return The a list of one or more package names, or "UNKNOWN" if it can not be determined.
     */
    private List<String> getPackageNames() {
        if (packageNames.size() == 0) {
            String packageNameProperty = getProperty(PACKAGE_PROPERTY);
            String src = "properties";

            // Get the package(s) from the properties, if we can.
            if (!packageNameProperty.equalsIgnoreCase(TBLoaderConstants.UNKNOWN)) {
                String[] packages = packageNameProperty.split(",");
                packageNames.addAll(Arrays.asList(packages));
            } else {
                // If we didn't have it in properties, look for the flash data or marker file(s).
                String[] files;

                if (getFlashData() != null && getFlashData().getImageName() != null
                    && !getFlashData().getImageName().equals("")) {
                    packageNames.add(getFlashData().getImageName());
                    src = "flash";
                } else if (tbSystem.exists()) {
                    files = tbSystem.list((parent, name) -> {
                        String lowercase = name.toLowerCase();
                        return lowercase.endsWith(".pkg");
                    });

                    if (files.length == 1) {
                        packageNames.add(files[0].substring(0, files[0].length() - 4));
                        src = "marker";
                    }
                } else {
                    packageNames.add(TBLoaderConstants.UNKNOWN);
                    src = "none";
                }
            }
            LOG.log(Level.FINE,
                String.format("TBL!: Got package name (%s) from %s.", String.join(",", packageNames), src));
        }
        return packageNames;
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

    /**
     * If there is a deployment.properties file (already loaded), and it has a value for the
     * desired property, return that value. Otherwise return the default value.
     *
     * @param name of the desired property.
     * @return the property, or the default value.
     */
    protected String getProperty(String name) {
        if (tbDeploymentProperties != null) {
            return tbDeploymentProperties.getProperty(name, TBLoaderConstants.UNKNOWN);
        }
        return TBLoaderConstants.UNKNOWN;
    }

    public abstract String getSerialNumber();

    protected abstract String getSynchDir();

    protected abstract String getLastUpdateDate();

    protected abstract String getFirmwareVersion();

    public abstract DEVICE_VERSION getDeviceVersion();


    // If all files in a group are found, consider it a match.
    private static final String[][] v1Files = new String[][]{{"config.txt", "profiles.txt"}};
    private static final String[][] v2Files = new String[][]{
        {"device_ID.txt", "firmware_ID.txt"},
        {"QC_Pass.txt", "bootcount.txt"}
    };

    /**
     * Determine if the device looks like a TBv1 or a TBv2.
     *
     * @param tbRoot Root directory of the device to be examined.
     * @return TBv2 if looks like a V2, TBv1 if looks like a V1, NONE otherwise.
     */
    public static DEVICE_VERSION getDeviceVersion(TbFile tbRoot) {
        TbFile tbSystem = tbRoot.open(TB_SYSTEM_PATH);
        for (String[] fnGroup : v1Files) {
            boolean found = true;
            for (String fn : fnGroup) {
                found &= tbSystem.open(fn).exists();                
            }
            if (found) return DEVICE_VERSION.TBv1;
        }
        for (String[] fnGroup : v2Files) {
            boolean found = true;
            for (String fn : fnGroup) {
                found &= tbSystem.open(fn).exists();
            }
            if (found) return DEVICE_VERSION.TBv2;
        }
        return DEVICE_VERSION.UNKNOWN;
    }

    /**
     * Looks in the TalkingBook's system directory for any files with a ".srn" extension whose name does not begin
     * with "-erase". If any such files are found, returns the name of the first one found (ie, one selected at random),
     * without the extension.
     * <p>
     * NOTE: this is a static function so that the Talking Book Connection manager can get the device serial
     * number quickly and cheaply (basically, without parsing the flash data).
     *
     * @param tbRoot The root of the Talking Book file system.
     * @return The file's name found (minus extension), or UNKNOWN
     */
    public static String getSerialNumberFromFileSystem(TbFile tbRoot) {
        String sn = TBLoaderConstants.UNKNOWN;
        if (tbRoot == null) {
            return sn;
        }
        TbFile tbSystem = tbRoot.open(TB_SYSTEM_PATH);
        Properties props = loadDeploymentProperties(tbSystem);

        sn = props.getProperty(TALKING_BOOK_ID_PROPERTY, TBLoaderConstants.UNKNOWN);

        if (sn.equalsIgnoreCase(TBLoaderConstants.UNKNOWN)) {
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
                    sn = TBLoaderConstants.UNKNOWN;
                }
                if (!sn.equals(TBLoaderConstants.UNKNOWN)) {
                    LOG.log(Level.FINE, "TBL!: No stats SRN. Found *.srn file:" + sn);
                } else {
                    LOG.log(Level.INFO, "TBL!: No stats SRN and no good *.srn file found.");
                }
            }
        }
        return sn;
    }


}
