package org.literacybridge.acm.gui.assistants.Deployment;

import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.store.Category;
import org.literacybridge.acm.store.MetadataStore;
import org.literacybridge.acm.store.RFC3066LanguageCode;
import org.literacybridge.acm.store.SearchResult;
import org.literacybridge.core.spec.ProgramSpec;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.literacybridge.acm.Constants.CATEGORY_TB_CATEGORIES;

class DeploymentContext {

    int deploymentNo = -1;
    ProgramSpec programSpec;

    class PlaylistPrompts {

        private final String title;
        private final String languagecode;

        private MetadataStore store = ACMConfiguration.getInstance()
            .getCurrentDB()
            .getMetadataStore();
        private String categoryId;


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
            List<Category> categoryList = Arrays.asList(store.getTaxonomy().getCategory(CATEGORY_TB_CATEGORIES));
            List<Locale> localeList = Arrays.asList(new RFC3066LanguageCode(languagecode).getLocale());

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
}
