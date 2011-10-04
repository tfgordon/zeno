package zeno2.db;

import java.sql.ResultSet;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;

import zeno2.kernel.Community;
import zeno2.kernel.Group;
import zeno2.kernel.NameInUseException;
import zeno2.kernel.NotFoundException;
import zeno2.kernel.NoPermissionException;
import zeno2.kernel.Principal;
import zeno2.kernel.ZenoException;
import zeno2.util.CriterionParser;


public class DBPrincipalFactory implements PrincipalFactory {
		
	MonitorImpl monitor;
	DBClient dbclient;
	//PermissionChecker prchecker;
	XPrincipalChecker prchecker;
	String username;
	String clientInfo;
		
	
	public DBPrincipalFactory(MonitorImpl monitor,
								String username, String password, String clientInfo)
			throws ZenoException {
		this.monitor = monitor;
		this.dbclient = monitor.getDBClient();
		this.username = username;
		this.clientInfo = clientInfo;
		prchecker = new XPrincipalChecker(monitor, username, clientInfo);
	}
	
	public void reportError(String op, Throwable e) {
		monitor.reportError(op + "  " + e);
	}
	
	public PermissionChecker getPrincipalChecker() {
		return prchecker;
	}

	public void checkPermission(String method, Object object)
			throws ZenoException {
		if (!monitor.isZenoAdmin(username))
			prchecker.checkPermission(method, object);
	}
	
	
	
	protected boolean isSelf(String uid) {
		return username.equals(uid);
	}
	
	
	public boolean hasRole(String role, Object obj) throws ZenoException {
		
		return prchecker.hasRole(role, obj);
	}
	
	
	protected void addToCommunity(String community, String id, boolean isAdmin) 
			throws ZenoException {
		 
		try {
			StringBuffer buf = new StringBuffer();
			buf.append("insert into community_member (community, member, is_admin) ");
			buf.append("values(");
			buf.append(DBClient.format(community));
			buf.append(", ");
			buf.append(DBClient.format(id));
			buf.append(", ");
			buf.append(DBClient.format(isAdmin));
			buf.append(")");		
			dbclient.executeUpdate(buf.toString());
		} catch (java.sql.SQLException e) {
			String msg = e.getMessage();
			int index = msg.indexOf("Duplicate entry");
			if (index == -1) {
				reportError("DBXPrincipalFactory.addToCommunity", e);
				throw new ZenoException("DBException");
			}
		}
	}
	
	public Community createCommunity(String cid, String name)
			throws ZenoException {
		
		checkPermission("PrincipalFactory.createCommunity", this);
		
		Date creationDate = new java.util.Date();
		StringBuffer buf = new StringBuffer();
		buf.append("insert into principal ");
		buf.append("(id, class, common_name, creator, creation_date) ");
		buf.append(" values(");
		buf.append(DBClient.format(cid));
		buf.append(", ");
		buf.append(DBClient.format("community"));
		buf.append(", ");
		buf.append(DBClient.format(name));
		buf.append(", ");
		buf.append(DBClient.format(username));
		buf.append(", ");
		buf.append(DBClient.format(creationDate));
		buf.append(")");
		try {
			dbclient.executeUpdate(buf.toString());
		} catch (java.sql.SQLException e) {
			int index = e.getMessage().indexOf("Duplicate entry");
			if (index != -1)
				throw new NameInUseException("PrincipalExists " + cid);
			else {
				reportError("DBPrincipalFactory.createCommunity", e);
				throw new ZenoException("DBException");
			}
		}
		
		addToCommunity(cid, this.username, true);
		return (Community)loadPrincipal(cid);
	}
				
	public Principal createUser(String community, 
									String uid, String name, String email)
			throws ZenoException {
		
		checkPermission("PrincipalFactory.createUser", community);
		
		Date creationDate = new java.util.Date();
		StringBuffer buf = new StringBuffer();
		buf.append("insert into principal ");
		buf.append("(id, class, common_name, email,");
		buf.append(" password,creator, creation_date) ");
		buf.append(" values(");
		buf.append(DBClient.format(uid));
		buf.append(", ");
		buf.append(DBClient.format("person"));
		buf.append(", ");
		buf.append(DBClient.format(name));
		buf.append(", ");
		buf.append(DBClient.format(email));
		buf.append(", ");
		buf.append(DBClient.format(uid));
		buf.append(", ");
		buf.append(DBClient.format(username));
		buf.append(", ");
		buf.append(DBClient.format(creationDate));
		buf.append(")");
		try {	
			dbclient.executeUpdate(buf.toString());
		} catch (java.sql.SQLException e) {
			int index = e.getMessage().indexOf("Duplicate entry");
			if (index != -1)
				throw new NameInUseException("PrincipalExists " + uid);
			else {
				reportError("DBPrincipalFactory.createUser", e);
				throw new ZenoException("DBException");
			}
		}
		
		addToCommunity(community, uid, false);
		return loadPrincipal(uid);
	}
	
	public void register(String community)
			throws ZenoException {
		
		checkPermission("PrincipalFactory.register", community);
		addToCommunity(community, username, false);
	}
	
	public Principal registerAs(String community, 
									String uid, String name, String password)
			throws ZenoException {
		
		checkPermission("PrincipalFactory.registerAs", community);
		
		Date creationDate = new java.util.Date();
		StringBuffer buf = new StringBuffer();
		buf.append("insert into principal ");
		buf.append("(id, class, common_name, email,");
		buf.append(" password, creator, creation_date) ");
		buf.append(" values(");
		buf.append(DBClient.format(uid));
		buf.append(", ");
		buf.append(DBClient.format("person"));
		buf.append(", ");
		buf.append(DBClient.format(name));
		buf.append(", ");
		buf.append(DBClient.format(""));
		buf.append(", ");
		buf.append(DBClient.format(password));
		buf.append(", ");
		buf.append(DBClient.format(uid));
		buf.append(", ");
		buf.append(DBClient.format(creationDate));
		buf.append(")");
		try {	
			dbclient.executeUpdate(buf.toString());
		} catch (java.sql.SQLException e) {
			int index = e.getMessage().indexOf("Duplicate entry");
			if (index != -1)
				throw new NameInUseException("PrincipalExists " + uid);
			else {
				reportError("DBPrincipalFactory.registerAs", e);
				throw new ZenoException("DBException");
			}
		}
		
		addToCommunity(community, uid, false);
		DBPrincipalFactory newfactory = 
			new DBPrincipalFactory(monitor, uid, password, clientInfo);
		Principal newprincipal = null;
		try {
			newprincipal = newfactory.loadPrincipal(uid);
		} catch (ZenoException e) {
			System.out.println("DBPrincipalFactory register as " + e);
		}
		return newprincipal;
	}
	
