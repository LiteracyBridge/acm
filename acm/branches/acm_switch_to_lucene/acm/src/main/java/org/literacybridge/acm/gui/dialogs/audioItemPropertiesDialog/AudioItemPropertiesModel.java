package org.literacybridge.acm.gui.dialogs.audioItemPropertiesDialog;

import static org.literacybridge.acm.metadata.MetadataSpecification.DC_IDENTIFIER;
import static org.literacybridge.acm.metadata.MetadataSpecification.DC_LANGUAGE;
import static org.literacybridge.acm.metadata.MetadataSpecification.DC_PUBLISHER;
import static org.literacybridge.acm.metadata.MetadataSpecification.DC_RELATION;
import static org.literacybridge.acm.metadata.MetadataSpecification.DC_SOURCE;
import static org.literacybridge.acm.metadata.MetadataSpecification.DC_TITLE;
import static org.literacybridge.acm.metadata.MetadataSpecification.LB_BENEFICIARY;
import static org.literacybridge.acm.metadata.MetadataSpecification.LB_DATE_RECORDED;
import static org.literacybridge.acm.metadata.MetadataSpecification.LB_DURATION;
import static org.literacybridge.acm.metadata.MetadataSpecification.LB_ENGLISH_TRANSCRIPTION;
import static org.literacybridge.acm.metadata.MetadataSpecification.LB_GOAL;
import static org.literacybridge.acm.metadata.MetadataSpecification.LB_KEYWORDS;
import static org.literacybridge.acm.metadata.MetadataSpecification.LB_MESSAGE_FORMAT;
import static org.literacybridge.acm.metadata.MetadataSpecification.LB_NOTES;
import static org.literacybridge.acm.metadata.MetadataSpecification.LB_PRIMARY_SPEAKER;
import static org.literacybridge.acm.metadata.MetadataSpecification.LB_STATUS;
import static org.literacybridge.acm.metadata.MetadataSpecification.LB_TARGET_AUDIENCE;
import static org.literacybridge.acm.metadata.MetadataSpecification.LB_TIMING;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.swing.table.AbstractTableModel;

import org.apache.commons.lang.StringUtils;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.content.AudioItem;
import org.literacybridge.acm.gui.resourcebundle.LabelProvider;
import org.literacybridge.acm.gui.util.UIUtils;
import org.literacybridge.acm.gui.util.language.LanguageUtil;
import org.literacybridge.acm.metadata.Metadata;
import org.literacybridge.acm.metadata.MetadataField;
import org.literacybridge.acm.metadata.MetadataSpecification;
import org.literacybridge.acm.metadata.MetadataValue;
import org.literacybridge.acm.metadata.RFC3066LanguageCode;
import org.literacybridge.acm.repository.AudioItemRepository.AudioFormat;

import com.google.common.collect.Maps;

public class AudioItemPropertiesModel extends AbstractTableModel {
	public static final String NO_LONGER_USED = "NO LONGER USED";
	static final String STATUS_NAME = "Status";
	public static final String[] STATUS_VALUES = {"Current", NO_LONGER_USED};
	static final Map<String, Integer> STATUS_VALUES_MAP = Maps.newHashMap();
	static {
		for (int i = 0; i < STATUS_VALUES.length; i++) {
			STATUS_VALUES_MAP.put(STATUS_VALUES[i], i);
		}
	}
	
	private String[] columnNames = {LabelProvider.getLabel("AUDIO_ITEM_PROPERTIES_HEADER_PROPERTY", LanguageUtil.getUILanguage())
								  , LabelProvider.getLabel("AUDIO_ITEM_PROPERTIES_HEADER_VALUE", LanguageUtil.getUILanguage()),
								  "" // edit column doesn't have a title
								  };
	
	static final int TITLE_COL = 0;
	static final int VALUE_COL = 1;
	static final int EDIT_COL = 2;
	
