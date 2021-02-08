package org.literacybridge.acm.config;

import org.amplio.CloudSync;
import org.apache.commons.lang3.NotImplementedException;
import org.literacybridge.acm.Constants;
import org.literacybridge.acm.cloud.Authenticator;
import org.literacybridge.acm.gui.CommandLineParams;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.function.BiConsumer;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.isEmpty;

public class ACMConfiguration {
    private static final Logger LOG = Logger.getLogger(ACMConfiguration.class.getName());

    private final Map<String, DBConfiguration> knownDbs = new HashMap<>();
    private DBConfiguration currentDB = null;

    private String title;
    private final boolean disableUI;
    private boolean forceSandbox;
    private final boolean testData; // to simulate test data for more convenient test iteration
    private final boolean doUpdate; // update without prompting
    // If true, show all categories in the category browser. Default is a filtered view.
    private final boolean allCategories;
    // If true, show a configuration dialog.
    private final boolean showConfiguration;
    // If true, don't lock (or unlock) the database. For testing purposes.
    private boolean noDbCheckout;
    private boolean devo;

    private final Properties UsersConfigurationProperties = new Properties();

    private static final String ACM_PREFIX = Constants.ACM_DIR_NAME + "-";

    private final static String NON_FILE_CHARS = "[\\\\/~;:*?'\"]";

    /**
     * Given a name, either ACM-{project} or {project, return the cannonical, uppercased acm
     * directory name.
     * @param acmName The name to be cannonicalized.
     * @return the upper-cased ACM directory name.
     */
    public static String cannonicalAcmDirectoryName(String acmName) {
        if (isEmpty(acmName)) return null;
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
        if (isEmpty(projectName)) return null;
        projectName = projectName.toUpperCase().replaceAll(NON_FILE_CHARS, "");
        if (projectName.startsWith(ACM_PREFIX)) {
            projectName = projectName.substring(ACM_PREFIX.length());
        }
        return projectName;
    }

    /**
     * Get the shared instance of the configuration class. Initialize it if not
     * already initialized.
     */
    private static ACMConfiguration instance;

    public synchronized static ACMConfiguration getInstance() {
        if (instance == null) {
            CommandLineParams emptyParams = new CommandLineParams();
            instance = new ACMConfiguration(emptyParams);
        }
        return instance;
    }

    public static synchronized boolean isInitialized() {
        return instance != null;
    }

    private static final Set<String> testAcms = new HashSet<>(Arrays.asList("ACM-TEST", "TEST"));
    public static boolean isTestAcm() {
        if (instance == null || instance.getCurrentDB() == null) return false;
        if (instance.isNoDbCheckout()) return true;
        return testAcms.contains(instance.getCurrentDB().getAcmDbDirName());
    }
    public static boolean isSandbox() {
        if (instance == null || instance.getCurrentDB() == null) return false;
        return (instance.getCurrentDB().isSandboxed());
    }
    public static boolean isTestData() {
        return isTestAcm() && instance != null && instance.testData;
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
        AmplioHome.getDropboxDir();
        
        loadUserProps();

        boolean propsChanged = false;
        for (String prop : Constants.OBSOLETE_PROPERTY_NAMES) {
            if (UsersConfigurationProperties.getProperty(prop) != null) {
                UsersConfigurationProperties.remove(prop);
                propsChanged = true;
            }
        }

        if (params.titleACM != null) {
            title = params.titleACM;
        }

        disableUI = params.disableUI;
        forceSandbox = params.sandbox;
        doUpdate = params.update;
        allCategories = params.allCategories;
        showConfiguration = params.config;
        testData = params.testData;
        devo = params.devo;

        setupACMGlobalPaths();

        discoverDBs();

        if (propsChanged) {
            writeUserProps();
        }
    }

    /**
     * @return a map {programid:description} }of the known ACMs (those found on this machine).
     */
    public List<String> getLocalDbs() {
        return knownDbs.keySet()
                .stream()
                .map(ACMConfiguration::cannonicalProjectName)
                .filter(Objects::nonNull)
                .sorted(String::compareToIgnoreCase)
                .collect(Collectors.toList());
    }

    /**
     * @return a map {programid:description} }of the known ACMs (those found on this machine).
     */
    public Map<String, String> getLocalProgramDbs() {
        Map<String, String> programDbs = knownDbs.entrySet()
            .stream()
            .collect(Collectors.toMap(e->ACMConfiguration.cannonicalProjectName(e.getKey()), e->e.getValue().getDescription()));
        return programDbs;
    }

