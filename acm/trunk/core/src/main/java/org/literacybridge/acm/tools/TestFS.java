package org.literacybridge.acm.tools;

import org.literacybridge.acm.core.MessageBus;
import org.literacybridge.acm.core.MessageBus.Message;
import org.literacybridge.acm.device.DeviceConnectEvent;
import org.literacybridge.acm.device.FileSystemMonitor;
import org.literacybridge.acm.device.LiteracyBridgeTalkingBookRecognizer;

public class TestFS {
	public static void main(String[] args) throws Exception {
		FileSystemMonitor monitor = new FileSystemMonitor();
		monitor.addDeviceRecognizer(new LiteracyBridgeTalkingBookRecognizer());
		
		monitor.start();
		
		MessageBus bus = MessageBus.getInstance();
		bus.addListener(DeviceConnectEvent.class, new MessageBus.MessageListener() {
			
			@Override
			public void receiveMessage(Message message) {
				System.out.println(message);
			}
		});
		
		Object o = new Object();
		synchronized(o) {
			o.wait();
		}
	}
}	
