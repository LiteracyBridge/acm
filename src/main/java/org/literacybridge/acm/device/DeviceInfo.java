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
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof DeviceInfo)) {
			return false;
		}
		
		DeviceInfo other = (DeviceInfo) o;
		
		return other.deviceUID.equals(this.deviceUID);
	}
	
	@Override
	public int hashCode() {
		return this.deviceUID.hashCode();
	}
}
