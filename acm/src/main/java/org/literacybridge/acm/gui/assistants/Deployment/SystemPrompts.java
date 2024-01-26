package org.literacybridge.acm.gui.assistants.Deployment;

import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.gui.assistants.PromptsImport.PromptsInfo;
import org.literacybridge.acm.store.*;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.literacybridge.acm.Constants.*;

public class SystemPrompts {
    public static final String SHORT_TITLE = "%s";
    private static MetadataStore store = ACMConfiguration.getInstance()
            .getCurrentDB()
            .getMetadataStore();

    String id;
    private String title;
    private String language;
    private String categoryId;
    private File shortPromptFile;
    AudioItem shortPromptItem;
    private File longPromptFile;
    AudioItem longPromptItem;

    public SystemPrompts(String id, String promptTitle, String promptsLanguage) {
        this.id = id;
        this.title = promptTitle;
        this.language = promptsLanguage;
        this.categoryId = "";
        this.shortPromptFile = null;
        this.longPromptFile = null;
        this.shortPromptItem = null;
        this.longPromptItem = null;
    }

    public SystemPrompts(String title, String languagecode, String categoryId, File shortPromptFile, AudioItem shortPromptItem, File longPromptFile, AudioItem longPromptItem) {
        this.title = title;
        this.language = languagecode;
        this.categoryId = categoryId;
        this.shortPromptFile = shortPromptFile;
        this.shortPromptItem = shortPromptItem;
        this.longPromptFile = longPromptFile;
        this.longPromptItem = longPromptItem;
    }

    public void findPrompts() {
        findPromptsInAcmContent();
    }

    private void findPromptsInAcmContent() {
        List<Category> categoryList = Collections.singletonList(store.getTaxonomy()
                .getCategory("0-4-1"));
        List<Locale> localeList = Collections.singletonList(new RFC3066LanguageCode(language).getLocale());

        SearchResult searchResult = store.search(id, categoryList, localeList);
        Map<String, AudioItem> items = searchResult.getAudioItems()
                .stream()
                .map(store::getAudioItem)
                .collect(Collectors.toMap(audioItem -> audioItem.getTitle().trim(), c -> c));

        SearchResult _search = store.search(title, categoryList, localeList);
        Map<String, AudioItem> _items = _search.getAudioItems()
                .stream()
                .map(store::getAudioItem)
                .collect(Collectors.toMap(audioItem -> audioItem.getTitle().trim(), c -> c));

        // Items list returns null. Can't find our system prompt in store
        String s = "result";
    }
}
