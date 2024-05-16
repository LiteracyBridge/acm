package org.literacybridge.acm.config;

import org.literacybridge.acm.Constants;

import java.io.File;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.logging.Logger;

/**
    File structure.
        ~/Amplio
            acm_config.properties       # Recording counter, "device id", contact info
            programs.info               # Information about the programs to which this user has access
            credentials.info            # email, user name, lightly obfuscated password, cognito claims
            tbsrnstore.info             # SRN allocation info for users who have logged on to TB-Loader
            acm-dbs                     # Per program directory of content database
            TB-History                  # Cached global, and not-yet-uploaded local deployment & collection history
            TB-Loaders                  # Per program directory of current deployment data
            ACM
                acm.jar                 # Main file for acm
                ...                     # Other acm files
                lib / *                 # Other .jar files for acm
                jre                     # Local java jre installation
                converters              # Contains ffmpeg and audiobatchconverter
            cache                       # Intended for .wav files
            temp                        # Intended for things not to be written
            collectiondir               # Statistics and UF is collected here
            uploadqueue                 # When ready to be uploaded to S3, files are moved here.
            sandbox                     # Changes to the program data is staged here, until the user commits it
            updates                     # Installers are downloaded and kept up-to-date here


 */
public class AmplioHome {
    private static final Logger LOG = Logger.getLogger(AmplioHome.class.getName());

    private final static File USER_HOME_DIR = new File(System.getProperty("user.home", "."));

    private static final String ACM_DB_DIRS_ROOT = "acm-dbs";

    public enum VERSION {
        v1("LiteracyBridge"),       // "classic", LiteracyBridge home directory.
        v2("Amplio");               // Amplio home directory

        VERSION(String directoryName) {
            this.directoryName = directoryName;
        }
        final String directoryName;
    }

    ///////////////////////////////////////////////////////////////////////////
    //
    // Begin instance data and methods.
    //
    // Where we keep app specific data. This is config files, cached data, logs and stats queued for
    // upload, content data, etc.
    // Which home directory is it? Old-style "LiteracyBridge" or new style "Amplio"?
    private VERSION version;
    // The file matching the home directory, ~/LiteracyBridge or ~/Amplio
    private File homeDirectory;
    AmplioHome() {
        this.homeDirectory = new File("Does not exist");
    }

    protected File getHomeDirectory() {
        return homeDirectory;
    }

    protected File getTempDirectory() {
        File dir;
        if (version == VERSION.v1) {
            dir = new File(getAppAcmDir(), Constants.TempDir);
        }
        else {
            dir = new File(getHomeDirectory(), Constants.TempDir);
        }
        if (!dir.exists()) //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
        return dir;
    }

    private File sandboxesDir = null;

    protected synchronized File getSandboxesDirectory() {
        if (sandboxesDir == null) {
            File dir;
            String override = System.getenv("sandboxes");
            if (override != null && override.length() > 0) {
                dir = new File(override);
                boolean sandboxesOverride = true;
            } else if (version == VERSION.v1) {
                dir = new File(getAppAcmDir(), "sandbox");
            } else {
                dir = new File(getHomeDirectory(), "sandbox");
            }
            if (!dir.exists()) //noinspection ResultOfMethodCallIgnored
                dir.mkdirs();
            sandboxesDir = dir;
        }
        return sandboxesDir;
    }
    //
    // End instance data and methods
    //
    ///////////////////////////////////////////////////////////////////////////
    //
    // Begin static data and methods.
    //

    private static AmplioHome instance = null;
    public static synchronized AmplioHome getInstance() {
        if (instance == null) {
            instance = new AmplioHome();
            findHomeDirectory(instance);
        }
        return instance;
    }

    /**
     * For tests.
     * @param testInstance The test AmplioHome instance.
     * @return the same instance.
     */
    @SuppressWarnings("unused")
    public synchronized static AmplioHome getInstance(AmplioHome testInstance) {
        instance = testInstance;
        return instance;
    }

    /**
     * The application home directory, ~/LiteracyBridge or ~/Amplio.
     * @return the home directory.
     */
    public synchronized static File getDirectory() {
        return getInstance().getHomeDirectory();
    }

    public static boolean isOldStyleHomeDirectory() {
        return getInstance().version == VERSION.v1;
    }
    public static boolean isOldStyleSetup() { return isOldStyleHomeDirectory(); }