    public List<String> getLocalDbxDbs() {
        return knownDbs.entrySet()
                .stream()
                .filter(e->e.getValue().getPathProvider().isDropboxDb())
                .map(Map.Entry<String,DBConfiguration>::getKey)
                .map(ACMConfiguration::cannonicalProjectName)
                .filter(Objects::nonNull)
                .sorted(String::compareToIgnoreCase)
                .collect(Collectors.toList());
    }

    /**
     * Try to sync with S3.
     * @param program to be synchronized.
     * @return true if the sync is successful, false if an exception is thrown.
     */
    private boolean tryCloudSync(String program) {
        try {
            CloudSync.RemoteResponse response;
            response = CloudSync.sync(program + "_PROGSPEC");
            if (response.responseHasError) return false;
            response = CloudSync.sync(program + "_DB");
            return !response.responseHasError;
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * Starts the background sync process if it is not already running.
     * Syncs the given program.
     * The first sync (or several) will fail if we had to start the background process. When a failure
     * occurs wait a bit and try again. Double the wait each time. Retry up to 7 times.
     * Wait 250, 500, 1000, 2000, 4000, 8000, 16000 ms.
     * @param program to be sync'd.
     * @return true if the sync was successful.
     */
    private boolean cloudStartAndSync(String program) {
        CloudSync.start(true);
        long delay = 250;
        for (int i=0; i<=7; i++) {
            if (tryCloudSync(program)) {
                return true;
            } else {
                try {
                    System.out.printf("Exception syncing, waiting %dms to try again...", delay);
                    Thread.sleep(delay);
                    System.out.print("retrying.\n");
                } catch (InterruptedException ignored2) {
                }
                delay *= 2;
            }
        }
        return false;
    }

    public enum S3SyncState {
        REQUIRED_FOR_S3,
        FAILED,
        NOT_REQUIRED
    }

    /**
     * "Open" the given db. Globally locks the database if not opening "sandboxed". (Here, "global" mean
     * "on planet Earth".)
     * @param dbName name of the program database to be opened.
     * @param waiter A callback to be invoked if the db is an S3 database. The waiter accepts a Runnable
     *               that the waiter should call after making appropriate preparations.
     * @return true
     * @throws Exception if the database can't be opened.
     */
    private synchronized boolean setCurrentDB(String dbName, S3SyncState syncState, BiConsumer<Runnable,Runnable> waiter) throws Exception {
        PathsProvider dbPathProvider = getPathProvider(dbName);
        if (dbPathProvider == null) {
            throw new IllegalArgumentException("DB '" + dbName + "' not known.");
        }
        DBConfiguration dbConfig = new DBConfiguration(dbPathProvider);
        dbConfig.setSyncFailure(syncState==S3SyncState.FAILED);

        if (currentDB != null) {
            AcmLocker.unlockDb();
        }

        //noinspection MismatchedReadAndWriteOfArray
        boolean[] inSync = {true};
        if (!dbPathProvider.isDropboxDb() && syncState==S3SyncState.REQUIRED_FOR_S3) {
            // No permits, so acquire will wait until the release.
            Semaphore available = new Semaphore(0);
            waiter.accept(()-> inSync[0] = cloudStartAndSync(dbPathProvider.getProgramName()), available::release);
            available.acquire();
        }
        // TODO: Do something with value of inSync
        boolean initialized = dbConfig.init();
        currentDB = dbConfig;

        return initialized;
    }

    /**
     * Version of setCurrentDB that handles all the callbacks. May not be suitable for GUI applications
     * because the sync happens on the calling thread.
     * @param dbName name of the program database to be opened.
     * @return true
     * @throws Exception if the database can't be opened.
     */
    public boolean setCurrentDB(String dbName) throws Exception {
        return setCurrentDB(dbName, S3SyncState.REQUIRED_FOR_S3);
    }
    public boolean setCurrentDB(String dbName, S3SyncState syncState) throws Exception {
        // Pass a Consumer that simply calls its passed Runnable.
        return setCurrentDB(dbName, syncState, (a,b)->{a.run();b.run();});
    }

    /**
     * Commits the current database. If running interactively, prompts the user first.
     *
     * TODO: Prompt the user before calling this.
     */
    public synchronized void commitCurrentDB() {
        if (currentDB != null && !currentDB.isSandboxed()) {
            if (isDisableUI()) {
                // Headless version, immediately commits changes.
                currentDB.commitDbChanges();
            } else {
                // Interactive version, prompts first.
                currentDB.updateDb();
            }
            if (!currentDB.getPathProvider().isDropboxDb()) {
                tryCloudSync(currentDB.getProgramName());
            }
        }
    }

    public synchronized void closeCurrentDB() {
        if (currentDB != null) {
            currentDB.closeDb();
            AcmLocker.unlockDb();
            currentDB = null;
        }
    }

    public synchronized DBConfiguration getCurrentDB() {
        return currentDB;
    }

    @SuppressWarnings("DeprecatedIsStillUsed")
    @Deprecated
    public synchronized void createNewDb(String templateDbName, String newDbName) {
        throw new NotImplementedException("createNewDb has been deimplemented.");
    }

    public String getUserName() {
        return Authenticator.getInstance().getUserSelfName();
    }

    public String getUserContact() {
        return Authenticator.getInstance().getUserContact();
    }

    public String getNewAudioItemUID() {
        String value = UsersConfigurationProperties.getProperty(Constants.RECORDING_COUNTER_PROP);
        int counter = (value == null) ? 0 : Integer.parseInt(value, Character.MAX_RADIX);
        counter++;
        value = Integer.toString(counter, Character.MAX_RADIX);
        String uuid = "LB-2" + "_" + getDeviceID() + "_" + value;

        // make sure we remember that this uuid was already used
        UsersConfigurationProperties.setProperty(Constants.RECORDING_COUNTER_PROP, value);
        writeUserProps();

        return uuid;
    }

    /**
     * Only intended for software testing, to allow more frequent allocations in test.
     * @return the size of the blocks of TB serial numbers that should be allocated at one time.
     */
    public int getTbSrnAllocationSize() {
        String value = getUserConfigurationItem(Constants.TB_SRN_ALLOCATION_SIZE, Constants.TB_SRN_ALLOCATION_SIZE_DEFAULT.toString());
        try {
            return Integer.parseInt(value);
        } catch (Exception ignored) {
            // ignore
        }
        return Constants.TB_SRN_ALLOCATION_SIZE_DEFAULT;
    }

    public String getUserConfigurationItem(String name, String defaultValue) {
        String value = UsersConfigurationProperties.getProperty(name);
        return (value == null) ? defaultValue : value;
    }

    private String getDeviceID() {
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
            //noinspection ResultOfMethodCallIgnored
            appHomeDir.mkdirs();
        }

        if (AmplioHome.isDropboxOverride()) {
            noDbCheckout = true;
            LOG.info("No database checkout will be performed (because of dropbox override).");
        }
    }

    /**
     * Gets a PathProvider for the given program.
     * @param programId Also known as the "project", or the "acm name" (minus the ACM- part).
     * @return a PathProvider for the given project.
     */
    public PathsProvider getPathProvider(String programId) {
        PathsProvider result = knownDbs.get(cannonicalProjectName(programId)).getPathProvider();
        if (result == null) {
            result = knownDbs.get(cannonicalAcmDirectoryName(programId)).getPathProvider();
        }
        return result;
    }

    /**
     * Find the ACM databases on the local computer. Look first in Dropbox, then in the Amplio / Literacybridge
     * directory. If a database is found in both, the one in Amplio/Literacybridge takes precedence.
     *
     * Populates the knownDbs map {programid : PathsProvider} structure.
     */
    private void discoverDBs() {
        knownDbs.putAll(findContainedAcmDbs(AmplioHome.getDropboxDir(), true));
        knownDbs.putAll(findContainedAcmDbs(AmplioHome.getHomeDbsRootDir(), false));
    }

    /**
     * "Discover" a newly added program. Used after downloading program content from the cloud.
     * @param program to be "discovered" and added to the list of known programs.
     */
    public void discoverDB(String program) {
        program = cannonicalProjectName(program);
        assert program != null;
        File programDir = new File(AmplioHome.getHomeDbsRootDir(), program);
        if (programDir.isDirectory() && knownDbs.containsKey(program) && knownDbs.get(program).getPathProvider().isDropboxDb()) {
            knownDbs.put(program, new DBConfiguration(new PathsProvider(program, false)));
        }
    }

    /**
     * Find the ACM databases in the given directory. An ACM database is recognized by virtue of containing
     * at least one "db123.zip" file. False positives are possible.
     * @param containingDir to be searched.
     * @param isDropbox true if this is the dropbox directory.
     * @return A map of {programid : PathsProvider}
     */
    private Map<String, DBConfiguration> findContainedAcmDbs(File containingDir, boolean isDropbox) {
        Map<String, DBConfiguration> result = new HashMap<>();
        // Regex to match & extract the number from strings like db123.zip
        String dbRegex = "^db(\\d+).zip$";

        if (containingDir != null && containingDir.exists() && containingDir.isDirectory()) {
            File[] dirs = containingDir.listFiles(File::isDirectory);
            if (dirs != null) {
                for (File d : dirs) {
                    if (d.exists() && d.isDirectory()) {
                        File dbConfigFile = new File(d, Constants.CONFIG_PROPERTIES);
                        File contentDir = new File(d, Constants.RepositoryHomeDir);
                        String[] dbFiles = d.list((dir, name) -> name.matches(dbRegex));
                        if (dbConfigFile.exists() && dbConfigFile.isFile() &&
                                contentDir.exists() && contentDir.isDirectory() &&
                                dbFiles != null && dbFiles.length>0) {
                            PathsProvider pathsProvider = new PathsProvider(d.getName(), isDropbox);
                            result.put(cannonicalProjectName(d.getName()), new DBConfiguration(pathsProvider));
                        }
                    }
                }
            }
        }
        return result;
    }

    public String getTitle() {
        if (title != null)
            return title;
        return ACMConfiguration.getInstance().getCurrentDB().getAcmDbDirName();
    }

    public boolean isDisableUI() {
        return disableUI;
    }

    boolean isDoUpdate() {
        return doUpdate && !forceSandbox;
    }

    public void setForceSandbox(boolean forceSandbox) {
        this.forceSandbox = forceSandbox;
    }
    public boolean isForceSandbox() {
        return forceSandbox;
    }

    boolean isAllCategories() {
        return allCategories;
    }

    boolean isNoDbCheckout() { return noDbCheckout; }

    public boolean isShowConfiguration() {
        return showConfiguration;
    }

    public boolean isDevo() {
        return devo;
    }

    /**
     * The Application's home directory, NOT the shared (dropbox) directory.
     * @return ~/LiteracyBridge
     */
    public File getApplicationHomeDirectory() {
        // ~/LiteracyBridge
        return AmplioHome.getDirectory();
    }

    /**
     * The ~/LiteracyBridge/ACM/software directory.
     * @return it.
     */
    public File getSoftwareDir() {
//        return new File(getApplicationHomeDirectory(), "/ACM/software");
        return AmplioHome.getAppSoftwareDir();
    }

    /**
     * ~/LiteracyBridge/TB-Loaders
     * This directory may contain one or more local TB-Loader directories for programs.
     * @return The directory.
     */
    public File getLocalTbLoadersDir() {
//        return new File(getApplicationHomeDirectory(), Constants.TBLoadersHomeDir);
        return AmplioHome.getLocalTbLoadersDir();
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
        PathsProvider pp = getPathProvider(project);
        return pp.getLocalTbLoaderDir();
    }
    
    public Properties getConfigPropertiesFor(String acmName) {
        Properties properties = null;
        File configFile = getPathProvider(acmName).getProgramConfigFile();
        if (configFile != null) {
            try (FileInputStream fis = new FileInputStream(configFile);
                 BufferedInputStream in = new BufferedInputStream(fis)) {
                properties = new Properties();
                properties.load(in);
            } catch (IOException ignored) {
                System.err.printf("Unable to load configuration file: %s\n", configFile.getName());
            }
        }
        return properties;
    }

    private void loadUserProps() {
        if (AmplioHome.getUserConfigFile().exists()) {
            try (FileInputStream fis = new FileInputStream(AmplioHome.getUserConfigFile());
                 BufferedInputStream in = new BufferedInputStream(fis)) {
                UsersConfigurationProperties.load(in);
            } catch (IOException e) {
                throw new RuntimeException(
                        "Unable to load configuration file: " + AmplioHome.getUserConfigFile(), e);
            }
        }
    }

    private void writeUserProps() {
        try (FileOutputStream fos = new FileOutputStream(AmplioHome.getUserConfigFile());
             BufferedOutputStream out = new BufferedOutputStream(fos)) {
            UsersConfigurationProperties.store(out, null);
        } catch (IOException e) {
            throw new RuntimeException(
                    "Unable to write configuration file: " + AmplioHome.getUserConfigFile(), e);
        }
    }

}
