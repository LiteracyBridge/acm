package org.literacybridge.acm.gui;

import org.jdesktop.swingx.JXFrame;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.literacybridge.acm.Constants;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.device.FileSystemMonitor;
import org.literacybridge.acm.device.LiteracyBridgeTalkingBookRecognizer;
import org.literacybridge.acm.gui.ResourceView.ResourceView;
import org.literacybridge.acm.gui.ResourceView.ToolbarView;
import org.literacybridge.acm.gui.playerAPI.SimpleSoundPlayer;
import org.literacybridge.acm.gui.resourcebundle.LabelProvider;
import org.literacybridge.acm.gui.util.SimpleMessageService;
import org.literacybridge.acm.repository.FileSystemGarbageCollector.GCInfo;
import org.literacybridge.acm.repository.WavFilePreCaching;
import org.literacybridge.acm.store.Category;
import org.literacybridge.acm.store.MetadataStore;
import org.literacybridge.acm.store.Playlist;
import org.literacybridge.acm.store.SearchResult;
import org.literacybridge.acm.utils.LogHelper;
import org.literacybridge.acm.utils.OsUtils;
import org.literacybridge.acm.utils.SwingUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Application extends JXFrame {
  private static final Logger LOG = Logger
      .getLogger(Application.class.getName());

  private static final long serialVersionUID = -7011153239978361786L;

  public static double JAVA_VERSION = getJavaVersion();

  private final ResourceView resourceView;

  public ResourceView getResourceView() {
    return resourceView;
  }

  static double getJavaVersion() {
    String version = System.getProperty("java.version");
    int pos = version.indexOf('.');
    pos = version.indexOf('.', pos+1);
    return Double.parseDouble (version.substring (0, pos));
  }

  // message pump
  private static SimpleMessageService simpleMessageService = new SimpleMessageService();

  private Color backgroundColor;
  private final ACMStatusBar statusBar;
  private final BackgroundTaskManager taskManager;

  public static SimpleMessageService getMessageService() {
    return simpleMessageService;
  };

  // file system monitor for the audio devices
  private static FileSystemMonitor fileSystemMonitor = new FileSystemMonitor();

  public static FileSystemMonitor getFileSystemMonitor() {
    return fileSystemMonitor;
  }

  private static FilterState filterState = new FilterState();

  public static FilterState getFilterState() {
    return filterState;
  }

  // application instance
  private static Application application;

  public static Application getApplication() {
    return application;
  }

  private SimpleSoundPlayer player = new SimpleSoundPlayer();

  private Application(SplashScreen splashScreen) throws IOException {
    super();
    this.backgroundColor = getBackground();
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        try {
          if (!ACMConfiguration.getInstance().getCurrentDB().isSandboxed()) {
              ACMConfiguration.getInstance().getCurrentDB().updateDb();
          }
          ACMConfiguration.getInstance().closeCurrentDB();
        } catch (Exception e1) {
          e1.printStackTrace();
        }
      }
    });

    String sandboxWarning = (ACMConfiguration.getInstance().getCurrentDB().isSandboxed()) ?
        "               CHANGES WILL *NOT* BE SAVED!   ":"";

    String title = String.format("%s (%s)   User: '%s'    %s (v%d)%s",
        LabelProvider.getLabel("TITLE_LITERACYBRIDGE_ACM"),
        Constants.ACM_VERSION,
        ACMConfiguration.getInstance().getUserName(),
        ACMConfiguration.getInstance().getTitle(),
        ACMConfiguration.getInstance().getCurrentDB().getCurrentDbVersion(),
        sandboxWarning);

    setTitle(title);
    // toolbar view on top
    resourceView = new ResourceView();
    ToolbarView toolbarView = new ToolbarView(resourceView.audioItemView);
    add(toolbarView, BorderLayout.PAGE_START);
    add(resourceView, BorderLayout.CENTER);

    statusBar = new ACMStatusBar();
    setStatusBar(statusBar);
    taskManager = new BackgroundTaskManager(statusBar);

    try {
      splashScreen.setProgressLabel("Updating index...");
    } catch (Exception e) {
      e.printStackTrace();
    }

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

  public void setProgressMessage(String message) {
    statusBar.setProgressMessage(message);
    ;
  }

  public BackgroundTaskManager getTaskManager() {
    return taskManager;
  }

  public SimpleSoundPlayer getSoundPlayer() {
    return this.player;
  }

  public static void main(String[] args) throws Exception {
      new LogHelper().withName("ACM.log").initialize();
    // We can use this to put the menu in the right place on MacOS. When we have a menu.
    //System.setProperty("apple.laf.useScreenMenuBar", "true");
    // This doesn't work because somehow the property has already been read by this point.
    // Something to do with the AppKit thread starting earlier than this.
    //System.setProperty("apple.eawt.quitStrategy", "CLOSE_ALL_WINDOWS");
    System.out.println("starting main()");
    CommandLineParams params = new CommandLineParams();
    CmdLineParser parser = new CmdLineParser(params);
    try {
      parser.parseArgument(args);
      params.sharedACM = ACMConfiguration.cannonicalAcmDirectoryName(params.sharedACM);
    } catch (CmdLineException e) {
      System.err.println(e.getMessage());
      System.err.println(
          "java -cp acm.jar;lib/* org.literacybridge.acm.gui.Application [options...]");
      parser.printUsage(System.err);
      return;
    }
    startUp(params);
    if (params.cleanUnreferenced) {
      ACMConfiguration.getInstance().getCurrentDB().getRepository().cleanUnreferencedFiles();
    }
  }

  private static void startUp(CommandLineParams params) throws Exception {
    SplashScreen splash = new SplashScreen();

//      URL iconURL = Application.class.getResource("/tb_headset.png");
    URL iconURL = Application.class.getResource("/tb.png");
    Image iconImage = new ImageIcon(iconURL).getImage();
    if (OsUtils.MAC_OS) {
      OsUtils.setOSXApplicationIcon(iconImage);
    } else {
      splash.setIconImage(iconImage);
    }
    OsUtils.enableOSXQuitStrategy();


      // set look & feel
      SwingUtils.setLookAndFeel("");
      splash.showSplashScreen();

      if (Runtime.getRuntime().maxMemory() < 400 * 1024 * 1024) {
        JOptionPane.showMessageDialog(null,
            "Not enough memory available for JVM. Please make sure your"
                + " java command contains the argument -XMX512m (or more).");
        System.exit(0);
      }

    // String dbDirName = null, repositoryDirName= null;
    // initialize config and generate random ID for this acm instance
    splash.setProgressLabel("Initializing...");
    ACMConfiguration.initialize(params);

    // init database
    try {
      // TODO: when we have a homescreen this call will be delayed until the
      // user selects a DB
      // TODO: createEmtpyDB should be factored out when the UI has a create DB
      // button.
      ACMConfiguration.getInstance().setCurrentDB(params.sharedACM, true);
    } catch (Exception e) {
      e.printStackTrace();
      JOptionPane.showMessageDialog(null,
          "Unable to connect to database. Please try restarting the ACM.");
      System.exit(1);
    }

      application = new Application(splash);
      OsUtils.enableOSXFullscreen(application);
      if (!OsUtils.MAC_OS) {
        application.setIconImage(iconImage);
      }
      splash.setProgressLabel("Initialization complete. Launching UI...");
      application.setSize(1000, 725);

      application.setVisible(true);
      application.toFront();
      splash.close();

      // Prompt the user to update to Java 8.
      if (JAVA_VERSION < 1.8) {
        String message = "This computer needs to be updated to Java 8." +
                "\n\nPlease contact ICT staff to arrange for the update." +
                "\n\nThe update will only take a few minutes. Please try" +
                "\nto do this in the next few days. In the meantime, the" +
                "\nACM will continue to work normally." +
                "\n\nThank you!";
        JOptionPane.showMessageDialog(null, message, "Please Update Java", JOptionPane.INFORMATION_MESSAGE);
      }

      LOG.log(Level.INFO, "ACM successfully started.");
      WavFilePreCaching caching = new WavFilePreCaching();
      GCInfo gcInfo = ACMConfiguration.getInstance().getCurrentDB().getRepository().getGcInfo();

      if (gcInfo.isGcRecommended()) {
        long sizeMB = gcInfo.getCurrentSizeInBytes() / 1024 / 1024;
        if (!caching.hasUncachedA18Files()) {
          int answer = JOptionPane.showOptionDialog(null,
              "The WAV cache is currently using " + sizeMB
                  + " MB disk space and a cleanup is recommended. Perform cleanup?",
              "WAV Cache", JOptionPane.YES_NO_OPTION,
              JOptionPane.QUESTION_MESSAGE, null, null, JOptionPane.YES_OPTION);
          JOptionPane.showMessageDialog(null,
              "Not enough memory available for JVM. Please make sure your"
                  + " java command contains the argument -XMX512m (or more).");

          if (answer == JOptionPane.YES_OPTION) {
            ACMConfiguration.getInstance().getCurrentDB().getRepository().gc();
          }
        }
      }
      if (ACMConfiguration.getInstance().getCurrentDB().shouldPreCacheWav()) {
        caching.cacheNewA18Files();
      }

      application.resourceView.audioItemView.requestFocusInWindow();
  }

  public static class FilterState {
    private String previousFilterState = null;

    private String filterString;
    private List<Category> filterCategories = new ArrayList<>();
    private List<Locale> filterLanguages = new ArrayList<>();
    private Playlist selectedPlaylist;

    public synchronized String getFilterString() {
      return filterString;
    }

    public synchronized void setFilterString(String filterString) {
      this.filterString = filterString;
      updateResult();
    }

    public synchronized List<Category> getFilterCategories() {
      return filterCategories;
    }

    public synchronized void setFilterCategories(
        List<Category> filterCategories) {
      this.filterCategories = filterCategories;
      updateResult();
    }

    public synchronized List<Locale> getFilterLanguages() {
      return filterLanguages;
    }

    public synchronized void setFilterLanguages(List<Locale> filterLanguages) {
      this.filterLanguages = filterLanguages;
      updateResult();
    }

    public synchronized void setSelectedPlaylist(Playlist selectedPlaylist) {
      this.selectedPlaylist = selectedPlaylist;
      updateResult();
    }

    public synchronized Playlist getSelectedPlaylist() {
      return selectedPlaylist;
    }

    public void updateResult() {
      updateResult(false);
    }

    public void updateResult(boolean force) {
      if (!force && previousFilterState != null
          && previousFilterState.equals(this.toString())) {
        return;
      }

      previousFilterState = this.toString();

      final MetadataStore store = ACMConfiguration.getInstance().getCurrentDB()
          .getMetadataStore();
      final SearchResult result;

      if (selectedPlaylist == null) {
        result = store.search(filterString, filterCategories, filterLanguages);
      } else {
        result = store.search(filterString, selectedPlaylist);
      }

      // call UI back
      Runnable updateUI = new Runnable() {
        @Override
        public void run() {
          Application.getMessageService().pumpMessage(result);
        }
      };

      if (SwingUtilities.isEventDispatchThread()) {
        updateUI.run();
      } else {
        try {
          SwingUtilities.invokeAndWait(updateUI);
        } catch (InterruptedException e) {
          e.printStackTrace();
        } catch (InvocationTargetException e) {
          e.printStackTrace();
        }
      }
    }

    @Override
    public String toString() {
      StringBuilder builder = new StringBuilder();
      if (filterString != null) {
        builder.append("FS:").append(filterString);
        builder.append(",");
      }
      if (filterCategories != null && !filterCategories.isEmpty()) {
        for (Category cat : filterCategories) {
          builder.append("FC:").append(cat.getUuid());
          builder.append(",");
        }
      }
      if (filterLanguages != null && !filterLanguages.isEmpty()) {
        for (Locale lang : filterLanguages) {
          builder.append("FL:").append(lang.getLanguage()).append("-")
              .append(lang.getCountry());
          builder.append(",");
        }
      }
      if (selectedPlaylist != null) {
        builder.append("ST:").append(selectedPlaylist.getName());
        builder.append(",");
      }
      return builder.toString();
    }
  }

}
