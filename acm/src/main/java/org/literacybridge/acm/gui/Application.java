package org.literacybridge.acm.gui;

import org.apache.commons.lang3.StringUtils;
import org.jdesktop.swingx.JXFrame;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.literacybridge.acm.Constants;
import org.literacybridge.acm.cloud.Authenticator;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.config.AmplioHome;
import org.literacybridge.acm.config.PathsProvider;
import org.literacybridge.acm.device.FileSystemMonitor;
import org.literacybridge.acm.device.LiteracyBridgeTalkingBookRecognizer;
import org.literacybridge.acm.gui.MainWindow.MainView;
import org.literacybridge.acm.gui.MainWindow.ToolbarView;
import org.literacybridge.acm.gui.dialogs.AcmCheckoutTest;
import org.literacybridge.acm.gui.dialogs.LafTester;
import org.literacybridge.acm.gui.dialogs.S3SyncDialog;
import org.literacybridge.acm.gui.playerAPI.SimpleSoundPlayer;
import org.literacybridge.acm.gui.resourcebundle.LabelProvider;
import org.literacybridge.acm.gui.util.FilterState;
import org.literacybridge.acm.gui.util.SimpleMessageService;
import org.literacybridge.acm.utils.LogHelper;
import org.literacybridge.acm.utils.OsUtils;
import org.literacybridge.acm.utils.SwingUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.literacybridge.acm.cloud.Authenticator.LoginOptions.CHOOSE_PROGRAM;
import static org.literacybridge.acm.cloud.Authenticator.LoginOptions.INCLUDE_FB_ACMS;
import static org.literacybridge.acm.cloud.Authenticator.LoginOptions.LOCAL_OR_S3;
import static org.literacybridge.acm.cloud.Authenticator.LoginOptions.NOP;
import static org.literacybridge.acm.cloud.Authenticator.LoginOptions.NO_WAIT;
import static org.literacybridge.acm.cloud.Authenticator.LoginOptions.OFFER_DEMO_MODE;
import static org.literacybridge.acm.cloud.Authenticator.LoginOptions.OFFLINE_EMAIL_CHOICE;
import static org.literacybridge.acm.cloud.Authenticator.LoginOptions.SUGGEST_DEMO_MODE;

public class Application extends JXFrame {
  private static final Logger LOG = Logger
      .getLogger(Application.class.getName());

  private static final long serialVersionUID = -7011153239978361786L;

  public static double JAVA_VERSION = getJavaVersion();

  private final MainView mainView;

  public MainView getMainView() {
    return mainView;
  }

  static double getJavaVersion() {
    String version = System.getProperty("java.version");
    int pos = version.indexOf('.');
    pos = version.indexOf('.', pos+1);
    return Double.parseDouble (version.substring (0, pos));
  }

  // message pump
  private static final SimpleMessageService simpleMessageService = new SimpleMessageService();

  private Color backgroundColor;
  private final ACMStatusBar statusBar;
  private final BackgroundTaskManager taskManager;

  public static SimpleMessageService getMessageService() {
    return simpleMessageService;
  }

  // file system monitor for the audio devices
  private static final FileSystemMonitor fileSystemMonitor = new FileSystemMonitor();

  public static FileSystemMonitor getFileSystemMonitor() {
    return fileSystemMonitor;
  }

  private static final FilterState filterState = new FilterState();

  public static FilterState getFilterState() {
    return filterState;
  }

  // application instance
  private static Application application;

  public static Application getApplication() {
    return application;
  }

  private final SimpleSoundPlayer player = new SimpleSoundPlayer();

