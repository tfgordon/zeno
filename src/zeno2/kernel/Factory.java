package zeno2.kernel;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Date;

/**
 *  Zeno 2 Factory. A Factory is used to make instances of
 *  zeno2.kernel.ZenoResource for a particular, authenticated user. A factory is
 *  associated with a session and reused to make all the resources needed by the
 *  user during the session.
 *
 *@author     T. Gordon
 *@created    September 12, 2002
 */

public interface Factory {

	/**
	 *  Returns the logged in user.
	 *
	 *@return    The User value
	 */

	public Principal getUser();


	/**
	 *  returns whether the user logged in is the zeno user specified in the
	 *  zeno.properties file
	 *
	 *@return    The ZenoAdmin value
	 */
	public boolean isZenoAdmin();


	/**
	 *  Returns whether the user logged in has the secified role regarding object.
	 *  Valid roles for resources are reader, writer, editor, creator Valid roles
	 *  for principals are systemAdmin, admin, mate (The user is a mate for an
	 *  object if he belongs to the same community)
	 *
	 *@param  role               Description of Parameter
	 *@param  object             Description of Parameter
	 *@return                    Description of the Returned Value
	 *@exception  ZenoException  Description of Exception
	 */

	public boolean hasRole(String role, Object object) throws ZenoException;


	/**
	 *  The bag is a set of strings, where each string is the id of a resource.
	 *  Since the bag is an instance variable of a factory, its contents do not
	 *  persist between sessions.
	 *
	 *@return    The Bag value
	 */

	public Set getBag();


	/**
	 *  Gets the monitor
	 *
	 *@return    The Monitor value
	 */

	public Monitor getMonitor();


	/**
	 *  This method is for constructing a ZenoResource Java object for working with
	 *  an <em>existing</em> resource in the database. An instance of the most
	 *  concrete implementation of the ZenoResource interface possible is made,
	 *  using the zenoClass property.
	 *
	 *@param  id                 Description of Parameter
	 *@return                    Description of the Returned Value
	 *@exception  ZenoException  Description of Exception
	 */

	public ZenoResource loadResource(int id) throws ZenoException;


	/**
	 *  Creates and stores a <em>new</em> article in the given journal.
	 *
	 *@param  parent             Description of Parameter
	 *@return                    Description of the Returned Value
	 *@exception  ZenoException  Description of Exception
	 */

	public Article createArticle(Journal parent) throws ZenoException;


	/**
	 *  Creates and stores a <em>new</em> article in the given topic.
	 *
	 *@param  parent             Description of Parameter
	 *@return                    Description of the Returned Value
	 *@exception  ZenoException  Description of Exception
	 */

	public Article createArticle(Topic parent) throws ZenoException;


	/**
	 *  Creates and stores a <em>new</em> topic in the given journal.
	 *
	 *@param  parent             Description of Parameter
	 *@return                    Description of the Returned Value
	 *@exception  ZenoException  Description of Exception
	 */

	public Article createTopic(Journal parent) throws ZenoException;


	/**
	 *  Creates and stores a <em>new</em> subjournal in the given parent journal.
	 *
	 *@param  parent             Description of Parameter
	 *@return                    Description of the Returned Value
	 *@exception  ZenoException  Description of Exception
	 */

	public Journal createJournal(Journal parent) throws ZenoException;


	/**
	 *  Get the URL of the user's default Style Sheet, to be used when formatting
	 *  journals and articles. This default may depend on the user's default
	 *  journal and may be overridden by setting the styleSheet property of a
	 *  journal.
	 *
	 *@return    The DefaultStyleSheet value
	 */

	public String getDefaultStyleSheet();


	/**
	 *  Get the id of the user's default journal. This can be used, for example, to
	 *  display the top-level journal of the site after the user logs in, when no
	 *  other journal has been passed to the login servlet as a parameter.
	 *
	 *@return    The DefaultJournal value
	 */

	public int getDefaultJournal();


	/**
	 *  Description of the Method
	 *
	 *@param  id                 Description of Parameter
	 *@exception  ZenoException  Description of Exception
	 */
	public void subscribeJournal(int id) throws ZenoException;


