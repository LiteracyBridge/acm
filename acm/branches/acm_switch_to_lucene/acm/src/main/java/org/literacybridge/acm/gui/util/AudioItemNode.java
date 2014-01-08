package org.literacybridge.acm.gui.util;

import org.literacybridge.acm.content.AudioItem;

public class AudioItemNode {
	private final AudioItem audioItem;
	private final String label;
	private boolean enabled;
	
	public AudioItemNode(String label, AudioItem audioItem) {
		this.label = label;
		this.audioItem = audioItem;
		enabled = false;
	}

	@Override 
	public String toString() {
		return label;
	}

	public AudioItem getAudioItem() {
		return audioItem;
	}

	public boolean isEnabled() {
		return enabled;
	}
	
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}	
}
