package org.literacybridge.acm.store;

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
    private int numValues;

    public Metadata() {
        this.fields = new LinkedHashMap<MetadataField<?>, ListWrapper<?>>();
    }

    public int getNumberOfValues() {
        return numValues;
    }

    public int getNumberOfFields() {
        return this.fields.size();
    }

    public <F> void setMetadataField(MetadataField<F> field, MetadataValue<F> value) {
        if ((value == null) || (value.getValue() == null)) {
            return;
        }

        ListWrapper<F> fieldValues = new ListWrapper<F>(field, new LinkedList<MetadataValue<F>>());
        this.fields.put(field, fieldValues);

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
        return list != null ? list.list : null;
    }

    public void clear() {
        this.fields.clear();
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
