package zeno2.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import zeno2.kernel.Article;
import zeno2.kernel.Attachment;
import zeno2.kernel.Constants;
import zeno2.kernel.Factory;
import zeno2.kernel.Journal;
import zeno2.kernel.Link;
import zeno2.kernel.Monitor;
import zeno2.kernel.XLink;
import zeno2.kernel.ZenoResource;
import org.apache.log4j.Logger;
 
/**
 * <strong>ZenoGXLServlet</strong> provides XML facilities for Zeno resources.
 * 
 * Exports in G(raph)(e)X(change)L(anguage) gxl format. 
 * 
 * Parameters:
 *   /zeno/gxl/<jid>.<ext>  exports a journal with id <jid> with all subjournals as gxl. 
 *
 * it's not complete yet 
 * @author Juergen Walther
 * @version $Revision: 0.9 $ $Date: 2001/05/15 01:14:37 $
 */
public class ZenoGXLServlet extends HttpServlet {
	static Logger log;
	private int nestingLevel = 0;
	private int counter = 0;
	private HashSet linkSet = new HashSet();
	static final String GXL_HEADERS =
		"<?xml version='1.0' encoding='UTF-8'?>\n"
			+ "<!DOCTYPE gxl SYSTEM \"/zeno/dtd/gxl.dtd\">\n"
			+ "<gxl xmlns:xlink=\"http://www.w3.org/1999/xlink\">\n";
	static final String ACTION_LIST = "list";
	static final String ACTION_TREE = "tree";

	/**
	 * Gracefully shut down this servlet, releasing any resources
	 * that were allocated.
	 */
	public void destroy() {
		log.info("Finalizing " + getServletName() + " servlet");
		linkSet = null;
	}

	public void doGet(HttpServletRequest req, HttpServletResponse res)
		throws ServletException, IOException {
		Monitor monitor;
		Factory factory;
		HttpSession session = req.getSession(false);
		log.debug("session = " + session);
		try {
			monitor = (Monitor) getServletContext().getAttribute(Constants.MONITOR_KEY);
			log.debug("monitor = " + monitor);
			if (session == null) {
				req.getSession(true);
				log.debug("new session ");
				String userid = "zenoadmin";
				String passwd = "zeno%admin";
				factory = monitor.login(userid, passwd);
				session.setAttribute(Constants.FACTORY_KEY, factory);
				log.debug("logged in as zenoadmin. factory = " + factory);
			} else {
				factory = (Factory) session.getAttribute(Constants.FACTORY_KEY);
			};
			String rid_str = req.getPathInfo();
			String rid_num = rid_str.substring(1, rid_str.indexOf("."));
			log.debug("loading resource with id = " + rid_num);
			ZenoResource resource = factory.loadResource(Integer.parseInt(rid_num));
			/*
			String id = req.getParameter("id").toLowerCase();
			String action = req.getParameter("action").toLowerCase();
			log.debug("getting resource with id = " + id + ", action = " + action);
			ZenoResource resource = factory.loadResource(Integer.parseInt(id));
			*/
			setNoCache(req, res);
			res.setContentType("text/xml; charset=UTF-8");
			//res.setContentType("text/plain; charset=UTF-8");
			PrintWriter pw = res.getWriter();
			if (resource instanceof Journal) {
				//if (action.equals(ACTION_TREE)) {
					marshallTopJournal((Journal) resource, pw);
				//}
				//if (action.equals(ACTION_LIST)) {
				//	marshallTopJournal((Journal) resource, pw);
				//}
			} else {
				throw new zeno2.kernel.ZenoNotFoundException(
					"Resource " + resource + " is not a journal");
			}
		} catch (Exception e) {
			log.error("Exception: " + e);
			throw new UnavailableException(e.getMessage());
		}
	}

	public void doPost(HttpServletRequest req, HttpServletResponse res)
		throws ServletException, IOException {
		doGet(req, res);
	}

	public String getServletInfo() {
		return getServletName();
	} 
	
	/**
	* Initialize this servlet, including loading our initial database from
	* persistent storage.  
	* 
	* @exception ServletException if we cannot configure ourselves correctly
	*/
	public void init() throws ServletException {
		Logger zenologger =
			(Logger) getServletContext().getAttribute(Constants.LOGGER_KEY);
		log = zenologger.getLogger(ZenoGXLServlet.class.getName());
		log.debug("Initializing " + getServletName() + " servlet");
	}

	// gxl functionality

