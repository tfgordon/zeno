package zeno2.db;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import zeno2.kernel.Article;
import zeno2.kernel.ArticleCollection;
import zeno2.kernel.Community;
import zeno2.kernel.CreationEvent;
import zeno2.kernel.Factory;
import zeno2.kernel.Group;
import zeno2.kernel.Journal;
import zeno2.kernel.NotFoundException;
import zeno2.kernel.Monitor;
import zeno2.kernel.Plugin;
import zeno2.kernel.Principal;
import zeno2.kernel.Topic;
import zeno2.kernel.ZenoException;
import zeno2.kernel.ZenoResource;
import zeno2.util.ZenoUtilities;

/**
 *  Description of the Class
 *
 *@author     oppor
 *@created    September 12, 2002
 */
public class FactoryImpl implements Factory {

	MonitorImpl monitor;
	DBClient dbclient;
	LdapClient ldapclient;
	PrincipalFactory prfactory;
	PermissionChecker checker;
	Principal user;
	String username;
	String zenoUserName;
	Set bag;
	static int DEFAULT_JOURNAL_ID = 1;


	/**
	 *  Constructor for the FactoryImpl object
	 *
	 *@param  monitor            Description of Parameter
	 *@param  username           Description of Parameter
	 *@param  password           Description of Parameter
	 *@param  clientInfo         Description of Parameter
	 *@exception  ZenoException  Description of Exception
	 */
	public FactoryImpl(MonitorImpl monitor,
			String username, String password, String clientInfo)
			 throws ZenoException {
		this.monitor = monitor;
		this.dbclient = monitor.getDBClient();
		//this.ldapclient = monitor.getLdapClient(username, password);
		this.prfactory = monitor.getPrincipalFactory(username, password, clientInfo);
		this.checker = monitor.getPermissionChecker(username);
		this.bag = new TreeSet();
		this.user = prfactory.loadPrincipal(username);
		this.username = username;
		//this.zenoUserName = zenoUserName;

	}


	/**
	 *  Gets the PermissionChecker attribute of the FactoryImpl object
	 *
	 *@return    The PermissionChecker value
	 */
	public PermissionChecker getPermissionChecker() {
		return this.checker;
	}


	/**
	 *  Gets the User attribute of the FactoryImpl object
	 *
	 *@return    The User value
	 */
	public Principal getUser() {
		return this.user;
	}


	/**
	 *  Gets the Bag attribute of the FactoryImpl object
	 *
	 *@return    The Bag value
	 */
	public Set getBag() {
		return this.bag;
	}


	/**
	 *  Gets the Home attribute of the FactoryImpl object
	 *
	 *@param  create             Description of Parameter
	 *@return                    The Home value
	 *@exception  ZenoException  Description of Exception
	 */
	public Journal getHome(boolean create) throws ZenoException {

		JournalImpl home = null;
		String uid = getUser().getId();
		StringBuffer buf = new StringBuffer();
		buf.append("select * from resource, journal where parent=2");
		buf.append(" and title= ");
		buf.append(DBClient.format(uid));
		buf.append(" and resource.id = journal.id");

		try {
			ResultSet rs = dbclient.executeQuery(buf.toString());
			if (rs.next()) {
				home = loadJournal(rs);
			}
			else if (create) {
				home = new JournalImpl(this);
				home.create(null);
				home.parentId = 2;
				home.title = uid;
				home.modified = true;
				home.save();
			}
		}
		catch (java.sql.SQLException e) {
			reportError("Factory.loadResource", e);
			throw new ZenoException("DatabaseException");
		}
		return home;
	}


	/**
	 *  Gets the Monitor attribute of the FactoryImpl object
	 *
	 *@return    The Monitor value
	 */
	public Monitor getMonitor() {
		return monitor;
	}

	// obsolete
	/**
	 *  Gets the ArticleCollections attribute of the FactoryImpl object
	 *
	 *@param  date               Description of Parameter
	 *@param  subscribedOnly     Description of Parameter
	 *@return                    The ArticleCollections value
	 *@exception  ZenoException  Description of Exception
	 */
	
	public List getArticleCollections(Date date, boolean subscribedOnly)
			throws ZenoException {
		List articles = loadNewArticles(date, subscribedOnly);
		List artCollections = 
			ArticleCollection.groupArticlesByJournal(articles);
		ArticleCollection.sort(artCollections, "byJournal");
		return artCollections;
	}
	
	/**
	 *  Gets the ArticleCollections attribute of the FactoryImpl object
	 *
	 *@param  date               Description of Parameter
	 *@param  mode               Description of Parameter
	 *@return                    The ArticleCollections value
	 *@exception  ZenoException  Description of Exception
	 */
	
	public List getArticleCollections(Date date, int mode, boolean newOnly)
			throws ZenoException {
		List articles = loadModifiedArticles(date, mode, newOnly);
		List artCollections = 
			ArticleCollection.groupArticlesByJournal(articles);
		ArticleCollection.sort(artCollections, "byJournal");
		return artCollections;
	}
	
	public List getArticleCollections(Date date, int mode)
			throws ZenoException {
		List articles = loadModifiedArticles(date, mode, true);
		List artCollections = 
			ArticleCollection.groupArticlesByJournal(articles);
		ArticleCollection.sort(artCollections, "byJournal");
		return artCollections;
	}


	/**
	 *  Gets the Topjournals attribute of the FactoryImpl object
	 *
	 *@return                    The Topjournals value
	 *@exception  ZenoException  Description of Exception
	 */
	public Set getTopjournals() throws ZenoException {
		try {
			checkPermission("Factory.getTopjournals", this);
			HashSet result = new HashSet();
			StringBuffer buf = new StringBuffer();
			buf.append("select * from resource where ");
			buf.append(" parent=0");
			buf.append(" and class='journal'");
			//buf.append(" and marked_for_deletion='false'");
			ResultSet rs = dbclient.executeQuery(buf.toString());
			while (rs.next()) {
				result.add(loadResource(rs));
			}
			return result;
		}
		catch (java.sql.SQLException e) {
			reportError("Journal.getSubjournals", e);
			throw new ZenoException("DataBaseException");
		}
	}


	/**
	 *  Gets the DefaultJournal attribute of the FactoryImpl object
	 *
	 *@return    The DefaultJournal value
	 */
	public int getDefaultJournal() {
		return 1;
	}


	/**
	 *  Gets the DefaultStyleSheet attribute of the FactoryImpl object
	 *
	 *@return    The DefaultStyleSheet value
	 */
	public String getDefaultStyleSheet() {
		// to do
		return "";
	}


