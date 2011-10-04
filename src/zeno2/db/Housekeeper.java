package zeno2.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import zeno2.kernel.Monitor;

public class Housekeeper extends Thread {
	MonitorImpl monitor;
	DBClient dbclient;
	String operation;
	// milliseconds
	long period;
	boolean keepRunning = true;

	public Housekeeper(Monitor monitor, String operation, long period) {
		this.monitor = (MonitorImpl)monitor;
		this.dbclient = this.monitor.getDBClient();
		this.operation = operation;
		this.period = period;
	}
	
		
	public List getExpiredArticles(int jnid) {
		
		List ids = new ArrayList();
		Date now = new Date();
		StringBuffer buf = new StringBuffer();
		buf.append("select resource.id, parent, title, expiration_date, nr from resource, article ");
		buf.append(" where resource.id=article.id and parent=");
		buf.append(DBClient.format(jnid));
		buf.append(" and expiration_date > ");
		buf.append("00000000000000");
		buf.append(" and expiration_date <");
		buf.append(DBClient.format(now));
		buf.append(" order by parent, id");
		try {
			ResultSet rs = monitor.dbclient.executeQuery(buf.toString());
			while(rs.next()) {
				int id = rs.getInt("resource.id");
				ids.add(new Integer(id));
				System.out.print(id);
				System.out.print("   ");
				System.out.print(rs.getString("title"));
				System.out.print("   ");
				System.out.print(rs.getTimestamp("expiration_date"));
				System.out.print("   ");
				System.out.println(rs.getInt("nr"));
			}
		} catch(SQLException e) {
			monitor.reportError(e.getMessage());
		}
		return ids;
	}
	
	public List getExpiredArticles() {
		
		List ids = new ArrayList();
		Date now = new Date();
		StringBuffer buf = new StringBuffer();
		buf.append("select id, expiration_date, nr from article ");
		buf.append(" where expiration_date > ");
		buf.append("00000000000000");
		buf.append(" and expiration_date <");
		buf.append(DBClient.format(now));
		buf.append(" order by id");
		try {
			ResultSet rs = monitor.dbclient.executeQuery(buf.toString());
			while(rs.next()) {
				int id = rs.getInt("id");
				ids.add(new Integer(id));
				System.out.print(id);
				System.out.print("   ");
				System.out.print(rs.getTimestamp("expiration_date"));
				System.out.print("   ");
				System.out.println(rs.getInt("nr"));
			}
		} catch(SQLException e) {
			monitor.reportError("Housekeeper.getExpiredArticles", e);
		}
		return ids;
	}
	
	public void unpublishArticles(List ids) {
		if (ids.isEmpty()) return;
		StringBuffer buf = new StringBuffer();
		buf.append("update article set published='false'");
		buf.append(", expiration_date=00000000000000");
		buf.append(" where id in (");
		Iterator it = ids.iterator();
		while(it.hasNext()) {
			buf.append(it.next());
			if (it.hasNext()) 
				buf.append(",");
		}
		buf.append(")");
		try {
			dbclient.executeUpdate(buf.toString());
			markModified(ids);
		} catch(SQLException e) {
			monitor.reportError("Housekeeper.unpublishArticles", e); 
		}
	}
	
	public void markModified(List ids) {
		if (ids.isEmpty()) return;
		try {
			StringBuffer buf = new StringBuffer();
			buf.append("select id, creation_date from resource");
			buf.append(" where id in (");
			Iterator it = ids.iterator();
			while(it.hasNext()) {
				buf.append(it.next());
				if (it.hasNext()) 
					buf.append(",");
			}
			buf.append(")");
			ResultSet rs = dbclient.executeQuery(buf.toString());
			
			String zenoadmin = monitor.getProperty("zenoUserName", "nn");
			Date now = new Date();
			buf.setLength(0);
			buf.append("update resource set creation_date = ?");
			buf.append(", modifier=");
			buf.append(DBClient.format(zenoadmin));
			buf.append(", modification_date=");
			buf.append(DBClient.format(now));
			buf.append(" where id = ?");
			PreparedStatement pdstm;
			Connection con = dbclient.getConnection();
			pdstm = con.prepareStatement(buf.toString());
			while(rs.next()) {
				try {
					int id = rs.getInt("id");
					Timestamp  date = rs.getTimestamp("creation_date");
					pdstm.clearParameters();
					pdstm.setTimestamp(1, date);
					pdstm.setInt(2, id);
					pdstm.executeUpdate();
				} catch(SQLException e) {
					monitor.reportError("Housekeeper.markModified", e); 
				}
			}
		} catch(SQLException e) {
			monitor.reportError("Housekeeper.markModified", e); 
		}
	}
	
