package org.literacybridge.acm.gui.ResourceView.audioItems;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.text.Collator;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Observable;
import java.util.Observer;

import javax.swing.DropMode;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableColumn;

import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.decorator.AbstractHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.jdesktop.swingx.decorator.HighlighterFactory;
import org.literacybridge.acm.api.IDataRequestResult;
import org.literacybridge.acm.config.ControlAccess;
import org.literacybridge.acm.content.AudioItem;
import org.literacybridge.acm.content.LocalizedAudioItem;
import org.literacybridge.acm.gui.Application;
import org.literacybridge.acm.gui.messages.PlayLocalizedAudioItemMessage;
import org.literacybridge.acm.gui.messages.RequestAndSelectAudioItemMessage;
import org.literacybridge.acm.gui.messages.RequestAudioItemMessage;
import org.literacybridge.acm.gui.messages.RequestAudioItemToPlayMessage;
import org.literacybridge.acm.gui.messages.RequestedAudioItemMessage;
import org.literacybridge.acm.gui.resourcebundle.LabelProvider;
import org.literacybridge.acm.gui.util.LocalizedAudioItemNode;
import org.literacybridge.acm.gui.util.language.LanguageUtil;
import org.literacybridge.acm.gui.util.language.UILanguageChanged;
import org.literacybridge.acm.metadata.Metadata;
import org.literacybridge.acm.metadata.MetadataSpecification;
import org.literacybridge.acm.metadata.MetadataValue;

public class AudioItemView extends Container implements Observer {

	private static final long serialVersionUID = -2886958461177831842L;

	public static final DataFlavor AudioItemDataFlavor = new DataFlavor(LocalizedAudioItem.class, "LocalizedAudioItem");
	protected IDataRequestResult currResult = null;
	public JXTable audioItemTable = null;

	private AudioItemViewMouseListener mouseListener;
	
	private TableColumn orderingColumn;
	protected boolean firstDataSet = false;
	
	public AudioItemView() {
		AudioItemTableModel.initializeTableColumns(getColumnTitles(LanguageUtil.getUILanguage()));

		setLayout(new BorderLayout());
		createTable();
		addHandler();
		addToMessageService();
	}
	
	protected void addToMessageService() {
		Application.getMessageService().addObserver(this);
	}

	private void createTable() {
		audioItemTable = new JXTable();
		audioItemTable.setShowGrid(false, false);
		if (!ControlAccess.isACMReadOnly()) {
			audioItemTable.setDragEnabled(true);
			audioItemTable.setDropMode(DropMode.INSERT_ROWS);
			audioItemTable.setTransferHandler(new AudioItemTransferHandler());
		}
		
		// use fixed color; there seems to be a bug in some plaf implementations that cause strange rendering
		if (ControlAccess.isACMReadOnly() || ControlAccess.isSandbox()) {
			audioItemTable.addHighlighter(HighlighterFactory.createAlternateStriping(
					Color.LIGHT_GRAY, new Color(237, 243, 254)));			
		} else  {
			audioItemTable.addHighlighter(HighlighterFactory.createAlternateStriping(
					Color.white, new Color(237, 243, 254)));
		}
		
		JScrollPane scrollPane = new JScrollPane(audioItemTable);
		scrollPane.setPreferredSize(new Dimension(800, 500));
	
		add(BorderLayout.CENTER, scrollPane);
	}
	
	private void updateTable(AudioItemTableModel model) {
		audioItemTable.setModel(model);
		if (!firstDataSet) {
			initColumnSize();
			orderingColumn = audioItemTable.getTableHeader().getColumnModel().getColumn(AudioItemTableModel.PLAYLIST_ORDER);
			firstDataSet = true;
		}
		if (Application.getFilterState().getSelectedTag() == null && audioItemTable.getColumnCount() == audioItemTable.getModel().getColumnCount()) {
			audioItemTable.removeColumn(orderingColumn);
		} else if (Application.getFilterState().getSelectedTag() != null && audioItemTable.getColumnCount() < audioItemTable.getModel().getColumnCount()) {
			audioItemTable.addColumn(orderingColumn);
		}
	}

