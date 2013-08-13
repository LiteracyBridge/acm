package org.literacybridge.acm.importexport;

import org.literacybridge.acm.device.DeviceInfo;

public class DeviceSynchronizer {
	private static final DeviceSynchronizer instance = new DeviceSynchronizer();
	
	private DeviceSynchronizer(){
		// singleton
	}
	
	public static DeviceSynchronizer getInstance() {
		return instance;
	}
	
	public synchronized void sync(DeviceInfo info) {
		
	}
	
	
}
