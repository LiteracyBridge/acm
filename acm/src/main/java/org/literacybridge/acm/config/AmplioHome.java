package org.literacybridge.acm.config;

import org.literacybridge.acm.Constants;
import org.literacybridge.acm.utils.DropboxFinder;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.Properties;
import java.util.logging.Logger;

/**
    File structure.
        ~/Dropbox (Amplio)      (or ~/Amplio/acm-dbs
            ACM-PROG-NAME               # various config files for the program
                TB-Loaders              # bat files to run TB-Loader
                    TB_Options
                        activeLists
                        basic
                        config_files
                        firmware
                        languages
                        system_menus
                    communities
                    metadata            # Partial metadata snapshot (This is incomplete)
                    packages            # "Packages" are exported here. These could be cleaned up immediately.
                    published           # The published deployments go here
                content                 # Deeply nested directory
                programspec             # Currently active program spec is cached here

        ~/Amplio
            acm-dbs                     # Per program directory of content database
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
            updater                     # Installers are downloaded and kept up-to-date here

        ~/LiteracyBridge
            acm_config.properties       # Recording counter, "device id", Dropbox path, contact info
            credentials.info            # email, user name, lightly obfuscated password, cognito claims
            tbsrnstore.info             # SRN allocation info for users who have logged on to TB-Loader
            ACM
                r1234.rev               # Version marker file
                ACM.bat                 # runs the ACM
                cache                   # Audio files we want to keep, but not sync
                software
                    build.properties    # timestamp, counter for the installed build
                    acm.jar
                    lib/*
                    converters/ *
                    icons/ *
                    RemoveDrive.exe
                    splash-acm.jpg
                    N .bat files         # invoke ../ACM.bat with program id as argument
                temp
            collected-data
            collectiondir
            logs
            status
            TB-Loaders
            uploadqueue
 */
public class AmplioHome {
    private static final Logger LOG = Logger.getLogger(AmplioHome.class.getName());

    private final static File USER_HOME_DIR = new File(System.getProperty("user.home", "."));
    private final static String LiteracybridgeHomeDirName = "LiteracyBridge";
    private final static String AmplioHomeDirName = "Amplio";

    private static final String ACM_DB_DIRS_ROOT = "acm-dbs";

    ///////////////////////////////////////////////////////////////////////////
    //
    // Begin instance data and methods.
    //
    // Where we keep app specific data. This is config files, cached data, logs and stats queued for
    // upload, content data, etc.
    private File homeDirectory;
    private boolean oldStyleHomeDirectory;
    AmplioHome() {
        this.homeDirectory = new File("Does not exist");
    }

    protected File getHomeDirectory() {
        return homeDirectory;
    }

