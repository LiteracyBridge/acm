package org.literacybridge.acm.tbloader;

import static javax.swing.GroupLayout.Alignment.BASELINE;
import static javax.swing.GroupLayout.Alignment.LEADING;
import static org.literacybridge.core.tbloader.TBLoaderConstants.COLLECTED_DATA_DROPBOXDIR_PREFIX;
import static org.literacybridge.core.tbloader.TBLoaderConstants.COLLECTED_DATA_SUBDIR_NAME;
import static org.literacybridge.core.tbloader.TBLoaderConstants.COMMUNITIES_SUBDIR;
import static org.literacybridge.core.tbloader.TBLoaderConstants.CONTENT_BASIC_SUBDIR;
import static org.literacybridge.core.tbloader.TBLoaderConstants.CONTENT_SUBDIR;
import static org.literacybridge.core.tbloader.TBLoaderConstants.DEVICE_FILE_EXTENSION;
import static org.literacybridge.core.tbloader.TBLoaderConstants.IMAGES_SUBDIR;
import static org.literacybridge.core.tbloader.TBLoaderConstants.NEED_SERIAL_NUMBER;
import static org.literacybridge.core.tbloader.TBLoaderConstants.NO_DRIVE;
import static org.literacybridge.core.tbloader.TBLoaderConstants.NO_SERIAL_NUMBER;
import static org.literacybridge.core.tbloader.TBLoaderConstants.PROJECT_FILE_EXTENSION;
import static org.literacybridge.core.tbloader.TBLoaderConstants.SCRIPT_SUBDIR;
import static org.literacybridge.core.tbloader.TBLoaderConstants.STARTING_SERIALNUMBER;
import static org.literacybridge.core.tbloader.TBLoaderConstants.TRIGGER_FILE_CHECK;
import static org.literacybridge.core.tbloader.TBLoaderConstants.UNPUBLISHED_REV;

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
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileSystemView;

import org.jdesktop.swingx.JXDatePicker;
import org.literacybridge.acm.Constants;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.utils.OsUtils;
import org.literacybridge.core.OSChecker;
import org.literacybridge.core.ProgressListener;
import org.literacybridge.core.fs.DefaultTBFileSystem;
import org.literacybridge.core.fs.RelativePath;
import org.literacybridge.core.fs.TBFileSystem;
import org.literacybridge.core.tbloader.CommandLineUtils;
import org.literacybridge.core.tbloader.DeploymentInfo;
import org.literacybridge.core.tbloader.TBDeviceInfo;
import org.literacybridge.core.tbloader.TBLoaderConfig;
import org.literacybridge.core.tbloader.TBLoaderConstants;
import org.literacybridge.core.tbloader.TBLoaderCore;

@SuppressWarnings("serial")
public class TBLoader extends JFrame {
  private static final Logger LOG = Logger.getLogger(TBLoader.class.getName());
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
  private JTextArea status;
  private JTextArea status2;
  private JButton updateButton;
  private JButton grabStatsOnlyButton;
  private String revision;

  private int durationSeconds;
  private TBLoaderCore currentDrive;    // @TODO: "currentDrive" is as misleading as possible
  private String srnPrefix;
  private String newProject;
  private String oldProject;
  private String syncSubPath;

  private JCheckBox forceFirmware;

  private DeploymentInfo oldDeploymentInfo;

  private TBLoaderConfig tbLoaderConfig;

  class WindowEventHandler extends WindowAdapter {
    @Override
    public void windowClosing(WindowEvent evt) {
      refreshFileListForCollectedData();
      LOG.log(Level.INFO, "closing app");
      System.exit(0);
    }
  }

  private String currentLocation[] = new String[] { "Select location",
      "Community", "Jirapa office", "Wa office", "Other" };

