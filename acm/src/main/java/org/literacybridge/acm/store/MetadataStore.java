package org.literacybridge.acm.store;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.lucene.index.IndexWriter;
import org.literacybridge.acm.api.IDataRequestResult;
import org.literacybridge.acm.index.AudioItemIndex;

import com.google.common.collect.Sets;

public abstract class MetadataStore {
    private static final Logger LOG = Logger.getLogger(MetadataStore.class.getName());

    private final Taxonomy taxonomy;

    public abstract Transaction newTransaction();

    public abstract AudioItem newAudioItem(String uid);
    public abstract AudioItem getAudioItem(String uid);
    public abstract void deleteAudioItem(String uid);
    public abstract Iterable<AudioItem> getAudioItems();

    public abstract Playlist newPlaylist(String name);
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
        Transaction t = newTransaction();
        t.add(p);
        t.commit();
    }

    public static class Transaction {
        private final Set<Persistable> objects = Sets.newHashSet();
        private final AudioItemIndex index;
        private final IndexWriter writer;

        public Transaction(AudioItemIndex index) throws IOException {
            this.index = index;
            this.writer = index.newWriter();
        }

        public final void commit() {
            boolean success = false;
            try {
                for (Persistable o : objects) {
                    o.commitTransaction(this);
                }
                success = true;
            } catch (IOException e) {
                LOG.log(Level.SEVERE, "IOException while commiting a transaction.", e);
            } finally {
                if (success) {
                    boolean success2 = false;
                    try {
                        writer.close();
                        success2 = true;
                    } catch (IOException e) {
                        LOG.log(Level.SEVERE, "IOException while commiting a transaction.", e);
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
                writer.rollback();
            } catch (IOException e) {
                LOG.log(Level.SEVERE, "IOException while rolling back a transaction.", e);
            }
        }

        public void add(Persistable object) {
            objects.add(object);
        }

        public AudioItemIndex getIndex() {
            return index;
        }

        public IndexWriter getWriter() {
            return writer;
        }
    }
}
