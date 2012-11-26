package org.literacybridge.acm.content;

import java.util.Locale;


public class LocalizedAudioLabel {
	private Locale locale;
	private String label;
	private String description;
	private LocalizedAudioItem audioLabel;
	
	public LocalizedAudioLabel(String label, String description, LocalizedAudioItem audioLabel) {
		this.label = label;
		this.description = description;
		this.audioLabel = audioLabel;
	}
	
	public String getLabel() {
		return this.label;
	}
	
	public String getDescription() {
		return this.description;
	}
	
	public LocalizedAudioItem getAudioLable() {
		return this.audioLabel;
	}
	
	@Override
	public String toString() {
		return this.label;
	}
}
