package org.literacybridge.acm.core;

import org.literacybridge.acm.api.IDataRequestResult;
import org.literacybridge.acm.api.IDataRequestService;

public class DataRequestService implements IDataRequestService {
	private static final IDataRequestService instance = new DataRequestService();
	
	private DataRequestService() {
		// singleton
	}
	
	public static IDataRequestService getInstance() {
		return instance;
	}
	
	/* (non-Javadoc)
	 * @see main.java.org.literacybridge.acm.api.IDataRequestService#getData()
	 */
	public IDataRequestResult getData() {
		return getData(null);
	}

	/* (non-Javadoc)
	 * @see main.java.org.literacybridge.acm.api.IDataRequestService#getData(java.lang.String)
	 */
	public IDataRequestResult getData(String filterString) {
		// FIXME: fetch data from DB
		return null;
	}
}