    private static void findHomeDirectory(AmplioHome instance) {
        File homeDir = null;
        VERSION version = VERSION.v1; // If we can't tell.
        // Look for the directory from which we're running.
        File executableFile = new File(AmplioHome.class.getProtectionDomain().getCodeSource().getLocation().getPath());
        if (executableFile.exists() && executableFile.isFile()) {
            // Build a list of the containing directories for our running code, leaf directory last. Only care about
            // the user's home directory tree.
            Path homePath = USER_HOME_DIR.toPath();
            LinkedList<File> parentDirs = new LinkedList<>();
            File parentDir = executableFile.getParentFile();
            boolean foundHome = false;
            while (parentDir != null) {
                parentDirs.addFirst(parentDir);
                parentDir = parentDir.getParentFile();
                if (parentDir != null && parentDir.toPath().equals(homePath)) {
                    foundHome = true;
                    break;
                }
            }
            // Was the running code's containing directory found in the user's home directory? If so, look for
            // Amplio or LiteracyBridge in that list of parent directories.
            if (foundHome) {
                for (File dir : parentDirs) {
                    String name = dir.getName();
                    if (name.equalsIgnoreCase(VERSION.v1.directoryName) || name.equalsIgnoreCase(VERSION.v2.directoryName)) {
                        homeDir = dir;
                        version = name.equalsIgnoreCase(VERSION.v1.directoryName) ? VERSION.v1 : VERSION.v2;
                        break;
                    }
                }
            }
        }
        // If we didn't find the running code's parent to be Amplio or LiteracyBridge, in the user's home
        // directory tree, look for those two directly.
        if (homeDir == null) {
            homeDir = new File(USER_HOME_DIR, VERSION.v2.directoryName);
            if (homeDir.exists() && homeDir.isDirectory()) {
                version = VERSION.v2;
            } else {
                File legacyDir = new File(USER_HOME_DIR, VERSION.v1.directoryName);
                if (legacyDir.exists() && legacyDir.isDirectory()) {
                    homeDir = legacyDir;
                    //noinspection ConstantConditions
                    version = VERSION.v1;
                }
            }
        }
        // Either ~/LiteracyBridge or ~/Amplio
        instance.homeDirectory = homeDir;
        instance.version = version;
        System.out.printf("Found %s Amplio home directory in %s\n", version, homeDir.getName());
    }



    /**
     * ACM databases go here.
     *
     * @return ~/Amplio/acm-dbs
     */
    public static File getHomeDbsRootDir() {
        return new File(getDirectory(), ACM_DB_DIRS_ROOT);
    }

    /**
     * The ACM software and some temporary ACM runtime data directories goes here.
     *
     * AKA 'File getCodeDir()'
     *
     * @return ~/Amplio/ACM
     */
    public static File getAppAcmDir() {
        return new File(getDirectory(), "ACM");
    }

    /**
     * The ACM software proper.
     *
     * @return ~/Amplio/ACM/software
     */
    public static File getAppSoftwareDir() {
        if (getInstance().version == VERSION.v1) {
            return new File(getAppAcmDir(), "software");
        } else {
            return getAppAcmDir();
        }
    }

    /**
     * @return The location of the a18 conversion utilities. Windows only.
     */
    public static File getA18UtilsDir() {
        return new File(getAppSoftwareDir(), "converters" + File.separatorChar + "a18");
    }

    /**
     * @return The location of the STM32 "cube" utilities. Windows only.
     */
    public static File getStmUtilsDir() {
        return new File(getAppSoftwareDir(), "cube");
    }

    public static File getLogsDir() {
        return new File(getDirectory(), "logs");
    }

    /**
     * ACM temporary files.
     *
     * @return ~/Amplio/temp or ~/LiteracyBridge/ACM/temp
     */
    public static File getTempsDir() {
        return getInstance().getTempDirectory();
    }

    public static File getSandboxesDir() {
        return getInstance().getSandboxesDirectory();
    }
    /**
     * Audio files in alternate formats. We don't want to upload them, but we also don't want to
     * have to re-create them again. So cache them here.
     *
     * @return ~/Amplio/cache or ~/LiteracyBridge/ACM/cache
     */
    public static File getCachesDir() {
        if (getInstance().version == VERSION.v1) {
            return new File(getAppAcmDir(), Constants.CACHE_DIR_NAME);
        } else {
            return new File(getDirectory(), Constants.CACHE_DIR_NAME);
        }
    }

    /**
     * The ~/Amplio/acm_config.properties or ~/LiteracyBridge/acm_config.properties file.
     * Per computer information.
     *
     * @return it.
     */
    public static File getUserConfigFile() {
        return new File(getDirectory(), Constants.USERS_APPLICATION_PROPERTIES);
    }

    /**
     * TB-Loaders are built here, and downloaded and expanded here.
     *
     * @return ~/Amplio/TB-Loaders or ~/LiteracyBridge/TB-Loaders
     */
    public static File getLocalTbLoadersDir() {
        return new File(getDirectory(), Constants.TBLoadersHomeDir);
    }

    public static File getTbLoaderHistoriesDir() {
        return new File(getDirectory(), Constants.TBLoadersHistoryDir);
    }

}
