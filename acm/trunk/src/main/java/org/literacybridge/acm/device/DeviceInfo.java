package org.literacybridge.acm.device;

import java.io.File;

public class DeviceInfo {
	private File pathToDevice;
	private String deviceUID;
	
	public DeviceInfo(String uid, File path) {
		this.deviceUID = uid;
		this.pathToDevice = path;
	}
	
	public File getPathToDevice() {
		return this.pathToDevice;
	}
	
	public String getDeviceUID() {
		return this.deviceUID;
	}
}
