package org.literacybridge.acm.gui.assistants.ContentImport;

import org.apache.commons.lang3.tuple.Pair;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.gui.Assistant.Assistant;
import org.literacybridge.acm.gui.Assistant.AssistantPage;
import org.literacybridge.acm.gui.UIConstants;
import org.literacybridge.acm.gui.assistants.common.AcmAssistantPage;
import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.store.Category;
import org.literacybridge.acm.store.MetadataStore;
import org.literacybridge.acm.store.RFC3066LanguageCode;
import org.literacybridge.acm.store.SearchResult;

import javax.swing.*;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Base class for Content Import Assistant pages. Contains code shared amongst the pages.
 *
 * @param <Context>
 */
abstract class ContentImportBase<Context> extends AcmAssistantPage<Context> {

    ContentImportBase(Assistant.PageHelper<Context> listener) {
        super(listener);
    }

    public static class ImportReminderLine {
        private final Box hbox;
        private final JLabel deployment;
        private final JLabel language;

        public JComponent getLine() { return hbox; }
        public JLabel getDeployment() { return deployment; }
        public JLabel getLanguage() { return language; }

        public ImportReminderLine() {
            hbox = Box.createHorizontalBox();
            hbox.add(new JLabel("Importing message content for deployment "));
            deployment = makeBoxedLabel();
            hbox.add(deployment);
            hbox.add(new JLabel(" and language "));
            language = makeBoxedLabel();
            hbox.add(language);
            hbox.add(Box.createHorizontalGlue());
        }
    }

    /**
     * Given a message title (ie, from the Program Spec), see if we already have such an
     * audio item in the desired language.
     *
     * @param title        The title to search for.
     * @param languagecode The language in which we want the audio item.
     * @return the AudioItem if it exists, otherwise null.
     */
    static AudioItem findAudioItemForTitle(String title, String languagecode) {
        MetadataStore store = ACMConfiguration.getInstance().getCurrentDB().getMetadataStore();
        List<Category> categoryList = new ArrayList<>();
        List<Locale> localeList = Collections.singletonList(new RFC3066LanguageCode(languagecode).getLocale());

        SearchResult searchResult = store.search(title, categoryList, localeList);
        // Filter because search will return near matches.
        @SuppressWarnings("UnnecessaryLocalVariable")
        AudioItem item = searchResult.getAudioItems()
            .stream()
            .map(store::getAudioItem)
            .filter(it -> it.getTitle().equals(title))
            .findAny()
            .orElse(null);
        return item;
    }

}
