package org.literacybridge.acm.config;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.literacybridge.acm.Constants;
import org.literacybridge.acm.gui.CommandLineParams;
import org.literacybridge.acm.utils.DropboxFinder;
import org.literacybridge.acm.utils.Version;

import javax.swing.*;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ACMConfiguration {
    private static final Logger LOG = Logger.getLogger(ACMConfiguration.class.getName());

    private final Map<String, DBConfiguration> allDBs = Maps.newHashMap();
    // @TODO: There isn't an apparent reason for this to be an AtomicReference; we're not
    //   using any of the 'compareAnd...' operations. All we're getting (that I can see -- b.e.)
    //   is a wrapper around a 'volatile currentDB'.
    private final AtomicReference<DBConfiguration> currentDB = new AtomicReference<DBConfiguration>();
    private final File LB_HOME_DIR = new File(Constants.USER_HOME_DIR, Constants.LiteracybridgeHomeDirName);

    private String title;
    private boolean disableUI = false;
    private boolean forceSandbox = false;
    // If true, show all categories in the category browser. Default is a filtered view.
    private boolean allCategories = false;
    // If true, show a configuration dialog.
    private boolean showConfiguration = false;
    private boolean verbose = false; // TODO: some means to set it true.
    // If true, don't lock (or unlock) the database. For testing purposes.
    private boolean noDbCheckout;

    private final Properties UsersConfigurationProperties = new Properties();
    private File globalShareDir;

    private static final String ACM_PREFIX = Constants.ACM_DIR_NAME + "-";

    private final static String NON_FILE_CHARS = "[\\\\/~;:*?'\"]";

    /**
     * Given a name, either ACM-{project} or {project, return the cannonical, uppercased acm
     * directory name.
     * @param acmName The name to be cannonicalized.
     * @return the upper-cased ACM directory name.
     */
    public static String cannonicalAcmDirectoryName(String acmName) {
        acmName = acmName.toUpperCase().replaceAll(NON_FILE_CHARS, "");
        if (!acmName.startsWith(ACM_PREFIX)) {
            acmName = ACM_PREFIX + acmName;
        }
        return acmName;
    }

    /**
     * Given a name, either ACM-{project} or {project, return the cannonical, uppercased project
     * name.
     * @param projectName The name to be cannonicalized.
     * @return the upper-cased project name.
     */
    public static String cannonicalProjectName(String projectName) {
        projectName = projectName.toUpperCase().replaceAll(NON_FILE_CHARS, "");
        if (projectName.startsWith(ACM_PREFIX)) {
            projectName = projectName.substring(ACM_PREFIX.length());
        }
        return projectName;
    }

    /**
     * Get the shared instance of the configuration class. Initialize it if not
     * already initialized.
     *
     * @return The shared ACMConfiguration instance.
     */
    private static ACMConfiguration instance;

    public synchronized static ACMConfiguration getInstance() {
        if (instance == null) {
            CommandLineParams emptyParams = new CommandLineParams();
            instance = new ACMConfiguration(emptyParams);
        }
        return instance;
    }

    /**
     * Initialize the shared ACMConfiguration instance. Must not already be
     * initialized.
     *
     * @param params ACM Command Line Parameter.
     */
    public synchronized static void initialize(CommandLineParams params) {
        if (instance != null) {
            throw new IllegalStateException(
                    "Attempt to initialize ACMConfiguration after it is already initialized");
        }
        instance = new ACMConfiguration(params);
    }

    private ACMConfiguration(CommandLineParams params) {
        loadUserProps();

        if (params.titleACM != null) {
            title = params.titleACM;
        }

        disableUI = params.disableUI;
        forceSandbox = params.sandbox;
        allCategories = params.allCategories;
        showConfiguration = params.config;

        setupACMGlobalPaths();

        for (DBConfiguration config : discoverDBs()) {
            allDBs.put(config.getSharedACMname(), config);
            if (verbose) {
                System.out.println("Found DB " + config.getSharedACMname());
            }
        }

        if (StringUtils.isEmpty(UsersConfigurationProperties.getProperty(Constants.USER_NAME))) {
            String username = JOptionPane.showInputDialog(null, "Enter Username:",
                                                          "Missing Username",
                                                          JOptionPane.PLAIN_MESSAGE);
            UsersConfigurationProperties.put(Constants.USER_NAME, username);
            // propsChanged = true;
        }
        if (StringUtils.isEmpty(UsersConfigurationProperties.getProperty(Constants.USER_CONTACT_INFO))) {
            String contactinfo = JOptionPane.showInputDialog(null, "Enter Phone #:",
                                                             "Missing Contact Info",
                                                             JOptionPane.PLAIN_MESSAGE);
            UsersConfigurationProperties.put(Constants.USER_CONTACT_INFO, contactinfo);
            // propsChanged = true;
        }

        writeUserProps();
    }

    /**
     * Helper because most callers don't create a new DB.
     *
     * @param dbName
     * @throws Exception
     */
    public boolean setCurrentDB(String dbName) throws Exception {
        return setCurrentDB(dbName, false);
    }

    // TODO: when we have a homescreen this method needs to be split up into
    // different steps,
    // e.g. close DB, open new DB, etc.
    public synchronized boolean setCurrentDB(String dbName, boolean createEmptyDB) throws Exception {
        DBConfiguration dbConfig = allDBs.get(dbName);
        if (dbConfig == null) {
            if (!createEmptyDB) {
                throw new IllegalArgumentException("DB '" + dbName + "' not known.");
            } else {
                dbConfig = new DBConfiguration(dbName);
                allDBs.put(dbName, dbConfig);
            }
        }

        DBConfiguration oldDB = currentDB.get();
        if (oldDB != null) {
            LockACM.unlockDb();
        }

        boolean initialized = dbConfig.init();
        currentDB.set(dbConfig);
        return initialized;
    }

    public synchronized void closeCurrentDB() {
        DBConfiguration oldDB = currentDB.get();
        if (oldDB != null) {
            oldDB.closeDb();
            LockACM.unlockDb();
            currentDB.set(null);
        }
    }

    public synchronized DBConfiguration getCurrentDB() {
        return currentDB.get();
    }

    public synchronized DBConfiguration getDb(String acmName) {
        acmName = ACMConfiguration.cannonicalAcmDirectoryName(acmName);
        return allDBs.get(acmName);
    }

    /**
     * This is a hacky way to create a new Acm database, basing it on another Acm
     * database. Ideally, there should be a means of modifying the language list,
     * the access list, and the taxonomy without manually editing files in the
     * database directory. When/if we do get such functions, this should be re-
     * written in terms of those.
     * <p>
     * Note that the database is created with no TB-Loaders directory, no system
     * messages, no content.
     *
     * @param templateDbName The name of the existing database used as a template.
     * @param newDbName      The name of the new database, to be created. Must not exist.
     */
    public synchronized void createNewDb(String templateDbName, String newDbName) throws Exception {
        final String[] templateFilenames = new String[] { "config.properties",
                "accessList.txt", "Install-ACM.bat" };
        final String[] optionalFilenames = new String[] {"category.whitelist", "lb_taxonomy.yaml"};

        // Validate the arguments.
        if (allDBs.get(newDbName) != null) {
            throw new IllegalArgumentException(String.format("DB '%s' already exists.", newDbName));
        }
        DBConfiguration templateDbConfiguration = allDBs.get(templateDbName);
        if (templateDbConfiguration == null) {
            throw new IllegalArgumentException(String.format("DB '%s' not known.", templateDbName));
        }

        // Create the new configuration, and the new directory.
        DBConfiguration newDbConfiguration = new DBConfiguration(newDbName);
        File newDbDir = newDbConfiguration.getSharedACMDirectory();

        // Populate from the template.
        File templateDbDir = templateDbConfiguration.getSharedACMDirectory();
        try {
            newDbDir.mkdirs();
            for (String name : templateFilenames) {
                File templateFile = new File(templateDbDir, name);
                FileUtils.copyFileToDirectory(templateFile, newDbDir);
            }
            for (String name : optionalFilenames) {
                File optionalFile = new File(templateDbDir, name);
                if (optionalFile.exists()) {
                    FileUtils.copyFileToDirectory(optionalFile, newDbDir);
                }
            }
        } catch (IOException e) {
            // Could not create or copy something. Try to clean up.
            FileUtils.deleteQuietly(newDbDir);
            throw e;
        }

        // Set new DB to use AWS locking from the beginning.
        Properties newDbProps = new Properties();
        File propsFile = newDbConfiguration.getConfigurationPropertiesFile();
        try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(propsFile))) {
            newDbProps.load(in);
            newDbProps.setProperty(Constants.USE_AWS_LOCKING, Boolean.TRUE.toString());
            try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(propsFile))) {
                newDbProps.store(out, null);
            }
        }

        // Remember the new database, and return it.
        allDBs.put(newDbName, newDbConfiguration);
        setCurrentDB(newDbName);
    }

    public String getUserName() {
        return UsersConfigurationProperties.getProperty("USER_NAME");
    }

    public String getUserContact() {
        return UsersConfigurationProperties.getProperty(Constants.USER_CONTACT_INFO);
    }

    private String getRecordingCounter() {
        return UsersConfigurationProperties.getProperty(Constants.RECORDING_COUNTER_PROP);
    }

    private void setRecordingCounter(String counter) {
        UsersConfigurationProperties.setProperty(Constants.RECORDING_COUNTER_PROP, counter);
        writeUserProps();
    }

    public String getNewAudioItemUID() throws IOException {
        String value = getRecordingCounter();
        int counter = (value == null) ? 0 : Integer.parseInt(value, Character.MAX_RADIX);
        counter++;
        value = Integer.toString(counter, Character.MAX_RADIX);
        String uuid = "LB-2" + "_" + getDeviceID() + "_" + value;

        // make sure we remember that this uuid was already used
        setRecordingCounter(value);
        // writeUserProps();

        return uuid;
    }

    private String getDeviceID() throws IOException {
        String value = UsersConfigurationProperties.getProperty(Constants.DEVICE_ID_PROP);
        if (value == null) {
            final int n = 10;
            Random rnd = new Random();
            // generate 10-digit unique ID for this acm instance
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < n; i++) {
                builder.append(
                        Character.forDigit(rnd.nextInt(Character.MAX_RADIX), Character.MAX_RADIX));
            }
            value = builder.toString();
            UsersConfigurationProperties.setProperty(Constants.DEVICE_ID_PROP, value);
            writeUserProps();
        }

        return value;
    }

    /**
     * The Global Shared Directory is where all the ACMs an supporting files are
     * kept. In general, this will be in Dropbox. For testing, it will be
     * different, and it could be different for some specialized circumstances.
     * <p>
     * Side effect: globalShareDir is set to a directory, or the application
     * exits.
     */
    private void setupACMGlobalPaths() {
        File appHomeDir = getApplicationHomeDirectory();
        if (!appHomeDir.exists()) {
            appHomeDir.mkdirs();
        }

        boolean dirUpdated = false;
        // If there is an environment override for 'dropbox', use that for the
        // global directory.
        String override = System.getenv("dropbox");
        if (override != null && override.length() > 0) {
            globalShareDir = new File(override);
            noDbCheckout = true;
            LOG.info(String.format("Using override for shared global directory: %s", override));
            LOG.info("No database checkout will be performed (because of dropbox override).");
        } else {
            String globalSharePath = UsersConfigurationProperties.getProperty(Constants.GLOBAL_SHARE_PATH);
            if (globalSharePath != null) {
                globalShareDir = new File(globalSharePath);
            }
            // If we didn't find the global directory, try to get it from Dropbox
            // directly.
            if (globalSharePath == null || globalShareDir == null || !globalShareDir.exists()) {
                globalSharePath = DropboxFinder.getDropboxPath();
                globalShareDir = new File(globalSharePath);
                dirUpdated = true;
                LOG.info(String.format("Using Dropbox configuration for shared global directory: %s",
                                      globalSharePath));
            }
            // If we still didn't find the directory, prompt the user.
            if (!globalShareDir.exists() || !globalShareDir.isDirectory()) {
                JFileChooser fc = new JFileChooser();
                fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                fc.setDialogTitle("Select Dropbox directory.");
                int returnVal = fc.showOpenDialog(null);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    globalShareDir = fc.getSelectedFile();
                    dirUpdated = true;
                }
            }
        }
        // If we STILL didn't find the global directory, abort.
        if (globalShareDir == null || !globalShareDir.exists() || !globalShareDir.isDirectory()) {
            LOG.warning("Unable to find shared global directory. Shutting down");
            JOptionPane.showMessageDialog(null,"Dropbox directory has not been identified. Shutting down.");
            System.exit(1);
        }
        if (dirUpdated) {
            UsersConfigurationProperties.put(Constants.GLOBAL_SHARE_PATH, globalShareDir.getAbsolutePath());
        }
    }

    private List<DBConfiguration> discoverDBs() {
        // Regex to match & extract the number from strings like db123.zip
        String dbRegex = "^db(\\d+).zip$";
        List<DBConfiguration> dbs = Lists.newLinkedList();
        if (getGlobalShareDir().exists()) {
            File[] dirs = getGlobalShareDir().listFiles(new FileFilter() {
                @Override
                public boolean accept(File path) {
                    return path.isDirectory();
                }
            });

            for (File d : dirs) {
                if (d.exists() && d.isDirectory()) {
                    File accessList = new File(d, Constants.DB_ACCESS_FILENAME);
                    String dbFiles[] = d.list((dir, name) -> name.matches(dbRegex));
                    if (accessList.exists() && dbFiles.length>0) {
                        dbs.add(new DBConfiguration(d.getName()));
                    }
                }
            }
        }
        return dbs;
    }

    public String getTitle() {
        if (title != null)
            return title;
        return ACMConfiguration.getInstance().getCurrentDB().getSharedACMname();
    }

    public boolean isDisableUI() {
        return disableUI;
    }

    public boolean isForceSandbox() {
        return forceSandbox;
    }

    public boolean isAllCategories() {
        return allCategories;
    }

    public boolean isNoDbCheckout() { return noDbCheckout; }

    public boolean isShowConfiguration() {
        return showConfiguration;
    }

    public File getGlobalShareDir() {
        return globalShareDir;
    }

    /**
     * The Application's home directory, NOT the shared (dropbox) directory.
     * @return ~/LiteracyBridge
     */
    public File getApplicationHomeDirectory() {
        // ~/LiteracyBridge
        return LB_HOME_DIR;
    }

    /**
     * The ~/LiteracyBridge/ACM/software directory.
     * @return it.
     */
    public File getSoftwareDir() {
        return new File(getApplicationHomeDirectory(), "/ACM/software");
    }

    /**
     * The ~/LiteracyBridge/acm_config.properties file. User's name, contact info.
     * @return it.
     */
    private File getUsersConfigurationPropertiesFile() {
        return new File(getApplicationHomeDirectory(), Constants.USERS_APPLICATION_PROPERTIES);
    }

    public File getAcmDirFor(String acmName) {
        File acmDir = null;
        acmName = ACMConfiguration.cannonicalAcmDirectoryName(acmName);

        if (globalShareDir != null) {
            acmDir = new File(globalShareDir, acmName);
            if (!acmDir.exists() || !acmDir.isDirectory()) {
                acmDir = null;
            }
        }
        return acmDir;
    }

    /**
     * ~/LiteracyBridge/TB-Loaders/{project}
     * This directory may contain a *.rev file, marking the locally installed deployment,
     * a content directory with deployments, a metadata directory with metadata, and/or a
     * batch file to run the TB-Loader for the project.
     * @param project The project for which to get the local project directory.
     * @return The directory.
     */
    public File getLocalTbLoaderDirFor(String project) {
        project = cannonicalProjectName(project);
        return new File(getApplicationHomeDirectory(),
            Constants.TBLoadersHomeDir + File.separator + project);
    }

    public File getSharedAcmDirectoryFor(String acmName) {
        acmName = cannonicalAcmDirectoryName(acmName);
        return new File(globalShareDir, acmName);
    }

    /**
     * Gets a File containing the configuration properties for the given ACM database.
     * @param acmName The ACM for which the configuration file is desired.
     * @return The File.
     */
    public File getSharedConfigurationFileFor(String acmName) {
        // ~/Dropbox/ACM-DEMO/config.properties
        return new File(getSharedAcmDirectoryFor(acmName), Constants.CONFIG_PROPERTIES);
    }

    public File getTbLoaderDirFor(String acmName) {
        File tbLoaderDir = null;
        File acmDir = getAcmDirFor(acmName);

        if (acmDir != null) {
            tbLoaderDir = new File(acmDir, Constants.TBLoadersHomeDir);
            if (!tbLoaderDir.exists() || !tbLoaderDir.isDirectory()) {
                tbLoaderDir = null;
            }
        }
        return tbLoaderDir;
    }

    public File getProgramSpecDirFor(String acmName) {
        File progspecDir = null;
        File acmDir = getAcmDirFor(acmName);

        if (acmDir != null) {
            progspecDir = new File(acmDir, Constants.ProgramSpecDir);
            if (!progspecDir.exists() || !progspecDir.isDirectory()) {
                progspecDir = null;
            }
        }
        return progspecDir;
    }

    public File getUserFeedbackWhitelistFileFor(String acmName) {
        File whitelistFile = null;
        File acmDir = getAcmDirFor(acmName);

        if (acmDir != null) {
            whitelistFile = new File(acmDir, Constants.USER_FEEDBACK_WHITELIST_FILENAME);
            if (!whitelistFile.exists() || !whitelistFile.isFile()) {
                whitelistFile = null;
            }
        }
        return whitelistFile;
    }

    public File getConfigFileFor(String acmName) {
        File configFile = null;
        File acmDir = getAcmDirFor(acmName);

        if (acmDir != null) {
            configFile = new File(acmDir, Constants.CONFIG_PROPERTIES);
            if (!configFile.exists() || !configFile.isFile()) {
                configFile = null;
            }
        }
        return configFile;
    }

    public Properties getConfigPropertiesFor(String acmName) {
        Properties properties = null;
        File configFile = getConfigFileFor(acmName);
        if (configFile != null) {
            try {
                properties = new Properties();
                BufferedInputStream in = new BufferedInputStream(new FileInputStream(
                    configFile));
                properties.load(in);
            } catch (IOException ignored) {
                System.err.printf("Unable to load configuration file: %s\n", configFile.getName());
            }
        }
        return properties;
    }

    private void loadUserProps() {
        if (getUsersConfigurationPropertiesFile().exists()) {
            try {
                BufferedInputStream in = new BufferedInputStream(
                        new FileInputStream(getUsersConfigurationPropertiesFile()));
                UsersConfigurationProperties.load(in);
            } catch (IOException e) {
                throw new RuntimeException(
                        "Unable to load configuration file: " + getUsersConfigurationPropertiesFile(), e);
            }
        }
    }

    private void writeUserProps() {
        try {
            BufferedOutputStream out = new BufferedOutputStream(
                    new FileOutputStream(getUsersConfigurationPropertiesFile()));
            UsersConfigurationProperties.store(out, null);
            out.flush();
            out.close();
        } catch (IOException e) {
            throw new RuntimeException(
                    "Unable to write configuration file: " + getUsersConfigurationPropertiesFile(), e);
        }
    }

}