	public List getArticlesToClose() {
		
		List ids = new ArrayList();
		Date now = new Date();
		StringBuffer buf = new StringBuffer();
		buf.append("select id from housekeeping ");
		buf.append(" where operation='c'");
		buf.append(" and exec_date <");
		buf.append(DBClient.format(now));
		try {
			ResultSet rs = monitor.dbclient.executeQuery(buf.toString());
			while(rs.next()) {
				int id = rs.getInt("id");
				ids.add(new Integer(id));
			}
		} catch(SQLException e) {
			monitor.reportError("Housekeeper.getArticlesToClose", e); 
		}
		return ids;
	}
		
	
	public void closeArticles(List ids) {
		if (ids.isEmpty()) return;
		try {
			StringBuffer buf = new StringBuffer();
			buf.append("select id, creation_date from resource");
			buf.append(" where id in (");
			Iterator it = ids.iterator();
			while(it.hasNext()) {
				buf.append(it.next());
				if (it.hasNext()) 
					buf.append(",");
			}
			buf.append(")");
			ResultSet rs = dbclient.executeQuery(buf.toString());
			
			String zenoadmin = monitor.getProperty("zenoUserName", "nn");
			Date now = new Date();
			buf.setLength(0);
			buf.append("update resource set locked='true'");
			buf.append(", creation_date = ?");
			buf.append(", modifier=");
			buf.append(DBClient.format(zenoadmin));
			buf.append(", modification_date=");
			buf.append(DBClient.format(now));
			buf.append(" where id = ?");
			PreparedStatement pdstm;
			Connection con = dbclient.getConnection();
			pdstm = con.prepareStatement(buf.toString());
			while(rs.next()) {
				try {
					int id = rs.getInt("id");
					Timestamp  date = rs.getTimestamp("creation_date");
					pdstm.clearParameters();
					pdstm.setTimestamp(1, date);
					pdstm.setInt(2, id);
					pdstm.executeUpdate();
				} catch(SQLException e) {
					monitor.reportError("Housekeeper.closeArticles", e); 
				}
			}
		} catch(SQLException e) {
			monitor.reportError("Housekeeper.closeArticles", e); 
		}
	}
	
	public void removeClosedEntries(List ids) {
	 	if (ids.isEmpty()) return;
		StringBuffer buf = new StringBuffer();
		buf.append("delete from housekeeping ");
		buf.append(" where operation='c'");
		buf.append(" and id in (");
		Iterator it = ids.iterator();
		while(it.hasNext()) {
			buf.append(it.next());
			if (it.hasNext()) 
				buf.append(",");
		}
		buf.append(")");
		try {
			monitor.dbclient.executeUpdate(buf.toString());
		} catch(SQLException e) {
			monitor.reportError("Housekeeper.removeClosedEntries", e); 
		}
	}
	
	
	public void run() {
		
		Date start = null;
		Date end = null;
		while(keepRunning) {
			start = new Date();
			//monitor.reportError("starting operation " + operation + " at " + start);
			if ("close".equals(operation)) {
				List ids = getArticlesToClose();
				closeArticles(ids);
				removeClosedEntries(ids);
			} else if ("hide".equals(operation)) {
				List ids = getExpiredArticles();
				unpublishArticles(ids);
				markModified(ids);
			}	else {
				monitor.reportError("Houskeeper.run: unknown operation " + operation);
			}
			end = new Date();
			long duration = end.getTime() - start.getTime();
			try {
				sleep(period - duration);
			} catch(InterruptedException e) {
				monitor.reportError("Housekeeper.run", e);
			}
		}
	}
	
	
	
}