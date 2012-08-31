package org.literacybridge.acm;

import java.io.File;

public class Constants {
	public final static String LiteracybridgeHomeDirName	= ".literacybridge";
	public final static String DefaultSharedDirName1		= "Dropbox";
	public final static String DefaultSharedDirName2		= "My Documents/Dropbox";
	public final static String DerbyDBHomeDir 				= ".db";
	public final static String RepositoryHomeDirName 		= "content";
	public final static String DefaultSharedDB				= "ACM-test/" + DerbyDBHomeDir;
	public final static String DefaultSharedRepository		= "ACM-test/" + RepositoryHomeDirName;	
	public final static String TBBuildsHomeDirName			= "TB-builds";
	public final static String TBDefinitionsHomeDirName		= "TB-definitions";
	public final static String CONFIG_PROPERTIES 			= "config.properties";
	public final static String USER_WRITE_LOCK_FILENAME		= "locked.txt";
	public final static String DB_ACCESS_FILENAME			= "accessList.txt";
	public final static String CACHE_DIR_NAME			    = "cache";
	
    public final static File USER_HOME_DIR = new File(System.getProperty("user.home", "."));
   
 }
