package org.literacybridge.acm.tbloader;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.kohsuke.args4j.CmdLineParser;
import org.literacybridge.acm.Constants;
import org.literacybridge.acm.cloud.Authenticator;
import org.literacybridge.acm.cloud.TbSrnHelper;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.gui.Application;
import org.literacybridge.acm.gui.dialogs.PopUp;
import org.literacybridge.acm.utils.LogHelper;
import org.literacybridge.acm.utils.OsUtils;
import org.literacybridge.acm.utils.SwingUtils;
import org.literacybridge.core.OSChecker;
import org.literacybridge.core.fs.FsFile;
import org.literacybridge.core.fs.OperationLog;
import org.literacybridge.core.fs.TbFile;
import org.literacybridge.core.spec.ProgramSpec;
import org.literacybridge.core.spec.Recipient;
import org.literacybridge.core.tbloader.DeploymentInfo;
import org.literacybridge.core.tbloader.TBDeviceInfo;
import org.literacybridge.core.tbloader.TBLoaderConfig;
import org.literacybridge.core.tbloader.TBLoaderConstants;
import org.literacybridge.core.tbloader.TBLoaderCore;
import org.literacybridge.core.tbloader.TBLoaderUtils;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.literacybridge.acm.Constants.TBLoadersLogDir;
import static org.literacybridge.acm.Constants.TbCollectionWorkDir;
import static org.literacybridge.acm.cloud.Authenticator.LoginOptions.CHOOSE_PROGRAM;
import static org.literacybridge.acm.cloud.Authenticator.LoginOptions.OFFLINE_EMAIL_CHOICE;
import static org.literacybridge.core.tbloader.TBLoaderConstants.DEPLOYMENT_NUMBER;
import static org.literacybridge.core.tbloader.TBLoaderConstants.ISO8601;
import static org.literacybridge.core.tbloader.TBLoaderUtils.getImageForCommunity;
import static org.literacybridge.core.tbloader.TBLoaderUtils.getPackagesInDeployment;

@SuppressWarnings({"ResultOfMethodCallIgnored", "ConstantConditions" })
public class TBLoader extends JFrame {
    private static final Logger LOG = Logger.getLogger(TBLoader.class.getName());

    private static TBLoader tbLoader;
    private File deploymentDir;

    public static TBLoader getApplication() {
        return tbLoader;
    }

    static final String OLD_TBS_PREFIX = "A-";
    private static final String NEW_TBS_PREFIX = "B-";

    public enum TB_ID_STRATEGY {
        AUTOMATIC("A new Talking Book ID is allocated ony when needed."),
        MANUAL("The user can also request a new Talking Book ID."),
        AUTOMATIC_ON_PROGRAM_CHANGE("The Talking Book is assigned a new ID when moving between programs."),
        MANUAL_ON_PROGRAM_CHANGE("New Talking Book ID when needed, on request, or for new programs.");

        String description;
        TB_ID_STRATEGY(String description) {
            this.description = description;
        }
        boolean allowsManual() {
            return this==MANUAL || this==MANUAL_ON_PROGRAM_CHANGE;
        }
        boolean onProgramChange() {
            return this==AUTOMATIC_ON_PROGRAM_CHANGE || this==MANUAL_ON_PROGRAM_CHANGE;
        }
    }
    TB_ID_STRATEGY tbIdStrategy = TB_ID_STRATEGY.AUTOMATIC;

    public enum TEST_DEPLOYMENT_STRATEGY {
        DEFAULT_OFF("'Test' is off unless explicitly turned on."),
        RETAIN("The value from the Talking Book is retained."),
        DEFAULT_ON("'Test' is on unless explicitly turned off.");

        String description;
        TEST_DEPLOYMENT_STRATEGY(String description) {
            this.description = description;
        }
        boolean initFromTb(boolean tbValue) {
            if (this == DEFAULT_OFF)
                return false;
            else if (this == DEFAULT_ON)
                return true;
            else // MAINTAIN
                return tbValue;
        }
    }
    TEST_DEPLOYMENT_STRATEGY testStrategy = TEST_DEPLOYMENT_STRATEGY.DEFAULT_OFF;

    private final JFrame applicationWindow;

    private String currentTbFirmware;
    private String newTbFirmware;


    private String currentTbSrn;
    private String newTbSrn;
    private boolean isNewSerialNumber;

    private TBDeviceInfo currentTbDevice;
    private final String srnPrefix;
    private String newProject;

    // Deployment info read from Talking Book.
    private DeploymentInfo oldDeploymentInfo;

    private TbFile softwareDir;
    private String deviceIdHex;
    private String userEmail;
    private String userName;
    private File collectionWorkDir;
    private File uploadQueueDeviceDir;
    private File uploadQueueDir;
    private File uploadQueueCDDir;
    private TbFile temporaryDir;

    // All content is relative to this.
    private File localTbLoaderDir;
    private File logsDir;

    private final Set<String> acceptableFirmwareVersions = new HashSet<>();
    private TbLoaderPanel tbLoaderPanel;

    // Metadata about the project. Optional, may be null.
    private ProgramSpec programSpec = null;
    private DeploymentChooser deploymentChooser;
    private FsRootMonitor fsRootMonitor;
    private StatisticsUploader statisticsUploader;

    ProgramSpec getProgramSpec() {
        if (programSpec == null) {
            try {
                File programspecDir = new File(localTbLoaderDir,
                    TBLoaderConstants.CONTENT_SUBDIR + File.separator + deploymentChooser.getNewDeployment() + File.separator + Constants.ProgramSpecDir);
                programSpec = new ProgramSpec(programspecDir);
            } catch (Exception ignored) {
                // leave null
            }
        }
        return programSpec;
    }

    String getProgram() {
        return newProject;
    }
    
    File getLocalTbLoaderDir() {
        return localTbLoaderDir;
    }

    // Options.
    @SuppressWarnings("unused")
    private boolean allowPackageChoice;

