package org.literacybridge.acm.audioconverter.gui.fileFilters;

import java.io.File;

import javax.swing.filechooser.FileFilter;

public class A18FileFilter extends FileFilter {

	// used to customize description string
	private boolean isSourceFilter = true; 
	
	public A18FileFilter(boolean sourceFilter) {
		this.isSourceFilter = sourceFilter;
	}
	
	public boolean accept(File f) {
		if (f.isDirectory()) {
			return true;
	    }

	    String extension = Utils.getExtension(f);
	    if (extension != null) {
			if (isSourceFilter && extension.equals(Utils.a18)) {
		        return true;
			}
	    }

		return false;	// false as default
	}

	public String getDescription() {		
		if (isSourceFilter) {
			return "A18 Audio files (*.a18)";
		} else {
			return "Convert to A18 audio files (*.a18)";
		}
	}

}
