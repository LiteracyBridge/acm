package org.literacybridge.acm.api;

import java.util.List;

import org.literacybridge.acm.categories.Taxonomy.Category;
import org.literacybridge.acm.content.AudioItem;

/**
 * This is the result object returned by {@link IDataRequestService} containing all necessary
 * data to render the UI.
 *
 */
public interface IDataRequestResult {
	/**
	 * Returns the root category which should be used to walk
	 * the tree of categories. The root category is a virtual root 
	 * and should generally not be displayed by the UI.
	 */
	public abstract Category getRootCategory();

	/**
	 * Returns the facet count for the passed-in category. 
	 */
	public abstract int getFacetCount(Category category);

	/**
	 * Returns the facet count for the passed-in language. 
	 */
	public abstract int getLanguageFacetCount(String languageCode);
	
	/**
	 * Returns the list of AudioItems to be displayed. 
	 */
	public abstract List<AudioItem> getAudioItems();

}