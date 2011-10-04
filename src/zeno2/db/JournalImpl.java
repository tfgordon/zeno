package zeno2.db;

import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import zeno2.kernel.Article;
import zeno2.kernel.ArticleCollection;
import zeno2.kernel.BeginEvent;
import zeno2.kernel.CreationEvent;
import zeno2.kernel.EndEvent;
import zeno2.kernel.ExpirationEvent;
import zeno2.kernel.Journal;
import zeno2.kernel.Link;
import zeno2.kernel.ModificationEvent;
import zeno2.kernel.NameInUseException;
import zeno2.kernel.NoPermissionException;
import zeno2.kernel.OutlineNode;
import zeno2.kernel.PreviewElement;

import zeno2.kernel.ZenoCollection;
import zeno2.kernel.ZenoEvent;
import zeno2.kernel.ZenoException;
import zeno2.kernel.ZenoResource;
import zeno2.util.ZenoUtilities;

/**
 *  Description of the Class
 *
 *@author     oppor
 *@created    September 12, 2002
 */
public class JournalImpl extends CollectionImpl implements Journal {

	String articleLabels = "";
	String qualifiers = "";
	String linkLabels = "";
	int revisionPeriod = 0;// hours
	int attachmentSizeLimit = -1;
	boolean topicMode = true;
	String styleSheet = "";


	/**
	 *  Constructor for the JournalImpl object
	 *
	 *@param  factory  Description of Parameter
	 */
	protected JournalImpl(FactoryImpl factory) {
		super(factory);
		super.zenoClass = "journal";
	}


	/**
	 *  Constructor for the JournalImpl object
	 *
	 *@param  factory  Description of Parameter
	 *@param  id       Description of Parameter
	 */
	protected JournalImpl(FactoryImpl factory, int id) {
		super(factory, id);
		super.zenoClass = "journal";
	}


	/**
	 *  Sets the article labels of the journal. The labels should be separated by
	 *  commas. Save to make persistent.
	 *
	 *@param  labels             The new ArticleLabels value
	 *@exception  ZenoException  Description of Exception
	 */

	public void setArticleLabels(String labels) throws ZenoException {
		factory.checkPermission("Journal.setArticleLabels", this);
		if (!loaded) {
			loadProperties();
		}
		this.articleLabels = labels;
		this.modified = true;
	}


	/**
	 *  Sets the editor labels of the journal. The labels should be separated by
	 *  commas. Save to make persistent.
	 *
	 *@param  labels             The new Qualifiers value
	 *@exception  ZenoException  Description of Exception
	 */

	public void setQualifiers(String labels) throws ZenoException {
		factory.checkPermission("Journal.setQualifiers", this);
		if (!loaded) {
			loadProperties();
		}
		this.qualifiers = labels;
		this.modified = true;
	}


	/**
	 *  Set the links labels from a comma separated string. Save to make
	 *  persistent.
	 *
	 *@param  labels             The new LinkLabels value
	 *@exception  ZenoException  Description of Exception
	 */

	public void setLinkLabels(String labels) throws ZenoException {
		factory.checkPermission("Journal.setLinkLabels", this);
		if (!loaded) {
			loadProperties();
		}
		this.linkLabels = labels;
		this.modified = true;
	}


	/**
	 *  Sets the TopicMode attribute of the JournalImpl object
	 *
	 *@param  on                 The new TopicMode value
	 *@exception  ZenoException  Description of Exception
	 */
	public void setTopicMode(boolean on) throws ZenoException {
		factory.checkPermission("Journal.setTopicMode", this);
		if (!loaded) {
			loadProperties();
		}
		this.topicMode = on;
		this.modified = true;
	}


	/**
	 *  Sets the AttachmentSizeLimit attribute of the JournalImpl object
	 *
	 *@param  size               The new AttachmentSizeLimit value
	 *@exception  ZenoException  Description of Exception
	 */
	public void setAttachmentSizeLimit(int size) throws ZenoException {
		factory.checkPermission("Journal.setAttachmentSizeLimit", this);
		if (!loaded) {
			loadProperties();
		}
		this.attachmentSizeLimit = size;
		this.modified = true;
	}


	/**
	 *  Sets the MailAlias attribute of the JournalImpl object
	 *
	 *@param  alias              The new MailAlias value
	 *@exception  ZenoException  Description of Exception
	 */
	public void setMailAlias(String alias) throws ZenoException {

		if (alias == null | alias.equals("")) {
			removeProperty("MailAlias");
		}
		else {
			StringBuffer buf = new StringBuffer();
			buf.append("select resource from property");
			buf.append(" where name='mailAlias' and value=");
			buf.append(DBClient.format(alias));
			try {
				ResultSet rs = factory.dbclient.executeQuery(buf.toString());
				if (rs.next()) {
					int resid = rs.getInt(1);
					if (resid == this.id) {
						;
					}//nothing to do
					else {
						throw new NameInUseException("mailAliasInUse");
					}
				}
				else {
					setProperty("MailAlias", alias);
				}
			}
			catch (java.sql.SQLException e) {
				factory.reportError("ZenoResource.setMailAlias", e);
				throw new ZenoException("DataBaseException");
			}
		}
	}


	/**
	 *  Sets the revision property. Save to make persistent.
	 *
	 *@param  hours              The new RevisionPeriod value
	 *@exception  ZenoException  Description of Exception
	 */

	public void setRevisionPeriod(int hours) throws ZenoException {
		factory.checkPermission("Journal.setRevisionPeriod", this);
		if (!loaded) {
			loadProperties();
		}
		this.revisionPeriod = hours;
		this.modified = true;
	}


	/**
	 *  Sets the URL of the Cascaded Style Sheet to be used for formatting this
	 *  journal, overriding system defaults.
	 *
	 *@param  url                The new StyleSheetUrl value
	 *@exception  ZenoException  Description of Exception
	 */

	public void setStyleSheetUrl(String url) throws ZenoException {
		factory.checkPermission("Journal.setStyleSheetUrl", this);
		if (!loaded) {
			loadProperties();
		}
		this.styleSheet = url;
		this.modified = true;
	}


	/**
	 *  Returns a comma separated list of labels to be used to label articles.
	 *  Example: "issue, position, pro, con".
	 *
	 *@return                    The ArticleLabels value
	 *@exception  ZenoException  Description of Exception
	 */

	public String getArticleLabels() throws ZenoException {
		factory.checkPermission("Journal.getArticleLabels", this);
		if (!loaded) {
			loadProperties();
		}
		return this.articleLabels;
	}


	/**
	 *  Returns a comma separated list of labels reserved for use by editors or
	 *  moderators to label articles. Examples: "warning, yellow card, red card,
	 *  senior contributor".
	 *
	 *@return                    The Qualifiers value
	 *@exception  ZenoException  Description of Exception
	 */

	public String getQualifiers() throws ZenoException {
		factory.checkPermission("Journal.getQualifiers", this);
		if (!loaded) {
			loadProperties();
		}
		return this.qualifiers;
	}


	/**
	 *  Returns a comma separated list of link labels. Examples: "reply, about,
	 *  refines, distinguishes".
	 *
	 *@return                    The LinkLabels value
	 *@exception  ZenoException  Description of Exception
	 */

	public String getLinkLabels() throws ZenoException {
		factory.checkPermission("Journal.getLinkLabels", this);
		if (!loaded) {
			loadProperties();
		}
		return this.linkLabels;
	}


	/**
	 *  Gets the TopicMode attribute of the JournalImpl object
	 *
	 *@return                    The TopicMode value
	 *@exception  ZenoException  Description of Exception
	 */
	public boolean getTopicMode() throws ZenoException {
		factory.checkPermission("Journal.getTopicMode", this);
		if (!loaded) {
			loadProperties();
		}
		return topicMode;
	}


	/**
	 *  Gets the AttachmentSizeLimit attribute of the JournalImpl object
	 *
	 *@return                    The AttachmentSizeLimit value
	 *@exception  ZenoException  Description of Exception
	 */
	public int getAttachmentSizeLimit() throws ZenoException {
		factory.checkPermission("Journal.getAttachmentSizeLimit", this);
		if (!loaded) {
			loadProperties();
		}
		return attachmentSizeLimit;
	}


	/**
	 *  Gets the MailAlias attribute of the JournalImpl object
	 *
	 *@return                    The MailAlias value
	 *@exception  ZenoException  Description of Exception
	 */
	public String getMailAlias() throws ZenoException {
		String alias = getProperty("MailAlias");
		if (alias == null) {
			return "";
		}
		else {
			return alias;
		}
	}


	/**
	 *  Gets the Subjournals attribute of the JournalImpl object
	 *
	 *@return                    The Subjournals value
	 *@exception  ZenoException  Description of Exception
	 */
	public Set getSubjournals() throws ZenoException {
		return getSubjournals(true);
	}
	
	public int subjournalCount() throws ZenoException {
		Set journals = getSubjournals(true);
		return journals.size();
	}

	/**
	 *  Gets the number of hours during which the creator of an article may modify
	 *  it. After expiration of this period the article is published and closed.
	 *
	 *@return                    The RevisionPeriod value
	 *@exception  ZenoException  Description of Exception
	 */

	public int getRevisionPeriod() throws ZenoException {
		factory.checkPermission("Journal.getRevisionPeriod", this);
		if (!loaded) {
			loadProperties();
		}
		return this.revisionPeriod;
	}
	
