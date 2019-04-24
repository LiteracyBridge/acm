package org.literacybridge.acm.gui.assistants.Deployment;

import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.store.Category;
import org.literacybridge.acm.store.MetadataStore;
import org.literacybridge.acm.store.RFC3066LanguageCode;
import org.literacybridge.acm.store.SearchResult;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.literacybridge.acm.Constants.CATEGORY_TB_CATEGORIES;

/**
 * Helper class to find the short and long prompts for a playlist. The prompts may be provided
 * as they always have been as messages named by the category, in the TB_Options / languages /
 * {language} / cat directory (ie, 2-0.a18 and i2-0.a18), or as messages in the ACM, categorized
 * as "Other Messages / Talking Book / TB Categories" with a title matching the playlist title.
 *
 * Because there are two ways to specify the prompts, there can be ambiguity, with both a
 * category prompt and an ACM message. The appropriate response is to warn the user, but to use
 * the ACM message, because that is what the user has the most control over.
 *
 * There could also be a situation in which there is one ACM message and one category prompt.
 * This is unlikely, because it would mean that the category prompt was only half-way implemented,
 * which would not have worked properly ("I have some work to do..." error on the TB, when the
 * missing message was not found.) Note, however, that this is not ambiguous or wrong, only
 * unexpected.
 */
class PlaylistPrompts {

    private final String title;
    private final String languagecode;

    private MetadataStore store = ACMConfiguration.getInstance()
        .getCurrentDB()
        .getMetadataStore();
    String categoryId;


    File shortPromptFile;
    File longPromptFile;
    AudioItem shortPromptItem;
    AudioItem longPromptItem;

    PlaylistPrompts(String playlistTitle, String languagecode) {
        this.title = playlistTitle;
        this.languagecode = languagecode;
    }

    void findPrompts() {
        findCategoryPrompts();
        findContentPrompts();
    }

    boolean hasShortPrompt() {
        return shortPromptFile != null || shortPromptItem != null;
    }
    boolean hasLongPrompt() {
        return longPromptFile != null || longPromptItem != null;
    }
    boolean hasBothPrompts() {
        return hasShortPrompt() && hasLongPrompt();
    }
    boolean hasShortPromptAmbiguity() {
        return shortPromptFile != null && shortPromptItem != null;
    }
    boolean hasLongPromptAmbiguity() {
        return longPromptFile != null && longPromptItem != null;
    }
    boolean hasEitherPromptAmbiguity() {
        return hasShortPromptAmbiguity() || hasLongPromptAmbiguity();
    }
    boolean hasMixedPrompts() {
        // Has both prompts, only one of each, but one is a file and the other is a category prompt.
        // It would be very weird (and difficult) for this to happen.
        return hasBothPrompts() && !hasEitherPromptAmbiguity() &&
            ((shortPromptFile==null) != (longPromptFile==null));
    }

    /**
     * Look for prompts named like "2-0.a18" and "i2-0.a18" in the TB_Options/languages/{lang}/cat
     * directory.
     */
    private void findCategoryPrompts() {
        getCategoryId();
        // If we know the category...
        if (categoryId != null) {
            // Where to look for category prompts.
            File tbLoadersDir = ACMConfiguration.getInstance().getCurrentDB().getTBLoadersDirectory();
            String languagesPath =
                "TB_Options" + File.separator + "languages" + File.separator + languagecode + File.separator + "cat";
            File categoriesDir = new File(tbLoadersDir, languagesPath);

            // Look for short and long files.
            String filename = String.format("%s.a18", categoryId);
            File promptFile = new File(categoriesDir, filename);
            if (promptFile.exists()) shortPromptFile = promptFile;
            filename = "i" + filename;
            promptFile = new File(categoriesDir, filename);
            if (promptFile.exists()) longPromptFile = promptFile;
        }
    }

    /**
     * Look for a category name that matches the playlist title, only considering leaf nodes.
     * If the bare title isn't found, look for "General ${category}".
     */
    private void getCategoryId() {
        // Look for the category name amongst the leaf nodes.
        categoryId = StreamSupport.stream(store.getTaxonomy()
            .breadthFirstIterator()
            .spliterator(), false)
            .filter(c -> !c.hasChildren())
            .filter(c -> c.getCategoryName().equalsIgnoreCase(title))
            .map(Category::getId)
            .findFirst()
            .orElse(null);
        if (categoryId == null) {
            // If it wasn't there, decorate like "General Category".
            final String generalTitle = "General " + title;
            categoryId = StreamSupport.stream(store.getTaxonomy()
                .breadthFirstIterator()
                .spliterator(), false)
                .filter(c -> !c.hasChildren())
                .filter(c -> c.getCategoryName().equalsIgnoreCase(generalTitle))
                .map(Category::getId)
                .findFirst()
                .orElse(null);
        }
    }

    /**
     * Look in the content for audio items with a title that matches the playlist
     * title, and for one that matches the playlist title + " : description".
     */
    private void findContentPrompts() {
        // Get any audio items in "TB Categories", given language, that textually match the playlist title.
        List<Category> categoryList = Collections.singletonList(store.getTaxonomy()
            .getCategory(CATEGORY_TB_CATEGORIES));
        List<Locale> localeList = Collections.singletonList(new RFC3066LanguageCode(languagecode).getLocale());

        SearchResult searchResult = store.search(title, categoryList, localeList);
        Map<String, AudioItem> items = searchResult.getAudioItems()
            .stream()
            .map(store::getAudioItem)
            .collect(Collectors.toMap(AudioItem::getTitle, c -> c));

        // Case insensitive, match pattern and optional " : description"
        String regex = "(?i)^(" + Pattern.quote(title) + ")([: ]+description)?$";
        Pattern pattern = Pattern.compile(regex);
        for (Map.Entry<String, AudioItem> e : items.entrySet()) {
            Matcher matcher = pattern.matcher(e.getKey());
            if (matcher.matches() && matcher.groupCount()==2) {
                if (matcher.group(2) != null) {
                    longPromptItem = e.getValue();
                } else {
                    shortPromptItem = e.getValue();
                }
            }
        }
    }
}
