package org.literacybridge.acm.store;

import org.literacybridge.acm.utils.IOUtils;
import org.literacybridge.core.spec.LanguageLabelProvider;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class RFC3066LanguageCode extends LanguageLabelProvider.RFC3066LanguageCode {

    public RFC3066LanguageCode(String code) {
        super(code);
    }

    public static MetadataValue<RFC3066LanguageCode> deserialize(DataInput in)
      throws IOException {
    String value = IOUtils.readUTF8(in);
    return new MetadataValue<>(
        new RFC3066LanguageCode(value));
  }

  public static void serialize(DataOutput out,
      MetadataValue<RFC3066LanguageCode> value) throws IOException {
    IOUtils.writeAsUTF8(out, value.toString());
  }

}
