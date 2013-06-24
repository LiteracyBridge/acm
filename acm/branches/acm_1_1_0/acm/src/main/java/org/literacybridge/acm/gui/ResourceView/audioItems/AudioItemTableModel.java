package org.literacybridge.acm.gui.ResourceView.audioItems;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.List;

import javax.swing.table.AbstractTableModel;



import org.literacybridge.acm.api.IDataRequestResult;
import org.literacybridge.acm.categories.Taxonomy.Category;
import org.literacybridge.acm.config.Configuration;
import org.literacybridge.acm.content.AudioItem;
import org.literacybridge.acm.content.LocalizedAudioItem;
import org.literacybridge.acm.metadata.MetadataSpecification;
import org.literacybridge.acm.metadata.MetadataValue;
import org.literacybridge.acm.repository.AudioItemRepository;
import org.literacybridge.acm.repository.AudioItemRepository.AudioFormat;
import org.literacybridge.acm.utils.IOUtils;
import org.literacybridge.acm.gui.util.LocalizedAudioItemNode;
import org.literacybridge.acm.gui.util.UIUtils;
import org.literacybridge.acm.gui.util.language.LanguageUtil;

public class AudioItemTableModel  extends AbstractTableModel {

	private static final long serialVersionUID = -2998511081572936717L;

	// positions of the table columns
	public static final int NUM_COLUMNS 	 = 6; // keep in sync
	public static final int INFO_ICON 		 = 0;
	public static final int TITLE 			 = 1;
	public static final int DURATION 		 = 2;
	public static final int CATEGORIES 		 = 3;
	public static final int SOURCE			 = 4;
	public static final int LANGUAGES 		 = 5;
//	public static final int OPEN_COUNT 		 = 4;
//	public static final int COMPLETION_COUNT = 5;
//	public static final int COPY_COUNT 		 = 6;
//	public static final int SURVEY1_COUNT 	 = 7;
//	public static final int APPLY_COUNT 	 = 8;
//	public static final int NOHELP_COUNT 	 = 9;
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
					if (values != null) {
						cellText = values.get(0).getValue();
					} else {
						AudioItemRepository repository = Configuration.getRepository();
						File f = repository.getAudioFile(audioItem, AudioFormat.A18);
						DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(f)));
						in.skipBytes(4);
						int bps = IOUtils.readLittleEndian16(in);
						in.close();
						long sec = (f.length() * 8 + bps/2) / bps;
						int min = (int)(sec / 60L);
						//String test = String.valueOf(sec) + "/" + String.valueOf(bps) + "/" + String.valueOf(f.length());
						sec -= min * 60;
						String sMin = String.valueOf(min);
						String sSec = String.valueOf(sec);
						if (sMin.length()==1)
							sMin = "0" + sMin;
						if (sSec.length()==1)
							sSec = "0" + sSec;
						String duration = sMin + ":" + sSec + ((bps==16000)?"  l":"  h");
						//duration += test;
						audioItem.getLocalizedAudioItem(null).getMetadata().setMetadataField(MetadataSpecification.LB_DURATION, new MetadataValue<String>(duration));
						audioItem.commit();
						values = localizedAudioItem.getMetadata().getMetadataValues(MetadataSpecification.LB_DURATION);
						if (values != null) {
							cellText = values.get(0).getValue();
						}
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
				case LANGUAGES: {
					cellText = LanguageUtil.getLocalizedLanguageName(localizedAudioItem.getLocale());
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
