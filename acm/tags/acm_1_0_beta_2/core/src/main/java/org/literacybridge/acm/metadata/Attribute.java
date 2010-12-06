package org.literacybridge.acm.metadata;

public class Attribute<T> {
	private boolean required;
	
	public Attribute() {
		this(false);
	}
	
	public Attribute(boolean required) {
		this.required = required;
	}
	
	public boolean isRequired() {
		return this.required;
	}
}