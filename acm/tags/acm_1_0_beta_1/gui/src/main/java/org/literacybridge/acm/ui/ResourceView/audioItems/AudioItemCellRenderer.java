package org.literacybridge.acm.ui.ResourceView.audioItems;

import java.awt.Component;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import org.literacybridge.acm.ui.ResourceView.audioItems.AudioItemTableModel.LocalizedAudioItemNode;

public class AudioItemCellRenderer extends DefaultTableCellRenderer {
	
	int highlightedRow = -1;
	
	@Override
	public Component getTableCellRendererComponent(JTable table,
													Object value, 
													boolean isSelected, 
													boolean hasFocus, 
													int row,
													int column) {
		
	    JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

		LocalizedAudioItemNode status = (LocalizedAudioItemNode) value;

        if (AudioItemTableModel.INFO_ICON == column && highlightedRow == row) {
        	label.setIcon(createImageIcon("/pencil-yellow.png", ""));
        } else {
        	label.setIcon(null);
        }

        label.setText(status.toString());
        
		return label;
	}
	
	/** Returns an ImageIcon, or null if the path was invalid. */
	protected ImageIcon createImageIcon(String path,
	                                           String description) {
	    java.net.URL imgURL = getClass().getResource(path);
	    if (imgURL != null) {
	        return new ImageIcon(imgURL, description);
	    } else {
	        System.err.println("Couldn't find file: " + path);
	        return null;
	    }
	}
}
