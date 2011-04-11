package org.literacybridge.acm.ui.dialogs.audioItemPropertiesDialog;

import java.util.Locale;

import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableModel;

import org.jdesktop.swingx.JXTable;

public class AudioItemPropertiesTable extends JXTable {

	private static final long serialVersionUID = 8525763640242614093L;
	private JComboBox cb = new JComboBox();
	private LanguageComboBoxModel languageComboBoxModel = new LanguageComboBoxModel();
		
	// LanguageUtil.getLocalizedLanguageName(supportedLanguages.get(index));
	
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
	
	private Locale getCurrentLocale() {
		AudioItemPropertiesModel model = getAudioItemPropertiesModel();
		if (model != null) {
			return model.getMetadataLocale();
		}	
		
		return Locale.ENGLISH;
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
