package org.literacybridge.acm.gui.dialogs.audioItemPropertiesDialog;

import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;
import java.util.Locale;

import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

import org.apache.commons.lang.StringUtils;
import org.jdesktop.swingx.JXTable;
import org.literacybridge.acm.gui.Application;
import org.literacybridge.acm.gui.UIConstants;
import org.literacybridge.acm.gui.messages.SearchRequestMessage;
import org.literacybridge.acm.gui.util.UIUtils;
import org.literacybridge.acm.metadata.MetadataSpecification;

public class AudioItemPropertiesTable extends JXTable {

	private static final long serialVersionUID = 8525763640242614093L;
	private static ImageIcon editImageIcon = new ImageIcon(UIConstants.getResource(UIConstants.ICON_EDIT_16_PX));
	
	private JComboBox languageBox = new JComboBox();
	private JComboBox messageFormatBox = new JComboBox(new DefaultComboBoxModel(new String[] {"Drama", "Interview", "Lecture", "Song", "Story",
			 																				  "User feedback", "Success story", "Endorsement"}));
	private JComboBox targetAudienceBox = new JComboBox(new DefaultComboBoxModel(new String[] {"All", "Boys", "Girls", "Children", "Farmers",
																							   "Fathers", "Mothers", "Parents", "Pregnant women", "Livestock rearer"}));
	
	private JComboBox noLongerUsedBox = new JComboBox(new DefaultComboBoxModel(AudioItemPropertiesModel.STATUS_VALUES));
	private LanguageComboBoxModel languageComboBoxModel = new LanguageComboBoxModel();
	
	public AudioItemPropertiesTable(final AudioItemPropertiesDialog dialog) {
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

				if (e.getClickCount() == 2) {
					Object value = getValueAt(row, AudioItemPropertiesModel.VALUE_COL);
					if (value != null) {
						String text = value.toString();
						if (!StringUtils.isEmpty(text)) {
							try {
								if (text.startsWith("www.")) {
									text = "http://" + text;
								}
								URI uri = new URI(text);
								Desktop.getDesktop().browse(uri);
								return;
							} catch (Exception ex) {
								// ignore - this may not be a uri
							}
							
							AudioItemProperty<?> property = getAudioItemPropertiesModel().getAudioItemProperty(row);
							if (property instanceof AudioItemProperty.MetadataProperty) {
								if (((AudioItemProperty.MetadataProperty) property).getMetadataField() == MetadataSpecification.DC_RELATION) {
									searchRequest(text);
									dialog.setVisible(false);
									return;
								}
							}
							if (property.getName().equals("Related Message Title")) {
								searchRequest(text);
								dialog.setVisible(false);
								return;							
							}
						}
					}
				}
				
				if (col == AudioItemPropertiesModel.EDIT_COL && getAudioItemPropertiesModel().showEditIcon(row)) {
					UIUtils.showDialog(Application.getApplication(), 
							           new CategoriesAndTagsEditDialog(Application.getApplication(),
							        		                           getAudioItemPropertiesModel().getSelectedAudioItem()));
				}
				
			}
		});
	}
	
	private void searchRequest(String query) {
		Application.getFilterState().setFilterCategories(null);
		Application.getFilterState().setFilterLanguages(null);
		Application.getFilterState().setFilterString(query);
		Application.getMessageService().pumpMessage(new SearchRequestMessage(query));
	}
	
	@Override
	public TableCellEditor getCellEditor(int row, int column) {		
		if (isLanguageRow(row)) {
			return getLanguageEditor();
		}

		// TODO: refactor this and make combo boxes more generic
		AudioItemProperty property = getAudioItemPropertiesModel().getAudioItemProperty(row);
		if (property instanceof AudioItemProperty.MetadataProperty) {
			if (((AudioItemProperty.MetadataProperty) property).getMetadataField() == MetadataSpecification.LB_MESSAGE_FORMAT) {
				return new DefaultCellEditor(messageFormatBox);	
			}
			if (((AudioItemProperty.MetadataProperty) property).getMetadataField() == MetadataSpecification.LB_TARGET_AUDIENCE) {
				return new DefaultCellEditor(targetAudienceBox);	
			}
		}
		
		if (property.getName().equals(AudioItemPropertiesModel.STATUS_NAME)) {
			return new DefaultCellEditor(noLongerUsedBox);			
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
		languageBox.setModel(new LanguageComboBoxModel());   
		return new DefaultCellEditor(languageBox);
	}

	@Override
	public void setValueAt(Object aValue, int row, int column) {
		if (isLanguageRow(row)) {
			int index = languageBox.getSelectedIndex();
			Locale l = languageComboBoxModel.getLocalForIndex(index);
			super.setValueAt(l, row, column);
			return;
		}
		
		super.setValueAt(aValue, row, column);
	}
	
}
