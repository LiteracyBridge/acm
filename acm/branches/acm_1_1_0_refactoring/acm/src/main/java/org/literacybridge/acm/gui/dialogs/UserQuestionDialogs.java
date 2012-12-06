package org.literacybridge.acm.gui.dialogs;

import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import org.literacybridge.acm.config.Configuration;
import org.literacybridge.acm.utils.FileUtil;

public class UserQuestionDialogs {

	private Configuration configuration;
	
	public UserQuestionDialogs(Configuration configuration) {
		this.configuration = configuration;
	}
	
	public void promptUserForMissingDetails() {
		
		// User Name
		if (configuration.getUserNameOrNull() == null) {
			String userName = promptUserForUserName();
			configuration.setUserName(userName);
		}
		
		// Contact Info
		if (configuration.getUserContactInfoOrNull() == null) {
			String contactInfo = promptUserForContactInfo();
			configuration.setUserContactInfo(contactInfo);
		}
		
		// use Global Share as default if available
		File globalShareDirectory = null;
		if (configuration.HasValidGlobalShareDirectory()) {
			globalShareDirectory = configuration.getGlobalShareDirectory();					
		}
		
		// Database directory
		if (configuration.getDatabaseDirectory() == null) {
			File directory = promptUserForDirectory("Select Database directory.", globalShareDirectory);
			configuration.setDatabaseDirectory(directory);
		}
		
		//  Repository (Content)
		if (configuration.getContentDirectory() == null) {
			File directory = promptUserForDirectory("Select Repository (Content) directory", globalShareDirectory);
			configuration.setContentDirectory(directory);
		}
	}
	
	private File promptUserForDirectory(String title, File rootDirectory) {
		JFileChooser fc = null;
		if (FileUtil.isValidDirectory(rootDirectory)) {
			fc = new JFileChooser(rootDirectory.getAbsolutePath());
		} else {
			fc = new JFileChooser();
		}
		
		fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		fc.setDialogTitle(title);
		int returnVal = fc.showOpenDialog(null);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			return fc.getSelectedFile();
		}
		
		return null;
	}
	
	private String promptUserForUserName() {
		return (String)JOptionPane.showInputDialog(null, "Enter Username:", "Missing Username", JOptionPane.PLAIN_MESSAGE);
	}
	
	private String promptUserForContactInfo() {
		return (String)JOptionPane.showInputDialog(null, "Enter Phone #:", "Missing Contact Info", JOptionPane.PLAIN_MESSAGE);
	}
}
	




