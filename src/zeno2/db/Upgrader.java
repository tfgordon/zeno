package zeno2.db;

import java.io.PrintWriter;
import java.util.*;
import java.sql.*;
import zeno2.kernel.*;
import zeno2.db.*;


public class Upgrader {
	MonitorImpl monitor;
	DBClient dbclient;
	PrintWriter out;
	
	public Upgrader(String configFile, PrintWriter prw) 
			throws ZenoException {
		
		monitor = new MonitorImpl();
		out = prw != null ? prw : new PrintWriter(System.out, true);;
		monitor.setErrorWriter(out);
		monitor.configure(configFile);
		
	}
	
	
	protected void reportError(String where, Throwable e) {
		out.println("Error at " + where + ": " + e.getMessage());
	}
	
	//----------------- target test -------------
	
	public void testConnection() throws SQLException {
		
		DBClient dbclient = monitor.getDBClient();	
		String request;
		
		try { 
			
			request = "select count(*) from principal";
			ResultSet rs = dbclient.executeQuery(request);
			rs.next();
			out.println(rs.getInt(1) + " zeno principals");	
		
		} catch(SQLException e) {
			reportError("testConnection", e);
		}	
	}
	
	//---------------- target: links -----------	
	
	public void upgradeLinkTable() throws SQLException {
	
		DBClient dbclient = monitor.getDBClient();	
		String request;
		
		try {
			request = "alter table link add  column source_mark" +
						 " enum ('true', 'false') not null default 'false'";
			dbclient.executeUpdate(request);	
		} catch(SQLException e) {
			reportError("upgradeLinkTable", e);
		}		
					 
		try {
			request = "alter table link add  column target_mark" +
						 " enum ('true', 'false') not null default 'false'";
			dbclient.executeUpdate(request);
			out.println("link table upgraded");
		} catch(SQLException e) {
			reportError("upgradeLinkTable", e);
		}		
	}
	
	public void upgradeLinks(String type)
			throws SQLException {
			
		DBClient dbclient = monitor.getDBClient();	
		StringBuffer buf = new StringBuffer();
		String request = "";
		String whereClause = "";
		if (type.equals("source")) {
			request = "update link set source_mark='true' where source=?";
			whereClause = " where marked_for_deletion='true' and id=source";
		} else if (type.equals("target")) {
			request = "update link set target_mark='true' where target=?";
			whereClause = " where marked_for_deletion='true' and id=target";
		}
		
		PreparedStatement pdstm;
		Connection con = dbclient.getConnection();
		pdstm = con.prepareStatement(request);
		
		buf.append("select distinct id from resource, link");
		buf.append(whereClause);
		ResultSet rs = dbclient.executeQuery(buf.toString());
		while(rs.next()) {
			int cid = rs.getInt("id");
			pdstm.clearParameters();
			pdstm.setInt(1, cid);
			pdstm.executeUpdate();	
		}
		out.println("links marked with deleted " + type);
	}
	
	//---------------- target: topics -----------	
	
	public void upgradeTopicTables() throws SQLException {
	
		DBClient dbclient = monitor.getDBClient();	
		String request;
		
		try {
			request = "alter table resource add column part int not null default 0";
			dbclient.executeUpdate(request);
			out.println("resource table upgraded");
		} catch(SQLException e) {
			reportError("upgradeTopicTables", e);
		}
		
		try {
			request = "alter table journal add column attachment_size_limit int default -1";
			dbclient.executeUpdate(request);
		} catch(SQLException e) {
			reportError("upgradeTopicTables", e);
		}	
		
		try {
			request = "alter table journal add column topic_mode" +
						 " enum ('true', 'false') not null default 'true'"; 
			dbclient.executeUpdate(request);
			out.println("journal table upgraded");
		} catch(SQLException e) {
			reportError("upgradeTopicTables", e);
		}		
	}
	