	private void marshallTopJournal(Journal journal, PrintWriter pw) {
		long before = System.currentTimeMillis();
		pw.println(GXL_HEADERS);
		linkSet = new HashSet();
		counter = 0;
		marshallJournalTree(journal, pw);
		pw.println("</gxl>");
		long after = System.currentTimeMillis();
		log.debug("GXL generation took " + (after - before) + " ms");
	}

	private void marshallJournalTree(
		Journal journal,
		PrintWriter pw) { // recursive depth first tree walk
		try {
			pw.println("<graph id=\"g" + journal.getId() + "\" hypergraph=\"true\">");
			marshallJournalAttributes(journal, pw);
			// attributes
			Iterator members = journal.getMembers();
			while (members.hasNext()) {
				ZenoResource member = (ZenoResource) members.next();
				if (member instanceof Article)
					marshallArticlesAttributes((Article) member, pw);
				// nodes and edges
			};
			Set subJournals = journal.getSubjournals();
			if (subJournals.isEmpty()) { // end recursion test
				pw.println("</graph>");
				return;
			} else {
				// create hyper nodes
				Iterator journalIterator = subJournals.iterator();
				while (journalIterator.hasNext()) {
					Journal subJournal = (Journal) journalIterator.next();
					pw.println("<node id=\"h" + subJournal.getId() + "\">");
					//objectToLog(journal,pw);
					marshallJournalTree(subJournal, pw);
					pw.println("</node>");
				};
			};
			pw.println("</graph>");
		} catch (Exception exc) {
		}
	}

	/**
	* 
	* 
	*/
	private void genEdges(PrintWriter pw) {
		Object member;
		Iterator links = linkSet.iterator();
		while (links.hasNext()) {
			member = (Object) links.next();
			log.debug(member);
		};
		Collection noDups = new HashSet(linkSet);
		log.debug(noDups);
		log.debug("noDups.size = " + noDups.size());
		/*
			List links = article.getLinks(0);
			for (int i = 0; i < links.size(); i++) {
				Link link = (Link) links.get(i);
				pw.println("<edge from=\"n" + link.getSourceId() + "\" to=\"n" + link.getTargetId() + "\">");
				pw.println("<attr name=\"Label\">");
				pw.println("<strng>" + link.getLabel() + "</strng>");
				pw.println("</attr>");
				pw.println("</edge>");
				log.debug( link );
			}
		*/
	}

	/**
	* 
	* 
	*/
	private void marshallJournalAttributes(Journal journal, PrintWriter pw) {
		try {
			pw.println("<attr name=\"Title\">");
			pw.println("<strng>" + encode(journal.getTitle()) + "</strng>");
			pw.println("</attr>");
			pw.println("<attr name=\"Rank\">");
			pw.println("<int>" + journal.getRank() + "</int>");
			pw.println("</attr>");
			pw.println("<attr name=\"Creator\">");
			pw.println("<strng>" + encode(journal.getCreator()) + "</strng>");
			pw.println("</attr>");
			pw.println("<attr name=\"Note\">");
			pw.println("<strng>" + encode(journal.getNote()) + "</strng>");
			pw.println("</attr>");
			pw.println("<attr name=\"CreationDate\">");
			pw.println("<date>" + journal.getCreationDate() + "</date>");
			pw.println("</attr>");
			pw.println("<attr name=\"ModificationDate\">");
			pw.println("<date>" + journal.getModificationDate() + "</date>");
			pw.println("</attr>");
			pw.println("<attr name=\"ArticleLabels\">");
			pw.println("<strng>" + encode(journal.getArticleLabels()) + "</strng>");
			pw.println("</attr>");
			pw.println("<attr name=\"LinkLabels\">");
			pw.println("<strng>" + encode(journal.getLinkLabels()) + "</strng>");
			pw.println("</attr>");
			pw.println("<attr name=\"Qualifiers\">");
			pw.println("<strng>" + encode(journal.getQualifiers()) + "</strng>");
			pw.println("</attr>");
			// all additional properties
			List props = journal.getPropertyKeys();
			for (int i = 0; i < props.size(); i++) {
				String propName = (String) props.get(i);
				pw.println("<attr name=\"" + propName + "\">");
				pw.println("<strng>" + encode(journal.getProperty(propName)) + "</strng>");
				pw.println("</attr>");
			}
		} catch (Exception exc) {
		}
	}

