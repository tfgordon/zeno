package zeno2.db;

import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Vector;

import javax.naming.AuthenticationException;
import javax.naming.CommunicationException;
import javax.naming.Context;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.NoPermissionException;
import javax.naming.directory.Attribute;
import javax.naming.directory.AttributeInUseException;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.NoSuchAttributeException;
import javax.naming.directory.SearchControls;
import zeno2.kernel.NameInUseException;
import zeno2.kernel.ZenoException;

public class LdapClient {
	MonitorImpl monitor;
	DirContext topctx;
	String ctxdn = "dc=zeno";
	Hashtable ldapEnv;
	String userName;

	class UidEnumerator implements Enumeration {
		Vector dns;
		int index = 0;

		UidEnumerator(Vector dns) {
			this.dns = dns;
		}

		public boolean hasMoreElements() {
			if (dns == null)
				return false;
			else
				return index < dns.size();
		}

		public Object nextElement() {
			if (dns == null)
				return null;
			else
				return getuid((String) dns.elementAt(index++));
		}
	}

	LdapClient(
		MonitorImpl monitor,
		String ldapServer,
		String ldapBase,
		String userName,
		String password)
		throws ZenoException {
		this.monitor = monitor;
		this.ctxdn = ldapBase;
		this.userName = getuid(userName);

		ldapEnv = new Hashtable();
		ldapEnv.put(
			Context.INITIAL_CONTEXT_FACTORY,
			"com.sun.jndi.ldap.LdapCtxFactory");
		ldapEnv.put(Context.PROVIDER_URL, "ldap://" + ldapServer);
		ldapEnv.put(Context.SECURITY_AUTHENTICATION, "simple");
		ldapEnv.put(Context.SECURITY_PRINCIPAL, getdn(userName));
		ldapEnv.put(Context.SECURITY_CREDENTIALS, password);
		try {
			DirContext initctx = new InitialDirContext(ldapEnv);
			topctx = (DirContext) initctx.lookup(ldapBase);
		} catch (javax.naming.AuthenticationException e) {
			reportError("LdapClient.constuctor", e);
			throw new zeno2.kernel.NoPermissionException("InvalidAuthentication");
		} catch (NamingException e) {
			reportError("LdapClient.constuctor", e);
			throw new ZenoException(e.getMessage());
		}
	}

	LdapClient(DirContext ctx, String ctxdn) {
		this.topctx = ctx;
		this.ctxdn = ctxdn;
	}

	public static String getUid(String id) {
		int index = id.indexOf(",");
		if (index != -1)
			id = id.substring(0, index);
		if (id.startsWith("uid="))
			return id.substring(4);
		else
			return id;
	}

	public static boolean equals(String id1, String id2) {
		return getUid(id1).equals(getUid(id2));
	}

	public String getdn(String id) {
		int index = id.indexOf(",");
		if (index != -1)
			return id;
		else
			if (id.startsWith("uid="))
				return id + "," + ctxdn;
			else
				return "uid=" + id + "," + ctxdn;
	}

	public String getrdn(String id) {
		int index = id.indexOf(",");
		if (index != -1)
			return id.substring(0, index);
		else
			if (id.startsWith("uid="))
				return id;
			else
				return "uid=" + id;
	}

	public String getuid(String id) {
		String rdn = getrdn(id);
		return rdn.substring(4);
	}

	public Vector getuids(Vector dns) {
		Vector result = new Vector();
		for (int i = 0; i < dns.size(); i++) {
			String rdn = getrdn((String) dns.elementAt(i));
			result.addElement(rdn.substring(4));
		}
		return result;
	}

	public Enumeration getUidEnumeration(Vector dns) {
		return new UidEnumerator(dns);
	}

	public Set getUidSet(Vector dns) {
		Set result = new HashSet();
		for (int i = 0; i < dns.size(); i++) {
			String uid = getuid((String) dns.elementAt(i));
			result.add(uid);
		}
		return result;
	}

	public Set getUidSet(Set dns) {
		Set result = new HashSet();
		Iterator it = dns.iterator();
		while (it.hasNext()) {
			String uid = getuid((String) it.next());
			result.add(uid);
		}
		return result;
	}

