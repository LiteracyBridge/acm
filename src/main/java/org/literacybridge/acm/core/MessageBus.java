package org.literacybridge.acm.core;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MessageBus {
	public abstract static class Message {}
	
	public abstract static class MessageListener {
		public abstract void receiveMessage(Message message);
	}
	
	private static MessageBus singleton;
	
	public static MessageBus getInstance() {
		if (singleton == null) {
			singleton = new MessageBus();
		}
		return singleton;
	}
	
	private MessageBus() {
		// singleton
		this.listeners = new HashMap<Class<? extends Message>, List<MessageListener>>();
		this.messageSuperClassChainCache = new HashMap<Class<? extends Message>, Set<Class<? extends Message>>>();
	}
	
	private Map<Class<? extends Message>, List<MessageListener>> listeners;
	private Map<Class<? extends Message>, Set<Class<? extends Message>>> messageSuperClassChainCache;
	
	public void addListener(Class<? extends Message> messageType, MessageListener listener) {
		List<MessageListener> l = this.listeners.get(messageType);
		if (l == null) {
			l = new LinkedList<MessageListener>();
			this.listeners.put(messageType, l);
		}
		l.add(listener);
	}
	
	public void sendMessage(Message message) {
		Set<Class<? extends Message>> listOfUniqueClasses = this.messageSuperClassChainCache.get(message.getClass());
		
		if (listOfUniqueClasses == null) {
			listOfUniqueClasses = new HashSet<Class<? extends Message>>();

			Class<?> clazz = message.getClass();
			do {
				listOfUniqueClasses.add(clazz.asSubclass(Message.class));
				clazz = clazz.getSuperclass();
			} while (Message.class.isAssignableFrom(clazz));
			
			this.messageSuperClassChainCache.put(message.getClass(), listOfUniqueClasses);
		}
		
		// now send message to all listeners
		for (Class <? extends Message> m : listOfUniqueClasses) {
			List<MessageListener> list = this.listeners.get(m);
			for (MessageListener listener : list) {
				listener.receiveMessage(message);
			}
		}
	}
}
