package org.literacybridge.acm.config;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import org.literacybridge.acm.Constants;

@SuppressWarnings("serial")
public class DBInfo extends Properties {
    private boolean checkedOut;
	private final static String DB_NAME = "DB_NAME";
	private final static String DB_KEY = "DB_KEY";
	private final static String DB_CURRENT_FILENAME = "DB_CURRENT_FILENAME";
	private final static String DB_NEXT_FILENAME = "DB_NEXT_FILENAME";
	
    
	public String getDbName() {
		return getProperty(DBInfo.DB_NAME);
	}
	public void setDbName(String dbName) {
		setProperty(DBInfo.DB_NAME, dbName);
	}
	public String getDbKey() {
		return getProperty(DBInfo.DB_KEY);
	}
	public void setDbKey(String dbKey) {
		setProperty(DBInfo.DB_KEY, dbKey);
	}
	public String getCurrentFilename() {
		return getProperty(DBInfo.DB_CURRENT_FILENAME);
	}
	public String getNextFilename() {
		return getProperty(DBInfo.DB_NEXT_FILENAME);
	}
   	public void setFilenames(String currentFilename, String nextFilename) {
		setProperty(DBInfo.DB_CURRENT_FILENAME, currentFilename);
		setProperty(DBInfo.DB_NEXT_FILENAME,nextFilename);
	}
	public boolean isCheckedOut() {
		return checkedOut;
	}
	
	public void setCheckedOut(boolean checkedOut) {
		this.checkedOut = checkedOut;
		if (checkedOut)
			writeProps();  // this is the only time we need to write properties to disk
	}
	
	public void writeProps() {
		try {
			File dbDir = Configuration.getDatabaseDirectory();
			if (!dbDir.exists())
				dbDir.mkdirs();
			BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(getCheckedOutPropertiesFile()));
			super.store(out, null);
			out.flush();
			out.close();
		} catch (IOException e) {
			throw new RuntimeException("Unable to write configuration file: " + getCheckedOutPropertiesFile(), e);
		}
	}

	public void deleteCheckoutFile() {
		File f = getCheckedOutPropertiesFile();
		f.delete();
	}
	
	private File getCheckedOutPropertiesFile() {
		File fDB = new File(Configuration.getTempACMsDirectory());
		return new File(fDB, Configuration.getSharedACMname() + Constants.CHECKOUT_PROPERTIES_SUFFIX);
	}

	public DBInfo() {
		File f = getCheckedOutPropertiesFile();
		if (f.exists()) {
			try {
				BufferedInputStream in = new BufferedInputStream(new FileInputStream(f));
				load(in);
				in.close();
				checkedOut = true; // using the setter would cause an immediate rewrite of the same file
			} catch (IOException e) {
				checkedOut = false;
			}
		}
	}
}

