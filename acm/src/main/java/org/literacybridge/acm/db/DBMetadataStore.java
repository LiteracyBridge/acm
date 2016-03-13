package org.literacybridge.acm.db;

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.store.Category;
import org.literacybridge.acm.store.MetadataStore;
import org.literacybridge.acm.store.Playlist;
import org.literacybridge.acm.store.SearchResult;
import org.literacybridge.acm.store.Taxonomy;
import org.literacybridge.acm.store.Transaction;

/**
 * @deprecated: We're removing Derby DB from the ACM and are switching to a Lucene index
 *              for storing and searching all metadata.
 */
@Deprecated
public class DBMetadataStore extends MetadataStore {
    public DBMetadataStore(Taxonomy taxonomy) {
        super(taxonomy);
    }

    @Override
    public AudioItem getAudioItem(String uid) {
        return PersistentAudioItem.convert(PersistentAudioItem.getFromDatabase(uid));
    }

    @Override
    public Iterable<AudioItem> getAudioItems() {
        return toAudioItemList(PersistentAudioItem.getFromDatabase());
    }

    @Override
    public SearchResult search(String searchFilter,
            List<Category> categories, List<Locale> locales) {
        throw new UnsupportedOperationException("Searching the Derby DB is not supported anymore.");
    }

    @Override
    public SearchResult search(String searchFilter, Playlist playlist) {
        throw new UnsupportedOperationException("Searching the Derby DB is not supported anymore.");
    }

    static List<AudioItem> toAudioItemList(List<PersistentAudioItem> list) {
        List<AudioItem> results = new LinkedList<AudioItem>();
        for (PersistentAudioItem item : list) {
            results.add(PersistentAudioItem.convert(item));
        }
        return results;
    }

    @Override
    public Playlist newPlaylist(String uid) {
        throw new UnsupportedOperationException("Not supported anymore.");
    }

    @Override
    public Playlist getPlaylist(String uid) {
        return PersistentTag.convert(PersistentTag.getFromDatabase(uid));
    }

    @Override
    public Iterable<Playlist> getPlaylists() {
        return PersistentTag.toPlaylists(PersistentTag.getFromDatabase());
    }

    @Override
    public Transaction newTransaction() {
        throw new UnsupportedOperationException("Writing to Derby DB is not supported anymore.");
    }

    @Override
    public void deleteAudioItem(String uid) {
        throw new UnsupportedOperationException("Writing to Derby DB is not supported anymore.");
    }

    @Override
    public void deletePlaylist(String uid) {
        throw new UnsupportedOperationException("Writing to Derby DB is not supported anymore.");
    }

    @Override
    public AudioItem newAudioItem(String uid) {
        throw new UnsupportedOperationException("Not supported anymore.");
    }
}
