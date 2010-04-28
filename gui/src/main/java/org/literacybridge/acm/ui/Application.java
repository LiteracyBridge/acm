package org.literacybridge.acm.ui;

import java.awt.BorderLayout;

import javax.swing.JFrame;
import javax.swing.UIManager;

import org.literacybridge.acm.device.FileSystemMonitor;
import org.literacybridge.acm.device.LiteracyBridgeTalkingBookRecognizer;
import org.literacybridge.acm.ui.ResourceView.ResourceView;
import org.literacybridge.acm.ui.ResourceView.ToolbarView;
import org.literacybridge.acm.util.SimpleMessageService;

public class Application extends JFrame {


	
	
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
}
