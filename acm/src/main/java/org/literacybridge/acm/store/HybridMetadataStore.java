package org.literacybridge.acm.store;

import java.io.File;
import java.util.List;
import java.util.Locale;

import org.literacybridge.acm.api.IDataRequestResult;

public class HybridMetadataStore extends MetadataStore {
    private final MetadataStore luceneStore;
    private final MetadataStore dbStore;

    public HybridMetadataStore(File acmDirectory, MetadataStore luceneStore, MetadataStore dbStore) {
        super(acmDirectory);
        this.luceneStore = luceneStore;
        this.dbStore = dbStore;
    }

    @Override
    public Transaction newTransaction() {
        return luceneStore.newTransaction();
    }

    @Override
    public AudioItem newAudioItem(String uid) {
        return luceneStore.newAudioItem(uid);
    }

    @Override
    public AudioItem getAudioItem(String uid) {
        return luceneStore.getAudioItem(uid);
    }

    @Override
    public void deleteAudioItem(String uid) {
        luceneStore.deleteAudioItem(uid);
    }

    @Override
    public Iterable<AudioItem> getAudioItems() {
        return luceneStore.getAudioItems();
    }

    @Override
    public Playlist newPlaylist(String uid) {
        return dbStore.newPlaylist(uid);
    }

    @Override
    public Playlist getPlaylist(String uid) {
        return dbStore.getPlaylist(uid);
    }

    @Override
    public void deletePlaylist(String uid) {
        dbStore.deletePlaylist(uid);
    }

    @Override
    public Iterable<Playlist> getPlaylists() {
        return dbStore.getPlaylists();
    }

    @Override
    public IDataRequestResult search(String searchFilter,
            List<Category> categories, List<Locale> locales) {
        return luceneStore.search(searchFilter, categories, locales);
    }

    @Override
    public IDataRequestResult search(String searchFilter,
            Playlist selectedTag) {
        return luceneStore.search(searchFilter, selectedTag);
    }
}
