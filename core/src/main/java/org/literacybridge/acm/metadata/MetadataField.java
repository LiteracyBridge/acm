package org.literacybridge.acm.metadata;

public class MetadataField<T> {
	private Attribute<?>[] attributes;
	
	MetadataField(Attribute<?>... attributes) {
		this.attributes = attributes;
	}
	
	void validateValue(T value) throws InvalidMetadataException {
		// do nothing by default
	}
	
	public Attribute<?>[] getAttributes() {
		return this.attributes;
	}
}