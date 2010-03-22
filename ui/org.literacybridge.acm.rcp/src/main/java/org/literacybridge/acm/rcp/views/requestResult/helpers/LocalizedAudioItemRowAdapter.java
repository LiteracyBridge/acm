package org.literacybridge.acm.rcp.views.requestResult.helpers;

import org.literacybridge.acm.content.AudioItem;
import org.literacybridge.acm.content.LocalizedAudioItem;

public class LocalizedAudioItemRowAdapter implements
		ILocalizedAudioItemTableRowAdapter {

	private LocalizedAudioItem item = null;
	private AudioItem parent = null;
	
	public LocalizedAudioItemRowAdapter(LocalizedAudioItem item, AudioItem parent) {
		this.item = item;
		this.parent = parent;
	}
	
	@Override
	public LocalizedAudioItem getLocalizedAudioItem() {
		return item;
	}

	@Override
	public AudioItem getParent() {
		return parent;
	}
}
