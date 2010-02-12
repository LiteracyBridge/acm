package org.literacybridge.acm.metadata;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


public class MetadataSet {
	// java does runtime erasure of generic types - using this wrapper we can return List<MetadataValue<F>> in getMetadataValues()
	private class ListWrapper<T> {
		List<MetadataValue<T>> list;
		ListWrapper(List<MetadataValue<T>> list) {this.list = list;};
	}
	
	private Map<MetadataField<?>, ListWrapper<?>> fields;
	
	public MetadataSet() {
		this.fields = new LinkedHashMap<MetadataField<?>, ListWrapper<?>>();
	}
	
	@SuppressWarnings("unchecked")
	public <F> void addMetadataField(MetadataField<F> field, MetadataValue<F> value) {
		ListWrapper<F> fieldValues;
		if (this.fields.containsKey(field)) {
			fieldValues = (ListWrapper<F>) this.fields.get(field);
		} else {
			fieldValues = new ListWrapper<F>(new LinkedList<MetadataValue<F>>());
			this.fields.put(field, fieldValues);
		}
		
		fieldValues.list.add(value);
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
