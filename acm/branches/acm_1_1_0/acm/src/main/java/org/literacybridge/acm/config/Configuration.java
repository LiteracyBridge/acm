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

	private static Configuration instance;    
    private static String title;
    private static File repositoryDirectory;
    private static File cacheDirectory;
    private static File dbDirectory;
	private static String sharedACM = null;
    private boolean readOnly = false;
    private boolean sandbox = false;
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
	
	public boolean isSandbox() {
		return sandbox;
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
//			if (args.readonly)
//				instance.setReadOnly(true); 
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
			InitializeAcmConfiguration();
			ControlAccess.determineAccess();
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
}
