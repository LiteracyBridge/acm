package org.literacybridge.acm.rcp.views.category;

import java.util.List;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.literacybridge.acm.api.IDataRequestResult;
import org.literacybridge.acm.categories.Taxonomy.Category;
import org.literacybridge.acm.rcp.views.adapters.ICategoryResultContainer;
import org.literacybridge.acm.rcp.views.adapters.CategoryResultContainer;

public class CategoryTreeContentProvider implements ITreeContentProvider {
	
	private IDataRequestResult result 	= null;
	private Category rootCategory 		= null;
	
	@Override
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof CategoryResultContainer) {
			return getTreeNodesFromCategories(((ICategoryResultContainer) parentElement).getCategory().getChildren());
		}
		
		return null;
	}

	@Override
	public Object getParent(Object element) {
		if (element instanceof CategoryResultContainer) {
			return ((ICategoryResultContainer) element).getCategory().getParent();
		}
		
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		if (element instanceof CategoryResultContainer) {
			return ((ICategoryResultContainer) element).getCategory().hasChildren();
		}
		
		return false;
	}

	@Override
	public Object[] getElements(Object inputElement) {
		if (rootCategory != null) {
			return getTreeNodesFromCategories(rootCategory.getChildren());
		}
		
		return null;
	}

	private ICategoryResultContainer[] getTreeNodesFromCategories(List<Category> categories) {
		if (categories != null && categories.size() > 0) {
			int numItem = categories.size();
			ICategoryResultContainer[] array = new ICategoryResultContainer[numItem];
			int i=0;
			for (Category c : categories) {
				array[i++] = new CategoryResultContainer(c, result);
			}
			return array;	
		}
		
		return null;
	}
	
	@Override
	public void dispose() {
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		if (newInput instanceof IDataRequestResult) {
			result = (IDataRequestResult) newInput;
			rootCategory = result.getRootCategory();
		}
	}
}
