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
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.persistence.EntityManager;
import javax.swing.JOptionPane;

import org.literacybridge.acm.Constants;
import org.literacybridge.acm.db.Persistence;
import org.literacybridge.acm.db.Persistence.DatabaseConnection;
import org.literacybridge.acm.metadata.RFC3066LanguageCode;
import org.literacybridge.acm.repository.AudioItemRepository;

@SuppressWarnings("serial")
public class DBConfiguration extends Properties {
	private boolean initialized = false;
    private File repositoryDirectory;
    private File cacheDirectory;
    private File dbDirectory;
    private File tbLoadersDirectory;
    private File sharedACMDirectory;
	private String acmName = null;
//  private static boolean pathsOverridden = false;
	private List<Locale> audioLanguages = null;
	private Map<Locale,String> languageLables = new HashMap<Locale, String>();
    
	private AudioItemRepository repository;
	private ControlAccess controlAccess;
	private DatabaseConnection dbConn;

	public DBConfiguration(String acmName) {
		this.acmName = acmName;
	}
	
	public AudioItemRepository getRepository() {
		return repository;
	}
	
    void setRepository(AudioItemRepository newRepository) {
		repository = newRepository;
	}
	
	// Call this methods to get the non-shared directory root for config, content cache, builds, etc...
	public String getACMDirectory() {
		File acm = new File (getLiteracyBridgeHomeDir(), Constants.ACM_DIR_NAME);
		if (!acm.exists())
			acm.mkdirs();
		return acm.getAbsolutePath();
	}
		
	public static File getLiteracyBridgeHomeDir() {
		return ACMConfiguration.LB_HOME_DIR;
	}

	String getTempACMsDirectory() {
		File temp = new File (getACMDirectory(), Constants.TempDir);
		if (!temp.exists())
			temp.mkdirs();
		return temp.getAbsolutePath();
	}
	
	public File getDatabaseDirectory() {
		if (dbDirectory == null)
			dbDirectory = new File(getTempACMsDirectory(), getSharedACMname() + "/" + Constants.DBHomeDir);
		return dbDirectory;
		//		return new File(getSharedACMDirectory(), Constants.DerbyDBHomeDir);
	}
	
	public File getRepositoryDirectory() {
		return repositoryDirectory;
	}

	public File getCacheDirectory() {
		return cacheDirectory;
	}

	public File getTBLoadersDirectory() {
		return tbLoadersDirectory;
	}
	