    static class WindowEventHandler extends WindowAdapter {
        @Override
        public void windowClosing(WindowEvent evt) {
            OperationLog.log("TbLoaderShutdown").finish();
            LOG.log(Level.INFO, "closing app");
            System.exit(0);
        }
    }

    private boolean startUpDone = false;
    private boolean refreshingDriveInfo = false;
    private boolean updatingTB = false;

    public static void main(String[] args) throws Exception {
        System.out.println("starting main()");

        TbLoaderArgs tbArgs = new TbLoaderArgs();
        CmdLineParser parser = new CmdLineParser(tbArgs);
        try {
            parser.parseArgument(args);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            System.err.println(
                "java -cp acm.jar;lib/* org.literacybridge.acm.tbloader.TBLoader [options...]");
            parser.printUsage(System.err);
            System.exit(100);
        }

        tbLoader = new TBLoader(tbArgs);
        tbLoader.runApplication();
    }

    private TBLoader(TbLoaderArgs tbArgs) {
        // String project, String srnPrefix) {
        this.newProject = ACMConfiguration.cannonicalProjectName(tbArgs.project);

        applicationWindow = this;

        if (tbArgs.srnPrefix != null && TBLoaderConstants.VALID_SRN_PREFIXES.contains(tbArgs.srnPrefix.toUpperCase())) {
            srnPrefix = tbArgs.srnPrefix.toUpperCase();
        } else if (tbArgs.oldTbs) {
            srnPrefix = TBLoaderConstants.OLD_TB_SRN_PREFIX;
        } else {
            srnPrefix = TBLoaderConstants.NEW_TB_SRN_PREFIX; // for latest Talking Book hardware
        }

        this.allowPackageChoice = tbArgs.choices;
    }

    private void runApplication() throws Exception {
        long startupTimer = -System.currentTimeMillis();

        OsUtils.enableOSXQuitStrategy();
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        this.addWindowListener(new WindowEventHandler());
        this.backgroundColor = getBackground();
        SwingUtils.setLookAndFeel("seaglass");

        boolean oldTbs = srnPrefix.equalsIgnoreCase(TBLoaderConstants.OLD_TB_SRN_PREFIX);
        String iconName = oldTbs ? "/tb_loader-OLD-TBs.png" : "/tb_loader.png";
        URL iconURL = Application.class.getResource(iconName);
        Image iconImage = new ImageIcon(iconURL).getImage();
        if (OsUtils.MAC_OS) {
            OsUtils.setOSXApplicationIcon(iconImage);
        } else {
            applicationWindow.setIconImage(iconImage);
        }

        // Don't count authentication time in startup -- user wait time.
        startupTimer += System.currentTimeMillis();
        authenticate();
        startupTimer -= System.currentTimeMillis();

        // Set options that are controlled by project config file.
        System.out.printf("Starting TB-Loader for %s\n", newProject);
        loadConfiguration();

        setDeviceIdAndPaths();

        // Initialized java logging, as well as operational logging.
        initializeLogging(logsDir);
        OperationLog.log("WindowsTBLoaderStart")
            .put("tbcdid", deviceIdHex)
            .put("project", newProject)
            .finish();

        // Looks in Dropbox and in ~/LiteracyBridge for deployments. May prompt user for which
        // Deployment version, or update to latest.
        deploymentChooser = new DeploymentChooser(this);
        deploymentChooser.select();

        initializeProgramSpec();

        initializeGui();

//        JOptionPane.showMessageDialog(applicationWindow,
//                                      "Remember to power Talking Book with batteries before connecting with USB.",
//                                      "Use Batteries!", JOptionPane.PLAIN_MESSAGE);
        LOG.log(Level.INFO, "set visibility - starting drive monitoring");

        fsRootMonitor = new FsRootMonitor(this::rootsHandler);
        fsRootMonitor.start();

        startUpDone = true;
        setEnabledStates();

        startupTimer += System.currentTimeMillis();
        System.out.printf("Startup in %d ms.\n", startupTimer);

        statisticsUploader = new StatisticsUploader(this,
            collectionWorkDir,
            uploadQueueDir,
            uploadQueueCDDir,
            uploadQueueDeviceDir);
        statisticsUploader.zipAndUpload();
    }

    public FsRootMonitor getFsRootMonitor() {
        return fsRootMonitor;
    }

    /**
     * Authenticate the user, to prepare for cloud access.
     */
    private void authenticate() {
        Authenticator authInstance = Authenticator.getInstance();
        authInstance.setLocallyAvailablePrograms(DeploymentsManager.getLocalPrograms());
        Authenticator.LoginResult result = authInstance.getUserIdentity(this, "TB-Loader", newProject, OFFLINE_EMAIL_CHOICE, CHOOSE_PROGRAM);
        if (result == Authenticator.LoginResult.FAILURE) {
            JOptionPane.showMessageDialog(this,
                "Authentication is required to use the TB-Loader.",
                "Authentication Failure",
                JOptionPane.ERROR_MESSAGE);
            System.exit(13);
        }

        TbSrnHelper srnHelper = authInstance.getTbSrnHelper();
        srnHelper.prepareForAllocation();
        newProject = authInstance.getUserProgram();
    }

