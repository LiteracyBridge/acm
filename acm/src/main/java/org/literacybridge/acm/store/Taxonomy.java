package org.literacybridge.acm.store;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.google.common.collect.Maps;
import org.literacybridge.acm.config.CategoryFilter;
import org.literacybridge.acm.gui.Application;

import static org.literacybridge.acm.store.Category.*;

public class Taxonomy implements Cloneable {
  private Integer revision;

  private final Category rootCategory;
  // Map of categoryId : Category. Maps every categoryid in the Taxonomy to its Category object.
  private final Map<String, Category> categories;

  @Override
  public Taxonomy clone() {
    Taxonomy clone = null;
    try {
      super.clone();
      Category newRoot = this.rootCategory.clone();
      clone = new Taxonomy(newRoot);
      clone.revision = this.revision;
      // We've cloned the categories, now add them to the Taxonomy's map of categories.
      List<Category> categoriesToAdd = new ArrayList<>(newRoot.getChildren());
      while (categoriesToAdd.size() > 0) {
        Category cat = categoriesToAdd.remove(0);
        categoriesToAdd.addAll(cat.getChildren());
        clone.categories.put(cat.getId(), cat);
      }

    } catch (CloneNotSupportedException e) {
      e.printStackTrace();
    }
      return clone;
  }

  private Taxonomy(Category root) {
    categories = Maps.newHashMap();
    this.rootCategory = root;
    categories.put(root.getId(), root);
  }

    /**
     * Loads the latest of the built-in, or project taxonomy.
     * @param acmDirectory with possibly updated taxonomy.
     * @return the latest taxonomy.
     */
  public static Taxonomy createTaxonomy(CategoryFilter categoryFilter, File acmDirectory) {
    Category root = new CategoryBuilder(TaxonomyLoader.LB_TAXONOMY_UID)
        .withName("root")
        .withOrder(0)
        .build();

    if (categoryFilter == null) {
        categoryFilter = new CategoryFilter(null);
    }
    Taxonomy taxonomy = new Taxonomy(root);
    TaxonomyLoader.loadLatestTaxonomy(categoryFilter, acmDirectory, taxonomy);
    return taxonomy;
  }

  public Category getRootCategory() {
    return rootCategory;
  }

  public Category getCategory(String categoryId) {
    return categories.get(categoryId);
  }

  public Collection<Category> getCategoryList() {
    return rootCategory.getChildren();
  }

  public boolean isRoot(Category category) {
    return category == this.rootCategory;
  }

  public void addChild(Category parent, Category newChild) {
    parent.addChild(newChild);
    categories.put(newChild.getId(), newChild);
  }

  public Integer getRevision() {
    return revision;
  }
  public void setRevision(int revision) {
    if (this.revision != null) throw new IllegalStateException("Revision has already been set.");
    this.revision = revision;
  }

  public void updateCategoryVisibility(Map<String, Boolean> categoriesToUpdate) {
    boolean changed = false;
    for (Map.Entry<String,Boolean> entry : categoriesToUpdate.entrySet()) {
      Category cat = getCategory(entry.getKey());
      Boolean visible = entry.getValue();
      if (cat.isVisible() != visible) {
        cat.updateVisibility(visible);
        changed = true;
      }
    }
    if (changed) {
      Application.getMessageService().pumpMessage(new CategoryVisibilitiesUpdated());
    }
  }

  public Iterable<Category> breadthFirstIterator() {
    return rootCategory.breadthFirstIterator();
  }

  // The Class type of this class is enough to let receivers know that the visibilities were updated.
  public static class CategoryVisibilitiesUpdated {

  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Taxonomy taxonomy = (Taxonomy) o;
    return getRevision().equals(taxonomy.getRevision())
        && rootCategory.equals(taxonomy.rootCategory) && categories.equals(taxonomy.categories);
  }

  @Override
  public int hashCode() {
    return Objects.hash(getRevision(), rootCategory, categories);
  }
}
