package org.literacybridge.acm.gui.messages;

import org.literacybridge.acm.content.LocalizedAudioItem;

public class PlayLocalizedAudioItemMessage extends Message {

	private LocalizedAudioItem localizedAudioItem;

	public PlayLocalizedAudioItemMessage(LocalizedAudioItem localizedAudioItem) {
		super();
		this.localizedAudioItem = localizedAudioItem;
	}

	public LocalizedAudioItem getLocalizedAudioItem() {
		return localizedAudioItem;
	}
}
