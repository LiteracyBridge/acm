package org.literacybridge.acm.rcp.views.requestResult;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.literacybridge.acm.api.IDataRequestResult;
import org.literacybridge.acm.content.AudioItem;
import org.literacybridge.acm.demo.DemoRepository;
import org.literacybridge.acm.rcp.views.requestResult.helpers.AudioItemTableRowAdapter;
import org.literacybridge.acm.rcp.views.requestResult.helpers.IAudioItemTableRowAdapter;
import org.literacybridge.acm.rcp.views.requestResult.helpers.ILocalizedAudioItemTableRowAdapter;
import org.literacybridge.acm.rcp.views.requestResult.helpers.LocalizedAudioItemRowAdapter;

public class AudioItemTableContentProvider implements ITreeContentProvider {

	private List<AudioItem> audioItemList = null;
	
	@Override
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof IAudioItemTableRowAdapter) {
			IAudioItemTableRowAdapter adapter = (IAudioItemTableRowAdapter) parentElement;
			return packageLocalizedAudioItems(adapter.getAudioItem()).toArray();
		}
		
		return null;
	}

	@Override
	public Object getParent(Object element) {
		if (element instanceof ILocalizedAudioItemTableRowAdapter) {
			ILocalizedAudioItemTableRowAdapter adapter = (ILocalizedAudioItemTableRowAdapter) element;
			adapter.getParent();
		}
		
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		if (element instanceof IAudioItemTableRowAdapter) {
			IAudioItemTableRowAdapter adapter = (IAudioItemTableRowAdapter) element;
			return (adapter.getAudioItem().getAvailableLocalizations().size() > 0);
		}
		
		return false;
	}

	@Override
	public Object[] getElements(Object inputElement) {
		return packageAudioItems().toArray();
	}

	@Override
	public void dispose() {
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {	
		// Same demo data
		DemoRepository demo = new DemoRepository();
		IDataRequestResult result = demo.getDataRequestResult();
		audioItemList = result.getAudioItems();		
	}
	
	private Set<IAudioItemTableRowAdapter> packageAudioItems() {
		HashSet<IAudioItemTableRowAdapter> set = new HashSet<IAudioItemTableRowAdapter>();
		for (AudioItem item : audioItemList) {
			IAudioItemTableRowAdapter adapter = new AudioItemTableRowAdapter(item);
			set.add(adapter);
		}
		
		return set;
	}
	
	private Set<ILocalizedAudioItemTableRowAdapter> packageLocalizedAudioItems(AudioItem owner) {
		Set<ILocalizedAudioItemTableRowAdapter> set = new HashSet<ILocalizedAudioItemTableRowAdapter>();
		for (Locale locale : owner.getAvailableLocalizations()) {
			ILocalizedAudioItemTableRowAdapter adapter = new LocalizedAudioItemRowAdapter(owner.getLocalizedAudioItem(locale), owner);
			set.add(adapter);
		}	
		 
		 return set;
	}

}
