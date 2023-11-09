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
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Observer;
import java.util.Set;

import javax.swing.*;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.decorator.HighlighterFactory;
import org.jdesktop.swingx.sort.TableSortController;
import org.jdesktop.swingx.table.TableColumnExt;
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
import org.literacybridge.acm.store.Metadata;
import org.literacybridge.acm.store.MetadataField;
import org.literacybridge.acm.store.MetadataSpecification;
import org.literacybridge.acm.store.MetadataStore;
import org.literacybridge.acm.store.MetadataValue;
import org.literacybridge.acm.store.SearchResult;

import static org.literacybridge.acm.gui.MainWindow.audioItems.AudioItemTableModel.playlistOrderColumn;
import static org.literacybridge.acm.gui.MainWindow.audioItems.AudioItemTableModel.contentidColumn;
import static org.literacybridge.acm.gui.MainWindow.audioItems.AudioItemTableModel.sdgGoalsColumn;
import static org.literacybridge.acm.gui.MainWindow.audioItems.AudioItemTableModel.sdgTargetsColumn;
import static org.literacybridge.acm.store.MetadataSpecification.LB_SDG_GOALS;
import static org.literacybridge.acm.store.MetadataSpecification.LB_SDG_TARGETS;

public class AudioItemView extends Container {

  private static final long serialVersionUID = -2886958461177831842L;

  public static final DataFlavor AudioItemDataFlavor = new DataFlavor(
      AudioItem.class, "LocalizedAudioItem");
    private static final int FAST_TOOLTIP_DELAY = 50;
    private SearchResult currResult = null;
  JXTable audioItemTable = null;

  private AudioItemViewMouseListener mouseListener;
  private final AudioItemTableModel tableModel;

  private boolean widthsAndColumnsSet = false;
  private JTableColumnSelector columnSelector;

  public AudioItemView() {
    setLayout(new BorderLayout());
    tableModel = new AudioItemTableModel();
    createTable();
    addHandler();
    addToMessageService();

    initColumnSize();

    // Enable the SDG columns by default if there is SDG data in the metadata.
    MetadataStore store = ACMConfiguration.getInstance().getCurrentDB().getMetadataStore();
    boolean hasSdgFields = store.getAudioItems().stream().anyMatch(item->{
      Metadata md = item.getMetadata();
      Set<MetadataField<?>> keys = md.keySet();
      return keys.contains(LB_SDG_GOALS) || keys.contains(LB_SDG_TARGETS);
    });
    columnSelector.setColumnVisible(sdgGoalsColumn.getColumnIndex(), hasSdgFields);
    columnSelector.setColumnVisible(sdgTargetsColumn.getColumnIndex(), hasSdgFields);
    for (int ix : tableModel.defaultHiddenColumns) {
        columnSelector.setColumnVisible(ix, false);
    }

    setComparators();

  }

  void setComparators() {
      // Set the comparator (only the Playlist Order has such a thing).
      TableColumnModel columnModel = audioItemTable.getColumnModel();
      TableSortController<?> tableRowSorter = (TableSortController<?>) audioItemTable
          .getRowSorter();
      for (ColumnInfo<?> columnInfo : tableModel.getColumnInfos()) {
          Comparator<?> comparator = columnInfo.getComparator();
          if (comparator != null) {
              Enumeration<TableColumn> et = columnModel.getColumns();
              while (et.hasMoreElements()) {
                  TableColumn tc = et.nextElement();
                  if (columnInfo.getColumnName().equals(tc.getHeaderValue()) && tc instanceof TableColumnExt) {
                      TableColumnExt tce = (TableColumnExt)tc;
                      tce.setComparator(comparator);
                  }
              }
              tableRowSorter.setComparator(columnInfo.getColumnIndex(), comparator);
          }
      }
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


    // Specify the column index for the duration and the highlight color
    int durationColumnIndex = 1; // Change to match your duration column index
    Color highlightColor = Color.RED; // Customize the highlight color

    // Create and apply the custom highlighter
    CorruptedAudioHighlighter customHighlighter = new CorruptedAudioHighlighter(durationColumnIndex, highlightColor);
    audioItemTable.addHighlighter(customHighlighter);

    audioItemTable.setSortOrder(
        AudioItemTableModel.dateFileModifiedColumn.getColumnIndex(),
        SortOrder.DESCENDING);

    JScrollPane scrollPane = new JScrollPane(audioItemTable);
    scrollPane.setPreferredSize(new Dimension(800, 500));

    add(BorderLayout.CENTER, scrollPane);
  }