	/**
	 *  Gets the DefaultJournalId attribute of the FactoryImpl object
	 *
	 *@return    The DefaultJournalId value
	 */
	public int getDefaultJournalId() {
		// to do: load id from config file
		return DEFAULT_JOURNAL_ID;
	}


	/**
	 *  Gets the MaxUploadSize attribute of the FactoryImpl object
	 *
	 *@return    The MaxUploadSize value
	 */
	public long getMaxUploadSize() {
		// to do
		return 2000000;
	}


	/**
	 *  Gets the CommunitiesForRegistration attribute of the FactoryImpl object
	 *
	 *@param  isMember           Description of Parameter
	 *@return                    The CommunitiesForRegistration value
	 *@exception  ZenoException  Description of Exception
	 */
	public List getCommunitiesForRegistration(boolean isMember) throws ZenoException {
		return prfactory.getCommunitiesForRegistration(isMember);
	}


	/**
	 *  Gets the ZenoAdmin attribute of the FactoryImpl object
	 *
	 *@return    The ZenoAdmin value
	 */
	public boolean isZenoAdmin() {
		return monitor.isZenoAdmin(username);
	}


	/**
	 *  Gets the PrincipalsInRole attribute of the FactoryImpl object
	 *
	 *@param  journalId          Description of Parameter
	 *@param  role               Description of Parameter
	 *@param  usersOnly          Description of Parameter
	 *@return                    The PrincipalsInRole value
	 *@exception  ZenoException  Description of Exception
	 */
	public Set getPrincipalsInRole(int journalId, String role, boolean usersOnly)
			 throws ZenoException {
		Set principals = new HashSet();
		List roleDefinition = loadRoleDefinition(journalId, role);
		collectPrincipalsInRole(principals, roleDefinition, usersOnly);
		return principals;
	}


	/**
	 *  Gets the Principals attribute of the FactoryImpl object
	 *
	 *@param  journalId          Description of Parameter
	 *@param  usersOnly          Description of Parameter
	 *@return                    The Principals value
	 *@exception  ZenoException  Description of Exception
	 */
	public Set getPrincipals(int journalId, boolean usersOnly)
			 throws ZenoException {
		Set principals = new HashSet();
		List[] roleDefinitions = loadAllRoleDefinitions(journalId);
		for (int i = 0; i < roleDefinitions.length; i++) {
			collectPrincipalsInRole(principals, roleDefinitions[i], usersOnly);
		}
		return principals;
	}


	/**
	 *  Returns a list of all known Plugin ids
	 *
	 *@return                    The PluginIds value
	 *@exception  ZenoException  Description of Exception
	 */
	public List getPluginIds() throws ZenoException {
		String query = "select id from plugin order by id";
		ResultSet rs = null;
		ArrayList pluginIds = new ArrayList();
		try {
			rs = dbclient.executeQuery(query);
			while (rs.next()) {
				pluginIds.add(rs.getString("id"));
			}
		}
		catch (SQLException e) {
			reportError("Factory.getPluginIds", e);
			throw new ZenoException("DatabaseException");
		}
		return pluginIds;
	}


	/**
	 *  loadResource loads all and only the generic properties of all resources.
	 *  The specific properties of subclasses are loaded only when the
	 *  ZenoResource.loadProperties method is called.
	 *
	 *@param  message  Description of Parameter
	 */

	public void reportError(String message) {
		monitor.reportError(username + "  " + message);
	}


	/**
	 *  Description of the Method
	 *
	 *@param  operation  Description of Parameter
	 *@param  e          Description of Parameter
	 */
	public void reportError(String operation, Throwable e) {
		monitor.reportError(username + "  " + operation + "  " + e);
	}


	/**
	 *  Description of the Method
	 *
	 *@param  operation  Description of Parameter
	 *@param  message    Description of Parameter
	 */
	public void reportError(String operation, String message) {
		monitor.reportError(username + "  " + operation + "  " + message);
	}


	/**
	 *  Description of the Method
	 *
	 *@param  id                 Description of Parameter
	 *@return                    Description of the Returned Value
	 *@exception  ZenoException  Description of Exception
	 */
	public ZenoResource loadResource(int id) throws ZenoException {
		try {
			StringBuffer buf = new StringBuffer();
			buf.append("select * from resource where id=");
			buf.append(DBClient.format(id));
			ResultSet rs = dbclient.executeQuery(buf.toString());
			ResourceImpl resource = null;

			if (rs.next()) {
				String zenoClass = rs.getString("class");
				if (zenoClass.equals("article")) {
					resource = new ArticleImpl(this, id);
					resource.fill(rs);
				}
				else if (zenoClass.equals("topic")) {
					resource = new TopicImpl(this, id);
					resource.fill(rs);
				}
				else if (zenoClass.equals("journal")) {
					resource = new JournalImpl(this, id);
					resource.fill(rs);
				}
				else {
					throw new ZenoException("NoSuchClass " + zenoClass);
				}
			}
			else {
				throw new NotFoundException("NoSuchResource " + id);
			}
			return resource;
		}
		catch (java.sql.SQLException e) {
			reportError("Factory.loadResource", e);
			throw new ZenoException("DatabaseException");
		}
	}


	/**
	 *  Description of the Method
	 *
	 *@param  id                 Description of Parameter
	 *@return                    Description of the Returned Value
	 *@exception  ZenoException  Description of Exception
	 */
	public ZenoResource loadResource(String id) throws ZenoException {
		int dbid = Integer.parseInt(id);
		return loadResource(dbid);
	}
	
	public List loadResources(List ids) throws ZenoException {
		// ids list of String od Integer
		
		List result = new ArrayList();
		if (ids.isEmpty())
			return result;
	
		try {
			StringBuffer buf = new StringBuffer();
			//buf.append("select * from resource");
			buf.append("select * from resource left join article");
			buf.append(" on resource.id = article.id");
			buf.append(" where resource.id in (");
			Iterator it = ids.iterator();
			while(it.hasNext()) {
				buf.append(it.next());
				if (it.hasNext()) 
					buf.append(",");
			}
			buf.append(")");
			ResultSet rs = dbclient.executeQuery(buf.toString());
			while(rs.next()) {
				ResourceImpl resource = loadResource(rs);
				if (resource instanceof ArticleImpl)
					((ArticleImpl)resource).loadProperties(rs);
				result.add(resource);
			}
			return result;
		} catch (java.sql.SQLException e) {
			reportError("Factory.loadResources", e);
			throw new ZenoException("DatabaseException");
		}
	}


