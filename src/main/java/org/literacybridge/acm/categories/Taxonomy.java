package main.java.org.literacybridge.acm.categories;

import java.util.List;
import java.util.Map;

public class Taxonomy {
	private Map<String, Category> taxonomy;
	
	public Category getCategory(String catID) {
		return this.taxonomy.get(catID);
	}
	
	public void addCategory(Category category) {
		// first find root of passed in category
		while (category.getParent() != null) {
			category = category.getParent();
		}
		
		doAddCategory(category);
	}
	
	private void doAddCategory(Category category) {
		// now add all in DFS order
		if (!this.taxonomy.containsKey(category.getID())) {
			// it's a new category
			if (category.getParent() != null) {
				Category parent = this.taxonomy.get(category.getParent().getID());
				assert parent != null; // can't be null, because we're starting at the root
				parent.addChild(category);
			}
			this.taxonomy.put(category.getID(), category);
		}
		
		if (category.hasChildren()) {
			List<Category> children = category.getChildren();
			for (Category child : children) {
				doAddCategory(child);
			}
		}
	}
}
