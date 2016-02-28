package org.literacybridge.acm.store;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public abstract class MetadataField<T> {
  private final String name;

  protected MetadataField(String name) {
    this.name = name;
  }

  void validateValue(T value) throws InvalidMetadataException {
    // do nothing by default
  }

  public String getName() {
    return this.name;
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof MetadataField)) {
      return false;
    }

    return ((MetadataField<?>) other).name.equals(name);
  }

  protected abstract MetadataValue<T> deserialize(DataInput in)
      throws IOException;

  protected abstract void serialize(DataOutput out, MetadataValue<T> value)
      throws IOException;
}