	//obsolete
	/**
	 *  Description of the Method
	 *
	 *@param  date               Description of Parameter
	 *@param  subscribedOnly     Description of Parameter
	 *@return                    Description of the Returned Value
	 *@exception  ZenoException  Description of Exception
	 */
	public List loadNewArticles(Date date, boolean subscribedOnly) throws ZenoException {

		List result = new ArrayList();
		String jids = prfactory.getProperty(username, "system", "zeno2.subscribed");
		if ((jids == null || jids.equals("")) && subscribedOnly) {
			return result;
		}
		if (date == null) {
			date = ((DBPrincipalImpl) user).getLastLogin();
		}
		StringBuffer buf = new StringBuffer();
		buf.append("select * from resource, article ");
		buf.append("where creation_date >= ");
		buf.append(DBClient.format(date));
		if (subscribedOnly) {
			buf.append(" and parent in (");
			buf.append(jids);
			buf.append(") ");
		}
		buf.append(" and resource.id = article.id");
		try {
			ResultSet rs = dbclient.executeQuery(buf.toString());
			while (rs.next()) {
				Article cart = loadArticle(rs);
				if (hasRole("reader", cart)) {
					result.add(loadArticle(rs));
				}
			}
		}
		catch (SQLException e) {
			reportError("factory.loadNewArticles", e);
			throw new ZenoException("DataBaseException");
		}
		return result;
	}


	/**
	 *  Description of the Method
	 *
	 *@param  date               Description of Parameter
	 *@param  mode               Description of Parameter
	 *@return                    Description of the Returned Value
	 *@exception  ZenoException  Description of Exception
	 */
	public List loadModifiedArticles(Date date, int mode, boolean newOnly) 
			throws ZenoException {
		
		String jids = "";
		if (mode == 0) {
			//all
			return loadModifiedArticles(date, jids, newOnly);
		} else if (mode == 1) {
			//subsribed
			jids = prfactory.getProperty(username, "system", "zeno2.subscribed");
			if (jids == null || jids.equals(""))
				return Collections.EMPTY_LIST;
			else 
				return loadModifiedArticles(date, jids, newOnly);
		} else if (mode == 2) {
			//subscribed with descendants
			Set xsubsribed = getSubscribedWithDescendants();
			jids = ZenoUtilities.setToString(xsubsribed, ",");
			if (jids.equals(""))
				return Collections.EMPTY_LIST;
			else 
				return loadModifiedArticles(date, jids, newOnly);
		} else
			return Collections.EMPTY_LIST;
	}
	
	public List loadNewArticles(Date date, int mode)
			throws ZenoException {
		return loadModifiedArticles(date, mode, true);
	}
	
	/**
	 *  Description of the Method
	 *
	 *@param  text               Description of Parameter
	 *@return                    Description of the Returned Value
	 *@exception  ZenoException  Description of Exception
	 */
	public List searchJournals(String text) throws ZenoException {

		List result = new ArrayList();
		StringBuffer buf = new StringBuffer();
		buf.append("select * from resource , journal where ");
		buf.append(" resource.id=journal.id");

		buf.append(" and (resource.title like ");
		buf.append(DBClient.format(text));
		buf.append(" or resource.note like ");
		buf.append(DBClient.format(text));
		buf.append(")");
		/*
		 * buf.append(" and resource.title like ");
		 * buf.append(DBClient.format(text));
		 */
		buf.append(" order by title");
		try {
			ResultSet rs = dbclient.executeQuery(buf.toString());
			while (rs.next()) {
				Journal journal = loadJournal(rs);
				if (hasRole("reader", journal)) {
					result.add(journal);
				}
			}
			return result;
		}
		catch (java.sql.SQLException e) {
			reportError("Factory.journalSearch", e);
			throw new ZenoException("DataBaseException");
		}
	}


	/**
	 *  Description of the Method
	 *
	 *@return                    Description of the Returned Value
	 *@exception  ZenoException  Description of Exception
	 */
	public List loadSubscribedJournals() throws ZenoException {
		List journals = new ArrayList();
		String subscproperty = prfactory.getProperty(username, "system", "zeno2.subscribed");
		Set subscset = ZenoUtilities.stringToSet(subscproperty, ", ");
		Iterator setit = subscset.iterator();
		while (setit.hasNext()) {
			String id = (String) setit.next();
			try {
				ZenoResource resource = loadResource(Integer.parseInt(id));
				if (hasRole("reader", resource)) {
					journals.add(resource);
				}
			}
			catch (NotFoundException e) {
				//report??
			}
		}
		return journals;
	}


	/**
	 *  Description of the Method
	 *
	 *@return                    Description of the Returned Value
	 *@exception  ZenoException  Description of Exception
	 */
	public List loadMarkedJournals() throws ZenoException {

		List result = new ArrayList();
		StringBuffer buf = new StringBuffer();
		buf.append("select * from resource , journal where ");
		buf.append(" resource.id=journal.id");
		buf.append(" and marked_for_deletion='true'");
		buf.append(" order by title");
		try {
			ResultSet rs = dbclient.executeQuery(buf.toString());
			while (rs.next()) {
				Journal journal = loadJournal(rs);
				if (hasRole("editor", journal)) {
					result.add(journal);
				}
			}
			return result;
		}
		catch (java.sql.SQLException e) {
			reportError("Factory.loadMarkedJournals", e);
			throw new ZenoException("DataBaseException");
		}
	}


	/**
	 *  Description of the Method
	 *
	 *@param  role               Description of Parameter
	 *@param  unsubsribedOnly    Description of Parameter
	 *@return                    Description of the Returned Value
	 *@exception  ZenoException  Description of Exception
	 */
	public List loadAccessibleJournals(String role, boolean unsubsribedOnly)
			 throws ZenoException {
		String subscproperty = prfactory.getProperty(username, "system", "zeno2.subscribed");
		Set subscset = ZenoUtilities.stringToSet(subscproperty, ", ");
		StringBuffer buf = new StringBuffer();
		buf.append("select id, title from resource where class=");
		buf.append(DBClient.format("journal"));
		buf.append(" order by title");
		List result = new ArrayList();
		try {
			ResultSet rs = dbclient.executeQuery(buf.toString());
			while (rs.next()) {
				int id = rs.getInt("id");
				if ((!unsubsribedOnly || !subscset.contains(Integer.toString(id)))
						 && hasRole(role, new Integer(id))) {
					result.add(loadResource(id));
				}
			}
		}
		catch (SQLException e) {
			reportError("factory.loadAccessibleJournals", e);
			throw new ZenoException("DataBaseException");
		}
		return result;
	}

