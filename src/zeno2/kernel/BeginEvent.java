package zeno2.kernel;

import java.util.Date;

/** Zeno2 BeginEvent. */

public class BeginEvent extends ZenoEvent {

	public BeginEvent(Object source, String principalId, Date date) {
		super(source, principalId, date);
	}
}