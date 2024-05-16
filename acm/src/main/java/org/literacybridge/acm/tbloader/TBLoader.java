package org.literacybridge.acm.tbloader;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.kohsuke.args4j.CmdLineParser;
import org.literacybridge.acm.Constants;
import org.literacybridge.acm.cloud.Authenticator;
import org.literacybridge.acm.cloud.TbSrnHelper;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.config.DBConfiguration;
import org.literacybridge.acm.gui.Application;
import org.literacybridge.acm.gui.dialogs.PopUp;
import org.literacybridge.acm.gui.resourcebundle.LabelProvider;
import org.literacybridge.acm.utils.LogHelper;
import org.literacybridge.acm.utils.OsUtils;
import org.literacybridge.acm.utils.SwingUtils;
import org.literacybridge.core.OSChecker;
import org.literacybridge.core.fs.FsFile;
import org.literacybridge.core.fs.OperationLog;
import org.literacybridge.core.fs.TbFile;
import org.literacybridge.core.spec.ProgramSpec;
import org.literacybridge.core.spec.Recipient;
import org.literacybridge.core.tbdevice.TbDeviceInfo;
import org.literacybridge.core.tbloader.DeploymentInfo;
import org.literacybridge.core.tbloader.TBLoaderConfig;
import org.literacybridge.core.tbloader.TBLoaderConstants;
import org.literacybridge.core.tbloader.TBLoaderCore;
import org.literacybridge.core.tbloader.TBLoaderUtils;
import org.literacybridge.core.tbloader.TbsCollected;
import org.literacybridge.core.tbloader.TbsDeployed;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileSystemView;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.literacybridge.acm.Constants.ALLOW_PACKAGE_CHOICE;
import static org.literacybridge.acm.Constants.TBLoadersLogDir;
import static org.literacybridge.acm.cloud.Authenticator.LoginOptions.CHOOSE_PROGRAM;
import static org.literacybridge.acm.cloud.Authenticator.LoginOptions.NOP;
import static org.literacybridge.acm.cloud.Authenticator.LoginOptions.NO_WAIT;
import static org.literacybridge.acm.cloud.Authenticator.LoginOptions.OFFLINE_EMAIL_CHOICE;
import static org.literacybridge.core.tbloader.TBLoaderConstants.DEPLOYMENT_NUMBER;
import static org.literacybridge.core.tbloader.TBLoaderConstants.ISO8601;
import static org.literacybridge.core.tbloader.TBLoaderUtils.getPackageForCommunity;
import static org.literacybridge.core.tbloader.TBLoaderUtils.getPackagesInDeployment;

@SuppressWarnings({"ResultOfMethodCallIgnored", "ConstantConditions", "CommentedOutCode", "IOStreamConstructor", "JavadocBlankLines"})
public class TBLoader extends JFrame {
    public static final String CANNOT_FIND_THE_STATISTICS = "Cannot find the statistics!";
    private static final Logger LOG = Logger.getLogger(TBLoader.class.getName());
    private TbsCollected tbsCollected;
    private TbsDeployed tbsDeployed;

    public static class TbLoaderConfig {
        private boolean strictTbV2Firmware = true;
        private boolean hasDfuSupport;
        private boolean hasTbV2Devices;
        private boolean allowPackageChoice = false;
        private boolean suppressDosTools = false;
        private boolean offerTbV2FirmwareWithStats = false;
        private boolean isTestMode = false;
        private String  pseudoTbDir = null;
        private boolean doNotUpload = false;
        private TB_LOADER_HISTORY_MODE tbLoaderHistoryMode = TB_LOADER_HISTORY_MODE.DEFAULT;
        private boolean showHistoryDetailLineNumbers;

        public boolean hasTbV2Devices() { return hasTbV2Devices; }
        public boolean hasDfuSupport() { return hasDfuSupport; }
        public boolean isStrictTbV2FIrmware() { return strictTbV2Firmware; }
        public boolean allowPackageChoice() { return allowPackageChoice; }
        @SuppressWarnings("unused")
        public boolean isSuppressDosTools() { return suppressDosTools; }
        public boolean offerTbV2FirmwareWithStats() { return offerTbV2FirmwareWithStats; }
        public boolean isTestMode() { return isTestMode; }
        public boolean isPseudoTb() { return StringUtils.isNotBlank(pseudoTbDir); }
        public File pseudoTbDir() { return StringUtils.isBlank(pseudoTbDir) ? null : new File(pseudoTbDir); }
        public boolean isDoNotUpload() { return doNotUpload; }
        public TB_LOADER_HISTORY_MODE getTbLoaderHistoryMode() {return tbLoaderHistoryMode;}
        public void setTbLoaderHistoryMode(TB_LOADER_HISTORY_MODE tbLoaderHistoryMode) {this.tbLoaderHistoryMode = tbLoaderHistoryMode;}
        public boolean getHistoryDetailLineNumbers() { return showHistoryDetailLineNumbers;}
        public void setHistoryDetailLineNumbers(boolean newValue) { showHistoryDetailLineNumbers = newValue; }
    }
    final TbLoaderConfig tbLoaderConfig = new TbLoaderConfig();

    /**
     * A class to hold the data needed to create a TbDeviceInfo. Allows us to obtain whatever flavor of
     * TbDeviceInfo is needed (basically auto vs user-selected).
     */
    class TbDeviceInfoHolder {
        final File root;
        final String label;

        public TbDeviceInfoHolder(File root, String label) {
            this.root = root;
            this.label = label;
        }

