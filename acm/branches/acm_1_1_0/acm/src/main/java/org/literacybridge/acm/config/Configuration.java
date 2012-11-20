package org.literacybridge.acm.config;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
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
import java.util.Properties;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;

import org.literacybridge.acm.Constants;
import org.literacybridge.acm.audioconverter.converters.BaseAudioConverter.ConversionException;
import org.literacybridge.acm.content.AudioItem;
import org.literacybridge.acm.gui.CommandLineParams;
import org.literacybridge.acm.metadata.RFC3066LanguageCode;
import org.literacybridge.acm.repository.AudioItemRepository;
import org.literacybridge.acm.repository.AudioItemRepository.AudioFormat;
import org.literacybridge.acm.repository.CachingRepository;
import org.literacybridge.acm.repository.FileSystemRepository;

public class Configuration extends Properties {

	private static Configuration instance;
	// This must be always the initial root directory, if ACM already exists on a machine
    private static HashSet<String> uncachedFiles = new HashSet<String>();

    JProgressBar progressBar;	
    
    private static String title;
    private static File repositoryDirectory;
    private static File cacheDirectory;
    private static File dbDirectory;
	private static String sharedACM = null;
    private boolean readOnly = false;
    private boolean pathsOverridden = false;
	private final static String USER_NAME = "USER_NAME";
	private final static String USER_CONTACT_INFO = "USER_CONTACT_INFO";
	private final static String DEFAULT_REPOSITORY = "DEFAULT_REPOSITORY";
	private final static String DEFAULT_DB = "DEFAULT_DB";
	private final static String GLOBAL_SHARE_PATH = "GLOBAL_SHARE_PATH";
	private final static String RECORDING_COUNTER_PROP = "RECORDING_COUNTER";
	private final static String DEVICE_ID_PROP = "DEVICE_ID";
	private final static String AUDIO_LANGUAGES = "AUDIO_LANGUAGES";	
	private List<Locale> audioLanguages = null;
	private Map<Locale,String> languageLables = new HashMap<Locale, String>();
    
	private AudioItemRepository repository;

	public static Configuration getConfiguration() {
		return instance;
	}
	
	public AudioItemRepository getRepository() {
		return repository;
	}
	
	public boolean isACMReadOnly() {
		return readOnly;
	}
	
	// Call this methods to get the non-shared directory root for config, content cache, builds, etc...
	public static String getACMDirectory() {
	    final File DEFAULT_LITERACYBRIDGE_SYSTEM_DIR = new File(Constants.USER_HOME_DIR, Constants.LiteracybridgeHomeDirName);
		File acm = new File (DEFAULT_LITERACYBRIDGE_SYSTEM_DIR,Constants.ACM_DIR_NAME);
		if (!acm.exists())
			acm.mkdirs();
		return acm.getAbsolutePath();
	}

	public static File getDatabaseDirectory() {
		return dbDirectory;
		//		return new File(getSharedACMDirectory(), Constants.DerbyDBHomeDir);
	}
	
	public static File getRepositoryDirectory() {
		return repositoryDirectory;
	}

	public static File getCacheDirectory() {
		return cacheDirectory;
	}
	
