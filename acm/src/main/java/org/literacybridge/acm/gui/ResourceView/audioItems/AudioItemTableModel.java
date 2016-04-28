package org.literacybridge.acm.gui.ResourceView.audioItems;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.swing.table.AbstractTableModel;

import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.gui.Application;
import org.literacybridge.acm.gui.ResourceView.audioItems.ColumnInfo.ValueProvider;
import org.literacybridge.acm.gui.dialogs.audioItemPropertiesDialog.AudioItemPropertiesModel;
import org.literacybridge.acm.gui.resourcebundle.LabelProvider;
import org.literacybridge.acm.gui.util.AudioItemNode;
import org.literacybridge.acm.gui.util.UIUtils;
import org.literacybridge.acm.gui.util.language.LanguageUtil;
import org.literacybridge.acm.repository.AudioItemRepository.AudioFormat;
import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.store.Committable;
import org.literacybridge.acm.store.MetadataSpecification;
import org.literacybridge.acm.store.MetadataStore;
import org.literacybridge.acm.store.MetadataStore.DataChangeListener;
import org.literacybridge.acm.store.Playlist;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class AudioItemTableModel extends AbstractTableModel implements DataChangeListener {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    public static final ColumnInfo<String> INFO_ICON_COLUMN = ColumnInfo.newColumnInfo("", 25, ColumnInfo.WIDTH_NOT_SET, new ValueProvider<String>(true) {
        @Override
        protected AudioItemNode<String> getValue(AudioItem audioItem) {
            return new AudioItemNode<String>(audioItem, "");
        }
    });
    public static final ColumnInfo<String> TITLE_COLUMN = ColumnInfo.newMetadataColumnInfo(LabelProvider.AUDIO_ITEM_TABLE_COLUMN_TITLE, ColumnInfo.WIDTH_NOT_SET, 230, MetadataSpecification.DC_TITLE);
    public static final ColumnInfo<String> DURATION_COLUMN = ColumnInfo.newMetadataColumnInfo(LabelProvider.AUDIO_ITEM_TABLE_COLUMN_DURATION, ColumnInfo.WIDTH_NOT_SET, 65, MetadataSpecification.LB_DURATION);
    public static final ColumnInfo<String> CATEGORIES_COLUMN = ColumnInfo.newColumnInfo(LabelProvider.AUDIO_ITEM_TABLE_COLUMN_CATEGORIES, ColumnInfo.WIDTH_NOT_SET, 140, new ValueProvider<String>(true) {
        @Override
        protected AudioItemNode<String> getValue(AudioItem audioItem) {
            String value = UIUtils.getCategoryListAsString(audioItem);
            return new AudioItemNode<String>(audioItem, value);
        }
    });
    public static final ColumnInfo<String> SOURCE_COLUMN = ColumnInfo.newMetadataColumnInfo(LabelProvider.AUDIO_ITEM_TABLE_COLUMN_SOURCE, ColumnInfo.WIDTH_NOT_SET, 140, MetadataSpecification.DC_SOURCE);
    public static final ColumnInfo<String> LANGUAGES_COLUMN = ColumnInfo.newColumnInfo(LabelProvider.AUDIO_ITEM_TABLE_COLUMN_LANGUAGE, ColumnInfo.WIDTH_NOT_SET, 140, new ValueProvider<String>(true) {
        @Override
        protected AudioItemNode<String> getValue(AudioItem audioItem) {
            String value = LanguageUtil.getLocalizedLanguageName(AudioItemPropertiesModel
                    .getLanguage(audioItem, MetadataSpecification.DC_LANGUAGE));
            return new AudioItemNode<String>(audioItem, value);
        }
    });
    public static final ColumnInfo<String> DATE_FILE_MODIFIED_COLUMN = ColumnInfo.newColumnInfo(LabelProvider.AUDIO_ITEM_TABLE_COLUMN_DATE_FILE_MODIFIED, ColumnInfo.WIDTH_NOT_SET, 140, new ValueProvider<String>(true) {
        @Override
        protected AudioItemNode<String> getValue(AudioItem audioItem) {
            String value = "";
            File file = ACMConfiguration.getInstance().getCurrentDB().getRepository().getAudioFile(audioItem, AudioFormat.A18);
            if (file != null) {
                Date date = new Date(file.lastModified());
                value = DATE_FORMAT.format(date);
            }

            return new AudioItemNode<String>(audioItem, value);
        }
    });
    public static final ColumnInfo<Integer> PLAYLIST_ORDER_COLUMN = ColumnInfo.newColumnInfo(LabelProvider.AUDIO_ITEM_TABLE_COLUMN_PLAYLIST_ORDER, ColumnInfo.WIDTH_NOT_SET, 60, new ValueProvider<Integer>(false) {
        @Override
        protected AudioItemNode<Integer> getValue(AudioItem audioItem) {
            Playlist tag = Application.getFilterState().getSelectedPlaylist();
            int position = 0;
            if (tag != null) {
                position = tag.getAudioItemPosition(audioItem.getUuid()) + 1;
            }
            return new AudioItemNode<Integer>(audioItem, position);
        }
    }).setComparator(new Comparator<AudioItemNode<Integer>>() {
        @Override
        public int compare(AudioItemNode<Integer> o1,
                AudioItemNode<Integer> o2) {
            return Integer.compare(o1.getValue(), o2.getValue());
        }
    });

    private final MetadataStore store;

    private final Map<String, Integer> uuidToRowIndexMap;
    private final List<AudioItemNodeRow> rowIndexToUuidMap;

    private final ColumnInfo<?>[] columns;

    public AudioItemTableModel() {
        this.store = ACMConfiguration.getInstance().getCurrentDB().getMetadataStore();
        this.uuidToRowIndexMap = Maps.newHashMap();
        this.rowIndexToUuidMap = Lists.newArrayList();

        columns = initializeColumnInfoArray(
                INFO_ICON_COLUMN,
                TITLE_COLUMN,
                DURATION_COLUMN,
                CATEGORIES_COLUMN,
                SOURCE_COLUMN,
                LANGUAGES_COLUMN,
                DATE_FILE_MODIFIED_COLUMN,
                PLAYLIST_ORDER_COLUMN);

        // iterate over all audioItems and add them to the uuid->rowIndex map
        for (AudioItem item : store.getAudioItems()) {
            addNewAudioItem(item);
        }


        this.store.addDataChangeListener(this);
    }

    private ColumnInfo<?>[] initializeColumnInfoArray(ColumnInfo<?>... infos) {
        for (int columnIndex = 0; columnIndex < infos.length; columnIndex++) {
            infos[columnIndex].setColumnIndex(columnIndex);
        }
        return infos;
    }

    public String getAudioItemUuid(int rowIndex) {
        return rowIndexToUuidMap.get(rowIndex).audioItem.getUuid();
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
            return rowIndexToUuidMap.get(rowIndex).columns[columnIndex];
        } else {
            AudioItem audioItem = rowIndexToUuidMap.get(rowIndex).audioItem;
            return column.getValueProvider().getValue(audioItem);
        }
    }

    private AudioItemNodeRow convertToAudioItemNodeRow(AudioItem audioItem) {
        AudioItemNodeRow audioItemNodeRow = new AudioItemNodeRow(audioItem, columns.length);

        for (int columnIndex = 0; columnIndex < columns.length; columnIndex++) {
            ValueProvider<?> valueProvider = columns[columnIndex].getValueProvider();
            if (valueProvider.isValueCachable()) {
                audioItemNodeRow.columns[columnIndex] = valueProvider.getValue(audioItem);
            }
        }

        return audioItemNodeRow;
    }

    private int addNewAudioItem(AudioItem item) {
        int row = rowIndexToUuidMap.size();
        uuidToRowIndexMap.put(item.getUuid(), row);
        rowIndexToUuidMap.add(convertToAudioItemNodeRow(item));
        return row;
    }

    private static final class AudioItemNodeRow {
        final AudioItem audioItem;
        final AudioItemNode<?>[] columns;

        AudioItemNodeRow(AudioItem audioItem, int numColumns) {
            this.audioItem = audioItem;
            columns = new AudioItemNode[numColumns];
        }
    }

    @Override
    public void fireChangeEvent(Committable item, DataChangeEventType eventType) {
        if (item instanceof AudioItem) {
            AudioItem audioItem = (AudioItem) item;
            if (eventType == DataChangeEventType.ITEM_ADDED) {
                int row = addNewAudioItem(audioItem);
                fireTableRowsInserted(row, row);
            } else {
                int row = uuidToRowIndexMap.get(audioItem.getUuid());

                if (eventType == DataChangeEventType.ITEM_MODIFIED) {
                    rowIndexToUuidMap.set(row, convertToAudioItemNodeRow(audioItem));
                    fireTableRowsUpdated(row, row);
                } else if (eventType == DataChangeEventType.ITEM_DELETED) {
                    uuidToRowIndexMap.remove(audioItem.getUuid());
                    rowIndexToUuidMap.remove(row);
                    fireTableRowsDeleted(row, row);
                }
            }
        }
    }
}
