package org.literacybridge.acm.importexport;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.literacybridge.acm.categories.Taxonomy.Category;
import org.literacybridge.acm.config.Configuration;
import org.literacybridge.acm.content.AudioItem;
import org.literacybridge.acm.metadata.MetadataSpecification;
import org.literacybridge.acm.metadata.MetadataValue;

public class FileImporter {
	private static final Logger LOG = Logger.getLogger(FileImporter.class.getName());
	
	public static abstract class Importer {
		protected abstract void importSingleFile(Category category, File file) throws IOException;
		protected abstract String[] getSupportedFileExtensions();
	}
	
	private FileFilter filter;
	private Map<String, Importer> map;
	
	private static FileImporter instance = new FileImporter(new Importer[] {
			new A18Importer(), new MP3Importer(), new WAVImporter()
	});
	
	public static FileImporter getInstance() {
		return instance;
	}
	
	private FileImporter(Importer... importers) {
		Set<String> extensions = new HashSet<String>();
		map = new HashMap<String, Importer>();
		
		for (Importer imp : importers) {
			for (String extension : imp.getSupportedFileExtensions()) {
				extensions.add(extension);
				map.put(extension, imp);
			}
		}
		
		filter = getFileExtensionFilter(extensions);
	}
	
	
	public void importFile(Category category, File file) throws IOException {
		if (!file.exists()) {
			throw new FileNotFoundException(file.toString());
		}
		
		if (file.isDirectory()) {
			throw new IllegalArgumentException(file.toString() + " is a directory.");
		} else {			
			String ext = getFileExtension(file);
			Importer imp = map.get(ext);
			if (imp == null) {
				throw new UnsupportedOperationException(ext + " not supported.");
			}
			
			String title = stripFileExtension(file);
			// check if file name matches an existing audio item id
			
			AudioItem item = AudioItem.getFromDatabase(title);
			if (item != null) {
				try {
					Configuration.getRepository().updateAudioItem(item, file);
					// Commenting line below since duration is set with updateDuration as called from storeAudioFile()
					// item.getLocalizedAudioItem(null).getMetadata().setMetadataField(MetadataSpecification.LB_DURATION, new MetadataValue<String>(""));
					item.commit();
				} catch (Exception e) {
					LOG.log(Level.WARNING, "Unable to update files for audioitem with id=" + title, e);
				}
			} else {
				// new audio item - import
				imp.importSingleFile(category, file);
			}
		}
	}
	
	public void importDirectory(Category category, File dir, boolean recursive) throws IOException {
		List<File> filesToImport = new LinkedList<File>();
		gatherFiles(dir, recursive, filesToImport);
		
		for (File f : filesToImport) {
			try {
				importFile(category, f);
			} catch (Exception e) {
				LOG.log(Level.WARNING, "Failed to import file " + f, e);
			}
		}
	}
	
	private void gatherFiles(File dir, boolean recursive, List<File> filesToImport) throws IOException {
		File[] files = dir.listFiles(filter);
		for (File f : files) {
			filesToImport.add(f);
		}
		
		if (recursive) {
			File[] subdirs = dir.listFiles(new FileFilter() {
				@Override
				public boolean accept(File pathname) {
					return pathname.isDirectory();
				}
			});
		
			for (File subDir : subdirs) {
				gatherFiles(subDir, recursive, filesToImport);
			}
		}
	}
	
	public static String getFileExtension(File file) {
		return getFileExtension(file.getName());
	}
	
	public static String getFileExtension(String fileName) {
		return fileName.substring(fileName.length() - 4, fileName.length()).toLowerCase();
	}

	public static String stripFileExtension(File file) {
		return stripFileExtension(file.getName());
	}
	
	public static String stripFileExtension(String fileName) {
		return fileName.substring(0, fileName.length() - 4);
	}
	
	public static FileFilter getFileExtensionFilter(final Set<String> extensions) {
		return new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				if (pathname.isDirectory()) {
					return false;
				}
				
				return extensions.contains(getFileExtension(pathname));
			}
		};
	}
}
