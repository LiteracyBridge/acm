package org.literacybridge.acm.rcp.views.requestResult.helpers;

import org.literacybridge.acm.categories.Taxonomy.Category;
import org.literacybridge.acm.content.AudioItem;

public interface IAudioItemTableRowAdapter {
	public AudioItem getAudioItem();
	public Category getCategory();
}