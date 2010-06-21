package org.literacybridge.acm.content;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.literacybridge.acm.categories.Taxonomy.Category;
import org.literacybridge.acm.db.Persistable;
import org.literacybridge.acm.db.PersistentAudioItem;
import org.literacybridge.acm.db.PersistentCategory;
import org.literacybridge.acm.db.PersistentLocale;
import org.literacybridge.acm.db.PersistentLocalizedAudioItem;

/**
 * An AudioItem is a unique audio entity, identified by its audioItemID.
 * It is optionally associated with one or more categories and must have
 * at least one {@link LocalizedAudioItem}. If multiple translations of content
 * represented by this AudioItem are available, then there must an instance
 * of {@link LocalizedAudioItem} per translation referenced by this AudioItem.
 *  
 */
public class AudioItem implements Persistable {
    
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
	
	public AudioItem(String uuId, Category... categories) {
            this(uuId);
			for (Category c : categories) {
                addCategory(c);
			}
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
            mItem.addPersistentAudioItemCategory(category.getPersistentObject());
	}
	
	public List<Category> getCategoryList() {
            List<Category> categories = new LinkedList<Category>();
            for (PersistentCategory c : mItem.getPersistentCategoryList()) {
                categories.add(new Category(c));
	}
            return categories;
	}
	
	public LocalizedAudioItem getLocalizedAudioItem(Locale locale) {
            for (PersistentLocalizedAudioItem item : mItem.getPersistentLocalizedAudioItems()) {
                PersistentLocale l = item.getPersistentLocale();
                if ((locale.getCountry().equals(l.getCountry()) && 
                    (locale.getLanguage().equals(l.getLanguage())))) {
                    return new LocalizedAudioItem(item);         
	}
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
        
    public static List<AudioItem> getFromDatabaseBySearch(String searchFilter, List<PersistentCategory> categories) {
        List<AudioItem> results = new LinkedList<AudioItem>();
    	List<PersistentAudioItem> items = PersistentAudioItem.getFromDatabaseBySearch(searchFilter, categories);
        for (PersistentAudioItem item : items) {
        	results.add(new AudioItem(item));
        }
        return results;
    }
    
    public static List<AudioItem> getFromDatabaseByCategory(List<Category> categories) {
    	List<PersistentCategory> pCategories = new LinkedList<PersistentCategory>();
    	for (Category category : categories) {
    		pCategories.add(category.getPersistentObject());
    	}
    	List<PersistentAudioItem> items = PersistentAudioItem.getFromDatabaseByCategory(pCategories);
        List<AudioItem> results = new LinkedList<AudioItem>();    	
        for (PersistentAudioItem item : items) {
        	results.add(new AudioItem(item));
        }
        return results;
    }    
}