	public static void showAttributes(Attributes attrs, PrintWriter prw)
		throws Exception {

		if (prw == null)
			prw = new PrintWriter(System.out, true);
		NamingEnumeration enum = attrs.getAll();
		while (enum.hasMore()) {
			Attribute attr = (Attribute) enum.next();
			prw.println("attribute: " + attr.getID());
			NamingEnumeration values = attr.getAll();
			while (values.hasMore()) {
				Object val = values.next();
				prw.println(val);
			}
		}
		prw.println("--------------");
	}

	public static String getValue(Attributes attrs, String attrId) {
		//useful for a single valued attribute only
		try {
			Attribute attr = attrs.get(attrId);
			return (attr != null) ? (String) attr.get() : "";
		} catch (NamingException e) {
			return "";
		} catch (java.util.NoSuchElementException e) {
			return "";
		}
	}

	public static Set getValueSet(Attributes attrs, String attrId) {
		Set result = new HashSet();
		try {
			Attribute attr = attrs.get(attrId);
			if (attr != null) {
				NamingEnumeration enum = attr.getAll();
				while (enum.hasMore()) {
					String value = (String) enum.next();
					result.add(value);
				}
			}
			return result;
		} catch (NamingException e) {
			return result;
		}
	}

	public static Vector getValueVector(Attributes attrs, String attrId) {
		//useful for a multible valued attribute only
		Vector result = new Vector();
		try {
			Attribute attr = attrs.get(attrId);
			if (attr != null) {
				NamingEnumeration enum = attr.getAll();
				while (enum.hasMore()) {
					String value = (String) enum.next();
					result.addElement(value);
				}
			}
			return result;
		} catch (NamingException e) {
			return result;
		}
	}

	public static String[] attributesToArray(Attributes attrs, String[] attrIds) {
		//useful for single valued attributes only
		String[] array = new String[attrIds.length];
		for (int i = 0; i < attrIds.length; i++) {
			array[i] = getValue(attrs, attrIds[i]);
		}
		return array;
	}

	public void reportError(String message) {
		monitor.reportError(userName + "  " + message);
	}

	public void reportError(Throwable e) {
		monitor.reportError(userName + "  " + e);
	}

	public void reportError(String operation, Throwable e) {
		monitor.reportError(userName + "  " + operation + "  " + e);
	}

	//--------------------------------------------------------

	public Attributes getAttributes(String id, String[] attrIds)
		throws zeno2.kernel.ZenoException {

		int cnr = 0;
		while (true) {
			try {
				if (attrIds == null)
					return topctx.getAttributes(getrdn(id));
				else
					return topctx.getAttributes(getrdn(id), attrIds);
			} catch (CommunicationException e1) {
				if (cnr++ < 5);
				//reconfigure();
				else {
					reportError("LdapClient.getAttributes", e1);
					throw new zeno2.kernel.ZenoException("LdapException");
				}
			} catch (NameNotFoundException e2) {
				reportError("LdapClient.getAttributes", e2);
				throw new zeno2.kernel.NotFoundException("NoSuchPrincipal " + getuid(id));
			} catch (NamingException e3) {
				reportError("LdapClient.getAttributes", e3);
				throw new zeno2.kernel.ZenoException("LdapException");
			}
		}
	}

	/** possible values fo op:
	* DirContext.REPLACE_ATTRIBUTE, 
	*	DirContext.ADD_ATTRIBUTE, 
	*	DirContext.REMOVE_ATTRIBUTE
	*/

