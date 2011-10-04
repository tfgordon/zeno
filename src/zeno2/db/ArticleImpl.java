package zeno2.db;

import java.io.File;
import java.io.InputStream;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;

import zeno2.kernel.Article;
import zeno2.kernel.Attachment;
import zeno2.kernel.Journal;
import zeno2.kernel.NotFoundException;
import zeno2.kernel.Principal;
import zeno2.kernel.ReadEvent;
import zeno2.kernel.Topic;
import zeno2.kernel.ZenoCollection;
import zeno2.kernel.ZenoException;
import zeno2.kernel.ZenoResource;

import zeno2.db.AttachmentImpl;
import java.io.InputStream;


public class ArticleImpl extends ResourceImpl implements Article {
	String author = null;
	String label = "";
	String qualifier = "";
	boolean isTopic = false;
	String keywords = "";
	java.util.Date expirationDate = null;
	java.util.Date beginDate = null;
	java.util.Date endDate = null;
	boolean published = true;
	int nr = 0;

	protected ArticleImpl(FactoryImpl factory) {
		super(factory);
		super.zenoClass = "article";
	}

	protected ArticleImpl(FactoryImpl factory, int id) {
		super(factory, id);
		super.zenoClass = "article";
	}
	
	
	protected void create(ZenoCollection parent) throws ZenoException {
		
		if (id != 0) return;

		try {
			super.create(parent);
			
			if (this.author == null)
				this.author = this.creator;
			
			StringBuffer buf = new StringBuffer();
			buf.append("insert into article ");
			buf.append("(id, author, label, is_topic, qualifier, keywords"); 
			buf.append(", expiration_date, begin_date, end_date, published)");
			buf.append(" values(");
			buf.append(DBClient.format(this.id));
			buf.append(", ");
			buf.append(DBClient.format(this.author));
			buf.append(", ");
			buf.append(DBClient.format(this.label));
			buf.append(", ");
			buf.append(DBClient.format(this.isTopic));
			buf.append(", ");
			buf.append(DBClient.format(this.qualifier));
			buf.append(", ");
			buf.append(DBClient.format(this.keywords));
			buf.append(", ");
			buf.append(DBClient.format(this.expirationDate));
			buf.append(", ");
			buf.append(DBClient.format(this.beginDate));
			buf.append(", ");
			buf.append(DBClient.format(this.endDate));
			buf.append(", ");
			buf.append(DBClient.format(this.published));
			buf.append(")");
			factory.dbclient.executeUpdate(buf.toString());
		} catch (java.sql.SQLException e) {
			factory.reportError("Article.create", e);
			throw new ZenoException("DataBaseException");
		}
	}
	
	protected String getAlias() throws ZenoException {
		String newAlias = getLabel();
		if (newAlias == null || newAlias.equals(""))
			newAlias = getTitle();
		else
			newAlias = newAlias +  ": " + getTitle();
		return newAlias;
	}
		

	public void save() throws ZenoException {
		// permissions already checked by set methods
		try {
			// to do: begin transaction
			if (modified & loaded) {
				// save article specific properties first
				// so that modified event is reported only
				// after all tables have been updated
				StringBuffer buf = new StringBuffer();
				buf.append("update article");
				buf.append(" set author=");
				buf.append(DBClient.format(this.author));
				buf.append(", label=");
				buf.append(DBClient.format(this.label));
				buf.append(", qualifier=");
				buf.append(DBClient.format(this.qualifier));
				buf.append(", keywords=");
				buf.append(DBClient.format(this.keywords));
				buf.append(", expiration_date=");
				buf.append(DBClient.format(this.expirationDate));
				buf.append(", begin_date=");
				buf.append(DBClient.format(this.beginDate));
				buf.append(", end_date=");
				buf.append(DBClient.format(this.endDate));
				buf.append(", published=");
				buf.append(DBClient.format(this.published));
				buf.append(", is_topic=");
				buf.append(DBClient.format(this.isTopic));
				buf.append(" where id =");
				buf.append(this.id);
				factory.dbclient.executeUpdate(buf.toString());
				// do not reset modified variable, as this is done
				// by super.save()
			}
			//heg  locking at first save
			//this.closed = true;
			super.save();
			// to do: commit transaction
		} catch (java.sql.SQLException e) {
			// to do: rollback
			factory.reportError("Article.save", e);
			throw new ZenoException("DataBaseException");
		}
	}
	
