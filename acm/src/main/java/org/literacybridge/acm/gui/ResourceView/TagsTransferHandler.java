package org.literacybridge.acm.gui.ResourceView;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

import javax.swing.JList;
import javax.swing.TransferHandler;

import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.gui.Application;
import org.literacybridge.acm.gui.ResourceView.TagsListModel.TagLabel;
import org.literacybridge.acm.gui.ResourceView.audioItems.AudioItemView;
import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.store.Playlist;

public class TagsTransferHandler extends TransferHandler {
    static DataFlavor[] supportedFlavors = new DataFlavor[] {
            AudioItemView.AudioItemDataFlavor
    };

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
                assignTag(support, ((TagLabel) target.getModel().getElementAt(index)).getTag());
                return true;
            }

        } catch (UnsupportedFlavorException e) {
        } catch (IOException e) {}

        return false;
    }

    private void assignTag(TransferHandler.TransferSupport support, final Playlist tag) throws IOException, UnsupportedFlavorException {
        Transferable t = support.getTransferable();

        final AudioItem[] audioItems = (AudioItem[]) t.getTransferData(AudioItemView.AudioItemDataFlavor);

        for (AudioItem item : audioItems) {
            if (!item.hasPlaylist(tag)) {
                try {
                    item.addPlaylist(tag);
                    ACMConfiguration.getCurrentDB().getMetadataStore().commit(item);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        Application.getFilterState().updateResult();
    }
}
