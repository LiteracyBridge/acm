package org.literacybridge.acm.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.SplashScreen;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFrame;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.jdesktop.swingx.JXFrame;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.literacybridge.acm.Constants;
import org.literacybridge.acm.api.IDataRequestResult;
import org.literacybridge.acm.config.Configuration;
import org.literacybridge.acm.config.ControlAccess;
import org.literacybridge.acm.core.DataRequestService;
import org.literacybridge.acm.db.Persistence;
import org.literacybridge.acm.db.PersistentCategory;
import org.literacybridge.acm.db.PersistentLocale;
import org.literacybridge.acm.db.PersistentTag;
import org.literacybridge.acm.device.FileSystemMonitor;
import org.literacybridge.acm.device.LiteracyBridgeTalkingBookRecognizer;
import org.literacybridge.acm.gui.ResourceView.ResourceView;
import org.literacybridge.acm.gui.ResourceView.ToolbarView;
import org.literacybridge.acm.gui.playerAPI.SimpleSoundPlayer;
import org.literacybridge.acm.gui.resourcebundle.LabelProvider;
import org.literacybridge.acm.gui.util.SimpleMessageService;
import org.literacybridge.acm.gui.util.language.LanguageUtil;
import org.literacybridge.acm.repository.WavCaching;

public class Application extends JXFrame {
	private static final Logger LOG = Logger.getLogger(Application.class.getName());
	
	private static final long serialVersionUID = -7011153239978361786L;

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
	
