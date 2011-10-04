package zeno2.db;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.Properties;
import java.util.Vector;

import zeno2.kernel.Factory;
import zeno2.kernel.Group;
import zeno2.kernel.Monitor;
import zeno2.kernel.NoPermissionException;
import zeno2.kernel.NotFoundException;
import zeno2.kernel.Principal;
import zeno2.kernel.ZenoEvent;
import zeno2.kernel.ZenoEventListener;
import zeno2.kernel.ZenoException;

/**
 *  Zeno2 Monitor Implementation.
 *
 *@author     oppor
 *@created    September 12, 2002
 */

public class MonitorImpl implements zeno2.kernel.Monitor {

	DBClient dbclient;
	String dbServer = "localhost";
	String dbName = "zenodb";

	LdapClient ldapclient;
	//String ldapServer = "zeno.gmd.de";
	String ldapServer = "";
	String ldapBase = "dc=zeno";

	//uid not dn
	String zenoUserName = "nn";
	String domainName = "localhost";
	String webApplication = "zeno2";

	Properties env = new Properties();
	PrincipalFactory prfactory;
	PrintWriter errorprw;
	SesamChecker sesamChecker = new SesamChecker();
	Vector listenerRunners;
	private String dbUserName = "mysql";
	private String dbPassword = "mysql";
	private String ldapUserName = "uid=root,dc=zeno";
	private String ldapPassword = "ad%min";
	private String zenoPassword = "nn";


	/**
	 *  Constructor for the MonitorImpl object
	 */
	public MonitorImpl() {
		listenerRunners = new Vector();
	}


	/**
	 *  Sets the ErrorWriter attribute of the MonitorImpl object
	 *
	 *@param  prw  The new ErrorWriter value
	 */
	public void setErrorWriter(PrintWriter prw) {
		if (this.errorprw != null) {
			this.errorprw.flush();
		}
		this.errorprw = prw;
	}


	/**
	 *  Gets the Property attribute of the MonitorImpl object
	 *
	 *@param  key       Description of Parameter
	 *@param  defvalue  Description of Parameter
	 *@return           The Property value
	 */
	public String getProperty(String key, String defvalue) {
		return env.getProperty(key, defvalue);
	}


	/**
	 *  Gets the DomainName attribute of the MonitorImpl object
	 *
	 *@return    The DomainName value
	 */
	public String getDomainName() {
		return domainName;
	}


	/**
	 *  Gets the WebApplication attribute of the MonitorImpl object
	 *
	 *@return    The WebApplication value
	 */
	public String getWebApplication() {
		return webApplication;
	}


	/**
	 *  Adds a feature to the ZenoEventListener attribute of the MonitorImpl object
	 *
	 *@param  listener  The feature to be added to the ZenoEventListener attribute
	 */
	public void addZenoEventListener(ZenoEventListener listener) {

		ListenerRunner runner = new ListenerRunner(listener);
		synchronized (listenerRunners) {
			listenerRunners.add(runner);
		}
	}


	/**
	 *  Description of the Method
	 *
	 *@param  listener  Description of Parameter
	 */
	public void removeZenoEventListener(ZenoEventListener listener) {
		synchronized (listenerRunners) {
			for (int i = 0; i < listenerRunners.size(); i++) {
				ListenerRunner runner = (ListenerRunner) listenerRunners.elementAt(i);
				if (runner.isRunnerFor(listener)) {
					listenerRunners.removeElementAt(i);
				}
			}
		}
	}


	/**
	 *  Configure the monitor by loading a Java property file with the given
	 *  pathname from the local file system.
	 *
	 *@param  pathname           Description of Parameter
	 *@exception  ZenoException  Description of Exception
	 */

	public void configure(String pathname) throws ZenoException {

		File file = new File(pathname);
		try {
			configure(new FileInputStream(file));
		}
		catch (FileNotFoundException e) {
			throw new NotFoundException("NoSuchFile " + pathname);
		}
	}
	
	public void configure(String pathname, boolean startTasks) 
			throws ZenoException {
	
		File file = new File(pathname);
		try {
			configure(new FileInputStream(file));
		} catch(FileNotFoundException e) {
			throw new NotFoundException("NoSuchFile " + pathname);
		}
		Housekeeper keeper;
		long period = 0;
		if (startTasks) {
			
			//hide task handles expiration dates
			try {
				period = Long.parseLong(env.getProperty("hide.period"));
				keeper = new Housekeeper(this, "hide", period * 60000);
				keeper.start();
			} catch(NumberFormatException e) {
				reportError("invalid period for hide task");
			} 	
			//close task handles revision periods
			try {
				period = Long.parseLong(env.getProperty("close.period"));
				keeper = new Housekeeper(this, "close", period * 60000);
				keeper.start();
			} catch(NumberFormatException e) {
				reportError("invalid period for close task");
			} 
		}
	}
	


