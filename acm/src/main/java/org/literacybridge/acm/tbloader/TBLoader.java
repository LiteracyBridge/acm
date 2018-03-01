package org.literacybridge.acm.tbloader;

import org.jdesktop.swingx.JXDatePicker;
import org.literacybridge.acm.Constants;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.utils.OsUtils;
import org.literacybridge.core.OSChecker;
import org.literacybridge.core.fs.FsFile;
import org.literacybridge.core.fs.OperationLog;
import org.literacybridge.core.fs.TbFile;
import org.literacybridge.core.tbloader.DeploymentInfo;
import org.literacybridge.core.tbloader.ProgressListener;
import org.literacybridge.core.tbloader.TBDeviceInfo;
import org.literacybridge.core.tbloader.TBLoaderConfig;
import org.literacybridge.core.tbloader.TBLoaderConstants;
import org.literacybridge.core.tbloader.TBLoaderCore;
import org.literacybridge.core.tbloader.TBLoaderUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import static java.awt.GridBagConstraints.BOTH;
import static java.awt.GridBagConstraints.HORIZONTAL;
import static java.awt.GridBagConstraints.LINE_START;
import static java.awt.GridBagConstraints.NONE;
import static java.awt.GridBagConstraints.RELATIVE;
import static org.literacybridge.core.tbloader.TBLoaderUtils.isSerialNumberFormatGood;
import static org.literacybridge.core.tbloader.TBLoaderUtils.isSerialNumberFormatGood2;

@SuppressWarnings("serial")
public class TBLoader extends JFrame {
    private static final Logger LOG = Logger.getLogger(TBLoader.class.getName());

    private JFrame applicationWindow;

    // This string must match firmware code to generate special dings on startup
    // as reminder that specific name has not been set.
    private static final String NO_COMMUNITY_SELECTED = "Non-specific";

    private String imageRevision = "(no rev)";
    private String dateRotation;

    // Global swing components.
    private JLabel warning;

    private JLabel deviceLabel;
    private JComboBox<TBDeviceInfo> driveList;

    private JLabel currentLocationLabel;
    private JComboBox<String> currentLocationList;

    private JLabel nextLabel;
    private JLabel prevLabel;

    private JLabel deploymentLabel;
    private JComboBox<String> newDeploymentList;
    private JTextField oldDeploymentText;

    private JLabel communityFilterLabel;
    private FilteringComboBoxModel<String> newCommunityModel;
    private JTextField newCommunityFilter;
    private JLabel communityLabel;
    private JComboBox<String> newCommunityList;
    private JTextField oldCommunityText;

    private JLabel contentPackageLabel;
    private JTextField newImageText;
    private JTextField oldImageText;

    private JLabel dateLabel;
    private JXDatePicker datePicker;
    private JTextField lastUpdatedText;

    private JLabel firmwareVersionLabel;
    private JTextField newFirmwareVersionText;
    private JTextField oldFirmwareVersionText;

    private JLabel srnLabel;
    private JTextField newSrnText;
    private JTextField oldSrnText;

    private JLabel optionsLabel;
    private JCheckBox forceFirmware;
    private JCheckBox testDeployment;

    private JButton updateButton;
    private JButton getStatsButton;

    private JTextArea statusCurrent;
    private JTextArea statusFilename;
    private JTextArea statusLog;
    private JScrollPane statusScroller;

    private boolean isNewSerialNumber;
    private TBDeviceInfo currentTbDevice;
    private String srnPrefix;
    private String newProject;

    // Deployment info read from Talking Book.
    private DeploymentInfo oldDeploymentInfo;

    private TBLoaderConfig tbLoaderConfig;

    // All content is relative to this.
    private File baseDirectory;
    private File logsDir;
    // Document change listener, listens for changes to filter text box, updates the filter.
    private DocumentListener filterChangeListener = new DocumentListener() {
        // Listen for any change to the text
        public void changedUpdate(DocumentEvent e) { common(); }
        public void removeUpdate(DocumentEvent e) { common(); }
        public void insertUpdate(DocumentEvent e) { common(); }
        void common() { newCommunityModel.setFilterString(newCommunityFilter.getText()); }
    };