	public List getArticles(boolean all, String order) throws ZenoException {
		try {
			StringBuffer buf = new StringBuffer();
			buf.append("select * from resource, article");
			buf.append(" where parent=");
			buf.append(DBClient.format(this.id));
			buf.append(" and resource.id = article.id");
			if (! all) {
				buf.append(" and marked_for_deletion='false'");
				buf.append(" and published='true'");
			}
			if (order != null && !order.equals("")) {
				buf.append(" order by ");
				buf.append(order);
			}
			ResultSet rs = factory.dbclient.executeQuery(buf.toString());
			
			List list = new ArrayList();
			while (rs.next()) {
				list.add(factory.loadArticle(rs));
			}
			return list;
		} catch (java.sql.SQLException e) {
			factory.reportError("Journal.getArticles", e);
			throw new ZenoException("DataBaseException");
		}
	}


	/**
	 *  Returns an enumeration containing all articles of the journal, Only
	 *  articles which are published and not marked for deletion are included..
	 *
	 *@return                    The Articles value
	 *@exception  ZenoException  Description of Exception
	 */

	public Iterator getArticles() throws ZenoException {
		factory.checkPermission("Journal.getArticles", this);
		List articles = getArticles(false, "rank, title");
		return articles.iterator();
	}
	
	public List getArticlesByTopic() throws ZenoException {
		factory.checkPermission("Journal.getArticles", this);
		List articles = getArticles(true, "part, rank, title");
		List collections = ArticleCollection.groupArticlesByTopic(articles);
		ArticleCollection.sort(collections, "byTopic");
		return ArticleCollection.addAll2(collections);
	}
	
	public List getArticleCollections() throws ZenoException {
		factory.checkPermission("Journal.getArticles", this);
		List articles = getArticles(false, "part, rank, title");
		List collections = ArticleCollection.groupArticlesByTopic(articles);
		ArticleCollection.sort(collections, "byTopic");
		return collections;
	}
	
	/**
	 *  The "topics" of a journal are the "top-level" articles. The topics are
	 *  sorted by rank and then title.
	 *
	 *@return                    The Topics value
	 *@exception  ZenoException  Description of Exception
	 */

	public List getTopics() throws ZenoException {
		try {
			factory.checkPermission("Journal.getTopics", this);
			StringBuffer buf = new StringBuffer();
			buf.append("select * from resource, article");
			buf.append(" where parent=");
			buf.append(DBClient.format(this.id));
			buf.append(" and resource.id=article.id");
			//temp until db content is upgraded
			buf.append(" and (is_topic='true' or class='topic')");
			buf.append(" and marked_for_deletion='false'");
			buf.append(" and published='true'");
			buf.append(" order by rank, title");
			ResultSet rs = factory.dbclient.executeQuery(buf.toString());
			List list = new ArrayList();
			while (rs.next()) {
				list.add(factory.loadArticle(rs));
			}
			return list;
		}
		catch (java.sql.SQLException e) {
			factory.reportError("Journal.getTopics", e);
			throw new ZenoException("DataBaseException");
		}
	}


	/**
	 *  Returns the n most recently created articles in a journal, sorted by
	 *  creation date. Only published and not marked for deletion articles are
	 *  included.
	 *
	 *@param  n                  Description of Parameter
	 *@return                    The RecentArticles value
	 *@exception  ZenoException  Description of Exception
	 */

	public List getRecentArticles(int n) throws ZenoException {
		try {
			factory.checkPermission("Journal.getRecentArticles", this);
			List list = new ArrayList();
			StringBuffer buf = new StringBuffer();
			buf.append("select * from resource, article");
			buf.append(" where parent=");
			buf.append(DBClient.format(this.id));
			buf.append(" and resource.id=article.id");
			buf.append(" and resource.marked_for_deletion='false'");
			buf.append(" and article.published='true'");
			buf.append(" order by resource.id desc");
			ResultSet rs = factory.dbclient.executeQuery(buf.toString());
			while (rs.next() && n > 0) {
				list.add(factory.loadArticle(rs));
				n--;
			}
			return list;
		}
		catch (java.sql.SQLException e) {
			factory.reportError("Journal.getRecentArticles", e);
			throw new ZenoException("DataBaseException");
		}
	}


	/**
	 *  Gets the RecentArticles attribute of the JournalImpl object
	 *
	 *@param  date               Description of Parameter
	 *@return                    The RecentArticles value
	 *@exception  ZenoException  Description of Exception
	 */
	public List getRecentArticles(Date date)
			 throws ZenoException {

		factory.checkPermission("ZenoCollection.getRecentArticles", this);
		if (date == null) {
			DBPrincipalImpl user = (DBPrincipalImpl) factory.getUser();
			date = user.getLastLogin();
		}

		try {
			List result = new ArrayList();

			StringBuffer buf = new StringBuffer();
			buf.append("select * from resource, article");
			buf.append(" where parent=");
			buf.append(DBClient.format(this.id));
			buf.append("and resource.id=article.id ");
			buf.append(" and marked_for_deletion='false'");
			buf.append(" and published='true'");
			buf.append(" and creation_date >= ");
			buf.append(DBClient.format(date));
			buf.append(" order by resource.id desc");

			ResultSet rs = factory.dbclient.executeQuery(buf.toString());
			while (rs.next()) {
				result.add(factory.loadArticle(rs));
			}
			return result;
		}
		catch (java.sql.SQLException e) {
			factory.reportError("Journal.getArticlesBetween", e);
			throw new ZenoException("DataBaseException");
		}
	}


	/**
	 *  Returns articles created after date, sorted by creation date. Only articles
	 *  published and not marked for deletion are included. uses date of last login
	 *  if date is null
	 *
	 *@param  date               Description of Parameter
	 *@return                    The RecentArticles value
	 *@exception  ZenoException  Description of Exception
	 */

	public List getRecentArticles(String date)
			 throws ZenoException {

		factory.checkPermission("Journal.getRecentArticles", this);
		Date dateobj = null;
		if (date == null || date.equals("")) {
			DBPrincipalImpl user = (DBPrincipalImpl) factory.getUser();
			dateobj = user.getLastLogin();
		}
		else {
			dateobj = ZenoUtilities.getDateAndTimeFromIsoString(date);
		}

		try {
			List result = new ArrayList();

			StringBuffer buf = new StringBuffer();
			buf.append("select * from resource, article where ");
			buf.append(" and parent=");
			buf.append(DBClient.format(this.id));
			buf.append(" resource.id=article.id ");
			buf.append(" and marked_for_deletion='false'");
			buf.append(" and published='true'");
			buf.append(" and creation_date >= ");
			buf.append(DBClient.format(dateobj));
			buf.append(" order by resource.id desc");

			ResultSet rs = factory.dbclient.executeQuery(buf.toString());
			while (rs.next()) {
				result.add(factory.loadArticle(rs));
			}
			return result;
		}
		catch (java.sql.SQLException e) {
			factory.reportError("Journal.getArticlesBetween", e);
			throw new ZenoException("DataBaseException");
		}
	}


	/**
	 *  Gets the RecentTopics attribute of the JournalImpl object
	 *
	 *@param  n                  Description of Parameter
	 *@return                    The RecentTopics value
	 *@exception  ZenoException  Description of Exception
	 */
	public List getRecentTopics(int n) throws ZenoException {

		factory.checkPermission("Journal.getRecentTopics", this);
		try {
			List list = new ArrayList();
			StringBuffer buf = new StringBuffer();
			buf.append("select * from resource, article");
			buf.append(" where parent=");
			buf.append(DBClient.format(this.id));
			buf.append(" and resource.id=article.id");
			//temp until db content is upgraded
			buf.append(" and (is_topic='true' or class='topic')");
			buf.append(" and resource.marked_for_deletion='false'");
			buf.append(" and article.published='true'");
			buf.append(" order by resource.id desc");
			ResultSet rs = factory.dbclient.executeQuery(buf.toString());
			while (rs.next() && n > 0) {
				list.add(factory.loadArticle(rs));
				n--;
			}
			return list;
		}
		catch (java.sql.SQLException e) {
			factory.reportError("Journal.getRecentArticles", e);
			throw new ZenoException("DataBaseException");
		}
	}


	/**
	 *  Gets the RecentTopics attribute of the JournalImpl object
	 *
	 *@param  date               Description of Parameter
	 *@return                    The RecentTopics value
	 *@exception  ZenoException  Description of Exception
	 */
	public List getRecentTopics(Date date) throws ZenoException {

		factory.checkPermission("ZenoCollection.getRecentTopics", this);
		if (date == null) {
			DBPrincipalImpl user = (DBPrincipalImpl) factory.getUser();
			date = user.getLastLogin();
		}

		try {
			List result = new ArrayList();

			StringBuffer buf = new StringBuffer();
			buf.append("select * from resource, article");
			buf.append(" where parent=");
			buf.append(DBClient.format(this.id));
			buf.append("and resource.id=article.id ");
			//temp until db content is upgraded
			buf.append(" and (is_topic='true' or class='topic')");
			buf.append(" and marked_for_deletion='false'");
			buf.append(" and published='true'");
			buf.append(" and creation_date >= ");
			buf.append(DBClient.format(date));
			buf.append(" order by resource.id desc");

			ResultSet rs = factory.dbclient.executeQuery(buf.toString());
			while (rs.next()) {
				result.add(factory.loadArticle(rs));
			}
			return result;
		}
		catch (java.sql.SQLException e) {
			factory.reportError("Journal.getRecentTopics", e);
			throw new ZenoException("DataBaseException");
		}
	}
	
