package org.literacybridge.acm.gui.MainWindow;

import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JTree;
import javax.swing.TransferHandler;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import org.literacybridge.acm.device.DeviceInfo;
import org.literacybridge.acm.gui.Application;
import org.literacybridge.acm.gui.MainWindow.audioItems.AudioItemView;
import org.literacybridge.acm.gui.dialogs.BusyDialog;
import org.literacybridge.acm.gui.resourcebundle.LabelProvider;
import org.literacybridge.acm.gui.util.UIUtils;
import org.literacybridge.acm.importexport.A18DeviceExporter;
import org.literacybridge.acm.store.AudioItem;

public class ExportToDeviceTransferHandler extends TransferHandler {
    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger
            .getLogger(ExportToDeviceTransferHandler.class.getName());

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
        if (!A18DeviceExporter.canExportToDevice(device)) {
            Application.getApplication().setStatusMessage("Can not export to this device.");
            return false;
        }

        Transferable transferable = support.getTransferable();

        try {
            final AudioItem[] audioItems = (AudioItem[]) transferable
                    .getTransferData(AudioItemView.AudioItemDataFlavor);
            // don't piggyback on the drag&drop thread
            Runnable job = new Runnable() {

                private boolean onProgress(Integer n, Integer m) {
                    // Exported n of m...
                    dialog.update(String.format(template, n, m));
                    return !dialog.isStopRequested();
                }
                String template = LabelProvider.getLabel("EXPORTED_N_OF_M");
                BusyDialog dialog;

                @Override
                public void run() {
                    Application app = Application.getApplication();
                    dialog = UIUtils.showDialog(app,
                            new BusyDialog(LabelProvider.getLabel("EXPORTING_TO_TALKINGBOOK"), app, true));
                    try {
                        int count = 0;
                        for (AudioItem item : audioItems) {
                            try {
                                A18DeviceExporter.exportToDevice(item, device);
                            } catch (Exception e) {
                                LOG.log(Level.WARNING, "Unable to export AudioItem with id=" + item.getUuid(), e);
                            }
                            if (!onProgress(++count, audioItems.length)) {
                                break;
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
                // A18Exporter.export(audioItem, device);
            }
        };

        new Thread(job).start();

        return true;
    }
}