	public Principal createCollective(String community, 
										String uid, String name, String password)
			throws ZenoException {
		
		checkPermission("PrincipalFactory.createCollective", community);
		
		Date creationDate = new java.util.Date();
		StringBuffer buf = new StringBuffer();
		buf.append("insert into principal ");
		buf.append("(id, class, common_name, email,");
		buf.append(" password, creator, creation_date) ");
		buf.append(" values(");
		buf.append(DBClient.format(uid));
		buf.append(", ");
		buf.append(DBClient.format("collective"));
		buf.append(", ");
		buf.append(DBClient.format(name));
		buf.append(", ");
		buf.append(DBClient.format(""));
		buf.append(", ");
		buf.append(DBClient.format(password));
		buf.append(", ");
		buf.append(DBClient.format(username));
		buf.append(", ");
		buf.append(DBClient.format(creationDate));
		buf.append(")");
		try {	
			dbclient.executeUpdate(buf.toString());
		} catch (java.sql.SQLException e) {
			int index = e.getMessage().indexOf("Duplicate entry");
			if (index != -1)
				throw new NameInUseException("PrincipalExists " + uid);
			else {
				reportError("DBPrincipalFactory.createCollective", e);
				throw new ZenoException("DBException");
			}
		}
		
		addToCommunity(community, uid, false);
		return loadPrincipal(uid);
	}
	
	
	public Group createGroup(String community, String uid, String name) 
			throws ZenoException {
		
		checkPermission("PrincipalFactory.createGroup", community);
		
		Date creationDate = new java.util.Date();
		StringBuffer buf = new StringBuffer();
		buf.append("insert into principal ");
		buf.append("(id, class, common_name, creator, creation_date) ");
		buf.append(" values(");
		buf.append(DBClient.format(uid));
		buf.append(", ");
		buf.append(DBClient.format("group"));
		buf.append(", ");
		buf.append(DBClient.format(name));
		buf.append(", ");
		buf.append(DBClient.format(username));
		buf.append(", ");
		buf.append(DBClient.format(creationDate));
		buf.append(")");
		try {	
			dbclient.executeUpdate(buf.toString());
		} catch (java.sql.SQLException e) {
			int index = e.getMessage().indexOf("Duplicate entry");
			if (index != -1)
				throw new NameInUseException("PrincipalExists " + uid);
			else {
				reportError("DBPrincipalFactory.createGroup", e);
				throw new ZenoException("DBException");
			}
		}
	
		addToCommunity(community, uid, false);
		return (Group)loadPrincipal(uid);
	}
	
	protected String getPrincipalType(String id) throws ZenoException {
		
		StringBuffer buf = new StringBuffer();
		buf.append("select class from principal where id =");
		buf.append(DBClient.format(id));
		String type = "";
		try { 
			ResultSet rs = dbclient.executeQuery(buf.toString());
			if (rs.next()) 
				type = rs.getString("class");
			else
				throw new NotFoundException("NoSuchPrincipal " + id);
		} catch (java.sql.SQLException e) {
			reportError("DBPrincipalFactory.getPrincipalType", e);
			throw new ZenoException("DBException");
		}
		return type;
	}
	
	public Principal loadPrincipal(String uid) throws ZenoException {
		
		StringBuffer buf = new StringBuffer();
		buf.append("select * from principal where id = ");
		buf.append(DBClient.format(uid));
		try {
			ResultSet rs = dbclient.executeQuery(buf.toString());
			if (rs.next()) {
				return loadPrincipal(rs);
			} else
				throw new NotFoundException("NoSuchPrincipal " + uid);
		} catch (java.sql.SQLException e) {
			reportError("DBPrincipalFactory.loadPrincipal", e);
			throw new ZenoException("DBException");
		}
	}

	protected DBPrincipalImpl loadPrincipal(ResultSet rs) throws ZenoException {
		try {
			DBPrincipalImpl principal = null;
			String zenoClass = rs.getString("class");
			String id = rs.getString("id");
			if (zenoClass.equals("person")) {
				principal = new DBPrincipalImpl(id, this);
				principal.fill(rs);
			} else if (zenoClass.equals("collective")) {
				principal = new DBCollectiveImpl(id, this);
				principal.fill(rs);
			} else if (zenoClass.equals("group")) {
				principal = new DBGroupImpl(id, this);
				principal.fill(rs);
			} else if (zenoClass.equals("community")) {
				principal = new DBCommunityImpl(id, this);
				principal.fill(rs); 
			} else {
				throw new ZenoException("NoSuchClass");
			}
			return principal;
		} catch (java.sql.SQLException e) {
			reportError("DBPrincipalFactory.loadPrincipal", e);
			throw new ZenoException("DatabaseException");
		}
	}
	
	protected List loadPrincipals(Collection ids) {
		List result = new ArrayList();
		Iterator it = ids.iterator();
		while(it.hasNext()) {
			String id = (String)it.next();
			try {
				Principal pr = loadPrincipal(id);
				result.add(pr);
			} catch(ZenoException e) {
				reportError("DBPrincipalFactory.loadPrincipals", e);
			}
		}
		Collections.sort(result, new PrincipalComparator());
		return result;
	}
	
	class PrincipalComparator implements Comparator {
	
		public int compare (Object o1, Object o2) {
			int result = 0;
			if (o1 instanceof Principal && o2 instanceof Principal) {
				Principal p1 = (Principal) o1;
				Principal p2 = (Principal) o2;
				result = stringCompare(p1.getId(), p2.getId());
			} 
			return result;
		}
	