	/**
	 * Central message handler for the audio item view
	 */
	@Override
	public void update(Observable o, Object arg) {
		if (arg instanceof IDataRequestResult) {
			currResult = (IDataRequestResult) arg;
			updateTable(new AudioItemTableModel(currResult));
		}
		
		if (arg instanceof UILanguageChanged) {
			UILanguageChanged newLocale = (UILanguageChanged) arg;
			updateControlLanguage(newLocale.getNewLocale());
		}
		
		if (arg instanceof RequestAudioItemMessage) {
			RequestAudioItemMessage requestAudioItemMessage = (RequestAudioItemMessage) arg;	
			
			AudioItem audioItem = null;
			switch ( requestAudioItemMessage.getRequestType()) {
			case Current:
				audioItem = getCurrentAudioItem();
				break;
			case Next:
				audioItem = getNextAudioItem();
				break;
			case Previews:
				audioItem = getPreviousAudioItem();
				break;
			}
			
			if (audioItem != null) {
				selectAudioItem(audioItem);
				
				if (arg instanceof RequestAndSelectAudioItemMessage) {
					RequestedAudioItemMessage newMsg = new RequestedAudioItemMessage(audioItem);
					Application.getMessageService().pumpMessage(newMsg);
				} else if (arg instanceof RequestAudioItemToPlayMessage) {
					LocalizedAudioItem lai = audioItem.getLocalizedAudioItem(LanguageUtil.getUserChoosenLanguage());
					PlayLocalizedAudioItemMessage newMsg = new PlayLocalizedAudioItemMessage(lai);
					Application.getMessageService().pumpMessage(newMsg);				
				}	
			}
		}
	}
	

	private void updateControlLanguage(Locale newLocale) {
		audioItemTable.getColumnModel().getColumn(AudioItemTableModel.TITLE)
										.setHeaderValue(LabelProvider.getLabel(LabelProvider.AUDIO_ITEM_TABLE_COLUMN_TITLE , newLocale));
		audioItemTable.getColumnModel().getColumn(AudioItemTableModel.DURATION)
										.setHeaderValue(LabelProvider.getLabel(LabelProvider.AUDIO_ITEM_TABLE_COLUMN_DURATION , newLocale));
		audioItemTable.getColumnModel().getColumn(AudioItemTableModel.CATEGORIES)
										.setHeaderValue(LabelProvider.getLabel(LabelProvider.AUDIO_ITEM_TABLE_COLUMN_CATEGORIES , newLocale));
		audioItemTable.getColumnModel().getColumn(AudioItemTableModel.SOURCE)
										.setHeaderValue(LabelProvider.getLabel(LabelProvider.AUDIO_ITEM_TABLE_COLUMN_SOURCE , newLocale));
		audioItemTable.getColumnModel().getColumn(AudioItemTableModel.LANGUAGES)
										.setHeaderValue(LabelProvider.getLabel(LabelProvider.AUDIO_ITEM_TABLE_COLUMN_LANGUAGE , newLocale));
		audioItemTable.getColumnModel().getColumn(AudioItemTableModel.DATE_FILE_MODIFIED)
										.setHeaderValue(LabelProvider.getLabel(LabelProvider.AUDIO_ITEM_TABLE_COLUMN_DATE_FILE_MODIFIED , newLocale));
		audioItemTable.getColumnModel().getColumn(AudioItemTableModel.PLAYLIST_ORDER)
										.setHeaderValue(LabelProvider.getLabel(LabelProvider.AUDIO_ITEM_TABLE_COLUMN_PLAYLIST_ORDER , newLocale));
	}

	private String[] getColumnTitles(Locale locale) {
		// order MUST fit match to table titles
		String[] columnTitleArray = new String[AudioItemTableModel.NUM_COLUMNS]; // SET
		columnTitleArray[AudioItemTableModel.INFO_ICON] = "";
		columnTitleArray[AudioItemTableModel.TITLE] = LabelProvider.getLabel(LabelProvider.AUDIO_ITEM_TABLE_COLUMN_TITLE , locale);
		columnTitleArray[AudioItemTableModel.DURATION] = LabelProvider.getLabel(LabelProvider.AUDIO_ITEM_TABLE_COLUMN_DURATION , locale);
//		columnTitleArray[AudioItemTableModel.COPY_COUNT] = LabelProvider.getLabel(LabelProvider.AUDIO_ITEM_TABLE_COLUMN_COPY_COUNT, locale);
//		columnTitleArray[AudioItemTableModel.OPEN_COUNT] = LabelProvider.getLabel(LabelProvider.AUDIO_ITEM_TABLE_COLUMN_OPEN_COUNT, locale);
//		columnTitleArray[AudioItemTableModel.COMPLETION_COUNT] = LabelProvider.getLabel(LabelProvider.AUDIO_ITEM_TABLE_COLUMN_COMPLETION_COUNT, locale);
//		columnTitleArray[AudioItemTableModel.SURVEY1_COUNT] = LabelProvider.getLabel(LabelProvider.AUDIO_ITEM_TABLE_COLUMN_SURVEY1_COUNT, locale);
//		columnTitleArray[AudioItemTableModel.APPLY_COUNT] = LabelProvider.getLabel(LabelProvider.AUDIO_ITEM_TABLE_COLUMN_APPLY_COUNT, locale);
//		columnTitleArray[AudioItemTableModel.NOHELP_COUNT] = LabelProvider.getLabel(LabelProvider.AUDIO_ITEM_TABLE_COLUMN_NOHELP_COUNT, locale);
		columnTitleArray[AudioItemTableModel.CATEGORIES] = LabelProvider.getLabel(LabelProvider.AUDIO_ITEM_TABLE_COLUMN_CATEGORIES , locale);
		columnTitleArray[AudioItemTableModel.LANGUAGES] = LabelProvider.getLabel(LabelProvider.AUDIO_ITEM_TABLE_COLUMN_LANGUAGE , locale);
		columnTitleArray[AudioItemTableModel.SOURCE] = LabelProvider.getLabel(LabelProvider.AUDIO_ITEM_TABLE_COLUMN_SOURCE , locale);
		columnTitleArray[AudioItemTableModel.DATE_FILE_MODIFIED] = LabelProvider.getLabel(LabelProvider.AUDIO_ITEM_TABLE_COLUMN_DATE_FILE_MODIFIED , locale);
		columnTitleArray[AudioItemTableModel.PLAYLIST_ORDER] = LabelProvider.getLabel(LabelProvider.AUDIO_ITEM_TABLE_COLUMN_PLAYLIST_ORDER , locale);
				
		return columnTitleArray;
	}

