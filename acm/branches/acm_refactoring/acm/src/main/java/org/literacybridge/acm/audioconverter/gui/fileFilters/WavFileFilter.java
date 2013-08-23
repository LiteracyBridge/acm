package org.literacybridge.acm.audioconverter.gui.fileFilters;

import java.io.File;

import javax.swing.filechooser.FileFilter;

public class WavFileFilter extends FileFilter {

	// used to customize description string
	private boolean isSourceFilter = true; 
	
	public WavFileFilter(boolean sourceFilter) {
		this.isSourceFilter = sourceFilter;
	}
	
	public boolean accept(File f) {
		if (f.isDirectory()) {
			return true;
	    }

	    String extension = Utils.getExtension(f);
	    if (isSourceFilter && extension != null) {
			if (extension.equals(Utils.wav)) {
		        return true;
			}
	    }

		return false;	// false as default
	}

	public String getDescription() {
		if (isSourceFilter) {
			return "wav Audio files (*.wav)";
		} else {
			return "Convert to WAV audio files (*.wav)";
		}
	}
}