	// Added Code Zak **********************************

		public void setNotify(int id, int mode) throws ZenoException  {
	String subscProperty = "";
	Set subscSet = null;
	if (mode == 1) {
			subscProperty = prfactory.getProperty(username, "system", "zeno2.dailyNotify");
			subscSet = ZenoUtilities.stringToSet(subscProperty, ", ");
			subscSet.add(Integer.toString(id));
			subscProperty = ZenoUtilities.setToString(subscSet, ", ");
			prfactory.setProperty(username, "system",  "zeno2.dailyNotify", subscProperty);
			if ((prfactory.getProperty(username, "system", "zeno2.lastDailyNotify")) == null)
		prfactory.setProperty(username, "system", "zeno2.lastDailyNotify", "0");
	}
	else if (mode == 2) {
			subscProperty = prfactory.getProperty(username, "system", "zeno2.weeklyNotify");
			subscSet = ZenoUtilities.stringToSet(subscProperty, ", ");
			subscSet.add(Integer.toString(id));
			subscProperty = ZenoUtilities.setToString(subscSet, ", ");
			prfactory.setProperty(username, "system",  "zeno2.weeklyNotify", subscProperty);
			if ((prfactory.getProperty(username, "system", "zeno2.lastWeeklyNotify")) == null)
		prfactory.setProperty(username, "system", "zeno2.lastWeeklyNotify", "0");
	}
	else if (mode == 3) {
			subscProperty = prfactory.getProperty(username, "system", "zeno2.monthlyNotify");
			subscSet = ZenoUtilities.stringToSet(subscProperty, ", ");
			subscSet.add(Integer.toString(id));
			subscProperty = ZenoUtilities.setToString(subscSet, ", ");
			prfactory.setProperty(username, "system",  "zeno2.monthlyNotify", subscProperty);
			if ((prfactory.getProperty(username, "system", "zeno2.lastMonthlyNotify")) == null)
		prfactory.setProperty(username, "system", "zeno2.lastMonthlyNotify", "0");
	}
		}

		public void unNotify(int id, int mode) throws ZenoException  {
	
	String subscProperty = "";
	Set subscSet = null;

	if (mode == 1) {
			subscProperty = prfactory.getProperty(username, "system", "zeno2.dailyNotify");
			subscSet = ZenoUtilities.stringToSet(subscProperty, ", ");
			subscSet.remove(Integer.toString(id));
			subscProperty = ZenoUtilities.setToString(subscSet, ", ");
			prfactory.setProperty(username, "system",  "zeno2.dailyNotify", subscProperty);
	}
	else if (mode == 2) {
			subscProperty = prfactory.getProperty(username, "system", "zeno2.weeklyNotify");
			subscSet = ZenoUtilities.stringToSet(subscProperty, ", ");
			subscSet.remove(Integer.toString(id));
			subscProperty = ZenoUtilities.setToString(subscSet, ", ");
			prfactory.setProperty(username, "system",  "zeno2.weeklyNotify", subscProperty);
	}
	else if (mode == 3) {
			subscProperty = prfactory.getProperty(username, "system", "zeno2.monthlyNotify");
			subscSet = ZenoUtilities.stringToSet(subscProperty, ", ");
			subscSet.remove(Integer.toString(id));
			subscProperty = ZenoUtilities.setToString(subscSet, ", ");
			prfactory.setProperty(username, "system",  "zeno2.monthlyNotify", subscProperty);
	}
		}


		public List loadSubscribedJournalsForNotification(int mode) throws ZenoException {
	List journals = new ArrayList();
	String subscproperty = "";
	if (mode == 1)
			subscproperty =  prfactory.getProperty(username, "system", "zeno2.dailyNotify");
	else if (mode == 2)	   
			subscproperty = prfactory.getProperty(username, "system", "zeno2.weeklyNotify");
	else if (mode == 3)
			subscproperty = prfactory.getProperty(username, "system", "zeno2.monthlyNotify");

	Set subscset = ZenoUtilities.stringToSet(subscproperty, ", ");
	Iterator setit = subscset.iterator();
	while (setit.hasNext()) {
			String id = (String) setit.next();
			try {
		ZenoResource resource = loadResource(Integer.parseInt(id));
		if (hasRole("reader", resource)) {
				journals.add(resource);
		}
			}
			catch (NotFoundException e) {
		//report??
			}
	}
	return journals;
		}

		//*************************************

	/**
	 *  Description of the Method
	 *
	 *@param  id                 Description of Parameter
	 *@exception  ZenoException  Description of Exception
	 */
	public void subscribeJournal(int id) throws ZenoException {

		String subscProperty = prfactory.getProperty(username, "system", "zeno2.subscribed");
		Set subscSet = ZenoUtilities.stringToSet(subscProperty, ", ");
		subscSet.add(Integer.toString(id));
		subscProperty = ZenoUtilities.setToString(subscSet, ", ");
		prfactory.setProperty(username, "system", "zeno2.subscribed", subscProperty);
	}


	/**
	 *  Description of the Method
	 *
	 *@param  id                 Description of Parameter
	 *@exception  ZenoException  Description of Exception
	 */
	public void unsubscribeJournal(int id) throws ZenoException {

		String subscProperty = prfactory.getProperty(username, "system", "zeno2.subscribed");
		Set subscSet = ZenoUtilities.stringToSet(subscProperty, ", ");
		subscSet.remove(Integer.toString(id));
		subscProperty = ZenoUtilities.setToString(subscSet, ", ");
		prfactory.setProperty(username, "system", "zeno2.subscribed", subscProperty);
	}


