package org.literacybridge.acm.rcp.editors.audioItem;

import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Image;

public class AudioItemLabelProvider implements ITableLabelProvider {

	@Override
	public Image getColumnImage(Object element, int columnIndex) {
		return null;
	}

	@Override
	public String getColumnText(Object element, int columnIndex) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void dispose() {		
	}

	@Override
	public boolean isLabelProperty(Object element, String property) {	
		return false;
	}

	@Override
	public void removeListener(ILabelProviderListener listener) {
	}

	@Override
	public void addListener(ILabelProviderListener listener) {		
	}	
}
