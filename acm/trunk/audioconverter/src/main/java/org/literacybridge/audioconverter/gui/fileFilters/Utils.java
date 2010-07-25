package org.literacybridge.audioconverter.gui.fileFilters;

import java.io.File;

public class Utils {

	public final static String a18 = "a18";
	public final static String wav = "wav";
	public final static String mp3 = "mp3";
	
	/*
	 * Get the extension of a file.
	 */  
	public static String getExtension(File f) {
	    String ext = null;
	    String s = f.getName();
	    int i = s.lastIndexOf('.');
	
	    if (i > 0 &&  i < s.length() - 1) {
	        ext = s.substring(i+1).toLowerCase();
	    }
	    return ext;
	}
}

