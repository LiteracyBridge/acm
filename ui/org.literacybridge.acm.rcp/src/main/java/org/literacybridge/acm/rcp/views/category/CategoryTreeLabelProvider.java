package org.literacybridge.acm.rcp.views.category;

import java.util.Locale;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;
import org.literacybridge.acm.categories.Taxonomy.Category;
import org.literacybridge.acm.rcp.views.adapters.CategoryResultContainer;
import org.literacybridge.acm.rcp.views.adapters.ICategoryResultContainer;

public class CategoryTreeLabelProvider implements ILabelProvider {

	@Override
	public Image getImage(Object element) {
		return null;
	}

	@Override
	public String getText(Object element) {
		if (element instanceof CategoryResultContainer) {
			
			ICategoryResultContainer tna = (ICategoryResultContainer) element;
			Category c = tna.getCategory();
			return c.getCategoryName(Locale.GERMAN) 
						+ "(" 
						+ (tna.GetDataRequestResult().getFacetCount(c)) 
						+ ")";
		}
		
		return "<error>";
	}
	
	
	@Override
	public void addListener(ILabelProviderListener listener) {
		// TODO Auto-generated method stub

	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isLabelProperty(Object element, String property) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void removeListener(ILabelProviderListener listener) {
		// TODO Auto-generated method stub

	}

}
