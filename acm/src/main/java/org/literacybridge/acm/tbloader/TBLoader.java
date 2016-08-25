package org.literacybridge.acm.tbloader;

import static javax.swing.GroupLayout.Alignment.BASELINE;
import static javax.swing.GroupLayout.Alignment.LEADING;
import static org.literacybridge.core.tbloader.TBLoaderConstants.COLLECTED_DATA_DROPBOXDIR_PREFIX;
import static org.literacybridge.core.tbloader.TBLoaderConstants.COLLECTION_SUBDIR;
import static org.literacybridge.core.tbloader.TBLoaderConstants.COMMUNITIES_SUBDIR;
import static org.literacybridge.core.tbloader.TBLoaderConstants.CONTENT_BASIC_SUBDIR;
import static org.literacybridge.core.tbloader.TBLoaderConstants.CONTENT_SUBDIR;
import static org.literacybridge.core.tbloader.TBLoaderConstants.DEFAULT_GROUP_LABEL;
import static org.literacybridge.core.tbloader.TBLoaderConstants.DEVICE_FILE_EXTENSION;
import static org.literacybridge.core.tbloader.TBLoaderConstants.GROUP_FILE_EXTENSION;
import static org.literacybridge.core.tbloader.TBLoaderConstants.IMAGES_SUBDIR;
import static org.literacybridge.core.tbloader.TBLoaderConstants.NEED_SERIAL_NUMBER;
import static org.literacybridge.core.tbloader.TBLoaderConstants.NO_DRIVE;
import static org.literacybridge.core.tbloader.TBLoaderConstants.NO_SERIAL_NUMBER;
import static org.literacybridge.core.tbloader.TBLoaderConstants.SCRIPT_SUBDIR;
import static org.literacybridge.core.tbloader.TBLoaderConstants.STARTING_SERIALNUMBER;
import static org.literacybridge.core.tbloader.TBLoaderConstants.TEMP_COLLECTION_DIR;
import static org.literacybridge.core.tbloader.TBLoaderConstants.TRIGGER_FILE_CHECK;
import static org.literacybridge.core.tbloader.TBLoaderConstants.UNPUBLISHED_REV;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;

import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileSystemView;

import org.apache.commons.io.FileUtils;
import org.jdesktop.swingx.JXDatePicker;
import org.literacybridge.acm.Constants;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.utils.OSChecker;
import org.literacybridge.acm.utils.ZipUnzip;
import org.literacybridge.core.tbloader.TBInfo;

@SuppressWarnings("serial")
public class TBLoader extends JFrame {
  private static final Logger LOG = Logger.getLogger(TBLoader.class.getName());

  private static String imageRevision = "(no rev)";
  private static String dateRotation;
  private static JComboBox newDeploymentList;
  private static JTextField newCommunityFilter;
  private static FilteringComboBoxModel newCommunityModel;
  private static JComboBox newCommunityList;
  private static JComboBox currentLocationList;
  private static JComboBox driveList;
  private static JTextField oldSrnText;
  private static JTextField newSrnText;
  private static JTextField oldFirmwareRevisionText;
  private static JTextField newFirmwareRevisionText;
  private static JTextField oldImageText;
  private static JTextField newImageText;
  private static JTextField oldDeploymentText;
  private static JTextField oldCommunityText;
  private static JTextField lastUpdatedText;
  private static JLabel oldValue;
  private static JLabel newValue;
  private static JTextArea status;
  private static JTextArea status2;
  private static String homepath;
  private static JButton updateButton;
  private static JButton grabStatsOnlyButton;
  private static String dropboxCollectionFolder;
  private static String copyTo;
  private static String pathOperationalData;
  private static String revision;
  public static String deploymentName;
  public static String sourcePackage;
  public static int durationSeconds;
  public static DriveInfo currentDrive;
  private static String srnPrefix;
  static String project;
  static File tempCollectionFile;
  static String syncSubPath;
  private static JCheckBox forceFirmware;

  TBInfo tbStats;
  static String volumeSerialNumber = "";
  private static String deviceID; // this device is the computer/tablet/phone that is running the TB Loader

