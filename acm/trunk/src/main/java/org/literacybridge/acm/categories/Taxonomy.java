package org.literacybridge.acm.categories;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.literacybridge.acm.content.LocalizedAudioLabel;

public class Taxonomy {
	private Map<Integer, Category> index;
	private Category root;
	private int maxCategoryID;
	
	public Taxonomy() {
		this.index = new HashMap<Integer, Category>();
		root = new Category(0);
		index.put(0, root);
	}
	
	public Category getRootCategory() {
		return this.root;
	}
	
	public int getMaxCategoryID() {
		return this.maxCategoryID;
	}
	
	public boolean addChild(Category parent, Category newChild) {
		int parentID = parent.getID();
		
		if (!index.containsKey(parentID)) {
			throw new IllegalArgumentException("Parent not found in taxonomy.");
		}
		if (index.containsKey(newChild.getID())) {
			return false;
		}
		int id = newChild.getID();
		this.maxCategoryID = (id > this.maxCategoryID) ? id : this.maxCategoryID;
		parent.addChild(newChild);
		this.index.put(id, newChild);
		return true;
	}
	
	public Category getCategory(int categoryID) {
		return this.index.get(categoryID);
	}
	
	public static class Category {
		private final int id;
		private Map<Locale, LocalizedAudioLabel> localizedNames;
		private Category parent;
		private List<Category> children;
		private Category[] childrenArray;
		
		public Category(int id) {
			this.id = id;
			this.localizedNames = new HashMap<Locale, LocalizedAudioLabel>();
		}
		
		public void setCategoryDescription(Locale locale, String name, String description) {
			this.localizedNames.put(locale, new LocalizedAudioLabel(name, description, null));
		}
		
		public int getID() {
			return this.id;
		}
		
		public LocalizedAudioLabel getCategoryName(Locale languageCode) {
			return this.localizedNames.get(languageCode);
		}
		
		public Category getParent() {
			return this.parent;
		}
		
		public List<Category> getChildren() {
			return this.children;
		}

		public Category[] getChildrenArray() {
			if (this.childrenArray == null) {
				this.childrenArray = new Category[this.children.size()];
				this.children.toArray(this.childrenArray);
			}
			
			return this.childrenArray;
		}

		
		public boolean hasChildren() {
			return this.children != null && !this.children.isEmpty();
		}
		
		private void addChild(Category category) {
			if (category.getParent() != null && category.getParent() != this) {
				throw new IllegalArgumentException("Inconsistent categories hierarchy.");
			}
			
			if (this.children == null) {
				this.children = new ArrayList<Category>();
			}
			
			this.children.add(category);
			category.parent = this;
			
			// invalidate cache
			this.childrenArray = null;
		}
	}

}
