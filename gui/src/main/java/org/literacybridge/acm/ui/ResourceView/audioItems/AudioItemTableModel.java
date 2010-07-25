package org.literacybridge.acm.ui.ResourceView.audioItems;

import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.literacybridge.acm.api.IDataRequestResult;
import org.literacybridge.acm.categories.Taxonomy.Category;
import org.literacybridge.acm.content.AudioItem;
import org.literacybridge.acm.content.LocalizedAudioItem;
import org.literacybridge.acm.metadata.MetadataSpecification;
import org.literacybridge.acm.util.language.LanguageUtil;

public class AudioItemTableModel  extends AbstractTableModel {

	private static final long serialVersionUID = -2998511081572936717L;

	// positions of the table columns
	public static final int NUM_COLUMNS = 4; // keep in sync
	public static final int INFO_ICON = 0;
	public static final int TITLE 	= 1;
	public static final int CREATOR 	= 2;
	public static final int CATEGORIES 	= 3;
	private static String[] columns = null;
	
	private IDataRequestResult result = null;
	
	
	public AudioItemTableModel(IDataRequestResult result, String[] initalColumnNames) {
		this.result = result;
		if (result != null) {
			result.getAudioItems();			
		}
		
		if (initalColumnNames != null) {
			columns = initalColumnNames;	
		}
		
	}
	
	public AudioItemTableModel(IDataRequestResult result) {
		this(result, null);
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
		if (result != null) {
			return result.getAudioItems().size();	
		}
		
		return 0;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
	
		AudioItem audioItem = (AudioItem) result.getAudioItems().get(rowIndex);
		LocalizedAudioItem localizedAudioItem = audioItem.getLocalizedAudioItem(LanguageUtil.getUserChoosenLanguage());	
			
		String cellText = "";
		try {
			switch (columnIndex) {
			case INFO_ICON:
				cellText = "";
				break;
			case TITLE:
				cellText = localizedAudioItem.getMetadata().getMetadataValues(
						MetadataSpecification.DC_TITLE).get(0).getValue();
				break;
			case CREATOR:
				cellText = localizedAudioItem.getMetadata().getMetadataValues(
						MetadataSpecification.DC_CREATOR).get(0).getValue();
				break;
			case CATEGORIES:
				List<Category> categories = localizedAudioItem.getParentAudioItem().getCategoryList();
				StringBuilder builder = new StringBuilder();
				
				for (int i = 0; i < categories.size(); i++) {
					Category cat = categories.get(i);
					builder.append(cat.getCategoryName(LanguageUtil.getUserChoosenLanguage()));
					if (i != categories.size() - 1) {
						builder.append(", ");
					}
				}
				
				cellText = builder.toString();
				break;
			default:
				cellText = "";
				break;
			}
		} catch (Exception e) {
			// e.printStackTrace();
		}
	
		return new LocalizedAudioItemNode(localizedAudioItem, cellText, audioItem);
	}
	
	
	/**
	 * Helper
	 */
	
	
	public class LocalizedAudioItemNode {
		private final LocalizedAudioItem localizedAudioItem;
		private final String label;
		private final AudioItem parent;
		
		LocalizedAudioItemNode(LocalizedAudioItem localizedAudioItem, String label, AudioItem parent) {
			this.label = label;
			this.localizedAudioItem = localizedAudioItem;
			this.parent = parent;
		}

		public AudioItem getParent() {
			return parent;
		}

		@Override 
		public String toString() {
			return label;
		}

		public LocalizedAudioItem getLocalizedAudioItem() {
			return localizedAudioItem;
		}	
	}
}
