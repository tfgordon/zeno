package zeno2.db;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import zeno2.kernel.XLink;
import zeno2.kernel.ZenoException;
import zeno2.kernel.ZenoResource;


public class XLinkImpl implements XLink {
	int source;
	String type;
	String reference;
	String name;
	FactoryImpl factory;


	public XLinkImpl(
		FactoryImpl factory,
		int source,
		String type,
		String reference,
		String name
		) 
	{
		this.factory = factory;
		this.source = source;
		this.type = type;
		this.reference = reference;
		this.name = name;
	}
	
	
	public int getSourceId() {
		return source;
	}
	
	public ZenoResource getSource() throws ZenoException {
		return factory.loadResource(source);
	}

	public String getType() {
		return type;
	}
	
	public String getReference() {
		return reference;
	}
	
	public String getName() {
		return name;
	}

	
	public void setName(String name) {
		this.name = name;
	}
	
	public void save() throws ZenoException {
		//only the name might be changed
		StringBuffer buf = new StringBuffer();
		buf.append("update xlink set name =");
		buf.append(DBClient.format(name));
		buf.append(" where source =");
		buf.append(DBClient.format(source));
		buf.append(" and type =");
		buf.append(DBClient.format(type));
		buf.append(" and reference =");
		buf.append(DBClient.format(reference));
		try {
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
	

	public String toString() {
		return "[XLink "
			+ source
			+ " "
			+ type
			+ "  "
			+ reference
			+ "  "
			+ name
			+ "]";
	}
	
	protected static List getXLinksWhere(FactoryImpl factory, String request) 
			throws ZenoException {
			
		try {
			ResultSet rs = factory.dbclient.executeQuery(request);
			List result = new ArrayList();
			while (rs.next()) {
				XLinkImpl xlink =
					new XLinkImpl(
						factory,
						rs.getInt("source"),
						rs.getString("type"),
						rs.getString("reference"),
						rs.getString("name"));
				result.add(xlink);
			}
			return result;
		} catch (java.sql.SQLException e) {
			factory.reportError("ZenoResource.getXLinksWhere", e);
			throw new ZenoException("DataBaseException");
		}
	}
	
	
	protected static void copyXLinks(
		FactoryImpl factory,
		List links,
		Hashtable newids) {
		// only inner links are copied

		Iterator it = links.iterator();
		StringBuffer buf = new StringBuffer();
		while (it.hasNext()) {
			XLinkImpl xlink = (XLinkImpl) it.next();
			Integer oldSource = new Integer(xlink.source);
			Integer newSource = (Integer) newids.get(oldSource);
			if (newSource != null) {
				buf.setLength(0);
				buf.append("insert into xlink ");
				buf.append("(source, type, reference, name)");
				buf.append("values(");
				buf.append(DBClient.format(newSource.intValue()));
				buf.append(", ");
				buf.append(DBClient.format(xlink.type));
				buf.append(", ");
				buf.append(DBClient.format(xlink.reference));
				buf.append(", ");
				buf.append(DBClient.format(xlink.name));
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
	
	
}
