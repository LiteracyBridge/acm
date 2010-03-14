package org.literacybridge.acm.content;

import java.io.File;
import java.util.Locale;

import org.literacybridge.acm.metadata.Metadata;

public class LocalizedAudioItem {
	/** This ID must be unique across all audio items */
	private String uID;
	
	/** The language and country codes */
	private Locale locale;
	
	/** Metadata of this audio item */
	private Metadata metadata;
	
	/** This should be the path to the DAISY package.xml file */
	private File pathToItem;
	
	/** Contains references to all files that are referenced from this item */
	private Manifest manifest;
	
	public LocalizedAudioItem(String uID, Locale locale, File pathToItem) {
		this.uID = uID;
		this.locale = locale;
		this.pathToItem = pathToItem;
		this.metadata = new Metadata();
		this.manifest = new Manifest();
	}
	
	public Locale getLocale() {
		return this.locale;
	}
	
	public File getPackageFile() {
		return this.pathToItem;
	}
	
	public Metadata getMetadata() {
		return this.metadata;
	}
	
	public Manifest getManifest() {
		return this.manifest;
	}
}