	public void finish(boolean anonymously) throws ZenoException {
		
		factory.checkPermission("Article.finish", this);
		if (anonymously)
			finishAnonymously(true);
		else {
			int revisionPeriod = 0;
			StringBuffer buf = new StringBuffer();
			buf.append("select revision_period from journal");
			buf.append(" where id=");
			buf.append(DBClient.format(this.parentId));
			try {
				ResultSet rs = factory.dbclient.executeQuery(buf.toString());
				if (rs.next()) 
					revisionPeriod = rs.getInt("revision_period");
			} catch (java.sql.SQLException e) {
				factory.reportError("Article.finished", e);
			}
			finish(revisionPeriod);
		}
	}
	
	protected void finishAnonymously(boolean close) throws ZenoException {
		StringBuffer buf = new StringBuffer();
		buf.append("update resource set creator = 'anonymous'");
		buf.append(", modifier = 'anonymous'");
		buf.append(", creation_date =");
		buf.append(DBClient.format(this.creationDate));
		if (close)
			buf.append(", locked = 'true'");
		buf.append(" where id =");
		buf.append(DBClient.format(this.id));
		try {
			factory.dbclient.executeUpdate(buf.toString());
		} catch (java.sql.SQLException e) {
			factory.reportError("Article.finished", e);
			throw new ZenoException("DataBaseException");
		}
		buf.setLength(0);
		buf.append("update article set author = 'anonymous'");
		buf.append("where id =");
		buf.append(DBClient.format(this.id));
		try {
			factory.dbclient.executeUpdate(buf.toString());
		} catch (java.sql.SQLException e) {
			factory.reportError("Article.finished", e);
			throw new ZenoException("DataBaseException");
		}
	}
		
	protected void finish(int revisionPeriod) {
		
		StringBuffer buf = new StringBuffer();	
		if (revisionPeriod == 0) {
			buf.append("update resource set locked = 'true'");
			buf.append(" where id=");
			buf.append(DBClient.format(this.id));
			try {
				factory.dbclient.executeUpdate(buf.toString());
			} catch (java.sql.SQLException e) {
				factory.reportError("Article.finished", e);
			}
		}
		if (revisionPeriod > 0) {
			//revisionPeriod in hours
			Date now = new Date();
			Date execDate = 
				new Date(now.getTime() + (3600000 * revisionPeriod));
			buf.setLength(0);
			buf.append("insert housekeeping (id, exec_date, operation)");
			buf.append(" values(");
			buf.append(DBClient.format(this.id));
			buf.append(", ");
			buf.append(DBClient.format(execDate));
			buf.append(", 'c'");
			buf.append(")");
			try {
				factory.dbclient.executeUpdate(buf.toString());
			} catch (java.sql.SQLException e) {
				factory.reportError("Article.finished", e);
			}
		}	
	}

	/** load the article specific properties.  The generic properties
	shared by all resources were already loaded when the article was
	constructed using the loadResource method of the factory. */

	public void loadProperties() throws ZenoException {
		if (!loaded) {
			try {
				String request = "select * from article where id = " + this.id;
				ResultSet rs = factory.dbclient.executeQuery(request);
				if (rs.next()) {
					loadProperties(rs);
				}
				this.loaded = true;
			} catch (java.sql.SQLException e) {
				factory.reportError("Article.loadProperties", e);
				throw new ZenoException("DataBaseException");
			}
		}
	}

