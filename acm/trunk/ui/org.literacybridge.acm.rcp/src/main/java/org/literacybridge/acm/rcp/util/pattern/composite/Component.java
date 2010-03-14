package org.literacybridge.acm.rcp.util.pattern.composite;

public abstract class Component {
	
	private Object object		= null;
	private Component parent	= null;
	
	public Component(Object o) {
		this.object = o;
	}
	
	public Object getObject() {
		return object;
	}
	
	public boolean hasObject() {
		return object != null;
	}
	
	public void setParent(Component parent) {
		this.parent = parent;
	}
	
	public Component getParent() {
		return parent;
	}
}
