package org.literacybridge.acm.ui.ResourceView;

import java.util.logging.Level;
import java.util.logging.Logger;

import java.awt.Container;
import java.awt.Dialog;
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
import org.literacybridge.acm.importexport.A18DeviceExporter;
import org.literacybridge.acm.resourcebundle.LabelProvider;
import org.literacybridge.acm.ui.Application;
import org.literacybridge.acm.ui.ResourceView.audioItems.AudioItemView;
import org.literacybridge.acm.ui.dialogs.BusyDialog;
import org.literacybridge.acm.util.UIUtils;
import org.literacybridge.acm.util.language.LanguageUtil;

public class ExportToDeviceTransferHandler extends TransferHandler {
	private static final long serialVersionUID = 1L;
	private static final Logger LOG = Logger.getLogger(ExportToDeviceTransferHandler.class.getName());

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
		final DeviceInfo device = (DeviceInfo) parent.getUserObject();
		
		Transferable transferable = support.getTransferable();
		
		try {
			final AudioItem[] audioItems =  (AudioItem[]) transferable.getTransferData(AudioItemView.AudioItemDataFlavor);
			// don't piggyback on the drag&drop thread
			Runnable job = new Runnable() {

				@Override
				public void run() {
					Application app = Application.getApplication();
					Container dialog = UIUtils.showDialog(app, new BusyDialog(LabelProvider.getLabel("EXPORTING_TO_TALKINGBOOK", LanguageUtil.getUILanguage()), app));
					try {
						for (AudioItem item : audioItems) {
							try {
								A18DeviceExporter.exportToDevice(item.getLocalizedAudioItem(LanguageUtil.getUserChoosenLanguage()), device);
							} catch (Exception e) {
								LOG.log(Level.WARNING, "Unable to export AudioItem with id=" + item.getUuid(), e);
							}
						}
					} finally {
						UIUtils.hideDialog(dialog);
					}
				}
			};
			
			new Thread(job).start();
			
		} catch (IOException e) {
			LOG.log(Level.WARNING, "Exporting audioitems failed.", e);
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