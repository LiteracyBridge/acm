package org.literacybridge.acm.rcp.editors.audioItem;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.literacybridge.acm.content.AudioItem;

public class AudioItemContentProvider implements ITreeContentProvider {

	private AudioItem audioItem = null;
	
	@Override
	public Object[] getChildren(Object parentElement) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object getParent(Object element) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Object[] getElements(Object inputElement) {
		if (audioItem != null) {
			
		}
		
		return null;
	}

	@Override
	public void dispose() {
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {	
		if (newInput instanceof AudioItem) {
			AudioItem newAudioItem = (AudioItem) newInput;
			audioItem = newAudioItem;			
		}
	}
}
