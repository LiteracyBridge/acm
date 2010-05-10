package org.literacybridge.acm.ui.ResourceView.audioItems;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.util.Locale;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.TransferHandler;
import javax.swing.tree.TreePath;

import org.jdesktop.swingx.JXTreeTable;
import org.jdesktop.swingx.decorator.HighlighterFactory;
import org.literacybridge.acm.api.IDataRequestResult;
import org.literacybridge.acm.content.AudioItem;
import org.literacybridge.acm.content.LocalizedAudioItem;
import org.literacybridge.acm.resourcebundle.LabelProvider;
import org.literacybridge.acm.ui.Application;
import org.literacybridge.acm.ui.dialogs.AudioItemPropertiesDialog;
import org.literacybridge.acm.util.language.LanguageUtil;
import org.literacybridge.acm.util.language.UILanguageChanged;

public class AudioItemView extends Container implements Observer {

	private static final long serialVersionUID = -2886958461177831842L;

	public final static DataFlavor AudioItemDataFlavor = new DataFlavor(LocalizedAudioItem.class, "LocalizedAudioItem");
	
	// model
	private IDataRequestResult currResult = null;

	// table
	private JXTreeTable audioItemTable = null;
	
	public AudioItemView(IDataRequestResult result) {
		setLayout(new BorderLayout());
		createTable();
		addHandlers();
		Application.getMessageService().addObserver(this);
		this.currResult = result;
	}

	private void createTable() {
		audioItemTable = new JXTreeTable();
		audioItemTable.setTreeTableModel(new AudioItemTableModel(currResult, getColumnTitles(LanguageUtil.getUILanguage())));
		audioItemTable.setShowGrid(false, false); 
		audioItemTable.setDragEnabled(true);
		audioItemTable.setTransferHandler(new TransferHandler() {
			
			@Override
            public int getSourceActions(JComponent c) {
                return COPY;
            }
            
			@Override
			protected Transferable createTransferable(final JComponent c) {
				final DataFlavor[] flavors = new DataFlavor[] {AudioItemDataFlavor};
				
				return new Transferable() {
					@Override
					public Object getTransferData(DataFlavor flavor)
							throws UnsupportedFlavorException, IOException {
						JXTreeTable table = (JXTreeTable)c;
		                int row = table.getSelectedRow();
		                AudioItemTableModel.LocalizedAudioItemNode item = 
		                	(AudioItemTableModel.LocalizedAudioItemNode) table.getModel().getValueAt(row, 0);
		                return item.localizedAudioItem;
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

		// use fixed color; there seems to be a bug in some plaf implementations that cause strange rendering
		audioItemTable.addHighlighter(HighlighterFactory.createAlternateStriping(
				Color.white, new Color(237, 243, 254)));
		
		JScrollPane scrollPane = new JScrollPane(audioItemTable);
		add(BorderLayout.CENTER, scrollPane);
	}

	private void updateTable() {
		audioItemTable.setTreeTableModel(new AudioItemTableModel(currResult));
	}

	private void addHandlers() {
	    MouseListener mouseListener = new PopupListener(this);
	    audioItemTable.addMouseListener(mouseListener);
	    audioItemTable.getTableHeader().addMouseListener(mouseListener);
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
		audioItemTable.getColumnModel().getColumn(AudioItemTableModel.LANGUAGE)
										.setHeaderValue(LabelProvider.getLabel(LabelProvider.AUDIO_ITEM_TABLE_COLUMN_LANGUAGE , newLocale));
	}
	
	private String[] getColumnTitles(Locale locale) {
		// order MUST fit match to table titles
		String[] columnTitleArray = new String[3];
		columnTitleArray[AudioItemTableModel.TITLE] = LabelProvider.getLabel(LabelProvider.AUDIO_ITEM_TABLE_COLUMN_TITLE , locale);
		columnTitleArray[AudioItemTableModel.CREATOR] = LabelProvider.getLabel(LabelProvider.AUDIO_ITEM_TABLE_COLUMN_CREATOR , locale);
		columnTitleArray[AudioItemTableModel.LANGUAGE] = LabelProvider.getLabel(LabelProvider.AUDIO_ITEM_TABLE_COLUMN_LANGUAGE , locale);
			
		return columnTitleArray;
	}
	
	private class PopupListener extends MouseAdapter {
		private AudioItemView adaptee = null;

		public PopupListener(AudioItemView adaptee) {
			this.adaptee = adaptee;
		}
		
		public void mouseReleased(MouseEvent e) {
			if (e.getClickCount() == 2) {
				showAudioItemDlg(e);				
			}
		}

		private void showAudioItemDlg(MouseEvent e) {
			int index = adaptee.audioItemTable.getSelectedRow();
			AudioItem audioItem = getValueAt(index, 0);
			System.out.println("UUID: " + audioItem.getUuid());
			
			AudioItemPropertiesDialog dlg = new AudioItemPropertiesDialog(
					Application.getApplication(), currResult.getAudioItems(),
					audioItem);
			dlg.setVisible(true);
		}
	}

    public AudioItem getValueAt(int row, int col) {
        TreePath tPath = audioItemTable.getPathForRow(row);
        Object[] oPath = tPath.getPath();
        int len = oPath.length;
        Object o = oPath[len - 1]; // get leaf
        
        AudioItem item = null;
        if (o instanceof AudioItem) {
        	item = (AudioItem) o;
        } else if (o instanceof LocalizedAudioItem) {
        	LocalizedAudioItem lItem = (LocalizedAudioItem) o;
        	item = lItem.getParentAudioItem();
        }
 
        return item;
    }
}

