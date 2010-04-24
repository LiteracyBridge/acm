package org.literacybridge.acm.ui;

import java.awt.BorderLayout;

import javax.swing.JFrame;

import org.literacybridge.acm.ui.ResourceView.ResourceView;
import org.literacybridge.acm.ui.ResourceView.ToolbarView;
import org.literacybridge.acm.util.SimpleMessageService;

public class Application extends JFrame {

	// message pump
	private static SimpleMessageService instance = new SimpleMessageService();
	
	public static SimpleMessageService getMessageService() {
		return instance;
	};

	public static Application application = new Application();
	
	private Application() {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		// toolbar view on top
	    ToolbarView toolbarView = new ToolbarView();
	    getContentPane().add(toolbarView, BorderLayout.PAGE_START);
	    
	    // Resource view
	    ResourceView resourceView = new ResourceView();	    
	    add(resourceView, BorderLayout.CENTER);
	}
	
	public static Application getApplication() {
		return application;
	}
	
	
	
	public static void main(String[] args) {
		Application app = Application.getApplication();
		app.setSize(1000, 600);
		app.setVisible(true);
	}	
}
