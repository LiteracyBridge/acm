package org.literacybridge.acm.rcp.views.adapters;

import org.literacybridge.acm.api.IDataRequestResult;
import org.literacybridge.acm.categories.Taxonomy.Category;

public interface ICategoryResultContainer {

	public abstract Category getCategory();

	public abstract IDataRequestResult GetDataRequestResult();

}