package org.literacybridge.acm.core;

import java.util.List;
import java.util.Map;

import org.literacybridge.acm.api.IDataRequestResult;
import org.literacybridge.acm.categories.Taxonomy.Category;
import org.literacybridge.acm.content.AudioItem;

public class DataRequestResult implements IDataRequestResult {
	private Category rootCategory;
	private Map<Integer, Integer> facetCounts;
	private List<AudioItem> audioItems;
	
	public DataRequestResult(Category rootCategory, Map<Integer, Integer> facetCounts, List<AudioItem> audioItems) {
		this.rootCategory = rootCategory;
		this.facetCounts = facetCounts;
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
	
	
}
