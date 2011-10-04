package zeno2.db;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import zeno2.kernel.Link;
import zeno2.kernel.ZenoException;
import zeno2.kernel.ZenoResource;

/**
 *  Zeno 2 Link Implementation
 *
 *@author     oppor
 *@created    September 24, 2001
 */

public class LinkImpl implements Link {
	String label;
	String sourceAlias;
	String targetAlias;
	int source;
	int target;
	String flag;
	FactoryImpl factory;
	ZenoResource sourceResource;
	ZenoResource targetResource;

	/**
	 *  Constructor for the LinkImpl object
	 *
	 *@param  factory      Description of Parameter
	 *@param  label        Description of Parameter
	 *@param  sourceAlias  Description of Parameter
	 *@param  targetAlias  Description of Parameter
	 *@param  source       Description of Parameter
	 *@param  target       Description of Parameter
	 *@since
	 */
	public LinkImpl(
		FactoryImpl factory,
		String label,
		String sourceAlias,
		String targetAlias,
		int source,
		int target) {
		this.factory = factory;
		this.label = label;
		this.sourceAlias = sourceAlias;
		this.targetAlias = targetAlias;
		this.source = source;
		this.target = target;
		this.flag = "b";
	}
	
	public LinkImpl(
		FactoryImpl factory,
		String label,
		String sourceAlias,
		String targetAlias,
		int source,
		int target,
		String flag) {
		this.factory = factory;
		this.label = label;
		this.sourceAlias = sourceAlias;
		this.targetAlias = targetAlias;
		this.source = source;
		this.target = target;
		this.flag = (flag == null) ? "" : flag;
	}

	/**
	 *  Change the source alias. Save to make persistent.
	 *
	 *@param  sourceAlias  The new SourceAlias value
	 *@since
	 */
	public void setSourceAlias(String sourceAlias) {
		if (!this.sourceAlias.equals(sourceAlias)) {
			if (sourceAlias.equals("")) {
				try {
					ResourceImpl res = (ResourceImpl)factory.loadResource(source);
					flag = ( flag.equals("b") || flag.equals("t")) ? "b" : "s";
					this.sourceAlias = res.getAlias();
				} catch (ZenoException e) {}
			} else {
				flag = ( flag.equals("b") || flag.equals("t")) ? "t" : "";
				this.sourceAlias = sourceAlias;
			}
		}
	}


	/**
	 *  Change the target alias. Save to make persistent.
	 *
	 *@param  targetAlias  The new TargetAlias value
	 *@since
	 */
	public void setTargetAlias(String targetAlias) {
		if (!this.targetAlias.equals(targetAlias)) {
			if (targetAlias.equals("")) {
				try {
					ResourceImpl res = (ResourceImpl)factory.loadResource(target);
					flag = ( flag.equals("b") || flag.equals("s")) ? "b" : "t";
					this.targetAlias = res.getAlias();
				} catch (ZenoException e) {}
			} else {
				flag = ( flag.equals("b") || flag.equals("s")) ? "s" : "";
				this.targetAlias = targetAlias;
			}
		}
	}

	/**
	 *  Get the label of the link.
	 *
	 *@return    The Label value
	 *@since
	 */

	public String getLabel() {
		return label;
	}

	/**
	 *  Get the source alias of the link.
	 *
	 *@return    The SourceAlias value
	 *@since
	 */
	public String getSourceAlias() {
		return sourceAlias;
	}
	
	
	/**
	 *  Get the source alias of the link provided by the user.
	 *
	 *@return    The SourceAlias value
	 *@since
	 */
	public String getUserSourceAlias() {
		if (flag.equals("s") || flag.equals("b"))
			return "";
		else
			return sourceAlias;
	}
	
	/**
	 *  Get the target alias of the link.
	 *
	 *@return    The TargetAlias value
	 *@since
	 */
	public String getTargetAlias() {
		return targetAlias;
	}
	
	/**
	 *  Get the target alias of the link provided by the user.
	 *
	 *@return    The TargetAlias value
	 *@since
	 */
	public String getUserTargetAlias() {
		if (flag.equals("t") || flag.equals("b"))
			return "";
		else
			return targetAlias;
	}

	/**
	 *  Get the source resource of the link.
	 *
	 *@return                    The Source value
	 *@exception  ZenoException  Description of Exception
	 *@since
	 */

	public ZenoResource getSource() throws ZenoException {
		if (sourceResource == null)
			sourceResource = factory.loadResource(source);
		return sourceResource;
	}

	/**
	 *  Gets the SourceId attribute of the LinkImpl object
	 *
	 *@return    The SourceId value
	 *@since
	 */
	public int getSourceId() {
		return source;
	}

	/**
	 *  Get the target resource of the link.
	 *
	 *@return                    The Target value
	 *@exception  ZenoException  Description of Exception
	 *@since
	 */

	public ZenoResource getTarget() throws ZenoException {
		if (targetResource == null)
			targetResource = factory.loadResource(target);
		return targetResource;
	}

	/**
	 *  Gets the TargetId attribute of the LinkImpl object
	 *
	 *@return    The TargetId value
	 *@since
	 */
	public int getTargetId() {
		return target;
	}
	
	public String getFlag() {
		return flag;
	}
	
	public ZenoResource getSourceResource() {
		return sourceResource;
	}
	
	public void setSourceResource(ZenoResource resource) {
		this.sourceResource = resource;
	}
	
	public ZenoResource getTargetResource() {
		return targetResource;
	}
	
