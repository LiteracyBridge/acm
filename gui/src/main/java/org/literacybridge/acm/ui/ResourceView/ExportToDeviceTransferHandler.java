package org.literacybridge.acm.ui.ResourceView;

import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

import javax.swing.JTree;
import javax.swing.TransferHandler;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import org.literacybridge.acm.content.AudioItem;
import org.literacybridge.acm.content.LocalizedAudioItem;
import org.literacybridge.acm.device.DeviceInfo;
import org.literacybridge.acm.ui.ResourceView.audioItems.AudioItemView;

public class ExportToDeviceTransferHandler extends TransferHandler {
	private static final long serialVersionUID = 1L;


	@Override
	public boolean canImport(TransferHandler.TransferSupport support) {
		if (!support.isDrop()) {
			return false;
		}

		if (!support.isDataFlavorSupported(AudioItemView.AudioItemDataFlavor)) {
			return false;
		}

		support.setShowDropLocation(true);
		
		return true;
	}

	@Override
	public boolean importData(TransferHandler.TransferSupport support) {
		if (!canImport(support)) {
			return false;
		}

		// Get drop location info.
		JTree.DropLocation dl = (JTree.DropLocation) support.getDropLocation();
		TreePath dest = dl.getPath();
		DefaultMutableTreeNode parent = (DefaultMutableTreeNode) dest.getLastPathComponent();
		DeviceInfo device = (DeviceInfo) parent.getUserObject();
		
		Transferable transferable = support.getTransferable();
		final AudioItem audioItem;
		
		try {
			LocalizedAudioItem item =  (LocalizedAudioItem) transferable.getTransferData(AudioItemView.AudioItemDataFlavor);
			System.out.println("Exporting to + " + device.getPathToDevice() + ":\n" + item.getMetadata().toString());
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		} catch (UnsupportedFlavorException e) {
			return false;
		}
		
		// don't piggyback on the drag&drop thread
		Runnable job = new Runnable() {

			@Override
			public void run() {
				//A18Exporter.export(audioItem, device);
			}
		};
		
		new Thread(job).start();
		
		return true;
	}
}