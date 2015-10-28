package org.literacybridge.acm.db;

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.store.Category;
import org.literacybridge.acm.store.MetadataStore;
import org.literacybridge.acm.store.Playlist;

public class DBMetadataStore extends MetadataStore {
    @Override
    public AudioItem newAudioItem(String uid) {
        return new DBAudioItem(uid);
    }

    @Override
    public AudioItem getAudioItem(String uid) {
        PersistentAudioItem item = PersistentAudioItem.getFromDatabase(uid);
        if (item == null) {
            return null;
        }
        return new DBAudioItem(item);
    }

    @Override
    public Iterable<AudioItem> getAudioItems() {
        return toAudioItemList(PersistentAudioItem.getFromDatabase());
    }

    @Override
    public Iterable<AudioItem> search(String searchFilter,
            List<Category> categories, List<Locale> locales) {
        return toAudioItemList(PersistentQueries.searchForAudioItems(searchFilter, categories, locales));
    }

    @Override
    public Iterable<AudioItem> search(String searchFilter,
            Playlist selectedTag) {
        return toAudioItemList(PersistentQueries.searchForAudioItems(searchFilter, (DBPlaylist) selectedTag));
    }

    //    static AudioItem getFromDatabase(int id) {
    //        PersistentAudioItem item = PersistentAudioItem.getFromDatabase(id);
    //        if (item == null) {
    //            return null;
    //        }
    //        return new DBAudioItem(item);
    //    }
    //
    static List<AudioItem> toAudioItemList(List<PersistentAudioItem> list) {
        List<AudioItem> results = new LinkedList<AudioItem>();
        for (PersistentAudioItem item : list) {
            results.add(new DBAudioItem(item));
        }
        return results;
    }

    @Override
    public Playlist newPlaylist(String uid) {
        return new DBPlaylist();
    }

    @Override
    public Playlist getPlaylist(String uid) {
        return DBPlaylist.getFromDatabase(uid);
    }

    @Override
    public Iterable<Playlist> getPlaylists() {
        return DBPlaylist.getFromDatabase();
    }

    @Override
    public Category newCategory(String uid) {
        return new DBCategory(uid);
    }

    @Override
    public Category getCategory(String uid) {
        return DBCategory.getFromDatabase(uid);
    }
}
