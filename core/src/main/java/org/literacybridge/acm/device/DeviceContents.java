package org.literacybridge.acm.device;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.literacybridge.acm.importexport.StatisticsImporter;

public class DeviceContents {
	public final static String CONFIG_FILE = "config.txt";
	public final static String SYSTEM_SUBFOLDER = "system";
	public final static String STATISTICS_SUBFOLDER = "statistics";
	public final static String MESSAGES_SUBFOLDER = "messages";
	public final static String LISTS_SUBFOLDER = "lists";
	public final static String LANGUAGES_FILE = "languages.txt";
	public final static String TOPICS_FILE = "topics.txt";
	
	public final static String LANGUAGES_SUBFOLDER_PROPERTY_NAME = "LANGUAGES_PATH";
	public final static String LIST_MASTER_PROPERTY_NAME = "LIST_MASTER";
	public final static String LISTS_SUBFOLDER_PROPERTY_NAME = "LISTS_PATH";
	public final static String USER_SUBFOLDER_PROPERTY_NAME = "USER_PATH";
	public final static String LIST_TXT_FILE_SUFFIX = ".txt";
	
	public final static String STATS_SUB_DIR = "stats";
	public final static String OTHER_DEVICE_STATS_SUB_DIR = "ostats";
	
	public static class CategoryList {
		public static class Item {
			public String audioItemName;
			public boolean isApplication;
			
			public Item(String name) {
				if (name.startsWith("^")) {
					isApplication = true;
					audioItemName = name.substring(1);
				} else {
					isApplication = false;
					audioItemName = name;
				}
			}
			
			public String getName() {
				return audioItemName;
			}
			
			public boolean isAudioApplication() {
				return isApplication;
			}
			
			@Override public String toString() {
				return (isApplication ? "^" : "") + audioItemName;
			}
		}
		
		private String name;
		private List<Item> audioItems;
		
		private CategoryList(String name) {
			this.name = name;
			this.audioItems = new ArrayList<Item>();
		}
		
		@Override public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("[" + name + "]").append('\n');;
			for (Item audioItem : audioItems) {
				builder.append(audioItem).append('\n');
			}
			return builder.toString();
		}
	}
	
	private File pathToDevice;
	private Properties deviceConfig;
	
	public DeviceContents(File pathToDevice) throws IOException {
		this.pathToDevice = pathToDevice;
		loadDeviceInfos();
	}
	
	public void importStats() throws IOException {
		internalImportStats(STATS_SUB_DIR);		
	}
	
	public void importOtherDeviceStats() throws IOException {
		internalImportStats(OTHER_DEVICE_STATS_SUB_DIR);
	}
	
	private void internalImportStats(String subFolder) throws IOException  {
		StatisticsImporter importer = new StatisticsImporter();
		File systemPath = new File(pathToDevice, STATISTICS_SUBFOLDER);
		File statsPath = new File(systemPath, subFolder);
		importer.importStatsFolder(statsPath);		
	}
	
	public List<File> loadAudioItems() throws IOException {
		List<File> audioItems = new ArrayList<File>();
		String userPath = cleanPath(deviceConfig.getProperty(USER_SUBFOLDER_PROPERTY_NAME));
		File userFolder = new File(pathToDevice, userPath);
		
		for (File f : userFolder.listFiles(new FileFilter() {			
			@Override public boolean accept(File file) {
				return file.getName().toLowerCase().endsWith(".a18");
			}
		})) {
			audioItems.add(f);
		}
		
		return audioItems;
	}
	
	private void loadDeviceInfos() throws IOException {
		// first load config file
		deviceConfig = new Properties();
		BufferedReader in = null;
		try {
			File systemPath = new File(pathToDevice, SYSTEM_SUBFOLDER);
			in = new BufferedReader(new FileReader(new File(systemPath, CONFIG_FILE)));
			//legacyParse(in, deviceConfig);
			deviceConfig.load(in);
		} finally {
			if (in != null) {
				in.close();
			}
		}
	}
		
	public String getConfigProperty(String propertyName) {
		return deviceConfig.getProperty(propertyName);
	}
	
	private static List<String> loadListFromFile(File f) throws IOException {
		List<String> list = new ArrayList<String>();
		
		BufferedReader in = null;
		try {
			in = new BufferedReader(new FileReader(f));
			while (in.ready()) {
				String line = in.readLine();
				list.add(line);
			}
		} finally {
			if (in != null) {
				in.close();
			}
		}

		return list;
	}
	
	public static void main(String args[]) throws IOException {
		File pathToDevice = new File(args[0]);
		DeviceContents contents = new DeviceContents(pathToDevice);
		System.out.println("Config\n=======================");
		System.out.println(contents.deviceConfig);
		System.out.println();
		System.out.println();
		System.out.println(contents.loadAudioItems());
	}
	
	private static final String cleanPath(String path) {
		if (path.startsWith("a:/")) {
			path = path.substring(3);
		}
		
		path = path.replaceAll("\\\\", "/");
		
		return path;
	}
}