	private final int stringCompare(String a1, String a2) {
			String s1 = a1.toLowerCase();
			String s2 = a2.toLowerCase();
			int len1 = s1.length();
			int len2 = s2.length();
			int n = Math.min(len1, len2);
			for (int i = 0; i < n; i++) {
				char c1 = s1.charAt(i);
				char c2 = s2.charAt(i);
				if (c1 != c2) { return c1 - c2; }
			}
			return len1 - len2;
		}
	}	
	
	
	protected void erasePrincipal(String id) throws ZenoException {
		StringBuffer buf = new StringBuffer();
		buf.append("delete from principal where id=");
		buf.append(DBClient.format(id));
		try {	
			dbclient.executeUpdate(buf.toString());
		} catch (java.sql.SQLException e) {
			reportError("DBPrincipalFactory.erasePrincipal", e);
			throw new ZenoException("DBException");
		}
		
		buf.setLength(0);
		buf.append("delete from principal_property where principal=");
		buf.append(DBClient.format(id));
		try {
			dbclient.executeUpdate(buf.toString());
		} catch (java.sql.SQLException e) {
			reportError("DBPrincipalFactory.erasePrincipal", e);
			throw new ZenoException("DataBaseException");
		}
	}
	
	private void removeFromRole(String id) throws ZenoException {
		StringBuffer buf = new StringBuffer();
		buf.append("delete from role where principal=");
		buf.append(DBClient.format(id));
		try {
			dbclient.executeUpdate(buf.toString());
		} catch (java.sql.SQLException e) {
			reportError("DBPrincipalFactory.removeFromRole", e);
			throw new ZenoException("DBException");
		}
	}
	
	protected void removeFromCommunity(String community, String id) 
			throws ZenoException {
		
		StringBuffer buf = new StringBuffer();
		buf.append("delete from community_member ");
		buf.append(" where member =");
		buf.append(DBClient.format(id));
		if (!community.equals("")) {
			buf.append(" and community =");
			buf.append(DBClient.format(community));
		}
		
		try {	
			dbclient.executeUpdate(buf.toString());
		} catch (java.sql.SQLException e) {
			reportError("DBXPrincipalFactory.removeFromCommunity", e);
			throw new ZenoException("DBException");
		}
	}
	
	protected boolean hasOtherAdmins(String community, String uid)
			throws ZenoException {
		
		boolean result = false;
		try {
			StringBuffer buf = new StringBuffer();
			buf.append("select count(*) from community_member");
			buf.append(" where community = ");
			buf.append(DBClient.format(community));
			buf.append(" and is_admin = 'true'");
			buf.append(" and member <>");
			buf.append(DBClient.format(uid));
			ResultSet rs = dbclient.executeQuery(buf.toString());
			if (rs.next()) 
				result =  rs.getInt(1) > 0;
		} catch (java.sql.SQLException e) {
			reportError("DBPrincipalFactory.hasOtherAdmin", e);
			throw new ZenoException("DBException");
		}
		return result;	
	}
	
	protected void removeFromAllGroups(String community, String id) 
			throws ZenoException {
			
		StringBuffer buf = new StringBuffer();
		buf.append("delete from group_member where zgroup=? and member=?");
		PreparedStatement pdstm;
		try {
			Connection con = dbclient.getConnection();
			pdstm = con.prepareStatement(buf.toString());
		} catch (java.sql.SQLException e) {
			reportError("DBXPrincipalFactory.removeFromAllGroups", e);
			throw new ZenoException("DB error");
		}
		
		Vector containers = getContainers(community, id);
		Enumeration enum = containers.elements();
		while(enum.hasMoreElements()) {
			try {
				String gid = (String)enum.nextElement();
				pdstm.clearParameters();
				pdstm.setString(1, gid);
				pdstm.setString(2, id);
				pdstm.executeUpdate();
			} catch (java.sql.SQLException e) {
				reportError("DBXPrincipalFactory.removeFromAllGroups", e);
				throw new ZenoException("DB error");
			}	
		}
	}
	
	//obsolete???
	private void removeFromAllGroups(String id) throws ZenoException {
		StringBuffer buf = new StringBuffer();
		buf.append("delete from group_member where member=");
		buf.append(DBClient.format(id));
		try {
			dbclient.executeUpdate(buf.toString());
		} catch (java.sql.SQLException e) {
			reportError("DBPrincipalFactory.removeFromAllGroups", e);
			throw new ZenoException("DBException");
		}
	}
	
	protected void removeGroupMember(String id) throws ZenoException {
		StringBuffer buf = new StringBuffer();
		buf.append("delete from group_member where zgroup=");
		buf.append(DBClient.format(id));
		try {
			dbclient.executeUpdate(buf.toString());
		} catch (java.sql.SQLException e) {
			reportError("DBPrincipalFactory.removeFromAllGroups", e);
			throw new ZenoException("DBException");
		}
	}
	
	public void removePrincipal(String community, String id)
			throws ZenoException {
			
		String type = getPrincipalType(id);
		
		if ("person".equals(type)) {
		
			if (hasOtherAdmins(community, id)) {
				removeFromCommunity(community, id);
				removeFromAllGroups(community, id);
				List communities = getCommunities(id, "member");
				if (communities.size() == 0) {
					erasePrincipal(id);
					removeFromRole(id);
				}
			} else {
				throw new NoPermissionException("lastAdmin");
			}
			
		} else if ("collective".equals(type)) {
		
			removeFromCommunity(community, id);
			removeFromAllGroups(community, id);
			List communities = getCommunities(id, "member");
			if (communities.size() == 0) {
				erasePrincipal(id);
				removeFromRole(id);
			}	
			
		} else if ("group".equals(type)) {
		
				removeFromCommunity(community, id);
				removeFromAllGroups(community, id);
				removeGroupMember(id);
				erasePrincipal(id);
				removeFromRole(id);
		}
	}
	
	
	public void removeCommunity(String community) throws ZenoException {
	
		List groups = getCommunityMembers(community, "group");
		Iterator git = groups.iterator();
		while(git.hasNext()) {
			String group = (String)git.next();
			erasePrincipal(group);
			removeGroupMember(group);
		}
		List persons = getCommunityMembers(community, "person");
		Iterator pit = groups.iterator();
		while(pit.hasNext()) {
			String person = (String)pit.next();
			if (getCommunities(person, "member").size() == 1)
				erasePrincipal(person);
		}
		StringBuffer buf = new StringBuffer();
		buf.append("delete from community_member where community=");
		buf.append(DBClient.format(community));
		try {
			dbclient.executeUpdate(buf.toString());
		} catch (java.sql.SQLException e) {
			reportError("DBPrincipalFactory.removeCommunity", e);
			throw new ZenoException("DataBaseException");
		}
		erasePrincipal(community);
	}
	