	protected void loadProperties(ResultSet rs) throws java.sql.SQLException {
		/* assumes the rs is not null and not empty */
		this.author = rs.getString("author");
		this.label = rs.getString("label");
		this.isTopic = rs.getBoolean("is_topic");
		this.qualifier = rs.getString("qualifier");
		this.keywords = rs.getString("keywords");
		/*
		this.expirationDate = rs.getTimestamp("expiration_date");
		this.beginDate = rs.getTimestamp("begin_date");
		this.endDate = rs.getTimestamp("end_date");
		*/
		this.expirationDate = DBClient.getTimestamp(rs, "expiration_date"); 
		this.beginDate = DBClient.getTimestamp(rs, "begin_date"); 
		this.endDate = DBClient.getTimestamp(rs, "end_date"); 
		this.published = rs.getBoolean("published");
		this.nr = rs.getInt("nr");
		this.loaded = true;
	}
	
	/** like move but without marking target as modified */
	
	protected void moveTo(ZenoCollection collection) throws ZenoException {
	
		factory.checkPermission("ZenoResource.move", this);
		
		int oldParentId = this.parentId;
		int newParentId = collection.getId();
		int newPart = 0;
		if (collection instanceof Topic) {
			newParentId = ((TopicImpl)collection).parentId;
			newPart = collection.getId();
		}
		TopicImpl oldTopic = (TopicImpl)getTopic();
		JournalImpl oldParent = 
			(JournalImpl)((oldParentId != newParentId) ? getParent() : null);
		
		if (oldParentId != newParentId) 
			NumberHandler.setNumber(this.id, 0);	
		
		try {
			StringBuffer buf = new StringBuffer();
			buf.append("update resource set parent=");
			buf.append(DBClient.format(newParentId));
			buf.append(", part=");
			buf.append(DBClient.format(newPart));
			buf.append(", creation_date =");
			buf.append(DBClient.format(this.creationDate));
			buf.append(" where id=");
			buf.append(DBClient.format(this.id));
			factory.dbclient.executeUpdate(buf.toString());
			this.parentId = newParentId;
			this.part = newPart;
		} catch (java.sql.SQLException e) {
			factory.reportError("Article.move", e);
			throw new ZenoException("DataBaseException");
		}
		
		if (oldParentId != newParentId) 
			oldParent.modified();
		if (oldTopic != null)
				oldTopic.modified();
		
	}
	
	public void move(ZenoCollection collection) throws ZenoException {
		
		factory.checkPermission("Journal.paste", collection);
		
		int oldParentId = this.parentId;
		int newParentId = ((ResourceImpl)collection).parentId;
		moveTo(collection);
		if (oldParentId != newParentId)
			NumberHandler.setNextNumber(this);
		((ResourceImpl)collection).modified();
		if (collection instanceof Topic && newParentId != oldParentId) {
			((JournalImpl)collection.getParent()).modified();
		}
	}
	
	/*
	public ZenoResource copy(ZenoCollection collection, boolean withLinks)
			throws ZenoException {
		
		factory.checkPermission("Journal.paste", collection);

		Hashtable newids = (withLinks) ? new Hashtable() : null;
		ArticleImpl newarticle = (ArticleImpl)copy(collection, newids, "full");
		((JournalImpl)collection).modified();
		if (withLinks) {
			List toplinks = getLinks(1);
			LinkImpl.copyLinks(factory, toplinks, newids);
		}

		return newarticle;
	}
	*/
	
	
	public ZenoResource copy(ZenoCollection collection, boolean withLinks)
		throws ZenoException {
		
		JournalImpl parent;
		ZenoResource copy;
		int currentPart = this.part;		
		Hashtable newids = (withLinks) ? new Hashtable() : null;
		if (collection instanceof Topic) {
			parent = (JournalImpl)collection.getParent();
			this.part = collection.getId();
		} else {
			parent = (JournalImpl)collection;
			this.part = 0;
		}
		factory.checkPermission("Journal.paste", parent);
		
		copy = copy(parent, newids, "full");
		NumberHandler.setNextNumber((ArticleImpl)copy);
		this.part = currentPart;
		parent.modified();
		if (parent != collection)
			((ResourceImpl)collection).modified();
			
		if (withLinks) {
			List toplinks = getLinks(1);
			LinkImpl.copyLinks(factory, toplinks, newids);
		}
		
		return copy; 
	}
	
