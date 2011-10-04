package zeno2.db;

import java.sql.ResultSet;
import java.sql.SQLException;
import zeno2.kernel.Plugin;
import zeno2.kernel.ZenoException;


/**
 * Zeno 2 Plugin, i.e. a set of attributes, which enable the Zeno
 * webapplication to communicate with annother webapplication
 *
 *@author     <a href="mailto:lothar.oppor@ais.fhg.de">Lothar Oppor</a>
 *@created    2002-03-21
 *@version    $zenoVersion $
 */

public class PluginImpl implements Plugin {
	
	FactoryImpl factory;
	String id;
	String journalMenu = "";
	String journalIcon = "";
	String journalURL = "";
	String journalParams = "";
	String articleMenu = "";
	String articleIcon = "";
	String articleURL = "";
	String articleParams = "";
	String title = "";
	String addOn = "";
	boolean empty = true;
	
	protected PluginImpl (FactoryImpl factory,String id) {
		this.factory = factory;
		this.id = id;
	}
	
	protected PluginImpl (FactoryImpl factory,ResultSet rs) 
			 throws SQLException {
		this.factory = factory;
		if (rs.next()) {
			this.id = rs.getString("id");
			this.journalMenu = rs.getString("journalMenu");
			this.journalIcon = rs.getString("journalIcon");
			this.journalURL = rs.getString("journalURL");
			this.journalParams = rs.getString("journalParams");
			this.articleMenu = rs.getString("articleMenu");
			this.articleIcon = rs.getString("articleIcon");
			this.articleURL = rs.getString("articleURL");
			this.articleParams = rs.getString("articleParams");
			this.title = rs.getString("title");
			this.addOn = rs.getString("addOn");
			this.empty = false;
		}
	}
	
	
	
	
	/** create a new plugin table entry*/
	public void create() throws ZenoException {
		StringBuffer buf = new StringBuffer();
		try {
			buf.append("insert into plugin values(");
			buf.append(DBClient.format(this.id));
			buf.append(",");
			buf.append(DBClient.format(this.journalMenu));
			buf.append(",");
			buf.append(DBClient.format(this.journalIcon));
			buf.append(",");
			buf.append(DBClient.format(this.journalURL));
			buf.append(",");
			buf.append(DBClient.format(this.journalParams));
			buf.append(",");
			buf.append(DBClient.format(this.articleMenu));
			buf.append(",");
			buf.append(DBClient.format(this.articleIcon));
			buf.append(",");
			buf.append(DBClient.format(this.articleURL));
			buf.append(",");
			buf.append(DBClient.format(this.articleParams));
			buf.append(",");
			buf.append(DBClient.format(this.title));
			buf.append(",");
			buf.append(DBClient.format(this.addOn));
			buf.append(")");
			factory.dbclient.executeUpdate(buf.toString());
		}
		catch (SQLException e) {
			factory.reportError("Plugin.create",e);
			throw new ZenoException("DataBaseException");
		}
	}
	
	/** Rewrite the DataBase table entry of this Plugin. */
	public void save() throws ZenoException {
		StringBuffer buf = new StringBuffer();
		try {
			buf.append("update plugin set id=");
			buf.append(DBClient.format(this.id));
			buf.append(",journalMenu=");
			buf.append(DBClient.format(this.journalMenu));
			buf.append(",journalIcon=");
			buf.append(DBClient.format(this.journalIcon));
			buf.append(",journalURL=");
			buf.append(DBClient.format(this.journalURL));
			buf.append(",journalParams=");
			buf.append(DBClient.format(this.journalParams));
			buf.append(",articleMenu=");
			buf.append(DBClient.format(this.articleMenu));
			buf.append(",articleIcon=");
			buf.append(DBClient.format(this.articleIcon));
			buf.append(",articleURL=");
			buf.append(DBClient.format(this.articleURL));
			buf.append(",articleParams=");
			buf.append(DBClient.format(this.articleParams));
			buf.append(",title=");
			buf.append(DBClient.format(this.title));
			buf.append(",addOn=");
			buf.append(DBClient.format(this.addOn));
			buf.append(")");
			factory.dbclient.executeUpdate(buf.toString());
		}
		catch (SQLException e) {
			factory.reportError("Plugin.save",e);
			throw new ZenoException("DataBaseException");
		}
	}
	
	/** Destroy the DataBase table entry of this Plugin. */
	public void delete() throws ZenoException {
		StringBuffer buf = new StringBuffer();
		try {
			buf.append("delete from plugin where id=");
			buf.append(DBClient.format(this.id));
			factory.dbclient.executeUpdate(buf.toString());
		}
		catch (SQLException e) {
			factory.reportError("Plugin.delete",e);
			throw new ZenoException("DataBaseException");
		}
	}
	
	/** Gets the plugin id (name). */
	public String getId() {
		return id;
	}

	/** Gets the journalMenu attribute. */
	public String getJournalMenu() {
		return journalMenu;
	}

	/** Sets the journalMenu attribute to jm. */
	public void setJournalMenu(String jm) {
		this.journalMenu = jm;
	}

	/** Gets the journalIcon attribute. */
	public String getJournalIcon(){
		return this.journalIcon;
	}

	/** Sets the journalIcon attribute to ji. */
	public void setJournalIcon(String ji) {
		this.journalIcon = ji;
	}

	/** Gets the journalURL attribute. */
	public String getJournalURL() {
		return this.journalURL;
	}

	/** Sets the journalURL attribute to ju. */
	public void setJournalURL(String ju) {
		this.journalURL = ju;
	}

	/** Gets the journalParams attribute. */
	public String getJournalParams() {
		return this.journalParams;
	}

	/** Sets the journalParams attribute to jp. */
	public void setJournalParams(String jp) {
		this.journalParams = jp;
	}

	/** Gets the articleMenu attribute. */
	public String getArticleMenu() {
		return this.articleMenu;
	}

	/** Sets the articleMenu attribute to am. */
	public void setArticleMenu(String am) {
		this.articleMenu = am;
	}

	/** Gets the articleIcon attribute. */
	public String getArticleIcon() {
		return this.articleIcon;
	}

	/** Sets the articleIcon attribute to ai. */
	public void setArticleIcon(String ai) {
		this.articleIcon = ai;
	}

	/** Gets the articleURL attribute. */
	public String getArticleURL() {
		return this.articleURL;
	}

	/** Sets the articleURL attribute to au. */
	public void setArticleURL(String au) {
		this.articleURL = au;
	}

	/** Gets the articleParams attribute. */
	public String getArticleParams() {
		return this.articleParams;
	}

	/** Sets the articleParams attribute to ap. */
	public void setArticleParams(String ap) {
		this.articleParams = ap;
	}

	
	/** Gets the title attribute. */
	public String getTitle() {
		return this.title;
	}
	
	/** Sets the title attribute. */
	public void setTitle(String tit) {
		this.title = tit;
	}
	
	/** Gets the addOn attribute. */
	public String getAddOn() {
		return this.addOn;
	}

	/** Sets the addOn attribute to addOn. */
	public void setAddOn(String addOn) {
		this.addOn = addOn;
	}
	public boolean isEmpty() {
		return this.empty;
	}
	
	public String toString() {
		return "[" + id + "," + this.journalMenu + "," + this.journalIcon 
			+ "," + this.journalURL + "," + this.journalParams + "," 
			+ this.articleMenu + "," + this.articleIcon + "," + this.articleURL 
			+ "," + this.articleParams + "," + this.title  + "," + this.addOn + "]";
	}

}