	public int communityMemberCount(String community, String type)
			throws ZenoException {
		StringBuffer buf = new StringBuffer();
		buf.append("select count(*) from principal, community_member where ");
		buf.append(" principal.id = community_member.member");
		buf.append(" and community_member.community =");
		buf.append(DBClient.format(community));
		if (!type.equals("")) {
			buf.append(" and principal.class=");
			buf.append(DBClient.format(type));
		}
		try {
			int count = -1;
			ResultSet rs = dbclient.executeQuery(buf.toString());
			if(rs.next()) {
				count = rs.getInt(1);
			}
			return count;
		} catch (java.sql.SQLException e) {
			reportError("DBXPrincipalFactory.communityMemberCount", e);
			throw new ZenoException("DBException");
		}
	}		
	
	
	public List getCommunityMembers(String community, String type)
			throws ZenoException {
		List result = new ArrayList();
		StringBuffer buf = new StringBuffer();
		buf.append("select * from principal, community_member where ");
		buf.append(" principal.id = community_member.member");
		buf.append(" and community_member.community =");
		buf.append(DBClient.format(community));
		if (!type.equals("")) {
			buf.append(" and principal.class=");
			buf.append(DBClient.format(type));
		}
		try {
			ResultSet rs = dbclient.executeQuery(buf.toString());
			while(rs.next()) {
				String id = rs.getString("principal.id");
				result.add(id);
			}
			return result;
		} catch (java.sql.SQLException e) {
			reportError("DBXPrincipalFactory.getCommunityMembers", e);
			throw new ZenoException("DBException");
		}
	}
	
	public boolean isCommunityMember(String community, String id)
			throws ZenoException {
			
		StringBuffer buf = new StringBuffer();
		buf.append("select member from community_member");
		buf.append(" where community=");
		buf.append(DBClient.format(community));
		buf.append(" and member=");
		buf.append(DBClient.format(id));
		try {
			ResultSet rs = dbclient.executeQuery(buf.toString());
			return rs.next();
		} catch (java.sql.SQLException e) {
			reportError("DBXPrincipalFactory.addToCommunity", e);
			throw new ZenoException("DBException");
		}
	}
	
	public boolean areCoMembers(String id1, String id2) throws ZenoException {
		
		StringBuffer buf = new StringBuffer();
		buf.append("select count(*) from");
		buf.append(" community_member= tab1, community_member= tab2");
		buf.append(" where tab1.community = tab2.community");
		buf.append(" and tab1.member=");
		buf.append(DBClient.format(id1));
		buf.append(" and tab2.member=");
		buf.append(DBClient.format(id2));
		try {
			ResultSet rs = dbclient.executeQuery(buf.toString());
			if (rs.next())
				return rs.getInt(1) > 0;
			else
				return false;
		} catch (java.sql.SQLException e) {
			monitor.reportError("XPrincipalChecker.isAdmin " + e);
			return false;
		}
	}
	
	public List getAdmins(String community)
			throws ZenoException {
			
		try {
			List admins = new ArrayList();
			StringBuffer buf = new StringBuffer();
			buf.append("select member from community_member");
			buf.append(" where community = ");
			buf.append(DBClient.format(community));
			buf.append(" and is_admin = 'true'");
			ResultSet rs = dbclient.executeQuery(buf.toString());
			while (rs.next()) {
				admins.add(rs.getString("member"));
			}
			return admins;
		} catch (java.sql.SQLException e) {
			reportError("DBPrincipalFactory.getAdmins", e);
			throw new ZenoException("DBException");
		}	
	}
	
	protected void setAdmin(String community, String id, boolean isAdmin)
			throws ZenoException {
		
		StringBuffer buf = new StringBuffer();
		buf.append("update community_member set is_admin =");
		buf.append(DBClient.format(isAdmin));
		buf.append(" where community =");
		buf.append(DBClient.format(community));
		buf.append(" and member = ");
		buf.append(DBClient.format(id));
		try {	
			dbclient.executeUpdate(buf.toString());
		} catch (java.sql.SQLException e) {
			reportError("DBXPrincipalFactory.addAdmin", e);
			throw new ZenoException("DBException");
		}
	}
	
	public List getPropertyKeys(String uid) throws ZenoException {
		//factory.checkPermission("ZenoResource.getPropertyKeys", this);
		
		List result = new ArrayList();
		StringBuffer buf = new StringBuffer();
		buf.append("select name from principal_property where principal = ");
		buf.append(DBClient.format(uid));
		try {	
			ResultSet rs = dbclient.executeQuery(buf.toString());
			while (rs.next()){
				result.add(rs.getString("name"));
			}
			return result;
		} catch (java.sql.SQLException e) {
			reportError("DBPrincipalFactory.getPropertyKeys", e);
			throw new ZenoException("DataBaseException");
		}
	}
	
	protected List getProperties(String id, String community)
			throws ZenoException {
		List result = new ArrayList();
		try {
			StringBuffer buf = new StringBuffer();
			buf.append("select name, value from principal_property where principal = ");
			buf.append(DBClient.format(id));
			buf.append(" and community=");
			buf.append(DBClient.format(community));
			ResultSet rs = dbclient.executeQuery(buf.toString());
			while (rs.next()) {
				String name = rs.getString("name");
				String value = rs.getString("value");
				result.add(name);
				result.add(value);
				}
		} catch (java.sql.SQLException e) {
			reportError("DBPrincipalFactory.getProperty", e);
			throw new ZenoException("DatabaseException");
		}
		return result;
	}		


