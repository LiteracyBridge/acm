package org.literacybridge.acm.gui.dialogs.audioItemPropertiesDialog;

import org.literacybridge.acm.metadata.MetadataField;
import org.literacybridge.acm.metadata.RFC3066LanguageCode;

public class AudioItemRFC3066LanguageProperty extends AudioItemPropertiesObject<RFC3066LanguageCode> {

	public AudioItemRFC3066LanguageProperty(MetadataField<RFC3066LanguageCode> code, boolean isEditable) {
		super(code, isEditable);
	}
}
