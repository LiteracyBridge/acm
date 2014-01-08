package org.literacybridge.acm.categories;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.literacybridge.acm.db.Persistable;
import org.literacybridge.acm.db.PersistentCategory;
import org.literacybridge.acm.db.PersistentLocale;

public class Taxonomy implements Persistable {

	private static String rootUUID = DefaultLiteracyBridgeTaxonomy.LB_TAXONOMY_UID;

	private Category mRootCategory;

	private static Taxonomy taxonomy;
	
	public Taxonomy() {
		String title = "root";
		String desc = "root node";
		PersistentCategory root = new PersistentCategory(title, desc, rootUUID);
		mRootCategory = new Category(root);
	}

	public Taxonomy(String uuid) {
		PersistentCategory root = PersistentCategory.getFromDatabase(uuid);
		if (root == null) {
			String title = "root";
			String desc = "root node";
			root = new PersistentCategory(title, desc, uuid);
			mRootCategory = new Category(root);
		}
	}
	
	public static Taxonomy getTaxonomy() {
		return getTaxonomy(DefaultLiteracyBridgeTaxonomy.LB_TAXONOMY_UID);
	}
	
	private static Taxonomy getTaxonomy(String uuid) {
		if (taxonomy != null) {
			return taxonomy;
		}
		
		taxonomy = new Taxonomy();
		
		PersistentCategory root = PersistentCategory.getFromDatabase(uuid);
		DefaultLiteracyBridgeTaxonomy.TaxonomyRevision latestRevision = DefaultLiteracyBridgeTaxonomy.loadLatestTaxonomy();

		if (root == null) {	
			String title = "root";
			String desc = "root node";
			root = new PersistentCategory(title, desc, rootUUID);
			taxonomy = createNewTaxonomy(latestRevision, root, null);
			taxonomy.commit();
		} else if (root.getRevision() < latestRevision.revision) {
			updateTaxonomy(root, latestRevision);
			taxonomy.commit();
			DefaultLiteracyBridgeTaxonomy.print(taxonomy.mRootCategory);
		} else {
			taxonomy.mRootCategory = new Category(root);
		}

		return taxonomy;
	}
	
	private static Taxonomy createNewTaxonomy(DefaultLiteracyBridgeTaxonomy.TaxonomyRevision revision, PersistentCategory existingRoot, final Map<String, PersistentCategory> existingCategories) {
		Taxonomy taxonomy = new Taxonomy();
		existingRoot.setRevision(revision.revision);
		existingRoot.setOrder(0);
		taxonomy.mRootCategory = new Category(existingRoot);
		
		revision.createTaxonomy(taxonomy, existingCategories);
		return taxonomy;
	}
	
	private static void updateTaxonomy(PersistentCategory existingRoot, DefaultLiteracyBridgeTaxonomy.TaxonomyRevision latestRevision) {
		System.out.println("Updating taxonomy");
		
		final Map<String, PersistentCategory> existingCategories = new HashMap<String, PersistentCategory>();
		traverse(null, existingRoot, new Function() {
			@Override public void apply(PersistentCategory parent, PersistentCategory root) {
				existingCategories.put(root.getUuid(), root);
			}			
		});
		
		existingRoot.clearPersistentChildCategories();
		for (PersistentCategory cat : existingCategories.values()) {
			cat.clearPersistentChildCategories();
			cat.setPersistentParentCategory(null);
			cat.commit();
		}
		
		taxonomy = createNewTaxonomy(latestRevision, existingRoot, existingCategories);		
	}
	
	private static interface Function {
		public void apply(PersistentCategory parent, PersistentCategory root);
	}
	
	private static void traverse(PersistentCategory parent, PersistentCategory root, Function function) {
		function.apply(parent, root);
		for (PersistentCategory child : root.getPersistentChildCategoryList()) {
			traverse(root, child, function);
		}
	}

	public Category getRootCategory() {
		return mRootCategory;
	}

	public List<Category> getCategoryList() {
		return mRootCategory.getChildren();
	}

	public boolean isRoot(Category category) {
		return category == this.mRootCategory;
	}

