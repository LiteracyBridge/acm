package org.literacybridge.acm.config;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.literacybridge.acm.Constants;
import org.literacybridge.acm.categories.Taxonomy;
import org.literacybridge.acm.gui.CommandLineParams;
import org.literacybridge.acm.utils.DropboxFinder;

import javax.swing.*;
import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

public class ACMConfiguration {
    private static final Logger LOG = Logger.getLogger(ACMConfiguration.class.getName());

    private  final Map<String, DBConfiguration> allDBs = Maps.newHashMap();
    private  final AtomicReference<DBConfiguration> currentDB = new AtomicReference<DBConfiguration>();
    final File LB_HOME_DIR = new File(Constants.USER_HOME_DIR, Constants.LiteracybridgeHomeDirName);

    private  String title;
    private  boolean disableUI = false;
    private  boolean forceSandbox = false;
    private  final Properties ACMGlobalConfigProperties = new Properties();
    private  File globalShareDir;

    /**
     * Get the shared instance of the configuration class. Initialize it if not already initialized.
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
     * Initialize the shared ACMConfiguration instance. Must not already be initialized.
     *
     * @param params ACM Command Line Parameter.
     */
    public synchronized static void initialize(CommandLineParams params) {
        if (instance != null) {
            throw new IllegalStateException("Attempt to initialize ACMConfiguration after it is already initialized");
        }
        instance = new ACMConfiguration(params);
    }

    private ACMConfiguration(CommandLineParams params) {
        loadProps();

        if (params.titleACM != null) {
            title = params.titleACM;
        }

        disableUI = params.disableUI;
        forceSandbox = params.sandbox;

        setupACMGlobalPaths();

        for (DBConfiguration config : discoverDBs()) {
            allDBs.put(config.getSharedACMname(), config);
            System.out.println("Found DB " + config.getSharedACMname());
        }

        if (!ACMGlobalConfigProperties.containsKey(Constants.USER_NAME)) {
            String username = (String)JOptionPane.showInputDialog(null, "Enter Username:", "Missing Username", JOptionPane.PLAIN_MESSAGE);
            ACMGlobalConfigProperties.put(Constants.USER_NAME, username);
            //propsChanged = true;
        }
        if (!ACMGlobalConfigProperties.containsKey(Constants.USER_CONTACT_INFO)) {
            String contactinfo = (String)JOptionPane.showInputDialog(null, "Enter Phone #:", "Missing Contact Info", JOptionPane.PLAIN_MESSAGE);
            ACMGlobalConfigProperties.put(Constants.USER_CONTACT_INFO, contactinfo);
            //propsChanged = true;
        }

        writeProps();
    }

    // TODO: when we have a homescreen this method needs to be split up into different steps,
    // e.g. close DB, open new DB, etc.
    public synchronized  void setCurrentDB(String dbName, boolean createEmptyDB) throws Exception {
        DBConfiguration config = allDBs.get(dbName);
        if (config == null) {
            if (!createEmptyDB) {
                throw new IllegalArgumentException("DB '" + dbName + "' not known.");
            } else {
                config = new DBConfiguration(dbName);
                allDBs.put(dbName, config);
            }
        }

        DBConfiguration oldDB = currentDB.get();
        if (oldDB != null) {
            oldDB.getDatabaseConnection().close();
            Taxonomy.resetTaxonomy(); // necessary
            LockACM.unlockFile();
        }

        config.init();

        System.out.println("Connecting to DB " + config.getSharedACMname());
        config.connectDB();

        currentDB.set(config);
    }

    public synchronized  void closeCurrentDB() {
        DBConfiguration oldDB = currentDB.get();
        if (oldDB != null) {
            oldDB.getDatabaseConnection().close();
        }
    }

    public synchronized  DBConfiguration getCurrentDB() {
        return currentDB.get();
    }

    public File dirACM(String acmName) {
        File f = null;

        if (globalShareDir != null) {
            f = new File (globalShareDir, acmName + File.separator + Constants.TBLoadersHomeDir);
            if (!f.exists()) {
                f = null;
            }
        }
        return f;
    }

    public String getUserName() {
        return ACMGlobalConfigProperties.getProperty("USER_NAME");
    }

    public String getUserContact() {
        return ACMGlobalConfigProperties.getProperty(Constants.USER_CONTACT_INFO);
    }

    public String getRecordingCounter() {
        return ACMGlobalConfigProperties.getProperty(Constants.RECORDING_COUNTER_PROP);
    }

