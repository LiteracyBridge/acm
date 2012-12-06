package org.literacybridge.acm.config;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;

import org.literacybridge.acm.Constants;
import org.literacybridge.acm.audioconverter.converters.BaseAudioConverter.ConversionException;
import org.literacybridge.acm.content.AudioItem;
import org.literacybridge.acm.metadata.RFC3066LanguageCode;
import org.literacybridge.acm.repository.AudioItemRepository;
import org.literacybridge.acm.repository.AudioItemRepository.AudioFormat;
import org.literacybridge.acm.repository.CachingRepository;
import org.literacybridge.acm.repository.FileSystemRepository;
import org.literacybridge.acm.utils.FileUtil;

public class Configuration {

	private static Configuration instance;

    private static HashSet<String> uncachedFiles = new HashSet<String>();

    private static File cacheDirectory;
    
    private boolean readOnly = true;
   
 	private List<Locale> audioLanguages = null;
	private Map<Locale,String> languageLables = new HashMap<Locale, String>();
    
	private AudioItemRepository repository;

	
	// properties
	private AcmProperties acmProperties;
	// Stores path used by ACM
	private AcmPaths acmPaths;
	
	private Configuration() {
		createLiteracyBridgeDirectoryIfNecessary();
	}
	
	public static Configuration getConfiguration() {
		if (instance == null) {
			instance = new Configuration();
		}
		return instance;
	}
	
	public AcmPaths getAcmPaths() {
		return acmPaths;
	}
	
	public AudioItemRepository getRepository() {
		return repository;
	}
	
	public boolean isACMReadOnly() {
		return readOnly;
	}

	private void createLiteracyBridgeDirectoryIfNecessary() {
		File file = AcmPaths.getLiteracyBridgeSystemDirectory();
		FileUtil.createDirectoryIfNecessary(file);
	}
	
	private void createLiteracyBridgeCacheDirectoryIfNecessary() {
		File file = acmPaths.getLiteracyBridgeCacheDirectory();
		FileUtil.createDirectoryIfNecessary(file);
	}
	
	public File getDefaultLiteracyBridgeDirectory() {
		return acmPaths.getLiteracyBridgeCacheDirectory();
	}
	
	public File getDatabaseDirectory() {
		return acmPaths.getDatabaseDirectory();
	}
	
	public File getContentDirectory() {
		return acmPaths.getContentDirectory();
	}
	
	public File getCacheDirectory() {
		return cacheDirectory;
	}
	
	public File getGlobalShareDirectory() {
		return acmPaths.getGlobalShareDirectory();
	}
	
	public boolean HasValidGlobalShareDirectory() {
		return acmPaths.HasValidGlobalShareDirectory();
	}
	
	public void setDatabaseDirectory(File databaseDirectory) {
		acmPaths.setDatabaseDirectory(databaseDirectory);
		acmProperties.setDefaultDatabase(databaseDirectory.getAbsolutePath());
	}
	
	public void setContentDirectory(File contentDirectory) {
		acmPaths.setContentDirectory(contentDirectory);
		acmProperties.setDefaultRepositoryFilePath(contentDirectory.getAbsolutePath());
	}

	public String getUserNameOrNull() {
		return acmProperties.getUserNameOrNull();
	}
	
	public void setUserName(String userName) {
		acmProperties.setUserName(userName);
	}
	
	public String getUserContactInfoOrNull() {
		return acmProperties.getUserContactInfoOrNull();
	}
	
	public void setUserContactInfo(String userContactInfo) {
		acmProperties.setUserContactInfo(userContactInfo);
	}
	
	public void init(String databaseDirectory, String repositoryDirectory) {
		initializeConfiguration(databaseDirectory, repositoryDirectory);
	}
	
	public void storeConfiguration() {
		File propertiesFile = new File(AcmPaths.getLiteracyBridgeSystemDirectory() , AcmProperties.DefaultConfigurationPropertiesFileName);
		acmProperties.writeProperties(propertiesFile);
	}
	
	public void setFallbackPaths() {
		setDatabaseDirectory(AcmPaths.getFallbackDatabaseDirectoryPath());
		setContentDirectory(AcmPaths.getFallBackContentDirectoryPath());
		
		// create if necessary
		FileUtil.createDirectoryIfNecessary(getDatabaseDirectory());
		FileUtil.createDirectoryIfNecessary(getContentDirectory());
	}

