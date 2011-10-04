package zeno2.db;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import zeno2.kernel.ZenoResource;
import zeno2.kernel.ZenoCollection;
import zeno2.kernel.ZenoException;
import zeno2.kernel.NoPermissionException;

/** Zeno 2 Collection Implementation */

abstract public class CollectionImpl
	extends ResourceImpl
	implements ZenoCollection {

	public CollectionImpl(FactoryImpl factory) {
		super(factory);
	}

	public CollectionImpl(FactoryImpl factory, int id) {
		super(factory, id);
	}

	/** Returns an enumeration of the members of this collection which are not
	marked for deletion. Each member is a ZenoResource. The order of the
	members in this enumeration is unspecified. */

	public java.util.Iterator getMembers() throws ZenoException {
		try {
			List result = new ArrayList();
			StringBuffer buf = new StringBuffer();
			buf.append("select * from resource where parent = ");
			buf.append(DBClient.format(id));
			buf.append(" and marked_for_deletion='false'");
			ResultSet rs = factory.dbclient.executeQuery(buf.toString());
			while (rs.next()) {
				result.add(factory.loadResource(rs));
			}
			return result.iterator();
		} catch (java.sql.SQLException e) {
			factory.reportError("Collection.getMembers", e);
			throw new ZenoException("DataBaseException");
		}
	}
	
	protected List getMembers(String type, boolean all) throws ZenoException {
		try {
			List result = new ArrayList();
			StringBuffer buf = new StringBuffer();
			buf.append("select * from resource where parent = ");
			buf.append(DBClient.format(id));
			if (type != null) {
				buf.append(" and class = ");
				buf.append(DBClient.format(type));
			} if (! all)
				buf.append(" and marked_for_deletion='false'");
			ResultSet rs = factory.dbclient.executeQuery(buf.toString());
			while (rs.next()) {
				result.add(factory.loadResource(rs));
			}
			return result;
		} catch (java.sql.SQLException e) {
			factory.reportError("Collection.getMembers", e);
			throw new ZenoException("DataBaseException");
		}
	}
	
	protected List getDirectMembers() throws ZenoException {
		try {
			List result = new ArrayList();
			StringBuffer buf = new StringBuffer();
			buf.append("select * from resource where parent = ");
			buf.append(DBClient.format(id));
			buf.append(" and (part = 0 or part = id)");
			buf.append(" and marked_for_deletion='false'");
			ResultSet rs = factory.dbclient.executeQuery(buf.toString());
			while (rs.next()) {
				result.add(factory.loadResource(rs));
			}
			return result;
		} catch (java.sql.SQLException e) {
			factory.reportError("Collection.getMembers", e);
			throw new ZenoException("DataBaseException");
		}
	}
	

	/** Returns the members sorted by rank and then title, not including
	members marked for deletion. */

	public java.util.Iterator sortMembers() throws ZenoException {
		try {
			List result = new ArrayList();
			StringBuffer buf = new StringBuffer();
			buf.append("select * from resource where parent = ");
			buf.append(DBClient.format(id));
			buf.append(" and marked_for_deletion='false'");
			buf.append(" order by rank, title");
			ResultSet rs = factory.dbclient.executeQuery(buf.toString());
			while (rs.next()) {
				result.add(factory.loadResource(rs));
			}
			return result.iterator();
		} catch (java.sql.SQLException e) {
			factory.reportError("Collection.sortMembers", e);
			throw new ZenoException("DataBaseException");
		}
	}

	/** Permanently remove all member resources marked for deletion.
	Does not recursively compact member collections. */

	abstract public void compact() throws ZenoException;

	/** Returns a set of all member resources which have been marked
	for deletion in this collection.  Does not list resources in member
	collections. */

	public Iterator getTrash() throws ZenoException {
		try {
			List result = new ArrayList();
			StringBuffer buf = new StringBuffer();
			buf.append("select * from resource where parent = ");
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
			factory.reportError("Collection.getTrash", e);
			throw new ZenoException("DataBaseException");
		}
	}
	
	public boolean markForDeletion() throws ZenoException {
		factory.checkPermission("ZenoResource.markForDeletion", this);
		if (this.markedForDeletion)
			return true;
		else {
			boolean success = true;
			Iterator it = getMembers();
			while(it.hasNext()){
				try {
					ZenoResource res = (ZenoResource)it.next();
					success = res.markForDeletion() && success;
				} catch(NoPermissionException e) {
					success = false;
				} catch(ZenoException e) {
					factory.reportError("Collection.markForDeletion", e);
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
				ZenoResource res = (ZenoResource)it.next();
				if (res instanceof ZenoCollection  )
					((CollectionImpl)res).propagateUnmark();
				else 
					((ResourceImpl)res).setDeletionMark(false);
			} catch(NoPermissionException e) {
			} catch(ZenoException e) {
				factory.reportError("Collection.unmarkForDeletion", e);
			}
		}
	}
	
	public boolean unmarkForDeletion(boolean propagate) throws ZenoException {
		
		boolean result = unmarkForDeletion();
		if (result && propagate)
			propagateUnmark();
		return result;
	}

}