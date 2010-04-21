package org.literacybridge.acm.ui.ResourceView;

import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.literacybridge.acm.api.IDataRequestResult;
import org.literacybridge.acm.content.AudioItem;
import org.literacybridge.acm.metadata.Metadata;
import org.literacybridge.acm.metadata.MetadataSpecification;

import org.literacybridge.acm.util.LanguageUtil;

public class AudioItemTableModel extends AbstractTableModel {

	private static final long serialVersionUID = -2998511081572936717L;

	private final int TITLE 	= 0;
	private final int CREATOR 	= 1;
	private final int LANGUAGE 	= 2;
	private String[] columns = { "Title", "Creator", "Language" };
	
	private List<AudioItem> audioItemList = null;
	
	public AudioItemTableModel(IDataRequestResult result) {
		audioItemList = null;
		if (result != null) {
			audioItemList = result.getAudioItems();			
		}
	}
	
	@Override
	public int getColumnCount() {
		return columns.length;
	}

	@Override
	public int getRowCount() {
		return (audioItemList != null) ? audioItemList.size() : 0;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		Metadata metadata = audioItemList.get(rowIndex).getLocalizedAudioItem(LanguageUtil.getUserChoosenLanguage()).getMetadata();	
		
		String cellText = "<error occurred>";
		try {
			switch (columnIndex) {
			case TITLE:
				cellText = metadata.getMetadataValues(MetadataSpecification.DC_TITLE).get(0).getValue();
				break;
			case CREATOR:
				cellText = metadata.getMetadataValues(MetadataSpecification.DC_CREATOR).get(0).getValue();
				break;
			case LANGUAGE:
				cellText = metadata.getMetadataValues(MetadataSpecification.DC_LANGUAGE).get(0).getValue().toString();
				break;
			default:
				cellText = "<error occurred>";
				break;
			}
		} catch (Exception e) {
			// e.printStackTrace();
		}
		
		return cellText;
	}
}
