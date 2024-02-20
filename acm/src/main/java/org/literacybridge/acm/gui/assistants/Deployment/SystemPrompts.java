package org.literacybridge.acm.gui.assistants.Deployment;

import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.gui.assistants.PromptsImport.PromptsInfo;
import org.literacybridge.acm.store.*;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.literacybridge.acm.Constants.*;

public class SystemPrompts {
    public final static String SYSTEM_MESSAGE_CATEGORY = "System Messages";
    private static MetadataStore store = ACMConfiguration.getInstance()
            .getCurrentDB()
            .getMetadataStore();

    private String title;
    private String language;
    private String description;
    private String categoryId;
    private File shortPromptFile;
    public AudioItem shortPromptItem;
    private File longPromptFile;
    public AudioItem longPromptItem;

    private static final Map<String, AudioItem> itemCache = new HashMap<>();
    private static long itemCacheStoreChangeCount = -1;
    private static Locale itemCacheLocale = null;

    public SystemPrompts(String title, String languagecode) {
        this.title = title;
        this.description = "";
        this.language = languagecode;
        this.categoryId = null;
        this.shortPromptFile = null;
        this.longPromptFile = null;
        this.shortPromptItem = null;
        this.longPromptItem = null;
    }

    public SystemPrompts(String title, String description, String language) {
        this.title = title;
        this.description = description;
        this.language = language;
        this.categoryId = null;
        this.shortPromptFile = null;
        this.longPromptFile = null;
        this.shortPromptItem = null;
        this.longPromptItem = null;
    }

    public SystemPrompts(String title) {
        this.title = title;
    }

    public SystemPrompts(String title, String languagecode, String categoryId,
                         File shortPromptFile, AudioItem shortPromptItem, File longPromptFile, AudioItem longPromptItem) {
        this.title = title;
        this.language = languagecode;
        this.categoryId = categoryId;
        this.shortPromptFile = shortPromptFile;
        this.shortPromptItem = shortPromptItem;
        this.longPromptFile = longPromptFile;
        this.longPromptItem = longPromptItem;
    }

    public void renamePrompt(String newtitle) {
        this.description = newtitle;
    }

    public void setShortPromptFile(String filename) {

    }

    public boolean findPrompts() {
        return findPromptsInAcmContent();
    }

    public void addAudioItem(String name) {
        AudioItem e = store.getAudioItem(name);
        if (e != null) {
            shortPromptItem = e;
        }
        return;
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
                .collect(Collectors.toMap(audioItem -> audioItem.getAudioId().trim(), c -> c)); // getTitle

        // Items list returns null. Can't find our system prompt in store
        if (items.size() == 0) {
            return false;
        }

        // match pattern and optional " : description"
        String regex = "(?i)^(" + Pattern.quote(title.trim()) + ")([: ]+(description|invite|invitation|prompt|long|action))?$";
        Pattern pattern = Pattern.compile(regex);
        for (Map.Entry<String, AudioItem> e : items.entrySet()) {
            Matcher matcher = pattern.matcher(e.getKey());
            if (matcher.matches() && matcher.groupCount()==3) {
                if (matcher.group(2) != null) {
                    longPromptItem = e.getValue();
                } else {
                    shortPromptItem = e.getValue();
                    if (categoryId == null) {
                        //categoryId=shortPromptItem.getId();
                        categoryId = SYSTEM_MESSAGE_CATEGORY;
                    }
                }
            }
        }

        if (longPromptItem == null || shortPromptItem == null) {
            searchIgnoringUnderscores(categoryList, localeList);
        }

        return longPromptItem != null || shortPromptItem != null;
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
                if (matcher.group(2) != null) {
                    if (longPromptItem == null) {
                        longPromptItem = e.getValue();
                    }
                } else {
                    if (shortPromptItem == null) {
                        shortPromptItem = e.getValue();
                        if (categoryId == null) {
                            categoryId=shortPromptItem.getId();
                        }
                    }
                }
            }
        }
    }

}