	private Application() throws IOException {
		super();
		this.backgroundColor = getBackground();
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);	    

		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				try {
					if (!ControlAccess.isSandbox())
						ControlAccess.updateDB();
				}
			    catch(Exception e1) {
			    	e1.printStackTrace();
			    } 
			}
		});

		String title = new String(LabelProvider.getLabel("TITLE_LITERACYBRIDGE_ACM", LanguageUtil.getUILanguage())); 
		title += " (" + Constants.ACM_VERSION + ")";
		if (Configuration.getACMname() != null)
			title += "                   " + Configuration.getACMname();
		else if (Configuration.getSharedACMname() != null)
			title += "                   " + Configuration.getSharedACMname();			
		if (ControlAccess.isACMReadOnly())
			title += "                   * READ ONLY *";
		if (ControlAccess.isSandbox())
			title += "                   CHANGES WILL *NOT* BE SAVED!   ";

		setTitle(title);
		// toolbar view on top
	    ResourceView resourceView = new ResourceView();	    
	    ToolbarView toolbarView = new ToolbarView(resourceView.audioItemView);
	    add(toolbarView, BorderLayout.PAGE_START);
	    add(resourceView, BorderLayout.CENTER);
	    	    
	    statusBar = new ACMStatusBar();
	    setStatusBar(statusBar);
	    taskManager = new BackgroundTaskManager(statusBar);
	    
	    // starts file system monitor after UI has been initialized
		fileSystemMonitor.addDeviceRecognizer(new LiteracyBridgeTalkingBookRecognizer());	
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
	
	public BackgroundTaskManager getTaskManager() {
		return taskManager;
	}
	
	public SimpleSoundPlayer getSoundPlayer() {
		return this.player;
	}
	
	public static void main(String[] args) throws IOException {
		System.out.println("starting main()");
		CommandLineParams params = new CommandLineParams();
		CmdLineParser parser = new CmdLineParser(params);
		try {
			parser.parseArgument(args);
		} catch (CmdLineException e) {
		    System.err.println(e.getMessage());
		    System.err.println("java -cp acm.jar;lib/* org.literacybridge.acm.gui.Application [options...]");
		    parser.printUsage(System.err);
		    return;
		}
		startUp(params);
	}
	
	public static void startUp(CommandLineParams params) throws IOException {
//		String dbDirName = null, repositoryDirName= null;
		// initialize config and generate random ID for this acm instance
		Configuration.init(params);
		
		boolean showUI = !params.disableUI;
		
		// init database
		try {
			Persistence.initialize();
			// DB migration if necessary
			System.out.print("Updating database... ");
			Persistence.maybeRunMigration();
			System.out.println("done.");
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		SplashScreen splash = null;
		
		if (showUI) {
			// set look & feel
			
			// Not sure why, but making this call before setting the seaglass look and feel
			// prevents an UnsatisfiedLinkError to be thrown
			final LookAndFeel defaultLAF = UIManager.getLookAndFeel();
			try {
			    UIManager.setLookAndFeel("com.seaglasslookandfeel.SeaGlassLookAndFeel");
			} catch (Exception e) {
				try {
					LOG.log(Level.WARNING, "Unable to set look and feel.", e);
					UIManager.setLookAndFeel(defaultLAF);
				} catch (Exception e1) {
					LOG.log(Level.WARNING, "Unable to set look and feel.", e1);
				}
			}
			
			splash = SplashScreen.getSplashScreen();
		}

		
		application = new Application();
		
		if (showUI) {
			application.setSize(1000, 750);
			
			if (splash != null) {
				splash.close();
			}
			application.setVisible(true);
			application.toFront();
			
            LOG.log(Level.INFO, "ACM successfully started.");
			new WavCaching().cacheNewA18Files();
		}
	}	
	
	public static class FilterState {
		private String previousFilterState = null;
		
		private String filterString;
		private List<PersistentCategory> filterCategories;
		private List<PersistentLocale> filterLanguages;
		private PersistentTag selectedTag;
		
		public synchronized String getFilterString() {
			return filterString;
		}
		public synchronized void setFilterString(String filterString) {
			this.filterString = filterString;
			updateResult();
		}
		public synchronized List<PersistentCategory> getFilterCategories() {
			return filterCategories;
		}
		public synchronized void setFilterCategories(
				List<PersistentCategory> filterCategories) {
			this.filterCategories = filterCategories;
			updateResult();
		}
		
		public synchronized List<PersistentLocale> getFilterLanguages() {
			return filterLanguages;
		}
		public synchronized void setFilterLanguages(List<PersistentLocale> filterLanguages) {
			this.filterLanguages = filterLanguages;
			updateResult();
		}
		
		public synchronized void setSelectedTag(PersistentTag selectedTag) {
			this.selectedTag = selectedTag;
			updateResult();
		}
		
		public synchronized PersistentTag getSelectedTag() {
			return selectedTag;
		}
		
		public void updateResult() {
			updateResult(false);
		}
		
		public void updateResult(boolean force) {
			if (!force && previousFilterState != null && previousFilterState.equals(this.toString())) {
				return;
			}
			
			previousFilterState = this.toString();
			
			final IDataRequestResult result;
			
			if (selectedTag == null) {
				result = DataRequestService.getInstance().getData(
						LanguageUtil.getUserChoosenLanguage(), 
						filterString, filterCategories, filterLanguages);
			} else {
				result = DataRequestService.getInstance().getData(
						LanguageUtil.getUserChoosenLanguage(), 
						filterString, selectedTag);				
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
		
		@Override public String toString() {
			StringBuilder builder = new StringBuilder();
			if (filterString != null) {
				builder.append("FS:").append(filterString);
				builder.append(",");
			}
			if (filterCategories != null && !filterCategories.isEmpty()) {
				for (PersistentCategory cat : filterCategories) {
					builder.append("FC:").append(cat.getUuid());
					builder.append(",");					
				}
			}
			if (filterLanguages != null && !filterLanguages.isEmpty()) {
				for (PersistentLocale lang : filterLanguages) {
					builder.append("FL:").append(lang.getLanguage()).append("-").append(lang.getCountry());
					builder.append(",");					
				}
			}
			if (selectedTag != null) {
				builder.append("ST:").append(selectedTag.getName());
				builder.append(",");
			}			
			return builder.toString();
		}
	}	
	
}
