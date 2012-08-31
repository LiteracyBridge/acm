package org.literacybridge.acm.config;

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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import org.literacybridge.acm.Constants;
import org.literacybridge.acm.metadata.RFC3066LanguageCode;
import org.literacybridge.acm.repository.AudioItemRepository;
import org.literacybridge.acm.repository.AudioItemRepository.AudioFormat;
import org.literacybridge.acm.repository.CachingRepository;
import org.literacybridge.acm.repository.FileSystemRepository;

public class Configuration extends Properties {

	private static Configuration instance;
	// This must be always the initial root directory, if ACM already exists on a machine
    private final static File DEFAULT_LITERACYBRIDGE_SYSTEM_DIR = new File(Constants.USER_HOME_DIR, Constants.LiteracybridgeHomeDirName);
    
    private static File repositoryDirectory;
    private static File cacheDirectory;
    private static File dbDirectory;
    private boolean readOnly = true;
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
	public static String getLiteracyBridgeSystemDirectory() {
		String dir = null;
		dir = DEFAULT_LITERACYBRIDGE_SYSTEM_DIR.getAbsolutePath();
		File f = new File (dir);
		if (!f.exists())
			f.mkdir();
		return dir;
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
		return new File(getLiteracyBridgeSystemDirectory(), Constants.TBBuildsHomeDirName);
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

	public static void init(String overrideDBDirectory, String overrideRepositoryDirectory) {
		if (instance == null) {
			instance = new Configuration();
			if (overrideDBDirectory != null) {
				File dbPath = new File(overrideDBDirectory);
				File repPath = new File (overrideRepositoryDirectory);
				if (dbPath.exists() && repPath.exists()) {
					instance.pathsOverridden = true;
					setDatabaseDirectory(dbPath);
					setRepositoryDirectory(repPath);
				} else
					System.out.println("DB or Repository Path does not exist.  Ignoring override.");
			}
			InitializeConfiguration();
		}
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
		return new File(getLiteracyBridgeSystemDirectory(), Constants.CONFIG_PROPERTIES);
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
		cacheDirectory = new File(DEFAULT_LITERACYBRIDGE_SYSTEM_DIR, Constants.CACHE_DIR_NAME);
		
		if (!cacheDirectory.exists()) {
			cacheDirectory.mkdirs();
		}
		
		instance.repository = new CachingRepository(
				new FileSystemRepository(cacheDirectory, 
						new FileSystemRepository.FileSystemGarbageCollector(2 * 1024 * 1024 * 1024, // 2GB cache
							new FilenameFilter() {
								@Override public boolean accept(File file, String name) {
									return name.toLowerCase().endsWith("." + AudioFormat.WAV.getFileExtension());
								}
							})),
				new FileSystemRepository(getRepositoryDirectory()));
	}

	private static File getGlobalShareDirectory() {
		// This function returns a File to the user's Dropbox directory 
		// if Dropbox was installed in the default location.
		// Otherwise, it returns null.
		File file = new File(Constants.USER_HOME_DIR,Constants.DefaultSharedDirName1);
		if (!file.exists()) {
			file = new File(Constants.USER_HOME_DIR,Constants.DefaultSharedDirName1);
		}
		if (file.exists())
			return file;
		else
			return null;
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
		
		if (getConfigurationPropertiesFile().exists()) {
			try {
				BufferedInputStream in = new BufferedInputStream(new FileInputStream(getConfigurationPropertiesFile()));
				instance.load(in);
			} catch (IOException e) {
				throw new RuntimeException("Unable to load configuration file: " + getConfigurationPropertiesFile(), e);
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
			if (globalShare.exists())
				instance.put(GLOBAL_SHARE_PATH, globalShare.getAbsolutePath());
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
			instance.writeProps();
		}
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
	
}
