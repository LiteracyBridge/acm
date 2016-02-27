package org.literacybridge.acm.core;

import java.util.List;
import java.util.Map;

import org.literacybridge.acm.api.IDataRequestResult;
import org.literacybridge.acm.store.Category;

public class DataRequestResult implements IDataRequestResult {
    private final Map<String, Integer> facetCounts;
    private final Map<String, Integer> languageFacetCounts;
    private final List<String> audioItems;

    public DataRequestResult(Map<String, Integer> facetCounts,
            Map<String, Integer> languageFacetCounts, List<String> audioItems) {
        this.facetCounts = facetCounts;
        this.languageFacetCounts = languageFacetCounts;
        this.audioItems = audioItems;
    }

    /* (non-Javadoc)
     * @see main.java.org.literacybridge.acm.api.IDataRequestResult#getFacetCount(main.java.org.literacybridge.acm.categories.Taxonomy.Category)
     */
    public int getFacetCount(Category category) {
        if (category == null) {
            return 0;
        }
        Integer count = facetCounts.get(category.getUuid());
        if (count == null) {
            return 0;
        } else {
            return count;
        }
    }

    /* (non-Javadoc)
     * @see main.java.org.literacybridge.acm.api.IDataRequestResult#getAudioItems()
     */
    public List<String> getAudioItems() {
        return audioItems;
    }

    @Override
    public int getLanguageFacetCount(String languageCode) {
        if (languageCode == null) {
            return 0;
        }
        Integer count = languageFacetCounts.get(languageCode);
        if (count == null) {
            return 0;
        } else {
            return count;
        }
    }
}
