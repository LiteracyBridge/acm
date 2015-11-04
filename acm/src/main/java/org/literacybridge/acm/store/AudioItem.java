package org.literacybridge.acm.store;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * An AudioItem is a unique audio entity, identified by its audioItemID.
 * It is optionally associated with one or more categories and must have
 * at least one {@link LocalizedAudioItem}. If multiple translations of content
 * represented by this AudioItem are available, then there must an instance
 * of {@link LocalizedAudioItem} per translation referenced by this AudioItem.
 *
 */
public abstract class AudioItem implements Persistable {
    private final String uid;
    protected final Map<String, Category> categories;

    public AudioItem(String uid) {
        this.uid = uid;
        this.categories = Maps.newHashMap();
    }

    public final String getUuid() {
        return uid;
    }

    public abstract Metadata getMetadata();

    public abstract String getRevision();

    public final void addCategory(Category category) {
        if (hasCategory(category)) {
            return;
        }

        // first we check if a leaf category is added (which should always be the case),
        // otherwise we find an appropriate leaf
        if (category.hasChildren()) {
            do {
                // always pick the first child, which usually is the 'general' child category
                category = category.getSortedChildren().get(0);
            } while (category.hasChildren());
        }

        // make sure all parents up to the root are added as well
        do {
            if (!hasCategory(category)) {
                categories.put(category.getUuid(), category);
            }
            category = category.getParent();
        } while (category != null);
    }

    @Deprecated
    protected final void addCategoriesDirectly(Collection<Category> cats) {
        for (Category cat : cats) {
            categories.put(cat.getUuid(), cat);
        }
    }

    public final boolean hasCategory(Category category) {
        return categories.containsKey(category.getUuid());
    }

    public final void removeCategory(Category category) {
        categories.remove(category.getUuid());

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
            categories.remove(cat.getUuid());
        }

        return !toRemove.isEmpty();
    }

    public final void removeAllCategories() {
        categories.clear();
    }

    public final Iterable<Category> getCategoryList() {
        return categories.values();
    }

    public final Iterable<Category> getCategoryLeavesList() {
        return Iterables.filter(getCategoryList(), new Predicate<Category>() {
            @Override public boolean apply(Category cat) {
                return !cat.hasChildren();
            }
        });
    }

    public abstract void addPlaylist(Playlist playlist);
    public abstract boolean hasPlaylist(Playlist playlist);
    public abstract void removePlaylist(Playlist playlist);
    public abstract Iterable<Playlist> getPlaylists();
}