  private Application(SplashScreen splashScreen) {
    super();
    this.backgroundColor = getBackground();
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        try {
          if (!ACMConfiguration.getInstance().getCurrentDB().isSandboxed()) {
              // Will ask user if they want to save.
              ACMConfiguration.getInstance().commitCurrentDB();
          }
          ACMConfiguration.getInstance().closeCurrentDB();
        } catch (Exception e1) {
          e1.printStackTrace();
        }
      }
    });

    if (ACMConfiguration.getInstance().isDevo()) {
      JMenuBar menuBar = new JMenuBar();
      JMenu menu = new JMenu("Developer");
      menuBar.add(menu);
      JMenuItem menuItem = new JMenuItem("Access Control...");
      menu.add(menuItem);
      menuItem.addActionListener(e -> new AcmCheckoutTest(this).setVisible(true));
      menuItem = new JMenuItem("UI Defaults...");
      menu.add(menuItem);
      menuItem.addActionListener(e -> new LafTester(this).setVisible(true));
      setJMenuBar(menuBar);
    }

    Authenticator authInstance = Authenticator.getInstance();
    String greeting = authInstance.getUserSelfName();
    if (StringUtils.isEmpty(greeting)) {
      greeting = String.format("Hello, %s", authInstance.getUserEmail());
    }
    String sandboxWarning = (ACMConfiguration.getInstance().getCurrentDB().isSandboxed()) ?
        "  --  CHANGES WILL *NOT* BE SAVED!":"";
    String cloudIndicator = ACMConfiguration.getInstance().getCurrentDB().getPathProvider().isDropboxDb()
                            ? (OsUtils.WINDOWS ? "dbx" : "∅")
                            : (OsUtils.WINDOWS ? "s3" : "✓");
    String layoutIndicator = AmplioHome.isOldStyleHomeDirectory()
                             ? (OsUtils.WINDOWS ? "v1" : "∅")
                             : (OsUtils.WINDOWS ? "v2" : "✓");

      String dbVersion = "v" +
          ACMConfiguration.getInstance().getCurrentDB().getCurrentDbVersion() +
          cloudIndicator;
      String title = String.format("%s  --  %s (%s%s)  --  %s (%s)%s",
            greeting,
            LabelProvider.getLabel("TITLE_LITERACYBRIDGE_ACM"),
            Constants.ACM_VERSION,
            layoutIndicator,
            ACMConfiguration.getInstance().getTitle(),
          dbVersion,
            sandboxWarning);

    setTitle(title);
    // toolbar view on top
    mainView = new MainView();
    ToolbarView toolbarView = new ToolbarView(mainView.audioItemView);
    add(toolbarView, BorderLayout.PAGE_START);
    add(mainView, BorderLayout.CENTER);

    statusBar = new ACMStatusBar();
    setStatusBar(statusBar);
    taskManager = new BackgroundTaskManager(statusBar);

    // starts file system monitor after UI has been initialized
    fileSystemMonitor
        .addDeviceRecognizer(new LiteracyBridgeTalkingBookRecognizer());
    fileSystemMonitor.start();
  }

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

  public void setStatusMessage(String message) {
    statusBar.setStatusMessage(message);
  }

  public void setStatusMessage(String message, int displayDurationMillis) {
    statusBar.setStatusMessage(message, displayDurationMillis);
  }

  public void setProgressMessage(String message) {
    statusBar.setProgressMessage(message);
  }

  public BackgroundTaskManager getTaskManager() {
    return taskManager;
  }

  public SimpleSoundPlayer getSoundPlayer() {
    return this.player;
  }

  /**
   * Main entry point for the ACM application.
   * @param args to the app.
   * @throws Exception if initialization fails.
   */
  public static void main(String[] args) throws Exception {
      new LogHelper().inDirectory(AmplioHome.getLogsDir()).withName("ACM.log").initialize();

    // We can use this to put the menu in the right place on MacOS. When we have a menu.
    System.setProperty("apple.laf.useScreenMenuBar", "true");
    // This doesn't work because somehow the property has already been read by this point.
    // Something to do with the AppKit thread starting earlier than this.
    //System.setProperty("apple.eawt.quitStrategy", "CLOSE_ALL_WINDOWS");

    System.out.println("starting main()");
    CommandLineParams params = new CommandLineParams();
    CmdLineParser parser = new CmdLineParser(params);
    try {
      parser.parseArgument(args);
    } catch (CmdLineException e) {
      System.err.println(e.getMessage());
      System.err.println(
          "java -cp acm.jar;lib/* org.literacybridge.acm.gui.Application [options...]");
      parser.printUsage(System.err);
      return;
    }

    startUp(params);

    if (params.cleanUnreferenced) {
      ACMConfiguration.getInstance().getCurrentDB().cleanUnreferencedFiles();
    }
  }

  private static void startUp(CommandLineParams params) throws Exception {
    preRunChecks();

    SplashScreen splash = null;
    URL iconURL = Application.class.getResource("/tb.png");
    assert iconURL != null;
    Image iconImage = new ImageIcon(iconURL).getImage();
    if (OsUtils.MAC_OS) {
      OsUtils.setOSXApplicationIcon(iconImage);
    }
    OsUtils.enableOSXQuitStrategy();

      // set look & feel; we use Sea Glass by default.
      SwingUtils.setLookAndFeel(params.nimbus?"nimbus":"");

    params.update = true; // may be overridden later.
    ACMConfiguration.initialize(params);

    authenticateAndChooseProgram(params);

    if (isEmpty(params.sharedACM)) {
      JOptionPane.showMessageDialog(null,
          "No ACM chosen Can not continue.");
      System.exit(1);
    }

    openAcmDb(params.sharedACM);

    application = new Application(null);
      OsUtils.enableOSXFullscreen(application);
      if (!OsUtils.MAC_OS) {
        application.setIconImage(iconImage);
      }
      application.setSize(1000, 725);
      application.setLocation(20, 20);

      application.setVisible(true);
      application.toFront();

      LOG.log(Level.INFO, "ACM successfully started.");
      ACMConfiguration.getInstance().getCurrentDB().setupWavCaching(sizeMB->{
        int answer = JOptionPane.showOptionDialog(null,
                "The WAV cache is currently using " + sizeMB
                        + " MB disk space and a cleanup is recommended. Perform cleanup?",
                "WAV Cache", JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE, null, null, JOptionPane.YES_OPTION);
        return (answer == JOptionPane.YES_OPTION);
      });
      application.mainView.audioItemView.requestFocusInWindow();
  }

  /**
   * Opens the ACM database. If the ACM is in S3, performs a sync. If the ACM has never been downloaded, that
   * may take some time.
   * @param sharedACM ACM to be opened.
   */
  private static void openAcmDb(String sharedACM) {
      boolean syncOk = true;
      // Migration from Dropbox to S3 required?
      if (Authenticator.getInstance().isProgramS3(sharedACM)) {
          PathsProvider pathsProvider = ACMConfiguration.getInstance().getPathProvider(sharedACM);
          if (pathsProvider == null) {
              // The database doesn't exist locally, but it does exist in S3. ".DOWNLOAD" will set up
              // synchronization with Dropbox.
              syncOk = syncFromS3(sharedACM, S3SyncDialog.SYNC_STYLE.DOWNLOAD, false);
          } else if (pathsProvider.isDropboxDb()) {
              // The database is configured to sync with S3, but locally it is still syncing with Dropbox
              // TOOD: Move from Dropbox to S3, to save the download. (Download works, but wastes time).
              syncOk = syncFromS3(sharedACM, S3SyncDialog.SYNC_STYLE.DOWNLOAD, true);
          } else {
              // Mere sync required.
              syncOk = syncFromS3(sharedACM, S3SyncDialog.SYNC_STYLE.SYNC, false);
          }
      }

      // init database
      try {
          String text = "Syncing database for " + sharedACM;
          ACMConfiguration.S3SyncState syncState = syncOk
                                                   ? ACMConfiguration.S3SyncState.NOT_REQUIRED
                                                   : ACMConfiguration.S3SyncState.FAILED;
          ACMConfiguration.getInstance().setCurrentDB(sharedACM, syncState);
      } catch (Exception e) {
          e.printStackTrace();
          JOptionPane.showMessageDialog(null,
                  "Unable to connect to database. Please try restarting the ACM.");
          System.exit(1);
      }
  }

  /**
   * Sync the given program with S3.
   * @param program The program to be synchronized.
   * @param syncStyle DOWNLOAD or SYNC
   * @param haveDropboxCopy is unused
   * @return true if the sync completed successfully.
   */
  private static boolean syncFromS3(String program,
          S3SyncDialog.SYNC_STYLE syncStyle,
          boolean haveDropboxCopy) {
      S3SyncDialog dialog = new S3SyncDialog(null, program, syncStyle);
      dialog.go();
      ACMConfiguration.getInstance().discoverDB(program);
      return !dialog.hasSyncError();
  }

  /**
   * If there is no ACM specified on the command line, query the user.
   * @param params from the command line.
   */
  private static void authenticateAndChooseProgram(CommandLineParams params) {
      Authenticator authInstance = Authenticator.getInstance();
      authInstance.setLocallyAvailablePrograms(ACMConfiguration.getInstance().getLocalDbs(),
              ACMConfiguration.getInstance().getLocalDbxDbs());
      Authenticator.LoginResult result = authInstance.getUserIdentity(null,
              LabelProvider.getLabel("TITLE_LITERACYBRIDGE_ACM"),
              ACMConfiguration.cannonicalProjectName(params.sharedACM),
              OFFLINE_EMAIL_CHOICE,
              CHOOSE_PROGRAM,
              LOCAL_OR_S3, //LOCAL_DATA_ONLY,
              params.sandbox ? SUGGEST_DEMO_MODE : OFFER_DEMO_MODE, // vs no demo mode at all
              INCLUDE_FB_ACMS,
              params.go ? NO_WAIT : NOP);
      if (result == Authenticator.LoginResult.FAILURE) {
          JOptionPane.showMessageDialog(null,
                  "Authentication is required to use the ACM.",
                  "Authentication Failure",
                  JOptionPane.ERROR_MESSAGE);
          System.exit(13);
      }

      params.sharedACM = authInstance.getUserProgram();
      boolean sandbox = authInstance.isSandboxSelected();
      // Even if the user didn't ask for sandbox, if they have no role in the db they picked, force sandbox.
      if (!sandbox) {
          List<String> ACM_ROLES = Arrays.asList("*", "AD", "PM", "CO");
          Set<String> roles = authInstance.getUserRoles();
          sandbox = Collections.disjoint(roles, ACM_ROLES);
      }
      ACMConfiguration.getInstance().setForceSandbox(sandbox);
  }

  /**
   * Performs some checks before starting the application. If we determine that the
   * application can't run, calls exit()
   */
  private static void preRunChecks() {
    // Check system memory. If insufficient, show a message and exit.
    // This doesn't seem to still be necessary. be: 2020-07-30
//    if (Runtime.getRuntime().maxMemory() < 400 * 1024 * 1024) {
//      JOptionPane.showMessageDialog(null,
//              "Not enough memory available for JVM. Please make sure your"
//                      + " java command contains the argument -Xmx512m (or more).");
//      System.exit(0);
//    }

    // Prompt the user to update to Java 8. Can't fail.
    if (JAVA_VERSION < 1.8) {
      String message = "This computer needs to be updated to Java 8." +
          "\n\nPlease contact ICT staff to arrange for the update." +
          "\n\nThe update will only take a few minutes. Please try" +
          "\nto do this in the next few days. In the meantime, the" +
          "\nACM will continue to work normally." +
          "\n\nThank you!";
      JOptionPane.showMessageDialog(null, message, "Please Update Java", JOptionPane.INFORMATION_MESSAGE);
    }
  }

}
