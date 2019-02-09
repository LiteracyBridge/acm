package org.literacybridge.acm.store;

import java.io.File;
import java.util.Map;

import com.google.common.collect.Maps;

public class Taxonomy {
  private Integer revision;

  private final Category mRootCategory;
  // Map of categoryId : Category. Maps every categoryid in the Taxonomy to its Category object.
  private final Map<String, Category> categories;

  private Taxonomy(Category root) {
    categories = Maps.newHashMap();
    this.mRootCategory = root;
    categories.put(root.getId(), root);
  }

    /**
     * Loads the latest of the built-in, or project taxonomy.
     * @param acmDirectory with possibly updated taxonomy.
     * @return the latest taxonomy.
     */
  public static Taxonomy createTaxonomy(File acmDirectory) {
    Category root = new Category(TaxonomyLoader.LB_TAXONOMY_UID);
    root.setName("root");
    root.setOrder(0);

    Taxonomy taxonomy = new Taxonomy(root);
    TaxonomyLoader.loadLatestTaxonomy(acmDirectory, taxonomy);
    return taxonomy;
  }

  public Category getRootCategory() {
    return mRootCategory;
  }

  public Category getCategory(String categoryId) {
    return categories.get(categoryId);
  }

  public Iterable<Category> getCategoryList() {
    return mRootCategory.getChildren();
  }

  public boolean isRoot(Category category) {
    return category == this.mRootCategory;
  }

  public void addChild(Category parent, Category newChild) {
    parent.addChild(newChild);
    newChild.setParent(parent);
    categories.put(newChild.getId(), newChild);
  }

  public Integer getRevision() {
    return revision;
  }
  public void setRevision(int revision) {
    if (this.revision != null) throw new IllegalStateException("Revision has already been set.");
    this.revision = revision;
  }

  public static class CategoryVisibilitiesUpdated {

  }
}
