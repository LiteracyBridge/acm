package org.literacybridge.acm.core;

import java.util.List;
import java.util.Locale;

import org.literacybridge.acm.api.IDataRequestResult;
import org.literacybridge.acm.api.IDataRequestService;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.config.DBConfiguration;
import org.literacybridge.acm.index.AudioItemIndex;
import org.literacybridge.acm.store.Category;
import org.literacybridge.acm.store.MetadataStore;
import org.literacybridge.acm.store.Playlist;

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
        MetadataStore store = ACMConfiguration.getCurrentDB().getMetadataStore();
        return store.search(filterString, categories, locales);
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
        MetadataStore store = ACMConfiguration.getCurrentDB().getMetadataStore();
        return store.search(filterString, selectedPlaylist);
    }

    private AudioItemIndex getAudioItemIndex() {
        DBConfiguration db = ACMConfiguration.getCurrentDB();
        if (db != null) {
            return db.getAudioItemIndex();
        }
        return null;
    }
}
