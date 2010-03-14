package org.literacybridge.acm.rcp.views.requestResult;

import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.literacybridge.acm.content.AudioItem;
import org.literacybridge.acm.rcp.views.adapters.ICategoryResultContainer;
import org.literacybridge.acm.rcp.views.requestResult.helpers.AudioItemTableRowAdapter;
import org.literacybridge.acm.rcp.views.requestResult.helpers.IAudioItemTableRowAdapter;

public class AudioItemTableContentProvider implements IStructuredContentProvider {

	private List<ICategoryResultContainer> crcList 	= null;
	
	@Override
	public Object[] getElements(Object inputElement) {		
		return getFilterAudioItemsByCategory();
	}

	@Override
	public void dispose() {
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		if (newInput instanceof AudioTableInput) {
			crcList = ((AudioTableInput) newInput).getContainer();
		}
	}
	
	public Object[] getFilterAudioItemsByCategory() {
		Vector<IAudioItemTableRowAdapter> vector = new Vector<IAudioItemTableRowAdapter>();

		// bad performance, but we want the audio items sorted after the sequence of the passed categories. Must be optimized!
		for (Iterator<ICategoryResultContainer> iter = crcList.iterator(); iter.hasNext();) {
			ICategoryResultContainer crc = (iter.next());
			for(AudioItem item : crc.GetDataRequestResult().getAudioItems()) {
				if (item.getCategories().contains(crc.getCategory())) {
					vector.add(new AudioItemTableRowAdapter(item, crc.getCategory()));
				}
			}
		}
		
		return vector.toArray();
	}
}