	public List getModifiedArticles(Date date, int nr) throws ZenoException {
		factory.checkPermission("Journal.getModifiedArticles", this);
		if (date == null) {
			DBPrincipalImpl user = (DBPrincipalImpl)factory.getUser();
			date = user.getLastLogin();
		}
		
		try {
			List result = new ArrayList();
			StringBuffer buf = new StringBuffer();
			buf.append("select * from resource, article");
			buf.append(" where parent=");
			buf.append(DBClient.format(this.id));
			buf.append(" and resource.id=article.id ");
			buf.append(" and marked_for_deletion='false'");
			buf.append(" and published='true'");
			buf.append(" and modification_date >= ");
			buf.append(DBClient.format(date));
			buf.append(" order by resource.modification_date desc");
			ResultSet rs = factory.dbclient.executeQuery(buf.toString());
			while (rs.next() && nr != 0) {
				Article art = factory.loadArticle(rs);
				result.add(art);
				nr--;
			}
			return result;
		} catch (java.sql.SQLException e) {
			factory.reportError("Journal.getModifiedArticles", e);
			throw new ZenoException("DataBaseException");
		}
	}



	/**
	 *  Returns a List articles with BeginDate or EndDate property belonging to the
	 *  specified time intervall (including end points) which are published and not
	 *  marked for deletion. The articles are ordered by beginDate, rank, title.
	 *
	 *@param  startDate          Description of Parameter
	 *@param  endDate            Description of Parameter
	 *@return                    The ArticlesBetween value
	 *@exception  ZenoException  Description of Exception
	 */

	public List getArticlesBetween(Date startDate, Date endDate)
			 throws ZenoException {
		try {
			factory.checkPermission("Journal.getArticlesBetween", this);
			List result = new ArrayList();
			StringBuffer buf = new StringBuffer();
			String bd = DBClient.format(startDate);
			String ed = DBClient.format(endDate);
			buf.append("select * from resource, article where ");
			buf.append(" resource.id=article.id ");
			buf.append(" and parent=");
			buf.append(DBClient.format(this.id));
			buf.append(" and marked_for_deletion='false'");
			buf.append(" and published='true'");
			buf.append(" and ((begin_date >= " + bd);
			buf.append("        and begin_date <= " + ed + ")");
			buf.append("    or (end_date >= " + bd);
			buf.append("        and end_date <= " + ed + ")");
			buf.append("      )");
			buf.append(" order by begin_date, rank, title");
			ResultSet rs = factory.dbclient.executeQuery(buf.toString());
			while (rs.next()) {
				result.add(factory.loadArticle(rs));
			}
			return result;
		}
		catch (java.sql.SQLException e) {
			factory.reportError("Journal.getArticlesBetween", e);
			throw new ZenoException("DataBaseException");
		}
	}


	/**
	 *  Returns a list of ZenoEvent objects, ordered by date, for the CreationDate,
	 *  ModificationDate, ExpirationDate and ScheduledDate properties of pubished
	 *  and not marked for deletion articles in this journal. This method simply
	 *  lists the member articles chronologically, using temporal properties of the
	 *  articles. It does not access a log file or protocol.
	 *
	 *@param  startDate          Description of Parameter
	 *@param  endDate            Description of Parameter
	 *@return                    The EventsDuring value
	 *@exception  ZenoException  Description of Exception
	 */

	public Iterator getEventsDuring(Date startDate, Date endDate)
			 throws ZenoException {
		try {
			factory.checkPermission("Journal.getEventsDuring", this);
			List result = new ArrayList();
			StringBuffer buf = new StringBuffer();
			String bd = DBClient.format(startDate);
			String ed = DBClient.format(endDate);
			buf.append("select * from resource, article where ");
			buf.append(" resource.id=article.id ");
			buf.append(" and parent=");
			buf.append(DBClient.format(this.id));
			buf.append(" and marked_for_deletion='false'");
			buf.append(" and published='true'");
			buf.append(" and ((creation_date > " + bd);
			buf.append("       and creation_date < " + ed + ")");
			buf.append("    or (modification_date > " + bd);
			buf.append("        and modification_date < " + ed + ")");
			buf.append("    or (begin_date > " + bd);
			buf.append("        and begin_date < " + ed + ")");
			buf.append("    or (end_date > " + bd);
			buf.append("        and end_date < " + ed + ")");
			buf.append("    or (expiration_date > " + bd);
			buf.append("        and expiration_date < " + ed + ")");
			buf.append("      )");
			ResultSet rs = factory.dbclient.executeQuery(buf.toString());
			while (rs.next()) {
				Article art = (Article) factory.loadArticle(rs);
				if (art.getCreationDate().after(startDate)
						 && art.getCreationDate().before(endDate)) {
					result.add(new CreationEvent(art, art.getCreator(), art.getCreationDate()));
				}
				if (art.getModificationDate().after(startDate)
						 && art.getModificationDate().before(endDate)) {
					result.add(
							new ModificationEvent(art, art.getModifier(), art.getModificationDate()));
				}
				if (art.getBeginDate() != null
						 && art.getBeginDate().after(startDate)
						 && art.getBeginDate().before(endDate)) {
					result.add(new BeginEvent(art, art.getCreator(), art.getBeginDate()));
				}
				if (art.getEndDate() != null
						 && art.getEndDate().after(startDate)
						 && art.getEndDate().before(endDate)) {
					result.add(new EndEvent(art, art.getCreator(), art.getEndDate()));
				}
				if (art.getExpirationDate() != null
						 && art.getExpirationDate().after(startDate)
						 && art.getExpirationDate().before(endDate)) {
					result.add(new ExpirationEvent(art, art.getCreator(), art.getExpirationDate()));
				}
			}

			Collections.sort(result,
				new Comparator() {
					/**
					 *  Description of the Method
					 *
					 *@param  o1  Description of Parameter
					 *@param  o2  Description of Parameter
					 *@return     Description of the Returned Value
					 */
					public int compare(Object o1, Object o2) {
						if (o1 instanceof ZenoEvent && o2 instanceof ZenoEvent) {
							ZenoEvent e1 = (ZenoEvent) o1;
							ZenoEvent e2 = (ZenoEvent) o2;
							return e1.getDate().compareTo(e2.getDate());
						}
						else {
							return 0;
						}
					}
				});
			return result.iterator();
		}
		catch (java.sql.SQLException e) {
			factory.reportError("Journal.getEventsDuring", e);
			throw new ZenoException("DataBaseException");
		}
	}


	/**
	 *  Returns a list of all article members which are unpublished but not marked
	 *  for deletion.
	 *
	 *@return                    The UnpublishedArticles value
	 *@exception  ZenoException  Description of Exception
	 */

	public List getUnpublishedArticles() throws ZenoException {
		try {
			factory.checkPermission("Journal.getUnpublishedArticles", this);
			List list = new ArrayList();
			StringBuffer buf = new StringBuffer();
			buf.append("select * from resource, article where");
			buf.append(" resource.id=article.id");
			buf.append(" and parent=");
			buf.append(DBClient.format(this.id));
			buf.append(" and published='false'");
			buf.append(" and marked_for_deletion='false'");
			ResultSet rs = factory.dbclient.executeQuery(buf.toString());
			while (rs.next()) {
				list.add(factory.loadArticle(rs));
			}
			return list;
		}
		catch (java.sql.SQLException e) {
			factory.reportError("Journal.getUnpublishedArticles", e);
			throw new ZenoException("DataBaseException");
		}
	}
	
	
	/**
	 *  Gets the URL of the Cascaded Style Sheet to be used, if one has been set
	 *  for this journal. If not, the empty string is returned.
	 *
	 *@return                    The StyleSheetUrl value
	 *@exception  ZenoException  Description of Exception
	 */

	public String getStyleSheetUrl() throws ZenoException {
		factory.checkPermission("Journal.getStyleSheetUrl", this);
		if (!loaded) {
			loadProperties();
		}
		return this.styleSheet;
	}



	/**
	 *  Returns a list of strings naming the supported roles, e.g. "editor",
	 *  "writer", and "reader".
	 *
	 *@return                    The Roles value
	 *@exception  ZenoException  Description of Exception
	 */

	public List getRoles() throws ZenoException {
		factory.checkPermission("Journal.getRoles", this);
		List list = new ArrayList();
		list.add("editor");
		list.add("writer");
		list.add("reader");
		return list;
	}


	/**
	 *  Gets the ids (strings) of principals with the given role. The members of
	 *  groups in the role definition are included, recursively, in this set. If
	 *  the userOnly parameter is true, the ids of groups are filtered out of the
	 *  result.
	 *
	 *@param  role               Description of Parameter
	 *@param  usersOnly          Description of Parameter
	 *@return                    The PrincipalsInRole value
	 *@exception  ZenoException  Description of Exception
	 */

	public Set getPrincipalsInRole(String role, boolean usersOnly)
			 throws ZenoException {
		factory.checkPermission("Journal.getPrincipalsInRole", this);
		Set set = factory.getPrincipalsInRole(this.id, role, usersOnly);
		return set;
	}


	/**
	 *  Gets the definition of the role. Each item in the list is the identifier of
	 *  a principal, i.e. user or group.
	 *
	 *@param  role               Description of Parameter
	 *@return                    The RoleDefinition value
	 *@exception  ZenoException  Description of Exception
	 */

	public List getRoleDefinition(String role) throws ZenoException {
		factory.checkPermission("Journal.getRoleDefinition", this);
		return factory.loadRoleDefinition(this.id, role);
	}


	/**
	 *  Gets the set of ids of all principals (i.e. users and groups) having any
	 *  role in the journal. The ids of members of groups and, recursively,
	 *  subgroups are included in the result. If the usersOnly parameter is true,
	 *  the ids of groups are filtered out of the result. Not called the "members"
	 *  of the journal to avoid confusion with collection membership.
	 *
	 *@param  usersOnly          Description of Parameter
	 *@return                    The Principals value
	 *@exception  ZenoException  Description of Exception
	 */

