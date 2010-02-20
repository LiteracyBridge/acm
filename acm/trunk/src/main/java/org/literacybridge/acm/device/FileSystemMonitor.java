package org.literacybridge.acm.device;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.literacybridge.acm.core.MessageBus;
import org.literacybridge.acm.utils.OSChecker;

public class FileSystemMonitor extends Thread {
	private static final int DEFAULT_SLEEP_TIME = 5000; // 5 secs
	
	private Set<DeviceInfo> connectedDevices;
	private Set<DeviceRecognizer> deviceRecognizers;
	
	public FileSystemMonitor() {
		this.connectedDevices = new HashSet<DeviceInfo>();
		this.deviceRecognizers = new HashSet<DeviceRecognizer>();
		setDaemon(true); // don't prevent JVM from exiting 
		start();
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
				DeviceInfo device = checkForDevice(root);
				if (device != null) {
					currentDevices.add(device);
				}
			}
			
			boolean sendMessage = false;
			for (DeviceInfo device : currentDevices) {
				if (this.connectedDevices.contains(device)) {
					sendMessage = true;
					break;
				}
			}
			
			if (sendMessage || // new device connected? 
					this.connectedDevices.size() != currentDevices.size()) // or device disconnected? 
				{
				// send message, because list of active devices changed
				MessageBus.getInstance().sendMessage(new DeviceConnectEvent(Collections.unmodifiableSet(currentDevices)));
				this.connectedDevices = currentDevices;
			}
			
			try {
				sleep(DEFAULT_SLEEP_TIME);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
	}
}
