package org.literacybridge.acm.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.literacybridge.acm.api.IDataRequestResult;
import org.literacybridge.acm.api.IDataRequestService;
import org.literacybridge.acm.categories.Taxonomy;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.config.DBConfiguration;
import org.literacybridge.acm.db.PersistentAudioItem;
import org.literacybridge.acm.db.PersistentCategory;
import org.literacybridge.acm.db.PersistentLocale;
import org.literacybridge.acm.db.PersistentTag;
import org.literacybridge.acm.gui.Application;
import org.literacybridge.acm.index.AudioItemIndex;

public class DataRequestService implements IDataRequestService {
	private static final IDataRequestService instance = new DataRequestService();

	private DataRequestService() {
	}

	public static IDataRequestService getInstance() {
		return instance;
	}

	/* (non-Javadoc)
	 * @see main.java.org.literacybridge.acm.api.IDataRequestService#getData()
	 */
	public IDataRequestResult getData(Locale locale) {
		return getData(locale, "", null);
	}

	/* (non-Javadoc)
	 * @see main.java.org.literacybridge.acm.api.IDataRequestService#getData(java.lang.String)
	 */
	public IDataRequestResult getData(Locale locale, String filterString, List<PersistentCategory> categories, List<PersistentLocale> locales) {
		AudioItemIndex index = getAudioItemIndex();
		if (index != null) {
			try {
				return index.search(filterString, categories, locales);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		// Couldn't get results from Lucene index - fall back to DB
		Collection<PersistentAudioItem> items = PersistentAudioItem.getFromDatabaseBySearch(filterString, categories, locales);
		Map<Integer, Integer> facetCounts = Taxonomy.getFacetCounts(filterString, categories, locales);
		List<String> audioItems = new ArrayList<String>(items.size());
		for (PersistentAudioItem item : items) {
			audioItems.add(item.getUuid());
		}

		Map<String, Integer> languageFacetCounts = PersistentLocale.getFacetCounts(filterString, categories, locales);

		Taxonomy taxonomy = Taxonomy.getTaxonomy();
		DataRequestResult result = new DataRequestResult(taxonomy.getRootCategory(), facetCounts, languageFacetCounts, audioItems,
				PersistentTag.getFromDatabase());
		return result;
	}

	@Override
	public IDataRequestResult getData(Locale locale,
			List<PersistentCategory> filterCategories, List<PersistentLocale> locales) {
		return getData(locale, null, filterCategories, locales);
	}

	@Override
	public IDataRequestResult getData(Locale locale, PersistentTag selectedTag) {
		return getData(locale, "", selectedTag);
	}

	@Override
	public IDataRequestResult getData(Locale locale, String filterString,
			PersistentTag selectedTag) {
		AudioItemIndex index = getAudioItemIndex();
		if (index != null) {
			try {
				return index.search(filterString, selectedTag);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		// Couldn't get results from Lucene index - fall back to DB
		Collection<PersistentAudioItem> items = PersistentAudioItem.getFromDatabaseBySearch(filterString, selectedTag);
		Map<Integer, Integer> facetCounts = Taxonomy.getFacetCounts(filterString, null, null);
		List<String> audioItems = new ArrayList<String>(items.size());
		for (PersistentAudioItem item : items) {
			audioItems.add(item.getUuid());
		}

		Map<String, Integer> languageFacetCounts = PersistentLocale.getFacetCounts(filterString, null, null);

		Taxonomy taxonomy = Taxonomy.getTaxonomy();
		DataRequestResult result = new DataRequestResult(taxonomy.getRootCategory(), facetCounts, languageFacetCounts, audioItems,
				PersistentTag.getFromDatabase());
		return result;
	}

	private AudioItemIndex getAudioItemIndex() {
		DBConfiguration db = ACMConfiguration.getInstance().getCurrentDB();
		if (db != null) {
			return db.getAudioItemIndex();
		}
		return null;
	}
}