	public void upgradeTopics(int parentId)
		throws SQLException {
			
		DBClient dbclient = monitor.getDBClient();	
		
		String request = 
			"update resource set class='topic', creation_date=?, part=? where id=?";
		PreparedStatement pdstm;
		Connection con = dbclient.getConnection();
		pdstm = con.prepareStatement(request);
		
		StringBuffer buf = new StringBuffer();
		buf.append("select resource.id, resource.creation_date from resource, article");
		buf.append(" where resource.id = article.id and is_topic ='true'");
		if (parentId != -1) {
			buf.append(" and parent = ");
			buf.append(DBClient.format(parentId));
		}
		ResultSet rs = dbclient.executeQuery(buf.toString());
		while(rs.next()) {
			int cid = rs.getInt("id");
			java.sql.Timestamp  date = rs.getTimestamp("creation_date");
			pdstm.clearParameters();
			pdstm.setTimestamp(1, date);
			pdstm.setInt(2, cid);
			pdstm.setInt(3, cid);
			pdstm.executeUpdate();
		}
	}
	
	public void upgradeTopics(int parentId, boolean recursively)
		throws SQLException {
		
		upgradeTopics(parentId);
		if (recursively) {
			DBClient dbclient = monitor.getDBClient();
			StringBuffer buf = new StringBuffer();
			buf.append("select id from resource where class='journal' and parent=");
			buf.append(DBClient.format(parentId));
			ResultSet rs = dbclient.executeQuery(buf.toString());
			while(rs.next()) {
				int cid = rs.getInt("id");
				upgradeTopics(cid, recursively);
			}
		}
	}
		
	//---------------- target: communities -----------
	
	public void upgradeAdrbookTables() throws SQLException {
	
		DBClient dbclient = monitor.getDBClient();	
		String request;
		
		try {
			request = "alter table principal add column " +
							" (creator varchar(16) references principal (id), " +
 							" creation_date timestamp not null) ";
 			dbclient.executeUpdate(request);				
 		} catch(SQLException e) {
			reportError("upgradeAdrbookTables", e);
		}
		
		try {
		
			request = "alter table principal_property add column " +
						" community varchar(16) not null default 'system' after principal," +
						" drop primary key, " +
						" add primary key (principal,community,name(255))";
		
			dbclient.executeUpdate(request);
		} catch(SQLException e) {
			reportError("upgradeAdrbookTables", e);
		}				
		
		try {
			request = "create table community_member (" + 
 						" community varchar(16) not null references principal(id)," +
						" member varchar(16) not null references principal(id)," +
 						" is_admin enum ('true', 'false') not null default 'false'," +
 						" PRIMARY KEY (community,member) " +
						" );";
						
			dbclient.executeUpdate(request);
			out.println("community_member table added");
		} catch(SQLException e) {
			reportError("upgradeAdrbookTables", e);
		}
	}
	
	public void createCommunity(String cid, String name) 
			throws SQLException {
		
		DBClient dbclient = monitor.getDBClient();
		String zenoadmin = monitor.getProperty("zenoUserName", "nn");
		StringBuffer buf = new StringBuffer();
		buf.append("insert into principal ");
		buf.append("(id, class, common_name, creator, creation_date) ");
		buf.append(" values(");
		buf.append(DBClient.format(cid));
		buf.append(", 'community', ");
		buf.append(DBClient.format(name));
		buf.append(", ");
		buf.append(DBClient.format(zenoadmin));
		buf.append(", ");
		buf.append(DBClient.format(new java.util.Date()));
		buf.append(")");
		try {
			dbclient.executeUpdate(buf.toString());
			out.println("community " + cid + " created");
		} catch (java.sql.SQLException e) {
			int index = e.getMessage().indexOf("Duplicate entry");
			if (index == -1)
				reportError("createCommunity", e);
		}
		
		buf.setLength(0);
		buf.append("insert into community_member (community, member, is_admin) ");
		buf.append(" values(");
		buf.append(DBClient.format(cid));
		buf.append(", ");
		buf.append(DBClient.format(zenoadmin));
		buf.append(", 'true')");
		try {	
			dbclient.executeUpdate(buf.toString());
		} catch (java.sql.SQLException e) {
			int index = e.getMessage().indexOf("Duplicate entry");
			if (index == -1)
				reportError("createCommunity", e);
		}	
	}
	
