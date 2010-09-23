package org.literacybridge.acm.ui.dialogs.audioItemImportDialog;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.swing.table.AbstractTableModel;

import org.literacybridge.acm.api.IDataRequestResult;
import org.literacybridge.acm.categories.Taxonomy.Category;
import org.literacybridge.acm.content.AudioItem;
import org.literacybridge.acm.content.LocalizedAudioItem;
import org.literacybridge.acm.metadata.MetadataSpecification;
import org.literacybridge.acm.util.LocalizedAudioItemNode;
import org.literacybridge.acm.util.language.LanguageUtil;

@SuppressWarnings("serial")
public class AudioItemImportModel extends AbstractTableModel {

	// positions of the table columns
	public static final int NUM_COLUMNS = 4; // keep in sync
	public static final int INFO_ICON = 0;
	public static final int TITLE 	= 1;
	public static final int CREATOR 	= 2;
	public static final int CATEGORIES 	= 3;
	private static String[] columns = null;
	
	protected IDataRequestResult result = null;
	
	private HashMap<Integer, LocalizedAudioItemNode> rowIndex2audioItem = new HashMap<Integer, LocalizedAudioItemNode>();
	
	public List<AudioItem> getEnabledAudioItems() {
		Vector<AudioItem> list = new Vector<AudioItem>();

		Iterator<LocalizedAudioItemNode> iter = rowIndex2audioItem.values().iterator();
		while(iter.hasNext()) {
			LocalizedAudioItemNode node = iter.next();
			if (node.isEnabled()) {
				list.add(node.getParent());
			}
		}
		
		return list;
	}
	
	public AudioItemImportModel(IDataRequestResult result) {
		this.result = result;
		initializeModel(result);
	}
	
	public static void initializeTableColumns( String[] initalColumnNames) {
		columns = initalColumnNames;	
	}
	
	private void initializeModel(IDataRequestResult result) {
		for(int i=0; i<result.getAudioItems().size(); ++i) {
			AudioItem audioItem = result.getAudioItems().get(i);
			LocalizedAudioItem localizedAudioItem = audioItem.getLocalizedAudioItem(LanguageUtil.getUserChoosenLanguage());	
			rowIndex2audioItem.put(new Integer(i), new LocalizedAudioItemNode(localizedAudioItem, "", audioItem));
	
		}		
	}
	
	@Override
	public int getColumnCount() {
		return columns.length;
	}
	
	@Override
	public String getColumnName(int column) {
		return columns[column];
	}	

	@Override
	public int getRowCount() {
		return rowIndex2audioItem.size();
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
	
		LocalizedAudioItemNode item = rowIndex2audioItem.get(rowIndex);
		AudioItem audioItem = item.getParent();
		LocalizedAudioItem localizedAudioItem = audioItem.getLocalizedAudioItem(LanguageUtil.getUserChoosenLanguage());	
			
		try {
			switch (columnIndex) {
			case INFO_ICON:
				return new Boolean(item.isEnabled());			
			case TITLE:
				return localizedAudioItem.getMetadata().getMetadataValues(
						MetadataSpecification.DC_TITLE).get(0).getValue();
			case CREATOR:
				return localizedAudioItem.getMetadata().getMetadataValues(
						MetadataSpecification.DC_CREATOR).get(0).getValue();
			case CATEGORIES:
				List<Category> categories = localizedAudioItem.getParentAudioItem().getCategoryList();
				StringBuilder builder = new StringBuilder();
				
				for (int i = 0; i < categories.size(); i++) {
					Category cat = categories.get(i);
					builder.append(cat.getCategoryName(LanguageUtil.getUserChoosenLanguage()));
					if (i != categories.size() - 1) {
						builder.append(", ");
					}
				}
				
				return builder.toString();
			default:
				return "";
			}
		} catch (Exception e) {
			// e.printStackTrace();
		}
	
		return "";
	}
		
	@Override
    public Class<?> getColumnClass(int c) {
    	if (c == INFO_ICON) {
    		return Boolean.class;
    	} 
    	
    	return String.class;
    }

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		if (columnIndex == INFO_ICON) {
			return true;
		}
		
		return super.isCellEditable(rowIndex, columnIndex);
	}
	
	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
		if (columnIndex == INFO_ICON) {
			LocalizedAudioItemNode node = rowIndex2audioItem.get(new Integer(rowIndex));
			Boolean enable = (Boolean) aValue;
			node.setEnabled(enable.booleanValue());
			//fireTableCellUpdated(rowIndex, columnIndex);
		}

		super.setValueAt(aValue, rowIndex, columnIndex);
	}

	
}
