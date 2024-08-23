package org.literacybridge.acm.config;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.literacybridge.acm.audioconverter.converters.BaseAudioConverter;
import org.literacybridge.acm.gui.assistants.Deployment.SystemPrompts;
import org.literacybridge.acm.gui.assistants.PromptsImport.PromptsInfo;
import org.literacybridge.acm.importexport.AudioImporter;
import org.literacybridge.acm.repository.AudioItemRepository;
import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.store.Category;
import org.literacybridge.acm.store.MetadataSpecification;
import org.literacybridge.acm.store.MetadataStore;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

import static org.literacybridge.acm.Constants.*;

public class PromptsDB {
    private final MetadataStore store = ACMConfiguration.getInstance()
            .getCurrentDB()
            .getMetadataStore();

    private final Category categoryTbSystem = store.getTaxonomy().getCategory(CATEGORY_TB_SYSTEM);
    private final Category categoryTbCategories = store.getTaxonomy().getCategory(CATEGORY_TB_CATEGORIES);

    private final AudioImporter importer = AudioImporter.getInstance();

    public void migrateSystemPrompts() {
        // read all system prompts from known file location and tag them

        // Get directory containing all language system prompts
        File tbLoadersDir = ACMConfiguration.getInstance().getCurrentDB().getProgramTbLoadersDir();
        String languagesPath = "TB_Options" + File.separator + "languages";
        File languagesDir = new File(tbLoadersDir, languagesPath);

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
                    if (dirFile.isDirectory() && dirFile.getName().equalsIgnoreCase("cat")) {
                        // playlist prompts
                        for (File catFile : Objects.requireNonNull(dirFile.listFiles())) {
                            if (isAudioFile(catFile.getName())) {
                                migratePlaylistPrompt(catFile, languageCode);
                            }
                        }
                    } else if (isAudioFile(dirFile.getName())) {
                        migrateSystemPrompt(dirFile, languageCode);
                    }
                }
            }
        }
    }

    /**
     * Migrate one system prompt from a file into the content repository.
     *
     * This function migrates one system prompt audio file from a known location in the file system into
     * the content repository. The name of the file is the prompt-ID (like "1"). This is translated to
     * the more descriptive name (like "begin speaking") that we suggest customers use when recording prompts.
     *
     * @param promptFile The file to be migrated.
     * @param languageCode The language of the file.
     */
    private void migrateSystemPrompt(File promptFile, String languageCode) {
        String promptId = FilenameUtils.removeExtension(promptFile.getName());
        PromptsInfo promptsInfo = PromptsInfo.getInstance();
        try {
            // prompts 0 & 7 are not handled atm
            PromptsInfo.PromptInfo promptInfo = promptsInfo.getPrompt(promptId);
            if (promptInfo != null) {
                String promptTitle = promptInfo.getPromptTitle();

                doMigration(promptFile, languageCode, promptTitle, promptId, categoryTbSystem);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Migrate one playlist prompt from a file into the content repository.
     *
     * This function migrates one playlist prompt audio file from a known location in the file system into
     * the content repository. The name of the file is the generally a category-id (like "1-2"), which is
     * looked up in the taxonomy to translate to a more meaningful name (like "Livestock"). The exceptions are
     * "$0-1" => "talking book" and "9-0" => "user feedback", which are known in the "prompts_ex.csv" file.
     *
     * @param promptFile The file to be migrated.
     * @param languageCode The language of the file.
     */
    private void migratePlaylistPrompt(File promptFile, String languageCode) {
        try {
            String promptId = FilenameUtils.removeExtension(promptFile.getName());
            String promptTitle = null;
            PromptsInfo.PromptInfo promptInfo = PromptsInfo.getInstance().getPrompt(promptId);
            // Is it a standard prompt, "$1-0" or "9-0"?
            if (promptInfo != null) {
                // Yes, use title from PromptsInfo
                promptTitle = promptInfo.getPromptTitle();
            } else {
                // No, not a standard prompt, so try to find in taxonomy.
                boolean isInvitation = false;
                String promptCategoryId = promptId;
                if (promptId.charAt(0)=='i' || promptId.charAt(0)=='I') {
                    isInvitation = true;
                    promptCategoryId = promptId.substring(1);
                }
                Category promptCategory = ACMConfiguration.getInstance().
                        getCurrentDB()
                        .getMetadataStore()
                        .getTaxonomy()
                        .getCategory(promptCategoryId);
                if (promptCategory != null) {
                    promptTitle = promptCategory.getCategoryName();
                    // "General Ariculture" => "Agriculture".
                    if (promptTitle.startsWith("General ")) {
                        promptTitle=promptTitle.substring(8);
                    }
                    if (isInvitation) {
                        promptTitle += " - Invitation";
                    }
                }
            }

            // If we know what this prompt should be called, migrate it.
            if (StringUtils.isNotBlank(promptTitle)) {
                doMigration(promptFile, languageCode, promptTitle, promptId, categoryTbCategories);
            }

        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    /**
     * Performs the actual migration of a prompt file. Imports or updates the content to/in the repository,
     * sets the title, language code and category appropriately, and schedules the file to be removed from
     * the file system if user saves changes.
     * @param promptFile The file to be imported.
     * @param languageCode The language code of the file.
     * @param promptTitle The prompt title, as it should appear in the repository.
     * @param promptId The prompt-id, as it *might* already be in the repository. If this is found to be
     *                 the case, the audio item will be renamed.
     * @param category The category, TB_SYSTEM ('0-4-1') or TB_CATEGORY ('0-4-2').
     * @throws AudioItemRepository.UnsupportedFormatException * If the audio file is not in a supported format.
     * @throws BaseAudioConverter.ConversionException If there is an exception in the conversion process (corrupted file).
     * @throws IOException If there is an IO esception.
     * @throws AudioItemRepository.DuplicateItemException * If we try to add an item that's already been added.
     *
     *     * Can't happen in this code.
     */
    private void doMigration(File promptFile, String languageCode, String promptTitle, String promptId, Category category) throws AudioItemRepository.UnsupportedFormatException, BaseAudioConverter.ConversionException, IOException, AudioItemRepository.DuplicateItemException {
        PromptImportHandler handler = new PromptImportHandler(category, languageCode, promptTitle, promptFile.getName());
        SystemPrompts systemPrompts = new SystemPrompts(promptId, promptTitle, languageCode, category);
        AudioItem audioItem = systemPrompts.findPrompt();

        if (audioItem != null) {
            // Existing item; update content.
            File importableFile = promptFile.getAbsoluteFile();
            importer.updateAudioItemFromFile(audioItem, importableFile, handler);
        } else {
            // New item.
            audioItem = importer.importAudioItemFromFile(promptFile, handler);
            store.newAudioItem(audioItem);
        }

        // Schedule the file for deletion from the OS file system
        //ACMConfiguration.getInstance().getCurrentDB().getSandbox().delete(promptFile);
    }

    /**
     * Helper class to set the category, language, and title of an imported prompt.
     */
    private static class PromptImportHandler implements AudioImporter.AudioItemProcessor {
        private final Category category;
        private final String language;
        private final String title;
        private final String filename;

        PromptImportHandler(Category category, String language, String title, String sourceFilename) {
            this.category = category;
            this.language = language;
            this.title = title;
            this.filename = sourceFilename;
        }

        @Override
        public void process(AudioItem item) {
            assert item != null;

            item.removeAllCategories();
            item.addCategory(category);
            item.getMetadata().put(MetadataSpecification.DC_LANGUAGE, language);
            item.getMetadata().put(MetadataSpecification.DC_TITLE, title);
            item.getMetadata().put(MetadataSpecification.DC_SOURCE, filename);
        }
    }

    private boolean isAudioFile(String name) {
        return name.toLowerCase().endsWith(".a18") || name.toLowerCase().endsWith(".mp3") || name.toLowerCase().endsWith(".wav") ||
                name.toLowerCase().endsWith(".ogg") || name.toLowerCase().endsWith(".m4a") || name.toLowerCase().endsWith(".wma") || name.toLowerCase().endsWith(".aac");
    }
}