    /**
     * Reads the TB-Loader id of this computer.
     * Determines where files come from, and where they go.
     *
     * @throws IOException if any file errors.
     */
    private void setDeviceIdAndPaths() throws IOException {
        try {
            TbSrnHelper srnHelper = Authenticator.getInstance().getTbSrnHelper();
            deviceIdHex = srnHelper.getTbSrnAllocationInfo().getTbloaderidHex();
            userEmail = Authenticator.getInstance().getUserEmail();
            userName = Authenticator.getInstance().getUserName();

            localTbLoaderDir = ACMConfiguration.getInstance().getLocalTbLoaderDirFor(newProject);
            localTbLoaderDir.mkdirs();
            softwareDir = new FsFile(ACMConfiguration.getInstance().getSoftwareDir());

            File appHome = ACMConfiguration.getInstance().getApplicationHomeDirectory();
            collectionWorkDir = new File(appHome, TbCollectionWorkDir);
            uploadQueueDir = new File(appHome, Constants.uploadQueue);
            uploadQueueCDDir = new File(uploadQueueDir, "collected-data");
            uploadQueueDeviceDir = new File(uploadQueueCDDir,"tbcd" + deviceIdHex);

            logsDir = new File(ACMConfiguration.getInstance().getApplicationHomeDirectory(),
                Constants.TBLoadersHomeDir + File.separator + TBLoadersLogDir);
            logsDir.mkdirs();

            temporaryDir = new FsFile(Files.createTempDirectory("tbloader-tmp").toFile());
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Exception while setting DeviceId and paths", e);
            throw e;
        }
    }


    private void initializeLogging(File logsDir) throws IOException {
        // Set up the program log. For debugging the execution of the TBLoader application.

        new LogHelper().inDirectory(logsDir).absolute().withName("tbloaderlog.%g").initialize();
        LOG.log(Level.INFO, "WindowsTBLoaderStart\n");

        // Set up the operation log. Tracks what is done, by whom.
        OperationLogImpl opLogImpl = new OperationLogImpl(logsDir);
        OperationLog.setImplementation(opLogImpl);
    }

    private void initializeProgramSpec() {
        deploymentDir = new File(localTbLoaderDir,
                TBLoaderConstants.CONTENT_SUBDIR + File.separator + deploymentChooser.getNewDeployment());

        File programspecDir = new File(deploymentDir, Constants.ProgramSpecDir);
        programSpec = new ProgramSpec(programspecDir);
        String acceptables = programSpec.getDeploymentProperties().getProperty(
            TBLoaderConstants.ACCEPTABLE_FIRMWARE_VERSIONS);
        if (StringUtils.isNotBlank(acceptables)) {
            acceptableFirmwareVersions.addAll(Arrays.stream(acceptables.split(","))
                                                    .map(String::trim)
                                                    .filter(StringUtils::isNotBlank)
                                                    .collect(Collectors.toList()));
        }
    }

    private void initializeGui() {
        setTitle(String.format("TB-Loader %s", newProject));

        String[] packagesInDeployment = null;
        Properties deploymentProperties = getProgramSpec().getDeploymentProperties();
        allowPackageChoice = allowPackageChoice || deploymentProperties.size()==0;
        packagesInDeployment = getPackagesInDeployment(deploymentDir);

        TbLoaderPanel.Builder builder = new TbLoaderPanel.Builder()
            .withProgramSpec(programSpec)
            .withPackagesInDeployment(packagesInDeployment)
            .withSettingsClickedListener(TblSettingsDialog::showDialog)
            .withGoListener(this::onTbLoaderGo)
            .withRecipientListener(this::onRecipientSelected)
            .withDeviceListener(this::onDeviceSelected)
            .withForceFirmwareListener(this::onForceFirmwareChanged)
            .withForceSrnListener(this::onForceSrnChanged)
            .withTbIdStrategy(tbIdStrategy)
            .withAllowPackageChoice(allowPackageChoice);

        tbLoaderPanel = builder.build();
        tbLoaderPanel.setEnabled(false);

        // Make Old-Talking-Book-Mode really obvious.
        if (srnPrefix.equalsIgnoreCase(TBLoaderConstants.OLD_TB_SRN_PREFIX)) {
            tbLoaderPanel.setBackground(Color.CYAN);
        }

        setSize(800, 700);
        add(tbLoaderPanel, BorderLayout.CENTER);
        setLocationRelativeTo(null);

        // Populate various fields, choices.
        tbLoaderPanel.setNewDeployment(deploymentChooser.getDeploymentProvenance(getProgramSpec()));
        fillFirmwareVersion();

        setVisible(true);
    }

    // Configuration settings.
    void loadConfiguration() {
        Properties config = ACMConfiguration.getInstance().getConfigPropertiesFor(newProject);
        if (config != null) {
            String valStr = config.getProperty("PACKAGE_CHOICE", "FALSE");
            this.allowPackageChoice |= Boolean.parseBoolean(valStr);

            valStr = config.getProperty("ALLOW_FORCE_SRN", "FALSE");
            if (Boolean.parseBoolean(valStr)) {
                this.tbIdStrategy = TB_ID_STRATEGY.MANUAL;
            }
            valStr = config.getProperty("TB_ID_STRATEGY");
            if (valStr != null) {
                try {
                    this.tbIdStrategy = TB_ID_STRATEGY.valueOf(valStr);
                } catch(Exception ignored) { }
            }

            valStr = config.getProperty("TEST_DEPLOYMENT_STRATEGY");
            if (valStr != null) {
                try {
                    this.testStrategy = TEST_DEPLOYMENT_STRATEGY.valueOf(valStr);
                } catch(Exception ignored) { }
            }

        }
    }

    void setAllowPackageChoice(boolean allowPackageChoice) {
        if (this.allowPackageChoice != allowPackageChoice) {
            tbLoaderPanel.enablePackageChoice(allowPackageChoice);
        }
        this.allowPackageChoice = allowPackageChoice;
    }
    TB_ID_STRATEGY getTbIdStrategy() {
        return this.tbIdStrategy;
    }
    void setTbIdStrategy(int srnStrategyOrdinal) {
        setSrnStrategy(TB_ID_STRATEGY.values()[srnStrategyOrdinal]);
    }
    void setSrnStrategy(TB_ID_STRATEGY srnStrategy) {
        if (srnStrategy != this.tbIdStrategy) {
            tbLoaderPanel.setTbIdStrategy(srnStrategy);
        }
        this.tbIdStrategy = srnStrategy;
    }
    TEST_DEPLOYMENT_STRATEGY getTestStrategy() {
        return this.testStrategy;
    }
    void setTestStrategy(int testStrategyOrdinal) {
        setTestStrategy(TEST_DEPLOYMENT_STRATEGY.values()[testStrategyOrdinal]);
    }
    void setTestStrategy(TEST_DEPLOYMENT_STRATEGY testStrategy) {
        if (testStrategy != this.testStrategy) {
            if (oldDeploymentInfo != null) {
                tbLoaderPanel.setTestDeployment(testStrategy.initFromTb(oldDeploymentInfo.isTestDeployment()));
            } else {
                tbLoaderPanel.setTestDeployment(testStrategy == TEST_DEPLOYMENT_STRATEGY.DEFAULT_ON);
            }
        }
        this.testStrategy = testStrategy;
    }
    