	public String getProperty(String id, String community, String propertyName)
			throws ZenoException {
		try {
			StringBuffer buf = new StringBuffer();
			buf.append("select value from principal_property where principal = ");
			buf.append(DBClient.format(id));
			buf.append(" and community=");
			buf.append(DBClient.format(community));
			buf.append(" and name=");
			buf.append(DBClient.format(propertyName));
			ResultSet rs = dbclient.executeQuery(buf.toString());
			if (rs.next()) {
				return rs.getString("value");
			} else {
				return null;
			}
		} catch (java.sql.SQLException e) {
			reportError("DBPrincipalFactory.getProperty", e);
			throw new ZenoException("DatabaseException");
		}
	}

	public void setProperty(String id, String community, 
								String propertyName, String propertyValue)
			throws ZenoException {
		
		try {
			StringBuffer buf = new StringBuffer();
	
			if (getProperty(id, community, propertyName) == null) {
				// insert new property
				buf.append("insert into principal_property");
				buf.append(" (principal, community, name, value)");
				buf.append(" values(");
				buf.append(DBClient.format(id));
				buf.append(", ");
				buf.append(DBClient.format(community));
				buf.append(", ");
				buf.append(DBClient.format(propertyName));
				buf.append(", ");
				buf.append(DBClient.format(propertyValue));
				buf.append(")");
			} else {
				// update existing property
				buf.append("update principal_property set value=");
				buf.append(DBClient.format(propertyValue));
				buf.append(" where principal = ");
				buf.append(DBClient.format(id));
				buf.append(" and community=");
				buf.append(DBClient.format(community));
				buf.append(" and name=");
				buf.append(DBClient.format(propertyName));
			}
		
			dbclient.executeUpdate(buf.toString());

		} catch (java.sql.SQLException e) {
			reportError("DBPrincipalFactory.setProperty", e);
			throw new ZenoException("DataBaseException");
		}
	}

	public void removeProperty(String id, String community, String propertyName)
		throws ZenoException {

		StringBuffer buf = new StringBuffer();
		buf.append("delete from principal_property where principal=");
		buf.append(DBClient.format(id));
		buf.append(" and community=");
		buf.append(DBClient.format(community));
		buf.append(" and name=");
		buf.append(DBClient.format(propertyName));
		try {
			dbclient.executeUpdate(buf.toString());
		} catch (java.sql.SQLException e) {
			reportError("DBPrincipalFactory.removeProperty", e);
			throw new ZenoException("DataBaseException");
		}
	}

//obsolete ???
	public void removeAllProperties(String id) throws ZenoException {

		StringBuffer buf = new StringBuffer();
		buf.append("delete from principal_property where principal=");
		buf.append(DBClient.format(id));
		try {
			dbclient.executeUpdate(buf.toString());
		} catch (java.sql.SQLException e) {
			reportError("DBPrincipalFactory.removeProperty", e);
			throw new ZenoException("DataBaseException");
		}

	}
	
	public boolean checkClientInfo(String username, String clientInfo) {
		try {
			String membership = getProperty(username, "system", "membership");
			if (membership == null || "".equals(membership.trim()))
				return true;
			else 
				return CriterionParser.matchCriterion(membership, clientInfo);
		} catch (ZenoException e) {
			reportError("checkClientInfo", e);
			return false;
		}
	}
	
	
	public boolean checkPassword(String name, String password)
		throws ZenoException {

		try {
			StringBuffer buf = new StringBuffer();
			buf.append("select * from principal where id=");
			buf.append(DBClient.format(name));
			buf.append(" and password= ");
			buf.append(DBClient.format(password));
			buf.append(" and (class = 'person' or class ='collective')");
			ResultSet rs = dbclient.executeQuery(buf.toString());
			return rs.next();
		} catch (java.sql.SQLException e) {
			reportError("checkPassword", e);
			throw new ZenoException("DBExcepyion");
		}
	}

	public void setPassword(String name, String oldPassword, String newPassword)
			throws ZenoException {
		
		if (monitor.isZenoAdmin(name) && !(name.equals(username))) 
			return;
		
		try {
			StringBuffer buf = new StringBuffer();
			buf.append("select creation_date from principal where id =");
			buf.append(DBClient.format(name));
			ResultSet rs = dbclient.executeQuery(buf.toString());
			rs.next();
			Date creationDate = rs.getTimestamp("creation_date");
			buf.setLength(0);
			buf.append("update principal set password= ");
			buf.append(DBClient.format(newPassword));
			// to avoid automatic update 
			buf.append(", creation_date =");
			buf.append(DBClient.format(creationDate));
			buf.append(" where id = ");
			buf.append(DBClient.format(name));
			buf.append(" and password = ");
			buf.append(DBClient.format(oldPassword));
			dbclient.executeUpdate(buf.toString());
		} catch (java.sql.SQLException e) {
			reportError("DBPrincipalFactory.setPassword", e);
			throw new ZenoException("DBException");
		}
	}

	public void setPassword(String name, String newPassword) throws ZenoException {
		
		if (monitor.isZenoAdmin(name) && !(name.equals(username))) 
			return;
		
		try {
			StringBuffer buf = new StringBuffer();
			buf.append("select creation_date from principal where id =");
			buf.append(DBClient.format(name));
			ResultSet rs = dbclient.executeQuery(buf.toString());
			rs.next();
			Date creationDate = rs.getTimestamp("creation_date");
			buf.setLength(0);
			buf.append("update principal set password= ");
			buf.append(DBClient.format(newPassword));
			// to avoid automatic update 
			buf.append(", creation_date =");
			buf.append(DBClient.format(creationDate));
			buf.append(" where id = ");
			buf.append(DBClient.format(name));
			dbclient.executeUpdate(buf.toString());
		} catch (java.sql.SQLException e) {
			reportError("DBPrincipalFactory.setPassword", e);
			throw new ZenoException("DBException");
		}
	}
	
	
	public void addMembers(String id, List list) throws ZenoException {
		try {
			StringBuffer buf = new StringBuffer();
			buf.append("insert into group_member (zgroup, member) ");
			buf.append("values(");
			buf.append(DBClient.format(id));
			buf.append(", ");
			int size = buf.length();
			Iterator it = list.iterator();
			while (it.hasNext()) {
				buf.setLength(size);
				String cid = (String) it.next();
				buf.append(DBClient.format(cid));
				buf.append(")");
				dbclient.executeUpdate(buf.toString());
			}
		} catch (java.sql.SQLException e) {
			reportError("DBPrincipalFactory.addMembers", e);
			throw new ZenoException("DBException");
		}
	}