	public void modifyAttributes(String id, int op, Attributes attrs)
		throws ZenoException {

		int cnr = 0;
		while (true) {
			try {
				topctx.modifyAttributes(getrdn(id), op, attrs);
				return;
			} catch (CommunicationException e1) {
				if (cnr++ < 5);
				//reconfigure();
				else
					reportError("LdapClient.modifytAttributes", e1);
				throw new zeno2.kernel.ZenoException("LdapException");
			} catch (NameNotFoundException e2) {
				reportError("LdapClient.getAttributes", e2);
				throw new zeno2.kernel.NotFoundException("NoSuchPrincipal " + getuid(id));
			} catch (AttributeInUseException e3) {
				reportError("LdapClient.modifytAttributes", e3);
				return;
			} catch (NoSuchAttributeException e4) {
				reportError("LdapClient.modifytAttributes", e4);
				return;
			} catch (javax.naming.NoPermissionException e5) {
				reportError("LdapClient.modifytAttributes", e5);
				throw new zeno2.kernel.NoPermissionException("NoPermisssion " + getuid(id));
			} catch (NamingException e6) {
				reportError("LdapClient.modifytAttributes", e6);
				throw new ZenoException("LdapException");
			}
		}
	}

	public void replaceAttribute(String id, String attribute, String value)
		throws ZenoException {
		BasicAttributes attrs = new BasicAttributes(attribute, value, true);
		modifyAttributes(id, DirContext.REPLACE_ATTRIBUTE, attrs);
	}

	public void removeAttribute(String id, String attribute) throws ZenoException {
		BasicAttributes attrs = new BasicAttributes(attribute, null, true);
		modifyAttributes(id, DirContext.REMOVE_ATTRIBUTE, attrs);
	}

	public void addAttributeValue(String id, String attribute, String value)
		throws ZenoException {
		BasicAttributes attrs = new BasicAttributes(attribute, value, true);
		modifyAttributes(getrdn(id), DirContext.ADD_ATTRIBUTE, attrs);
	}

	public void removeAttributeValue(String id, String attribute, String value)
		throws ZenoException {

		BasicAttributes attrs = new BasicAttributes(attribute, value, true);
		modifyAttributes(getrdn(id), DirContext.REMOVE_ATTRIBUTE, attrs);
	}

	public boolean checkPassword(String name, String password)
		throws ZenoException {
		try {
			BasicAttributes matchAttrs = new BasicAttributes(true);
			matchAttrs.put("objectclass", "person");
			matchAttrs.put("uid", getuid(name));
			matchAttrs.put("userpassword", password);
			Enumeration enum = topctx.search("", matchAttrs);
			return enum.hasMoreElements();
		} catch (javax.naming.NameNotFoundException e1) {
			reportError("LdapClient.checkPassword", e1);
			return false;
		} catch (NamingException e2) {
			reportError("LdapClient.checkPassword", e2);
			throw new ZenoException("Ldap Exception");
		}
	}

	public void changePassword(String name, String newpwd) throws ZenoException {
		BasicAttributes attrs = new BasicAttributes("userpassword", newpwd, true);
		modifyAttributes(name, DirContext.REPLACE_ATTRIBUTE, attrs);
	}

	public void createPrincipal(String uid, Attributes attrs)
		throws ZenoException {
		try {
			topctx.createSubcontext(getrdn(uid), attrs);
		} catch (NameAlreadyBoundException e) {
			throw new NameInUseException("PrincipalExists " + uid);
		} catch (javax.naming.NoPermissionException e) {
			throw new zeno2.kernel.NoPermissionException("NoPermission createPrincipal");
		} catch (NamingException e2) {
			reportError("LdapClient.createPrincipal", e2);
			throw new ZenoException("LdapException");
		}
	}

	public void removePrincipal(String uid) throws ZenoException {
		try {
			topctx.unbind(getrdn(uid));
		} catch (javax.naming.NoPermissionException e) {
			throw new zeno2.kernel.NoPermissionException("NoPermission removePrincipal");
		} catch (NamingException e2) {
			reportError("LdapClient.removePrincipal", e2);
			throw new ZenoException("LdapException");
		}
	}

	public NamingEnumeration search(String rdn, String filter, SearchControls ctr)
		throws ZenoException {

		int cnr = 0;
		while (true) {
			try {
				return topctx.search(rdn, filter, ctr);
			} catch (CommunicationException e1) {
				if (cnr++ < 5);
				//reconfigure();
				else {
					reportError("LdapClient.search", e1);
					throw new zeno2.kernel.ZenoException("LdapException");
				}
			} catch (NamingException e) {
				reportError("LdapClient.search", e);
				throw new zeno2.kernel.ZenoException("LdapException");
			}
		}
	}

}