package org.literacybridge.acm.audioconverter.gui;

//
// - Entry point for the java wrapper
// org.literacybridge.audioconverter.gui.AudioConverter

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.literacybridge.acm.audioconverter.converters.A18ToMP3Converter;
import org.literacybridge.acm.audioconverter.converters.A18ToWavConverter;
import org.literacybridge.acm.audioconverter.converters.AnyToA18Converter;
import org.literacybridge.acm.audioconverter.converters.BaseAudioConverter;
import org.literacybridge.acm.audioconverter.converters.BaseAudioConverter.ConversionException;
import org.literacybridge.acm.audioconverter.gui.fileView.DataModel;
import org.literacybridge.acm.audioconverter.gui.fileView.FileTableModel;
import org.literacybridge.acm.audioconverter.gui.fileView.DataModel.FileInfo;

class WindowEventHandler extends WindowAdapter {
	  public void windowClosing(WindowEvent evt) {
		  
		((AudioConverter) evt.getSource()).saveProgramSettings();
	    System.exit(0);
	  }
	}


public class AudioConverter extends JFrame implements ActionListener,
		DocumentListener {

	// serial number
	private static final long serialVersionUID = -1515229785484369683L;
	
	
	// options dialog
	OptionsDialog mOptionsDialog;
	
	// buttons
	JButton sourceButton, targetButton, convertButton, optionsButton, detailsButton, checkAllButton, uncheckAllButton;
	// text fields
	JTextField sourceDir, targetDir;
	// File table
	FileTableModel fileTableModel;
	JTable sourceFileTable;
	// path to the directories
	String sourceDirPath, targetDirPath;
	// labels
	JLabel sourceLabel, targetLabel, titleLabel, converionDirectionLabel;
	// panels
	JComponent mainPanel, detailsPanel;
	// progress bar
	JProgressBar progressBar;
	// details panel is shown
	boolean detailsVisible = false; // false as default
	// result text fields
	JTextArea okFilesTF, badFilesTF;
	JScrollPane okScrollPane, badScrollPane;
	// define the conversion direction
	JComboBox convertCB;
	// buffer proceeded file names
	StringBuffer okFilesBuffer, badFilesBuffer;
	
	JCheckBox overwriteCheckBox;

	// conversion
	DataModel fileModel = null;
	BaseAudioConverter currentConverter = null;
	FileTableModel fileTableMode = null;
	
	// Properties
	Properties programSettings = new Properties();
	String SourcePath;
	String DestinationPath;
	
	static final String SettingsFile = "AudioConverter.props";
	
	// converters
	private final List<BaseAudioConverter> converters = new ArrayList<BaseAudioConverter>();
	
	private static final List<Class<? extends BaseAudioConverter>> ConverterClasses = new ArrayList<Class<? extends BaseAudioConverter>>();
	static {
		ConverterClasses.add(A18ToWavConverter.class);
		ConverterClasses.add(AnyToA18Converter.class);
		ConverterClasses.add(A18ToMP3Converter.class);
	}
	

	
	public AudioConverter() {
		// initialize JFrame
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setTitle("Literacy Bridge Audio Converter");

		// load available converters
		loadConverters();

		
		buildControls(false);
		setLocation(200, 200);
		setResizable(false);
		setVisible(true);
		
		// load program Settings from props file
		loadProgramSettings();
		
		this.addWindowListener(new WindowEventHandler());
		// this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		
		
	}
	
	private int loadConverters() {
		for (Class<? extends BaseAudioConverter> clazz : ConverterClasses) {
			try {
				BaseAudioConverter instance = clazz.newInstance();
				instance.validateConverter();
				this.converters.add(instance);
			} catch (Exception e) {
				JOptionPane.showMessageDialog(
						this,
						"Converter " + clazz.getName() + " could not be loaded.",
						"Converter initializatoin problem",
						JOptionPane.ERROR_MESSAGE, null);
			}
		}
		return this.converters.size();
	}
	
	private void loadProgramSettings(){
		
		// Load program settings from AudioConverter.props
		try
		{
			 programSettings.load(new FileInputStream(SettingsFile));
			 
		 if (programSettings.getProperty("SourcePath") != null) {
			 this.sourceDirPath = programSettings.getProperty("SourcePath");
			 
			 // Update source text field
			 sourceDir.setText(this.sourceDirPath);
			 
		 }
		 if (programSettings.getProperty("TargetPath") != null) {
			 this.targetDirPath = programSettings.getProperty("TargetPath");	
			 
			 // Update target text field
			 targetDir.setText(this.targetDirPath);
		 }	
			
		}
		catch (Exception e) {
		      // TODO: Create file if not existing
		}

	}
	
	public void saveProgramSettings(){
		
		// Save program settings in AudioConverter.props
		try
		{
			boolean savingRequired = false;
			
			if (this.sourceDirPath != null) {
				programSettings.setProperty("SourcePath", sourceDirPath);
				savingRequired = true;
			}
			if (this.targetDirPath != null) {
				programSettings.setProperty("TargetPath", targetDirPath);
				savingRequired = true;
			}
		 
			if (savingRequired){
			 	FileOutputStream out = new FileOutputStream(SettingsFile);
				programSettings.store(out, "");
				out.close(); 
			}
		}
		catch (Exception e) {
		      // nothing
		}

	}
	
	private void buildControls(boolean bShowDetails) {
		getContentPane().removeAll();
		getContentPane().setLayout(new GridBagLayout());

		// main panel (for the center)	
		mainPanel = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		// globals
		int row 	= 0; // current row number

		Insets ins  = new Insets(2, 2, 2, 2); // padding
		gbc.insets 	= ins;
		gbc.fill 	= GridBagConstraints.BOTH; // resize only horizontal
		gbc.gridy 	= row;
			
		
		/*
		 *  title message
		 */
		titleLabel = new JLabel("Please choose directories and conversion format:");
		titleLabel.setHorizontalTextPosition(JLabel.CENTER);
		titleLabel.setForeground(Color.RED);
		gbc.gridx 	= 0;
		gbc.gridwidth = 5;
		mainPanel.add(titleLabel, gbc);
		
		
		gbc.gridy = ++row;
		
		/*
		 * controls of source directory
		 */
		sourceLabel = new JLabel("Source dir: ");
		gbc.gridx 	= 0;
		gbc.gridwidth = 1;
		mainPanel.add(sourceLabel, gbc);

		sourceDir = new JTextField();
		sourceDir.setEditable(false);
		gbc.gridx 	= 1;
		gbc.gridwidth = 3;
		sourceDir.setPreferredSize(new Dimension(300, sourceDir.getHeight()));		 
		mainPanel.add(sourceDir, gbc);
		sourceDir.getDocument().addDocumentListener(this);
		if (sourceDirPath != null) sourceDir.setText(sourceDirPath);

		sourceButton = new JButton("Select...");
		gbc.gridx 	 = 4;
		gbc.gridwidth  = 1;
		mainPanel.add(sourceButton, gbc);
		sourceButton.addActionListener(this);

		
		gbc.gridy = ++row;
		
		/*
		 * controls for the conversion direction
		 */
		converionDirectionLabel = new JLabel("Format: ");
		gbc.gridx 	= 0;
		gbc.gridwidth = 1;
		mainPanel.add(converionDirectionLabel, gbc);

		if (convertCB == null) {
			convertCB = createFormatComboBox();
			convertCB.addActionListener(this);		
		}
		gbc.gridx 	= 1;
		gbc.gridwidth = 3;
		mainPanel.add(convertCB, gbc);
		gbc.insets 	= ins;
		
		gbc.gridy = ++row;		
		
		/*
		 * grid for source files
		 */		
		if (sourceFileTable == null) {
			sourceFileTable = createFileTable();	// create customized table
		}
		JScrollPane tmp = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		tmp.getViewport().setView(sourceFileTable);
		tmp.setPreferredSize(new Dimension(375, 200));
		gbc.gridx 	 = 0;
		gbc.gridwidth  = 4;
		int height = 4;
		gbc.gridheight = height;
		mainPanel.add(tmp, gbc);
		// All button
		checkAllButton = new JButton("All");
		checkAllButton.addActionListener(this);
		gbc.gridx 	 = 4;
		gbc.gridwidth  = 1;
		gbc.gridheight = 1;
		mainPanel.add(checkAllButton, gbc);
		// All button
		uncheckAllButton = new JButton("None");
		uncheckAllButton.addActionListener(this);
		gbc.gridx = 4;
		gbc.gridy = row+1;
		gbc.gridwidth  = 1;
		mainPanel.add(uncheckAllButton, gbc);
		
		row += height;
		gbc.gridy = ++row;
		
		/*
		 * controls of target directory
		 */
		targetLabel = new JLabel("Target dir: ");
		gbc.gridx 		= 0;
		gbc.gridwidth	= 1;
		mainPanel.add(targetLabel, gbc);

		targetDir = new JTextField();
		targetDir.setPreferredSize(new Dimension(200, 10));
		targetDir.setEditable(false);
		gbc.gridx 		= 1;
		gbc.gridwidth	= 3;		
		mainPanel.add(targetDir, gbc);
		targetDir.getDocument().addDocumentListener(this);
		if (targetDirPath != null) targetDir.setText(targetDirPath);

		targetButton = new JButton("Select...");
		gbc.gridx = 4;
		gbc.gridwidth = 1;
		mainPanel.add(targetButton, gbc);
		targetButton.addActionListener(this);

		
		gbc.gridy = ++row;
		
		overwriteCheckBox = new JCheckBox("Overwrite files", false);
		gbc.gridx = 0;
		gbc.gridwidth = 5;
		mainPanel.add(overwriteCheckBox, gbc);
		
		gbc.gridy = ++row;
		
		/*
		 * progress bar 
		 */
		progressBar = new JProgressBar();
		progressBar.setStringPainted(true);
		gbc.gridx 	= 0;
		gbc.gridwidth = 5;
		mainPanel.add(progressBar, gbc);

		
		gbc.gridy = ++row;
		
		/*
		 * buttons 
		 */
	
		detailsButton = new JButton("Details");
		detailsButton.setEnabled(true);
		detailsButton.addActionListener(this);
		JPanel detailsPanel = new JPanel();	
		detailsPanel.add(detailsButton);
		gbc.gridx 	= 0;
		gbc.gridwidth = 1;
		mainPanel.add(detailsPanel, gbc);
		
		
		optionsButton = new JButton("Options");
		optionsButton.setEnabled(true);
		optionsButton.addActionListener(this);
				
		convertButton = new JButton("Convert");
		convertButton.addActionListener(this);
		convertButton.setEnabled(false);

		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING)); 		
		buttonPanel.add(optionsButton);
		buttonPanel.add(convertButton);		
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));		
		gbc.gridx 	= 1;
		gbc.gridwidth = 4;				
		mainPanel.add(buttonPanel, gbc);

		
		// Show details at bottom of JFrame
		if (bShowDetails) {
			buildDetailsPanel();
		}
					
		// add Panels to parent frame
		organizePanels();
		// enable children after everything is initialized
		enableChildren();
	}
	
	private void organizePanels() {

		// add main panel to parent
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill 	= GridBagConstraints.HORIZONTAL; // resize only horizontal
		gbc.gridx = 0; 
		gbc.gridy = 0;
		gbc.weightx = 0;
		gbc.weighty = 0;
		getContentPane().add(mainPanel, gbc);
		
		// add detail panel to parent
		if (detailsVisible) {
			gbc.gridx = 0; 
			gbc.gridy = 1;
			gbc.weighty = 1.0;
			gbc.fill 	= GridBagConstraints.BOTH; // resize
			getContentPane().add(detailsPanel, gbc);
		}

		pack();	
	}
	
	private JTable createFileTable() {
	    fileTableMode = new FileTableModel();
		JTable table = new JTable(fileTableMode);
		// most space for the file names
		table.getColumnModel().getColumn(0).setMaxWidth(60);
		table.getColumnModel().getColumn(2).setMaxWidth(60);
		table.getColumnModel().getColumn(3).setMaxWidth(60);
		return table;
	}
	
	private JComboBox createFormatComboBox() {
		JComboBox tmp = new JComboBox();
		// add empty item as default
		tmp.addItem(makeObj("Select conversion direction"));
		for (BaseAudioConverter c : this.converters) {
			tmp.addItem(makeObj(c.getShortDescription()));
		}
		tmp.setSelectedIndex(0); // default
		return tmp;
	}
	
	private Object makeObj(final String item)  {
		return new Object() { public String toString() { return item; } };
	}	
	
	private void buildDetailsPanel() {
		detailsPanel = new JPanel(new GridBagLayout());
		detailsPanel.setBorder(new TitledBorder("Conversion Status"));

		GridBagConstraints gbc = new GridBagConstraints();
		int row 	= 0; // current row number
		gbc.gridx 	= 0;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.fill 	= GridBagConstraints.BOTH;
		gbc.insets  = new Insets(2, 2, 2, 2); 

		JLabel okLabel = new JLabel("Success:");
		gbc.gridy 	= row;
		detailsPanel.add(okLabel, gbc);
		
		++row;
		
		okFilesTF	= new JTextArea(5, 10);
		okFilesTF.setEditable(false);
		gbc.gridy 	= row;
		okScrollPane = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		okScrollPane.getViewport().setView(okFilesTF);
		detailsPanel.add(okScrollPane, gbc);
		if (okFilesBuffer != null && okFilesBuffer.length() != 0) {
			okFilesTF.append(okFilesBuffer.toString());
		}
		
		++row;
		
		JLabel failedLabel = new JLabel("Failed:");
		gbc.gridy 	= row;
		detailsPanel.add(failedLabel, gbc);
		
		++row;
		
		badFilesTF	= new JTextArea(5, 10);
		badFilesTF.setEditable(false);
		gbc.gridy = row;
		badScrollPane = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		badScrollPane.getViewport().setView(badFilesTF);
		detailsPanel.add(badScrollPane, gbc);
		if (badFilesBuffer != null && badFilesBuffer.length() != 0) {
			badFilesTF.append(badFilesBuffer.toString());
		}
	}
	
	public void actionPerformed(ActionEvent e) {

		// Handle open button action.
		if (e.getSource() == sourceButton) {
			JFileChooser fc = null;
			if (sourceDirPath != null) fc = new JFileChooser(sourceDirPath);
			else if (this.sourceDir.getText().length() != 0) fc = new JFileChooser(this.sourceDir.getText());
			else fc = new JFileChooser();
			
			fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			fc.setDialogTitle("Select source directory...");
			int returnVal = fc.showOpenDialog(AudioConverter.this);
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				File file = fc.getSelectedFile();
				fileModel = new DataModel(file);
				fileTableMode.setFileInfoList(fileModel);
				sourceDirPath = file.getAbsolutePath();
				this.sourceDir.setText(sourceDirPath);
				programSettings.setProperty("SourcePath", sourceDirPath);

				
			}	
		} else if (e.getSource() == targetButton) {
			JFileChooser fc = null;
			if (targetDirPath != null) fc = new JFileChooser(targetDirPath);
			else if (this.targetDir.getText().length() != 0) fc = new JFileChooser(this.targetDir.getText());
			else fc = new JFileChooser();

			fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			fc.setDialogTitle("Select target directory...");
			
			int returnVal = fc.showSaveDialog(AudioConverter.this);
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				File file = fc.getSelectedFile();
				targetDirPath = file.getAbsolutePath();
				this.targetDir.setText(targetDirPath);
				programSettings.setProperty("TargetPath", targetDirPath);
			}
		} else if (e.getSource() == detailsButton) {
				
			if (detailsVisible) {
				detailsVisible = false;
				buildControls(false);
			} else {
				detailsVisible = true;
				buildControls(true);
			}		
		} else if (e.getSource() == convertCB) {
			updateFileTable();
		} else if (e.getSource() == checkAllButton) {
			if (fileModel != null && fileTableMode != null) {
				fileModel.checkAll(true);
				fileTableMode.updateTable();
			}
		} else if (e.getSource() == uncheckAllButton) {
			if (fileModel != null && fileTableMode != null) {
				fileModel.checkAll(false);
				fileTableMode.updateTable();				
			}
		} else if (e.getSource() == optionsButton) {
			boolean encodeToA18 = false;
			if (currentConverter instanceof AnyToA18Converter) {
				encodeToA18 = true;
			}
			getOptionsDialog().show(encodeToA18);
		} else if (e.getSource() == convertButton) {
			
			// Check if convert is there
			if (currentConverter != null && fileModel != null) {
					
				//  progress bar
				progressBar.setStringPainted(true);
				if (fileModel.getNumFilesToConvert() != 0) {
					progressBar.setMaximum(fileModel.getNumFilesToConvert());
				}
				progressBar.setValue(0);
				
				if (detailsVisible) {
					okFilesTF.setText("");
					badFilesTF.setText("");
				}
				
				okFilesBuffer = new StringBuffer();
				badFilesBuffer = new StringBuffer();
				
				final File targetDirPath = new File(targetDir.getText());

				// run conversion in separate thread to avoid 
				//that the GUI freezes
				Runnable runnable = new Runnable() {
					public void run() {
						Enumeration fileEnumeration = fileModel.getFileInfoList().elements();
						for (int i=0; fileEnumeration.hasMoreElements(); ++i) {
							FileInfo fileInfo = (FileInfo) fileEnumeration.nextElement();
							if (!fileInfo.doConvert()) continue;
							
							File currFile = fileInfo.getFileRef();
							try {
								try {
									currentConverter.convertFile(currFile, targetDirPath, targetDirPath,
											overwriteCheckBox.isSelected(), getOptionsDialog().getParameterList());
									if (detailsVisible) {
										okFilesTF.append(currFile.getName() + "\n");
									}
									okFilesBuffer.append(currFile.getName()	+ "\n");
								} catch (ConversionException e) {
									if (detailsVisible) {
										badFilesTF.append(currFile.getName() + "\n");
									}
									badFilesBuffer.append(currFile.getName() + "\n");
								}

							} catch (Exception ex) {
								ex.printStackTrace();
							}
							// update progress bar
							progressBar.setValue(i + 1);
						}
						updateFileTable();
					}
				};

				// start conversion
				Thread t = new Thread(runnable);
				t.start();
			}
			else
			{
				JOptionPane.showMessageDialog(this, "Please choose a target format for the conversion",
				"Converter Error", JOptionPane.ERROR_MESSAGE,
				null);
				return;
			}
		}
	}

	private void updateFileTable() {
		int curSel = convertCB.getSelectedIndex();
		
		if (fileModel != null && fileTableMode != null) {
			fileModel.updateFiles();
			if (curSel == 0) { //default
				currentConverter = null;
				fileModel.showOnlyFilesWithExtension(null);
				fileTableMode.updateTable();				
			} else {
				currentConverter = converters.get(curSel - 1);
				fileModel.showOnlyFilesWithExtension(currentConverter.getSourceFileExtensions());
				fileTableMode.updateTable();
			}
		}
	}
	
	public void changedUpdate(DocumentEvent e) {
		// NOOP
	}

	public void insertUpdate(DocumentEvent e) {
		enableChildren();
	}

	public void removeUpdate(DocumentEvent e) {
		enableChildren();
	}

	protected void enableChildren() {
		boolean enableConversion = sourceDirPath != null && targetDirPath != null 
									&& sourceDirPath.length() > 0 && targetDirPath.length() > 0;
		convertButton.setEnabled(enableConversion);
		boolean enableFormatCB = fileModel != null && fileTableMode != null;
		convertCB.setEnabled(enableFormatCB);
		 
	}
	
	private OptionsDialog getOptionsDialog() {
		if (mOptionsDialog == null) {
			mOptionsDialog = new OptionsDialog(this);
		}
		return mOptionsDialog;
	}
	
	public static void main(String[] args) {
		// Schedule a job for the event dispatch thread:
		// creating and showing this application's GUI.
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				// Turn off metal's use of bold fonts
				UIManager.put("swing.boldMetal", Boolean.FALSE);
				new AudioConverter();
			}
		});
	}
}
