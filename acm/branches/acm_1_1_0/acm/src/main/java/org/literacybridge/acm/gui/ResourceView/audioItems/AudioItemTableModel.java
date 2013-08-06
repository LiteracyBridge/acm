package org.literacybridge.acm.gui.ResourceView.audioItems;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.apache.commons.lang.StringUtils;
import org.literacybridge.acm.api.IDataRequestResult;
import org.literacybridge.acm.config.Configuration;
import org.literacybridge.acm.content.AudioItem;
import org.literacybridge.acm.content.LocalizedAudioItem;
import org.literacybridge.acm.db.PersistentTag;
import org.literacybridge.acm.db.PersistentTagOrdering;
import org.literacybridge.acm.gui.Application;
import org.literacybridge.acm.gui.util.LocalizedAudioItemNode;
import org.literacybridge.acm.gui.util.UIUtils;
import org.literacybridge.acm.gui.util.language.LanguageUtil;
import org.literacybridge.acm.metadata.MetadataSpecification;
import org.literacybridge.acm.metadata.MetadataValue;
import org.literacybridge.acm.repository.AudioItemRepository.AudioFormat;

public class AudioItemTableModel  extends AbstractTableModel {

	private static final long serialVersionUID = -2998511081572936717L;

	private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
	
	// positions of the table columns
	public static final int NUM_COLUMNS 	   = 8; // keep in sync
	public static final int INFO_ICON 		   = 0;
	public static final int TITLE 			   = 1;
	public static final int DURATION 		   = 2;
	public static final int CATEGORIES 		   = 3;
	public static final int SOURCE			   = 4;
	public static final int MESSAGE_FORMAT	   = 5;
	public static final int DATE_FILE_MODIFIED = 6;
	public static final int PLAYLIST_ORDER	   = 7;
//	public static final int OPEN_COUNT 		   = 4;
//	public static final int COMPLETION_COUNT   = 5;
//	public static final int COPY_COUNT 		   = 6;
//	public static final int SURVEY1_COUNT 	   = 7;
//	public static final int APPLY_COUNT 	   = 8;
//	public static final int NOHELP_COUNT 	   = 9;
	private static String[] columns = null;
	
	protected IDataRequestResult result = null;
	
	
	public static void initializeTableColumns( String[] initalColumnNames) {
		columns = initalColumnNames;	
	}
	
	public AudioItemTableModel(IDataRequestResult result) {
		this.result = result;
		if (result != null) {
			result.getAudioItems();			
		}
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
				case INFO_ICON: {
					cellText = "";
					break;
				}
				case TITLE: {
					List<MetadataValue<String>> values = localizedAudioItem.getMetadata().getMetadataValues(
							MetadataSpecification.DC_TITLE);
					if (values != null) {
						cellText = values.get(0).getValue();
					}
					break;
				}
				case DURATION: {
					List<MetadataValue<String>> values = localizedAudioItem.getMetadata().getMetadataValues(
							MetadataSpecification.LB_DURATION);
					if (values != null && !StringUtils.isEmpty(values.get(0).getValue())) {
						cellText = values.get(0).getValue();
					}
					break;
				}
				case CATEGORIES: {
					cellText = UIUtils.getCategoryListAsString(localizedAudioItem.getParentAudioItem());
					break;
				}
				case SOURCE: {
					List<MetadataValue<String>> values = localizedAudioItem.getMetadata().getMetadataValues(
							MetadataSpecification.DC_SOURCE);
					if (values != null) {
						cellText = values.get(0).getValue();
					}
					break;
				}
				case MESSAGE_FORMAT: {
					List<MetadataValue<String>> values = localizedAudioItem.getMetadata().getMetadataValues(
							MetadataSpecification.LB_MESSAGE_FORMAT);
					if (values != null) {
						cellText = values.get(0).getValue();
					}
					break;
				}
				case DATE_FILE_MODIFIED: {
					File file = Configuration.getRepository().getAudioFile(audioItem, AudioFormat.A18);
					if (file != null) {
						Date date = new Date(file.lastModified());
						cellText = dateFormat.format(date);
					}					
					break;
				}
				case PLAYLIST_ORDER: {
					PersistentTag tag = Application.getFilterState().getSelectedTag();
					if (tag == null) {
						cellText = "";
					} else {
						PersistentTagOrdering ordering = PersistentTagOrdering.getFromDatabase(audioItem.getPersistentAudioItem(), tag);
						cellText = ordering.getPosition().toString();
					}
					break;
				}
//				case COPY_COUNT: {
//					List<MetadataValue<Integer>> values = localizedAudioItem.getMetadata().getMetadataValues(
//							MetadataSpecification.LB_COPY_COUNT);
//					cellText = values != null ? "" + values.get(0).getValue() : "0"; 
//					break;
//				}
//				case OPEN_COUNT: {
//					List<MetadataValue<Integer>> values = localizedAudioItem.getMetadata().getMetadataValues(
//							MetadataSpecification.LB_OPEN_COUNT);
//					cellText = values != null ? "" + values.get(0).getValue() : "0";
//					break;
//				}
//				case COMPLETION_COUNT: {
//					List<MetadataValue<Integer>> values = localizedAudioItem.getMetadata().getMetadataValues(
//							MetadataSpecification.LB_COMPLETION_COUNT);
//					cellText = values != null ? "" + values.get(0).getValue() : "0";
//					break;
//				}
//				case SURVEY1_COUNT: {
//					List<MetadataValue<Integer>> values = localizedAudioItem.getMetadata().getMetadataValues(
//							MetadataSpecification.LB_SURVEY1_COUNT);
//					cellText = values != null ? "" + values.get(0).getValue() : "0";
//					break;
//				}
//				case APPLY_COUNT: {
//					List<MetadataValue<Integer>> values = localizedAudioItem.getMetadata().getMetadataValues(
//							MetadataSpecification.LB_APPLY_COUNT);
//					cellText = values != null ? "" + values.get(0).getValue() : "0";
//					break;
//				}
//				case NOHELP_COUNT: {
//					List<MetadataValue<Integer>> values = localizedAudioItem.getMetadata().getMetadataValues(
//							MetadataSpecification.LB_NOHELP_COUNT);
//					cellText = values != null ? "" + values.get(0).getValue() : "0";
//					break;
//				}


				default: {
					cellText = "";
					break;
				}
			}
		} catch (Exception e) {
			 e.printStackTrace();
		}
	
		return new LocalizedAudioItemNode(localizedAudioItem, cellText, audioItem);
	}
	
	

	

}
