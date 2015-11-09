package org.literacybridge.acm.config;

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
import java.util.logging.Logger;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import org.literacybridge.acm.Constants;
import org.literacybridge.acm.gui.CommandLineParams;
import org.literacybridge.acm.store.Taxonomy;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class ACMConfiguration {
    private static final Logger LOG = Logger.getLogger(ACMConfiguration.class.getName());

    private static final Map<String, DBConfiguration> allDBs = Maps.newHashMap();
    private static final AtomicReference<DBConfiguration> currentDB = new AtomicReference<DBConfiguration>();
    static final File LB_HOME_DIR = new File(Constants.USER_HOME_DIR, Constants.LiteracybridgeHomeDirName);

    private static String title;
    private static boolean disableUI = false;
    private static boolean forceSandbox = false;
    private static final Properties ACMGlobalConfigProperties = new Properties();
    private static File globalShareDir;

    public synchronized static void initialize(CommandLineParams args) {
        loadProps();

        if (args.titleACM != null) {
            title = args.titleACM;
        }

        disableUI = args.disableUI;
        forceSandbox = args.sandbox;

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
    public synchronized static void setCurrentDB(String dbName, boolean createEmptyDB) throws Exception {
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
        currentDB.set(config);
    }

    public synchronized static void closeCurrentDB() {
        DBConfiguration oldDB = currentDB.get();
        if (oldDB != null) {
            oldDB.getDatabaseConnection().close();
        }
    }

    public synchronized static DBConfiguration getCurrentDB() {
        return currentDB.get();
    }

    public static File dirACM(String acmName) {
        File f = null;
        loadProps();

        String globalSharePath = ACMGlobalConfigProperties.getProperty(Constants.GLOBAL_SHARE_PATH);
        if (globalSharePath != null) {
            f = new File (globalSharePath,acmName + "/" + Constants.TBLoadersHomeDir);
            if (!f.exists()) {
                f = null;
            }
        }
        return f;
    }

    public static String getUserName() {
        return ACMGlobalConfigProperties.getProperty("USER_NAME");
    }

    public static String getUserContact() {
        return ACMGlobalConfigProperties.getProperty(Constants.USER_CONTACT_INFO);
    }

    public static String getRecordingCounter() {
        return ACMGlobalConfigProperties.getProperty(Constants.RECORDING_COUNTER_PROP);
    }

    public static void setRecordingCounter(String counter) {
        ACMGlobalConfigProperties.setProperty(Constants.RECORDING_COUNTER_PROP, counter);
        ACMConfiguration.writeProps();
    }

    public static String getNewAudioItemUID() throws IOException {
        String value = ACMConfiguration.getRecordingCounter();
        int counter = (value == null) ? 0 : Integer.parseInt(value, Character.MAX_RADIX);
        counter++;
        value = Integer.toString(counter, Character.MAX_RADIX);
        String uuid = "LB-2" + "_"  + getDeviceID() + "_" + value;

        // make sure we remember that this uuid was already used
        ACMConfiguration.setRecordingCounter(value);
        //writeProps();

        return uuid;
    }

    public static String getDeviceID() throws IOException {
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


    private static void setupACMGlobalPaths() {
        String globalSharePath = ACMGlobalConfigProperties.getProperty(Constants.GLOBAL_SHARE_PATH);
        if (globalSharePath != null) {
            globalShareDir = new File (globalSharePath);
        }

        if (globalSharePath == null || globalShareDir == null || !globalShareDir.exists()) {
            //try default dropbox installation
            globalShareDir = new File (Constants.USER_HOME_DIR, Constants.DefaultSharedDirName1);
            if (!globalShareDir.exists()) {
                globalShareDir = new File (Constants.USER_HOME_DIR, Constants.DefaultSharedDirName2);
            }

        }

        if (!globalShareDir.exists()) {
            JFileChooser fc = new JFileChooser();
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            fc.setDialogTitle("Select Dropbox directory.");
            int returnVal = fc.showOpenDialog(null);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                globalShareDir = fc.getSelectedFile();
            } else {
                JOptionPane.showMessageDialog(null,"Dropbox directory has not been identified. Shutting down.");
                System.exit(0);
            }
        }

        ACMGlobalConfigProperties.put(Constants.GLOBAL_SHARE_PATH, globalShareDir.getAbsolutePath());
    }

    private static List<DBConfiguration> discoverDBs() {
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

    public static String getACMname() {
        return title;
    }

    public static boolean isDisableUI() {
        return disableUI;
    }

    public static boolean isForceSandbox() {
        return forceSandbox;
    }

    public static File getGlobalShareDir() {
        return globalShareDir;
    }

    private static File getConfigurationPropertiesFile() {
        return new File(LB_HOME_DIR, Constants.GLOBAL_CONFIG_PROPERTIES);
    }

    public static void loadProps() {
        if (getConfigurationPropertiesFile().exists()) {
            try {
                BufferedInputStream in = new BufferedInputStream(new FileInputStream(getConfigurationPropertiesFile()));
                ACMGlobalConfigProperties.load(in);
            } catch (IOException e) {
                throw new RuntimeException("Unable to load configuration file: " + getConfigurationPropertiesFile(), e);
            }
        }
    }

    public static void writeProps() {
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
