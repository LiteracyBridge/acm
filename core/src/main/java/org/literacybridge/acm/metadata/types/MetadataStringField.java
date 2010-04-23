package org.literacybridge.acm.metadata.types;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.literacybridge.acm.metadata.Attribute;
import org.literacybridge.acm.metadata.MetadataField;
import org.literacybridge.acm.metadata.MetadataValue;


public class MetadataStringField extends MetadataField<String> {

	public MetadataStringField(Attribute<?>... attributes) {
		super(attributes);
	}

	@Override
	public MetadataValue<String> deserialize(DataInput in) throws IOException {
		String value = in.readUTF();
		return new MetadataValue<String>(value);
	}

	@Override
	public void serialize(DataOutput out, MetadataValue<String> value) throws IOException {
		out.writeUTF(value.getValue());
	}
}
