package org.literacybridge.acm.ui;

import java.awt.BorderLayout;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.UIManager;

import org.jdesktop.swingx.JXFrame;
import org.literacybridge.acm.api.IDataRequestResult;
import org.literacybridge.acm.core.DataRequestService;
import org.literacybridge.acm.db.PersistentCategory;
import org.literacybridge.acm.device.FileSystemMonitor;
import org.literacybridge.acm.device.LiteracyBridgeTalkingBookRecognizer;
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
	public static Application application = new Application();
	
	public static Application getApplication() {
		return application;
	}
	
	private Application() {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);	    
		// set look & feel
		try {
		    UIManager.setLookAndFeel("com.seaglasslookandfeel.SeaGlassLookAndFeel");
		} catch (Exception e) {
		    e.printStackTrace();
		}
		
		
		setTitle("Literacy Bridge - Talking Book Management");
		// toolbar view on top
	    ToolbarView toolbarView = new ToolbarView();
	    getContentPane().add(toolbarView, BorderLayout.PAGE_START);
	    // Resource view
	    ResourceView resourceView = new ResourceView();	    
	    add(resourceView, BorderLayout.CENTER);
	    
	    // starts file system monitor after UI has been initialized
		fileSystemMonitor.addDeviceRecognizer(new LiteracyBridgeTalkingBookRecognizer());	
		fileSystemMonitor.start();
	}
	
	
	public static void main(String[] args) {
		Application app = Application.getApplication();
		app.setSize(1000, 600);
		app.setVisible(true);
	}	
	
	public static class FilterState {
		private String filterString;
		private List<PersistentCategory> filterCategories;
		
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
		
		private void updateResult() {
			IDataRequestResult result = DataRequestService.getInstance().getData(
					LanguageUtil.getUserChoosenLanguage(), 
					filterString, filterCategories);
			Application.getMessageService().pumpMessage(result);	
		}
	}
}
