package org.literacybridge.acm.device;

import java.io.File;
import java.io.IOException;

public class DeviceInfo {
	private File pathToDevice;
	private String deviceUID;
	private DeviceContents deviceContents;
	
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
	
	public DeviceContents getDeviceContents() throws IOException {
		if (this.deviceContents == null) {
			this.deviceContents = new DeviceContents(pathToDevice);
		}
		return this.deviceContents;
	}
	
	@Override
	public String toString() {
		return this.pathToDevice.toString();
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
