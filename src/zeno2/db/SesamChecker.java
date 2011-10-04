package zeno2.db;

import zeno2.kernel.ZenoException;

public class SesamChecker implements PermissionChecker {

	public SesamChecker() {
	}

	public void checkPermission(String operation, Object obj)
		throws ZenoException {
		return;
	}

	public void checkPermission(String operation, Object source, Object target)
		throws ZenoException {
		return;
	}

	public boolean hasRole(String role, Object object) throws ZenoException {
		if (role.equals("guest"))
			return false;
		else
			return true;
	}

}