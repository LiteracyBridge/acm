package org.literacybridge.acm.core;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.literacybridge.acm.api.IDataRequestResult;
import org.literacybridge.acm.categories.Taxonomy.Category;
import org.literacybridge.acm.content.AudioItem;
import org.literacybridge.acm.metadata.MetadataSpecification;

public class DataRequestResult implements IDataRequestResult {
	private final Category rootCategory;
	private final Map<Integer, Integer> facetCounts;
	private final Map<String, Integer> languageFacetCounts;
	private final List<AudioItem> audioItems;
	
	public DataRequestResult(Category rootCategory, Map<Integer, Integer> facetCounts, Map<String, Integer> languageFacetCounts, List<AudioItem> audioItems) {
		this.rootCategory = rootCategory;
		this.facetCounts = facetCounts;
		this.languageFacetCounts = languageFacetCounts;
		this.audioItems = audioItems;
	}
	
	/* (non-Javadoc)
	 * @see main.java.org.literacybridge.acm.api.IDataRequestResult#getRootCategory()
	 */
	public Category getRootCategory() {
		return this.rootCategory;
	}
	
	/* (non-Javadoc)
	 * @see main.java.org.literacybridge.acm.api.IDataRequestResult#getFacetCount(main.java.org.literacybridge.acm.categories.Taxonomy.Category)
	 */
	public int getFacetCount(Category category) {
		if (category == null) {
			return 0;
		}
		Integer count = facetCounts.get(category.getId());
		if (count == null) {
			return 0;
		} else {
			return count;
		}
	}
	
	/* (non-Javadoc)
	 * @see main.java.org.literacybridge.acm.api.IDataRequestResult#getAudioItems()
	 */
	public List<AudioItem> getAudioItems() {
		return audioItems;
	}

	@Override
	public int getLanguageFacetCount(String languageCode) {
		if (languageCode == null) {
			return 0;
		}
		Integer count = languageFacetCounts.get(languageCode);
		if (count == null) {
			return 0;
		} else {
			return count;
		}
	}
}
