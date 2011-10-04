package zeno2.db;

import java.io.PrintWriter;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.Vector;

import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;
import zeno2.kernel.Principal;
import zeno2.kernel.ZenoException;

public class LdapPrincipalImpl implements Principal {
	String id;
	String name = "";
	String email = "";
	String organization = "";
	String orgRole;
	String description;

	boolean changed = false;
	LdapPrincipalFactory factory;

	public LdapPrincipalImpl(String id, PrincipalFactory factory) {
		this.id = id;
		this.factory = (LdapPrincipalFactory)factory;
	}

	public LdapPrincipalImpl(
		String id,
		PrincipalFactory factory,
		Attributes attrs) {
		this.id = id;
		this.name = LdapClient.getValue(attrs, "cn");
		this.email = LdapClient.getValue(attrs, "mail");
		this.organization = LdapClient.getValue(attrs, "organization");
		this.orgRole = LdapClient.getValue(attrs, "orgRole");
		this.description = LdapClient.getValue(attrs, "description");
		this.factory = (LdapPrincipalFactory)factory;
	}

	public String getId() {
		return id;
	}

	public String getName() {
		if (!name.equals(""))
			return name;
		else
			return id;
	}

	public void setName(String name) {
		if (!this.name.equals(name)) {
			this.name = name;
			changed = true;
		}
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		if (!this.email.equals(email)) {
			this.email = email;
			changed = true;
		}
	}

	public String getOrganization() {
		return organization;
	}

	public void setOrganization(String organization) {
		if (!this.organization.equals(organization)) {
			this.organization = organization;
			changed = true;
		}
	}
	
	public String getCreator() {
		return "";
	}
	
	public Date getCreationDate() {
		return null;
	}

	public String getOrgRole() {
		return orgRole;
	}

	public void setOrgRole(String orgRole) {
		if (!this.orgRole.equals(orgRole)) {
			this.orgRole = orgRole;
			changed = true;
		}
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		if (!this.description.equals(description)) {
			this.description = description;
			changed = true;
		}
	}
		
	
	public List getPropertyNamesUsed(String community) throws ZenoException {
		return null;
	}

	public String getProperty(String community, String propertyName) throws ZenoException {
		return factory.getProperty(this.id, community, propertyName);
	}

	public void setProperty(String community, String propertyName, String propertyValue)
		throws ZenoException {
		factory.setProperty(this.id, community, propertyName, propertyValue);
	}

	public void removeProperty(String community, String propertyName) throws ZenoException {
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

	public boolean checkPassword(String password) throws ZenoException {
		return factory.checkPassword(this.id, password);
	}

	public void setPassword(String newPassword) throws ZenoException {
		factory.setPassword(this.id, newPassword);
	}

	public List getGroups(boolean directOnly) throws ZenoException {
		Set groups = factory.getGroups(this.id, directOnly);
		return new ArrayList(groups);
	}
	
	public List getCommunities() throws ZenoException {
		return null;
	}

	public void save() throws ZenoException {
		if (!changed)
			return;

		BasicAttributes attrs = new BasicAttributes(true);
		attrs.put("cn", this.name);
		attrs.put("mail", this.email);
		attrs.put("organization", this.organization);
		attrs.put("orgRole", this.orgRole);
		attrs.put("description", this.description);
		factory.save(this.id, attrs);

	}
	
	public void leaveCommunity(String community) throws ZenoException {
	}
	
	public void leaveGroup(String group) throws ZenoException {
	}

	//-----------------------------------------------

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