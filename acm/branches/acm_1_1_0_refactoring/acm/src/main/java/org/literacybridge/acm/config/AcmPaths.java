package org.literacybridge.acm.config;

import java.io.File;

import org.literacybridge.acm.utils.FileUtil;

public class AcmPaths {

	private AcmProperties acmProperties;
		
	private File globalShareDirectory;
	private File databaseDirectory;
	private File contentDirectory;

	private final static String LiteracybridgeHomeDirName		= ".literacybridge";
	private final static String DropboxDefaultHomeFolderUnix	= "Dropbox";
	private final static String DropboxDefaultHomeFolderWindows	= "My Documents/Dropbox";

	private final static String DerbyDatabaseHomeDirName 	= ".db";
	private final static String ContentDirectoryName 		= "content";
	private final static String DefaultDatabasePath			= "ACM-ops/" + DerbyDatabaseHomeDirName;
	private final static String DefaultContentPath			= "ACM-ops/" + ContentDirectoryName;	
	private final static String CacheDirectoryName	    	= "cache";
	private final static String TBBuildsHomeDirName			= "TB-builds";

	
	public final static String USER_WRITE_LOCK_FILENAME		= "locked.txt";
	public final static String DB_ACCESS_FILENAME			= "accessList.txt";

	private final static File UserHomeDirectory = new File(System.getProperty("user.home", "."));

	// This must be always the initial root directory, if ACM already exists on a machine
	private final static File DefaultLiteracyBridgeDirectory = new File(UserHomeDirectory, LiteracybridgeHomeDirName);
	private final static File LiteracyBridgeCacheDirectory = new File(DefaultLiteracyBridgeDirectory, CacheDirectoryName);
	private final static File TBBuildsHomeDirectory = new File(DefaultLiteracyBridgeDirectory, TBBuildsHomeDirName);
	// Fallback paths (old behavior)
	private final static File FallbackDatabaseDirectoryPath = new File(DefaultLiteracyBridgeDirectory, DerbyDatabaseHomeDirName);
	private final static File FallbackContentDirectoryPath = new File(DefaultLiteracyBridgeDirectory, ContentDirectoryName);
	
	
	public AcmPaths(AcmProperties acmProperties) {
		initialize(acmProperties);
	}
	
	private boolean initialize(AcmProperties acmProperties) {
		this.acmProperties = acmProperties;
		
		boolean globalShareDirectoryFound = searchGlobalShareDirectory();
		boolean hasContentDirectory = searchContentDirectory();
		boolean hasDatabaseDirectory = searchDatabaseDirectory();
		
		return globalShareDirectoryFound && hasContentDirectory && hasDatabaseDirectory;
	}
	
	// Should always exist
	public static File getLiteracyBridgeSystemDirectory() {
		return DefaultLiteracyBridgeDirectory;
	}
	
	public File getLiteracyBridgeCacheDirectory() {
		return LiteracyBridgeCacheDirectory;
	}
	
	public File getContentDirectory() {
		return contentDirectory;
	}
	
	public File getDatabaseDirectory() {
		return databaseDirectory;
	}
	
	public File getGlobalShareDirectory() {
		return globalShareDirectory;
	}
	
	public File getTBBuildsHomeDirectory() {
		return TBBuildsHomeDirectory;
	}
		
	public void setDatabaseDirectory(File databaseDirectory) {
		this.databaseDirectory = databaseDirectory;
	}
	
	public void setContentDirectory(File contentDirectory) {
		this.contentDirectory = contentDirectory;
	}

	public File getLockFile() {
		return new File(getDatabaseDirectory(), USER_WRITE_LOCK_FILENAME);
	}

	public File getDBAccessFile() {
		return new File(getDatabaseDirectory(), DB_ACCESS_FILENAME);
	}
		
	public boolean HasValidGlobalShareDirectory() {
		return globalShareDirectory != null;
	}
	
