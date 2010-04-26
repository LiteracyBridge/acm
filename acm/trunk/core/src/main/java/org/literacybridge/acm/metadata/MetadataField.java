package org.literacybridge.acm.metadata;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public abstract class MetadataField<T> {
	private final String name;
	private Attribute<?>[] attributes;
	
	protected MetadataField(String name, Attribute<?>... attributes) {
		this.attributes = attributes;
		this.name = name;
	}
	
	void validateValue(T value) throws InvalidMetadataException {
		// do nothing by default
	}
	
	public Attribute<?>[] getAttributes() {
		return this.attributes;
	}
	
	public String getName() {
		return this.name;
	}
	
	protected abstract MetadataValue<T> deserialize(DataInput in) throws IOException; 
	protected abstract void serialize(DataOutput out, MetadataValue<T> value) throws IOException; 
}