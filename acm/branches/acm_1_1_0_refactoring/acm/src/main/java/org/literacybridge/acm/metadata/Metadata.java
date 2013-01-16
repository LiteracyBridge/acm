package org.literacybridge.acm.metadata;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.literacybridge.acm.db.Persistable;
import org.literacybridge.acm.db.PersistentMetadata;
import org.literacybridge.acm.metadata.types.MetadataStatisticsField;

public class Metadata implements Persistable {

	// java does runtime erasure of generic types - using this wrapper
	// we can return List<MetadataValue<F>> in getMetadataValues()
	// and also do type-safe value validation

	private class ListWrapper<T> {
		MetadataField<T> field;
		List<MetadataValue<T>> list;

		ListWrapper(MetadataField<T> field, List<MetadataValue<T>> list) {
			this.field = field;
			this.list = list;
		};

		void validateValues() throws InvalidMetadataException {
			for (MetadataValue<T> value : list) {
				field.validateValue(value.getValue());
			}
		}
	}

	private PersistentMetadata mMetadata;

	private boolean isRefreshing = false;

	private Map<MetadataField<?>, ListWrapper<?>> fields;
	private int numValues;

	public Metadata() {
		this.fields = new LinkedHashMap<MetadataField<?>, ListWrapper<?>>();
		mMetadata = new PersistentMetadata();
	}

	public int getNumberOfValues() {
		return numValues;
	}
	
	public int getNumberOfFields() {
		return this.fields.size();
	}

	public Metadata(PersistentMetadata metadata) {
		this.fields = new LinkedHashMap<MetadataField<?>, ListWrapper<?>>();
		mMetadata = metadata;
		refreshFieldsFromPersistenceObject();
	}

	public Integer getId() {
		return mMetadata.getId();
	}

	public Metadata commit() {
		mMetadata = mMetadata.<PersistentMetadata> commit();
		return this;
	}

	public void destroy() {
		mMetadata.destroy();
	}

	public Metadata refresh() {
		mMetadata = mMetadata.<PersistentMetadata> refresh();
		refreshFieldsFromPersistenceObject();
		return this;
	}

	@SuppressWarnings("unchecked")
	public <F> void setMetadataField(MetadataField<F> field, MetadataValue<F> value) {
		if ((value == null) || (value.getValue() == null)) {
			return;
		}
		
		ListWrapper<F> fieldValues = new ListWrapper<F>(field, new LinkedList<MetadataValue<F>>());
		this.fields.put(field, fieldValues);
		
		value.setAttributes(field.getAttributes());		
		fieldValues.list.add(value);		
		
		addMetadataToPersistenceObject(field, value);
	}
	
	public void validate() throws InvalidMetadataException {
		for (ListWrapper<?> entry : fields.values()) {
			entry.validateValues();
		}
	}

	public Iterator<MetadataField<?>> getFieldsIterator() {
		return this.fields.keySet().iterator();
	}

	@SuppressWarnings("unchecked")
	public <F> List<MetadataValue<F>> getMetadataValues(MetadataField<F> field) {
		ListWrapper<F> list = (ListWrapper<F>) this.fields.get(field);
		return list != null ? list.list : null;
	}

	//
	// Persistence Helper methods
	//

	private <F> void addMetadataToPersistenceObject(MetadataField<F> field,
			MetadataValue<F> value) {
		if (isRefreshing == true) {
			return;
		}
		if (field == MetadataSpecification.DC_IDENTIFIER) {
			mMetadata.setDc_identifier(value.getValue().toString());
		} else if (field == MetadataSpecification.DC_PUBLISHER) {
			mMetadata.setDc_publisher(value.getValue().toString());
		} else if (field == MetadataSpecification.DC_RELATION) {
			mMetadata.setDc_relation(value.getValue().toString());
		} else if (field == MetadataSpecification.DC_SOURCE) {
			mMetadata.setDc_source(value.getValue().toString());
		} else if (field == MetadataSpecification.DC_TITLE) {
			mMetadata.setDc_title(value.getValue().toString());
		} else if (field == MetadataSpecification.DTB_REVISION) {
			mMetadata.setDtb_revision(value.getValue().toString());
		} else if (field == MetadataSpecification.DC_LANGUAGE) {
			// TODO ugly, needs to be implemented properly
			mMetadata.setPersistentLocale(mMetadata
					.getPersistentLocalizedAudioItem().getPersistentLocale());
		} else if (field == MetadataSpecification.LB_DURATION) {
			mMetadata.setDuration(value.getValue().toString());
		} else if (field == MetadataSpecification.LB_MESSAGE_FORMAT) {
			mMetadata.setMessage_format(value.getValue().toString());
		} else if (field == MetadataSpecification.LB_TARGET_AUDIENCE) {
			mMetadata.setTarget_audience(value.getValue().toString());
		} else if (field == MetadataSpecification.LB_DATE_RECORDED) {
			mMetadata.setDate_recorded(value.getValue().toString());		
		} else if (field == MetadataSpecification.LB_KEYWORDS) {
			mMetadata.setKeywords(value.getValue().toString());
		} else if (field == MetadataSpecification.LB_TIMING) {
			mMetadata.setTiming(value.getValue().toString());
		} else if (field == MetadataSpecification.LB_PRIMARY_SPEAKER) {
			mMetadata.setPrimary_speaker(value.getValue().toString());
		} else if (field == MetadataSpecification.LB_GOAL) {
			mMetadata.setGoal(value.getValue().toString());
		} else if (field == MetadataSpecification.LB_ENGLISH_TRANSCRIPTION) {
			mMetadata.setEnglish_transcription(value.getValue().toString());
		} else if (field == MetadataSpecification.LB_NOTES) {
			mMetadata.setNotes(value.getValue().toString());
		}		
		}