	public Set getPrincipals(boolean usersOnly) throws ZenoException {
		factory.checkPermission("Journal.getPrincipals", this);
		Set set = factory.getPrincipals(this.id, usersOnly);
		return set;
	}


	/**
	 *  Gets the ActivePrincipals attribute of the JournalImpl object
	 *
	 *@return                    The ActivePrincipals value
	 *@exception  ZenoException  Description of Exception
	 */
	public Set getActivePrincipals() throws ZenoException {
		StringBuffer buf = new StringBuffer();
		buf.append("select distinct author from article, resource");
		buf.append(" where article.id=resource.id");
		buf.append(" and parent=");
		buf.append(DBClient.format(this.id));
		Set activePrincipals = new HashSet();
		try {
			ResultSet rs = factory.dbclient.executeQuery(buf.toString());

			while (rs.next()) {
				String princid = rs.getString("author");
				activePrincipals.add(princid);
			}
		}
		catch (java.sql.SQLException e) {
			factory.reportError("JournalImpl.getActivePrincipals", e);
			throw new ZenoException("DBException");
		}

		return activePrincipals;
	}


	/**
	 *  Returns an ordered list of OutlineNodes, with the article of the given id
	 *  as the source of the link in each node. At each level of the tree, the
	 *  links are sorted by label and source alias. Since journals are arbitrary
	 *  graphs, not trees, duplicates or cycles are broken by expanding only a
	 *  single occurrence of each node. The other occurrences appear in the tree,
	 *  but are not expanded. The linkLabel parameter can be used to include
	 *  articles which can be reached only via links with the given label, where
	 *  each child node in the tree must be the source of a link targeting the
	 *  parent node. If the linkLabel parameter is null or the empty string, all
	 *  links are used to generate the tree.
	 *
	 *@param  resourceId         Description of Parameter
	 *@param  linkLabel          Description of Parameter
	 *@return                    The Outline value
	 *@exception  ZenoException  Description of Exception
	 */

	// released version
	public List getOutline(boolean extended) throws ZenoException {
	
		OutlineGenerator generator =
			new OutlineGenerator(factory, this, extended);
		return generator.getOutline(getTopics(), "", -1);
	}
	
	
	/*
	//heg temp
	public List getOutline(boolean extended) throws ZenoException {
		
		OutlineGenerator generator =
			new OutlineGenerator(factory, this, true);
		List nodes = generator.getFullOutline(getArticleCollections(), "", -1);
		return nodes;
	}
	*/
	
	public List getFullOutline() throws ZenoException {
	
		OutlineGenerator generator =
			new OutlineGenerator(factory, this, true);
		List nodes = generator.getFullOutline(getArticleCollections(), "", -1);
		return generator.groupNodes2(nodes);
	}
	 
	public OutlineNode getOutline(int resourceId, String linkLabel, boolean extended) 
			throws ZenoException {
	
		OutlineGenerator generator =
			new OutlineGenerator(factory, this, extended);
		return generator.getOutline(resourceId, linkLabel);
	}
	
	 public List getOutline (int resourceId, String linkLabel) 
			throws ZenoException
	{	
		OutlineNode node = getOutline(resourceId, linkLabel, false);	
		return node.getChildren();
	}
	

	/**
	 *  load the journal specific properties. The generic properties shared by all
	 *  resources were already loaded when the journal was constructed using the
	 *  loadResource method of the factory.
	 *
	 *@exception  ZenoException  Description of Exception
	 */

	public void loadProperties() throws ZenoException {
		if (!loaded) {
			try {
				String request = "select * from journal where id = " + DBClient.format(this.id);
				ResultSet rs = factory.dbclient.executeQuery(request);
				if (rs.next()) {
					loadProperties(rs);
				}
				this.loaded = true;
			}
			catch (java.sql.SQLException e) {
				factory.reportError("Journal.loadProperties", e);
				throw new ZenoException("DataBaseException");
			}
		}
	}


	/**
	 *  Description of the Method
	 *
	 *@param  parent             Description of Parameter
	 *@exception  ZenoException  Description of Exception
	 */
	public void create(ZenoCollection parent) throws ZenoException {

		if (id != 0) {
			return;
		}

		try {
			super.create(parent);

			StringBuffer buf = new StringBuffer();
			buf.append("insert into journal (");
			buf.append("id, article_labels, qualifiers");
			buf.append(", link_labels, revision_period, style_sheet");
			buf.append(", attachment_size_limit, topic_mode");
			buf.append(") values(");
			buf.append(DBClient.format(this.id));
			buf.append(", ");
			buf.append(DBClient.format(this.articleLabels));
			buf.append(", ");
			buf.append(DBClient.format(this.qualifiers));
			buf.append(", ");
			buf.append(DBClient.format(this.linkLabels));
			buf.append(", ");
			buf.append(DBClient.format(this.revisionPeriod));
			buf.append(", ");
			buf.append(DBClient.format(this.styleSheet));
			buf.append(", ");
			buf.append(DBClient.format(this.attachmentSizeLimit));
			buf.append(", ");
			buf.append(DBClient.format(this.topicMode));
			buf.append(")");
			factory.dbclient.executeUpdate(buf.toString());
			this.created = true;
			//initRoleDefinition((Journal)parent);
			factory.addPrincipalToRole(this.id, "editor");
		}
		catch (java.sql.SQLException e) {
			factory.reportError("Journal.create", e);
			throw new ZenoException("DataBaseException");
		}
	}


	/**
	 *  Return a List of journal members sorted by rank and then title.
	 *
	 *@return                    Description of the Returned Value
	 *@exception  ZenoException  Description of Exception
	 */

	public List sortSubjournals() throws ZenoException {
		try {
			factory.checkPermission("Journal.sortSubjournals", this);
			ArrayList result = new ArrayList();
			StringBuffer buf = new StringBuffer();
			buf.append("select * from resource, journal where ");
			buf.append(" parent=");
			buf.append(DBClient.format(this.id));
			buf.append(" and class='journal'");
			buf.append(" and resource.id=journal.id");
			buf.append(" and marked_for_deletion='false'");
			buf.append(" order by rank, title");
			ResultSet rs = factory.dbclient.executeQuery(buf.toString());
			while (rs.next()) {
				Journal subjournal = factory.loadJournal(rs);
				if (factory.hasRole("reader", subjournal)) {
					result.add(subjournal);
				}
			}
			return result;
		}
		catch (java.sql.SQLException e) {
			factory.reportError("Journal.getSubjournals", e);
			throw new ZenoException("DataBaseException");
		}
	}


	/**
	 *  Returns the number of members of the journal which are articles.
	 *  Unpublished articles and articles marked for deletion are NOT included in
	 *  the count.
	 *
	 *@return                    Description of the Returned Value
	 *@exception  ZenoException  Description of Exception
	 */

	public int articleCount() throws ZenoException {
		factory.checkPermission("Journal.articleCount", this);
		return articleCount("unmarked");
	}


	/**
	 *  Description of the Method
	 *
	 *@return                    Description of the Returned Value
	 *@exception  ZenoException  Description of Exception
	 */
	public int markedArticleCount() throws ZenoException {
		factory.checkPermission("Journal.markedArticleCount", this);
		return articleCount("marked");
	}
	
	public void renumberArticles() throws ZenoException {
		//NumberHandler.setUnsetNumber(this);
		NumberHandler.renumber(this);
	}

	/**
	 *  Description of the Method
	 *
	 *@param  collection         Description of Parameter
	 *@exception  ZenoException  Description of Exception
	 */
	public void move(ZenoCollection collection) throws ZenoException {

		factory.checkPermission("Journal.paste", collection);
		moveTo(collection);
		((JournalImpl) collection).modified();
	}



	/**
	 *  Move some members of this journal to some other collection.
	 *
	 *@param  members            Description of Parameter
	 *@param  collection         Description of Parameter
	 *@exception  ZenoException  Description of Exception
	 */
	/*
	 * public void move(Set members, ZenoCollection collection) throws ZenoException {
	 * factory.checkPermission("Journal.move", this);
	 * factory.checkPermission("Journal.paste", collection);
	 * StringBuffer buf = new StringBuffer();
	 * buf.append("update resource set parent = ");
	 * buf.append(DBClient.format(collection.getId()));
	 * buf.append(", creation_date =");
	 * buf.append(DBClient.format(this.creationDate));
	 * buf.append(" where id = ");
	 * Iterator it = members.iterator();
	 * while (it.hasNext()) {
	 * ResourceImpl resource = (ResourceImpl) it.next();
	 * buf.append(DBClient.format(resource.id));
	 * if (it.hasNext())
	 * buf.append(" or id = ");
	 * }
	 * try {
	 * factory.dbclient.executeUpdate(buf.toString());
	 * } catch (java.sql.SQLException e) {
	 * factory.reportError("Journal.move", e);
	 * throw new ZenoException("DataBaseException");
	 * }
	 * this.modified();
	 * ((JournalImpl) collection).modified();
	 * }
	 */
	//obsolete use moveHere instead
	public void move(Set members, ZenoCollection collection) throws ZenoException {
		//to be replaced by paste
		factory.checkPermission("Journal.paste", collection);

		Iterator it = members.iterator();
		while (it.hasNext()) {
			try {
				ZenoResource resource = (ZenoResource) it.next();
				resource.move(collection);
			}
			catch (ZenoException e) {
			}
		}
	}


	/**
	 *  Description of the Method
	 *
	 *@param  members            Description of Parameter
	 *@exception  ZenoException  Description of Exception
	 */
	public void moveHere(Set members) throws ZenoException {

		factory.checkPermission("Journal.paste", this);

		Iterator it = members.iterator();
		boolean modified = false;
		while (it.hasNext()) {
			try {
				ResourceImpl resource = (ResourceImpl) it.next();
				resource.moveTo(this);
				modified = true;
			}
			catch (ZenoException e) {
			}
		}
		if (modified) {
			NumberHandler.setUnsetNumber(this);
			modified();
		}
	}