	/**
	 *  Description of the Method
	 *
	 *@param  authorId           Description of Parameter
	 *@param  title              Description of Parameter
	 *@param  articleLabel       Description of Parameter
	 *@param  qualifier          Description of Parameter
	 *@param  fromDate           Description of Parameter
	 *@param  toDate             Description of Parameter
	 *@param  fullText           Description of Parameter
	 *@param  order              Description of Parameter
	 *@return                    Description of the Returned Value
	 *@exception  ZenoException  Description of Exception
	 */
	public List searchArticles(
			String authorId,
			String title,
			String articleLabel,
			String qualifier,
			Date fromDate,
			Date toDate,
			String fullText,
			String order)
			 throws ZenoException {

		try {

			StringBuffer buf = new StringBuffer();
			buf.append("select * from resource, article where ");
			buf.append(" resource.id=article.id");
			buf.append(" and marked_for_deletion='false'");
			if (authorId != null) {
				buf.append(" and author like ");
				buf.append(DBClient.format(authorId));
			}
			if (title != null) {
				buf.append(" and title like ");
				buf.append(DBClient.format(title));
			}
			if (articleLabel != null) {
				buf.append(" and label like ");
				buf.append(DBClient.format(articleLabel));
			}
			if (qualifier != null) {
				buf.append(" and qualifier like ");
				buf.append(DBClient.format(qualifier));
			}
			if (fromDate != null) {
				buf.append(" and creation_date >= ");
				buf.append(DBClient.format(fromDate));
			}
			if (toDate != null) {
				buf.append(" and creation_date <= ");
				buf.append(DBClient.format(toDate));
			}

			if (fullText != null) {
				buf.append(" and (note like ");
				buf.append(DBClient.format(fullText));
				buf.append(" or title like ");
				buf.append(DBClient.format(fullText));
				buf.append(" or keywords like ");
				buf.append(DBClient.format(fullText));
				buf.append(")");
			}

			if (order != null && !order.equals("")) {
				buf.append(" order by parent," + order);
			}
			else {
				buf.append(" order by parent");
			}

			List articles = new ArrayList();
			ResultSet rs = dbclient.executeQuery(buf.toString());
			while (rs.next()) {
				Article cart = loadArticle(rs);
				if (hasRole("reader", cart)) {
					articles.add(cart);
				}
			}
			List artCollections = 
				ArticleCollection.groupArticlesByJournal(articles);
			ArticleCollection.sort(artCollections, "byJournal");	
			return artCollections;
		}
		catch (java.sql.SQLException e) {
			reportError("Factory.searchArticles", e);
			throw new ZenoException("DataBaseException");
		}
	}


	/**
	 *  Description of the Method
	 *
	 *@param  parent             Description of Parameter
	 *@return                    Description of the Returned Value
	 *@exception  ZenoException  Description of Exception
	 */
	public Article createArticle(Journal parent) throws ZenoException {
		try {
			// to do: begin transaction
			checkPermission("Factory.createArticle", parent);
			ArticleImpl article = new ArticleImpl(this);
			article.create(parent);
			NumberHandler.setNextNumber(article);

			if (parent != null) {
				((JournalImpl) parent).modified();
			}
			monitor.reportEvent(
					new CreationEvent(
					new Integer(article.id), user.getId(), article.creationDate));

			return article;
		}
		catch (ZenoException e) {
			// to do: rollback
			throw e;
		}
	}


	/**
	 *  Description of the Method
	 *
	 *@param  topic              Description of Parameter
	 *@return                    Description of the Returned Value
	 *@exception  ZenoException  Description of Exception
	 */
	public Article createArticle(Topic topic) throws ZenoException {
		try {
			// to do: begin transaction
			checkPermission("Factory.createArticle", topic);
			Journal parent = (Journal) topic.getParent();
			checkPermission("Factory.createArticle", parent);
			ArticleImpl article = new ArticleImpl(this);
			article.part = topic.getId();
			article.create(parent);
			NumberHandler.setNextNumber(article);

			if (topic != null) {
				((TopicImpl) topic).modified();
				((JournalImpl) parent).modified();
			}
			monitor.reportEvent(
					new CreationEvent(
					new Integer(article.id), user.getId(), article.creationDate));

			return article;
		}
		catch (ZenoException e) {
			// to do: rollback
			throw e;
		}
	}


	/**
	 *  Description of the Method
	 *
	 *@param  parent             Description of Parameter
	 *@return                    Description of the Returned Value
	 *@exception  ZenoException  Description of Exception
	 */
	public Article createTopic(Journal parent) throws ZenoException {
		try {
			// to do: begin transaction
			checkPermission("Factory.createTopic", parent);
			TopicImpl topic = new TopicImpl(this);
			topic.create(parent);
			NumberHandler.setNextNumber(topic);

			if (parent != null) {
				((JournalImpl) parent).modified();
			}
			monitor.reportEvent(
					new CreationEvent(
					new Integer(topic.id), user.getId(), topic.creationDate));

			return topic;
		}
		catch (ZenoException e) {
			// to do: rollback
			throw e;
		}
	}


	/**
	 *  If the parent is null, a root journal is created. checkPermission is
	 *  responsible for deciding whether the user has permission to create a root
	 *  journal.
	 *
	 *@param  parent             Description of Parameter
	 *@return                    Description of the Returned Value
	 *@exception  ZenoException  Description of Exception
	 */

	public Journal createJournal(Journal parent) throws ZenoException {
		try {
			checkPermission("Factory.createJournal", parent);
			JournalImpl journal = new JournalImpl(this);
			// to do: begin transaction
			journal.create(parent);
			//if (parent != null)
			//	journal.replaceRoleDefinition(parent, null, false);
			if (parent != null) {
				((JournalImpl) parent).modified();
			}
			monitor.reportEvent(
					new CreationEvent(
					new Integer(journal.id), user.getId(), journal.creationDate));
			return journal;
		}
		catch (ZenoException e) {
			// to do: rollback transaction
			throw e;
		}
	}


	/**
	 *  Description of the Method
	 *
	 *@param  type               Description of Parameter
	 *@param  reference          Description of Parameter
	 *@param  name               Description of Parameter
	 *@return                    Description of the Returned Value
	 *@exception  ZenoException  Description of Exception
	 */
	public List findXLinks(String type, String reference, String name)
			 throws ZenoException {
		StringBuffer buf = new StringBuffer();
		buf.append("select * from xlink where");
		buf.append(" type like ");
		buf.append(DBClient.format(type));
		if (reference != null && !reference.equals("")) {
			buf.append(" and reference like ");
			buf.append(DBClient.format(reference));

		}
		if (name != null && !name.equals("")) {
			buf.append(" and name like ");
			buf.append(DBClient.format(name));

		}
		return XLinkImpl.getXLinksWhere(this, buf.toString());
	}


	/**
	 *  Description of the Method
	 *
	 *@param  role               Description of Parameter
	 *@param  object             Description of Parameter
	 *@return                    Description of the Returned Value
	 *@exception  ZenoException  Description of Exception
	 */
	public boolean hasRole(String role, Object object) throws ZenoException {
		return checker.hasRole(role, object) || prfactory.hasRole(role, object);
	}

	//------------------------- ldap stuff ----------------------------------

