package org.literacybridge.acm.gui.dialogs.audioItemPropertiesDialog;

import static org.literacybridge.acm.metadata.MetadataSpecification.DC_CONTRIBUTOR;
import static org.literacybridge.acm.metadata.MetadataSpecification.DC_CREATOR;
import static org.literacybridge.acm.metadata.MetadataSpecification.DC_DESCRIPTION;
import static org.literacybridge.acm.metadata.MetadataSpecification.DC_IDENTIFIER;
import static org.literacybridge.acm.metadata.MetadataSpecification.DC_LANGUAGE;
import static org.literacybridge.acm.metadata.MetadataSpecification.DC_PUBLISHER;
import static org.literacybridge.acm.metadata.MetadataSpecification.DC_RELATION;
import static org.literacybridge.acm.metadata.MetadataSpecification.DC_SOURCE;
import static org.literacybridge.acm.metadata.MetadataSpecification.DC_SUBJECT;
import static org.literacybridge.acm.metadata.MetadataSpecification.DC_TITLE;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.swing.table.AbstractTableModel;

import org.literacybridge.acm.config.Configuration;
import org.literacybridge.acm.content.AudioItem;
import org.literacybridge.acm.content.LocalizedAudioItem;
import org.literacybridge.acm.gui.resourcebundle.LabelProvider;
import org.literacybridge.acm.gui.util.language.LanguageUtil;
import org.literacybridge.acm.metadata.Metadata;
import org.literacybridge.acm.metadata.MetadataField;
import org.literacybridge.acm.metadata.MetadataSpecification;
import org.literacybridge.acm.metadata.MetadataValue;
import org.literacybridge.acm.metadata.RFC3066LanguageCode;
import org.literacybridge.acm.repository.AudioItemRepository.AudioFormat;

public class AudioItemPropertiesModel extends AbstractTableModel {
	private String[] columnNames = {LabelProvider.getLabel("AUDIO_ITEM_PROPERTIES_HEADER_PROPERTY", LanguageUtil.getUILanguage())
								  , LabelProvider.getLabel("AUDIO_ITEM_PROPERTIES_HEADER_VALUE", LanguageUtil.getUILanguage())};
	
	private final int TITLE_COL = 0;
	private final int VALUE_COL = 1;
	
	private Metadata metadata = null;
	private AudioItem audioItem = null;
	private List<AudioItemProperty> audioItemPropertiesObject = new ArrayList<AudioItemProperty>();
	private final boolean readOnly;
	
	public AudioItemPropertiesModel(AudioItem audioItem, Metadata metadata, boolean readOnly) {
		this.metadata = metadata;
		this.audioItem = audioItem;
		this.readOnly = readOnly;
		
		// TODO: make this list configurable
		// for now just remove some unneeded ones
		audioItemPropertiesObject.add(new AudioItemProperty.MetadataProperty(DC_TITLE, true));
		audioItemPropertiesObject.add(new AudioItemProperty.MetadataProperty(DC_CREATOR, true));
		audioItemPropertiesObject.add(new AudioItemProperty.MetadataProperty(DC_SUBJECT, true));
		audioItemPropertiesObject.add(new AudioItemProperty.MetadataProperty(DC_DESCRIPTION, true));
		audioItemPropertiesObject.add(new AudioItemProperty.MetadataProperty(DC_PUBLISHER, true));
		audioItemPropertiesObject.add(new AudioItemProperty.MetadataProperty(DC_CONTRIBUTOR, true));
		// audioItemPropertiesObject.add(new AudioItemProperty.MetadataProperty(DC_FORMAT));
		audioItemPropertiesObject.add(new AudioItemProperty.MetadataProperty(DC_IDENTIFIER, false));
		audioItemPropertiesObject.add(new AudioItemProperty.MetadataProperty(DC_RELATION, false));
		audioItemPropertiesObject.add(new AudioItemProperty.MetadataProperty(DC_SOURCE, false));
		// audioItemPropertiesObject.add(new AudioItemProperty.MetadataProperty(DC_COVERAGE));
		// audioItemPropertiesObject.add(new AudioItemProperty.MetadataProperty(DC_RIGHTS));		
		audioItemPropertiesObject.add(new AudioItemProperty.LanguageProperty(DC_LANGUAGE, true));
		audioItemPropertiesObject.add(new AudioItemProperty(false) {
			@Override public String getName() { 
				return "File name";
			}

			@Override public String getValue(AudioItem audioItem, Metadata metadata) {
				return Configuration.getConfiguration().getRepository().getAudioFile(audioItem, AudioFormat.A18).getName();
			}

			@Override
			public void setValue(AudioItem audioItem, Metadata metadata,
					Object newValue) {
				// not supported
			}			
		});
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
	
	public Locale getMetadataLocale() {
		return getLanguage(metadata, DC_LANGUAGE);
	}

	protected static Locale getLanguage(Metadata metadata, MetadataField<RFC3066LanguageCode> language) {
		// only shows first language
		for (MetadataValue<RFC3066LanguageCode> mv : metadata.getMetadataValues(language)) {
			RFC3066LanguageCode code = mv.getValue();
			return code.getLocale(); 		
		}
		
		return null;
	}
	
	boolean isLanguageRow(int row) {
		AudioItemProperty obj = audioItemPropertiesObject.get(row);
		if (obj instanceof AudioItemProperty.LanguageProperty) {
			return true;
		}
		
		return false;
	}
	
	@Override
	public Object getValueAt(int row, int col) {
		AudioItemProperty obj = audioItemPropertiesObject.get(row);
		
		switch (col) {
		case TITLE_COL:
			return obj.getName();
		case VALUE_COL:
			return obj.getValue(audioItem, metadata);
		default:
			break;
		}
			
		return LabelProvider.getLabel("ERROR", LanguageUtil.getUILanguage());
	}
	
	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		if (readOnly) {
			return false;
		}
		if (columnIndex != VALUE_COL) {
			return false;
		}
		
		AudioItemProperty obj = audioItemPropertiesObject.get(rowIndex);
		return obj.isEditable();
	}

	@Override
	public void setValueAt(Object aValue, int row, int col) {
		if (readOnly || aValue == null) {
			return;
		}
		
		String newValue = aValue.toString();
		AudioItemProperty<?> obj = audioItemPropertiesObject.get(row);
		
		if (obj instanceof AudioItemProperty.MetadataProperty) {
			((AudioItemProperty.MetadataProperty) obj).setValue(audioItem, metadata, newValue);
		} else if (obj instanceof AudioItemProperty.LanguageProperty) {
			Locale newLocale = (Locale) aValue;
			((AudioItemProperty.LanguageProperty) obj).setValue(audioItem, metadata, newLocale);
		}
		
		incrementRevision(metadata);
		metadata.commit();
	}
	
	protected static void setStringValue(MetadataField<String> field, Metadata metadata, String value) {
		metadata.setMetadataField(field, new MetadataValue<String>(value));
	}

	
	protected static void setLocaleValue(MetadataField<RFC3066LanguageCode> field, AudioItem audioItem, Metadata metadata, Locale newLocale) {
		Locale oldLocale = getLanguage(metadata, DC_LANGUAGE);
		LocalizedAudioItem localizedItem = audioItem.getLocalizedAudioItem(oldLocale);
		localizedItem.setLocale(newLocale);
		localizedItem.commit();
		audioItem.commit();
		metadata.setMetadataField(field, new MetadataValue<RFC3066LanguageCode>(new RFC3066LanguageCode(newLocale.getLanguage())));
		incrementRevision(metadata);
		metadata.commit();
	}
	
	private static void incrementRevision(Metadata metadata) {
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