    protected File getTempDirectory() {
        File dir;
        if (oldStyleHomeDirectory) {
            dir = new File(getAppAcmDir(), Constants.TempDir);
        }
        else {
            dir = new File(getHomeDirectory(), Constants.TempDir);
        }
        if (!dir.exists()) dir.mkdirs();
        return dir;
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

    public synchronized static File getDirectory() {
        return getInstance().getHomeDirectory();
    }

    public static boolean isOldStyleHomeDirectory() {
        return getInstance().oldStyleHomeDirectory;
    }

    private static File findHomeDirectory(AmplioHome instance) {
        File homeDir = null;
        boolean isOldStyle = false;
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
                    if (name.equalsIgnoreCase(AmplioHomeDirName) || name.equalsIgnoreCase(LiteracybridgeHomeDirName)) {
                        homeDir = dir;
                        isOldStyle = name.equalsIgnoreCase(LiteracybridgeHomeDirName);
                        break;
                    }
                }
            }
        }
        // If we didn't find the running code's parent to be Amplio or LiteracyBridge, in the user's home
        // directory tree, look for those two directly.
        if (homeDir == null) {
            homeDir = new File(USER_HOME_DIR, AmplioHomeDirName);
            if (!homeDir.exists() || !homeDir.isDirectory()) {
                File legacyDir = new File(USER_HOME_DIR, LiteracybridgeHomeDirName);
                if (legacyDir.exists() && legacyDir.isDirectory()) {
                    homeDir = legacyDir;
                    isOldStyle = true;
                }
            }
        }
        // Either ~/LiteracyBridge or ~/Amplio
        instance.homeDirectory = homeDir;
        instance.oldStyleHomeDirectory = isOldStyle;
        System.out.printf("Found %sAmplio home directory in %s\n", isOldStyle?"old-style ":"", homeDir.getName());
        return homeDir;
    }



    // If any code ever queries the Dropbox directory, this will be populated.
    static File dropboxDir = null;
    static boolean dropboxOverride = false;

    /**
     * Non-dropbox ACM databases go here.
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
        return new File(getAppAcmDir(), "software");
    }

    /**
     * ACM temporary files.
     *
     * @return ~/Amplio/temp or ~/LiteracyBridge/ACM/temp
     */
    public static File getTempsDir() {
        return getInstance().getTempDirectory();
    }

    /**
     * Audio files in alternate formats. We don't want to upload them, but we also don't want to
     * have to re-create them again. So cache them here.
     *
     * @return ~/Amplio/ACM/cache
     */
    public static File getCachesDir() {
        if (getInstance().oldStyleHomeDirectory) {
            return new File(getAppAcmDir(), Constants.CACHE_DIR_NAME);
        } else {
            return new File(getDropboxDir(), Constants.CACHE_DIR_NAME);
        }
    }


    /**
     * The ~/LiteracyBridge/acm_config.properties file. Per computer information (dropbox path).
     *
     * @return it.
     */
    public static File getUserConfigFile() {
        return new File(getDirectory(), Constants.USERS_APPLICATION_PROPERTIES);
    }

    /**
     * TB-Loaders are built here, and downloaded and expanded here.
     *
     * @return ~/Amplio/TB-Loaders
     */
    public static File getLocalTbLoadersDir() {
        return new File(getDirectory(), Constants.TBLoadersHomeDir);
    }




    /**
     * Helper function to find the Dropbox directory if it is needed.
     *
     * Note: this function may read and may write the per-user config file.
     *
     * @return path to Dropbox, if it can be determined.
     */
    synchronized static File getDropboxDir() {
        if (dropboxDir != null) return dropboxDir;

        Properties userConfig = new Properties();
        File userConfigFile = getUserConfigFile();;
        boolean dirUpdated = false;
        // If there is an environment override for 'dropbox', use that for the
        // global directory.
        String override = System.getenv("dropbox");
        if (override != null && override.length() > 0) {
            dropboxDir = new File(override);
            dropboxOverride = true;
            LOG.info(String.format("Using override for Dropbox directory: %s", override));
        }
        else {
            if (userConfigFile.exists()) {
                try {FileInputStream fis = new FileInputStream(userConfigFile);
                    BufferedInputStream in = new BufferedInputStream(fis);
                    userConfig.load(in);
                } catch (IOException ignored) {
                }
            }
            String dropboxPath = userConfig.getProperty(Constants.GLOBAL_SHARE_PATH);
            if (dropboxPath != null) {
                dropboxDir = new File(dropboxPath);
            }
            // If we didn't find the global directory, try to get it from Dropbox
            // directly.
            if (dropboxPath == null || !dropboxDir.exists()) {
                dropboxPath = DropboxFinder.getDropboxPath();
                dropboxDir = new File(dropboxPath);
                dirUpdated = true;
                LOG.info(String.format("Using Dropbox configuration for shared global directory: %s",
                        dropboxPath));
            }
        }
        // We sill didn't find the global directory, so we must not use it on this machine.
        if (dropboxDir == null || !dropboxDir.exists() || !dropboxDir.isDirectory()) {
            dropboxDir = null;
            dirUpdated = false; // Don't try to write null.
        }
        if (dirUpdated) {
            userConfig.setProperty(Constants.GLOBAL_SHARE_PATH, dropboxDir.getAbsolutePath());
            try {
                FileOutputStream fos = new FileOutputStream(userConfigFile);
                BufferedOutputStream bos = new BufferedOutputStream(fos);
                userConfig.store(bos, null);
            } catch (IOException ignored) {
            }
        }
        return dropboxDir;
    }

    static boolean isDropboxOverride() {
        return dropboxOverride;
    }
}
