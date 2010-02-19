/**
 * 
 */
package org.literacybridge.acm.device;

import java.util.Set;

import org.literacybridge.acm.core.MessageBus.Message;

public class DeviceConnectEvent extends Message {
	private Set<DeviceInfo> connectedDevices;
	
	public DeviceConnectEvent(Set<DeviceInfo> connectedDevices) {
		this.connectedDevices = connectedDevices;
	}
	
	public Set<DeviceInfo> getConnectedDevices() {
		return this.connectedDevices;
	}
}