    /**
     * Looks in the ~/LiteracyBridge/TB-Loaders/{project}/content/{deployment}/basic directory
     * for files named '*.img'. Any found are assumed to be firmware images for v1 talking books.
     * <p>
     * There *should* be exactly one (the TB-Builder should have selected the highest numbered one).
     * <p>
     * Populates the newFirmwareVersionText field.
     */
    private void fillFirmwareVersion() {
        newTbFirmware = "(No firmware)";

        // Like ~/LiteracyBridge/TB-Loaders/{project}/content/{deployment}/basic
        File basicContentPath = new File(localTbLoaderDir,
            TBLoaderConstants.CONTENT_SUBDIR + File.separator + deploymentChooser.getNewDeployment() + File.separator
                + TBLoaderConstants.CONTENT_BASIC_SUBDIR);
        LOG.log(Level.INFO, "DEPLOYMENT:" + deploymentChooser.getNewDeployment());
        try {
            File[] files;
            if (basicContentPath.exists()) {
                // get firmware for deployment
                files = basicContentPath.listFiles((dir, name) -> {
                    String lowercase = name.toLowerCase();
                    return lowercase.endsWith(".img");
                });
                if (files.length > 1) newTbFirmware = "(Multiple Firmwares!)";
                else if (files.length == 1) {
                    newTbFirmware = FilenameUtils.removeExtension(files[0].getName());
                    acceptableFirmwareVersions.add(newTbFirmware);
                }
                tbLoaderPanel.setNewFirmwareVersion(newTbFirmware);
            }
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "exception - ignore and keep going with default string", ex);
        }

    }

    private void setEnabledStates() {
        tbLoaderPanel.setEnabled(startUpDone && !refreshingDriveInfo && !updatingTB);
        fsRootMonitor.setEnabled(!updatingTB);
    }

    /**
     * Allocate a new TBLoaderConfig, with a new timestamp for the collected data.
     * @return the new TBLoaderConfig.
     */
    private TBLoaderConfig getTbLoaderConfig() {
        String collectionTimestamp = ISO8601.format(new Date());
        File collectedDataDirectory = new File(collectionWorkDir, collectionTimestamp);
        TbFile collectedDataTbFile = new FsFile(collectedDataDirectory);

        return new TBLoaderConfig.Builder().withTbLoaderId(deviceIdHex)
            .withCollectedDataDirectory(collectedDataTbFile)
            .withTempDirectory(temporaryDir)
            .withWindowsUtilsDirectory(softwareDir)
            .withUserEmail(userEmail)
            .withUserName(userName)
            .build();
    }

    /**
     * Update the status line for pending uploads. If no pending uploads, hides the status
     * line.
     * @param progress An UploadStatus object with the current status.
     */
    void updateUploadStatus(StatisticsUploader.UploadStatus progress) {
        if (tbLoaderPanel != null) {
            String status = null;
            if (progress.nFiles > 0) {
                status = String.format("%s in %d files waiting to be uploaded.",
                    TBLoaderUtils.getBytesString(progress.nBytes),
                    progress.nFiles);
            }
            tbLoaderPanel.setUploadStatus(status);
        }
    }


    private Color backgroundColor;

    @Override
    public void setBackground(Color bgColor) {
        // Workaround for weird bug in seaglass look&feel that causes a
        // java.awt.IllegalComponentStateException when e.g. a combo box
        // in this dialog is clicked on
        if (bgColor.getAlpha() == 0) {
            super.setBackground(backgroundColor);
        } else {
            super.setBackground(bgColor);
            backgroundColor = bgColor;
        }
    }

    /**
     * If user checks the option to allocate a new SRN, this will prompt them to consider their
     * choice. Switches the display between the old SRN and "-- to be assigned --".
     *
     * @param forceSrn is unused.
     */
    private void onForceSrnChanged(Boolean forceSrn) {
        if (forceSrn) {
            List<Object> options = new ArrayList<>();
            Object defaultOption;
            options.add(UIManager.getString("OptionPane.yesButtonText"));
            options.add(UIManager.getString("OptionPane.noButtonText"));
            defaultOption = UIManager.getString("OptionPane.noButtonText");

            String message = "Are you sure that you want to allocate a new Serial Number"
                + "\nfor this Talking Book? You should not do this unless you have"
                + "\na very good reason to believe that this SRN is duplicated on"
                + "\nmore than one Talking Book.";
            String title = "Really replace SRN?";

            int answer = JOptionPane.showOptionDialog(this,
                message,
                title,
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null,
                options.toArray(),
                defaultOption);
            if (answer == JOptionPane.YES_OPTION) {
                newTbSrn = TBLoaderConstants.NEED_SERIAL_NUMBER;
                tbLoaderPanel.setNewSrn(newTbSrn);
            } else {
                tbLoaderPanel.setForceTbId(false);
            }
        } else {
            newTbSrn = currentTbSrn;
            tbLoaderPanel.setNewSrn(newTbSrn);
        }
    }


    private void onForceFirmwareChanged(Boolean aBoolean) {
        setFirmwareLabel();
    }
    private void setCurrentTbFirmware(String prevFirmwareRevision) {
        if (currentTbFirmware==null || !currentTbFirmware.equals(prevFirmwareRevision)) {
            currentTbFirmware = prevFirmwareRevision;
            tbLoaderPanel.setOldFirmwareVersion(prevFirmwareRevision);
            setFirmwareLabel();
        }
    }
    private void setFirmwareLabel() {
        String firmwareLabel = "Firmware";
        if (StringUtils.isBlank(currentTbFirmware)) {
            firmwareLabel += ":";
        } else if (acceptableFirmwareVersions.contains(currentTbFirmware) && !tbLoaderPanel.isForceFirmware()) {
            firmwareLabel += " (keep):";
        } else {
            firmwareLabel += " (update):";
        }
        tbLoaderPanel.setFirmwareVersionLabel(firmwareLabel);
    }

    private File prevSelected = null;

    private String getRecipientIdForCommunity(String communityDirName) {
        // ~/LiteracyBridge/TB-Loaders/{project}/content/{deployment}/communities/{communitydir}
        File deploymentDirectory = new File(localTbLoaderDir,
            TBLoaderConstants.CONTENT_SUBDIR + File.separator + deploymentChooser.getNewDeployment());

        return TBLoaderUtils.getRecipientIdForCommunity(deploymentDirectory, communityDirName);
    }

    /**
     * Returns the directory for the selected community. If the community was selected via a
     * RecipientList, then the directory is looked up in the recipient map from the recipient id.
     *
     * If the community was selected via a list of directories, the directory is selected
     * directly.
     */
    private String getMappedDirectoryForRecipient(String recipientid) {
        if (StringUtils.isBlank(recipientid)) {
            return null;
        }
        // Selection by recipient hierarchy.
        Map<String,String> recipientMap = getProgramSpec().getRecipientsMap();
        return recipientMap.get(recipientid);

    }

    private String getNewPackage() {
        return tbLoaderPanel.getNewPackage();
    }

    /**
     * Allocates and persists the next serial number for this device (pc running TB-Loader).
     *
     * @return The next serial number.
     */
    private int allocateNextSerialNumberFromTbLoader() throws Exception {
        int serialnumber;
            serialnumber = Authenticator.getInstance().getTbSrnHelper().allocateNextSrn();
            if (serialnumber == 0) {
                throw new Exception("SRN out of bounds for this TB Loader device. (Too many assigned and can't allocate more. Are you offline?)");
            }
        return serialnumber;
    }

    FileSystemView fsView = FileSystemView.getFileSystemView();
    private void rootsHandler(List<File> files) {
        List<TBDeviceInfo> newList = new ArrayList<>();
        int index = -1;
        for (File root : files) {
            String label = fsView.getSystemDisplayName(root);
            newList.add(new TBDeviceInfo(new FsFile(root), label, srnPrefix));
            if (prevSelected != null && root.getAbsolutePath().equals(prevSelected.getAbsolutePath())) {
                index = newList.size()-1;
            }
            if (index==-1 && label.startsWith(srnPrefix)) {
                index = newList.size()-1;
            }
        }
        if (newList.size() == 0) {
            LOG.log(Level.INFO, "No drives");
            newList.add(TBDeviceInfo.getNullDeviceInfo());
            index = 0;
            tbLoaderPanel.getProgressDisplayManager().clearLog();
        }
        if (index == -1) {
            index = newList.size()-1;
        }

        refreshingDriveInfo = true;
        setEnabledStates();

        tbLoaderPanel.fillDriveList(newList, index);
        if (index != -1) {
            currentTbDevice = tbLoaderPanel.getSelectedDevice();
        }

        populatePreviousValuesFromCurrentDrive();
        refreshUI();

        refreshingDriveInfo = false;
        setEnabledStates();
    }


    private void fillPackageFromRecipient(Recipient recipient) {
        Properties deploymentProperties = getProgramSpec().getDeploymentProperties();
        String key = recipient.languagecode;
        if (StringUtils.isNotEmpty(recipient.variant)) {
            key = key + ',' + recipient.variant;
        }
        String imageName = deploymentProperties.getProperty(key);
        if (imageName == null) {
            imageName = deploymentProperties.getProperty(recipient.languagecode);
        }
        boolean ok = imageName != null;
        if (StringUtils.isEmpty(imageName)) {
            // Like ~/LiteracyBridge/TB-Loaders/{project}/content/{deployment}/basic
            File deploymentDir = new File(localTbLoaderDir,
                    TBLoaderConstants.CONTENT_SUBDIR + File.separator + deploymentChooser.getNewDeployment());
            Map<String, String> recipientMap = getProgramSpec().getRecipientsMap();
            if (recipientMap != null) {
                String recipientDirName = recipientMap.get(recipient.recipientid);
                if (StringUtils.isNotEmpty(recipientDirName)) {
                    imageName = getImageForCommunity(deploymentDir, recipientDirName);
                }
            }
            if (StringUtils.isEmpty(imageName)) {
                imageName = "";
            }
        }
        tbLoaderPanel.setNewPackage(imageName);
    }

    /**
     * Checks for the case of an old TB (VCR style controls) plugged in when expecting a new
     * style TB (Tree / Bowl / Table), and vice-versa.
     * @return true if the Talking Book seems to be the proper old/new style. Note that this
     *      is based solely on the serial number of the device.
     */
    private boolean isOldVsNewOk() {
        String sn = currentTbDevice.getSerialNumber();

        if (!TBLoaderUtils.isSerialNumberFormatGood(srnPrefix, sn)) {
            if (sn != null && sn.length() > 2 && sn.startsWith("-", 1)) {
                if (sn.compareToIgnoreCase(TBLoaderConstants.OLD_TB_SRN_PREFIX) == 0) {
                    JOptionPane.showMessageDialog(applicationWindow,
                        "This appears to be an OLD TB.  If so, please close this program and open the TB Loader for old TBs.",
                        "OLD TB!",
                        JOptionPane.WARNING_MESSAGE);
                    return false;
                } else if (sn.compareToIgnoreCase(TBLoaderConstants.NEW_TB_SRN_PREFIX) == 0) {
                    JOptionPane.showMessageDialog(applicationWindow,
                        "This appears to be a NEW TB.  If so, please close this program and open the TB Loader for new TBs.",
                        "NEW TB!",
                        JOptionPane.WARNING_MESSAGE);
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Populates the values in the right-hand side, the "previous deployment" side of the main screen.
     */
    private void populatePreviousValuesFromCurrentDrive() {
        if (!isOldVsNewOk()) return;

        String driveLabel = currentTbDevice.getLabelWithoutDriveLetter();
        if (!driveLabel.equals(TBLoaderConstants.NO_DRIVE)
                && !TBLoaderUtils.isSerialNumberFormatGood(srnPrefix, driveLabel)) {
            String message = "The TB's statistics cannot be found. Please follow these steps:\n 1. Unplug the TB\n 2. Hold down the * while turning on the TB\n "
                + "3. Observe the solid red light.\n 4. Now plug the TB into the laptop.\n 5. If you see this message again, please continue with the loading -- you tried your best.";
            String title = "Cannot find the statistics!";

            new PopUp.Builder()
                .withTitle(title)
                .withContents(message)
                .withOptOut()
                .go();
        }

        oldDeploymentInfo = currentTbDevice.createDeploymentInfo(newProject);
        tbLoaderPanel.fillPrevDeploymentInfo(oldDeploymentInfo);
        if (oldDeploymentInfo != null) {
            setCurrentTbFirmware(oldDeploymentInfo.getFirmwareRevision());

            currentTbSrn = oldDeploymentInfo.getSerialNumber();
            if (!TBLoaderUtils.isSerialNumberFormatGood(srnPrefix, currentTbSrn)) {
                currentTbSrn = TBLoaderConstants.NEED_SERIAL_NUMBER;
            }
            newTbSrn = TBLoaderUtils.newSerialNumberNeeded(srnPrefix, currentTbSrn) ? TBLoaderConstants.NEED_SERIAL_NUMBER : currentTbSrn;
            selectRecipientFromCurrentDrive();
        } else {
            setCurrentTbFirmware("");
            newTbSrn = TBLoaderConstants.NEED_SERIAL_NUMBER;
        }
        tbLoaderPanel.setNewSrn(newTbSrn);
        // Apply the Test Deployment Strategy to the previous TB value.
        tbLoaderPanel.setTestDeployment(testStrategy.initFromTb(oldDeploymentInfo.isTestDeployment()));
    }

    private void selectRecipientFromCurrentDrive() {
        String recipientid = oldDeploymentInfo.getRecipientid();
        // If we think we know this recipient, pre-select it in the recipient chooser.
        if (getProgramSpec().getRecipients().getRecipient(recipientid) != null) {
            tbLoaderPanel.setSelectedRecipient(recipientid);
            fillPackageFromRecipient(getProgramSpec().getRecipients().getRecipient(recipientid));
        }
    }

    /**
     * Handles combo box selections for the Drive list.
     *
     * @param selectedDevice The selected device.
     */
    private void onDeviceSelected(TBDeviceInfo selectedDevice) {
        // Is this a new value?
        if (refreshingDriveInfo || !startUpDone || updatingTB) return;

        currentTbSrn = "";
        currentTbDevice = selectedDevice;
        if (currentTbDevice != null && currentTbDevice.getRootFile() != null) {
            LOG.log(Level.INFO,
                "Drive changed: " + currentTbDevice.getRootFile()
                    + currentTbDevice.getLabel());
            populatePreviousValuesFromCurrentDrive();
        } else {
            tbLoaderPanel.fillPrevDeploymentInfo(null);
        }
    }

    /**
     * Handles combo box selections for the Community list.
     *
     * @param recipient The selected recipient.
     */
    private void onRecipientSelected(Recipient recipient) {
        if (refreshingDriveInfo || !startUpDone) return;

        if (recipient != null) {
            fillPackageFromRecipient(recipient);
        } else {
            tbLoaderPanel.setNewPackage("");
        }
//        setEnabledStates();
    }

    /**
     * Handles button presses for the "Update" and "Get Stats" buttons.
     *
     */
    public enum Operation {Update, CollectStats}
    private void onTbLoaderGo(Operation operation) {

        setEnabledStates();
        try {
            LOG.log(Level.INFO, "ACTION: " + operation.name());

            TbFile drive = currentTbDevice.getRootFile();
            if (drive == null) {
                refreshUI();
                return;
            }
            String devicePath = drive.getAbsolutePath();
            prevSelected = new File(devicePath);


            if (operation == Operation.Update) {
                if (getNewPackage().equalsIgnoreCase(TBLoaderConstants.MISSING_PACKAGE)) {
                    String text = "Can not update a Talking Book for this Community,\n"
                        + "because there is no Content Package.";
                    String heading = "Missing Package";
                    JOptionPane.showMessageDialog(applicationWindow,
                        text,
                        heading,
                        JOptionPane.PLAIN_MESSAGE);
                    refreshUI();
                    return;
                }

                // If the Talking Book needs a new serial number, allocate one. We did not do it before this to
                // avoid wasting allocations.
                assert(tbLoaderPanel.getNewSrn().equals(newTbSrn));
                isNewSerialNumber = false;
                if (tbLoaderPanel.isForceSrn()
                    || !TBLoaderUtils.isSerialNumberFormatGood(srnPrefix, newTbSrn)
                    || TBLoaderUtils.newSerialNumberNeeded(srnPrefix, newTbSrn)) {
                    int intSrn = allocateNextSerialNumberFromTbLoader();
                    isNewSerialNumber = true;
                    String lowerSrn = String.format("%04x", intSrn);
                    newTbSrn = (srnPrefix + deviceIdHex + lowerSrn).toUpperCase();
                    currentTbDevice.setSerialNumber(newTbSrn);
                    tbLoaderPanel.setNewSrn(newTbSrn);
                }
            }

            LOG.log(Level.INFO, "ID:" + currentTbDevice.getSerialNumber());
            tbLoaderPanel.getProgressDisplayManager().clear("STATUS: Starting");

            updatingTB = true;
            setEnabledStates();
            CopyThread t = new CopyThread(devicePath, operation);
            t.start();

        } catch (Exception ex) {
            LOG.log(Level.WARNING, ex.toString(), ex);
            JOptionPane.showMessageDialog(applicationWindow,
                "An error occured.",
                "Error",
                JOptionPane.ERROR_MESSAGE);
            resetUI(false);
        }
    }

    private void resetUI(boolean resetDrives) {
        LOG.log(Level.INFO, "Resetting UI");
        tbLoaderPanel.resetUi();
        if (resetDrives && !refreshingDriveInfo) {
            LOG.log(Level.INFO, " -fill drives list");
            fsRootMonitor.refresh();
        } else if (resetDrives) {
            LOG.log(Level.INFO, " - drive list currently being filled by drive monitor");
        }
        LOG.log(Level.INFO, " -refresh UI");
        refreshUI();
    }

    private void refreshUI() {
        boolean connected;

        tbLoaderPanel.setGridColumnWidths();
        setEnabledStates();

        TBDeviceInfo selectedDevice = tbLoaderPanel.getSelectedDevice();
        connected = selectedDevice != null && selectedDevice.getRootFile() != null;
        if (connected && !updatingTB) {
            tbLoaderPanel.getProgressDisplayManager().setStatus("STATUS: Ready");
            LOG.log(Level.INFO, "STATUS: Ready");
        } else {
            if (!connected) {
                tbLoaderPanel.fillPrevDeploymentInfo(null);
                setCurrentTbFirmware("");
                LOG.log(Level.INFO, "STATUS: " + TBLoaderConstants.NO_DRIVE);
                tbLoaderPanel.getProgressDisplayManager().setStatus("STATUS: " + TBLoaderConstants.NO_DRIVE);
            }
            if (tbLoaderPanel.getSelectedRecipient() != null) {
                fillPackageFromRecipient(tbLoaderPanel.getSelectedRecipient());
            }
        }
    }


    private void onCopyFinished(final String endMsg, final String endTitle) {
        onCopyFinished(endMsg, endTitle, JOptionPane.PLAIN_MESSAGE);
    }

    private void onCopyFinished(final String endMsg, final String endTitle, int msgType) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(applicationWindow, endMsg, endTitle, msgType);
            updatingTB = false;
            setEnabledStates();
            resetUI(true);
            LOG.log(Level.INFO, endMsg);
        });
    }


    // TODO: Move this to its own file.
    public class CopyThread extends Thread {

        final Operation operation;
        final String devicePath;
        boolean criticalError = false;
        boolean alert = true;

        CopyThread(String devicePath, Operation operation) {
            this.devicePath = devicePath;
            this.operation = operation;
        }

        private void grabStatsOnly() {
            OperationLog.Operation opLog = OperationLog.startOperation("TbLoaderGrabStats");
            opLog.put("serialno", oldDeploymentInfo.getSerialNumber())
                .put("project", oldDeploymentInfo.getProjectName())
                .put("deployment", oldDeploymentInfo.getDeploymentName())
                .put("package", oldDeploymentInfo.getPackageName())
                .put("community", oldDeploymentInfo.getCommunity());

            TBLoaderCore.Result result = null;
            try {
                TBLoaderConfig tbLoaderConfig = getTbLoaderConfig();

                TBLoaderCore tbLoader = new TBLoaderCore.Builder().withTbLoaderConfig(tbLoaderConfig)
                    .withTbDeviceInfo(currentTbDevice)
                    .withOldDeploymentInfo(oldDeploymentInfo)
//                    .withLocation(currentLocationChooser.getSelectedItem().toString())
                    .withRefreshFirmware(false)
                    .withStatsOnly()
                    .withProgressListener(tbLoaderPanel.getProgressDisplayManager())
                    .build();
                result = tbLoader.collectStatistics();

                opLog.put("success", result.gotStatistics);
            } catch (Exception e) {
                opLog.put("success", false);
            } finally {
                opLog.finish();
                String endMsg, endTitle;
                if (result != null && result.gotStatistics) {
                    endMsg = "Got Stats!";
                    endTitle = "Success";
                } else {
                    endMsg = "Could not get stats for some reason.";
                    endTitle = "Failure";
                }
                onCopyFinished(endMsg, endTitle);
                statisticsUploader.zipAndUpload();
            }
        }

        private void update() {
            String recipientid = tbLoaderPanel.getSelectedRecipient().recipientid;
            String directory = getMappedDirectoryForRecipient(recipientid);
            assert(tbLoaderPanel.getNewSrn().equals(newTbSrn));
            assert(recipientid.equals(getRecipientIdForCommunity(directory)));
            DeploymentInfo.DeploymentInfoBuilder builder = new DeploymentInfo.DeploymentInfoBuilder()
                .withSerialNumber(newTbSrn)
                .withNewSerialNumber(isNewSerialNumber)
                .withProjectName(newProject)
                .withDeploymentName(deploymentChooser.getNewDeployment())
                .withPackageName(getNewPackage())
                .withUpdateDirectory(null)
                .withUpdateTimestamp(tbLoaderPanel.getDateRotation())
                .withFirmwareRevision(newTbFirmware)
                .withCommunity(directory)
                .withRecipientid(recipientid)
                .asTestDeployment(tbLoaderPanel.isTestDeployment());
            if (getProgramSpec() != null) {
                String numberStr = getProgramSpec().getDeploymentProperties().getProperty(DEPLOYMENT_NUMBER);
                if (numberStr != null) {
                    builder.withDeploymentNumber(Integer.parseInt(numberStr));
                }
            }

            DeploymentInfo newDeploymentInfo = builder.build();

            String endMsg = null;
            String endTitle = null;
            int endMessageType = JOptionPane.PLAIN_MESSAGE;
            OperationLog.Operation opLog = OperationLog.startOperation("TbLoaderUpdate");
            opLog.put("serialno", newDeploymentInfo.getSerialNumber())
                .put("project", newDeploymentInfo.getProjectName())
                .put("deployment", newDeploymentInfo.getDeploymentName())
                .put("package", newDeploymentInfo.getPackageName())
                .put("community", newDeploymentInfo.getCommunity());
            if (!newDeploymentInfo.getProjectName().equals(oldDeploymentInfo.getProjectName())) {
                opLog.put("oldProject", oldDeploymentInfo.getProjectName());
            }
            if (!newDeploymentInfo.getCommunity().equals(oldDeploymentInfo.getCommunity())) {
                opLog.put("oldCommunity", oldDeploymentInfo.getCommunity());
            }
            if (!newDeploymentInfo.getSerialNumber().equals(oldDeploymentInfo.getSerialNumber())) {
                opLog.put("oldSerialno", oldDeploymentInfo.getSerialNumber());
            }

            File newDeploymentContentDir = new File(localTbLoaderDir,
                TBLoaderConstants.CONTENT_SUBDIR + File.separator
                    + newDeploymentInfo.getDeploymentName());

            TbFile sourceImage = new FsFile(newDeploymentContentDir);

            TBLoaderCore.Result result = null;
            try {
                TBLoaderConfig tbLoaderConfig = getTbLoaderConfig();
                String acceptableFirmwareVersions = programSpec.getDeploymentProperties().getProperty(
                    TBLoaderConstants.ACCEPTABLE_FIRMWARE_VERSIONS);

                TBLoaderCore tbLoader = new TBLoaderCore.Builder().withTbLoaderConfig(tbLoaderConfig)
                    .withTbDeviceInfo(currentTbDevice)
                    .withDeploymentDirectory(sourceImage)
                    .withOldDeploymentInfo(oldDeploymentInfo)
                    .withNewDeploymentInfo(newDeploymentInfo)
                    .withAcceptableFirmware(acceptableFirmwareVersions)
//                    .withLocation(currentLocationChooser.getSelectedItem().toString())
                    .withRefreshFirmware(tbLoaderPanel.isForceFirmware())
                    .withProgressListener(tbLoaderPanel.getProgressDisplayManager())
                    .build();
                result = tbLoader.update();

                opLog.put("gotstatistics", result.gotStatistics)
                    .put("corrupted", result.corrupted)
                    .put("reformatfailed",
                        result.reformatOp == TBLoaderCore.Result.FORMAT_OP.failed)
                    .put("verified", result.verified);

                if (result.corrupted
                    && result.reformatOp != TBLoaderCore.Result.FORMAT_OP.succeeded) {
                    endMsg =
                        "There is an error in the Talking Book SD card, and it needs to be re-formatted."
                            + "\nPlease re-format the Talking Book SD card, and try again.";
                    endTitle = "Corrupted SD Card";
                    endMessageType = JOptionPane.ERROR_MESSAGE;
                } else if (!result.gotStatistics) {
                    LOG.log(Level.SEVERE, "Could not get statistics!");
                    tbLoaderPanel.getProgressDisplayManager().error("Could not get statistics.");
                    if (result.corrupted) {
                        if (!OSChecker.WINDOWS) {
                            LOG.log(Level.INFO,
                                "Reformatting memory card is not supported on this platform.\nTry using TBLoader for Windows.");
                            JOptionPane.showMessageDialog(applicationWindow,
                                "Reformatting memory card is not supported on this platform.\nTry using TBLoader for Windows.",
                                "Failure!",
                                JOptionPane.ERROR_MESSAGE);
                        }

                        if (result.reformatOp == TBLoaderCore.Result.FORMAT_OP.failed) {
                            LOG.log(Level.SEVERE, "STATUS:Reformat Failed");
                            LOG.log(Level.SEVERE,
                                "Could not reformat memory card.\nMake sure you have a good USB connection\nand that the Talking Book is powered with batteries, then try again.\n\nIf you still cannot reformat, replace the memory card.");
                            JOptionPane.showMessageDialog(applicationWindow,
                                "Could not reformat memory card.\nMake sure you have a good USB connection\nand that the Talking Book is powered with batteries, then try again.\n\nIf you still cannot reformat, replace the memory card.",
                                "Failure!",
                                JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }

                for (int i = 1; i <= (result.gotStatistics ? 3 : 6); i++)
                    Toolkit.getDefaultToolkit().beep();
            } catch (Exception e) {
                opLog.put("exception", e.getMessage());
                if (alert) {
                    JOptionPane.showMessageDialog(applicationWindow,
                        e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                    criticalError = true;
                    LOG.log(Level.SEVERE, "CRITICAL ERROR:", e);
                } else LOG.log(Level.WARNING, "NON-CRITICAL ERROR:", e);
                endMsg = String.format("Exception updating TB-Loader: %s", e.getMessage());
                endTitle = "An Exception Occurred";
            } finally {
                opLog.finish();
                if (endMsg == null && result != null && result.verified) {
                    endMsg =
                        "Talking Book has been updated and verified\nin " + result.duration + ".";
                    endTitle = "Success";
                } else {
                    if (endMsg == null) {
                        endMsg = "Update failed verification.  Try again or replace memory card.";
                    }
                    if (endTitle == null) {
                        endTitle = "Failure";
                    }
                }
                onCopyFinished(endMsg, endTitle, endMessageType);
                statisticsUploader.zipAndUpload();
            }

        }

        @Override
        public void run() {
            if (this.operation == Operation.Update) {
                update();
            } else if (this.operation == Operation.CollectStats) {
                grabStatsOnly();
            }
        }
    }
}
