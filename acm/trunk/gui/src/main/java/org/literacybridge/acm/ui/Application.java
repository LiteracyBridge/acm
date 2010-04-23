package org.literacybridge.acm.ui;

import java.awt.BorderLayout;

import javax.swing.JFrame;

import org.literacybridge.acm.ui.ResourceView.ResourceView;
import org.literacybridge.acm.ui.ResourceView.ToolbarView;
import org.literacybridge.acm.util.SimpleMessageService;

public class Application {

	// message pump
	private static SimpleMessageService instance = new SimpleMessageService();
	
	public static SimpleMessageService getMessageService() {
		return instance;
	};
	
	
	
	public static void main(String[] args) {
	    JFrame frame = new JFrame();
	    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

	    // toolbar view on top
	    ToolbarView toolbarView = new ToolbarView();
	    frame.getContentPane().add(toolbarView, BorderLayout.PAGE_START);
	    
	    // Resource view
	    ResourceView resourceView = new ResourceView();	    
	    frame.add(resourceView, BorderLayout.CENTER);
	  
	    frame.setSize(1000, 600);
	    frame.setVisible(true);
	}
	
}
