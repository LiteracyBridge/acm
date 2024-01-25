package org.literacybridge.acm.store;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.*;

import com.google.common.collect.Lists;
import com.opencsv.CSVReader;
import org.apache.commons.io.input.BOMInputStream;
import org.literacybridge.acm.gui.assistants.PromptsImport.PromptImportAssistant;
import org.literacybridge.acm.gui.assistants.PromptsImport.PromptTarget;
import org.literacybridge.acm.gui.assistants.PromptsImport.PromptsInfo;

public abstract class MetadataStore {
    private final Taxonomy taxonomy;

    private final List<DataChangeListener> dataChangeListeners;
    private boolean haveChanges = false;
    private long changeCount = 0L;

    Map<String, PromptsInfo.PromptInfo> promptsMap = new LinkedHashMap<>();

    public static final String PROMPTS_FILE_NAME = "prompts_ex.csv";
    public void loadPromptsInfo() {
        InputStream csvStream = PromptImportAssistant.class.getClassLoader()
                .getResourceAsStream(PROMPTS_FILE_NAME);
        try (BOMInputStream bis = new BOMInputStream(csvStream);
             Reader ir = new InputStreamReader(bis);
             CSVReader reader = new CSVReader(ir)) {
            Collection<PromptsInfo.PromptInfo> prompts = new LinkedList<>();
            for (String[] line : reader.readAll()) {
                prompts.add(new PromptsInfo.PromptInfo(line));
            }
            prompts.forEach(i->promptsMap.put(i.getId(), i));
        } catch (Exception ignored) {
            // Ignore
        }
    }

    public Map<String, PromptsInfo.PromptInfo> getPromptsMap() {
        return this.promptsMap;
    }

    public abstract Transaction newTransaction();

    public abstract AudioItem newAudioItem(String uid);

    public abstract AudioItem getAudioItem(String uid);

    public abstract void deleteAudioItem(String uid);

    public abstract Collection<AudioItem> getAudioItems();

    public abstract Playlist newPlaylist(String name);

    public abstract Playlist getPlaylist(String uid);

    public abstract Playlist findPlaylistByName(String name);

    public abstract void deletePlaylist(String uid);

  public abstract Collection<Playlist> getPlaylists();

  public boolean hasChanges() {
    return haveChanges;
  }

  public long getChangeCount() {
      return changeCount;
  }

  public MetadataStore(Taxonomy taxonomy) {
    this.taxonomy = taxonomy;
    this.changeCount++;
    dataChangeListeners = Lists.newLinkedList();
  }

  public final Taxonomy getTaxonomy() {
    return taxonomy;
  }

  public final Category getCategory(String uid) {
    return taxonomy.getCategory(uid);
  }

  public final void addDataChangeListener(DataChangeListener listener) {
    this.dataChangeListeners.add(listener);
  }

  protected final void fireChangeEvent(List<DataChangeEvent> events) {
    haveChanges = true;
    for (DataChangeListener listener : dataChangeListeners) {
      listener.dataChanged(events);
    }
  }

  public abstract SearchResult search(String searchFilter,
      List<Category> categories, List<Locale> locales);

  public abstract SearchResult search(String searchFilter, Playlist selectedPlaylist);

  public final void commit(Committable... objects) throws IOException {
    for (Committable c : objects) {
      c.ensureIsCommittable();
    }

    Transaction t = newTransaction();
    t.addAll(objects);
    t.commit();
  }

  public static class DataChangeEvent {
    private final Committable item;
    private final DataChangeEventType eventType;

    public DataChangeEvent(Committable item, DataChangeEventType eventType) {
      this.item = item;
      this.eventType = eventType;
    }

    public Committable getItem() {
      return item;
    }

    public DataChangeEventType getEventType() {
      return eventType;
    }
  }

  public enum DataChangeEventType {
    ITEM_ADDED, ITEM_MODIFIED, ITEM_DELETED
  }

  public interface DataChangeListener {
    void dataChanged(List<DataChangeEvent> events);
  }
}
