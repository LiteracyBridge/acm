package org.literacybridge.acm.ui.ResourceView;

import java.awt.Dialog;
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

import org.literacybridge.acm.importexport.FileImporter;
import org.literacybridge.acm.resourcebundle.LabelProvider;
import org.literacybridge.acm.ui.Application;
import org.literacybridge.acm.ui.ResourceView.CategoryView.CategoryTreeNodeObject;
import org.literacybridge.acm.ui.dialogs.BusyDialog;
import org.literacybridge.acm.util.UIUtils;
import org.literacybridge.acm.util.language.LanguageUtil;

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

		if (!support.isDataFlavorSupported(fileFlavor)) {
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
		DefaultMutableTreeNode parent = (DefaultMutableTreeNode) dest.getLastPathComponent();
		final CategoryTreeNodeObject obj = (CategoryTreeNodeObject) parent.getUserObject();
		
		// don't piggyback on the drag&drop thread
		Runnable job = new Runnable() {

			@Override
			public void run() {
				Application parent = Application.getApplication();
				Dialog busy = UIUtils.showDialog(parent, new BusyDialog(LabelProvider.getLabel("IMPORTING_FILES", LanguageUtil.getUserChoosenLanguage()), parent));
				try {
					for (File f : files) {
						if (f.isDirectory()) {
							FileImporter.getInstance().importDirectory(obj.getCategory(), f, false);
						} else {
							FileImporter.getInstance().importFile(obj.getCategory(), f);
						}
					}
				} catch (IOException e) {
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