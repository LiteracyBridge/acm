package org.literacybridge.acm.gui.MainWindow.audioItems;

import org.jdesktop.swingx.JXTableHeader;
import org.literacybridge.acm.gui.Application;
import org.literacybridge.acm.gui.dialogs.AudioItemContextMenuDialog;
import org.literacybridge.acm.gui.dialogs.AudioItemRenameDialog;
import org.literacybridge.acm.gui.messages.RequestAudioItemMessage;
import org.literacybridge.acm.gui.messages.RequestAudioItemToPlayMessage;
import org.literacybridge.acm.gui.util.UIUtils;
import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.store.SearchResult;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class AudioItemViewMouseListener extends MouseAdapter {
  private AudioItemView audioItemView;
  private SearchResult currentResult;

  AudioItemViewMouseListener(AudioItemView audioItemView) {
    this.audioItemView = audioItemView;
  }

  @Override
  public void mouseReleased(MouseEvent e) {
    if (e.getClickCount() == 2) {
      RequestAudioItemToPlayMessage msg = new RequestAudioItemToPlayMessage(
          RequestAudioItemMessage.RequestType.Current);
      Application.getMessageService().pumpMessage(msg);
    }
  }

  @Override
  public void mouseClicked(MouseEvent e) {
      // This same handler is used for BOTH the audio item table, and the header for that table.
      // (I know, right?) To distinguish, look at the source of the event. Translate clicks on
      // the header to row -1.
      boolean headerClick = e.getSource() instanceof JXTableHeader;
      boolean rightButtonClicked = e.getButton() == MouseEvent.BUTTON3;

      // Was the click on the info icon?
      int col = audioItemView.audioItemTable.columnAtPoint(e.getPoint());
      int modelColumn = audioItemView.audioItemTable.convertColumnIndexToModel(col);

      boolean infoIconClicked = modelColumn == AudioItemTableModel.infoIconColumn.getColumnIndex();

      // Was the click on a selected row?
      int row = headerClick ? -1 : audioItemView.audioItemTable.rowAtPoint(e.getPoint());
      int[] selectedRows = audioItemView.audioItemTable.getSelectedRows();
      boolean selectedRowClicked = false;
      for (int selected: selectedRows) {
          selectedRowClicked |= (row == selected);
      }

      // If either right mouse button, or info icon was clicked, then show the dialog, if
      // the click was on a selected row.
      // If not on a selected row, beep.
      if (rightButtonClicked || infoIconClicked) {
          if (selectedRowClicked)
                showAudioItemDlg(e);
          else
              Toolkit.getDefaultToolkit().beep();
      }
  }

    private void showRenameDlg(MouseEvent e) {
        AudioItem audioItem = audioItemView.getCurrentAudioItem();
        AudioItemRenameDialog dialog = new AudioItemRenameDialog(Application.getApplication(),
            audioItem);
        UIUtils.showDialog(dialog, e.getXOnScreen(), e.getYOnScreen());
    }

    private void showAudioItemDlg(MouseEvent e) {
        // always the first item of a selection.
        AudioItem clickedAudioItem = audioItemView.getCurrentAudioItem();
        if (clickedAudioItem != null) {

            int[] selectedRows = audioItemView.audioItemTable.getSelectedRows();
            AudioItem[] selectedAudioItems = new AudioItem[selectedRows.length];
            for (int i = 0; i < selectedRows.length; i++) {
                selectedAudioItems[i] = audioItemView
                    .getAudioItemAtTableRow(selectedRows[i]);
            }

            Point componentOrigin = e.getComponent().getLocationOnScreen();
            AudioItemContextMenuDialog dlg = new AudioItemContextMenuDialog(Application.getApplication(),
                    clickedAudioItem, selectedAudioItems, audioItemView, currentResult);
            UIUtils.showDialog(dlg, componentOrigin.x + e.getX() + 2, componentOrigin.y + e.getY());
        }
    }


    void setCurrentResult(SearchResult currentResult) {
    this.currentResult = currentResult;
  }

}
