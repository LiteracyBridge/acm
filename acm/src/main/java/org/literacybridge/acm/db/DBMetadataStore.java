package org.literacybridge.acm.db;

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import org.literacybridge.acm.db.Taxonomy.Category;
import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.store.MetadataStore;

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
        return toAudioItemList(PersistentQueries.searchForAudioItems(searchFilter, selectedTag));
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
}
