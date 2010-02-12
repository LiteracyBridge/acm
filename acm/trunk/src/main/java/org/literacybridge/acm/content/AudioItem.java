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
	/** Multiple translations of the same content share this ID */
	private final String audioItemID;
	
	/** The categories this AudioItem belongs to */
	private Set<Category> categories;
	
	/** All available localized versions of this audio item */
	private Map<Locale, LocalizedAudioItem> localizedItems;
	
	public AudioItem(String id) {
		this(id, (Category[]) null);
	}
	
	public AudioItem(String id, Category... categories) {
		this.audioItemID = id;
		this.categories = new HashSet<Category>();
		this.localizedItems = new HashMap<Locale, LocalizedAudioItem>();
		
		if (categories != null) {
			for (Category c : categories) {
				this.categories.add(c);
			}
		}
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
	
	public void addLocalizedAudioItem(Locale locale, LocalizedAudioItem localizedAudioItem) {
		this.localizedItems.put(locale, localizedAudioItem);
	}
	
	public String getAudioItemId() {
		return this.audioItemID;
	}
}
