package org.literacybridge.acm.rcp.editors.audioItem;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.literacybridge.acm.content.AudioItem;
import org.literacybridge.acm.metadata.Metadata;
import org.literacybridge.acm.metadata.MetadataSpecification;
import org.literacybridge.acm.rcp.util.LanguageUtil;

public class AudioItemInput implements IEditorInput {

	private AudioItem audioItem = null;
	
	public AudioItemInput(AudioItem audioItem) {
		this.audioItem = audioItem;
	}
	
	public AudioItem getAudioItem() {
		return audioItem;
	}

	@Override
	public boolean exists() {
		return false;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return null;
	}

	@Override
	public String getName() {
		return getMetadata4DefaultLanguage().getMetadataValues(MetadataSpecification.DC_TITLE).get(0).getValue();
	}

	private Metadata getMetadata4DefaultLanguage() {
		return audioItem.getLocalizedAudioItem(LanguageUtil.getUserChoosenLanguage()).getMetadata();
	}
	
	@Override
	public IPersistableElement getPersistable() {
		return null;
	}

	@Override
	public String getToolTipText() {
		return getMetadata4DefaultLanguage().getMetadataValues(MetadataSpecification.DC_TITLE).get(0).getValue();
	}

	@Override
	public Object getAdapter(Class adapter) {
		return null;
	}

}