	/**
	 *  Description of the Method
	 *
	 *@param  id                 Description of Parameter
	 *@exception  ZenoException  Description of Exception
	 */
	public void unsubscribeJournal(int id) throws ZenoException;


	/**
	 *  Description of the Method
	 *
	 *@return                    Description of the Returned Value
	 *@exception  ZenoException  Description of Exception
	 */
	public List loadSubscribedJournals() throws ZenoException;


	/**
	 *  Description of the Method
	 *
	 *@return                    Description of the Returned Value
	 *@exception  ZenoException  Description of Exception
	 */
	public List loadMarkedJournals() throws ZenoException;


	/**
	 *  Description of the Method
	 *
	 *@param  role               Description of Parameter
	 *@param  unsubscribedOnly   Description of Parameter
	 *@return                    Description of the Returned Value
	 *@exception  ZenoException  Description of Exception
	 */
	public List loadAccessibleJournals(String role, boolean unsubscribedOnly)
			 throws ZenoException;

	/**
	 *  Description of the Method
	 *
	 *@param  text               Description of Parameter
	 *@return                    Description of the Returned Value
	 *@exception  ZenoException  Description of Exception
	 */
	public List searchJournals(String text) throws ZenoException;


	/**
	 *  obsolete !!!! Loads articles created ofter date from all subscribed
	 *  journals if subscribedOnly is true and from all readable journals otherwise
	 *  uses date of last login if date is null. Returns a list of articles
	 *
	 *@param  date               Description of Parameter
	 *@param  subscribedOnly     Description of Parameter
	 *@return                    Description of the Returned Value
	 *@exception  ZenoException  Description of Exception
	 */

	public List loadNewArticles(Date date, boolean subscribedOnly)
			 throws ZenoException;

	/**
	 *  obsolete!!!!! Loads articles created ofter date from all subscribed
	 *  journals if subscribedOnly is true and from all readable journals otherwise
	 *  uses date of last login if date is null. Returns a list of
	 *  articleCollection which collect all articles belonging to the same journal.
	 *
	 *@param  date               Description of Parameter
	 *@param  mode               Description of Parameter
	 *@return                    The ArticleCollections value
	 *@exception  ZenoException  Description of Exception
	 */
	
	public List getArticleCollections(Date date, boolean subscribedOnly)
			 throws ZenoException;

	/**
	 *  Loads articles created ofter date from all journals specified by mode: 0 -
	 *  from all readable journals, 1 - from subscribed journals 2 - from subsribed
	 *  journals and its descendants uses date of last login if date is null.
	 *  Returns a list of articles
	 *
	 *@param  date               Description of Parameter
	 *@param  mode               Description of Parameter
	 *@return                    Description of the Returned Value
	 *@exception  ZenoException  Description of Exception
	 */

	public List loadNewArticles(Date date, int mode)
			 throws ZenoException;
	
	/**
	 *  Loads articles modified ofter date from all journals specified by mode: 0 -
	 *  from all readable journals, 1 - from subscribed journals 2 - from subsribed
	 *  journals and its descendants uses date of last login if date is null.
	 *  If newOnly is tue only articles newly created are loaded.
	 *  Returns a list of article collection which collect all articles belonging
	 *  to the same journal.
	 *
	 *@param  date               Description of Parameter
	 *@param  subscribedOnly     Description of Parameter
	 *@return                    The ArticleCollections value
	 *@exception  ZenoException  Description of Exception
	 */
	
	public List getArticleCollections(Date date, int mode, boolean newOnly)
			 throws ZenoException;

	/**
	 *  Loads articles created ofter date from all journals specified by mode: 0 -
	 *  from all readable journals, 1 - from subscribed journals 2 - from subsribed
	 *  journals and its descendants uses date of last login if date is null.
	 *  Returns a list of article collection which collect all articles belonging
	 *  to the same journal.
	 *
	 *@param  date               Description of Parameter
	 *@param  subscribedOnly     Description of Parameter
	 *@return                    The ArticleCollections value
	 *@exception  ZenoException  Description of Exception
	 */
	
