package zeno2.db;

import java.io.PrintWriter;
import java.sql.Blob;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import zeno2.kernel.Journal;
import zeno2.kernel.Link;
import zeno2.kernel.XLink;
import zeno2.kernel.CreationEvent;
import zeno2.kernel.ModificationEvent;
import zeno2.kernel.NoPermissionException; 
import zeno2.kernel.NotFoundException;
import zeno2.kernel.PathElement;
import zeno2.kernel.Principal;
import zeno2.kernel.ZenoCollection;
import zeno2.kernel.ZenoException; 
import zeno2.kernel.ZenoResource;

/**
 *  Description of the Class
 *
 *@author     oppor
 *@created    September 27, 2001
 */
public abstract class ResourceImpl implements  Cloneable, ZenoResource {
	int id = 0;
	int parentId = 0;
	int part = 0;
	String zenoClass;
	String title = "";
	String note = null;
	int rank = 0;
	String creator = "";
	java.util.Date creationDate;
	String modifier = "";
	java.util.Date modificationDate;
	boolean markedForDeletion = false;
	boolean closed = false;

	boolean loaded = false;
	boolean created = false;
	boolean modified = false;
	boolean adaptAlias = false;

	FactoryImpl factory;
	MonitorImpl monitor;

	/**
	 *  Constructor for the ResourceImpl object
	 *
	 *@param  factory  Description of Parameter
	 *@since
	 */
	public ResourceImpl(FactoryImpl factory) {
		this.factory = factory;
		this.monitor = (MonitorImpl)factory.getMonitor();
	}

	/**
	 *  Constructor for the ResourceImpl object
	 *
	 *@param  factory  Description of Parameter
	 *@param  id       Description of Parameter
	 *@since
	 */
	public ResourceImpl(FactoryImpl factory, int id) {
		this(factory);
		this.id = id;
	}

	/**
	 *  Sets the Title attribute of the ResourceImpl object
	 *
	 *@param  title              The new Title value
	 *@exception  ZenoException  Description of Exception
	 *@since
	 */
	public void setTitle(String title) throws ZenoException {
		factory.checkPermission("ZenoResource.setTitle", this);
		if (!this.title.equals(title)) {
			this.title = title;
			adaptAlias = true;
			modified = true;
		}
	}

	/**
	 *  Sets the Note attribute of the ResourceImpl object
	 *
	 *@param  note               The new Note value
	 *@exception  ZenoException  Description of Exception
	 *@since
	 */
	public void setNote(String note) throws ZenoException {
		factory.checkPermission("ZenoResource.setNote", this);
		this.note = note;
		modified = true;
	}

	/**
	 *  Sets the Rank attribute of the ResourceImpl object
	 *
	 *@param  rank               The new Rank value
	 *@exception  ZenoException  Description of Exception
	 *@since
	 */
	public void setRank(int rank) throws ZenoException {
		factory.checkPermission("ZenoResource.setRank", this);
		this.rank = rank;
		modified = true;
	}

	/**
	 *  Sets the MarkedForDeletion attribute of the ResourceImpl object
	 *
	 *@param  markedForDeletion  The new MarkedForDeletion value
	 *@exception  ZenoException  Description of Exception
	 *@since
	 */
	public void setMarkedForDeletion(boolean markedForDeletion)
		throws ZenoException {
		factory.checkPermission("ZenoResource.setMarkedForDeletion", this);
		if (this.markedForDeletion != markedForDeletion) {
			this.markedForDeletion = markedForDeletion;
			this.modified = true;
		}
	}
	
