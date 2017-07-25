package org.literacybridge.acm.tbloader;

import static javax.swing.GroupLayout.Alignment.BASELINE;
import static javax.swing.GroupLayout.Alignment.LEADING;
import static org.literacybridge.core.tbloader.TBLoaderUtils.isSerialNumberFormatGood;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
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

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileSystemView;

import org.jdesktop.swingx.JXDatePicker;
import org.literacybridge.acm.Constants;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.utils.OsUtils;
import org.literacybridge.core.OSChecker;
import org.literacybridge.core.fs.FsFile;
import org.literacybridge.core.fs.OperationLog;
import org.literacybridge.core.fs.TbFile;
import org.literacybridge.core.tbloader.ProgressListener;
import org.literacybridge.core.tbloader.DeploymentInfo;
import org.literacybridge.core.tbloader.TBDeviceInfo;
import org.literacybridge.core.tbloader.TBLoaderConfig;
import org.literacybridge.core.tbloader.TBLoaderConstants;
import org.literacybridge.core.tbloader.TBLoaderCore;
import org.literacybridge.core.tbloader.TBLoaderUtils;

@SuppressWarnings("serial")
public class TBLoader extends JFrame {
    private static final Logger LOG = Logger.getLogger(TBLoader.class.getName());

    private JFrame applicationWindow;
    private OperationLogImpl opLogImpl;

    // This string must match firmware code to generate special dings on startup
    // as reminder that specific name has not been set.
    private static final String NO_COMMUNITY_SELECTED = "Non-specific";

    private String imageRevision = "(no rev)";
    private String dateRotation;
    private JComboBox<String> newDeploymentList;
    private JTextField newCommunityFilter;
    private FilteringComboBoxModel newCommunityModel;
    private JComboBox<String> newCommunityList;
    private JComboBox<String> currentLocationList;
    private JComboBox<TBDeviceInfo> driveList;
    private JTextField oldSrnText;
    private JTextField newSrnText;
    private JTextField oldFirmwareRevisionText;
    private JTextField newFirmwareRevisionText;
    private JTextField oldImageText;
    private JTextField newImageText;
    private JTextField oldDeploymentText;
    private JTextField oldCommunityText;
    private JTextField lastUpdatedText;
    private JLabel oldValue;
    private JLabel newValue;
    private JTextArea statusRight;
    private JTextArea statusLeft;
    private JButton updateButton;
    private JButton grabStatsOnlyButton;
    private String revision;

    private int durationSeconds;
    private TBDeviceInfo currentTbDevice;
    private String srnPrefix;
    private String newProject;
    private String oldProject;
    private String syncSubPath;

    private JCheckBox forceFirmware;

    private DeploymentInfo oldDeploymentInfo;

    private TBLoaderConfig tbLoaderConfig;

    // All content is relative to this.
    private File baseDirectory;
    private File logsDir;

    class WindowEventHandler extends WindowAdapter {
        @Override
        public void windowClosing(WindowEvent evt) {
            OperationLog.logEvent("TbLoaderShutdown");
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

    public TBLoader(String project, String srnPrefix) throws IOException {
        project = ACMConfiguration.cannonicalProjectName(project);
        applicationWindow = this;
        this.newProject = project;
        this.oldProject = project; // until we have a better value...
        if (srnPrefix != null) {
            this.srnPrefix = srnPrefix;
        } else {
            this.srnPrefix = "b-"; // for latest Talking Book hardware
        }
    }

    private void runApplication() throws Exception {
        OsUtils.enableOSXQuitStrategy();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
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
        opLogImpl = new OperationLogImpl(logsDir, tbLoaderConfig.getTbLoaderId());
        OperationLog.setImplementation(opLogImpl);
        OperationLog.Operation opLog = OperationLog.log("WindowsTBLoaderStart")
            .put("tbcdid", tbLoaderConfig.getTbLoaderId())
            .put("project", newProject);

        // get image revision
        File[] files = baseDirectory.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                String lowercase = name.toLowerCase();
                return lowercase.endsWith(".rev");
            }
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
                System.exit(NORMAL);
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
                System.exit(NORMAL);
            }
            opLog.put("MultipleVersions", "KeepUnpublished");
        }
        opLog.finish();

