package zeno2.db;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import java.sql.ResultSet;
import java.sql.SQLException;

import zeno2.kernel.Principal;
import zeno2.kernel.Group;
import zeno2.kernel.ZenoException;
import zeno2.util.CriterionParser;

public class XPrincipalChecker implements PermissionChecker {
	DBClient dbclient;
	MonitorImpl monitor;
	String username;
	String type;
	Hashtable clientInfo;
	//String ipaddr;
	//to avoid repeated checks
	Object lastMate;

	public XPrincipalChecker(MonitorImpl monitor, String username, String clientInfo) {
		this.monitor = monitor;
		this.dbclient = monitor.getDBClient();
		this.username = username;
		this.type = getType(username);
		this.clientInfo = CriterionParser.parseValues(clientInfo);
	}
	
	protected String getType(String name) {
		String result = "";
		StringBuffer buf = new StringBuffer();
		buf.append("select class from principal where id=");
		buf.append(DBClient.format(name));
		try {	
			ResultSet rs = dbclient.executeQuery(buf.toString());
			if (rs.next()) {
				result = rs.getString("class");
			} 
		} catch (java.sql.SQLException e) {
			monitor.reportError("DBPrincipalFactory.getTypes "+ e);
		}
		return result;
	}
	
	
	public boolean isZenoAdmin() {
		return monitor.isZenoAdmin(username);
	}
	
	public boolean isSystemAdmin() throws ZenoException {
		return isMember("system", true) || isZenoAdmin();
	}
	
	public boolean isAdmin(Object obj) throws ZenoException {
		
		if (isSystemAdmin())
			return true;
		else if (obj instanceof String)
			// obj is cid
			return isMember((String)obj, true);
		else if (obj instanceof DBCommunityImpl)
			return isMember( ((DBCommunityImpl)obj).id, true);
		else if (obj instanceof Principal)
			return isMate(((DBPrincipalImpl)obj).id, true);	
		else
			return false;
	}
	
	private boolean isMateUncached(Object obj) throws ZenoException {
		
		if (isSelf(obj))
			return true;
		if (isSystemAdmin())
			return true;
		if (obj instanceof String)
			// obj is cid
			return isMember((String)obj, false);
		if (obj instanceof DBCommunityImpl)
			return isMember( ((DBCommunityImpl)obj).id, false);
		if (obj instanceof Principal) 
			return isMate(((DBPrincipalImpl)obj).id, false);	
		else
			return false;
	}
	
	public boolean isMate(Object obj) throws ZenoException {
		boolean gleich = obj == lastMate;
		if (obj == lastMate)
			return true;
		boolean result = isMateUncached(obj);
		if (result)
			lastMate = obj;
		return result;
	}
	
	/*
	public boolean isRegistered() {
		return (!username.equals("guest") && !username.equals("any"));
	}
	*/
	
	public boolean isUser() {
		if (!type.equals("person"))
			return false;
		if (username.equals("guest")) 
			return false;
		if (username.equals("any"))
			return false;
		else
			return true;
	}

	public boolean isSelf(Object obj) {
		// only used for user not collectives
		if (isUser() && obj instanceof Principal) {
			String principal = ((Principal) obj).getId();
			return username.equals(principal);
		} else {
			return false;
		}
	}
	
	
	/** is meber (and admin) of community */
	
	protected boolean isMember(String community, boolean isAdmin)
			throws ZenoException {
		
		boolean result = false;
		StringBuffer buf = new StringBuffer();
		buf.append("select count(*) from community_member");
		buf.append(" where community=");
		buf.append(DBClient.format(community));
		buf.append(" and member =");
		buf.append(DBClient.format(this.username));
		if (isAdmin) {
			buf.append(" and is_admin='true'");
		}
		
		try {	
			ResultSet rs = dbclient.executeQuery(buf.toString());
			if (rs.next()) {
				result = (rs.getInt(1) > 0);
			} 
		} catch (java.sql.SQLException e) {
			monitor.reportError("DBPrincipalFactory.getAdmins "+ e);
		}
		return result;	
	}
	
	/** is meber (and admin) of a community to which id belongs */
	
