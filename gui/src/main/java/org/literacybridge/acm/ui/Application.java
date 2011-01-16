package org.literacybridge.acm.ui;

import java.awt.BorderLayout;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Locale;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.jdesktop.swingx.JXFrame;
import org.literacybridge.acm.api.IDataRequestResult;
import org.literacybridge.acm.config.Configuration;
import org.literacybridge.acm.core.DataRequestService;
import org.literacybridge.acm.db.PersistentCategory;
import org.literacybridge.acm.device.FileSystemMonitor;
import org.literacybridge.acm.device.LiteracyBridgeTalkingBookRecognizer;
import org.literacybridge.acm.playerAPI.SimpleSoundPlayer;
import org.literacybridge.acm.resourcebundle.LabelProvider;
import org.literacybridge.acm.ui.ResourceView.ResourceView;
import org.literacybridge.acm.ui.ResourceView.ToolbarView;
import org.literacybridge.acm.util.SimpleMessageService;
import org.literacybridge.acm.util.language.LanguageUtil;

public class Application extends JXFrame {

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
		// set look & feel
		try {
		    UIManager.setLookAndFeel("com.seaglasslookandfeel.SeaGlassLookAndFeel");
		} catch (Exception e) {
		    e.printStackTrace();
		}
		
		setTitle(LabelProvider.getLabel("TITLE_LITERACYBRIDGE_ACM", LanguageUtil.getUILanguage()));
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
		application = new Application();
		
		// initialize config and generate random ID for this acm instance
		Configuration.getConfiguration();
		
		application.setSize(1000, 750);
		application.setVisible(true);
	}	
	
	public static class FilterState {
		private String filterString;
		private List<PersistentCategory> filterCategories;
		private List<Locale> languages;
		
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
		
		public synchronized List<Locale> getFilterLanguages() {
			return languages;
		}
		public synchronized void setFilterLanguages(List<Locale> filterLanguages) {
			this.languages = filterLanguages;
			updateResult();
		}
		
		public void updateResult() {
			
			// TODO: Integrate languages in filter
			final IDataRequestResult result = DataRequestService.getInstance().getData(
					LanguageUtil.getUserChoosenLanguage(), 
					filterString, filterCategories);

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
	}
	
}
