package org.literacybridge.acm.content;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.literacybridge.acm.categories.Taxonomy.Category;

/**
 * An AudioItem is a unique audio entity, identified by its audioItemID.
 * It is optionally associated with one or more categories and must have
 * at least one {@link LocalizedAudioItem}. If multiple translations of content
 * represented by this AudioItem are available, then there must an instance
 * of {@link LocalizedAudioItem} per translation referenced by this AudioItem.
 *  
 */
public class AudioItem {
        /** Database ID **/
        private int mID;
    
	/** Multiple translations of the same content share this ID */
	private final String mUUID;
	
	/** The categories this AudioItem belongs to */
	private Set<Category> categories;
	
	/** All available localized versions of this audio item */
	private Map<Locale, LocalizedAudioItem> localizedItems;
	
	public AudioItem(String uuID) {
		this(uuID, (Category[]) null);
	}
	
	public AudioItem(String uuId, Category... categories) {
		this.mUUID = uuId;
		this.categories = new HashSet<Category>();
		this.localizedItems = new HashMap<Locale, LocalizedAudioItem>();
		
		if (categories != null) {
			for (Category c : categories) {
				this.categories.add(c);
			}
		}
	}
	
        public int getId() {
            return mID;
        }
        
        public void setId(int id) {
            mID = id;
        }
        
        public String getUUId() {
            return mUUID;
        }
        
	public void addCategory(Category category) {
		this.categories.add(category);
	}
	
	public Set<Category> getCategories() {
		return this.categories;
	}
	
	public LocalizedAudioItem getLocalizedAudioItem(Locale locale) {
		return this.localizedItems.get(locale);
	}
	
	public Set<Locale> getAvailableLocalizations() {
		return this.localizedItems.keySet();
	}
	
	public void addLocalizedAudioItem(Locale locale, LocalizedAudioItem localizedAudioItem) {
		this.localizedItems.put(locale, localizedAudioItem);
	}
}
