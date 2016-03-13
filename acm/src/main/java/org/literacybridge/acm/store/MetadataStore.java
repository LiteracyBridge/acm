package org.literacybridge.acm.store;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public abstract class MetadataStore {
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

    public MetadataStore(Taxonomy taxonomy) {
        this.taxonomy = taxonomy;
    }

    public final Taxonomy getTaxonomy() {
        return taxonomy;
    }

    public final Category getCategory(String uid) {
        return taxonomy.getCategory(uid);
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
}
