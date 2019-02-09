package org.literacybridge.acm.gui.ResourceView;

import java.awt.BorderLayout;

import javax.swing.JSplitPane;

import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.gui.Application;
import org.literacybridge.acm.gui.ResourceView.audioItems.AudioItemView;
import org.literacybridge.acm.gui.util.ACMContainer;
import org.literacybridge.acm.store.MetadataStore;
import org.literacybridge.acm.store.SearchResult;

public class ResourceView extends ACMContainer {
  private static final long serialVersionUID = 1464102221036629153L;

  public AudioItemView audioItemView;
  private SidebarView sidebarView;

  public AudioItemView getAudioItemView() {
    return audioItemView;
  }

  public SidebarView getSidebarView() {
    return sidebarView;
  }

  public ResourceView() {
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
