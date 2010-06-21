package org.literacybridge.audioconverter.gui.fileView;

import java.util.HashMap;

import javax.swing.table.AbstractTableModel;

import org.literacybridge.audioconverter.gui.fileView.DataModel.FileInfo;

public class FileTableModel extends AbstractTableModel {
 
	private static final long serialVersionUID = -7158149341702217223L;
	
	// columns
	final int COL_CONVERT = 0;
	final int COL_NAME = 1;
	final int COL_EXTENSION = 2;
	final int COL_SIZE = 3;
	
	private HashMap columnNames = new HashMap();
	// rows
	private DataModel model = null;
	
	public FileTableModel() {
		super();
		columnNames.put(new Integer(COL_CONVERT), "Convert");
		columnNames.put(new Integer(COL_NAME), "Name");
		columnNames.put(new Integer(COL_EXTENSION), "Extension");
		columnNames.put(new Integer(COL_SIZE), "Size");
	}
	
	public void setFileInfoList(DataModel dataModel) {
		model = dataModel;
		fireTableDataChanged();
	}
	
	public void updateTable() {
		fireTableDataChanged();
	}
	
	public String getColumnName(int column) {
		return columnNames.get(new Integer(column)).toString();
	}

	public int getColumnCount() {
		return columnNames.size();
	}

	public int getRowCount() {
		if (model == null) return 0;
		
		return model.getFileInfoList().size();
	}

	public Object getValueAt(int rowIndex, int columnIndex) {
		if (model == null) return null;
		
		FileInfo fi = (FileInfo) model.getFileInfoList().get(rowIndex);
		switch (columnIndex) {
		case COL_CONVERT:
			return new Boolean(fi.doConvert());
		case COL_NAME:
			return fi.getFileName();
		case COL_EXTENSION:
			return fi.getFileExtension();
		case COL_SIZE:
			return fi.getFileSize();
			default:
				return null;
		}
	}
	
    public Class getColumnClass(int c) {
		if (model == null) return null;
		
        return getValueAt(0, c).getClass();
    }
    
    public boolean isCellEditable(int row, int col) {
        //Note that the data/cell address is constant,
        //no matter where the cell appears onscreen.
        if (col == COL_CONVERT) {
            return true;
        }
        
        return false;
    }
    
    public void setValueAt(Object value, int row, int col) {
    	if (model != null) {
    		if (col == COL_CONVERT) {
    			FileInfo fi = (FileInfo) model.getFileInfoList().get(row);
    			boolean convert = ((Boolean)value).booleanValue();
    			fi.setConvert(convert);
    		}    		
    	}
    	
    	fireTableCellUpdated(row, col);
    }  
}
