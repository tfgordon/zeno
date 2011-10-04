package zeno2.kernel;

import java.util.Date;
import java.util.EventObject;

/** Zeno2 Events. Used for notification, event propogation and sorting resources
chronologically. */

public class ZenoEvent extends java.util.EventObject {
	Date date;
	String principalId;

	ZenoEvent(Object source) {
		super(source);
	}

	ZenoEvent(Object source, String principalId, Date date) {
		super(source);
		this.date = date;
		this.principalId = principalId;
	}

	/** Returns the date of the event. */

	public Date getDate() {
		return date;
	}

	/** Returns the id of the principal who caused the event. */

	public String getPrincipalId() {
		return principalId;
	}

}