        TbDeviceInfo getDeviceInfo() {
            if (root==null) return TbDeviceInfo.getNullDeviceInfo();
            return TbDeviceInfo.getDeviceInfoFor(new FsFile(root), label, srnPrefix, tbLoaderPanel.getSelectedDeviceVersion());
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private static TBLoader tbLoaderTpplication;
    public final TbLoaderArgs tbArgs;
    private File deploymentDir;

    public static TBLoader getApplication() {
        return tbLoaderTpplication;
    }

    public enum TB_ID_STRATEGY {
        AUTOMATIC("A new Talking Book ID is allocated ony when needed."),
        MANUAL("The user can also request a new Talking Book ID."),
        AUTOMATIC_ON_PROGRAM_CHANGE("The Talking Book is assigned a new ID when moving between programs."),
        MANUAL_ON_PROGRAM_CHANGE("New Talking Book ID when needed, on request, or for new programs.");

        final String description;
        TB_ID_STRATEGY(String description) {
            this.description = description;
        }
        boolean allowsManual() {
            return this==MANUAL || this==MANUAL_ON_PROGRAM_CHANGE;
        }

        /**
         * Returns true if the Talking Book should be assigned a new ID when it is deployed with
         * content for a different program than previously deployed on that TB.
         * @return true if the TB should receive a new ID when the program changes.
         */
        @SuppressWarnings("unused")
        boolean onProgramChange() {
            return this==AUTOMATIC_ON_PROGRAM_CHANGE || this==MANUAL_ON_PROGRAM_CHANGE;
        }
    }
    TB_ID_STRATEGY tbIdStrategy = TB_ID_STRATEGY.AUTOMATIC;

    public enum TEST_DEPLOYMENT_STRATEGY {
        DEFAULT_OFF("'Test' is off unless explicitly turned on."),
        RETAIN("The value from the Talking Book is retained."),
        DEFAULT_ON("'Test' is on unless explicitly turned off.");

        final String description;
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

    public enum TB_LOADER_HISTORY_MODE {
        OFF("Don't display the # TBs to update or collect."),
        AUTO("Display # TBs to update or collect based on current recipient selection and operation"),
        CUSTOM("Manually choose the recipient granularity for update & collection counters"),
        ;

        public final static TB_LOADER_HISTORY_MODE DEFAULT = AUTO;
        final String description;
        TB_LOADER_HISTORY_MODE(String description) {
            this.description = description;
        }
    }

    private final JFrame applicationWindow;

    private String currentTbFirmware;
    private String newTbFirmware;

    private String currentTbSrn;
    private String newTbSrn;
    private boolean isNewSerialNumber;

    private TbDeviceInfo currentTbDevice;
    private final String srnPrefix;
    private String newProject;

    // Deployment info read from Talking Book.
    private DeploymentInfo oldDeploymentInfo;

    File softwareDir;
    public CommandLineUtils commandLineUtils;
    private String deviceIdHex;
    private String userEmail;
    private String userName;
    private File collectionWorkDir;
    private File uploadQueueDir;
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

    String getProgram() {
        return newProject;
    }
    ProgramSpec getProgramSpec() {
        assert(programSpec != null);
        return programSpec;
    }
    String getNewDeployment() {
        return deploymentChooser.getNewDeployment();
    }
    File getLocalTbLoaderDir() {
        return localTbLoaderDir;
    }
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

        tbLoaderTpplication = new TBLoader(tbArgs);
        tbLoaderTpplication.runApplication();
    }

    private TBLoader(TbLoaderArgs tbArgs) {
        this.tbArgs = tbArgs;
        this.newProject = ACMConfiguration.cannonicalProjectName(tbArgs.project);

        applicationWindow = this;

        if (tbArgs.srnPrefix != null && TBLoaderConstants.VALID_SRN_PREFIXES.contains(tbArgs.srnPrefix.toUpperCase())) {
            srnPrefix = tbArgs.srnPrefix.toUpperCase();
        } else if (tbArgs.oldTbs) {
            srnPrefix = TBLoaderConstants.OLD_TB_SRN_PREFIX;
        } else {
            srnPrefix = TBLoaderConstants.NEW_TB_SRN_PREFIX; // for latest Talking Book hardware
        }

        this.tbLoaderConfig.allowPackageChoice = tbArgs.choices;
        this.tbLoaderConfig.isTestMode = tbArgs.testMode;
    }

    private void runApplication() throws Exception {
        long startupTimer = -System.currentTimeMillis();

        OsUtils.enableOSXQuitStrategy();
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        this.addWindowListener(new WindowEventHandler());
        this.backgroundColor = getBackground();
        SwingUtils.setLookAndFeel("seaglass");
        UIDefaults defaults = UIManager.getLookAndFeelDefaults();
        if (defaults.get("Table.alternateRowColor") == null)
            defaults.put("Table.alternateRowColor", new Color(240, 240, 240));

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

        // Looks in S3 and in ~/Amplio for deployments. May prompt user for which
        // Deployment version, or update to latest.
        deploymentChooser = new DeploymentChooser(this);
        deploymentChooser.select();

        initializeProgramSpec();

        tbLoaderConfig.hasDfuSupport = DfuCheck.go();

        initializeGui();

//        JOptionPane.showMessageDialog(applicationWindow,
//                                      "Remember to power Talking Book with batteries before connecting with USB.",
//                                      "Use Batteries!", JOptionPane.PLAIN_MESSAGE);
        LOG.log(Level.INFO, "set visibility - starting drive monitoring");

        fsRootMonitor = new FsRootMonitor(this::rootsHandler);
        fsRootMonitor.start();

        startUpDone = true;
        setEnabledStates();

        TbHistory.getInstance().initializeHistory();
        refreshTbHistory();

        startupTimer += System.currentTimeMillis();
        System.out.printf("Startup in %d ms.\n", startupTimer);

        statisticsUploader = new StatisticsUploader(this, uploadQueueDir);
        statisticsUploader.zipAndEnqueue(collectionWorkDir, "abandoned-data/tbcd"+deviceIdHex);
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
        Authenticator.LoginResult result = authInstance.authenticateAndChooseProgram(this,
            LabelProvider.getLabel("TBLOADER_PROGRAM_NAME"),
            newProject,
            /*LOCAL_OR_S3,*/
            OFFLINE_EMAIL_CHOICE,
            CHOOSE_PROGRAM,
            tbArgs.autoGo?NO_WAIT:NOP
            );
        if (result == Authenticator.LoginResult.FAILURE) {
            JOptionPane.showMessageDialog(this,
                "Authentication is required to use the TB-Loader.",
                "Authentication Failure",
                JOptionPane.ERROR_MESSAGE);
            System.exit(13);
        }

        TbSrnHelper srnHelper = authInstance.getTbSrnHelper();
        srnHelper.prepareForAllocation();
        newProject = authInstance.getSelectedProgramid();
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
            softwareDir = ACMConfiguration.getInstance().getSoftwareDir();

            File appHome = ACMConfiguration.getInstance().getApplicationHomeDirectory();
            collectionWorkDir = new File(appHome, Constants.TbCollectionWorkDir);
            uploadQueueDir = new File(appHome, Constants.uploadQueue);

            logsDir = new File(ACMConfiguration.getInstance().getApplicationHomeDirectory(),
                Constants.TBLoadersHomeDir + File.separator + TBLoadersLogDir);
            logsDir.mkdirs();

            temporaryDir = new FsFile(Files.createTempDirectory("tbloader-tmp").toFile());
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Exception while setting DeviceId and paths", e);
            throw e;
        }
    }

