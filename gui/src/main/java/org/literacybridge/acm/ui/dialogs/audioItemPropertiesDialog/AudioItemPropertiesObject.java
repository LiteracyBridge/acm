package org.literacybridge.acm.ui.dialogs.audioItemPropertiesDialog;

import org.literacybridge.acm.metadata.MetadataField;

public class AudioItemPropertiesObject <T> {
	private MetadataField<T> fieldID = null;
	private final boolean editable;
	
	public AudioItemPropertiesObject(MetadataField<T> fieldID, boolean editable) {
		this.fieldID = fieldID;
		this.editable = editable;
	}

	public MetadataField<T> getFieldID() {
		return fieldID;
	}	
	
	public boolean isEditable() {
		return editable;
	}
}
