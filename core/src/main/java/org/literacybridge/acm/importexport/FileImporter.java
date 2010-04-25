package org.literacybridge.acm.importexport;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.literacybridge.acm.categories.Taxonomy.Category;

public abstract class FileImporter {
	private FileFilter filter;
	
	public FileImporter(FileFilter fileFilter) {
		this.filter = fileFilter;
	}
	
	protected abstract void importSingleFile(Category category, File file) throws IOException;
	
	public void importFile(Category category, File file) throws IOException {
		if (!file.exists()) {
			throw new FileNotFoundException(file.toString());
		}
		
		if (file.isDirectory()) {
			throw new IllegalArgumentException(file.toString() + " is a directory.");
		} else {
			importSingleFile(category, file);
		}
	}
	
	public void importDirectory(Category category, File dir, boolean recursive) throws IOException {
		List<File> filesToImport = new LinkedList<File>();
		gatherFiles(dir, recursive, filesToImport);
		
		for (File f : filesToImport) {
			importSingleFile(category, f);
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
	
	public static FileFilter getFileExtensionFilter(final String extension) {
		return new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				if (pathname.isDirectory()) {
					return false;
				}
				String name = pathname.getName();
				return name.substring(name.length() - 4, name.length()).equalsIgnoreCase(extension);
			}
		};
	}
}
