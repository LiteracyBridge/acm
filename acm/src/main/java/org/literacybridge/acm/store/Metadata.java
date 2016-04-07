package org.literacybridge.acm.store;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class Metadata {
    private Map<MetadataField<?>, MetadataValue<?>> fields;

    public Metadata() {
        this.fields = new LinkedHashMap<MetadataField<?>, MetadataValue<?>>();
    }

    public int getNumberOfFields() {
        return this.fields.size();
    }

    public <F> void setMetadataField(MetadataField<F> field, MetadataValue<F> value) {
        if ((value == null) || (value.getValue() == null)) {
            return;
        }

        this.fields.put(field, value);
    }

    public void validate() throws InvalidMetadataException {
        // TODO: when MetadataFields support things like required/optional, we can implement validation here
    }

    public Iterator<MetadataField<?>> getFieldsIterator() {
        return this.fields.keySet().iterator();
    }

    public boolean hasMetadataField(MetadataField<?> field) {
        return this.fields.containsKey(field);
    }

    @SuppressWarnings("unchecked")
    public <F> MetadataValue<F> getMetadataValue(MetadataField<F> field) {
        return (MetadataValue<F>) this.fields.get(field);
    }

    public void clear() {
        this.fields.clear();
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        Iterator<MetadataField<?>> fieldsIterator = LBMetadataIDs.FieldToIDMap.keySet().iterator();
        while (fieldsIterator.hasNext()) {
            MetadataField<?> field = fieldsIterator.next();
            MetadataValue<?> value = getMetadataValue(field);
            if (value != null) {
                builder.append(field.getName() + " = " + value.getValue() + "\n");
            }
        }
        return builder.toString();
    }
}
