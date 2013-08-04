package org.literacybridge.acm.config;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import org.literacybridge.acm.Constants;
import org.literacybridge.acm.gui.CommandLineParams;
import org.literacybridge.acm.metadata.RFC3066LanguageCode;
import org.literacybridge.acm.repository.AudioItemRepository;

@SuppressWarnings("serial")
public class Configuration extends Properties {

	private static final File LB_HOME_DIR = new File(Constants.USER_HOME_DIR, Constants.LiteracybridgeHomeDirName);
	
	private static Configuration instance;
    private static String title;
    private static File repositoryDirectory;
    private static File cacheDirectory;
    private static File dbDirectory;
    private static File tbLoadersDirectory;
    private static File sharedACMDirectory;
	private static String sharedACM = null;
//  private static boolean pathsOverridden = false;
    private static boolean disableUI = false;
	private final static String USER_NAME = "USER_NAME";
	private final static String USER_CONTACT_INFO = "USER_CONTACT_INFO";
	//private final static String DEFAULT_REPOSITORY = "DEFAULT_REPOSITORY";
	//private final static String DEFAULT_DB = "DEFAULT_DB";
	private final static String GLOBAL_SHARE_PATH = "GLOBAL_SHARE_PATH";
	private final static String RECORDING_COUNTER_PROP = "RECORDING_COUNTER";
	private final static String DEVICE_ID_PROP = "DEVICE_ID";
	private final static String AUDIO_LANGUAGES = "AUDIO_LANGUAGES";	
	private static List<Locale> audioLanguages = null;
	private static Map<Locale,String> languageLables = new HashMap<Locale, String>();
    
	private static AudioItemRepository repository;

	public static Configuration getConfiguration() {
		return instance;
	}
	
	public static AudioItemRepository getRepository() {
		return repository;
	}
	
	static void setRepository(AudioItemRepository newRepository) {
		repository = newRepository;
	}
	
	// Call this methods to get the non-shared directory root for config, content cache, builds, etc...
	public static String getACMDirectory() {
		File acm = new File (getLiteracyBridgeHomeDir(), Constants.ACM_DIR_NAME);
		if (!acm.exists())
			acm.mkdirs();
		return acm.getAbsolutePath();
	}
		
	public static File getLiteracyBridgeHomeDir() {
		return LB_HOME_DIR;
	}

	static String getTempACMsDirectory() {
		File temp = new File (getACMDirectory(), Constants.TempDir);
		if (!temp.exists())
			temp.mkdirs();
		return temp.getAbsolutePath();
	}
	
	public static File getDatabaseDirectory() {
		if (dbDirectory == null)
			dbDirectory = new File(Configuration.getTempACMsDirectory(),Configuration.getSharedACMname() + "/" + Constants.DBHomeDir);
		return dbDirectory;
		//		return new File(getSharedACMDirectory(), Constants.DerbyDBHomeDir);
	}
	
	public static File getRepositoryDirectory() {
		return repositoryDirectory;
	}

	public static File getCacheDirectory() {
		return cacheDirectory;
	}

	public static File getTBLoadersDirectory() {
		return tbLoadersDirectory;
	}
	
