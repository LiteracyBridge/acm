package org.literacybridge.acm.rcp.views.requestResult;

import java.util.Locale;
import java.util.Set;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.literacybridge.acm.categories.Taxonomy.Category;
import org.literacybridge.acm.content.AudioItem;
import org.literacybridge.acm.metadata.Metadata;
import org.literacybridge.acm.metadata.MetadataSpecification;
import org.literacybridge.acm.rcp.core.Activator;
import org.literacybridge.acm.rcp.util.LanguageUtil;
import org.literacybridge.acm.rcp.views.requestResult.helpers.IAudioItemTableRowAdapter;
import org.literacybridge.acm.rcp.views.requestResult.helpers.ILocalizedAudioItemTableRowAdapter;

public class AudioItemTableLabelProvider extends ColumnLabelProvider implements
		ITableLabelProvider {

	@Override
	public Image getColumnImage(Object element, int columnIndex) {
		if (columnIndex == 0) {
			Locale local = null;
			if (element instanceof IAudioItemTableRowAdapter) {
				IAudioItemTableRowAdapter adapter = (IAudioItemTableRowAdapter) element;
				local = adapter.getDescriptor().getLocale();
			} else if (element instanceof ILocalizedAudioItemTableRowAdapter) {
				ILocalizedAudioItemTableRowAdapter adapter = (ILocalizedAudioItemTableRowAdapter) element;
				local = adapter.getLocalizedAudioItem().getLocale();
			}
			
			if (local != null) {
				return Activator.getImageDescriptor("/icons/lang_flags/" + LanguageUtil.getCountryCodeForLanguage(local) + ".gif").createImage();
			}		
		}

		return null;
	}

	@Override
	public String getColumnText(Object element, int columnIndex) {
		Metadata metadata = null;
		AudioItem item = null;
		
		if (element instanceof IAudioItemTableRowAdapter) {
			IAudioItemTableRowAdapter adapter = (IAudioItemTableRowAdapter) element;
			item = adapter.getAudioItem();
			metadata = item.getLocalizedAudioItem(LanguageUtil.getUserChoosenLanguage()).getMetadata();			
		} else if (element instanceof ILocalizedAudioItemTableRowAdapter) {
			ILocalizedAudioItemTableRowAdapter adapter = (ILocalizedAudioItemTableRowAdapter) element;
			metadata = adapter.getLocalizedAudioItem().getMetadata();
			item = adapter.getParent();
		}
			
		if (metadata != null) {
			Set<Category> categories = item.getCategories();
			switch (columnIndex) {
			case 0:
				return metadata.getMetadataValues(MetadataSpecification.DC_TITLE).get(0).getValue();
			case 1:
				return metadata.getMetadataValues(MetadataSpecification.DC_CREATOR).get(0).getValue();
			case 2:
				return metadata.getMetadataValues(MetadataSpecification.DC_LANGUAGE).get(0).getValue().toString();
			default:
				return "<error>";	
			}
		}
			
			
		return "<error>";
	}
}
