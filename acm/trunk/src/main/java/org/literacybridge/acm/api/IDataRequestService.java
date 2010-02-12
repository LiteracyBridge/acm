package org.literacybridge.acm.api;

public interface IDataRequestService {

	public abstract IDataRequestResult getData();

	public abstract IDataRequestResult getData(String filterString);

}