	/**
	* 
	* 
	*/
	private void marshallArticlesAttributes(Article article, PrintWriter pw) {
		try {
			//remember links from articles
			pw.println("<node id=\"n" + article.getId() + "\">");
			pw.println("<attr name=\"Label\">");
			pw.println("<strng>" + encode(article.getLabel()) + "</strng>");
			pw.println("</attr>");
			pw.println("<attr name=\"Title\">");
			pw.println("<strng>" + encode(article.getTitle()) + "</strng>");
			pw.println("</attr>");
			pw.println("<attr name=\"Qualifier\">");
			pw.println("<strng>" + encode(article.getQualifier()) + "</strng>");
			pw.println("</attr>");
			pw.println("<attr name=\"Rank\">");
			pw.println("<int>" + article.getRank() + "</int>");
			pw.println("</attr>");
			pw.println("<attr name=\"Author\">");
			pw.println("<strng>" + encode(article.getAuthor()) + "</strng>");
			pw.println("</attr>");
			pw.println("<attr name=\"Creator\">");
			pw.println("<strng>" + encode(article.getCreator()) + "</strng>");
			pw.println("</attr>");
			pw.println("<attr name=\"Note\">");
			pw.println("<strng>" + encode(article.getNote()) + "</strng>");
			pw.println("</attr>");
			pw.println("<attr name=\"CreationDate\">");
			pw.println("<date>" + article.getCreationDate() + "</date>");
			pw.println("</attr>");
			pw.println("<attr name=\"ModificationDate\">");
			pw.println("<date>" + article.getModificationDate() + "</date>");
			pw.println("</attr>");
			pw.println("<attr name=\"ExpirationDate\">");
			pw.println("<date>" + article.getExpirationDate() + "</date>");
			pw.println("</attr>");
			pw.println("<attr name=\"BeginDate\">");
			pw.println("<date>" + article.getBeginDate() + "</date>");
			pw.println("</attr>");
			pw.println("<attr name=\"EndDate\">");
			pw.println("<date>" + article.getEndDate() + "</date>");
			pw.println("</attr>");
			// all additional properties
			List props = article.getPropertyKeys();
			for (int i = 0; i < props.size(); i++) {
				String propName = (String) props.get(i);
				pw.println("<attr name=\"" + propName + "\">");
				pw.println("<strng>" + encode(article.getProperty(propName)) + "</strng>");
				pw.println("</attr>");
			}
			pw.println("</node>");

			// all attachments
			List attachments = article.getAttachments();
			for (int i = 0; i < attachments.size(); i++) {
				Attachment attm = (Attachment) attachments.get(i);

				pw.println("<node id=\"a" + attm.getId() + "\">");
				pw.println("<attr name=\"Label\">");
				pw.println("<strng>" + "Attachment" + "</strng>");
				pw.println("</attr>");
				pw.println("<attr name=\"Name\">");
				pw.println("<strng>" + encode(attm.getName()) + "</strng>");
				pw.println("</attr>");
				pw.println("<attr name=\"mimeType\">");
				pw.println("<strng>" + attm.getMimeType() + "</strng>");
				pw.println("</attr>");
				pw.println("<attr name=\"size\">");
				pw.println("<float>" + attm.size() + "</float>");
				pw.println("</attr>");
				pw.println("</node>");

				pw.println(
					"<edge from=\"n" + article.getId() + "\" to=\"a" + attm.getId() + "\">");
				String propName = (String) attm.getName();
				pw.println("<attr name=\"Label\">");
				pw.println("<strng>" + "has_attachment" + "</strng>");
				pw.println("</attr>");
				pw.println("</edge>");
			}

			// all XLinks
			List xlinks = article.getXLinks("");
			for (int i = 0; i < xlinks.size(); i++) {
				XLink xlink = (XLink) xlinks.get(i);
				counter++;
				pw.println("<node id=\"x" + counter + "\">");
				pw.println("<attr name=\"Label\">");
				pw.println("<strng>" + "X_Ref_" + counter + "</strng>");
				pw.println("</attr>");
				pw.println("<attr name=\"Name\">");
				pw.println("<strng>" + encode(xlink.getName()) + "</strng>");
				pw.println("</attr>");
				pw.println("<attr name=\"Type\">");
				pw.println("<strng>" + xlink.getType() + "</strng>");
				pw.println("</attr>");
				pw.println("<attr name=\"Reference\">");
				pw.println("<strng>" + encode(xlink.getReference()) + "</strng>");
				pw.println("</attr>");
				pw.println("</node>");

				pw.println("<edge from=\"n" + article.getId() + "\" to=\"x" + counter + "\">");
				String propName = (String) xlink.getName();
				pw.println("<attr name=\"Label\">");
				pw.println("<strng>" + "references" + "</strng>");
				pw.println("</attr>");
				pw.println("</edge>");
			}

			/*
			// all outgoing Links
			//linkSet.addAll(article.getLinks(1));
			List outlinks = article.getLinks(1);
			for (int i = 0; i < outlinks.size(); i++) {
				Link link = (Link) outlinks.get(i);
				pw.println(
					"<edge from=\"n"
						+ link.getSourceId()
						+ "\" to=\"n"
						+ link.getTargetId()
						+ "\">");
				pw.println("<attr name=\"Label\">");
				pw.println("<strng>" + encode(link.getLabel()) + "</strng>");
				pw.println("</attr>");
				pw.println("<attr name=\"SourceAlias\">");
				pw.println("<strng>" + encode(link.getSourceAlias()) + "</strng>");
				pw.println("</attr>");
				pw.println("<attr name=\"TargetAlias\">");
				pw.println("<strng>" + encode(link.getTargetAlias()) + "</strng>");
				pw.println("</attr>");
				pw.println("<attr name=\"UserSourceAlias\">");
				pw.println("<strng>" + encode(link.getUserSourceAlias()) + "</strng>");
				pw.println("</attr>");
				pw.println("<attr name=\"UserTargetAlias\">");
				pw.println("<strng>" + encode(link.getUserTargetAlias()) + "</strng>");
				pw.println("</attr>");
				pw.println("</edge>");
				//log.debug(link);
			}
			*/
			// all incomming Links
			//linkSet.addAll(article.getLinks(-1));
			List inlinks = article.getLinks(-1);
			for (int i = 0; i < inlinks.size(); i++) {
				Link link = (Link) inlinks.get(i);
				pw.println(
					"<edge from=\"n"
						+ link.getSourceId()
						+ "\" to=\"n"
						+ link.getTargetId()
						+ "\">");
				pw.println("<attr name=\"Label\">");
				pw.println("<strng>" + encode(link.getLabel()) + "</strng>");
				pw.println("</attr>");
				pw.println("<attr name=\"SourceAlias\">");
				pw.println("<strng>" + encode(link.getSourceAlias()) + "</strng>");
				pw.println("</attr>");
				pw.println("<attr name=\"TargetAlias\">");
				pw.println("<strng>" + encode(link.getTargetAlias()) + "</strng>");
				pw.println("</attr>");
				pw.println("<attr name=\"UserSourceAlias\">");
				pw.println("<strng>" + encode(link.getUserSourceAlias()) + "</strng>");
				pw.println("</attr>");
				pw.println("<attr name=\"UserTargetAlias\">");
				pw.println("<strng>" + encode(link.getUserTargetAlias()) + "</strng>");
				pw.println("</attr>");
				pw.println("</edge>");
				//log.debug(link);
			}
		} catch (Exception exc) {
		}
	}

