package org.literacybridge.acm.store;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.literacybridge.acm.store.MetadataStore.Transaction;

import com.google.common.collect.Sets;

public abstract class MetadataStore {
    public static abstract class Transaction {
        private final Set<Persistable> objects = Sets.newHashSet();

        public abstract void begin();

        public final void commit() {
            boolean success = false;
            try {
                for (Persistable o : objects) {
                    //o.commitTransaction(this);
                }
                success = true;
            } finally {
                if (success) {
                    boolean success2 = false;
                    try {
                        doCommit();
                        success2 = true;
                    } finally {
                        if (!success2) {
                            rollback();
                        }
                    }
                } else {
                    rollback();
                }
            }
        }

        public final void rollback() {
            try {
                doRollback();
            } finally {
                for (Persistable o : objects) {
                    o.refresh();
                }
            }
        }

        protected abstract void doCommit();
        protected abstract void doRollback();

        public void add(Persistable object) {
            objects.add(object);
        }
    }

    public abstract Transaction newTransaction();

    public abstract AudioItem newAudioItem(String uid);
    public abstract AudioItem getAudioItem(String uid);
    public abstract Iterable<AudioItem> getAudioItems();
    public abstract Iterable<AudioItem> search(String searchFilter, List<Category> categories, List<Locale> locales);
    public abstract Iterable<AudioItem> search(String searchFilter, Playlist selectedTag);

    public abstract Playlist newPlaylist(String uid);
    public abstract Playlist getPlaylist(String uid);
    public abstract Iterable<Playlist> getPlaylists();

    public abstract Category newCategory(String uid);
    public abstract Category getCategory(String uid);

    public abstract Metadata newMetadata();

    public abstract Map<Integer, Integer> getFacetCounts(String filter, List<Category> categories, List<Locale> locales);
    public abstract Map<String, Integer> getLanguageFacetCounts(String filter, List<Category> categories, List<Locale> locales);

    public final void commit(Persistable p) {
        Transaction t = newTransaction();
        t.add(p);
        t.begin();
        t.commit();
    }

    public final void delete(Persistable p) {
        p.destroy();
    }

    public final void refresh(Persistable p) {
        p.refresh();
    }
}
