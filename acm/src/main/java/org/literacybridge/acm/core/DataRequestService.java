package org.literacybridge.acm.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.literacybridge.acm.api.IDataRequestResult;
import org.literacybridge.acm.api.IDataRequestService;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.config.DBConfiguration;
import org.literacybridge.acm.db.Playlist;
import org.literacybridge.acm.db.Taxonomy;
import org.literacybridge.acm.db.Taxonomy.Category;
import org.literacybridge.acm.index.AudioItemIndex;
import org.literacybridge.acm.store.AudioItem;

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
    public IDataRequestResult getData(Locale locale, String filterString, List<Category> categories, List<Locale> locales) {
        AudioItemIndex index = getAudioItemIndex();
        if (index != null) {
            try {
                return index.search(filterString, categories, locales);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Couldn't get results from Lucene index - fall back to DB
        Iterable<AudioItem> items = ACMConfiguration.getCurrentDB().getMetadataStore().search(filterString, categories, locales);
        Map<Integer, Integer> facetCounts = Taxonomy.getFacetCounts(filterString, categories, locales);
        List<String> audioItems = new ArrayList<String>();
        for (AudioItem item : items) {
            audioItems.add(item.getUuid());
        }

        Map<String, Integer> languageFacetCounts = Taxonomy.getLanguageFacetCounts(filterString, categories, locales);

        Taxonomy taxonomy = Taxonomy.getTaxonomy();
        DataRequestResult result = new DataRequestResult(taxonomy.getRootCategory(), facetCounts, languageFacetCounts, audioItems,
                Playlist.getFromDatabase());
        return result;
    }

    @Override
    public IDataRequestResult getData(Locale locale,
            List<Category> filterCategories, List<Locale> locales) {
        return getData(locale, null, filterCategories, locales);
    }

    @Override
    public IDataRequestResult getData(Locale locale, Playlist selectedPlaylist) {
        return getData(locale, "", selectedPlaylist);
    }

    @Override
    public IDataRequestResult getData(Locale locale, String filterString,
            Playlist selectedPlaylist) {
        AudioItemIndex index = getAudioItemIndex();
        if (index != null) {
            try {
                return index.search(filterString, selectedPlaylist);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Couldn't get results from Lucene index - fall back to DB
        Iterable<AudioItem> items = ACMConfiguration.getCurrentDB().getMetadataStore().search(filterString, selectedPlaylist);
        Map<Integer, Integer> facetCounts = Taxonomy.getFacetCounts(filterString, null, null);
        List<String> audioItems = new ArrayList<String>();
        for (AudioItem item : items) {
            audioItems.add(item.getUuid());
        }

        Map<String, Integer> languageFacetCounts = Taxonomy.getLanguageFacetCounts(filterString, null, null);

        Taxonomy taxonomy = Taxonomy.getTaxonomy();
        DataRequestResult result = new DataRequestResult(taxonomy.getRootCategory(), facetCounts, languageFacetCounts, audioItems,
                Playlist.getFromDatabase());
        return result;
    }

    private AudioItemIndex getAudioItemIndex() {
        DBConfiguration db = ACMConfiguration.getCurrentDB();
        if (db != null) {
            return db.getAudioItemIndex();
        }
        return null;
    }
}
