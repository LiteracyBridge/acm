package org.literacybridge.acm.ui.ResourceView.audioItems;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.swing.table.AbstractTableModel;

import org.jdesktop.swingx.treetable.AbstractTreeTableModel;
import org.literacybridge.acm.api.IDataRequestResult;
import org.literacybridge.acm.content.AudioItem;
import org.literacybridge.acm.content.LocalizedAudioItem;
import org.literacybridge.acm.metadata.Metadata;
import org.literacybridge.acm.metadata.MetadataSpecification;

import org.literacybridge.acm.util.LanguageUtil;

public class AudioItemTableModel  extends AbstractTreeTableModel {

	private static final long serialVersionUID = -2998511081572936717L;

	private final int TITLE 	= 0;
	private final int CREATOR 	= 1;
	private final int LANGUAGE 	= 2;
	private String[] columns = { "Title", "Creator", "Language" };
	
	private IDataRequestResult result = null;
	private List<AudioItem> audioItemList = null;
	
	public AudioItemTableModel(IDataRequestResult result) {
		this.result = result;
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
		Metadata metadata = null;
		
		if (node instanceof LocalizedAudioItem) {
			LocalizedAudioItem lItem = (LocalizedAudioItem) node;
			metadata = lItem.getMetadata();
		}
		
		if (node instanceof AudioItem) {
			AudioItem audioItem = (AudioItem) node;
			metadata = audioItem.getLocalizedAudioItem(LanguageUtil.getUserChoosenLanguage()).getMetadata();	
		}	
		
		String cellText = "<error occurred>";
		try {
			switch (column) {
			case TITLE:
				cellText = metadata.getMetadataValues(
						MetadataSpecification.DC_TITLE).get(0).getValue();
				break;
			case CREATOR:
				cellText = metadata.getMetadataValues(
						MetadataSpecification.DC_CREATOR).get(0).getValue();
				break;
			case LANGUAGE:
				cellText = metadata.getMetadataValues(
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

		return cellText;
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
	
	
}