	public void removeMembers(String id, List list) throws ZenoException {
		try {
			StringBuffer buf = new StringBuffer();
			buf.append("delete from group_member where zgroup=");
			buf.append(DBClient.format(id));
			buf.append(" and member in (");
			Iterator it = list.iterator();
			while (it.hasNext()) {
				String cid = (String) it.next();
				buf.append(DBClient.format(cid));
				if (it.hasNext())
					buf.append(", ");
			}
			buf.append(")");
			dbclient.executeUpdate(buf.toString());

		} catch (java.sql.SQLException e) {
			reportError("DBPrincipalFactory.removeMembers", e);
			throw new ZenoException("DBException");
		}
	}
	
	
	
	protected Vector getGroupMembers(String gid) throws ZenoException {
		Vector members = new Vector();
		StringBuffer buf = new StringBuffer();
		buf.append("select * from group_member where zgroup=");
		buf.append(DBClient.format(gid));
		try {
			ResultSet rs = dbclient.executeQuery(buf.toString());
			while (rs.next()) {
				members.addElement(rs.getString("member"));
			}
			return members;
		} catch(java.sql.SQLException e) {
			reportError("DBPrincipalFactory.geGroupMembers", e);
			throw new ZenoException("DBException");
		}
	}
	
	
	//heg protected
	public Vector getMembers(String id) throws ZenoException {

		try {
			String query = "select class from principal where id =" + DBClient.format(id);
			ResultSet rs = dbclient.executeQuery(query);
			if (rs.next()) {
				String zclass = rs.getString("class");
				if ("person".equals(zclass))
					return null;
				else {

					Vector members = new Vector();
					String query2 =
						"select * from group_member where zgroup=" + DBClient.format(id);
					ResultSet rs2 = dbclient.executeQuery(query2);
					while (rs2.next()) {
						members.addElement(rs2.getString("member"));
					}
					return members;
				}
			} else
				throw new NotFoundException("NoSuchPrincipal " + id);
		} catch (java.sql.SQLException e) {
			reportError("DBPrincipalFactory.getMembers", e);
			throw new ZenoException("DBException");
		}
	}

	protected Vector getPersons(String id) throws ZenoException {
		Vector members = new Vector();
		try {
			StringBuffer buf = new StringBuffer();
			buf.append("select member, class from group_member, principal");
			buf.append(" where member = id and zgroup = ");
			buf.append(DBClient.format(id));
			buf.append(" and class = 'person'");

			ResultSet rs = dbclient.executeQuery(buf.toString());
			while (rs.next()) {
				members.addElement(rs.getString("member"));
			}
		} catch (java.sql.SQLException e) {
			reportError("DBPrincipalFactory.getPersons", e);
			throw new ZenoException("DBException");
		}
		return members;
	}

	public Set getDirectMembers(String id, boolean personsOnly)
		throws ZenoException {
		Vector members;
		if (personsOnly)
			members = getPersons(id);
		else
			members = getMembers(id);
		return new HashSet(members);
	}

	/**
	* adds direct and indirect members of dn to result
	* without repetitions
	*/

	protected void collectMembers(String dn, Set result, Set persons)
		throws ZenoException {
		try {
			Vector newdns = getMembers(dn);
			if (newdns == null) {
				persons.add(dn);
			} else {
				for (int i = 0; i < newdns.size(); i++) {
					String cdn = (String) newdns.elementAt(i);
					if (!result.contains(cdn)) {
						result.add(cdn);
						collectMembers(cdn, result, persons);
					}
				}
			}
		} catch (NotFoundException e) {
			result.remove(dn);
		}
	}

	protected void collectMembers(List dns, Set principals, Set persons)
		throws ZenoException {
		Iterator it = dns.iterator();
		while (it.hasNext()) {
			String cprincipal = (String) it.next();
			collectMembers(cprincipal, principals, persons);
		}
	}

	/**
	* returns direct and indirect members of rdn as a vector of dns
	* without repetitions
	*/

	public Set getAllMembers(String id, boolean personsOnly) throws ZenoException {
		Set result = new HashSet();
		Set persons = new HashSet();
		collectMembers(id, result, persons);
		if (personsOnly)
			return persons;
		else
			return result;
	}

	public boolean isDirectMember(String parentdn, String childdn)
		throws ZenoException {
		//returns true if memberdn is direct  member of rdn
		Vector members;
		try {
			members = getMembers(parentdn);
		} catch (NotFoundException e) {
			return false;
		}
		if (members == null)
			return false;
		else
			return members.contains(childdn);
	}

	public boolean isIndirectMember(String parentdn, String childdn)
		throws ZenoException {

		return isIndirectMember(parentdn, childdn, new Vector());
	}
	
	private boolean isIndirectMember(
		String parentdn,
		String childdn,
		Vector checked)
			throws ZenoException {
		//returns true if memberdn is direct or indirect member of rdn
		Vector members;
		String type;
		try {
			type = getPrincipalType(parentdn);
		} catch (NotFoundException e) {
			return false;
		}
		if (type.equals("community"))
			return isCommunityMember(parentdn, childdn);
		else if (type.equals("person"))
			return false;
		else {
			members = getMembers(parentdn);
			if (members.contains(childdn))
				return true;
			else {
				checked.addElement(parentdn);
				for (int i = 0; i < members.size(); i++) {
					String subdn = (String) members.elementAt(i);
					if (!checked.contains(subdn))
						if (isIndirectMember(subdn, childdn, checked))
							return true;
				}
				return false;
			}
		}
	}
	