	protected boolean isParentMarked() throws ZenoException {
		if (parentId == 0)
			return false;
		StringBuffer buf = new StringBuffer();
		buf.append("select marked_for_deletion from resource ");
		buf.append("where id=");
		buf.append(DBClient.format(parentId));
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
	
	protected void markLinks(boolean mark) throws ZenoException {
		try {
			StringBuffer buf = new StringBuffer();
			buf.append("update link set target_mark =");
			buf.append(DBClient.format(mark));
			buf.append(" where target=");
			buf.append(DBClient.format(id));
			factory.dbclient.executeUpdate(buf.toString());
		
			buf.setLength(0);
			buf.append("update link set source_mark =");
			buf.append(DBClient.format(mark));
			buf.append(" where source=");
			buf.append(DBClient.format(id));
			factory.dbclient.executeUpdate(buf.toString());
			
		} catch(java.sql.SQLException e) {
			factory.reportError("Article.adaptAlias", e);
			throw new ZenoException("DataBaseException");
		}	
	}
		 
	
	/** internal method without checks */
	
	protected void setDeletionMark(boolean mark)
				throws ZenoException {
		if (this.markedForDeletion != mark) {
			this.markedForDeletion = mark;
			this.modified = true;
			save();
			markLinks(mark);
		}
	}
	
	public boolean markForDeletion() throws ZenoException {
		factory.checkPermission("ZenoResource.markForDeletion", this);
		setDeletionMark(true);
		return true;
	}
	
	public boolean unmarkForDeletion() throws ZenoException {
		factory.checkPermission("ZenoResource.unmarkForDeletion", this);
		if (!this.markedForDeletion)
			return true;
		else if (isParentMarked())
			return false;
		else {
			setDeletionMark(false);
			return true;
		}
	}

	/**
	 *  Sets the Lock attribute of the ResourceImpl object
	 *
	 *@param  closed             The new Lock value
	 *@exception  ZenoException  Description of Exception
	 *@since
	 */
	//obsolete
	public void setLock(boolean closed) throws ZenoException {
		factory.checkPermission("ZenoResource.setLock", this);
		this.closed = closed;
		this.modified = true;
	}
	
	public void open() throws ZenoException {
		factory.checkPermission("ZenoResource.open", this);
		if (this.closed == true) {
			this.closed = false;
			this.modified = true;
			save();
		}
	}
	
	public void close() throws ZenoException {
		factory.checkPermission("ZenoResource.close", this);
		if (this.closed == false) {
			this.closed = true;
			this.modified = true;
			save();
		}
	}

	/**
	 *  Sets the Property attribute of the ResourceImpl object
	 *
	 *@param  propertyName       The new Property value
	 *@param  propertyValue      The new Property value
	 *@exception  ZenoException  Description of Exception
	 *@since
	 */
	public void setProperty(String propertyName, String propertyValue)
		throws ZenoException {
		try {
			factory.checkPermission("ZenoResource.setProperty", this);

			// to do: start transaction
			StringBuffer buf = new StringBuffer();

			if (getProperty(propertyName) == null) {
				// insert new property
				buf.append("insert into property (value, resource, name)");
				buf.append(" values(");
				buf.append(DBClient.format(propertyValue));
				buf.append(", ");
				buf.append(DBClient.format(id));
				buf.append(", ");
				buf.append(DBClient.format(propertyName));
				buf.append(")");
			} else {
				// update existing property
				buf.append("update property set value=");
				buf.append(DBClient.format(propertyValue));
				buf.append(" where resource = ");
				buf.append(DBClient.format(id));
				buf.append(" and name=");
				buf.append(DBClient.format(propertyName));
			}

			factory.dbclient.executeUpdate(buf.toString());

			// record and report modification
			this.modified = true;
			save();
			// to do: commit transaction
		} catch (java.sql.SQLException e) {
			// to do: roll back
			factory.reportError("ZenoResource.setProperty", e);
			throw new ZenoException("DataBaseException");
		}
	}

	/**
	 *  Everyone may read this property. No permission checking is necessary. The
	 *  id is null if the article has not been loaded or created using a factory.
	 *
	 *@return    The Id value
	 *@since
	 */

	public int getId() {
		return id;
	}

	/**
	 *  Gets the URL attribute of the ResourceImpl object
	 *
	 *@return    The URL value
	 *@since
	 */
	public String getURL() {
		return "http://"
			+ monitor.getDomainName()
			+ "/"
			+ monitor.getWebApplication()
			+ "/view/"
			+ id;
	}

	/**
	 *  Gets the Title attribute of the ResourceImpl object
	 *
	 *@return                    The Title value
	 *@exception  ZenoException  Description of Exception
	 *@since
	 */
	public String getTitle() throws ZenoException {
		factory.checkPermission("ZenoResource.getTitle", this);
		return this.title;
	}

	/**
	 *  Gets the Note attribute of the ResourceImpl object
	 *
	 *@return                    The Note value
	 *@exception  ZenoException  Description of Exception
	 *@since
	 */
	public String getNote() throws ZenoException {
		try {
			factory.checkPermission("ZenoResource.getNote", this);
			if (note == null) {
				StringBuffer buf = new StringBuffer();
				buf.append("select note from resource ");
				buf.append("where id=");
				buf.append(DBClient.format(this.id));
				ResultSet rs = factory.dbclient.executeQuery(buf.toString());
				if (rs.next()) {
					this.note = rs.getString("note");
				}
			}
			return this.note;
		} catch (java.sql.SQLException e) {
			factory.reportError("ZenoResource.getNote", e);
			throw new ZenoException("DataBaseException");
		}
	}

	/**
	 *  Gets the Rank attribute of the ResourceImpl object
	 *
	 *@return                    The Rank value
	 *@exception  ZenoException  Description of Exception
	 *@since
	 */
	public int getRank() throws ZenoException {
		factory.checkPermission("ZenoResource.getRank", this);
		return this.rank;
	}

	/**
	 *  Gets the Creator attribute of the ResourceImpl object
	 *
	 *@return                    The Creator value
	 *@exception  ZenoException  Description of Exception
	 *@since
	 */
	public String getCreator() throws ZenoException {
		factory.checkPermission("ZenoResource.getCreator", this);
		return this.creator;
	}

	/**
	 *  Gets the CreationDate attribute of the ResourceImpl object
	 *
	 *@return                    The CreationDate value
	 *@exception  ZenoException  Description of Exception
	 *@since
	 */
	public java.util.Date getCreationDate() throws ZenoException {
		factory.checkPermission("ZenoResource.getCreationDate", this);
		return this.creationDate;
	}

	/**
	 *  Gets the Modifier attribute of the ResourceImpl object
	 *
	 *@return                    The Modifier value
	 *@exception  ZenoException  Description of Exception
	 *@since
	 */
	public String getModifier() throws ZenoException {
		factory.checkPermission("ZenoResource.getModifier", this);
		return this.modifier;
	}

	/**
	 *  Gets the ModificationDate attribute of the ResourceImpl object
	 *
	 *@return                    The ModificationDate value
	 *@exception  ZenoException  Description of Exception
	 *@since
	 */
	public java.util.Date getModificationDate() throws ZenoException {
		factory.checkPermission("ZenoResource.getModificationDate", this);
		if (this.modificationDate == null) {
			return this.creationDate;
		} else {
			return this.modificationDate;
		}
	}

	/**
	 *  Gets the Parent attribute of the ResourceImpl object
	 *
	 *@return                    The Parent value
	 *@exception  ZenoException  Description of Exception
	 *@since
	 */
	public ZenoCollection getParent() throws ZenoException {
		factory.checkPermission("ZenoResource.getParent", this);
		return (ZenoCollection) factory.loadResource(parentId);
	}
	
	public int getParentId() {
		return parentId;
	}

	/**
	 *  Gets the Journal attribute of the ResourceImpl object
	 *
	 *@return                    The Journal value
	 *@exception  ZenoException  Description of Exception
	 *@since
	 */
	public Journal getJournal() throws ZenoException {
		return (Journal) getParent();
	}

	/**
	 *  Gets the Path attribute of the ResourceImpl object
	 *
	 *@return                    The Path value
	 *@exception  ZenoException  Description of Exception
	 *@since
	 */
	
	public List getPath() throws ZenoException {
		return getPath(0);
	}
	
	
	public List getPath(int stopid) throws ZenoException {
		try {
			factory.checkPermission("ZenoResource.getPath", this);
			List l1 = new ArrayList();
			int pid = this.parentId;
			int rid = this.id;
			while (pid != 0 && pid != stopid) {
				StringBuffer buf = new StringBuffer();
				buf.append("select title, parent, class from resource");
				buf.append("  where id=");
				buf.append(DBClient.format(pid));
				ResultSet rs = factory.dbclient.executeQuery(buf.toString());
				if (rs.next()) {
					rid = pid;
					pid = rs.getInt("parent");
					l1.add(new PathElement(rid, rs.getString("title"), rs.getString("class")));
				} else {
					break;
				}
			}
			Collections.reverse(l1);
			return l1;
		} catch (java.sql.SQLException e) {
			factory.reportError("ZenoResource.getPath", e);
			throw new ZenoException("DataBaseException");
		}
	}

	/**
	 *  Gets the MarkedForDeletion attribute of the ResourceImpl object
	 *
	 *@return                    The MarkedForDeletion value
	 *@exception  ZenoException  Description of Exception
	 *@since
	 */
	public boolean getMarkedForDeletion() throws ZenoException {
		factory.checkPermission("ZenoResource.getMarkedForDeletion", this);
		return this.markedForDeletion;
	}

	/**
	 *  Gets the Links attribute of the ResourceImpl object
	 *
	 *@param  direction          Description of Parameter
	 *@return                    The Links value
	 *@exception  ZenoException  Description of Exception
	 *@since
	 */
	public List getLinks(int direction) throws ZenoException {
		factory.checkPermission("ZenoResource.getLinks", this);
		StringBuffer buf = new StringBuffer();
		buf.append("select * from link");
		if (direction > 0) {
			buf.append(" where source=");
			buf.append(DBClient.format(id));
		} else if (direction < 0) {
			buf.append(" where target=");
			buf.append(DBClient.format(id));
		} else {
			buf.append(" where source=");
			buf.append(DBClient.format(id));
			buf.append(" or target=");
			buf.append(DBClient.format(id));
		}
		buf.append(" and source_mark='false' and target_mark='false'");
		return LinkImpl.getLinksWhere(factory, buf.toString());
	}
	
	public List getLinks(int direction, boolean extended) throws ZenoException {
		List links = getLinks(direction);
		if (extended) {
			Hashtable restable = new Hashtable();
			restable.put(new Integer(this.id), this);
			LinkImpl.fillLinks(factory, links, restable, true);
		}
		return links;
	}

	/**
	 *  Gets the Link attribute of the ResourceImpl object
	 *
	 *@param  label              Description of Parameter
	 *@param  targetid           Description of Parameter
	 *@return                    The Link value
	 *@exception  ZenoException  Description of Exception
	 *@since
	 */
	public Link getLink(String label, int targetid) throws ZenoException {
		factory.checkPermission("ZenoResource.getLink", this);
		StringBuffer buf = new StringBuffer();
		buf.append("select * from link");
		buf.append(" where source=");
		buf.append(DBClient.format(id));
		buf.append(" and target=");
		buf.append(DBClient.format(targetid));
		buf.append(" and label=");
		buf.append(DBClient.format(label));
		buf.append(" and source_mark='false' and target_mark='false'");
		try {
			ResultSet rs = factory.dbclient.executeQuery(buf.toString());
			if (rs.next()) {
				Link link =
					new LinkImpl(
						factory,
						rs.getString("label"),
						rs.getString("source_alias"),
						rs.getString("target_alias"),
						rs.getInt("source"),
						rs.getInt("target"),
						rs.getString("flag"));
				return link;
			} else {
				throw new NotFoundException("NoSuchLink");
			}
		} catch (java.sql.SQLException e) {
			factory.reportError("ZenoResource.getLink", e);
			// to do: rollback
			throw new ZenoException("DBException");
		}
	}

	/**
	 *  Gets the LinkLabelsUsed attribute of the ResourceImpl object
	 *
	 *@param  direction          Description of Parameter
	 *@return                    The LinkLabelsUsed value
	 *@exception  ZenoException  Description of Exception
	 *@since
	 */
	public List getLinkLabelsUsed(int direction) throws ZenoException {
		try {
			factory.checkPermission("ZenoResource.getLinkLabelsUsed", this);
			StringBuffer buf = new StringBuffer();
			if (direction > 0) {
				buf.append("select label from link where source=");
				buf.append(DBClient.format(id));
			} else if (direction < 0) {
				buf.append("select label from link where target=");
				buf.append(DBClient.format(id));
			} else {
				buf.append("select label from link where (source=");
				buf.append(DBClient.format(id));
				buf.append(" or target=");
				buf.append(DBClient.format(id));
				buf.append(")");
			}
			buf.append(" and source_mark='false' and target_mark='false'");
			ResultSet rs = factory.dbclient.executeQuery(buf.toString());
			Set s = new TreeSet();
			while (rs.next()) {
				s.add(rs.getString("label"));
			}
			List result = new ArrayList(s);
			return result;
		} catch (java.sql.SQLException e) {
			factory.reportError("ZenoResource.getLinkLabelsUsed", e);
			throw new ZenoException("DataBaseException");
		}
	}

	/**
	 *  Gets the LinksWithLabel attribute of the ResourceImpl object
	 *
	 *@param  direction          Description of Parameter
	 *@param  label              Description of Parameter
	 *@return                    The LinksWithLabel value
	 *@exception  ZenoException  Description of Exception
	 *@since
	 */
	public List getLinksWithLabel(int direction, String label)
		throws ZenoException {
		factory.checkPermission("ZenoResource.getLinksWithLabel", this);
		return getLinksWithLabel(factory, this.id, direction, label);
	}
	
	public XLink getXLink(String type, String reference) 
			throws ZenoException {
		
		factory.checkPermission("ZenoResource.getXLink", this);	
		StringBuffer buf = new StringBuffer();
		buf.append("select * from xlink where ");
		buf.append(" source=");
		buf.append(DBClient.format(this.id));
		buf.append(" and type =");
		buf.append(DBClient.format(type));
		buf.append(" and reference =");
		buf.append(DBClient.format(reference));
		List links = XLinkImpl.getXLinksWhere(factory, buf.toString());
		if (links.isEmpty())
			throw new NotFoundException("NoSuchLink");
		else 
			return (XLink)links.get(0);
	}
	
	public List getXLinks(String type) 
			throws ZenoException {
		
		factory.checkPermission("ZenoResource.getXLinks", this);	
		StringBuffer buf = new StringBuffer();
		buf.append("select * from xlink where ");
		buf.append(" source=");
		buf.append(DBClient.format(this.id));
		if (type != null && !type.equals("")) {
			buf.append(" and type like ");
			buf.append(DBClient.format(type));
		}
		return XLinkImpl.getXLinksWhere(factory, buf.toString());
	}
	
	public List getPropertyKeys() throws ZenoException {
		factory.checkPermission("ZenoResource.getPropertyKeys", this);
		List result = new ArrayList();
		StringBuffer buf = new StringBuffer();
		buf.append("select name from property where resource = ");
		buf.append(DBClient.format(id));
		try {	
			ResultSet rs = factory.dbclient.executeQuery(buf.toString());
			while (rs.next()){
				result.add(rs.getString("name"));
			}
			return result;
		} catch (java.sql.SQLException e) {
			factory.reportError("ZenoResource.getPropertyKeys", e);
			throw new ZenoException("DataBaseException");
		}
	}

	/**
	 *  Returns null if the property has no value
	 *
	 *@param  propertyName       Description of Parameter
	 *@return                    The Property value
	 *@exception  ZenoException  Description of Exception
	 *@since
	 */

	public String getProperty(String propertyName) throws ZenoException {
		factory.checkPermission("ZenoResource.getProperty", this);
		try {
			StringBuffer buf = new StringBuffer();
			buf.append("select value from property where resource = ");
			buf.append(DBClient.format(id));
			buf.append(" and name=");
			buf.append(DBClient.format(propertyName));
			ResultSet rs = factory.dbclient.executeQuery(buf.toString());
			if (rs.next()) {
				return rs.getString("value");
			} else {
				return null;
			}
		} catch (java.sql.SQLException e) {
			factory.reportError("ZenoResource.getProperty", e);
			throw new ZenoException("DataBaseException");
		}
	}

	/**
	 *  Checks whether a resource with this id still exists. This check can be
	 *  useful at any time, even after the resource has been successfully loaded.
	 *  It may be that the resource was deleted after it was loaded. Permission
	 *  checking is not done, since a NoPermission exception would reveal that the
	 *  resource exists.
	 *
	 *@return                    Description of the Returned Value
	 *@exception  ZenoException  Description of Exception
	 *@since
	 */

	public boolean exists() throws ZenoException {
		try {
			String query = "select id from resource where id = " + id;
			ResultSet rs = factory.dbclient.executeQuery(query);
			return (rs != null && rs.next());
		} catch (java.sql.SQLException e) {
			factory.reportError("ZenoResource.exists", e);
			throw new ZenoException("DataBaseException");
		}
	}

	/**
	 *  loads the specific properties of this type of resource. Should be
	 *  overridden by subclasses. The generic properties of all resources are not
	 *  loaded here, since they are already loaded by the loadResource method of
	 *  the Factory. Resources newly created by the Factory do not need to be
	 *  loaded, since they by definition do not yet have properties stored in the
	 *  persistent store.
	 *
	 *@exception  ZenoException  Description of Exception
	 *@since
	 */

	public abstract void loadProperties() throws ZenoException;

	/**
	 *  Description of the Method
	 *
	 *@return                    Description of the Returned Value
	 *@exception  ZenoException  Description of Exception
	 *@since
	 */
	public int noteSize() throws ZenoException {
		factory.checkPermission("ZenoResource.noteSize", this);
		return note.length();
	}

	/**
	 *  Description of the Method
	 *
	 *@return                    Description of the Returned Value
	 *@exception  ZenoException  Description of Exception
	 *@since
	 */
	public boolean hasParent() throws ZenoException {
		factory.checkPermission("ZenoResource.hasParent", this);
		return parentId != 0;
	}
	
	protected String getAlias() throws ZenoException {
		return this.title;
	}
	
	
	protected void adaptAlias()  throws ZenoException {
		adaptAlias(getAlias());
	}
	
	protected void adaptAlias(String newAlias) throws ZenoException {
		try {
			StringBuffer buf = new StringBuffer();
			buf.append("update link set target_alias =");
			buf.append(DBClient.format(newAlias));
			buf.append(" where target=");
			buf.append(DBClient.format(id));
			buf.append(" and (flag='t' or flag='b')");
			factory.dbclient.executeUpdate(buf.toString());
		
			buf.setLength(0);
			buf.append("update link set source_alias =");
			buf.append(DBClient.format(newAlias));
			buf.append(" where source=");
			buf.append(DBClient.format(id));
			buf.append(" and (flag='s' or flag='b')");
			factory.dbclient.executeUpdate(buf.toString());
			
		} catch(java.sql.SQLException e) {
			factory.reportError("Article.adaptAlias", e);
			throw new ZenoException("DataBaseException");
		}	
	}

	/**
	 *  Description of the Method
	 *
	 *@exception  ZenoException  Description of Exception
	 *@since
	 */
	public void save() throws ZenoException {
		try {
			// permissions already checked by set methods
			if (modified) {
				Principal user = factory.getUser();

				StringBuffer buf = new StringBuffer();
				buf.append("update resource");
				buf.append(" set class =");
				buf.append(DBClient.format(this.zenoClass));
				buf.append(", title =");
				buf.append(DBClient.format(this.title));
				if (this.note != null) {
					buf.append(", note = ");
					buf.append(DBClient.format(this.note));
				}
				buf.append(", rank =");
				buf.append(DBClient.format(this.rank));
				buf.append(", creator =");
				buf.append(DBClient.format(this.creator));
				buf.append(", creation_date =");
				buf.append(DBClient.format(this.creationDate));
				buf.append(", modifier =");
				this.modifier = user.getId();
				buf.append(DBClient.format(this.modifier));
				buf.append(", modification_date =");
				this.modificationDate = new java.util.Date();
				buf.append(DBClient.format(this.modificationDate));
				buf.append(", parent = ");
				buf.append(DBClient.format(this.parentId));
				buf.append(", marked_for_deletion =");
				buf.append(DBClient.format(this.markedForDeletion));
				buf.append(", locked =");
				buf.append(DBClient.format(this.closed));
				buf.append(" where id =");
				buf.append(this.id);
				factory.dbclient.executeUpdate(buf.toString());
				if (adaptAlias) {
						adaptAlias();
						adaptAlias = false;
					}
				monitor.reportEvent(
					new ModificationEvent(
						new Integer(this.id),
						user.getId(),
						this.modificationDate));

				modified = false;
			}
		} catch (java.sql.SQLException e) {
			factory.reportError("ZenoResource.save", e);
			throw new ZenoException("DataBaseException");
		}
	}

	/**
	 *  Description of the Method
	 *
	 *@return                    Description of the Returned Value
	 *@exception  ZenoException  Description of Exception
	 *@since
	 */
	public boolean locked() throws ZenoException {
		factory.checkPermission("ZenoResource.isClosed", this);
		return this.closed;
	}
	
	public boolean isClosed() throws ZenoException {
		factory.checkPermission("ZenoResource.isClosed", this);
		return this.closed;
	}
	
	public String getStatus() throws ZenoException {
		factory.checkPermission("ZenoResource.getStatus", this);
		String status = "";
		if (markedForDeletion)
			status = "d";
		if (closed)
			status = status + "c";
		return status;
	}
	
	protected boolean isVisibleForReader() {
		return !this.markedForDeletion; 
	}
	
	public abstract void move(ZenoCollection collection)
		throws ZenoException;

	
	protected abstract void moveTo(ZenoCollection collection) 
		throws ZenoException;

	/**
	 *  Description of the Method
	 *
	 *@param  collection         Description of Parameter
	 *@exception  ZenoException  Description of Exception
	 *@since
	 */
	/*
	public void move(ZenoCollection collection) throws ZenoException {
		factory.checkPermission("ZenoResource.move", this);
		factory.checkPermission("Journal.paste", collection);

		this.parentId = collection.getId();

		// record and report modification
		this.modified = true;
		save();

		JournalImpl parent = (JournalImpl) getParent();
		parent.modified();

		((JournalImpl) collection).modified();
	}
	*/

	/**
	 *  Description of the Method
	 *
	 *@param  collection         Description of Parameter
	 *@param  withLabels         Description of Parameter
	 *@return                    Description of the Returned Value
	 *@exception  ZenoException  Description of Exception
	 *@since
	 */
	public abstract ZenoResource copy(
		ZenoCollection collection,
		boolean withLabels)
		throws ZenoException;

		
	protected ZenoResource copy(ZenoCollection collection, Hashtable newids, String mode)
			throws ZenoException {
			
		ResourceImpl clone;
		Integer cloneId;
		java.util.Date cloneCreationDate = new java.util.Date();
		
		if (!loaded)
			loadProperties();
		try {
			clone = (ResourceImpl)this.clone();
			clone.id = 0;
			if (mode.equals("adopt")) {
				clone.creator = null;
				clone.creationDate = cloneCreationDate;
				clone.modifier = null;
				clone.modificationDate = null;
			}
			clone.create(collection);
			cloneId = new Integer(clone.id);
			String userId = factory.getUser().getId();
			factory.monitor.reportEvent(
				new CreationEvent(cloneId, userId, cloneCreationDate));
	
		} catch (CloneNotSupportedException e) {
			return null;
		}
		
		//copy user defined properties	
		try {
			int targetid = clone.getId();
			StringBuffer buf = new StringBuffer();
			buf.append("select * from property where resource = ");
			buf.append(DBClient.format(id));
			ResultSet rs = factory.dbclient.executeQuery(buf.toString());
			while(rs.next()) {
				buf.setLength(0);
				buf.append("insert into property ");
				buf.append("(resource, name, value)");
				buf.append("values(");
				buf.append(DBClient.format(targetid));
				buf.append(", ");
				buf.append(DBClient.format(rs.getString("name")));
				buf.append(", ");
				buf.append(DBClient.format(rs.getString("value")));
				buf.append(")");
				factory.dbclient.executeUpdate(buf.toString());
			}
		} catch (java.sql.SQLException e) {
			factory.reportError("Link.copyProperties", e);
		}

		if (newids != null) {
			newids.put(new Integer(this.id), cloneId);
			List xlinks = getXLinks("");
			XLinkImpl.copyXLinks(factory, xlinks, newids);
		}

		return clone;
	}

	/**
	 *  Adds a feature to the Link attribute of the ResourceImpl object
	 *
	 *@param  sourceAlias        The feature to be added to the Link attribute
	 *@param  label              The feature to be added to the Link attribute
	 *@param  targetId           The feature to be added to the Link attribute
	 *@param  targetAlias        The feature to be added to the Link attribute
	 *@exception  ZenoException  Description of Exception
	 *@since
	 */
	public void addLink(
			String sourceAlias,
			String label,
			int targetId,
			String targetAlias)
		throws ZenoException {
		try {
			factory.checkPermission("ZenoResource.addLink", this);
			
			ResourceImpl target = 
					(ResourceImpl)factory.loadResource(targetId);
			if (markedForDeletion || target.markedForDeletion) {
				throw new NoPermissionException("source/target_deleted");
			}
			String flag = "";
			if (sourceAlias.equals("")) {
				sourceAlias = getAlias();
				flag = "s";
			}
			if (targetAlias.equals("")) {
				targetAlias = target.getAlias();
				flag =  flag.equals("s") ? "b" : "t";
			}
			// to do: start transaction
			StringBuffer buf = new StringBuffer();
			
			buf.append("insert into link ");
			buf.append("(source, source_alias, label, target, target_alias, flag)");
			buf.append("values(");
			buf.append(DBClient.format(id));
			buf.append(", ");
			buf.append(DBClient.format(sourceAlias));
			buf.append(", ");
			buf.append(DBClient.format(label));
			buf.append(", ");
			buf.append(DBClient.format(targetId));
			buf.append(", ");
			buf.append(DBClient.format(targetAlias));
			buf.append(", ");
			buf.append(DBClient.format(flag));
			buf.append(")");

			factory.dbclient.executeQuery(buf.toString());

			// record and report modification
			this.modified = true;
			save();
			// to do: commit transaction
		} catch (java.sql.SQLException e) {
			// to do: roll back
			factory.reportError("ZenoResource.addLink", e);
			throw new ZenoException("DataBaseException");
		}
	}

	/**
	 *  Description of the Method
	 *
	 *@param  label              Description of Parameter
	 *@param  targetId           Description of Parameter
	 *@exception  ZenoException  Description of Exception
	 *@since
	 */
	public void deleteLink(String label, int targetId) throws ZenoException {
		try {
			factory.checkPermission("ZenoResource.deleteLink", this);
			StringBuffer buf = new StringBuffer();

			buf.append("delete from link where ");
			buf.append("label=");
			buf.append(DBClient.format(label));
			buf.append(" and target=");
			buf.append(DBClient.format(targetId));
			buf.append(" and source=");
			buf.append(DBClient.format(this.id));

			// to do: start transaction
			factory.dbclient.executeQuery(buf.toString());

			// record and report modification
			this.modified = true;
			save();
			// to do: commit transaction

		} catch (java.sql.SQLException e) {
			factory.reportError("ZenoResource.deleteLink", e);
			// to do: rollback
			throw new ZenoException("DataBaseException");
		}
	}
	
	public void addXLink(String type, String reference, String name)
		throws ZenoException {
		try {
			factory.checkPermission("ZenoResource.addXLink", this);
			
			// to do: start transaction
			StringBuffer buf = new StringBuffer();
			
			buf.append("insert into xlink ");
			buf.append("(source, type, reference, name)");
			buf.append(" values(");
			buf.append(DBClient.format(id));
			buf.append(", ");
			buf.append(DBClient.format(type));
			buf.append(", ");
			buf.append(DBClient.format(reference));
			buf.append(", ");
			buf.append(DBClient.format(name));
			buf.append(")");
			factory.dbclient.executeQuery(buf.toString());

			// to do: commit transaction
		} catch (java.sql.SQLException e) {
			// to do: roll back
			factory.reportError("ZenoResource.addXLink", e);
			throw new ZenoException("DataBaseException");
		}
	}

	
	public void deleteXLink(String type, String reference) throws ZenoException {
		try {
			factory.checkPermission("ZenoResource.deleteXLink", this);
			StringBuffer buf = new StringBuffer();

			buf.append("delete from xlink where ");
			buf.append("type =");
			buf.append(DBClient.format(type));
			buf.append(" and reference =");
			buf.append(DBClient.format(reference));
		
			// to do: start transaction
			factory.dbclient.executeQuery(buf.toString());

			// to do: commit transaction

		} catch (java.sql.SQLException e) {
			factory.reportError("ZenoResource.deleteXLink", e);
			// to do: rollback
			throw new ZenoException("DataBaseException");
		}
	}

	/**
	 *  Description of the Method
	 *
	 *@param  propertyName       Description of Parameter
	 *@exception  ZenoException  Description of Exception
	 *@since
	 */
	public void removeProperty(String propertyName) throws ZenoException {

		StringBuffer buf = new StringBuffer();
		buf.append("delete from property where resource=");
		buf.append(DBClient.format(this.id));
		buf.append(" and name=");
		buf.append(DBClient.format(propertyName));
		try {
			factory.dbclient.executeUpdate(buf.toString());
		} catch (java.sql.SQLException e) {
			factory.reportError("ZenoResource.removeProperty", e);
			throw new ZenoException("DataBaseException");
		}

	}

	/**
	 *  Description of the Method
	 *
	 *@return    Description of the Returned Value
	 *@since
	 */
	public String toString() {
		return "[" + zenoClass + " " + id + " " + title + " " + creator + "]";
	}

	/**
	 *  Description of the Method
	 *
	 *@param  prw  Description of Parameter
	 *@since
	 */
	public void show(PrintWriter prw) {
		if (prw == null) {
			prw = new PrintWriter(System.out, true);
		}
		prw.println("<resource ");
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
		prw.println("    loaded=" + quote(loaded));
		prw.println("    modified=" + quote(modified));
		prw.println("    locked=" + quote(closed));
		prw.println(">");
		try {
			prw.println(getNote());
		} catch (ZenoException e) {
			factory.reportError("ZenoResource.show", e);
		}
		prw.println("</resource>");
	}

	/**
	 *  Creates a <em>new</em> resource and makes it persistent. The save method
	 *  does not need to be called until properties have the resource have been
	 *  changed. Can be specialized to initialize the properties and content of
	 *  particular kinds of resources.
	 *
	 *@param  parent             Description of Parameter
	 *@exception  ZenoException  Description of Exception
	 *@since
	 */
	
	
	protected void create(ZenoCollection parent) throws ZenoException {
		
		if (id != 0) return;
		
		try {
			if (this.creator == null | "".equals(this.creator)) {
				Principal user = factory.getUser();
				this.creator = user.getId();
				this.creationDate = new java.util.Date();
			}
			if (parent != null) {
				if (parent.exists())
					this.parentId = parent.getId();
				else 
					throw new ZenoException("NoSuchParent");
			}
			
			StringBuffer buf = new StringBuffer();
			buf.append("insert into resource ");
			buf.append("(id, class, title, note, rank, creator, creation_date, modifier");
			buf.append(", modification_date, parent, marked_for_deletion, locked, part)");
			buf.append("  values(null, ");
			buf.append(DBClient.format(this.zenoClass));
			buf.append(", ");
			buf.append(DBClient.format(this.title));
			buf.append(", ");
			buf.append(DBClient.format(this.note == null ? "" : note));
			buf.append(", ");
			buf.append(DBClient.format(this.rank));
			buf.append(", ");
			buf.append(DBClient.format(this.creator));
			buf.append(", ");
			buf.append(DBClient.format(this.creationDate));
			buf.append(", ");
			buf.append(DBClient.format(this.modifier));
			buf.append(", ");
			buf.append(DBClient.format(this.modificationDate));
			buf.append(", ");
			buf.append(DBClient.format(this.parentId));
			buf.append(", ");
			buf.append(DBClient.format(this.markedForDeletion));
			buf.append(", ");
			buf.append(DBClient.format(this.closed));
			buf.append(", ");
			buf.append(DBClient.format(this.part));
			buf.append(")");
			factory.dbclient.executeUpdate(buf.toString());
			modified = false;
			
			String request = "select last_insert_id() from resource";
			ResultSet rs = factory.dbclient.executeQuery(request);
			if (rs.next()) {
				this.id = rs.getInt(1);
			}
		} catch (java.sql.SQLException e) {
			factory.reportError("ZenoResource.create", e);
			throw new ZenoException("DatabaseException");
		}
	}

	/**
	 *  Description of the Method
	 *
	 *@param  rs                         Description of Parameter
	 *@exception  java.sql.SQLException  Description of Exception
	 *@since
	 */
	protected void fill(ResultSet rs) throws java.sql.SQLException {
		/*
		 * assumes the rs is not null and not empty
		 */
		this.id = rs.getInt("id");
		this.zenoClass = rs.getString("class");
		this.title = rs.getString("title");
		Blob blob = rs.getBlob("note");
		if (blob.length() < 1000) {
			this.note = rs.getString("note");
		}
		this.rank = rs.getInt("rank");
		this.creator = rs.getString("creator");
		this.creationDate = rs.getTimestamp("creation_date");
		this.modifier = rs.getString("modifier");
		this.modificationDate = rs.getTimestamp("modification_date");
		this.parentId = rs.getInt("parent");
		this.markedForDeletion = rs.getBoolean("marked_for_deletion");
		this.closed = rs.getBoolean("locked");
		this.part = rs.getInt("part");
	}

	/**
	 *  Description of the Method
	 *
	 *@exception  ZenoException  Description of Exception
	 *@since
	 */
	protected void modified() throws ZenoException {

		try {
			Principal user = factory.getUser();
			this.modifier = user.getId();
			this.modificationDate = new java.util.Date();

			StringBuffer buf = new StringBuffer();
			buf.append("update resource");
			buf.append(" set modifier =");
			buf.append(DBClient.format(this.modifier));
			buf.append(", modification_date =");
			buf.append(DBClient.format(this.modificationDate));
			buf.append(", creation_date =");
			buf.append(DBClient.format(this.creationDate));
			buf.append(" where id =");
			buf.append(this.id);

			factory.dbclient.executeUpdate(buf.toString());
			monitor.reportEvent(
				new ModificationEvent(
					new Integer(this.id),
					user.getId(),
					this.modificationDate));

		} catch (java.sql.SQLException e) {
			factory.reportError("ZenoResource.modified", e);
			throw new ZenoException("DataBaseException");
		}
	}

	/**
	 *  Description of the Method
	 *
	 *@param  s  Description of Parameter
	 *@return    Description of the Returned Value
	 *@since
	 */
	protected String quote(String s) {
		return "\"" + s + "\"";
	}

	/**
	 *  Description of the Method
	 *
	 *@param  i  Description of Parameter
	 *@return    Description of the Returned Value
	 *@since
	 */
	protected String quote(int i) {
		return "\"" + i + "\"";
	}

	/**
	 *  Description of the Method
	 *
	 *@param  d  Description of Parameter
	 *@return    Description of the Returned Value
	 *@since
	 */
	protected String quote(java.util.Date d) {
		return "\"" + d + "\"";
	}

	/**
	 *  Description of the Method
	 *
	 *@param  b  Description of Parameter
	 *@return    Description of the Returned Value
	 *@since
	 */
	protected String quote(boolean b) {
		return "\"" + b + "\"";
	}

	//heg obsolete
	/**
	 *  Gets the LinksWithLabel attribute of the ResourceImpl class
	 *
	 *@param  factory            Description of Parameter
	 *@param  resourceId         Description of Parameter
	 *@param  direction          Description of Parameter
	 *@param  label              Description of Parameter
	 *@return                    The LinksWithLabel value
	 *@exception  ZenoException  Description of Exception
	 *@since
	 */
	protected static List getLinksWithLabel(
		FactoryImpl factory,
		int resourceId,
		int direction,
		String label)
		throws ZenoException {
		try {
			StringBuffer buf = new StringBuffer();
			buf.append("select * from link where ");
			if (label != null && !label.equals("")) {
					buf.append("label=");
					buf.append(DBClient.format(label));
					buf.append(" and");
				}
			if (direction > 0) {
				buf.append(" source=");
				buf.append(DBClient.format(resourceId));
			} else if (direction < 0) {
				buf.append(" target=");
				buf.append(DBClient.format(resourceId));
			} else {
				buf.append(" (source=");
				buf.append(DBClient.format(resourceId));
				buf.append(" or target=");
				buf.append(DBClient.format(resourceId));
				buf.append(")");
			}
			buf.append(" and source_mark='false' and target_mark='false'");
			ResultSet rs = factory.dbclient.executeQuery(buf.toString());
			List result = new ArrayList();
			while (rs.next()) {
				Link link =
					new LinkImpl(
						factory,
						rs.getString("label"),
						rs.getString("source_alias"),
						rs.getString("target_alias"),
						rs.getInt("source"),
						rs.getInt("target"),
						rs.getString("flag"));
				result.add(link);
			}
			return result;
		} catch (java.sql.SQLException e) {
			factory.reportError("ZenoResource.getLinkLabels", e);
			throw new ZenoException("DataBaseException");
		}
	}
}