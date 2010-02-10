package main.java.org.literacybridge.acm.content;

import java.io.File;

import main.java.org.literacybridge.acm.metadata.MetadataSet;

public class LocalizedAudioItem {
	/** This ID must be unique across all audio items */
	private String uID;
	
	/** The language code */
	private String language;
	
	/** Metadata of this audio item */
	private MetadataSet metadata;
	
	/** This should be the path to the DAISY package.xml file */
	private File pathToItem;
	
	/** Contains references to all files that are referenced from this item */
	private Manifest manifest;
}
