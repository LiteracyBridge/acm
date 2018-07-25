package org.literacybridge.acm.gui.MainWindow;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

import javax.swing.JList;
import javax.swing.TransferHandler;

import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.gui.Application;
import org.literacybridge.acm.gui.MainWindow.PlaylistListModel.PlaylistLabel;
import org.literacybridge.acm.gui.MainWindow.audioItems.AudioItemView;
import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.store.Playlist;

public class PlaylistTransferHandler extends TransferHandler {
  static DataFlavor[] supportedFlavors = new DataFlavor[] {
      AudioItemView.AudioItemDataFlavor };

  @Override
  public boolean canImport(TransferHandler.TransferSupport support) {
    if (!support.isDrop()) {
      return false;
    }

    for (DataFlavor flavor : supportedFlavors) {
      if (support.isDataFlavorSupported(flavor)) {
        support.setShowDropLocation(true);
        return true;
      }
    }

    return false;
  }

  @Override
  public boolean importData(TransferHandler.TransferSupport support) {
    if (!canImport(support)) {
      return false;
    }
    // Get drop location info.
    JList.DropLocation dl = (JList.DropLocation) support.getDropLocation();
    int index = dl.getIndex();

    JList target = (JList) support.getComponent();

    // Extract transfer data.
    try {
      if (support.isDataFlavorSupported(AudioItemView.AudioItemDataFlavor)) {
        assignPlaylist(support,
            ((PlaylistLabel) target.getModel().getElementAt(index)).getPlaylist());
        return true;
      }

    } catch (UnsupportedFlavorException e) {
    } catch (IOException e) {
    }

    return false;
  }

  private void assignPlaylist(TransferHandler.TransferSupport support,
      final Playlist playlist) throws IOException, UnsupportedFlavorException {
    Transferable t = support.getTransferable();

    final AudioItem[] audioItems = (AudioItem[]) t
        .getTransferData(AudioItemView.AudioItemDataFlavor);

    for (AudioItem item : audioItems) {
      if (!item.hasPlaylist(playlist)) {
        try {
          item.addPlaylist(playlist);
          playlist.addAudioItem(item.getUuid());
          ACMConfiguration.getInstance().getCurrentDB().getMetadataStore()
              .commit(item, playlist);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
    Application.getFilterState().updateResult(true);
  }
}
