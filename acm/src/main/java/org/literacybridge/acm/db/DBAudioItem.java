package org.literacybridge.acm.db;

import java.io.IOException;
import java.util.Collection;

import javax.persistence.EntityManager;

import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.config.DBConfiguration;
import org.literacybridge.acm.gui.AudioItemCache;
import org.literacybridge.acm.index.AudioItemIndex;
import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.store.Category;
import org.literacybridge.acm.store.Metadata;
import org.literacybridge.acm.store.MetadataSpecification;
import org.literacybridge.acm.store.Playlist;

/**
 * @deprecated: We're removing Derby DB from the ACM and are switching to a Lucene index
 *              for storing and searching all metadata.
 */
@Deprecated
final class DBAudioItem extends AudioItem {
    private PersistentAudioItem mItem;

    public DBAudioItem(PersistentAudioItem item) {
        super(item.getUuid());
        mItem = item;
        refresh();
    }

    public DBAudioItem(String uuid) {
        super(uuid);
        mItem = new PersistentAudioItem();
        mItem.setUuid(uuid);
    }

    @Override
    public void addPlaylist(Playlist playlist) {
        mItem.addPersistentAudioItemTag(((DBPlaylist) playlist).getTag());
    }

    @Override
    public boolean hasPlaylist(Playlist playlist) {
        return mItem.hasPersistentAudioItemTag(((DBPlaylist) playlist).getTag());
    }

    @Override
    public void removePlaylist(Playlist playlist) {
        mItem.removePersistentTag(((DBPlaylist) playlist).getTag());
    }

    public PersistentAudioItem getPersistentAudioItem() {
        return mItem;
    }

    @Override
    public Collection<Playlist> getPlaylists() {
        return DBPlaylist.toPlaylists(mItem.getPersistentTagList());
    }

    @Override
    public Metadata getMetadata() {
        return new DBMetadata(mItem.getPersistentLocalizedAudioItem().getPersistentMetadata());
    }

    @Override
    public String getRevision() {
        return getMetadata().getMetadataValues(MetadataSpecification.DTB_REVISION).get(0).getValue();
    }

    @SuppressWarnings("unchecked")
    public AudioItem commit() {
        return commit(null);
    }

    @SuppressWarnings("unchecked")
    public AudioItem commit(EntityManager em) {
        // add all categories from in-memory list to DB
        mItem.removeAllPersistentCategories();
        for (Category cat : getCategoryList()) {
            mItem.addPersistentAudioItemCategory(PersistentCategory.getFromDatabase(cat.getUuid()));
        }

        mItem = mItem.<PersistentAudioItem>commit(em);
        DBConfiguration db = ACMConfiguration.getCurrentDB();
        if (db != null) {
            AudioItemIndex index = db.getAudioItemIndex();
            if (index != null) {
                try {
                    index.updateAudioItem(this);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            AudioItemCache cache = db.getAudioItemCache();
            if (cache != null) {
                cache.invalidate(getUuid());
            }
        }


        return this;
    }

    public void destroy() {
        mItem.destroy();
    }

    @SuppressWarnings("unchecked")
    public AudioItem refresh() {
        mItem = mItem.<PersistentAudioItem>refresh();

        // add all categories from DB to in-memory list
        removeAllCategories();
        for (PersistentCategory cat : mItem.getPersistentCategoryList()) {
            this.categories.put(cat.getUuid(), new DBCategory(cat));
        }
        return this;
    }
}