	public List getArticleCollections(Date date, int mode)
			 throws ZenoException;

	/**
	 *  Returns a list of article collections. Each collection contains all
	 *  articles with the specified properties from a journal. Null can be used as
	 *  a wild card, matching all values. The parameters fromDate and toDate
	 *  specify an intervall: an article is selected if its creationDate is >=
	 *  fromDate and <= endDate. An unspecified date parameter is interpreted as
	 *  "earliest date in the past" and "latest date in the future",
	 *  respectively.The full text parameter searches notes, keywords, and titles.
	 *  The full text parameter searches the values of "title", "note" and
	 *  "keywords" (as if they were concatenated to a single string). Full text
	 *  should be a pattern with % as wildcard representing any number of
	 *  characters. The collections are ordered by title of the collectionn's
	 *  journal. The order parameter can be used to specify the order in which the
	 *  articles are listed in each collection. This is a comma separated list of
	 *  words from the following set: "author", "title", "creation_date", "label",
	 *  "qualifier", "rank". Use the empty string if you do not want to specify an
	 *  order.
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
			 throws ZenoException;


	/**
	 *  Returns a list of external links of the specified type matching reference
	 *  and name which might both be pattern
	 *
	 *@param  type               Description of Parameter
	 *@param  reference          Description of Parameter
	 *@param  name               Description of Parameter
	 *@return                    Description of the Returned Value
	 *@exception  ZenoException  Description of Exception
	 */

	public List findXLinks(String type, String reference, String name)
			 throws ZenoException;


	/**
	 *  Returns the user's maximum allowed size of uploaded files, in bytes.
	 *
	 *@return    The MaxUploadSize value
	 */

	public long getMaxUploadSize();


	/**
	 *  Loads a Plugin Attribute set from the Database Table Plugin
	 *
	 *@param  id                 Description of Parameter
	 *@return                    Description of the Returned Value
	 *@exception  ZenoException  Description of Exception
	 */

	public Plugin loadPlugin(String id) throws ZenoException;


	/**
	 *  Creates a <em>new</em> Plugin Attribute Set
	 *
	 *@param  id                 Description of Parameter
	 *@return                    Description of the Returned Value
	 *@exception  ZenoException  Description of Exception
	 */

	public Plugin createPlugin(String id) throws ZenoException;


	/**
	 *  Returns a list of all known Plugin ids
	 *
	 *@return                    The PluginIds value
	 *@exception  ZenoException  Description of Exception
	 */

	public List getPluginIds() throws ZenoException;


	/**
	 *  Loads an existing principal from the persistent store.
	 *
	 *@param  id                 Description of Parameter
	 *@return                    Description of the Returned Value
	 *@exception  ZenoException  Description of Exception
	 */

	public Principal loadPrincipal(String id) throws ZenoException;


	/**
	 *  Returns an Iterator of principals belonging to the specified community
	 *  which id, name or email starts with one of the names specified in the
	 *  query, The query should be a white space separated list of names
	 *
	 *@param  community          Description of Parameter
	 *@param  query              Description of Parameter
	 *@param  type               Description of Parameter
	 *@return                    Description of the Returned Value
	 *@exception  ZenoException  Description of Exception
	 */

	public Iterator searchPrincipals(String community, String query, String type)
			 throws ZenoException;


	/**
	 *  Retuns a list of communities avaiable for self registration according to
	 *  the admission and rejection criteria. The list includes those communities
	 *  the user belongs to, if isMember is true; they are omitted otherwise;
	 *
	 *@param  isMember           Description of Parameter
	 *@return                    The CommunitiesForRegistration value
	 *@exception  ZenoException  Description of Exception
	 */

	public List getCommunitiesForRegistration(boolean isMember) throws ZenoException;


	/**
	 *  Adds the user to the specified community if the admission and rejection
	 *  criteria allow for self registration
	 *
	 *@param  community          Description of Parameter
	 *@exception  ZenoException  Description of Exception
	 */

	public void register(String community) throws ZenoException;


