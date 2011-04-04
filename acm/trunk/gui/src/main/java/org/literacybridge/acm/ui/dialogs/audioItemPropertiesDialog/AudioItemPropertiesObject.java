package org.literacybridge.acm.ui.dialogs.audioItemPropertiesDialog;

import org.literacybridge.acm.metadata.MetadataField;

public class AudioItemPropertiesObject <T> {
	private MetadataField<T> fieldID = null;
	
	public AudioItemPropertiesObject(MetadataField<T> fieldID) {
		this.fieldID = fieldID;
	}

	public MetadataField<T> getFieldID() {
		return fieldID;
	}	
}
