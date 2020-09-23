package org.literacybridge.acm.gui.assistants.Deployment;

import org.literacybridge.acm.Constants;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.store.Category;
import org.literacybridge.acm.store.MetadataStore;
import org.literacybridge.acm.store.RFC3066LanguageCode;
import org.literacybridge.acm.store.SearchResult;

import java.io.File;
import java.util.ArrayList;
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
public class PlaylistPrompts {
    public static final String SHORT_TITLE = "%s";
    // Long prompt decorators in the ACM: description|invite|invitation|prompt|long|action
    public static final String LONG_TITLE = "%s : invitation";
    public static final String[] LONG_TITLE_LIST = {"%s : invitation", "%s : description", "%s : action"};

    private final String title;
    private final String languagecode;

    private MetadataStore store = ACMConfiguration.getInstance()
        .getCurrentDB()
        .getMetadataStore();
    String categoryId;

    // Files like "2-0.a18" and "i2-0.a18"
    private File shortPromptFile;
    private File longPromptFile;
    // Audio items with titles like "Health" and "Health - invitation"
    AudioItem shortPromptItem;
    AudioItem longPromptItem;

    public PlaylistPrompts(String playlistTitle, String languagecode) {
        this.title = playlistTitle;
        this.languagecode = languagecode;
    }

    public void findPrompts() {
        findPromptsInLanguageFiles();
        findPromptsInAcmContent();
    }

    public String getTitle() {
        return title;
    }
    public String getLanguagecode() {
        return languagecode;
    }

    public AudioItem getShortItem() {
        return shortPromptItem;
    }
    public File getShortFile() {
        return shortPromptFile;
    }
    public AudioItem getLongItem() {
        return longPromptItem;
    }
    public File getLongFile() {
        return longPromptFile;
    }
    public boolean hasShortPrompt() {
        return shortPromptFile != null || shortPromptItem != null;
    }
    public boolean hasLongPrompt() {
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
     * Look up the prompt in the taxonomy to find any matching category(ies) -- there may be two.
     *
     * Then look for files named like "2-0.a18" and "i2-0.a18" in the TB_Options/languages/{lang}/cat
     * directory.
     *
     * Sets the instance variable "categoryId" to the found category id, if matching file(s) found.
     */
    private void findPromptsInLanguageFiles() {
        // Look in the taxonomy for categories that match the playlist name. Playlist "Health" will match both
        // "2: Health" and "2-0: General Health" (ie "General" is more or less ignored).
        List<String> categoryIds = getCategoryIds(); // zero, one, or two items.
        // If we know the category...
        if (categoryIds.size() == 1 && categoryIds.get(0).equals(Constants.CATEGORY_INTRO_MESSAGE)) {
            // The intro message has no prompts; only a category. The intro message is handled specially
            // in the "control.txt", and is played right after "Welcome to the Talking Book!".
            this.categoryId = categoryIds.get(0);

        } else if (categoryIds.size() > 0) {
            // Where to look for category prompts.
            File tbLoadersDir = ACMConfiguration.getInstance().getCurrentDB().getProgramTbLoadersDir();
            String languagesPath =
                "TB_Options" + File.separator + "languages" + File.separator + languagecode + File.separator + "cat";
            File categoriesDir = new File(tbLoadersDir, languagesPath);
            // Look for "${categoryId}.a18" and "i${categoryId}.a18", for all (1 or 2) found category ids. If
            // both were found, the last one wins, which will be the most-nested one.
            boolean foundShort = false, foundLong = false, foundBoth = false;
            for (String categoryId : categoryIds) {
                // Look for short and long files.
                String filename = String.format("%s.a18", categoryId);
                File shortPromptFile = new File(categoriesDir, filename);
                File longPromptFile = new File(categoriesDir, "i" + filename);
                if (shortPromptFile.exists() && longPromptFile.exists()) {
                    this.shortPromptFile = shortPromptFile;
                    this.longPromptFile = longPromptFile;
                    this.categoryId = categoryId;
                    foundBoth = true;
                    break;
                } else {
                    if (shortPromptFile.exists()) {
                        this.shortPromptFile = shortPromptFile;
                        this.categoryId = categoryId;
                        foundShort = true;
                    }
                    if (longPromptFile.exists()) {
                        this.longPromptFile = longPromptFile;
                        this.categoryId = categoryId;
                        foundLong = true;
                    }
                }
            }

            if (foundShort && foundLong && !foundBoth) {
                // Found both recordings, but for different category ids (like "2.a18" and "i2-0.a18"), and never together.
                this.longPromptFile = null;
                this.shortPromptFile = null;
                this.categoryId = null;
            }
        }
    }

    /**
     * Look for a category name that matches the playlist title, only considering leaf nodes.
     * If the bare title isn't found, look for "General ${category}".
     *
     * @return any potential category ids that were found. May be none, one, or two category ids.
     */
    private List<String> getCategoryIds() {
        List<String> result = new ArrayList<>();
        // Look for the category name as-is.
        String categoryId = StreamSupport.stream(store.getTaxonomy()
            .breadthFirstIterator()
            .spliterator(), false)
            // Strictly, non-leafs aren't assignable categories, however they might have been
            // used as playlist categories. So while it would be appropriate to filter out non-
            // leaf nodes, it doesn't work with reality.
            //.filter(c -> !c.hasChildren())
            .filter(c -> c.isKnownAs(title))
            .map(Category::getId)
            .findFirst()
            .orElse(null);
        if (categoryId != null) {
            result.add(categoryId);
        }
        // Look for the category name decorated as "General Category"
        final String generalTitle = "General " + title;
        categoryId = StreamSupport.stream(store.getTaxonomy()
            .breadthFirstIterator()
            .spliterator(), false)
            // Same comment as above.
            //.filter(c -> !c.hasChildren())
            .filter(c -> c.isKnownAs(generalTitle))
            .map(Category::getId)
            .findFirst()
            .orElse(null);
        if (categoryId != null) {
            result.add(categoryId);
        }
        return result;
    }

    /**
     * Look in the content for audio items with a title that matches the playlist
     * title, and for one that matches the playlist title + " : description".
     */
    private void findPromptsInAcmContent() {
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
        String regex = "(?i)^(" + Pattern.quote(title) + ")([: ]+(description|invite|invitation|prompt|long|action))?$";
        Pattern pattern = Pattern.compile(regex);
        for (Map.Entry<String, AudioItem> e : items.entrySet()) {
            Matcher matcher = pattern.matcher(e.getKey());
            if (matcher.matches() && matcher.groupCount()==3) {
                if (matcher.group(2) != null) {
                    longPromptItem = e.getValue();
                } else {
                    shortPromptItem = e.getValue();
                }
            }
        }
    }
}