    /**
     * Gets the key to which to upload collected statistics and user feedback for the
     * current device. We use different keys for v1 vs v2 devices, because the statistics
     * are formatted differently, and must be pre-processed differently.
     * @param tbDevice The device for which to get the key.
     * @return the key.
     */
    private String getUploadKeyPrefix(TbDeviceInfo tbDevice) {
        String key = tbDevice.getDeviceVersion()==TbDeviceInfo.DEVICE_VERSION.TBv2
                ? "collected-data.v2"
                : "collected-data";
        return key + "/tbcd" + deviceIdHex;
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
        // If the deployment.properties in the program spec allows package choice, turn it on.
        String value = programSpec.getDeploymentProperties().getProperty(ALLOW_PACKAGE_CHOICE, "FALSE");
        tbLoaderConfig.allowPackageChoice |= Boolean.parseBoolean(value);
        value = programSpec.getDeploymentProperties().getProperty(Constants.HAS_TBV2_DEVICES, "FALSE");
        this.tbLoaderConfig.hasTbV2Devices |= Boolean.parseBoolean(value);

    }

    private void initializeGui() {
        setTitle(String.format("TB-Loader %s", newProject));

        String[] packagesInDeployment;
        Properties deploymentProperties = getProgramSpec().getDeploymentProperties();
        // If the deployment properties doesn't have a map of language-variant to package, always allow the user
        // to select the package, since we can't reliably do it automatically.
        tbLoaderConfig.allowPackageChoice |= deploymentProperties.isEmpty();
        packagesInDeployment = getPackagesInDeployment(deploymentDir);
        List<String> packagesList = Arrays.asList(packagesInDeployment);
        // Get a more readable version of the package names.
        Map<String,String> packageNameMap = buildPackageNamesMap(packagesList);

        /*tbLoaderPanel.refresh();*/
        TbLoaderPanel.Builder builder = new TbLoaderPanel.Builder()
            .withProgramSpec(programSpec)
            .withPackagesInDeployment(packagesInDeployment)
            .withPackageNameMap(packageNameMap)
            .withSettingsClickedListener(TblSettingsDialog::showDialog)
            .withGoListener(this::onTbLoaderGo)
            .withRecipientListener(this::onRecipientSelected)
            .withDeviceSelectedListener(this::onDeviceSelected)
            .withDeviceVersionSelectedListener(this::onDeviceVersionSelected)
            .withForceFirmwareListener(this::onForceFirmwareChanged)
            .withForceSrnListener(this::onForceSrnChanged)
            .withTbIdStrategy(tbIdStrategy)
            .withUpdateTb2FirmwareListener(this::onUpdateTb2Firmware)
            .withAllowPackageChoice(tbLoaderConfig.allowPackageChoice())
            .withTbLoaderConfig(tbLoaderConfig)
            ;

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

    private Map<String, String> buildPackageNamesMap(List<String> packagesList) {
        // If we were able to open the ACM database, we can use it to get friendlier names for languages.
        // Otherwise, we'll have to use the languae code.
        DBConfiguration dbConfig = ACMConfiguration.getInstance().getDbConfiguration(newProject);
        Function<String,String> labeler = dbConfig != null ? dbConfig::getLanguageLabel : x->null;
        Properties deploymentProperties = getProgramSpec().getDeploymentProperties();

        // Converts properties like
        //  sil=LBG-COVID19-6-sil
        //  sil,f=LBG-COVID196silf
        // to a map like
        // { "LBG-COVID19-6-sil" : "Tumu Sissali (sil)", "LBG-COVID196silf" : "Tumu Sissali (sil), Variant: f", ...}
        // That is, a more readable version of the package name.
        Map<String,String> packageNameMap = deploymentProperties.stringPropertyNames().stream()
            .filter(k->packagesList.contains(deploymentProperties.get(k).toString()))
            .collect(Collectors.toMap(k->deploymentProperties.get(k).toString(), k->{
                String[] parts = k.split(",");
                String label = labeler.apply(parts[0]);
                String languageName = label==null ? parts[0] : (label + " (" + parts[0] + ')');
                if (parts.length > 1) {
                    languageName += ", Variant: " + parts[1];
                }
                return languageName;
            }));
        // Ensure every package is in map of { name: friendly-label }.
        packagesList.stream()
            .filter(p -> !packageNameMap.containsKey(p))
            .forEach(p -> packageNameMap.put(p, p));
        return packageNameMap;
    }

    private void refreshTbHistory() {
        tbLoaderPanel.setTbLoaderHistoryMode(tbLoaderConfig.getTbLoaderHistoryMode());
        TbHistory tbHistory = TbHistory.getInstance();
        tbHistory.setRelevantRecipients(tbLoaderPanel.getRecipientsForPartialSelection());
    }

    /**
     * The user clicked the button to update the TBv2 firmware. Open the dialog to walk the
     * user through using DFU mode to update the firmware.
     * @return true if we were able to update the firmware, false otherwise.
     */
    private Boolean onUpdateTb2Firmware() {
        // Like ~/Amplio/TB-Loaders/{programid}/content/{deployment}
        File deploymentPath = new File(localTbLoaderDir,TBLoaderConstants.CONTENT_SUBDIR + File.separator +
            deploymentChooser.getNewDeployment());
        // Firmware versions for TBv2
        File firmwarePath = new File(deploymentPath, "firmware.v2");
        File firmware = new File(firmwarePath, "TBookRev2b.hex");
        Tb2FirmwareUpdater updater =  new Tb2FirmwareUpdater(this, firmware);
        tbLoaderPanel.setEnabled(false);
        boolean result;
        try {
            updater.setVisible(true);
            result = updater.isOk();
        } finally {
            tbLoaderPanel.setEnabled(true);
        }
        return result;
    }

    /**
     * Load per-program configuration items (from the program's config.properties file).
     */
    void loadConfiguration() {
        DBConfiguration dbConfig = ACMConfiguration.getInstance().getDbConfiguration(newProject);
        if (dbConfig != null) {
            // If the config file allows package choice, turn on the option. We also check the deployment properties later.
            this.tbLoaderConfig.allowPackageChoice |= dbConfig.isPackageChoice();
            this.tbLoaderConfig.hasTbV2Devices = dbConfig.hasTbV2Devices();

            String valStr = dbConfig.getProperty("ALLOW_FORCE_SRN", "FALSE");
            if (Boolean.parseBoolean(valStr)) {
                this.tbIdStrategy = TB_ID_STRATEGY.MANUAL;
            }
            valStr = dbConfig.getProperty("TB_ID_STRATEGY");
            if (valStr != null) {
                try {
                    this.tbIdStrategy = TB_ID_STRATEGY.valueOf(valStr);
                } catch(Exception ignored) { }
            }

            valStr = dbConfig.getProperty("TEST_DEPLOYMENT_STRATEGY");
            if (valStr != null) {
                try {
                    this.testStrategy = TEST_DEPLOYMENT_STRATEGY.valueOf(valStr);
                } catch(Exception ignored) { }
            }

            valStr = dbConfig.getProperty("TB_LOADER_HISTORY_MODE");
            if (valStr != null) {
                try {
                    this.tbLoaderConfig.setTbLoaderHistoryMode(TB_LOADER_HISTORY_MODE.valueOf(valStr));
                } catch(Exception ignored) { }
            }

        }
    }

    /**
     * If the user changes the value of "allow package choice" (in the settings page), persist the
     * setting here.
     * @param allowPackageChoice If true, allow the TB-Loader user to choose which package(s) to put
     *                           onto a given Talking Book.
     */
    void setAllowPackageChoice(boolean allowPackageChoice) {
        boolean changed = this.tbLoaderConfig.allowPackageChoice != allowPackageChoice;
        this.tbLoaderConfig.allowPackageChoice = allowPackageChoice;
        if (changed) {
            tbLoaderPanel.enablePackageChoice(allowPackageChoice);
        }
    }
    boolean allowsPackageChoice() {
        return this.tbLoaderConfig.allowPackageChoice();
    }

    void setPseudoTb(String pseudoTbDir) {
        this.tbLoaderConfig.pseudoTbDir = pseudoTbDir;
    }

    @SuppressWarnings("SameParameterValue")
    void setTestMode(boolean testMode) {
        this.tbLoaderConfig.isTestMode = testMode;
    }

    void setDoNotUpload(boolean doNotUpload) {
        this.tbLoaderConfig.doNotUpload = doNotUpload;
    }

    /**
     * Get and set the value of TB ID strategy. See TB_ID_STRATEGY for more.
     */
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

    /**
     * Gets and sets the value of the "Testing" strategy ("Deploying today for testing only"). See
     * TEST_DEPLOYMENT_STRATEGY for more.
     */
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

    TB_LOADER_HISTORY_MODE getTbLoaderHistoryMode() { return tbLoaderConfig.getTbLoaderHistoryMode();}
    void setTbLoaderHistoryMode(int tbLoaderHistoryOrdianl) {
        setTbLoaderHistoryMode(TB_LOADER_HISTORY_MODE.values()[tbLoaderHistoryOrdianl]);
    }
    void setTbLoaderHistoryMode(TB_LOADER_HISTORY_MODE tbLoaderHistoryMode) {
        if (tbLoaderConfig.getTbLoaderHistoryMode() != tbLoaderHistoryMode) {
            tbLoaderConfig.setTbLoaderHistoryMode(tbLoaderHistoryMode);
            refreshTbHistory();
        }
    }
    boolean getHistoryDetailLineNumbers() {
        return tbLoaderConfig.getHistoryDetailLineNumbers();
    }
    void setHistoryDetailLineNumbers(boolean newValue) {
        if (tbLoaderConfig.getHistoryDetailLineNumbers() != newValue) {
            tbLoaderConfig.setHistoryDetailLineNumbers(newValue);
            refreshTbHistory();
        }
    }

    public boolean isStrictTbV2Firmware() {
        return this.tbLoaderConfig.strictTbV2Firmware;
    }
    public void setStrictTbV2Firmware(boolean strict) {
        this.tbLoaderConfig.strictTbV2Firmware = strict;
    }

    public boolean offerTbV2FirmwareWithStats() {
        return this.tbLoaderConfig.offerTbV2FirmwareWithStats;
    }
    public void offerTbV2FirmwareWithStats(boolean offer) {
        this.tbLoaderConfig.offerTbV2FirmwareWithStats = offer;
    }

    public void setHasTbV2Devices(boolean hasTbV2Devices) {
        this.tbLoaderConfig.hasTbV2Devices = hasTbV2Devices;
    }
    public boolean getHasTbV2Devices() {
        return this.tbLoaderConfig.hasTbV2Devices;
    }

    public boolean isSuppressDosTools() { return this.tbLoaderConfig.suppressDosTools; }
    public void setSuppressDosTools(boolean suppress) { this.tbLoaderConfig.suppressDosTools = suppress; }

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
        acceptableFirmwareVersions.clear();

        // Like ~/Amplio/TB-Loaders/{programid}/content/{deployment}
        File deploymentPath = new File(localTbLoaderDir,TBLoaderConstants.CONTENT_SUBDIR + File.separator +
            deploymentChooser.getNewDeployment());
        if (tbLoaderPanel.getDeviceVersion() == TbDeviceInfo.DEVICE_VERSION.TBv2) {
            // Firmware versions for TBv2
            File firmwarePath = new File(deploymentPath, "firmware.v2");
            File firmwareVersionTxt = new File(firmwarePath, "firmware_built.txt");
            if (firmwareVersionTxt.exists()) {
                try (InputStream fis = new FileInputStream(firmwareVersionTxt);
                     InputStreamReader isr = new InputStreamReader(fis);
                     BufferedReader br = new BufferedReader(isr)) {
                    newTbFirmware = br.readLine().trim();
                    acceptableFirmwareVersions.add(newTbFirmware);
                    tbLoaderPanel.setNewFirmwareVersion(newTbFirmware);
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        } else if (tbLoaderPanel.getDeviceVersion() == TbDeviceInfo.DEVICE_VERSION.TBv1) {
            // Firmware versions for TBv1
            // Like ~/LiteracyBridge/TB-Loaders/{project}/content/{deployment}/basic
            File firmwareDir = new File(deploymentPath, TBLoaderConstants.CONTENT_BASIC_SUBDIR);
            if (!firmwareDir.isDirectory()) new File(deploymentPath, "firmware.v1");
            if (!firmwareDir.isDirectory()) new File(deploymentPath, "firmware");

            LOG.log(Level.INFO, "DEPLOYMENT:" + deploymentChooser.getNewDeployment());
            try {
                File[] files;
                if (firmwareDir.isDirectory()) {
                    // get firmware for deployment
                    files = firmwareDir.listFiles((dir, name) -> {
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
        } else {
            tbLoaderPanel.setNewFirmwareVersion("");
        }

    }

    FsRootMonitor.FilterParams getFsFilterParams() {
        return fsRootMonitor.getFilterParams();
    }
    void setFsFilterParams(FsRootMonitor.FilterParams filterParams) {
        fsRootMonitor.setFilterParams(filterParams);
        tbLoaderPanel.resetDeviceVersion();
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
        commandLineUtils = new CommandLineUtils(softwareDir);
        String collectionTimestamp = ISO8601.format(new Date());
        File collectionRoot = collectionWorkDir;

        // If we're deploying to a pseudo-device, don't collect statistics to where
        // the'll upload. Collect to a pseudo-location instead.
        if (tbLoaderConfig.isPseudoTb()) {
            collectionRoot = new File(tbLoaderConfig.pseudoTbDir + "_stats");
        }

        File collectedDataDirectory = new File(collectionRoot, collectionTimestamp);
        TbFile collectedDataTbFile = new FsFile(collectedDataDirectory);

        TBLoaderConfig.Builder builder = new TBLoaderConfig.Builder().withTbLoaderId(deviceIdHex)
            .withCollectedDataDirectory(collectedDataTbFile)
            .withTempDirectory(temporaryDir)
            .withUserEmail(userEmail)
            .withUserName(userName);
        if (!tbLoaderConfig.suppressDosTools && OSChecker.WINDOWS) {
            builder.withFileSystemUtilities(commandLineUtils);
        }
        return builder.build();
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
        return recipientMap.getOrDefault(recipientid, recipientid);

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
        int lastMatchIndex = -1;
        String currentPath = (currentTbDevice == null || currentTbDevice.getRootFile() == null) ? "::" : currentTbDevice.getRootFile().getAbsolutePath();
        List<TbDeviceInfoHolder> newList = new ArrayList<>();
        int index = -1;
        for (File root : files) {
            String label = fsView.getSystemDisplayName(root);
            newList.add(new TbDeviceInfoHolder(root, label));
                //TbDeviceInfo.getDeviceInfoFor(new FsFile(root), label, srnPrefix, tbLoaderPanel.getSelectedDeviceVersion()));
            if (prevSelected != null && root.getAbsolutePath().equals(prevSelected.getAbsolutePath())) {
                index = newList.size()-1;
            }
            if (index==-1 && root.getAbsolutePath().equals(currentPath)) {
                index = newList.size()-1;
            }
            if (index==-1 && label.startsWith(srnPrefix)) {
                lastMatchIndex = newList.size()-1;
            }
        }

        if (tbLoaderConfig.isPseudoTb()) {
            newList.add(new TbDeviceInfoHolder(tbLoaderConfig.pseudoTbDir(), "pseudo"));
            if (prevSelected != null && tbLoaderConfig.pseudoTbDir().getAbsolutePath().equals(prevSelected.getAbsolutePath())) {
                index = newList.size()-1;
            }
        }

        if (index==-1 && lastMatchIndex!=-1) {
            index=lastMatchIndex;
        }
        if (newList.isEmpty()) {
            LOG.log(Level.INFO, "No drives");
            newList.add(new TbDeviceInfoHolder(null, null));
            index = 0;
            tbLoaderPanel.getProgressDisplayManager().clearLog();
        }
        if (index == -1) {
            index = newList.size()-1;
        }

        refreshingDriveInfo = true;

        try {
            tbLoaderPanel.fillDriveList(newList, index);
            setEnabledStates();
            if (index != -1) {
                currentTbDevice = tbLoaderPanel.getSelectedDevice();
            }

            populatePreviousValuesFromCurrentDrive();
            refreshUI();
        } finally {
            refreshingDriveInfo = false;
            setEnabledStates();
        }
    }


    private void fillPackageFromRecipient(Recipient recipient) {
        Properties deploymentProperties = getProgramSpec().getDeploymentProperties();
        String key = recipient.languagecode;
        if (StringUtils.isNotEmpty(recipient.variant)) {
            key = key + ',' + recipient.variant;
        }
        String contentPackage = deploymentProperties.getProperty(key);
        if (contentPackage == null) {
            contentPackage = deploymentProperties.getProperty(recipient.languagecode);
        }
        if (StringUtils.isEmpty(contentPackage)) {
            // Like ~/LiteracyBridge/TB-Loaders/{project}/content/{deployment}/basic
            File deploymentDir = new File(localTbLoaderDir,
                    TBLoaderConstants.CONTENT_SUBDIR + File.separator + deploymentChooser.getNewDeployment());
            Map<String, String> recipientMap = getProgramSpec().getRecipientsMap();
            if (recipientMap != null) {
                String recipientDirName = recipientMap.getOrDefault(recipient.recipientid, recipient.recipientid);
                if (StringUtils.isNotEmpty(recipientDirName)) {
                    contentPackage = getPackageForCommunity(deploymentDir, recipientDirName);
                }
            }
            if (StringUtils.isEmpty(contentPackage)) {
                contentPackage = "";
            }
        }
        // If ever we can configure the program specification with multiple default content packages, change this to
        // setDefaultPackages(listOfPackages);
        tbLoaderPanel.setDefaultPackage(contentPackage);
    }

    /**
     * Checks for the case of an old TB (VCR style controls) plugged in when expecting a new
     * style TB (Tree / Bowl / Table), and vice-versa.
     * @return true if the Talking Book seems to be the proper old/new style. Note that this
     *      is based solely on the serial number of the device.
     */
    private boolean isOldVsNewOk() {
        String sn = currentTbDevice.getSerialNumber();
        if (currentTbDevice.getDeviceVersion() == TbDeviceInfo.DEVICE_VERSION.TBv1) {
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
        }
        return true;
    }

    /**
     * Populates the values in the right-hand side, the "previous deployment" side of the main screen.
     */
    private void populatePreviousValuesFromCurrentDrive() {
        if (!isOldVsNewOk()) return;

        String[] packagesInDeployment = getPackagesInDeployment(deploymentDir, tbLoaderPanel.getDeviceVersion());
        List<String> packagesList = Arrays.asList(packagesInDeployment);
        Map<String,String> packageNameMap = buildPackageNamesMap(packagesList);
        tbLoaderPanel.setAvailablePackages(packagesInDeployment, packageNameMap);

        fillFirmwareVersion();

        String driveLabel = currentTbDevice.getLabelWithoutDriveLetter();
        if (driveLabel.equals(TBLoaderConstants.NO_DRIVE)) {
            // If no drive, don't change anything.
            return;
        }
        if (!TBLoaderUtils.isSerialNumberFormatGood(srnPrefix, driveLabel) && tbLoaderPanel.getDeviceVersion()== TbDeviceInfo.DEVICE_VERSION.TBv1) {
            String message = "The TB's statistics cannot be found. Please follow these steps:\n 1. Unplug the TB\n 2. Hold down the * while turning on the TB\n "
                + "3. Observe the solid red light.\n 4. Now plug the TB into the laptop.\n 5. If you see this message again, please continue with the loading -- you tried your best.";

            new PopUp.Builder()
                .withTitle(CANNOT_FIND_THE_STATISTICS)
                .withContents(message)
                .withOptOut()
                .go();
        }

        oldDeploymentInfo = currentTbDevice.createDeploymentInfo(newProject);
        tbLoaderPanel.fillPrevDeploymentInfo(oldDeploymentInfo, currentTbDevice);
        if (oldDeploymentInfo != null) {
            setCurrentTbFirmware(oldDeploymentInfo.getFirmwareRevision());

            currentTbSrn = oldDeploymentInfo.getSerialNumber();
            if (!currentTbDevice.isSerialNumberFormatGood(currentTbSrn)) {
                currentTbSrn = TBLoaderConstants.NEED_SERIAL_NUMBER;
            }
            newTbSrn = currentTbDevice.newSerialNumberNeeded() ? TBLoaderConstants.NEED_SERIAL_NUMBER : currentTbSrn;
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
    private void onDeviceSelected(TbDeviceInfo selectedDevice) {
        // Is this a new value?
        if (refreshingDriveInfo || !startUpDone || updatingTB) return;

        currentTbSrn = "";

        // If we're deploying to a pseudo device, create a dummy TbDeviceInfo for it.
        if (tbLoaderConfig.isPseudoTb()) {
            List<String> packages = tbLoaderPanel.getSelectedPackages();
            String packageDirName = String.join("_", packages);
            File packageDir = new File(tbLoaderConfig.pseudoTbDir(), packageDirName);
            if (!packageDir.exists()) {
                packageDir.mkdirs();
            }
            currentTbDevice = TbDeviceInfo.getDeviceInfoFor(new FsFile(packageDir),
                "pseudo",
                srnPrefix,
                tbLoaderPanel.getSelectedDeviceVersion());
            populatePreviousValuesFromCurrentDrive();
            return;
        }

        currentTbDevice = selectedDevice;
        if (currentTbDevice != null && currentTbDevice.getRootFile() != null) {
            LOG.log(Level.INFO,
                "Drive changed: " + currentTbDevice.getRootFile()
                    + currentTbDevice.getLabel());
            populatePreviousValuesFromCurrentDrive();
        } else {
            tbLoaderPanel.fillPrevDeploymentInfo(null, null);
        }
    }

    private void onDeviceVersionSelected(TbDeviceInfo.DEVICE_VERSION device_version) {
//        String[] packagesInDeployment = getPackagesInDeployment(deploymentDir, device_version);
//        List<String> packagesList = Arrays.asList(packagesInDeployment);
//        Map<String,String> packageNameMap = buildPackageNamesMap(packagesList);
//        tbLoaderPanel.setAvailablePackages(packagesInDeployment, packageNameMap);
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
            tbLoaderPanel.setDefaultPackage("");
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
                if (!tbLoaderPanel.hasSelectedPackage()) {
                    String text;
                    if (tbLoaderConfig.allowPackageChoice()) {
                        text = "Please choose a Content Package to\n" +
                            "update this Talking Book.";
                    } else {
                        text = "Can not update a Talking Book for this Community,\n" +
                            "because there is no Content Package.";
                    }
                    String heading = "No Content Package";
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
                if ((tbLoaderPanel.getForceTbId()
                    || !TBLoaderUtils.isSerialNumberFormatGood(srnPrefix, newTbSrn)
                    || TBLoaderUtils.newSerialNumberNeeded(srnPrefix, newTbSrn))
                    && currentTbDevice.getDeviceVersion()==TbDeviceInfo.DEVICE_VERSION.TBv1) {
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

        TbDeviceInfo selectedDevice = tbLoaderPanel.getSelectedDevice();
        connected = selectedDevice != null && selectedDevice.getRootFile() != null;
        if (connected && !updatingTB) {
            tbLoaderPanel.getProgressDisplayManager().setStatus("STATUS: Ready");
            LOG.log(Level.INFO, "STATUS: Ready");
        } else {
            if (!connected) {
                tbLoaderPanel.fillPrevDeploymentInfo(null, null);
                setCurrentTbFirmware("");
                tbLoaderPanel.setNewSrn("");
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

        private void collectStatsOnly() {
            OperationLog.Operation opLog = OperationLog.startOperation("TbLoaderGrabStats");
            opLog.put("serialno", oldDeploymentInfo.getSerialNumber())
                .put("project", oldDeploymentInfo.getProjectName())
                .put("deployment", oldDeploymentInfo.getDeploymentName())
                .put("package", String.join(",", oldDeploymentInfo.getPackageNames()))
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

                tbsCollected = tbLoader.getTbsCollected();
                if (tbsCollected != null) {
                    TbHistory.getInstance().addTbCollected(tbsCollected);
                }

                opLog.put("success", result.gotStatistics);
            } catch (Exception e) {
                opLog.put("success", false);
            } finally {
                opLog.finish();
                String endMsg, endTitle;
                if (currentTbDevice.isFsCheckTimeout()) {
                    String letter = currentTbDevice.getDriveLetter();
                    endMsg = String.format("TB-Loader was unable to check the storage on the Talking Book device.\nPlease run 'chkdsk /f %s' and try again",
                            letter);
                    endTitle = "Device Check Error";
                } else if (currentTbDevice.isFsAccessDenied()) {
                    String letter = currentTbDevice.getDriveLetter();
                    endMsg = String.format("Due to Windows Security, TB-Loader was unable to check the storage on the Talking Book device.\n" +
                                    "If there are problems with the device, please run 'chkdsk /f %s' (as Administrator) and try again",
                            letter);
                    endTitle = "Device Check Error";
                } else if (result != null && result.gotStatistics) {
                    endMsg = "Got Stats!";
                    endTitle = "Success";
                } else {
                    endMsg = "Could not get stats for some reason.";
                    endTitle = "Failure";
                }
                onCopyFinished(endMsg, endTitle);
                if (tbLoaderConfig.isDoNotUpload()) {
                    try {
                        FileUtils.cleanDirectory(collectionWorkDir);
                    } catch (IOException e) {
                        new PopUp.Builder()
                            .withTitle("Can not clean collection directory")
                            .withContents("Couldn't remove files from "+collectionWorkDir.getName()+". Please manually clean the directory" +
                                " to prevent polluting the production data.")
                            .go();
                        System.exit(1);
                    }
                } else {
                    statisticsUploader.zipAndEnqueue(collectionWorkDir, getUploadKeyPrefix(currentTbDevice));
                }
            }
        }

        private void collectStatsAndDeployNewContent() {
            String recipientid = tbLoaderPanel.getSelectedRecipient().recipientid;
            String directory = getMappedDirectoryForRecipient(recipientid);
            List<String> selectedPackages = tbLoaderPanel.getSelectedPackages();

            ////////////////////////////////////////////////////////////////////////////////////////////////////////////
            ////////////////////////////////////////////////////////////////////////////////////////////////////////////
            // HACK to work around problem with encoded package names.
            if (newProject.equals("UNICEF-GH-CHPS") && currentTbDevice.getDeviceVersion()== TbDeviceInfo.DEVICE_VERSION.TBv1) {
                selectedPackages = selectedPackages.stream()
                        .map(pkg -> {
                            switch (pkg) {
                                case "UNICEF-GH-CHPS-5-dga":
                                    return "NCF-GH-CHPS5dga";
                                case "UNICEF-GH-CHPS-5-en":
                                    return "UNICEF-GH-CHPS5en";
                                case "UNICEF-GH-CHPS-5-ssl":
                                    return "NCF-GH-CHPS5ssl";
                            }
                            return pkg;})
                        .collect(Collectors.toList());
            }
            ////////////////////////////////////////////////////////////////////////////////////////////////////////////
            ////////////////////////////////////////////////////////////////////////////////////////////////////////////

            assert(tbLoaderPanel.getNewSrn().equals(newTbSrn));
            assert(recipientid.equals(getRecipientIdForCommunity(directory)));
            DeploymentInfo.DeploymentInfoBuilder builder = new DeploymentInfo.DeploymentInfoBuilder()
                .withSerialNumber(newTbSrn)
                .withNewSerialNumber(isNewSerialNumber)
                .withProjectName(newProject)
                .withDeploymentName(deploymentChooser.getNewDeployment())
                .withPackageNames(selectedPackages)
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
                .put("package", String.join(",", newDeploymentInfo.getPackageNames()))
                .put("community", newDeploymentInfo.getCommunity());
            if (oldDeploymentInfo != null) {
                if (!newDeploymentInfo.getProjectName().equals(oldDeploymentInfo.getProjectName())) {
                    opLog.put("oldProject", oldDeploymentInfo.getProjectName());
                }
                if (!newDeploymentInfo.getCommunity().equals(oldDeploymentInfo.getCommunity())) {
                    opLog.put("oldCommunity", oldDeploymentInfo.getCommunity());
                }
                if (!newDeploymentInfo.getSerialNumber().equals(oldDeploymentInfo.getSerialNumber())) {
                    opLog.put("oldSerialno", oldDeploymentInfo.getSerialNumber());
                }
            }

            File newDeploymentContentDir = new File(localTbLoaderDir,
                TBLoaderConstants.CONTENT_SUBDIR + File.separator
                    + newDeploymentInfo.getDeploymentName());

            TbFile deploymentContents = new FsFile(newDeploymentContentDir);

            TBLoaderCore.Result result = null;
            try {
                TBLoaderConfig tbLoaderConfig = getTbLoaderConfig();
                String acceptableFirmwareVersions = programSpec.getDeploymentProperties().getProperty(
                    TBLoaderConstants.ACCEPTABLE_FIRMWARE_VERSIONS);

                TBLoaderCore tbLoader = new TBLoaderCore.Builder().withTbLoaderConfig(tbLoaderConfig)
                    .withTbDeviceInfo(currentTbDevice)
                    .withTbDeviceVersion(tbLoaderPanel.getDeviceVersion())
                    .withDeploymentDirectory(deploymentContents)
                    .withOldDeploymentInfo(oldDeploymentInfo!=null?oldDeploymentInfo:newDeploymentInfo)
                    .withNewDeploymentInfo(newDeploymentInfo)
                    .withAcceptableFirmware(acceptableFirmwareVersions)
//                    .withLocation(currentLocationChooser.getSelectedItem().toString())
                    .withRefreshFirmware(tbLoaderPanel.isForceFirmware())
                    .withProgressListener(tbLoaderPanel.getProgressDisplayManager())
                    .withProgramSpec(programSpec)
                    .build();
                result = tbLoader.update();


                tbsCollected = tbLoader.getTbsCollected();
                tbsDeployed = tbLoader.getTbsDeployed();
                if (tbsCollected != null) {
                    TbHistory.getInstance().addTbCollected(tbsCollected);
                }
                if (tbsDeployed != null) {
                    TbHistory.getInstance().addTbDeployed(tbsDeployed);
                }

                opLog.put("gotstatistics", result.gotStatistics)
                    .put("corrupted", result.corrupted)
                    .put("reformatfailed",
                        result.reformatOp == TBLoaderCore.Result.FORMAT_OP.failed)
                    .put("verified", result.verified);

                if (currentTbDevice.isFsCheckTimeout()) {
                    String letter = currentTbDevice.getDriveLetter();
                    endMsg = String.format("TB-Loader was unable to check the storage on the Talking Book device.\nPlease run 'chkdsk /f %s' and try again",
                            letter);
                    endTitle = "Device Check Error";
                } else if (currentTbDevice.isFsAccessDenied()) {
                    String letter = currentTbDevice.getDriveLetter();
                    endMsg = String.format("Due to Windows Security, TB-Loader was unable to check the storage on the Talking Book device.\n" +
                                    "If there are problems with the device, please run 'chkdsk /f %s' (as Administrator) and try again",
                            letter);
                    endTitle = "Device Check Error";
                } else if (result.corrupted
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
                        if (tbLoaderPanel.getDeviceVersion() == TbDeviceInfo.DEVICE_VERSION.TBv2) {
                            endMsg = "Update failed verification.  Please try again.";
                        } else {
                            endMsg = "Update failed verification.  Try again or replace memory card.";
                        }
                    }
                    if (endTitle == null) {
                        endTitle = "Failure";
                    }
                }
                onCopyFinished(endMsg, endTitle, endMessageType);
                if (tbLoaderConfig.isDoNotUpload()) {
                    try {
                        FileUtils.cleanDirectory(collectionWorkDir);
                    } catch (IOException e) {
                        new PopUp.Builder()
                            .withTitle("Can not clean collection directory")
                            .withContents("Couldn't remove files from "+collectionWorkDir.getName()+". Please manually clean the directory" +
                                " to prevent polluting the production data.")
                            .go();
                        System.exit(1);
                    }
                } else {
                    statisticsUploader.zipAndEnqueue(collectionWorkDir, getUploadKeyPrefix(currentTbDevice));
                }
            }

        }

        @Override
        public void run() {
            tbsCollected = null;
            tbsDeployed = null;

            if (this.operation == Operation.Update) {
                collectStatsAndDeployNewContent();
            } else if (this.operation == Operation.CollectStats) {
                collectStatsOnly();
            }
        }
    }
}
