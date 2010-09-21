package org.literacybridge.acm.ui.ResourceView.audioItems;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.IOException;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.TransferHandler;

import org.literacybridge.acm.api.IDataRequestResult;
import org.literacybridge.acm.content.AudioItem;

public class AudioItemView extends GenericAudioItemTable {

	private boolean isInitialized = false;
	private AudioItemViewMouseListener mouseListener;
	
	public AudioItemView() {
		addHandler();
	}
	
	// Special handlers
	public void setData(IDataRequestResult result) {
		updateTable(new AudioItemTableModel(result));
		mouseListener.setCurrentResult(result);
	}

	public void addHandler() {
		
		final AudioItemCellRenderer renderer = new AudioItemCellRenderer();
		audioItemTable.setDefaultRenderer(Object.class, renderer);
		
		audioItemTable.addMouseMotionListener(new MouseMotionListener() {
			
			@Override
			public void mouseMoved(MouseEvent e) {
	           JTable table =  (JTable)e.getSource();
	           renderer.highlightedRow = table.rowAtPoint(e.getPoint());
	           table.repaint();
			}
			
			@Override
			public void mouseDragged(MouseEvent e) {
			}
		});
		
		audioItemTable.addMouseListener(new MouseListener() {
			@Override
			public void mouseExited(MouseEvent e) {
	           JTable table =  (JTable)e.getSource();
	           renderer.highlightedRow = -1;
	           table.repaint();
			}
			
			@Override public void mouseReleased(MouseEvent e) {}
			@Override public void mousePressed(MouseEvent e)  {}
			@Override public void mouseEntered(MouseEvent e)  {}
			@Override public void mouseClicked(MouseEvent e)  {}
		});
		
		audioItemTable.setTransferHandler(new TransferHandler() {
			
			@Override
            public int getSourceActions(JComponent c) {
                return MOVE;
            }
            
			@Override
			protected Transferable createTransferable(final JComponent c) {
				final DataFlavor[] flavors = new DataFlavor[] {AudioItemDataFlavor};
				
				return new Transferable() {
					@Override
					public Object getTransferData(DataFlavor flavor)
							throws UnsupportedFlavorException, IOException {
						JTable table = (JTable) c;
						int[] rows = table.getSelectedRows();

						AudioItem[] audioItems = new AudioItem[rows.length];
						for (int i = 0; i < rows.length; i++) {
							AudioItemTableModel.LocalizedAudioItemNode item = 
			                	(AudioItemTableModel.LocalizedAudioItemNode) table.getModel().getValueAt(rows[i], 0);
							audioItems[i] = item.getLocalizedAudioItem().getParentAudioItem();
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
		});
		
	    mouseListener = new AudioItemViewMouseListener(this);
	    audioItemTable.addMouseListener(mouseListener);
	    audioItemTable.getTableHeader().addMouseListener(mouseListener);
	}
	
} // class

