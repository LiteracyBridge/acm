package org.literacybridge.acm.ui.ResourceView;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JTree;
import javax.swing.TransferHandler;
import javax.swing.tree.TreePath;

import org.literacybridge.acm.importexport.FileImporter;
import org.literacybridge.acm.ui.Application;
import org.literacybridge.acm.ui.ResourceView.CategoryView.CategoryTreeNode;
import org.literacybridge.acm.ui.dialogs.BusyDialog;

public class TreeTransferHandler extends TransferHandler {
	private static final long serialVersionUID = 1L;

	DataFlavor fileFlavor;

	public TreeTransferHandler() {
		fileFlavor = DataFlavor.javaFileListFlavor;
	}

	@Override
	public boolean canImport(TransferHandler.TransferSupport support) {
		if (!support.isDrop()) {
			return false;
		}
		support.setShowDropLocation(true);
		if (!support.isDataFlavorSupported(fileFlavor)) {
			return false;
		}

		return true;
	}

	@Override
	public boolean importData(TransferHandler.TransferSupport support) {
		if (!canImport(support)) {
			return false;
		}
		// Extract transfer data.
		final List<File> files;
		try {
			Transferable t = support.getTransferable();
			files = (List<File>) t.getTransferData(fileFlavor);
		} catch (UnsupportedFlavorException e) {
			return false;
		} catch (IOException e) {
			return false;
		}
		// Get drop location info.
		JTree.DropLocation dl = (JTree.DropLocation) support.getDropLocation();
		TreePath dest = dl.getPath();
		final CategoryTreeNode parent = (CategoryTreeNode) dest
				.getLastPathComponent();

		// don't piggyback on the drag&drop thread
		Runnable job = new Runnable() {

			@Override
			public void run() {
				JDialog busy = BusyDialog.show("Importing files...");
				try {
					for (File f : files) {
						if (f.isDirectory()) {
							FileImporter.getInstance().importDirectory(parent.category, f, false);
						} else {
							FileImporter.getInstance().importFile(parent.category, f);
						}
					}
					
				} catch (IOException e) {
					// TODO: error handling
					e.printStackTrace();
				} finally {
					busy.setVisible(false);
					Application.getFilterState().updateResult();
				}
			}
		};
		
		new Thread(job).start();
		
		return true;
	}
}
