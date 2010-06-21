package org.literacybridge.acm.metadata;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.literacybridge.acm.db.Persistable;
import org.literacybridge.acm.db.PersistentLocale;
import org.literacybridge.acm.db.PersistentMetadata;

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
	public <F> void addMetadataField(MetadataField<F> field,
			MetadataValue<F> value) {
		if ((value == null) || (value.getValue() == null)) {
			return;
		}
		ListWrapper<F> fieldValues;
		if (this.fields.containsKey(field)) {
			fieldValues = (ListWrapper<F>) this.fields.get(field);
		} else {
			fieldValues = new ListWrapper<F>(field,
					new LinkedList<MetadataValue<F>>());
			this.fields.put(field, fieldValues);
		}

		value.setAttributes(field.getAttributes());

		fieldValues.list.add(value);

		numValues++;
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
		if (field == MetadataSpecification.DC_CONTRIBUTOR) {
			mMetadata.setDc_contributor(value.getValue().toString());
		} else if (field == MetadataSpecification.DC_COVERAGE) {
			mMetadata.setDc_coverage(value.getValue().toString());
		} else if (field == MetadataSpecification.DC_CREATOR) {
			mMetadata.setDc_creator(value.getValue().toString());
		} else if (field == MetadataSpecification.DC_DATE) {
			// mMetadata.setDc_date(value.getValue());
		} else if (field == MetadataSpecification.DC_DESCRIPTION) {
			mMetadata.setDc_description(value.getValue().toString());
		} else if (field == MetadataSpecification.DC_FORMAT) {
			mMetadata.setDc_format(value.getValue().toString());
		} else if (field == MetadataSpecification.DC_IDENTIFIER) {
			mMetadata.setDc_identifier(value.getValue().toString());
		} else if (field == MetadataSpecification.DC_PUBLISHER) {
			mMetadata.setDc_publisher(value.getValue().toString());
		} else if (field == MetadataSpecification.DC_RELATION) {
			mMetadata.setDc_relation(value.getValue().toString());
		} else if (field == MetadataSpecification.DC_RIGHTS) {
			mMetadata.setDc_rights(value.getValue().toString());
		} else if (field == MetadataSpecification.DC_SOURCE) {
			mMetadata.setDc_source(value.getValue().toString());
		} else if (field == MetadataSpecification.DC_SUBJECT) {
			mMetadata.setDc_subject(value.getValue().toString());
		} else if (field == MetadataSpecification.DC_TITLE) {
			mMetadata.setDc_title(value.getValue().toString());
		} else if (field == MetadataSpecification.DC_TYPE) {
			mMetadata.setDc_type(value.getValue().toString());
		} else if (field == MetadataSpecification.DTB_REVISION) {
			mMetadata.setDtb_revision(value.getValue().toString());
		} else if (field == MetadataSpecification.DTB_REVISION_DATE) {
			// mMetadata.setDtb_revision_date(value.getValue());
		} else if (field == MetadataSpecification.DTB_REVISION_DESCRIPTION) {
			mMetadata.setDtb_revision_description(value.getValue().toString());
		} else if (field == MetadataSpecification.LB_COPY_COUNT) {
			mMetadata.setLb_copy_count("default", 
					Integer.parseInt(value.getValue().toString()));
		} else if (field == MetadataSpecification.LB_PLAY_COUNT) {
			mMetadata.setLb_play_count("default", 
					Integer.parseInt(value.getValue().toString()));
		} else if (field == MetadataSpecification.LB_RATING) {
			mMetadata.setLb_rating(Short.valueOf(value.getValue().toString()));
		} else if (field == MetadataSpecification.DC_LANGUAGE) {
			// TODO ugly, needs to be implemented properly
			mMetadata.setPersistentLocale(mMetadata
					.getPersistentLocalizedAudioItem().getPersistentLocale());
		}
	}

	private void refreshFieldsFromPersistenceObject() {
		try {
			isRefreshing = true;
			this.fields.clear();
			addMetadataField(MetadataSpecification.DC_CONTRIBUTOR,
					new MetadataValue<String>(mMetadata.getDc_contributor()));
			addMetadataField(MetadataSpecification.DC_COVERAGE,
					new MetadataValue<String>(mMetadata.getDc_coverage()));
			addMetadataField(MetadataSpecification.DC_CREATOR,
					new MetadataValue<String>(mMetadata.getDc_creator()));
			// addMetadataField(MetadataSpecification.DC_DATE, new
			// MetadataValue<String>(mMetadata.getDc_date().toString()));
			addMetadataField(MetadataSpecification.DC_DESCRIPTION,
					new MetadataValue<String>(mMetadata.getDc_description()));
			addMetadataField(MetadataSpecification.DC_FORMAT,
					new MetadataValue<String>(mMetadata.getDc_format()));
			addMetadataField(MetadataSpecification.DC_IDENTIFIER,
					new MetadataValue<String>(mMetadata.getDc_identifier()));
			addMetadataField(MetadataSpecification.DC_PUBLISHER,
					new MetadataValue<String>(mMetadata.getDc_publisher()));
			addMetadataField(MetadataSpecification.DC_RELATION,
					new MetadataValue<String>(mMetadata.getDc_relation()));
			addMetadataField(MetadataSpecification.DC_RIGHTS,
					new MetadataValue<String>(mMetadata.getDc_rights()));
			addMetadataField(MetadataSpecification.DC_SOURCE,
					new MetadataValue<String>(mMetadata.getDc_source()));
			addMetadataField(MetadataSpecification.DC_SUBJECT,
					new MetadataValue<String>(mMetadata.getDc_subject()));
			addMetadataField(MetadataSpecification.DC_TITLE,
					new MetadataValue<String>(mMetadata.getDc_title()));
			addMetadataField(MetadataSpecification.DC_TYPE,
					new MetadataValue<String>(mMetadata.getDc_type()));
			addMetadataField(MetadataSpecification.DTB_REVISION,
					new MetadataValue<String>(mMetadata.getDtb_revision()));
			addMetadataField(MetadataSpecification.DTB_REVISION_DATE,
					new MetadataValue<String>(
							(mMetadata.getDtb_revision_date() == null) ? null
									: mMetadata.getDtb_revision_date()
											.toString()));
			addMetadataField(MetadataSpecification.DTB_REVISION_DESCRIPTION,
					new MetadataValue<String>(mMetadata
							.getDtb_revision_description()));
			addMetadataField(MetadataSpecification.LB_COPY_COUNT,
					mMetadata.getLb_copy_count() == 0 ? null : new MetadataValue<Integer>(mMetadata.getLb_copy_count()));
			addMetadataField(MetadataSpecification.LB_PLAY_COUNT,
					mMetadata.getLb_play_count() == 0 ? null : new MetadataValue<Integer>(mMetadata.getLb_play_count()));
			addMetadataField(MetadataSpecification.LB_RATING,
					new MetadataValue<Integer>(
							(mMetadata.getLb_rating() == null || mMetadata.getLb_rating().intValue() == 0) ? null
									: mMetadata.getLb_rating().intValue()));
			addMetadataField(MetadataSpecification.DC_LANGUAGE,
					new MetadataValue<RFC3066LanguageCode>(
							(mMetadata.getPersistentLocale() == null || mMetadata.getPersistentLocale().getLanguage() == null) 
							? null : new RFC3066LanguageCode(mMetadata.getPersistentLocale().getLanguage())));
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			isRefreshing = false;
		}
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
