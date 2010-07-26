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

import org.literacybridge.acm.content.AudioItem;
import org.literacybridge.acm.importexport.FileImporter;
import org.literacybridge.acm.resourcebundle.LabelProvider;
import org.literacybridge.acm.ui.Application;
import org.literacybridge.acm.ui.ResourceView.CategoryView.CategoryTreeNodeObject;
import org.literacybridge.acm.ui.ResourceView.audioItems.AudioItemView;
import org.literacybridge.acm.ui.dialogs.BusyDialog;
import org.literacybridge.acm.util.UIUtils;
import org.literacybridge.acm.util.language.LanguageUtil;

public class TreeTransferHandler extends TransferHandler {
	private static final long serialVersionUID = 1L;

	static DataFlavor[] supportedFlavors = new DataFlavor[] {
		DataFlavor.javaFileListFlavor,
		AudioItemView.AudioItemDataFlavor
	};

	@Override
	public boolean canImport(TransferHandler.TransferSupport support) {
		if (!support.isDrop()) {
			return false;
		}

		for (DataFlavor flavor : supportedFlavors) {
			if (support.isDataFlavorSupported(flavor)) {
				return true;
			}
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
		final CategoryTreeNodeObject target = (CategoryTreeNodeObject) parent.getUserObject();
		
		// Extract transfer data.
		try {
			if (support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
				importExternalFiles(support, target);
				return true;
			} else if (support.isDataFlavorSupported(AudioItemView.AudioItemDataFlavor)) {
				reassignCategory(support, target);
				return true;
			}
			
		} catch (UnsupportedFlavorException e) {
		} catch (IOException e) {}
		
		return false;
	}
	
	private void importExternalFiles(TransferHandler.TransferSupport support, final CategoryTreeNodeObject target) throws IOException, UnsupportedFlavorException {
		Transferable t = support.getTransferable();
		final List<File> files = (List<File>) t.getTransferData(DataFlavor.javaFileListFlavor);

		
		// don't piggyback on the drag&drop thread
		Runnable job = new Runnable() {

			@Override
			public void run() {
				Application parent = Application.getApplication();
				Dialog busy = UIUtils.showDialog(parent, new BusyDialog(LabelProvider.getLabel("IMPORTING_FILES", LanguageUtil.getUserChoosenLanguage()), parent));
				try {
					for (File f : files) {
						if (f.isDirectory()) {
							FileImporter.getInstance().importDirectory(target.getCategory(), f, false);
						} else {
							FileImporter.getInstance().importFile(target.getCategory(), f);
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
	}
	
	private void reassignCategory(TransferHandler.TransferSupport support, final CategoryTreeNodeObject target) throws IOException, UnsupportedFlavorException {
		Transferable t = support.getTransferable();
		boolean move = support.getSourceDropActions() == TransferHandler.MOVE;
		
		final AudioItem[] audioItems = (AudioItem[]) t.getTransferData(AudioItemView.AudioItemDataFlavor);

		for (AudioItem item : audioItems) {
			if (move) {
				item.removeAllCategories();
			}
			item.addCategory(target.getCategory());
			item.commit();
		}
		Application.getFilterState().updateResult();
	}
}