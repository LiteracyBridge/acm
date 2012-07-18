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

import org.literacybridge.acm.Constants;
import org.literacybridge.acm.metadata.RFC3066LanguageCode;

public class Configuration extends Properties {

	// This must be always the initial root directory, if ACM already exists on a machine
    private final static File DEFAULT_LITERACYBRIDGE_SYSTEM_DIR = new File(Constants.USER_HOME_DIR, Constants.LiteracybridgeHomeDirName);
    private static final File REDIRECT_TO_DATABASE_FILE = new File(DEFAULT_LITERACYBRIDGE_SYSTEM_DIR, "redirectToDatabase.txt");

    // static constructor
    static {
    	init();
    }
	
	// Call this methods to get the actual root directories paths...

	public static String getLiteracyBridgeSystemDirectory() {
		if (instance.containsKey(NEW_DATABASE_DIRECTORY)) {
			return instance.getProperty(NEW_DATABASE_DIRECTORY);
		}
		
		// default
		return DEFAULT_LITERACYBRIDGE_SYSTEM_DIR.getAbsolutePath();
	}
	
	public static File getDatabaseDirectory() {
		return new File(getLiteracyBridgeSystemDirectory(), Constants.DerbyDBHomeDir);
	}
	
	public static File GetConfigurationPropertiesFile() {
		return new File(getLiteracyBridgeSystemDirectory(), Constants.CONFIG_PROPERTIES);
	}

	public static File GetRepositoryDirectory() {
		return new File(getLiteracyBridgeSystemDirectory(), Constants.RepositoryHomeDirName);
	}
	
	
	
	// Database source
	// Default: internal used database directory
	// Other: User can define a path to a different DB folder
	public enum Source { Default, Other };

	
	private static Configuration instance;
	
	
	private static void init() {
		if (instance == null) {
			instance = new Configuration();
			InitializeConfiguration();
		}
	}
	
	
	public static Configuration getConfiguration() {
		return instance;
	}
	
	private static void InitializeConfiguration() {
		// check if a redirect to a different LB Root directory exits.
		if (REDIRECT_TO_DATABASE_FILE.exists()) {
			try {
				BufferedInputStream in = new BufferedInputStream(new FileInputStream(REDIRECT_TO_DATABASE_FILE));
				instance.load(in);
			} catch (IOException e) {
				throw new RuntimeException("Unable to load redirection file: " + REDIRECT_TO_DATABASE_FILE, e);
			}
			
			File newLiteracyBridgeRootDirectory = null;
			if (instance.containsKey(NEW_DATABASE_DIRECTORY)) { 
				newLiteracyBridgeRootDirectory = new File(instance.getProperty(NEW_DATABASE_DIRECTORY));
				
			}
			
			// if a redirect file exits, the new path should be valid
			if (newLiteracyBridgeRootDirectory == null || !newLiteracyBridgeRootDirectory.exists()) {
				throw new RuntimeException("The redirection should contain the entry " + NEW_DATABASE_DIRECTORY + "=path");
			}
		}
		
		InitializeAcmConfiguration();
	}
	
	private static void InitializeAcmConfiguration() {
		if (GetConfigurationPropertiesFile().exists()) {
			try {
				BufferedInputStream in = new BufferedInputStream(new FileInputStream(GetConfigurationPropertiesFile()));
				instance.load(in);
			} catch (IOException e) {
				throw new RuntimeException("Unable to load configuration file: " + GetConfigurationPropertiesFile(), e);
			}
		}
		
		if (!instance.containsKey(AUDIO_LANGUAGES)) {
			instance.put(AUDIO_LANGUAGES, "en,dga(\"Dagaare\"),tw(\"Twi\"),sfw(\"Sehwi\")");
			instance.writeProps();
		}
	}
	
	
	public void writeProps() {
		try {
			BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(GetConfigurationPropertiesFile()));
			super.store(out, null);
			out.flush();
			out.close();
		} catch (IOException e) {
			throw new RuntimeException("Unable to write configuration file: " + GetConfigurationPropertiesFile(), e);
		}
	}
	
	
	//====================================================================================================================
	
	
	
	
	private final static String RECORDING_COUNTER_PROP = "RECORDING_COUNTER";
	private final static String DEVICE_ID_PROP = "DEVICE_ID";
	private final static String AUDIO_LANGUAGES = "AUDIO_LANGUAGES";
	private final static String NEW_DATABASE_DIRECTORY = "NEW_DATABASE_DIRECTORY";
	

	private List<Locale> audioLanguages = null;
	private Map<Locale,String> languageLables = new HashMap<Locale, String>();
	
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
}