	/**
	 *  Description of the Method
	 *
	 *@param  collection         Description of Parameter
	 *@param  withLinks          Description of Parameter
	 *@return                    Description of the Returned Value
	 *@exception  ZenoException  Description of Exception
	 */
	public ZenoResource copy(ZenoCollection collection, boolean withLinks)
			 throws ZenoException {

		factory.checkPermission("Journal.paste", collection);
		if (collection instanceof TopicImpl) {
			throw new NoPermissionException("noSubJournals");
		}
		if (!contains(this.getId(), collection.getId())) {
			Hashtable newids = (withLinks) ? new Hashtable() : null;
			//heg
			JournalImpl newjournal = (JournalImpl) copy(collection, newids, "full");
			((JournalImpl) collection).modified();
			if (withLinks) {
				copyLinks(newids);
				List toplinks = getLinks(1);
				LinkImpl.copyLinks(factory, toplinks, newids);
			}

			return newjournal;
		}
		else {
			String msg = this.getId() + " contains " + collection.getId();
			factory.reportError("JournalImpl.copy", msg);
			return null;
		}
	}


	/**
	 *  Copy some members of this journal to some other collection. If a member in
	 *  the selected set has a link to another member of the set, it will be
	 *  updated to point the new id of the copy in the destination collection.
	 *  Links to other resources are copied unchanged, but the links of these other
	 *  resources will be updated, if permitted, to add links pointing to the
	 *  copies.
	 *
	 *@param  members            Description of Parameter
	 *@param  collection         Description of Parameter
	 *@param  withLinks          Description of Parameter
	 *@exception  ZenoException  Description of Exception
	 */

	//obsolete use copyHere instead
	public void copy(Set members, ZenoCollection collection, boolean withLinks)
			 throws ZenoException {

		factory.checkPermission("Journal.paste", collection);

		Set fmembers = filterSet(members, collection);

		Hashtable newids = (withLinks) ? new Hashtable() : null;
		boolean targetModified = false;
		Iterator mit = fmembers.iterator();
		while (mit.hasNext()) {
			ResourceImpl member = (ResourceImpl) mit.next();
			try {
				ResourceImpl copy =
						(ResourceImpl) member.copy(collection, newids, "full");
				targetModified = true;
			}
			catch (NoPermissionException e) {
				//to do report
			}
		}
		NumberHandler.setUnsetNumber(this);
		if (targetModified) {
			((JournalImpl) collection).modified();
		}

		if (withLinks) {
			Set ids = newids.keySet();
			if (!ids.isEmpty()) {
				Iterator it = ids.iterator();
				StringBuffer buf = new StringBuffer();
				buf.append("select * from link where source in (");
				while (it.hasNext()) {
					Integer integer = (Integer) it.next();
					buf.append(integer);
					if (it.hasNext()) {
						buf.append(", ");
					}
				}
				buf.append(")");
				//heg
				buf.append(" and source_mark='false' and target_mark='false'");
				List links = LinkImpl.getLinksWhere(factory, buf.toString());
				LinkImpl.copyLinks(factory, links, newids);
			}
		}

	}


	/**
	 *  Description of the Method
	 *
	 *@param  members            Description of Parameter
	 *@param  withLinks          Description of Parameter
	 *@exception  ZenoException  Description of Exception
	 */
	public void copyHere(Set members, boolean withLinks)
			 throws ZenoException {

		factory.checkPermission("Journal.paste", this);

		Set fmembers = filterSet(members, this);

		Hashtable newids = (withLinks) ? new Hashtable() : null;
		Iterator mit = fmembers.iterator();
		boolean targetModified = false;
		while (mit.hasNext()) {
			ResourceImpl member = (ResourceImpl) mit.next();
			try {
				ResourceImpl copy = (ResourceImpl) member.copy(this, newids, "full");
				targetModified = true;
			}
			catch (NoPermissionException e) {
				//to do report
			}
		}
		NumberHandler.setUnsetNumber(this);
		if (targetModified) {
			this.modified();
		}

		if (withLinks) {
			Set ids = newids.keySet();
			if (!ids.isEmpty()) {
				Iterator it = ids.iterator();
				StringBuffer buf = new StringBuffer();
				buf.append("select * from link where source in (");
				while (it.hasNext()) {
					Integer integer = (Integer) it.next();
					buf.append(integer);
					if (it.hasNext()) {
						buf.append(", ");
					}
				}
				buf.append(")");
				//heg
				buf.append(" and source_mark='false' and target_mark='false'");
				List links = LinkImpl.getLinksWhere(factory, buf.toString());
				LinkImpl.copyLinks(factory, links, newids);
			}
		}

	}


	/**
	 *  Checks whether a principal with the given id has the given role. Membership
	 *  in groups used to define roles is checked.
	 *
	 *@param  id                 Description of Parameter
	 *@param  role               Description of Parameter
	 *@return                    Description of the Returned Value
	 *@exception  ZenoException  Description of Exception
	 */

	public boolean hasRole(String id, String role) throws ZenoException {
		factory.checkPermission("Journal.hasRole", this);
		return factory.hasRole(this.id, id, role);
	}


	/**
	 *  Adds a principal (i.e. user or group) to the role. This change is made
	 *  immediately. There is no need to use the save method.
	 *
	 *@param  id                 The feature to be added to the PrincipalToRole
	 *      attribute
	 *@param  role               The feature to be added to the PrincipalToRole
	 *      attribute
	 *@exception  ZenoException  Description of Exception
	 */

	public void addPrincipalToRole(String id, String role) throws ZenoException {
		factory.checkPermission("Journal.addPrincipalToRole", this);
		factory.addPrincipalToRole(this.id, id, role);
		//
		this.modified = true;
		save();
		return;
	}


	/**
	 *  Deletes a principal from the given role. This change is made immediately.
	 *  There is no need to use the save method.
	 *
	 *@param  id                 Description of Parameter
	 *@param  role               Description of Parameter
	 *@exception  ZenoException  Description of Exception
	 */

	public void deletePrincipalFromRole(String id, String role)
			 throws ZenoException {
		factory.checkPermission("Journal.deletePrincipalFromRole", this);
		factory.deletePrincipalFromRole(this.id, id, role);
		this.modified = true;
		save();
		return;
	}


	/**
	 *  Replaces the role definition by the role definition of source If role is
	 *  null the definitions of all roles are replaced
	 *
	 *@param  source             Description of Parameter
	 *@param  role               Description of Parameter
	 *@param  recursively        Description of Parameter
	 *@exception  ZenoException  Description of Exception
	 */

	public void replaceRoleDefinition(Journal source, String role, boolean recursively)
			 throws ZenoException {

		List principals;
		List roles;
		if (role == null) {
			roles = getRoles();
		}
		else {
			roles = new ArrayList();
			roles.add(role);
		}
		Object[][] roledefs = new Object[roles.size()][2];
		for (int i = 0; i < roles.size(); i++) {
			role = (String) roles.get(i);
			principals = source.getRoleDefinition(role);
			roledefs[i][0] = role;
			roledefs[i][1] = principals;
		}
		replaceRoleDefinition(roledefs, recursively);
	}

	//----------------------------------------------------------

	/**
	 *  Description of the Method
	 *
	 *@exception  ZenoException  Description of Exception
	 */
	public void save() throws ZenoException {
		// permissions already checked by set methods
		try {
			// to do: begin transaction
			if (modified & loaded) {
				// save journal specific properties first
				// so that modified event is reported only
				// after all tables have been updated
				StringBuffer buf = new StringBuffer();
				buf.append("update journal");
				buf.append(" set article_labels=");
				buf.append(DBClient.format(this.articleLabels));
				buf.append(", qualifiers=");
				buf.append(DBClient.format(this.qualifiers));
				buf.append(", link_labels=");
				buf.append(DBClient.format(this.linkLabels));
				buf.append(", revision_period=");
				buf.append(DBClient.format(this.revisionPeriod));
				buf.append(", style_sheet=");
				buf.append(DBClient.format(this.styleSheet));
				buf.append(", attachment_size_limit=");
				buf.append(DBClient.format(this.attachmentSizeLimit));
				buf.append(", topic_mode=");
				buf.append(DBClient.format(this.topicMode));
				buf.append(" where id =");
				buf.append(this.id);
				factory.dbclient.executeUpdate(buf.toString());
				// do not reset modified variable, as this is done
				// by super.save()
			}
			super.save();
			// to do: commit transaction
		}
		catch (java.sql.SQLException e) {
			// to do: rollback
			factory.reportError("Journal.save", e);
			throw new ZenoException("DataBaseException");
		}
	}


