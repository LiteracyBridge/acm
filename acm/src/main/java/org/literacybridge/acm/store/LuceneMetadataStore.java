package org.literacybridge.acm.store;

import com.google.common.collect.Maps;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LuceneMetadataStore extends MetadataStore {
  private static final Logger LOG = Logger
      .getLogger(LuceneMetadataStore.class.getName());

  private final AudioItemIndex index;
  private final Map<String, Playlist> playlistCache;
  private final Map<String, AudioItem> audioItemCache;

  private AtomicReference<Transaction> activeTransaction = new AtomicReference<Transaction>();

  public LuceneMetadataStore(Taxonomy taxonomy, File indexDirectory)
      throws IOException {
      super(taxonomy);
      // initialize Lucene index
      if (!AudioItemIndex.indexExists(indexDirectory)) {
          this.index = AudioItemIndex.newIndex(indexDirectory, taxonomy);
      } else {
        this.index = AudioItemIndex.load(indexDirectory, taxonomy);
      }

      this.playlistCache = Maps.newLinkedHashMap();
      this.audioItemCache = Maps.newLinkedHashMap();

      // fill caches
      try {
          Iterable<AudioItem> audioItems = index.getAudioItems();
          for (AudioItem audioItem : audioItems) {
              audioItemCache.put(audioItem.getId(), audioItem);
          }

          Iterable<Playlist> playlists = index.getPlaylists();
          for (Playlist playlist : playlists) {
              playlistCache.put(playlist.getId(), playlist);
              for (String uid : playlist.getAudioItemList()) {
                  AudioItem audioItem = audioItemCache.get(uid);
                  if (audioItem != null) {
                      audioItem.addPlaylist(playlist);
                      audioItemCache.put(audioItem.getId(), audioItem);
                  }
              }
          }

          // This should only be called once
          loadPromptsInfo();
      } catch (IOException e) {
          throw new RuntimeException("Unable to initialize caches", e);
      }

    addDataChangeListener(new DataChangeListener() {
      @Override
      public void dataChanged(List<DataChangeEvent> events) {
        for (DataChangeEvent event : events) {
          Committable item = event.getItem();
          if (item instanceof Playlist) {
            Playlist playlist = (Playlist) item;
            if (event.getEventType() == DataChangeEventType.ITEM_DELETED) {
              playlistCache.remove(playlist.getId());
            } else {
              playlistCache.put(playlist.getId(), playlist);
            }
          }
          if (item instanceof AudioItem) {
            AudioItem audioItem = (AudioItem) item;
            if (event.getEventType() == DataChangeEventType.ITEM_DELETED) {
              audioItemCache.remove(audioItem.getId());
            } else {
              audioItemCache.put(audioItem.getId(), audioItem);
            }
          }
        }
      }
    });
  }

  @Override
  public AudioItem getAudioItem(String uid) {
    return audioItemCache.get(uid);
  }

  @Override
  public Collection<AudioItem> getAudioItems() {
    return audioItemCache.values();
  }

  @Override
  public SearchResult search(String query, List<Category> categories,
      List<Locale> locales) {
    try {
      return index.search(query, categories, locales);
    } catch (IOException e) {
      LOG.log(Level.SEVERE, "IOException while searching Lucene index.", e);
      return null;
    }
  }

  @Override
  public SearchResult search(String query, Playlist selectedPlaylist) {
    try {
      return index.search(query, selectedPlaylist);
    } catch (IOException e) {
      LOG.log(Level.SEVERE, "IOException while searching Lucene index.", e);
      return null;
    }
  }

  @Override
  public Playlist newPlaylist(String name) {
    return index.newPlaylist(name);
  }

  @Override
  public Playlist getPlaylist(String uuid) {
    return playlistCache.get(uuid);
  }

  @Override
  public Collection<Playlist> getPlaylists() {
    return playlistCache.values();
  }

  @Override
  public Playlist findPlaylistByName(String name) {
    for (Playlist playlist : playlistCache.values()) {
      if (playlist.getName().equals(name))
        return playlist;
    }
    return null;
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
      LOG.log(Level.SEVERE,
          "IOException while starting transaction with Lucene index.", e);
      return null;
    }
  }

  @Override
  public void deleteAudioItem(String uuid) {
    final AudioItem item = getAudioItem(uuid);
    if (item == null) {
      throw new NoSuchElementException(
          "AudioItem with " + uuid + " does not exist.");
    }

    item.delete();
  }

  @Override
  public void deletePlaylist(String uuid) {
    final Playlist playlist = getPlaylist(uuid);
    if (playlist == null) {
      throw new NoSuchElementException(
          "Playlist with " + uuid + " does not exist.");
    }

    playlist.delete();
  }

  @Override
  public AudioItem newAudioItem(String uid) {
    return new AudioItem(uid);
  }

    @Override
    public void newAudioItem(AudioItem audioItem) {
        audioItemCache.put(audioItem.getId(), audioItem);
    }
}
