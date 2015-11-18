package org.literacybridge.acm.store;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.literacybridge.acm.api.IDataRequestResult;

import com.google.common.collect.Sets;

public abstract class MetadataStore {
    private final Taxonomy taxonomy;

    public abstract Transaction newTransaction();

    public abstract AudioItem newAudioItem(String uid);
    public abstract AudioItem getAudioItem(String uid);
    public abstract void deleteAudioItem(String uid);
    public abstract Iterable<AudioItem> getAudioItems();

    public abstract Playlist newPlaylist(String uid);
    public abstract Playlist getPlaylist(String uid);
    public abstract void deletePlaylist(String uid);
    public abstract Iterable<Playlist> getPlaylists();

    public MetadataStore(File acmDirectory) {
        taxonomy = Taxonomy.createTaxonomy(acmDirectory);
    }

    public final Taxonomy getTaxonomy() {
        return taxonomy;
    }

    public final Category getCategory(String uid) {
        return taxonomy.getCategory(uid);
    }

    public abstract IDataRequestResult search(String searchFilter, List<Category> categories, List<Locale> locales);
    public abstract IDataRequestResult search(String searchFilter, Playlist selectedTag);

    public final void commit(Persistable p) {
        //        Transaction t = newTransaction();
        //        t.add(p);
        //        t.begin();
        //        t.commit();
    }

    public static abstract class Transaction {
        private final Set<Persistable> objects = Sets.newHashSet();

        public abstract void begin();

        public final void commit() {
            boolean success = false;
            try {
                for (Persistable o : objects) {
                    o.commitTransaction(this);
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
            doRollback();
        }

        protected abstract void doCommit();
        protected abstract void doRollback();

        public void add(Persistable object) {
            objects.add(object);
        }
    }
}
