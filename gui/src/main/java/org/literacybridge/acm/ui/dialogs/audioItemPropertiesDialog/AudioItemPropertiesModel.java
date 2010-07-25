package org.literacybridge.acm.ui.dialogs.audioItemPropertiesDialog;

import static org.literacybridge.acm.metadata.MetadataSpecification.DC_CONTRIBUTOR;
import static org.literacybridge.acm.metadata.MetadataSpecification.DC_COVERAGE;
import static org.literacybridge.acm.metadata.MetadataSpecification.DC_CREATOR;
import static org.literacybridge.acm.metadata.MetadataSpecification.DC_DESCRIPTION;
import static org.literacybridge.acm.metadata.MetadataSpecification.DC_FORMAT;
import static org.literacybridge.acm.metadata.MetadataSpecification.DC_PUBLISHER;
import static org.literacybridge.acm.metadata.MetadataSpecification.DC_RELATION;
import static org.literacybridge.acm.metadata.MetadataSpecification.DC_RIGHTS;
import static org.literacybridge.acm.metadata.MetadataSpecification.DC_SOURCE;
import static org.literacybridge.acm.metadata.MetadataSpecification.DC_SUBJECT;
import static org.literacybridge.acm.metadata.MetadataSpecification.DC_TITLE;

import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.table.AbstractTableModel;

import org.literacybridge.acm.metadata.Metadata;
import org.literacybridge.acm.metadata.MetadataField;
import org.literacybridge.acm.metadata.MetadataValue;
import org.literacybridge.acm.resourcebundle.LabelProvider;
import org.literacybridge.acm.util.language.LanguageUtil;

public class AudioItemPropertiesModel extends AbstractTableModel {

	private String[] columnNames = {LabelProvider.getLabel("AUDIO_ITEM_PROPERTIES_HEADER_PROPERTY", LanguageUtil.getUserChoosenLanguage())
								  , LabelProvider.getLabel("AUDIO_ITEM_PROPERTIES_HEADER_VALUE", LanguageUtil.getUserChoosenLanguage())};
	
	private final int TITLE_COL = 0;
	private final int VALUE_COL = 1;
	
	private Metadata metadata = null;
	private Vector<AudioItemPropertiesObject> audioItemPropertiesObject = new Vector<AudioItemPropertiesObject>();
	
	public AudioItemPropertiesModel(Metadata metadata) {
		this.metadata = metadata;
		audioItemPropertiesObject.add(new AudioItemPropertiesObject(DC_TITLE));
		audioItemPropertiesObject.add(new AudioItemPropertiesObject(DC_CREATOR));
		audioItemPropertiesObject.add(new AudioItemPropertiesObject(DC_SUBJECT));
		audioItemPropertiesObject.add(new AudioItemPropertiesObject(DC_DESCRIPTION));
		audioItemPropertiesObject.add(new AudioItemPropertiesObject(DC_PUBLISHER));
		audioItemPropertiesObject.add(new AudioItemPropertiesObject(DC_CONTRIBUTOR));
		audioItemPropertiesObject.add(new AudioItemPropertiesObject(DC_FORMAT));
		audioItemPropertiesObject.add(new AudioItemPropertiesObject(DC_RELATION));
		audioItemPropertiesObject.add(new AudioItemPropertiesObject(DC_SOURCE));
		audioItemPropertiesObject.add(new AudioItemPropertiesObject(DC_COVERAGE));
		audioItemPropertiesObject.add(new AudioItemPropertiesObject(DC_RIGHTS));		
	}
	
	@Override
	public String getColumnName(int arg0) {
		return columnNames[arg0];
	}

	@Override
	public int getColumnCount() {
		return columnNames.length;
	}

	@Override
	public int getRowCount() {
		return audioItemPropertiesObject.size();
	}

	@Override
	public Object getValueAt(int row, int col) {
		AudioItemPropertiesObject obj = audioItemPropertiesObject.get(row);
		
		switch (col) {
		case 0:
			return LabelProvider.getLabel(obj.getFieldID(), LanguageUtil.getUserChoosenLanguage());
		case 1:
			return Metadata.getCommaSeparatedList(metadata, obj.getFieldID());
		default:
			break;
		}
			
		return LabelProvider.getLabel("ERROR", LanguageUtil.getUserChoosenLanguage());
	}
	
	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		return (columnIndex == VALUE_COL);
	}

	@Override
	public void setValueAt(Object aValue, int row, int col) {
		String newValue = aValue.toString();
		AudioItemPropertiesObject obj = audioItemPropertiesObject.get(row);
		setValues(obj.getFieldID(), metadata, newValue);
		metadata.commit();
	}

	private void setValues(MetadataField<String> field, Metadata metadata,
			String commaSeparatedListValue) {
		StringTokenizer t = new StringTokenizer(commaSeparatedListValue, ",");

		List<MetadataValue<String>> existingValues = metadata
				.getMetadataValues(field);
		Iterator<MetadataValue<String>> it = null;

		if (existingValues != null) {
			it = existingValues.iterator();
		}

		while (t.hasMoreTokens()) {
			String value = t.nextToken().trim();
			if (it != null && it.hasNext()) {
				it.next().setValue(value);
			} else {
				metadata.addMetadataField(field, new MetadataValue<String>(
						value));
			}
		}
	}
}
