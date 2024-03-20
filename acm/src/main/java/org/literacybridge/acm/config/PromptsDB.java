package org.literacybridge.acm.config;

import org.apache.commons.io.FilenameUtils;
import org.literacybridge.acm.gui.assistants.Deployment.SystemPrompts;
import org.literacybridge.acm.gui.assistants.PromptsImport.PromptsInfo;
import org.literacybridge.acm.importexport.AudioImporter;
import org.literacybridge.acm.store.*;

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
        Category systemCategory = store.getTaxonomy().getCategory(CATEGORY_TB_SYSTEM);
        Category tbCategories = store.getTaxonomy().getCategory(CATEGORY_TB_CATEGORIES);
        Category talkingBook = store.getTaxonomy().getCategory(CATEGORY_TB);
        Category agricultureCategory = store.getTaxonomy().getCategory(CATGEORY_GENERAL_AGRICULTURE);

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
                                if (catFile.getName().toLowerCase().endsWith(".a18") || catFile.getName().toLowerCase().endsWith(".mp3") ||
                                        catFile.getName().toLowerCase().endsWith(".wav") || catFile.getName().toLowerCase().endsWith(".ogg")) {
                                    String playlistId = FilenameUtils.removeExtension(catFile.getName());
                                    try {
                                        // create a new playlist handler
                                        PlaylistPromptsImportHandler handler = new PlaylistPromptsImportHandler(agricultureCategory, languageCode, playlistId);
                                        // store the audio item in store
                                        AudioItem audioItem = importer.importAudioItemFromFile(catFile, handler);
                                        audioItem.removeCategory(tbCategories);
                                        audioItem.removeCategory(systemCategory);
                                        store.newAudioItem(audioItem);
                                    } catch (Exception ignored) {

                                    }
                                    // delete the file
                                    catFile.delete();
                                }
                            }
                        }
                    } else if (dirFile.getName().toLowerCase().endsWith(".a18") || dirFile.getName().toLowerCase().endsWith(".mp3") ||
                            dirFile.getName().toLowerCase().endsWith(".wav") || dirFile.getName().toLowerCase().endsWith(".ogg")) {
                        String messageId = FilenameUtils.removeExtension(dirFile.getName());
                        try {
                            // prompts 0 & 7 are not handled atm
                            PromptsInfo.PromptInfo tempPromptInfo = promptsInfo.getPrompt(messageId);
                            if (tempPromptInfo != null) {
                                String desc = tempPromptInfo.getFilename();
                                SystemPrompts tempSystemPrompts = new SystemPrompts(messageId, desc, languageCode);
                                // Save file with a18 extension
                                boolean exists = tempSystemPrompts.findPrompts();
                                SystemPromptsImportHandler handler = new SystemPromptsImportHandler(systemCategory, languageCode, messageId, desc);
                                if (exists) {
                                    AudioItem audioItem;
                                    if (tempSystemPrompts.longPromptItem != null) {
                                        audioItem = tempSystemPrompts.longPromptItem;
                                    } else {
                                        audioItem = tempSystemPrompts.shortPromptItem;
                                    }
                                    // change audio item metadata
                                    //audioItem.getMetadata().put(MetadataSpecification.DC_LANGUAGE, languageCode);
                                    audioItem.getMetadata().put(MetadataSpecification.DC_TITLE, desc);
                                    //String audioId = audioItem.getAudioId();
                                    //assert audioId != null;
                                    //audioItem.getMetadata().put(MetadataSpecification.DC_IDENTIFIER, audioId);
                                } else {
                                    // store the audio item in store
                                    AudioItem audioItem = importer.importAudioItemFromFile(dirFile, handler);
                                    store.newAudioItem(audioItem);
                                }
                            }
                            // now delete the file from the Windows file system
                            dirFile.delete();
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
        private final String messageId;
        private final String description;

        SystemPromptsImportHandler(Category category, String language, String messageId, String description) {
            this.category = category;
            this.language = language;
            this.messageId = messageId;
            this.description = description;
        }

        @Override
        public void process(AudioItem item) {
            assert item != null;

            String audioId = item.getAudioId();
            assert audioId != null;

            item.addCategory(category);
            item.getMetadata().put(MetadataSpecification.DC_LANGUAGE, language);
            item.getMetadata().put(MetadataSpecification.DC_TITLE, description);
            item.getMetadata().put(MetadataSpecification.DC_IDENTIFIER, audioId);
        }
    }

    private static class PlaylistPromptsImportHandler implements AudioImporter.AudioItemProcessor {
        private final Category category;
        private final String language;
        private final String playlistId;
        private final String description;

        public PlaylistPromptsImportHandler(Category category, String language, String playlistId) {
            this.category = category;
            this.language = language;
            this.playlistId = playlistId;
            this.description = "";
        }

        @Override
        public void process(AudioItem item) {
            assert item != null;

            String audioId = item.getAudioId();
            assert audioId != null;

            item.addCategory(category);
            item.getMetadata().put(MetadataSpecification.DC_LANGUAGE, language);
            item.getMetadata().put(MetadataSpecification.DC_IDENTIFIER, audioId);
        }
    }
}