	protected void addPrincipalsTo(String community, ResultSet rs)
			throws SQLException {
	
		DBClient dbclient = monitor.getDBClient();	
		String request = "insert into community_member (community, member) " +
								" values(?, ?)";
		PreparedStatement pdstm;
		Connection con = dbclient.getConnection();
		pdstm = con.prepareStatement(request);
		
		while(rs.next()) {
			String cid = rs.getString("id");
			pdstm.clearParameters();
			pdstm.setString(1, community);
			pdstm.setString(2, cid);
			try {
				pdstm.executeUpdate();
			} catch(SQLException e) {
				reportError("addPrincipalsTo", e);
			}	
		}
		out.println("principals added to " + community);
	}
	
	protected void addFreePrincipalsTo(String community, ResultSet rs)
			throws SQLException {
	
		DBClient dbclient = monitor.getDBClient();	
		String request = "insert into community_member (community, member) " +
								" values(?, ?)";
		PreparedStatement pdstm;
		Connection con = dbclient.getConnection();
		pdstm = con.prepareStatement(request);
		
		while(rs.next()) {
			String cid = rs.getString("id");
			String ccom = rs.getString("community");
			if (ccom == null) {
				pdstm.clearParameters();
				pdstm.setString(1, community);
				pdstm.setString(2, cid);				
				try {
					pdstm.executeUpdate();
				} catch(SQLException e) {
					int index = e.getMessage().indexOf("Duplicate entry");
					if (index == -1)
						reportError("addFreePrincipalsTo", e);
				}
			}
		}
		out.println("principals without community added to " + community);
	}
	
	public void addPrincipalsTo(String community, String group, String type)
			throws SQLException {
			
		DBClient dbclient = monitor.getDBClient();	
		StringBuffer buf = new StringBuffer();
		buf.append("select id from principal, group_member");
		buf.append(" where class = ");
		buf.append(DBClient.format(type));
		buf.append(" and group_member.zgroup=");
		buf.append(DBClient.format(group));
		buf.append(" and group_member.member = id");	
		ResultSet rs = dbclient.executeQuery(buf.toString());	
		addPrincipalsTo(community, rs);
	}
	
	public void addFreePrincipalsTo(String community, String type)
			throws SQLException {
			
		DBClient dbclient = monitor.getDBClient();	
		StringBuffer buf = new StringBuffer();
		buf.append("select id, community from principal");
		buf.append(" left join community_member on id = member");
		if (! type.equals("")) {
			buf.append(" where class = ");
			buf.append(DBClient.format(type));
		}
		ResultSet rs = dbclient.executeQuery(buf.toString());	
		addFreePrincipalsTo(community, rs);
	}
	
	/** transforms former person any into a community */
	
	public void adaptAny() throws SQLException {
		
		DBClient dbclient = monitor.getDBClient();	
		String request = "update principal set class='community', password =''" +
								" where id ='any'";
		try {	
			dbclient.executeUpdate(request);
		} catch (java.sql.SQLException e) {
			reportError("adapAny", e);
		}	
	}
	
	public void adaptGuest() throws SQLException {
		
		DBClient dbclient = monitor.getDBClient();	
		String request = "update principal set class='collective', password =''" +
								" where id ='guest'";
		try {	
			dbclient.executeUpdate(request);
		} catch (java.sql.SQLException e) {
			reportError("adaptGuest", e);
		}	
	}
	
	public void addGuestTo(String community) 	throws SQLException {				
		
		DBClient dbclient = monitor.getDBClient();	
		String request = "insert into community_member (community, member) " +
								" values(" + DBClient.format(community) +
								" , 'guest')";
		try {	
			dbclient.executeUpdate(request);
		} catch (java.sql.SQLException e) {
			reportError("addGuestTo", e);
		}	
	}
	
