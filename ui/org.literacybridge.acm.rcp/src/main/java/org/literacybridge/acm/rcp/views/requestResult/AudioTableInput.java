package org.literacybridge.acm.rcp.views.requestResult;

import java.util.List;

import org.literacybridge.acm.rcp.views.adapters.ICategoryResultContainer;

public class AudioTableInput {
	
	private List<ICategoryResultContainer> container = null;
	
	public AudioTableInput(List<ICategoryResultContainer> container) {
		this.container = container;
	}

	public List<ICategoryResultContainer> getContainer() {
		return container;
	}
}
