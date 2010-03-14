package org.literacybridge.acm.metadata;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Metadata {
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
	
	private Map<MetadataField<?>, ListWrapper<?>> fields;
	
	public Metadata() {
		this.fields = new LinkedHashMap<MetadataField<?>, ListWrapper<?>>();
	}
	
	@SuppressWarnings("unchecked")
	public <F> void addMetadataField(MetadataField<F> field, MetadataValue<F> value) {
		ListWrapper<F> fieldValues;
		if (this.fields.containsKey(field)) {
			fieldValues = (ListWrapper<F>) this.fields.get(field);
		} else {
			fieldValues = new ListWrapper<F>(field, new LinkedList<MetadataValue<F>>());
			this.fields.put(field, fieldValues);
		}
		
		value.setAttributes(field.getAttributes());

		fieldValues.list.add(value);
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
		return list.list;
	}
}
