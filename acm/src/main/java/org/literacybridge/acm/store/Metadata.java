package org.literacybridge.acm.store;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.literacybridge.acm.store.LBMetadataIDs.FieldToIDMap;

public class Metadata {
  private Map<MetadataField<?>, MetadataValue<?>> fields;

  public Metadata() {
    this.fields = new LinkedHashMap<>();
  }

  public Set<Map.Entry<MetadataField<?>, MetadataValue<?>>> entrySet() {
    return fields.entrySet();
  }

  public Set<MetadataField<?>> keySet() {
    return fields.keySet();
  }

  public int size() {
    return this.fields.size();
  }

  void clear() {
    this.fields.clear();
  }

  public void validate() throws InvalidMetadataException {
  }

  Iterator<MetadataField<?>> getFieldsIterator() {
    return this.fields.keySet().iterator();
  }

  public boolean containsField(MetadataField<?> field) {
    return this.fields.containsKey(field);
  }

  public <F> void putMetadataField(MetadataField<F> field, MetadataValue<F> value) {
    if ((value == null) || (value.getValue() == null)) {
      return;
    }

    this.fields.put(field, value);
  }

  @SuppressWarnings("unchecked")
  public <F> MetadataValue<F> getMetadataValue(MetadataField<F> field) {
    return (MetadataValue<F>) this.fields.get(field);
  }

  /**
   * Gets a metadata value as a string.
   * @param field The metadata field to retrieve.
   * @param <F> The Type of the metadata field.
   * @return The value as a string.
   */
  public <F> String get(MetadataField<F> field) {
    MetadataValue<F> fieldValue = getMetadataValue(field);
    if (fieldValue != null) {
      F value = fieldValue.getValue();
      if (value != null)
        return value.toString();
    }
    return null;
  }

  /**
   * Gets a metadata value from its name.
   * @param name of the metadata field.
   * @return the value as a string.
   */
  public String get(String name) {
    MetadataField<?> field = LBMetadataIDs.NameToFieldMap.get(name);
    return get(field);
  }

  /**
   * Stores any metadata value from a String.
   * @param field The metadata field to be set.
   * @param value The String value to be set.
   * @param <F> The Type of the metadata field.
   */
  @SuppressWarnings("unchecked")
  public <F, T> void put(MetadataField<F> field, T value) {
    try {

      // Because Java generic are by erasure, we only need the non-generic constructor. There
      // only IS the non-generic constructor. Yes, this is totally unchecked.
      Class valueClass = LBMetadataIDs.FieldToValueClassMap.get(field);
      Constructor valueCtor = valueClass.getConstructor(String.class);
      String stringValue = value.toString();
      MetadataValue mdValue = new MetadataValue(valueCtor.newInstance(stringValue));
      this.fields.put(field, mdValue);
    } catch (Exception e) {
      // Turn into IllegalStateException. This really can't happen, because all fields
      // are defined and construction from String is always supported.
      throw new IllegalStateException((e));
    }
  }

  /**
   * Stores any metadata field via its name.
   * @param name of the field to be set.
   * @param value The string value to set.
   */
  public <T> void put(String name, T value) {
    MetadataField<?> field = LBMetadataIDs.NameToFieldMap.get(name);
    put(field, value.toString());
  }

  /**
   * Adds values from another Metadata object to this object. If the value
   * already exists, in this object, it will be overwritten.
   * 
   * @param otherMetadata Another metadata object from which to get values.
   * @param ignoring Optional list of metadata field types to be ignored.
   */
  public void addValuesFromOtherWithExclusions(Metadata otherMetadata, MetadataField<?>... ignoring) {
    addValuesFromOtherWithExclusions(otherMetadata, true, ignoring);
  }
  public void addValuesFromOtherWithExclusions(Metadata otherMetadata, boolean replaceExisting, MetadataField<?>... ignoring) {
    Set<String> ignored = Arrays.stream(ignoring)
                                .map(MetadataField::getName)
                                .collect(Collectors.toSet());
    for (MetadataField<?> field : otherMetadata.fields.keySet()) {
      if (!ignored.contains(field.getName()) && (replaceExisting || !this.containsField(field))) {
        this.fields.put(field, otherMetadata.fields.get(field));
      }
    }
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    for (MetadataField<?> field : FieldToIDMap.keySet()) {
      MetadataValue<?> value = getMetadataValue(field);
      if (value != null) {
        builder.append(field.getName()).append(" = ").append(value.getValue()).append("\n");
      }
    }
    return builder.toString();
  }
}
