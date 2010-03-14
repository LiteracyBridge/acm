package org.literacybridge.acm.rcp.util.pattern.composite;

import java.util.Vector;

public class Composite extends Component {
	
	private Vector children = null;
	
	public Composite(Object owner) {
		super(owner);
		children = new Vector();
	}
	
	public void addChild(Component c) {
		children.add(c);
		c.setParent(this);
	}
	
	public void removeChild(Component c) {
		children.remove(c);
	}
	
	public boolean hasChildren() {
		return children.size() > 0;
	}
	
	public Object[] getChildren() {
		return children.toArray();
	}
}
