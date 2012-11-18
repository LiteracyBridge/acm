package org.literacybridge.acm.gui.dialogs.audioItemPropertiesDialog;

import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Locale;

import javax.swing.DefaultCellEditor;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableModel;

import org.jdesktop.swingx.JXTable;
import org.literacybridge.acm.gui.Application;
import org.literacybridge.acm.gui.UIConstants;
import org.literacybridge.acm.gui.util.UIUtils;

public class AudioItemPropertiesTable extends JXTable {

	private static final long serialVersionUID = 8525763640242614093L;
	private static ImageIcon editImageIcon = new ImageIcon(UIConstants.getResource(UIConstants.ICON_EDIT_16_PX));
	
	private JComboBox cb = new JComboBox();
	private LanguageComboBoxModel languageComboBoxModel = new LanguageComboBoxModel();
	
	public AudioItemPropertiesTable() {
		setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
			@Override public Component getTableCellRendererComponent(JTable table,
																	 Object value, 
																	 boolean isSelected, 
																	 boolean hasFocus, 
																	 int row,
																	 int column) {
				
			    JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

		        if (AudioItemPropertiesModel.EDIT_COL == column && getAudioItemPropertiesModel().showEditIcon(row)) {
		        	label.setIcon(editImageIcon);
		        } else {
		        	label.setIcon(null);
		        }

				return label;
			}

		});
		
		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				int row = rowAtPoint(e.getPoint());
				int col = columnAtPoint(e.getPoint());
				
				if (col == AudioItemPropertiesModel.EDIT_COL && getAudioItemPropertiesModel().showEditIcon(row)) {
					UIUtils.showDialog(Application.getApplication(), 
							           new CategoriesAndTagsEditDialog(Application.getApplication(),
							        		                           getAudioItemPropertiesModel().getSelectedAudioItem()));
				}
				
			}
		});
	}
	
	@Override
	public TableCellEditor getCellEditor(int row, int column) {
		if (isLanguageRow(row)) {
			return getLanguageEditor();
		}
		
		return super.getCellEditor(row, column);
	}

	private boolean isLanguageRow(int row) {
		AudioItemPropertiesModel model = getAudioItemPropertiesModel();
		if (model != null) {
			return model.isLanguageRow(row);
		}
		
		return false;
	}
	
	private AudioItemPropertiesModel getAudioItemPropertiesModel() {
		TableModel model = getModel();
		if (model != null && model instanceof AudioItemPropertiesModel){
			return (AudioItemPropertiesModel) getModel();
		}
		
		return null;
	}
	
	private TableCellEditor getLanguageEditor() {		
		cb.setModel(new LanguageComboBoxModel());   
		return new DefaultCellEditor(cb);
	}

	@Override
	public void setValueAt(Object aValue, int row, int column) {
		if (isLanguageRow(row)) {
			int index = cb.getSelectedIndex();
			Locale l = languageComboBoxModel.getLocalForIndex(index);
			super.setValueAt(l, row, column);
			return;
		}
		
		super.setValueAt(aValue, row, column);
	}
	
}
