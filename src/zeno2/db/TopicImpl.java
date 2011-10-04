package zeno2.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Collections;
import java.util.Comparator;
import java.util.Set;

import zeno2.kernel.Article;
import zeno2.kernel.BeginEvent;
import zeno2.kernel.CreationEvent;
import zeno2.kernel.EndEvent;
import zeno2.kernel.ExpirationEvent;
import zeno2.kernel.Journal;
import zeno2.kernel.Link;
import zeno2.kernel.ModificationEvent;
import zeno2.kernel.NoPermissionException;
import zeno2.kernel.OutlineNode;
import zeno2.kernel.Topic;
import zeno2.kernel.ZenoResource;
import zeno2.kernel.ZenoCollection;
import zeno2.kernel.ZenoEvent;
import zeno2.kernel.ZenoException;



/** Zeno2 Topic Implementation */

 public class TopicImpl extends ArticleImpl
		implements Topic 
	{

	public TopicImpl(FactoryImpl factory) {
		super(factory);
		zenoClass = "topic";
		//temp
		isTopic = true;
	}

	public TopicImpl(FactoryImpl factory, int id) {
		super(factory, id);
		zenoClass = "topic";
		//temp
		isTopic = true;
	}
	 
	
	public void create(ZenoCollection parent) throws ZenoException {
		super.create(parent);
		this.part = this.id;
		StringBuffer buf = new StringBuffer();
		buf.append("update resource set part=");
		buf.append(DBClient.format(this.id));
		buf.append(", creation_date =");
		buf.append(DBClient.format(this.creationDate));
		buf.append(" where id=");
		buf.append(DBClient.format(this.id));
		try {
			factory.dbclient.executeUpdate(buf.toString());
		} catch (java.sql.SQLException e) {
			factory.reportError("Topic.create", e);
			throw new ZenoException("DataBaseException");
		}
		
	}
	
	public void finish(boolean anonymously) throws ZenoException {
		
		factory.checkPermission("Article.finish", this);
		if (anonymously)
			finishAnonymously(false);
	}
	
	protected List getMembers(boolean all) throws ZenoException {
		try {
			List result = new ArrayList();
			StringBuffer buf = new StringBuffer();
			buf.append("select * from resource where part = ");
			buf.append(DBClient.format(id));
			if (! all)
				buf.append(" and marked_for_deletion='false'");
			buf.append(" order by rank, title");
			ResultSet rs = factory.dbclient.executeQuery(buf.toString());
			while (rs.next()) {
				result.add(factory.loadResource(rs));
			}
			return result;
		} catch (java.sql.SQLException e) {
			factory.reportError("Topic.getMembers", e);
			throw new ZenoException("DataBaseException");
		}
	}
	
	/** Returns an enumeration of the members of this collection which are not
	marked for deletion. Each member is a ZenoResource. The order of the
	members in this enumeration is unspecified. */

	public java.util.Iterator getMembers() throws ZenoException {
		try {
			List result = new ArrayList();
			StringBuffer buf = new StringBuffer();
			buf.append("select * from resource where part = ");
			buf.append(DBClient.format(id));
			buf.append(" and marked_for_deletion='false'");
			ResultSet rs = factory.dbclient.executeQuery(buf.toString());
			while (rs.next()) {
				result.add(factory.loadResource(rs));
			}
			return result.iterator();
		} catch (java.sql.SQLException e) {
			factory.reportError("Topic.getMembers", e);
			throw new ZenoException("DataBaseException");
		}
	}

	/** Returns the members sorted by rank and then title, not including
	members marked for deletion. */

	public java.util.Iterator sortMembers() throws ZenoException {
		try {
			List result = new ArrayList();
			StringBuffer buf = new StringBuffer();
			buf.append("select * from resource where part = ");
			buf.append(DBClient.format(id));
			buf.append(" and marked_for_deletion='false'");
			buf.append(" order by rank, title");
			ResultSet rs = factory.dbclient.executeQuery(buf.toString());
			while (rs.next()) {
				result.add(factory.loadResource(rs));
			}
			return result.iterator();
		} catch (java.sql.SQLException e) {
			factory.reportError("Topic.sortMembers", e);
			throw new ZenoException("DataBaseException");
		}
	}
	
	public void compact() throws ZenoException {
		
		factory.checkPermission("ZenoCollection.compact", this);
		
		try {
			StringBuffer buf = new StringBuffer();
			buf.append("select id from resource where part = ");
			buf.append(DBClient.format(id));
			buf.append(" and marked_for_deletion='true'");
			buf.append(" and class='article'");
			ResultSet rs = factory.dbclient.executeQuery(buf.toString());
			while (rs.next()) {
				removeArticle(rs.getInt("id"));
				this.modified = true;
			}
			if (this.markedForDeletion) {
				removeArticle(this.id);
				this.modified = true;
			} else
				save();
		
			if (this.modified) {
				JournalImpl parent = (JournalImpl)getParent();
				parent.modified();
			}
			// to do: commit transaction
		} catch (java.sql.SQLException e) {
			// to do: rollback transaction
			factory.reportError("Topic.compact", e);
			throw new ZenoException("DataBaseException");
		}
	}
	
	public void removeArticles(List ids) throws ZenoException {
		
		factory.checkPermission("ZenoCollection.compact", this);
		if (ids == null || ids.isEmpty())
			return;
		else if (this.markedForDeletion && 
						(ids.contains(Integer.toString(this.id)) ||
						ids.contains(new Integer(this.id))))
			compact();
		else {			
			try {
				StringBuffer buf = new StringBuffer();
				buf.append("select id from resource where part = ");
				buf.append(DBClient.format(id));
				buf.append(" and marked_for_deletion='true'");
				buf.append(" and id in (");
				Iterator it = ids.iterator();
				while(it.hasNext()) {
					buf.append(it.next());
					if (it.hasNext()) 
						buf.append(",");
				}
				buf.append(")");
					
				ResultSet rs = factory.dbclient.executeQuery(buf.toString());
				
				while (rs.next()) {
					removeArticle(rs.getInt("id"));
					this.modified = true;
				}
				if (this.modified) {
					JournalImpl parent = (JournalImpl)getParent();
					parent.modified();
				}
				save();
		
			} catch (java.sql.SQLException e) {
				factory.reportError("Topic.compact", e);
				throw new ZenoException("DataBaseException");
			}
		}
	}
	
	private void removeArticle(int id) throws ZenoException {
			//id should be a member
		try {
			// delete row from resource table
			StringBuffer buf = new StringBuffer();
			buf.append("delete from resource where id=");
			buf.append(DBClient.format(id));
			factory.dbclient.executeUpdate(buf.toString());

			// delete row from article table
			buf = new StringBuffer();
			buf.append("delete from article where id=");
			buf.append(DBClient.format(id));
			factory.dbclient.executeUpdate(buf.toString());

			// delete rows from link table
			buf = new StringBuffer();
			buf.append("delete from link where source=");
			buf.append(DBClient.format(id));
			buf.append(" or target=");
			buf.append(DBClient.format(id));
			factory.dbclient.executeUpdate(buf.toString());
			
			// delete rows from xlink table
			buf = new StringBuffer();
			buf.append("delete from xlink where source=");
			buf.append(DBClient.format(id));
			factory.dbclient.executeUpdate(buf.toString());

			// delete rows from property table
			buf = new StringBuffer();
			buf.append("delete from property where resource=");
			buf.append(DBClient.format(id));
			factory.dbclient.executeUpdate(buf.toString());

			// delete rows from attachment table
			AttachmentImpl.deleteAttachments(factory, id);
		} catch (java.sql.SQLException e) {
			factory.reportError("Topic.compact", e);
			throw new ZenoException("DataBaseException");
		}
	
	}
		

	/** Returns a set of all member resources which have been marked
	for deletion in this collection.  Does not list resources in member
	collections. */

	public Iterator getTrash() throws ZenoException {
		try {
			List result = new ArrayList();
			StringBuffer buf = new StringBuffer();
			buf.append("select * from resource where part = ");
			buf.append(DBClient.format(id));
			buf.append(" and marked_for_deletion='true'");
			buf.append(" order by title");
			ResultSet rs = factory.dbclient.executeQuery(buf.toString());
			while (rs.next()) {
				ZenoResource resource = factory.loadResource(rs);
				if (factory.hasRole("reader", resource))
					result.add(resource);
			}
			return result.iterator();
		} catch (java.sql.SQLException e) {
			factory.reportError("Topic.getTrash", e);
			throw new ZenoException("DataBaseException");
		}
	}
	
	public boolean isTopic() throws ZenoException {
		//temp
		factory.checkPermission("Article.isTopic", this);
		return true;
	}
	
	public void setIsTopic(boolean isTopic) throws ZenoException {
		//temp 
		if (! isTopic) 
			dissolve();
	}
	
	public Article dissolve() throws ZenoException {
	
		factory.checkPermission("Topic.dissolve", this);
		Article article = transformTopic(0);
		moveMembersTo(this.parentId, 0);
		return article;
	}
		
	protected Article transformTopic(int newpart) throws ZenoException {
		
		StringBuffer buf = new StringBuffer();
		buf.append("update resource set class='article'");
		buf.append(", part=");
		buf.append(DBClient.format(newpart));
		buf.append(", creation_date =");
		buf.append(DBClient.format(this.creationDate));
		buf.append(" where id=");
		buf.append(DBClient.format(this.id));
		try {
			factory.dbclient.executeUpdate(buf.toString());
		} catch (java.sql.SQLException e) {
			factory.reportError("ArticleImpl.transformTopic", e);
			throw new ZenoException("DBException");
		}
		//temp
		buf.setLength(0);
		buf.append("update article set is_topic=");
		buf.append(DBClient.format(false));
		buf.append(" where id =");
		buf.append(DBClient.format(this.id));
		try {
			factory.dbclient.executeUpdate(buf.toString());
		} catch (java.sql.SQLException e) {
			factory.reportError("ArticleImpl.transformArticle", e);
			throw new ZenoException("DBException");
		}
		return (ArticleImpl)factory.loadResource(this.id);
	}
	
	protected void moveMembersTo(int newparent, int newpart) throws ZenoException {
		
		StringBuffer buf = new StringBuffer();
		
		try {	
			buf.append("update resource set creation_date= ?");
			buf.append(", parent= ");
			buf.append(DBClient.format(newparent));
			buf.append(", part=");
			buf.append(DBClient.format(newpart));
			buf.append(" where id= ?");
			PreparedStatement pdstm;
			Connection con = factory.dbclient.getConnection();
			pdstm = con.prepareStatement(buf.toString());
			
			buf.setLength(0);
			buf.append("select id, creation_date from resource");
			buf.append(" where part=");
			buf.append(DBClient.format(this.id));
			ResultSet rs = factory.dbclient.executeQuery(buf.toString());
			
			while(rs.next()) {
				try {
					int cid = rs.getInt("id");
					java.sql.Timestamp  date = rs.getTimestamp("creation_date");
					pdstm.clearParameters();
					pdstm.setTimestamp(1, date);
					pdstm.setInt(2, cid);
					pdstm.executeUpdate();
				} catch (java.sql.SQLException e) {
					factory.reportError("Topic.moveMembersTo", e);
				}
			}
			
		} catch (java.sql.SQLException e) {
			factory.reportError("Topic.moveMembersTo", e);
			throw new ZenoException("DataBaseException");
		}
	}
	
	public void move(ZenoCollection collection) throws ZenoException {
		
		factory.checkPermission("ZenoCollection.paste", collection);
		moveTo(collection);
		JournalImpl parent;
		if (collection instanceof Topic)
			parent = (JournalImpl)collection.getParent();
		else
			parent = (JournalImpl)collection;
		NumberHandler.setUnsetNumber(parent);
		parent.modified();
		if (collection instanceof Topic)
			((TopicImpl)collection).modified();
	}
	
	protected void moveTo(ZenoCollection collection) throws ZenoException {
		
		factory.checkPermission("ZenoResource.move", this);
		JournalImpl newParent;
		int newpart = this.id;
		if (collection instanceof Topic) {
			newParent = (JournalImpl)collection.getParent();
			newpart = collection.getId();
		} else
			newParent = (JournalImpl)collection;
		if (newParent.getId() != this.parentId)
			NumberHandler.unsetNumber(this);
			
		moveMembersTo(newParent.getId(), newpart);
		if (collection instanceof Topic)
			transformTopic(newpart);
		
		if (newParent.getId() != this.parentId)
			((JournalImpl)getParent()).modified();
	}
					
	
	public Article copyAsArticle(ZenoCollection collection, boolean withLinks) 
			throws ZenoException {
		
		factory.checkPermission("ZenoCollection.paste", collection);
		JournalImpl parent;
		int copyPart = 0;
		if (collection instanceof TopicImpl) {
			parent = (JournalImpl)collection.getParent();
			copyPart = collection.getId();
		} else {
			parent = (JournalImpl)collection;
		}
		Hashtable newids = (withLinks) ? new Hashtable() : null;
		TopicImpl copy = (TopicImpl)super.copy(parent, newids, "full");
		Article article = copy.transformTopic(copyPart);
		NumberHandler.setNextNumber((ArticleImpl)copy);
		
		if (collection instanceof Topic) {
			((JournalImpl)collection.getParent()).modified();
			((TopicImpl)collection).modified();
		} else
			((JournalImpl)collection.getParent()).modified();
		
		return article;
	}
	
	public ZenoResource copy(ZenoCollection collection, boolean withLinks)
		throws ZenoException {
		
		factory.checkPermission("ZenoCollection.paste", collection);
		Hashtable newids = (withLinks) ? new Hashtable() : null;
		ZenoResource copy = copy(collection, newids, "full");
		JournalImpl parent;
		if (collection instanceof Topic)
			parent = (JournalImpl)collection.getParent();
		else
			parent = (JournalImpl)collection;
		NumberHandler.setUnsetNumber(parent);
		parent.modified();
		if (collection instanceof Topic)
			((TopicImpl)collection).modified();
			
		if (withLinks)
			copyLinks(newids);
		
		return copy;
	}		
	
	public ZenoResource copy(ZenoCollection collection, Hashtable newids, String mode) 
			throws ZenoException {
		
		factory.checkPermission("Topic.copy", this);
		
		JournalImpl parent;
		if (collection instanceof TopicImpl) {
			parent = (JournalImpl)collection.getParent();
		} else {
			parent = (JournalImpl)collection;
		}
		ZenoResource copy = super.copy(parent, newids, mode);
		int copyPart = copy.getId();
		if (collection instanceof Topic) {
			copyPart = collection.getId();
			((TopicImpl)copy).transformTopic(copyPart);
		}
		
		Iterator mit = getMembers();
		while (mit.hasNext()) {
			ResourceImpl member = (ResourceImpl) mit.next();
			if (member.id != this.id) {
				member.part = copyPart;
				ResourceImpl mcopy = 
					(ResourceImpl) member.copy(collection, newids, mode);
				member.part = this.id;
				Integer oldId = new Integer(member.id);
				Integer newId = new Integer(mcopy.id);
				newids.put(oldId, newId);
			}
		}
		return (Article)factory.loadResource(copy.getId());
	}
	
	protected ZenoResource copy(ZenoCollection collection, Hashtable newids, 
									String mode, boolean copynr)
			throws ZenoException {
		
		factory.checkPermission("Topic.copy", this);
		
		TopicImpl topic = 
			(TopicImpl)super.copy(collection, newids, mode, copynr);
	
		Iterator mit = getMembers();
		while (mit.hasNext()) {
			ArticleImpl member = (ArticleImpl) mit.next();
			if (member.id != this.id) {
				member.part = topic.id;
				ResourceImpl copy = 
					(ResourceImpl)member.copy(collection, newids, mode, copynr);
				member.part = this.id;
				Integer oldId = new Integer(member.id);
				Integer newId = new Integer(copy.id);
				newids.put(oldId, newId);
			}
		}
		return topic;
	}
	
	
	protected void copyLinks(Hashtable newids) throws ZenoException {

		StringBuffer buf = new StringBuffer();
		buf.append(
			"select label, source_alias, target_alias, source, target, flag from link, resource");
		buf.append(" where source=id and  part=");
		buf.append(DBClient.format(this.id));
		//heg
		buf.append(" and source_mark='false' and target_mark='false'");
		try {
			List links = LinkImpl.getLinksWhere(factory, buf.toString());
			LinkImpl.copyLinks(factory, links, newids);
		} catch (ZenoException e) {
			factory.reportError("Topic.copyLinks", e);
		}
		
	}
	
	public void moveHereArticles(Set resources) 
			throws ZenoException {
		
		factory.checkPermission("ZenoCollection.paste", this);
		
		Iterator rit = resources.iterator();
		boolean topicModified = false;
		boolean parentModified = false;
		while (rit.hasNext()) {
			ResourceImpl resource = (ResourceImpl) rit.next();
			int resParentId = resource.parentId;
			if (resource instanceof Article) {
				try {
					resource.moveTo(this);
					topicModified = true;
					if (resParentId != this.parentId)
						parentModified = true;
				} catch (NoPermissionException e) {
					//to do report
				}
			}
		}
		if (topicModified) {
			NumberHandler.setUnsetNumber(this);
			this.modified();
		}
		if (parentModified) {
			((JournalImpl)getParent()).modified();
		}
	}
		
		
	public void copyHereArticles(Set resources, boolean withLinks) 
			throws ZenoException {
		
		factory.checkPermission("ZenoCollection.paste", this);
		
		JournalImpl journal = (JournalImpl)getParent();
		Hashtable newids = (withLinks) ? new Hashtable() : null;
		Iterator rit = resources.iterator();
		boolean targetModified = false;
		while (rit.hasNext()) {
			ResourceImpl resource = (ResourceImpl) rit.next();
			if (resource instanceof Article) {
				try {
					int currentPart = resource.part;
					resource.part = this.id;
					ResourceImpl copy = 
						(ResourceImpl) resource.copy(this, newids, "full");
					resource.part = currentPart;
					targetModified = true;
				} catch (NoPermissionException e) {
					//to do report
				}
			}
		}
		NumberHandler.setUnsetNumber(this);
		if (targetModified) {
			journal.modified();
			this.modified();
		}
	
		if (withLinks) {
			Set ids = newids.keySet();
			if (! ids.isEmpty()) {
				Iterator it = ids.iterator();
				StringBuffer buf = new StringBuffer();
				buf.append("select * from link where source in (");
				while (it.hasNext()) {
					Integer integer = (Integer) it.next();
					buf.append(integer);
					if (it.hasNext())
						buf.append(", ");
				}
				buf.append(")");
				//heg
				buf.append(" and source_mark='false' and target_mark='false'");
				List links = LinkImpl.getLinksWhere(factory, buf.toString());
				LinkImpl.copyLinks(factory, links, newids);
			}
		}
	}
	
	public void setPublished(boolean published) throws ZenoException {
		factory.checkPermission("Article.setPublished", this);
		if (!loaded) {
			loadProperties();
		}
		if (this.published != published) {
			this.published = published;
			this.modified = true;
			save();
			if (! published) {
				Iterator it = getMembers(true).iterator();
				while(it.hasNext()){
					ArticleImpl art = null;
					try {
						art = (ArticleImpl)it.next();
						if (art.id != this.id)
							art.setPublished(published);
					} catch(ZenoException e) {
						factory.reportError("Topic.setPublished", e);
					}
				}
			}
		}	
	}
	
	
	public boolean markForDeletion() throws ZenoException {
		
		factory.checkPermission("ZenoResource.markForDeletion", this);
		if (this.markedForDeletion)
			return true;
		else {
			boolean success = true;
			Iterator it = getMembers(true).iterator();
			while(it.hasNext()){
				ResourceImpl res = null;
				try {
					res = (ResourceImpl)it.next();
					if (res.id != this.id)
						success = res.markForDeletion() && success;
				} catch(ZenoException e) {
					factory.reportError("Topic.markForDeletion", e);
					success = false;
				}
			}
			if (success)
				setDeletionMark(true);
			return success;
		}
	}
	
	protected void propagateUnmark() throws ZenoException {
		
		factory.checkPermission("ZenoResource.unmarkForDeletion", this);
		setDeletionMark(false);
		Iterator it = getTrash();
		while(it.hasNext()){
			try {
				ResourceImpl res = (ResourceImpl)it.next();
				if (res.id != this.id)
					res.setDeletionMark(false);
			} catch(ZenoException e) {
				factory.reportError("Topic.unmarkForDeletion", e);
			}
		}
	}
	
	public boolean unmarkForDeletion(boolean propagate) throws ZenoException {
		
		boolean result = unmarkForDeletion();
		if (result && propagate)
			propagateUnmark();
		return result;
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
	
	
	//--------------- new collection methods -------------------
	
	protected int articleCount(String mode) throws ZenoException {
		try {
			StringBuffer buf = new StringBuffer();
			buf.append("select count(*) from resource, article");
			buf.append(" where part=");
			buf.append(DBClient.format(this.id));
			buf.append(" and resource.id = article.id ");
			if ("unmarked".equals(mode)) {
				buf.append(" and marked_for_deletion='false'");
				buf.append(" and published='true'");
			} else if ("marked".equals(mode)) {
				buf.append(" and marked_for_deletion='true'");
			}
			ResultSet rs = factory.dbclient.executeQuery(buf.toString());
			if (rs.next()) {
				return rs.getInt(1);
			} else {
				return 0;
			}
		} catch (java.sql.SQLException e) {
			factory.reportError("Topic.articleCount", e);
			throw new ZenoException("DataBaseException");
		}
	}

	/** Returns the number of members of the journal which are articles.
	Unpublished articles and articles marked for deletion are NOT
	included in the count. */

	public int articleCount() throws ZenoException {
		factory.checkPermission("ZenoCollection.articleCount", this);
		return articleCount("unmarked");
	}
	
	public int markedArticleCount() throws ZenoException {
		factory.checkPermission("ZenoCollection.markedArticleCount", this);
		return articleCount("marked");
	}
	
	
	public Iterator getArticles() throws ZenoException {
		try {
			factory.checkPermission("ZenoCollection.getArticles", this);
			StringBuffer buf = new StringBuffer();
			buf.append("select * from resource, article");
			buf.append(" where class = 'article' and part=");
			buf.append(DBClient.format(this.id));
			buf.append(" and resource.id = article.id");
			buf.append(" and marked_for_deletion='false'");
			buf.append(" and published = 'true'");
			buf.append(" order by rank, title");
			ResultSet rs = factory.dbclient.executeQuery(buf.toString());
			List list = new ArrayList();
			list.add(this);
			while (rs.next()) {
				list.add(factory.loadArticle(rs));
			}
			return list.iterator();
		} catch (java.sql.SQLException e) {
			factory.reportError("Topic.getArticles", e);
			throw new ZenoException("DataBaseException");
		}
	}
	
	public List getRecentArticles(int n) throws ZenoException {
		try {
			factory.checkPermission("ZenoCollection.getRecentArticles", this);
			List list = new ArrayList();
			StringBuffer buf = new StringBuffer();
			buf.append("select * from resource, article");
			buf.append(" where part=");
			buf.append(DBClient.format(this.id));
			buf.append(" and resource.id=article.id");
			buf.append(" and marked_for_deletion='false'");
			buf.append(" and article.published='true'");
			buf.append(" order by resource.id desc");
			ResultSet rs = factory.dbclient.executeQuery(buf.toString());
			while (rs.next() && n > 0) {
				list.add(factory.loadArticle(rs));
				n--;
			}
			return list;
		} catch (java.sql.SQLException e) {
			factory.reportError("Topic.getRecentArticles", e);
			throw new ZenoException("DataBaseException");
		}
	}
	
	public List getRecentArticles(Date date)
			throws ZenoException {
		
		factory.checkPermission("ZenoCollection.getRecentArticles", this);
		if (date == null ) {
			DBPrincipalImpl user = (DBPrincipalImpl)factory.getUser();
			date = user.getLastLogin();
		}
		
		try {
			List result = new ArrayList();
			
			StringBuffer buf = new StringBuffer();
			buf.append("select * from resource, article");
			buf.append(" where part=");
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
		} catch (java.sql.SQLException e) {
			factory.reportError("Topic.getRecentArticles", e);
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
			buf.append(" where part=");
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
	
	public List getArticlesBetween(Date startDate, Date endDate) 
		throws ZenoException {
		try {
			factory.checkPermission("ZenoCollection.getArticlesBetween", this);
			List result = new ArrayList();
			StringBuffer buf = new StringBuffer();
			String bd = DBClient.format(startDate);
			String ed = DBClient.format(endDate);
			buf.append("select * from resource, article where ");
			buf.append(" resource.id=article.id ");
			buf.append(" and part=");
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
		} catch (java.sql.SQLException e) {
			factory.reportError("Topic.getArticlesBetween", e);
			throw new ZenoException("DataBaseException");
		}
	}
	
	public Iterator getEventsDuring(Date startDate, Date endDate)
			throws ZenoException {
		try {
			factory.checkPermission("ZenoCollection.getEventsDuring", this);
			List result = new ArrayList();
			StringBuffer buf = new StringBuffer();
			String bd = DBClient.format(startDate);
			String ed = DBClient.format(endDate);
			buf.append("select * from resource, article where ");
			buf.append(" resource.id=article.id ");
			buf.append(" and part=");
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
				if (art.getBeginDate().after(startDate)
					&& art.getBeginDate().before(endDate)) {
					result.add(new BeginEvent(art, art.getCreator(), art.getBeginDate()));
				}
				if (art.getEndDate().after(startDate) && art.getEndDate().before(endDate)) {
					result.add(new EndEvent(art, art.getCreator(), art.getEndDate()));
				}
				if (art.getExpirationDate().after(startDate)
					&& art.getExpirationDate().before(endDate)) {
					result.add(new ExpirationEvent(art, art.getCreator(), art.getExpirationDate()));
				}
			}
			Collections.sort(result, new Comparator() { 
									
				public int compare(Object o1, Object o2) {
					if (o1 instanceof ZenoEvent && o2 instanceof ZenoEvent) {
						ZenoEvent e1 = (ZenoEvent) o1;
						ZenoEvent e2 = (ZenoEvent) o2;
						return e1.getDate().compareTo(e2.getDate());
					} else {
						return 0;
					}
				}
			});
			return result.iterator();
		} catch (java.sql.SQLException e) {
			factory.reportError("Topic.getEventsDuring", e);
			throw new ZenoException("DataBaseException");
		}
	}
	
	public List getUnpublishedArticles() throws ZenoException {
		try {
			factory.checkPermission("ZenoCollection.getUnpublishedArticles", this);
			List list = new ArrayList();
			StringBuffer buf = new StringBuffer();
			buf.append("select * from resource, article where");
			buf.append(" resource.id=article.id");
			buf.append(" and part=");
			buf.append(DBClient.format(this.id));
			buf.append(" and published='false'");
			buf.append(" and marked_for_deletion='false'");
			ResultSet rs = factory.dbclient.executeQuery(buf.toString());
			while (rs.next()) {
				list.add(factory.loadArticle(rs));
			}
			return list;
		} catch (java.sql.SQLException e) {
			factory.reportError("Topic.getUnpublishedArticles", e);
			throw new ZenoException("DataBaseException");
		}
	}
	
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
		try {
			factory.checkPermission("ZenoCollection.search", this);
			StringBuffer buf = new StringBuffer();
			//buf.append("select resource.id from resource, article where ");
			buf.append("select * from resource, article where ");
			buf.append(" part=");
			buf.append(DBClient.format(this.id));
			buf.append(" and resource.id=article.id");
			if (authorId != null) {
				buf.append(" and author=");
				buf.append(DBClient.format(authorId));
			}
			if (title != null) {
				buf.append(" and title=");
				buf.append(DBClient.format(title));
			}
			if (articleLabel != null) {
				buf.append(" and label=");
				buf.append(DBClient.format(articleLabel));
			}
			if (qualifier != null) {
				buf.append(" and qualifier=");
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
			if (fullText != null) {
				buf.append(" and (match (resource.title, resource.note) against (");
				buf.append(DBClient.format(fullText));
				buf.append(")");
				buf.append(" or match (article.keywords) against (");
				buf.append(DBClient.format(fullText));
				buf.append("))");
			}
			*/
			if (fullText != null) {
				buf.append(" and (note like " );
				buf.append(DBClient.format(fullText));
				buf.append(" or title like " );
				buf.append(DBClient.format(fullText));
				buf.append(" or keywords like " );
				buf.append(DBClient.format(fullText));
				buf.append(")");
			}
			if (order != null)
				if (!order.equals("")) {
					buf.append(" order by " + order);
				}
			List list = new ArrayList();
			ResultSet rs = factory.dbclient.executeQuery(buf.toString());
			while (rs.next()) {
				list.add(factory.loadArticle(rs));
			}
			return list.iterator();
		} catch (java.sql.SQLException e) {
			factory.reportError("Topic.search", e);
			throw new ZenoException("DataBaseException");
		}
	}

	
		
}
