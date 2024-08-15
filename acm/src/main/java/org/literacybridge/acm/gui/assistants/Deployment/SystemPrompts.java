package org.literacybridge.acm.gui.assistants.Deployment;

import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.store.*;

import static org.literacybridge.acm.Constants.*;
import static org.literacybridge.acm.store.MetadataSpecification.DC_TITLE;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SystemPrompts {
    private static final MetadataStore store = ACMConfiguration.getInstance()
            .getCurrentDB()
            .getMetadataStore();

    private final String promptId;
    private final String language;
    private final String promptTitle;
    private final Category promptCategory;
    public AudioItem promptItem;

    private static final Map<String, AudioItem> itemCache = new HashMap<>();
    private static long itemCacheStoreChangeCount = -1;
    private static Locale itemCacheLocale = null;

    /**
     * Constructor with support for pre-defined playlist prompts.
     * @param promptId The prompt ID, like "1" or "9-0"
     * @param promptTitle The friendly name, like "begin speaking" or "user feedback".
     * @param language The language of the prompt, "en", "dga".
     * @param promptCategory CATEGORY_TB_SYSTEM for system prompts, CATEGORY_TB_CATEGORY for playlist prompts.
     */
    public SystemPrompts(String promptId, String promptTitle, String language, Category promptCategory) {
        this.promptId = promptId;
        this.promptTitle = promptTitle;
        this.language = language;
        this.promptItem = null;
        this.promptCategory = promptCategory;
    }

    /**
     * Constructor with support for system prompts only.
     * @param promptId The prompt ID, like "1".
     * @param promptTitle The friendly name, like "begin speaking".
     * @param language The language of the prompt, "en", "dga".
     */
    public SystemPrompts(String promptId, String promptTitle, String language) {
        this(promptId, promptTitle, language, store.getTaxonomy().getCategory(CATEGORY_TB_SYSTEM));
    }

        public AudioItem findPrompt() {
        List<Category> categoryList = Collections.singletonList(promptCategory);    //   root
        List<Locale> localeList = Collections.singletonList(new RFC3066LanguageCode(language).getLocale());

        // Names are displayed in human-readable forms, like "begin speaking", so prefer to search for that form
        // instead of the prompt id.
        SearchResult searchResult = null;
        if (!promptTitle.isEmpty()) {
            searchResult = store.search(promptTitle, categoryList, localeList);
        }
        // If there was no prompt title, or searching for the message title returned nothing, try searching for the
        // prompt id, like "1".
        if (searchResult == null || searchResult.getAudioItems().isEmpty()) {
            searchResult = store.search(promptId, categoryList, localeList);
        }
        // Did the search find something?
        if (!searchResult.getAudioItems().isEmpty()) {
            boolean found = false;
            List<String> items = new ArrayList<>(searchResult.getAudioItems());
            for (String itemId : items) {
                promptItem = store.getAudioItem(itemId);
                if (promptItem != null) {
                    if (promptItem.getMetadata().getMetadataValue(DC_TITLE).getValue().equals(promptTitle)) {
                        found = true;
                    }
                }
            }
            if (!found) promptItem = null;
            //promptItem = store.getAudioItem(items.get(0));
        }
        // Nothing yet, look harder.
        // Since we're making searches based on the audio titles,
        // there's no need for a second title search
        //if (promptItem == null) {
        //    searchIgnoringUnderscores(categoryList, localeList);
        //}

        return promptItem;
    }

    private void searchIgnoringUnderscores(List<Category> categoryList, List<Locale> localeList) {
        assert(localeList.size() == 1);
        // Keep a cache of all audio items. Refresh when the repository changes.
        if (itemCacheStoreChangeCount != store.getChangeCount() || !localeList.get(0).equals(itemCacheLocale)) {
            itemCacheStoreChangeCount = store.getChangeCount();
            itemCacheLocale = localeList.get(0);
            itemCache.clear();
            SearchResult searchResult = store.search(null, categoryList, localeList);
            Map<String, AudioItem> items = searchResult.getAudioItems()
                    .stream()
                    .map(store::getAudioItem)
                    .collect(Collectors.toMap(AudioItem::getTitle, c -> c));
            itemCache.putAll(items);
        }

        // Replaces spaces and underscores with a regex accepting either one.
        String regex = "(?i)^(" + promptId.replaceAll("[ _]", "[ _]") + ")([: ]+(description|invite|invitation|prompt|long|action))?$";
        Pattern pattern = Pattern.compile(regex);
        for (Map.Entry<String, AudioItem> e : itemCache.entrySet()) {
            Matcher matcher = pattern.matcher(e.getKey());
            if (matcher.matches() && matcher.groupCount()==3) {
                promptItem = e.getValue();
                break;
            }
        }
    }

}
