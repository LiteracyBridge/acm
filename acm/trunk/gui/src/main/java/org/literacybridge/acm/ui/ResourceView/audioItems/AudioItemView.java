package org.literacybridge.acm.ui.ResourceView.audioItems;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.IOException;
import java.util.Locale;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.TransferHandler;

import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.decorator.HighlighterFactory;
import org.literacybridge.acm.api.IDataRequestResult;
import org.literacybridge.acm.content.AudioItem;
import org.literacybridge.acm.content.LocalizedAudioItem;
import org.literacybridge.acm.resourcebundle.LabelProvider;
import org.literacybridge.acm.ui.Application;
import org.literacybridge.acm.ui.ResourceView.audioItems.AudioItemTableModel.LocalizedAudioItemNode;
import org.literacybridge.acm.util.language.LanguageUtil;
import org.literacybridge.acm.util.language.UILanguageChanged;

public class AudioItemView extends Container implements Observer {

	private static final long serialVersionUID = -2886958461177831842L;

	public final static DataFlavor AudioItemDataFlavor = new DataFlavor(LocalizedAudioItem.class, "LocalizedAudioItem");
	
	// model
	private IDataRequestResult currResult = null;

	// table - TODO this should not be public
	public JXTable audioItemTable = null;
	
	public AudioItemView(IDataRequestResult result) {
		// Set add the beginning as the result is already used in latter handlers
		this.currResult = result;
		
		setLayout(new BorderLayout());
		createTable();;
		Application.getMessageService().addObserver(this);
	
		initColumnSize();
	}

	private void createTable() {
		audioItemTable = new JXTable();
		audioItemTable.setModel(new AudioItemTableModel(currResult, getColumnTitles(LanguageUtil.getUILanguage())));
		audioItemTable.setShowGrid(false, false); 
		audioItemTable.setDragEnabled(true);
		
		// add handler
		addAudioItemTableHandler();
		
		// use fixed color; there seems to be a bug in some plaf implementations that cause strange rendering
		audioItemTable.addHighlighter(HighlighterFactory.createAlternateStriping(
				Color.white, new Color(237, 243, 254)));
		
		JScrollPane scrollPane = new JScrollPane(audioItemTable);
		scrollPane.setPreferredSize(new Dimension(800, 500));

		add(BorderLayout.CENTER, scrollPane);
	}

	// Special handlers
	protected void addAudioItemTableHandler() {
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
		
	    MouseListener mouseListener = new AudioItemViewMouseListener(this, currResult);
	    audioItemTable.addMouseListener(mouseListener);
	    audioItemTable.getTableHeader().addMouseListener(mouseListener);
	}


	private void updateTable() {
		audioItemTable.setModel(new AudioItemTableModel(currResult));
	}


	@Override
	public void update(Observable o, Object arg) {
		if (arg instanceof IDataRequestResult) {
			currResult = (IDataRequestResult) arg;
			updateTable();
		}
		
		if (arg instanceof UILanguageChanged) {
			UILanguageChanged newLocale = (UILanguageChanged) arg;
			updateControlLanguage(newLocale.getNewLocale());
		}
	}

	private void updateControlLanguage(Locale newLocale) {
		audioItemTable.getColumnModel().getColumn(AudioItemTableModel.TITLE)
										.setHeaderValue(LabelProvider.getLabel(LabelProvider.AUDIO_ITEM_TABLE_COLUMN_TITLE , newLocale));
		audioItemTable.getColumnModel().getColumn(AudioItemTableModel.CREATOR)
										.setHeaderValue(LabelProvider.getLabel(LabelProvider.AUDIO_ITEM_TABLE_COLUMN_CREATOR , newLocale));
		audioItemTable.getColumnModel().getColumn(AudioItemTableModel.CATEGORIES)
										.setHeaderValue(LabelProvider.getLabel(LabelProvider.AUDIO_ITEM_TABLE_COLUMN_CATEGORIES , newLocale));
	}

	
	private String[] getColumnTitles(Locale locale) {
		// order MUST fit match to table titles
		String[] columnTitleArray = new String[AudioItemTableModel.NUM_COLUMNS]; // SET
		columnTitleArray[AudioItemTableModel.INFO_ICON] = "";
		columnTitleArray[AudioItemTableModel.TITLE] = LabelProvider.getLabel(LabelProvider.AUDIO_ITEM_TABLE_COLUMN_TITLE , locale);
		columnTitleArray[AudioItemTableModel.CREATOR] = LabelProvider.getLabel(LabelProvider.AUDIO_ITEM_TABLE_COLUMN_CREATOR , locale);
		columnTitleArray[AudioItemTableModel.CATEGORIES] = LabelProvider.getLabel(LabelProvider.AUDIO_ITEM_TABLE_COLUMN_CATEGORIES , locale);
			
		return columnTitleArray;
	}


	private void initColumnSize() {
		audioItemTable.setAutoCreateColumnsFromModel( false );

		audioItemTable.getTableHeader().getColumnModel().getColumn(AudioItemTableModel.INFO_ICON).setMinWidth(25);
		audioItemTable.getTableHeader().getColumnModel().getColumn(AudioItemTableModel.INFO_ICON).setMaxWidth(25);
		audioItemTable.getTableHeader().getColumnModel().getColumn(AudioItemTableModel.TITLE).setPreferredWidth(250);
		audioItemTable.getTableHeader().getColumnModel().getColumn(AudioItemTableModel.CREATOR).setPreferredWidth(150);
		audioItemTable.getTableHeader().getColumnModel().getColumn(AudioItemTableModel.CATEGORIES).setPreferredWidth(150);
	}
	
	
	
	public AudioItem getValueAt(int row, int col) {
    	Object o = audioItemTable.getModel().getValueAt(row, col);
        
        AudioItem item = null;
        if (o instanceof LocalizedAudioItemNode) {
        	item = ((LocalizedAudioItemNode) o).getParent();
        } else if (o instanceof LocalizedAudioItem) {
        	LocalizedAudioItem lItem = (LocalizedAudioItem) o;
        	item = lItem.getParentAudioItem();
        }
 
        return item;
    }
    
    public boolean selectAudioItem(AudioItem audioItem) {
    	for (int i = 0; i < audioItemTable.getRowCount(); i++) {
    		AudioItem item = getValueAt(i, 0);
    		if (item != null) {
    			if (item.equals(audioItem)) {
    				ListSelectionModel selectionModel = audioItemTable.getSelectionModel();
    				if (selectionModel != null) {
        				selectionModel.setSelectionInterval(i, i);	
        				return true;
    				}
    			}
    		}
    	}
    	
    	return false;
    }	

} // class

