package org.literacybridge.acm.tools;

import org.literacybridge.acm.core.MessageBus;
import org.literacybridge.acm.core.MessageBus.Message;

public class MessageQueueDumper {
	public static void main(String[] args) throws Exception {
		MessageBus bus = MessageBus.getInstance();
		MessageQueueDumper d = new MessageQueueDumper(bus);
		synchronized(d) {
			d.wait();
		}
	}
	
	public MessageQueueDumper(final MessageBus bus) throws Exception {
		bus.addListener(Message.class, new MessageBus.MessageListener() {
			
			@Override
			public void receiveMessage(Message message) {
				System.out.println(message.getClass());
			}
		});
	}
	
	
}