    class WindowEventHandler extends WindowAdapter {
        @Override
        public void windowClosing(WindowEvent evt) {
            OperationLog.log("TbLoaderShutdown").finish();
            LOG.log(Level.INFO, "closing app");
            System.exit(0);
        }
    }

    private String currentLocation[] = new String[] { "Select location", "Community",
            "Jirapa office", "Wa office", "Other" };

    private boolean startUpDone = false;
    private boolean refreshingDriveInfo = false;
    private boolean updatingTB = false;

    public static void main(String[] args) throws Exception {
        String project = args[0];
        String srnPrefix = "b-"; // for latest Talking Book hardware

        if (args.length == 2) {
            srnPrefix = args[1];
        }

        new TBLoader(project, srnPrefix).runApplication();
    }

    private TBLoader(String project, String srnPrefix) {
        project = ACMConfiguration.cannonicalProjectName(project);
        applicationWindow = this;
        this.newProject = project;
        if (srnPrefix != null) {
            this.srnPrefix = srnPrefix;
        } else {
            this.srnPrefix = "b-"; // for latest Talking Book hardware
        }
    }

    private void runApplication() throws Exception {
        OsUtils.enableOSXQuitStrategy();
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        this.addWindowListener(new WindowEventHandler());
        setDeviceIdAndPaths();

        // Set up the program log. For debugging the execution of the TBLoader application.

        // Put log output into the ~/Dropbox/tbcd1234/log directory
        // - rotate through 10 files
        // - up to 4MB per file
        // - Keep appending to a file until it reaches the limit.
        // If there is no format set, use this one-line format.
        String format = System.getProperty("java.util.logging.SimpleFormatter.format");
        if (format==null || format.length()==0) {
            System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS %4$s %2$s %5$s%6$s%n");
        }
        Path relativeLogPath = Paths.get(System.getProperty("user.home")).relativize(Paths.get(logsDir.getAbsolutePath()));
        String logPattern = "%h/" + relativeLogPath.toString() + "/tbloaderlog.%g";
        Logger rootLogger = Logger.getLogger("");
        FileHandler logHandler = new FileHandler(logPattern,
                16*1024*1024,
                10, true);
        logHandler.setFormatter(new SimpleFormatter());
        logHandler.setLevel(Level.INFO);
        rootLogger.removeHandler(rootLogger.getHandlers()[0]);
        rootLogger.setLevel(Level.INFO);
        rootLogger.addHandler(logHandler);
        LOG.log(Level.INFO, "\n\n********************************************************************************");
        LOG.log(Level.INFO, "WindowsTBLoaderStart\n");

        // Set up the operation log. Tracks what is done, by whom.
        OperationLogImpl opLogImpl = new OperationLogImpl(logsDir);
        OperationLog.setImplementation(opLogImpl);
        OperationLog.Operation opLog = OperationLog.log("WindowsTBLoaderStart")
            .put("tbcdid", tbLoaderConfig.getTbLoaderId())
            .put("project", newProject);

        // get image revision
        File[] files = baseDirectory.listFiles((dir, name) -> {
            String lowercase = name.toLowerCase();
            return lowercase.endsWith(".rev");
        });
        if (files.length > 1) {
            // Multiple Image Revisions! -- Delete all to go back to published version, unless one marks it as UNPUBLISHED
            boolean unpublished = false;
            for (File f : files) {
                if (!f.getName().startsWith(TBLoaderConstants.UNPUBLISHED_REV)) {
                    f.delete();
                } else {
                    unpublished = true;
                    imageRevision = f.getName();
                    imageRevision = imageRevision.substring(0, imageRevision.length() - 4);
                }
            }
            if (!unpublished) {
                JOptionPane.showMessageDialog(applicationWindow,
                                              "Revision conflict. Please click OK to shutdown.\nThen restart the TB-Loader to get the latest published version.");
                System.exit(1);
            }
        } else if (files.length == 1) {
            imageRevision = files[0].getName();
            imageRevision = imageRevision.substring(0, imageRevision.length() - 4);
        }
        setTitle("TB-Loader " + Constants.ACM_VERSION + "/" + imageRevision);
        if (imageRevision.startsWith(TBLoaderConstants.UNPUBLISHED_REV)) {
            Object[] options = { "Yes-refresh from published", "No-keep unpublished" };
            int answer = JOptionPane.showOptionDialog(this,
                                                      "This TB Loader is running an unpublished version.\nWould you like to delete the unpublished version and use the latest published version?",
                                                      "Unpublished", JOptionPane.YES_NO_OPTION,
                                                      JOptionPane.QUESTION_MESSAGE, null, options,
                                                      options[1]);
            if (answer == JOptionPane.YES_OPTION) {
                files[0].delete();
                JOptionPane.showMessageDialog(applicationWindow,
                                              "Click OK to shutdown. Then restart to get the latest published version of the TB Loader.");
                opLog.put("MultipleVersions", "CleanAndRestart")
                    .finish();
                System.exit(1);
            }
            opLog.put("MultipleVersions", "KeepUnpublished");
        }
        opLog.finish();

        initializeGui();
        JOptionPane.showMessageDialog(applicationWindow,
                                      "Remember to power Talking Book with batteries before connecting with USB.",
                                      "Use Batteries!", JOptionPane.PLAIN_MESSAGE);
        LOG.log(Level.INFO, "set visibility - starting drive monitoring");
        deviceMonitorThread.setDaemon(true);
        deviceMonitorThread.start();
        startUpDone = true;
    }

