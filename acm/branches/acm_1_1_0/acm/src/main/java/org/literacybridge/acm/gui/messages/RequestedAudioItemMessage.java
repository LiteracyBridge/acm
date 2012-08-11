package org.literacybridge.acm.gui.messages;

import org.literacybridge.acm.content.AudioItem;

public class RequestedAudioItemMessage extends Message {
	private AudioItem audioItem;

	public RequestedAudioItemMessage(AudioItem audioItem) {
		super();
		this.audioItem = audioItem;
	}

	public AudioItem getAudioItem() {
		return audioItem;
	}
}
