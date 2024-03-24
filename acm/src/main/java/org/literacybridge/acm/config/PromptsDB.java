package org.literacybridge.acm.config;

import org.apache.commons.io.FilenameUtils;
import org.literacybridge.acm.gui.assistants.Deployment.SystemPrompts;
import org.literacybridge.acm.gui.assistants.PromptsImport.PromptsInfo;
import org.literacybridge.acm.importexport.AudioImporter;
import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.store.Category;
import org.literacybridge.acm.store.MetadataSpecification;
import org.literacybridge.acm.store.MetadataStore;

import java.io.File;
import java.util.Objects;

import static org.literacybridge.acm.Constants.*;

public class PromptsDB {
    private final MetadataStore store;

    PromptsDB() {
        store = ACMConfiguration.getInstance()
                .getCurrentDB()
                .getMetadataStore();
    }

    public void migrateSystemPrompts() {
        // read all system prompts from known file location and tag them
        Category categoryTbSystem = store.getTaxonomy().getCategory(CATEGORY_TB_SYSTEM);
        Category categoryTbCategories = store.getTaxonomy().getCategory(CATEGORY_TB_CATEGORIES);
        //Category agricultureCategory = store.getTaxonomy().getCategory(CATGEORY_GENERAL_AGRICULTURE);

        AudioImporter importer = AudioImporter.getInstance();

        // Get directory containing all language system prompts
        File tbLoadersDir = ACMConfiguration.getInstance().getCurrentDB().getProgramTbLoadersDir();
        String languagesPath = "TB_Options" + File.separator + "languages";
        File languagesDir = new File(tbLoadersDir, languagesPath);

        PromptsInfo promptsInfo = new PromptsInfo();

        // Directory containing language codes
        for (File languageCodeDir : Objects.requireNonNull(languagesDir.listFiles())) {
            if (languageCodeDir.isDirectory()) {
                String languageCode = languageCodeDir.getName();
                // folders such as `en.old`, `hau.old` etc. should be skipped
                if (languageCode.contains(".old")) {
                    continue;
                }
                for (File dirFile : Objects.requireNonNull(languageCodeDir.listFiles())) {
                    // are there subdirectories in the language folders?
                    if (dirFile.isDirectory()) {
                        String dirname = dirFile.getName();
                        if (dirname.equals("cat")) {
                            // playlist prompts
                            // Lookup names in General Agriculture
                            for (File catFile : Objects.requireNonNull(dirFile.listFiles())) {

                                if (isAudioFile(catFile.getName())) {
                                    //String playlistId = FilenameUtils.removeExtension(catFile.getName());
                                    try {
                                        // create a new playlist handler
                                        PlaylistPromptsImportHandler handler = new PlaylistPromptsImportHandler(categoryTbCategories, languageCode); // agricultureCategory, playlistId
                                        // store the audio item in store
                                        AudioItem audioItem = importer.importAudioItemFromFile(catFile, handler);
                                        audioItem.removeCategory(categoryTbSystem);
                                        store.newAudioItem(audioItem);
                                    } catch (Exception exception) {
                                        exception.printStackTrace();
                                    }
                                    // delete the file
                                    if (!catFile.delete()){
                                        System.out.println();
                                    }
                                }
                            }
                        }
                    } else if (isAudioFile(dirFile.getName())) {
                        String messageId = FilenameUtils.removeExtension(dirFile.getName());
                        try {
                            // prompts 0 & 7 are not handled atm
                            PromptsInfo.PromptInfo promptInfo = promptsInfo.getPrompt(messageId);
                            if (promptInfo != null) {
                                String messageTitle = promptInfo.getFilename();
                                SystemPrompts systemPrompts = new SystemPrompts(messageId, messageTitle, languageCode);
                                // Save file with a18 extension
                                boolean exists = systemPrompts.findPrompts();
                                SystemPromptsImportHandler handler = new SystemPromptsImportHandler(categoryTbSystem, languageCode, messageTitle); // messageId
                                if (exists) {
                                    AudioItem audioItem = null;
                                    if (systemPrompts.promptItem != null) {
                                        audioItem = systemPrompts.promptItem;
                                    }
                                    // this is done to remove intellij warnings
                                    assert  audioItem != null;
                                    // change audio item metadata
                                    //audioItem.getMetadata().put(MetadataSpecification.DC_LANGUAGE, languageCode);
                                    audioItem.getMetadata().put(MetadataSpecification.DC_TITLE, messageTitle);

                                    File importableFile = languageCodeDir.getAbsoluteFile();
                                    importer.updateAudioItemFromFile(audioItem, importableFile, handler);
                                } else {
                                    // store the audio item in store
                                    AudioItem audioItem = importer.importAudioItemFromFile(dirFile, handler);
                                    store.newAudioItem(audioItem);
                                }
                            }
                            // now delete the file from the Windows file system
                            if (!dirFile.delete()) {
                                System.out.println();
                            }
                        } catch (Exception ignored) {

                        }
                    }
                }
            }
        }
    }

    private static class SystemPromptsImportHandler implements AudioImporter.AudioItemProcessor {
        private final Category category;
        private final String language;
        private final String title;

        SystemPromptsImportHandler(Category category, String language, String description) {
            this.category = category;
            this.language = language;
            this.title = description;
        }

        @Override
        public void process(AudioItem item) {
            assert item != null;

            item.addCategory(category);
            item.getMetadata().put(MetadataSpecification.DC_LANGUAGE, language);
            item.getMetadata().put(MetadataSpecification.DC_TITLE, title);
        }
    }

    private static class PlaylistPromptsImportHandler implements AudioImporter.AudioItemProcessor {
        private final Category category;
        private final String language;

        public PlaylistPromptsImportHandler(Category category, String language) {
            this.category = category;
            this.language = language;
        }

        @Override
        public void process(AudioItem item) {
            assert item != null;

            item.addCategory(category);
            item.getMetadata().put(MetadataSpecification.DC_LANGUAGE, language);
        }
    }

    private boolean isAudioFile(String name) {
        return name.toLowerCase().endsWith(".a18") || name.toLowerCase().endsWith(".mp3") || name.toLowerCase().endsWith(".wav") ||
                name.toLowerCase().endsWith(".ogg") || name.toLowerCase().endsWith(".m4a") || name.toLowerCase().endsWith(".wma") || name.toLowerCase().endsWith(".aac");
    }
}