    private void setDeviceIdAndPaths() throws IOException {
        try {
            File applicationHomeDirectory = ACMConfiguration.getInstance()
                .getApplicationHomeDirectory();
            baseDirectory = new File(applicationHomeDirectory,
                Constants.TBLoadersHomeDir + File.separator + newProject);
            baseDirectory.mkdirs();
            TbFile softwareDir = new FsFile(ACMConfiguration.getInstance().getSoftwareDir());

            File dropboxDir = ACMConfiguration.getInstance().getGlobalShareDir();
            if (!dropboxDir.exists()) {
                JOptionPane.showMessageDialog(applicationWindow, dropboxDir.getAbsolutePath()
                        + " does not exist; cannot find the Dropbox path. Please contact ICT staff.",
                    "Cannot Find Dropbox!", JOptionPane.PLAIN_MESSAGE);
                System.exit(1);
            }
            //TbFile dropboxFile = new FsFile(dropboxDir);

            // Look for ~/LiteracyBridge/1234.dev file.
            File[] files = applicationHomeDirectory.listFiles((dir, name) -> {
                String lowercase = name.toLowerCase();
                return lowercase.endsWith(TBLoaderConstants.DEVICE_FILE_EXTENSION);
            });
            if (files.length == 1) {
                String deviceId = files[0].getName()
                    .substring(0, files[0].getName().length() - 4)
                    .toUpperCase();

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
                        JOptionPane.PLAIN_MESSAGE);
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
                    JOptionPane.PLAIN_MESSAGE);
                System.exit(1);
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Exception while setting DeviceId and paths", e);
            throw e;
        }
    }

    private void initializeGui() {
        JPanel panel = createComponents();

        layoutComponents(panel);

        setSize(700, 600);
        add(panel, BorderLayout.CENTER);
        setLocationRelativeTo(null);

        //Logger.init();
        fillDeploymentList();
        resetUI(true);
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
                currentLocationList,
                newDeploymentList,
                oldDeploymentText,
                newCommunityFilter,
                newCommunityList,
                oldCommunityText,
                newImageText,
                oldImageText,
                datePicker,
                lastUpdatedText,
                newFirmwareVersionText,
                oldFirmwareVersionText,
                newSrnText,
                oldSrnText,
                forceFirmware,
                testDeployment,
                updateButton,
                getStatsButton
            };
        }
        int maxMinWidth = 0;
        for (JComponent c : columnComponents) maxMinWidth = Math.max(maxMinWidth, c.getMinimumSize().width);
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
        Insets zi = new Insets(3,3,3,2);
        return new GridBagConstraints(x, y, 1, 1, 0, 0, LINE_START, HORIZONTAL, zi, 0, 0);
    }
    
    private void layoutComponents(JPanel panel) {
        panel.setBorder(new EmptyBorder(9,10,9,9));
        GridBagLayout layout = new GridBagLayout();
        panel.setLayout(layout);
        GridBagConstraints c;

        // Warning.
        c = gbc(1, 0);
        c.gridwidth = 2;
        panel.add(warning, c);
        
        // TB Drive letter / volume label.
        c = gbc(0, 1);
        panel.add(deviceLabel, c);
        c.gridx = RELATIVE;
        panel.add(driveList, c);

        // Current location
        c = gbc(0, 2);
        panel.add(currentLocationLabel, c);
        c.gridx = RELATIVE;
        panel.add(currentLocationList, c);

        // Next / Previous
        c = gbc(1, 3);
        c.weightx = 1;
        panel.add(nextLabel, c);
        c.gridx = RELATIVE;
        panel.add(prevLabel, c);

        // Deployment.
        c = gbc(0, 4);
        panel.add(deploymentLabel, c);
        c.gridx = RELATIVE;
        panel.add(newDeploymentList, c);
        panel.add(oldDeploymentText, c);

        // Community Filter
        c = gbc(0, 5);
        panel.add(communityFilterLabel, c);
        c.gridx = RELATIVE;
        panel.add(newCommunityFilter, c);
        
        // Community.
        c = gbc(0, 6);
        panel.add(communityLabel, c);
        c.gridx = RELATIVE;
        panel.add(newCommunityList, c);
        panel.add(oldCommunityText, c);
        
        // Package (aka 'Content', aka 'image')
        c = gbc(0, 7);
        panel.add(contentPackageLabel, c);
        c.gridx = RELATIVE;
        panel.add(newImageText, c);
        panel.add(oldImageText, c);
        
        // Deployment date.
        c = gbc(0, 8);
        panel.add(dateLabel, c);
        c.gridx = RELATIVE;
        panel.add(datePicker, c);
        panel.add(lastUpdatedText, c);
        
        // Firmware version.
        c = gbc(0, 9);
        panel.add(firmwareVersionLabel, c);
        c.gridx = RELATIVE;
        panel.add(newFirmwareVersionText, c);
        panel.add(oldFirmwareVersionText, c);
        
        // TB Serial Number.
        c = gbc(0, 10);
        panel.add(srnLabel, c);
        c.gridx = RELATIVE;
        panel.add(newSrnText, c);
        panel.add(oldSrnText, c);
        
        // Options
        c = gbc(0, 11);
        panel.add(optionsLabel, c);
        c.gridx = RELATIVE;
        panel.add(forceFirmware, c);
        panel.add(testDeployment, c);

        // Buttons.
        c = gbc(1, 13);
        c.fill = NONE;
        panel.add(updateButton, c);
        c.gridx = RELATIVE;
        panel.add(getStatsButton, c);

        // Status display
        c = gbc(0, 14);
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
    }

    private JPanel createComponents() {
        JPanel panel = new JPanel();

        // "Use with NEW TBs only" / "Use with OLD TBs only"
        if (srnPrefix.equals("a-")) {
            panel.setBackground(Color.CYAN);
            warning = new JLabel("Use with OLD TBs only");
            warning.setForeground(Color.RED);
        } else {
            warning = new JLabel("Use with NEW TBs only");
            warning.setForeground(Color.RED);
        }

        // Windows drive letter and volume name.
        deviceLabel = new JLabel("Talking Book Device:");
        driveList = new JComboBox<>();
        driveList.addActionListener(this::driveListActionPerformed);

        // Select "Community", "LBG Office", "Other"
        currentLocationLabel = new JLabel("Current Location:");
        currentLocationList = new JComboBox<>(currentLocation);

        // Headings for "Next", "Previous"
        nextLabel = new JLabel("Next");
        prevLabel = new JLabel("Previous");

        // Deployment name / version.
        deploymentLabel = new JLabel("Update:");
        newDeploymentList = new JComboBox<>();
        newDeploymentList.addActionListener(this::newDeploymentListActionPerformed);
        oldDeploymentText = new JTextField();
        oldDeploymentText.setEditable(false);

        // Community filter. (Type desired filter into text field.)
        communityFilterLabel = new JLabel("Community filter:");
        newCommunityModel = new FilteringComboBoxModel<>();
        newCommunityFilter = new JTextField("", 40);
        newCommunityFilter.getDocument().addDocumentListener(filterChangeListener);

        // Select community.
        communityLabel = new JLabel("Community:");
        newCommunityList = new JComboBox<>(newCommunityModel);
        newCommunityList.addActionListener(this::newCommunityListActionPerformed);
        oldCommunityText = new JTextField();
        oldCommunityText.setEditable(false);

        // Show Content Package name.
        contentPackageLabel = new JLabel("Content:");
        newImageText = new JTextField();
        newImageText.setEditable(false);
        oldImageText = new JTextField();
        oldImageText.setEditable(false);

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
        oldFirmwareVersionText = new JTextField();
        oldFirmwareVersionText.setEditable(false);

        // Show serial number.
        srnLabel = new JLabel("Serial number:");
        newSrnText = new JTextField();
        newSrnText.setEditable(false);
        oldSrnText = new JTextField();
        oldSrnText.setEditable(false);

        // Options
        optionsLabel = new JLabel("Options:");
        forceFirmware = new JCheckBox();
        forceFirmware.setText("Force firmware refresh");
        forceFirmware.setSelected(false);
        forceFirmware.setToolTipText("Check to force a re-flash of the firmware. This should almost never be needed.");

        testDeployment = new JCheckBox();
        testDeployment.setText("Only testing the deployment");
        testDeployment.setSelected(false);
        testDeployment.setToolTipText("Check if only testing the Deployment. Uncheck if sending the Deployment out to the field.");

        // Update / gather statistics buttons.
        updateButton = new JButton("Update TB");
        updateButton.setEnabled(false);
        updateButton.addActionListener(this::buttonActionPerformed);
        getStatsButton = new JButton("Get Stats");
        getStatsButton.setEnabled(false);
        getStatsButton.addActionListener(this::buttonActionPerformed);

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
     * Looks in the ~/LiteracyBridge/TB-Loaders/{project}/content/{deployment}/basic directory
     * for files named '*.img'. Any found are assumed to be firmware images for v1 talking books.
     *
     * There *should* be exactly one (the TB-Builder should have selected the highest numbered one).
     *
     * Populates the newFirmwareVersionText field.
     */
    private void getFirmwareRevisionNumbers() {
        String firmwareVersion = "(No firmware)";

        // Like ~/LiteracyBridge/TB-Loaders/{project}/content/{deployment}/basic
        File basicContentPath = new File(baseDirectory,
                                         TBLoaderConstants.CONTENT_SUBDIR + File.separator
                                                 + newDeploymentList.getSelectedItem().toString()
                                                 + "/" + TBLoaderConstants.CONTENT_BASIC_SUBDIR);
        LOG.log(Level.INFO, "DEPLOYMENT:" + newDeploymentList.getSelectedItem().toString());
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
        } catch (Exception ignore) {
            LOG.log(Level.WARNING, "exception - ignore and keep going with default string", ignore);
        }

    }

    private File prevSelected = null;
    private int prevSelectedCommunity = -1;

    private synchronized void fillDeploymentList() {
        int indexSelected = -1;
        File contentPath = new File(baseDirectory, TBLoaderConstants.CONTENT_SUBDIR);
        newDeploymentList.removeAllItems();
        File[] packageFolder = contentPath.listFiles();
        for (int i = 0; i < packageFolder.length; i++) {
            if (packageFolder[i].isHidden())
                continue;
            newDeploymentList.addItem(packageFolder[i].getName());
            if (imageRevision.startsWith(packageFolder[i].getName())) {
                indexSelected = i;
            }
        }
        if (indexSelected != -1) {
            newDeploymentList.setSelectedIndex(indexSelected);
        }
    }

    private synchronized void fillCommunityList() {
        String filter = newCommunityModel.setFilterString(null);
        newCommunityList.removeAllItems();
        File[] files;

        File fCommunityDir = new File(baseDirectory,
                                      TBLoaderConstants.CONTENT_SUBDIR + File.separator
                                              + newDeploymentList.getSelectedItem().toString() + "/"
                                              + TBLoaderConstants.COMMUNITIES_SUBDIR);

        files = fCommunityDir.listFiles((dir, name) -> dir.isDirectory());
        newCommunityList.addItem(NO_COMMUNITY_SELECTED);
        for (File f : files) {
            newCommunityList.addItem(f.getName());
        }
        setCommunityList();
        newCommunityModel.setFilterString(filter);
    }

    private String getRecipientIdForCommunity(String communityDirName) {
        // ~/LiteracyBridge/TB-Loaders/{project}/content/{deployment}/communities/{communitydir}
        File deploymentDirectory = new File(baseDirectory,
            TBLoaderConstants.CONTENT_SUBDIR + File.separator
                + newDeploymentList.getSelectedItem().toString());

        return TBLoaderUtils.getRecipientIdForCommunity(deploymentDirectory, communityDirName);
    }

    private synchronized void setCommunityList() {
        if (prevSelectedCommunity != -1)
            newCommunityList.setSelectedIndex(prevSelectedCommunity);
        else {
            int count = newCommunityList.getItemCount();
            for (int i = 0; i < count; i++) {
                if (newCommunityList.getItemAt(i).equalsIgnoreCase(oldCommunityText.getText())) {
                    newCommunityList.setSelectedIndex(i);
                    break;
                }
            }
        }
        getImageFromCommunity(newCommunityList.getSelectedItem().toString());
    }

    /**
     * Allocates and persists the next serial number for this device (pc running TB-Loader).
     *
     * @return The next serial number.
     */
    private int allocateNextSerialNumberFromTbLoader() throws Exception {
        int serialnumber = TBLoaderConstants.STARTING_SERIALNUMBER;
        String deviceId = tbLoaderConfig.getTbLoaderId();
        String devFilename = deviceId + TBLoaderConstants.DEVICE_FILE_EXTENSION; // xxxx.dev
        String txtFilename = deviceId + ".txt";
        File fDev = new File(ACMConfiguration.getInstance().getApplicationHomeDirectory(),
                          devFilename);
        File fTxt = new File(ACMConfiguration.getInstance().getApplicationHomeDirectory(),
            txtFilename);

        // Get the most recent serial number assigned.
        if (fDev.exists()) {
            try (DataInputStream in = new DataInputStream(new FileInputStream(fDev))) {
                serialnumber = in.readInt();
            } catch (EOFException e) {
                // No counter yet; normal for new device pc. Starting serial number is the right thing, in that case.
            } catch (IOException e) {
                // This shoudn't happen since we checked f.exists()
                LOG.log(Level.WARNING, "Unexpected IOException reading " + devFilename
                        + "; ignoring and using default serial number", e);
            }
            if (serialnumber >= 0xFFFF) {
                throw new Exception("SRN out of bounds for this TB Loader device.");
            }
            fDev.delete();
        }
        if (serialnumber == TBLoaderConstants.STARTING_SERIALNUMBER) {
            // if file doesn't exist, use the SRN = STARTING_SERIALNUMBER
            // TODO:raise exception and tell user to register the device or ensure file wasn't lost
        }

        // The number we're assigning now...
        serialnumber++;

        try (DataOutputStream os = new DataOutputStream(new FileOutputStream(fDev))) {
            os.writeInt(serialnumber);
        }
        try (FileWriter fw = new FileWriter(fTxt);
            PrintWriter pw = new PrintWriter(fw)) {
            pw.printf("%04x", serialnumber);
        }

        // Back up the file in case of loss.
        File dropboxDir = ACMConfiguration.getInstance().getGlobalShareDir();
        File backupDir = new File(dropboxDir,
                                  TBLoaderConstants.COLLECTED_DATA_DROPBOXDIR_PREFIX + deviceId);
        File backupFile = new File(backupDir, devFilename);
        try (DataOutputStream os = new DataOutputStream(new FileOutputStream(backupFile))) {
            os.writeInt(serialnumber);
        }
        File backupText = new File(backupDir, txtFilename);
        try (FileWriter fw = new FileWriter(backupText);
            PrintWriter pw = new PrintWriter(fw)) {
            pw.printf("%04x", serialnumber);
        }

        return serialnumber;
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
                        fillDriveList(roots);
                        if (!driveList.getItemAt(0).getLabel().equals(TBLoaderConstants.NO_DRIVE)) {
                            statusDisplay.clearLog();
                        }
                        populatePreviousValuesFromCurrentDrive();
                        fillCommunityList();
                        getFirmwareRevisionNumbers();
                        refreshUI();
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

    private void getImageFromCommunity(String community) {
        // ~/LiteracyBridge/TB-Loaders/{project}/content/{deployment}
        File deploymentDirectory = new File(baseDirectory,
                                            TBLoaderConstants.CONTENT_SUBDIR + File.separator
                                                    + newDeploymentList.getSelectedItem()
                                                    .toString());
//    TBFileSystem imagesTbFs = DefaultTBFileSystem.open(imagesDir);
        String imageName = TBLoaderUtils.getImageForCommunity(deploymentDirectory, community);
        newImageText.setText(imageName);
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
            oldImageText.setText(oldDeploymentInfo.getPackageName());
            oldDeploymentText.setText(oldDeploymentInfo.getDeploymentName());
            lastUpdatedText.setText(oldDeploymentInfo.getUpdateTimestamp());

            //TODO: Better check that this works properly!
            newSrnText.setText(oldDeploymentInfo.getSerialNumber());

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
    private void driveListActionPerformed(ActionEvent e) {
        TBDeviceInfo di;
        if (refreshingDriveInfo || !startUpDone)
            return;

        oldSrnText.setText("");
        newSrnText.setText("");
        di = (TBDeviceInfo) ((JComboBox<String>) e.getSource()).getSelectedItem();
        currentTbDevice = di;
        if (di != null && di.getRootFile() != null) {
            LOG.log(Level.INFO,
                "Drive changed: " + di.getRootFile().toString() + di.getLabel());
            populatePreviousValuesFromCurrentDrive();
            fillCommunityList();
            getFirmwareRevisionNumbers();
        }
    }
    /**
     * Handles combo box selections for the Community list.
     *
     * @param e The combo selection event.
     */
    private void newCommunityListActionPerformed(ActionEvent e) {
        if (refreshingDriveInfo || !startUpDone)
            return;

        JComboBox<String> cl = (JComboBox<String>) e.getSource();
        if (cl.getSelectedItem() != null) {
            getImageFromCommunity(cl.getSelectedItem().toString());
        }
    }
    /**
     * Handles combo box selections for the Deployment List.
     *
     * @param e The combo selection event.
     */
    private void newDeploymentListActionPerformed(ActionEvent e) {
        if (refreshingDriveInfo || !startUpDone)
            return;

        getFirmwareRevisionNumbers();
        refreshUI();
        getImageFromCommunity(newCommunityList.getSelectedItem().toString());
    }

    /**
     * Handles button presses for the "Update" and "Get Stats" buttons.
     *
     * @param e The button press event.
     */
    private void buttonActionPerformed(ActionEvent e) {
        TBDeviceInfo di;
        Operation operation;
        boolean isUpdate = false;
        JButton b = (JButton) e.getSource();

        if (refreshingDriveInfo || !startUpDone)
            return;

        if (b == updateButton) {
            isUpdate = true;
            operation = Operation.Update;
        } else if (b == getStatsButton) {
            operation = Operation.CollectStats;
        } else {
            throw new IllegalArgumentException("'buttonActionPerformed' called for unknown button");
        }

        disableAll();
        try {
            LOG.log(Level.INFO, "ACTION: " + b.getText());

            di = currentTbDevice;

            TbFile drive = di.getRootFile();
            if (drive == null) {
                refreshUI();
                return;
            }
            String devicePath = drive.getAbsolutePath();
            prevSelected = new File(devicePath);

            if (oldCommunityText.getText().trim().length() == 0)
                oldCommunityText.setText("UNKNOWN");
            if (oldDeploymentText.getText().trim().length() == 0)
                oldDeploymentText.setText("UNKNOWN");

            String community = newCommunityList.getSelectedItem().toString();
            LOG.log(Level.INFO, "Community: " + community);

            if (isUpdate) {
                if (dateRotation == null || currentLocationList.getSelectedIndex() == 0) {
                    StringBuilder text = new StringBuilder("You must first select ");
                    StringBuilder heading = new StringBuilder("Need ");
                    String joiner = "";
                    if (dateRotation == null) {
                        text.append("a rotation date");
                        heading.append("Date");
                        joiner = " and ";
                    }
                    if (currentLocationList.getSelectedIndex() == 0) {
                        text.append(joiner).append("your location");
                        heading.append(joiner).append("Location");
                    }
                    text.append(".");
                    heading.append("!");
                    JOptionPane.showMessageDialog(applicationWindow,
                                                  text.toString(),
                                                  heading.toString(),
                                                  JOptionPane.PLAIN_MESSAGE);
                    refreshUI();
                    return;
                }
                
                if (newImageText.getText().equalsIgnoreCase(TBLoaderConstants.MISSING_PACKAGE)) {
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

                if (community.equals(NO_COMMUNITY_SELECTED)) {
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
                } else
                    prevSelectedCommunity = newCommunityList.getSelectedIndex();

                // If the Talking Book needs a new serial number, allocate one. We did not do it before this to
                // avoid wasting allocations.
                String srn = newSrnText.getText();
                isNewSerialNumber = false;
                if (srn.equalsIgnoreCase(TBLoaderConstants.NEED_SERIAL_NUMBER) ||
                        !isSerialNumberFormatGood(srnPrefix, srn) ||
                        !isSerialNumberFormatGood2(srn)) {
                    int intSrn = allocateNextSerialNumberFromTbLoader();
                    isNewSerialNumber = true;
                    String lowerSrn = String.format("%04x", intSrn);
                    srn = (srnPrefix + tbLoaderConfig.getTbLoaderId() + lowerSrn).toUpperCase();
                    di.setSerialNumber(srn);
                    newSrnText.setText(srn);
                }
            }
            if (!isUpdate && dateRotation == null) {
                dateRotation = new Date().toString();
            }

            LOG.log(Level.INFO, "ID:" + di.getSerialNumber());
            statusDisplay.clear("STATUS: Starting");

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
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(applicationWindow, endMsg, endTitle, JOptionPane.PLAIN_MESSAGE);
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
        disableAll();
        
        setGridColumnWidths();

        connected = isDriveConnected();
        if (connected && !updatingTB) {
            updateButton.setEnabled(true);
            getStatsButton.setEnabled(true);
            statusDisplay.setStatus("STATUS: Ready");
            LOG.log(Level.INFO, "STATUS: Ready");
        } else {
            updateButton.setEnabled(false);
            getStatsButton.setEnabled(false);
            if (!connected) {
                oldDeploymentText.setText("");
                oldCommunityText.setText("");
                oldFirmwareVersionText.setText("");
                forceFirmware.setSelected(false);
                oldImageText.setText("");
                newSrnText.setText("");
                oldSrnText.setText("");
                lastUpdatedText.setText("");
                LOG.log(Level.INFO, "STATUS: " + TBLoaderConstants.NO_DRIVE);
                statusDisplay.setStatus("STATUS: " + TBLoaderConstants.NO_DRIVE);
            }
            if (newCommunityList.getSelectedItem() != null) {
                getImageFromCommunity(newCommunityList.getSelectedItem().toString());
            }
        }
    }

    private void disableAll() {
        updateButton.setEnabled(false);
        getStatsButton.setEnabled(false);
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
                    .withLocation(currentLocationList.getSelectedItem().toString())
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
            String community = newCommunityList.getSelectedItem().toString();
            DeploymentInfo.DeploymentInfoBuilder builder = new DeploymentInfo.DeploymentInfoBuilder()
                .withSerialNumber(newSrnText.getText())
                .withNewSerialNumber(isNewSerialNumber)
                .withProjectName(newProject)
                .withDeploymentName(newDeploymentList.getSelectedItem().toString())
                .withPackageName(newImageText.getText())
                .withUpdateDirectory(null)
                .withUpdateTimestamp(dateRotation)
                .withFirmwareRevision(newFirmwareVersionText.getText())
                .withCommunity(community)
                .withRecipientid(getRecipientIdForCommunity(community))
                .asTestDeployment(testDeployment.isSelected());
            DeploymentInfo newDeploymentInfo = builder.build();

            String endMsg = null;
            String endTitle = null;
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

            File newDeploymentContentDir = new File(baseDirectory, TBLoaderConstants.CONTENT_SUBDIR
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
                    .withLocation(currentLocationList.getSelectedItem().toString())
                    .withRefreshFirmware(forceFirmware.isSelected())
                    .withProgressListener(statusDisplay)
                    .build();
                result = tbLoader.update();

                opLog.put("gotstatistics", result.gotStatistics)
                    .put("corrupted", result.corrupted)
                    .put("reformatfailed", result.reformatFailed)
                    .put("verified", result.verified);

                if (!result.gotStatistics) {
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

                        if (result.reformatFailed) {
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
                onCopyFinished(endMsg, endTitle);
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