	/**  transforms former basic properties into properties specific for 
	the specified community */
	
	public void adaptProperties(String community)
			throws SQLException { 
			
		DBClient dbclient = monitor.getDBClient();
		StringBuffer buf = new StringBuffer();
		buf.append("select principal.* from principal, community_member");
		buf.append(" where class = 'person'");
		buf.append(" and id = member and community=");
		buf.append(DBClient.format(community));
		ResultSet rs = dbclient.executeQuery(buf.toString());
	
		String request = "insert into principal_property " +
								" (principal, community, name, value) " +
								" values(?, ?, ?, ?)";
		PreparedStatement pdstm;
		Connection con = dbclient.getConnection();
		pdstm = con.prepareStatement(request);
		
		while(rs.next()) {
			String principal = rs.getString("id");
			String description = rs.getString("description");
			String orgRole = rs.getString("org_role");
			String organization = rs.getString("organization");
			insertProperty(pdstm, principal, community, "description", description);
			insertProperty(pdstm, principal, community, "orgRole", orgRole);
			insertProperty(pdstm, principal, community, "organization", organization);
		}
		out.println("basic properties moved to " + community);
	}
		
	
	private void insertProperty(PreparedStatement pdstm, String principal,
											String community, String name, String value) {
		if (value != null && !"".equals(value)) {
			try {
				pdstm.clearParameters();
				pdstm.setString(1, principal);
				pdstm.setString(2, community);
				pdstm.setString(3, name);
				pdstm.setString(4, value);
				pdstm.executeUpdate();
			} catch(SQLException e) {
				int index = e.getMessage().indexOf("Duplicate entry");
				if (index == -1)
					reportError("insertProperty", e);
			}
		}
	}
	
	//------------------- target plugin -----------------------------
	
	public void createPluginTable() {
	
		DBClient dbclient = monitor.getDBClient();	
		String request =
				"create table plugin (" +
 					" id varchar(16) not null primary key," +
 					" journalMenu varchar(16)," +
					" journalIcon varchar(64)," +
 					" journalURL varchar(16)," +
 					" journalParams tinytext," +
 					" articleMenu varchar(16)," +
 					" articleIcon varchar(64)," +
 					" articleURL varchar(16)," +
 					" articleParams tinytext," +
 					" title tinytext," +
 					" addOn text" +
					 " );";
		try {
			dbclient.executeUpdate(request); 
			out.println("plugin table added");
		} catch (java.sql.SQLException e) {
			reportError("createPluginTable", e);
		}
	}
		
		
	//------------------- target numbering --------------------------
	
	public void upgradeTablesForNumbering() {
	
		DBClient dbclient = monitor.getDBClient();	
		String request;

		request = "alter table journal add maxnr int not null default 0";
		try {
			dbclient.executeUpdate(request);
		} catch (java.sql.SQLException e) {
			reportError("upgradeTablesForNumbering", e);
		}
		
		request = "alter table article add dummy_date timestamp after keywords";
		try {
			dbclient.executeUpdate(request);
		} catch (java.sql.SQLException e) {
			reportError("upgradeTablesForNumbering", e);
		}		
		
		request = "alter table article add nr int not null default 0";
		try {
			dbclient.executeUpdate(request);
		} catch (java.sql.SQLException e) {
			reportError("upgradeTablesForNumbering", e);
		}
	}
	
		
	public void adaptArticles() {
		
		DBClient dbclient = monitor.getDBClient();
		String request = "select id from resource where class='journal'";
		try {
			ResultSet rs = dbclient.executeQuery(request);
			while(rs.next()) {
				int jnid = rs.getInt("id");
				NumberHandler.setUnsetNumber(jnid);
			}
		} catch(SQLException e) {
			reportError("adaptArticles", e);
		}
	} 
	
	//------------------- target houeskeeping --------------------------
	
