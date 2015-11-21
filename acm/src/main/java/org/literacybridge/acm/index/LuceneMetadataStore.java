package org.literacybridge.acm.index;

import java.io.File;
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

public class LuceneMetadataStore extends MetadataStore {
    private static final Logger LOG = Logger.getLogger(LuceneMetadataStore.class.getName());

    private final AudioItemIndex index;

    public LuceneMetadataStore(File acmDirectory, AudioItemIndex index) {
        super(acmDirectory);
        this.index = index;
    }

    @Override
    public AudioItem newAudioItem(String uid) {
        return new AudioItem(uid);
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
    public Transaction newTransaction() {
        try {
            return new Transaction(index);
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "IOException while starting transaction with Lucene index.", e);
            return null;
        }
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
