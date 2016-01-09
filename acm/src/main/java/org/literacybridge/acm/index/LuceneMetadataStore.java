package org.literacybridge.acm.index;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.literacybridge.acm.api.IDataRequestResult;
import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.store.Category;
import org.literacybridge.acm.store.MetadataStore;
import org.literacybridge.acm.store.Playlist;
import org.literacybridge.acm.store.Taxonomy;

/**
 *
 * TODOs:
 * OK fix playlist ordering bug
 * OK rename playlists
 * OK generate playlist uuids
 * OK store playlist uuid->name mapping as commit data in Lucene index
 * - clean-up transactions API
 * OK checkin/out Lucene index with dropbox instead of DB
 * - finish AudioItemCache (sort ordering)
 * - add playlist cache
 * - pass reference to MetadataStore around, instead of singleton pattern
 * - default sort order should be insertion order
 *
 */
public class LuceneMetadataStore extends MetadataStore {
    private static final Logger LOG = Logger.getLogger(LuceneMetadataStore.class.getName());

    private final AudioItemIndex index;

    public LuceneMetadataStore(Taxonomy taxonomy, AudioItemIndex index) {
        super(taxonomy);
        this.index = index;
    }

    @Override
    public AudioItem getAudioItem(String uid) {
        try {
            return index.getAudioItem(uid);
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "IOException while load audioItem " + uid + " from Lucene index.", e);
            return null;
        }
    }

    @Override
    public Iterable<AudioItem> getAudioItems() {
        try {
            return index.getAudioItems();
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "IOException while loadign audioItems from Lucene index.", e);
            return null;
        }
    }

    @Override
    public IDataRequestResult search(String query,
            List<Category> categories, List<Locale> locales) {
        try {
            return index.search(query, categories, locales);
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "IOException while searching Lucene index.", e);
            return null;
        }
    }

    @Override
    public IDataRequestResult search(String query, Playlist playlist) {
        try {
            return index.search(query, playlist);
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "IOException while searching Lucene index.", e);
            return null;
        }
    }

    @Override
    public Playlist newPlaylist(String name) {
        Playlist playlist = null;
        Transaction t = newTransaction();
        boolean success = false;
        try {
            playlist = index.addPlaylist(name, t);
            t.commit();
            success = true;
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "IOException while adding playlist name=(" + name + ") from Lucene index.", e);
        } finally {
            if (!success) {
                t.rollback();
            }
        }

        return playlist;
    }

    @Override
    public Playlist getPlaylist(String uuid) {
        try {
            return index.getPlaylist(uuid);
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "IOException while retrieving playlist (uuid=" + uuid + ") from Lucene index.", e);
            return null;
        }
    }

    @Override
    public Iterable<Playlist> getPlaylists() {
        try {
            return index.getPlaylists();
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "IOException while retrieving playlists from Lucene index.", e);
            return null;
        }
    }

    @Override
    public Transaction newTransaction() {
        try {
            return new Transaction(index);
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "IOException while starting transaction with Lucene index.", e);
            return null;
        }
    }

    @Override
    public void deleteAudioItem(String uuid) {
        Transaction t = newTransaction();
        boolean success = false;
        try {
            index.deleteAudioItem(uuid, t);
            t.commit();
            success = true;
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "IOException while deleting audioitem uuid=(" + uuid + ") from Lucene index.", e);
        } finally {
            if (!success) {
                t.rollback();
            }
        }
    }

    @Override
    public void deletePlaylist(String uuid) {
        Playlist playlist = getPlaylist(uuid);
        Transaction t = newTransaction();
        boolean success = false;
        try {
            for (String audioItemUuid : playlist.getAudioItemList()) {
                AudioItem audioItem = getAudioItem(audioItemUuid);
                audioItem.removePlaylist(playlist);
                t.add(audioItem);
            }

            index.deletePlaylist(uuid, t);
            t.commit();
            success = true;
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "IOException while deleting playlist uuid=(" + uuid + ") from Lucene index.", e);
        } finally {
            if (!success) {
                t.rollback();
            }
        }
    }
}
