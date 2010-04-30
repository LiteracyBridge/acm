package org.literacybridge.acm.ui.ResourceView;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.swing.JTree;
import javax.swing.TransferHandler;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

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
		DefaultMutableTreeNode parent = (DefaultMutableTreeNode) dest
				.getLastPathComponent();

		System.out.println("Importing into node: " + parent);
		for (File f : files) {
			System.out.println("\t" + f);
		}
		
		return true;
	}
}
