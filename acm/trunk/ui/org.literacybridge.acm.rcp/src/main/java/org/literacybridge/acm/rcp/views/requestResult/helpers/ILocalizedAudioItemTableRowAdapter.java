package org.literacybridge.acm.rcp.views.requestResult.helpers;

import org.literacybridge.acm.content.AudioItem;
import org.literacybridge.acm.content.LocalizedAudioItem;

public interface ILocalizedAudioItemTableRowAdapter {
	
	/**
	 * The AudioItem that is the parent of this localized audio item.
	 * 
	 * @return parent
	 */
	public AudioItem getParent();

	/**
	 * LocalizedAudioItem represented by this adapter.
	 * 
	 * @return item
	 */
	public LocalizedAudioItem getLocalizedAudioItem();
	
}
