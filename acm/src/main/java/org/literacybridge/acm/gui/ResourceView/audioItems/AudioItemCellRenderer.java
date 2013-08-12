package org.literacybridge.acm.gui.ResourceView.audioItems;

import static org.literacybridge.acm.metadata.MetadataSpecification.LB_STATUS;

import java.awt.Component;
import java.awt.Font;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import org.literacybridge.acm.gui.UIConstants;
import org.literacybridge.acm.gui.dialogs.audioItemPropertiesDialog.AudioItemPropertiesModel;
import org.literacybridge.acm.gui.util.LocalizedAudioItemNode;
import org.literacybridge.acm.metadata.MetadataValue;

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
 
        List<MetadataValue<Integer>> statuses = status.getLocalizedAudioItem().getMetadata().getMetadataValues(LB_STATUS);
		if (statuses != null && !statuses.isEmpty() && AudioItemPropertiesModel.STATUS_VALUES[statuses.get(0).getValue()] == AudioItemPropertiesModel.NO_LONGER_USED) {
            Font italicsLabel =new Font(label.getFont().getName(),Font.ITALIC,label.getFont().getSize());
            label.setFont(italicsLabel);
        }
		return label;
	}
}