	private void initColumnSize() {
		audioItemTable.setAutoCreateColumnsFromModel( false );
	
		audioItemTable.getTableHeader().getColumnModel().getColumn(AudioItemTableModel.INFO_ICON).setMaxWidth(25);
		audioItemTable.getTableHeader().getColumnModel().getColumn(AudioItemTableModel.TITLE).setPreferredWidth(230);
		audioItemTable.getTableHeader().getColumnModel().getColumn(AudioItemTableModel.DURATION).setPreferredWidth(65);
//		audioItemTable.getTableHeader().getColumnModel().getColumn(AudioItemTableModel.COPY_COUNT).setPreferredWidth(50);
//		audioItemTable.getTableHeader().getColumnModel().getColumn(AudioItemTableModel.OPEN_COUNT).setPreferredWidth(55);
//		audioItemTable.getTableHeader().getColumnModel().getColumn(AudioItemTableModel.COMPLETION_COUNT).setPreferredWidth(55);
//		audioItemTable.getTableHeader().getColumnModel().getColumn(AudioItemTableModel.SURVEY1_COUNT).setPreferredWidth(55);
//		audioItemTable.getTableHeader().getColumnModel().getColumn(AudioItemTableModel.APPLY_COUNT).setPreferredWidth(50);
//		audioItemTable.getTableHeader().getColumnModel().getColumn(AudioItemTableModel.NOHELP_COUNT).setPreferredWidth(65);
		audioItemTable.getTableHeader().getColumnModel().getColumn(AudioItemTableModel.CATEGORIES).setPreferredWidth(140);
		audioItemTable.getTableHeader().getColumnModel().getColumn(AudioItemTableModel.SOURCE).setPreferredWidth(140);
		audioItemTable.getTableHeader().getColumnModel().getColumn(AudioItemTableModel.DATE_FILE_MODIFIED).setPreferredWidth(140);
		audioItemTable.getTableHeader().getColumnModel().getColumn(AudioItemTableModel.PLAYLIST_ORDER).setPreferredWidth(60);
		
		Comparator<Object> comparator = new Comparator<Object>() {
			@Override public int compare(Object o1, Object o2) {
				try {
					Integer i1 = new Integer(o1.toString());
					Integer i2 = new Integer(o2.toString());
					return i1.compareTo(i2);
				} catch (NumberFormatException e) {
					return Collator.getInstance().compare(o1.toString(), o2.toString());
				}
			}
		};
		
//		audioItemTable.getColumnExt(AudioItemTableModel.COPY_COUNT).setComparator(comparator);
//		audioItemTable.getColumnExt(AudioItemTableModel.OPEN_COUNT).setComparator(comparator);
//		audioItemTable.getColumnExt(AudioItemTableModel.COMPLETION_COUNT).setComparator(comparator);
//		audioItemTable.getColumnExt(AudioItemTableModel.SURVEY1_COUNT).setComparator(comparator);
//		audioItemTable.getColumnExt(AudioItemTableModel.APPLY_COUNT).setComparator(comparator);
//		audioItemTable.getColumnExt(AudioItemTableModel.NOHELP_COUNT).setComparator(comparator);
	}

	boolean hasSelectedRows() {
		return audioItemTable.getSelectedRow() != -1;
	}
	
	AudioItem getCurrentAudioItem() {
		int tableRow = audioItemTable.getSelectedRow();
		if (tableRow == -1) {
			// select first row if available
			if (audioItemTable.getRowCount() > 0) {
				tableRow = 0;
				selectTableRow(tableRow);
			}
			else
			{
				return null;
			}
		}
		int modelRow = audioItemTable.convertRowIndexToModel(tableRow);
		
		return getValueAt(modelRow, 0);
	}
	
