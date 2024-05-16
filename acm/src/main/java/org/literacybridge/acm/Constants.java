package org.literacybridge.acm;

import java.io.File;

public class Constants {
  public final static String ACM_VERSION = "r2311221"; // yy mm dd n
  public final static String LiteracybridgeHomeDirName = "LiteracyBridge";
  public final static String AmplioHomeDirName = "Amplio";
  public final static String ACM_DIR_NAME = "ACM";
  public final static String CACHE_DIR_NAME = "cache";
  public final static String TempDir = "temp";
  public final static String DBHomeDir = "db";
  public final static String RepositoryHomeDir = "content";
  public final static String LuceneIndexDir = "index";
  public final static String TBLoadersHomeDir = "TB-Loaders";
  public final static String TBLoadersHistoryDir = "TB-History";
  public final static String TBLoadersLogDir = "tbl-logs";
  public final static String TbCollectionWorkDir = "collectiondir";
  public final static String uploadQueue = "uploadqueue";
  public final static String ProgramSpecDir = "programspec";
  public final static String USERS_APPLICATION_PROPERTIES = "acm_config.properties";
  public final static String CONFIG_PROPERTIES = "config.properties";
  public final static String CHECKOUT_PROPERTIES_SUFFIX = "-checkedOut.properties";
  public final static String USER_FEEDBACK_INCLUDELIST_FILENAME = "userfeedback.includelist";
  public static final String CATEGORY_INCLUDELIST_FILENAME = "category.includelist";
  public final static File   USER_HOME_DIR = new File(System.getProperty("user.home", "."));
  public final static File   JAVA_TMP_DIR = new File(System.getProperty("java.io.tmpdir"));
  public final static long   DEFAULT_CACHE_SIZE_IN_BYTES = 2L * 1024L * 1024L * 1024L; // 2GB

  public final static String USER_NAME = "USER_NAME";
  public final static String USER_CONTACT_INFO = "USER_CONTACT_INFO";
  @Deprecated
  public final static String GLOBAL_SHARE_PATH = "GLOBAL_SHARE_PATH";
  public final static String RECORDING_COUNTER_PROP = "RECORDING_COUNTER";
  public final static String DEVICE_ID_PROP = "DEVICE_ID";
  public final static String AUDIO_LANGUAGES = "AUDIO_LANGUAGES";
  public final static String PRE_CACHE_WAV = "PRE_CACHE_WAV";
  public final static String CACHE_SIZE_PROP_NAME = "CACHE_SIZE_IN_BYTES";
  public final static String DESCRIPTION_PROP_NAME = "DESCRIPTION";
  public final static String FRIENDLY_NAME_PROP_NAME = "NAME";
  public final static String STRICT_DEPLOYMENT_NAMING = "STRICT_DEPLOYMENT_NAMING";
    public final static String USER_FEEDBACK_HIDDEN = "USER_FEEDBACK_HIDDEN";
    public final static String USER_FEEDBACK_PUBLIC = "USER_FEEDBACK_PUBLIC";
  public final static String DE_DUPLICATE_AUDIO = "DE_DUPLICATE_AUDIO";
  public final static String CONFIGURATION_DIALOG = "CONFIGURATION_DIALOG";
  public final static String NATIVE_AUDIO_FORMATS = "NATIVE_AUDIO_FORMATS";
  public final static String ALLOW_PACKAGE_CHOICE = "PACKAGE_CHOICE";
  // So that test machines can be configured to do frequent allocations.
  public final static String TB_SRN_ALLOCATION_SIZE = "TB_SRN_ALLOCATION_SIZE";
  public final static Integer TB_SRN_ALLOCATION_SIZE_DEFAULT = 512;
  public final static String LATEST_UPDATE_SETUP_WARNING = "LATEST_UPDATE_SETUP_WARNING";
  public final static String ALWAYS_WARN_SETUP = "ALWAYS_WARN_SETUP";
  public final static String BATCH_RECORD = "record.log";
  public final static String S3_BUCKET = "acm-logging";
  public final static String FUZZY_THRESHOLD = "FUZZY_THRESHOLD";
  public final static String WARN_FOR_MISSING_GREETINGS = "WARN_FOR_MISSING_GREETINGS";
  public final static String FORCE_WAV_CONVERSION = "FORCE_WAV_CONVERSION";
    public final static String HAS_TBV2_DEVICES = "HAS_TBV2_DEVICES";
  public final static String NOTIFY_LIST = "NOTIFY_LIST";

  // Gather obsolete property names here. We could write code to remove these from the properties file.
  public final static String[] OBSOLETE_PROPERTY_NAMES = {"NEXT_CORRELATION_ID", "DEPLOYMENT_CHOICE", "USE_AWS_LOCKING",
                                /*"USER_NAME", "USER_CONTACT_INFO"*/};

  public final static String CATEGORY_GENERAL_OTHER = "0-0";
  public final static String CATGEORY_GENERAL_AGRICULTURE = "1-0";
  public final static String CATEGORY_TB = "0-4";
  public final static String CATEGORY_TB_SYSTEM = "0-4-1";
  public final static String CATEGORY_TB_CATEGORIES = "0-4-2";
  public final static String CATEGORY_SURVEY = "0-8";
  public final static String CATEGORY_INTRO_MESSAGE = "0-5";
  public final static String CATEGORY_UNCATEGORIZED_FEEDBACK = "9-0";
  public static final String CATEGORY_TOO_SHORT_FEEDBACK = "92-2";
  public static final String CATEGORY_TOO_LONG_FEEDBACK = "92-6";
  public static final String CATEGORY_UNKNOWN_LENGTH_FEEDBACK = "92-8";
  public static final String CATEGORY_TB_INSTRUCTIONS = "0-1";
  public static final String CATEGORY_COMMUNITIES = "0-3";
  public static final String CATEGORY_TUTORIAL = "$0-1";

  public static final String TUTORIAL_LIST = CATEGORY_TUTORIAL + ".txt";

  public static final String BELL_SOUND_V1 = "0.a18";
  public static final String BELL_SOUND_V2 = "0.mp3";
  public static final String SILENCE_V1 = "7.a18";
  public static final String SILENCE_V2 = "7.mp3";
  public static final String CUSTOM_GREETING_V1 = "10.a18";

  public static final int FUZZY_THRESHOLD_MAXIMUM = 100;
  public static final int FUZZY_THRESHOLD_DEFAULT = 80;
  public static final int FUZZY_THRESHOLD_MINIMUM = 60;

  // Maximum length of a package name. This is a TB firmware restriction.
  public static final int MAX_PACKAGE_NAME_LENGTH = 17;

  public static final int AMPLIO_GREEN_R = 40;  // 0x28;
  public static final int AMPLIO_GREEN_G = 154; // 0x9a;
  public static final int AMPLIO_GREEN_B = 106; // 0x6a;
    public final static String NON_FILE_CHARS = "[\\\\/~;:*?'\"]";


    public enum USER_FEEDBACK_PUBLIC_OPTION {
        ALWAYS, OPT_OUT, OPT_IN, NEVER;

        public boolean isOverrideable() {
            return this==OPT_OUT || this==OPT_IN;
        }
        public boolean isPublic() {
            return this==ALWAYS || this==OPT_OUT;
        }
        public boolean isHidden() {
            return !isPublic();
        }
    }

}
