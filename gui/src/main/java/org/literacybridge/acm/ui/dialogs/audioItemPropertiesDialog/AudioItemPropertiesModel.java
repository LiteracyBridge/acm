package org.literacybridge.acm.ui.dialogs.audioItemPropertiesDialog;

import static org.literacybridge.acm.metadata.MetadataSpecification.DC_CONTRIBUTOR;
import static org.literacybridge.acm.metadata.MetadataSpecification.DC_CREATOR;
import static org.literacybridge.acm.metadata.MetadataSpecification.DC_DESCRIPTION;
import static org.literacybridge.acm.metadata.MetadataSpecification.DC_LANGUAGE;
import static org.literacybridge.acm.metadata.MetadataSpecification.DC_PUBLISHER;
import static org.literacybridge.acm.metadata.MetadataSpecification.DC_RELATION;
import static org.literacybridge.acm.metadata.MetadataSpecification.DC_SOURCE;
import static org.literacybridge.acm.metadata.MetadataSpecification.DC_SUBJECT;
import static org.literacybridge.acm.metadata.MetadataSpecification.DC_TITLE;
import static org.literacybridge.acm.metadata.MetadataSpecification.DC_IDENTIFIER;

import java.util.List;
import java.util.Locale;
import java.util.Vector;

import javax.swing.table.AbstractTableModel;

import org.literacybridge.acm.content.AudioItem;
import org.literacybridge.acm.content.LocalizedAudioItem;
import org.literacybridge.acm.metadata.Metadata;
import org.literacybridge.acm.metadata.MetadataField;
import org.literacybridge.acm.metadata.MetadataSpecification;
import org.literacybridge.acm.metadata.MetadataValue;
import org.literacybridge.acm.metadata.RFC3066LanguageCode;
import org.literacybridge.acm.resourcebundle.LabelProvider;
import org.literacybridge.acm.util.language.LanguageUtil;

public class AudioItemPropertiesModel extends AbstractTableModel {

	private String[] columnNames = {LabelProvider.getLabel("AUDIO_ITEM_PROPERTIES_HEADER_PROPERTY", LanguageUtil.getUILanguage())
								  , LabelProvider.getLabel("AUDIO_ITEM_PROPERTIES_HEADER_VALUE", LanguageUtil.getUILanguage())};
	
	private final int TITLE_COL = 0;
	private final int VALUE_COL = 1;
	
	private Metadata metadata = null;
	private AudioItem audioItem = null;
	private Vector<AudioItemPropertiesObject<?>> audioItemPropertiesObject = new Vector<AudioItemPropertiesObject<?>>();
	
	public AudioItemPropertiesModel(AudioItem audioItem, Metadata metadata) {
		this.metadata = metadata;
		this.audioItem = audioItem;
		
		// TODO: make this list configurable
		// for now just remove some unneeded ones
		audioItemPropertiesObject.add(new AudioItemStringProperty(DC_TITLE, true));
		audioItemPropertiesObject.add(new AudioItemStringProperty(DC_CREATOR, true));
		audioItemPropertiesObject.add(new AudioItemStringProperty(DC_SUBJECT, true));
		audioItemPropertiesObject.add(new AudioItemStringProperty(DC_DESCRIPTION, true));
		audioItemPropertiesObject.add(new AudioItemStringProperty(DC_PUBLISHER, true));
		audioItemPropertiesObject.add(new AudioItemStringProperty(DC_CONTRIBUTOR, true));
		// audioItemPropertiesObject.add(new AudioItemStringProperty(DC_FORMAT));
		audioItemPropertiesObject.add(new AudioItemStringProperty(DC_IDENTIFIER, false));
		audioItemPropertiesObject.add(new AudioItemStringProperty(DC_RELATION, false));
		audioItemPropertiesObject.add(new AudioItemStringProperty(DC_SOURCE, false));
		// audioItemPropertiesObject.add(new AudioItemStringProperty(DC_COVERAGE));
		// audioItemPropertiesObject.add(new AudioItemStringProperty(DC_RIGHTS));		
		audioItemPropertiesObject.add(new AudioItemRFC3066LanguageProperty(DC_LANGUAGE, true));				
	}
	
	@Override
	public String getColumnName(int arg0) {
		return columnNames[arg0];
	}

	@Override
	public int getColumnCount() {
		return columnNames.length;
	}

	@Override
	public int getRowCount() {
		return audioItemPropertiesObject.size();
	}

