package zeno2.db;

import zeno2.kernel.Principal;
import zeno2.kernel.ZenoException;

public class PrincipalChecker implements PermissionChecker {
	DBClient dbclient;
	MonitorImpl monitor;
	String username;

	public PrincipalChecker(MonitorImpl monitor, String username) {
		this.monitor = monitor;
		this.dbclient = monitor.getDBClient();
		this.username = username;
	}

	public boolean isRegistered() {
		return (!username.equals("guest") && !username.equals("any"));
	}

	public boolean isZenoAdmin() {
		return monitor.isZenoAdmin(username);
	}

	public boolean isSelf(Object obj) {
		String principal = ((Principal) obj).getId();
		return username.equals(principal);
	}

	public boolean isAdmin(Object obj) {
		String principal = ((Principal) obj).getId();
		String xusername = "%" + username + "%";
		StringBuffer buf = new StringBuffer();
		//buf.append("select count(*) from principal_property");
		buf.append(" where principal=");
		buf.append(DBClient.format(principal));
		buf.append(" and name='zeno2.admin'");
		buf.append(" and value like ");
		buf.append(DBClient.format(xusername));
		int nr = dbclient.count("principal_property", buf.toString());
		return (nr > 0);
	}

	public boolean hasRole(String role, Object obj) throws ZenoException {
		return false;
	}

	public void checkPermission(String operation, Object obj)
		throws ZenoException {
		boolean result = false;
		if (operation.startsWith("PrincipalFactory"))
			result = checkPrincipalFactoryPermissions(operation.substring(17), obj);
		else
			if (operation.startsWith("Principal"))
				result = checkPrincipalPermissions(operation.substring(10), obj);
			else
				if (operation.startsWith("Group"))
					result = checkGroupPermissions(operation.substring(6), obj);
				else
					monitor.reportError("PrincipalChecker.checkPermission unknown " + operation);

		if (!result) {
			throw new zeno2.kernel.NoPermissionException(
				"NoPermission " + operation + " " + obj);
		}

	}

	public boolean checkPrincipalPermissions(String op, Object obj)
		throws ZenoException {
		if (op.startsWith("getEmail"))
			return true;
		else
			if (op.startsWith("getName"))
				return true;
			else
				if (op.startsWith("get"))
					return isRegistered();
				else
					if (op.equals("setPassword"))
						return (isZenoAdmin() | isSelf(obj));
					else
						if (op.startsWith("set"))
							return (isAdmin(obj) | isSelf(obj));
						else
							if (op.equals("removeProperty"))
								return (isAdmin(obj) | isSelf(obj));
							else
								return false;
	}

	public boolean checkGroupPermissions(String op, Object obj)
		throws ZenoException {
		if (op.equals("getMembers"))
			return isRegistered();
		else
			if (op.equals("addMember"))
				return isAdmin(obj);
			else
				if (op.equals("removeMember"))
					return isAdmin(obj);
				else
					return false;
	}

	public boolean checkPrincipalFactoryPermissions(String op, Object obj)
		throws ZenoException {
		if (op.equals("createUser"))
			return isZenoAdmin();
		else
			if (op.equals("createGroup"))
				return isZenoAdmin();
			else
				if (op.equals("removePrincipal"))
					return isZenoAdmin();
				else
					if (op.equals("searchPrincipals"))
						return isRegistered();
					else
						return false;
	}

	/* operations
	
	checkPermission("Principal.getName", this);
	checkPermission("Principal.setName", this);
	checkPermission("Principal.getEmail", this);
	checkPermission("Principal.setEmail", this);
	checkPermission("Principal.getOrganization", this);
	checkPermission("Principal.setOrganization", this);
	checkPermission("Principal.getOrgRole", this);
	checkPermission("Principal.setOrgRole", this);
	checkPermission("Principal.getDecription", this);
	checkPermission("Principal.setDescription", this);	
	
	checkPermission("Principal.getProperty", this);
	checkPermission("Principal.setProperty", this);
	checkPermission("Principal.removeProperty", this);
	
	checkPermission("Principal.setPassword", this);
	
	checkPermission("Principal.getGroups", this);
	
	checkPermission("Group.getMembers", this); 
	checkPermission("Group.addMember", this);
	checkPermission("Group.removeMember", this);
	
	checkPermission("PrincipalFactory.createUser", this);
	checkPermission("PrincipalFactory.createGroup", this);
	checkPermission("PrincipalFactory.removePrincipal", this);
	checkPermission("PrincipalFactory.searchPrincipals", this);
	
	*/

}