	public void createHousekeepingTable() {
	
		DBClient dbclient = monitor.getDBClient();	
		String request =
				"create table housekeeping ("
					+ "id int not null references journal (id),"
					+ "exec_date datetime not null,"
					+ "operation char(1) not null,"
					+  "PRIMARY KEY (id,operation)"
					+ ")";
		try {
			dbclient.executeUpdate(request); 
			out.println("plugin table added");
		} catch (java.sql.SQLException e) {
			reportError("createHousekeepingTable", e);
		}
	}
	
	//----------------------------------------------------------------
	
	public void upgrade(String target, String password)
				throws ZenoException {
		
		DBClient dbclient = monitor.getDBClient();
		String zenoadmin = monitor.getProperty("zenoUserName", "nn");
		
		try {
			StringBuffer buf = new StringBuffer();
			buf.append("select * from principal where id =");
			buf.append(DBClient.format(zenoadmin));
			buf.append(" and password=");
			buf.append(DBClient.format(password));
			ResultSet rs = dbclient.executeQuery(buf.toString());
			if (!rs.next())
				throw new NoPermissionException("InvalidAuthentication");
		} catch (java.sql.SQLException e) {
			reportError("checkPassword", e);
			throw new ZenoException("DBException");
		}
		
		upgrade(target);
		out.println("upgrade finished for " + target);
		
	}
	
	void upgrade(String target) {
		
		if (target.equals("test")) {
	  		
	  		try {
	  			testConnection();
	  		} catch(SQLException e) {
	  			reportError("upgrade test", e);
	  		}
	  	
	  	} else if (target.equals("links")) {
	  		
	  		try {
	  			upgradeLinkTable();
	  			upgradeLinks("source");
	  			upgradeLinks("target");
	  		} catch(SQLException e) {
	  			reportError("upgrade links", e);
	  		}	
	  	
	  	} else if (target.equals("topics")) {
	  		
	  		try {
	  			upgradeTopicTables();
	 			upgradeTopics(-1);
	 		} catch(SQLException e) {
	 			reportError("upgrade topics", e);
	  		}		
  	  		
	  	} else if (target.equals("communities")) {
	  		
	  		try{
	  			upgradeAdrbookTables();
	  		
	  			createCommunity("system", "System Community");
	  			createCommunity("common", "Common Community");
	  			createCommunity("extern", "External Community");
	  		
	  			adaptAny();
	  			adaptGuest();
	  			addGuestTo("extern");
	  		
	  			addFreePrincipalsTo("common", "person");
	  			addFreePrincipalsTo("common", "group");
	  		
	  			adaptProperties("common");
	  		
	  		} catch(SQLException e) {
	  			reportError("upgrade communities", e);
	  		}
	  		
	  	} else if (target.equals("plugin")) {
	  		
	   			createPluginTable();
	  		
	  	} else if (target.equals("numbering")) {
	  		
	   			upgradeTablesForNumbering();
	  			adaptArticles();
	  			
	  	} else if (target.equals("housekeeping")) {
	  	
	  		createHousekeepingTable();	
	    
	  	} else if (target.equals("all")) {
	  	
	  		upgrade("links");
	  		upgrade("topics");
	  		upgrade("communities");
	  		upgrade("plugin");
	  		upgrade("numbering");
	  		upgrade("housekeeping");
	  		
	   	} else
	  	
	  		out.println("unknown target " + target);
	}
	
	public static void main(String[] args) throws ZenoException, SQLException {
	
		if (args.length != 3) {
		 System.err.println("Usage: java Example1 configFile target password");
		 System.exit(-1);
	  	}

	  	String configFile = args[0];
	  	String target = args[1];
	  	String password = args[2];
	  	PrintWriter prw = new PrintWriter(System.out, true);
	  	
	  	Upgrader upgrader = new Upgrader(configFile, prw);
	  	
	  	upgrader.upgrade(target, password);
	}
	
	
	
	
}
