package net.haspamelodica.am2900me.swtui;

import java.util.ArrayList;
import java.util.List;

public class ListenerManager {
	private final List<Runnable> listeners;

	public ListenerManager() {
		this.listeners = new ArrayList<>();
	}

	public void addListener(Runnable listener) {
		listeners.add(listener);
	}

	public void removeListener(Runnable listener) {
		listeners.remove(listener);
	}

	public void callAllListeners() {
		for (Runnable l : new ArrayList<>(listeners))// avoid ConcurrentModificationException
			try {
				l.run();
			} catch (Exception e) {
				e.printStackTrace();
			}
	}
}