	protected ZenoResource copy(ZenoCollection collection, Hashtable newids, String mode)
			throws ZenoException {
		
		factory.checkPermission("Article.copy", this);
		
		ZenoResource copy;
		if (collection instanceof Topic) {
			Journal journal = (Journal)collection.getParent();
			int currentPart = this.part;
			this.part = collection.getId();
			copy = super.copy(journal, newids, mode);
			this.part = currentPart;
		} else 
			copy = super.copy(collection, newids, mode);
	
		List attachments = getAttachments();
		AttachmentImpl.copyAttachments(factory, attachments, newids);

		return copy;
	}
	
	protected ZenoResource copy(ZenoCollection collection, Hashtable newids, 
									String mode, boolean copynr)
			throws ZenoException {
		factory.checkPermission("Article.copy", this);
		
		ZenoResource copy;
		if (collection instanceof Topic) {
			Journal journal = (Journal)collection.getParent();
			int currentPart = this.part;
			this.part = collection.getId();
			copy = super.copy(journal, newids, mode);
			this.part = currentPart;
		} else 
			copy = super.copy(collection, newids, mode);
		if (copynr) {
			NumberHandler.setNumber(copy.getId(), nr);
		}
	
		List attachments = getAttachments();
		AttachmentImpl.copyAttachments(factory, attachments, newids);

		return copy;
	}
		

	/*
	protected ZenoResource copy(ZenoCollection collection, Hashtable newids, String mode)
			throws ZenoException {
		
		factory.checkPermission("Article.copy", this);
		
		ZenoResource newres = super.copy(collection, newids, mode);
		
		List attachments = getAttachments();
		AttachmentImpl.copyAttachments(factory, attachments, newids);

		return newres;
	}
	*/
		

	public String getAuthor() throws ZenoException {
		factory.checkPermission("Article.getAuthor", this);
		if (!loaded) {
			loadProperties();
		}
		return this.author;
	}

	public void setAuthor(String principalId) throws ZenoException {
		factory.checkPermission("Article.setAuthor", this);
		if (!loaded) {
			loadProperties();
		}
		this.author = principalId;
		this.modified = true;
	}
	
	public Topic getTopic() throws ZenoException {
		if (this.part == 0)
			return null;
		else if (part == id)
			return (Topic)this;
		else
			return (Topic)factory.loadResource(this.part);
	}
	
	public int getTopicId() {
		return part;
	}

	public boolean isTopic() throws ZenoException {
		factory.checkPermission("Article.isTopic", this);
		if (!loaded) {
			loadProperties();
		}
		return this.isTopic;
	}
	
	/*
	public void setIsTopic(boolean isTopic) throws ZenoException {
		factory.checkPermission("Article.setIsTopic", this);
		if (!loaded) {
			loadProperties();
		}
		this.isTopic = isTopic;
		this.modified = true;
	}
	*/
	
	public void setIsTopic(boolean isTopic) throws ZenoException {
		//temp 
		factory.checkPermission("Article.setIsTopic", this);
		if (isTopic) 
			transformArticle();
	}
	
	protected boolean isTopicMarked() throws ZenoException {
		if (part == 0)
			return false;
		if (id == part)
			return false;
		StringBuffer buf = new StringBuffer();
		buf.append("select marked_for_deletion from resource ");
		buf.append("where id=");
		buf.append(DBClient.format(part));
		try {
			ResultSet rs = factory.dbclient.executeQuery(buf.toString());
			if (rs.next()) 
				return rs.getBoolean(1);
			else
				return false;
		} catch (java.sql.SQLException e) {
			factory.reportError("ZenoResource.IsParentMarked", e);
			throw new ZenoException("DataBaseException");
		}
	}
	
	/*
	public boolean unmarkForDeletion() throws ZenoException {
		factory.checkPermission("ZenoResource.unmarkForDeletion", this);
		if (!this.markedForDeletion)
			return true;
		else if (isTopicMarked())
			return false;
		else if (isParentMarked())
			return false;
		else {
			setDeletionMark(false);
			return true;
		}
	}
	*/
	
