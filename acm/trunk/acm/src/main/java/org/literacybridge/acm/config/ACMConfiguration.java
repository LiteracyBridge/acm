package org.literacybridge.acm.config;

import java.io.File;
import java.io.FileFilter;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import org.literacybridge.acm.Constants;
import org.literacybridge.acm.gui.CommandLineParams;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class ACMConfiguration {
	private static final Logger LOG = Logger.getLogger(ACMConfiguration.class.getName());
	
	private static final Map<String, DBConfiguration> allDBs = Maps.newHashMap();
	private static final AtomicReference<DBConfiguration> currentDB = new AtomicReference<DBConfiguration>();
	static final File LB_HOME_DIR = new File(Constants.USER_HOME_DIR, Constants.LiteracybridgeHomeDirName);
	
    private static String title;
    private static boolean disableUI = false;
	private static final Properties ACMGlobalConfigProperties = new Properties();
	private static File globalShareDir;
	
	public synchronized static void initialize(CommandLineParams args) {
		if (args.titleACM != null) {
			title = args.titleACM;
		}
		
		disableUI = args.disableUI;
		
		setupACMGlobalPaths();
		
		for (DBConfiguration config : discoverDBs()) {
			allDBs.put(config.getSharedACMname(), config);
			System.out.println("Found DB " + config.getSharedACMname());
		}
	}

	// TODO: when we have a homescreen this method needs to be split up into different steps,
	// e.g. close DB, open new DB, etc.
	public synchronized static void setCurrentDB(String dbName) throws Exception {
		DBConfiguration config = allDBs.get(dbName);
		
		if (config == null) {
			throw new IllegalArgumentException("DB '" + dbName + "' not known.");
		}
		
		DBConfiguration oldDB = currentDB.get();
		if (oldDB != null) {
			oldDB.getDatabaseConnection().close();
		}

		config.init(); 
		
		System.out.println("Connecting to DB " + config.getSharedACMname());
		config.connectDB();
		
		currentDB.set(config);
	}
	
	public synchronized static DBConfiguration getCurrentDB() {
		return currentDB.get();
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
	
	public static File getGlobalShareDir() {
		return globalShareDir;
	}
}
