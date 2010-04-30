package org.literacybridge.acm.ui.ResourceView;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.swing.JTree;
import javax.swing.TransferHandler;
import javax.swing.tree.TreePath;

import org.literacybridge.acm.importexport.FileImporter;
import org.literacybridge.acm.ui.ResourceView.CategoryView.CategoryTreeNode;

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
		List<File> files = null;
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
		CategoryTreeNode parent = (CategoryTreeNode) dest
				.getLastPathComponent();

		try {
			for (File f : files) {
				if (f.isDirectory()) {
					FileImporter.getInstance().importDirectory(parent.category, f, false);
				} else {
					FileImporter.getInstance().importFile(parent.category, f);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return true;
	}
}