	/**
	 *  Selects all articles in this journal created during the specified interval
	 *  with the given properties. Null can be used as a wild card, matching all
	 *  values. The full text parameter searches notes, keywords, and titles. The
	 *  full text parameter should be a white space separated list of words to
	 *  search for. Only articles in which ALL the words occur are selected. The
	 *  order parameter can be used to specify the order in which the articles are
	 *  list in the result. This is a comma space separated list of words from the
	 *  following set: "author", "title", "creation_date", "label", "qualifier".
	 *  Use the empty string if you do not want to specify an order.
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
	
	public Iterator search(
			String authorId,
			String title,
			String articleLabel,
			String qualifier,
			Date fromDate,
			Date toDate,
			String fullText,
			String order)
			 throws ZenoException {
			 	
		//String mode = factory.hasRole("editor", this) ? "editor" : "nn";
		List articles = search(authorId, title, articleLabel, qualifier,
								fromDate, toDate, fullText, order, "reader");
		return articles.iterator();
	}
	
	
	public List searchByTopic(
		String authorId,
		String title,
		String articleLabel,
		String qualifier,
		Date fromDate,
		Date toDate,
		String fullText)
				throws ZenoException {
				
		String mode = factory.hasRole("editor", this) ? "editor" : "nn";
		List articles = search(authorId, title, articleLabel, qualifier, 
										fromDate, toDate, fullText, 
										"part, rank, title", mode);
		List artcollections = ArticleCollection.groupArticlesByTopic(articles);
		List addedtopics = ArticleCollection.addTopics(artcollections);
		return ArticleCollection.addAll3(artcollections, addedtopics);		
	} 
					

	protected List search(
			String authorId,
			String title,
			String articleLabel,
			String qualifier,
			Date fromDate,
			Date toDate,
			String fullText,
			String order,
			String mode)
			 throws ZenoException {
		try {
			factory.checkPermission("Journal.search", this);
			StringBuffer buf = new StringBuffer();
			//buf.append("select resource.id from resource, article where ");
			buf.append("select * from resource, article where ");
			buf.append(" parent=");
			buf.append(DBClient.format(this.id));
			buf.append(" and resource.id=article.id");
			if (!mode.equals("editor")) {
				buf.append(" and marked_for_deletion='false'");
				buf.append(" and published='true'");
			}
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
			/*
			 * if (fullText != null) {
			 * buf.append(" and (match (resource.title, resource.note) against (");
			 * buf.append(DBClient.format(fullText));
			 * buf.append(")");
			 * buf.append(" or match (article.keywords) against (");
			 * buf.append(DBClient.format(fullText));
			 * buf.append("))");
			 * }
			 */
			if (fullText != null) {
				buf.append(" and (note like ");
				buf.append(DBClient.format(fullText));
				buf.append(" or title like ");
				buf.append(DBClient.format(fullText));
				buf.append(" or keywords like ");
				buf.append(DBClient.format(fullText));
				buf.append(")");
			}
			if (order != null) {
				if (!order.equals("")) {
					buf.append(" order by " + order);
				}
			}
			List list = new ArrayList();
			ResultSet rs = factory.dbclient.executeQuery(buf.toString());
			while (rs.next()) {

				//list.add(factory.loadResource(rs.getInt("id")));
				list.add(factory.loadArticle(rs));
			}
			//return list.iterator();
			return list;
		}
		catch (java.sql.SQLException e) {
			factory.reportError("Journal.search", e);
			throw new ZenoException("DataBaseException");
		}
	}


	/**
	 *  Description of the Method
	 *
	 *@param  commonName         Description of Parameter
	 *@param  email              Description of Parameter
	 *@param  preferredUsername  Description of Parameter
	 *@exception  ZenoException  Description of Exception
	 */
	public void register(String commonName, String email, String preferredUsername)
			 throws ZenoException {
		factory.checkPermission("Journal.register", this);
		// to do
		return;
	}


	/**
	 *  Description of the Method
	 *
	 *@exception  ZenoException  Description of Exception
	 */
	public void remove() throws ZenoException {
		factory.checkPermission("Journal.remove", this);
		if (markedForDeletion) {
			compact(false);
			destroy();
			JournalImpl parent = (JournalImpl) getParent();
			parent.modified();
		}
	}


	/**
	 *  Description of the Method
	 *
	 *@exception  ZenoException  Description of Exception
	 */
	public void compact() throws ZenoException {
		compact(true);
	}


	/**
	 *  Description of the Method
	 *
	 *@return                    Description of the Returned Value
	 *@exception  ZenoException  Description of Exception
	 */
	public PreviewElement genCompactPreview()
			 throws ZenoException {
		PreviewElement element = new PreviewElement("compact", this);
		genCompactPreview(element, 0);
		return element;
	}


	/**
	 *  Description of the Method
	 *
	 *@return                    Description of the Returned Value
	 *@exception  ZenoException  Description of Exception
	 */
	public PreviewElement genRemovePreview() throws ZenoException {
		PreviewElement element = genCompactPreview();
		element.setOperation("remove");
		if (markedForDeletion && element.getResult().equals("fully_compacted")) {
			element.setResult("removed");
		}
		return element;
	}


	/**
	 *  Description of the Method
	 *
	 *@return                    Description of the Returned Value
	 *@exception  ZenoException  Description of Exception
	 */
	public PreviewElement genDeletePreview() throws ZenoException {

		PreviewElement element = new PreviewElement("delete", this);
		boolean succcess = genDeletePreview(element, 0);
		return element;
	}


	/**
	 *  vali modes: undelete, xundelete
	 *
	 *@param  mode               Description of Parameter
	 *@return                    Description of the Returned Value
	 *@exception  ZenoException  Description of Exception
	 */

	public PreviewElement genUndeletePreview(String mode)
			 throws ZenoException {
		PreviewElement element = new PreviewElement(mode, this);
		boolean success = genUndeletePreview(element, 0);
		List subPreview = element.getSubPreview();
		if (subPreview.isEmpty()) {
			return success ? null : element;
		}
		else {
			Iterator it = subPreview.iterator();
			while (it.hasNext()) {
				PreviewElement current = (PreviewElement) it.next();
				current.setResult("undeleted");
			}
			element.setResult(success ? "undeleted" : "still_deleted");
			return element;
		}
	}


	/**
	 *  Description of the Method
	 *
	 *@return                    Description of the Returned Value
	 *@exception  ZenoException  Description of Exception
	 */
	public boolean unmarkWithAncestors() throws ZenoException {

		try {
			factory.checkPermission("ZenoResource.unmarkForDeletion", this);
			if (isParentMarked()) {
				JournalImpl parent =
						(JournalImpl) factory.loadResource(this.parentId);
				if (parent.unmarkWithAncestors()) {
					setDeletionMark(false);
					return true;
				}
				else {
					return false;
				}
			}
			else {
				setDeletionMark(false);
				return true;
			}
		}
		catch (NoPermissionException e) {
			return false;
		}
	}


	/**
	 *  Description of the Method
	 *
	 *@return    Description of the Returned Value
	 */
	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append("[Journal ");
		buf.append(this.id);
		buf.append(" ");
		buf.append(this.title);
		buf.append(" by ");
		buf.append(this.creator);
		buf.append("]");
		return buf.toString();
	}


	/**
	 *  Description of the Method
	 *
	 *@param  prw  Description of Parameter
	 */
	public void show(PrintWriter prw) {
		if (prw == null) {
			prw = new PrintWriter(System.out, true);
		}
		if (!loaded) {
			try {
				loadProperties();
			}
			catch (ZenoException e) {
			}
		}
		prw.println("<journal ");
		prw.println("    id=" + quote(id));
		prw.println("    class=" + quote(zenoClass));
		prw.println("    title=" + quote(title));
		prw.println("    rank=" + quote(rank));
		prw.println("    creator=" + quote(creator));
		prw.println("    creation_date=" + quote(creationDate));
		prw.println("    modifier=" + quote(modifier));
		prw.println("    modification_date: " + quote(modificationDate));
		prw.println("    parent_id=" + quote(parentId));
		prw.println("    marked_for_deletion=" + quote(markedForDeletion));
		prw.println("    closed=" + quote(closed));
		prw.println("    article_labels=" + quote(articleLabels));
		prw.println("    qualifiers=" + quote(qualifiers));
		prw.println("    link_labels=" + quote(linkLabels));
		prw.println("    revision_period=" + quote(revisionPeriod));
		prw.println("    style_sheet=" + quote(styleSheet));
		prw.println();
		prw.println("    loaded=" + quote(loaded));
		prw.println("    modified=" + quote(modified));
		prw.println(">");
		try {
			prw.println(getNote());
		}
		catch (ZenoException e) {
		}
		prw.println("</journal>");
	}



	/**
	 *  Return an set containing the members of this journal which are also
	 *  journals.
	 *
	 *@param  unmarkedOnly       Description of Parameter
	 *@return                    The Subjournals value
	 *@exception  ZenoException  Description of Exception
	 */

	protected Set getSubjournals(boolean unmarkedOnly) throws ZenoException {
		try {
			factory.checkPermission("Journal.getSubjournals", this);
			HashSet result = new HashSet();
			StringBuffer buf = new StringBuffer();
			buf.append("select * from resource, journal where ");
			buf.append(" parent=");
			buf.append(DBClient.format(this.id));
			buf.append(" and class='journal'");
			buf.append(" and resource.id=journal.id");
			if (unmarkedOnly) {
				buf.append(" and marked_for_deletion='false'");
			}
			buf.append(" order by rank, title");
			ResultSet rs = factory.dbclient.executeQuery(buf.toString());
			while (rs.next()) {
				Journal subjournal = factory.loadJournal(rs);
				if (factory.hasRole("reader", subjournal)) {
					result.add(subjournal);
				}
			}
			return result;
		}
		catch (java.sql.SQLException e) {
			factory.reportError("Journal.getSubjournals", e);
			throw new ZenoException("DataBaseException");
		}
	}


	/**
	 *  Gets the FreeArticles attribute of the JournalImpl object
	 *
	 *@return                    The FreeArticles value
	 *@exception  ZenoException  Description of Exception
	 */
	protected Iterator getFreeArticles() throws ZenoException {

		try {
			StringBuffer buf = new StringBuffer();
			buf.append("select * from resource, article");
			buf.append(" where parent=");
			buf.append(DBClient.format(this.id));
			buf.append(" and class='article'");
			buf.append(" and part=0");
			buf.append(" and marked_for_deletion='false'");
			buf.append(" and published = 'true'");
			buf.append(" and resource.id = article.id");
			buf.append(" order by rank, title");
			ResultSet rs = factory.dbclient.executeQuery(buf.toString());
			List list = new ArrayList();
			while (rs.next()) {
				list.add(factory.loadArticle(rs));
			}
			return list.iterator();
		}
		catch (java.sql.SQLException e) {
			factory.reportError("Journal.getArticles", e);
			throw new ZenoException("DataBaseException");
		}
	}


	/**
	 *  Description of the Method
	 *
	 *@param  rs                         Description of Parameter
	 *@exception  java.sql.SQLException  Description of Exception
	 */
	protected void loadProperties(ResultSet rs) throws java.sql.SQLException {
		/*
		 * assumes the rs is not null and not empty
		 */
		this.articleLabels = rs.getString("article_labels");
		this.qualifiers = rs.getString("qualifiers");
		this.linkLabels = rs.getString("link_labels");
		this.revisionPeriod = rs.getInt("revision_period");
		this.styleSheet = rs.getString("style_sheet");
		this.attachmentSizeLimit = rs.getInt("attachment_size_limit");
		this.topicMode = rs.getBoolean("topic_mode");
		this.loaded = true;
	}


	/**
	 *  Description of the Method
	 *
	 *@param  mode               Description of Parameter
	 *@return                    Description of the Returned Value
	 *@exception  ZenoException  Description of Exception
	 */
	protected int articleCount(String mode) throws ZenoException {
		try {
			StringBuffer buf = new StringBuffer();
			buf.append("select count(*) from resource, article");
			buf.append(" where parent=");
			buf.append(DBClient.format(this.id));
			buf.append(" and resource.id = article.id ");
			if ("unmarked".equals(mode)) {
				buf.append(" and marked_for_deletion='false'");
				buf.append(" and published='true'");
			}
			else if ("marked".equals(mode)) {
				buf.append(" and marked_for_deletion='true'");
			}
			ResultSet rs = factory.dbclient.executeQuery(buf.toString());
			if (rs.next()) {
				return rs.getInt(1);
			}
			else {
				return 0;
			}
		}
		catch (java.sql.SQLException e) {
			factory.reportError("Journal.articleCount", e);
			throw new ZenoException("DataBaseException");
		}
	}


	/**
	 *  Checks whether id is a direct or indirect member of parentid
	 *
	 *@param  parentid  Description of Parameter
	 *@param  id        Description of Parameter
	 *@return           Description of the Returned Value
	 */

	protected boolean contains(int parentid, int id) {
		if (parentid == id) {
			return true;
		}
		StringBuffer buf = new StringBuffer();
		buf.append("select id from resource where ");
		buf.append(" parent=");
		buf.append(DBClient.format(parentid));
		buf.append(" and class='journal'");
		try {
			ResultSet rs = factory.dbclient.executeQuery(buf.toString());
			while (rs.next()) {
				int subid = rs.getInt("id");
				if (contains(subid, id)) {
					return true;
				}
			}
		}
		catch (java.sql.SQLException e) {
		}
		return false;
	}


	/**
	 *  Description of the Method
	 *
	 *@param  collection         Description of Parameter
	 *@exception  ZenoException  Description of Exception
	 */
	protected void moveTo(ZenoCollection collection) throws ZenoException {

		factory.checkPermission("ZenoResource.move", this);
		if (collection instanceof TopicImpl) {
			throw new NoPermissionException("noCollectionInTopic");
		}
		if (contains(this.getId(), collection.getId())) {
			String msg = this.getId() + " contains " + collection.getId();
			factory.reportError("JournalImpl.move", msg);
			throw new NoPermissionException("noCycle");
		}
		JournalImpl parent = (JournalImpl) getParent();

		try {
			int newParentId = collection.getId();
			StringBuffer buf = new StringBuffer();
			buf.append("update resource set parent=");
			buf.append(DBClient.format(newParentId));
			buf.append(", creation_date =");
			buf.append(DBClient.format(this.creationDate));
			buf.append(" where id=");
			buf.append(DBClient.format(this.id));
			factory.dbclient.executeUpdate(buf.toString());
			this.parentId = newParentId;
		}
		catch (java.sql.SQLException e) {
			factory.reportError("Journal.move", e);
			throw new ZenoException("DataBaseException");
		}

		parent.modified();
	}


	/**
	 *  Description of the Method
	 *
	 *@param  collection         Description of Parameter
	 *@param  newids             Description of Parameter
	 *@param  mode               Description of Parameter
	 *@return                    Description of the Returned Value
	 *@exception  ZenoException  Description of Exception
	 */
	protected ZenoResource copy(ZenoCollection collection, Hashtable newids, String mode)
			 throws ZenoException {

		factory.checkPermission("Journal.copy", this);

		Journal journal = (Journal) super.copy(collection, newids, mode);
		if (mode.equals("adopt")) {
			journal.replaceRoleDefinition((Journal) collection, null, false);
		}
		else {
			journal.replaceRoleDefinition(this, null, false);
		}

		Iterator it;
		it = getFreeArticles();
		while (it.hasNext()) {
			ResourceImpl member = (ResourceImpl) it.next();
			try {
				ResourceImpl copy = (ResourceImpl) member.copy(journal, newids, mode);
				Integer oldId = new Integer(member.id);
				Integer newId = new Integer(copy.id);
				newids.put(oldId, newId);
			}
			catch (NoPermissionException e) {
				//to do report ???
			}
		}

		it = getTopics().iterator();
		while (it.hasNext()) {
			ResourceImpl member = (ResourceImpl) it.next();
			try {
				ResourceImpl copy = (ResourceImpl) member.copy(journal, newids, mode);
				Integer oldId = new Integer(member.id);
				Integer newId = new Integer(copy.id);
				newids.put(oldId, newId);
			}
			catch (NoPermissionException e) {
				//to do report ???
			}
		}

		it = getSubjournals().iterator();
		while (it.hasNext()) {
			ResourceImpl member = (ResourceImpl) it.next();
			try {
				ResourceImpl copy = (ResourceImpl) member.copy(journal, newids, mode);
				Integer oldId = new Integer(member.id);
				Integer newId = new Integer(copy.id);
				newids.put(oldId, newId);
			}
			catch (NoPermissionException e) {
				//to do report ???
			}
		}

		return journal;
	}



	/**
	 *  Description of the Method
	 *
	 *@param  newids             Description of Parameter
	 *@exception  ZenoException  Description of Exception
	 */
	protected void copyLinks(Hashtable newids) throws ZenoException {

		StringBuffer buf = new StringBuffer();
		buf.append(
				"select label, source_alias, target_alias, source, target, flag from link, resource");
		buf.append(" where source=id and  parent=");
		buf.append(DBClient.format(this.id));
		//heg
		buf.append(" and source_mark='false' and target_mark='false'");
		try {
			List links = LinkImpl.getLinksWhere(factory, buf.toString());
			LinkImpl.copyLinks(factory, links, newids);
		}
		catch (ZenoException e) {
			factory.reportError("Journal.copyLinks", e);
		}

		Set subjournals = getSubjournals();
		Iterator it = subjournals.iterator();
		while (it.hasNext()) {
			JournalImpl subjournal = (JournalImpl) it.next();
			try {
				subjournal.copyLinks(newids);
			}
			catch (ZenoException e) {
				factory.reportError("Journal.copyLinks", e);
			}
		}

	}


	/**
	 *  Description of the Method
	 *
	 *@param  members  Description of Parameter
	 *@param  target   Description of Parameter
	 *@return          Description of the Returned Value
	 */
	protected Set filterSet(Set members, ZenoCollection target) {
		Set fmembers = new HashSet();
		Iterator mit = members.iterator();
		while (mit.hasNext()) {
			ZenoResource resource = (ZenoResource) mit.next();
			if (resource instanceof ZenoCollection) {
				if (!contains(resource.getId(), target.getId())) {
					fmembers.add(resource);
				}
				else {
					String msg = resource.getId() + " contains " + target.getId();
					factory.reportError("JournalImpl.move", msg);
				}
			}
			else {
				fmembers.add(resource);
			}
		}
		return fmembers;
	}


	/**
	 *  Description of the Method
	 *
	 *@param  roledefs           Description of Parameter
	 *@param  recursively        Description of Parameter
	 *@exception  ZenoException  Description of Exception
	 */
	protected void replaceRoleDefinition(Object[][] roledefs, boolean recursively)
			 throws ZenoException {

		factory.checkPermission("Journal.addPrincipalToRole", this);
		factory.replaceRoleDefinition(this.getId(), roledefs);

		if (recursively) {
			Iterator subjnIterator = getSubjournals().iterator();
			while (subjnIterator.hasNext()) {
				JournalImpl subjn = (JournalImpl) subjnIterator.next();
				try {
					subjn.replaceRoleDefinition(roledefs, recursively);
				}
				catch (NoPermissionException e) {
					System.out.println("no replace permission for " + subjn);
				}
			}
		}

		this.modified = true;
		save();
		return;
	}

	/**
	 *  Description of the Method
	 *
	 *@exception  ZenoException  Description of Exception
	 */
	protected void destroy() throws ZenoException {

		if (!markedForDeletion) {
			return;
		}

		//if (!isEmpty()) return;
		StringBuffer buf;
		buf = new StringBuffer();
		buf.append("select count(*) from resource");
		buf.append(" where parent=");
		buf.append(DBClient.format(getId()));
		try {
			ResultSet rs1 = factory.dbclient.executeQuery(buf.toString());
			if (rs1.next() && (rs1.getInt(1) != 0)) {
				System.out.println(getId() + " not empty");
				return;
			}
		}
		catch (java.sql.SQLException e) {
			return;
		}

		try {
			// delete from resource table
			buf = new StringBuffer();
			buf.append("delete from resource where id=");
			buf.append(DBClient.format(getId()));
			factory.dbclient.executeUpdate(buf.toString());

			// delete rows from link table
			buf = new StringBuffer();
			buf.append("delete from link where source=");
			buf.append(DBClient.format(getId()));
			buf.append(" or target=");
			buf.append(DBClient.format(getId()));
			factory.dbclient.executeUpdate(buf.toString());

			// delete rows from xlink table
			buf = new StringBuffer();
			buf.append("delete from xlink where source=");
			buf.append(DBClient.format(getId()));
			factory.dbclient.executeUpdate(buf.toString());

			// delete rows from property table
			buf = new StringBuffer();
			buf.append("delete from property where resource=");
			buf.append(DBClient.format(getId()));
			factory.dbclient.executeUpdate(buf.toString());

			// delete rows from journal table
			buf = new StringBuffer();
			buf.append("delete from journal where id=");
			buf.append(DBClient.format(getId()));
			factory.dbclient.executeUpdate(buf.toString());

			// delete role definitions
			buf = new StringBuffer();
			buf.append("delete from role where journal=");
			buf.append(DBClient.format(getId()));
			factory.dbclient.executeUpdate(buf.toString());

		}
		catch (Exception e) {
			factory.reportError("Journal.destroy", e);
		}
	}


	/**
	 *  Description of the Method
	 *
	 *@param  checkPermission    Description of Parameter
	 *@exception  ZenoException  Description of Exception
	 */
	protected void compact(boolean checkPermission) throws ZenoException {

		try {
			if (checkPermission) {
				factory.checkPermission("ZenoCollection.compact", this);
			}

			// first compact articles marked for deletion

			StringBuffer buf = new StringBuffer();
			buf.append("select id from resource where parent = ");
			buf.append(DBClient.format(id));
			buf.append(" and marked_for_deletion='true'");
			buf.append(" and (class='article' or class='topic')");
			ResultSet rs = factory.dbclient.executeQuery(buf.toString());
			while (rs.next()) {
				// to do: start transaction

				// delete rows from resource table
				buf = new StringBuffer();
				buf.append("delete from resource where id=");
				buf.append(DBClient.format(rs.getInt("id")));
				factory.dbclient.executeUpdate(buf.toString());

				// delete rows from article table
				buf = new StringBuffer();
				buf.append("delete from article where id=");
				buf.append(DBClient.format(rs.getInt("id")));
				factory.dbclient.executeUpdate(buf.toString());

				// delete rows from link table
				buf = new StringBuffer();
				buf.append("delete from link where source=");
				buf.append(DBClient.format(rs.getInt("id")));
				buf.append(" or target=");
				buf.append(DBClient.format(rs.getInt("id")));
				factory.dbclient.executeUpdate(buf.toString());

				// delete rows from xlink table
				buf = new StringBuffer();
				buf.append("delete from xlink where source=");
				buf.append(DBClient.format(rs.getInt("id")));
				factory.dbclient.executeUpdate(buf.toString());

				// delete rows from property table
				buf = new StringBuffer();
				buf.append("delete from property where resource=");
				buf.append(DBClient.format(rs.getInt("id")));
				factory.dbclient.executeUpdate(buf.toString());

				// delete rows from attachment table

				/*
				 * buf = new StringBuffer();
				 * buf.append("delete from attachment where article=");
				 * buf.append(DBClient.format(rs.getInt("id")));
				 * factory.dbclient.executeUpdate(buf.toString());
				 */

				AttachmentImpl.deleteAttachments(factory, rs.getInt("id"));
			}

			// compact subjournals and destroy those marked for deletion
			Iterator subjournals = this.getMembers("journal", true).iterator();

			while (subjournals.hasNext()) {
				JournalImpl sj = (JournalImpl) subjournals.next();
				try {
					//getMarkedForDeletion() is permission checked
					if (sj.markedForDeletion) {
						//compact in any case and destroy
						sj.compact(false);
						sj.destroy();
						this.modified = true;
					}
					else {
						sj.compact(true);
					}
				}
				catch (NoPermissionException e) {
				}
			}
			// report modification
			save();

			// to do: commit transaction
		}
		catch (java.sql.SQLException e) {
			// to do: rollback transaction
			factory.reportError("Journal.compact", e);
			throw new ZenoException("DataBaseException");
		}
	}


	/**
	 *  Description of the Method
	 *
	 *@param  source             Description of Parameter
	 *@exception  ZenoException  Description of Exception
	 */
	private void initRoleDefinition(Journal source)
			 throws ZenoException {

		if (source == null) {
			factory.addPrincipalToRole(this.id, "editor");
		}
		else {
			Iterator rit = source.getRoles().iterator();
			while (rit.hasNext()) {
				String role = (String) rit.next();
				Iterator pit = source.getRoleDefinition(role).iterator();
				while (pit.hasNext()) {
					String principal = (String) pit.next();
					factory.addPrincipalToRole(this.id, principal, role);
				}
			}
		}
	}


	/**
	 *  Description of the Method
	 *
	 *@param  element            Description of Parameter
	 *@param  level              Description of Parameter
	 *@exception  ZenoException  Description of Exception
	 */
	private void genCompactPreview(PreviewElement element, int level)
			 throws ZenoException {

		String result = "not_compacted";
		boolean report = false;
		List editors = Collections.EMPTY_LIST;
		PreviewElement current = element;

		try {

			if (markedForDeletion) {
				report = true;
				result = (level == 0) ? "fully_compacted" : "removed";
			}
			else {
				factory.checkPermission("ZenoCollection.compact", this);
				if (articleCount("marked") > 0) {
					report = true;
					result = "partially_compacted";
				}
				else if (level == 0) {
					report = true;
				}
			}

			if (level == 0 || !markedForDeletion) {

				List journals = getMembers("journal", true);
				Iterator it = journals.iterator();
				while (it.hasNext()) {
					JournalImpl res = (JournalImpl) it.next();
					res.genCompactPreview(element, level + 1);
				}
			}

		}
		catch (NoPermissionException e) {
			report = true;
			editors = factory.loadRoleDefinition(getId(), "editor");
			if (editors.isEmpty()) {
				String zenoAdmin = monitor.getProperty("zenoUserName", "NN");
				editors.add(zenoAdmin);
			}
			result = "not_permitted";
		}

		if (report) {
			if (level > 0) {
				current = new PreviewElement(this);
				element.add(current);
			}
			current.setResult(result);
			current.setEditors(editors);
		}
	}


	/**
	 *  Description of the Method
	 *
	 *@param  element            Description of Parameter
	 *@param  level              Description of Parameter
	 *@return                    Description of the Returned Value
	 *@exception  ZenoException  Description of Exception
	 */
	private boolean genDeletePreview(PreviewElement element, int level)
			 throws ZenoException {

		boolean totalsuccess = true;
		int nextlevel = level + 1;
		try {
			if (markedForDeletion && level == 0) {
				element.setResult("is_deleted");
				return totalsuccess;
			}
			factory.checkPermission("ZenoResource.markForDeletion", this);
			List journals = getMembers("journal", false);
			Iterator it = journals.iterator();
			while (it.hasNext()) {
				JournalImpl res = (JournalImpl) it.next();
				PreviewElement current = element;
				if (nextlevel < 2) {
					current = new PreviewElement(res);
					element.add(current);
				}
				boolean success = res.genDeletePreview(current, nextlevel);
				totalsuccess = totalsuccess && success;
			}
			if (level < 2) {
				String result = totalsuccess ? "fully_deleted" : "partially_deleted";
				element.setResult(result);
			}
		}
		catch (NoPermissionException e) {
			totalsuccess = false;
			List editors = factory.loadRoleDefinition(getId(), "editor");
			if (editors.isEmpty()) {
				String zenoAdmin = monitor.getProperty("zenoUserName", "NN");
				editors.add(zenoAdmin);
			}
			if (level < 2) {
				element.setResult("not_permitted");
				element.setEditors(editors);
			}
			else {
				PreviewElement subelement = new PreviewElement(this);
				subelement.setResult("not_permitted");
				subelement.setEditors(editors);
				element.add(subelement);
			}
		}
		return totalsuccess;
	}


	/**
	 *  Description of the Method
	 *
	 *@param  element            Description of Parameter
	 *@param  level              Description of Parameter
	 *@return                    Description of the Returned Value
	 *@exception  ZenoException  Description of Exception
	 */
	private boolean genUndeletePreview(PreviewElement element, int level)
			 throws ZenoException {

		PreviewElement current = element;
		int nextlevel = level - 1;
		if (level < 0) {
			current = new PreviewElement(this);
			current.setResult("still_deleted");
			element.add(current);
		}
		try {
			factory.checkPermission("ZenoResource.unmarkForDeletion", this);
			if (isParentMarked()) {
				JournalImpl parent = (JournalImpl) factory.loadResource(this.parentId);
				return parent.genUndeletePreview(element, nextlevel);
			}
			else {
				return true;
			}
		}
		catch (NoPermissionException e) {
			List editors = factory.loadRoleDefinition(getId(), "editor");
			if (editors.isEmpty()) {
				String zenoAdmin = monitor.getProperty("zenoUserName", "NN");
				editors.add(zenoAdmin);
			}
			current.setResult("not_permitted");
			current.setEditors(editors);
			if (isParentMarked()) {
				JournalImpl parent = (JournalImpl) factory.loadResource(this.parentId);
				parent.genUndeletePreview(element, nextlevel);
			}
			return false;
		}
	}


}