	private AudioItem audioItem = null;
	private List<AudioItemProperty> audioItemPropertiesObject = new ArrayList<AudioItemProperty>();
	private final boolean readOnly;
	
	public AudioItemPropertiesModel(AudioItem audioItem, boolean readOnly) {
		this.audioItem = audioItem;
		this.readOnly = readOnly;
		
		// TODO: make this list configurable
		audioItemPropertiesObject.add(new AudioItemProperty(true) {
			@Override public String getName() { 
				return STATUS_NAME;
			}

			@Override public String getValue(AudioItem audioItem) {
				List<MetadataValue<Integer>> values = audioItem.getMetadata().getMetadataValues(LB_STATUS);
				if (values == null || values.isEmpty()) {
					return "Current";
				}
				
				return STATUS_VALUES[values.get(0).getValue()];
			}

			@Override
			public void setValue(AudioItem audioItem, Object newValue) {
				audioItem.getMetadata().setMetadataField(MetadataSpecification.LB_STATUS, 
						new MetadataValue<Integer>(STATUS_VALUES_MAP.get(newValue.toString())));
			}			
		});	
		audioItemPropertiesObject.add(new AudioItemProperty.MetadataProperty(DC_TITLE, true));
		// TODO: calculate duration of audio item
		audioItemPropertiesObject.add(new AudioItemProperty.MetadataProperty(LB_DURATION, false));
		audioItemPropertiesObject.add(new AudioItemProperty(false, true) {
			@Override public String getName() { 
				return "Categories";
			}

			@Override public String getValue(AudioItem audioItem) {
				return UIUtils.getCategoryListAsString(audioItem);
			}

			@Override
			public void setValue(AudioItem audioItem, Object newValue) {
				// not supported
			}			
		});
		audioItemPropertiesObject.add(new AudioItemProperty(false, false) {
			@Override public String getName() { 
				return "Playlists";
			}

			@Override public String getValue(AudioItem audioItem) {
				return UIUtils.getPlaylistAsString(audioItem);
			}

			@Override
			public void setValue(AudioItem audioItem, Object newValue) {
				// not supported
			}			
		});
		
		audioItemPropertiesObject.add(new AudioItemProperty.LanguageProperty(DC_LANGUAGE, true));

		// TODO: add combo boxes
		audioItemPropertiesObject.add(new AudioItemProperty.MetadataProperty(LB_MESSAGE_FORMAT, true));
		audioItemPropertiesObject.add(new AudioItemProperty.MetadataProperty(LB_TARGET_AUDIENCE, true));
		// TODO: add calendar picker
		audioItemPropertiesObject.add(new AudioItemProperty.MetadataProperty(LB_DATE_RECORDED, true));
		audioItemPropertiesObject.add(new AudioItemProperty.MetadataProperty(LB_KEYWORDS, true));
		audioItemPropertiesObject.add(new AudioItemProperty.MetadataProperty(LB_TIMING, true));
		audioItemPropertiesObject.add(new AudioItemProperty.MetadataProperty(DC_SOURCE, true));
		audioItemPropertiesObject.add(new AudioItemProperty.MetadataProperty(LB_PRIMARY_SPEAKER, true));
		audioItemPropertiesObject.add(new AudioItemProperty.MetadataProperty(LB_GOAL, true));
		audioItemPropertiesObject.add(new AudioItemProperty.MetadataProperty(DC_PUBLISHER, false));
		audioItemPropertiesObject.add(new AudioItemProperty.MetadataProperty(DC_IDENTIFIER, false));
		audioItemPropertiesObject.add(new AudioItemProperty.MetadataProperty(DC_RELATION, true));
		audioItemPropertiesObject.add(new AudioItemProperty(false) {
			@Override public String getName() { 
				return "Related Message Title";
			}

			@Override public String getValue(AudioItem audioItem) {
				List<MetadataValue<String>> values = audioItem.getMetadata().getMetadataValues(DC_RELATION);
				if (values != null && !values.isEmpty()) {
					String id = values.get(0).getValue();
					if (!StringUtils.isEmpty(id)) {
						AudioItem item = AudioItem.getFromDatabase(id);
						List<MetadataValue<String>> values1 = item.getMetadata().getMetadataValues(DC_TITLE);
						if (values1 != null && !values1.isEmpty()) {
							return values1.get(0).getValue();
						}
					}
				}
				
				return null;
			}

			@Override
			public void setValue(AudioItem audioItem, Object newValue) {
				// not supported
			}			
		});

		audioItemPropertiesObject.add(new AudioItemProperty(false) {
			@Override public String getName() { 
				return "File name";
			}

			@Override public String getValue(AudioItem audioItem) {
				File file = ACMConfiguration.getCurrentDB().getRepository().getAudioFile(audioItem, AudioFormat.A18);
				return file != null ? file.getName() : null;
			}

			@Override
			public void setValue(AudioItem audioItem, Object newValue) {
				// not supported
			}			
		});
		
		// TOOD: long text fields
		audioItemPropertiesObject.add(new AudioItemProperty.MetadataProperty(LB_ENGLISH_TRANSCRIPTION, true));
		audioItemPropertiesObject.add(new AudioItemProperty.MetadataProperty(LB_NOTES, true));
		audioItemPropertiesObject.add(new AudioItemProperty.MetadataProperty(LB_BENEFICIARY, true));
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
	
	public AudioItem getSelectedAudioItem() {
		return audioItem;
	}
	
	public boolean showEditIcon(int row) {
		return audioItemPropertiesObject.get(row).showEditIcon();
	}
	
	public Locale getMetadataLocale() {
		return getLanguage(audioItem, DC_LANGUAGE);
	}

	protected static Locale getLanguage(AudioItem audioItem, MetadataField<RFC3066LanguageCode> language) {
		// only shows first language
		for (MetadataValue<RFC3066LanguageCode> mv : audioItem.getMetadata().getMetadataValues(language)) {
			RFC3066LanguageCode code = mv.getValue();
			return code.getLocale(); 		
		}
		
		return null;
	}
	
	public AudioItemProperty getAudioItemProperty(int row) {
		return audioItemPropertiesObject.get(row);
	}
	
	boolean highlightRow(int row) {
		if (row == 0) {
			AudioItemProperty obj = audioItemPropertiesObject.get(row);
			String value = obj.getValue(audioItem);
			return !value.equals(STATUS_VALUES[0]);
		}
		
		return false;
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
			return obj.getValue(audioItem);
		case EDIT_COL:
			return "";
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
		return obj.isCellEditable();
	}

	@Override
	public void setValueAt(Object aValue, int row, int col) {
		if (readOnly || aValue == null) {
			return;
		}
		
		String newValue = aValue.toString();
		AudioItemProperty<?> obj = audioItemPropertiesObject.get(row);
		
		
		if (obj instanceof AudioItemProperty.LanguageProperty) {
			Locale newLocale = (Locale) aValue;
			((AudioItemProperty.LanguageProperty) obj).setValue(audioItem, newLocale);
		} else if (obj instanceof AudioItemProperty) {
			((AudioItemProperty) obj).setValue(audioItem, newValue);
		}
		
		Metadata metadata = audioItem.getMetadata();
		incrementRevision(metadata);
		metadata.commit();
		audioItem.commit();
	}
	
	protected static void setStringValue(MetadataField<String> field, Metadata metadata, String value) {
		metadata.setMetadataField(field, new MetadataValue<String>(value));
	}

	
	protected static void setLocaleValue(MetadataField<RFC3066LanguageCode> field, AudioItem audioItem, Locale newLocale) {
		Metadata metadata = audioItem.getMetadata();
		metadata.setMetadataField(field, new MetadataValue<RFC3066LanguageCode>(new RFC3066LanguageCode(newLocale.getLanguage())));
		metadata.commit();
		audioItem.commit();
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