	protected boolean isMate(String id, boolean isAdmin) 
			throws ZenoException {

		boolean result = false;
		StringBuffer buf = new StringBuffer();
		buf.append("select count(*) from");
		buf.append(" community_member= tab1, community_member= tab2");
		buf.append(" where tab1.community = tab2.community");
		buf.append(" and tab1.member=");
		buf.append(DBClient.format(id));
		buf.append(" and tab2.member=");
		buf.append(DBClient.format(username));
		if (isAdmin)
			buf.append(" and tab2.is_admin='true'");
		try {
			ResultSet rs = dbclient.executeQuery(buf.toString());
			if (rs.next()) {
				result = (rs.getInt(1) > 0);
			}
		} catch (java.sql.SQLException e) {
			monitor.reportError("XPrincipalChecker.isAdmin " + e);
		}
		return result;
	}
	
	public boolean hasRole(String role, Object obj) 
			throws ZenoException {
		if (role.equals("guest"))
			return username.equals("guest");
		if (role.equals("systemAdmin"))
			return isSystemAdmin();
		else if (role.equals("admin"))
			return isAdmin(obj);
		else if (role.equals("mate"))
			return isMate(obj);
		else if (role.equals("self_or_admin")) 
			return isSelf(obj) || isAdmin(obj);
		else if (role.equals("user"))
			return isUser();
		else
			return false;
	}
	
	public boolean checkSimpleCriterion(String criterion) {
		//criterion (type = pattern) or type = pattern
		StringTokenizer tok = new StringTokenizer(criterion, "()= ");
		String type = tok.nextToken();
		String pattern = tok.nextToken();
		if (type.equalsIgnoreCase("id"))
			return username.equals(pattern);
		else if (type.equalsIgnoreCase("community")) 
			try {
				return isMember(pattern);
			} catch(ZenoException e) {
				System.out.println(e);
				return false;
			}
		else if (type.equalsIgnoreCase("email")) 
			try {
				return hasEmail(pattern);
			} catch(ZenoException e) {
				System.out.println(e);
				return false;
			}	
		else 
			return CriterionParser.matchCriterion(criterion, this.clientInfo);
	}
	
	protected boolean isMember(String community)
			throws ZenoException {
		
		boolean result = false;
		StringBuffer buf = new StringBuffer();
		buf.append("select count(*) from community_member");
		buf.append(" where community like");
		buf.append(DBClient.format(community));
		buf.append(" and member =");
		buf.append(DBClient.format(this.username));
		try {	
			ResultSet rs = dbclient.executeQuery(buf.toString());
			if (rs.next()) {
				result = (rs.getInt(1) > 0);
			} 
		} catch (java.sql.SQLException e) {
			monitor.reportError("DBPrincipalFactory.isMember "+ e);
		}
		return result;	
	}
	
	protected boolean hasEmail(String email)
			throws ZenoException {
		
		boolean result = false;
		StringBuffer buf = new StringBuffer();
		buf.append("select count(*) from principal");
		buf.append(" where email like");
		buf.append(DBClient.format(email));
		buf.append(" and id =");
		buf.append(DBClient.format(this.username));
			
		try {	
			ResultSet rs = dbclient.executeQuery(buf.toString());
			if (rs.next()) {
				result = (rs.getInt(1) > 0);
			} 
		} catch (java.sql.SQLException e) {
			monitor.reportError("DBPrincipalFactory.getAdmins "+ e);
		}
		return result;	
	}
	
	
	public boolean checkCriterion(String criterion) {
		
		String operation = "match";
		if (criterion.startsWith("(and")) 
			operation = "and";
		else if 	(criterion.startsWith("(or")) 
			operation = "or";
		if (operation.equals("match")) 
			return checkSimpleCriterion(criterion);
		else {
			List criteria = CriterionParser.getSubCriteria(criterion);
			Iterator it = criteria.iterator();
			while(it.hasNext()) {
				String subcriterion = (String)it.next();
				boolean partresult = checkCriterion(subcriterion);
				if (operation.equals("and") && ! partresult)
						return false;
				if (operation.equals("or") && partresult)
						return true;
			}
			if (operation.equals("and"))
				return true;
			else 
				return false;
		}
	}
	

	public String getCriterion(String cid, String propertyName)
			throws ZenoException {
		try {
			StringBuffer buf = new StringBuffer();
			buf.append("select value from principal_property where principal = ");
			buf.append(DBClient.format(cid));
			buf.append(" and community='system'");
			buf.append(" and name=");
			buf.append(DBClient.format(propertyName));
			ResultSet rs = dbclient.executeQuery(buf.toString());
			if (rs.next()) {
				return rs.getString("value");
			} else {
				return "";
			}
		} catch (java.sql.SQLException e) {
			monitor.reportError("DBPrincipalFactory.getProperty " + e);
			throw new ZenoException("DatabaseException");
		}
	}
	
