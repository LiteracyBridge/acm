package org.literacybridge.acm.device;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class DeviceContents {
	public final static String CONFIG_FILE = "config.txt";
	public final static String LIST_SUBFOLDER_PROPERTY_NAME = "LIST_PATH";
	public final static String USER_SUBFOLDER_PROPERTY_NAME = "USER_PATH";
	public final static String LIST_TXT_FILE_SUFFIX = ".txt";
	
	public final static String MASTER_LIST_PROPERTY_NAME = "LIST_MASTER";
	
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
	private List<CategoryList> lists;
	private CategoryList masterList;
	
	public DeviceContents(File pathToDevice) throws IOException {
		this.pathToDevice = pathToDevice;
		lists = new ArrayList<DeviceContents.CategoryList>();
		loadDeviceInfos();
	}
	
	public List<File> loadAudioItems() throws IOException {
		List<File> audioItems = new ArrayList<File>();
		String userPath = cleanPath(deviceConfig.getProperty(USER_SUBFOLDER_PROPERTY_NAME));
		File userFolder = new File(pathToDevice, userPath);
		
		for (CategoryList list : lists) {
			for (CategoryList.Item item : list.audioItems) {
				if (!item.isApplication) {
					audioItems.add(new File(userFolder, item.audioItemName + ".a18"));
				}
			}
		}
		
		return audioItems;
	}
	
	private void loadDeviceInfos() throws IOException {
		// first load config file
		deviceConfig = new Properties();
		FileReader in = null;
		try {
			in = new FileReader(new File(pathToDevice, CONFIG_FILE));
			deviceConfig.load(in);
		} finally {
			if (in != null) {
				in.close();
			}
		}
		
		// now load lists
		loadLists();
	}
	
	private void loadLists() throws IOException { 
		String listPath = cleanPath(deviceConfig.getProperty(LIST_SUBFOLDER_PROPERTY_NAME));
		
		File listFolder = new File(pathToDevice, listPath);
		File[] listTxtFiles = listFolder.listFiles(new FilenameFilter() {
			@Override public boolean accept(File dir, String name) {
				return name.endsWith(LIST_TXT_FILE_SUFFIX);
			}
		});
		
		File masterListTxtFile = new File(pathToDevice,
				cleanPath(deviceConfig.getProperty(MASTER_LIST_PROPERTY_NAME)));
		masterList = loadList(masterListTxtFile);
		
		for (File listTxtFile : listTxtFiles) {
			CategoryList l = loadList(listTxtFile);
			if (!l.name.equals(masterList.name)) {
				lists.add(l);
			}
		}
	}
	
	private CategoryList loadList(File listTxtFile) throws IOException {
		String fileName = listTxtFile.getName();
		String listName = listTxtFile.getName().substring(
				0, fileName.length() - LIST_TXT_FILE_SUFFIX.length());
		
		CategoryList list = new CategoryList(listName);
		
		BufferedReader in = null;
		try {
			in = new BufferedReader(new FileReader(listTxtFile));
			while (in.ready()) {
				String audioItemName = in.readLine();
				list.audioItems.add(new CategoryList.Item(audioItemName));
			}
		} finally {
			if (in != null) {
				in.close();
			}
		}
		
		return list;
	}
	
	public String getConfigProperty(String propertyName) {
		return deviceConfig.getProperty(propertyName);
	}
	
	public static void main(String args[]) throws IOException {
		File pathToDevice = new File(args[0]);
		DeviceContents contents = new DeviceContents(pathToDevice);
		System.out.println("Config\n=======================");
		System.out.println(contents.deviceConfig);
		System.out.println();
		System.out.println("Master list\n=======================");
		System.out.println(contents.masterList);
		System.out.println();

		System.out.println("Lists\n=======================");
		for (CategoryList list : contents.lists) {
			System.out.println(list);
			System.out.println();
		}
	}
	
	private static final String cleanPath(String path) {
		if (path.startsWith("a:\\")) {
			path = path.substring(3);
		}
		
		path = path.replaceAll("\\\\", "/");
		
		return path;
	}
}
