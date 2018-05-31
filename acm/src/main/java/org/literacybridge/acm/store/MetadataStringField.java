package org.literacybridge.acm.store;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.literacybridge.acm.utils.IOUtils;

public class MetadataStringField extends MetadataField<String> {
  public MetadataStringField(String name) {
    super(name);
  }

  @Override
  public MetadataValue<String> deserialize(DataInput in) throws IOException {
    String value = IOUtils.readUTF8(in);
    return new MetadataValue<>(value);
  }

  @Override
  public void serialize(DataOutput out, MetadataValue<String> value)
      throws IOException {
    IOUtils.writeAsUTF8(out, value.getValue());
  }
}