	private String encode(String str) {
		if ((str.indexOf("<") >= 0)
			|| (str.indexOf(">") >= 0)
			|| (str.indexOf("'") >= 0)
			|| (str.indexOf('\"') >= 0)
			|| (str.indexOf("&") >= 0)) {
			StringBuffer filtered = new StringBuffer((int) (str.length() * 1.1));
			int i = 0, stop = str.length();
			while (i < stop) {
				char c = str.charAt(i++);
				switch (c) {
					case '<' :
						filtered.append("&lt;");
						break;
					case '>' :
						filtered.append("&gt;");
						break;
					case '\'' :
						filtered.append("&apos;");
						break;
					case '"' :
						filtered.append("&quot;");
						break;
					case '&' :
						filtered.append("&amp;");
						break; // more characters would come here
					default :
						filtered.append(c);
				}
			};
			return filtered.toString();
		} else
			return str;
	}

	private String encode_old(String str) {
		StringBuffer filtered = new StringBuffer((int) (str.length() * 1.1));
		int i = 0, stop = str.length();
		while (i < stop) {
			char c = str.charAt(i++);
			switch (c) {
				case '<' :
					filtered.append("&lt;");
					break;
				case '>' :
					filtered.append("&gt;");
					break;
				case '\'' :
					filtered.append("&apos;");
					break;
				case '"' :
					filtered.append("&quot;");
					break;
				case '&' :
					filtered.append("&amp;");
					break; // more characters would come here
				default :
					filtered.append(c);
			}
		};
		return filtered.toString();
	}

	private void setNoCache(
		HttpServletRequest request,
		HttpServletResponse response) {
		if (request.getProtocol().compareTo("HTTP/1.0") == 0) {
			response.setHeader("Pragma", "no-cache");
		} else
			if (request.getProtocol().compareTo("HTTP/1.1") == 0) {
				response.setHeader("Cache-Control", "no-cache");
			}
		response.setDateHeader("Expires", 0);
	}

}