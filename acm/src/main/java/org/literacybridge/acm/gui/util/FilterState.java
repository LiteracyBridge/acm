package org.literacybridge.acm.gui.util;

import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.gui.Application;
import org.literacybridge.acm.store.Category;
import org.literacybridge.acm.store.MetadataStore;
import org.literacybridge.acm.store.Playlist;
import org.literacybridge.acm.store.SearchResult;

import javax.swing.SwingUtilities;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FilterState {
    private String previousFilterState = null;

    private String filterString;
    private List<Category> filterCategories = new ArrayList<>();
    private List<Locale> filterLanguages = new ArrayList<>();
    private Playlist selectedPlaylist;

    public synchronized String getFilterString() {
        return filterString;
    }

    public synchronized void setFilterString(String filterString) {
        this.filterString = filterString;
        updateResult();
    }

    public synchronized List<Category> getFilterCategories() {
        return filterCategories;
    }

    public synchronized void setFilterCategories(
        List<Category> filterCategories) {
        this.filterCategories = filterCategories;
        updateResult();
    }

    public synchronized List<Locale> getFilterLanguages() {
        return filterLanguages;
    }

    public synchronized void setFilterLanguages(List<Locale> filterLanguages) {
        this.filterLanguages = filterLanguages;
        updateResult();
    }

    public synchronized void setSelectedPlaylist(Playlist selectedPlaylist) {
        this.selectedPlaylist = selectedPlaylist;
        updateResult();
    }

    public synchronized Playlist getSelectedPlaylist() {
        return selectedPlaylist;
    }

    public void updateResult() {
        updateResult(false);
    }

    public void updateResult(boolean force) {
        if (!force && previousFilterState != null
            && previousFilterState.equals(this.toString())) {
            return;
        }

        previousFilterState = this.toString();

        final MetadataStore store = ACMConfiguration.getInstance().getCurrentDB()
            .getMetadataStore();
        final SearchResult result;

        if (selectedPlaylist == null) {
            result = store.search(filterString, filterCategories, filterLanguages);
        } else {
            result = store.search(filterString, selectedPlaylist);
        }

        // call UI back
        Runnable updateUI = () -> Application.getMessageService().pumpMessage(result);

        if (SwingUtilities.isEventDispatchThread()) {
            updateUI.run();
        } else {
            try {
                SwingUtilities.invokeAndWait(updateUI);
            } catch (InterruptedException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        if (filterString != null) {
            builder.append("FS:").append(filterString);
            builder.append(",");
        }
        if (filterCategories != null && !filterCategories.isEmpty()) {
            for (Category cat : filterCategories) {
                builder.append("FC:").append(cat.getId());
                builder.append(",");
            }
        }
        if (filterLanguages != null && !filterLanguages.isEmpty()) {
            for (Locale lang : filterLanguages) {
                builder.append("FL:").append(lang.getLanguage()).append("-")
                    .append(lang.getCountry());
                builder.append(",");
            }
        }
        if (selectedPlaylist != null) {
            builder.append("ST:").append(selectedPlaylist.getName());
            builder.append(",");
        }
        return builder.toString();
    }
}
