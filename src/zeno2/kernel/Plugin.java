package zeno2.kernel;

/**
 * Zeno 2 Plugin, i.e. a set of attributes, which enable the Zeno
 * webapplication to communicate with annother webapplication
 *
 *@author     <a href="mailto:lothar.oppor@ais.fhg.de">Lothar Oppor</a>
 *@created    2002-03-21
 *@version    $zenoVersion $
 */

public interface Plugin {

	/** Create a new DataBase table entry for this Plugin. */
	public void create() throws ZenoException;

	/** Rewrite the DataBase table entry of this Plugin. */
	public void save() throws ZenoException;

	/** Destroy the DataBase table entry of this Plugin. */
	public void delete() throws ZenoException;

	/** Gets the plugin id (name). */
	public String getId();

	/** Gets the journalMenu attribute. */
	public String getJournalMenu();

	/** Sets the journalMenu attribute to jm. */
	public void setJournalMenu(String jm);

	/** Gets the journalIcon attribute. */
	public String getJournalIcon();

	/** Sets the journalIcon attribute to ji. */
	public void setJournalIcon(String ji) ;

	/** Gets the journalURL attribute. */
	public String getJournalURL();

	/** Sets the journalURL attribute to ju. */
	public void setJournalURL(String ju);

	/** Gets the journalParams attribute. */
	public String getJournalParams();

	/** Sets the journalParams attribute to jp. */
	public void setJournalParams(String jp);

	/** Gets the articleMenu attribute. */
	public String getArticleMenu();

	/** Sets the articleMenu attribute to am. */
	public void setArticleMenu(String am);

	/** Gets the articleIcon attribute. */
	public String getArticleIcon();

	/** Sets the articleIcon attribute to ai. */
	public void setArticleIcon(String ai);

	/** Gets the articleURL attribute. */
	public String getArticleURL();

	/** Sets the articleURL attribute to au. */
	public void setArticleURL(String au);

	/** Gets the articleParams attribute. */
	public String getArticleParams();

	/** Sets the articleParams attribute to ap. */
	public void setArticleParams(String ap);

	/** Gets the title attribute. */
	public String getTitle();

	/** Sets the title attribute to addOn. */
	public void setTitle(String title);
	
	/** Gets the addOn attribute. */
	public String getAddOn();

	/** Sets the addOn attribute to addOn. */
	public void setAddOn(String addOn);
	
	public boolean isEmpty();
	
	public String toString();

}
