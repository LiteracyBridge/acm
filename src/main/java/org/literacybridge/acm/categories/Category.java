package main.java.org.literacybridge.acm.categories;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Category {
	private String id;
	private Map<String, String> localizedNames;
	private Category parent;
	private List<Category> children;
	
	
	public String getID() {
		return this.id;
	}
	
	// TODO: localize 
	public String getCategoryName(String languageCode) {
		return this.localizedNames.get(languageCode);
	}
	
	public Category getParent() {
		return this.parent;
	}
	
	public List<Category> getChildren() {
		return this.children;
	}
	
	public boolean hasChildren() {
		return this.children != null && !this.children.isEmpty();
	}
	
	public void addChild(Category category) {
		if (category.getParent() != null && category.getParent() != this) {
			throw new IllegalArgumentException("Inconsistent categories hierarchy.");
		}
		
		if (this.children == null) {
			this.children = new LinkedList<Category>();
		}
		
		this.children.add(category);
		category.parent = this;
	}
}
