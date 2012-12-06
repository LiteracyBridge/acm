package org.literacybridge.acm.gui;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import javax.swing.SwingUtilities;

import org.literacybridge.acm.api.IDataRequestResult;
import org.literacybridge.acm.core.DataRequestService;
import org.literacybridge.acm.db.PersistentCategory;
import org.literacybridge.acm.db.PersistentLocale;
import org.literacybridge.acm.db.PersistentTag;
import org.literacybridge.acm.gui.util.language.LanguageUtil;

public class FilterState {
	private String previousFilterState = null;
	
	private String filterString;
	private List<PersistentCategory> filterCategories;
	private List<PersistentLocale> filterLanguages;
	private PersistentTag selectedTag;
	
	public synchronized String getFilterString() {
		return filterString;
	}
	public synchronized void setFilterString(String filterString) {
		this.filterString = filterString;
		updateResult();
	}
	public synchronized List<PersistentCategory> getFilterCategories() {
		return filterCategories;
	}
	public synchronized void setFilterCategories(
			List<PersistentCategory> filterCategories) {
		this.filterCategories = filterCategories;
		updateResult();
	}
	
	public synchronized List<PersistentLocale> getFilterLanguages() {
		return filterLanguages;
	}
	public synchronized void setFilterLanguages(List<PersistentLocale> filterLanguages) {
		this.filterLanguages = filterLanguages;
		updateResult();
	}
	
	public synchronized void setSelectedTag(PersistentTag selectedTag) {
		this.selectedTag = selectedTag;
		updateResult();
	}
	
	public synchronized PersistentTag getSelectedTag() {
		return selectedTag;
	}
	
	public void updateResult() {
		updateResult(false);
	}
	
	public void updateResult(boolean force) {
		if (!force && previousFilterState != null && previousFilterState.equals(this.toString())) {
			return;
		}
		
		previousFilterState = this.toString();
		
		final IDataRequestResult result;
		
		if (selectedTag == null) {
			result = DataRequestService.getInstance().getData(
					LanguageUtil.getUserChoosenLanguage(), 
					filterString, filterCategories, filterLanguages);
		} else {
			result = DataRequestService.getInstance().getData(
					LanguageUtil.getUserChoosenLanguage(), 
					filterString, selectedTag);				
		}

		// call UI back
		Runnable updateUI = new Runnable() {
			@Override
			public void run() {
				Application.getMessageService().pumpMessage(result);
			}
		};
		
		if (SwingUtilities.isEventDispatchThread()) {
			updateUI.run();
		} else {
			try {
				SwingUtilities.invokeAndWait(updateUI);
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
		}			
	}
	
	@Override public String toString() {
		StringBuilder builder = new StringBuilder();
		if (filterString != null) {
			builder.append("FS:").append(filterString);
			builder.append(",");
		}
		if (filterCategories != null && !filterCategories.isEmpty()) {
			for (PersistentCategory cat : filterCategories) {
				builder.append("FC:").append(cat.getUuid());
				builder.append(",");					
			}
		}
		if (filterLanguages != null && !filterLanguages.isEmpty()) {
			for (PersistentLocale lang : filterLanguages) {
				builder.append("FL:").append(lang.getLanguage()).append("-").append(lang.getCountry());
				builder.append(",");					
			}
		}
		if (selectedTag != null) {
			builder.append("ST:").append(selectedTag.getName());
			builder.append(",");
		}			
		return builder.toString();
	}
}