	public boolean unmarkForDeletion() throws ZenoException {
		factory.checkPermission("ZenoResource.unmarkForDeletion", this);
		if (!this.markedForDeletion)
			return true;
		else if (isParentMarked())
			return false;
		else {
			if (part == 0 || id == part  
				 || getTopic().unmarkForDeletion()) {
				setDeletionMark(false);
				return true;
			} else
				return false;
		}
	}
	
	public Topic transformArticle() throws ZenoException {
	
		factory.checkPermission("Article.transformArticle", this);
		StringBuffer buf = new StringBuffer();
		buf.append("update resource set class='topic'");
		buf.append(", part=");
		buf.append(DBClient.format(this.id));
		buf.append(", creation_date =");
		buf.append(DBClient.format(this.creationDate));
		buf.append(" where id=");
		buf.append(DBClient.format(this.id));
		try {
			factory.dbclient.executeUpdate(buf.toString());
		} catch (java.sql.SQLException e) {
			factory.reportError("ArticleImpl.transformArticle", e);
			throw new ZenoException("DBException");
		}
		//temp
		buf.setLength(0);
		buf.append("update article set is_topic=");
		buf.append(DBClient.format(true));
		buf.append(" where id =");
		buf.append(DBClient.format(this.id));
		try {
			factory.dbclient.executeUpdate(buf.toString());
		} catch (java.sql.SQLException e) {
			factory.reportError("ArticleImpl.transformArticle", e);
			throw new ZenoException("DBException");
		}
		return (Topic)factory.loadResource(this.id);
	}

	public String getKeywords() throws ZenoException {
		factory.checkPermission("Article.getKeywords", this);
		if (!loaded) {
			loadProperties();
		}
		return this.keywords;
	}

	public void setKeywords(String keywords) throws ZenoException {
		factory.checkPermission("Article.setKeywords", this);
		if (!loaded) {
			loadProperties();
		}
		this.keywords = keywords;
		this.modified = true;
	}

	/** Gets the expiration date of this article. */

	public Date getExpirationDate() throws ZenoException {
		factory.checkPermission("Article.getExpirationDate", this);
		if (!loaded) {
			loadProperties();
		}
		return this.expirationDate;
	}

	public void setExpirationDate(Date expirationDate) throws ZenoException {
		factory.checkPermission("Article.setExpirationDate", this);
		if (!loaded) {
			loadProperties();
		}
		this.expirationDate = expirationDate;
		this.modified = true;
	}

	public Date getBeginDate() throws ZenoException {
		factory.checkPermission("Article.getBeginDate", this);
		if (!loaded) {
			loadProperties();
		}
		return this.beginDate;
	}

	public void setBeginDate(Date beginDate) throws ZenoException {
		factory.checkPermission("Article.setBeginDate", this);
		if (!loaded) {
			loadProperties();
		}
		this.beginDate = beginDate;
		this.modified = true;
	}

	public Date getEndDate() throws ZenoException {
		factory.checkPermission("Article.getEndDate", this);
		if (!loaded) {
			loadProperties();
		}
		return this.endDate;
	}

	public void setEndDate(Date endDate) throws ZenoException {
		factory.checkPermission("Article.setEndDate", this);
		if (!loaded) {
			loadProperties();
		}
		this.endDate = endDate;
		this.modified = true;
	}

	public String getLabel() throws ZenoException {
		factory.checkPermission("Article.getLabel", this);
		if (!loaded) {
			loadProperties();
		}
		return this.label;
	}

	public void setLabel(String label) throws ZenoException {
		factory.checkPermission("Article.setLabel", this);
		if (!loaded) {
			loadProperties();
		}
		if (!this.label.equals(label)) {
			this.label = label;
			this.adaptAlias = true;
			this.modified = true;
		}
	}

	public String getQualifier() throws ZenoException {
		factory.checkPermission("Article.getQualifier", this);
		if (!loaded) {
			loadProperties();
		}
		return this.qualifier;
	}

