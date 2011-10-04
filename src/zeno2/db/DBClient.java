package zeno2.db;

import java.sql.Connection;
import java.sql.Date;
import java.sql.Timestamp;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.util.Hashtable;
import java.util.Vector;

import zeno2.kernel.ZenoException;

public class DBClient {
	String host;
	String db;
	String user;
	String password;
	String url;
	Connection con;
	MonitorImpl monitor;

	public DBClient(
		MonitorImpl monitor,
		String host,
		String db,
		String user,
		String password)
		throws ZenoException {

		// to be moved to the monitor
		try {
			java.lang.Class.forName("org.gjt.mm.mysql.Driver").newInstance();

			this.monitor = monitor;
			this.host = host;
			this.db = db;
			this.user = user;
			this.password = password;
			this.url =
				"jdbc:mysql://" + host + "/" + db + "?user=" + user + "&password=" + password;
		} catch (Exception e) {
			reportError("DBClient.creator", e);
			throw new ZenoException("DBException");

		}
	}

	public DBClient() {
		super();
	}

	public void reportError(String operation, Throwable e) {
		monitor.reportError(operation + "  " + e);
	}

	protected Connection getConnection() throws java.sql.SQLException {
		if (con == null) {
			con = DriverManager.getConnection(url);
		}
		return con;
	}

	private static String addEscapes(String line) {
		int index = line.indexOf("'");
		if (index == -1)
			return line;
		else {
			String newLine =
				line.substring(0, index) + "\\'" + addEscapes(line.substring(index + 1));
			return newLine;
		}
	}

	public static String format(String str) {
		if (str == null)
			return "''";
		else
			return "'" + addEscapes(str) + "'";
	}

	public static String format(int nr) {
		return Integer.toString(nr);
	}

	public static String format(java.util.Date date) {
		if (date == null)
			return "''";
		java.sql.Date sqldate = new java.sql.Date(date.getTime());
		java.sql.Time sqltime = new java.sql.Time(date.getTime());
		return "'" + sqldate.toString() + " " + sqltime.toString() + "'";
	}

	public static String format(boolean value) {
		if (value)
			return "'true'";
		else
			return "'false'";
	}

	public static String quote(String str) {
		return "'" + str + "'";
	}

	public static String quote(String str1, String str2, String str3) {
		return "'" + str1 + "', '" + str2 + "', '" + str3 + "'";
	}

	public static String genSetClause(Hashtable values, Vector keys) {
		StringBuffer buf = new StringBuffer();
		buf.append("set ");
		String sep = "";
		for (int i = 0; i < keys.size(); i++) {
			String key = (String) keys.elementAt(i);
			Object value = values.get(key);
			if (i > 0)
				sep = " ,";
			if (value instanceof String) {
				buf.append(sep);
				buf.append(key);
				buf.append(" = ");
				buf.append(quote((String) value));
			}
		}
		return buf.toString();
	}

	/*

	protected ResultSet executeQuery(String query) 
	    throws java.sql.SQLException 
	{
	    Connection con = getConnection();
	    Statement stm = con.createStatement();
	    ResultSet rs = stm.executeQuery(query);
	    return rs;
	}
	
	protected int executeUpdate(String request)
	    throws java.sql.SQLException
	{
	    Connection con = getConnection();
	    Statement stm = con.createStatement();
	    return stm.executeUpdate(request);
	}
	*/


	protected ResultSet executeQuery(String query) throws java.sql.SQLException {
		int cnr = 0;
		while (true) {
			try {
				Connection con = getConnection();
				Statement stm = con.createStatement();
				ResultSet rs = stm.executeQuery(query);
				return rs;
			} catch (java.sql.SQLException e) {
				if (e.getMessage().startsWith("Lost connection")
					| e.getMessage().startsWith("Communication link failure")) {
					if (cnr >= 5)
						throw e;
					else {
						con = null;
						System.out.println("cnr: " + cnr);
						cnr++;
					}
				} else
					throw e;
			}

		}
	}

	protected int executeUpdate(String request) throws java.sql.SQLException {
		int cnr = 0;
		while (true) {
			try {
				Connection con = getConnection();
				Statement stm = con.createStatement();
				int result = stm.executeUpdate(request);
				return result;
			} catch (java.sql.SQLException e) {
				if (e.getMessage().startsWith("Lost connection")
					| e.getMessage().startsWith("Communication link failure")) {
					if (cnr >= 5)
						throw e;
					else {
						con = null;
						System.out.println("cnr: " + cnr);
						cnr++;
					}
				} else
					throw e;
			}

		}
	}

	protected Object[] getRowArray(String request) {
		try {
			ResultSet rs = executeQuery(request);
			Object[] result = null;
			if (rs.next()) {
				ResultSetMetaData rsmd = rs.getMetaData();
				int colCount = rsmd.getColumnCount();
				result = new Object[colCount];
				for (int i = 0; i < colCount; i++) {
					//sic
					result[i] = rs.getObject(i + 1);
				}
			}
			return result;
		} catch (SQLException e) {
			reportError("DBClient.getRowArray", e);
			return null;
		}
	}

	protected Hashtable getRow(String request) {
		try {
			ResultSet rs = executeQuery(request);
			Hashtable result = null;
			if (rs.next()) {
				result = new Hashtable();
				Object value = null;
				ResultSetMetaData rsmd = rs.getMetaData();
				int colCount = rsmd.getColumnCount();
				for (int i = 1; i < colCount; i++) {
					value = rs.getObject(i);
					if (value != null)
						result.put(rsmd.getColumnLabel(i), value);
				}
			}
			return result;
		} catch (SQLException e) {
			reportError("DBClient.getRow", e);
			return null;
		}
	}

	protected int count(String table, String where) {
		int result = -1;
		try {
			String request = "select count(*) from " + table + " " + where;
			ResultSet rs = executeQuery(request);
			if (rs.next())
				result = rs.getInt(1);
		} catch (SQLException e) {
			reportError("DBClient.count", e);
		}
		return result;
	}
	
	public static Timestamp getTimestamp(ResultSet rs, String column) 
			throws java.sql.SQLException {
		long time = rs.getLong(column);
		if (time == 0)
			return null;
			//return new Timestamp(0);
		else
			return rs.getTimestamp(column);
	}

}