        JPanel panel = new JPanel();
        JLabel warning;
        //@TODO: this should be a tbloaderconfig propery
        if (tbLoaderConfig.getSrnPrefix().equals("a-")) {
            panel.setBackground(Color.CYAN);
            warning = new JLabel("Use with OLD TBS only");
            warning.setForeground(Color.RED);
        } else {
            warning = new JLabel("Use with NEW TBS only");
            warning.setForeground(Color.RED);
        }
        JLabel packageLabel = new JLabel("Update:");
        JLabel communityFilterLabel = new JLabel("Community filter:");
        JLabel communityLabel = new JLabel("Community:");
        JLabel currentLocationLabel = new JLabel("Current Location:");
        JLabel dateLabel = new JLabel("First Rotation Date:");
        oldDeploymentText = new JTextField();
        oldDeploymentText.setEditable(false);
        oldValue = new JLabel("Previous");
        newValue = new JLabel("Next");
        lastUpdatedText = new JTextField();
        lastUpdatedText.setEditable(false);
        oldCommunityText = new JTextField();
        oldCommunityText.setEditable(false);
        JLabel deviceLabel = new JLabel("Talking Book Device:");
        JLabel idLabel = new JLabel("Serial number:");
        JLabel revisionLabel = new JLabel("Firmware:");
        JLabel imageLabel = new JLabel("Content:");
        JLabel forceFirmwareText = new JLabel("Refresh?");
        statusRight = new JTextArea(2, 40);
        statusRight.setEditable(false);
        statusRight.setLineWrap(true);
        statusLeft = new JTextArea(2, 40);
        statusLeft.setEditable(false);
        statusLeft.setLineWrap(true);
        oldSrnText = new JTextField();
        oldSrnText.setEditable(false);
        newSrnText = new JTextField();
        newSrnText.setEditable(false);
        oldFirmwareRevisionText = new JTextField();
        oldFirmwareRevisionText.setEditable(false);
        newFirmwareRevisionText = new JTextField();
        newFirmwareRevisionText.setEditable(false);
        oldImageText = new JTextField();
        oldImageText.setEditable(false);
        newImageText = new JTextField();
        newImageText.setEditable(false);
        final JXDatePicker datePicker = new JXDatePicker(new Date());
        dateRotation = datePicker.getDate().toString();
        datePicker.getEditor().setEditable(false);
        datePicker.setFormats("yyyy/MM/dd"); //dd MMM yyyy
        datePicker.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dateRotation = datePicker.getDate().toString();
            }
        });

        newDeploymentList = new JComboBox<String>();
        newDeploymentList.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                comboBoxActionPerformed(e);
            }
        });
        newCommunityModel = new FilteringComboBoxModel();
        newCommunityFilter = new JTextField("", 40);
        newCommunityFilter.getDocument().addDocumentListener(new DocumentListener() {
            // Listen for any change to the text
            public void changedUpdate(DocumentEvent e) { common(); }
            public void removeUpdate(DocumentEvent e) { common(); }
            public void insertUpdate(DocumentEvent e) { common(); }
            void common() { newCommunityModel.setFilterString(newCommunityFilter.getText()); }
        });
        newCommunityList = new JComboBox(newCommunityModel);
        newCommunityList.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                comboBoxActionPerformed(e);
            }
        });
        driveList = new JComboBox<TBDeviceInfo>();
        driveList.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                comboBoxActionPerformed(e);
            }
        });
        currentLocationList = new JComboBox<String>(currentLocation);
        forceFirmware = new JCheckBox();
        forceFirmware.setSelected(true);
        updateButton = new JButton("Update TB");
        updateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                buttonActionPerformed(e);
            }
        });
        grabStatsOnlyButton = new JButton("Get Stats");
        grabStatsOnlyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                buttonActionPerformed(e);
            }
        });

        GroupLayout layout = new GroupLayout(panel);
        panel.setLayout(layout);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        layout.setHorizontalGroup(layout.createSequentialGroup()
                                          .addGroup(layout.createParallelGroup(LEADING)
                                                            .addComponent(deviceLabel)
                                                            .addComponent(currentLocationLabel)
                                                            .addComponent(packageLabel)
                                                            .addComponent(communityFilterLabel)
                                                            .addComponent(communityLabel)
                                                            .addComponent(imageLabel)
                                                            .addComponent(dateLabel)
                                                            .addComponent(revisionLabel)
                                                            .addComponent(idLabel)
                                                            .addComponent(forceFirmwareText))
                                          .addGroup(layout.createParallelGroup(LEADING)
                                                            .addComponent(warning)
                                                            .addComponent(driveList)
                                                            .addComponent(currentLocationList)
                                                            .addComponent(newValue)
                                                            .addComponent(newDeploymentList)
                                                            .addComponent(newCommunityFilter)
                                                            .addComponent(newCommunityList)
                                                            .addComponent(newImageText)
                                                            .addComponent(datePicker)
                                                            .addComponent(newFirmwareRevisionText)
                                                            .addComponent(newSrnText)
                                                            .addComponent(forceFirmware)
                                                            .addComponent(updateButton)
                                                            .addComponent(statusLeft))
                                          .addGroup(layout.createParallelGroup(LEADING)
                                                            .addComponent(oldValue)
                                                            .addComponent(oldDeploymentText)
                                                            .addComponent(oldCommunityText)
                                                            .addComponent(oldImageText)
                                                            .addComponent(lastUpdatedText)
                                                            .addComponent(oldFirmwareRevisionText)
                                                            .addComponent(oldSrnText)
                                                            .addComponent(forceFirmwareText)
                                                            .addComponent(grabStatsOnlyButton)
                                                            .addComponent(statusRight)));

        layout.setVerticalGroup(layout.createSequentialGroup()
                                        .addComponent(warning)
                                        .addGroup(layout.createParallelGroup(BASELINE)
                                                          .addComponent(deviceLabel)
                                                          .addComponent(driveList))
                                        .addGroup(layout.createParallelGroup(BASELINE)
                                                          .addComponent(currentLocationLabel)
                                                          .addComponent(currentLocationList))
                                        .addGroup(layout.createParallelGroup(BASELINE)
                                                          .addComponent(newValue)
                                                          .addComponent(oldValue))
                                        .addGroup(layout.createParallelGroup(BASELINE)
                                                          .addComponent(packageLabel)
                                                          .addComponent(newDeploymentList)
                                                          .addComponent(oldDeploymentText))
                                        .addGroup(layout.createParallelGroup(BASELINE)
                                                          .addComponent(communityFilterLabel)
                                                          .addComponent(newCommunityFilter))
                                        .addGroup(layout.createParallelGroup(BASELINE)
                                                          .addComponent(communityLabel)
                                                          .addComponent(newCommunityList)
                                                          .addComponent(oldCommunityText))
                                        .addGroup(layout.createParallelGroup(BASELINE)
                                                          .addComponent(imageLabel)
                                                          .addComponent(newImageText)
                                                          .addComponent(oldImageText))
                                        .addGroup(layout.createParallelGroup(BASELINE)
                                                          .addComponent(dateLabel)
                                                          .addComponent(datePicker)
                                                          .addComponent(lastUpdatedText))
                                        .addGroup(layout.createParallelGroup(BASELINE)
                                                          .addComponent(revisionLabel)
                                                          .addComponent(newFirmwareRevisionText)
                                                          .addComponent(oldFirmwareRevisionText))
                                        .addGroup(layout.createParallelGroup(BASELINE)
                                                          .addComponent(idLabel)
                                                          .addComponent(oldSrnText)
                                                          .addComponent(newSrnText))
                                        .addGroup(layout.createParallelGroup(BASELINE)
                                                          .addComponent(forceFirmwareText)
                                                          .addComponent(forceFirmware))
                                        .addGroup(layout.createParallelGroup(BASELINE)
                                                          .addComponent(updateButton)
                                                          .addComponent(grabStatsOnlyButton))
                                        .addGroup(layout.createParallelGroup(BASELINE)
                                                          .addComponent(statusLeft)
                                                          .addComponent(statusRight)));

        setSize(700, 550);
        add(panel, BorderLayout.CENTER);
        setLocationRelativeTo(null);

        //Logger.init();
        fillDeploymentList();
        resetUI(true);
        setVisible(true);
        JOptionPane.showMessageDialog(applicationWindow,
                                      "Remember to power Talking Book with batteries before connecting with USB.",
                                      "Use Batteries!", JOptionPane.DEFAULT_OPTION);
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
            TbFile softwareDir = new FsFile(baseDirectory).open(TBLoaderConstants.SOFTWARE_SUBDIR);

            File dropboxDir = ACMConfiguration.getInstance().getGlobalShareDir();
            if (!dropboxDir.exists()) {
                JOptionPane.showMessageDialog(applicationWindow, dropboxDir.getAbsolutePath()
                                                      + " does not exist; cannot find the Dropbox path. Please contact ICT staff.",
                                              "Cannot Find Dropbox!", JOptionPane.DEFAULT_OPTION);
                System.exit(ERROR);
            }
            //TbFile dropboxFile = new FsFile(dropboxDir);

            // Look for ~/LiteracyBridge/1234.dev file.
            File[] files = applicationHomeDirectory.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    String lowercase = name.toLowerCase();
                    return lowercase.endsWith(TBLoaderConstants.DEVICE_FILE_EXTENSION);
                }
            });
            if (files.length == 1) {
                String deviceId = files[0].getName()
                        .substring(0, files[0].getName().length() - 4)
                        .toUpperCase();

                // Like "~/Dropbox/tbcd1234"
                File tbLoaderDir = new File(dropboxDir, TBLoaderConstants.COLLECTED_DATA_DROPBOXDIR_PREFIX + deviceId);
                logsDir = new File(tbLoaderDir, "log");
                logsDir.mkdirs();
                //TbFile tbLoaderDir = dropboxFile.open(
                //        TBLoaderConstants.COLLECTED_DATA_DROPBOXDIR_PREFIX + deviceId);
                if (!tbLoaderDir.exists()) {
                    JOptionPane.showMessageDialog(applicationWindow, tbLoaderDir.toString()
                                                          + " does not exist; cannot find the Dropbox collected data path. Please contact ICT staff.",
                                                  "Cannot Find Dropbox Collected Data Folder!",
                                                  JOptionPane.DEFAULT_OPTION);
                    System.exit(ERROR);
                }
                // Like ~/Dropbox/tbcd1234/collected-data
                TbFile collectedDataDir = new FsFile(tbLoaderDir).open(
                        TBLoaderConstants.COLLECTED_DATA_SUBDIR_NAME);

                TbFile tempDir = new FsFile(Files.createTempDirectory("tbloader-tmp").toFile());

                tbLoaderConfig = new TBLoaderConfig.Builder()
                        .withTbLoaderId(deviceId)
                        .withProject(newProject)
                        .withSrnPrefix(srnPrefix)
                        .withCollectedDataDirectory(collectedDataDir)
                        .withTempDirectory(tempDir)
                        .withWindowsUtilsDirectory(softwareDir)
                        .build();
            } else {
                JOptionPane.showMessageDialog(applicationWindow,
                                              "This computer does not appear to be configured to use the TB Loader yet.  It needs a unique device tbSrn. Please contact ICT staff to get this.",
                                              "This Computer has no ID!",
                                              JOptionPane.DEFAULT_OPTION);
                System.exit(ERROR);
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Exception while setting DeviceId and paths", e);
            throw e;
        }
    }

    void getFirmwareRevisionNumbers() {
        revision = "(No firmware)";

        File basicContentPath = new File(baseDirectory,
                                         TBLoaderConstants.CONTENT_SUBDIR + File.separator
                                                 + newDeploymentList.getSelectedItem().toString()
                                                 + "/" + TBLoaderConstants.CONTENT_BASIC_SUBDIR);
        LOG.log(Level.INFO, "DEPLOYMENT:" + newDeploymentList.getSelectedItem().toString());
        try {
            File[] files;
            if (basicContentPath.exists()) {
                // get Package
                files = basicContentPath.listFiles(new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String name) {
                        String lowercase = name.toLowerCase();
                        return lowercase.endsWith(".img");
                    }
                });
                if (files.length > 1)
                    revision = "(Multiple Firmwares!)";
                else if (files.length == 1) {
                    revision = files[0].getName();
                    revision = revision.substring(0, revision.length() - 4);
                }
                newFirmwareRevisionText.setText(revision);
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

    private synchronized void fillCommunityList() throws IOException {
        String filter = newCommunityModel.setFilterString(null);
        newCommunityList.removeAllItems();
        File[] files;

        File fCommunityDir = new File(baseDirectory,
                                      TBLoaderConstants.CONTENT_SUBDIR + File.separator
                                              + newDeploymentList.getSelectedItem().toString() + "/"
                                              + TBLoaderConstants.COMMUNITIES_SUBDIR);

        files = fCommunityDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return dir.isDirectory();
            }
        });
        newCommunityList.addItem(NO_COMMUNITY_SELECTED);
        for (File f : files) {
            newCommunityList.addItem(f.getName());
        }
        setCommunityList();
        newCommunityModel.setFilterString(filter);
    }

    private synchronized void setCommunityList() throws IOException {
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
        try {
            getImageFromCommunity(newCommunityList.getSelectedItem().toString());
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
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
        File f = new File(ACMConfiguration.getInstance().getApplicationHomeDirectory(),
                          devFilename);

        // Get the most recent serial number assigned.
        if (f.exists()) {
            try (DataInputStream in = new DataInputStream(new FileInputStream(f))) {
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
            f.delete();
        }
        if (serialnumber == TBLoaderConstants.STARTING_SERIALNUMBER) {
            // if file doesn't exist, use the SRN = STARTING_SERIALNUMBER
            // TODO:raise exception and tell user to register the device or ensure file wasn't lost
        }

        // The number we're assigning now...
        serialnumber++;

        try (DataOutputStream os = new DataOutputStream(new FileOutputStream(f))) {
            os.writeInt(serialnumber);
        }
        // Back up the file in case of loss.
        File dropboxDir = ACMConfiguration.getInstance().getGlobalShareDir();
        File backupDir = new File(dropboxDir,
                                  TBLoaderConstants.COLLECTED_DATA_DROPBOXDIR_PREFIX + deviceId);
        File backupFile = new File(backupDir, devFilename);
        try (DataOutputStream os = new DataOutputStream(new FileOutputStream(backupFile))) {
            os.writeInt(serialnumber);
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
            driveList.addItem(new TBDeviceInfo(null, TBLoaderConstants.NO_DRIVE, srnPrefix));
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
        List<File> roots = new ArrayList<File>();
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
            Set<String> oldList = new HashSet<String>();

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
                            statusLeft.setText("");
                            statusLeft.setForeground(Color.BLACK);
                        }
                        try {
                            populatePreviousValuesFromCurrentDrive();
                        } catch (Exception e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                        try {
                            fillCommunityList();
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
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

    private String getImageFromCommunity(String community) throws Exception {
//    File imagesDir = new File(
//        CONTENT_SUBDIR + File.separator + newDeploymentList.getSelectedItem().toString() + "/"
//            + IMAGES_SUBDIR + "/");
        File deploymentDirectory = new File(baseDirectory,
                                            TBLoaderConstants.CONTENT_SUBDIR + File.separator
                                                    + newDeploymentList.getSelectedItem()
                                                    .toString());
//    TBFileSystem imagesTbFs = DefaultTBFileSystem.open(imagesDir);
        String imageName = TBLoaderUtils.getImageForCommunity(deploymentDirectory, community);
        newImageText.setText(imageName);
        return imageName;
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
                                          JOptionPane.DEFAULT_OPTION);
        }

        String sn = currentTbDevice.getSerialNumber(srnPrefix);

        if (!isSerialNumberFormatGood(srnPrefix, sn)) {
            if (sn.substring(1, 2).equals("-")) {
                if (sn.compareToIgnoreCase("b-") < 0) {
                    JOptionPane.showMessageDialog(applicationWindow,
                                                  "This appears to be an OLD TB.  If so, please close this program and open the TB Loader for old TBs.",
                                                  "OLD TB!", JOptionPane.WARNING_MESSAGE);
                    return;
                } else if (sn.compareToIgnoreCase("b-") > 0) {
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
            oldFirmwareRevisionText.setText(oldDeploymentInfo.getFirmwareRevision());
            oldImageText.setText(oldDeploymentInfo.getPackageName());
            oldDeploymentText.setText(oldDeploymentInfo.getDeploymentName());
            lastUpdatedText.setText(oldDeploymentInfo.getUpdateTimestamp());

            //TODO: Better check that this works properly!
            newSrnText.setText(oldDeploymentInfo.getSerialNumber());

            oldCommunityText.setText(oldDeploymentInfo.getCommunity());
            // If we want to do this...
            // oldProjectText.setTExt(oldDeploymentInfo.getProjectName());
        } else {
            newSrnText.setText(TBLoaderConstants.NEED_SERIAL_NUMBER);
        }
    }

    /**
     * Handles combo box selections for the Drive list, Community list, and Deployment List.
     *
     * @param e The combo selection event.
     */
    private void comboBoxActionPerformed(ActionEvent e) {
        TBDeviceInfo di;
        Object o = e.getSource();
        if (refreshingDriveInfo || !startUpDone)
            return;

        if (o == driveList) {
            oldSrnText.setText("");
            newSrnText.setText("");
            di = (TBDeviceInfo) ((JComboBox<String>) e.getSource()).getSelectedItem();
            currentTbDevice = di;
            if (di != null && di.getRootFile() != null) {
                LOG.log(Level.INFO,
                        "Drive changed: " + di.getRootFile().toString() + di.getLabel());
                try {
                    populatePreviousValuesFromCurrentDrive();
                } catch (Exception e1) {
                    // TODO Auto-generated catch block
                    JOptionPane.showMessageDialog(applicationWindow, e1.toString(), "Error",
                                                  JOptionPane.ERROR_MESSAGE);
                    e1.printStackTrace();
                }
                try {
                    fillCommunityList();
                } catch (IOException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
                getFirmwareRevisionNumbers();
            }
        } else if (o == newCommunityList) {
            JComboBox<String> cl = (JComboBox<String>) o;
            try {
                if (cl.getSelectedItem() != null) {
                    getImageFromCommunity(cl.getSelectedItem().toString());
                }
            } catch (Exception e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
        } else if (o == newDeploymentList) {
            getFirmwareRevisionNumbers();
            refreshUI();
            try {
                getImageFromCommunity(newCommunityList.getSelectedItem().toString());
            } catch (Exception e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
        }
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
        } else if (b == grabStatsOnlyButton) {
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

                if (community.equals(NO_COMMUNITY_SELECTED)) {
                    int response = JOptionPane.showConfirmDialog(this,
                                                                 "No community selected. This will prevent us from "+
                                                                         "generating accurate usage statistics.\nAre you sure?",
                                                                 "Confirm",
                                                                 JOptionPane.YES_NO_OPTION);
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
                if (newSrnText.getText().equalsIgnoreCase(TBLoaderConstants.NEED_SERIAL_NUMBER)) {
                    int intSrn = allocateNextSerialNumberFromTbLoader();
                    String lowerSrn = String.format("%04x", intSrn);
                    String srn = (tbLoaderConfig.getSrnPrefix() + tbLoaderConfig.getTbLoaderId()
                            + lowerSrn).toUpperCase();
                    di.setSerialNumber(srn);
                    newSrnText.setText(srn);
                }
            }
            if (!isUpdate && dateRotation == null) {
                dateRotation = new Date().toString();
            }

            LOG.log(Level.INFO, "ID:" + di.getSerialNumber());
            statusRight.setText("STATUS: Starting\n");
            statusLeft.setText("");
            statusLeft.setForeground(Color.BLACK);

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
        } else if (resetDrives && refreshingDriveInfo) {
            LOG.log(Level.INFO, " - drive list currently being filled by drive monitor");
        }
        LOG.log(Level.INFO, " -refresh UI");
        refreshUI();
    }

    private void onCopyFinished(final String endMsg, final String endTitle) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(applicationWindow, endMsg, endTitle, JOptionPane.DEFAULT_OPTION);
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

        updateButton.setText("Update TB");
        connected = isDriveConnected();
        if (connected && !updatingTB) {
            updateButton.setEnabled(true);
            grabStatsOnlyButton.setEnabled(true);
            statusRight.setText("STATUS: Ready");
            statusLeft.setText(statusLeft.getText() + "\n\n");
            LOG.log(Level.INFO, "STATUS: Ready");
        } else {
            updateButton.setEnabled(false);
            grabStatsOnlyButton.setEnabled(false);
            if (!connected) {
                oldDeploymentText.setText("");
                oldCommunityText.setText("");
                oldFirmwareRevisionText.setText("");
                oldImageText.setText("");
                newSrnText.setText("");
                oldSrnText.setText("");
                lastUpdatedText.setText("");
                LOG.log(Level.INFO, "STATUS: " + TBLoaderConstants.NO_DRIVE);
                statusRight.setText("STATUS: " + TBLoaderConstants.NO_DRIVE);
            }
            try {
                if (newCommunityList.getSelectedItem() != null) {
                    getImageFromCommunity(newCommunityList.getSelectedItem().toString());
                }
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    private void disableAll() {
        updateButton.setEnabled(false);
        grabStatsOnlyButton.setEnabled(false);
    }

    public enum Operation {Update, CollectStats};

    // TODO: Move this to its own file.
    public class CopyThread extends Thread {

        final Operation operation;
        final String devicePath;
        boolean criticalError = false;
        boolean alert = true;
        ProgressListener progressListenerListener = new ProgressListener() {
            ProgressListener.Steps currentStep = ProgressListener.Steps.ready;

            @Override
            public void step(ProgressListener.Steps step) {
                currentStep = step;
                statusRight.setText(step.description());
                LOG.log(Level.INFO, "STEP: " + step.description());
            }

            @Override
            public void detail(String value) {
                statusRight.setText(currentStep.description() + "\n\n" + value);
                LOG.log(Level.INFO, "DETAIL: " + value);
            }

            @Override
            public void log(String value) {
                statusLeft.setText(value + "\n" + statusLeft.getText());
                LOG.log(Level.INFO, "PROGRESS: " + value);
            }

            @Override
            public void log(boolean append, String value) {
                if (!append) {
                    log(value);
                } else {
                    LOG.log(Level.INFO, "PROGRESS: " + value);
                    String oldValue = statusLeft.getText();
                    int nl = oldValue.indexOf("\n");
                    if (nl > 0) {
                        String pref = oldValue.substring(0, nl);
                        String suff = oldValue.substring(nl + 1);
                        statusLeft.setText(pref + value + "\n" + suff);
                    } else {
                        statusLeft.setText(oldValue + value);
                    }
                }
            }

            public void error(String value) {
                log(value);
                statusLeft.setForeground(Color.RED);
                LOG.log(Level.SEVERE, "SEVERE: " + value);
            }
        };

        CopyThread(String devicePath, Operation operation) {
            this.devicePath = devicePath;
            this.operation = operation;
        }

        private void grabStatsOnly(DeploymentInfo newDeploymentInfo) {
            OperationLog.Operation opLog = OperationLog.startOperation("TbLoaderGrabStats");
            opLog.put("serialno", oldDeploymentInfo.getSerialNumber())
                    .put("project", oldDeploymentInfo.getProjectName())
                    .put("deployment", oldDeploymentInfo.getDeploymentName())
                    .put("package", oldDeploymentInfo.getPackageName())
                    .put("community", oldDeploymentInfo.getCommunity());

            TBLoaderCore.Result result = null;
            try {

                TBLoaderCore tbLoader = new TBLoaderCore.Builder().withTbLoaderConfig(
                        tbLoaderConfig)
                        .withTbDeviceInfo(currentTbDevice)
                        .withOldDeploymentInfo(oldDeploymentInfo)
                        .withNewDeploymentInfo(newDeploymentInfo)
                        .withLocation(currentLocationList.getSelectedItem().toString())
                        .withRefreshFirmware(false)
                        .withStatsOnly()
                        .withProgressListener(progressListenerListener)
                        .build();
                result = tbLoader.update();

                opLog.put("success", result.gotStatistics);
            } finally {
                opLog.finish();
                String endMsg, endTitle;
                if (result.gotStatistics) {
                    endMsg = "Got Stats!";
                    endTitle = "Success";
                } else {
                    endMsg = "Could not get stats for some reason.";
                    endTitle = "Failure";
                }
                onCopyFinished(endMsg, endTitle);
            }
        }

        private void update(DeploymentInfo newDeploymentInfo) {
            String endMsg = "";
            String endTitle = "";
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

                TBLoaderCore tbLoader = new TBLoaderCore.Builder().withTbLoaderConfig(
                        tbLoaderConfig)
                        .withTbDeviceInfo(currentTbDevice)
                        .withDeploymentDirectory(sourceImage)
                        .withOldDeploymentInfo(oldDeploymentInfo)
                        .withNewDeploymentInfo(newDeploymentInfo)
                        .withLocation(currentLocationList.getSelectedItem().toString())
                        .withRefreshFirmware(forceFirmware.isSelected())
                        .withProgressListener(progressListenerListener)
                        .build();
                result = tbLoader.update();

                opLog.put("gotstatistics", result.gotStatistics)
                    .put("corrupted", result.corrupted)
                    .put("reformatfailed", result.reformatFailed)
                    .put("verified", result.verified);

                if (!result.gotStatistics) {
                    LOG.log(Level.SEVERE, "Could not get statistics!");
                    progressListenerListener.error("Could not get statistics.");
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
                if (result.verified) {
                    endMsg = "Talking Book has been updated and verified\nin " + result.duration
                            + ".";
                    endTitle = "Success";
                } else {
                    endMsg = "Update failed verification.  Try again or replace memory card.";
                    endTitle = "Failure";
                }
                onCopyFinished(endMsg, endTitle);
            }

        }

        @Override
        public void run() {
            DeploymentInfo.DeploymentInfoBuilder builder = new DeploymentInfo.DeploymentInfoBuilder()
                    .withSerialNumber(newSrnText.getText())
                    .withProjectName(newProject)
                    .withDeploymentName(newDeploymentList.getSelectedItem().toString())
                    .withPackageName(newImageText.getText())
                    .withUpdateDirectory(null)
                    .withUpdateTimestamp(dateRotation)
                    .withFirmwareRevision(newFirmwareRevisionText.getText())
                    .withCommunity(newCommunityList.getSelectedItem().toString());
            DeploymentInfo newDeploymentInfo = builder.build();
            if (this.operation == Operation.Update) {
                update(newDeploymentInfo);
            } else if (this.operation == Operation.CollectStats) {
                grabStatsOnly(newDeploymentInfo);
            }
        }
    }

}
