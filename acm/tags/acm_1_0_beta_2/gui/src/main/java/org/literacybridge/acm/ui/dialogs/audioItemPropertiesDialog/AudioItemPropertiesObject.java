package org.literacybridge.acm.ui.dialogs.audioItemPropertiesDialog;

import org.literacybridge.acm.metadata.MetadataField;

public class AudioItemPropertiesObject {
	private MetadataField<String> fieldID = null;
	
	public AudioItemPropertiesObject(MetadataField<String> fieldID) {
		this.fieldID = fieldID;
	}

	public MetadataField<String> getFieldID() {
		return fieldID;
	}	
}
