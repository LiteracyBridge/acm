package org.literacybridge.acm.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.literacybridge.acm.api.IDataRequestResult;
import org.literacybridge.acm.api.IDataRequestService;
import org.literacybridge.acm.categories.Taxonomy;
import org.literacybridge.acm.content.AudioItem;
import org.literacybridge.acm.db.PersistentAudioItem;
import org.literacybridge.acm.db.PersistentCategory;

public class DataRequestService implements IDataRequestService {
	private static final IDataRequestService instance = new DataRequestService();
	
	private DataRequestService() {
		// singleton
	}
	
	public static IDataRequestService getInstance() {
		return instance;
	}
	
	/* (non-Javadoc)
	 * @see main.java.org.literacybridge.acm.api.IDataRequestService#getData()
	 */
	public IDataRequestResult getData(Locale locale) {
		Collection<PersistentAudioItem> items = PersistentAudioItem.getFromDatabase();
		return toResult(items);
	}

	/* (non-Javadoc)
	 * @see main.java.org.literacybridge.acm.api.IDataRequestService#getData(java.lang.String)
	 */
	public IDataRequestResult getData(Locale locale, String filterString, List<PersistentCategory> categories) {
		Collection<PersistentAudioItem> items = PersistentAudioItem.getFromDatabaseBySearch(filterString, categories);
		return toResult(items);
	}

	@Override
	public IDataRequestResult getData(Locale locale,
			List<PersistentCategory> filterCategories) {
		Collection<PersistentAudioItem> items = PersistentAudioItem.getFromDatabaseByCategory(filterCategories);
		return toResult(items);
	}
	
	private IDataRequestResult toResult(Collection<PersistentAudioItem> items) {
		List<AudioItem> audioItems = new ArrayList<AudioItem>(items.size());
		for (PersistentAudioItem item : items) {
			audioItems.add(new AudioItem(item));
		}
		
		Taxonomy taxonomy = Taxonomy.getTaxonomy();
		DataRequestResult result = new DataRequestResult(taxonomy.getRootCategory(), null, audioItems);
		return result;
	}
}
