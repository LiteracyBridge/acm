/**
 * 
 */
package org.literacybridge.acm.device;

import org.literacybridge.acm.core.MessageBus.Message;

public abstract class DeviceConnectEvent extends Message {
	protected DeviceInfo device;
	
	public final static class ConnectEvent extends DeviceConnectEvent {
		public ConnectEvent(DeviceInfo device) {
			super(device);
		}
	}

	public final static class DisconnectEvent extends DeviceConnectEvent {
		public DisconnectEvent(DeviceInfo device) {
			super(device);
		}
	}
	
	public DeviceConnectEvent(DeviceInfo device) {
		this.device = device;
	}
	
	public DeviceInfo getDeviceInfo() {
		return this.device;
	}
	
	public String toString() {
		return this.getClass().getSimpleName() + ": " + device.getPathToDevice();
	}
}