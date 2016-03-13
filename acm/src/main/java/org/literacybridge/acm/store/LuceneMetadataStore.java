package org.literacybridge.acm.store;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.literacybridge.acm.gui.AudioItemCache;

import com.google.common.collect.Maps;

public class LuceneMetadataStore extends MetadataStore {
    private static final Logger LOG = Logger.getLogger(LuceneMetadataStore.class.getName());

    private final AudioItemIndex index;
    private final Map<String, Playlist> playlistCache;
    private final AudioItemCache audioItemCache;

    private AtomicReference<Transaction> activeTransaction = new AtomicReference<Transaction>();

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
    public SearchResult search(String query,
            List<Category> categories, List<Locale> locales) {
        try {
            return index.search(query, categories, locales);
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "IOException while searching Lucene index.", e);
            return null;
        }
    }

    @Override
    public SearchResult search(String query, Playlist playlist) {
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
                if (playlist.isDeleteRequested()) {
                    playlistCache.remove(playlist.getUuid());
                } else {
                    playlistCache.put(playlist.getUuid(), playlist);
                }
            }

            @Override public void afterRollback() {
            }
        });

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
    public synchronized Transaction newTransaction() {
        try {
            final Transaction oldTransaction = activeTransaction.get();
            if (oldTransaction != null && oldTransaction.isActive()) {
                throw new IOException("Nested transactions are not allowed.");
            }

            Transaction newTransaction = index.newTransaction(this);
            activeTransaction.set(newTransaction);
            return newTransaction;
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

        item.delete();
    }

    @Override
    public void deletePlaylist(String uuid) {
        final Playlist playlist = getPlaylist(uuid);
        if (playlist == null) {
            throw new NoSuchElementException("Playlist with " + uuid + " does not exist.");
        }

        playlist.delete();
    }

    @Override
    public AudioItem newAudioItem(String uid) {
        final AudioItem audioItem = new AudioItem(uid);
        audioItem.setCommitListener(new Committable.CommitListener() {
            @Override public void afterCommit() {
                if (audioItem.isDeleteRequested()) {
                    audioItemCache.invalidate(audioItem.getUuid());
                } else {
                    audioItemCache.update(audioItem);
                }
            }

            @Override public void afterRollback() {
            }
        });

        return audioItem;
    }
}
