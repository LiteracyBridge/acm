package org.literacybridge.acm.ui.ResourceView.audioItems;

import java.awt.Component;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import org.literacybridge.acm.ui.UIConstants;
import org.literacybridge.acm.util.LocalizedAudioItemNode;

@SuppressWarnings("serial")
public class AudioItemCellRenderer extends DefaultTableCellRenderer {
	
	private static ImageIcon settingsImageIcon = new ImageIcon(UIConstants.getResource(UIConstants.ICON_SETTINGS_16_PX));
	
	public int highlightedRow = -1;
	
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
        	label.setIcon(settingsImageIcon);
        } else {
        	label.setIcon(null);
        }

        label.setText(status.toString());
        
		return label;
	}
}
