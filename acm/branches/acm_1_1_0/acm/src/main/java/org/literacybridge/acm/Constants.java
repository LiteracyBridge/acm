package org.literacybridge.acm;

import java.io.File;

public class Constants {
	public final static String ACM_VERSION					= "v1.0r1076";
	public final static String LiteracybridgeHomeDirName	= "LiteracyBridge";
	public final static String ACM_DIR_NAME			    	= "ACM";
	public final static String CACHE_DIR_NAME			    = "cache";
	public final static String DefaultSharedDirName1		= "Dropbox";
	public final static String DefaultSharedDirName2		= "My Documents/Dropbox";
	public final static String DBHomeDir 					= "db";
	public final static String RepositoryHomeDir 			= "content";
	public final static String TBLoadersHomeDir 			= "TB-Loaders";
	public final static String DefaultSharedDB				= "ACM-test/" + DBHomeDir;
	public final static String DefaultSharedRepository		= "ACM-test/" + RepositoryHomeDir;	
	public final static String TBBuildsHomeDirName			= "TB-builds";
	public final static String TBDefinitionsHomeDirName		= "TB-definitions";
	public final static String CONFIG_PROPERTIES 			= "config.properties";
	public final static String USER_WRITE_LOCK_FILENAME		= "locked.txt";
	public final static String DB_ACCESS_FILENAME			= "accessList.txt";
	
    public final static File USER_HOME_DIR = new File(System.getProperty("user.home", "."));
    public final static long CACHE_SIZE_IN_BYTES			= 2L * 1024L * 1024L * 1024L; // 2GB
 }
