package org.literacybridge.acm.gui.assistants.Deployment;

import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.store.*;

import static org.literacybridge.acm.Constants.*;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SystemPrompts {
    public final static String SYSTEM_MESSAGE_CATEGORY = "System Messages";
    private static final MetadataStore store = ACMConfiguration.getInstance()
            .getCurrentDB()
            .getMetadataStore();

    private final String title;
    private String language;
    private String description;
    private String categoryId;
    public AudioItem promptItem;

    private static final Map<String, AudioItem> itemCache = new HashMap<>();
    private static long itemCacheStoreChangeCount = -1;
    private static Locale itemCacheLocale = null;

    public SystemPrompts(String title, String languagecode) {
        this.title = title;
        this.description = "";
        this.language = languagecode;
        this.categoryId = null;
        this.promptItem = null;
    }

    public SystemPrompts(String title, String description, String language) {
        this.title = title;
        this.description = description;
        this.language = language;
        this.categoryId = null;
        this.promptItem = null;
    }

    public SystemPrompts(String title) {
        this.title = title;
    }

    public SystemPrompts(String title, String languagecode, String categoryId, AudioItem promptItem) {
        this.title = title;
        this.language = languagecode;
        this.categoryId = categoryId;
        this.promptItem = promptItem;
    }

    public boolean findPrompts() {
        return findPromptsInAcmContent();
    }

    public void addAudioItem(String name) {
        AudioItem e = store.getAudioItem(name);
        if (e != null) {
            promptItem = e;
        }
    }

    private boolean findPromptsInAcmContent() {
        List<Category> categoryList = Collections.singletonList(store.getTaxonomy()
                .getCategory(CATEGORY_TB_SYSTEM));    //   root
        List<Locale> localeList = Collections.singletonList(new RFC3066LanguageCode(language).getLocale());

        // Names are displayed in human-readable forms
        // so searches will have to take that form
        // instead of the file id
        SearchResult searchResult;
        if (description.isEmpty()) {
            searchResult = store.search(title, categoryList, localeList);
        } else {
            searchResult = store.search(description, categoryList, localeList);
        }

        Map<String, AudioItem> items = searchResult.getAudioItems()
                .stream()
                .map(store::getAudioItem)
                .collect(Collectors.toMap(audioItem -> audioItem.getTitle().trim(), c -> c)); //  getTitle

        // Items list returns null. Can't find our system prompt in store
        if (items.isEmpty()) {
            // Okay there can be instances where the description field is not NULL
            // but the search through file desc returns an empty item
            // In such cases just try searching with the file id
            searchResult = store.search(title, categoryList, localeList);
            items = searchResult.getAudioItems()
                    .stream()
                    .map(store::getAudioItem)
                    .collect(Collectors.toMap(audioItem -> audioItem.getTitle().trim(), c -> c));
            // if we still get an empty item list then there is indeed no instance of the file
            // in the content store
            if (items.isEmpty()) {
                return false;
            }
        }

        if (promptItem == null) {
            searchIgnoringUnderscores(categoryList, localeList);
        }

        if (promptItem == null) {
            // search by audio items
            Set<String> audioItems = searchResult.getAudioItems();
            for (String audio_items : audioItems) {
                AudioItem audioItem = store.getAudioItem(audio_items);

                Metadata metadata = audioItem.getMetadata();
                MetadataValue<String> dc_title = metadata.getMetadataValue(MetadataSpecification.DC_TITLE);
                if (dc_title.getValue().equals(description)) {
                    promptItem = audioItem;
                }
            }
        }

        return promptItem != null;
    }

    private void searchIgnoringUnderscores(List<Category> categoryList, List<Locale> localeList) {
        assert(localeList.size() == 1);
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

        String regex = "(?i)^(" + title.replace(" ", "[ _]") + ")([: ]+(description|invite|invitation|prompt|long|action))?$";
        Pattern pattern = Pattern.compile(regex);
        for (Map.Entry<String, AudioItem> e : itemCache.entrySet()) {
            Matcher matcher = pattern.matcher(e.getKey());
            if (matcher.matches() && matcher.groupCount()==3) {
                if (promptItem == null) {
                    promptItem = e.getValue();
                    if (categoryId == null) {
                        categoryId = SYSTEM_MESSAGE_CATEGORY;
                    }
                }
            }
        }
    }

}