	public static File getTBBuildsDirectory() {
		return new File(getACMDirectory(), Constants.TBBuildsHomeDirName);
	}

/*	
//	public static File getTBDefinitionsDirectory() {
//		return new File(getSharedACMDirectory(), Constants.TBDefinitionsHomeDirName);
//	}
*/	
	public void writeProps() {
		try {
			BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(getConfigurationPropertiesFile()));
			super.store(out, null);
			out.flush();
			out.close();
		} catch (IOException e) {
			throw new RuntimeException("Unable to write configuration file: " + getConfigurationPropertiesFile(), e);
		}
	}

	public static void init(CommandLineParams args) {
		File dbPath, repPath;
		if (instance == null) {
			instance = new Configuration();
			if (args.readonly)
				instance.setReadOnly(true); 
			if (args.titleACM != null)
				setACMtitle(args.titleACM);
			if (args.pathDB != null) {
				dbPath = new File(args.pathDB);
				if (args.pathRepository != null) {
					repPath = new File (args.pathRepository);
					if (dbPath.exists() && repPath.exists()) {
							instance.pathsOverridden = true;
							setDatabaseDirectory(dbPath);
							setRepositoryDirectory(repPath);
					} else
						System.out.println("DB or Repository Path does not exist.  Ignoring override.");
					}
			} else if (args.sharedACM != null) {
				instance.pathsOverridden = true;
				setSharedACMname(args.sharedACM);
			}
			InitializeConfiguration();
		}
	}
	
	private static void setSharedACMname(String newName) {
		sharedACM = newName;
	}

	public static String getSharedACMname() {
		return sharedACM;
	}
	
	private static void setACMtitle(String newName) {
		title = newName;
	}
	
	public static String getACMname() {
		return title;
	}

	public String getRecordingCounter() {
		return getProperty(RECORDING_COUNTER_PROP);
	}
	
	public void setRecordingCounter(String counter) {
		setProperty(RECORDING_COUNTER_PROP, counter);
	}
	
	public String getDeviceID() throws IOException {
		String value = getProperty(DEVICE_ID_PROP);
		if (value == null) {
			final int n = 10;
			Random rnd = new Random();
			// generate 10-digit unique ID for this acm instance
			StringBuilder builder = new StringBuilder();
			for (int i = 0; i < n; i++) {
				builder.append(Character.forDigit(rnd.nextInt(Character.MAX_RADIX), Character.MAX_RADIX));
			}
			value = builder.toString();
			setProperty(DEVICE_ID_PROP, value);
			writeProps();
		}
		
		return value;
	}
	
	public String getNewAudioItemUID() throws IOException {
		String value = getRecordingCounter();
		int counter = (value == null) ? 0 : Integer.parseInt(value, Character.MAX_RADIX);
		counter++;
		value = Integer.toString(counter, Character.MAX_RADIX);
		String uuid = "LB-2" + "_"  + getDeviceID() + "_" + value;
		
		// make sure we remember that this uuid was already used
		setRecordingCounter(value);
		writeProps();
		
		return uuid;
	}

	public static void cacheNewA18Files() {
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
			String languages = getProperty(AUDIO_LANGUAGES);
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

	private static void setDatabaseDirectory(File f) {
    	dbDirectory = f; // new File(f,Constants.DerbyDBHomeDir);
    }

    private static void setRepositoryDirectory(File f) {
    	repositoryDirectory = f; //new File(f,Constants.RepositoryHomeDirName);
    }
    
	private static File getConfigurationPropertiesFile() {
		return new File(getACMDirectory(), Constants.CONFIG_PROPERTIES);
	}

	private static File getLockFile() {
		return new File(getDatabaseDirectory(), Constants.USER_WRITE_LOCK_FILENAME);
	}

	private static File getDBAccessFile() {
		return new File(getDatabaseDirectory(), Constants.DB_ACCESS_FILENAME);
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
			
	private static void InitializeConfiguration() {
		InitializeAcmConfiguration();
		System.out.println("  Database:" + getDatabaseDirectory());
		System.out.println("  Repository:" + getRepositoryDirectory());
		System.out.println("  UserRWAccess:" + instance.userHasWriteAccess());
		System.out.println("  online:" + isOnline());
		System.out.println("  isAnotherUserWriting:" + instance.isAnotherUserWriting());

		if (!instance.isACMReadOnly())
			determineRWStatus();
		else 
			System.out.println("Command-line forced read-only mode.");
		instance.repository = new CachingRepository(
				new FileSystemRepository(cacheDirectory, 
						new FileSystemRepository.FileSystemGarbageCollector(Constants.CACHE_SIZE_IN_BYTES,
							new FilenameFilter() {
								@Override public boolean accept(File file, String name) {
									return name.toLowerCase().endsWith("." + AudioFormat.WAV.getFileExtension());
								}
							})),
				new FileSystemRepository(getRepositoryDirectory()));
//		instance.repository.convert(audioItem, targetFormat);		
	}
	
	private static void determineRWStatus() {
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
				File f = getLockFile();
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
				File lockFile = getLockFile();
				BufferedWriter output = new BufferedWriter(new FileWriter(lockFile));
			    output.write("User:"+ instance.getProperty(USER_NAME) + "    ");
				output.write("Contact:" + instance.getProperty(USER_CONTACT_INFO) + "\n");
			    output.write("Time:" + new Date().toString());
				output.close();
				lockFile.deleteOnExit();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private static void InitializeAcmConfiguration() {
		File globalShare = null;

		cacheDirectory = new File(getACMDirectory(), Constants.CACHE_DIR_NAME);		
		if (!cacheDirectory.exists()) {
			cacheDirectory.mkdirs();
		}
		if (getConfigurationPropertiesFile().exists()) {
			try {
				BufferedInputStream in = new BufferedInputStream(new FileInputStream(getConfigurationPropertiesFile()));
				instance.load(in);
			} catch (IOException e) {
				throw new RuntimeException("Unable to load configuration file: " + getConfigurationPropertiesFile(), e);
			}
		}

		String globalSharePath = instance.getProperty(GLOBAL_SHARE_PATH);
		if (globalSharePath != null) {
			globalShare = new File (globalSharePath);
		} 
		if (globalSharePath == null || globalShare == null || !globalShare.exists()) {
			//try default dropbox installation
			globalShare = new File (Constants.USER_HOME_DIR,Constants.DefaultSharedDirName1);
			if (!globalShare.exists())
				globalShare = new File (Constants.USER_HOME_DIR,Constants.DefaultSharedDirName2);
				
		}
		if (!globalShare.exists()) {
			JFileChooser fc = new JFileChooser();
			fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			fc.setDialogTitle("Select Dropbox directory.");
			int returnVal = fc.showOpenDialog(null);
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				globalShare = fc.getSelectedFile();
			} else if(getDatabaseDirectory() == null || getRepositoryDirectory() == null) {
				JOptionPane.showMessageDialog(null,"Dropbox directory has not been identified. Shutting down.");
				System.exit(0);								
			}
		}
		instance.put(GLOBAL_SHARE_PATH, globalShare.getAbsolutePath());

		if (getSharedACMname() != null && (getDatabaseDirectory() == null || getRepositoryDirectory() == null)) {
			File fACM = new File(globalShare,getSharedACMname());
			if (fACM.exists()) {
				File fDB = new File(fACM,Constants.DBHomeDir);
//				if (!fDB.exists()) 
//					fDB.mkdir();
				setDatabaseDirectory(fDB);
				instance.put(DEFAULT_DB,getDatabaseDirectory().getAbsolutePath());
				File fRepo = new File(fACM,Constants.RepositoryHomeDir);
				if (!fRepo.exists()) 
					fRepo.mkdir();
				setRepositoryDirectory(fRepo);			
				instance.put(DEFAULT_REPOSITORY,getRepositoryDirectory().getAbsolutePath());
			} else {
				JOptionPane.showMessageDialog(null,"ACM database " + getSharedACMname() + 
				" is not found within Dropbox.\n\nBe sure that you have accepted the Dropbox invitation\nto share the folder" +
				" by logging into your account at\nhttp://dropbox.com and click on the 'Sharing' link.\n\nShutting down.");
				System.exit(0);				
			}
		}
		
		if (!instance.containsKey(USER_NAME)) {
			String username = (String)JOptionPane.showInputDialog(null, "Enter Username:", "Missing Username", JOptionPane.PLAIN_MESSAGE);
			instance.put(USER_NAME, username);
		}
		if (!instance.containsKey(USER_CONTACT_INFO)) {
			String contactinfo = (String)JOptionPane.showInputDialog(null, "Enter Phone #:", "Missing Contact Info", JOptionPane.PLAIN_MESSAGE);
			instance.put(USER_CONTACT_INFO, contactinfo);
		}		
		if (!instance.containsKey(AUDIO_LANGUAGES)) {
			instance.put(AUDIO_LANGUAGES, "en,dga(\"Dagaare\"),tw(\"Twi\"),sfw(\"Sehwi\")");
			instance.writeProps();
		}
		if (!instance.pathsOverridden) {
			if (dbDirectory == null && instance.containsKey(DEFAULT_DB)) {
				setDatabaseDirectory(new File(instance.getProperty(DEFAULT_DB)));
			}
			if (repositoryDirectory == null && instance.containsKey(DEFAULT_REPOSITORY)) {
				setRepositoryDirectory(new File(instance.getProperty(DEFAULT_REPOSITORY)));
			}

			if (dbDirectory == null || !dbDirectory.exists()) {
				setDatabaseDirectory(new File (globalShare,Constants.DefaultSharedDB));
				if (dbDirectory.exists()) {
					instance.put(DEFAULT_DB,getDatabaseDirectory().getAbsolutePath());
				} else {
					JFileChooser fc = null;
					if (globalShare.exists())
						fc = new JFileChooser(globalShare.getAbsolutePath());
					else
						fc = new JFileChooser();
					fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
					fc.setDialogTitle("Select DB directory.");
					int returnVal = fc.showOpenDialog(null);
					if (returnVal == JFileChooser.APPROVE_OPTION) {
						File file = fc.getSelectedFile();
						setDatabaseDirectory(file);
						instance.put(DEFAULT_DB,getDatabaseDirectory().getAbsolutePath());
					}
				} 
			}		
			if (repositoryDirectory == null || !repositoryDirectory.exists()) {
				setRepositoryDirectory (new File (globalShare,Constants.DefaultSharedRepository));
				if (repositoryDirectory.exists()) {
					instance.put(DEFAULT_REPOSITORY,getRepositoryDirectory().getAbsolutePath());
				} else {
					JFileChooser fc = null;
					if (globalShare.exists())
						fc = new JFileChooser(globalShare.getAbsolutePath());
					else
						fc = new JFileChooser();
					fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
					fc.setDialogTitle("Select repository directory.");
					int returnVal = fc.showOpenDialog(null);
					if (returnVal == JFileChooser.APPROVE_OPTION) {
						File file = fc.getSelectedFile();
						setRepositoryDirectory(file);
						instance.put(DEFAULT_REPOSITORY,getRepositoryDirectory().getAbsolutePath());
					}
				}
			}					
		}
		instance.writeProps();
	}
		
	//====================================================================================================================
	
	
	private boolean isAnotherUserWriting() {
		boolean result;
		File f = getLockFile();
		result = f.exists();
		return result;
	}

	private boolean userHasWriteAccess() {
		String writeUser, thisUser;
		boolean userHasWriteAccess = false;
		
		thisUser = getProperty("USER_NAME");
		if (thisUser == null) {
			// No User Name found in config.properties.  Forcing Read-Only mode.
			return false;
		}
		File f = getDBAccessFile();
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
	
	private static void findUncachedWaveFiles () {
	    String audioItemName;
		File repository = new File(Configuration.getRepositoryDirectory(),"org\\literacybridge");
		if (repository.listFiles() != null) {
		    for (File audioItem : repository.listFiles()) {
			    if (".".equals(audioItem.getName()) || "..".equals(audioItem.getName()) || audioItem.isFile()) {
				      continue;  // Ignore the self and parent aliases.
				    }
			    audioItemName = audioItem.getName();
			    File cachedItem = new File(Configuration.getCacheDirectory(),"org\\literacybridge\\"+audioItemName+"\\"+audioItemName+".wav");
			    
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
//	private JTextArea taskOutput;
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
			
//			taskOutput = new JTextArea(5, 20);
//			taskOutput.setMargin(new Insets(5,5,5,5));
//			taskOutput.setEditable(false);
//			
			JPanel panel = new JPanel();
			panel.add(cancelButton);
			panel.add(progressBar);
			
			add(panel, BorderLayout.PAGE_START);
//			add(new JScrollPane(taskOutput), BorderLayout.CENTER);
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