	private Locale getLanguage(MetadataField<RFC3066LanguageCode> language) {
		// only shows first language
		for (MetadataValue<RFC3066LanguageCode> mv : metadata.getMetadataValues(language)) {
			RFC3066LanguageCode code = mv.getValue();
			return code.getLocale(); 		
		}
		
		return null;
	}
	
	boolean isLanguageRow(int row) {
		AudioItemPropertiesObject<?> obj = audioItemPropertiesObject.get(row);
		if (obj instanceof AudioItemRFC3066LanguageProperty) {
			return true;
		}
		
		return false;
	}
	
	public Locale getMetadataLocale() {
		return getLanguage(DC_LANGUAGE);
	}
	
	@Override
	public Object getValueAt(int row, int col) {
		AudioItemPropertiesObject<?> obj = audioItemPropertiesObject.get(row);
		
		switch (col) {
		case TITLE_COL:
			return LabelProvider.getLabel(obj.getFieldID(), LanguageUtil.getUILanguage());
		case VALUE_COL:
			if (obj instanceof AudioItemStringProperty) {
				return Metadata.getCommaSeparatedList(metadata, obj.getFieldID());				
			} else if (obj instanceof AudioItemRFC3066LanguageProperty) {
				MetadataField<RFC3066LanguageCode> currentLanguage = ((AudioItemRFC3066LanguageProperty) obj).getFieldID();
				return LanguageUtil.getLocalizedLanguageName(getLanguage(currentLanguage));
			}
			
			return "";
		default:
			break;
		}
			
		return LabelProvider.getLabel("ERROR", LanguageUtil.getUILanguage());
	}
	
	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		if (columnIndex != VALUE_COL) {
			return false;
		}
		
		AudioItemPropertiesObject<?> obj = audioItemPropertiesObject.get(rowIndex);
		return obj.isEditable();
	}

	@Override
	public void setValueAt(Object aValue, int row, int col) {
		if (aValue == null) return;
		
		String newValue = aValue.toString();
		AudioItemPropertiesObject<?> obj = audioItemPropertiesObject.get(row);
		
		if (obj instanceof AudioItemStringProperty) {
			setStringValue(((AudioItemStringProperty) obj).getFieldID(), metadata, newValue);
		} else if (obj instanceof AudioItemRFC3066LanguageProperty) {
			Locale newLocale = (Locale) aValue;
			setLocaleValue(((AudioItemRFC3066LanguageProperty) obj).getFieldID(), metadata, newLocale);
		}
		
		incrementRevision(metadata);
		metadata.commit();
	}

	
	private void setStringValue(MetadataField<String> field, Metadata metadata, String value) {
		metadata.setMetadataField(field, new MetadataValue<String>(value));
	}	
	
	private void setLocaleValue(MetadataField<RFC3066LanguageCode> field, Metadata metadata, Locale newLocale) {
		Locale oldLocale = getMetadataLocale();
		LocalizedAudioItem localizedItem = audioItem.getLocalizedAudioItem(oldLocale);
		localizedItem.setLocale(newLocale);
		localizedItem.commit();
		audioItem.commit();
		metadata.setMetadataField(field, new MetadataValue<RFC3066LanguageCode>(new RFC3066LanguageCode(newLocale.getLanguage())));
		incrementRevision(metadata);
		metadata.commit();
	}
	
	private void incrementRevision(Metadata metadata) {
		List<MetadataValue<String>> revisions = metadata.getMetadataValues(MetadataSpecification.DTB_REVISION);
		if (revisions != null) {
			String revision = revisions.get(0).getValue();
			long rev = 0;
			if (revision != null && !revision.isEmpty()) {
				try {
					rev = Long.parseLong(revision);
				} catch (NumberFormatException e) {
					// use 0
				}
			}
			
			rev++;
			setStringValue(MetadataSpecification.DTB_REVISION, metadata, Long.toString(rev));
		}
	}
	
	
	// TODO Ask Michael: What is that csv list for a single metadata field all about?
//	private void setValues(MetadataField<String> field, Metadata metadata,
//			String commaSeparatedListValue) {
//		StringTokenizer t = new StringTokenizer(commaSeparatedListValue, ",");
//
//		List<MetadataValue<String>> existingValues = metadata
//				.getMetadataValues(field);
//		Iterator<MetadataValue<String>> it = null;
//
//		if (existingValues != null) {
//			it = existingValues.iterator();
//		}
//
//		while (t.hasMoreTokens()) {
//			String value = t.nextToken().trim();
//			if (it != null && it.hasNext()) {
//				it.next().setValue(value);
//			} else {
//				metadata.addMetadataField(field, new MetadataValue<String>(
//						value));
//			}
//		}
//	}
}
