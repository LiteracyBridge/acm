package org.literacybridge.acm.repository;

import static org.literacybridge.acm.Constants.REPOSITORY_DIR;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.UUID;

import org.literacybridge.acm.content.LocalizedAudioItem;
import org.literacybridge.acm.metadata.Metadata;
import org.literacybridge.acm.metadata.MetadataSpecification;

public class Repository {
	private final File baseDir = REPOSITORY_DIR;
	
	private final static Repository instance = new Repository();
	
	private Repository() {
		// singleton
	}
	
	public static Repository getRepository() {
		return instance;
	}
	
	public void initialize() throws IOException {
		if (!baseDir.exists()) {
			baseDir.mkdirs();
		}
	}

	public static String getNewUUID() {
		UUID uuid = UUID.randomUUID();
		return uuid.toString();
	}
	
	/**
	 * Copies the given file to the directory of the localizedAudioItem
	 */
	public void store(File file, String targetFileName, LocalizedAudioItem localizedAudioItem) throws IOException {
		File dir = resolveName(localizedAudioItem);
		File toFile = new File(dir, targetFileName);
		copy(file, toFile);
	}
	
	// Returns the path of this audioitem in the repository
	public File resolveName(LocalizedAudioItem localizedAudioItem) {
		// TODO: For now we just use the unique ID of the audio item; in the future, we might want to use
		// a different way to construct the path
		
		StringBuilder builder = new StringBuilder();
		builder.append(baseDir.getAbsolutePath());
		builder.append(File.separator);
		builder.append(localizedAudioItem.getParentAudioItem().getUuid());
		builder.append(File.separator);
		Locale locale = localizedAudioItem.getLocale();
		builder.append(File.separator);
		builder.append(locale.getISO3Language());
		builder.append('-');
		builder.append(locale.getISO3Country());
		Metadata metadata = localizedAudioItem.getMetadata();
		String revision = metadata.getMetadataValues(MetadataSpecification.DTB_REVISION).get(0).getValue();
		builder.append(File.separator);
		builder.append(revision);
		
		String path = builder.toString();
		File dir = new File(path);
		if (!dir.exists()) {
			dir.mkdirs();
		}

		return dir;
	}
	
	public static void copy(File fromFile, File toFile) throws IOException {
	    FileInputStream from = null;
	    FileOutputStream to = null;
	    try {
	      from = new FileInputStream(fromFile);
	      to = new FileOutputStream(toFile);
	      byte[] buffer = new byte[4096];
	      int bytesRead;
	
	      while ((bytesRead = from.read(buffer)) != -1)
	        to.write(buffer, 0, bytesRead); // write
	    } finally {
	      if (from != null)
	        try {
	          from.close();
	        } catch (IOException e) {
	          // ignore
	        } finally {
		      if (to != null)
		        try {
		          to.close();
		        } catch (IOException e) {
		          // ignore
		        }
	        }
	    }
	}
}
