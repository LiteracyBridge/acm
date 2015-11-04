package org.literacybridge.acm.db;

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.store.Category;
import org.literacybridge.acm.store.Metadata;
import org.literacybridge.acm.store.MetadataStore;
import org.literacybridge.acm.store.Playlist;

import com.google.common.collect.Lists;

/**
 * @deprecated: We're removing Derby DB from the ACM and are switching to a Lucene index
 *              for storing and searching all metadata.
 */
@Deprecated
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
        return new DBPlaylist(uid);
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

    /**
     * Returns the facet count for all categories that are stored
     * in the database.
     *
     * Key: database id (getId())
     * Value: count value
     *
     * Note: Returns '0' for unassigned categories.
     */
    @Override
    public Map<Integer, Integer> getFacetCounts(String filter, List<Category> categories, List<Locale> locales) {
        List<PersistentCategory> persistentCategories = null;

        if (categories != null) {
            persistentCategories = Lists.newArrayListWithCapacity(categories.size());

            for (Category cat : categories) {
                persistentCategories.add(((DBCategory) cat).getPersistentObject());
            }
        }
        return PersistentCategory.getFacetCounts(filter, persistentCategories, locales);
    }

    @Override
    public Map<String, Integer> getLanguageFacetCounts(String filter, List<Category> categories, List<Locale> locales) {
        return PersistentQueries.getLanguageFacetCounts(filter, categories, locales);
    }

    @Override
    public Metadata newMetadata() {
        return new DBMetadata();
    }

}