	public boolean addChild(Category parent, Category newChild) {
		parent.addChild(newChild);
		return true;
	}

	/**
	 * Returns the facet count for all categories that are stored
	 * in the database.
	 * 
	 * Key: database id (getId())
	 * Value: count value
	 * 
	 * Note: Returns '0' for unassigned categories.
	 */
	public static Map<Integer, Integer> getFacetCounts(String filter, List<PersistentCategory> categories, List<PersistentLocale> locales) {
		return PersistentCategory.getFacetCounts(filter, categories, locales);
	}
	
	public Integer getId() {
		return mRootCategory.getId();
	}

	public Taxonomy commit() {
		mRootCategory.commit();
		return this;
	}

	public void destroy() {
		mRootCategory.destroy();
	}

	public Taxonomy refresh() {
		mRootCategory.refresh();
		return this;
	}

	public static class Category implements Persistable {
		private PersistentCategory mCategory;

		public Category() {
			mCategory = new PersistentCategory();
		}

		public Category(String uuid) {
			this();
			mCategory.setUuid(uuid);
		}

		public Category(PersistentCategory category) {
			mCategory = category;
		}
		
		@Override public int hashCode() {
			return mCategory.getUuid().hashCode();
		}
		
		@Override public boolean equals(Object o) {
			if (o == null || !(o instanceof Category)) {
				return false;
			}
			
			Category other = (Category) o;
			return other.getUuid().equals(getUuid());
		}

		public PersistentCategory getPersistentObject() {
			return mCategory;
		}
		
		public int getOrder() {
			return mCategory.getOrder();
		}
		
		public void setOrder(int order) {
			mCategory.setOrder(order);
		}

		public void setDefaultCategoryDescription(String name,
				String description) {
			mCategory.setTitle(name);
			mCategory.setDescription(description);
		}

		public String getCategoryName() {
			return mCategory.getTitle();
		}

		public Category getParent() {
			PersistentCategory parent = mCategory.getPersistentParentCategory();
			if (parent == null) {
				return null;
			}
			return new Category(mCategory.getPersistentParentCategory());
		}

		public void addChild(Category childCategory) {
			mCategory.addPersistentChildCategory(childCategory
					.getPersistentObject());
		}

		public List<Category> getChildren() {
			List<Category> children = new LinkedList<Category>();
			for (PersistentCategory child : mCategory
					.getPersistentChildCategoryList()) {
				children.add(new Category(child));
			}
			return children;
		}
		
		public List<Category> getSortedChildren() {
			List<Category> children = getChildren();
			Collections.sort(children, new Comparator<Category>() {
				@Override public int compare(Category c1, Category c2) {
					return c1.getOrder() - c2.getOrder();
				}
			});
			
			return children;
		}

//		private List<Category> getAllChildren(PersistentCategory category,
//				List<Category> children) {
//			List<PersistentCategory> categories = category
//					.getPersistentChildCategoryList();
//			if (categories.size() != 0) {
//				for (PersistentCategory c : categories) {
//					getAllChildren(c, children);
//				}
//			}
//			children.add(new Category(category));
//			return children;
//		}

		public boolean hasChildren() {
			List<PersistentCategory> children = mCategory
					.getPersistentChildCategoryList();
			return children != null && !children.isEmpty();
		}

		public Integer getId() {
			return mCategory.getId();
		}

		public Category commit() {
			mCategory = mCategory.<PersistentCategory> commit();
			return this;
		}

		public void destroy() {
			mCategory.destroy();
		}

		public Category refresh() {
			mCategory = mCategory.<PersistentCategory> refresh();
			return this;
		}

		public String getUuid() {
			return mCategory.getUuid();
		}

		public void setUuid(String uuid) {
			this.mCategory.setUuid(uuid);
		}

		public Category getFromDatabase() {
			PersistentCategory category = PersistentCategory
					.getFromDatabase(getId());
			if (category == null) {
				return null;
			}
			return new Category(category);
		}
		
		@Override public String toString() {
			return getCategoryName();
		}
	}

}