  void getFirmwareRevisionNumbers() {
    revision = "(No firmware)";

    File basicContentPath = new File(
        CONTENT_SUBDIR + newDeploymentList.getSelectedItem().toString() + "/"
            + CONTENT_BASIC_SUBDIR);
    LOG.log(Level.INFO,
        "DEPLOYMENT:" + newDeploymentList.getSelectedItem().toString());
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

  public TBLoader(String project, String srnPrefix) throws IOException {
    String acmDash = Constants.ACM_DIR_NAME + "-";
    if (project != null && project.toUpperCase().startsWith(acmDash)) {
      // Most apps take ACM-XYZ. The TB-Loader only wants XYZ.
      project = project.substring(acmDash.length());
    }
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

    // get image revision
    // TODO: fix assumption about current working directory. (It's assumed to be ~/LiteracyBridge/TB-Loaders/{PROJECT} )
    File swPath = new File(".");
    File[] files = swPath.listFiles(new FilenameFilter() {
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
        if (!f.getName().startsWith(UNPUBLISHED_REV)) {
          f.delete();
        } else {
          unpublished = true;
          imageRevision = f.getName();
          imageRevision = imageRevision.substring(0,
              imageRevision.length() - 4);
        }
      }
      if (!unpublished) {
        JOptionPane.showMessageDialog(null,
            "Revision conflict. Please click OK to shutdown.\nThen restart the TB-Loader to get the latest published version.");
        System.exit(NORMAL);
      }
    } else if (files.length == 1) {
      imageRevision = files[0].getName();
      imageRevision = imageRevision.substring(0, imageRevision.length() - 4);
    }
    setTitle("TB-Loader " + Constants.ACM_VERSION + "/" + imageRevision);
    if (imageRevision.startsWith(UNPUBLISHED_REV)) {
      Object[] options = { "Yes-refresh from published",
          "No-keep unpublished" };
      int answer = JOptionPane.showOptionDialog(null,
          "This TB Loader is running an unpublished version.\nWould you like to delete the unpublished version and use the latest published version?",
          "Unpublished", JOptionPane.YES_NO_OPTION,
          JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
      if (answer == JOptionPane.YES_OPTION) {
        files[0].delete();
        JOptionPane.showMessageDialog(null,
            "Click OK to shutdown. Then restart to get the latest published version of the TB Loader.");
        System.exit(NORMAL);
      }
    }

    JPanel panel = new JPanel();
    JLabel warning;
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
    status = new JTextArea(2, 40);
    status.setEditable(false);
    status.setLineWrap(true);
    status2 = new JTextArea(2, 40);
    status2.setEditable(false);
    status2.setLineWrap(true);
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
    final JXDatePicker datePicker = new JXDatePicker();
    datePicker.getEditor().setEditable(false);
    datePicker.setFormats(new String[] { "yyyy/MM/dd" }); //dd MMM yyyy
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
      public void common() {
        newCommunityModel.setFilterString(newCommunityFilter.getText());
      }
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
            .addComponent(status2))
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
            .addComponent(status)));

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
            .addComponent(status2)
            .addComponent(status)));

    setSize(700, 550);
    add(panel, BorderLayout.CENTER);
    setLocationRelativeTo(null);

    //Logger.init();
    fillDeploymentList();
    resetUI(true);
    setVisible(true);
    LOG.log(Level.INFO, "set visibility - starting drive monitoring");
    deviceMonitorThread.setDaemon(true);
    deviceMonitorThread.start();
    startUpDone = true;
    JOptionPane.showMessageDialog(null,
        "Remember to power Talking Book with batteries before connecting with USB.",
        "Use Batteries!", JOptionPane.DEFAULT_OPTION);
  }

  public boolean startUpDone = false;
  public boolean refreshingDriveInfo = false;
  public boolean updatingTB = false;

  public static void main(String[] args) throws Exception {
    String project = args[0];
    String srnPrefix = "b-"; // for latest Talking Book hardware

    if (args.length == 2) {
      srnPrefix = args[1];
    }

    new TBLoader(project, srnPrefix).runApplication();
  }

  private void setDeviceIdAndPaths() throws IOException {
    try {
      String homePath = System.getProperty("user.home");
      String LB_DIR = new String(homePath + "/LiteracyBridge");
      File f = new File(LB_DIR);
      f.mkdirs();

      File dropboxDir = ACMConfiguration.getInstance().getGlobalShareDir();
      if (!dropboxDir.exists()) {
        JOptionPane.showMessageDialog(null, dropboxDir.getAbsolutePath()
                + " does not exist; cannot find the Dropbox path. Please contact ICT staff.",
            "Cannot Find Dropbox!", JOptionPane.DEFAULT_OPTION);
        System.exit(ERROR);
      }
      TBFileSystem dropboxFS = DefaultTBFileSystem.open(dropboxDir);

      File[] files = f.listFiles(new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
          String lowercase = name.toLowerCase();
          return lowercase.endsWith(DEVICE_FILE_EXTENSION);
        }
      });
      if (files.length == 1) {
        String deviceID = files[0].getName()
            .substring(0, files[0].getName().length() - 4)
            .toUpperCase();

        // Like "/Users/mike/Dropbox (Literacy Bridge)/tbcd1234"
        RelativePath collectedDataFile = RelativePath.parse(
            COLLECTED_DATA_DROPBOXDIR_PREFIX + deviceID);
        if (!dropboxFS.fileExists(collectedDataFile)) {
          JOptionPane.showMessageDialog(null, collectedDataFile.toString()
                  + " does not exist; cannot find the Dropbox collected data path. Please contact ICT staff.",
              "Cannot Find Dropbox Collected Data Folder!",
              JOptionPane.DEFAULT_OPTION);
          System.exit(ERROR);
        }

        tbLoaderConfig = new TBLoaderConfig.Builder()
            .withDeviceID(deviceID)
            .withProject(newProject)
            .withSrnPrefix(srnPrefix)
            .withHomePath(homePath)
            .withDropbox(dropboxFS, collectedDataFile)
            .withTempFileSystem(DefaultTBFileSystem.open(Files.createTempDirectory("tbloader-tmp-" + System.currentTimeMillis()).toFile()))
            .build();
      } else {
        JOptionPane.showMessageDialog(null,
            "This computer does not appear to be configured to use the TB Loader yet.  It needs a unique device tbSrn. Please contact ICT staff to get this.",
            "This Computer has no ID!", JOptionPane.DEFAULT_OPTION);
        System.exit(ERROR);
      }
    } catch (Exception e) {
      LOG.log(Level.SEVERE, "Exception while setting DeviceId and paths", e);
      throw e;
    }
  }

  private File prevSelected = null;
  private int prevSelectedCommunity = -1;

  private synchronized void fillDeploymentList() {
    int indexSelected = -1;
    File contentPath = new File("content");
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

    File fCommunityDir = new File(
        CONTENT_SUBDIR + newDeploymentList.getSelectedItem().toString() + "/"
            + COMMUNITIES_SUBDIR);

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
        if (newCommunityList.getItemAt(i)
            .toString()
            .equalsIgnoreCase(oldCommunityText.getText())) {
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
  private int allocateNextSerialNumberFromDevice() throws Exception {
    int serialnumber = STARTING_SERIALNUMBER;
    String devFilename =
        tbLoaderConfig.getDeviceID() + DEVICE_FILE_EXTENSION; // xxxx.dev
    File f = new File(
        tbLoaderConfig.getHomePath() + File.separator + "LiteracyBridge" + File.separator
            + devFilename); // File f = new File(dropboxCollectionFolder,TBLoader.deviceID+".cnt");

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
    if (serialnumber == STARTING_SERIALNUMBER) {
      // if file doesn't exist, use the SRN = STARTING_SERIALNUMBER
      // TODO:raise exception and tell user to register the device or ensure file wasn't lost
    }

    // The number we're assigning now...
    serialnumber++;

    try (DataOutputStream os = new DataOutputStream(new FileOutputStream(f))) {
      os.writeInt(serialnumber);
    }
    // Back up the file in case of loss.
    try (InputStream fileContent = new FileInputStream(f)) {
    tbLoaderConfig.getDropboxFileSystem().createNewFile(
        new RelativePath(currentDrive.getDropboxCollectedDataPath(), devFilename),
        fileContent, true);
    }

    return serialnumber;
  }

  private synchronized void fillDriveList(File[] roots) {
    driveList.removeAllItems();
    currentDrive = null;
    int index = -1;
    int i = 0;
    for (File root : roots) {

      String label = FileSystemView.getFileSystemView()
          .getSystemDisplayName(root);
      if (label.trim().equals("CD Drive") || label.startsWith("DVD") || label.contains("Macintosh")) {
        continue;
      }
      // Ignore drives shared by Parallels. Value determined empirically.
      if (OsUtils.WINDOWS && label.indexOf(" on 'Mac' (") >= 0) {
        continue;
      }
      driveList.addItem(new TBDeviceInfo(DefaultTBFileSystem.open(root), label, tbLoaderConfig.getDeviceID()));
      if (prevSelected != null
          && root.getAbsolutePath().equals(prevSelected.getAbsolutePath())) {
        index = i;
      } else if (label.startsWith("TB") || label.substring(1, 2).equals("-"))
        index = i;
      i++;
    }
    if (driveList.getItemCount() == 0) {
      LOG.log(Level.INFO, "No drives");
      driveList.addItem(new TBDeviceInfo(null, NO_DRIVE, tbLoaderConfig.getDeviceID()));
      index = 0;
    }

    if (index == -1) {
      index = i - 1;
    }
    if (index != -1) {
      driveList.setSelectedIndex(index);
      currentDrive = new TBLoaderCore(tbLoaderConfig, (TBDeviceInfo) driveList.getSelectedItem());
    }
  }

  private synchronized File[] getRoots() {
    List<File> roots = new ArrayList<File>();
    // changing line below to allow TBLoader to run as a single .class file
    // (until new ACM version is running on Fidelis's laptop)
    if (OsUtils.WINDOWS) {
      for (File r : File.listRoots()) {
	      if (r.getAbsoluteFile().toString().compareTo("D:") >= 0
	          && r.listFiles() != null) {
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
    return roots.isEmpty() ? null : roots.toArray(new File[roots.size()]);
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
            if (!driveList.getItemAt(0).getLabel().equals(NO_DRIVE)) {
              status2.setText("");
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
          LOG.log(Level.WARNING, "Exception while refreshing list of connected devices.", e);
          throw new RuntimeException(e);
        }

      }
    }
  };

  private String getImageFromCommunity(String community) throws Exception {
    File imagedir = new File(
        CONTENT_SUBDIR + newDeploymentList.getSelectedItem().toString() + "/"
            + IMAGES_SUBDIR + "/");

    TBFileSystem sourceImage = DefaultTBFileSystem.open(imagedir);
    String imageName = TBLoaderCore.getImageFromCommunity(sourceImage, community);
    newImageText.setText(imageName);
    return imageName;
  }

  /**
   * Populates the values in the right-hand side, the "previous deployment" side of the main screen.
   *
   * @throws Exception
   */
  public void populatePreviousValuesFromCurrentDrive() throws IOException {
    try {
      currentDrive.loadTBStats();
    } catch (IOException e) {
      String driveLabel = currentDrive.getDevice().getLabelWithoutDriveLetter();
      if (currentDrive.isSerialNumberFormatGood(driveLabel)) {
        // could not find flashStats file -- but TB should save flashstats on normal shutdown and on *-startup.
        JOptionPane.showMessageDialog(null,
            "The TB's statistics cannot be found. Please follow these steps:\n 1. Unplug the TB\n 2. Hold down the * while turning on the TB\n "
                + "3. Observe the solid red light.\n 4. Now plug the TB into the laptop.\n 5. If you see this message again, please continue with the loading -- you tried your best.",
            "Cannot find the statistics!", JOptionPane.DEFAULT_OPTION);
      }
    }

    oldDeploymentInfo = currentDrive.loadDeploymentInfoFromDevice();
    if (oldDeploymentInfo != null) {
	    oldSrnText.setText(oldDeploymentInfo.getSerialNumber());
	    oldFirmwareRevisionText.setText(oldDeploymentInfo.getFirmwareRevision());
	    oldImageText.setText(oldDeploymentInfo.getPackageName());
	    oldDeploymentText.setText(oldDeploymentInfo.getDeploymentName());
	    lastUpdatedText.setText(oldDeploymentInfo.getLastUpdatedText());
	    newSrnText.setText(oldDeploymentInfo.getSerialNumber());
	    oldCommunityText.setText(oldDeploymentInfo.getCommunity());
        // If we want to do this...
        // oldProjectText.setTExt(oldDeploymentInfo.getProjectName());
    }
  }

  /**
   * Handles combo box selections for the Drive list, Community list, and Deployment List.
   *
   * @param e The combo selection event.
   */
  public void comboBoxActionPerformed(ActionEvent e) {
    TBDeviceInfo di;
    Object o = e.getSource();
    if (refreshingDriveInfo || !startUpDone)
      return;

    if (o == driveList) {
      oldSrnText.setText("");
      newSrnText.setText("");
      di = (TBDeviceInfo) ((JComboBox<String>) e.getSource()).getSelectedItem();
      currentDrive = new TBLoaderCore(tbLoaderConfig, di);
      if (di != null) {
        LOG.log(Level.INFO, "Drive changed: " + di.getFileSystem().toString() + di.getLabel());
        try {
          populatePreviousValuesFromCurrentDrive();
        } catch (Exception e1) {
          // TODO Auto-generated catch block
          JOptionPane.showMessageDialog(this, e1.toString(), "Error",
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
    return;
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
      throw new IllegalArgumentException(
          "'buttonActionPerformed' called for unknown button");
    }

    disableAll();
    try {
      LOG.log(Level.INFO, "ACTION: " + b.getText());

      di = currentDrive.getDevice();
      TBFileSystem drive = di.getFileSystem();
      if (drive == null) {
        refreshUI();
        return;
      }
      String devicePath = drive.getRootPath();
      prevSelected = new File(devicePath);

      if (oldCommunityText.getText().trim().length() == 0)
        oldCommunityText.setText("UNKNOWN");
      if (oldDeploymentText.getText().trim().length() == 0)
        oldDeploymentText.setText("UNKNOWN");

      String community = newCommunityList.getSelectedItem().toString();
      LOG.log(Level.INFO, "Community: " + community);

      if (isUpdate) {
        if (dateRotation == null
            || currentLocationList.getSelectedIndex() == 0) {
          JOptionPane.showMessageDialog(null,
              "You must first select a rotation date and a location.",
              "Need Date and Location!", JOptionPane.DEFAULT_OPTION);
          refreshUI();
          return;
        }

        if (community.equals(NO_COMMUNITY_SELECTED)) {
          int response = JOptionPane.showConfirmDialog(this,
              "No community selected.\nAre you sure?", "Confirm",
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
        if (di.serialNumber.equalsIgnoreCase(NEED_SERIAL_NUMBER)) {
          int intSrn = allocateNextSerialNumberFromDevice();
          String lowerSrn = String.format("%04x", intSrn);
          String srn = (tbLoaderConfig.getSrnPrefix() + tbLoaderConfig.getDeviceID()
              + lowerSrn).toUpperCase();
          di.setSerialNumber(srn);
          newSrnText.setText(srn);
        }
      }

      LOG.log(Level.INFO, "ID:" + di.getSerialNumber());
      status.setText("STATUS: Starting\n");

      CopyThread t = new CopyThread(devicePath, di.getSerialNumber(),
          operation);
      t.start();

      return;
    } catch (Exception ex) {
      LOG.log(Level.WARNING, ex.toString(), ex);
      JOptionPane.showMessageDialog(this, "An error occured.", "Error",
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

  private void onCopyFinished(boolean success, final String idString,
      final Operation operation, final String endMsg,
      final String endTitle) {
    SwingUtilities.invokeLater(() -> {
      updatingTB = false;
      resetUI(true);
      LOG.log(Level.INFO, endMsg);
      JOptionPane.showMessageDialog(null, endMsg, endTitle, JOptionPane.DEFAULT_OPTION);
    });
  }

  private synchronized boolean isDriveConnected() {
    boolean connected = false;
    TBFileSystem drive;

    if (driveList.getItemCount() > 0) {
      drive = ((TBDeviceInfo) driveList.getSelectedItem()).getFileSystem();
      if (drive != null)
        connected = true;
    }
    return connected;
  }

  /**
   * As nearly as I can tell, this is never actually used -- the trigger file
   * is never created.
   */
  private synchronized void refreshFileListForCollectedData() {
    String triggerFile = currentDrive.getCopyToFolder() + "/" + TRIGGER_FILE_CHECK;
    File f = new File(triggerFile);
    if (f.exists()) {
      status.setText("Updating list of files in collected-data");
      try {
        f.delete();
        CommandLineUtils.execute("cmd /C dir " + currentDrive.getCopyToFolder() + " /S > " + currentDrive.getCopyToFolder() + "/dir.txt");
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  private void refreshUI() {
    boolean connected;
    disableAll();
    refreshFileListForCollectedData();

    updateButton.setText("Update TB");
    connected = isDriveConnected();
    if (connected && !updatingTB) {
      updateButton.setEnabled(true);
      grabStatsOnlyButton.setEnabled(true);
      status.setText("STATUS: Ready");
      status2.setText(status2.getText() + "\n\n");
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
        LOG.log(Level.INFO, "STATUS: " + NO_DRIVE);
        status.setText("STATUS: " + NO_DRIVE);
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
    final String tbSrn;
    //final String datetime;
    boolean criticalError = false;
    boolean alert = false;

    public CopyThread(String devicePath, String tbSrn,
        Operation operation) {
      this.devicePath = devicePath;
      this.tbSrn = tbSrn;
      this.operation = operation;
    }

    private void grabStatsOnly(DeploymentInfo newDeploymentInfo) {
      TBLoaderCore.Result result = null;
      try {
        result = currentDrive.doUpdate(oldDeploymentInfo, newDeploymentInfo,
            currentLocationList.getSelectedItem().toString(), true, false, null,
            new ProgressListener() {
              @Override
              public void updateProgress(int progressPercent, String progressUpdate) {
                status2.setText(status2.getText() + "\n(" + progressPercent + "%): " + progressUpdate + "\n");
              }

              @Override
              public void addDetail(String detail) {
                status2.setText(status2.getText() + detail);
              }
            });
      } finally {
        String endMsg, endTitle;
        if (result.success) {
          endMsg = new String("Got Stats!");
          endTitle = new String("Success");
        } else {
          endMsg = new String("Could not get stats for some reason.");
          endTitle = new String("Failure");
        }
        onCopyFinished(result.success, tbSrn, this.operation, endMsg, endTitle);
      }
    }

    private void update(DeploymentInfo newDeploymentInfo) {
      String endMsg = "";
      String endTitle = "";

      File newDeploymentContentDir = new File(TBLoaderConstants.CONTENT_SUBDIR,
          newDeploymentInfo.getDeploymentName());

      TBFileSystem sourceImage = DefaultTBFileSystem.open(newDeploymentContentDir);

      TBLoaderCore.Result result = null;
      try {
        result = currentDrive.doUpdate(oldDeploymentInfo, newDeploymentInfo,
            currentLocationList.getSelectedItem().toString(), false, forceFirmware.isSelected(),
            sourceImage,
            new ProgressListener() {
              @Override
              public void updateProgress(int progressPercent, String progressUpdate) {
                status2.setText(status2.getText() + "\n(" + progressPercent + "%): " + progressUpdate + "\n");
              }
              @Override
              public void addDetail(String detail) {
                status2.setText(status2.getText() + detail);
              }
        });

        if (!result.success) {
          if (result.corrupted) {
            if (!OSChecker.WINDOWS) {
              LOG.log(Level.INFO,
                  "Reformatting memory card is not supported on this platform.\nTry using TBLoader for Windows.");
              JOptionPane.showMessageDialog(null,
                  "Reformatting memory card is not supported on this platform.\nTry using TBLoader for Windows.",
                  "Failure!", JOptionPane.ERROR_MESSAGE);
            }

            if (result.reformatFailed) {
              LOG.log(Level.INFO, "STATUS:Reformat Failed");
              LOG.log(Level.INFO,
                  "Could not reformat memory card.\nMake sure you have a good USB connection\nand that the Talking Book is powered with batteries, then try again.\n\nIf you still cannot reformat, replace the memory card.");
              JOptionPane.showMessageDialog(null,
                  "Could not reformat memory card.\nMake sure you have a good USB connection\nand that the Talking Book is powered with batteries, then try again.\n\nIf you still cannot reformat, replace the memory card.",
                  "Failure!", JOptionPane.ERROR_MESSAGE);
            }
          }
        }

        for (int i = 1; i <= (result.success ? 3 : 6); i++)
          Toolkit.getDefaultToolkit().beep();
      } catch (Exception e) {
        if (alert) {
          JOptionPane.showMessageDialog(null, e.getMessage(), "Error",
              JOptionPane.ERROR_MESSAGE);
          criticalError = true;
          LOG.log(Level.SEVERE, "CRITICAL ERROR:", e);
        } else
          LOG.log(Level.WARNING, "NON-CRITICAL ERROR:", e);
        endMsg = String.format("Exception updating TB-Loader: %s", e.getMessage());
        endTitle = "An Exception Occurred";
      } finally {
        if (result.verified) {
          endMsg = new String(
              "Talking Book has been updated and verified\nin " + result.duration + ".");
          endTitle = new String("Success");
        } else {
          endMsg = new String(
              "Update failed verification.  Try again or replace memory card.");
          endTitle = new String("Failure");
        }
        onCopyFinished(result.success, tbSrn, this.operation, endMsg,
            endTitle);
      }

    }

    @Override
    public void run() {
      DeploymentInfo newDeploymentInfo = new DeploymentInfo(newSrnText.getText(), newProject,
          newImageText.getText(), newDeploymentList.getSelectedItem().toString(),
          dateRotation, newFirmwareRevisionText.getText(), newCommunityList.getSelectedItem().toString());
      if (this.operation == Operation.Update) {
        update(newDeploymentInfo);
      } else if (this.operation == Operation.CollectStats) {
        grabStatsOnly(newDeploymentInfo);
      }
    }
  }

}