  private void updateTable() {
    // If a playlist is selected (ie, is filtering), make the playlist order column visible.
    boolean showPlaylistOrder = Application.getFilterState().getSelectedPlaylist() != null;
    columnSelector.setColumnVisible(playlistOrderColumn.getColumnIndex(), showPlaylistOrder);

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
              javax.swing.RowFilter.Entry<?, ?> entry) {
            return true;
          }
        });
      }

      audioItemTable.setRowFilter(new RowFilter<Object, Object>() {
        @Override
        public boolean include(
            javax.swing.RowFilter.Entry<?, ?> entry) {
          return currResult.getAudioItems().contains(
              tableModel.getAudioItemId((Integer) entry.getIdentifier()));
        }
      });
    }
  }

  /**
   * Central message handler for the audio item view
   */
  private Observer applicationMessageObserver = (o, arg) -> {
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

  public AudioItem getCurrentAudioItem() {
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

  private AudioItem getNextAudioItem() {
    int tableRow = audioItemTable.getSelectedRow();
    if (tableRow < audioItemTable.getRowCount() - 1) {
      tableRow++;
    }

    int modelRow = audioItemTable.convertRowIndexToModel(tableRow);
    return getValueAt(modelRow, 0);
  }

  private AudioItem getPreviousAudioItem() {
    int tableRow = audioItemTable.getSelectedRow();
    if (tableRow > 0) {
      tableRow--;
    }

    int modelRow = audioItemTable.convertRowIndexToModel(tableRow);
    return getValueAt(modelRow, 0);
  }

  AudioItem getAudioItemAtTableRow(int row) {
    int modelRow = audioItemTable.convertRowIndexToModel(row);
    return getValueAt(modelRow, 0);
  }

  private AudioItem getValueAt(int modelRow, int col) {
    Object o = audioItemTable.getModel().getValueAt(modelRow, col);

    AudioItem item = null;
    if (o instanceof AudioItemNode) {
      item = ((AudioItemNode<?>) o).getAudioItem();
    } else if (o instanceof AudioItem) {
      item = (AudioItem) o;
    }

    return item;
  }

  private boolean selectAudioItem(AudioItem audioItem) {
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

  private boolean selectTableRow(int rowStart, int rowEnd) {
    ListSelectionModel selectionModel = audioItemTable.getSelectionModel();
    if (selectionModel != null) {
      selectionModel.setSelectionInterval(rowStart, rowEnd);
      return true;
    }

    return false;
  }

  private boolean selectTableRow(int row) {
    return selectTableRow(row, row);
  }

  int getCurrentSelectedRow() {
    if (audioItemTable != null) {
      return audioItemTable.getSelectedRow();
    }

    return -1;
  }

  private int[] getCurrentSelectedRows() {
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

  private void addHandler() {

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
        int defaultDelay;

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
        .addListSelectionListener(e -> {
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
              //noinspection MagicConstant
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
              //noinspection MagicConstant
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
        });

    mouseListener = new AudioItemViewMouseListener(this);
    audioItemTable.addMouseListener(mouseListener);
    audioItemTable.getTableHeader().addMouseListener(mouseListener);

    columnSelector = new JTableColumnSelector();
    columnSelector.install(audioItemTable);
  }




  /**
   * A class that allows user to select visible columns of a JTable using a popup menu.
   *
   * @author Sergey A. Tachenov
   */
  class JTableColumnSelector {

    private JTable table;
    private final Map<Integer, TableColumn> hiddenColumns = new HashMap<>();
    private final Map<Integer, JCheckBoxMenuItem> columnCheckboxes = new HashMap<>();

    /**
     * Constructor. Call {@link #install(javax.swing.JTable) install} to actually
     * install it on a JTable.
     */
    JTableColumnSelector() {
    }

    /**
     * Installs this selector on a given table.
     * @param table the table to install this selector on
     */
    void install(JTable table) {
      this.table = table;
      table.getTableHeader().setComponentPopupMenu(createHeaderMenu());
    }

    /**
     * Exposes show/hide functionality to external code.
     * @param modelIndex Column to be shown or hidden.
     * @param visible if true, show, if false, hide.
     */
    void setColumnVisible(int modelIndex, boolean visible) {
      // Track in the column selector UI.
      if (columnCheckboxes.containsKey(modelIndex)) {
        columnCheckboxes.get(modelIndex).setSelected(visible);
        updateColumnModel(modelIndex, visible);
      }
    }

    private JPopupMenu createHeaderMenu() {
      final JPopupMenu headerMenu = new JPopupMenu();
      final TableModel model = table.getModel();
      for (int i = 0; i < model.getColumnCount(); ++i) {
        JCheckBoxMenuItem item = createMenuItem(i);
        if (item != null) {
          columnCheckboxes.put(i, item);
          headerMenu.add(item);
        }
      }
      return headerMenu;
    }

    private JCheckBoxMenuItem createMenuItem(final int modelIndex) {
      JCheckBoxMenuItem menuItem = null;
      final TableModel model = table.getModel();
      final String columnName = model.getColumnName(modelIndex);
      if (columnName != null && columnName.length() > 0) {
        menuItem = new JCheckBoxMenuItem(columnName);
        menuItem.setSelected(true);
        menuItem.addActionListener(action -> {
          JCheckBoxMenuItem item = (JCheckBoxMenuItem)action.getSource();
          updateColumnModel(modelIndex, item.isSelected());
        });
      }
      return menuItem;
    }

    private void updateColumnModel(int modelIndex, boolean visible) {
      if (visible)
        showColumn(modelIndex);
      else
        hideColumn(modelIndex);
    }

    private void showColumn(int modelIndex) {
      if (hiddenColumns.containsKey(modelIndex)) {
        TableColumn column = hiddenColumns.remove(modelIndex);
        TableColumnModel columnModel = table.getColumnModel();
        columnModel.addColumn(column);
        final int addedViewIndex = columnModel.getColumnCount() - 1;
        if (modelIndex < columnModel.getColumnCount())
          columnModel.moveColumn(addedViewIndex, modelIndex);
      }
    }

    private void hideColumn(int modelIndex) {
      int vIndex = table.convertColumnIndexToView(modelIndex);
      // Check if already hidden.
      if (vIndex >= 0) {
        TableColumnModel columnModel = table.getColumnModel();
        TableColumn column = columnModel.getColumn(vIndex);
        columnModel.removeColumn(column);
        hiddenColumns.put(modelIndex, column);
        workaroundForSwingIndexOutOfBoundsBug(column);
      }
    }

    private void workaroundForSwingIndexOutOfBoundsBug(TableColumn column) {
      JTableHeader tableHeader = table.getTableHeader();
      if (tableHeader.getDraggedColumn() == column) {
        tableHeader.setDraggedColumn(null);
      }
    }

  }

} // class