	public void setTargetResource(ZenoResource resource) {
		this.targetResource = resource ;
	}

	/**
	 *  Save changes. Raises an exception if the user has no permission or the
	 *  transaction could not be completed.
	 *
	 *@exception  ZenoException  Description of Exception
	 *@since
	 */

	public void save() throws ZenoException {
		try {
			factory.checkPermission("Link.save", this);
			StringBuffer buf = new StringBuffer();
			buf.append("update link set source_alias=");
			buf.append(DBClient.format(this.sourceAlias));
			buf.append(", target_alias=");
			buf.append(DBClient.format(this.targetAlias));
			buf.append(", flag=");
			buf.append(DBClient.format(this.flag));
			buf.append(" where source=");
			buf.append(DBClient.format(this.source));
			buf.append(" and target=");
			buf.append(DBClient.format(this.target));
			buf.append(" and label=");
			buf.append(DBClient.format(this.label));
			factory.dbclient.executeUpdate(buf.toString());	
			// record and report modification
			ResourceImpl r = (ResourceImpl) factory.loadResource(this.source);
			r.modified = true;
			r.save();
			// to do: commit transaction
		} catch (java.sql.SQLException e) {
			factory.reportError("Link.save", e);
			// to do: rollback
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
		return "[Link "
			+ label
			+ " "
			+ source
			+ " "
			+ sourceAlias
			+ "  "
			+ target
			+ "]";
	}

	/**
	 *  Gets the LinksWhere attribute of the LinkImpl class
	 *
	 *@param  factory            Description of Parameter
	 *@param  request            Description of Parameter
	 *@return                    The LinksWhere value
	 *@exception  ZenoException  Description of Exception
	 *@since
	 */
	public static List getLinksWhere(FactoryImpl factory, String request)
		throws ZenoException {
		try {
			ResultSet rs = factory.dbclient.executeQuery(request);
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
			factory.reportError("Link.getLinksWhere", e);
			// to do: rollback
			throw new ZenoException("DBException");
		}
	}

	/**
	 *  Description of the Method
	 *
	 *@param  factory  Description of Parameter
	 *@param  links    Description of Parameter
	 *@param  newids   Description of Parameter
	 *@since
	 */
	protected static void copyLinks(
		FactoryImpl factory,
		List links,
		Hashtable newids) {
		// only inner links are copied

		Iterator it = links.iterator();
		StringBuffer buf = new StringBuffer();
		while (it.hasNext()) {
			LinkImpl link = (LinkImpl) it.next();
			Integer oldSource = new Integer(link.source);
			Integer oldTarget = new Integer(link.target);
			Integer newSource = (Integer) newids.get(oldSource);
			Integer newTarget = (Integer) newids.get(oldTarget);
			//if (newSource != null &  newTarget != null) {
			// heg only inner links are copied at the moment
			//if (newTarget == null)
			//	newTarget = oldTarget;
			if (newSource != null) {
				if (newTarget == null) {
					newTarget = oldTarget;
				}
				buf.setLength(0);
				buf.append("insert into link ");
				buf.append("(source, source_alias, label, target, target_alias, flag)");
				buf.append("values(");
				buf.append(DBClient.format(newSource.intValue()));
				buf.append(", ");
				buf.append(DBClient.format(link.sourceAlias));
				buf.append(", ");
				buf.append(DBClient.format(link.label));
				buf.append(", ");
				buf.append(DBClient.format(newTarget.intValue()));
				buf.append(", ");
				buf.append(DBClient.format(link.targetAlias));
				buf.append(", ");
				buf.append(DBClient.format(link.flag));
				buf.append(")");
				try {
					factory.dbclient.executeQuery(buf.toString());
				} catch (java.sql.SQLException e) {
					factory.reportError("Link.copyLinks", e);
					// transfer as far as possible
				}
			}
		}

	}
	
	
	public static void fillLinks
				(FactoryImpl factory, List links, Hashtable resources, boolean remove) 
			throws ZenoException {
	
		List ids = new ArrayList();
		Iterator it = links.iterator();
		while(it.hasNext()) {
			LinkImpl link = (LinkImpl)it.next();
			Integer id = new Integer(link.getSourceId());
			if (resources.get(id) == null && !ids.contains(id))
				ids.add(id);
			id = new Integer(link.getTargetId());
			if (resources.get(id) == null && !ids.contains(id))
				ids.add(id);
		}
		List resList = factory.loadResources(ids);
		Iterator resit = resList.iterator();
		while(resit.hasNext()) {
			ResourceImpl resource = (ResourceImpl) resit.next();
			//if (factory.hasRole("reader", resource)) {
			if ( resource.isVisibleForReader()
					&&	factory.hasRole("reader", resource)) {
				Integer resid = new Integer(resource.getId());
				resources.put( resid, resource);
			}
		}
		Iterator it2 = links.iterator();
		while(it2.hasNext()) {
			LinkImpl link2 = (LinkImpl)it2.next();
			if (link2.getSourceResource() == null) {
				Integer source = new Integer(link2.getSourceId());
				link2.setSourceResource((ZenoResource)resources.get(source));
			}
			if (link2.getTargetResource() == null) {
				Integer target = new Integer(link2.getTargetId());
				link2.setTargetResource((ZenoResource)resources.get(target));
			}
			if (remove && (link2.getSourceResource() == null ||
			 		link2.getTargetResource() == null))
			 	it2.remove();
		} 
	}
	
	
}