package org.literacybridge.acm.db;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.persistence.EntityManager;

import org.literacybridge.acm.db.Taxonomy.Category;
import org.literacybridge.acm.metadata.Metadata;
import org.literacybridge.acm.metadata.MetadataSpecification;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * An AudioItem is a unique audio entity, identified by its audioItemID.
 * It is optionally associated with one or more categories and must have
 * at least one {@link LocalizedAudioItem}. If multiple translations of content
 * represented by this AudioItem are available, then there must an instance
 * of {@link LocalizedAudioItem} per translation referenced by this AudioItem.
 *
 */
public class AudioItem implements Persistable {
    private static final Logger LOG = Logger.getLogger(AudioItem.class.getName());

    private PersistentAudioItem mItem;

    public AudioItem(PersistentAudioItem item) {
        mItem = item;
    }

    public AudioItem(String uuid) {
        mItem = new PersistentAudioItem();
        mItem.setUuid(uuid);
    }

    public String getUuid() {
        return mItem.getUuid();
    }

    public void setUuid(String uuid) {
        mItem.setUuid(uuid);
    }

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
                mItem.addPersistentAudioItemCategory(category.getPersistentObject());
            }
            category = category.getParent();
        } while (category != null);
    }

    public boolean hasCategory(Category category) {
        return mItem.hasPersistentAudioItemCategory(category.getPersistentObject());
    }

    public void addPlaylist(Playlist playlist) {
        mItem.addPersistentAudioItemTag(playlist.getTag());
    }

    public boolean hasPlaylist(Playlist playlist) {
        return mItem.hasPersistentAudioItemTag(playlist.getTag());
    }

    public void removePlaylist(Playlist playlist) {
        mItem.removePersistentTag(playlist.getTag());
    }

    public void removeCategory(Category category) {
        mItem.removePersistentCategory(category.getPersistentObject());

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
            mItem.removePersistentCategory(cat.getPersistentObject());
        }

        return !toRemove.isEmpty();
    }

    public void removeAllCategories() {
        mItem.removeAllPersistentCategories();
    }

    public List<Category> getCategoryList() {
        List<Category> categories = new LinkedList<Category>();
        for (PersistentCategory c : mItem.getPersistentCategoryList()) {
            categories.add(new Category(c));
        }
        return categories;
    }

    public List<Category> getCategoryLeavesList() {
        List<Category> categories = new LinkedList<Category>();
        for (PersistentCategory c : mItem.getPersistentCategoryList()) {
            Category cat = new Category(c);
            if (!cat.hasChildren()) {
                categories.add(cat);
            }
        }
        return categories;
    }

    public PersistentAudioItem getPersistentAudioItem() {
        return mItem;
    }

    public Collection<Playlist> getPlaylists() {
        return Playlist.toPlaylists(mItem.getPersistentTagList());
    }

    public Metadata getMetadata() {
        return new Metadata(mItem.getPersistentLocalizedAudioItem().getPersistentMetadata());
    }

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

    public static List<AudioItem> getFromDatabase() {
        return Lists.transform(PersistentAudioItem.getFromDatabase(), new Function<PersistentAudioItem, AudioItem>() {
            @Override public AudioItem apply(PersistentAudioItem item) {
                return new AudioItem(item);
            }
        });
    }

    public static AudioItem getFromDatabase(String uuid) {
        PersistentAudioItem item = PersistentAudioItem.getFromDatabase(uuid);
        if (item == null) {
            return null;
        }
        return new AudioItem(item);
    }

    public static AudioItem getFromDatabase(int id) {
        PersistentAudioItem item = PersistentAudioItem.getFromDatabase(id);
        if (item == null) {
            return null;
        }
        return new AudioItem(item);
    }

    public static List<AudioItem> getFromDatabaseBySearch(String searchFilter,
            List<PersistentCategory> categories, List<PersistentLocale> locales) {
        return toAudioItemList(PersistentQueries.searchForAudioItems(searchFilter, categories, locales));
    }

    public static List<AudioItem> getFromDatabaseBySearch(String searchFilter,
            Playlist selectedTag) {
        return toAudioItemList(PersistentQueries.searchForAudioItems(searchFilter, selectedTag));
    }

    public static List<AudioItem> toAudioItemList(List<PersistentAudioItem> list) {
        List<AudioItem> results = new LinkedList<AudioItem>();
        for (PersistentAudioItem item : list) {
            results.add(new AudioItem(item));
        }
        return results;
    }

}
