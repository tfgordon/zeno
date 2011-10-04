package zeno2.db;

import zeno2.kernel.ZenoException;

public interface PermissionChecker {

	public void checkPermission(String operation, Object obj) throws ZenoException;

	public boolean hasRole(String role, Object obj) throws ZenoException;

}