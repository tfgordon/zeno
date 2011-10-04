package zeno2.db;

import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import zeno2.kernel.Principal;
import zeno2.kernel.ZenoException;
import zeno2.kernel.NoPermissionException;

public class DBPrincipalImpl implements Principal {

	String id;
	String name = "";
	String email = "";
	String organization = "";
	String orgRole = "";
	String description = "";
	String creator = "";
	Date creationDate;
	String community = "";
	List data;
	Date lastLogin;

	boolean changed = false;
	DBPrincipalFactory factory;

	public DBPrincipalImpl(String id, PrincipalFactory factory) {
		this.id = id;
		this.factory = (DBPrincipalFactory) factory;
	}

	private String setString(ResultSet rs, String key) 
			throws java.sql.SQLException{
		String value = rs.getString(key);
		return value != null ? value : "";
	}

	protected void fill(ResultSet rs) throws java.sql.SQLException {
		this.name = setString(rs, "common_name");
		this.email = setString(rs, "email");
		this.organization = setString(rs, "organization");
		this.orgRole = setString(rs, "org_role");
		this.description = setString(rs, "description");
		this.creator = setString(rs, "creator");
		this.creationDate = rs.getTimestamp("creation_date");
	}

	public String getId() {
		return id;
	}

	public String getName() throws ZenoException {
		try {
			if ("any".equals(name))
				return name;
			factory.checkPermission("Principal.getName", this);
			if (!name.equals(""))
				return name;
			else
				return id;
		} catch(NoPermissionException e) {
			return "***";
		}
	}

	public void setName(String name) throws ZenoException {
		factory.checkPermission("Principal.setName", this);
		if (!this.name.equals(name)) {
			this.name = name;
			changed = true;
		}
	}

	public String getEmail() throws ZenoException {
		factory.checkPermission("Principal.getEmail", this);
		return email;
	}

	public void setEmail(String email) throws ZenoException {
		factory.checkPermission("Principal.setEmail", this);
		if (!this.email.equals(email)) {
			this.email = email;
			changed = true;
		}
	}

	public String getOrganization() throws ZenoException {
		factory.checkPermission("Principal.getOrganization", this);
		return organization;
	}

	public void setOrganization(String organization) throws ZenoException {
		factory.checkPermission("Principal.setOrganization", this);
		if (!this.organization.equals(organization)) {
			this.organization = organization;
			changed = true;
		}
	}

	public String getOrgRole() throws ZenoException {
		factory.checkPermission("Principal.getOrgRole", this);
		return orgRole;
	}

	public void setOrgRole(String orgRole) throws ZenoException {
		factory.checkPermission("Principal.setOrgRole", this);
		if (!this.orgRole.equals(orgRole)) {
			this.orgRole = orgRole;
			changed = true;
		}
	}

	public String getDescription() throws ZenoException {
		factory.checkPermission("Principal.getDescription", this);
		return description;
	}

	public void setDescription(String description) throws ZenoException {
		factory.checkPermission("Principal.setDescription", this);
		if (!this.description.equals(description)) {
			this.description = description;
			changed = true;
		}
	}
	
	public String getCreator() throws ZenoException {
		factory.checkPermission("Principal.getCreator", this);
		return creator;
	}
	
	public Date getCreationDate() throws ZenoException {
		factory.checkPermission("Principal.getCreationDate", this);
		return creationDate;
	}
	
	
	public List getPropertyNamesUsed(String community) 
			throws ZenoException {
		if (!factory.isSelf(this.id))
			factory.checkPermission("Principal.getPropertyNamesUsed", community);
		if (!this.community.equals(community)) {
			this.community = community;
			this.data = factory.getProperties(this.id, community);
		}
		List names = new ArrayList();
		Iterator it = this.data.iterator();
		while(it.hasNext()) {
			names.add(it.next());
			// value
			it.next();
		}
		return names;
	}
	
	public String getProperty(String community, String propertyName) 
			throws ZenoException {
		if (!factory.isSelf(this.id))
			factory.checkPermission("Principal.getProperty", community);
		if (!this.community.equals(community)) {
			this.community = community;
			this.data = factory.getProperties(this.id, community);
		}
		int index = data.indexOf(propertyName);
		if (index != -1)
			return (String)data.get(index + 1);
		else
			return null; 
	}

