package org.literacybridge.acm.config;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import org.literacybridge.acm.utils.FileUtil;

public class AcmProperties {
	
	public final static String DefaultConfigurationPropertiesFileName = "config.properties";
		
	private Properties properties = new Properties();
		
	private final static String GLOBAL_SHARE_PATH = "GLOBAL_SHARE_PATH";
	private final static String DEFAULT_REPOSITORY = "DEFAULT_REPOSITORY";
	private final static String DEFAULT_DB = "DEFAULT_DB";
	private final static String USER_NAME = "USER_NAME";
	private final static String USER_CONTACT_INFO = "USER_CONTACT_INFO";
    private final static String RECORDING_COUNTER = "RECORDING_COUNTER";
	private final static String DEVICE_ID = "DEVICE_ID";
 	private final static String AUDIO_LANGUAGES = "AUDIO_LANGUAGES";	
	
		
	public String getGlobalShareDirectoryPathOrNull() {
		return getPropertyOrNull(GLOBAL_SHARE_PATH);
	}
	
	public String getDefaultRepositoryFilePathOrNull() {
		return getPropertyOrNull(DEFAULT_REPOSITORY);
	}
	
	public void setDefaultRepositoryFilePath(String reprositoryFilePath) {
		setProperty(DEFAULT_REPOSITORY, reprositoryFilePath);
	}
	
	public String getDefaultDatabaseFilePathOrNull() {
		return getPropertyOrNull(DEFAULT_DB);
	}
	
	public void setDefaultDatabase(String databaseFilePath) {
		setProperty(DEFAULT_DB, databaseFilePath);
	}
	
	public String getUserNameOrNull() {
		return getPropertyOrNull(USER_NAME);
	}
	
	public void setUserName(String userName) {
		setProperty(USER_NAME, userName);
	}
	
	public String getUserContactInfoOrNull() {
		return getPropertyOrNull(USER_CONTACT_INFO);
	}
	
	public void setUserContactInfo(String userContactInfo) {
		setProperty(USER_CONTACT_INFO, userContactInfo);
	}
	
	public String getRecordingCounterOrNull() {
		return getPropertyOrNull(RECORDING_COUNTER);
	}
	
	public void setRecordingCounter(String recordCounter) {
		setProperty(RECORDING_COUNTER, recordCounter);
	}
	
	public String getAudioLanguages() {
		return getPropertyOrNull(AUDIO_LANGUAGES);
	}

	public void setDeviceID(String deviceID) {
		setProperty(DEVICE_ID, deviceID);
	}
	
	public String getDeviceIDOrNull() {
		return getPropertyOrNull(DEVICE_ID);
	}
	
	private void setProperty(String key, String value) {
		properties.setProperty(key, value);
	}
	
	private String getPropertyOrNull(String key) {
		return properties.getProperty(key);
	}
	
	private void setDefaultsIfNecessary() {
		if (getPropertyOrNull(AUDIO_LANGUAGES) == null) {
			setProperty(AUDIO_LANGUAGES, "en,dga(\"Dagaare\"),tw(\"Twi\"),sfw(\"Sehwi\")");
		}
	}
	
	public void readProperties(File file) {
		if (FileUtil.isValidFile(file)) {
			try {
				BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));
				properties.load(in);
			} catch (IOException exception) {
				throw new RuntimeException("Unable to load configuration file: " + file.getAbsolutePath(), exception);
			}
		}
		
		setDefaultsIfNecessary();
	}
	
	public void writeProperties(File file) {
		try {
			BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));
			properties.store(out, null);
			out.flush();
			out.close();
		} catch (IOException exception) {
			throw new RuntimeException("Unable to write configuration file: " + file.getAbsolutePath(), exception);
		}
	}
}