package org.literacybridge.acm.store;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.literacybridge.acm.utils.IOUtils;

public class MetadataIntegerField extends MetadataField<Integer> {
  public MetadataIntegerField(String name) {
    super(name);
  }

  @Override
  protected MetadataValue<Integer> deserialize(DataInput in)
      throws IOException {
    int value = IOUtils.readLittleEndian32(in);
    return new MetadataValue<Integer>(value);
  }

  @Override
  protected void serialize(DataOutput out, MetadataValue<Integer> value)
      throws IOException {
    IOUtils.writeLittleEndian32(out, value.getValue());
  }
}
