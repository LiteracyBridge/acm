package org.literacybridge.acm.content;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Logger;

import org.literacybridge.acm.categories.Taxonomy.Category;
import org.literacybridge.acm.db.Persistable;
import org.literacybridge.acm.db.PersistentAudioItem;
import org.literacybridge.acm.db.PersistentCategory;
import org.literacybridge.acm.db.PersistentLocale;
import org.literacybridge.acm.db.PersistentLocalizedAudioItem;
import org.literacybridge.acm.db.PersistentTag;

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

    public AudioItem() {
        mItem = new PersistentAudioItem();
    }

    public AudioItem(PersistentAudioItem item) {
        mItem = item;
    }
	
	public AudioItem(String uuID) {
        this();
        mItem.setUuid(uuID);
	}
	
    public Integer getId() {
        return mItem.getId();
    }
    
    public String getUuid() {
        return mItem.getUuid();
    }
    
    public void setUuid(String uuid) {
        mItem.setUuid(uuid);
    }
        
	public void addCategory(Category category) {
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
			if (!mItem.hasPersistentAudioItemCategory(category.getPersistentObject())) {
				mItem.addPersistentAudioItemCategory(category.getPersistentObject());
			}
			category = category.getParent();
		} while (category != null);        
	}
	
	public boolean hasCategory(Category category) {
		return mItem.hasPersistentAudioItemCategory(category.getPersistentObject());
	}
	
	public void addTag(PersistentTag tag) {
		mItem.addPersistentAudioItemTag(tag);
	}
	
	public boolean hasTag(PersistentTag tag) {
		return mItem.hasPersistentAudioItemTag(tag);
	}
	
	public void removeTag(PersistentTag tag) {
		mItem.removePersistentTag(tag);
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
	
	public List<PersistentTag> getPlaylists() {
		return mItem.getPersistentTagList();
	}

	
	public LocalizedAudioItem getLocalizedAudioItem(Locale locale) {
		
			// TODO local will be ignored for beta 2
		
//            for (PersistentLocalizedAudioItem item : mItem.getPersistentLocalizedAudioItems()) {
//                PersistentLocale l = item.getPersistentLocale();
//                if ((locale.getCountry().equals(l.getCountry()) && 
//                    (locale.getLanguage().equals(l.getLanguage())))) {
//                    return new LocalizedAudioItem(item);         
//                }
//            }
//            return null;
		
			if (mItem.getPersistentLocalizedAudioItems() != null) {
				PersistentLocalizedAudioItem localizedItem = mItem.getPersistentLocalizedAudioItems().iterator().next();
				return new LocalizedAudioItem(localizedItem);
			}
			return null;
	}
	
	public Set<Locale> getAvailableLocalizations() {
        Set<Locale> results = new LinkedHashSet<Locale>();
        for (PersistentLocalizedAudioItem i : mItem.getPersistentLocalizedAudioItems()) {
            PersistentLocale locale = i.getPersistentLocale();
            results.add(new Locale(locale.getLanguage(),locale.getCountry()));
        }
        return results;
	}
	
	public void addLocalizedAudioItem(LocalizedAudioItem localizedAudioItem) {
        mItem.addPersistentLocalizedAudioItem(localizedAudioItem.getPersistentObject());
	}

    public void removeLocalizedAudioItem(LocalizedAudioItem localizedAudioItem) {
        mItem.removePersistentLocalizedAudioItem(localizedAudioItem.getPersistentObject());            
    }

    @SuppressWarnings("unchecked")
	public AudioItem commit() {
        mItem = mItem.<PersistentAudioItem>commit();
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
        List<AudioItem> results = new LinkedList<AudioItem>();
    	List<PersistentAudioItem> items = PersistentAudioItem.getFromDatabaseBySearch(searchFilter, categories, locales);
        for (PersistentAudioItem item : items) {
        	results.add(new AudioItem(item));
        }
        return results;
    }    
}
