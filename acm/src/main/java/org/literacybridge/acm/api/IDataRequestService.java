package org.literacybridge.acm.api;

import java.util.List;
import java.util.Locale;

import org.literacybridge.acm.db.Taxonomy.Category;
import org.literacybridge.acm.store.Playlist;

public interface IDataRequestService {

    public abstract IDataRequestResult getData(Locale locale);

    public abstract IDataRequestResult getData(Locale locale, Playlist selectedPlaylist);

    public abstract IDataRequestResult getData(Locale locale, String filterString, Playlist selectedPlaylist);

    public abstract IDataRequestResult getData(Locale locale, String filterString, List<Category> filterCategories, List<Locale> locales);

    public abstract IDataRequestResult getData(Locale locale, List<Category> filterCategories, List<Locale> locales);
}