	public boolean checkCriteria(String community) {
		
		try {
			String sysrejection = getCriterion("system", "rejection");
			String rejection = getCriterion(community, "rejection");
			String admission = getCriterion(community, "admission");
			
			if (sysrejection != null && ! "".equals(sysrejection)
				&& checkCriterion(sysrejection))
				return false;
			if ("".equals(admission))
				return false;
			if (!"".equals(rejection) && checkCriterion(rejection))
				return false;
			return checkCriterion(admission);
		} catch (ZenoException e) {
			return false;
		}
	}
	
	public boolean checkCriteria(String rejection, String admission) {
		if ("".equals(admission))
			return false;
		if ( !"".equals(rejection) && checkCriterion(rejection))
			return false;
		return checkCriterion(admission);
	}
	
	public boolean isApplicant(Object obj) throws ZenoException {
		
		if (isSystemAdmin())
			return true;
		if (obj instanceof String)
			// obj is cid
			return checkCriteria((String)obj);
		if (obj instanceof DBCommunityImpl)
			return checkCriteria( ((DBCommunityImpl)obj).id);
		else
			return false;
	}		
	

	public void checkPermission(String operation, Object obj)
		throws ZenoException {
		boolean result = false;
		if (operation.startsWith("PrincipalFactory"))
			result = checkPrincipalFactoryPermissions(operation.substring(17), obj);
		else if (operation.startsWith("Principal"))
			result = checkPrincipalPermissions(operation.substring(10), obj);
		else	if (operation.startsWith("Group"))
			result = checkGroupPermissions(operation.substring(6), obj);
		else	if (operation.startsWith("Community"))
			result = checkCommunityPermissions(operation.substring(10), obj);	
		else
			monitor.reportError("PrincipalChecker.checkPermission unknown " + operation);

		if (!result) {
			throw new zeno2.kernel.NoPermissionException(
				"NoPermission " + operation + " " + obj);
		}
	}

	public boolean checkPrincipalPermissions(String op, Object obj)
			throws ZenoException {
		//for property permissions obj is a community
		if (op.equals("getProperty"))
			return isMate(obj);
		if (op.startsWith("get"))
			return isMate(obj);
		if (op.equals("setProperty")) 
			return isAdmin(obj);
		if (op.equals("removeProperty"))
			return isAdmin(obj); 
		if (op.startsWith("get"))
			return isMate(obj);
		if (op.equals("setPassword"))
			return (isAdmin(obj) | isSelf(obj));
		if (op.startsWith("set"))
			return (isAdmin(obj) | isSelf(obj));
		
		if (op.startsWith("leave"))
			return (isAdmin(obj) | isSelf(obj));	
		else
			return false;
	}

	public boolean checkGroupPermissions(String op, Object obj)
			throws ZenoException {
		if (op.equals("getMembers"))
			return isMate(obj);
		if (op.equals("addMember"))
			return isAdmin(obj);
		if (op.equals("removeMember"))
			return isAdmin(obj);
		else
			return false;
	}
	
	public boolean checkCommunityPermissions(String op, Object obj)
			throws ZenoException {
		if (op.equals("getMembers"))
			return isMate(obj);
		if (op.equals("addMember"))
			return isAdmin(obj);
		if (op.equals("removeMember"))
			return isAdmin(obj);
		if (op.equals("getAdmins"))
			return isMate(obj);
		if (op.equals("addAdmin"))
			return isAdmin(obj);
		if (op.equals("removeAdmin"))
			return isAdmin(obj);	
		else
			return false;
	}

	public boolean checkPrincipalFactoryPermissions(String op, Object obj)
			throws ZenoException {
		if (op.equals("createUser"))
			return isAdmin(obj);
		if (op.equals("createCollective"))
			return isAdmin(obj);
		if (op.equals("createGroup"))
			return isAdmin(obj);
		if (op.equals("createCommunity"))
			return isSystemAdmin();
		if (op.equals("removeCommunity"))
			return isSystemAdmin();	
		if (op.equals("removePrincipal"))
			return isZenoAdmin();
		if (op.equals("searchPrincipals"))
			return isMate(obj);
		if (op.equals("simpleSearchPrincipals"))
			return true;
		if (op.startsWith("register"))
			return isApplicant(obj);	
		return false;
	}

	
}