	/**
	 *  Creates a <em>new</em> user belonging to the specified community and stores
	 *  it to the persistent store if the admission and rejection criteria allow
	 *  for self registration
	 *
	 *@param  community          Description of Parameter
	 *@param  id                 Description of Parameter
	 *@param  name               Description of Parameter
	 *@param  password           Description of Parameter
	 *@return                    Description of the Returned Value
	 *@exception  ZenoException  Description of Exception
	 */

	public Principal registerAs(String community, String id, String name, String password)
			 throws ZenoException;


	/**
	 *  Creates a <em>new</em> community and stores it to the persistent store. The
	 *  creator which has to be a system administrator becomes administrator of the
	 *  community
	 *
	 *@param  cid                Description of Parameter
	 *@param  name               Description of Parameter
	 *@return                    Description of the Returned Value
	 *@exception  ZenoException  Description of Exception
	 */

	public Community createCommunity(String cid, String name)
			 throws ZenoException;


	/**
	 *  Creates a <em>new</em> user belonging to the specified community and stores
	 *  it to the persistent store. The creator has to be an administrator of the
	 *  community
	 *
	 *@param  community          Description of Parameter
	 *@param  id                 Description of Parameter
	 *@param  name               Description of Parameter
	 *@param  email              Description of Parameter
	 *@return                    Description of the Returned Value
	 *@exception  ZenoException  Description of Exception
	 */

	public Principal createUser(String community, String id, String name, String email)
			 throws ZenoException;


	/**
	 *  Creates a <em>new</em> collective belonging to the specified community and
	 *  stores it to the persistent store. The creator has to be an administrator
	 *  of the community
	 *
	 *@param  community          Description of Parameter
	 *@param  id                 Description of Parameter
	 *@param  name               Description of Parameter
	 *@param  password           Description of Parameter
	 *@return                    Description of the Returned Value
	 *@exception  ZenoException  Description of Exception
	 */

	public Principal createCollective(String community, String id, String name, String password)
			 throws ZenoException;


	/**
	 *  Creates a <em>new</em> group belonging to the specified community and
	 *  stores it to the persistent store. The creator has to be an administrator
	 *  of the community.
	 *
	 *@param  community          Description of Parameter
	 *@param  id                 Description of Parameter
	 *@param  name               Description of Parameter
	 *@return                    Description of the Returned Value
	 *@exception  ZenoException  Description of Exception
	 */

	public Group createGroup(String community, String id, String name)
			 throws ZenoException;


	/**
	 *  Removes the specified community with all members not belonging to other
	 *  communities from the persistent store
	 *
	 *@param  cid                Description of Parameter
	 *@exception  ZenoException  Description of Exception
	 */

	public void removeCommunity(String cid)
			 throws ZenoException;

	/**
	 *  Sets the journal ID for the daily notification into the user daily notification list.
	 *
	 *@param  id                 Id of Journal to be set onto daily notification list
	 *@param  mode               1 - daily notification
	 *                           2 - weekly notification
	 *                           3 - monthly notification
	 */

				public void setNotify(int id, int mode) throws ZenoException;


	/**
	 *  Removes the journal ID from the daily notification list.
	 *
	 *@param  id                 Id of Journal to be removed from daily notification list
	 *@param  mode               1 - daily notification
	 *                           2 - weekly notification
	 *                           3 - monthly notification
	 */

				public void unNotify(int id, int mode) throws ZenoException;
	
	/**
			 *  Returns a list of journals subscribed form notification.
			*
			*@param  mode               1 - daily notification
			*                           2 - weekly notification
			*                           3 - monthly notification
			*@return                    Returns list of journals subscribed for notificaiton depending on mode.
			*@exception  ZenoException  Description of Exception
			*/
		public List loadSubscribedJournalsForNotification(int mode) throws ZenoException;
	

	/**
	 *  Removes the specified principal from the community. A principal not
	 *  belonging to other communities is removed from the persistent store Throws
	 *  a NoPermissionException if the principal is the last administrator of the
	 *  community
	 *
	 *@param  community          Description of Parameter
	 *@param  uid                Description of Parameter
	 *@exception  ZenoException  Description of Exception
	 */

	public void removePrincipal(String community, String uid) throws ZenoException;

}

