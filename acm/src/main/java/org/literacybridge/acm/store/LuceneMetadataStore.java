package org.literacybridge.acm.store;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.literacybridge.acm.api.IDataRequestResult;
import org.literacybridge.acm.gui.AudioItemCache;

import com.google.common.collect.Maps;

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
            Iterable<AudioItem> audioItems = index.getAudioItems();
            for (AudioItem audioItem : audioItems) {
                audioItemCache.update(audioItem);
            }

            Iterable<Playlist> playlists = index.getPlaylists();
            for (Playlist playlist : playlists) {
                playlistCache.put(playlist.getUuid(), playlist);
                for (String uid : playlist.getAudioItemList()) {
                    AudioItem audioItem = audioItemCache.get(uid);
                    audioItem.addPlaylist(playlist);
                    audioItemCache.update(audioItem);
                }
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
            return index.newTransaction(this);
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "IOException while starting transaction with Lucene index.", e);
            return null;
        }
    }

    @Override
    public void deleteAudioItem(String uuid) {
        final AudioItem item = getAudioItem(uuid);
        if (item == null) {
            throw new NoSuchElementException("AudioItem with " + uuid + " does not exist.");
        }

        try {
            item.delete();
            commit(item);
            audioItemCache.invalidate(uuid);
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "IOException while deleting playlist " + uuid, e);
        }
    }

    @Override
    public void deletePlaylist(String uuid) {
        final Playlist playlist = getPlaylist(uuid);
        if (playlist == null) {
            throw new NoSuchElementException("Playlist with " + uuid + " does not exist.");
        }

        playlist.delete();
        try {
            playlist.delete();
            commit(playlist);
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
