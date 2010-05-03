package org.literacybridge.acm.core;

import java.util.List;

import org.literacybridge.acm.api.IDataRequestResult;
import org.literacybridge.acm.categories.Taxonomy.Category;
import org.literacybridge.acm.content.AudioItem;

public class DataRequestResult implements IDataRequestResult {
	private Category rootCategory;
	private int[] facetCountArray;
	private List<AudioItem> audioItems;
	
	public DataRequestResult(Category rootCategory, int[] facetCountArray, List<AudioItem> audioItems) {
		this.rootCategory = rootCategory;
		this.facetCountArray = facetCountArray;
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
                return category.getAudioItemList().size();
	}
	
	/* (non-Javadoc)
	 * @see main.java.org.literacybridge.acm.api.IDataRequestResult#getAudioItems()
	 */
	public List<AudioItem> getAudioItems() {
		return audioItems;
	}
	
	
}
