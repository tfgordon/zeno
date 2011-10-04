package zeno2.kernel;

/** Zeno2 Exceptions
	- NoSuchResource
    - NoSuchParent
    - NoSuchAttachment
    - NoSuchPrincipal
	- NoSuchFile 
*/

public class NotFoundException extends ZenoException {

	public NotFoundException(String msg) {
		super(msg);
	}

}