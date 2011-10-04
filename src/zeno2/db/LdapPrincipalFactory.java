package zeno2.db;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import zeno2.kernel.Community;
import zeno2.kernel.Group;
import zeno2.kernel.NotFoundException;
import zeno2.kernel.Principal;
import zeno2.kernel.ZenoException;

public class LdapPrincipalFactory implements PrincipalFactory {
						
	MonitorImpl monitor;
	LdapClient ldapclient;
	String username;
	String password;
	// 0 standard; 1 zenoUser; 2 ldapUser
	int type;

	public LdapPrincipalFactory(
		MonitorImpl monitor,
		String username,
		String password,
		int type)
		throws ZenoException {
		this.monitor = monitor;
		this.ldapclient = monitor.getLdapClient(username, password);
		this.username = username;
		this.password = password;
		this.type = type;
	}

	public void reportError(String op, Throwable e) {
		monitor.reportError(op + "  " + e);
	}

	public void reportError(String msg) {
		monitor.reportError("LdapPrincipalFactory " + msg);
	}
	
	public void register(String community)
			throws ZenoException {
	}
	
	public List getCommunitiesForRegistration(boolean isMember) throws ZenoException {
		return null;
	}
	
	public Principal registerAs(String community, 
									String uid, String name, String password)
			throws ZenoException {
		return null;
	}
	
	public Community createCommunity(String cid, String name)
		throws ZenoException {
		return null;
	}

	public Principal createUser(String community, 
									String uid, String name, String email)
		throws ZenoException {
		if (type == 1)
			return monitor.createUser(uid, name, email);
		else {
			BasicAttributes attrs = new BasicAttributes(true);
			attrs.put("uid", uid);
			attrs.put("cn", name);
			attrs.put("mail", email);
			attrs.put("userpassword", uid);
			attrs.put("objectclass", "person");
			attrs.put("admin", ldapclient.getdn(username));

			ldapclient.createPrincipal(uid, attrs);
			return new LdapPrincipalImpl(uid, this, attrs);
		}
	}
	
	public Principal createCollective(String community, 
									String uid, String name, String password)
			throws ZenoException {
		return null;
	}
	
	
	public Group createGroup(String community, String uid, String name)
	//public  Group createGroup(String uid, String name, Set members)
	throws ZenoException {

		if (type == 1)
			return monitor.createGroup(uid, name);
		else {
			BasicAttributes attrs = new BasicAttributes(true);
			attrs.put("uid", uid);
			attrs.put("cn", name);
			attrs.put("objectclass", "groupOfNames");
			attrs.put("admin", ldapclient.getdn(username));
			/*
			Attribute memberAttr = new BasicAttribute("member");
			Iterator it = members.iterator();
			while(it.hasNext()) {
				String member = (String)it.next();
				memberAttr.add(ldapclient.getdn(member));
			}
			attrs.put(memberAttr);
			*/
			ldapclient.createPrincipal(uid, attrs);
			return new LdapGroupImpl(uid, this, attrs);
		}
	}

	public Principal loadPrincipal(String uid) throws ZenoException {
		String[] attrids =
			{ "cn", "mail", "organization", "orgRole", "description", "objectclass" };
		Attributes attrs = ldapclient.getAttributes(uid, attrids);
		Principal principal = null;
		if (attrs != null) {
			String objectclass = LdapClient.getValue(attrs, "objectclass");
			if (objectclass.equals("groupOfNames")) {
				principal = new LdapGroupImpl(uid, this, attrs);
			} else
				if (objectclass.equals("person")) {
					principal = new LdapPrincipalImpl(uid, this, attrs);
				} else {
					reportError(uid + " principal with invalid objectclass");
					throw new NotFoundException("NoSuchPrincipal " + uid);
				}
		} else {
			throw new NotFoundException("NoSuchPrincipal " + uid);
		}
		return principal;
	}

