package org.literacybridge.acm.ui.ResourceView.audioItems;

import java.util.Locale;
import java.util.Set;

import org.jdesktop.swingx.treetable.AbstractTreeTableModel;
import org.literacybridge.acm.api.IDataRequestResult;
import org.literacybridge.acm.content.AudioItem;
import org.literacybridge.acm.content.LocalizedAudioItem;
import org.literacybridge.acm.metadata.MetadataSpecification;
import org.literacybridge.acm.util.language.LanguageUtil;

public class AudioItemTableModel  extends AbstractTreeTableModel {

	private static final long serialVersionUID = -2998511081572936717L;

	// positions of the table columns
	public static final int TITLE 	= 0;
	public static final int CREATOR 	= 1;
	public static final int LANGUAGE 	= 2;
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
	
    public int getChildCount(Object parent) {
    	if (parent instanceof IDataRequestResult) {
    		IDataRequestResult res = (IDataRequestResult) parent;
    		return res.getAudioItems().size();
    	}
    	
        if (parent instanceof AudioItem) {
        	AudioItem audioItem = (AudioItem) parent;
        	if (audioItem != null) {
        		return audioItem.getAvailableLocalizations().size();
        	}
        }

        return 0;
    }

	@Override
    public Object getChild(Object parent, int index) {
		if (parent instanceof IDataRequestResult) {
			IDataRequestResult res = (IDataRequestResult) parent;
			return res.getAudioItems().get(index);
		}
		
		if (parent instanceof AudioItem) {
			AudioItem audioItem = (AudioItem) parent;
			Set<Locale> availableLocals = audioItem.getAvailableLocalizations();
			Object[] o = availableLocals.toArray();
			for (int i = 0; i<o.length; i++) {
				if (i == index) {
					Locale l = (Locale) o[i];
					return audioItem.getLocalizedAudioItem(l);
				}
			}
			
		}
        
        return null;
    }
		
	@Override
	public Object getValueAt(Object node, int column) { 
		 LocalizedAudioItem localizedAudioItem = null;
		
		if (node instanceof LocalizedAudioItem) {
			localizedAudioItem = (LocalizedAudioItem) node;
		}
		
		if (node instanceof AudioItem) {
			AudioItem audioItem = (AudioItem) node;
			localizedAudioItem = audioItem.getLocalizedAudioItem(LanguageUtil.getUserChoosenLanguage());	
		}	
		
		String cellText = "";
		try {
			switch (column) {
			case TITLE:
				cellText = localizedAudioItem.getMetadata().getMetadataValues(
						MetadataSpecification.DC_TITLE).get(0).getValue();
				break;
			case CREATOR:
				cellText = localizedAudioItem.getMetadata().getMetadataValues(
						MetadataSpecification.DC_CREATOR).get(0).getValue();
				break;
			case LANGUAGE:
				cellText = localizedAudioItem.getMetadata().getMetadataValues(
						MetadataSpecification.DC_LANGUAGE).get(0).getValue()
						.toString();
				break;
			default:
				cellText = "";
				break;
			}
		} catch (Exception e) {
			// e.printStackTrace();
		}

		return new LocalizedAudioItemNode(localizedAudioItem, cellText);
	}
		
    @Override
    public boolean isLeaf(Object node) {
        if (node instanceof LocalizedAudioItem) {
        	return true;
        }
        
        return false;
    }
    
	@Override
    public Object getRoot() {
		return result;
    }
    

	public int getIndexOfChild(Object parent, Object child) {
		AudioItem audioItemParent = (AudioItem) parent;
		LocalizedAudioItem childItem = (LocalizedAudioItem) child;

		if (audioItemParent != null && childItem != null) {
			Set<Locale> availableLocals = audioItemParent.getAvailableLocalizations();
			int i = 0;
			for (Locale l : availableLocals) {
				if (l.equals(childItem)) {
					return i;
				}
				i++;
			}
		}
		
		return 0;
	}
	
	public static final class LocalizedAudioItemNode {
		public final LocalizedAudioItem localizedAudioItem;
		private final String label;
		
		LocalizedAudioItemNode(LocalizedAudioItem localizedAudioItem, String label) {
			this.label = label;
			this.localizedAudioItem = localizedAudioItem;
		}
		
		@Override public String toString() {
			return label;
		}
	}
	
	
}
