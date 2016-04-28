package org.literacybridge.acm.gui.ResourceView.audioItems;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.swing.table.AbstractTableModel;

import org.apache.commons.lang.StringUtils;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.gui.Application;
import org.literacybridge.acm.gui.dialogs.audioItemPropertiesDialog.AudioItemPropertiesModel;
import org.literacybridge.acm.gui.util.AudioItemNode;
import org.literacybridge.acm.gui.util.UIUtils;
import org.literacybridge.acm.gui.util.language.LanguageUtil;
import org.literacybridge.acm.repository.AudioItemRepository.AudioFormat;
import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.store.Committable;
import org.literacybridge.acm.store.MetadataSpecification;
import org.literacybridge.acm.store.MetadataStore;
import org.literacybridge.acm.store.MetadataStore.DataChangeListener;
import org.literacybridge.acm.store.MetadataValue;
import org.literacybridge.acm.store.Playlist;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class AudioItemTableModel extends AbstractTableModel implements DataChangeListener {

    private static final long serialVersionUID = -2998511081572936717L;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    // positions of the table columns
    public static final int NUM_COLUMNS 	   = 8; // keep in sync
    public static final int INFO_ICON 		   = 0;
    public static final int TITLE 			   = 1;
    public static final int DURATION 		   = 2;
    public static final int CATEGORIES 		   = 3;
    public static final int SOURCE			   = 4;
    public static final int LANGUAGES          = 5;
    public static final int DATE_FILE_MODIFIED = 6;
    public static final int PLAYLIST_ORDER	   = 7;
    private static String[] columns = null;

    private final MetadataStore store;

    private final Map<String, Integer> uuidToRowIndexMap;
    private final List<AudioItemNodeRow> rowIndexToUuidMap;

    public static void initializeTableColumns(String[] initalColumnNames) {
        columns = initalColumnNames;
    }

    public AudioItemTableModel() {
        this.store = ACMConfiguration.getInstance().getCurrentDB().getMetadataStore();
        this.uuidToRowIndexMap = Maps.newHashMap();
        this.rowIndexToUuidMap = Lists.newArrayList();

        // iterate over all audioItems and add them to the uuid->rowIndex map
        for (AudioItem item : store.getAudioItems()) {
            addNewAudioItem(item);
        }

        this.store.addDataChangeListener(this);
    }

    public String getAudioItemUuid(int rowIndex) {
        return rowIndexToUuidMap.get(rowIndex).audioItem.getUuid();
    }

    @Override
    public int getColumnCount() {
        return columns.length;
    }

    @Override
    public String getColumnName(int column) {
        return columns[column];
    }

    @Override
    public int getRowCount() {
        return uuidToRowIndexMap.size();
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (columnIndex != PLAYLIST_ORDER) {
            return rowIndexToUuidMap.get(rowIndex).columns[columnIndex];
        } else {
            AudioItem audioItem = rowIndexToUuidMap.get(rowIndex).audioItem;
            Playlist tag = Application.getFilterState().getSelectedPlaylist();
            int position = 0;
            if (tag != null) {
                position = tag.getAudioItemPosition(audioItem.getUuid()) + 1;
            }
            return new AudioItemNode<Integer>(audioItem, position);
        }
    }

    private AudioItemNodeRow convertToAudioItemNodeRow(AudioItem audioItem) {
        AudioItemNodeRow audioItemNodeRow = new AudioItemNodeRow(audioItem);

        for (int columnIndex = 0; columnIndex < NUM_COLUMNS; columnIndex++) {
            String cellText = "";
            try {
                switch (columnIndex) {
                case INFO_ICON: {
                    cellText = "";
                    break;
                }
                case TITLE: {
                    MetadataValue<String> value = audioItem.getMetadata().getMetadataValue(
                            MetadataSpecification.DC_TITLE);
                    if (value != null) {
                        cellText = value.getValue();
                    }
                    break;
                }
                case DURATION: {
                    MetadataValue<String> value = audioItem.getMetadata().getMetadataValue(
                            MetadataSpecification.LB_DURATION);
                    if (value != null && !StringUtils.isEmpty(value.getValue())) {
                        cellText = value.getValue();
                    }
                    break;
                }
                case CATEGORIES: {
                    cellText = UIUtils.getCategoryListAsString(audioItem);
                    break;
                }
                case SOURCE: {
                    MetadataValue<String> value = audioItem.getMetadata().getMetadataValue(
                            MetadataSpecification.DC_SOURCE);
                    if (value != null) {
                        cellText = value.getValue();
                    }
                    break;
                }

                case LANGUAGES: {
                    cellText = LanguageUtil.getLocalizedLanguageName(AudioItemPropertiesModel
                            .getLanguage(audioItem, MetadataSpecification.DC_LANGUAGE));
                    break;
                }

                case DATE_FILE_MODIFIED: {
                    File file = ACMConfiguration.getInstance().getCurrentDB().getRepository().getAudioFile(audioItem, AudioFormat.A18);
                    if (file != null) {
                        Date date = new Date(file.lastModified());
                        cellText = dateFormat.format(date);
                    }
                    break;
                }

                default: {
                    cellText = "";
                    break;
                }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            audioItemNodeRow.columns[columnIndex] = new AudioItemNode<String>(audioItem, cellText);
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

        AudioItemNodeRow(AudioItem audioItem) {
            this.audioItem = audioItem;
            columns = new AudioItemNode[NUM_COLUMNS];
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
