package zeno2.db;

import java.util.Date;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Calendar;
import java.sql.ResultSet;
import java.sql.SQLException;

import zeno2.kernel.ZenoException;
import zeno2.kernel.Article;


public class DBNotify {
	protected DBClient dbClient = new DBClient();
	protected PrincipalFactory prfactory;
	protected String zenoadmin;
	
	public DBNotify() {
			super();
	}
		
	public DBNotify(MonitorImpl monitor) {
			
		this.dbClient = monitor.dbclient;
		this.prfactory = monitor.prfactory;
		this.zenoadmin = monitor.getProperty("zenoUserName", "NN");
	}
	
	public ResultSet executeQuery(String query) {
		try {
			return this.dbClient.executeQuery(query);
		} catch (SQLException sqle) {
			System.out.println("OOOPS Something is gone wrong in DBNotify.executeQuery()!");
			return null;
		}
	}
	
	protected void reportError(String operation, Throwable e) {
		System.out.print(DBClient.format(new Date()));
		System.out.print("  ");
		System.out.print(operation);
		System.out.print("  ");
		System.out.print(e);
	}
	
		
	public PrincipalFactory getPrincipalFactory(MonitorImpl monitor) {
		return monitor.prfactory;
	}
	
	
	public String filterJournals(String principal, String jids) {
			
		if (jids == null || "".equals(jids))
			return "";	
		if (principal.equals(this.zenoadmin))
			return jids;
		StringBuffer fjids = new StringBuffer();
		int nr = 0;	
		StringTokenizer tok = new StringTokenizer(jids, ",");
		while (tok.hasMoreTokens()) {
			String token = tok.nextToken();
			int jid = Integer.parseInt(token.trim());
			try {
				if (hasAccess(principal, jid)) {
					if (nr > 0)
						fjids.append(", ");
					fjids.append(token);
					nr++;
				}
			} catch(ZenoException e) {}
		}
		return fjids.toString();
	}
	
	protected List loadReader(int journalId)
			throws ZenoException {

		List pids = new ArrayList();
		StringBuffer buf = new StringBuffer(); 
		buf.append("select distinct principal from role ");
		buf.append(" where journal =");
		buf.append(DBClient.format(journalId));
		buf.append(" and (role_name='reader' or role_name='editor')");
		try {
			ResultSet rs = executeQuery(buf.toString());
			while (rs.next()) {
				pids.add(rs.getString("principal"));
			}
		} catch (java.sql.SQLException e) {
			reportError("DBNotify.loadReader", e);
			throw new ZenoException("DatabaseException");
		}
		return pids;
	}
	
	protected boolean hasAccess(String principal, int jid) 
			throws ZenoException {
		
		List pids = loadReader(jid);
		if (pids.contains("any"))
			//exclude guest???
			return true;	
		if (pids.contains(principal))
			return true;
		Iterator it = pids.iterator();
		while(it.hasNext()) {
			String cprincipal = (String)it.next();
			if (prfactory.isIndirectMember(cprincipal, principal))
				return true;
		}
		return false;
	}
	
	
	public String getDailyNotifyJournal(String principal) {
		try {
			ResultSet rs = this.dbClient.executeQuery("select value from principal_property where principal='"+principal+"' and name='zeno2.dailyNotify';");
			if(rs.next()) 
				return filterJournals(principal, rs.getString("value"));
			else
				return null;
		} catch (SQLException sqle) {
			System.out.println("OOOPS Something is gone wrong in DBNotify.getDailyNotifyJournal() with query:!");
			return null;
		}
	}
	
	public String getWeeklyNotifyJournal(String principal) {
		try {
			ResultSet rs = this.dbClient.executeQuery("select value from principal_property where principal='"+principal+"' and name='zeno2.weeklyNotify';");
			if(rs.next())
				return filterJournals(principal, rs.getString("value"));
			else
				return null;
		} catch (SQLException sqle) {
			System.out.println("OOOPS Something is gone wrong in DBNotify.getWeeklyNotifyJournal() with query:!");
			return null;
		}
	}
		
	public String getMonthlyNotifyJournal(String principal) {
		try {
			ResultSet rs = this.dbClient.executeQuery("select value from principal_property where principal='"+principal+"' and name='zeno2.monthlyNotify';");
			if(rs.next()) {
				return filterJournals(principal, rs.getString("value"));
			}
			else {
				return null;
			}
		} catch (SQLException sqle) {
			System.out.println("OOOPS Something is gone wrong in DBNotify.getMonthlyNotifyJournal() with query:!");
			return null;
		}
	}
	
	public String getEmail(String principal) {
		String query = new String("select email from principal where id='"+principal+"';");
		
		try {
			ResultSet rs = this.dbClient.executeQuery(query);
			if (rs.next()) {
				return rs.getString("email");
			} else {
				return null;
			}
		} catch (SQLException sqle) {
			System.out.println("OOOPS Something is gone wrong in DBNotify.getEmail() with query:!" + query);
			return null;
		}
	}
		
	public ResultSet loadNewArticles(Date date, String jids)
			throws ZenoException {
		ResultSet rs;
		if (jids == null || jids.equals("")) {
			return null;
		}
		
		if (date == null) {
			Calendar calendar = Calendar.getInstance();
			date = calendar.getTime();
		}
		
		StringBuffer buf = new StringBuffer();
		buf.append("select * from resource, article ");
		buf.append("where creation_date >= ");
		buf.append(DBClient.format(date));
		buf.append(" and marked_for_deletion='false'");
		buf.append(" and parent in (");
		buf.append(jids);
		buf.append(") ");

		buf.append(" and resource.id = article.id");
		buf.append(" order by parent, creation_date desc");
		
		try {
			rs = dbClient.executeQuery(buf.toString());
		} catch(SQLException e) {
			System.out.println("factory.loadNewArticles"+ e);
			throw new ZenoException("DataBaseException");
		}
		return rs;
	}
		
	public String getJournalTitle(String id) {
		StringBuffer buf = new StringBuffer();
		ResultSet rs;
		
		buf.append("select title from resource where id='" + id +"' and class='journal';");
		try {
				rs = dbClient.executeQuery(buf.toString());
				rs.next();
				return rs.getString("title");
		} catch (SQLException se) {
				System.out.println("Could not get Journal Title!" + se);
				return null;
		}
	}
	
	public String getMailAlias(String id) {
		StringBuffer buf = new StringBuffer();
		ResultSet rs;
		
		buf.append("select value from property where resource='" + id +"' and name='MailAlias';");
		try {
			rs = dbClient.executeQuery(buf.toString());
			if(rs.next())
				return rs.getString("value");
			else
				return null;
		} catch (SQLException se) {
			System.out.println("Could not get Mail Alias!" + se);
			return null;
		}
	}
		
	public ResultSet getResource(String resourceId) throws ZenoException {
		StringBuffer buf = new StringBuffer();
		ResultSet rs;

		//sleep for a while just to be shure that MYSQL has inserted the new
		//data into the table (not nice but is functioning
		
		try {
			java.lang.Thread.sleep(1000);
		} catch (java.lang.InterruptedException ie) {
			ie.printStackTrace();
		}
		
		buf.append("select title,note from resource where id='");
		buf.append(resourceId);
		buf.append("';");
		try {
			rs = dbClient.executeQuery(buf.toString());
			if(rs.next()) {
				rs.first();
				return rs;
			}
			else
				return null;
		} catch(SQLException e) {
			System.out.println("DBNotify.getResource"+ e);
			return null;
		}
	}
	
}
