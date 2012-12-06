package org.literacybridge.acm.gui;

import java.awt.BorderLayout;
import java.awt.SplashScreen;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFrame;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;

import org.jdesktop.swingx.JXFrame;
import org.literacybridge.acm.config.Configuration;
import org.literacybridge.acm.device.FileSystemMonitor;
import org.literacybridge.acm.device.LiteracyBridgeTalkingBookRecognizer;
import org.literacybridge.acm.gui.ResourceView.ResourceView;
import org.literacybridge.acm.gui.ResourceView.ToolbarView;
import org.literacybridge.acm.gui.dialogs.ConfigurationQuestionary;
import org.literacybridge.acm.gui.playerAPI.SimpleSoundPlayer;
import org.literacybridge.acm.gui.resourcebundle.LabelProvider;
import org.literacybridge.acm.gui.util.SimpleMessageService;
import org.literacybridge.acm.gui.util.language.LanguageUtil;
import org.literacybridge.acm.utils.CommandLineArguments;

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
		setCloseAction();
		
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
	
	private void setCloseAction() {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);	    
		addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosing(WindowEvent e) {
				// e.g. write properties file
				System.out.println("Save configuration...");
				Configuration.getConfiguration().storeConfiguration();
				super.windowClosing(e);
			}
		});
	}
	
	public SimpleSoundPlayer getSoundPlayer() {
		return this.player;
	}
	
	public static void main(String[] args) throws IOException {
				
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
		
		// TODO: replace by an advanced command line parser...
		CommandLineArguments commandLineArguments = CommandLineArguments.analyseCommandLineArguments(args);
			
		Configuration configuration = Configuration.getConfiguration();
		
		// initialize configuration and generate random ID for this ACM instance
		configuration.init(commandLineArguments.databaseDirectoryPath, commandLineArguments.repositoryDirectoryPath);
		
		// configuration completed, so check for details and request missing ones from user

		ConfigurationQuestionary dialog = new ConfigurationQuestionary();
		dialog.initializeControls(configuration);
		dialog.setVisible(true);
		
		
		final SplashScreen splash = SplashScreen.getSplashScreen();
		application = new Application();
		
		application.setSize(1000, 750);
		
		if (splash != null) {
			splash.close();
		}
		

		application.setVisible(true);
		application.toFront();
		
		
		
		LOG.log(Level.INFO, "ACM successfully started.");
		Configuration.getConfiguration().cacheNewA18Files();
	}	
	
	
}
