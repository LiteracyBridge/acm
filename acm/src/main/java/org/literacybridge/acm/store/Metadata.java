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

  public <F> void setMetadataField(MetadataField<F> field,
      MetadataValue<F> value) {
    if ((value == null) || (value.getValue() == null)) {
      return;
    }

    this.fields.put(field, value);
  }

  /**
   * This breaks the strong typing, but is actually closer to what we really
   * have: metadata is string:value pairs.
   * @param field
   * @param stringValue
   * @param <F>
   */
  public <F> void setMetadataField(MetadataField<F> field, String stringValue)  {
      MetadataValue<F> value = new MetadataValue<F>((F)stringValue);
      setMetadataField(field, value);
  }

  public void validate() throws InvalidMetadataException {
    // TODO: when MetadataFields support things like required/optional, we can
    // implement validation here
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

  /**
   * Adds values from another Metadata object to this object. If the value
   * already exists, in this object, it will be overwritten.
   * 
   * @param otherMetadata
   */
  public void addValuesFrom(Metadata otherMetadata) {
    Iterator<MetadataField<?>> fieldsIterator = LBMetadataIDs.FieldToIDMap
        .keySet().iterator();
    while (fieldsIterator.hasNext()) {
      MetadataField<?> field = fieldsIterator.next();
      MetadataValue<?> value = otherMetadata.getMetadataValue(field);
      if (value != null) {
        this.fields.put(field, value);
      }
    }
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    Iterator<MetadataField<?>> fieldsIterator = LBMetadataIDs.FieldToIDMap
        .keySet().iterator();
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
