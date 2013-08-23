package org.literacybridge.acm.gui.dialogs.audioItemPropertiesDialog;

import java.util.Locale;

import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.content.AudioItem;
import org.literacybridge.acm.gui.resourcebundle.LabelProvider;
import org.literacybridge.acm.gui.util.language.LanguageUtil;
import org.literacybridge.acm.metadata.Metadata;
import org.literacybridge.acm.metadata.MetadataField;
import org.literacybridge.acm.metadata.RFC3066LanguageCode;

public abstract class AudioItemProperty<V> {
	private final boolean isCellEditable;
	private final boolean showEditIcon;
	
	public AudioItemProperty(boolean editable) {
		this(editable, false);
	}

	public AudioItemProperty(boolean isCellEditable, boolean showEditIcon) {
		this.isCellEditable = isCellEditable;
		this.showEditIcon = showEditIcon;
	}
	
	public abstract String getName();
	public abstract String getValue(AudioItem audioItem, Metadata metadata);
	public abstract void setValue(AudioItem audioItem, Metadata metadata, V newValue);
	
	public boolean isCellEditable() {
		return isCellEditable;
	}
	
	public boolean showEditIcon() {
		return !ACMConfiguration.getCurrentDB().getControlAccess().isACMReadOnly() && showEditIcon;
	}
	
	public static class MetadataProperty extends AudioItemProperty<String> {
		private final MetadataField<String> field;
		
		public MetadataProperty(MetadataField<String> field,
								boolean editable) {
			super(editable);
			this.field = field;
		}
		
		public MetadataField<?> getMetadataField() {
			return field;
		}
		
		public String getName() {
			return LabelProvider.getLabel(field, LanguageUtil.getUILanguage());
		}
		
		public String getValue(AudioItem audioItem, Metadata metadata) {
			return Metadata.getCommaSeparatedList(metadata, field);
		}

		@Override
		public void setValue(AudioItem audioItem, Metadata metadata,
							  String newValue) {
			AudioItemPropertiesModel.setStringValue(field, metadata, newValue);
		}
	}

	public static class LanguageProperty extends AudioItemProperty<Locale> {
		private final MetadataField<RFC3066LanguageCode> field;
		
		public LanguageProperty(MetadataField<RFC3066LanguageCode> field,
								boolean editable) {
			super(editable);
			this.field = field;
		}
		
		public String getName() {
			return LabelProvider.getLabel(field, LanguageUtil.getUILanguage());
		}
		
		public String getValue(AudioItem audioItem, Metadata metadata) {
			return LanguageUtil.getLocalizedLanguageName(AudioItemPropertiesModel.getLanguage(metadata, field));
		}

		@Override
		public void setValue(AudioItem audioItem, Metadata metadata, Locale newLocale) {
			AudioItemPropertiesModel.setLocaleValue(field, audioItem, metadata, newLocale);
		}
	}

}
