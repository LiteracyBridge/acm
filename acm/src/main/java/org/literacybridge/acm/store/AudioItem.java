package org.literacybridge.acm.store;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * An AudioItem is a unique audio entity, identified by its audioItemID. It is
 * optionally associated with one or more categories and one ore more playlist.
 */
public class AudioItem extends Committable {
  private final String id;
  private final Metadata metadata;

  private final Map<String, Category> categories;
  private final Map<String, Playlist> playlists;

  AudioItem(String id) {
    this.id = id;
    this.metadata = new Metadata();
    this.categories = Maps.newHashMap();
    this.playlists = Maps.newHashMap();
  }

  public final String getId() {
    return id;
  }

  public final Metadata getMetadata() {
    return this.metadata;
  }

  public final void addCategory(Category category) {
    if (hasCategory(category)) {
      return;
    }

    // first we check if a leaf category is added (which should always be the
    // case),
    // otherwise we find an appropriate leaf
    if (category.hasChildren()) {
      do {
        // always pick the first child, which usually is the 'general' child
        // category
        category = category.getSortedChildren().iterator().next();
      } while (category.hasChildren());
    }

    // make sure all parents up to the root are added as well
    do {
      if (!hasCategory(category)) {
        categories.put(category.getId(), category);
      }
      category = category.getParent();
    } while (category != null);
  }

  public final void addCategories(Collection<Category> categories) {
      for (Category category : categories)
          addCategory(category);
  }

  public final boolean hasCategory(Category category) {
    return categories.containsKey(category.getId());
  }

  public final boolean hasCategory(String categoryId) {
    return categories.containsKey(categoryId);
  }

  public final void removeCategory(Category category) {
    categories.remove(category.getId());
    while (removeOrphanedNonLeafCategories())
      ;
  }

  // returns true, if any categories were removed
  private boolean removeOrphanedNonLeafCategories() {
    Set<Category> toRemove = Sets.newHashSet();
    for (Category cat : getCategoryList()) {
      if (cat.hasChildren()) {
        toRemove.add(cat);
      }
    }
    // now 'toRemove' contains all non-leaf categories that this audioitem
    // contains

    for (Category cat : getCategoryList()) {
      if (cat.getParent() != null) {
        toRemove.remove(cat.getParent());
      }
    }
    // now 'toRemove' only contains categories for which this audioitem has at
    // least one child

    for (Category cat : toRemove) {
      categories.remove(cat.getId());
    }

    return !toRemove.isEmpty();
  }

  public final void removeAllCategories() {
    categories.clear();
  }

  public final Collection<Category> getCategoryList() {
    return categories.values();
  }

  public final Collection<Category> getCategoryLeavesList() {
    return getCategoryList().stream().filter(cat -> !cat.hasChildren()).collect(Collectors.toList());
  }

  public final void addPlaylist(Playlist playlist) {
    playlists.put(playlist.getId(), playlist);
  }

  public final boolean hasPlaylist(Playlist playlist) {
    return playlists.containsKey(playlist.getId());
  }

  public final void removePlaylist(Playlist playlist) {
    playlists.remove(playlist.getId());
  }

  public final Collection<Playlist> getPlaylists() {
    return playlists.values();
  }

  @Override
  public boolean doCommit(Transaction t) throws IOException {
    if (isDeleteRequested()) {
      t.getIndex().deleteAudioItem(id, t);
      return false;
    } else {
      return t.getIndex().updateAudioItem(this, t.getWriter());
    }
  }

  @Override
  public void doRollback(Transaction t) throws IOException {
    categories.clear();
    playlists.clear();
    metadata.clear();
    t.getIndex().refresh(this);
  }

  // Convenience functions. Rational getters for the ridiculously over-engineered metadata values.
  public String getLanguageCode() {
      if (!metadata.containsField(MetadataSpecification.DC_LANGUAGE)) return null;
      return metadata.getMetadataValue(MetadataSpecification.DC_LANGUAGE).toString();
  }

  public String getTitle() {
      return metadata.getMetadataValue(MetadataSpecification.DC_TITLE).getValue();
  }

  public String getDuration() {
	    return metadata.getMetadataValue(MetadataSpecification.LB_DURATION).getValue().trim();
	}

  /**
   * Audio is considered as corrupted if it has a negative or 00:00 or hour (h) duration
   */
  public Boolean isCorrupted() {
  		return this.getDuration().matches( "(^-\\s?\\d\\d:\\d\\d\\s?\\w)|(00:00)|(\\d\\d:\\d\\d\\s+h)");
  }

  @Override
  public String toString() {
    return getTitle();
  }
}
