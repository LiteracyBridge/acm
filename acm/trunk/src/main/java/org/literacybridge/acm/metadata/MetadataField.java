package org.literacybridge.acm.metadata;

public class MetadataField<T> {
	MetadataField(Attribute<?>... attributes) {}
	
	boolean validateValue(T value) {
		return true;
	}
}