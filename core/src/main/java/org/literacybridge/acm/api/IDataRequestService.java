package org.literacybridge.acm.api;

import java.util.List;
import java.util.Locale;

import org.literacybridge.acm.db.PersistentCategory;

public interface IDataRequestService {

	public abstract IDataRequestResult getData(Locale locale);

	public abstract IDataRequestResult getData(Locale locale, String filterString, List<PersistentCategory> filterCategories);

	public abstract IDataRequestResult getData(Locale locale, List<PersistentCategory> filterCategories);
}