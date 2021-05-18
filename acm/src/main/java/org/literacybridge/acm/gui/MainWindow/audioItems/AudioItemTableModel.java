package org.literacybridge.acm.gui.MainWindow.audioItems;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.gui.Application;
import org.literacybridge.acm.gui.MainWindow.audioItems.ColumnInfo.ValueProvider;
import org.literacybridge.acm.gui.dialogs.audioItemPropertiesDialog.AudioItemPropertiesModel;
import org.literacybridge.acm.gui.resourcebundle.LabelProvider;
import org.literacybridge.acm.gui.util.AudioItemNode;
import org.literacybridge.acm.gui.util.UIUtils;
import org.literacybridge.acm.gui.util.language.LanguageUtil;
import org.literacybridge.acm.repository.AudioItemRepository;
import org.literacybridge.acm.repository.AudioItemRepository.AudioFormat;
import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.store.Committable;
import org.literacybridge.acm.store.MetadataSpecification;
import org.literacybridge.acm.store.MetadataStore;
import org.literacybridge.acm.store.MetadataStore.DataChangeListener;
import org.literacybridge.acm.store.Playlist;

import javax.swing.table.AbstractTableModel;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AudioItemTableModel extends AbstractTableModel {
  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat(
      "yyyy-MM-dd");

    public static final ColumnInfo<String> infoIconColumn = ColumnInfo.newColumnInfo("",
        25,
        ColumnInfo.WIDTH_NOT_SET,
        new ValueProvider<String>(true) {
            @Override
            protected AudioItemNode<String> getValue(AudioItem audioItem) {
                return new AudioItemNode<>(audioItem, "");
            }
        });

    public static final ColumnInfo<String> titleColumn = ColumnInfo.newMetadataColumnInfo(
        LabelProvider.AUDIO_ITEM_TABLE_COLUMN_TITLE,
        ColumnInfo.WIDTH_NOT_SET,
        230,
        MetadataSpecification.DC_TITLE);

    private static final ColumnInfo<String> durationColumn = ColumnInfo.newMetadataColumnInfo(
        LabelProvider.AUDIO_ITEM_TABLE_COLUMN_DURATION,
        ColumnInfo.WIDTH_NOT_SET,
        85,
        MetadataSpecification.LB_DURATION);

    private static final ColumnInfo<String> categoriesColumn = ColumnInfo.newColumnInfo(
        LabelProvider.AUDIO_ITEM_TABLE_COLUMN_CATEGORIES,
        ColumnInfo.WIDTH_NOT_SET,
        140,
        new ValueProvider<String>(true) {
            @Override
            protected AudioItemNode<String> getValue(AudioItem audioItem) {
                String value = UIUtils.getCategoryNamesAsString(audioItem);
                return new AudioItemNode<>(audioItem, value);
            }
        });

    private static final ColumnInfo<String> sourceColumn = ColumnInfo.newMetadataColumnInfo(
        LabelProvider.AUDIO_ITEM_TABLE_COLUMN_SOURCE,
        ColumnInfo.WIDTH_NOT_SET,
        140,
        MetadataSpecification.DC_SOURCE);

    public static final ColumnInfo<String> languagesColumn = ColumnInfo.newColumnInfo(LabelProvider.AUDIO_ITEM_TABLE_COLUMN_LANGUAGE,
        ColumnInfo.WIDTH_NOT_SET,
        140,
        new ValueProvider<String>(true) {
            @Override
            protected AudioItemNode<String> getValue(AudioItem audioItem) {
                Locale locale = AudioItemPropertiesModel.getLanguage(audioItem,
                    MetadataSpecification.DC_LANGUAGE);
                String label = LanguageUtil.getLocalizedLanguageName(locale);

                return new AudioItemNode<>(audioItem, label);
            }
        });

    public static final ColumnInfo<String> dateFileModifiedColumn = ColumnInfo.newColumnInfo(
        LabelProvider.AUDIO_ITEM_TABLE_COLUMN_DATE_FILE_MODIFIED,
        ColumnInfo.WIDTH_NOT_SET,
        140,
        new ValueProvider<String>(true) {
            @Override
            protected AudioItemNode<String> getValue(AudioItem audioItem) {
                String value = "";
                AudioItemRepository repository = ACMConfiguration.getInstance().getCurrentDB().getRepository();
                File file = repository.findAudioFileWithFormat(audioItem, AudioFormat.A18);
                if (file != null) {
                    Date date = new Date(file.lastModified());
                    value = DATE_FORMAT.format(date);
                }

                return new AudioItemNode<>(audioItem, value);
            }
        });

    static final ColumnInfo<String> sdgGoalsColumn = ColumnInfo.newMetadataColumnInfo(
        LabelProvider.AUDIO_ITEM_TABLE_COLUMN_SDG_GOALS,
        ColumnInfo.WIDTH_NOT_SET,
        140,
        MetadataSpecification.LB_SDG_GOALS);

    static final ColumnInfo<String> sdgTargetsColumn = ColumnInfo.newMetadataColumnInfo(
        LabelProvider.AUDIO_ITEM_TABLE_COLUMN_SDG_TARGETS,
        ColumnInfo.WIDTH_NOT_SET,
        140,
        MetadataSpecification.LB_SDG_TARGETS);

    static final ColumnInfo<String> contentidColumn = ColumnInfo.newMetadataColumnInfo(
        LabelProvider.AUDIO_ITEM_TABLE_COLUMN_CONTENTID,
        ColumnInfo.WIDTH_NOT_SET,
        140,
        MetadataSpecification.DC_IDENTIFIER);

    // DC_PUBLISHER
    // LB_MESSAGE_FORMAT
    // LB_TARGET_AUDIENCE
    // LB_DATE_RECORDED
    // LB_PRIMARY_SPEAKER

    public static final ColumnInfo<Integer> playlistOrderColumn = ColumnInfo.newColumnInfo(
        LabelProvider.AUDIO_ITEM_TABLE_COLUMN_PLAYLIST_ORDER,
        ColumnInfo.WIDTH_NOT_SET,
        60,
        new ValueProvider<Integer>(false) {
            @Override
            protected AudioItemNode<Integer> getValue(AudioItem audioItem) {
                // If there is a playlist selected, return this item's position in it. Will be
                // -1 if the item is not in the playlist.
                Playlist playlist = Application.getFilterState().getSelectedPlaylist();
                int position = 0;
                if (playlist != null) {
                    position = playlist.getAudioItemPosition(audioItem.getId()) + 1;
                }
                return new AudioItemNode<>(audioItem, position);
            }
        }).setComparator(Comparator.comparingInt(AudioItemNode::getValue));

    private final Map<String, Integer> uuidToRowIndexMap;
  private final List<AudioItemNodeRow> rowIndexToIdMap;

  private final ColumnInfo<?>[] columns;
  final int[] defaultHiddenColumns;

  AudioItemTableModel() {
      MetadataStore store = ACMConfiguration.getInstance().getCurrentDB()
          .getMetadataStore();
    this.uuidToRowIndexMap = Maps.newHashMap();
    this.rowIndexToIdMap = Lists.newArrayList();

    columns = initializeColumnInfoArray(infoIconColumn, contentidColumn, titleColumn,
        durationColumn, categoriesColumn, sourceColumn, languagesColumn,
        dateFileModifiedColumn, /*correlationIdColumn,*/ sdgGoalsColumn,
        sdgTargetsColumn, playlistOrderColumn);

    defaultHiddenColumns = new int[] {contentidColumn.getColumnIndex()};

    for (AudioItem item : store.getAudioItems()) {
      addNewAudioItem(item);
    }

    store.addDataChangeListener(storeChangedListener);
  }

    private final DataChangeListener storeChangedListener = new DataChangeListener() {
        @Override
        public void dataChanged(List <MetadataStore.DataChangeEvent> events) {
            if (events.size() > 1) {
                // bulk update
                fireTableDataChanged();
            } else {
                // optimized case for single row update
                Committable item = events.get(0).getItem();
                MetadataStore.DataChangeEventType eventType = events.get(0).getEventType();

                if (item instanceof AudioItem) {
                    AudioItem audioItem = (AudioItem) item;
                    if (eventType == MetadataStore.DataChangeEventType.ITEM_ADDED) {
                        int row = addNewAudioItem(audioItem);
                        fireTableRowsInserted(row, row);
                    } else {
                        int row = uuidToRowIndexMap.get(audioItem.getId());

                        if (eventType == MetadataStore.DataChangeEventType.ITEM_MODIFIED) {
                            rowIndexToIdMap.set(row, convertToAudioItemNodeRow(audioItem));
                            fireTableRowsUpdated(row, row);
                        } else if (eventType == MetadataStore.DataChangeEventType.ITEM_DELETED) {
                            removeAudioItem(audioItem);
                            fireTableRowsDeleted(row, row);
                        }
                    }
                }
            }
        }
    };


  private ColumnInfo<?>[] initializeColumnInfoArray(ColumnInfo<?>... infos) {
    for (int columnIndex = 0; columnIndex < infos.length; columnIndex++) {
      infos[columnIndex].setColumnIndex(columnIndex);
    }
    return infos;
  }

  public String getAudioItemId(int rowIndex) {
    return rowIndexToIdMap.get(rowIndex).audioItem.getId();
  }

  public ColumnInfo<?>[] getColumnInfos() {
    return columns;
  }

  @Override
  public int getColumnCount() {
    return columns.length;
  }

  @Override
  public String getColumnName(int column) {
    return columns[column].getColumnName(LanguageUtil.getUILanguage());
  }

  @Override
  public int getRowCount() {
    return uuidToRowIndexMap.size();
  }

  @Override
  public Object getValueAt(int rowIndex, int columnIndex) {
    ColumnInfo<?> column = columns[columnIndex];
    if (column.getValueProvider().isValueCachable()) {
      return rowIndexToIdMap.get(rowIndex).columns[columnIndex];
    } else {
      AudioItem audioItem = rowIndexToIdMap.get(rowIndex).audioItem;
      return column.getValueProvider().getValue(audioItem);
    }
  }

    private AudioItemNodeRow convertToAudioItemNodeRow(AudioItem audioItem) {
    AudioItemNodeRow audioItemNodeRow = new AudioItemNodeRow(audioItem,
        columns.length);

    for (int columnIndex = 0; columnIndex < columns.length; columnIndex++) {
      ValueProvider<?> valueProvider = columns[columnIndex].getValueProvider();
      if (valueProvider.isValueCachable()) {
        audioItemNodeRow.columns[columnIndex] = valueProvider
            .getValue(audioItem);
      }
    }

    return audioItemNodeRow;
  }

  private int addNewAudioItem(AudioItem item) {
    int row = rowIndexToIdMap.size();
    uuidToRowIndexMap.put(item.getId(), row);
    rowIndexToIdMap.add(convertToAudioItemNodeRow(item));
    return row;
  }

  /**
   * Removes an item from the id->index and index->id maps. Adjusts indices as necessary.
   * @param item The audio item to be removed.
     */
  private void removeAudioItem(AudioItem item) {
    int row = uuidToRowIndexMap.get(item.getId());
    // Adjust higher numbered rows in the id -> row map to account for row removed in middle.
    for (Map.Entry<String,Integer> e : uuidToRowIndexMap.entrySet()) {
      if (e.getValue() > row) {
        e.setValue(e.getValue() - 1);
      }
    }
    uuidToRowIndexMap.remove(item.getId());
    rowIndexToIdMap.remove(row);
  }

  /**
   * One instance of this class represents one materialized table row in
   * rowIndexToUuidMap.
   */
  private static final class AudioItemNodeRow {
    final AudioItem audioItem;
    final AudioItemNode<?>[] columns;

    AudioItemNodeRow(AudioItem audioItem, int numColumns) {
      this.audioItem = audioItem;
      columns = new AudioItemNode[numColumns];
    }
  }

}
