package org.literacybridge.acm.gui.ResourceView.audioItems;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.TransferHandler;

import org.literacybridge.acm.content.AudioItem;
import org.literacybridge.acm.gui.ResourceView.TreeTransferHandler;
import org.literacybridge.acm.gui.util.AudioItemNode;

public class AudioItemTransferHandler extends TransferHandler {
	private static final long serialVersionUID = 1L;
	private static final Logger LOG = Logger.getLogger(AudioItemTransferHandler.class.getName());

	@Override
	public boolean canImport(TransferHandler.TransferSupport support) {
		if (!support.isDrop()) {
			return false;
		}

		if (!support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
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

		try {
			TreeTransferHandler.importExternalFiles(support, null);
			return true;
		} catch (UnsupportedFlavorException e) {
			LOG.log(Level.WARNING, "Exception while importing files.", e);
		} catch (IOException e) {
			LOG.log(Level.WARNING, "Exception while importing files.", e);
		}
		
		return false;
	}

	
	@Override
    public int getSourceActions(JComponent c) {
        return TransferHandler.COPY_OR_MOVE;
    }
    
	@Override
	protected Transferable createTransferable(final JComponent c) {
		final DataFlavor[] flavors = new DataFlavor[] {AudioItemView.AudioItemDataFlavor};
		
		return new Transferable() {
			@Override
			public Object getTransferData(DataFlavor flavor)
					throws UnsupportedFlavorException, IOException {
				JTable table = (JTable) c;
				int[] rows = table.getSelectedRows();
				
				AudioItem[] audioItems = new AudioItem[rows.length];
				for (int i = 0; i < rows.length; i++) {
					AudioItemNode item = 
	                	(AudioItemNode) table.getModel().getValueAt(table.convertRowIndexToModel(rows[i]), 0);
					audioItems[i] = item.getAudioItem();
				}
				
                return audioItems; 
			}

			@Override
			public DataFlavor[] getTransferDataFlavors() {
				return (DataFlavor[]) flavors.clone();
			}

			@Override
			public boolean isDataFlavorSupported(DataFlavor flavor) {
		        for (int i = 0; i < flavors.length; i++) {
		    	    if (flavor.equals(flavors[i])) {
		    	        return true;
		    	    }
		    	}
		    	return false;
			}
		};
	}
}