	public void setProperty(String community, String propertyName, String propertyValue)
			throws ZenoException {
		if (!factory.isSelf(this.id))
			factory.checkPermission("Principal.setProperty", community);
		this.community = "";
		factory.setProperty(this.id, community, propertyName, propertyValue);
	}

	public void removeProperty(String community, String propertyName) throws ZenoException {
		if (!factory.isSelf(this.id))
			factory.checkPermission("Principal.removeProperty", this);
		factory.removeProperty(this.id, community, propertyName);
	}

	public String getProperty(String propertyName) throws ZenoException {
		return getProperty("any", propertyName);
	}

	public void setProperty(String propertyName, String propertyValue)
			throws ZenoException {
		setProperty("any", propertyName, propertyValue);
	}

	public void removeProperty(String propertyName) throws ZenoException {
		removeProperty("any", propertyName);
	}

	public java.util.Date getLastLogin() throws ZenoException {
		if (id.equals("guest") || id.equals("any"))
			lastLogin = new Date(0);
		if (lastLogin == null) {
			String value = factory.getProperty(this.id, "system","lastLogin");
			if (value == null)
				lastLogin = new Date(0);
			else {
				long time = Long.parseLong(value);
				lastLogin = new Date(time);
			}
		}
		return lastLogin;
	}

	public boolean checkPassword(String password) throws ZenoException {
		return factory.checkPassword(this.id, password);
	}

	public void setPassword(String newPassword) throws ZenoException {
		factory.checkPermission("Principal.setPassword", this);
		factory.setPassword(this.id, newPassword);
	}
	
	public List getCommunities() throws ZenoException {
	
		factory.checkPermission("Principal.getCommunities", this);
		List cids = factory.getAccessibleCommunities(this.id);
		List communities = factory.loadPrincipals(cids);
		return communities;
	}
	
	
	public List getGroups(boolean directOnly) throws ZenoException {
		factory.checkPermission("Principal.getGroups", this);
		Set groupIds = factory.getAccessibleGroups(this.id, directOnly);
		List groups = factory.loadPrincipals(groupIds);
		return groups;
	}
	
	public void leaveCommunity(String community) throws ZenoException {
		factory.checkPermission("Principal.leaveCommunity", this);
		factory.removePrincipal(community, this.id);
	}
	
	public void leaveGroup(String gid) throws ZenoException {
		factory.checkPermission("Principal.leaveGroup", this);
		List ids = new ArrayList();
		ids.add(this.id);
		factory.removeMembers(gid, ids);
	}
	
	public void save() throws ZenoException {
		
		if (!changed)
			return;
		StringBuffer buf = new StringBuffer();
		buf.append("update principal set common_name = ");
		buf.append(DBClient.format(this.name));
		buf.append(", email = ");
		buf.append(DBClient.format(this.email));
		buf.append(", organization = ");
		buf.append(DBClient.format(this.organization));
		buf.append(", org_role = ");
		buf.append(DBClient.format(this.orgRole));
		buf.append(", description = ");
		buf.append(DBClient.format(this.description));
		buf.append(", creation_date =");
		buf.append(DBClient.format(this.creationDate));
		buf.append(" where id=");
		buf.append(DBClient.format(id));
		try {
			factory.dbclient.executeUpdate(buf.toString());
		} catch (java.sql.SQLException e) {
			factory.reportError("DBPrincipalFactory.save", e);
			throw new ZenoException("DBException");
		}

	}
	
	//-----------------------------------------------
	
	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append("[");
		buf.append(getClass().getName());
		buf.append(" ");
		buf.append(this.id);
		buf.append(" (");
		buf.append(this.name);
		buf.append(")]");
		return buf.toString();
	}

	public void show(PrintWriter prw) {
		if (prw == null)
			prw = new PrintWriter(System.out, true);
		prw.println(getClass());
		prw.println("id " + this.id);
		prw.println("cn " + this.name);
		prw.println("mail " + this.email);
		prw.println("organization " + this.organization);
		prw.println("orgRole " + this.orgRole);
		prw.println("description " + this.description);
		prw.println("changed: " + changed);
		prw.println("-----------------");
	}

}