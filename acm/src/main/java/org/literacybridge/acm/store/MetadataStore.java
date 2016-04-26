package org.literacybridge.acm.store;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import org.literacybridge.acm.store.MetadataStore.DataChangeListener.DataChangeEventType;

import com.google.common.collect.Lists;

public abstract class MetadataStore {
    private final Taxonomy taxonomy;

    private final List<DataChangeListener> dataChangeListeners;

    public abstract Transaction newTransaction();

    public abstract AudioItem newAudioItem(String uid);
    public abstract AudioItem getAudioItem(String uid);
    public abstract void deleteAudioItem(String uid);
    public abstract Iterable<AudioItem> getAudioItems();

    public abstract Playlist newPlaylist(String name);
    public abstract Playlist getPlaylist(String uid);
    public abstract void deletePlaylist(String uid);
    public abstract Iterable<Playlist> getPlaylists();

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

    protected final void fireChangeEvent(Committable item, DataChangeEventType eventType) {
        for (DataChangeListener listener : dataChangeListeners) {
            listener.fireChangeEvent(item, eventType);
        }
    }

    public abstract SearchResult search(String searchFilter, List<Category> categories, List<Locale> locales);
    public abstract SearchResult search(String searchFilter, Playlist selectedTag);

    public final void commit(Committable... objects) throws IOException {
        for (Committable c : objects) {
            c.ensureIsCommittable();
        }

        Transaction t = newTransaction();
        t.addAll(objects);
        t.commit();
    }

    public interface DataChangeListener {
        public static enum DataChangeEventType {
            ITEM_ADDED,
            ITEM_MODIFIED,
            ITEM_DELETED
        }

        public void fireChangeEvent(Committable item, DataChangeEventType eventType);
    }
}
