package org.literacybridge.acm.config;

import org.literacybridge.acm.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

@SuppressWarnings("serial")
public class DBInfo extends Properties {
  private static final Logger LOG = LoggerFactory.getLogger(DBInfo.class);

  private boolean isCheckedOut;
  private final static String DB_NAME = "DB_NAME";
  private final static String DB_CHECKOUT_KEY = "DB_KEY";
  //for AWS
  private final static String AWS_KEY = "AWS_KEY";
  //
  private final static String DB_CURRENT_FILENAME = "DB_CURRENT_FILENAME";
  private final static String DB_NEXT_FILENAME = "DB_NEXT_FILENAME";

  private final static String DB_IS_NEW_CHECKOUT = "DB_IS_NEW_CHECKOUT";
  private final DBConfiguration config;

  public String getDbName() {
    return getProperty(DBInfo.DB_NAME);
  }

  public void setDbName(String dbName) {
    setProperty(DBInfo.DB_NAME, dbName);
  }

  public String getCheckoutKey() {
    String key = getProperty(DBInfo.AWS_KEY);
    if (key == null) {
      key = getProperty(DBInfo.DB_CHECKOUT_KEY);
    }
    return key;
  }

  public void setCheckoutKey(String dbKey) {
    setProperty(DBInfo.DB_CHECKOUT_KEY, dbKey);
  }

  public String getCurrentFilename() {
    return getProperty(DBInfo.DB_CURRENT_FILENAME);
  }

  public String getNextFilename() {
    return getProperty(DBInfo.DB_NEXT_FILENAME);
  }

  public void setFilenames(String currentFilename, String nextFilename) {
    setProperty(DBInfo.DB_CURRENT_FILENAME, currentFilename);
    setProperty(DBInfo.DB_NEXT_FILENAME, nextFilename);
  }

  public boolean isCheckedOut() {
    return isCheckedOut;
  }

  public void setCheckedOut() {
    // Already set? If not, persist it.
    boolean needWrite = !this.isCheckedOut;
    this.isCheckedOut = true;
    if (needWrite) {
        writeProps(); // this is the only time we need to write properties to disk
    LOG.info("Wrote checkout marker file.");
    }
  }

  public boolean isNewCheckoutRecord() {
    String prop = getProperty(DB_IS_NEW_CHECKOUT);
    return prop != null;
  }
  public void setNewCheckoutRecord() {
    setProperty(DB_IS_NEW_CHECKOUT, "true");
    // If the db is checked out, the file has been written. Update it with this info.
    // If the db is not checked out, it won't be saved, unless it is checked out
    // first, in which case the properties are persisted.
    if (isCheckedOut)
      writeProps();
  }

  public void writeProps() {
    try {
      File dbDir = config.getLocalTempDbDir();
      if (!dbDir.exists())
        dbDir.mkdirs();
      BufferedOutputStream out = new BufferedOutputStream(
          new FileOutputStream(getCheckedOutPropertiesFile()));
      super.store(out, null);
      out.flush();
      out.close();
    } catch (IOException e) {
      throw new RuntimeException("Unable to write configuration file: "
          + getCheckedOutPropertiesFile(), e);
    }
  }

  public void deleteCheckoutFile() {
    File f = getCheckedOutPropertiesFile();
    f.delete();
    LOG.info("Deleted checkout marker file.");
  }

  private File getCheckedOutPropertiesFile() {
    return config.getPathProvider().getLocalCheckoutFile();
  }

  public DBInfo(DBConfiguration config) {
    this.config = config;
    File f = getCheckedOutPropertiesFile();
    if (f.exists()) {
      try {
        BufferedInputStream in = new BufferedInputStream(
            new FileInputStream(f));
        load(in);
        in.close();
        isCheckedOut = true; // using the setter would cause an immediate rewrite of the same file
          System.out.print("Checkout marker file exists, db is checked out.\n");
      } catch (IOException e) {
        isCheckedOut = false;
          System.out.print("Can't read checkout marker file, but it exists; db is NOT checked out.\n");
      }
    }
  }
}
