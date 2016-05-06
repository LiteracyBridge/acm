package org.literacybridge.acm.store;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Sets;

/**
 * This object contains the results of a search request to the {@link MetadataStore}.
 *
 *  In particular, a SearchResults consists of:
 *  - a list of AudioItems matching the search criteria
 *  - a map of Categories of the matching AudioItems mapping to their facet counts
 *  - a map of Languages of the matching AudioItems mapping to their facet counts
 *
 *  A facet count is the number of occurrences of a particular Category or Language in the
 *  list of matching AudioItems.
 *
 *  E.g. if three AudioItems are returned, two in English and one in German, the languageFacetCounts map
 *  would look like this:
 *    (en -> 2, de -> 1)
 */
public class SearchResult {
    private final int totalNumDocsInIndex;
    private final Set<String> audioItems;

    private final Map<String, Integer> categoryFacetCounts;
    private final Map<String, Integer> languageFacetCounts;
    private final Map<String, Integer> playlistFacetCounts;

    public SearchResult(int totalNumDocsInIndex,
            Map<String, Integer> facetCounts,
            Map<String, Integer> languageFacetCounts,
            Map<String, Integer> playlistFacetCounts,
            List<String> audioItems) {
        this.totalNumDocsInIndex = totalNumDocsInIndex;
        this.categoryFacetCounts = facetCounts;
        this.languageFacetCounts = languageFacetCounts;
        this.playlistFacetCounts = playlistFacetCounts;
        this.audioItems = Sets.newLinkedHashSet(audioItems);
    }

    public int getTotalNumDocsInIndex() {
        return totalNumDocsInIndex;
    }

    public int getFacetCount(Category category) {
        if (category == null) {
            return 0;
        }
        Integer count = categoryFacetCounts.get(category.getUuid());
        if (count == null) {
            return 0;
        } else {
            return count;
        }
    }

    public int getFacetCount(Playlist playlist) {
        if (playlist == null) {
            return 0;
        }
        Integer count = playlistFacetCounts.get(playlist.getUuid());
        if (count == null) {
            return 0;
        } else {
            return count;
        }
    }

    public Set<String> getAudioItems() {
        return audioItems;
    }

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
