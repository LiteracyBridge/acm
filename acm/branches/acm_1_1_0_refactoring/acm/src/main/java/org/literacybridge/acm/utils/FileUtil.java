package org.literacybridge.acm.utils;

import java.io.File;

public class FileUtil {

	public static File GetDirectoryOrNull(String directoryPath) {
		File file = getFileIfExistingOrNull(directoryPath);
		return (file != null && file.isDirectory()) ? file : null;
	}

	public static File GetFileOrNull(String filePath) {
		File file = getFileIfExistingOrNull(filePath);
		return (file != null && file.isFile()) ? file : null;
	}

	public static boolean isValidFile(File file) {
		return file != null && file.exists() && file.isFile(); 
	}
	
	public static boolean isValidDirectory(String directoryPath) {
		File file = new File(directoryPath);
		return file != null && file.exists() && file.isDirectory();
	}

	public static boolean isValidFile(String filePath) {
		File file = new File(filePath);
		return file != null && file.exists() && file.isFile(); 
	}
	
	public static boolean isValidDirectory(File file) {
		return file != null && file.exists() && file.isDirectory();
	}

	
	public static void createDirectoryIfNecessary(File file) {
		if (!isValidDirectory(file)) {
			file.mkdir();
		}
	}
	
    public static File combine(String... paths)
    {
        File file = new File(paths[0]);

        for (int i = 1; i < paths.length ; i++) {
            file = new File(file, paths[i]);
        }

        return file;
    }
	
	// Helper
	private static File getFileIfExistingOrNull(String filePath) {
		if (filePath != null) {
			File file = new File(filePath);
			if (file.exists()) {
				return file;
			}
		}
		
		return null;
	}
}
