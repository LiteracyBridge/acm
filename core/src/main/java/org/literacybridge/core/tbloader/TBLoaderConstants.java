package org.literacybridge.core.tbloader;

import java.util.HashSet;
import java.util.Set;

import org.literacybridge.core.fs.RelativePath;
import org.literacybridge.core.fs.TBFileSystem;
import org.literacybridge.core.fs.TBFileSystem.FilenameFilter;

public class TBLoaderConstants {
  public static final String UNPUBLISHED_REV = "UNPUBLISHED";
  public static final String COLLECTED_DATA_SUBDIR_NAME = "collected-data";
  public static final String COLLECTED_DATA_DROPBOXDIR_PREFIX = "tbcd";
  public static final String COLLECTION_SUBDIR = "/"
      + COLLECTED_DATA_SUBDIR_NAME;
  public static String TEMP_COLLECTION_DIR = "";
  public static final String SW_SUBDIR = "./software/";
  public static final String CONTENT_SUBDIR = "./content";
  public static final RelativePath CONTENT_BASIC_SUBDIR = RelativePath.parse("basic");
  public static final RelativePath COMMUNITIES_SUBDIR = RelativePath.parse("communities");
  public static final RelativePath IMAGES_SUBDIR = RelativePath.parse("images");
  public static final String SCRIPT_SUBDIR = SW_SUBDIR + "scripts/";
  public static final String NO_SERIAL_NUMBER = "UNKNOWN";
  public static final String NEED_SERIAL_NUMBER = "-- to be assigned --";
  public static final String NO_DRIVE = "(nothing connected)";
  public static final String TRIGGER_FILE_CHECK = "checkdir";
  public static final int STARTING_SERIALNUMBER = 0;
  public static final String DEFAULT_GROUP_LABEL = "default";
  public static final String GROUP_FILE_EXTENSION = ".grp";
  public static final String DEVICE_FILE_EXTENSION = ".dev";
  public static final String PROJECT_FILE_EXTENSION = ".prj";

  public static final RelativePath BINARY_STATS_PATH = RelativePath.parse("statistics/stats/flashData.bin");
  public static final RelativePath BINARY_STATS_ALTERNATIVE_PATH = RelativePath.parse("statistics/flashData.bin");

  public static final RelativePath TB_SYSTEM_PATH = RelativePath.parse("system");
  public static final RelativePath TB_LANGUAGES_PATH = RelativePath.parse("languages");
  public static final RelativePath TB_LISTS_PATH = RelativePath.parse("messages/lists");
  public static final RelativePath TB_AUDIO_PATH = RelativePath.parse("messages/audio");

  public static final RelativePath SYS_DATA_TXT = RelativePath.parse("sysdata.txt");

  public static final String TB_DATA_PATH = "TalkingBookData";
  public static final String USER_RECORDINGS_PATH = "UserRecordings";

  public static final FilenameFilter XCOPY_EXCLUDE_FILTER;
  static {
      final Set<String> XCOPY_EXCLUDE_FILES = new HashSet<String>();
      for (String exclude : new String[] {"languages", "messages", "ostats", "Inbox", "archive", "img", "old", "config.bin", ".Spotlight-V100", "Android"}) {
        XCOPY_EXCLUDE_FILES.add(exclude);
      }
      XCOPY_EXCLUDE_FILTER = new FilenameFilter() {
        @Override
        public boolean accept(TBFileSystem fs, RelativePath dir, String name) {
          return !XCOPY_EXCLUDE_FILES.contains(name);
        }
      };
  }
}
