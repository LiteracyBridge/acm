package org.literacybridge.acm.metadata.types;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.literacybridge.acm.metadata.Attribute;
import org.literacybridge.acm.metadata.MetadataField;
import org.literacybridge.acm.metadata.MetadataValue;

public class MetadataIntegerField extends MetadataField<Integer> {
	public MetadataIntegerField(Attribute<?>... attributes) {
		super(attributes);
	}
	
	@Override
	protected MetadataValue<Integer> deserialize(DataInput in)
			throws IOException {
		int value = in.readInt();
		return new MetadataValue<Integer>(value);
	}

	@Override
	protected void serialize(DataOutput out, MetadataValue<Integer> value)
			throws IOException {
		out.writeInt(value.getValue());
	}
}
