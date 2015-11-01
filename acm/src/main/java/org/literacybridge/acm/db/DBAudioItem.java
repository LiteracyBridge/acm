package org.literacybridge.acm.db;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.persistence.EntityManager;

import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.store.Category;
import org.literacybridge.acm.store.Metadata;
import org.literacybridge.acm.store.MetadataSpecification;
import org.literacybridge.acm.store.Playlist;

import com.google.common.collect.Sets;

final class DBAudioItem extends AudioItem {
    private static final Logger LOG = Logger.getLogger(DBAudioItem.class.getName());

    private PersistentAudioItem mItem;

    public DBAudioItem(PersistentAudioItem item) {
        mItem = item;
    }

    public DBAudioItem(String uuid) {
        mItem = new PersistentAudioItem();
        mItem.setUuid(uuid);
    }

    @Override
    public String getUuid() {
        return mItem.getUuid();
    }

    @Override
    public void setUuid(String uuid) {
        mItem.setUuid(uuid);
    }

    @Override
    public void addCategory(Category category) {
        if (hasCategory(category)) {
            return;
        }

        // first we check if a leaf category is added (which should always be the case),
        // otherwise we find an appropriate leaf
        if (category.hasChildren()) {
            LOG.warning("Adding non-leaf category " + category.getUuid() + " to audioitem " + getUuid());
            do {
                // always pick the first child, which usually is the 'general' child category
                category = category.getSortedChildren().get(0);
            } while (category.hasChildren());
        }

        // make sure all parents up to the root are added as well
        do {
            if (!hasCategory(category)) {
                mItem.addPersistentAudioItemCategory(((DBCategory) category).getPersistentObject());
            }
            category = category.getParent();
        } while (category != null);
    }

    @Override
    public boolean hasCategory(Category category) {
        return mItem.hasPersistentAudioItemCategory(((DBCategory) category).getPersistentObject());
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

    @Override
    public void removeCategory(Category category) {
        mItem.removePersistentCategory(((DBCategory) category).getPersistentObject());

        // remove orphaned non-leaves
        while (removeOrphanedNonLeafCategories());
    }

    // returns true, if any categories were removed
    private boolean removeOrphanedNonLeafCategories() {
        Set<Category> toRemove = Sets.newHashSet();
        for (Category cat : getCategoryList()) {
            if (cat.hasChildren()) {
                toRemove.add(cat);
            }
        }
        // now 'toRemove' contains all non-leaf categories that this audioitem contains


        for (Category cat : getCategoryList()) {
            if (cat.getParent() != null) {
                toRemove.remove(cat.getParent());
            }
        }
        // now 'toRemove' only contains categories for which this audioitem has at least one child

        for (Category cat : toRemove) {
            mItem.removePersistentCategory(((DBCategory) cat).getPersistentObject());
        }

        return !toRemove.isEmpty();
    }

    @Override
    public void removeAllCategories() {
        mItem.removeAllPersistentCategories();
    }

    @Override
    public List<Category> getCategoryList() {
        List<Category> categories = new LinkedList<Category>();
        for (PersistentCategory c : mItem.getPersistentCategoryList()) {
            categories.add(new DBCategory(c));
        }
        return categories;
    }

    @Override
    public List<Category> getCategoryLeavesList() {
        List<Category> categories = new LinkedList<Category>();
        for (PersistentCategory c : mItem.getPersistentCategoryList()) {
            Category cat = new DBCategory(c);
            if (!cat.hasChildren()) {
                categories.add(cat);
            }
        }
        return categories;
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
        mItem = mItem.<PersistentAudioItem>commit(em);
        return this;
    }

    public void destroy() {
        mItem.destroy();
    }

    @SuppressWarnings("unchecked")
    public AudioItem refresh() {
        mItem = mItem.<PersistentAudioItem>refresh();
        return this;
    }
}
