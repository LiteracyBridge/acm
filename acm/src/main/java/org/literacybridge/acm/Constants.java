package org.literacybridge.acm;

import java.io.File;
import java.util.regex.Pattern;

public class Constants {
  public final static String ACM_VERSION = "r2105241"; // yy mm dd n
  public final static String LiteracybridgeHomeDirName = "LiteracyBridge";
  public final static String AmplioHomeDirName = "Amplio";
  public final static String ACM_DIR_NAME = "ACM";
  public final static String CACHE_DIR_NAME = "cache";
  public final static String TempDir = "temp";
  public final static String DBHomeDir = "db";
  public final static String RepositoryHomeDir = "content";
  public final static String LuceneIndexDir = "index";
  public final static String TBLoadersHomeDir = "TB-Loaders";
  public final static String TBLoadersLogDir = "tbl-logs";
  public final static String TbCollectionWorkDir = "collectiondir";
  public final static String uploadQueue = "uploadqueue";
  public final static String ProgramSpecDir = "programspec";
  public final static String USERS_APPLICATION_PROPERTIES = "acm_config.properties";
  public final static String CONFIG_PROPERTIES = "config.properties";
  public final static String CHECKOUT_PROPERTIES_SUFFIX = "-checkedOut.properties";
  public final static String USER_FEEDBACK_INCLUDELIST_FILENAME = "userfeedback.includelist";
  public static final String CATEGORY_INCLUDELIST_FILENAME = "category.includelist";

    public final static File USER_HOME_DIR = new File(
      System.getProperty("user.home", "."));
  public final static long DEFAULT_CACHE_SIZE_IN_BYTES = 2L * 1024L * 1024L * 1024L; // 2GB

  public final static String USER_NAME = "USER_NAME";
  public final static String USER_CONTACT_INFO = "USER_CONTACT_INFO";
  public final static String GLOBAL_SHARE_PATH = "GLOBAL_SHARE_PATH";
  public final static String RECORDING_COUNTER_PROP = "RECORDING_COUNTER";
  public final static String DEVICE_ID_PROP = "DEVICE_ID";
  public final static String AUDIO_LANGUAGES = "AUDIO_LANGUAGES";
  public final static String PRE_CACHE_WAV = "PRE_CACHE_WAV";
  public final static String CACHE_SIZE_PROP_NAME = "CACHE_SIZE_IN_BYTES";
  public final static String DESCRIPTION_PROP_NAME = "DESCRIPTION";
  public final static String STRICT_DEPLOYMENT_NAMING = "STRICT_DEPLOYMENT_NAMING";
  public final static String USER_FEEDBACK_HIDDEN = "USER_FEEDBACK_HIDDEN";
  public final static String DE_DUPLICATE_AUDIO = "DE_DUPLICATE_AUDIO";
  public final static String CONFIGURATION_DIALOG = "CONFIGURATION_DIALOG";
  public final static String NATIVE_AUDIO_FORMATS = "NATIVE_AUDIO_FORMATS";
  // So that test machines can be configured to do frequent allocations.
  public final static String TB_SRN_ALLOCATION_SIZE = "TB_SRN_ALLOCATION_SIZE";
  public final static Integer TB_SRN_ALLOCATION_SIZE_DEFAULT = 512;
  public final static String BATCH_RECORD = "record.log";
  public final static String S3_BUCKET = "acm-logging";
  public final static String FUZZY_THRESHOLD = "FUZZY_THRESHOLD";
  public final static String WARN_FOR_MISSING_GREETINGS = "WARN_FOR_MISSING_GREETINGS";
  public final static String NOTIFY_LIST = "NOTIFY_LIST";

  // Gather obsolete property names here. We could write code to remove these from the properties file.
  public final static String[] OBSOLETE_PROPERTY_NAMES = {"NEXT_CORRELATION_ID", "DEPLOYMENT_CHOICE", "USE_AWS_LOCKING",
                                /*"USER_NAME", "USER_CONTACT_INFO"*/};

  public final static String CATEGORY_GENERAL_OTHER = "0-0";
  public final static String CATEGORY_TB_SYSTEM = "0-4-1";
  public final static String CATEGORY_TB_CATEGORIES = "0-4-2";
  public final static String CATEGORY_INTRO_MESSAGE = "0-5";
  public final static String CATEGORY_UNCATEGORIZED_FEEDBACK = "9-0";
  public static final String CATEGORY_TOO_SHORT_FEEDBACK = "92-2";
  public static final String CATEGORY_TOO_LONG_FEEDBACK = "92-6";
  public static final String CATEGORY_UNKNOWN_LENGTH_FEEDBACK = "92-8";
  public static final String CATEGORY_TB_INSTRUCTIONS = "0-1";
  public static final String CATEGORY_COMMUNITIES = "0-3";
  public static final String CATEGORY_TUTORIAL = "$0-1";

  public static final String TUTORIAL_LIST = CATEGORY_TUTORIAL + ".txt";

  public static final String BELL_SOUND = "0.a18";
  public static final String CUSTOM_GREETING = "10.a18";

  public static final int FUZZY_THRESHOLD_MAXIMUM = 100;
  public static final int FUZZY_THRESHOLD_DEFAULT = 80;
  public static final int FUZZY_THRESHOLD_MINIMUM = 60;

  // Maximum length of a package name. This is a TB firmware restriction.
  public static final int MAX_PACKAGE_NAME_LENGTH = 20;

  public static final int AMPLIO_GREEN_R = 40;  // 0x28;
  public static final int AMPLIO_GREEN_G = 154; // 0x9a;
  public static final int AMPLIO_GREEN_B = 106; // 0x6a;
}
