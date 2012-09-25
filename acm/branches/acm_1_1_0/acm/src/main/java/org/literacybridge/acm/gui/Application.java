package org.literacybridge.acm.gui;

import java.util.logging.Level;
import java.util.logging.Logger;

import java.awt.BorderLayout;
import java.awt.SplashScreen;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

import org.jdesktop.swingx.JXFrame;
import org.literacybridge.acm.api.IDataRequestResult;
import org.literacybridge.acm.config.Configuration;
import org.literacybridge.acm.core.DataRequestService;
import org.literacybridge.acm.db.PersistentCategory;
import org.literacybridge.acm.db.PersistentLocale;
import org.literacybridge.acm.db.PersistentTag;
import org.literacybridge.acm.device.FileSystemMonitor;
import org.literacybridge.acm.device.LiteracyBridgeTalkingBookRecognizer;
import org.literacybridge.acm.gui.playerAPI.SimpleSoundPlayer;
import org.literacybridge.acm.gui.resourcebundle.LabelProvider;
import org.literacybridge.acm.gui.ResourceView.ResourceView;
import org.literacybridge.acm.gui.ResourceView.ToolbarView;
import org.literacybridge.acm.gui.util.SimpleMessageService;
import org.literacybridge.acm.gui.util.language.LanguageUtil;

public class Application extends JXFrame {
	private static final Logger LOG = Logger.getLogger(Application.class.getName());
	
	private static final long serialVersionUID = -7011153239978361786L;

	// message pump
	private static SimpleMessageService simpleMessageService = new SimpleMessageService();
	
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
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);	    
		
		String title = new String(LabelProvider.getLabel("TITLE_LITERACYBRIDGE_ACM", LanguageUtil.getUILanguage())); 
		if (Configuration.getConfiguration().isACMReadOnly())
			title += " * READ ONLY *";

		setTitle(title);
		// toolbar view on top
	    ResourceView resourceView = new ResourceView();	    
	    ToolbarView toolbarView = new ToolbarView(resourceView.audioItemView);
	    add(toolbarView, BorderLayout.PAGE_START);
	    add(resourceView, BorderLayout.CENTER);
	    
	    // starts file system monitor after UI has been initialized
		fileSystemMonitor.addDeviceRecognizer(new LiteracyBridgeTalkingBookRecognizer());	
		fileSystemMonitor.start();
	}
	
	public SimpleSoundPlayer getSoundPlayer() {
		return this.player;
	}
	
	public static void main(String[] args) throws IOException {
		
		String dbDirName = null, repositoryDirName= null;
		System.out.println("starting main()");
		if (args.length == 2) {
			System.out.println("db path = " + args[0]);
			System.out.println("repository path = " + args[1]);
			dbDirName = args[0];
			repositoryDirName = args[1];
		} else if (args.length == 0) {
			System.out.println("To override config.properties, add argument with db path followed by argument for repository path.");
		}
		// initialize config and generate random ID for this acm instance
		Configuration.init(dbDirName, repositoryDirName);
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
		
		final SplashScreen splash = SplashScreen.getSplashScreen();
		application = new Application();
		
		application.setSize(1000, 750);
		
		if (splash != null) {
			splash.close();
		}
		application.setVisible(true);
		application.toFront();
		
		LOG.log(Level.INFO, "ACM successfully started.");
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
