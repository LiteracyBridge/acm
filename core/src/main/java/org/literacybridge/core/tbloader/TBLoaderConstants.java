package org.literacybridge.core.tbloader;

import org.literacybridge.core.fs.RelativePath;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Pattern;

@SuppressWarnings("WeakerAccess")
public class TBLoaderConstants {
  public static final String UNKNOWN = "UNKNOWN";

  // group(1) is the deployment, group(2) is the revision.
  public final static Pattern DEPLOYMENT_REVISION_PATTERN = Pattern.compile("(.*)-([a-zA-Z]+)(?i:\\.rev)?$");
  public static final String UNPUBLISHED_REV = "UNPUBLISHED";
  // group(1) is timestamp, group(2) is deployment.
  public final static Pattern UNPUBLISHED_DEPLOYMENT_PATTERN =
                                  Pattern.compile(UNPUBLISHED_REV + "_([0-9.Z]*)_(.*?)(\\.rev)?$");
  public final static String UNPUBLISHED_REVISION_FORMAT = UNPUBLISHED_REV + "_%s_%s.rev";

  public final static String OLD_TB_SRN_PREFIX = "A-";
  public final static String NEW_TB_SRN_PREFIX = "C-";
  public final static List<String> VALID_SRN_PREFIXES = Arrays.asList(OLD_TB_SRN_PREFIX, NEW_TB_SRN_PREFIX);


    public static final String COLLECTED_DATA_SUBDIR_NAME = "collected-data";
  public static final String COLLECTED_DATA_DROPBOXDIR_PREFIX = "tbcd";
  //public static final String COLLECTION_SUBDIR = "/"
  //    + COLLECTED_DATA_SUBDIR_NAME;
  //public static String TEMP_COLLECTION_DIR = "";
  public static final String SOFTWARE_SUBDIR = "software";
  public static final String CONTENT_SUBDIR = "content";
  public static final String CONTENT_BASIC_SUBDIR = "basic";
  public static final RelativePath COMMUNITIES_SUBDIR = RelativePath.parse("communities");
  public static final RelativePath IMAGES_SUBDIR = RelativePath.parse("images");
  //public static final String SCRIPT_SUBDIR = SW_SUBDIR + "scripts/";
  public static final String NEED_SERIAL_NUMBER = "-- to be assigned --";
  public static final String MISSING_PACKAGE = "ERROR!  MISSING CONTENT IMAGE!";
  public static final String NO_DRIVE = "(nothing connected)";
  //public static final String TRIGGER_FILE_CHECK = "checkdir";
  public static final int STARTING_SERIALNUMBER = 0;
  public static final String DEFAULT_GROUP_LABEL = "default";
  public static final String GROUP_FILE_EXTENSION = ".grp";
  public static final String DEVICE_FILE_EXTENSION = ".dev";
  public static final String PROJECT_FILE_EXTENSION = ".prj";

  public static final RelativePath BINARY_STATS_PATH = RelativePath.parse("statistics/stats/flashData.bin");
  public static final RelativePath BINARY_STATS_ALTERNATIVE_PATH = RelativePath.parse("statistics/flashData.bin");

  public static final RelativePath TB_SYSTEM_PATH = RelativePath.parse("system");
  public static final RelativePath TB_LANGUAGES_PATH = RelativePath.parse("languages");
  public static final RelativePath TB_MESSAGES_PATH = RelativePath.parse("messages");
  public static final RelativePath TB_LISTS_PATH = RelativePath.parse("messages/lists");
  public static final RelativePath TB_AUDIO_PATH = RelativePath.parse("messages/audio");

  public static final RelativePath SYS_DATA_TXT = RelativePath.parse("sysdata.txt");
  public static final RelativePath DEPLOYMENT_PROPERTIES_NAME = RelativePath.parse("deployment.properties");
  public static final RelativePath STATS_COLLECTED_PROPERTIES_NAME = RelativePath.parse("stats_collected.properties");
  public static final RelativePath DIRS_TXT = RelativePath.parse("dir.txt");
  public static final RelativePath DIRS_POST_TXT = RelativePath.parse("dir_post.txt");

  public static final String TALKING_BOOK_DATA = "TalkingBookData";
  public static final String OPERATIONAL_DATA = "OperationalData";
  public static final String USER_RECORDINGS = "userrecordings";

    public static final TimeZone UTC = TimeZone.getTimeZone("UTC");
    public static final DateFormat ISO8601 = new SimpleDateFormat("yyyyMMdd'T'HHmmss.SSS'Z'", Locale.US); // Quoted "Z" to indicate UTC, no timezone offset
    public static final DateFormat ISO8601date = new SimpleDateFormat("yyyyMMdd", Locale.US);
    public static final DateFormat ISO8601time = new SimpleDateFormat("HHmmss.SSS'Z'", Locale.US); // Quoted "Z" to indicate UTC, no timezone offset

    public static final String ACCEPTABLE_FIRMWARE_VERSIONS = "ACCEPTABLE_FIRMWARE_VERSIONS";

    public static final String ACTION_PROPERTY = "ACTION";      // The action that was requested.
    public static final String CLEARED_FLASH_PROPERTY = "CLEARED_FLASH";
    public static final String TB_LOG_ACTION_PROPERTY = "TB_LOG_ACTION"; // The action as it was logged in the tb log file.
    public static final String PROJECT_PROPERTY = "PROJECT";
    public static final String DEPLOYMENT_PROPERTY = "DEPLOYMENT";
    public static final String DEPLOYMENT_NUMBER_PROPERTY = "DEPLOYMENT_NUMBER";
    public static final String PACKAGE_PROPERTY = "PACKAGE";
    public static final String COMMUNITY_PROPERTY = "COMMUNITY";
    public static final String RECIPIENTID_PROPERTY = "RECIPIENTID";
    public static final String TALKING_BOOK_ID_PROPERTY = "TALKINGBOOKID";
    public static final String TIMESTAMP_PROPERTY = "TIMESTAMP";
    public static final String TEST_DEPLOYMENT_PROPERTY = "TESTDEPLOYMENT";
    public static final String USERNAME_PROPERTY = "USERNAME";
    public static final String USEREMAIL_PROPERTY = "USEREMAIL";
    public static final String LOCATION_PROPERTY = "LOCATION";
    public static final String COORDINATES_PROPERTY = "COORDINATES";
    public static final String TBCDID_PROPERTY = "TBCDID";
    public static final String NEW_SERIAL_NUMBER_PROPERTY = "NEWTBID";
    public static final String FIRMWARE_PROPERTY = "FIRMWARE";
    public static final String LATEST_FIRMWARE_PROPERTY = "LATEST_FIRMWARE";
    public static final String DEPLOYMENT_UUID_PROPERTY = "DEPLOYEDUUID";
    public static final String STATS_COLLECTED_UUID_PROPERTY = "STATSUUID";
    public static final String DEPLOYMENT_CREATION_DATE = "DEPLOYMENT_CREATION_DATE";
    public static final String DEPLOYMENT_CREATION_TIME = "DEPLOYMENT_CREATION_TIME";
    public static final String DEPLOYMENT_CREATION_USER = "DEPLOYMENT_CREATION_USER";
    public static final String DEPLOYMENT_NUMBER = "DEPLOYMENT_NUMBER";
    public static final String PROGRAM_DESCRIPTION_PROPERTY = "DESCRIPTION";
    public static final String PROGRAM_ID_PROPERTY = "PROGRAMID";

    static {
        ISO8601.setTimeZone(UTC);
    }

}
