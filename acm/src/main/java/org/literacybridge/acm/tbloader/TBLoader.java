package org.literacybridge.acm.tbloader;

import org.jdesktop.swingx.JXDatePicker;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.literacybridge.acm.Constants;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.gui.Application;
import org.literacybridge.acm.gui.util.UIUtils;
import org.literacybridge.acm.utils.LogHelper;
import org.literacybridge.acm.utils.OsUtils;
import org.literacybridge.acm.utils.SwingUtils;
import org.literacybridge.core.OSChecker;
import org.literacybridge.core.fs.FsFile;
import org.literacybridge.core.fs.OperationLog;
import org.literacybridge.core.fs.TbFile;
import org.literacybridge.core.spec.ProgramSpec;
import org.literacybridge.core.tbloader.DeploymentInfo;
import org.literacybridge.core.tbloader.ProgressListener;
import org.literacybridge.core.tbloader.TBDeviceInfo;
import org.literacybridge.core.tbloader.TBLoaderConfig;
import org.literacybridge.core.tbloader.TBLoaderConstants;
import org.literacybridge.core.tbloader.TBLoaderCore;
import org.literacybridge.core.tbloader.TBLoaderUtils;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.awt.GridBagConstraints.BOTH;
import static java.awt.GridBagConstraints.CENTER;
import static java.awt.GridBagConstraints.FIRST_LINE_START;
import static java.awt.GridBagConstraints.HORIZONTAL;
import static java.awt.GridBagConstraints.LINE_START;
import static java.awt.GridBagConstraints.NONE;
import static java.awt.GridBagConstraints.RELATIVE;
import static java.lang.Math.max;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.literacybridge.core.tbloader.TBLoaderUtils.isSerialNumberFormatGood;
import static org.literacybridge.core.tbloader.TBLoaderUtils.isSerialNumberFormatGood2;

@SuppressWarnings("serial")
public class TBLoader extends JFrame {
    private static final Logger LOG = Logger.getLogger(TBLoader.class.getName());

    private JFrame applicationWindow;

    // This string must match firmware code to generate special dings on startup
    // as reminder that specific name has not been set.
    private static final String NO_COMMUNITY_SELECTED = "Non-specific";
    private static final String NON_SPECIFIC = NO_COMMUNITY_SELECTED;

    private String newDeployment;
    private String newDeploymentDescription;
    private String dateRotation;

    // Global swing components.
    private JLabel greeting;
    private Box greetingBox;

    private Box deviceBox;
    private JLabel deviceLabel;
    private JComboBox<TBDeviceInfo> driveList;

    private JLabel currentLocationLabel;
    private JComboBox<String> currentLocationChooser;

    private JLabel nextLabel;
    private JLabel prevLabel;

    private JLabel deploymentLabel;
    private JTextField newDeploymentText;
    private JTextField oldDeploymentText;

    private JLabel communityLabel;
    private JRecipientChooser recipientChooser;
    private JTextField oldCommunityText;


    private JLabel contentPackageLabel;
    private JComponent newPackageComponent;
    private JTextField newPackageText;
    private JComboBox<String> newPackageChooser;
    private JTextField oldPackageText;

    private JLabel dateLabel;
    private JXDatePicker datePicker;
    private JTextField lastUpdatedText;

    private JLabel firmwareVersionLabel;
    private JTextField newFirmwareVersionText;
    private JTextField oldFirmwareVersionText;
    private Box newFirmwareBox;

    private JLabel srnLabel;
    private Box newSrnBox;
    private JTextField newSrnText;
    private JCheckBox forceSrn;
    private JTextField oldSrnText;

    private JLabel optionsLabel;
    private JCheckBox forceFirmware;
    private JCheckBox testDeployment;

    private JButton updateButton;
    private JButton getStatsButton;
    private JComboBox<String> actionChooser;
    private JButton goButton;
    private Color defaultButtonBackgroundColor;
    private Box actionBox;

    private JTextArea statusCurrent;
    private JTextArea statusFilename;
    private JTextArea statusLog;
    private JScrollPane statusScroller;

    private boolean isNewSerialNumber;
    private TBDeviceInfo currentTbDevice;
    private String srnPrefix;
    private String newProject;
    // Let user choose deployment, or just use the latest?
    private boolean deploymentChoice;

    // Deployment info read from Talking Book.
    private DeploymentInfo oldDeploymentInfo;

    private TBLoaderConfig tbLoaderConfig;
    private String previousSrn;

    private static class TbLoaderArgs {
        @Option(name = "--oldtbs", aliases = "-o", usage = "Target OLD Talking Books.")
        boolean oldTbs = false;

        @Option(name = "--choose", aliases = "-c", usage = "Choose Deployment and/or Package.")
        boolean choices = false;

        @Argument(usage = "Project or ACM name to export.", index = 0, required = true, metaVar = "ACM")
        String project;

        @Argument(usage = "Serial number prefix, default '-b'.", index = 1, metaVar = "SRN_PREFIX")
        String srnPrefix = null;
    }

    // All content is relative to this.
    private File localTbLoaderDir;
    private File localDeploymentDir;
    private File logsDir;

    private String[] packagesInDeployment;

    // Metadata about the project. Optional, may be null.
    private ProgramSpec programSpec = null;

    // Options.
    private boolean allowPackageChoice = false;
    private boolean allowForceSrn = false;

    class WindowEventHandler extends WindowAdapter {
        @Override
        public void windowClosing(WindowEvent evt) {
            OperationLog.log("TbLoaderShutdown").finish();
            LOG.log(Level.INFO, "closing app");
            System.exit(0);
        }
    }

    private String[] currentLocationList = new String[] { "Select location...", "Community",
        "Jirapa office", "Wa office", "Other" };

    private static final String UPDATE_TB = "Update TB";
    private String[] actionList = new String[] {UPDATE_TB, "Collect Stats"};

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

