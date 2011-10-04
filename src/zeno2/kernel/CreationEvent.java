package zeno2.kernel;

import java.util.Date;

/** Zeno2 Creation Event.  Represents the event of creating a resource. */

public class CreationEvent extends ZenoEvent {

	public CreationEvent(Object source, String principalId, Date date) {
		super(source, principalId, date);
	}

}