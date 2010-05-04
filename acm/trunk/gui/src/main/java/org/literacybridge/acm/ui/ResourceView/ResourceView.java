package org.literacybridge.acm.ui.ResourceView;

import java.awt.BorderLayout;
import java.awt.Container;

import javax.swing.JSplitPane;

import org.literacybridge.acm.api.IDataRequestResult;
import org.literacybridge.acm.api.IDataRequestService;
import org.literacybridge.acm.core.DataRequestService;
import org.literacybridge.acm.db.Persistence;
import org.literacybridge.acm.ui.Application;
import org.literacybridge.acm.ui.ResourceView.audioItems.AudioItemView;
import org.literacybridge.acm.util.LanguageUtil;

public class ResourceView extends Container {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1464102221036629153L;

	public ResourceView() {
		createViewComponents();
	}
	
	private void createViewComponents() {
		setLayout(new BorderLayout());
		
		// init database
		try {
			Persistence.initialize();
		} catch (Exception e) {
			e.printStackTrace();
		}
		IDataRequestService dataService = DataRequestService.getInstance();
		IDataRequestResult result = dataService.getData(LanguageUtil.getUserChoosenLanguage());

		// Table with audio items
		AudioItemView audioItemView = new AudioItemView(result);
		
		//  Tree with categories 
		//  Create at the end, because this is the main selection provider
		CategoryView categoryView = new CategoryView(result);
		
		JSplitPane sp = new JSplitPane();
		// left-side
		sp.setLeftComponent(categoryView);
		// right-side
		sp.setRightComponent(audioItemView);
		
		sp.setOneTouchExpandable(true);
	    sp.setContinuousLayout(true);
	    sp.setDividerLocation(300);
		
	     add(BorderLayout.CENTER, sp);
	     
			Application.getMessageService().pumpMessage(result);
	}	
}
