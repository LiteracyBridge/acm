package main.java.org.literacybridge.acm.metadata;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;


public class MetadataValue<T> implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private T value;
	private Map<Attribute<?>, Object> attributes;
	
	public MetadataValue(T value) {
		this.value = value;
	}
	
	public <A> void addAttribute(Attribute<A> attribute, A value) {
		if (this.attributes == null) {
			this.attributes = new LinkedHashMap<Attribute<?>, Object>();
		}
		
		this.attributes.put(attribute, value);
	}
	
	public T getValue() {
		return this.value;
	}
	
	@SuppressWarnings("unchecked")
	public <A> A getAttribute(Attribute<A> attributeName) {
		if (this.attributes == null) {
			return null;
		}

		return (A) this.attributes.get(attributeName);
	}
}