	/**
	 *  Description of the Method
	 *
	 *@param  uid                Description of Parameter
	 *@return                    Description of the Returned Value
	 *@exception  ZenoException  Description of Exception
	 */
	public Principal loadPrincipal(String uid) throws ZenoException {
		if (uid != null && uid.equals(user.getId()))
			return user;
		else 
			return prfactory.loadPrincipal(uid);
	}


	/**
	 *  Description of the Method
	 *
	 *@param  community          Description of Parameter
	 *@param  query              Description of Parameter
	 *@param  type               Description of Parameter
	 *@return                    Description of the Returned Value
	 *@exception  ZenoException  Description of Exception
	 */
	public Iterator searchPrincipals(String community, String query, String type)
			 throws ZenoException {
		return prfactory.searchPrincipals(community, query, type);
	}


	/**
	 *  Description of the Method
	 *
	 *@param  community          Description of Parameter
	 *@exception  ZenoException  Description of Exception
	 */
	public void register(String community) throws ZenoException {
		prfactory.register(community);
	}


	/**
	 *  Description of the Method
	 *
	 *@param  community          Description of Parameter
	 *@param  uid                Description of Parameter
	 *@param  name               Description of Parameter
	 *@param  pwd                Description of Parameter
	 *@return                    Description of the Returned Value
	 *@exception  ZenoException  Description of Exception
	 */
	public Principal registerAs(String community,
			String uid, String name, String pwd)
			 throws ZenoException {
		return prfactory.registerAs(community, uid, name, pwd);
	}


	/**
	 *  Description of the Method
	 *
	 *@param  cid                Description of Parameter
	 *@param  name               Description of Parameter
	 *@return                    Description of the Returned Value
	 *@exception  ZenoException  Description of Exception
	 */
	public Community createCommunity(String cid, String name)
			 throws ZenoException {
		return prfactory.createCommunity(cid, name);
	}


	/**
	 *  Description of the Method
	 *
	 *@param  community          Description of Parameter
	 *@param  uid                Description of Parameter
	 *@param  name               Description of Parameter
	 *@param  email              Description of Parameter
	 *@return                    Description of the Returned Value
	 *@exception  ZenoException  Description of Exception
	 */
	public Principal createUser(String community,
			String uid, String name, String email)
			 throws ZenoException {
		return prfactory.createUser(community, uid, name, email);
	}


	/**
	 *  Description of the Method
	 *
	 *@param  community          Description of Parameter
	 *@param  uid                Description of Parameter
	 *@param  name               Description of Parameter
	 *@param  password           Description of Parameter
	 *@return                    Description of the Returned Value
	 *@exception  ZenoException  Description of Exception
	 */
	public Principal createCollective(String community,
			String uid, String name, String password)
			 throws ZenoException {
		return prfactory.createCollective(community, uid, name, password);
	}


	/**
	 *  Description of the Method
	 *
	 *@param  community          Description of Parameter
	 *@param  uid                Description of Parameter
	 *@param  name               Description of Parameter
	 *@return                    Description of the Returned Value
	 *@exception  ZenoException  Description of Exception
	 */
	public Group createGroup(String community, String uid, String name)
			 throws ZenoException {
		return prfactory.createGroup(community, uid, name);
	}


	/**
	 *  Description of the Method
	 *
	 *@param  community          Description of Parameter
	 *@param  uid                Description of Parameter
	 *@exception  ZenoException  Description of Exception
	 */
	public void removePrincipal(String community, String uid) throws ZenoException {
		prfactory.removePrincipal(community, uid);
	}


	/**
	 *  Description of the Method
	 *
	 *@param  cid                Description of Parameter
	 *@exception  ZenoException  Description of Exception
	 */
	public void removeCommunity(String cid) throws ZenoException {
		prfactory.removeCommunity(cid);
	}


	/**
	 *  Description of the Method
	 *
	 *@param  journalId          Description of Parameter
	 *@return                    Description of the Returned Value
	 *@exception  ZenoException  Description of Exception
	 */
	public List[] loadAllRoleDefinitions(int journalId) throws ZenoException {

		List readers = new ArrayList();
		List writers = new ArrayList();
		List editors = new ArrayList();
		try {
			String query =
					"select * from role " + " where journal =" + DBClient.format(journalId);
			ResultSet rs = dbclient.executeQuery(query);
			while (rs.next()) {
				String role = rs.getString("role_name");
				if ("reader".equals(role)) {
					readers.add(rs.getString("principal"));
				}
				else
						if ("writer".equals(role)) {
					writers.add(rs.getString("principal"));
				}
				else
						if ("editor".equals(role)) {
					editors.add(rs.getString("principal"));
				}
				else {
					//ignored other roles
					reportError("Factory.loadAllRoleDefinitions", "unknown role: " + role);
				}
			}
		}
		catch (java.sql.SQLException e) {
			reportError("Factory.loadAllRoleDefinitions", e);
			throw new ZenoException("DatabaseException");
		}
		List[] result = new List[3];
		result[0] = readers;
		result[1] = writers;
		result[2] = editors;
		return result;
	}


	/**
	 *  Description of the Method
	 *
	 *@param  journalId          Description of Parameter
	 *@param  roledefs           Description of Parameter
	 *@exception  ZenoException  Description of Exception
	 */
	public void replaceRoleDefinition(int journalId, Object[][] roledefs)
			 throws ZenoException {

		StringBuffer buf = new StringBuffer();
		buf.append("delete from role where journal =");
		buf.append(DBClient.format(journalId));
		buf.append(" and (");
		for (int i = 0; i < roledefs.length; i++) {
			if (i > 0) {
				buf.append(" or ");
			}
			buf.append("role_name = ");
			buf.append(DBClient.format((String) roledefs[i][0]));
		}
		buf.append(")");

		try {
			dbclient.executeUpdate(buf.toString());
		}
		catch (SQLException e) {
			reportError("Factory.replaceRoleDefinition", e);
			new ZenoException("DatabaseException");
		}

		for (int j = 0; j < roledefs.length; j++) {
			List principals = (List) roledefs[j][1];
			Iterator it = principals.iterator();

			while (it.hasNext()) {
				String principal = (String) it.next();
				String request =
						"insert into role (role_name, journal, principal)"
						 + " values( "
						 + DBClient.format((String) roledefs[j][0])
						 + ", "
						 + DBClient.format(journalId)
						 + ", "
						 + DBClient.format(principal)
						 + ")";
				try {
					dbclient.executeUpdate(request);
				}
				catch (SQLException e) {
					reportError("Factory.replaceRoleDefinitions", e);
					new ZenoException("DatabaseException");
				}
			}
		}
	}


