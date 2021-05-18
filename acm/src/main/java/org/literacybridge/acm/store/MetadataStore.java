package org.literacybridge.acm.store;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import com.google.common.collect.Lists;

public abstract class MetadataStore {
  private final Taxonomy taxonomy;

  private final List<DataChangeListener> dataChangeListeners;
  private boolean haveChanges = false;

  public abstract Transaction newTransaction();

  public abstract AudioItem newAudioItem(String uid);

  public abstract AudioItem getAudioItem(String uid);

  public abstract void deleteAudioItem(String uid);

  public abstract Collection<AudioItem> getAudioItems();

  public abstract Playlist newPlaylist(String name);

  public abstract Playlist getPlaylist(String uid);

  public abstract Playlist findPlaylistByName(String name);

  public abstract void deletePlaylist(String uid);

  public abstract Collection<Playlist> getPlaylists();

  public boolean hasChanges() {
    return haveChanges;
  }

  public MetadataStore(Taxonomy taxonomy) {
    this.taxonomy = taxonomy;
    dataChangeListeners = Lists.newLinkedList();
  }

  public final Taxonomy getTaxonomy() {
    return taxonomy;
  }

  public final Category getCategory(String uid) {
    return taxonomy.getCategory(uid);
  }

  public final void addDataChangeListener(DataChangeListener listener) {
    this.dataChangeListeners.add(listener);
  }

  protected final void fireChangeEvent(List<DataChangeEvent> events) {
    haveChanges = true;
    for (DataChangeListener listener : dataChangeListeners) {
      listener.dataChanged(events);
    }
  }

  public abstract SearchResult search(String searchFilter,
      List<Category> categories, List<Locale> locales);

  public abstract SearchResult search(String searchFilter, Playlist selectedPlaylist);

  public final void commit(Committable... objects) throws IOException {
    for (Committable c : objects) {
      c.ensureIsCommittable();
    }

    Transaction t = newTransaction();
    t.addAll(objects);
    t.commit();
  }

  public static class DataChangeEvent {
    private final Committable item;
    private final DataChangeEventType eventType;

    public DataChangeEvent(Committable item, DataChangeEventType eventType) {
      this.item = item;
      this.eventType = eventType;
    }

    public Committable getItem() {
      return item;
    }

    public DataChangeEventType getEventType() {
      return eventType;
    }
  }

  public enum DataChangeEventType {
    ITEM_ADDED, ITEM_MODIFIED, ITEM_DELETED
  }

  public interface DataChangeListener {
    void dataChanged(List<DataChangeEvent> events);
  }
}
