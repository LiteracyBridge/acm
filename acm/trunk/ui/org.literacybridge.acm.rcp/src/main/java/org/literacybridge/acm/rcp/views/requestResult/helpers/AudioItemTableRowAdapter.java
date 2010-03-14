package org.literacybridge.acm.rcp.views.requestResult.helpers;

import org.literacybridge.acm.categories.Taxonomy.Category;
import org.literacybridge.acm.content.AudioItem;

public class AudioItemTableRowAdapter implements IAudioItemTableRowAdapter {

	private AudioItem audioItem = null;
	// category this audioItems comes from
	private Category category = null;
	
	public AudioItemTableRowAdapter(AudioItem audioItem, Category category) {
		this.audioItem = audioItem;
		this.category = category;
	}

	public AudioItem getAudioItem() {
		return audioItem;
	}

	public Category getCategory() {
		return category;
	}
}
