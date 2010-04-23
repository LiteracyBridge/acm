package org.literacybridge.acm.metadata;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public abstract class MetadataField<T> {
	private Attribute<?>[] attributes;
	
	protected MetadataField(Attribute<?>... attributes) {
		this.attributes = attributes;
	}
	
	void validateValue(T value) throws InvalidMetadataException {
		// do nothing by default
	}
	
	public Attribute<?>[] getAttributes() {
		return this.attributes;
	}
	
	protected abstract MetadataValue<T> deserialize(DataInput in) throws IOException; 
	protected abstract void serialize(DataOutput out, MetadataValue<T> value) throws IOException; 
}