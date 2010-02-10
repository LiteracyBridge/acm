package main.java.org.literacybridge.acm.metadata;

public abstract class MetadataField<T> {
	MetadataField(Attribute<?>... attributes) {}
	
	boolean validateValue(T value) {
		return true;
	}
}