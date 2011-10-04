package zeno2.kernel;

/** Zeno2 Exceptions
	- AttachmentExists
	- PrincipalExists
*/

public class NameInUseException extends ZenoException {

	public NameInUseException(String msg) {
		super(msg);
	}

}