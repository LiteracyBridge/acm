package org.literacybridge.acm.gui.MainWindow.audioItems;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Locale;
import java.util.Observable;
import java.util.Observer;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableColumn;

import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.decorator.HighlighterFactory;
import org.jdesktop.swingx.sort.TableSortController;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.gui.Application;
import org.literacybridge.acm.gui.messages.AudioItemTableSortOrderMessage;
import org.literacybridge.acm.gui.messages.PlayAudioItemMessage;
import org.literacybridge.acm.gui.messages.RequestAndSelectAudioItemMessage;
import org.literacybridge.acm.gui.messages.RequestAudioItemMessage;
import org.literacybridge.acm.gui.messages.RequestAudioItemToPlayMessage;
import org.literacybridge.acm.gui.messages.RequestedAudioItemMessage;
import org.literacybridge.acm.gui.util.AudioItemNode;
import org.literacybridge.acm.gui.util.language.UILanguageChanged;
import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.store.MetadataSpecification;
import org.literacybridge.acm.store.MetadataValue;
import org.literacybridge.acm.store.SearchResult;

public class AudioItemView extends Container {

  private static final long serialVersionUID = -2886958461177831842L;

  public static final DataFlavor AudioItemDataFlavor = new DataFlavor(
      AudioItem.class, "LocalizedAudioItem");
    private static final int FAST_TOOLTIP_DELAY = 50;
    private SearchResult currResult = null;
  JXTable audioItemTable = null;

  private AudioItemViewMouseListener mouseListener;
  private final AudioItemTableModel tableModel;

  private TableColumn playlistOrderColumn;
  private TableColumn correlationIdColumn;
  private boolean firstDataSet = false;
  private boolean showCorrelationId = ACMConfiguration.getInstance().getCurrentDB().getNextCorrelationId() > 0;

  public AudioItemView() {
    setLayout(new BorderLayout());
    tableModel = new AudioItemTableModel();
    createTable();
    addHandler();
    addToMessageService();
  }

  private void addToMessageService() {
    Application.getMessageService().addObserver(applicationMessageObserver);
  }

  private void createTable() {
    audioItemTable = new JXTable();
    audioItemTable.setShowGrid(false, false);
    audioItemTable.setDragEnabled(true);
    audioItemTable.setDropMode(DropMode.ON);
    audioItemTable.setTransferHandler(new AudioItemTransferHandler());
    audioItemTable.setModel(tableModel);

    // use fixed color; there seems to be a bug in some plaf implementations
    // that cause strange rendering
    if (ACMConfiguration.getInstance().getCurrentDB().isSandboxed()) {
      audioItemTable.addHighlighter(HighlighterFactory
          .createAlternateStriping(Color.LIGHT_GRAY, new Color(237, 243, 254)));
    } else {
      audioItemTable.addHighlighter(HighlighterFactory
          .createAlternateStriping(Color.white, new Color(237, 243, 254)));
    }

    audioItemTable.setSortOrder(
        AudioItemTableModel.dateFileModifiedColumn.getColumnIndex(),
        SortOrder.ASCENDING);

    JScrollPane scrollPane = new JScrollPane(audioItemTable);
    scrollPane.setPreferredSize(new Dimension(800, 500));

    add(BorderLayout.CENTER, scrollPane);
  }

  private void updateTable() {
    // Cache the playlistOrderColumn
    if (!firstDataSet) {
      initColumnSize();
      playlistOrderColumn = audioItemTable.getTableHeader().getColumnModel()
          .getColumn(AudioItemTableModel.playlistOrderColumn.getColumnIndex());
      correlationIdColumn = audioItemTable.getTableHeader().getColumnModel()
          .getColumn(AudioItemTableModel.correlationIdColumn.getColumnIndex());
      firstDataSet = true;
    }

    // If a playlist is selected (ie, is filtering), make the playlist order column visible.
    boolean showPlaylistOrder = Application.getFilterState().getSelectedPlaylist() != null;

    // There's no evident way to query if a column is in the table. Removing when not there
    // has no effect. So, always remove the optional columns, then add back the ones we want.
    audioItemTable.removeColumn(playlistOrderColumn);
    audioItemTable.removeColumn(correlationIdColumn);

    if (showCorrelationId) {
      audioItemTable.addColumn(correlationIdColumn);
    }
    if (showPlaylistOrder) {
      audioItemTable.addColumn(playlistOrderColumn);
      TableSortController<AudioItemTableModel> tableRowSorter = (TableSortController<AudioItemTableModel>) audioItemTable
          .getRowSorter();
      for (ColumnInfo<?> columnInfo : tableModel.getColumnInfos()) {
        Comparator<?> comparator = columnInfo.getComparator();
        if (comparator != null) {
          tableRowSorter.setComparator(columnInfo.getColumnIndex(), comparator);
        }
      }
    }

    if (currResult != null) {
      if (currResult.getAudioItems().isEmpty()) {
        audioItemTable.setRowFilter(new RowFilter<Object, Object>() {
          @Override
          public boolean include(
              javax.swing.RowFilter.Entry<?, ?> entry) {
            return false;
          }
        });
      }

      if (currResult.getAudioItems().size() == currResult
          .getTotalNumDocsInIndex()) {
        audioItemTable.setRowFilter(new RowFilter<Object, Object>() {
          @Override
          public boolean include(
              javax.swing.RowFilter.Entry<? extends Object, ? extends Object> entry) {
            return true;
          }
        });
      }

      audioItemTable.setRowFilter(new RowFilter<Object, Object>() {
        @Override
        public boolean include(
            javax.swing.RowFilter.Entry<?, ?> entry) {
          return currResult.getAudioItems().contains(
              tableModel.getAudioItemUuid((Integer) entry.getIdentifier()));
        }
      });
    }
  }