	/**
	 *  Description of the Method
	 *
	 *@param  stream             Description of Parameter
	 *@exception  ZenoException  Description of Exception
	 */
	public void configure(InputStream stream) throws ZenoException {

		try {
			env.load(stream);
		}
		catch (IOException e) {
			throw new ZenoException(e.getMessage());
		}

		dbServer = env.getProperty("dbServer");
		dbName = env.getProperty("dbName");
		domainName = env.getProperty("domainName");
		dbUserName = env.getProperty("dbUserName");
		dbPassword = env.getProperty("dbPassword");
		env.remove("dbUserName");
		env.remove("dbPassword");

		ldapServer = env.getProperty("ldapServer", ldapServer);
		ldapBase = env.getProperty("ldapBase", ldapBase);
		ldapUserName = env.getProperty("ldapUserName", ldapUserName);
		ldapPassword = env.getProperty("ldapPassword", ldapPassword);
		env.remove("ldapUserName");
		env.remove("ldapPassword");

		zenoUserName = LdapClient.getUid(env.getProperty("zenoUserName", zenoUserName));
		zenoPassword = env.getProperty("zenoPassword", zenoPassword);
		domainName = env.getProperty("domainName", domainName);
		webApplication = env.getProperty("webApplication", webApplication);
		env.remove("zenoPassword");

		this.dbclient = new DBClient(this, dbServer, dbName, dbUserName, dbPassword);

		if (!ldapServer.equals("")) {
			this.ldapclient =
					new LdapClient(this, ldapServer, ldapBase, ldapUserName, ldapPassword);
			this.prfactory = new LdapPrincipalFactory(this, ldapUserName, ldapPassword, 2);
		}
		else {
			this.prfactory =
					new DBPrincipalFactory(this, zenoUserName, zenoPassword, "");
		}

		if (!prfactory.checkPassword(zenoUserName, zenoPassword)) {
			throw new NoPermissionException("invalid zenoUser");
		}
		
		NumberHandler.configure(this);

		return;
	}


	/**
	 *  Description of the Method
	 *
	 *@param  username           Description of Parameter
	 *@param  password           Description of Parameter
	 *@param  clientInfo         Description of Parameter
	 *@return                    Description of the Returned Value
	 *@exception  ZenoException  Description of Exception
	 */
	public Factory login(String username, String password, String clientInfo)
			 throws ZenoException {
		if (username == null) {
			username = "guest";
		}
		if (username.equals("")) {
			username = "guest";
		}
		if (password == null) {
			password = "";
		}

		if (!checkClientInfo(username, clientInfo)) {
			throw new NoPermissionException("InvalidClient");
		}
		if (username.equals("guest") || checkPassword(username, password)) {
			Factory factory = new FactoryImpl(this, username, password, clientInfo);
			Principal user = factory.getUser();
			((DBPrincipalImpl) user).getLastLogin();
			if (!username.equals("guest")) {
				String time = Long.toString(new Date().getTime());
				prfactory.setProperty(username, "system", "lastLogin", time);
			}
			return factory;
		}
		else {
			throw new NoPermissionException("InvalidAuthentication");
		}
	}


	/**
	 *  Description of the Method
	 *
	 *@param  username           Description of Parameter
	 *@param  password           Description of Parameter
	 *@return                    Description of the Returned Value
	 *@exception  ZenoException  Description of Exception
	 */
	public Factory login(String username, String password)
			 throws ZenoException {
		return login(username, password, "ipadr=localhost");
	}


	/**
	 *  Reports a event, causing it to be propogated to all registered listeners
	 *  for this type of event
	 *
	 *@param  event  Description of Parameter
	 */

	public void reportEvent(ZenoEvent event) {
		for (int i = 0; i < listenerRunners.size(); i++) {
			ListenerRunner runner = (ListenerRunner) listenerRunners.elementAt(i);
			runner.putEvent(event);
		}
	}


	/**
	 *  Gets the DBClient attribute of the MonitorImpl object
	 *
	 *@return    The DBClient value
	 */
	protected DBClient getDBClient() {
		return dbclient;
	}


	/**
	 *  Gets the LdapClient attribute of the MonitorImpl object
	 *
	 *@return    The LdapClient value
	 */
	protected LdapClient getLdapClient() {
		return ldapclient;
	}


	/**
	 *  Gets the LdapClient attribute of the MonitorImpl object
	 *
	 *@param  userName           Description of Parameter
	 *@param  password           Description of Parameter
	 *@return                    The LdapClient value
	 *@exception  ZenoException  Description of Exception
	 */
	protected LdapClient getLdapClient(String userName, String password)
			 throws ZenoException {
		LdapClient personalLdapClient;
		if (ldapclient.getdn(userName).equals(ldapUserName)
				 && password.equals(ldapPassword)) {
			personalLdapClient = ldapclient;
		}
		else {
			personalLdapClient =
					new LdapClient(this, ldapServer, ldapBase, userName, password);
		}
		return personalLdapClient;
	}


	/**
	 *  Gets the PrincipalFactory attribute of the MonitorImpl object
	 *
	 *@return    The PrincipalFactory value
	 */
	protected PrincipalFactory getPrincipalFactory() {
		return prfactory;
	}