	protected Vector getContainers(String id) throws ZenoException {
		Vector result = new Vector();
		StringBuffer buf = new StringBuffer();
		buf.append("select * from group_member");
		buf.append(" where member=");
		buf.append(DBClient.format(id));
		try {
			ResultSet rs = dbclient.executeQuery(buf.toString());
			while (rs.next()) {
				result.add(rs.getString("zgroup"));
			}
			return result;
		} catch (java.sql.SQLException e) {
			reportError("DBPrincipalFactory.getContainers", e);
			throw new ZenoException("DBException");
		}
	}
	
	protected Vector getContainers(String community, String id) 
			throws ZenoException {
		
		Vector result = new Vector();
		StringBuffer buf = new StringBuffer();
		buf.append("select group_member.zgroup from group_member, community_member");
		buf.append(" where group_member.member=");
		buf.append(DBClient.format(id));
		buf.append(" and group_member.zgroup = community_member.member");
		buf.append(" and community_member.community =");
		buf.append(DBClient.format(community));
		try {
			ResultSet rs = dbclient.executeQuery(buf.toString());
			while (rs.next()) {
				result.add(rs.getString("zgroup"));
			}
		} catch (java.sql.SQLException e) {
			reportError("DBPrincipalFactory.getContainers", e);
			throw new ZenoException("DBException");
		}
		return result;
	}
	
	public Vector getContainers(List communities, String id) 
			throws ZenoException {
		
		Vector result = new Vector();
		StringBuffer buf = new StringBuffer();
		buf.append("select group_member.zgroup from group_member, community_member");
		buf.append(" where group_member.member=");
		buf.append(DBClient.format(id));
		buf.append(" and group_member.zgroup = community_member.member");	
		buf.append(" and community_member.community in (");
		Iterator comit = communities.iterator();
		while (comit.hasNext()) {
			buf.append(DBClient.format((String)comit.next()));
			if (comit.hasNext()) {
				buf.append(",");
			}
		}
		buf.append(")");
		try {
			ResultSet rs = dbclient.executeQuery(buf.toString());
			while (rs.next()) {
				result.add(rs.getString("zgroup"));
			}
		} catch (java.sql.SQLException e) {
			reportError("DBPrincipalFactory.getContainers", e);
			throw new ZenoException("DBException");
		}
		return result;
	}
		
	
	protected List getCommonCommunities(String id1, String id2) 
			throws ZenoException {
		
		List result = new ArrayList();
		StringBuffer buf = new StringBuffer();
		buf.append("select distinct tab1.community from");
		buf.append(" community_member= tab1, community_member= tab2");
		buf.append(" where tab1.community = tab2.community");
		buf.append(" and tab1.member=");
		buf.append(DBClient.format(id1));
		buf.append(" and tab2.member=");
		buf.append(DBClient.format(id2));
		try {
			ResultSet rs = dbclient.executeQuery(buf.toString());
			while (rs.next()) {
				result.add(rs.getString(1));
			}
		} catch (java.sql.SQLException e) {
			monitor.reportError("XPrincipalChecker.isAdmin " + e);
		}
		return result;
	}
	
	public Vector getAccessibleContainers(String id) 
			throws ZenoException {
		if (id.equals(this.username) || prchecker.hasRole("systemAdmin", id))
			return getContainers(id);
		else {
			List communities = getCommonCommunities(this.username, id);
			return getContainers(communities, id);
		}
	}
	
	protected void collectContainers(String id, Set result) throws ZenoException {
		//adds groups directly or indirectly containing dn to  result 
		//works recursively
		Vector containers = getContainers(id);
		for (int i = 0; i < containers.size(); i++) {
			String cdn = (String) containers.elementAt(i);
			if (!result.contains(cdn)) {
				result.add(cdn);
				collectContainers(cdn, result);
			}
		}
	}
	
	protected void collectAccessibleContainers(String id, Set result) 
			throws ZenoException {
		//adds groups directly or indirectly containing dn to  result 
		//works recursively
		Vector containers = getAccessibleContainers(id);
		for (int i = 0; i < containers.size(); i++) {
			String cdn = (String) containers.elementAt(i);
			if (!result.contains(cdn)) {
				result.add(cdn);
				//containers of cdn are automatically accessible
				collectContainers(cdn, result);
			}
		}
	}
	
	protected List getCommunities() throws ZenoException {
	
		try {	
			List communities = new ArrayList();
			StringBuffer buf = new StringBuffer();
			buf.append("select id from principal");
			buf.append(" where class = 'community'");
			ResultSet rs = dbclient.executeQuery(buf.toString());
			while (rs.next()) {
				String id = rs.getString("id");
				if (!"any".equals(id))
					communities.add(id);
			}
			return communities;
		} catch (java.sql.SQLException e) {
			reportError("DBPrincipalFactory.getAdmins", e);
			throw new ZenoException("DBException");
		}	
	}
	
	public List getCommunities(String member, String role) throws ZenoException {
		
		try {
			List communities = new ArrayList();
			StringBuffer buf = new StringBuffer();
			buf.append("select community from community_member");
			buf.append(" where member =");
			buf.append(DBClient.format(member));
			if ("admin".equals(role)) {
				buf.append(" and is_admin='true'");
			}
			ResultSet rs = dbclient.executeQuery(buf.toString());
			while (rs.next()) {
				communities.add(rs.getString("community"));
			}
			//communities.add("any");
			return communities;
		} catch (java.sql.SQLException e) {
			reportError("DBPrincipalFactory.getAdmins", e);
			throw new ZenoException("DBException");
		}	
	}
	
