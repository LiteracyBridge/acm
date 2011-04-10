package org.literacybridge.acm.ui.dialogs.audioItemPropertiesDialog;

import static org.literacybridge.acm.metadata.MetadataSpecification.DC_CONTRIBUTOR;
import static org.literacybridge.acm.metadata.MetadataSpecification.DC_COVERAGE;
import static org.literacybridge.acm.metadata.MetadataSpecification.DC_CREATOR;
import static org.literacybridge.acm.metadata.MetadataSpecification.DC_DESCRIPTION;
import static org.literacybridge.acm.metadata.MetadataSpecification.DC_FORMAT;
import static org.literacybridge.acm.metadata.MetadataSpecification.DC_PUBLISHER;
import static org.literacybridge.acm.metadata.MetadataSpecification.DC_RELATION;
import static org.literacybridge.acm.metadata.MetadataSpecification.DC_RIGHTS;
import static org.literacybridge.acm.metadata.MetadataSpecification.DC_SOURCE;
import static org.literacybridge.acm.metadata.MetadataSpecification.DC_SUBJECT;
import static org.literacybridge.acm.metadata.MetadataSpecification.DC_TITLE;
import static org.literacybridge.acm.metadata.MetadataSpecification.DC_LANGUAGE;

import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.table.AbstractTableModel;

import org.literacybridge.acm.content.AudioItem;
import org.literacybridge.acm.content.LocalizedAudioItem;
import org.literacybridge.acm.metadata.Metadata;
import org.literacybridge.acm.metadata.MetadataField;
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
		audioItemPropertiesObject.add(new AudioItemStringProperty(DC_TITLE));
		audioItemPropertiesObject.add(new AudioItemStringProperty(DC_CREATOR));
		audioItemPropertiesObject.add(new AudioItemStringProperty(DC_SUBJECT));
		audioItemPropertiesObject.add(new AudioItemStringProperty(DC_DESCRIPTION));
		audioItemPropertiesObject.add(new AudioItemStringProperty(DC_PUBLISHER));
		audioItemPropertiesObject.add(new AudioItemStringProperty(DC_CONTRIBUTOR));
		audioItemPropertiesObject.add(new AudioItemStringProperty(DC_FORMAT));
		audioItemPropertiesObject.add(new AudioItemStringProperty(DC_RELATION));
		audioItemPropertiesObject.add(new AudioItemStringProperty(DC_SOURCE));
		audioItemPropertiesObject.add(new AudioItemStringProperty(DC_COVERAGE));
		audioItemPropertiesObject.add(new AudioItemStringProperty(DC_RIGHTS));		
		audioItemPropertiesObject.add(new AudioItemRFC3066LanguageProperty(DC_LANGUAGE));				
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

	private Locale getLocaleForLanguageCode(String code) {
		if (Locale.ENGLISH.getLanguage().equals(code)) {
			return Locale.ENGLISH;
		} else if (Locale.GERMAN.getLanguage().equals(code)) {
			return Locale.GERMAN;
		} else if (Locale.FRENCH.getLanguage().equals(code)) {
			return Locale.FRENCH;
		}
		
		return Locale.ENGLISH;
	}
	
	private Locale getLanguage(MetadataField<RFC3066LanguageCode> language) {
		// only shows first language
		for (MetadataValue<RFC3066LanguageCode> mv : metadata.getMetadataValues(language)) {
			RFC3066LanguageCode code = mv.getValue();
			String languageCode = code.toString();
			return getLocaleForLanguageCode(languageCode); 		
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
		return (columnIndex == VALUE_COL);
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
		metadata.commit();
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
