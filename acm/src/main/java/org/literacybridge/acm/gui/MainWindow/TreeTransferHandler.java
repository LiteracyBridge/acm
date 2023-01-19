package org.literacybridge.acm.gui.MainWindow;

import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.gui.Application;
import org.literacybridge.acm.gui.MainWindow.SidebarView.CategoryTreeNodeObject;
import org.literacybridge.acm.gui.MainWindow.audioItems.AudioItemView;
import org.literacybridge.acm.gui.dialogs.BusyDialog;
import org.literacybridge.acm.gui.dialogs.PopUp;
import org.literacybridge.acm.gui.resourcebundle.LabelProvider;
import org.literacybridge.acm.gui.util.UIUtils;
import org.literacybridge.acm.importexport.AudioImporter;
import org.literacybridge.acm.repository.AudioItemRepository;
import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.store.Category;
import org.literacybridge.acm.store.Transaction;

import javax.swing.JOptionPane;
import javax.swing.JTree;
import javax.swing.TransferHandler;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TreeTransferHandler extends TransferHandler {
    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(TreeTransferHandler.class.getName());

    private static final DataFlavor[] supportedFlavors = new DataFlavor[]{
            DataFlavor.javaFileListFlavor,
            AudioItemView.AudioItemDataFlavor
    };

    @Override
    public boolean canImport(TransferHandler.TransferSupport support) {
        if (!support.isDrop()) {
            return false;
        }

        boolean supported = false;
        for (DataFlavor flavor : supportedFlavors) {
            if (support.isDataFlavorSupported(flavor)) {
                supported = true;
                break;
            }
        }

        if (!supported) {
            return false;
        }

        // Get drop location info.
        JTree.DropLocation dl = (JTree.DropLocation) support.getDropLocation();
        TreePath dest = dl.getPath();
        if (dest == null) {
            return false;
        }

        // For files, we can only copy; check if the source actions (a bitwise-OR of supported actions)
        // contains the COPY action.
        if (support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
            boolean copySupported = (COPY & support.getSourceDropActions()) == COPY;
            if (!copySupported) {
                return false;
            }
            support.setDropAction(COPY);
        }

        // only allow dropping on leaves
        DefaultMutableTreeNode parent = (DefaultMutableTreeNode) dest.getLastPathComponent();
        final CategoryTreeNodeObject target = (CategoryTreeNodeObject) parent.getUserObject();
        if (target.getCategory().hasChildren()) {
            return false;
        }

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
                importExternalFiles(support, target.getCategory());
                return true;
            } else if (support.isDataFlavorSupported(AudioItemView.AudioItemDataFlavor)) {
                assignCategory(support, target);
                return true;
            }
        } catch (UnsupportedFlavorException | IOException e) {
            LOG.log(Level.WARNING, "Exception while importing files.", e);
        }

        return false;
    }

    public static void importExternalFiles(
            TransferHandler.TransferSupport support, final Category category)
            throws IOException, UnsupportedFlavorException {
        Transferable t = support.getTransferable();
        //noinspection unchecked
        final List<File> files = (List<File>) t.getTransferData(DataFlavor.javaFileListFlavor);
        // don't piggyback on the drag&drop thread
        Runnable job = new Runnable() {
            private final FileFilter audioFilesFilter = AudioImporter.getInstance().getImportableFilesFilter();
            BusyDialog dialog = null;

            /**
             * The AudioImporter calls this to notify us of progress.
             * @param num Which # of import just completed.
             * @param max Total number of imports.
             */
            private boolean onProgress(Integer num, Integer max) {
                // Imported n of m...
                numImported = num;
                dialog.update(String.format(template, num, max));
                return !dialog.isStopRequested();
            }

            final String template = LabelProvider.getLabel("IMPORTED_N_OF_M");
            int numImported = 0;

            @Override
            public void run() {
                // Iterate over any directories in the dropped files.
                List<File> filesToImport = new LinkedList<>();
                files.forEach(f -> gatherFiles(f, filesToImport));

                Application parent = Application.getApplication();
                dialog = UIUtils.showDialog(parent,
                    new BusyDialog(LabelProvider.getLabel("IMPORTING_FILES"), parent, true));

                try {
                    int numImported = 0;
                    for (File f : filesToImport) {
                        try {
                            // TODO: See if an audio item of this name exists.
                            // TODO: Query user for the language to apply to the audio item
                            AudioImporter.getInstance()
                                .importDroppedFile(f, item -> item.addCategory(category));

                        } catch (AudioItemRepository.MissingItemException e) {
                            new PopUp.Builder().withParent(dialog)
                                .withTitle("Missing Audio Item")
                                .withContents("No audio item to update from " + f.getName())
                                .withMessageType(JOptionPane.ERROR_MESSAGE)
                                .withOptOut()
                                .go();
                        } catch (Exception e) {
                            LOG.log(Level.WARNING, "Failed to import file " + f, e);
                        }
                        boolean okToContinue = onProgress(++numImported, filesToImport.size());
                        if (!okToContinue) {
                            break;
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    UIUtils.hideDialog(dialog);
                    Application.getFilterState().updateResult(true);
                    Application.getApplication().setStatusMessage(String.format("%d Item(s) imported.", numImported));
                }
            }

            private void gatherFiles(File dir, List<File> filesToImport) {
                if (!dir.isDirectory()) {
                    filesToImport.add(dir);
                } else {
                    File[] files = dir.listFiles(audioFilesFilter);
                    if (files!=null) Collections.addAll(filesToImport, files);

                    File[] subdirs = dir.listFiles(File::isDirectory);

                    if (subdirs != null) {
                        for (File subDir : subdirs) {
                            gatherFiles(subDir, filesToImport);
                        }
                    }
                }
            }


        };

        new Thread(job).start();
    }

    /**
     * Handles dropping from audio items window onto a category.
     * @param support drop helper object from Swing
     * @param target where the object(s) was(were) dropped
     * @throws IOException is probably never thrown
     * @throws UnsupportedFlavorException ??
     */
    private void assignCategory(TransferHandler.TransferSupport support,
                                final CategoryTreeNodeObject target)
            throws IOException, UnsupportedFlavorException {
        Transferable t = support.getTransferable();
        final boolean move = support.getDropAction() == TransferHandler.MOVE;

        final AudioItem[] audioItems = (AudioItem[]) t
                .getTransferData(AudioItemView.AudioItemDataFlavor);
        // don't piggyback on the drag&drop thread
        Runnable job = () -> {
            Transaction transaction = ACMConfiguration.getInstance().getCurrentDB()
                    .getMetadataStore().newTransaction();
            boolean success = false;
            try {
                for (AudioItem item : audioItems) {
                    if (move) {
                        item.removeAllCategories();
                    }
                    item.addCategory(target.getCategory());
                    transaction.add(item);
                }
                transaction.commit();
                success = true;
            } catch (IOException e) {
                LOG.log(Level.SEVERE, "Unable to commit transaction.", e);
            } finally {
                if (!success) {
                    try {
                        transaction.rollback();
                    } catch (IOException e) {
                        LOG.log(Level.SEVERE, "Unable to rollback transaction.", e);
                    }
                }
                Application.getFilterState().updateResult(true);
            }
        };
        new Thread(job).start();

    }
}
