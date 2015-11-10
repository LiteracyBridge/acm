package org.literacybridge.acm.index;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.store.Category;
import org.literacybridge.acm.store.MetadataStore;
import org.literacybridge.acm.store.Playlist;

public class LuceneMetadataStore extends MetadataStore {

    public LuceneMetadataStore(File acmDirectory) {
        super(acmDirectory);
    }

    @Override
    public AudioItem newAudioItem(String uid) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public AudioItem getAudioItem(String uid) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Iterable<AudioItem> getAudioItems() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Iterable<AudioItem> search(String searchFilter,
            List<Category> categories, List<Locale> locales) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Iterable<AudioItem> search(String searchFilter,
            Playlist selectedTag) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Playlist newPlaylist(String uid) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Playlist getPlaylist(String uid) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Iterable<Playlist> getPlaylists() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<String, Integer> getFacetCounts(String filter,
            List<Category> categories, List<Locale> locales) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<String, Integer> getLanguageFacetCounts(String filter,
            List<Category> categories, List<Locale> locales) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Transaction newTransaction() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void deleteAudioItem(String uid) {
        // TODO Auto-generated method stub

    }

    @Override
    public void deletePlaylist(String uid) {
        // TODO Auto-generated method stub

    }
}