	public File getTBBuildsDirectory() {
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

	public void init() {
//		File dbPath, repPath;
		if (!initialized) {
//			if (args.readonly)
//				instance.setReadOnly(true); 
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
			InitializeAcmConfiguration();
			initializeLogger();
			controlAccess = new ControlAccess(this);
			controlAccess.init();
			
			initialized = true;
		}
	}
	
	public void connectDB() throws Exception {
		this.dbConn = Persistence.initialize(this);
	}
	
	public EntityManager getEntityManager() {
		return dbConn.getEntityManager();
	}
	
	public DatabaseConnection getDatabaseConnection() {
		return dbConn;
	}
	
	public ControlAccess getControlAccess() {
		return controlAccess;
	}
	
	public String getSharedACMname() {
		return acmName;
	}
	
	public File getSharedACMDirectory() {
		return sharedACMDirectory;		
	}
		
	public String getUserName() {
		return getProperty("USER_NAME");
	}

	public String getUserContact() {
		return getProperty(Constants.USER_CONTACT_INFO);
	}
	
	public String getRecordingCounter() {
		return getProperty(Constants.RECORDING_COUNTER_PROP);
	}
	
	public void setRecordingCounter(String counter) {
		setProperty(Constants.RECORDING_COUNTER_PROP, counter);
	}
	
	public String getDeviceID() throws IOException {
		String value = getProperty(Constants.DEVICE_ID_PROP);
		if (value == null) {
			final int n = 10;
			Random rnd = new Random();
			// generate 10-digit unique ID for this acm instance
			StringBuilder builder = new StringBuilder();
			for (int i = 0; i < n; i++) {
				builder.append(Character.forDigit(rnd.nextInt(Character.MAX_RADIX), Character.MAX_RADIX));
			}
			value = builder.toString();
			setProperty(Constants.DEVICE_ID_PROP, value);
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
	
	public long getCacheSizeInBytes() {
		long size = Constants.DEFAULT_CACHE_SIZE_IN_BYTES;
		String value = getProperty(Constants.CACHE_SIZE_PROP_NAME);
		if (value != null) {
			try {
				size = Long.parseLong(value);
			} catch (NumberFormatException e) {
				// ignore and use default value
			}
		}
		
		return size;
	}

	private final static Pattern LANGUAGE_LABEL_PATTERN = Pattern.compile(".*\\(\"(.+)\"\\).*");
	
	public String getLanguageLabel(Locale locale) {
		return languageLables.get(locale);
	}
	
	public List<Locale> getAudioLanguages() {
		if (audioLanguages == null) {
			audioLanguages = new ArrayList<Locale>();
			String languages = getProperty(Constants.AUDIO_LANGUAGES);
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

    private void setRepositoryDirectory(File f) {
    	repositoryDirectory = f; //new File(f,Constants.RepositoryHomeDirName);
    }
    
	private File getConfigurationPropertiesFile() {
		return new File(getACMDirectory(), Constants.CONFIG_PROPERTIES);
	}

	private void InitializeAcmConfiguration() {
		boolean propsChanged = false;
		
		cacheDirectory = new File(getACMDirectory(), Constants.CACHE_DIR_NAME);		
		if (!cacheDirectory.exists()) {
			cacheDirectory.mkdirs();
		}
		if (getConfigurationPropertiesFile().exists()) {
			try {
				BufferedInputStream in = new BufferedInputStream(new FileInputStream(getConfigurationPropertiesFile()));
				load(in);
			} catch (IOException e) {
				throw new RuntimeException("Unable to load configuration file: " + getConfigurationPropertiesFile(), e);
			}
		}


		if (getSharedACMname() != null && (getDatabaseDirectory() == null || getRepositoryDirectory() == null)) {
			File fACM = new File(ACMConfiguration.getGlobalShareDir(), getSharedACMname());
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
		
		if (!containsKey(Constants.USER_NAME)) {
			String username = (String)JOptionPane.showInputDialog(null, "Enter Username:", "Missing Username", JOptionPane.PLAIN_MESSAGE);
			put(Constants.USER_NAME, username);
			propsChanged = true;
		}
		if (!containsKey(Constants.USER_CONTACT_INFO)) {
			String contactinfo = (String)JOptionPane.showInputDialog(null, "Enter Phone #:", "Missing Contact Info", JOptionPane.PLAIN_MESSAGE);
			put(Constants.USER_CONTACT_INFO, contactinfo);
			propsChanged = true;
		}		
		if (!containsKey(Constants.AUDIO_LANGUAGES)) {
			put(Constants.AUDIO_LANGUAGES, "en,dga(\"Dagaare\"),ssl(\"Sisaala\"),tw(\"Twi\"),");  //sfw(\"Sehwi\"),
			propsChanged = true;
		}
		if (!containsKey(Constants.CACHE_SIZE_PROP_NAME)) {
			put(Constants.CACHE_SIZE_PROP_NAME, Long.toString(Constants.DEFAULT_CACHE_SIZE_IN_BYTES));
			propsChanged = true;
		}
		
		if (propsChanged) {
			writeProps();
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
		sharedACMDirectory = new File(ACMConfiguration.getGlobalShareDir(), getSharedACMname());
		tbLoadersDirectory = new File(sharedACMDirectory, Constants.TBLoadersHomeDir);
		writeProps();
	}	
	
	private void initializeLogger() {
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
