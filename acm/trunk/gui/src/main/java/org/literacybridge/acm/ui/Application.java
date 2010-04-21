package org.literacybridge.acm.ui;

import java.awt.BorderLayout;

import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;

import org.literacybridge.acm.ui.ResourceView.ResourceView;
import org.literacybridge.acm.ui.ResourceView.ToolbarView;
import org.literacybridge.acm.ui.player.PlayerUI;
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

	    JDesktopPane desktop = new JDesktopPane();

	    
	    ResourceView resourceView = new ResourceView();
	    JInternalFrame test = new JInternalFrame("Resource", true, true, true, true);
	    test.add(resourceView);
	    test.setBounds(25, 25, 400, 300);
	    test.setVisible(true);
	    desktop.add(test);

	    
	    ToolbarView playerUI = new ToolbarView();
	    JInternalFrame playerIFrame = new JInternalFrame("Player", true, true, true, true);
	    playerIFrame.add(playerUI);
	    playerIFrame.setBounds(450, 25, 350, 130);
	    playerIFrame.setVisible(true);
	    desktop.add(playerIFrame);    
	    
	    
	    desktop.setVisible(true);
	    
	    
	    frame.add(desktop, BorderLayout.CENTER);
	    frame.setSize(1000, 600);
	    frame.setVisible(true);
	}
	
}
