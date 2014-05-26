package org.literacybridge.acm;

import java.io.File;

public class Constants {
	public final static String ACM_VERSION					= "v1.0r1203";
	public final static String LiteracybridgeHomeDirName	= "LiteracyBridge";
	public final static String ACM_DIR_NAME			    	= "ACM";
	public final static String CACHE_DIR_NAME			    = "cache";
	public final static String DefaultSharedDirName1		= "Dropbox";
	public final static String DefaultSharedDirName2		= "My Documents/Dropbox";
	public final static String TempDir 						= "temp";
	public final static String DBHomeDir 					= "db";
	public final static String RepositoryHomeDir 			= "content";
	public final static String TBLoadersHomeDir 			= "TB-Loaders";
	public final static String DefaultSharedDB				= "ACM-test/" + DBHomeDir;
	public final static String DefaultSharedRepository		= "ACM-test/" + RepositoryHomeDir;	
	public final static String TBBuildsHomeDirName			= "TB-builds";
	public final static String TBDefinitionsHomeDirName		= "TB-definitions";
	public final static String GLOBAL_CONFIG_PROPERTIES 	= "acm_config.properties";
	public final static String CONFIG_PROPERTIES 			= "config.properties";
	public final static String CHECKOUT_PROPERTIES_SUFFIX	= "-checkedOut.properties";
	public final static String USER_WRITE_LOCK_FILENAME		= "locked.txt";
	public final static String DB_ACCESS_FILENAME			= "accessList.txt";
	
    public final static File USER_HOME_DIR = new File(System.getProperty("user.home", "."));
    public final static long DEFAULT_CACHE_SIZE_IN_BYTES	= 2L * 1024L * 1024L * 1024L; // 2GB
    
	public final static String USER_NAME = "USER_NAME";
	public final static String USER_CONTACT_INFO = "USER_CONTACT_INFO";
	//public final static String DEFAULT_REPOSITORY = "DEFAULT_REPOSITORY";
	//public final static String DEFAULT_DB = "DEFAULT_DB";
	public final static String GLOBAL_SHARE_PATH = "GLOBAL_SHARE_PATH";
	public final static String RECORDING_COUNTER_PROP = "RECORDING_COUNTER";
	public final static String DEVICE_ID_PROP = "DEVICE_ID";
	public final static String AUDIO_LANGUAGES = "AUDIO_LANGUAGES";	
	public final static String CACHE_SIZE_PROP_NAME = "CACHE_SIZE_IN_BYTES";
 }
