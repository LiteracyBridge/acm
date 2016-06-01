package org.literacybridge.core.tbloader;

public class TBLoaderConstants {
  public static final String UNPUBLISHED_REV = "UNPUBLISHED";
  public static final String COLLECTED_DATA_SUBDIR_NAME = "collected-data";
  public static final String COLLECTED_DATA_DROPBOXDIR_PREFIX = "tbcd";
  public static final String COLLECTION_SUBDIR =
      "/" + COLLECTED_DATA_SUBDIR_NAME;
  public static String TEMP_COLLECTION_DIR = "";
  public static final String SW_SUBDIR = "./software/";
  public static final String CONTENT_SUBDIR = "./content/";
  public static final String CONTENT_BASIC_SUBDIR = "basic/";
  public static final String COMMUNITIES_SUBDIR = "communities/";
  public static final String IMAGES_SUBDIR = "images/";
  public static final String SCRIPT_SUBDIR = SW_SUBDIR + "scripts/";
  public static final String NO_SERIAL_NUMBER = "UNKNOWN";
  public static final String NEED_SERIAL_NUMBER = "-- to be assigned --";
  public static final String NO_DRIVE = "(nothing connected)";
  public static final String TRIGGER_FILE_CHECK = "checkdir";
  public static final int STARTING_SERIALNUMBER = 0;
  public static final String DEFAULT_GROUP_LABEL = "default";
  public static final String GROUP_FILE_EXTENSION = ".grp";
  public static final String DEVICE_FILE_EXTENSION = ".dev";
}
