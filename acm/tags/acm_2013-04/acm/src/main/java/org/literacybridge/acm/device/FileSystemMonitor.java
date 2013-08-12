package org.literacybridge.acm.device;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.literacybridge.acm.core.MessageBus;
import org.literacybridge.acm.utils.OSChecker;

public class FileSystemMonitor extends Thread {
	private static final int DEFAULT_SLEEP_TIME = 3000; // 3 secs
	
	private Set<DeviceInfo> connectedDevices;
	private Set<DeviceRecognizer> deviceRecognizers;
	
	public FileSystemMonitor() {
		this.connectedDevices = new HashSet<DeviceInfo>();
		this.deviceRecognizers = new HashSet<DeviceRecognizer>();
		setDaemon(true); // don't prevent JVM from exiting 
	}

	public void addDeviceRecognizer(DeviceRecognizer recognizer) {
		this.deviceRecognizers.add(recognizer);
	}
	
	private DeviceInfo checkForDevice(File pathToDevice) {
		for (DeviceRecognizer r : this.deviceRecognizers) {
			DeviceInfo info = r.identifyDevice(pathToDevice);
			if (info != null) {
				return info;
			}
		}
		
		return null;
	}
	
	public void run() {
		while (true) {
			File[] roots;
			if (OSChecker.WINDOWS) {
				roots = File.listRoots();
			} else if (OSChecker.MAC_OS) {
				roots = new File("/Volumes").listFiles();
			} else {
				// TODO: support linux
				roots = null;
			}
			Set<DeviceInfo> currentDevices = new HashSet<DeviceInfo>();
			for (File root : roots) {
				// check if this is a supported device
				if (root.isDirectory()) {
					DeviceInfo device = checkForDevice(root);
					if (device != null) {
						currentDevices.add(device);
					}
				}
			}
			
			int newDevices = 0;
			for (DeviceInfo device : currentDevices) {
				if (!this.connectedDevices.contains(device)) {
					MessageBus.getInstance().sendMessage(new DeviceConnectEvent.ConnectEvent(device));
					newDevices++;
				}
			}

			if (this.connectedDevices.size() != currentDevices.size() - newDevices) {
				// we need to determine which devices were disconnected
				for (DeviceInfo device : this.connectedDevices) {
					if (!currentDevices.contains(device)) {
						MessageBus.getInstance().sendMessage(new DeviceConnectEvent.DisconnectEvent(device));
					}
				}
			}
			
			this.connectedDevices = currentDevices;
			
			try {
				sleep(DEFAULT_SLEEP_TIME);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
	}
}