	protected Principal loadPrincipal(Attributes attrs) throws ZenoException {
		//attrs must contain the attribute objectclass
		Principal principal;
		String objectclass = LdapClient.getValue(attrs, "objectclass");
		String uid = LdapClient.getValue(attrs, "uid");
		if (objectclass.equals("groupOfNames")) {
			principal = new LdapGroupImpl(uid, this, attrs);
		} else
			if (objectclass.equals("person")) {
				principal = new LdapPrincipalImpl(uid, this, attrs);
			} else {
				reportError(uid + " principal with invalid objectclass");
				throw new NotFoundException("NoSuchPrincipal " + uid);
			}
		return principal;
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
	

	public void save(String id, Attributes attrs) throws ZenoException {
		ldapclient.modifyAttributes(id, DirContext.REPLACE_ATTRIBUTE, attrs);
	}

	public String getProperty(String id, String community, String propertyName)
		throws ZenoException {
		try {
			String[] attrids = new String[1];
			attrids[0] = propertyName;
			Attributes attrs = ldapclient.getAttributes(id, attrids);
			Attribute attr = attrs.get(propertyName);
			if (attr != null)
				return (String) attr.get();
			else
				return null;
		} catch (javax.naming.NamingException e) {
			return "";
		}
	}

	public void setProperty(String id, String community, 
								String propertyName, String propertyValue)
		throws ZenoException {

		ldapclient.replaceAttribute(id, propertyName, propertyValue);
	}

	public void removeProperty(String id, String community, String propertyName)
		throws ZenoException {
		ldapclient.removeAttribute(id, propertyName);
	}
	
	public boolean checkClientInfo(String name, String clientInfo) 
		throws ZenoException {
		return true;
	}

	public boolean checkPassword(String name, String password)
		throws ZenoException {
		if (type == 2) {
			if (LdapClient.equals(name, this.username) && password.equals(this.password))
				return true;
			else
				return ldapclient.checkPassword(name, password);
		} else
			return monitor.checkPassword(name, password);
	}

	public void setPassword(String name, String oldPassword, String newPassword)
		throws ZenoException {
		if (checkPassword(name, oldPassword))
			ldapclient.changePassword(name, newPassword);
		else
			throw new zeno2.kernel.NoPermissionException("InvalidAuthentication");
	}

	public void setPassword(String name, String newPassword) throws ZenoException {
		ldapclient.changePassword(name, newPassword);
	}

	public void addMembers(String id, List list) throws ZenoException {
		BasicAttributes attrs = new BasicAttributes(true);
		Iterator it = list.iterator();
		while (it.hasNext()) {
			String cid = (String) it.next();
			attrs.put("member", ldapclient.getdn(cid));
		}
		ldapclient.modifyAttributes(
			ldapclient.getdn(id),
			DirContext.ADD_ATTRIBUTE,
			attrs);
	}

	public void removeMembers(String id, List list) throws ZenoException {
		BasicAttributes attrs = new BasicAttributes(true);
		Iterator it = list.iterator();
		while (it.hasNext()) {
			String cid = (String) it.next();
			attrs.put("member", ldapclient.getdn(cid));
		}
		ldapclient.modifyAttributes(
			ldapclient.getrdn(id),
			DirContext.REMOVE_ATTRIBUTE,
			attrs);
	}

	protected boolean hasObjectclass(String dn, String objectclass)
		throws ZenoException {
		try {
			dn = ldapclient.getdn(dn);
			String[] attrIds = { "objectclass" };
			Attributes attrs = ldapclient.getAttributes(dn, attrIds);
			String cobjclass = LdapClient.getValue(attrs, "objectclass");
			return cobjclass.equals(objectclass);
		} catch (NotFoundException e) {
			return false;
		}
	}

	protected boolean isPerson(String dn) throws ZenoException {
		return hasObjectclass(dn, "person");
	}

	protected boolean isGroup(String dn) throws ZenoException {
		return hasObjectclass(dn, "groupOfNames");
	}

	protected Vector filterObjectclass(Vector dns, String objectclass)
		throws ZenoException {
		Vector result = new Vector();
		String[] attrids = { "objectclass" };
		for (int i = 0; i < dns.size(); i++) {
			String cdn = (String) dns.elementAt(i);
			if (hasObjectclass(cdn, objectclass))
				result.addElement(cdn);
		}
		return result;
	}

	/**
	* returns direct members of id as a vector of dns
	* returns null for a person
	* throws a NotFoundException if id is unknown
	*
	*/

	public Vector getMembers(String id) throws ZenoException {

		String[] attrIds = { "member", "objectclass" };
		Attributes attrs = ldapclient.getAttributes(id, attrIds);
		String objectclass = LdapClient.getValue(attrs, "objectclass");
		if ("person".equals(objectclass))
			return null;
		else
			return LdapClient.getValueVector(attrs, "member");
	}

	public Vector getPersons(String id) throws ZenoException {
		Vector members = getMembers(id);
		return filterObjectclass(members, "person");
	}

	public Set getDirectMembers(String id, boolean personsOnly)
		throws ZenoException {
		Vector members = getMembers(id);
		if (personsOnly)
			members = filterObjectclass(members, "person");
		return ldapclient.getUidSet(members);
	}

	/**
	* adds direct and indirect members of dn to result
	* adds dn to persons if dn represents a person	
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

	/**
	* returns direct and indirect members of id as a set of uids
	*/

	public Set getAllMembers(String id, boolean personsOnly) throws ZenoException {
		Set result = new HashSet();
		Set persons = new HashSet();
		collectMembers(id, result, persons);
		if (personsOnly)
			return ldapclient.getUidSet(persons);
		else
			return ldapclient.getUidSet(result);
	}

	public boolean isDirectMember(String parentdn, String childdn)
		throws ZenoException {
		//returns true if memberdn is direct  member of rdn
		parentdn = ldapclient.getdn(parentdn);
		childdn = ldapclient.getdn(childdn);
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

		parentdn = ldapclient.getdn(parentdn);
		childdn = ldapclient.getdn(childdn);
		return isIndirectMember(parentdn, childdn, new Vector());
	}

	private boolean isIndirectMember(
		String parentdn,
		String childdn,
		Vector checked)
		throws ZenoException {
		//returns true if memberdn is direct or indirect member of rdn
		parentdn = ldapclient.getdn(parentdn);
		childdn = ldapclient.getdn(childdn);
		Vector members;
		try {
			members = getMembers(parentdn);
		} catch (NotFoundException e) {
			return false;
		}
		if (members == null)
			return false;
		else
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

	/** returns groups directly containing dn as a vector of dns */

	protected Vector getContainers(String dn) throws ZenoException {
		Vector result = new Vector();
		SearchControls ctrs = new SearchControls();
		ctrs.setSearchScope(SearchControls.ONELEVEL_SCOPE);
		String filter = "(member=" + dn + ")";
		NamingEnumeration srenum = ldapclient.search("", filter, ctrs);
		try {
			while (srenum.hasMore()) {
				SearchResult sr = (SearchResult) srenum.next();
				result.addElement(ldapclient.getdn(sr.getName()));
			}
			return result;
		} catch (javax.naming.NamingException e) {
			reportError("LdapPrincipalFactory", e);
			throw new ZenoException("LdapException");
		}
	}

	protected void collectContainers(String dn, Set result) throws ZenoException {
		//adds groups directly or indirectly containing dn to  result 
		//works recursively
		Vector containers = getContainers(dn);
		for (int i = 0; i < containers.size(); i++) {
			String cdn = (String) containers.elementAt(i);
			if (!result.contains(cdn)) {
				result.add(cdn);
				collectContainers(cdn, result);
			}
		}
	}

	public Set getGroups(String id, boolean directOnly) throws ZenoException {
		String dn = ldapclient.getdn(id);
		if (directOnly) {
			Vector containers = getContainers(dn);
			return ldapclient.getUidSet(containers);
		} else {
			Set result = new HashSet();
			collectContainers(dn, result);
			return ldapclient.getUidSet(result);
		}
	}

	public void removeFromAllGroups(String memberdn) throws ZenoException {

		List list = new ArrayList();
		list.add(memberdn);
		Vector containers = getContainers(memberdn);
		for (int i = 0; i < containers.size(); i++) {
			String groupdn = (String) containers.elementAt(i);
			removeMembers(groupdn, list);
		}
	}

	public void removePrincipal(String community, String uid) 
			throws ZenoException {
		if (type == 1)
			monitor.removePrincipal(uid);
		else {
			ldapclient.removePrincipal(uid);
			removeFromAllGroups(uid);

		}
	}
	
	public void removeCommunity(String uid) throws ZenoException {
	}

	public NamingEnumeration search(String query, int limit) throws ZenoException {
		SearchControls ctls = new SearchControls();
		ctls.setCountLimit(limit);
		StringTokenizer tokenizer = new StringTokenizer(query);
		StringBuffer filter = new StringBuffer();
		filter.append("(| ");
		while (tokenizer.hasMoreElements()) {
			String token = tokenizer.nextToken();
			String xtoken = token + "*" + ")";
			filter.append("(uid=");
			filter.append(xtoken);
			filter.append("(cn=");
			filter.append(xtoken);
			filter.append("(mail=");
			filter.append(xtoken);
		}
		filter.append(")");
		return ldapclient.search("", filter.toString(), ctls);
	}

	public Iterator searchPrincipals(String community, String query, String type) 
			throws ZenoException {
		List result = new ArrayList();
		NamingEnumeration srenum = search(query, 1000);
		try {
			while (srenum.hasMore()) {
				SearchResult sr = (SearchResult) srenum.next();
				Attributes attrs = sr.getAttributes();
				Principal principal = loadPrincipal(attrs);
				result.add(principal);
			}
			return result.iterator();
		} catch (javax.naming.NamingException e) {
			reportError("LdapPrincipalFactory", e);
			throw new ZenoException("LdapException");
		}
	}
	
	public boolean hasRole(String role, Object obj) throws ZenoException {
		return false;
	}

}