  /**
   * Central message handler for the audio item view
   */
  private Observer applicationMessageObserver = new Observer() {
    @Override
    public void update(Observable o, Object arg) {
      if (arg instanceof SearchResult) {
        currResult = (SearchResult) arg;
        updateTable();
      }

      if (arg instanceof UILanguageChanged) {
        UILanguageChanged newLocale = (UILanguageChanged) arg;
        updateControlLanguage(newLocale.getNewLocale());
      }

      if (arg instanceof AudioItemTableSortOrderMessage) {
        AudioItemTableSortOrderMessage message = (AudioItemTableSortOrderMessage) arg;
        audioItemTable.setSortOrder(message.getIdentifier(), message.getSortOrder());
      }

      if (arg instanceof RequestAudioItemMessage) {
        RequestAudioItemMessage requestAudioItemMessage = (RequestAudioItemMessage) arg;

        AudioItem audioItem = null;
        switch (requestAudioItemMessage.getRequestType()) {
        case Current:
          audioItem = getCurrentAudioItem();
          break;
        case Next:
          audioItem = getNextAudioItem();
          break;
        case Previews:
          audioItem = getPreviousAudioItem();
          break;
        }

        if (audioItem != null) {
          selectAudioItem(audioItem);

          if (arg instanceof RequestAndSelectAudioItemMessage) {
            RequestedAudioItemMessage newMsg = new RequestedAudioItemMessage(audioItem);
            Application.getMessageService().pumpMessage(newMsg);
          } else if (arg instanceof RequestAudioItemToPlayMessage) {
            PlayAudioItemMessage newMsg = new PlayAudioItemMessage(audioItem);
            Application.getMessageService().pumpMessage(newMsg);
          }
        }
      }
    }
  };

  private void updateControlLanguage(Locale newLocale) {
    for (ColumnInfo<?> columnInfo : tableModel.getColumnInfos()) {
      audioItemTable.getColumnModel().getColumn(columnInfo.getColumnIndex())
          .setHeaderValue(columnInfo.getColumnName(newLocale));
    }
  }

  private void initColumnSize() {
    audioItemTable.setAutoCreateColumnsFromModel(false);
    for (ColumnInfo<?> columnInfo : tableModel.getColumnInfos()) {
      if (columnInfo.getMaxWidth() != ColumnInfo.WIDTH_NOT_SET) {
        audioItemTable.getTableHeader().getColumnModel()
            .getColumn(columnInfo.getColumnIndex())
            .setMaxWidth(columnInfo.getMaxWidth());
      }
      if (columnInfo.getPreferredWidth() != ColumnInfo.WIDTH_NOT_SET) {
        audioItemTable.getTableHeader().getColumnModel()
            .getColumn(columnInfo.getColumnIndex())
            .setPreferredWidth(columnInfo.getPreferredWidth());
      }
    }
  }

  boolean hasSelectedRows() {
    return audioItemTable.getSelectedRow() != -1;
  }

  AudioItem getCurrentAudioItem() {
    int tableRow = audioItemTable.getSelectedRow();
    if (tableRow == -1) {
      // select first row if available
      if (audioItemTable.getRowCount() > 0) {
        tableRow = 0;
        selectTableRow(tableRow);
      } else {
        return null;
      }
    }
    int modelRow = audioItemTable.convertRowIndexToModel(tableRow);

    return getValueAt(modelRow, 0);
  }

  AudioItem getNextAudioItem() {
    int tableRow = audioItemTable.getSelectedRow();
    if (tableRow < audioItemTable.getRowCount() - 1) {
      tableRow++;
    }

    int modelRow = audioItemTable.convertRowIndexToModel(tableRow);
    return getValueAt(modelRow, 0);
  }

  AudioItem getPreviousAudioItem() {
    int tableRow = audioItemTable.getSelectedRow();
    if (tableRow > 0) {
      tableRow--;
    }

    int modelRow = audioItemTable.convertRowIndexToModel(tableRow);
    return getValueAt(modelRow, 0);
  }

