package org.literacybridge.acm.gui.MainWindow;

import java.awt.BorderLayout;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;
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

import static org.literacybridge.acm.Constants.CATEGORY_TB_CATEGORIES;
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
}