  class WindowEventHandler extends WindowAdapter {
    public void windowClosing(WindowEvent evt) {
      checkDirUpdate();
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

  public TBLoader(String project, String srnPrefix) {
    TBLoader.project = project;
    if (srnPrefix != null) {
      TBLoader.srnPrefix = srnPrefix;
    } else {
      TBLoader.srnPrefix = "b-"; // for latest Talking Book hardware
    }
  }

  private void runApplication() throws Exception {
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    this.addWindowListener(new WindowEventHandler());
    setDeviceIdAndPaths();

    // get image revision
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
    if (TBLoader.srnPrefix.equals("a-")) {
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
      public void actionPerformed(ActionEvent e) {
        dateRotation = datePicker.getDate().toString();
      }
    });

    newDeploymentList = new JComboBox();
    newDeploymentList.addActionListener(new ActionListener() {
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
      public void actionPerformed(ActionEvent e) {
        comboBoxActionPerformed(e);
      }
    });
    driveList = new JComboBox();
    driveList.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        comboBoxActionPerformed(e);
      }
    });
    currentLocationList = new JComboBox(currentLocation);
    forceFirmware = new JCheckBox();
    updateButton = new JButton("Update TB");
    updateButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        buttonActionPerformed(e);
      }
    });
    grabStatsOnlyButton = new JButton("Get Stats");
    grabStatsOnlyButton.addActionListener(new ActionListener() {
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

  public static boolean startUpDone = false;
  public static boolean refreshingDriveInfo = false;
  public static boolean updatingTB = false;
  private static String lastSynchDir;

  public static void main(String[] args) throws Exception {
    TBLoader tbloader = null;
    if (args.length == 1) {
      tbloader = new TBLoader(args[0], null);
    } else if (args.length == 2) {
      tbloader = new TBLoader(args[0], args[1]);
    }
    tbloader.runApplication();
  }

  public static String getDateTime() {
    SimpleDateFormat sdfDate = new SimpleDateFormat(
        "yyyy'y'MM'm'dd'd'HH'h'mm'm'ss's'");
    String dateTime = sdfDate.format(new Date());
    return dateTime;
  }

  private static String getLogFileName() {
    String filename;
    File f;

    filename = pathOperationalData + "/logs";
    f = new File(filename);
    if (!f.exists())
      f.mkdirs();
    filename += "/log-" + (TBLoader.currentDrive.datetime.equals("") ?
        getDateTime() :
        TBLoader.currentDrive.datetime) + ".txt";
    return filename;
  }

  private void setDeviceIdAndPaths() {
    String path;

    try {
      homepath = System.getProperty("user.home");
      BufferedReader reader;
      String LB_DIR = new String(homepath + "/LiteracyBridge");
      File f = new File(LB_DIR);
      f.mkdirs();

      File[] files = f.listFiles(new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
          String lowercase = name.toLowerCase();
          return lowercase.endsWith(DEVICE_FILE_EXTENSION);
        }
      });
      if (files.length == 1) {
        TBLoader.deviceID = files[0].getName()
            .substring(0, files[0].getName().length() - 4)
            .toUpperCase();
      } else {
        JOptionPane.showMessageDialog(null,
            "This computer does not appear to be configured to use the TB Loader yet.  It needs a unique device tbSrn. Please contact ICT staff to get this.",
            "This Computer has no ID!", JOptionPane.DEFAULT_OPTION);
        System.exit(ERROR);
      }
      // This is used to store collected data until finished with one TB, when it is zipped up and moved to the collectedDataFile setup below
      TEMP_COLLECTION_DIR = LB_DIR + COLLECTION_SUBDIR + "/" + TBLoader.project;
      f = new File(TEMP_COLLECTION_DIR);
      f.mkdirs();
      TBLoader.tempCollectionFile = f;

      File dropboxDir = ACMConfiguration.getInstance().getGlobalShareDir();
      if (!dropboxDir.exists()) {
        JOptionPane.showMessageDialog(null, dropboxDir.getAbsolutePath()
                + " does not exist; cannot find the Dropbox path. Please contact ICT staff.",
            "Cannot Find Dropbox!", JOptionPane.DEFAULT_OPTION);
        System.exit(ERROR);
      }
      // Like "/Users/mike/Dropbox (Literacy Bridge)/tbcd1234"
      File collectedDataFile = new File(dropboxDir,
          COLLECTED_DATA_DROPBOXDIR_PREFIX + TBLoader.deviceID);
      if (!collectedDataFile.exists()) {
        JOptionPane.showMessageDialog(null, collectedDataFile.getAbsolutePath()
                + " does not exist; cannot find the Dropbox collected data path. Please contact ICT staff.",
            "Cannot Find Dropbox Collected Data Folder!",
            JOptionPane.DEFAULT_OPTION);
        System.exit(ERROR);
      }
      copyTo = collectedDataFile.getAbsolutePath();
    } catch (Exception e) {
      LOG.log(Level.WARNING, "Exception while setting DeviceId and paths", e);
    }
    // Like "/Users/mike/Dropbox (Literacy Bridge)/tbcd1234/collected-data"
    copyTo += COLLECTION_SUBDIR;
    dropboxCollectionFolder = copyTo;
    // Like "/Users/mike/Dropbox (Literacy Bridge)/tbcd1234/collected-data/XYZ"
    copyTo += "/" + TBLoader.project;
    new File(
        copyTo).mkdirs();  // creates COLLECTION_SUBDIR if good path is found
    // Like "/Users/mike/Dropbox (Literacy Bridge)/tbcd1234/collected-data/XYZ/OperationalData/1234"
    pathOperationalData = copyTo + "/OperationalData/" + TBLoader.deviceID;
    LOG.log(Level.INFO, "copyTo:" + copyTo);
  }

  int idCounter = 0;

  private File prevSelected = null;
  private int prevSelectedCommunity = -1;

  private void getStatsFromCurrentDrive() throws IOException {
    DriveInfo di = TBLoader.currentDrive;
    if (di.drive == null)
      return;
    File rootPath = new File(di.drive.getAbsolutePath());
    File statsPath = new File(rootPath, "statistics/stats/flashData.bin");
    if (!statsPath.exists()) {
      statsPath = new File(rootPath, "statistics/flashData.bin");
    }
    tbStats = new TBInfo(statsPath.toString());
    if (tbStats.getCountReflashes() == -1)
      tbStats = null;
    if (!statsPath.exists())
      throw new IOException();
  }

  private String getCommunityFromCurrentDrive() {
    String communityName = "UNKNOWN";
    DriveInfo di = TBLoader.currentDrive;
    if (di.drive == null) {
      return communityName;
    }
    File rootPath = new File(di.drive.getAbsolutePath());
    File systemPath = new File(di.drive.getAbsolutePath(), "system");
    try {
      File[] files;
      // get Location file info
      // check root first, in case device was just assigned a new community (e.g. from this app)
      files = rootPath.listFiles(new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
          String lowercase = name.toLowerCase();
          return lowercase.endsWith(".loc");
        }
      });
      if (files == null) {
        LOG.log(Level.INFO, "This does not look like a TB: " + rootPath);

      } else if (files.length == 1) {
        String locFileName = files[0].getName();
        communityName = locFileName.substring(0, locFileName.length() - 4);
      } else if (files.length == 0 && systemPath.exists()) {
        // get Location file info
        files = systemPath.listFiles(new FilenameFilter() {
          @Override
          public boolean accept(File dir, String name) {
            String lowercase = name.toLowerCase();
            return lowercase.endsWith(".loc");
          }
        });
        if (files.length == 1) {
          String locFileName = files[0].getName();
          communityName = locFileName.substring(0, locFileName.length() - 4);
        }
      }
    } catch (Exception ignore) {
      LOG.log(Level.WARNING, "Exception while reading community", ignore);
      // ignore and keep going with empty string
    }
    LOG.log(Level.INFO, "TB's current community name is " + communityName);
    return communityName;
  }

  private synchronized void fillDeploymentList() {

    int indexSelected = -1;
    File contentPath = new File(CONTENT_SUBDIR);
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
    newCommunityList.addItem(
        "Non-specific");  // This string must match firmware code to generate special dings on startup
    // as reminder that specific name has not been set.
    for (File f : files) {
      newCommunityList.addItem(f.getName());
    }
    setCommunityList();
  }

  private synchronized void setCommunityList() throws IOException {
    String driveCommunity;
    String driveLabel;
    try {
      getStatsFromCurrentDrive();
    } catch (IOException e) {
      driveLabel = TBLoader.currentDrive.getLabelWithoutDriveLetter();
      if (isSerialNumberFormatGood(driveLabel)) {
        // could not find flashStats file -- but TB should save flashstats on normal shutdown and on *-startup.
        JOptionPane.showMessageDialog(null,
            "The TB's statistics cannot be found. Please follow these steps:\n 1. Unplug the TB\n 2. Hold down the * while turning on the TB\n "
                + "3. Observe the solid red light.\n 4. Now plug the TB into the laptop.\n 5. If you see this message again, please continue with the loading -- you tried your best.",
            "Cannot find the statistics!", JOptionPane.DEFAULT_OPTION);
      }
    }
    driveCommunity = getCommunityFromCurrentDrive();
    if (tbStats != null && tbStats.getLocation() != null && !tbStats.getLocation().equals(
        ""))
      oldCommunityText.setText(tbStats.getLocation());
    else
      oldCommunityText.setText(driveCommunity);

    if (prevSelectedCommunity != -1)
      newCommunityList.setSelectedIndex(prevSelectedCommunity);
    else {
      int count = newCommunityList.getItemCount();
      for (int i = 0; i < count; i++) {
        if (newCommunityList.getItemAt(i)
            .toString()
            .equalsIgnoreCase(driveCommunity)) {
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
        TBLoader.deviceID + DEVICE_FILE_EXTENSION; // xxxx.dev
    File f = new File(
        homepath + File.separator + "LiteracyBridge" + File.separator
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
    FileUtils.copyFile(f, new File(dropboxCollectionFolder, devFilename));

    return serialnumber;
  }

  /**
   * Looks in the TalkingBook's system directory for any files with a ".srn" extension who's name does not begin
   * with "-erase". If any such files are found, returns the name of the first one found (ie, one selected at random),
   * without the extension.
   *
   * @param systemPath The TalkingBook's system directory.
   * @return The file's name found (minus extension), or null if no file found.
   */
  private String getSerialNumberFromSystem(File systemPath) {
    String sn = NO_SERIAL_NUMBER;
    File[] files;

    if (tbStats != null && isSerialNumberFormatGood(tbStats.getSerialNumber())
        && isSerialNumberFormatGood2(tbStats.getSerialNumber()))
      sn = tbStats.getSerialNumber();
    else if (systemPath.exists()) {
      files = systemPath.listFiles(new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
          String lowercase = name.toLowerCase();
          return lowercase.endsWith(".srn") && !lowercase.startsWith("-erase");
        }
      });
      if (files.length > 0) {
        String tsnFileName = files[0].getName();
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

    if (!isSerialNumberFormatGood(sn)) {
      if (sn.substring(1, 2).equals("-")) {
        // TODO: This code probably only works in "the TB Loader for new TBs", because "a-0".compareTo("a-") is NOT < 0, but .compareTo("b-") is.
        if (sn.compareToIgnoreCase(TBLoader.srnPrefix) < 0)
          JOptionPane.showMessageDialog(null,
              "This appears to be an OLD TB.  If so, please close this program and open the TB Loader for old TBs.",
              "OLD TB!", JOptionPane.WARNING_MESSAGE);
        else if (sn.compareToIgnoreCase(TBLoader.srnPrefix) > 0)
          JOptionPane.showMessageDialog(null,
              "This appears to be a NEW TB.  If so, please close this program and open the TB Loader for new TBs.",
              "NEW TB!", JOptionPane.WARNING_MESSAGE);
      }
    }

    return sn;
  }

  /**
   * Looks in the TalkingBook's system directory for any files with a ".rev" or ".img" extension. If any such files
   * are found, returns the name of the first one found (ie, one selected at random), without the extension.
   *
   * @param systemPath The TalkingBook's system directory.
   * @return The file's name found (minus extension), or "UNKNOWN" if no file found, or if the file name consists
   * only of the extension (eg, a file named ".img" will return "UNKNOWN").
   */
  private String getFirmwareRevisionFromSystem(File systemPath) {
    String rev = "UNKNOWN";
    File[] files;

    if (systemPath.exists()) {
      // get firmware revision number from .rev or .img file
      files = systemPath.listFiles(new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
          String lowercase = name.toLowerCase();
          return lowercase.endsWith(".img") || lowercase.endsWith(".rev");
        }
      });

      for (int i = 0; i < files.length; i++) {
        String revFileName = files[i].getName();
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
   * @param systemPath The TalkingBook's system directory.
   * @return The file's name found (minus extension), or "UNKNOWN" if no file found.
   */
  private String getPkgNameFromSystem(File systemPath) {
    String pkg = "UNKNOWN";
    File[] files;

    if (tbStats != null && tbStats.getImageName() != null
        && !tbStats.getImageName().equals(""))
      pkg = tbStats.getImageName();
    else if (systemPath.exists()) {
      // get package name from .pkg file
      files = systemPath.listFiles(new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
          String lowercase = name.toLowerCase();
          return lowercase.endsWith(".pkg");
        }
      });
      if (files.length == 1) {
        pkg = files[0].getName().substring(0, files[0].getName().length() - 4);
      }
    }

    return pkg;
  }

  /**
   * Look in the tbstats structure for a deployment name (called "deploymentNumber", but really a string). If none,
   * look in the TalkingBook's system directory for a file with a ".dep" extension. If there is exactly
   * one such file, returns the file's name, sans extension.
   *
   * @param systemPath The TalkingBook's system directory.
   * @return The file's name found (minus extension), or "UNKNOWN" if no file found.
   */
  private String getDeploymentNameFromSystem(File systemPath) {
    String depl = "UNKNOWN";
    File[] files;

    if (tbStats != null && tbStats.getDeploymentNumber() != null
        && !tbStats.getDeploymentNumber().equals(""))
      depl = tbStats.getDeploymentNumber();
    else if (systemPath.exists()) {
      // get deployment name from .dep file
      files = systemPath.listFiles(new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
          String lowercase = name.toLowerCase();
          return lowercase.endsWith(".dep");
        }
      });
      if (files.length == 1) {
        depl = files[0].getName().substring(0, files[0].getName().length() - 4);
      }
    }

    return depl;
  }

  /**
   * Look in the tbstats structure for updateDate. If none, parse the lastSynchDir, if there is one.
   *
   * @return The last sync date, or "UNKNOWN" if not available.
   */
  private String getLastUpdateDate() {
    String lastUpdate = "UNKNOWN";

    if (tbStats != null && tbStats.getUpdateDate() != -1)
      lastUpdate = tbStats.getUpdateYear() + "/" + tbStats.getUpdateMonth() + "/"
          + tbStats.getUpdateDate();
    else {
      String strLine = TBLoader.lastSynchDir; // 1111y11m11d
      if (strLine != null) {
        int y = strLine.indexOf('y');
        int m = strLine.indexOf('m');
        int d = strLine.indexOf('d');
        lastUpdate =
            strLine.substring(0, y) + "/" + strLine.substring(y + 1, m) + "/"
                + strLine.substring(m + 1, d);
      }
    }

    return lastUpdate;
  }

  /**
   * Looks in the TalkingBook's system directory for a file named "last_updated.txt",
   * and reads the first line from it. Stores any value so read into TBLoader.lastSynchDir.
   *
   * @param systemPath The TalkingBook's system directory.
   */
  private void readLastSynchDirFromSystem(File systemPath) {
    File[] files;
    files = systemPath.listFiles(new FilenameFilter() {
      @Override
      public boolean accept(File dir, String name) {
        String lowercase = name.toLowerCase();
        return lowercase.equals("last_updated.txt");
      }
    });
    if (files.length == 1) {
      try (FileInputStream fstream = new FileInputStream(files[0]);
          DataInputStream in = new DataInputStream(fstream);
          BufferedReader br = new BufferedReader(new InputStreamReader(in))
      ) {
        String strLine;
        if ((strLine = br.readLine()) != null) {
          TBLoader.lastSynchDir = strLine;
        }
        in.close();
      } catch (Exception e) { //Catch and ignore exception if any
        System.err.println("Ignoring error: " + e.getMessage());
      }
    }
  }

  /**
   * Populates the values in the right-hand side, the "previous deployment" side of the main screen.
   *
   * @throws Exception
   */
  private synchronized void populatePreviousValuesFromCurrentDrive()
      throws Exception {
    String sn = NO_SERIAL_NUMBER; // "UNKNOWN"
    DriveInfo di = TBLoader.currentDrive;
    if (di == null || di.drive == null)
      return;
    File systemPath = new File(di.drive.getAbsolutePath(), "system");
    try {
      sn = getSerialNumberFromSystem(systemPath);
      oldSrnText.setText(sn);

      String rev = getFirmwareRevisionFromSystem(systemPath);
      oldFirmwareRevisionText.setText(rev);

      // Previous image or package. Displays as "Content"
      // TODO: package vs image - consistency in nomenclature.
      String pkg = getPkgNameFromSystem(systemPath);
      oldImageText.setText(pkg);

      // Previous deployment name. Displays as "Update"
      String depl = getDeploymentNameFromSystem(systemPath);
      oldDeploymentText.setText(depl);

      // Last updated date. Displays as "First Rotation Date".
      readLastSynchDirFromSystem(systemPath);
      String lastUpdate = getLastUpdateDate();
      lastUpdatedText.setText(lastUpdate);

    } catch (Exception ignore) {
      LOG.log(Level.WARNING, "exception - ignore and keep going with empty strings", ignore);
    }
    sn = sn.toUpperCase();
    if (!isSerialNumberFormatGood2(sn)) {
      // We will allocate a new-style serial number before we update the device.
      sn = NEED_SERIAL_NUMBER;
    }
    di.serialNumber = sn;
    newSrnText.setText(sn);
  }

  private synchronized void fillDriveList(File[] roots) {
    driveList.removeAllItems();
    TBLoader.currentDrive = null;
    int index = -1;
    int i = 0;
    for (File root : roots) {
      if (root.getAbsoluteFile().toString().compareTo("D:") >= 0
          && root.listFiles() != null) {
        String label = FileSystemView.getFileSystemView()
            .getSystemDisplayName(root);
        if (label.trim().equals("CD Drive") || label.startsWith("DVD"))
          continue;
        // Ignore drives shared by Parallels. Value determined empirically.
        if (OSChecker.WINDOWS && label.indexOf(" on 'Mac' (") >= 0) {
          continue;
        }
        driveList.addItem(new DriveInfo(root, label));
        if (prevSelected != null && root.getAbsolutePath()
            .equals(prevSelected.getAbsolutePath())) {
          index = i;
        } else if (label.startsWith("TB") || label.substring(1, 2).equals("-"))
          index = i;
        i++;
      }
    }
    if (driveList.getItemCount() == 0) {
      LOG.log(Level.INFO, "No drives");
      driveList.addItem(new DriveInfo(null, NO_DRIVE));
      index = 0;
    }

    if (index == -1) {
      index = i - 1;
    }
    if (index != -1) {
      driveList.setSelectedIndex(index);
      TBLoader.currentDrive = (DriveInfo) driveList.getSelectedItem();
    }
  }

  private synchronized File[] getRoots() {
    File[] roots = null;
    // changing line below to allow TBLoader to run as a single .class file
    // (until new ACM version is running on Fidelis's laptop)
    if (System.getProperty("os.name")
        .startsWith("Windows")) { // (OSChecker.WINDOWS) {
      roots = File.listRoots();
    } else if (System.getProperty("os.name")
        .startsWith("Mac OS")) { //(OSChecker.MAC_OS) {
      roots = new File("/Volumes").listFiles();
    }
    return roots;
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
            if (!((DriveInfo) driveList.getItemAt(0)).label.equals(NO_DRIVE)) {
              status2.setText("");
            }
            try {
              fillCommunityList();
            } catch (IOException e) {
              // TODO Auto-generated catch block
              e.printStackTrace();
            }
            try {
              populatePreviousValuesFromCurrentDrive();
            } catch (Exception e) {
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

  private void logTBData(String action) {
    final String VERSION_TBDATA = "v03";
    BufferedWriter bw;
    // Like "/Users/mike/Dropbox (Literacy Bridge)/tbcd1234/collected-data/XYZ/OperationalData/1234/tbData"
    String tbDataPath = pathOperationalData + "/tbData";
    File f = new File(tbDataPath);
    if (!f.exists())
      f.mkdirs();
    SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy'y'MM'm'dd'd'");
    String strDate = sdfDate.format(new Date());
    // Like "/Users/mike/Dropbox (Literacy Bridge)/tbcd1234/collected-data/XYZ/OperationalData/1234/tbData/tbData-v03-2020y12m25d-1234.csv"
    String filename =
        tbDataPath + "/tbData-" + VERSION_TBDATA + "-" + strDate + "-"
            + TBLoader.deviceID + ".csv";
    try {
      DriveInfo di = TBLoader.currentDrive;
      boolean isNewFile;
      f = new File(filename);
      isNewFile = !f.exists();
      bw = new BufferedWriter(new FileWriter(filename, true));
      if (bw != null) {
        if (isNewFile) {
          bw.write(
              "PROJECT,UPDATE_DATE_TIME,OUT_SYNCH_DIR,LOCATION,ACTION,DURATION_SEC,");
          bw.write(
              "OUT-SN,OUT-DEPLOYMENT,OUT-IMAGE,OUT-FW-REV,OUT-COMMUNITY,OUT-ROTATION-DATE,");
          bw.write(
              "IN-SN,IN-DEPLOYMENT,IN-IMAGE,IN-FW-REV,IN-COMMUNITY,IN-LAST-UPDATED,IN-SYNCH-DIR,IN-DISK-LABEL,CHKDSK CORRUPTION?,");
          bw.write("FLASH-SN,FLASH-REFLASHES,");
          bw.write(
              "FLASH-DEPLOYMENT,FLASH-IMAGE,FLASH-COMMUNITY,FLASH-LAST-UPDATED,FLASH-CUM-DAYS,FLASH-CORRUPTION-DAY,FLASH-VOLT,FLASH-POWERUPS,FLASH-PERIODS,FLASH-ROTATIONS,");
          bw.write(
              "FLASH-MSGS,FLASH-MINUTES,FLASH-STARTS,FLASH-PARTIAL,FLASH-HALF,FLASH-MOST,FLASH-ALL,FLASH-APPLIED,FLASH-USELESS");
          for (int i = 0; i < 5; i++) {
            bw.write(
                ",FLASH-ROTATION,FLASH-MINUTES-R" + i + ",FLASH-PERIOD-R" + i
                    + ",FLASH-HRS-POST-UPDATE-R" + i + ",FLASH-VOLT-R" + i);
          }
          bw.write("\n");
        }
        bw.write(TBLoader.project.toUpperCase() + ",");
        bw.write(TBLoader.currentDrive.datetime.toUpperCase() + ",");
        bw.write(TBLoader.currentDrive.datetime.toUpperCase() + "-"
            + TBLoader.deviceID.toUpperCase() + ",");
        bw.write(currentLocationList.getSelectedItem().toString().toUpperCase()
            + ",");
        bw.write(action + ",");
        bw.write(Integer.toString(TBLoader.durationSeconds) + ",");
        bw.write(di.serialNumber.toUpperCase() + ",");
        bw.write(
            newDeploymentList.getSelectedItem().toString().toUpperCase() + ",");
        bw.write(newImageText.getText().toUpperCase() + ",");
        bw.write(newFirmwareRevisionText.getText() + ",");
        bw.write(
            newCommunityList.getSelectedItem().toString().toUpperCase() + ",");
        bw.write(dateRotation + ",");
        bw.write(oldSrnText.getText().toUpperCase() + ",");
        bw.write(oldDeploymentText.getText().toUpperCase() + ",");
        bw.write(oldImageText.getText().toUpperCase() + ",");
        bw.write(oldFirmwareRevisionText.getText() + ",");
        bw.write(oldCommunityText.getText().toUpperCase() + ",");
        bw.write(lastUpdatedText.getText() + ",");
        bw.write(TBLoader.lastSynchDir.toUpperCase() + ",");
        bw.write(TBLoader.currentDrive.label + ",");
        bw.write(di.corrupted + ",");
        if (tbStats != null) {
          bw.write(tbStats.getSerialNumber().toUpperCase() + ",");
          bw.write(tbStats.getCountReflashes() + ",");
          bw.write(tbStats.getDeploymentNumber().toUpperCase() + ",");
          bw.write(tbStats.getImageName().toUpperCase() + ",");
          bw.write(tbStats.getLocation().toUpperCase() + ",");
          bw.write(tbStats.getUpdateYear() + "/" + tbStats.getUpdateMonth() + "/"
              + tbStats.getUpdateDate() + ",");
          bw.write(tbStats.getCumulativeDays() + ",");
          bw.write(tbStats.getCorruptionDay() + ",");
          bw.write(tbStats.getLastInitVoltage() + ",");
          bw.write(tbStats.getPowerups() + ",");
          bw.write(tbStats.getPeriods() + ",");
          bw.write(tbStats.getProfileTotalRotations() + ",");
          bw.write(tbStats.getTotalMessages() + ",");
          int totalSecondsPlayed = 0, countStarted = 0, countQuarter = 0, countHalf = 0, countThreequarters = 0, countCompleted = 0, countApplied = 0, countUseless = 0;
          for (int m = 0; m < tbStats.getTotalMessages(); m++) {
            for (int r = 0; r < (tbStats.getProfileTotalRotations() < 5 ?
                tbStats.getProfileTotalRotations() :
                5); r++) {
              totalSecondsPlayed += tbStats.getStats()[m][r].getTotalSecondsPlayed();
              countStarted += tbStats.getStats()[m][r].getCountStarted();
              countQuarter += tbStats.getStats()[m][r].getCountQuarter();
              countHalf += tbStats.getStats()[m][r].getCountHalf();
              countThreequarters += tbStats.getStats()[m][r].getCountThreequarters();
              countCompleted += tbStats.getStats()[m][r].getCountCompleted();
              countApplied += tbStats.getStats()[m][r].getCountApplied();
              countUseless += tbStats.getStats()[m][r].getCountUseless();
            }
          }
          bw.write(totalSecondsPlayed / 60 + ",");
          bw.write(countStarted + ",");
          bw.write(countQuarter + ",");
          bw.write(countHalf + ",");
          bw.write(countThreequarters + ",");
          bw.write(countCompleted + ",");
          bw.write(countApplied + ",");
          bw.write(String.valueOf(countUseless));
          for (int r = 0; r < (tbStats.getProfileTotalRotations() < 5 ?
              tbStats.getProfileTotalRotations() :
              5); r++) {
            bw.write(
                "," + r + "," + tbStats.totalPlayedSecondsPerRotation(r) / 60
                    + "," + tbStats.getRotations()[r].getStartingPeriod() + ",");
            bw.write(tbStats.getRotations()[r].getHoursAfterLastUpdate() + ","
                + tbStats.getRotations()[r].getInitVoltage());
          }
        }
        bw.write("\n");
        bw.flush();
        bw.close();
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private String getImageFromCommunity(String community) throws Exception {
    if (community == null)
      return null;
    String imageName = "";
    String groupName = "";
    File[] images;
    File imagedir = new File(
        CONTENT_SUBDIR + newDeploymentList.getSelectedItem().toString() + "/"
            + IMAGES_SUBDIR + "/");
    images = imagedir.listFiles(new FilenameFilter() {
      @Override
      public boolean accept(File dir, String name) {
        return dir.isDirectory();
      }
    });
    if (images != null && images.length == 1) {
      // grab first image package
      imageName = images[0].getName();
    } else if (images != null) {
      File fCommunityDir = new File(
          CONTENT_SUBDIR + newDeploymentList.getSelectedItem().toString() + "/"
              + COMMUNITIES_SUBDIR + "/" + community + "/" + "system");

      if (fCommunityDir.exists()) {
        // get groups
        File[] groups = fCommunityDir.listFiles(new FilenameFilter() {
          @Override
          public boolean accept(File dir, String name) {
            String lowercase = name.toLowerCase();
            return lowercase.endsWith(GROUP_FILE_EXTENSION);
          }
        });
        for (File group : groups) {
          // for every group that the community belongs to look for a match in each of images's group listing
          groupName = group.getName();
          groupName = groupName.substring(0, groupName.length() - 4);
          for (File image : images) {
            File imageSysFolder = new File(image, "system/");
            File[] imageGroups = imageSysFolder.listFiles(new FilenameFilter() {
              @Override
              public boolean accept(File dir, String name) {
                String lowercase = name.toLowerCase();
                return lowercase.endsWith(GROUP_FILE_EXTENSION);
              }
            });
            for (File imageGroup : imageGroups) {
              String imageGroupName = imageGroup.getName();
              imageGroupName = imageGroupName.substring(0,
                  imageGroupName.length() - 4);
              if (imageGroupName.equalsIgnoreCase(groupName)) {
                imageName = image.getName();
                break;
              }
            }
            if (!imageName.equals(""))
              break;
          }
        }
      }
      if (imageName.equals("")) {
        // no match of groups between community and multiple packages
        // Only hope is to find a default package
        for (File image : images) {
          File imageSysFolder = new File(image, "system/");
          File[] imageDefaultGroup = imageSysFolder.listFiles(
              new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                  String lowercase = name.toLowerCase();
                  return lowercase.endsWith(
                      DEFAULT_GROUP_LABEL + GROUP_FILE_EXTENSION);
                }
              });
          if (imageDefaultGroup.length == 1) {
            imageName = image.getName();
          }
        }
      }
    }
    if (imageName.equals("")) {
      imageName = "ERROR!  MISSING CONTENT IMAGE!";
    }
    newImageText.setText(imageName);
    return imageName;
  }

  /**
   * Tests whether a string is a valid "serial number" for this TB-Loader (sensitive to whether the TB-Loader
   * is being run in "old TB mode" or "new TB mode").
   *
   * @param srn - the string to check
   * @return TRUE if the string could be a serial number, FALSE if not.
   */
  public boolean isSerialNumberFormatGood(String srn) {
    boolean isGood;
    if (srn == null)
      isGood = false;
    else if (srn.toLowerCase().startsWith(TBLoader.srnPrefix.toLowerCase())
        && srn.length() == 10)
      isGood = true;
    else {
      isGood = false;
      LOG.log(Level.INFO, "***Incorrect Serial Number Format:" + srn + "***");
    }
    return isGood;
  }

  /**
   * Tests whether a string is a valid new-style "serial number".
   *
   * @param srn
   * @return
   */
  public boolean isSerialNumberFormatGood2(String srn) {
    boolean isGood = false;
    try {
      if (srn != null && srn.length() == 10 && srn.substring(1, 2).equals("-")
          && (srn.substring(0, 1).equalsIgnoreCase("A") || srn.substring(0, 1)
          .equalsIgnoreCase("B"))) {
        int highBytes = Integer.parseInt(srn.substring(2, 6), 0x10);
        // TODO: What was this trying to do? It appears to try to guess that if the "device number" part of a srn is
        // in the range 0-N, then the SRN must be > X, where N was "0x10" and X was "0x200" (it was the case that if
        // a number was assigned, the device number would have been smaller than 0x10 for most devices, and the
        // serial numbers were arbitrarily started at 0x200 for each device). I (Bill) *think* that this just
        // missed getting updated as the device number(s) increased; an ordinary bug. Does show the value of
        // burned-in serial numbers.
        //
        // The number below needs to be greater than the highest assigned "device" (TB Laptop). As of 23-May-16,
        // that highest assigned device number is 0x14. Opening up the range eases maintenance, but lets more
        // corrupted srns get through. 0x2f is somewhere in the middle.
        if (highBytes < 0x2f) {
          int lowBytes = Integer.parseInt(srn.substring(6), 0x10);
          if (lowBytes >= STARTING_SERIALNUMBER) {
            isGood = true;
          }
        }
      }
    } catch (NumberFormatException e) {
      isGood = false;
    }
    return isGood;
  }

  /**
   * Handles combo box selections for the Drive list, Community list, and Deployment List.
   *
   * @param e The combo selection event.
   */
  public void comboBoxActionPerformed(ActionEvent e) {
    DriveInfo di;
    Object o = e.getSource();
    if (refreshingDriveInfo || !startUpDone)
      return;

    if (o == driveList) {
      oldSrnText.setText("");
      newSrnText.setText("");
      di = (DriveInfo) ((JComboBox) e.getSource()).getSelectedItem();
      TBLoader.currentDrive = di;
      if (di != null) {
        LOG.log(Level.INFO, "Drive changed: " + di.drive + di.label);
        try {
          fillCommunityList();
        } catch (IOException e1) {
          // TODO Auto-generated catch block
          e1.printStackTrace();
        }
        try {
          populatePreviousValuesFromCurrentDrive();
        } catch (Exception e1) {
          // TODO Auto-generated catch block
          JOptionPane.showMessageDialog(this, e1.toString(), "Error",
              JOptionPane.ERROR_MESSAGE);
          e1.printStackTrace();
        }
        getFirmwareRevisionNumbers();
      }
    } else if (o == newCommunityList) {
      JComboBox cl = (JComboBox) o;
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
    DriveInfo di;
    CopyThread.Operation operation;
    boolean isUpdate = false;
    JButton b = (JButton) e.getSource();

    if (refreshingDriveInfo || !startUpDone)
      return;

    if (b == updateButton) {
      isUpdate = true;
      operation = CopyThread.Operation.Update;
    } else if (b == grabStatsOnlyButton) {
      operation = CopyThread.Operation.CollectStats;
    } else {
      throw new IllegalArgumentException(
          "'buttonActionPerformed' called for unknown button");
    }

    disableAll();
    try {
      LOG.log(Level.INFO, "ACTION: " + b.getText());

      di = TBLoader.currentDrive;
      File drive = di.drive;
      if (drive == null) {
        refreshUI();
        return;
      }
      String devicePath = drive.getAbsolutePath();
      prevSelected = drive;

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
          return;
        }

        if (newCommunityList.getSelectedIndex() == 0) {
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
        if (di.serialNumber == NEED_SERIAL_NUMBER) {
          int intSrn = allocateNextSerialNumberFromDevice();
          String lowerSrn = String.format("%04x", intSrn);
          String srn = (TBLoader.srnPrefix + TBLoader.deviceID
              + lowerSrn).toUpperCase();
          di.serialNumber = srn;
          newSrnText.setText(srn);
        }
      }

      LOG.log(Level.INFO, "ID:" + di.serialNumber);
      status.setText("STATUS: Starting\n");

      CopyThread t = new CopyThread(this, devicePath, di.serialNumber,
          operation);
      t.start();

      refreshUI();
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

  void onCopyFinished(boolean success, final String idString,
      final CopyThread.Operation operation, final String endMsg,
      final String endTitle) {
    updatingTB = false;
    resetUI(true);
    LOG.log(Level.INFO, endMsg);
    JOptionPane.showMessageDialog(null, endMsg, endTitle,
        JOptionPane.DEFAULT_OPTION);
  }

  private synchronized boolean isDriveConnected() {
    boolean connected = false;
    File drive;

    if (driveList.getItemCount() > 0) {
      drive = ((DriveInfo) driveList.getSelectedItem()).drive;
      if (drive != null)
        connected = true;
    }
    return connected;
  }

  private synchronized void checkDirUpdate() {
    String triggerFile = copyTo + "/" + TRIGGER_FILE_CHECK;
    File f = new File(triggerFile);
    if (f.exists()) {
      status.setText("Updating list of files to send");
      try {
        f.delete();
        execute("cmd /C dir " + copyTo + " /S > " + copyTo + "/dir.txt");
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  private void refreshUI() {
    boolean connected;
    disableAll();
    checkDirUpdate();

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
  }

  // TODO: Move this to its own file.
  public static class CopyThread extends Thread {
    public enum Operation {Update, CollectStats}

    final Operation operation;
    final String devicePath;
    final String tbSrn;
    //final String datetime;
    final TBLoader callback;
    boolean criticalError = false;
    boolean alert = false;
    boolean success = false;
    long startTime;

    public CopyThread(TBLoader callback, String devicePath, String tbSrn,
        Operation operation) {
      this.callback = callback;
      this.devicePath = devicePath;
      this.tbSrn = tbSrn;
      this.operation = operation;
    }

    private void setStartTime() {
      startTime = System.nanoTime();
    }

    private String getDuration() {
      String elapsedTime;
      double durationSeconds;
      int durationMinutes;
      long durationNanoseconds = System.nanoTime() - startTime;
      durationSeconds = (double) durationNanoseconds / 1000000000.0;
      TBLoader.durationSeconds = (int) durationSeconds;
      if (durationSeconds > 60) {
        durationMinutes = (int) durationSeconds / 60;
        durationSeconds -= durationMinutes * 60;
        elapsedTime = new String(
            Integer.toString(durationMinutes) + " minutes " + Integer.toString(
                (int) durationSeconds) + " seconds");
      } else
        elapsedTime = new String(
            Integer.toString((int) durationSeconds) + " seconds");
      return elapsedTime;
    }

    private boolean executeFile(File file) {
      boolean success = true;
      String errorLine = "";
      criticalError = false;
      Calendar cal = Calendar.getInstance();
      String month = String.valueOf(cal.get(Calendar.MONTH) + 1);
      String dateInMonth = String.valueOf(cal.get(Calendar.DAY_OF_MONTH));
      String year = String.valueOf(cal.get(Calendar.YEAR));

      try {
        BufferedReader reader;
        String syncdirFullPath = TEMP_COLLECTION_DIR
            + TBLoader.syncSubPath;  // at the end, this gets zipped up into the copyTo (Dropbox dir)
        String feedbackCommunityPath =
            copyTo + "/UserRecordings/" + TBLoader.oldDeploymentText.getText()
                + "/" +
                TBLoader.deviceID + "/" + TBLoader.oldCommunityText.getText();

        reader = new BufferedReader(new FileReader(file));
        while (reader.ready() && !criticalError) {
          String cmd = reader.readLine();
          if (cmd.startsWith("rem ")) {
            status.setText("STATUS: " + cmd.substring(4));
            LOG.log(Level.INFO, cmd.substring(4));
            continue;
          }
          cmd = cmd.replaceAll("\\$\\{device_drive\\}",
              devicePath.substring(0, 2));
          //cmd = cmd.replaceAll("\\$\\{srn\\}", tbSrn);
          cmd = cmd.replaceAll("\\$\\{new_srn\\}", tbSrn.toUpperCase());
          cmd = cmd.replaceAll("\\$\\{device_id\\}",
              TBLoader.deviceID.toUpperCase());  // this is the computer/tablet/phone tbSrn
          cmd = cmd.replaceAll("\\$\\{datetime\\}",
              TBLoader.currentDrive.datetime);
          cmd = cmd.replaceAll("\\$\\{syncpath\\}",
              Matcher.quoteReplacement(syncdirFullPath));
          cmd = cmd.replaceAll("\\$\\{syncdir\\}",
              TBLoader.currentDrive.syncdir);
          cmd = cmd.replaceAll("\\$\\{recording_path\\}",
              Matcher.quoteReplacement(feedbackCommunityPath));
          cmd = cmd.replaceAll("\\$\\{dateInMonth\\}", dateInMonth);
          cmd = cmd.replaceAll("\\$\\{month\\}", month);
          cmd = cmd.replaceAll("\\$\\{year\\}", year);
          cmd = cmd.replaceAll("\\$\\{send_now_dir\\}",
              Matcher.quoteReplacement(copyTo));
          cmd = cmd.replaceAll("\\$\\{new_revision\\}",
              TBLoader.newFirmwareRevisionText.getText());
          cmd = cmd.replaceAll("\\$\\{old_revision\\}",
              TBLoader.oldFirmwareRevisionText.getText());
          cmd = cmd.replaceAll("\\$\\{new_deployment\\}",
              TBLoader.newDeploymentList.getSelectedItem()
                  .toString()
                  .toUpperCase());
          cmd = cmd.replaceAll("\\$\\{old_deployment\\}",
              TBLoader.oldDeploymentText.getText().toUpperCase());
          cmd = cmd.replaceAll("\\$\\{new_community\\}",
              TBLoader.newCommunityList.getSelectedItem()
                  .toString()
                  .toUpperCase());
          cmd = cmd.replaceAll("\\$\\{old_community\\}",
              TBLoader.oldCommunityText.getText().toUpperCase());
          cmd = cmd.replaceAll("\\$\\{new_image\\}",
              TBLoader.newImageText.getText().toUpperCase());
          cmd = cmd.replaceAll("\\$\\{old_image\\}",
              TBLoader.oldImageText.getText().toUpperCase());
          cmd = cmd.replaceAll("\\$\\{volumeSRN\\}", volumeSerialNumber);
          //cmd = cmd.replaceAll("\\$\\{holding_dir\\}", Matcher.quoteReplacement(TEMP_COLLECTION_DIR));
          //cmd = cmd.replaceAll("\\$\\{hand\\}", handValue);
          alert = cmd.startsWith("!");
          if (alert)
            cmd = cmd.substring(1);
          errorLine = execute("cmd /C " + cmd);
          if (errorLine != null && alert) {
            if (!errorLine.equalsIgnoreCase(
                "TB not found.  Unplug/replug USB and try again.") && !errorLine
                .equalsIgnoreCase("File system corrupted")) {
              JOptionPane.showMessageDialog(null, errorLine, "Error",
                  JOptionPane.ERROR_MESSAGE);
            }
            criticalError = true;
            success = false;
            break;
          }
        }
        reader.close();
      } catch (Exception e) {
        LOG.log(Level.WARNING, e.toString(), e);
      }
      return success;
    }

    private void grabStatsOnly() {
      String endMsg = "";
      String endTitle = "";
      try {
        boolean gotStats, hasCorruption, goodCard;
        setStartTime();
        success = false;
        goodCard = executeFile(new File(SCRIPT_SUBDIR + "checkConnection.txt"));
        if (!goodCard) {
          return;
        }
        TBLoader.status2.setText("Checking Memory Card");
        LOG.log(Level.INFO, "STATUS:Checking Memory Card");
        hasCorruption = !executeFile(new File(SCRIPT_SUBDIR + "chkdsk.txt"));
        if (hasCorruption) {
          TBLoader.currentDrive.corrupted = true;
          TBLoader.status2.setText(
              TBLoader.status2.getText() + "...Corrupted\nGetting Stats");
          LOG.log(Level.INFO, "STATUS:Corrupted...Getting Stats");
          executeFile(new File(SCRIPT_SUBDIR + "chkdsk-save.txt"));
        } else {
          TBLoader.status2.setText(
              TBLoader.status2.getText() + "...Good\nGetting Stats");
          LOG.log(Level.INFO, "STATUS:Good Card\nGetting Stats");
        }
        gotStats = executeFile(new File(SCRIPT_SUBDIR + "grab.txt"));
        callback.logTBData("stats-only");
        if (gotStats) {
          TBLoader.status2.setText(
              TBLoader.status2.getText() + "...Got Stats\nErasing Flash Stats");
          LOG.log(Level.INFO, "STATUS:Got Stats!\nErasing Flash Stats");
          executeFile(new File(SCRIPT_SUBDIR + "eraseFlashStats.txt"));
          TBLoader.status2.setText(TBLoader.status2.getText()
              + "...Erased Flash Stats\nDisconnecting");
          LOG.log(Level.INFO, "STATUS:Erased Flash Stats");
          LOG.log(Level.INFO, "STATUS:Disconnecting TB");
          executeFile(new File(SCRIPT_SUBDIR + "disconnect.txt"));
          TBLoader.status2.setText(TBLoader.status2.getText() + "...Complete");
          LOG.log(Level.INFO, "STATUS:Complete");
          success = true;
          endMsg = new String("Got Stats!");
          endTitle = new String("Success");
        } else {
          TBLoader.status2.setText(
              TBLoader.status2.getText() + "...No Stats!\n");
          LOG.log(Level.INFO, "STATUS:No Stats!");
          endMsg = new String("Could not get stats for some reason.");
          endTitle = new String("Failure");
        }
        // zip up stats
        String sourceFullPath = TEMP_COLLECTION_DIR + TBLoader.syncSubPath;
        String targetFullPath = copyTo + TBLoader.syncSubPath + ".zip";
        File sourceFile = new File(sourceFullPath);
        sourceFile.getParentFile().mkdirs();
        ZipUnzip.zip(sourceFile, new File(targetFullPath), true);
        FileUtils.deleteDirectory(sourceFile);
      } catch (IOException e) {
        LOG.log(Level.WARNING, "Unable to zip device files:", e);
      } finally {
        callback.onCopyFinished(success, tbSrn, this.operation, endMsg,
            endTitle);
      }
    }

    private void update() {
      String endMsg = "";
      String endTitle = "";

      try {
        boolean gotStats, hasCorruption, verified, goodCard;
        setStartTime();
        success = false;
        goodCard = executeFile(new File(SCRIPT_SUBDIR + "checkConnection.txt"));
        if (!goodCard) {
          return;
        }
        TBLoader.status2.setText("Checking Memory Card");
        LOG.log(Level.INFO, "STATUS:Checking Memory Card");
        hasCorruption = !executeFile(new File(SCRIPT_SUBDIR + "chkdsk.txt"));
        if (hasCorruption) {
          TBLoader.currentDrive.corrupted = true;
          TBLoader.status2.setText(
              TBLoader.status2.getText() + "...Corrupted\nGetting Stats");
          LOG.log(Level.INFO, "STATUS:Corrupted...Getting Stats\n");
          executeFile(new File(SCRIPT_SUBDIR + "chkdsk-save.txt"));
        } else {
          TBLoader.status2.setText(
              TBLoader.status2.getText() + "...Good\nGetting Stats");
          LOG.log(Level.INFO, "STATUS:Good Card...Getting Stats\n");
        }
        gotStats = executeFile(new File(SCRIPT_SUBDIR + "grab.txt"));
        if (gotStats) {
          TBLoader.status2.setText(
              TBLoader.status2.getText() + "...Got Stats\n");
          LOG.log(Level.INFO, "STATUS:Got Stats\n");
        } else {
          TBLoader.status2.setText(
              TBLoader.status2.getText() + "...No Stats!\n");
          LOG.log(Level.INFO, "STATUS:No Stats!\n");
        }
        // zip up stats
        String sourceFullPath = TEMP_COLLECTION_DIR + TBLoader.syncSubPath;
        String targetFullPath = copyTo + TBLoader.syncSubPath + ".zip";
        File sourceFile = new File(sourceFullPath);
        sourceFile.getParentFile().mkdirs();
        ZipUnzip.zip(sourceFile, new File(targetFullPath), true);
        FileUtils.deleteDirectory(sourceFile);

        if (hasCorruption) {
          TBLoader.status2.setText(TBLoader.status2.getText() + "Reformatting");
          LOG.log(Level.INFO, "STATUS:Reformatting");
          goodCard = executeFile(new File(SCRIPT_SUBDIR + "reformat.txt"));
          if (!goodCard) {
            TBLoader.status2.setText(
                TBLoader.status2.getText() + "...Failed\n");
            LOG.log(Level.INFO, "STATUS:Reformat Failed");
            LOG.log(Level.INFO,
                "Could not reformat memory card.\nMake sure you have a good USB connection\nand that the Talking Book is powered with batteries, then try again.\n\nIf you still cannot reformat, replace the memory card.");
            JOptionPane.showMessageDialog(null,
                "Could not reformat memory card.\nMake sure you have a good USB connection\nand that the Talking Book is powered with batteries, then try again.\n\nIf you still cannot reformat, replace the memory card.",
                "Failure!", JOptionPane.ERROR_MESSAGE);
            return;
          } else {
            TBLoader.status2.setText(TBLoader.status2.getText() + "...Good\n");
            LOG.log(Level.INFO, "STATUS:Format was good");
          }
        } else {
          if (!newSrnText.getText()
              .equalsIgnoreCase(
                  TBLoader.currentDrive.getLabelWithoutDriveLetter())) {
            LOG.log(Level.INFO, "STATUS:Relabeling volume");
            TBLoader.status2.setText(
                TBLoader.status2.getText() + "Relabeling\n");
            executeFile(new File(SCRIPT_SUBDIR + "relabel.txt"));
          }
        }
        TBLoader.status2.setText(
            TBLoader.status2.getText() + "Updating TB Files");
        LOG.log(Level.INFO, "STATUS:Updating TB Files");
        executeFile(new File(SCRIPT_SUBDIR + "update.txt"));
        LOG.log(Level.INFO, "STATUS:Updated");
        LOG.log(Level.INFO, "STATUS:Adding Image Content");
        verified = executeFile(new File(SCRIPT_SUBDIR + "customCommunity.txt"));
        if (TBLoader.forceFirmware.isSelected()) {
          // rename firmware at root to system.img to force TB to update itself
          String rootPath = devicePath.substring(0, 2);
          File root = new File(rootPath);
          File firmware = new File(root, TBLoader.revision + ".img");
          firmware.renameTo(new File(root, "system.img"));
          TBLoader.status2.setText(
              TBLoader.status2.getText() + "\nRefreshed firmware...");
          LOG.log(Level.INFO, "STATUS:Forced Firmware Refresh");
        }
        TBLoader.status2.setText(TBLoader.status2.getText() + "...Updated\n");
        if (verified) {
          String duration;
          TBLoader.status2.setText(TBLoader.status2.getText()
              + "Updated & Verified\nDisconnecting TB");
          LOG.log(Level.INFO, "STATUS:Updated & Verified...Disconnecting TB");
          executeFile(new File(SCRIPT_SUBDIR + "disconnect.txt"));
          TBLoader.status2.setText(TBLoader.status2.getText() + "...Complete");
          LOG.log(Level.INFO, "STATUS:Complete");
          success = true;
          duration = getDuration();
          if (TBLoader.forceFirmware.isSelected())
            callback.logTBData("update-fw");
          else
            callback.logTBData("update");
          endMsg = new String(
              "Talking Book has been updated and verified\nin " + duration
                  + ".");
          endTitle = new String("Success");
        } else {
          String duration;
          duration = getDuration();
          callback.logTBData("update-failed verification");
          success = false;
          TBLoader.status2.setText(
              TBLoader.status2.getText() + "...Failed Verification in "
                  + duration + "\n");
          LOG.log(Level.INFO, "STATUS:Failed Verification");
          endMsg = new String(
              "Update failed verification.  Try again or replace memory card.");
          endTitle = new String("Failure");
        }
        for (int i = 1; i <= (success ? 3 : 6); i++)
          Toolkit.getDefaultToolkit().beep();
      } catch (Exception e) {
        if (alert) {
          JOptionPane.showMessageDialog(null, e.getMessage(), "Error",
              JOptionPane.ERROR_MESSAGE);
          criticalError = true;
          LOG.log(Level.SEVERE, "CRITICAL ERROR:", e);
        } else
          LOG.log(Level.WARNING, "NON-CRITICAL ERROR:", e);
      } finally {
        callback.onCopyFinished(success, tbSrn, this.operation, endMsg,
            endTitle);
      }

    }

    @Override
    public void run() {
      // Like "/TalkingBookData/2010-1/1234/DEMO-SEATTLE/B-12340201/2020y12m25d00h00m01s-1234"
      TBLoader.syncSubPath =
          "/TalkingBookData/" + TBLoader.oldDeploymentText.getText() + "/" +
              TBLoader.deviceID + "/" + TBLoader.oldCommunityText.getText()
              + "/" + tbSrn + "/" + TBLoader.currentDrive.syncdir;
      if (this.operation == Operation.Update)
        update();
      else if (this.operation == Operation.CollectStats)
        grabStatsOnly();
    }
  }

  private static String dosErrorCheck(String line) {
    String errorMsg = null;

    if (line.contains("New")) {
      //  file copy validation failed (some files missing on target)
      errorMsg = line;//.substring(line.length()-30);
    } else if (line.contains("Invalid media or Track 0 bad - disk unusable")) {
      // formatting error
      errorMsg = "Bad memory card.  Please discard and replace it.";
    } else if (line.contains("Specified drive does not exist.")
        || line.startsWith(
        "The volume does not contain a recognized file system.")) {
      errorMsg = "Either bad memory card or USB connection problem.  Try again.";
    } else if (line.contains("Windows found problems with the file system") /* || line.startsWith("File Not Found") */
        || line.startsWith("The system cannot find the file")) {
      // checkdisk shows corruption
      errorMsg = "File system corrupted";
    } else if (line.startsWith("The system cannot find the path specified.")) {
      errorMsg = "TB not found.  Unplug/replug USB and try again.";
    }
    return errorMsg;
  }

  static String execute(String cmd) throws Exception {
    String line;
    String errorLine = null;
    LOG.log(Level.INFO, "Executing:" + cmd);
    Process proc = Runtime.getRuntime().exec(cmd);

    BufferedReader br1 = new BufferedReader(
        new InputStreamReader(proc.getInputStream()));
    BufferedReader br2 = new BufferedReader(
        new InputStreamReader(proc.getErrorStream()));

    do {
      line = br1.readLine();
      LOG.log(Level.INFO, line);
      if (line != null && errorLine == null)
        errorLine = dosErrorCheck(line);
    } while (line != null);

    do {
      line = br2.readLine();
      LOG.log(Level.INFO, line);
      if (line != null && errorLine == null)
        errorLine = dosErrorCheck(line);
    } while (line != null);

    proc.waitFor();
    return errorLine;
  }

  private static class DriveInfo {
    final File drive;
    String label;
    String serialNumber;
    boolean corrupted;
    String datetime = "";
    String syncdir = "";

    public DriveInfo(File drive, String label) {
      this.drive = drive;
      this.label = label.trim();
      this.corrupted = false;
      this.serialNumber = "";
      this.datetime = getDateTime();
      this.syncdir = this.datetime + "-" + TBLoader.deviceID;
    }

    public String getLabelWithoutDriveLetter() {
      String label = this.label.substring(0, this.label.lastIndexOf('(') - 1);
      return label;
    }

    @Override
    public String toString() {
      if (label.isEmpty()) {
        return drive.toString();
      }
      return label;
    }
  }
}
