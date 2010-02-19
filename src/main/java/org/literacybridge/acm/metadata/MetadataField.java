package org.literacybridge.acm.metadata;

public class MetadataField<T> {
	private Attribute<?>[] attributes;
	
	MetadataField(Attribute<?>... attributes) {
		this.attributes = attributes;
	}
	
	boolean validateValue(T value) {
		return true;
	}
	
	public Attribute<?>[] getAttributes() {
		return this.attributes;
	}
}