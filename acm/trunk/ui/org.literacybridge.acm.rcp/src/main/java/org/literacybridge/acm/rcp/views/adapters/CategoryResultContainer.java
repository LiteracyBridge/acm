package org.literacybridge.acm.rcp.views.adapters;

import org.literacybridge.acm.api.IDataRequestResult;
import org.literacybridge.acm.categories.Taxonomy.Category;

public class CategoryResultContainer implements ICategoryResultContainer {
	private Category category = null;
	private IDataRequestResult result = null;	// where does this category come from...
	
	public CategoryResultContainer(Category category, IDataRequestResult result) {
		this.category = category;
		this.result = result;
	}
	
	/* (non-Javadoc)
	 * @see org.literacybridge.acm.rcp.views.category.helpers.ITreeNodeAdapter#getCategory()
	 */
	public Category getCategory() {
		return category;
	}
	
	/* (non-Javadoc)
	 * @see org.literacybridge.acm.rcp.views.category.helpers.ITreeNodeAdapter#GetDataRequestResult()
	 */
	public IDataRequestResult GetDataRequestResult() {
		return result;
	}
}