	private void refreshFieldsFromPersistenceObject() {
		try {
			isRefreshing = true;
			this.fields.clear();
			setMetadataField(MetadataSpecification.DC_IDENTIFIER,
					new MetadataValue<String>(mMetadata.getDc_identifier()));
			setMetadataField(MetadataSpecification.DC_PUBLISHER,
					new MetadataValue<String>(mMetadata.getDc_publisher()));
			setMetadataField(MetadataSpecification.DC_RELATION,
					new MetadataValue<String>(mMetadata.getDc_relation()));
			setMetadataField(MetadataSpecification.DC_SOURCE,
					new MetadataValue<String>(mMetadata.getDc_source()));
			setMetadataField(MetadataSpecification.DC_TITLE,
					new MetadataValue<String>(mMetadata.getDc_title()));
			setMetadataField(MetadataSpecification.DTB_REVISION,
					new MetadataValue<String>(mMetadata.getDtb_revision()));
			setMetadataField(MetadataSpecification.LB_DATE_RECORDED,
					new MetadataValue<String>(mMetadata.getDate_recorded()));
			setStatisticsField(MetadataSpecification.LB_APPLY_COUNT);
			setStatisticsField(MetadataSpecification.LB_COMPLETION_COUNT);
			setStatisticsField(MetadataSpecification.LB_COPY_COUNT);
			setStatisticsField(MetadataSpecification.LB_OPEN_COUNT);
			setStatisticsField(MetadataSpecification.LB_SURVEY1_COUNT);
			setStatisticsField(MetadataSpecification.LB_NOHELP_COUNT);
			setMetadataField(MetadataSpecification.DC_LANGUAGE,
					new MetadataValue<RFC3066LanguageCode>(
							(mMetadata.getPersistentLocale() == null || mMetadata.getPersistentLocale().getLanguage() == null) 
							? null : new RFC3066LanguageCode(mMetadata.getPersistentLocale().getLanguage())));
			setMetadataField(MetadataSpecification.LB_DURATION,
					new MetadataValue<String>(mMetadata.getDuration()));
			setMetadataField(MetadataSpecification.LB_MESSAGE_FORMAT,
					new MetadataValue<String>(mMetadata.getMessage_format()));
			setMetadataField(MetadataSpecification.LB_TARGET_AUDIENCE,
					new MetadataValue<String>(mMetadata.getTarget_audience()));
			setMetadataField(MetadataSpecification.LB_KEYWORDS,
					new MetadataValue<String>(mMetadata.getKeywords()));
			setMetadataField(MetadataSpecification.LB_TIMING,
					new MetadataValue<String>(mMetadata.getTiming()));
			setMetadataField(MetadataSpecification.LB_PRIMARY_SPEAKER,
					new MetadataValue<String>(mMetadata.getPrimary_speaker()));
			setMetadataField(MetadataSpecification.LB_GOAL,
					new MetadataValue<String>(mMetadata.getGoal()));
			setMetadataField(MetadataSpecification.LB_ENGLISH_TRANSCRIPTION,
					new MetadataValue<String>(mMetadata.getEnglish_transcription()));
			setMetadataField(MetadataSpecification.LB_NOTES,
					new MetadataValue<String>(mMetadata.getNotes()));
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			isRefreshing = false;
		}
	}
	
	private final void setStatisticsField(MetadataStatisticsField statisticsField) {
		int count = mMetadata.getStatistic(statisticsField);
		setMetadataField(statisticsField, count == 0 ? null : new MetadataValue<Integer>(count));
	}
	
	public String toString() {
		StringBuilder builder = new StringBuilder();
		Iterator<MetadataField<?>> fieldsIterator = LBMetadataIDs.FieldToIDMap.keySet().iterator();
		while (fieldsIterator.hasNext()) {
			MetadataField<?> field = fieldsIterator.next();
			String valueList = getCommaSeparatedList(this, field);
			if (valueList != null) {
				builder.append(field.getName() + " = " + valueList + "\n");
			}
		}
		return builder.toString();
	}

	public static <T> String getCommaSeparatedList(Metadata metadata, MetadataField<T> field) {
		StringBuilder builder = new StringBuilder();
		List<MetadataValue<T>> fieldValues = metadata.getMetadataValues(field);
		if (fieldValues != null) {
			for (int i = 0; i < fieldValues.size(); i++) {
				builder.append(fieldValues.get(i).getValue());
				if (i != fieldValues.size() - 1) {
					builder.append(", ");
				}
			}
			return builder.toString();
		} else {
			return null;
		}
	}

	
}
