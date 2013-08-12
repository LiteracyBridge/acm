package org.literacybridge.acm.gui.messages;

public abstract class RequestAudioItemMessage extends Message {

	public enum RequestType {
		Current,
		Previews,
		Next
	};
	
	private RequestType type;

	public RequestAudioItemMessage(RequestType type) {
		super();
		this.type = type;
	}

	public RequestType getRequestType() {
		return type;
	}
}
