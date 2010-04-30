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

import org.literacybridge.acm.categories.Taxonomy.Category;

public class FileImporter {
	public static abstract class Importer {
		protected abstract void importSingleFile(Category category, File file) throws IOException;
		protected abstract String[] getSupportedFileExtensions();
	}
	
	private FileFilter filter;
	private Map<String, Importer> map;
	
	private static FileImporter instance = new FileImporter(new Importer[] {
			new A18Importer(), new MP3Importer()
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
			imp.importSingleFile(category, file);
		}
	}
	
	public void importDirectory(Category category, File dir, boolean recursive) throws IOException {
		List<File> filesToImport = new LinkedList<File>();
		gatherFiles(dir, recursive, filesToImport);
		
		for (File f : filesToImport) {
			importFile(category, f);
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
		String name = file.getName();
		return name.substring(name.length() - 4, name.length()).toLowerCase();
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
