package org.literacybridge.acm.store;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.literacybridge.acm.api.IDataRequestResult;
import org.literacybridge.acm.gui.AudioItemCache;

import com.google.common.collect.Maps;

/**
 *
 * TODOs:
 * OK fix playlist ordering bug
 * OK rename playlists
 * OK generate playlist uuids
 * OK store playlist uuid->name mapping as commit data in Lucene index
 * OK clean-up transactions API
 * OK checkin/out Lucene index with dropbox instead of DB
 * - finish AudioItemCache (sort ordering)
 * - default sort order should be insertion order
 * OK add playlist cache
 * - pass reference to MetadataStore around, instead of singleton pattern
 *
 */
public class LuceneMetadataStore extends MetadataStore {
    private static final Logger LOG = Logger.getLogger(LuceneMetadataStore.class.getName());

    private final AudioItemIndex index;
    private final Map<String, Playlist> playlistCache;
    private final AudioItemCache audioItemCache;

    public LuceneMetadataStore(Taxonomy taxonomy, AudioItemIndex index) {
        super(taxonomy);
        this.playlistCache = Maps.newHashMap();
        this.index = index;
        this.audioItemCache = new AudioItemCache();

        // fill caches
        try {
            Iterable<Playlist> playlists = index.getPlaylists();
            for (Playlist playlist : playlists) {
                playlistCache.put(playlist.getUuid(), playlist);
            }

            Iterable<AudioItem> audioItems = index.getAudioItems();
            for (AudioItem audioItem : audioItems) {
                audioItemCache.update(audioItem);
            }
        } catch (IOException e) {
            throw new RuntimeException("Unable to initialize caches", e);
        }
    }

    @Override
    public AudioItem getAudioItem(String uid) {
        return audioItemCache.get(uid);
    }

    @Override
    public Iterable<AudioItem> getAudioItems() {
        return audioItemCache.getAudioItems();
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
        final Playlist playlist = index.newPlaylist(name);
        playlist.setCommitListener(new Committable.CommitListener() {
            @Override public void afterCommit() {
                playlistCache.put(playlist.getUuid(), playlist);
            }

            @Override public void afterRollback() {
            }
        });

        try {
            commit(playlist);
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "IOException while creating new playlist.", e);
            return null;
        }
        return playlist;
    }

    @Override
    public Playlist getPlaylist(String uuid) {
        return  playlistCache.get(uuid);
    }

    @Override
    public Iterable<Playlist> getPlaylists() {
        return playlistCache.values();
    }

    @Override
    public Transaction newTransaction() {
        try {
            return index.newTransaction();
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "IOException while starting transaction with Lucene index.", e);
            return null;
        }
    }

    @Override
    public void deleteAudioItem(String uuid) {
        try {
            commit(new Committable() {
                @Override
                public void doCommit(Transaction t) throws IOException {
                    index.deleteAudioItem(uuid, t);
                }

                @Override
                public void doRollback(Transaction t) throws IOException {
                }
            });
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "IOException while deleting playlist " + uuid, e);
        }
    }

    @Override
    public void deletePlaylist(String uuid) {
        Playlist playlist = getPlaylist(uuid);
        try {
            commit(new Committable() {
                @Override
                public void doCommit(Transaction t) throws IOException {
                    for (String audioItemUuid : playlist.getAudioItemList()) {
                        AudioItem audioItem = getAudioItem(audioItemUuid);
                        audioItem.removePlaylist(playlist);
                        t.add(audioItem);
                    }

                    index.deletePlaylist(uuid, t);
                }

                @Override
                public void doRollback(Transaction t) throws IOException {
                }
            });
            playlistCache.remove(uuid);
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "IOException while deleting playlist " + uuid, e);
        }
    }

    @Override
    public AudioItem newAudioItem(String uid) {
        final AudioItem audioItem = new AudioItem(uid);
        audioItem.setCommitListener(new Committable.CommitListener() {
            @Override public void afterCommit() {
                audioItemCache.update(audioItem);
            }

            @Override public void afterRollback() {
            }
        });

        return audioItem;
    }
}