	/**
	 *  Adds a feature to the PrincipalToRole attribute of the FactoryImpl object
	 *
	 *@param  journalId          The feature to be added to the PrincipalToRole
	 *      attribute
	 *@param  principal          The feature to be added to the PrincipalToRole
	 *      attribute
	 *@param  roleName           The feature to be added to the PrincipalToRole
	 *      attribute
	 *@exception  ZenoException  Description of Exception
	 */
	public void addPrincipalToRole(
			int journalId,
			String principal,
			String roleName)
			 throws ZenoException {

		List roledefinition = loadRoleDefinition(journalId, roleName);
		if (roledefinition.contains(principal)) {
			return;
		}
		String request =
				"insert into role (role_name, journal, principal)"
				 + " values( "
				 + DBClient.format(roleName)
				 + ", "
				 + DBClient.format(journalId)
				 + ", "
				 + DBClient.format(principal)
				 + ")";
		try {
			dbclient.executeUpdate(request);
		}
		catch (SQLException e) {
			reportError("Factory.addPrincipalToRole", e);
			new ZenoException("DatabaseException");
		}
	}


	/**
	 *  Adds a feature to the PrincipalToRole attribute of the FactoryImpl object
	 *
	 *@param  journalId          The feature to be added to the PrincipalToRole
	 *      attribute
	 *@param  roleName           The feature to be added to the PrincipalToRole
	 *      attribute
	 *@exception  ZenoException  Description of Exception
	 */
	public void addPrincipalToRole(int journalId, String roleName)
			 throws ZenoException {
		addPrincipalToRole(journalId, user.getId(), roleName);
	}


	/**
	 *  Description of the Method
	 *
	 *@param  journalId          Description of Parameter
	 *@param  principal          Description of Parameter
	 *@param  roleName           Description of Parameter
	 *@exception  ZenoException  Description of Exception
	 */
	public void deletePrincipalFromRole(
			int journalId,
			String principal,
			String roleName)
			 throws ZenoException {
		String request =
				"delete from role  where "
				 + " journal = "
				 + DBClient.format(journalId)
				 + " and principal ="
				 + DBClient.format(principal)
				 + " and role_name ="
				 + DBClient.format(roleName);
		try {
			dbclient.executeUpdate(request);
		}
		catch (SQLException e) {
			reportError("Factory.deletePrincipalFromRole", e);
			new ZenoException("DatabaseException");
		}
	}


	/**
	 *  Description of the Method
	 *
	 *@param  journalId          Description of Parameter
	 *@param  principal          Description of Parameter
	 *@param  role               Description of Parameter
	 *@return                    Description of the Returned Value
	 *@exception  ZenoException  Description of Exception
	 */
	public boolean hasRole(int journalId, String principal, String role)
			 throws ZenoException {
		List roleDefinition = loadRoleDefinition(journalId, role);
		return hasRole(roleDefinition, principal);
	}


	/**
	 *  Loads a Plugin Attribute set from the Database Table Plugin
	 *
	 *@param  id                 Description of Parameter
	 *@return                    Description of the Returned Value
	 *@exception  ZenoException  Description of Exception
	 */
	public Plugin loadPlugin(String id) throws ZenoException {
		String query = "select * from plugin where id=" + DBClient.format(id);
		try {
			ResultSet rs = dbclient.executeQuery(query);
			return new PluginImpl(this, rs);
		}
		catch (SQLException e) {
			reportError("Factory.loadPlugin", e);
			throw new ZenoException("DatabaseException");
		}
	}


	/**
	 *  Creates a <em>new</em> Plugin Attribute Set
	 *
	 *@param  id                 Description of Parameter
	 *@return                    Description of the Returned Value
	 *@exception  ZenoException  Description of Exception
	 */
	public Plugin createPlugin(String id) throws ZenoException {
		return new PluginImpl(this, id);
	}


	/**
	 *  Gets the PrincipalFactory attribute of the FactoryImpl object
	 *
	 *@return    The PrincipalFactory value
	 */
	protected PrincipalFactory getPrincipalFactory() {
		return this.prfactory;
	}


	/**
	 *  Gets the DBClient attribute of the FactoryImpl object
	 *
	 *@return    The DBClient value
	 */
	protected DBClient getDBClient() {
		return this.dbclient;
	}


	/**
	 *  Gets the LdapClient attribute of the FactoryImpl object
	 *
	 *@return    The LdapClient value
	 */
	protected LdapClient getLdapClient() {
		return this.ldapclient;
	}


	/**
	 *  Gets the SubscribedWithDescendants attribute of the FactoryImpl object
	 *
	 *@return                    The SubscribedWithDescendants value
	 *@exception  ZenoException  Description of Exception
	 */
	protected Set getSubscribedWithDescendants() throws ZenoException {
		Set journalids = new HashSet();
		List subscribed = loadSubscribedJournals();
		Iterator it = subscribed.iterator();
		while (it.hasNext()) {
			collectDescendants((Journal) it.next(), journalids);
		}
		return journalids;
	}


	/**
	 *  Description of the Method
	 *
	 *@param  rs                 Description of Parameter
	 *@return                    Description of the Returned Value
	 *@exception  ZenoException  Description of Exception
	 */
	protected ResourceImpl loadResource(ResultSet rs) throws ZenoException {
		try {
			ResourceImpl resource = null;
			String zenoClass = rs.getString("class");
			if (zenoClass.equals("article")) {
				resource = new ArticleImpl(this);
				resource.fill(rs);
			}
			else if (zenoClass.equals("topic")) {
				resource = new TopicImpl(this);
				resource.fill(rs);
			}
			else if (zenoClass.equals("journal")) {
				resource = new JournalImpl(this);
				resource.fill(rs);
			}
			else {
				throw new ZenoException("NoSuchClass " + zenoClass);
			}
			return resource;
		}
		catch (java.sql.SQLException e) {
			reportError("Factory.loadResource", e);
			throw new ZenoException("DatabaseException");
		}
	}


	/**
	 *  Description of the Method
	 *
	 *@param  rs                 Description of Parameter
	 *@return                    Description of the Returned Value
	 *@exception  ZenoException  Description of Exception
	 */
	protected ArticleImpl loadArticle(ResultSet rs) throws ZenoException {
		try {
			ArticleImpl article = null;
			String zenoClass = rs.getString("class");
			if (zenoClass.equals("article")) {
				article = new ArticleImpl(this);
				article.fill(rs);
				article.loadProperties(rs);
			}
			else if (zenoClass.equals("topic")) {
				article = new TopicImpl(this);
				article.fill(rs);
				article.loadProperties(rs);
			}
			return article;
		}
		catch (java.sql.SQLException e) {
			reportError("Factory.loadArticle", e);
			throw new ZenoException("DatabaseException");
		}
	}


