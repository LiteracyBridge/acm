package org.literacybridge.acm.gui.ResourceView.audioItems;

import org.jdesktop.swingx.JXTableHeader;
import org.literacybridge.acm.gui.Application;
import org.literacybridge.acm.gui.dialogs.AudioItemContextMenuDialog;
import org.literacybridge.acm.gui.messages.RequestAudioItemMessage;
import org.literacybridge.acm.gui.messages.RequestAudioItemToPlayMessage;
import org.literacybridge.acm.gui.util.UIUtils;
import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.store.SearchResult;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class AudioItemViewMouseListener extends MouseAdapter {
  private AudioItemView adaptee;
  private SearchResult currentResult;

  AudioItemViewMouseListener(AudioItemView adaptee) {
    this.adaptee = adaptee;
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
      int col = adaptee.audioItemTable.columnAtPoint(e.getPoint());
      boolean infoIconClicked = col == AudioItemTableModel.infoIconColumn.getColumnIndex();

      // Was the click on a selected row?
      int row = headerClick ? -1 : adaptee.audioItemTable.rowAtPoint(e.getPoint());
      boolean selectedRowClicked = false;
      for (int selected: adaptee.audioItemTable.getSelectedRows()) {
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

    private void showAudioItemDlg(MouseEvent e) {
        // always the first item of a selection.
        AudioItem clickedAudioItem = adaptee.getCurrentAudioItem();
        if (clickedAudioItem != null) {

            int[] selectedRows = adaptee.audioItemTable.getSelectedRows();
            AudioItem[] selectedAudioItems = new AudioItem[selectedRows.length];
            for (int i = 0; i < selectedRows.length; i++) {
                selectedAudioItems[i] = adaptee
                    .getAudioItemAtTableRow(selectedRows[i]);
            }

            UIUtils.showDialog(
                new AudioItemContextMenuDialog(Application.getApplication(),
                    clickedAudioItem, selectedAudioItems, adaptee, currentResult),
                e.getXOnScreen() + 2, e.getYOnScreen());
        }
    }

  void setCurrentResult(SearchResult currentResult) {
    this.currentResult = currentResult;
  }

}