	/**
	 *  Gets the PrincipalFactory attribute of the MonitorImpl object
	 *
	 *@param  userName           Description of Parameter
	 *@param  password           Description of Parameter
	 *@param  clientInfo         Description of Parameter
	 *@return                    The PrincipalFactory value
	 *@exception  ZenoException  Description of Exception
	 */
	protected PrincipalFactory getPrincipalFactory(String userName,
			String password,
			String clientInfo)
			 throws ZenoException {
		PrincipalFactory prfactory;
		if (ldapclient != null) {
			int type = prfactoryType(userName, password);
			prfactory = new LdapPrincipalFactory(this, userName, password, type);
		}
		else {
			prfactory = new DBPrincipalFactory(this, userName, password, clientInfo);
		}
		return prfactory;
	}


	/**
	 *  Gets the PermissionChecker attribute of the MonitorImpl object
	 *
	 *@param  userName  Description of Parameter
	 *@return           The PermissionChecker value
	 */
	protected PermissionChecker getPermissionChecker(String userName) {

		if (LdapClient.equals(userName, zenoUserName)) {
			return sesamChecker;
		}
		else {
			return new DefaultChecker(this, userName);
		}
	}



	/**
	 *  Gets the ZenoAdmin attribute of the MonitorImpl object
	 *
	 *@param  userName  Description of Parameter
	 *@return           The ZenoAdmin value
	 */
	protected boolean isZenoAdmin(String userName) {
		return LdapClient.equals(userName, zenoUserName);
	}


	/**
	 *  Description of the Method
	 *
	 *@exception  Throwable  Description of Exception
	 */
	protected void finalize() throws Throwable {
		if (errorprw != null) {
			errorprw.flush();
		}
		super.finalize();
	}


	/**
	 *  Description of the Method
	 *
	 *@param  username           Description of Parameter
	 *@param  password           Description of Parameter
	 *@return                    Description of the Returned Value
	 *@exception  ZenoException  Description of Exception
	 */
	protected boolean checkPassword(String username, String password)
			 throws ZenoException {
		return prfactory.checkPassword(username, password);
	}


	/**
	 *  Description of the Method
	 *
	 *@param  username           Description of Parameter
	 *@param  clientInfo         Description of Parameter
	 *@return                    Description of the Returned Value
	 *@exception  ZenoException  Description of Exception
	 */
	protected boolean checkClientInfo(String username, String clientInfo)
			 throws ZenoException {
		return prfactory.checkClientInfo(username, clientInfo);
	}


	/**
	 *  Description of the Method
	 *
	 *@param  uid                Description of Parameter
	 *@param  name               Description of Parameter
	 *@param  email              Description of Parameter
	 *@return                    Description of the Returned Value
	 *@exception  ZenoException  Description of Exception
	 */
	protected Principal createUser(String uid, String name, String email)
			 throws ZenoException {
		return prfactory.createUser(uid, name, email, "");
	}


	/**
	 *  Description of the Method
	 *
	 *@param  uid                Description of Parameter
	 *@param  name               Description of Parameter
	 *@return                    Description of the Returned Value
	 *@exception  ZenoException  Description of Exception
	 */
	protected Group createGroup(String uid, String name) throws ZenoException {
		return prfactory.createGroup(uid, name, "");
	}


	/**
	 *  Description of the Method
	 *
	 *@param  uid                Description of Parameter
	 *@exception  ZenoException  Description of Exception
	 */
	protected void removePrincipal(String uid) throws ZenoException {
		prfactory.removePrincipal("", uid);
	}


	/**
	 *  Description of the Method
	 *
	 *@param  msg  Description of Parameter
	 */
	protected void reportError(String msg) {
		if (errorprw != null) {
			errorprw.print(DBClient.format(new Date()));
			errorprw.print("  ");
			errorprw.println(msg);
		}
	}
	
	protected void reportError(String operation, Throwable e) {
		if (errorprw != null) {
			errorprw.print(DBClient.format(new Date()));
			errorprw.print("  ");
			errorprw.print(operation);
			errorprw.print("  ");
			errorprw.print(e);
		}
	}

	/**
	 *  Description of the Method
	 *
	 *@param  userName  Description of Parameter
	 *@param  password  Description of Parameter
	 *@return           Description of the Returned Value
	 */
	private int prfactoryType(String userName, String password) {
		if (LdapClient.equals(userName, ldapUserName) && password.equals(ldapPassword)) {
			return 2;
		}
		else
				if (LdapClient.equals(userName, zenoUserName) && password.equals(zenoPassword)) {
			return 1;
		}
		else {
			return 0;
		}
	}
	
	public void startTask( String pwd, String operation, long period) {
		if (zenoPassword.equals(pwd)) {
			Housekeeper keeper = new Housekeeper(this, operation, period);
			reportError(operation + " started");
			keeper.start();
		} else {
			reportError("no permission to start " + operation);
		}
	}

}

