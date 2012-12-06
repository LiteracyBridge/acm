package org.literacybridge.acm.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import org.literacybridge.acm.config.Configuration;
import org.literacybridge.acm.gui.util.UIUtils;
import org.literacybridge.acm.utils.FileUtil;

public class ConfigurationQuestionary extends JDialog implements ActionListener {

	private final static String COMMAND_DONE = "DONE";
	private final static String COMMAND_USE_DEFAULTS = "USE_DEFAULTS";

	
	private final JPanel contentPanel = new JPanel();
	private JTextField userNameTF;
	private JTextField contactInfoTF;
	private JTextField databaseTF;
	private JTextField repositoryTF;

	private String userName;
	private String contactInfo;
	private String databaseFilePath;
	private String repositoryFilePath;
	
	private boolean useDefaultDirectories;
	
	private Configuration configuration;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		try {
			ConfigurationQuestionary dialog = new ConfigurationQuestionary();
			dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
			dialog.setVisible(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private boolean validateInput() {
		boolean valid = true;
		if (userName.equals("")) {
			valid = false;
			showErrorMessage("Please enter an User Name");
		} else if (contactInfo.equals("")) {
			valid = false;
			showErrorMessage("Please enter your contact info");			
		} else if (!useDefaultDirectories && !FileUtil.isValidDirectory(databaseFilePath)) {
			valid = false;
			showErrorMessage("The Database directory is not valid");		
		} else if (!useDefaultDirectories && !FileUtil.isValidDirectory(repositoryFilePath)) {
			valid = false;
			showErrorMessage("The Repository (Content) directory is not valid");
		}
		
		return valid;
	}
	

	private void readDataFromControls() {
		userName = userNameTF.getText();
		contactInfo = contactInfoTF.getText();
		databaseFilePath = databaseTF.getText();
		repositoryFilePath = repositoryTF.getText();
	}
	

	private void closeDialogAndStoreData() {
		configuration.setUserName(userName);
		configuration.setUserContactInfo(contactInfo);
		
		if (useDefaultDirectories) {
			configuration.setFallbackPaths();
		} else {
			configuration.setDatabaseDirectory(new File(databaseFilePath));
			configuration.setContentDirectory(new File(repositoryFilePath));			
		}
		
		configuration.storeConfiguration();
		
		setVisible(false);
	}
	
	@Override
	public void actionPerformed(ActionEvent actionEvent) {
		String actionCommand = actionEvent.getActionCommand();
		if (actionCommand == COMMAND_DONE) {
			
			readDataFromControls();
			// user can only close dialog if data are valid
			boolean valid = validateInput();
			if (valid) {
				closeDialogAndStoreData();
			}
		}	
		else if (actionCommand == COMMAND_USE_DEFAULTS) {
			JCheckBox checkBox = (JCheckBox) actionEvent.getSource();
			useDefaultDirectories = checkBox.isSelected();
			
			if (useDefaultDirectories) {
				// store the paths locally
				readDataFromControls();
				databaseTF.setText("");
				repositoryTF.setText("");
			} else {
				// restore old settings
				databaseTF.setText(databaseFilePath);
				repositoryTF.setText(repositoryFilePath);
			}
			
			databaseTF.setEditable(!useDefaultDirectories);
			databaseTF.setEnabled(!useDefaultDirectories);
			repositoryTF.setEditable(!useDefaultDirectories);
			repositoryTF.setEnabled(!useDefaultDirectories);
		}
	}
	
	
	public void initializeControls(Configuration configuration) {
		this.configuration = configuration;
		
		String userName = configuration.getUserNameOrNull();
		if (userName != null) {
			userNameTF.setText(userName);
		}
		
		String contactInfo = configuration.getUserContactInfoOrNull();
		if (contactInfo != null) {
			contactInfoTF.setText(contactInfo);
		}
		
		File databaseDirectory = configuration.getDatabaseDirectory();
		if (databaseDirectory != null) {
			databaseTF.setText(databaseDirectory.getAbsolutePath());
		}
	
		File repositoryDirectory = configuration.getContentDirectory();
		if (repositoryDirectory != null) {
			repositoryTF.setText(repositoryDirectory.getAbsolutePath());
		}		
	}
	
	
	private void showErrorMessage(String message) {
		
		JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.INFORMATION_MESSAGE);
	}
	
	/**
	 * Create the dialog.
	 */
	public ConfigurationQuestionary() {
		setResizable(false);
		setModalityType(ModalityType.APPLICATION_MODAL);
		setModal(true);
		setAlwaysOnTop(true);
		setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		setTitle("Please enter your configuration");
		setBounds(100, 100, 450, 255);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		GridBagLayout gbl_contentPanel = new GridBagLayout();
		gbl_contentPanel.columnWidths = new int[]{0, 0, 0, 0};
		gbl_contentPanel.rowHeights = new int[]{0, 0, 0, 0, 0, 0, 0};
		gbl_contentPanel.columnWeights = new double[]{0.0, 0.0, 1.0, Double.MIN_VALUE};
		gbl_contentPanel.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
		contentPanel.setLayout(gbl_contentPanel);
		{
			JLabel lblUserName = new JLabel("User Name:");
			lblUserName.setFont(new Font("Lucida Grande", Font.BOLD, 13));
			GridBagConstraints gbc_lblUserName = new GridBagConstraints();
			gbc_lblUserName.anchor = GridBagConstraints.EAST;
			gbc_lblUserName.insets = new Insets(0, 0, 5, 5);
			gbc_lblUserName.gridx = 0;
			gbc_lblUserName.gridy = 0;
			contentPanel.add(lblUserName, gbc_lblUserName);
		}
		{
			userNameTF = new JTextField();
			GridBagConstraints gbc_userNameTF = new GridBagConstraints();
			gbc_userNameTF.gridwidth = 2;
			gbc_userNameTF.insets = new Insets(0, 0, 5, 0);
			gbc_userNameTF.fill = GridBagConstraints.HORIZONTAL;
			gbc_userNameTF.gridx = 1;
			gbc_userNameTF.gridy = 0;
			contentPanel.add(userNameTF, gbc_userNameTF);
			userNameTF.setColumns(10);
		}
		{
			JLabel contactInfoLabel = new JLabel("Contact info (Phone):");
			contactInfoLabel.setFont(new Font("Lucida Grande", Font.BOLD, 13));
			GridBagConstraints gbc_contactInfoLabel = new GridBagConstraints();
			gbc_contactInfoLabel.anchor = GridBagConstraints.EAST;
			gbc_contactInfoLabel.insets = new Insets(0, 0, 5, 5);
			gbc_contactInfoLabel.gridx = 0;
			gbc_contactInfoLabel.gridy = 1;
			contentPanel.add(contactInfoLabel, gbc_contactInfoLabel);
		}
		{
			contactInfoTF = new JTextField();
			GridBagConstraints gbc_contactInfoTF = new GridBagConstraints();
			gbc_contactInfoTF.gridwidth = 2;
			gbc_contactInfoTF.insets = new Insets(0, 0, 5, 0);
			gbc_contactInfoTF.fill = GridBagConstraints.HORIZONTAL;
			gbc_contactInfoTF.gridx = 1;
			gbc_contactInfoTF.gridy = 1;
			contentPanel.add(contactInfoTF, gbc_contactInfoTF);
			contactInfoTF.setColumns(10);
		}
		{
			JLabel lblPathsToAcm = new JLabel("ACM directories (advanced):");
			lblPathsToAcm.setFont(new Font("Lucida Grande", Font.BOLD, 13));
			GridBagConstraints gbc_lblPathsToAcm = new GridBagConstraints();
			gbc_lblPathsToAcm.insets = new Insets(0, 0, 5, 5);
			gbc_lblPathsToAcm.anchor = GridBagConstraints.WEST;
			gbc_lblPathsToAcm.gridwidth = 2;
			gbc_lblPathsToAcm.gridx = 0;
			gbc_lblPathsToAcm.gridy = 3;
			contentPanel.add(lblPathsToAcm, gbc_lblPathsToAcm);
		}
		{
			JCheckBox useDefaultsCheckBox = new JCheckBox("use Defaults");
			GridBagConstraints gbc_useDefaultsCheckBox = new GridBagConstraints();
			gbc_useDefaultsCheckBox.anchor = GridBagConstraints.WEST;
			gbc_useDefaultsCheckBox.insets = new Insets(0, 0, 5, 0);
			gbc_useDefaultsCheckBox.gridx = 2;
			gbc_useDefaultsCheckBox.gridy = 3;
			contentPanel.add(useDefaultsCheckBox, gbc_useDefaultsCheckBox);
			useDefaultsCheckBox.setActionCommand(COMMAND_USE_DEFAULTS);
			useDefaultsCheckBox.addActionListener(this);
		}
		{
			JLabel databaseLabel = new JLabel("Database:");
			databaseLabel.setFont(new Font("Lucida Grande", Font.BOLD, 13));
			GridBagConstraints gbc_databaseLabel = new GridBagConstraints();
			gbc_databaseLabel.anchor = GridBagConstraints.EAST;
			gbc_databaseLabel.insets = new Insets(0, 0, 5, 5);
			gbc_databaseLabel.gridx = 0;
			gbc_databaseLabel.gridy = 4;
			contentPanel.add(databaseLabel, gbc_databaseLabel);
		}
		{
			databaseTF = new JTextField();
			GridBagConstraints gbc_databaseTF = new GridBagConstraints();
			gbc_databaseTF.gridwidth = 2;
			gbc_databaseTF.insets = new Insets(0, 0, 5, 0);
			gbc_databaseTF.fill = GridBagConstraints.HORIZONTAL;
			gbc_databaseTF.gridx = 1;
			gbc_databaseTF.gridy = 4;
			contentPanel.add(databaseTF, gbc_databaseTF);
			databaseTF.setColumns(10);
		}
		{
			JLabel lblNewLabel = new JLabel("Repository (Content):");
			lblNewLabel.setFont(new Font("Lucida Grande", Font.BOLD, 13));
			GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
			gbc_lblNewLabel.anchor = GridBagConstraints.EAST;
			gbc_lblNewLabel.insets = new Insets(0, 0, 0, 5);
			gbc_lblNewLabel.gridx = 0;
			gbc_lblNewLabel.gridy = 5;
			contentPanel.add(lblNewLabel, gbc_lblNewLabel);
		}
		{
			repositoryTF = new JTextField();
			GridBagConstraints gbc_repositoryTF = new GridBagConstraints();
			gbc_repositoryTF.gridwidth = 2;
			gbc_repositoryTF.fill = GridBagConstraints.HORIZONTAL;
			gbc_repositoryTF.gridx = 1;
			gbc_repositoryTF.gridy = 5;
			contentPanel.add(repositoryTF, gbc_repositoryTF);
			repositoryTF.setColumns(10);
		}
		{
			JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
			{
				JButton doneButton = new JButton("Done");
				doneButton.setActionCommand(COMMAND_DONE);
				doneButton.addActionListener(this);
				buttonPane.add(doneButton);
			}
		}
		
		UIUtils.centerOnScreen(this, true);
	}
}