    public void setRecordingCounter(String counter) {
        ACMGlobalConfigProperties.setProperty(Constants.RECORDING_COUNTER_PROP, counter);
        writeProps();
    }

    public String getNewAudioItemUID() throws IOException {
        String value = getRecordingCounter();
        int counter = (value == null) ? 0 : Integer.parseInt(value, Character.MAX_RADIX);
        counter++;
        value = Integer.toString(counter, Character.MAX_RADIX);
        String uuid = "LB-2" + "_"  + getDeviceID() + "_" + value;

        // make sure we remember that this uuid was already used
        setRecordingCounter(value);
        //writeProps();

        return uuid;
    }

    public String getDeviceID() throws IOException {
        String value = ACMGlobalConfigProperties.getProperty(Constants.DEVICE_ID_PROP);
        if (value == null) {
            final int n = 10;
            Random rnd = new Random();
            // generate 10-digit unique ID for this acm instance
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < n; i++) {
                builder.append(Character.forDigit(rnd.nextInt(Character.MAX_RADIX), Character.MAX_RADIX));
            }
            value = builder.toString();
            ACMGlobalConfigProperties.setProperty(Constants.DEVICE_ID_PROP, value);
            writeProps();
        }

        return value;
    }


    /**
     * The Global Shared Directory is where all the ACMs an supporting files are kept.
     * In general, this will be in Dropbox. For testing, it will be different, and it
     * could be different for some specialized circumstances.
     *
     * Side effect: globalShareDir is set to a directory, or the application exits.
     */
    private  void setupACMGlobalPaths() {
        boolean dirUpdated = false;
        // If there is an environment override for 'dropbox', use that for the global directory.
        String override = System.getenv("dropbox");
        if (override != null && override.length() > 0) {
            globalShareDir = new File(override);
            LOG.info(String.format("Using override for shared global directory: %s", override));
        } else {
            String globalSharePath = ACMGlobalConfigProperties.getProperty(Constants.GLOBAL_SHARE_PATH);
            if (globalSharePath != null) {
                globalShareDir = new File(globalSharePath);
            }
            // If we didn't find the global directory, try to get it from Dropbox directly.
            if (globalSharePath == null || globalShareDir == null || !globalShareDir.exists()) {
                globalSharePath = DropboxFinder.getDropboxPath();
                globalShareDir = new File(globalSharePath);
                dirUpdated = true;
                LOG.info(String.format("Using Dropbox configuration for shared global directory: %s", globalSharePath));
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
            JOptionPane.showMessageDialog(null, "Dropbox directory has not been identified. Shutting down.");
            System.exit(0);
        }
        if (dirUpdated) {
            ACMGlobalConfigProperties.put(Constants.GLOBAL_SHARE_PATH, globalShareDir.getAbsolutePath());
        }
    }

    private  List<DBConfiguration> discoverDBs() {
        List<DBConfiguration> dbs = Lists.newLinkedList();
        if (getGlobalShareDir().exists()) {
            File[] dirs = getGlobalShareDir().listFiles(new FileFilter() {
                @Override public boolean accept(File path) {
                    return path.isDirectory();
                }
            });

            for (File d : dirs) {
                if (d.exists() && new File(d, Constants.DB_ACCESS_FILENAME).exists()) {
                    dbs.add(new DBConfiguration(d.getName()));
                }
            }
        }
        return dbs;
    }

    public String getACMname() {
        return title;
    }

    public boolean isDisableUI() {
        return disableUI;
    }

    public boolean isForceSandbox() {
        return forceSandbox;
    }

    public File getGlobalShareDir() {
        return globalShareDir;
    }

    private  File getConfigurationPropertiesFile() {
        return new File(LB_HOME_DIR, Constants.GLOBAL_CONFIG_PROPERTIES);
    }

    private void loadProps() {
        if (getConfigurationPropertiesFile().exists()) {
            try {
                BufferedInputStream in = new BufferedInputStream(new FileInputStream(getConfigurationPropertiesFile()));
                ACMGlobalConfigProperties.load(in);
            } catch (IOException e) {
                throw new RuntimeException("Unable to load configuration file: " + getConfigurationPropertiesFile(), e);
            }
        }
    }

    private void writeProps() {
        try {
            BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(getConfigurationPropertiesFile()));
            ACMGlobalConfigProperties.store(out, null);
            out.flush();
            out.close();
        } catch (IOException e) {
            throw new RuntimeException("Unable to write configuration file: " + getConfigurationPropertiesFile(), e);
        }
    }

}