        new TBLoader(tbArgs).runApplication();
    }

    private TBLoader(TbLoaderArgs tbArgs) {
        // String project, String srnPrefix, boolean deploymentChoice) {
        this.newProject = ACMConfiguration.cannonicalProjectName(tbArgs.project);

        applicationWindow = this;

        if (tbArgs.srnPrefix != null)
            srnPrefix = tbArgs.srnPrefix.toLowerCase();
        else if (tbArgs.oldTbs)
            srnPrefix = "a-";
        else
            srnPrefix = "b-"; // for latest Talking Book hardware

        this.deploymentChoice = tbArgs.choices;
        this.allowPackageChoice = tbArgs.choices;
    }

    private void runApplication() throws Exception {
        long startupTimer = -System.currentTimeMillis();

        OsUtils.enableOSXQuitStrategy();
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        this.addWindowListener(new WindowEventHandler());

        boolean oldTbs = srnPrefix.equalsIgnoreCase("a-");
        String iconName = oldTbs ? "/tb_loader-OLD-TBs.png" : "/tb_loader.png";
        URL iconURL = Application.class.getResource(iconName);
        Image iconImage = new ImageIcon(iconURL).getImage();
        if (OsUtils.MAC_OS) {
            OsUtils.setOSXApplicationIcon(iconImage);
        } else {
            applicationWindow.setIconImage(iconImage);
        }

        // Set options that are controlled by project config file.
        Properties config = ACMConfiguration.getInstance().getConfigPropertiesFor(newProject);
        if (config != null){
            String valStr = config.getProperty(Constants.DEPLOYMENT_CHOICE, "FALSE");
            this.deploymentChoice |= Boolean.parseBoolean(valStr);

            valStr = config.getProperty("PACKAGE_CHOICE", "FALSE");
            this.allowPackageChoice |= Boolean.parseBoolean(valStr);

            valStr = config.getProperty("ALLOW_FORCE_SRN", "FALSE");
            this.allowForceSrn |= Boolean.parseBoolean(valStr);
        }

        setDeviceIdAndPaths();

        // Initialized java logging, as well as operational logging.
        initializeLogging();
        OperationLog.log("WindowsTBLoaderStart")
            .put("tbcdid", tbLoaderConfig.getTbLoaderId())
            .put("project", newProject)
            .finish();

        // Looks in Dropbox and in ~/LiteracyBridge for deployments. May prompt user for which
        // Deployment version, or update to latest.
        getCurrentDeployments();

        initializeGui();

        // Populate various fields, choices.
        fillDeploymentList();
        fillCommunityList();
        fillFirmwareVersion();

//        JOptionPane.showMessageDialog(applicationWindow,
//                                      "Remember to power Talking Book with batteries before connecting with USB.",
//                                      "Use Batteries!", JOptionPane.PLAIN_MESSAGE);
        startUpDone = true;

        LOG.log(Level.INFO, "set visibility - starting drive monitoring");
        deviceMonitorThread.setDaemon(true);
        deviceMonitorThread.start();

        startupTimer += System.currentTimeMillis();
        System.out.printf("Startup in %d ms.\n", startupTimer);
    }

    /**
     * Determines the most recent deployment in ~/LiteracyBridge.
     */
    private void getCurrentDeployments() throws IOException {
        DeploymentsManager dm = new DeploymentsManager(newProject);
        DeploymentsManager.State state = dm.getState();
        boolean keepUnpublished = false; // If user chooses to keep unpublished Deployment.
        int answer;

        switch (state) {
        case Missing_Latest:
            // Problem with Dropbox, can't continue.
            String message = "TB-Loader can not determine the latest Deployment, and can not continue.\n"
                +"(There is no .rev file in the 'published' directory.)";
            JOptionPane.showMessageDialog(this, message,
                "Cannot Determine Latest Deployment", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
            break;
        case Bad_Local:
            // Either exit or delete & copy.
            Object[] optionsFix = { "Fix Automatically", "Exit and Fix Manually" };
            // Default: fix automatically
            answer = JOptionPane.showOptionDialog(this,
                "There is an error in the local deployment.\nDo you wish to exit and fix the problem yourself, or clean up automatically?",
                "Error in Local Deployment", JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE, null, optionsFix,
                optionsFix[0]);
            if (answer == JOptionPane.NO_OPTION ) {
                // User chose Exit and Fix Manually
                System.exit(1);
            }
            dm.clearLocalDeployments();
            break;
        case OK_Unpublished:
            // prompt for unpublished, keep or copy
            Object[] optionsRefresh = { "Keep Unpublished", "Refresh From Latest" };
            // Default: Keep Unpublished
            answer = JOptionPane.showOptionDialog(this,
                "This TB Loader is running an unpublished deployment.\nDo you wish to keep the unpublished version, or delete it and use the latest published version?",
                "Unpublished Deployment", JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE, null, optionsRefresh,
                optionsRefresh[0]);
            if (answer == JOptionPane.NO_OPTION) {
                // User chose Refresh.
                dm.clearLocalDeployments();
            } else {
                keepUnpublished = true;
            }

            break;
        case No_Deployment:
            // copy choice or latest
            break;
        case Not_Latest:
            // copy choice or latest
            break;
        case OK_Latest:
            // good to go
            break;
        }

        if (keepUnpublished) {
            newDeployment = dm.getLocalDeployment().localContent.getName();
            newDeploymentDescription = String.format("UNPUBLISHED: %s", newDeployment);
        } else {
            newDeployment = selectDeployment(dm);
            newDeploymentDescription = String.format("%s (%s)",
                newDeployment,
                dm.getLocalDeployment().localRevision);
        }
        localDeploymentDir = new File(localTbLoaderDir, TBLoaderConstants.CONTENT_SUBDIR + File.separator
            + newDeployment);
        fillPackageList();
    }

    /**
     * Selects the Deployment to be loaded. If the ACM is configured so that the user can
     * select, they're offered a choice of Deployments, otherwise the latest is used.
     * @param dm a DeploymentsManager with information about the local and global Deployments.
     * @return The "rev" of the deployment, like "DEMO-2018-4-b"
     * @throws IOException If there's a problem installing the Deployment.
     */
    private String selectDeployment(DeploymentsManager dm) throws IOException {
        DeploymentsManager.LocalDeployment localDeployment = dm.getLocalDeployment();
        DeploymentsManager.AvailableDeployments available = dm.getAvailableDeployments();
        String desiredDeployment = available.latestDeployment;

        if (deploymentChoice) {
            ManageDeploymentsDialog dialog = new ManageDeploymentsDialog(this, available.deployments, localDeployment.localDeployment);
            // Place the new dialog within the application frame.
            dialog.setLocation(this.getX()+20, this.getY()+20);
            dialog.setVisible(true);
            desiredDeployment = dialog.selection;
        }
        String desiredRevision = dm.getAvailableDeployments().getRevisionForDeployment(desiredDeployment);

        // If we don't have what we want, get it from Dropbox.
        if (!desiredDeployment.equals(localDeployment.localDeployment) ||
                !desiredRevision.equals(localDeployment.localRevision)) {
            Cursor waitCursor = new Cursor(Cursor.WAIT_CURSOR);
            Cursor defaultCursor = new Cursor(Cursor.DEFAULT_CURSOR);
            try {
                this.setCursor(waitCursor);
                Thread.sleep(1);
                dm.getDeployment(desiredDeployment);
            } catch (Exception ignored) {
                // Do nothing.
            } finally {
                this.setCursor(defaultCursor);
            }
        }
        return desiredDeployment;
    }


    private void initializeLogging() throws IOException {
        // Set up the program log. For debugging the execution of the TBLoader application.

        new LogHelper().inDirectory(logsDir).absolute().withName("tbloaderlog.%g").initialize();
        LOG.log(Level.INFO, "WindowsTBLoaderStart\n");

        // Set up the operation log. Tracks what is done, by whom.
        OperationLogImpl opLogImpl = new OperationLogImpl(logsDir);
        OperationLog.setImplementation(opLogImpl);
    }

    /**
     * Reads the TB-Loader id of this computer.
     * Determines where files come from, and where they go.
     * @throws IOException if any file errors.
     */
    private void setDeviceIdAndPaths() throws IOException {
        try {
            File applicationHomeDirectory = ACMConfiguration.getInstance()
                .getApplicationHomeDirectory();
            localTbLoaderDir = ACMConfiguration.getInstance().getLocalTbLoaderDirFor(newProject);
            localTbLoaderDir.mkdirs();
            TbFile softwareDir = new FsFile(ACMConfiguration.getInstance().getSoftwareDir());

            File dropboxDir = ACMConfiguration.getInstance().getGlobalShareDir();
            if (!dropboxDir.exists()) {
                JOptionPane.showMessageDialog(applicationWindow, dropboxDir.getAbsolutePath()
                        + " does not exist; cannot find the Dropbox path. Please contact ICT staff.",
                    "Cannot Find Dropbox!", JOptionPane.PLAIN_MESSAGE);
                System.exit(1);
            }

            // Look for ~/LiteracyBridge/1234.dev file.
            File[] files = applicationHomeDirectory.listFiles((dir, name) -> {
                String lowercase = name.toLowerCase();
                return lowercase.endsWith(TBLoaderConstants.DEVICE_FILE_EXTENSION);
            });
            if (files.length == 1) {
                String deviceId = files[0].getName()
                    .substring(0, files[0].getName().length() - 4)
                    .toUpperCase();

                if (!deviceId.matches("[a-fA-F0-9]{4}")) {
                    String msg = String.format("The TB-Loader uses a '*.dev' file to uniquely identify every TB-Loader laptop.\n" +
                                              "The file named '%s' is not a valid .dev file. Please contact Amplio\n to request "
                                            + "a TB-Loader id.", files[0].getName());
                    JOptionPane.showMessageDialog(applicationWindow, msg, "Invalid .dev File Name", JOptionPane.ERROR_MESSAGE);
                    LOG.log(Level.SEVERE, msg);
                    System.exit(1);
                }

                // Like "~/Dropbox/tbcd1234"
                File tbLoaderDir = new File(dropboxDir, TBLoaderConstants.COLLECTED_DATA_DROPBOXDIR_PREFIX + deviceId);
                // Like collected-data/{PROJECT}/OperationalData/{tbcdid}/logs
                String logsPath = TBLoaderConstants.COLLECTED_DATA_SUBDIR_NAME + File.separator + newProject + File.separator +
                    TBLoaderConstants.OPERATIONAL_DATA + File.separator + deviceId + File.separator + "logs";
                logsDir = new File(tbLoaderDir, logsPath);
                logsDir.mkdirs();
                if (!tbLoaderDir.exists()) {
                    JOptionPane.showMessageDialog(applicationWindow, tbLoaderDir.toString()
                            + " does not exist; cannot find the Dropbox collected data path. Please contact ICT staff.",
                        "Cannot Find Dropbox Collected Data Folder!",
                        JOptionPane.ERROR_MESSAGE);
                    System.exit(1);
                }
                // Like ~/Dropbox/tbcd1234/collected-data
                TbFile collectedDataDir = new FsFile(tbLoaderDir).open(
                    TBLoaderConstants.COLLECTED_DATA_SUBDIR_NAME);

                TbFile tempDir = new FsFile(Files.createTempDirectory("tbloader-tmp").toFile());

                tbLoaderConfig = new TBLoaderConfig.Builder()
                    .withTbLoaderId(deviceId)
                    .withCollectedDataDirectory(collectedDataDir)
                    .withTempDirectory(tempDir)
                    .withWindowsUtilsDirectory(softwareDir)
                    .withUserName(ACMConfiguration.getInstance().getUserName())
                    .build();
            } else {
                JOptionPane.showMessageDialog(applicationWindow,
                    "This computer does not appear to be configured to use the TB Loader yet.  It needs a unique device tbSrn. Please contact ICT staff to get this.",
                    "This Computer has no ID!",
                    JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Exception while setting DeviceId and paths", e);
            throw e;
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

    private void initializeGui() {
        this.backgroundColor = getBackground();

        SwingUtils.setLookAndFeel("seaglass");

        JPanel panel = createComponents();

        layoutComponents(panel);

        setSize(700, 700);
        add(panel, BorderLayout.CENTER);
        setLocationRelativeTo(null);

        setVisible(true);
    }

    /**
     * Sets the widths of columns 1 & 2 in the grid. Examines all components in those two
     * columns, finding the largest minimum width. It then sets the minimum width of the next/
     * prev labels to that width, thereby setting the two columns' minimum widths to the same
     * value. That, in turn, causes the two columns to be the same width, and to resize together.
     */
    private JComponent[] columnComponents;
    private void setGridColumnWidths() {
        if (columnComponents == null) {
            columnComponents = new JComponent[] {
                // This list should contain all components in columns 1 & 2, though not those
                // spanning multiple columns.
                // This list need not contain prevLabel or nextLabel; we assume that those two
                // are "small-ish", and won't actually provide the maximimum minimum width.
                driveList,
                currentLocationChooser,
                newDeploymentText, oldDeploymentText,
                recipientChooser, oldCommunityText,
                newPackageComponent, oldPackageText,
                datePicker, lastUpdatedText,
                newFirmwareVersionText, oldFirmwareVersionText,
                newSrnBox, oldSrnText,
                newFirmwareBox,
                updateButton,
                getStatsButton
            };
        }
        int maxMinWidth = 0;
        for (JComponent c : columnComponents) {
            if (c != null)
                maxMinWidth = max(maxMinWidth, c.getMinimumSize().width);
        }
        Dimension d = nextLabel.getMinimumSize();
        d.width = maxMinWidth;
        nextLabel.setMinimumSize(d);
        prevLabel.setMinimumSize(d);
    }

    /**
     * Our preferred default GridBagConstraint.
     * @param x column
     * @param y row
     * @return the new GridBagConstraint
     */
    private GridBagConstraints gbc(int x, int y) {
        Insets zi = new Insets(0,3,2,2);
        return new GridBagConstraints(x, y, 1, 1, 0, 0, LINE_START, HORIZONTAL, zi, 0, 0);
    }
    
    private void layoutComponents(JPanel panel) {
        panel.setBorder(new EmptyBorder(9,10,9,9));
        GridBagLayout layout = new GridBagLayout();
        panel.setLayout(layout);
        GridBagConstraints c;

        int y=0;
        // Greeting.
        c = gbc(0, y++);
        c.gridwidth = 3;
        c.fill = NONE;
        panel.add(greetingBox, c);

        // TB Drive letter / volume label.
        // Greeting.
        c = gbc(0, y++);
        c.gridwidth = 3;
        c.fill = NONE;
        panel.add(deviceBox, c);
        //layoutLine(panel, y++, deviceLabel, driveList, testDeployment);

        // Next / Previous
        c = gbc(1, y++);
        c.weightx = 1;
        panel.add(nextLabel, c);
        c.gridx = RELATIVE;
        panel.add(prevLabel, c);

        // Recipient Chooser.
        c = gbc(0, y++);
        c.anchor = FIRST_LINE_START;
        panel.add(communityLabel, c);
        c.gridx = RELATIVE;
        panel.add(recipientChooser, c);
        panel.add(oldCommunityText, c);

        c = gbc(0, y++);
        panel.add(deploymentLabel, c);
        c.gridx = RELATIVE;
        panel.add(newDeploymentText, c);
        panel.add(oldDeploymentText, c);

        // Package (aka 'Content', aka 'image')
        layoutLine(panel, y++, contentPackageLabel, newPackageComponent, oldPackageText);
        
        // Deployment date.
        layoutLine(panel, y++, dateLabel, datePicker, lastUpdatedText);
        
        // Firmware version.
        layoutLine(panel, y++, firmwareVersionLabel, newFirmwareBox, oldFirmwareVersionText);
        
        // TB Serial Number.
        layoutLine(panel, y++, srnLabel, newSrnBox, oldSrnText);

        // Action Buttons.
        c = gbc(0, y++);
        c.fill = NONE;
        c.anchor = CENTER;
        c.gridwidth = 3;
        panel.add(actionBox, c);

        // Status display
        c = gbc(0, y++);
        c.gridwidth = 3;
        panel.add(statusCurrent, c);
        c.gridy= RELATIVE;
        panel.add(statusFilename, c);
        c.weighty = 1;
        c.fill = BOTH;
        panel.add(statusScroller, c);

        // Set the checkbox minimum heights to a label's minimum height. Otherwise they have extra
        // vertical space.
        int checkboxHeight = optionsLabel.getMinimumSize().height;
        testDeployment.setMinimumSize(new Dimension(testDeployment.getMinimumSize().width, checkboxHeight));
        forceFirmware.setMinimumSize(new Dimension(forceFirmware.getMinimumSize().width, checkboxHeight));

        setGridColumnWidths();
        currentLocationChooser.doLayout();
    }

    private void layoutLine(JPanel panel, int lineNo, JComponent label, JComponent newValue, JComponent oldValue) {
        GridBagConstraints c = gbc(0, lineNo);
        panel.add(label, c);
        c.gridx = RELATIVE;
        panel.add(newValue, c);
        if (oldValue != null) {
            panel.add(oldValue, c);
        }
    }

    private JPanel createComponents() {
        JPanel panel = new JPanel();

        if (srnPrefix.equals("a-")) {
            panel.setBackground(Color.CYAN);
        } else {
        }

        greetingBox = Box.createHorizontalBox();
        String greetingString = String.format("<html><nobr>Hello <b>%s!</b> <i><span style='font-size:0.85em;color:gray'>(TB-Loader ID: %s)</span></i></nobr></html>",
            ACMConfiguration.getInstance().getUserName(),
            tbLoaderConfig.getTbLoaderId());
        greeting = new JLabel(greetingString);
        greetingBox.add(greeting);
        greetingBox.add(Box.createHorizontalStrut(10));
        // Select "Community", "LBG Office", "Other"
        currentLocationLabel = new JLabel("Updating from:");
        currentLocationChooser = new JComboBox<>(currentLocationList);
        currentLocationChooser.setBorder(new LineBorder(Color.RED, 1, true));
        currentLocationChooser.addActionListener(e -> {
            Border border;
            if (currentLocationChooser.getSelectedIndex() == 0) {
                border = new LineBorder(Color.RED, 1, true);
            } else {
                border = new LineBorder(new Color(0, 0, 0, 0), 1, true);
            }
            currentLocationChooser.setBorder(border);
            setEnabledStates();
        });
        greetingBox.add(currentLocationLabel);
        greetingBox.add(Box.createHorizontalStrut(10));
        greetingBox.add(currentLocationChooser);

        // "Use with NEW TBs only" / "Use with OLD TBs only"
        // Options
        optionsLabel = new JLabel("Options:");
        forceFirmware = new JCheckBox();
        forceFirmware.setText("Force refresh");
        forceFirmware.setSelected(false);
        forceFirmware.setToolTipText("Check to force a re-flash of the firmware. This should almost never be needed.");

        testDeployment = new JCheckBox();
        testDeployment.setText("Deploying today for testing only.");
        testDeployment.setSelected(false);
        testDeployment.setToolTipText("Check if only testing the Deployment. Uncheck if sending the Deployment out to the field.");

        forceSrn = new JCheckBox();
        forceSrn.setText("Replace");
        forceSrn.setSelected(false);
        forceSrn.setToolTipText("Check to force a new Serial Number. DO NOT USE THIS unless "
            + "you have a good reason to believe that this SRN has been "
            + "duplicated to multiple Talking Books. This should be exceedingly rare.");
        forceSrn.addActionListener(this::forceSrnListener);
        forceSrn.setVisible(allowForceSrn);

        // Windows drive letter and volume name.
        deviceBox = Box.createHorizontalBox();
        deviceLabel = new JLabel("Talking Book Device:");
        deviceBox.add(deviceLabel);
        deviceBox.add(Box.createHorizontalStrut(10));
        driveList = new JComboBox<>();
        deviceBox.add(driveList);
        deviceBox.add(Box.createHorizontalStrut(10));
        driveList.addItemListener(this::onTbDeviceChanged);
        deviceBox.add(testDeployment);

        // Headings for "Next", "Previous"
        nextLabel = new JLabel("Next");
        prevLabel = new JLabel("Previous");

        // Deployment name / version.
        deploymentLabel = new JLabel("Deployment:");
        newDeploymentText = new JTextField();
        oldDeploymentText = new JTextField();
        oldDeploymentText.setEditable(false);

        communityLabel = new JLabel("Community:");
        recipientChooser = new JRecipientChooser();
        recipientChooser.addActionListener(this::onCommunitySelected);
        oldCommunityText = new JTextField();
        oldCommunityText.setEditable(false);

        // Show Content Package name.
        contentPackageLabel = new JLabel("Content Package:");
        if (usePackageChooser()) {
            newPackageChooser = new JComboBox<>(packagesInDeployment);
            newPackageComponent = newPackageChooser;
        } else {
            newPackageText = new JTextField();
            newPackageText.setEditable(false);
            newPackageComponent = newPackageText;
        }
        oldPackageText = new JTextField();
        oldPackageText.setEditable(false);

        // Select "First Rotation Date", default today.
        dateLabel = new JLabel("First Rotation Date:");
        datePicker = new JXDatePicker(new Date());
        dateRotation = datePicker.getDate().toString();
        datePicker.getEditor().setEditable(false);
        datePicker.setFormats("yyyy/MM/dd"); //dd MMM yyyy
        datePicker.addActionListener(e -> dateRotation = datePicker.getDate().toString());
        lastUpdatedText = new JTextField();
        lastUpdatedText.setEditable(false);

        // Show firmware version.
        firmwareVersionLabel = new JLabel("Firmware:");
        newFirmwareVersionText = new JTextField();
        newFirmwareVersionText.setEditable(false);
        newFirmwareBox = Box.createHorizontalBox();
        newFirmwareBox.add(newFirmwareVersionText);
        newFirmwareBox.add(Box.createHorizontalStrut(10));
        newFirmwareBox.add(forceFirmware);

        oldFirmwareVersionText = new JTextField();
        oldFirmwareVersionText.setEditable(false);

        // Show serial number.
        srnLabel = new JLabel("Serial number:");
        newSrnText = new JTextField();
        newSrnText.setEditable(false);
        newSrnBox = Box.createHorizontalBox();
        newSrnBox.add(newSrnText);
        newSrnBox.add(Box.createHorizontalStrut(10));
        newSrnBox.add(forceSrn);

        oldSrnText = new JTextField();
        oldSrnText.setEditable(false);

        // Update / gather statistics buttons.
        updateButton = new JButton("Update TB");
        updateButton.setEnabled(false);
        updateButton.addActionListener(this::buttonActionPerformed);
        getStatsButton = new JButton("Get Stats");
        getStatsButton.setEnabled(false);
        getStatsButton.addActionListener(this::buttonActionPerformed);

        actionChooser = new JComboBox<String>(actionList);
        goButton = new JButton("Go!");
        defaultButtonBackgroundColor = goButton.getBackground();
        goButton.setEnabled(false);
        goButton.setForeground(Color.GRAY);
        goButton.setOpaque(true);
        goButton.addActionListener(this::buttonActionPerformed);
        actionBox = Box.createHorizontalBox();
        actionBox.add(Box.createHorizontalGlue());
        actionBox.add(actionChooser);
        actionBox.add(goButton);
        actionBox.add(Box.createHorizontalGlue());

        // This will paint the face of the button, but then clicking has no visual effect.
//        goButton.setBorderPainted(false);


        // Show status.
        statusCurrent = new JTextArea(1, 80);
        statusCurrent.setEditable(false);
        statusCurrent.setLineWrap(true);

        statusFilename = new JTextArea(1, 80);
        statusFilename.setEditable(false);
        statusFilename.setLineWrap(false);
        statusFilename.setFont(new Font("Sans-Serif", Font.PLAIN, 10));

        statusLog = new JTextArea(2, 80);
        statusLog.setEditable(false);
        statusLog.setLineWrap(true);

        statusScroller = new JScrollPane(statusLog);
        statusScroller.setBorder(null); // eliminate black border around status log
        statusScroller.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

        return panel;
    }

    /**
     * If user checks the option to allocate a new SRN, this will prompt them to consider their
     * choice. Switches the display between the old SRN and "-- to be assigned --".
     * @param actionEvent is unused.
     */
    private void forceSrnListener(ActionEvent actionEvent) {
        if (forceSrn.isSelected()) {
            List<Object> options = new ArrayList<Object>();
            Object defaultOption;
            options.add(UIManager.getString("OptionPane.yesButtonText"));
            options.add(UIManager.getString("OptionPane.noButtonText"));
            defaultOption = UIManager.getString("OptionPane.noButtonText");

            String message = "Are you sure that you want to allocate a new Serial Number" + "\nfor this Talking Book? You should not do this unless you have"
                + "\na very good reason to believe that this SRN is duplicated on" + "\nmore than one Talking Book.";
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
                newSrnText.setText(TBLoaderConstants.NEED_SERIAL_NUMBER);
            } else {
                forceSrn.setSelected(false);
            }
        } else {
            newSrnText.setText(previousSrn);
        }
    }

    /**
     * Looks in the ~/LiteracyBridge/TB-Loaders/{project}/content/{deployment}/basic directory
     * for files named '*.img'. Any found are assumed to be firmware images for v1 talking books.
     *
     * There *should* be exactly one (the TB-Builder should have selected the highest numbered one).
     *
     * Populates the newFirmwareVersionText field.
     */
    private void fillFirmwareVersion() {
        String firmwareVersion = "(No firmware)";

        // Like ~/LiteracyBridge/TB-Loaders/{project}/content/{deployment}/basic
        File basicContentPath = new File(localTbLoaderDir,
                                         TBLoaderConstants.CONTENT_SUBDIR + File.separator
                                             + newDeployment
                                             + File.separator + TBLoaderConstants.CONTENT_BASIC_SUBDIR);
        LOG.log(Level.INFO, "DEPLOYMENT:" + newDeployment);
        try {
            File[] files;
            if (basicContentPath.exists()) {
                // get Package
                files = basicContentPath.listFiles((dir, name) -> {
                    String lowercase = name.toLowerCase();
                    return lowercase.endsWith(".img");
                });
                if (files.length > 1)
                    firmwareVersion = "(Multiple Firmwares!)";
                else if (files.length == 1) {
                    firmwareVersion = files[0].getName();
                    firmwareVersion = firmwareVersion.substring(0, firmwareVersion.length() - 4);
                }
                newFirmwareVersionText.setText(firmwareVersion);
            }
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "exception - ignore and keep going with default string", ex);
        }

    }

    private File prevSelected = null;
    private int prevSelectedCommunity = -1;

    private synchronized void fillDeploymentList() {
        newDeploymentText.setText(newDeploymentDescription);
    }

    private synchronized void fillCommunityList() {
        File[] files;

        File fCommunityDir = new File(localTbLoaderDir,
                                      TBLoaderConstants.CONTENT_SUBDIR + File.separator
                                              + newDeployment + File.separator
                                              + TBLoaderConstants.COMMUNITIES_SUBDIR);

        files = fCommunityDir.listFiles((dir, name) -> dir.isDirectory());

        File programspecDir = new File(localTbLoaderDir,
            TBLoaderConstants.CONTENT_SUBDIR + File.separator + newDeployment + File.separator
                + Constants.ProgramSpecDir);
        try {
            programSpec = new ProgramSpec(programspecDir);
            recipientChooser.populate(programSpec, files);
            validate();
        } catch (Exception ignored) {
        }
    }

    private String getRecipientIdForCommunity(String communityDirName) {
        // ~/LiteracyBridge/TB-Loaders/{project}/content/{deployment}/communities/{communitydir}
        File deploymentDirectory = new File(localTbLoaderDir,
            TBLoaderConstants.CONTENT_SUBDIR + File.separator
                + newDeployment);

        return TBLoaderUtils.getRecipientIdForCommunity(deploymentDirectory, communityDirName);
    }

    private String getSelectedCommunity() {
        String communityDir = null;
        communityDir = recipientChooser.getCommunityDirectory();
        return communityDir;
    }

    private void selectCommunityFromCurrentDrive() {
        String community = oldCommunityText.getText();
        String recipientid = null;
        if (community != null && community.length() == 0) {
            // Select "no community".
            recipientid = "";
        } else if (oldDeploymentInfo != null) {
            recipientid = oldDeploymentInfo.getRecipientid();
        }
        recipientChooser.setSelectedCommunity(community, recipientid);
        fillPackageFromCommunity(community);
    }

    private void fillPackageList() {
        packagesInDeployment = TBLoaderUtils.getPackagesInDeployment(localDeploymentDir);
    }
    private boolean usePackageChooser() {
        return allowPackageChoice && packagesInDeployment != null && packagesInDeployment.length > 1;
    }
    private void setNewPackage(String newPackage) {
        if (usePackageChooser()) {
            newPackageChooser.setSelectedItem(newPackage);
        } else {
            newPackageText.setText(newPackage);
        }
    }
    private String getNewPackage() {
        if (usePackageChooser()) {
            return newPackageChooser.getSelectedItem().toString();
        } else {
            return newPackageText.getText();
        }
    }

    /**
     * Allocates and persists the next serial number for this device (pc running TB-Loader).
     *
     * @return The next serial number.
     */
    private int allocateNextSerialNumberFromTbLoader() throws Exception {
        int serialnumber = loadSerialNumber();

        if (serialnumber >= 0xFFFF) {
            throw new Exception("SRN out of bounds for this TB Loader device. (Too many assigned.)");
        } else if (serialnumber < 0) {
            serialnumber = TBLoaderConstants.STARTING_SERIALNUMBER;
        }
        if (serialnumber == TBLoaderConstants.STARTING_SERIALNUMBER) {
            // if file doesn't exist, use the SRN = STARTING_SERIALNUMBER
            // TODO:raise exception and tell user to register the device or ensure file wasn't lost
        }

        // The number we're assigning now...
        serialnumber++;

        // Save into the local directory, and to Dropbox.
        saveSerialNumber(serialnumber);

        return serialnumber;
    }

    /**
     * Loads the most recently assigned "serial number". Looks in the local directory, and in the
     * ~/Dropbox/tbcd1234 directory. Looks for a binary file named "1234.dev", and for ascii text
     * files with the number encoded as a 4-digit hex string, in "1234.txt" and "1234.hex". The
     * name "1234.txt" is deprecated.
     * @return The serial number read, or -1 if none read.
     */
    private int loadSerialNumber() {
        String deviceId = tbLoaderConfig.getTbLoaderId();
        File tbloaderIdDir = new File(ACMConfiguration.getInstance().getGlobalShareDir(),
            TBLoaderConstants.COLLECTED_DATA_DROPBOXDIR_PREFIX + deviceId);

        int localSn = loadSerialNumber(deviceId, ACMConfiguration.getInstance().getApplicationHomeDirectory());
        int backupSn = loadSerialNumber(deviceId, tbloaderIdDir);

        return max(localSn, backupSn);
    }

    static int loadSerialNumber(String deviceId, File backupDir) {
//        String deviceId = tbLoaderConfig.getTbLoaderId();
        String binaryFilename = deviceId + TBLoaderConstants.DEVICE_FILE_EXTENSION; // xxxx.dev
        File binaryFile = new File(backupDir, binaryFilename);
        File textFile = new File(backupDir, deviceId + ".txt");
        File hexFile = new File(backupDir, deviceId + ".hex");

        int binarySn = -1;
        int textSn = readHexSn(textFile);
        int hexSn = readHexSn(hexFile);

        if (binaryFile.exists()) {
            try (DataInputStream is = new DataInputStream(new FileInputStream(binaryFile))) {
                binarySn = is.readInt();
            } catch (IOException e) {
                String msg = String.format("Unexpected Exception reading %s in directory %s. Ignoring this file.",
                    binaryFile.getName(), binaryFile.getParentFile().getName());
                LOG.log(Level.WARNING, msg, e);
            }
        }
        return max(binarySn, max(textSn, hexSn));
    }

    private static int readHexSn(File file) {
        int result = -1;
        if (file.exists()) {
            try (FileReader fr = new FileReader(file);
                BufferedReader br = new BufferedReader(fr)) {
                String line = br.readLine().trim();
                if (line.length() == 4) {
                    result = Integer.parseInt(line, 16);
                }
            } catch (Exception e) {
                String msg = String.format("Unexpected Exception reading or parsing %s in directory %s. Ignoring this file.",
                    file.getName(), file.getParentFile().getName());
                LOG.log(Level.WARNING, msg, e);
            }
        }
        return result;
    }

    private void saveSerialNumber(int serialnumber) throws IOException {
        String deviceId = tbLoaderConfig.getTbLoaderId();
        File tbloaderIdDir = new File(ACMConfiguration.getInstance().getGlobalShareDir(),
            TBLoaderConstants.COLLECTED_DATA_DROPBOXDIR_PREFIX + deviceId);

        saveSerialNumber(deviceId, serialnumber, ACMConfiguration.getInstance().getApplicationHomeDirectory());
        saveSerialNumber(deviceId, serialnumber, tbloaderIdDir);
    }

    /**
     * Save the given "serial number" into the given directory. The file is saved in binary, to
     * a file named 1234.dev (where "1234" is the user's tb-loader id), and as a 4-digit hex
     * string to 1234.hex. If a file named 1234.txt exists, it will be deleted.
     *
     * @param serialnumber The serial number to be saved.
     * @param backupDir Directory into which to write the files.
     * @throws IOException if one of the files can't be written.
     */
    static void saveSerialNumber(String deviceId, int serialnumber, File backupDir)
        throws IOException
    {
//        String deviceId = tbLoaderConfig.getTbLoaderId();
        String binaryFilename = deviceId + TBLoaderConstants.DEVICE_FILE_EXTENSION; // xxxx.dev
        String textFilename = deviceId + ".txt";
        String hexFilename = deviceId + ".hex";
        File binaryFile = new File(backupDir, binaryFilename);
        File textFile = new File(backupDir, textFilename);
        File hexFile = new File(backupDir, hexFilename);

        try (DataOutputStream os = new DataOutputStream(new FileOutputStream(binaryFile))) {
            os.writeInt(serialnumber);
        }
        try (FileWriter fw = new FileWriter(hexFile);
            PrintWriter pw = new PrintWriter(fw)) {
            pw.printf("%04x", serialnumber);
        }
        if (textFile.exists()) {
            textFile.delete();
        }
    }

    private synchronized void fillDriveList(File[] roots) {
        driveList.removeAllItems();
        int index = -1;
        int i = 0;
        for (File root : roots) {

            String label = FileSystemView.getFileSystemView().getSystemDisplayName(root);
            if (label.trim().equals("CD Drive") || label.startsWith("DVD") ||
                    label.contains("Macintosh")) {
                continue;
            }
            // Ignore network drives. Includes host drives shared by Parallels.
            String typeDescr = FileSystemView.getFileSystemView().getSystemTypeDescription(root);
            if (typeDescr != null && typeDescr.equalsIgnoreCase("network drive")) {
                continue;
            }
            driveList.addItem(new TBDeviceInfo(new FsFile(root), label, srnPrefix));
            if (prevSelected != null && root.getAbsolutePath().equals(
                    prevSelected.getAbsolutePath())) {
                index = i;
            } else if (label.startsWith("TB") || (label.length() > 1 && label.substring(1, 2)
                    .equals("-")))
                index = i;
            i++;
        }
        if (driveList.getItemCount() == 0) {
            LOG.log(Level.INFO, "No drives");
            driveList.addItem(TBDeviceInfo.getNullDeviceInfo());
            index = 0;
        }

        if (index == -1) {
            index = i - 1;
        }
        if (index != -1) {
            driveList.setSelectedIndex(index);
            currentTbDevice = (TBDeviceInfo) driveList.getSelectedItem();
        }
        driveList.doLayout();
    }

    private synchronized File[] getRoots() {
        List<File> roots = new ArrayList<>();
        // changing line below to allow TBLoader to run as a single .class file
        // (until new ACM version is running on Fidelis's laptop)
        if (OsUtils.WINDOWS) {
            for (File r : File.listRoots()) {
                if (r.getAbsoluteFile().toString().compareTo("D:") >= 0 && r.listFiles() != null) {
                    roots.add(r);
                }
            }
        } else if (OsUtils.MAC_OS) {
            for (File r : new File("/Volumes").listFiles()) {
                if (r.listFiles() != null) {
                    roots.add(r);
                }
            }
        }
        return roots.isEmpty() ? new File[0] : roots.toArray(new File[roots.size()]);
    }

    private Thread deviceMonitorThread = new Thread() {
        @Override
        public void run() {
            Set<String> oldList = new HashSet<>();

            while (true) {

                // don't do this during a TB update; we know there will be drive changes, but want to ignore them
                // until the update is finished.
                if (!updatingTB) {
                    File[] roots = getRoots();

                    boolean needRefresh = oldList.size() != roots.length;
                    if (!needRefresh) {
                        for (File root : roots) {
                            if (!oldList.contains(root.getAbsolutePath())) {
                                needRefresh = true;
                                break;
                            }
                        }
                    }

                    if (needRefresh) {
                        refreshingDriveInfo = true;
                        LOG.log(Level.INFO, "deviceMonitor sees new drive");
                        UIUtils.invokeAndWait(() -> {
                            fillDriveList(roots);
                            if (!driveList.getItemAt(0).getLabel().equals(TBLoaderConstants.NO_DRIVE)) {
                                statusDisplay.clearLog();
                            }
                            populatePreviousValuesFromCurrentDrive();
                            selectCommunityFromCurrentDrive();
                            refreshUI();
                        });
                        oldList.clear();
                        for (File root : roots) {
                            oldList.add(root.getAbsolutePath());
                        }
                        refreshingDriveInfo = false;
                    }
                }
                try {
                    sleep(2000);
                } catch (InterruptedException e) {
                    LOG.log(Level.WARNING, "Exception while refreshing list of connected devices.",
                            e);
                    throw new RuntimeException(e);
                }

            }
        }
    };

    private void fillPackageFromCommunity(String community) {
        // ~/LiteracyBridge/TB-Loaders/{project}/content/{deployment}
        File deploymentDirectory = new File(localTbLoaderDir,
                                            TBLoaderConstants.CONTENT_SUBDIR + File.separator
                                                    + newDeployment);
        String imageName;
        if (isNotEmpty(community)) {
            imageName = TBLoaderUtils.getImageForCommunity(deploymentDirectory, community);
        } else {
            imageName = "";
        }
        setNewPackage(imageName);
    }

    /**
     * Populates the values in the right-hand side, the "previous deployment" side of the main screen.
     */
    private void populatePreviousValuesFromCurrentDrive() {
        String driveLabel = currentTbDevice.getLabelWithoutDriveLetter();
        if (!driveLabel.equals(TBLoaderConstants.NO_DRIVE) && !isSerialNumberFormatGood(srnPrefix,
                                                                                        driveLabel)) {
            // could not find flashStats file -- but TB should save flashstats on normal shutdown and on *-startup.
            JOptionPane.showMessageDialog(applicationWindow,
                                          "The TB's statistics cannot be found. Please follow these steps:\n 1. Unplug the TB\n 2. Hold down the * while turning on the TB\n "
                                                  + "3. Observe the solid red light.\n 4. Now plug the TB into the laptop.\n 5. If you see this message again, please continue with the loading -- you tried your best.",
                                          "Cannot find the statistics!",
                                          JOptionPane.PLAIN_MESSAGE);
        }

        String sn = currentTbDevice.getSerialNumber();

        if (!isSerialNumberFormatGood(srnPrefix, sn)) {
            if (sn != null && sn.length() > 2 && sn.substring(1, 2).equals("-")) {
                if (sn.compareToIgnoreCase("a-") == 0) {
                    JOptionPane.showMessageDialog(applicationWindow,
                                                  "This appears to be an OLD TB.  If so, please close this program and open the TB Loader for old TBs.",
                                                  "OLD TB!", JOptionPane.WARNING_MESSAGE);
                    return;
                } else if (sn.compareToIgnoreCase("b-") == 0) {
                    JOptionPane.showMessageDialog(applicationWindow,
                                                  "This appears to be a NEW TB.  If so, please close this program and open the TB Loader for new TBs.",
                                                  "NEW TB!", JOptionPane.WARNING_MESSAGE);
                    return;
                }
            }
        }

        oldDeploymentInfo = currentTbDevice.createDeploymentInfo(newProject);
        if (oldDeploymentInfo != null) {
            oldSrnText.setText(oldDeploymentInfo.getSerialNumber());
            oldFirmwareVersionText.setText(oldDeploymentInfo.getFirmwareRevision());
            oldPackageText.setText(oldDeploymentInfo.getPackageName());
            oldDeploymentText.setText(oldDeploymentInfo.getDeploymentName());
            lastUpdatedText.setText(oldDeploymentInfo.getUpdateTimestamp());

            //TODO: Better check that this works properly!
            previousSrn = oldDeploymentInfo.getSerialNumber();
            boolean needSrn = (previousSrn.equalsIgnoreCase(TBLoaderConstants.NEED_SERIAL_NUMBER) ||
                !isSerialNumberFormatGood(srnPrefix, previousSrn) ||
                !isSerialNumberFormatGood2(previousSrn));
            if (needSrn) previousSrn = TBLoaderConstants.NEED_SERIAL_NUMBER;
            newSrnText.setText(previousSrn);
            forceSrn.setVisible(allowForceSrn && !needSrn);
            forceSrn.setSelected(false);

            oldCommunityText.setText(oldDeploymentInfo.getCommunity());
            // If we want to do this, we need to add a display for old project (and new one, as well).
            // oldProjectText.setText(oldDeploymentInfo.getProjectName());

            // If the TB was previously used for testing, likely it is again.
            // TODO: If TB was previously used in testing, ask user if it still is.
//            if (oldDeploymentInfo.isTestDeployment()) {
//                testDeployment.setSelected(true);
//            }
            testDeployment.setSelected(false);

        } else {
            newSrnText.setText(TBLoaderConstants.NEED_SERIAL_NUMBER);
            forceSrn.setVisible(false);
            forceSrn.setSelected(false);

            oldSrnText.setText("");
            oldFirmwareVersionText.setText("");
            oldPackageText.setText("");
            oldDeploymentText.setText("");
            lastUpdatedText.setText("");
            oldCommunityText.setText("");
        }
    }

    /**
     * Handles combo box selections for the Drive list, Community list, and Deployment List.
     *
     * @param e The combo selection event.
     */

    /**
     * Handles combo box selections for the Drive list.
     *
     * @param e The combo selection event.
     */
    private void onTbDeviceChanged(ItemEvent e) {
        // Is this a new value?
        if (e.getStateChange() != ItemEvent.SELECTED)
            return;
        if (refreshingDriveInfo || !startUpDone)
            return;

        oldSrnText.setText("");
        newSrnText.setText("");
        previousSrn = "";
        forceSrn.setVisible(false);
        forceSrn.setSelected(false); // don't leave stale, invisible value.
        currentTbDevice = (TBDeviceInfo) driveList.getSelectedItem();
        if (currentTbDevice != null && currentTbDevice.getRootFile() != null) {
            LOG.log(Level.INFO,
                "Drive changed: " + currentTbDevice.getRootFile().toString() + currentTbDevice.getLabel());
            populatePreviousValuesFromCurrentDrive();
            selectCommunityFromCurrentDrive();
        }
    }
    /**
     * Handles combo box selections for the Community list.
     *
     * @param e The combo selection event.
     */
    private void onCommunitySelected(ActionEvent e) {
        if (refreshingDriveInfo || !startUpDone)
            return;

        String dir = recipientChooser.getCommunityDirectory();
        if (dir != null) {
            fillPackageFromCommunity(dir);
        } else {
            setNewPackage("");
        }
        setEnabledStates();
    }

    /**
     * Handles button presses for the "Update" and "Get Stats" buttons.
     *
     * @param e The button press event.
     */
    private void buttonActionPerformed(ActionEvent e) {
        Operation operation = Operation.Update;
        JButton theButton = (JButton) e.getSource();

        if (refreshingDriveInfo || !startUpDone)
            return;

        if (theButton == updateButton) {
            operation = Operation.Update;
        } else if (theButton == getStatsButton) {
            operation = Operation.CollectStats;
        } else if (theButton == goButton) {
            boolean isUpdate = actionChooser.getSelectedItem().toString().equalsIgnoreCase(UPDATE_TB);
            operation = isUpdate ? Operation.Update : Operation.CollectStats;
        } else{
            throw new IllegalArgumentException("'buttonActionPerformed' called for unknown button");
        }

        setEnabledStates();
        try {
            LOG.log(Level.INFO, "ACTION: " + theButton.getText());

            TbFile drive = currentTbDevice.getRootFile();
            if (drive == null) {
                refreshUI();
                return;
            }
            String devicePath = drive.getAbsolutePath();
            prevSelected = new File(devicePath);

            String community = getSelectedCommunity();
            LOG.log(Level.INFO, "Community: " + community);

            if (operation == Operation.Update) {
//                if (dateRotation == null || currentLocationChooser.getSelectedIndex() == 0) {
//                    StringBuilder text = new StringBuilder("You must first select ");
//                    StringBuilder heading = new StringBuilder("Need ");
//                    String joiner = "";
//                    if (dateRotation == null) {
//                        text.append("a rotation date");
//                        heading.append("Date");
//                        joiner = " and ";
//                    }
//                    if (currentLocationChooser.getSelectedIndex() == 0) {
//                        text.append(joiner).append("your location");
//                        heading.append(joiner).append("Location");
//                    }
//                    text.append(".");
//                    heading.append("!");
//                    JOptionPane.showMessageDialog(applicationWindow,
//                                                  text.toString(),
//                                                  heading.toString(),
//                                                  JOptionPane.PLAIN_MESSAGE);
//                    refreshUI();
//                    return;
//                }
                
                if (getNewPackage().equalsIgnoreCase(TBLoaderConstants.MISSING_PACKAGE)) {
                    String text = "Can not update a Talking Book for this Community,\n" +
                        "because there is no Content Package.";
                    String heading = "Missing Package";
                    JOptionPane.showMessageDialog(applicationWindow,
                        text,
                        heading,
                        JOptionPane.PLAIN_MESSAGE);
                    refreshUI();
                    return;
                }

                if (community.equals(NON_SPECIFIC)) {
                    int response = JOptionPane.showConfirmDialog(this,
                                                                 "No community selected. This will prevent us from "+
                                                                         "generating accurate usage statistics.\nAre you sure?",
                                                                 "Confirm",
                                                                 JOptionPane.YES_NO_OPTION);
                    if (response == JOptionPane.YES_OPTION) {
                        // Give them a second chance to do the right thing.
                        response = JOptionPane.showConfirmDialog(this,
                            "Without the community, we can not properly track deployments and usage.\n"+
                                "If the community is missing, please quit and ask that a correct Deployment be generated.\n"+
                                "Are you absolutely sure you want to continue with no community?",
                            "Please Select Community",
                            JOptionPane.YES_NO_OPTION);
                    }
                    if (response != JOptionPane.YES_OPTION) {
                        LOG.log(Level.INFO, "No community selected. Are you sure? NO");
                        refreshUI();
                        return;
                    } else
                        LOG.log(Level.INFO, "No community selected. Are you sure? YES");
                }
                
                // If the Talking Book needs a new serial number, allocate one. We did not do it before this to
                // avoid wasting allocations.
                String srn = newSrnText.getText();
                isNewSerialNumber = false;
                if (forceSrn.isSelected() ||
                        srn.equalsIgnoreCase(TBLoaderConstants.NEED_SERIAL_NUMBER) ||
                        !isSerialNumberFormatGood(srnPrefix, srn) ||
                        !isSerialNumberFormatGood2(srn)) {
                    int intSrn = allocateNextSerialNumberFromTbLoader();
                    isNewSerialNumber = true;
                    String lowerSrn = String.format("%04x", intSrn);
                    srn = (srnPrefix + tbLoaderConfig.getTbLoaderId() + lowerSrn).toUpperCase();
                    currentTbDevice.setSerialNumber(srn);
                    newSrnText.setText(srn);
                }
            }
            if (operation != Operation.Update && dateRotation == null) {
                dateRotation = new Date().toString();
            }

            LOG.log(Level.INFO, "ID:" + currentTbDevice.getSerialNumber());
            statusDisplay.clear("STATUS: Starting");

            updatingTB = true;
            setEnabledStates();
            CopyThread t = new CopyThread(devicePath, operation);
            t.start();

        } catch (Exception ex) {
            LOG.log(Level.WARNING, ex.toString(), ex);
            JOptionPane.showMessageDialog(applicationWindow, "An error occured.", "Error",
                                          JOptionPane.ERROR_MESSAGE);
            fillDeploymentList();
            resetUI(false);
        }
    }

    private void resetUI(boolean resetDrives) {
        LOG.log(Level.INFO, "Resetting UI");
        oldSrnText.setText("");
        newSrnText.setText("");
        forceSrn.setVisible(false);
        forceSrn.setSelected(false);
        if (resetDrives && !refreshingDriveInfo) {
            LOG.log(Level.INFO, " -fill drives list");
            fillDriveList(getRoots());
        } else if (resetDrives) {
            LOG.log(Level.INFO, " - drive list currently being filled by drive monitor");
        }
        LOG.log(Level.INFO, " -refresh UI");
        refreshUI();
    }

    private void onCopyFinished(final String endMsg, final String endTitle) {
        onCopyFinished(endMsg, endTitle, JOptionPane.PLAIN_MESSAGE);
    }
    private void onCopyFinished(final String endMsg, final String endTitle, int msgType) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(applicationWindow, endMsg, endTitle, msgType);
            updatingTB = false;
            resetUI(true);
            LOG.log(Level.INFO, endMsg);
        });
    }

    private synchronized boolean isDriveConnected() {
        boolean connected = false;
        TbFile drive;

        if (driveList.getItemCount() > 0) {
            drive = ((TBDeviceInfo) driveList.getSelectedItem()).getRootFile();
            if (drive != null)
                connected = true;
        }
        return connected;
    }

    private void refreshUI() {
        boolean connected;

        setGridColumnWidths();
        setEnabledStates();

        connected = isDriveConnected();
        if (connected && !updatingTB) {
            statusDisplay.setStatus("STATUS: Ready");
            LOG.log(Level.INFO, "STATUS: Ready");
        } else {
            if (!connected) {
                oldDeploymentText.setText("");
                oldCommunityText.setText("");
                oldFirmwareVersionText.setText("");
                forceFirmware.setSelected(false);
                oldPackageText.setText("");
                newSrnText.setText("");
                forceSrn.setVisible(false);
                forceSrn.setSelected(false);
                oldSrnText.setText("");
                lastUpdatedText.setText("");
                LOG.log(Level.INFO, "STATUS: " + TBLoaderConstants.NO_DRIVE);
                statusDisplay.setStatus("STATUS: " + TBLoaderConstants.NO_DRIVE);
            }
            if (getSelectedCommunity() != null) {
                fillPackageFromCommunity(getSelectedCommunity());
            }
        }
    }

    private void setEnabledStates() {
        // Must have device. Update must not be in progress.
        boolean enabled = isDriveConnected() && !updatingTB;
        // Must have set location.
        enabled = enabled && (currentLocationChooser.getSelectedIndex() != 0);
        // Must have community.
        enabled = enabled && (getSelectedCommunity() != null);
        updateButton.setEnabled(enabled);
        goButton.setEnabled(enabled);
        goButton.setBackground(enabled?Color.GREEN:defaultButtonBackgroundColor);
        goButton.setForeground(enabled?new Color(0, 192, 0):Color.GRAY);

        getStatsButton.setEnabled(enabled);
    }

    StatusDisplay statusDisplay = new StatusDisplay();
    private class StatusDisplay extends ProgressListener {
        ProgressListener.Steps currentStep = ProgressListener.Steps.ready;

        void clear(String value) {
            statusCurrent.setText(value);
            statusFilename.setText("");
            clearLog();
        }
        public void clearLog() {
            statusLog.setText("");
            statusLog.setForeground(Color.BLACK);
        }
        void setStatus(String value) {
            statusCurrent.setText(value);
            statusFilename.setText("");
        }

        @Override
        public void step(ProgressListener.Steps step) {
            currentStep = step;
            statusCurrent.setText(step.description());
            statusFilename.setText("");
            LOG.log(Level.INFO, "STEP: " + step.description());
        }

        @Override
        public void detail(String value) {
            statusFilename.setText(value);
            LOG.log(Level.INFO, "DETAIL: " + value);
        }

        @Override
        public void log(String value) {
            statusLog.setText(value + "\n" + statusLog.getText());
            LOG.log(Level.INFO, "PROGRESS: " + value);
        }

        @Override
        public void log(boolean append, String value) {
            if (!append) {
                log(value);
            } else {
                LOG.log(Level.INFO, "PROGRESS: " + value);
                String oldValue = statusLog.getText();
                int nl = oldValue.indexOf("\n");
                if (nl > 0) {
                    String pref = oldValue.substring(0, nl);
                    String suff = oldValue.substring(nl + 1);
                    statusLog.setText(pref + value + "\n" + suff);
                } else {
                    statusLog.setText(oldValue + value);
                }
            }
        }

        public void error(String value) {
            log(value);
            statusLog.setForeground(Color.RED);
            LOG.log(Level.SEVERE, "SEVERE: " + value);
        }

    };

    public enum Operation {Update, CollectStats}

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

                TBLoaderCore tbLoader = new TBLoaderCore.Builder()
                    .withTbLoaderConfig(tbLoaderConfig)
                    .withTbDeviceInfo(currentTbDevice)
                    .withOldDeploymentInfo(oldDeploymentInfo)
                    .withLocation(currentLocationChooser.getSelectedItem().toString())
                    .withRefreshFirmware(false)
                    .withStatsOnly()
                    .withProgressListener(statusDisplay)
                    .build();
                result = tbLoader.collectStatistics();

                opLog.put("success", result.gotStatistics);
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
            }
        }

        private void update() {
            String community = getSelectedCommunity();
            DeploymentInfo.DeploymentInfoBuilder builder = new DeploymentInfo.DeploymentInfoBuilder()
                .withSerialNumber(newSrnText.getText())
                .withNewSerialNumber(isNewSerialNumber)
                .withProjectName(newProject)
                .withDeploymentName(newDeployment)
                .withPackageName(getNewPackage())
                .withUpdateDirectory(null)
                .withUpdateTimestamp(dateRotation)
                .withFirmwareRevision(newFirmwareVersionText.getText())
                .withCommunity(community)
                .withRecipientid(getRecipientIdForCommunity(community))
                .asTestDeployment(testDeployment.isSelected());
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

            File newDeploymentContentDir = new File(localTbLoaderDir, TBLoaderConstants.CONTENT_SUBDIR
                    + File.separator + newDeploymentInfo.getDeploymentName());

            TbFile sourceImage = new FsFile(newDeploymentContentDir);

            TBLoaderCore.Result result = null;
            try {

                TBLoaderCore tbLoader = new TBLoaderCore.Builder()
                    .withTbLoaderConfig(tbLoaderConfig)
                    .withTbDeviceInfo(currentTbDevice)
                    .withDeploymentDirectory(sourceImage)
                    .withOldDeploymentInfo(oldDeploymentInfo)
                    .withNewDeploymentInfo(newDeploymentInfo)
                    .withLocation(currentLocationChooser.getSelectedItem().toString())
                    .withRefreshFirmware(forceFirmware.isSelected())
                    .withProgressListener(statusDisplay)
                    .build();
                result = tbLoader.update();

                opLog.put("gotstatistics", result.gotStatistics)
                    .put("corrupted", result.corrupted)
                    .put("reformatfailed", result.reformatOp== TBLoaderCore.Result.FORMAT_OP.failed)
                    .put("verified", result.verified);

                if (result.corrupted && result.reformatOp != TBLoaderCore.Result.FORMAT_OP.succeeded) {
                    endMsg = "There is an error in the Talking Book SD card, and it needs to be re-formatted."
                        + "\nPlease re-format the Talking Book SD card, and try again.";
                    endTitle = "Corrupted SD Card";
                    endMessageType = JOptionPane.ERROR_MESSAGE;
                } else if (!result.gotStatistics) {
                    LOG.log(Level.SEVERE, "Could not get statistics!");
                    statusDisplay.error("Could not get statistics.");
                    if (result.corrupted) {
                        if (!OSChecker.WINDOWS) {
                            LOG.log(Level.INFO,
                                    "Reformatting memory card is not supported on this platform.\nTry using TBLoader for Windows.");
                            JOptionPane.showMessageDialog(applicationWindow,
                                                          "Reformatting memory card is not supported on this platform.\nTry using TBLoader for Windows.",
                                                          "Failure!", JOptionPane.ERROR_MESSAGE);
                        }

                        if (result.reformatOp == TBLoaderCore.Result.FORMAT_OP.failed) {
                            LOG.log(Level.SEVERE, "STATUS:Reformat Failed");
                            LOG.log(Level.SEVERE,
                                    "Could not reformat memory card.\nMake sure you have a good USB connection\nand that the Talking Book is powered with batteries, then try again.\n\nIf you still cannot reformat, replace the memory card.");
                            JOptionPane.showMessageDialog(applicationWindow,
                                                          "Could not reformat memory card.\nMake sure you have a good USB connection\nand that the Talking Book is powered with batteries, then try again.\n\nIf you still cannot reformat, replace the memory card.",
                                                          "Failure!", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }

                for (int i = 1; i <= (result.gotStatistics ? 3 : 6); i++)
                    Toolkit.getDefaultToolkit().beep();
            } catch (Exception e) {
                opLog.put("exception", e.getMessage());
                if (alert) {
                    JOptionPane.showMessageDialog(applicationWindow, e.getMessage(), "Error",
                                                  JOptionPane.ERROR_MESSAGE);
                    criticalError = true;
                    LOG.log(Level.SEVERE, "CRITICAL ERROR:", e);
                } else
                    LOG.log(Level.WARNING, "NON-CRITICAL ERROR:", e);
                endMsg = String.format("Exception updating TB-Loader: %s", e.getMessage());
                endTitle = "An Exception Occurred";
            } finally {
                opLog.finish();
                if (endMsg == null && result != null && result.verified) {
                    endMsg = "Talking Book has been updated and verified\nin " + result.duration
                            + ".";
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
