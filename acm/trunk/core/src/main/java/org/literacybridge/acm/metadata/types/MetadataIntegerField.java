package org.literacybridge.acm.metadata.types;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.literacybridge.acm.metadata.Attribute;
import org.literacybridge.acm.metadata.MetadataField;
import org.literacybridge.acm.metadata.MetadataValue;
import org.literacybridge.acm.utils.IOUtils;

public class MetadataIntegerField extends MetadataField<Integer> {
	public MetadataIntegerField(String name, Attribute<?>... attributes) {
		super(name, attributes);
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
