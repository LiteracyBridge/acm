package org.literacybridge.acm.util;

import org.literacybridge.acm.content.AudioItem;
import org.literacybridge.acm.content.LocalizedAudioItem;

public class LocalizedAudioItemNode {
	private final LocalizedAudioItem localizedAudioItem;
	private final String label;
	private final AudioItem parent;
	private boolean enabled;
	
	public LocalizedAudioItemNode(LocalizedAudioItem localizedAudioItem, String label, AudioItem parent) {
		this.label = label;
		this.localizedAudioItem = localizedAudioItem;
		this.parent = parent;
		enabled = false;
	}

	public AudioItem getParent() {
		return parent;
	}

	@Override 
	public String toString() {
		return label;
	}

	public LocalizedAudioItem getLocalizedAudioItem() {
		return localizedAudioItem;
	}

	public boolean isEnabled() {
		return enabled;
	}
	
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}	
}
