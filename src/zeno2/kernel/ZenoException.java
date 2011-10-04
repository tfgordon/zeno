package zeno2.kernel;

/** Zeno2 Exceptions.  Error messages include:
    - DataBaseException
    - NotFoundException
    - NoSuchResource
   	- NoSuchParent
   	- NoSuchAttachment
   	- NoSuchPrincipal
   	- NoSuchFile
    - NoSuchResourceClass
    - NoPermission
    - NameInUse
*/

public class ZenoException extends Exception {

	public ZenoException(String message) {
		super(message);
	}
}