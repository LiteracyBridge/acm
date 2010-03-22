package org.literacybridge.acm.rcp.views.requestResult.helpers;

import org.literacybridge.acm.content.AudioItem;
import org.literacybridge.acm.content.LocalizedAudioItem;
import org.literacybridge.acm.rcp.util.LanguageUtil;

public class AudioItemTableRowAdapter implements IAudioItemTableRowAdapter {

	private AudioItem audioItem = null;

	public AudioItemTableRowAdapter(AudioItem audioItem) {
		this.audioItem = audioItem;
	}

	public AudioItem getAudioItem() {
		return audioItem;
	}

	@Override
	public LocalizedAudioItem getDescriptor() {
		// Three steps to determine the best fitting localized item
		LocalizedAudioItem bestFittingItem = null;
		
		// 1. "Your default language"
		bestFittingItem = audioItem.getLocalizedAudioItem(LanguageUtil.getUserChoosenLanguage());
		if (bestFittingItem != null) return bestFittingItem;
		
		// 2. Fallback language
		bestFittingItem = audioItem.getLocalizedAudioItem(LanguageUtil.GetFallbackLanguage());
		if (bestFittingItem != null) return bestFittingItem;
		
		// 3. First existing item (there must be at least one audioItem)
		return audioItem.getLocalizedAudioItem(audioItem.getAvailableLocalizations().iterator().next());
	}
}
