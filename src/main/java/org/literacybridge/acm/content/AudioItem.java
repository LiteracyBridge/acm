package main.java.org.literacybridge.acm.content;

import java.util.List;

public class AudioItem {
	/** Multiple translations of the same content share this ID */
	private String audioItemID;
	
	/** The categories this AudioItem belongs to */
	private List<String> categoryIDs;
	
	/** All available localized versions of this audio item */
	private List<LocalizedAudioItem> localizedItems;
}
