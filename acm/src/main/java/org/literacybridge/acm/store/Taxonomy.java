package org.literacybridge.acm.store;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Maps;
import org.literacybridge.acm.gui.Application;

public class Taxonomy implements Cloneable {
  private Integer revision;

  private final Category mRootCategory;
  // Map of categoryId : Category. Maps every categoryid in the Taxonomy to its Category object.
  private final Map<String, Category> categories;

  @Override
  public Taxonomy clone() {
    Taxonomy clone = null;
    try {
      super.clone();
      Category newRoot = this.mRootCategory.clone();
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

  public Collection<Category> getCategoryList() {
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

  public void updateCategoryVisibility(Map<String, Boolean> categoriesToUpdate) {
    boolean changed = false;
    for (Map.Entry<String,Boolean> entry : categoriesToUpdate.entrySet()) {
      Category cat = getCategory(entry.getKey());
      Boolean visible = entry.getValue();
      if (cat.isVisible() != visible) {
        cat.setVisible(visible);
        changed = true;
      }
    }
    if (changed) {
      Application.getMessageService().pumpMessage(new CategoryVisibilitiesUpdated());
    }
  }

  public Iterable<Category> breadthFirstIterator() {
    return new TaxonomyIterable();
  }

  private class TaxonomyIterable implements Iterable<Category> {

    @Override
    public Iterator<Category> iterator() {
      return new TaxonomyIterator(Taxonomy.this);
    }
  };

  private static class TaxonomyIterator implements Iterator<Category> {
    private List<Category> queue = new ArrayList<>();
    private TaxonomyIterator(Taxonomy taxonomy) {
      queue.addAll(taxonomy.getRootCategory().getSortedChildren());
    }
    @Override
    public boolean hasNext() {
      return !queue.isEmpty();
    }

    @Override
    public Category next() {
      Category next = queue.remove(0);
      queue.addAll(next.getSortedChildren());
      return next;
    }
  }

  public static class CategoryVisibilitiesUpdated {

  }
}
