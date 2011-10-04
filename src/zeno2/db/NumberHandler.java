package zeno2.db;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import zeno2.kernel.Journal;
import zeno2.kernel.Topic;
import zeno2.kernel.ZenoException;

public class NumberHandler {
	private static MonitorImpl monitor;
	
	protected static void configure(MonitorImpl monitor) {
		NumberHandler.monitor = monitor;
	}
	
	public static int getMaxNr(int jnid) {
		int maxnr = -1;
		StringBuffer buf = new StringBuffer();
		buf.append("select maxnr from journal");
		buf.append(" where journal.id =");
		buf.append(DBClient.format(jnid));
		try {
			ResultSet rs = monitor.dbclient.executeQuery(buf.toString());
			if (rs.next()) {
				maxnr = rs.getInt(1);
			}
		} catch(SQLException e) {
			monitor.reportError("NumberHandler.getMaxr " + e.toString());
		}
		return maxnr;
	}
	
	private static void setMaxNr(int jnid, int max) {
		StringBuffer buf = new StringBuffer();
		buf.append("update journal set maxnr =");
		buf.append(DBClient.format(max));
		buf.append(" where journal.id =");
		buf.append(DBClient.format(jnid));
		try {
			monitor.dbclient.executeUpdate(buf.toString());
		} catch(SQLException e) {
			monitor.reportError("NumberHandler.setMaxr " + e.toString());
		}
	}
	
	protected static void setMaxNr(int jnid) {
		int max = getRealMaxNr(jnid);
		if (max != -1)
			setMaxNr(jnid, max);
	}
	
		
	private static int getRealMaxNr(int jnid) {
		int maxnr = -1;
		StringBuffer buf = new StringBuffer();
		buf.append("select max(article.nr) from article, resource");
		buf.append(" where article.id= resource.id");
		buf.append(" and resource.parent =");
		buf.append(DBClient.format(jnid));
		try {
			ResultSet rs = monitor.dbclient.executeQuery(buf.toString());
			if (rs.next()) {
				maxnr = rs.getInt(1);
			}
		} catch(SQLException e) {
			monitor.reportError("NumberHandler.getRealMaxr " + e.toString());
		}
		return maxnr;
	}
		
	protected static void setNumber(int artid,  int nr) {
		
		StringBuffer buf = new StringBuffer();
		buf.append("update article set nr = ");
		buf.append(DBClient.format(nr));
		buf.append(" where id = ");
		buf.append(DBClient.format(artid));
		try {
			monitor.dbclient.executeUpdate(buf.toString());
		} catch(SQLException e) {
			monitor.reportError("NumberHandler.setNumber " + e.toString());
		}
	}
	
	
	private static void setNextNumber(int artid, int journalid) {
		int maxnr = getMaxNr(journalid) + 1;
		if (maxnr != 0) {
			setNumber(artid, maxnr);
			setMaxNr(journalid, maxnr);
		}
	}
	
	public static void setNextNumber(ArticleImpl article) {
		setNextNumber(article.id, article.parentId);
	}
	
	
	private static int setNumber(List artids, int maxnr) 
			throws SQLException {
	
		StringBuffer buf = new StringBuffer();
		buf.append("update article set nr = ? ");
		buf.append(" where id = ?");
		PreparedStatement pdstm;
		Connection con = monitor.dbclient.getConnection();
		pdstm = con.prepareStatement(buf.toString());
		Iterator artit = artids.iterator();
		while(artit.hasNext()) {
			maxnr++;
			Object cid = artit.next();
			if (cid instanceof String)
				cid = new Integer((String)cid);
			pdstm.clearParameters();
			pdstm.setInt(1, maxnr);
			pdstm.setInt(2, ((Integer)cid).intValue()); 
			pdstm.executeUpdate();	
		}
		return maxnr;
	}
	
	protected static void setUnsetNumber(int jnid) {
		
		StringBuffer buf = new StringBuffer();
		buf.append("select resource.id from resource, article");
		buf.append(" where  parent =");
		buf.append(DBClient.format(jnid));
		buf.append(" and resource.id=article.id ");
		buf.append(" and nr=0");
		buf.append(" order by resource.id");
		try {
			ResultSet rs = monitor.dbclient.executeQuery(buf.toString());
			List artids = new ArrayList();
			while(rs.next()) {
				artids.add(new Integer(rs.getInt("resource.id")));
			}
			int newmax = setNumber(artids, getMaxNr(jnid));
			setMaxNr(jnid, newmax);
		} catch(SQLException e) {
			monitor.reportError("NumberHandler.setUnsetNumber " + e.toString());
		}
	}
	
	protected static void setUnsetNumber(JournalImpl journal) {
		setUnsetNumber(journal.id);
	}
	
	protected static void setUnsetNumber(TopicImpl topic) {
		setUnsetNumber(topic.parentId);
	}
	
	
	private static void unsetNumber(List artids) {
		
		Iterator artit = artids.iterator();	
		StringBuffer buf = new StringBuffer();
		buf.append("update article set nr=0");
		buf.append(" where id in (");
		artids.iterator();
		while(artit.hasNext()) {
			buf.append(artit.next());
			if (artit.hasNext())
				buf.append(",");
		}
		buf.append(")");
		try {
			monitor.dbclient.executeUpdate(buf.toString());
		} catch(SQLException e) {
			monitor.reportError("NumberHandler.unsetNumber " + e.toString());
		}
	}
	
	protected static void unsetNumber(Topic topic) {
		
		int topicid = topic.getId();
		StringBuffer buf = new StringBuffer();
		buf.append("select id from resource");
		buf.append(" where  part =");
		buf.append(DBClient.format(topicid));
		buf.append(" order by id");
		List artids = new ArrayList();
		try {
			ResultSet rs = monitor.dbclient.executeQuery(buf.toString());
		while(rs.next()) {
				artids.add(new Integer(rs.getInt("resource.id")));
			}
		} catch(SQLException e) {
			monitor.reportError("NumberHandler.unsetNumber " + e.toString());
		}
		unsetNumber(artids);
		
	}
	
	protected static void renumber(Journal journal) {
		
		int jnid = journal.getId();	
		StringBuffer buf = new StringBuffer();
		buf.append("select id from resource");
			buf.append(" where  parent =");
			buf.append(DBClient.format(jnid));
			buf.append(" order by id");
		try {
			ResultSet rs = monitor.dbclient.executeQuery(buf.toString());
			List artids = new ArrayList();
			while(rs.next()) {
				artids.add(new Integer(rs.getInt("resource.id")));
			}
			int newmax = setNumber(artids, 0);
			setMaxNr(jnid, newmax);
		} catch(SQLException e) {
			monitor.reportError("NumberHandler.renumber " + e.toString());
		}
	}
	
	
	
}