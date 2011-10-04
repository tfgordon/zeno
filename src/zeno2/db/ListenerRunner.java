package zeno2.db;

import java.util.Vector;

import zeno2.kernel.CreationEvent;
import zeno2.kernel.CreationEventListener;
import zeno2.kernel.ModificationEvent;
import zeno2.kernel.ModificationEventListener;
import zeno2.kernel.ReadEvent;
import zeno2.kernel.ReadEventListener;
import zeno2.kernel.ZenoEvent;
import zeno2.kernel.ZenoEventListener;

public class ListenerRunner extends Thread {
	static int cnr = 0;
	int nr;
	Vector events;
	ZenoEventListener listener;

	public ListenerRunner(ZenoEventListener listener) {
		super();
		this.listener = listener;
		this.events = new Vector();
		this.nr = cnr++;
	}

	public void run() {
		while (events.size() > 0) {
			ZenoEvent event = (ZenoEvent) events.remove(0);
			listener.handleEvent(event);
		}
	}

	public void putEvent(ZenoEvent event) {

		if (listener instanceof CreationEventListener) {
			if (event instanceof CreationEvent)
				events.add(event);

		} else
			if (listener instanceof ModificationEventListener) {
				if (event instanceof ModificationEvent)
					events.add(event);

			} else
				if (listener instanceof ReadEventListener) {
					if (event instanceof ReadEvent)
						events.add(event);
				} else {
					events.add(event);
				}

		if (!isAlive()) {
			start();
			run();
		}
	}

	/*
	public void putEvent(ZenoEvent event) {
		if (event instanceof CreationEvent &&
				listener instanceof CreationEventListener) {
			events.add(event);
				
		}else if (event instanceof ModificationEvent &&
				listener instanceof ModificationEventListener) {
			events.add(event);
					
		}else if (event instanceof ReadEvent &&
				listener instanceof ReadEventListener) {
			
		} else {
			//events.add(event);
		}
		if (!isAlive()) {
			System.out.println(nr + "  has events " + events.size());
			start();
			run();
		}
		
	}
	
	
	
	public void putEvent(ZenoEvent event) {
		events.add(event);
	//System.out.println(nr + " has events:  " + events.size());
	//System.out.println(nr + " is alive  " + isAlive());
		if (!isAlive()) {
			//System.out.println(nr + " start called");
			start();
			run();
		}
	}
	*/

	public boolean isRunnerFor(ZenoEventListener listener) {
		return (this.listener == listener);
	}

}