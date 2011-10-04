package zeno2.kernel;

import java.util.Date;

/** Zeno2 Modification Event.  Represents the event of modifying a resource. */

public class ModificationEvent extends ZenoEvent {

	public ModificationEvent(Object source, String principalId, Date date) {
		super(source, principalId, date);
	}

}