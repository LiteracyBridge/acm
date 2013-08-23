package org.literacybridge.acm.gui.messages;

public class SearchRequestMessage {
	private final String searchString;
	
	public SearchRequestMessage(String searchString) {
		this.searchString = searchString;
	}
	
	public String getSearchString() {
		return searchString;
	}
}
