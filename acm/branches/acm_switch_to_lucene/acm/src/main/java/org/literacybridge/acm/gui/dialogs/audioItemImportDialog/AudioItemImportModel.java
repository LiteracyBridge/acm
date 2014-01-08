package org.literacybridge.acm.gui.dialogs.audioItemImportDialog;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.literacybridge.acm.categories.Taxonomy.Category;
import org.literacybridge.acm.content.AudioItem;
import org.literacybridge.acm.gui.util.AudioItemNode;
import org.literacybridge.acm.importexport.A18Importer;
import org.literacybridge.acm.metadata.MetadataSpecification;

@SuppressWarnings("serial")
public class AudioItemImportModel extends AbstractTableModel {

	// positions of the table columns
	public static final int NUM_COLUMNS = 4; // keep in sync
	public static final int INFO_ICON 	= 0;
	public static final int TITLE 		= 1;
	public static final int CATEGORIES 	= 2;
	public static final int LANGUAGES	= 3;
	private static String[] columns = null;
	
	private final AudioItemNode[] rowIndex2audioItem;
	private final List<File> sourceFiles;
	
	public List<File> getEnabledAudioItems() {
		List<File> list = new ArrayList<File>();

		for (int i = 0; i < rowIndex2audioItem.length; i++) {
			if (rowIndex2audioItem[i].isEnabled()) {
				list.add(sourceFiles.get(i));
			}
		}
		
		return list;
	}
	
	public void setStateForAllItems(boolean enable) {
		for (AudioItemNode node : rowIndex2audioItem) {
			node.setEnabled(enable);
		}
		
		fireTableDataChanged();
	}
	
	public AudioItemImportModel(List<File> filesToImport) throws IOException {
		rowIndex2audioItem = new AudioItemNode[filesToImport.size()];
		this.sourceFiles = filesToImport;
		initializeModel(filesToImport);
	}
	
	public static void initializeTableColumns( String[] initalColumnNames) {
		columns = initalColumnNames;	
	}
	
	private void initializeModel(List<File> filesToImport) throws IOException {
		
		for(int i=0; i<filesToImport.size(); ++i) {
			File file = filesToImport.get(i);
			AudioItem audioItem = A18Importer.loadMetadata(file);	
			rowIndex2audioItem[i] = new AudioItemNode("", audioItem);
	
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
		return rowIndex2audioItem.length;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
	
		AudioItemNode item = rowIndex2audioItem[rowIndex];
		AudioItem audioItem = item.getAudioItem();	
			
		try {
			switch (columnIndex) {
			case INFO_ICON:
				return new Boolean(item.isEnabled());			
			case TITLE:
				return audioItem.getMetadata().getMetadataValues(
						MetadataSpecification.DC_TITLE).get(0).getValue();
			case CATEGORIES:
				List<Category> categories = audioItem.getCategoryList();
				StringBuilder builder = new StringBuilder();
				
				for (int i = 0; i < categories.size(); i++) {
					Category cat = categories.get(i);
					builder.append(cat.getCategoryName());
					if (i != categories.size() - 1) {
						builder.append(", ");
					}
				}
				
				return builder.toString();
			case LANGUAGES:
				return audioItem.getMetadata().getMetadataValues(
						MetadataSpecification.DC_LANGUAGE).get(0).getValue();
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
			AudioItemNode node = rowIndex2audioItem[rowIndex];
			Boolean enable = (Boolean) aValue;
			node.setEnabled(enable.booleanValue());
		}

		super.setValueAt(aValue, rowIndex, columnIndex);
	}

	
}
