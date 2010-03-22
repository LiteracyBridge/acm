package org.literacybridge.acm.rcp.views.requestResult.helpers;

import org.literacybridge.acm.content.AudioItem;
import org.literacybridge.acm.content.LocalizedAudioItem;

public interface IAudioItemTableRowAdapter {
	
	/**
	 * The AudioItem that is represented by this adapter.
	 * 
	 * @return audio item
	 */
	public AudioItem getAudioItem();

	/**
	 * The details of this item will be shown to the user as general
	 * description of the audio item.
	 *  
	 * @return the item in the language that fits best to the needs of the user
	 */
	public LocalizedAudioItem getDescriptor();
}