	AudioItem getNextAudioItem() {
		int tableRow = audioItemTable.getSelectedRow();
		if (tableRow < audioItemTable.getRowCount()-1) {
			tableRow++;
		}
		
		int modelRow = audioItemTable.convertRowIndexToModel(tableRow);
		return getValueAt(modelRow, 0);
	}	

	AudioItem getPreviousAudioItem() {
		int tableRow = audioItemTable.getSelectedRow();
		if (tableRow > 0) {
			tableRow--;
		}
		
		int modelRow = audioItemTable.convertRowIndexToModel(tableRow);
		return getValueAt(modelRow, 0);
	}

	public AudioItem getAudioItemAtTableRow(int row) {
		int modelRow = audioItemTable.convertRowIndexToModel(row);
		return getValueAt(modelRow, 0);
	}
	
	private AudioItem getValueAt(int modelRow, int col) {
		Object o = audioItemTable.getModel().getValueAt(modelRow, col);
	    
	    AudioItem item = null;
	    if (o instanceof LocalizedAudioItemNode) {
	    	item = ((LocalizedAudioItemNode) o).getParent();
	    } else if (o instanceof LocalizedAudioItem) {
	    	LocalizedAudioItem lItem = (LocalizedAudioItem) o;
	    	item = lItem.getParentAudioItem();
	    }
	    
	    item.refresh();
	   	
	    return item;
	}

	public boolean selectAudioItem(AudioItem audioItem) {
		for (int i = 0; i < audioItemTable.getRowCount(); i++) {
			int modelIndex = audioItemTable.convertRowIndexToModel(i);
			AudioItem item = getValueAt(modelIndex, 0);
			if (item != null) {
				if (item.equals(audioItem)) {
					return selectTableRow(i);
				}
			}
		}
		
		return false;
	}
	
	boolean selectTableRow(int rowStart, int rowEnd) {
		ListSelectionModel selectionModel = audioItemTable.getSelectionModel();
		if (selectionModel != null) {
			selectionModel.setSelectionInterval(rowStart, rowEnd);	
			return true;
		}
		
		return false;
	}
	
	
	boolean selectTableRow(int row) {
		return selectTableRow(row, row);
	}
	
	int getCurrentSelectedRow() {
		if (audioItemTable != null) {
			return audioItemTable.getSelectedRow();
		}
		
		return -1;
	}
	
	int[] getCurrentSelectedRows() {
		if (audioItemTable != null) {
			return audioItemTable.getSelectedRows();
		}
		
		return new int []{};
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
		
		audioItemTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override public void valueChanged(ListSelectionEvent e) {
				String message;
				switch (audioItemTable.getSelectedRowCount()) {
					case 0: message = ""; break;
					case 1: message ="1 audio item selected."; break;
					default: {
						Calendar cal = Calendar.getInstance();
						cal.set(0, 0, 0, 0, 0, 0);

						for (int row : getCurrentSelectedRows()) {
							AudioItem audioItem = getAudioItemAtTableRow(row);
							LocalizedAudioItem localizedAudioItem = audioItem.getLocalizedAudioItem(LanguageUtil.getUserChoosenLanguage());
							if (localizedAudioItem != null) {
								List<MetadataValue<String>> metadata = localizedAudioItem.getMetadata().getMetadataValues(MetadataSpecification.LB_DURATION);
								if (metadata != null && !metadata.isEmpty()) {
									String duration = localizedAudioItem.getMetadata().getMetadataValues(MetadataSpecification.LB_DURATION).get(0).getValue();
									try {
										cal.add(Calendar.MINUTE, Integer.parseInt(duration.substring(0, 2)));
										cal.add(Calendar.SECOND, Integer.parseInt(duration.substring(3, 5)));
									} catch (NumberFormatException ex) {
										// ignore this audio item
									}
								}
							}
						}
						
						Calendar cal1 = Calendar.getInstance();
						cal1.set(0, 0, 1, 0, 0, 0);
						SimpleDateFormat format = cal.before(cal1) ? new SimpleDateFormat("HH:mm:ss") : new SimpleDateFormat("D 'd' HH:mm:ss");
						message = audioItemTable.getSelectedRowCount() + " audio items selected. Total duration: " + format.format(cal.getTime());
					}
				}
				
				Application.getApplication().setStatusMessage(message);
			}
		});
		
		mouseListener = new AudioItemViewMouseListener(this);
	    audioItemTable.addMouseListener(mouseListener);
	    audioItemTable.getTableHeader().addMouseListener(mouseListener);
	}
} // class