	public boolean HasValidDatabaseDirectory() {
		return databaseDirectory != null;
	}
	
	public boolean HasValidContentDirectory() {
		return contentDirectory != null;
	}
		
	private boolean searchDatabaseDirectory() {
		File databaseDirectory = getDatabaseDirectoryfromPropertiesOrNull();
		if (databaseDirectory != null) {
			this.databaseDirectory = databaseDirectory;
		} else {
			tryFindDatabaseDirectory();
		}
		
		return HasValidDatabaseDirectory();
	}
	
	private void tryFindDatabaseDirectory() {
		// search for default folder in the shared folder
		if (HasValidGlobalShareDirectory()) {
			File defaultDatabaseFile = new File(globalShareDirectory, DefaultDatabasePath);
			if (FileUtil.isValidDirectory(defaultDatabaseFile )) {
				this.databaseDirectory = defaultDatabaseFile;
			}
		}
	}
		
	private boolean searchContentDirectory() {
		File contentDirectory = getContentDirectoryfromPropertiesOrNull();
		if (contentDirectory != null) {
			this.contentDirectory = contentDirectory;
		} else {
			tryFindContentDirectory();
		}
		
		return HasValidContentDirectory();
	}
	
	private void tryFindContentDirectory() {
		// search for default folder in the shared folder
		if (HasValidGlobalShareDirectory()) {
			File defaultContentFile = new File(globalShareDirectory, DefaultContentPath);
			if (FileUtil.isValidDirectory(defaultContentFile)) {
				this.contentDirectory = defaultContentFile;
			}
		}
	}
	
	private boolean searchGlobalShareDirectory() {
		File globalShareDirectory = getGlobalShareDirectoryfromPropertiesOrNull();
		if (globalShareDirectory != null) {
			this.globalShareDirectory = globalShareDirectory;	// found
		} else {
			trySetDropboxDirectories();	// fallback
		}
	
		return HasValidGlobalShareDirectory();
	}
	
	private File getContentDirectoryfromPropertiesOrNull() {
		File file = null;
		String defaultContentDirectoryPath = acmProperties.getDefaultRepositoryFilePathOrNull();
		if (defaultContentDirectoryPath != null) {
			file = FileUtil.GetDirectoryOrNull(defaultContentDirectoryPath);
		}
		
		return file;
	}
	
	private File getDatabaseDirectoryfromPropertiesOrNull() {
		File file = null;
		String defaultDatabaseDirectoryPath = acmProperties.getDefaultDatabaseFilePathOrNull();
		if (defaultDatabaseDirectoryPath != null) {
			file = FileUtil.GetDirectoryOrNull(defaultDatabaseDirectoryPath);
		}
		
		return file;
	}
	
	private File getGlobalShareDirectoryfromPropertiesOrNull() {
		File file = null;
		String globalShareDirectoryPath = acmProperties.getGlobalShareDirectoryPathOrNull();
		if (globalShareDirectoryPath != null) {
			file = FileUtil.GetDirectoryOrNull(globalShareDirectoryPath);
		}
		
		return file;
	}
	
	private void trySetDropboxDirectories() {
		// Unix
		File dropboxUnixFolder = new File(UserHomeDirectory, DropboxDefaultHomeFolderUnix);
		if (FileUtil.isValidDirectory(dropboxUnixFolder)) {
			this.globalShareDirectory = dropboxUnixFolder;
		}
		
		// Windows
		File dropboxWindowsFolder = new File(UserHomeDirectory, DropboxDefaultHomeFolderWindows);
		if (FileUtil.isValidDirectory(dropboxWindowsFolder)) {
			this.globalShareDirectory = dropboxWindowsFolder;
		}
	}

	public static File getFallbackDatabaseDirectoryPath() {
		return FallbackDatabaseDirectoryPath;
	}

	public static File getFallBackContentDirectoryPath() {
		return FallbackContentDirectoryPath;
	}
}