	public static File getTBBuildsDirectory() {
		return new File(getACMDirectory(), Constants.TBBuildsHomeDirName);
	}

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
//		File dbPath, repPath;
		if (instance == null) {
			instance = new Configuration();
			disableUI = args.disableUI;
//			if (args.readonly)
//				instance.setReadOnly(true); 
			if (args.titleACM != null)
				setACMtitle(args.titleACM);
/*			NOT CURRENTLY USING THIS PARAMETER --  NEED TO RETHINK IT WHEN WE NEED IT
  			if (args.pathDB != null) {
				dbPath = new File(args.pathDB);
				if (args.pathRepository != null) {
					repPath = new File (args.pathRepository);
					if (dbPath.exists() && repPath.exists()) {
							pathsOverridden = true;
							setDatabaseDirectory(dbPath);
							setRepositoryDirectory(repPath);
					} else
						System.out.println("DB or Repository Path does not exist.  Ignoring override.");
					}
			} else 
*/
			if (args.sharedACM != null) {
				//pathsOverridden = true;
				setSharedACMname(args.sharedACM);
			}
			InitializeAcmConfiguration();
			initializeLogger();
			ControlAccess.init();
		}
	}
	
	private static void setSharedACMname(String newName) {
		sharedACM = newName;
	}

	public static String getSharedACMname() {
		return sharedACM;
	}
	
	public static File getSharedACMDirectory() {
		return sharedACMDirectory;		
	}
	
	public static boolean isDisableUI() {
		return disableUI;
	}
	
	private static void setACMtitle(String newName) {
		title = newName;
	}
	
	public static String getACMname() {
		return title;
	}

	public static String getUserName() {
		return instance.getProperty("USER_NAME");
	}

	public static String getUserContact() {
		return instance.getProperty(USER_CONTACT_INFO);
	}
	
	public static String getRecordingCounter() {
		return instance.getProperty(RECORDING_COUNTER_PROP);
	}
	
	public static void setRecordingCounter(String counter) {
		instance.setProperty(RECORDING_COUNTER_PROP, counter);
	}
	
	public static String getDeviceID() throws IOException {
		String value = instance.getProperty(DEVICE_ID_PROP);
		if (value == null) {
			final int n = 10;
			Random rnd = new Random();
			// generate 10-digit unique ID for this acm instance
			StringBuilder builder = new StringBuilder();
			for (int i = 0; i < n; i++) {
				builder.append(Character.forDigit(rnd.nextInt(Character.MAX_RADIX), Character.MAX_RADIX));
			}
			value = builder.toString();
			instance.setProperty(DEVICE_ID_PROP, value);
			instance.writeProps();
		}
		
		return value;
	}
	
	public static String getNewAudioItemUID() throws IOException {
		String value = getRecordingCounter();
		int counter = (value == null) ? 0 : Integer.parseInt(value, Character.MAX_RADIX);
		counter++;
		value = Integer.toString(counter, Character.MAX_RADIX);
		String uuid = "LB-2" + "_"  + getDeviceID() + "_" + value;
		
		// make sure we remember that this uuid was already used
		setRecordingCounter(value);
		instance.writeProps();
		
		return uuid;
	}

	private final static Pattern LANGUAGE_LABEL_PATTERN = Pattern.compile(".*\\(\"(.+)\"\\).*");
	
	public static String getLanguageLabel(Locale locale) {
		return languageLables.get(locale);
	}
	
	public static List<Locale> getAudioLanguages() {
		if (audioLanguages == null) {
			audioLanguages = new ArrayList<Locale>();
			String languages = instance.getProperty(AUDIO_LANGUAGES);
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

//	Commenting out since getDatabaseDirectory uses Constants to calculate path from other roots
//	static void setDatabaseDirectory(File f) {
//    	dbDirectory = f; // new File(f,Constants.DerbyDBHomeDir);
//    }

    private static void setRepositoryDirectory(File f) {
    	repositoryDirectory = f; //new File(f,Constants.RepositoryHomeDirName);
    }
    
	private static File getConfigurationPropertiesFile() {
		return new File(getACMDirectory(), Constants.CONFIG_PROPERTIES);
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
/*				File fDB = new File(fACM,Constants.DBHomeDir);
				setDatabaseDirectory(fDB);
				instance.put(DEFAULT_DB,getDatabaseDirectory().getAbsolutePath());
*/				File fRepo = new File(fACM,Constants.RepositoryHomeDir);
				setRepositoryDirectory(fRepo);			
//				instance.put(DEFAULT_REPOSITORY,getRepositoryDirectory().getAbsolutePath());
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
/*		COMMENTING THIS OUT SINCE WE ALWAYS PASS NAME OF ACM NOW - NEED TO RETHINK WHEN WE WANT TO USE CODE LIKE THIS AGAIN
		if (!pathsOverridden) {
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
*/
		sharedACMDirectory = new File(globalShare, getSharedACMname());
		tbLoadersDirectory = new File(sharedACMDirectory, Constants.TBLoadersHomeDir);
		instance.writeProps();
	}	
	
	private static void initializeLogger() {
		try {
			// Get the global logger to configure it
		    Logger logger = Logger.getLogger("");
	
		    logger.setLevel(Level.INFO);
		    String fileNamePattern = getACMDirectory() + File.separator + "acm.log.%g.%u.txt";
		    FileHandler fileTxt = new FileHandler(fileNamePattern);
	
		    Formatter formatterTxt = new SimpleFormatter();
		    fileTxt.setFormatter(formatterTxt);
		    logger.addHandler(fileTxt);
		} catch (IOException e) {
			throw new RuntimeException("Unable to initialize logging framework", e);
		}
	}
}
