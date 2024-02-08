package org.literacybridge.acm.gui.MainWindow;

import java.awt.BorderLayout;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import javax.swing.JSplitPane;

import org.apache.commons.io.FilenameUtils;
import org.literacybridge.acm.Constants;
import org.literacybridge.acm.audioconverter.converters.BaseAudioConverter;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.gui.Application;
import org.literacybridge.acm.gui.MainWindow.audioItems.AudioItemView;
import org.literacybridge.acm.gui.assistants.Deployment.PlaylistPrompts;
import org.literacybridge.acm.gui.assistants.Deployment.SystemPrompts;
import org.literacybridge.acm.gui.assistants.PromptsImport.PromptsInfo;
import org.literacybridge.acm.gui.assistants.util.AcmContent;
import org.literacybridge.acm.gui.util.ACMContainer;
import org.literacybridge.acm.importexport.AudioImporter;
import org.literacybridge.acm.repository.AudioItemRepository;
import org.literacybridge.acm.store.*;

import static org.literacybridge.acm.Constants.CATEGORY_TB_SYSTEM;

public class MainView extends ACMContainer {
  private static final long serialVersionUID = 1464102221036629153L;

  public AudioItemView audioItemView;
  private SidebarView sidebarView;

  public AudioItemView getAudioItemView() {
    return audioItemView;
  }

  public SidebarView getSidebarView() {
    return sidebarView;
  }

  public MainView() {
      createViewComponents();
      storeSystemPrompts();
  }

  private void createViewComponents() {
    setLayout(new BorderLayout());

    final MetadataStore store = ACMConfiguration.getInstance().getCurrentDB()
        .getMetadataStore();
    SearchResult result = store.search("", null);

    // Table with audio items
    audioItemView = new AudioItemView();
    audioItemView.setData(result);

    // Tree with categories
    // Create at the end, because this is the main selection provider
    sidebarView = new SidebarView(result);

    JSplitPane sp = new JSplitPane();
    // left-side
    sp.setLeftComponent(sidebarView);
    // right-side
    sp.setRightComponent(audioItemView);

    sp.setOneTouchExpandable(true);
    sp.setContinuousLayout(true);
    sp.setDividerLocation(300);

    add(BorderLayout.CENTER, sp);

    Application.getMessageService().pumpMessage(result);
  }

  public static void updateDataRequestResult() {
    final MetadataStore store = ACMConfiguration.getInstance().getCurrentDB()
        .getMetadataStore();
    SearchResult result = store.search("", null);
    Application.getMessageService().pumpMessage(result);
  }

    private void storeSystemPrompts() {
        // read all system prompts from known file location and tag them
        MetadataStore store = ACMConfiguration.getInstance().getCurrentDB().getMetadataStore();
        Map<String, PromptsInfo.PromptInfo> storePromptsInfoMap = store.getPromptsMap();
        Category systemCategory = store.getTaxonomy().getCategory(CATEGORY_TB_SYSTEM);
        AudioImporter importer = AudioImporter.getInstance();

        File tbLoadersDir = ACMConfiguration.getInstance().getCurrentDB().getProgramTbLoadersDir();
        String languagesPath = "TB_Options" + File.separator + "languages";
        File languagesDir = new File(tbLoadersDir, languagesPath);

        for (File languageCodeDir : languagesDir.listFiles()) {
            if (languageCodeDir.isDirectory()) {
                //File[] a18Files = languageCodeDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".a18"));
                for (File a18File : languageCodeDir.listFiles()) {
                    if (a18File.isDirectory()) {
                        String dirname = a18File.getName();
                        System.out.println(dirname);
                        if (dirname.equals("cat")) {
                            // playlist prompts
                            Category ufCategory = store.getCategory(Constants.CATEGORY_UNCATEGORIZED_FEEDBACK);
                            for (File catFiles : a18File.listFiles()) {
                                System.out.println(catFiles.getName());
                                String playlistName = FilenameUtils.removeExtension(catFiles.getName());
                                if (store.findPlaylistByName(playlistName) == null) {
                                    Playlist playlist = store.newPlaylist(playlistName);
                                    try {
                                        ImportHandler handler = new ImportHandler(ufCategory, languageCodeDir.getName(), playlistName);
                                        AudioItem audioItem = importer.importAudioItemFromFile(catFiles, handler);
                                        playlist.addAudioItem(audioItem);
                                        store.commit(playlist);
                                    } catch (IOException e) {
                                        continue;
                                    } catch (BaseAudioConverter.ConversionException e) {
                                        throw new RuntimeException(e);
                                    } catch (AudioItemRepository.UnsupportedFormatException e) {
                                        throw new RuntimeException(e);
                                    } catch (AudioItemRepository.DuplicateItemException e) {
                                        throw new RuntimeException(e);
                                    }
                                }
                            }
                        }
                    }
                    if (a18File.getName().toLowerCase().endsWith(".a18")) {
                        String filename = a18File.getName();
                        filename = FilenameUtils.removeExtension(filename);
                        try {
                            if (storePromptsInfoMap.containsKey(filename) || filename.equals(0) || filename.equals(7)) {
                                SystemPrompts tempPromts = new SystemPrompts(filename, languageCodeDir.getName());
                                boolean exists = tempPromts.findPrompts();
                                if (exists) {
                                    continue;
                                }
                                ImportHandler handler = new ImportHandler(systemCategory, languageCodeDir.getName(), filename);
                                AudioItem audioItem = importer.importAudioItemFromFile(a18File, handler);
                                store.newAudioItem(audioItem);
                            }
                        } catch (Exception e) {

                        }
                    }
                }
            }
        }
    }

    private class ImportHandler implements AudioImporter.AudioItemProcessor {
        private final Category category;
        private final String language;
        private final String filename;

        ImportHandler(Category category, String language, String filename) {
            this.category = category;
            this.language = language;
            this.filename = filename;
        }

        @Override
        public void process(AudioItem item) {
            if (item != null) {
                item.addCategory(category);
            }
            item.getMetadata().put(MetadataSpecification.DC_LANGUAGE, language);
            item.getMetadata().put(MetadataSpecification.DC_TITLE, filename);
        }
    }
}