  public AudioItem getAudioItemAtTableRow(int row) {
    int modelRow = audioItemTable.convertRowIndexToModel(row);
    return getValueAt(modelRow, 0);
  }

  private AudioItem getValueAt(int modelRow, int col) {
    Object o = audioItemTable.getModel().getValueAt(modelRow, col);

    AudioItem item = null;
    if (o instanceof AudioItemNode) {
      item = ((AudioItemNode) o).getAudioItem();
    } else if (o instanceof AudioItem) {
      item = (AudioItem) o;
    }

    return item;
  }

  public boolean selectAudioItem(AudioItem audioItem) {
    for (int i = 0; i < audioItemTable.getRowCount(); i++) {
      int modelIndex = audioItemTable.convertRowIndexToModel(i);
      AudioItem item = getValueAt(modelIndex, 0);
      if (item != null) {
        if (item.equals(audioItem)) {
          return selectTableRow(i);
        }
      }
    }

    return false;
  }

  boolean selectTableRow(int rowStart, int rowEnd) {
    ListSelectionModel selectionModel = audioItemTable.getSelectionModel();
    if (selectionModel != null) {
      selectionModel.setSelectionInterval(rowStart, rowEnd);
      return true;
    }

    return false;
  }

  boolean selectTableRow(int row) {
    return selectTableRow(row, row);
  }

  int getCurrentSelectedRow() {
    if (audioItemTable != null) {
      return audioItemTable.getSelectedRow();
    }

    return -1;
  }

  int[] getCurrentSelectedRows() {
    if (audioItemTable != null) {
      return audioItemTable.getSelectedRows();
    }

    return new int[] {};
  }

  // Special handlers
  public void setData(SearchResult result) {
    updateTable();
    mouseListener.setCurrentResult(result);
  }

  public void addHandler() {

    final AudioItemCellRenderer renderer = new AudioItemCellRenderer();
    audioItemTable.setDefaultRenderer(Object.class, renderer);
    audioItemTable.addMouseMotionListener(new MouseMotionListener() {

      @Override
      public void mouseMoved(MouseEvent e) {
        JTable table = (JTable) e.getSource();
        renderer.highlightedRow = table.rowAtPoint(e.getPoint());
        table.repaint();
      }

      @Override
      public void mouseDragged(MouseEvent e) {
      }
    });

    audioItemTable.addMouseListener(new MouseListener() {
        public int defaultDelay;

        @Override
      public void mouseExited(MouseEvent e) {
        JTable table = (JTable) e.getSource();
        renderer.highlightedRow = -1;
        table.repaint();
        ToolTipManager.sharedInstance().setInitialDelay(defaultDelay);
      }

      @Override
      public void mouseReleased(MouseEvent e) {
      }

      @Override
      public void mousePressed(MouseEvent e) {
      }

      @Override
      public void mouseEntered(MouseEvent e) {
          defaultDelay = ToolTipManager.sharedInstance().getInitialDelay();
          ToolTipManager.sharedInstance().setInitialDelay(FAST_TOOLTIP_DELAY);
      }

      @Override
      public void mouseClicked(MouseEvent e) {
      }
    });

    audioItemTable.getSelectionModel()
        .addListSelectionListener(new ListSelectionListener() {
          @Override
          public void valueChanged(ListSelectionEvent e) {
            String message;
            switch (audioItemTable.getSelectedRowCount()) {
            case 0:
              message = "";
              break;
            case 1:
              message = "1 audio item selected.";
              break;
            default: {
              Calendar cal = Calendar.getInstance();
              cal.set(0, 0, 0, 0, 0, 0);

              for (int row : getCurrentSelectedRows()) {
                AudioItem audioItem = getAudioItemAtTableRow(row);
                if (audioItem != null) {
                  MetadataValue<String> metadata = audioItem.getMetadata()
                      .getMetadataValue(MetadataSpecification.LB_DURATION);
                  if (metadata != null) {
                    String duration = metadata.getValue();
                    try {
                      cal.add(Calendar.MINUTE,
                          Integer.parseInt(duration.substring(0, 2)));
                      cal.add(Calendar.SECOND,
                          Integer.parseInt(duration.substring(3, 5)));
                    } catch (NumberFormatException ex) {
                      // ignore this audio item
                    }
                  }
                }
              }

              Calendar cal1 = Calendar.getInstance();
              cal1.set(0, 0, 1, 0, 0, 0);
              SimpleDateFormat format = cal.before(cal1)
                  ? new SimpleDateFormat("HH:mm:ss")
                  : new SimpleDateFormat("D 'd' HH:mm:ss");
              message = audioItemTable.getSelectedRowCount()
                  + " audio items selected. Total duration: "
                  + format.format(cal.getTime());
            }
            }

            Application.getApplication().setStatusMessage(message);
          }
        });

    mouseListener = new AudioItemViewMouseListener(this);
    audioItemTable.addMouseListener(mouseListener);
    audioItemTable.getTableHeader().addMouseListener(mouseListener);
  }
} // class
