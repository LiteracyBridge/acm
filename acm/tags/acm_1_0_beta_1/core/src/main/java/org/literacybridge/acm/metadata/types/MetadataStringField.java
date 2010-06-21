package org.literacybridge.acm.metadata.types;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.literacybridge.acm.metadata.Attribute;
import org.literacybridge.acm.metadata.MetadataField;
import org.literacybridge.acm.metadata.MetadataValue;
import org.literacybridge.acm.utils.IOUtils;


public class MetadataStringField extends MetadataField<String> {
	public MetadataStringField(String name, Attribute<?>... attributes) {
		super(name, attributes);
	}

	@Override
	public MetadataValue<String> deserialize(DataInput in) throws IOException {
		String value = IOUtils.readUTF8(in);
		return new MetadataValue<String>(value);
	}

	@Override
	public void serialize(DataOutput out, MetadataValue<String> value) throws IOException {
		IOUtils.writeAsUTF8(out, value.getValue());
	}
}
