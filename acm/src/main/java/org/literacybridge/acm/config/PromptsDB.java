package org.literacybridge.acm.config;

import org.apache.commons.io.FilenameUtils;
import org.literacybridge.acm.gui.assistants.Deployment.SystemPrompts;
import org.literacybridge.acm.gui.assistants.PromptsImport.PromptsInfo;
import org.literacybridge.acm.importexport.AudioImporter;
import org.literacybridge.acm.store.*;

import java.io.File;

import static org.literacybridge.acm.Constants.CATEGORY_TB_SYSTEM;

public class PromptsDB {
    private MetadataStore store;

    PromptsDB() {
        store = ACMConfiguration.getInstance()
                .getCurrentDB()
                .getMetadataStore();
    }

    public void migrateSystemPrompts() {
        // read all system prompts from known file location and tag them
        Category systemCategory = store.getTaxonomy().getCategory(CATEGORY_TB_SYSTEM);
        AudioImporter importer = AudioImporter.getInstance();
        ACMConfiguration acm = ACMConfiguration.getInstance();
        DBConfiguration config = ACMConfiguration.getInstance().getCurrentDB();

        // Get directory containing all language system prompts
        File tbLoadersDir = ACMConfiguration.getInstance().getCurrentDB().getProgramTbLoadersDir();
        String languagesPath = "TB_Options" + File.separator + "languages";
        File languagesDir = new File(tbLoadersDir, languagesPath);

        PromptsInfo promptsInfo = new PromptsInfo();

        // Directory containing language codes
        for (File languageCodeDir : languagesDir.listFiles()) {
            if (languageCodeDir.isDirectory()) {
                for (File dirFile : languageCodeDir.listFiles()) {
                    String languageCode = languageCodeDir.getName();
                    // are there subdirectories in the language folders?
                    if (dirFile.isDirectory()) {
                        String dirname = dirFile.getName();
                        /*if (dirname.equals("cat")) {
                            // playlist prompts
                            // Lookup names in General Agriculture
                            for (File catFiles : a18File.listFiles()) {
                                String playlistName = FilenameUtils.removeExtension(catFiles.getName());
                                // Do a name lookup
                                PlaylistPrompts playlistPrompts = new PlaylistPrompts(playlistName, languageCode);
                                Collection<Playlist> playListCollection = store.getPlaylists();
                                String _s = "";
                                if (store.findPlaylistByName(playlistName) == null) {
                                    Playlist playlist = store.newPlaylist(playlistName);
                                    String s = "";
                                    /*try {
                                        ImportHandler handler = new ImportHandler(tbCategory, languageCode, playlistName);
                                        AudioItem audioItem = importer.importAudioItemFromFile(catFiles, handler);
                                        playlist.addAudioItem(audioItem);
                                        store.commit(playlist);
                                    } catch (IOException e) {
                                        System.out.print("Failure creating audio item for ");
                                        System.out.println(catFiles.getName());
                                    } catch (BaseAudioConverter.ConversionException e) {
                                        throw new RuntimeException(e);
                                    } catch (AudioItemRepository.UnsupportedFormatException e) {
                                        throw new RuntimeException(e);
                                    } catch (AudioItemRepository.DuplicateItemException e) {
                                        throw new RuntimeException(e);
                                    }
                                }
                            }
                        }*/
                    }
                    if (dirFile.getName().toLowerCase().endsWith(".a18") || dirFile.getName().toLowerCase().endsWith(".mp3") ||
                            dirFile.getName().toLowerCase().endsWith(".wav") || dirFile.getName().toLowerCase().endsWith(".ogg")) {
                        String fileId = FilenameUtils.removeExtension(dirFile.getName());
                        try {
                            // prompts 0 & 7 are not handled atm
                            PromptsInfo.PromptInfo tempPromptInfo = promptsInfo.getPrompt(fileId);
                            if (tempPromptInfo != null) {
                                String desc = tempPromptInfo.getFilename();
                                SystemPrompts tempSystemPrompts = new SystemPrompts(fileId, desc, languageCode);
                                // Save file with a18 extension
                                boolean exists = tempSystemPrompts.findPrompts();
                                ImportHandler handler = new ImportHandler(systemCategory, languageCode, fileId, desc);
                                if (exists) {
                                    // if the file already exists, rename to phrases we can understand
                                    //tempSystemPrompts.renamePrompt(desc);
                                        /*AudioItem audioItem = null;
                                        if (tempSystemPrompts.longPromptItem != null) {
                                            audioItem = tempSystemPrompts.longPromptItem;
                                        } else {
                                            audioItem = tempSystemPrompts.shortPromptItem;
                                        }*/
                                    //importer.updateAudioItemFromFile(audioItem, importableFile, handler);
                                } else {
                                    // store the audio item in store
                                    AudioItem audioItem = importer.importAudioItemFromFile(dirFile, handler);
                                    store.newAudioItem(audioItem);
                                }
                            }
                            // now delete the file from the windows file system
                            //dirFile.delete();
                        } catch (Exception e) {

                        }
                    }
                }
            }
            // delete the language
            //languageCodeDir.delete();
        }
    }

    private class ImportHandler implements AudioImporter.AudioItemProcessor {
        private final Category category;
        private final String language;
        private final String fileId;
        private final String description;

        ImportHandler(Category category, String language, String fileId, String description) {
            this.category = category;
            this.language = language;
            this.fileId = fileId;
            this.description = description;
        }

        @Override
        public void process(AudioItem item) {
            if (item != null) {
                item.addCategory(category);
            }
            item.getMetadata().put(MetadataSpecification.DC_LANGUAGE, language);
            item.getMetadata().put(MetadataSpecification.DC_TITLE, description);
            item.getMetadata().put(MetadataSpecification.DC_IDENTIFIER, fileId);
        }
    }
}