	/**
	 *  Returns articles created after date and not marked for deletion from the
	 *  journals specified by jids, a komma separated list of ids
	 *
	 *@param  date               Description of Parameter
	 *@param  jids               Description of Parameter
	 *@return                    Description of the Returned Value
	 *@exception  ZenoException  Description of Exception
	 */

	protected List loadModifiedArticles(Date date, String jids, boolean newOnly) 
			throws ZenoException {
		
		List result = new ArrayList();
		if (date == null)
			date = ((DBPrincipalImpl)user).getLastLogin();
		StringBuffer buf = new StringBuffer();
		buf.append("select * from resource, article");
		if (newOnly)
			buf.append(" where creation_date >= ");
		else
			buf.append(" where modification_date >= ");
		buf.append(DBClient.format(date));
		buf.append(" and marked_for_deletion='false'");
		if (jids != null && !jids.equals("")) {
			buf.append(" and parent in (");
			buf.append(jids);
			buf.append(") ");
		}
		buf.append(" and resource.id = article.id");
		if (newOnly)
			buf.append(" order by parent, creation_date desc");
		else
			buf.append(" order by parent, modification_date desc");
		try {
			ResultSet rs = dbclient.executeQuery(buf.toString());
			while(rs.next()) {
				Article cart = loadArticle(rs);
				if (hasRole("reader", cart))
					result.add(cart);
			}
		} catch(SQLException e) {
			reportError("factory.loadModifiedArticles", e);
			throw new ZenoException ("DataBaseException");
		}
		return result;
	}
	


	/**
	 *  Collects the ids of jn and all its descandants in a set
	 *
	 *@param  jn          Description of Parameter
	 *@param  journalids  Description of Parameter
	 */

	protected void collectDescendants(Journal jn, Set journalids) {
		String idstr = Integer.toString(jn.getId());
		journalids.add(idstr);
		try {
			Set subjournals = ((JournalImpl) jn).getSubjournals(false);
			Iterator it = subjournals.iterator();
			while (it.hasNext()) {
				collectDescendants((Journal) it.next(), journalids);
			}
		}
		catch (ZenoException e) {
		}
	}


	/**
	 *  Description of the Method
	 *
	 *@param  rs                 Description of Parameter
	 *@return                    Description of the Returned Value
	 *@exception  ZenoException  Description of Exception
	 */
	protected JournalImpl loadJournal(ResultSet rs) throws ZenoException {

		try {
			JournalImpl journal = null;
			String zenoClass = rs.getString("class");
			if (zenoClass.equals("journal")) {
				journal = new JournalImpl(this);
				journal.fill(rs);
				journal.loadProperties(rs);
			}
			return journal;
		}
		catch (java.sql.SQLException e) {
			reportError("Factory.loadJournal", e);
			throw new ZenoException("DatabaseException");
		}
	}


	/**
	 *  Checks whether this user has permission to execute the given method on the
	 *  object. The method names are prefixed by their interface name in the
	 *  zeno.kernel package. Example: "ZenoResource.createArticle"
	 *
	 *@param  method             Description of Parameter
	 *@param  object             Description of Parameter
	 *@exception  ZenoException  Description of Exception
	 */

	protected void checkPermission(String method, Object object)
			 throws ZenoException {
		checker.checkPermission(method, object);
	}

	//---------------------------------------------------
	
	public void setCurrentRole(int jnid, String role) throws ZenoException {
		if (checker instanceof DefaultChecker)
			((DefaultChecker)checker).setCurrentRole(jnid, role);
	}
	
	public String getCurrentRole(int jnid) throws ZenoException {
		if (checker instanceof DefaultChecker)
			return ((DefaultChecker)checker).getCurrentRole(jnid);
		else
			return "editor";
	}
	
	public List getValidRoles(int jnid) throws ZenoException {
		if (checker instanceof DefaultChecker)
			return ((DefaultChecker)checker).getValidRoles(jnid);
		else {
			List list = new ArrayList();
			list.add("editor");
			return list;
		}
	}

	/**
	 *  Description of the Method
	 *
	 *@param  journalId          Description of Parameter
	 *@param  role               Description of Parameter
	 *@return                    Description of the Returned Value
	 *@exception  ZenoException  Description of Exception
	 */
	protected List loadRoleDefinition(int journalId, String role)
			 throws ZenoException {

		String query =
				"select * from role "
				 + " where journal ="
				 + DBClient.format(journalId)
				 + " and role_name="
				 + DBClient.format(role);

		List roleDefinition = new ArrayList();
		try {
			ResultSet rs = dbclient.executeQuery(query);
			while (rs.next()) {
				roleDefinition.add(rs.getString("principal"));
			}
		}
		catch (java.sql.SQLException e) {
			reportError("Factory.loadRoleDefinition", e);
			throw new ZenoException("DatabaseException");
		}
		return roleDefinition;
	}


	/**
	 *  Description of the Method
	 *
	 *@param  roleDefinition     Description of Parameter
	 *@param  principal          Description of Parameter
	 *@return                    Description of the Returned Value
	 *@exception  ZenoException  Description of Exception
	 */
	protected boolean hasRole(List roleDefinition, String principal)
			 throws ZenoException {
		if (roleDefinition.contains("any"))
			return true;
		if (roleDefinition.contains(principal)) {
			return true;
		}
		Iterator it = roleDefinition.iterator();
		while (it.hasNext()) {
			String cprincipal = (String) it.next();
			if (prfactory.isIndirectMember(cprincipal, principal)) {
				return true;
			}
		}
		return false;
	}


	/**
	 *  Description of the Method
	 *
	 *@param  principals         Description of Parameter
	 *@param  roleDefinition     Description of Parameter
	 *@param  usersOnly          Description of Parameter
	 *@exception  ZenoException  Description of Exception
	 */
	protected void collectPrincipalsInRole(
			Set principals,
			List roleDefinition,
			boolean usersOnly)
			 throws ZenoException {
		Iterator it = roleDefinition.iterator();
		while (it.hasNext()) {
			String id = (String) it.next();
			Set members = prfactory.getAllMembers(id, usersOnly);
			principals.addAll(members);
		}
	}
	

}

