package util;

import java.util.Observable;

public class SimpleMessageService extends Observable {

	public void pumpMessage(Object message) {
		setChanged();
		notifyObservers(message);
	}
}
