package zeno2.kernel;

import java.util.Date;

/** Zeno2 ReadEvent.  Represents the event of reading a resource. */

public class ReadEvent extends ZenoEvent {

	public ReadEvent(Object source, String principalId, Date date) {
		super(source, principalId, date);
	}

}