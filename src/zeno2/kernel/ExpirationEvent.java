package zeno2.kernel;

import java.util.Date;

/** Zeno2 Expiration Event. */

public class ExpirationEvent extends ZenoEvent {

	public ExpirationEvent(Object source, String principalId, Date date) {
		super(source, principalId, date);
	}

}