	public void setQualifier(String qualifier) throws ZenoException {
		factory.checkPermission("Article.setQualifier", this);
		if (!loaded) {
			loadProperties();
		}
		this.qualifier = qualifier;
		this.modified = true;
	}
	
	public int getNr() throws ZenoException {
		factory.checkPermission("Article.getNr", this);
		if (!loaded) {
			loadProperties();
		}
		return this.nr;
	}

	public boolean getPublished() throws ZenoException {
		factory.checkPermission("Article.getPublished", this);
		if (!loaded) {
			loadProperties();
		}
		return this.published;
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
			if (published && part != 0 && id != part) {
				getTopic().setPublished(published);
			}
		}
	}
	
	public String getStatus() throws ZenoException {
		if (!loaded) {
			loadProperties();
		}
		String status = super.getStatus();
		if (!published)
			status = status + "h";
		return status;
	} 
	
	protected boolean isVisibleForReader() {
		if (this.markedForDeletion)
			return false;
		else if (!loaded) 
			try {
				loadProperties();
				return this.published;
			} catch (ZenoException e) {
				return false;
			}
		else 
			return this.published;
	}

	public String getStyleSheetUrl() throws ZenoException {
		Journal journal = this.getJournal();
		return journal.getStyleSheetUrl();
	}
	
	public Attachment getAttachment(int id) throws ZenoException {
		try {
			factory.checkPermission("Article.getAttachment", this);
			StringBuffer buf = new StringBuffer();
			buf.append("select * from attachment ");
			buf.append("  where article=");
			buf.append(DBClient.format(this.id));
			buf.append("  and id =");
			buf.append(DBClient.format(id));
			ResultSet rs = factory.dbclient.executeQuery(buf.toString());
			if (rs.next()) {
				Attachment attachment = AttachmentImpl.getAttachment(factory, rs);
				return attachment;
			} else {
				throw new NotFoundException("NoSuchAttachment " + id + " for article " + this.id);
			}
		} catch (java.sql.SQLException e) {
			factory.reportError("Article.getAttachment", e);
			throw new ZenoException("DataBaseException");
		}
	}

	//obsolete
	public Attachment getAttachment(String name) throws ZenoException {
		try {
			factory.checkPermission("Article.getAttachment", this);
			StringBuffer buf = new StringBuffer();
			buf.append("select * from attachment ");
			buf.append("  where article=");
			buf.append(DBClient.format(this.id));
			buf.append("  and name=");
			buf.append(DBClient.format(name));
			ResultSet rs = factory.dbclient.executeQuery(buf.toString());
			if (rs.next()) {
				Attachment attachment = AttachmentImpl.getAttachment(factory, rs);
				return attachment;
			} else {
				throw new NotFoundException("NoSuchAttachment " + name);
			}
		} catch (java.sql.SQLException e) {
			factory.reportError("Article.getAttachment", e);
			throw new ZenoException("DataBaseException");
		}
	}

	public List getAttachments() throws ZenoException {
		try {
			factory.checkPermission("Article.getAttachments", this);
			List result = new ArrayList();
			StringBuffer buf = new StringBuffer();
			buf.append("select * from attachment ");
			buf.append("  where article=");
			buf.append(DBClient.format(this.id));
			ResultSet rs = factory.dbclient.executeQuery(buf.toString());
			while (rs.next()) {
				Attachment attachment = AttachmentImpl.getAttachment(factory, rs);
				result.add(attachment);
			}
			return result;
		} catch (java.sql.SQLException e) {
			factory.reportError("Article.getAttachments", e);
			throw new ZenoException("DataBaseException");
		}
	}

	public Attachment addAttachment(String name, String mimeType, File file)
		throws ZenoException {

		factory.checkPermission("Article.addAttachment", this);
		Attachment atch = AttachmentImpl.createAttachment(factory, this.id, name, mimeType, file);
		atch.create();
		this.modified = true;
		save();
		return atch;


	}

	public Attachment addAttachment(String name, String mimeType, InputStream inputStream)
			throws ZenoException {
			
		factory.checkPermission("Article.addAttachment", this);
		Attachment atch = AttachmentImpl.createAttachment(factory, this.id, name, mimeType, inputStream);
		atch.create();
		this.modified = true;
		save();
		return atch;
			
			
	}
	
	public void deleteAttachment(int id) throws ZenoException {
			/*
		try {
			factory.checkPermission("Article.deleteAttachment", this);
			StringBuffer buf = new StringBuffer();
			buf.append("delete from attachment where article=");
			buf.append(DBClient.format(this.id));
			buf.append(" and id =");
			buf.append(DBClient.format(id));

			// to do: start transaction
			factory.dbclient.executeUpdate(buf.toString());
			this.modified = true;
			save();
		} catch (java.sql.SQLException e) {
			// to do: rollback
			factory.reportError("Article.deleteAttachments", e);
			throw new ZenoException("DataBaseException");
		}
			*/
			try {
		factory.checkPermission("Article.deleteAttachment", this);
		AttachmentImpl.deleteAttachment(factory, this.id, id);

		this.modified = true;
		save();

			} catch (ZenoException e) {
		throw e;

			}
		
	}
	
	//obsolete
	public void deleteAttachment(String name) throws ZenoException {

		try {
				StringBuffer buf = new StringBuffer();
				buf.append("SELECT id FROM attachment WHERE article = ");
				buf.append(DBClient.format(this.id));
				buf.append(" AND name = ");
				buf.append(DBClient.format(name));

				ResultSet rs = factory.dbclient.executeQuery(buf.toString());

				if (rs.next()) 
			deleteAttachment(rs.getInt(1));
				/*
			factory.checkPermission("Article.deleteAttachment", this);
			StringBuffer buf = new StringBuffer();
			buf.append("delete from attachment where article=");
			buf.append(DBClient.format(this.id));
			buf.append(" and name=");
			buf.append(DBClient.format(name));

			// to do: start transaction
			factory.dbclient.executeUpdate(buf.toString());
			this.modified = true;
			save();
				*/
		} catch (java.sql.SQLException e) {
			// to do: rollback
			factory.reportError("Article.deleteAttachment", e);
			throw new ZenoException("DataBaseException");
		}
	}
	
	//obsolete
	public void renameAttachment(String oldname, String newname)
		throws ZenoException {
		try {
			factory.checkPermission("Article.renameAttachment", this);
			StringBuffer buf = new StringBuffer();
			buf.append("update attachment set name=");
			buf.append(DBClient.format(newname));
			buf.append(" where article=");
			buf.append(DBClient.format(this.id));
			buf.append(" and name=");
			buf.append(DBClient.format(oldname));
			factory.dbclient.executeUpdate(buf.toString());
			this.modified = true;
			save();
		} catch (java.sql.SQLException e) {
			factory.reportError("Article.renameAttachments", e);
			throw new ZenoException("DataBaseException");
		}
	}

	public void generateReadEvent() throws ZenoException {
		factory.checkPermission("Article.generateReadEvent", this);
		Principal user = factory.getUser();
		monitor.reportEvent(
			new ReadEvent(new Integer(this.id), user.getId(), new java.util.Date()));
	}
	
	public int incrementReadCount() throws ZenoException {
		int readCount= 0;
		try {
			StringBuffer buf = new StringBuffer();
			buf.append("select value from property where resource = ");
			buf.append(DBClient.format(id));
			buf.append(" and name='readCount'");
			ResultSet rs = factory.dbclient.executeQuery(buf.toString());
			if (rs.next()) {
				readCount = Integer.parseInt(rs.getString("value"));
			}	buf.setLength(0);
			readCount++;
			if (readCount == 1) {
				buf.append("insert into property (value, resource, name)");
				buf.append(" values(");
				buf.append(DBClient.format(Integer.toString(readCount)));
				buf.append(", ");
				buf.append(DBClient.format(id));
				buf.append(", 'readCount'");
				buf.append(")");
			} else {
				buf.append("update property set value =");
				buf.append(DBClient.format(Integer.toString(readCount)));
				buf.append(" where resource = ");
				buf.append(DBClient.format(id));
				buf.append(" and name='readCount'");
			}
			factory.dbclient.executeUpdate(buf.toString());
			return readCount;
		} catch (java.sql.SQLException e) {
			factory.reportError("ZenoResource.incrementReadCount", e);
			throw new ZenoException("DataBaseException");
		}
	}
	
	public Article createReply(String label) throws ZenoException {
		
		Article article;
		if (part != 0) {
			TopicImpl topic = (TopicImpl)getTopic();
			article = factory.createArticle(topic);
		} else {
			Journal journal = (Journal)getParent();
			article = factory.createArticle(journal);
		}
		article.addLink("", label, this.id, "");
		return article;
	}
	

	/** Returns a list of articles in this journal, ordered by rank and then title,
	which have link targeting this article. */

	public List getReplies() throws ZenoException {
		try {
			factory.checkPermission("Article.getReplies", this);
			List result = new ArrayList();
			StringBuffer buf = new StringBuffer();
			buf.append("select source from link ");
			buf.append("where target=");
			buf.append(DBClient.format(this.id));
			ResultSet rs = factory.dbclient.executeQuery(buf.toString());
			while (rs.next()) {
				ZenoResource r = factory.loadResource(rs.getInt("source"));
				if (r instanceof Article) {
					result.add(r);
				}
			}
			return result;
		} catch (java.sql.SQLException e) {
			factory.reportError("Article.getReplies", e);
			throw new ZenoException("DataBaseException");
		}
	}
	
	

	//heg
	/** resets sourceAlias or targetAlias in all links where the source or
	target is this article
	*/

	public void updateAllAlias() throws ZenoException {
		try {
			String alias = getLabel() + ": " + getTitle();
			StringBuffer buf = new StringBuffer();
			buf.append("update link set source_alias=");
			buf.append(DBClient.format(alias));
			buf.append(" where source =");
			buf.append(DBClient.format(getId()));
			factory.dbclient.executeUpdate(buf.toString());
			buf.setLength(0);
			buf.append("update link set target_alias=");
			buf.append(DBClient.format(alias));
			buf.append(" where target =");
			buf.append(DBClient.format(getId()));
			factory.dbclient.executeUpdate(buf.toString());
		} catch (java.sql.SQLException e) {
			factory.reportError("ArticleImpl.updateAllAlias", e);
			throw new ZenoException("DBException");
		}
	}

	public String toString() {
	//heg topic
		StringBuffer buf = new StringBuffer();
		if ("article".equals(this.zenoClass))
			buf.append("[Article ");
		else
			buf.append("[Topic ");
		buf.append(this.id);	
		buf.append(" (");
		buf.append(this.nr);
		buf.append(") ");
		buf.append(this.label);
		buf.append(" ");
		buf.append(this.title);
		buf.append(" part ");
		buf.append(this.part);
		buf.append(" by ");
		if (this.author == null || "".equals(this.author)) {
			buf.append("(");
			buf.append(this.creator);
			buf.append(")");
		} else
			buf.append(this.author);
		buf.append("]");
		return buf.toString();
	}

	public void show(boolean complete, PrintWriter prw) {
		if (prw == null)
			prw = new PrintWriter(System.out, true);
		if (complete && !loaded) {
			try {
				loadProperties();
			} catch (ZenoException e) {
			}
		}
		prw.println("<article ");
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
		prw.println("    author=" + quote(author));
		prw.println("    label=" + quote(label));
		prw.println("    qualifier=" + quote(qualifier));
		prw.println("    expiration_date=" + quote(expirationDate));
		prw.println("    begin_date=" + quote(beginDate));
		prw.println("    end_date=" + quote(endDate));
		prw.println("    published=" + quote(published));
		prw.println();
		prw.println("    loaded=" + quote(loaded));
		prw.println("    modified=" + quote(modified));
		prw.println(">");
		try {
			if (complete)
				prw.println(getNote());
			else
				if (this.note != null)
					prw.println(this.note);
		} catch (ZenoException e) {
			factory.reportError("Article.show", e);
		}
		prw.println("</article>");
	}

}