	public List getCommunitiesForRegistration(boolean isMember) throws ZenoException {
		
		String sysrejection = 
			getProperty("system", "system", "rejection");
			
		if (sysrejection != null && !sysrejection.equals("")
			 && prchecker.checkCriterion(sysrejection))
			return Collections.EMPTY_LIST;
			
		StringBuffer buf = new StringBuffer();
		buf.append("select id, name, value from principal, principal_property");
		buf.append(" where class='community' and id = principal");
		buf.append(" and community='system'");
		buf.append(" order by id, name");
		try {
			ResultSet rs = dbclient.executeQuery(buf.toString());
			List cids = new ArrayList();
			String cid = "";
			String cadmission = "";
			String crejection = ""; 
			while (rs.next()) {
				if (!cid.equals(rs.getString("id"))) {
					if (checkForRegistration(crejection, cadmission)) 
						cids.add(cid);
				cid = rs.getString("id");
				cadmission = "";
				crejection = "";
				}
				if ("admission".equals(rs.getString("name")))
					cadmission = rs.getString("value").trim();
				else if ("rejection".equals(rs.getString("name")))
					crejection = rs.getString("value").trim();
			}
			if (checkForRegistration(crejection, cadmission)) 
				cids.add(cid);
			if (!isMember) {
				cids.removeAll(getCommunities(username, "member"));
			}
			return loadPrincipals(cids);
		} catch (java.sql.SQLException e) {
			reportError("DBPrincipalFactory.getCommunitesForApply", e);
			throw new ZenoException("DBException");
		} catch(Exception e) {
			e.printStackTrace();
			return Collections.EMPTY_LIST;
		}	
	}
	
	
	private boolean checkForRegistration(String rejection, String admission) {
		if ("".equals(admission))
			return false;
		if (!("".equals(rejection)) && prchecker.checkCriterion(rejection))
			return false;
		return prchecker.checkCriterion(admission);
	}
	
	
	public List getAccessibleCommunities(String id) throws ZenoException {
		
		if (id.equals(this.username))
			if (prchecker.hasRole("systemAdmin", id))
				return getCommunities();
			else 
				return getCommunities(id, "member");	
		else
			if (prchecker.hasRole("systemAdmin", id))
				return getCommunities(id, "member");
			else 
				return getCommonCommunities(this.username, id);
	}

	public Set getAccessibleGroups(String id, boolean directOnly) 
			throws ZenoException {
		if (directOnly) {
			Vector containers = getAccessibleContainers(id);
			return new HashSet(containers);
		} else {
			Set result = new HashSet();
			collectAccessibleContainers(id, result);
			return result;
		}
	}
	
	public ResultSet simpleSearch(String query, String type, int limit) 
			throws java.sql.SQLException {
		
		StringBuffer buf = new StringBuffer();
		buf.append("select * from principal where ");
		if (!"".equals(type)) {
			buf.append(" class=");
			buf.append(DBClient.format(type));
			buf.append(" and (");
		} else
			buf.append(" ("); 
		StringTokenizer tokenizer = new StringTokenizer(query);
		while (tokenizer.hasMoreElements()) {
			String token = tokenizer.nextToken();
			String xtoken = DBClient.format(token + "%");
			//String xtoken = DBClient.format("%" + token + "%");
			buf.append(" id like ");
			buf.append(xtoken);
			if (tokenizer.hasMoreElements())
				buf.append(" or ");
		}
		buf.append(") order by id");
		return dbclient.executeQuery(buf.toString());
	}
	
	
	public ResultSet search(String community, String query, String type, int limit) 
			throws java.sql.SQLException {
		
		StringBuffer buf = new StringBuffer();
		buf.append("select * from principal, community_member where ");
		buf.append(" principal.id = community_member.member");
		int index = community.indexOf(",");
		if (index == -1) {
			buf.append(" and community_member.community =");
			buf.append(DBClient.format(community));
		} else {
			buf.append(" and community_member.community in (");
			//komma separated list
			buf.append(community);
			buf.append(")");
		}
		
		if (! "".equals(type)) {
				buf.append(" and class=");
				buf.append(DBClient.format(type));
		}
		buf.append(" and ("); 
		StringTokenizer tokenizer = new StringTokenizer(query);
		while (tokenizer.hasMoreElements()) {
			String token = tokenizer.nextToken();
			//String xtoken = DBClient.format(token + "%");
			String xtoken = DBClient.format("%" + token + "%");
			buf.append(" id like ");
			buf.append(xtoken);
			buf.append("or common_name like ");
			buf.append(xtoken);
			buf.append("or email like ");
			buf.append(xtoken);
			if (tokenizer.hasMoreElements())
				buf.append(" or ");
		}
		buf.append(") order by id");
		return dbclient.executeQuery(buf.toString());
	}
	
	public Iterator searchPrincipals(String query) 
			throws ZenoException {
		return searchPrincipals("", query, "");
	}
	
	public Iterator searchPrincipals(String community, String query) 
			throws ZenoException {
		return searchPrincipals(community, query, "");
	} 
	
	

	public Iterator searchPrincipals(String community, String query, String type) 
			throws ZenoException {
			
		ResultSet rs;
		try {
			if (community.equals("") | community.equals("any")) {
				checkPermission("PrincipalFactory.simpleSearchPrincipals", this);
				rs = simpleSearch(query, type, 100);
			} else if (community.equals("*")) {
				checkPermission("PrincipalFactory.simpleSearchPrincipals", this);
				String communities = genCommunitiesForSearch();
				rs = search(communities, query, type, 100);
			} else {
				checkPermission("PrincipalFactory.searchPrincipals", community);
				rs = search(community, query, type, 100);
			}
			
			List result = new ArrayList();
			String lastid = "";
			int nr = 1;
			while (rs.next()) {
				if (!rs.getString("id").equals(lastid)) {
					if (nr++ > 100)
						break;
					lastid = rs.getString("id");
					if (!"any".equals(lastid) && ! "system".equals(lastid))
						//any is a pseudo principal	
						result.add(loadPrincipal(rs));
				}
			}
			return result.iterator();
		} catch (java.sql.SQLException e) {
			reportError("DBPrincipalFactory.searchPrincipals", e);
			throw new ZenoException("DataBaseException");
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public String genCommunitiesForSearch() 
			throws ZenoException {
		Iterator pidit = getAccessibleCommunities(username).iterator();
		StringBuffer buf = new StringBuffer();
		
		while(pidit.hasNext()) {
			buf.append(DBClient.format((String)pidit.next()));
			if (pidit.hasNext())
				buf.append(", ");
		}
		return buf.toString();
	}
	
	
}