	public String getRecordingCounter() {
		return acmProperties.getRecordingCounterOrNull();
	}
	
	public void setRecordingCounter(String counter) {
		acmProperties.setRecordingCounter(counter);		
	}
	
	public String getDeviceID() throws IOException {
		String deviceID = acmProperties.getDeviceIDOrNull();
		if (deviceID == null) {
			final int n = 10;
			Random rnd = new Random();
			// generate 10-digit unique ID for this acm instance
			StringBuilder builder = new StringBuilder();
			for (int i = 0; i < n; i++) {
				builder.append(Character.forDigit(rnd.nextInt(Character.MAX_RADIX), Character.MAX_RADIX));
			}
			
			deviceID = builder.toString();
			acmProperties.setDeviceID(deviceID);
		}
		
		return deviceID;
	}
	
	public String getNewAudioItemUID() throws IOException {
		String value = getRecordingCounter();
		int counter = (value == null) ? 0 : Integer.parseInt(value, Character.MAX_RADIX);
		counter++;
		value = Integer.toString(counter, Character.MAX_RADIX);
		String uuid = "LB-2" + "_"  + getDeviceID() + "_" + value;
		
		// make sure we remember that this uuid was already used
		setRecordingCounter(value);
		return uuid;
	}

	public void cacheNewA18Files() {
		findUncachedWaveFiles();
		if (uncachedFiles.size() > 0) {
			javax.swing.SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					int s = uncachedFiles.size();
					System.out.println("Setting progress bar max to: " + Integer.toString(s) );
					ConvertNewA18Files p = new ConvertNewA18Files(s);
					p.createAndShowGUI();
				}
			});
		}		
	}
	
	private final static Pattern LANGUAGE_LABEL_PATTERN = Pattern.compile(".*\\(\"(.+)\"\\).*");
	
	public String getLanguageLabel(Locale locale) {
		return languageLables.get(locale);
	}
	
	public List<Locale> getAudioLanguages() {
		if (audioLanguages == null) {
			audioLanguages = new ArrayList<Locale>();
			String languages = acmProperties.getAudioLanguages();
			if (languages != null) {
				StringTokenizer tokenizer = new StringTokenizer(languages, ", ");
				while (tokenizer.hasMoreTokens()) {
					String code = tokenizer.nextToken();
					String label = null;
					Matcher labelMatcher = LANGUAGE_LABEL_PATTERN.matcher(code);
					if (labelMatcher.matches()) {
						label = labelMatcher.group(1);
						code = code.substring(0, code.indexOf("("));
					}
					RFC3066LanguageCode language = new RFC3066LanguageCode(code);
					Locale locale = language.getLocale();
					if (locale != null) {
						if (label != null) {
							languageLables.put(locale, label);
						}
						audioLanguages.add(locale);
					}
				}
				if (audioLanguages.isEmpty()) {
					audioLanguages.add(Locale.ENGLISH);
				}
			}
		}		
		return Collections.unmodifiableList(audioLanguages);
	}

	private void setReadOnly(boolean isRW) {
		readOnly = isRW;
	}
	
	private static boolean isOnline() {
		boolean result = false;
		URLConnection connection = null;
		try {
			connection = new URL("http://dropbox.com").openConnection();
			connection.connect();
			result = true;
		} catch (MalformedURLException e) {
			// this should not ever happen (if the URL above is good)
			e.printStackTrace();
		} catch (IOException e) {
			result = false;
			//e.printStackTrace();
		} 
		return result;
	}
			
	private void initializeConfiguration(String databaseDirectory, String repositoryDirectory) {
		acmProperties = new AcmProperties();
		
		// default properties path
		File propertiesFile = new File(AcmPaths.getLiteracyBridgeSystemDirectory(), AcmProperties.DefaultConfigurationPropertiesFileName);		
		if (FileUtil.isValidFile(propertiesFile)) {
			acmProperties.readProperties(propertiesFile);
		}
		
		// initialize paths with properties
		acmPaths = new AcmPaths(acmProperties);
	
		// override database path if valid
		File databaseDirectoryFile = FileUtil.GetDirectoryOrNull(databaseDirectory);
		if (databaseDirectory != null) {
			acmPaths.setDatabaseDirectory(databaseDirectoryFile);
		} else {
			System.out.println("CommandLine: No Database Path passed. Ignoring override.");				
		}
		
		// override content path if valid
		File contentDirectoryFile = FileUtil.GetDirectoryOrNull(repositoryDirectory);
		if (contentDirectoryFile != null) {
			acmPaths.setContentDirectory(contentDirectoryFile);
		} else {
			System.out.println("CommandLine: No Repository (Content) Path passed. Ignoring override.");				
		}
		
		
		createLiteracyBridgeCacheDirectoryIfNecessary();
		
/*		System.out.println("  UserRWAccess:" + instance.userHasWriteAccess());
		System.out.println("  online:" + isOnline());
		System.out.println("  isAnotherUserWriting:" + instance.isAnotherUserWriting());
*/
		determineRWStatus();
/*		if (instance.isReadOnly()) {
			System.out.println("Read Only");
		} else
			System.out.println("RW");
*/
		instance.repository = new CachingRepository(
				new FileSystemRepository(cacheDirectory, 
						new FileSystemRepository.FileSystemGarbageCollector(Constants.CACHE_SIZE_IN_BYTES,
							new FilenameFilter() {
								@Override public boolean accept(File file, String name) {
									return name.toLowerCase().endsWith("." + AudioFormat.WAV.getFileExtension());
								}
							})),
				new FileSystemRepository(getContentDirectory()));
//		instance.repository.convert(audioItem, targetFormat);		
	}

	private void determineRWStatus() {
		boolean readOnlyStatus = false;

		if (!instance.userHasWriteAccess()) {
			readOnlyStatus = true; 
		} else {
			String dialogMessage = new String();
			Object[] options = {"Use Read-Only Mode", "Shutdown", "Force Write Mode"};
			if (!isOnline()) {
				readOnlyStatus = true;
				dialogMessage = "Cannot connect to dropbox.com.";
			} else if (instance.isAnotherUserWriting()) {
				readOnlyStatus = true;
				dialogMessage = "Another user currently has write access to the ACM.\n";
				String line;
				File f = acmPaths.getLockFile();
				if (f.exists()) {
					try {
						BufferedReader in = new BufferedReader(new FileReader(f));
						while ((line = in.readLine()) != null) {
							dialogMessage += line + "\n";
						}
						in.close();
					} catch (IOException e) {
						throw new RuntimeException("Unable to load lock file: " + f, e);
					}
				}
			} 
			if (readOnlyStatus) {
				int n = JOptionPane.showOptionDialog(null, dialogMessage,"Cannot Get Write Access",JOptionPane.YES_NO_CANCEL_OPTION,
						JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
				if (n==0)
					JOptionPane.showMessageDialog(null,"You now have read-only access.");
				else if (n==1) {
					JOptionPane.showMessageDialog(null,"Shutting down.");
					System.exit(0);
				} else if (n==2) {
					dialogMessage = "Forcing write mode could cause another user\nto lose their work or could corrupt the database!!!\n\n" +
							"If you are sure you want to force write access,\ntype the word 'force' below.";
					String confirmation = (String)JOptionPane.showInputDialog(null,dialogMessage);
					if (confirmation != null && confirmation.equalsIgnoreCase("force")) {
						readOnlyStatus = false;
						JOptionPane.showMessageDialog(null,"You now have write access, but database corruption may occur\nif another user is also currently writing to the ACM.");
					}
					else {
						readOnlyStatus = true;
						JOptionPane.showMessageDialog(null,"You now have read-only access.");
					}
				}
			}
		}
		instance.setReadOnly(readOnlyStatus);
		if (!readOnlyStatus) {
		    // ACM is now read-write, so need to lock other users
			try {
				File lockFile = acmPaths.getLockFile();
				BufferedWriter output = new BufferedWriter(new FileWriter(lockFile));
			    output.write("User:"+ acmProperties.getUserNameOrNull() + "    ");
				output.write("Contact:" + acmProperties.getUserContactInfoOrNull() + "\n");
			    output.write("Time:" + new Date().toString());
				output.close();
				lockFile.deleteOnExit();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	//====================================================================================================================
	
	
	private boolean isAnotherUserWriting() {
		boolean result;
		File f = acmPaths.getLockFile();
		result = f.exists();
		return result;
	}

	private boolean userHasWriteAccess() {
		String writeUser, thisUser;
		boolean userHasWriteAccess = false;
		
		thisUser = acmProperties.getUserNameOrNull();
		if (thisUser == null) {
			// No User Name found in config.properties.  Forcing Read-Only mode.
			return false;
		}
		File f = acmPaths.getDBAccessFile();
		if (f.exists()) {
			try {
				BufferedReader in = new BufferedReader (new FileReader(f));
				while ((writeUser = in.readLine()) != null) {
					if (writeUser.equalsIgnoreCase(thisUser)) {
						userHasWriteAccess = true;
						break;
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			// Cannot find database access list. Forcing Read-Only mode. 
			return false;
		}
		return userHasWriteAccess;
	}	
	
	private void findUncachedWaveFiles () {
	    String audioItemName;
		File repository = new File(acmPaths.getContentDirectory(),"org\\literacybridge");
		if (repository.listFiles() != null) {
		    for (File audioItem : repository.listFiles()) {
			    if (".".equals(audioItem.getName()) || "..".equals(audioItem.getName()) || audioItem.isFile()) {
				      continue;  // Ignore the self and parent aliases.
				    }
			    audioItemName = audioItem.getName();
			    File cachedItem = new File(getCacheDirectory(),"org\\literacybridge\\"+audioItemName+"\\"+audioItemName+".wav");
			    
			    if (cachedItem.exists()) 
			    	System.out.print("found:");
			    else {
			    	System.out.print("not found:");
			    	uncachedFiles.add(audioItemName);
			    }
			    System.out.println(cachedItem.getAbsolutePath());
		    }
		}
	}
	
	public static class ConvertNewA18Files extends JPanel implements ActionListener, PropertyChangeListener {
	
	private JProgressBar progressBar;
	private JButton cancelButton;
	private Task task;
	public int max;
	
	class Task extends SwingWorker<Void, Void> {
		/*
	* Main task. Executed in background thread.
	*/
		private int max;
		
		public Task (int max) {
			this.max = max;
		}
		@Override
		public Void doInBackground() {
			int progress = 0;
			//Initialize progress property.
			setProgress(0);
			System.out.println("total:"+uncachedFiles.size());
			Iterator<String> i = uncachedFiles.iterator();
		    while (i.hasNext()) {
				String audioItemName = i.next();
				AudioItem item = AudioItem.getFromDatabase(audioItemName);
				if (item != null) {
					System.out.println("Converting " + audioItemName);
					try {
						Configuration.getConfiguration().getRepository().convert(item, AudioFormat.WAV);
					} catch (ConversionException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} else {
					// .a18 in repository but no entry in the database
					// not sure why this would happen, but for now - don't convert to wav.
					System.out.println("Will not convert " + audioItemName + "; not in DB.");
				}
		        setProgress(++progress);
			}
			return null;
		}
		
		/*
		* Executed in event dispatching thread
		*/
		@Override
		public void done() {
			cancelButton.setEnabled(true);
			setCursor(null); //turn off the wait cursor
//			taskOutput.append("Done!\n");
			
		}
	
	}
	
		public ConvertNewA18Files(int max) {
			super(new BorderLayout());
			this.max = max;
			//Create the demo's UI.
			cancelButton = new JButton("Cancel");
			cancelButton.setActionCommand("start");
			cancelButton.addActionListener(this);
			
			progressBar = new JProgressBar(0, max);
			progressBar.setValue(0);
			progressBar.setStringPainted(true);
	
			JPanel panel = new JPanel();
			panel.add(cancelButton);
			panel.add(progressBar);
			
			add(panel, BorderLayout.PAGE_START);
			setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
		
		}
		
		/**
		* Invoked when the user presses the start button.
		*/
		public void actionPerformed(ActionEvent evt) {
			cancelButton.setEnabled(false);
			setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			task.cancel(true);
		}
		
		/**
		* Invoked when task's progress property changes.
		*/
		public void propertyChange(PropertyChangeEvent evt) {
			if ("progress" == evt.getPropertyName()) {
				int progress = (Integer) evt.getNewValue();
				progressBar.setValue(progress);
			} 
		}
		
		
		/**
		* Create the GUI and show it. As with all GUI code, this must run
		* on the event-dispatching thread.
		*/
		public void createAndShowGUI() {
			//Create and set up the window.
			JFrame frame = new JFrame("Converting New Files");
			frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			//Create and set up the content pane.
			JComponent newContentPane = this;
			newContentPane.setOpaque(true); //content panes must be opaque
			frame.setContentPane(newContentPane);
			
			frame.pack();
			frame.setVisible(true);
			setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			task = new Task(this.max);
			task.addPropertyChangeListener(this);
			task.execute();	
		}
	
	}

}
