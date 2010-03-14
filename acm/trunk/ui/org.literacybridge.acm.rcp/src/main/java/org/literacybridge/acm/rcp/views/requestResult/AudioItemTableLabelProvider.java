package org.literacybridge.acm.rcp.views.requestResult;

import java.util.Locale;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.literacybridge.acm.categories.Taxonomy.Category;
import org.literacybridge.acm.content.AudioItem;
import org.literacybridge.acm.metadata.Metadata;
import org.literacybridge.acm.metadata.MetadataSpecification;
import org.literacybridge.acm.rcp.views.requestResult.helpers.IAudioItemTableRowAdapter;

public class AudioItemTableLabelProvider extends LabelProvider implements
		ITableLabelProvider {

	@Override
	public Image getColumnImage(Object element, int columnIndex) {
		return null;
	}

	@Override
	public String getColumnText(Object element, int columnIndex) {
		if (element instanceof IAudioItemTableRowAdapter) {
			IAudioItemTableRowAdapter adapter = (IAudioItemTableRowAdapter) element;
			AudioItem item = adapter.getAudioItem();
			Category cat = adapter.getCategory();
			Metadata metadata = item.getLocalizedAudioItem(Locale.GERMAN).getMetadata();

			switch (columnIndex) {
			case 0:
				return metadata.getMetadataValues(MetadataSpecification.DC_TITLE).get(0).getValue();
			case 1:
				return cat.getCategoryName(Locale.GERMAN).getLabel();
			case 2:
				return metadata.getMetadataValues(MetadataSpecification.DC_CREATOR).get(0).getValue();
			case 3:
				return metadata.getMetadataValues(MetadataSpecification.DC_LANGUAGE).get(0).getValue();
			default:
				return "<error>";
		
			}
		}	
		return "<error>";
	}
}
