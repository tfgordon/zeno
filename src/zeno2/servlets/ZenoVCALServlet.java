package zeno2.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.*;

import javax.mail.internet.MimeUtility;
import javax.servlet.ServletException;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import zeno2.kernel.Article;
import zeno2.kernel.Constants;
import zeno2.kernel.Factory;
import zeno2.kernel.Journal;
import zeno2.kernel.Monitor;
import zeno2.kernel.ZenoResource;
import zeno2.util.TransVCALDate;
import org.apache.log4j.Logger;

/**
 * <strong>ZenoVCALServlet</strong> provides vCalendar facilities for Zeno resources.
 * 
 * Exports a vCalendar object for a zeno article or journal 
 * used to describe calendar event(s). 
 * 
 * URL: normally /zeno/vcal/<id>.vcs
 *   <id> may be an articleid or a journalid
 *
 * it's not complete yet 
 * 
 * @author Juergen Walther
 * @version $Revision: 0.9 $ $Date: 2001/05/15 01:14:37 $
 */
public class ZenoVCALServlet extends HttpServlet {
	static Logger log;
	static final String CRLF = "\r\n";
	static final String VCARD_HEADER = "BEGIN:VCALENDAR\r\n" + "VERSION:1.0\r\n";
	static final String VCARD_FOOTER = "END:VCALENDAR\r\n";
	static final String VEVENT_HEADER = "BEGIN:VEVENT\r\n";
	static final String VEVENT_FOOTER = "END:VEVENT\r\n";

	/**
	 * Gracefully shut down this servlet, releasing any resources
	 * that were allocated.
	 */
	public void destroy() {
		log.info("Finalizing " + getServletName() + " servlet");
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
			setNoCache(req, res);
			res.setContentType("text/x-vCalendar; charset=UTF-8");
			//res.setContentType("text/plain; charset=UTF-8");
			PrintWriter pw = res.getWriter();
			if (resource instanceof Article) {
				marshallArticle(req, (Article) resource, pw);
			} else
				if (resource instanceof Journal) {
					marshallJournal(req, (Journal) resource, pw);
				} else {
					throw new zeno2.kernel.ZenoNotFoundException(
						"Resource " + resource + " is not an Article or Journal");
				}
		} catch (Exception e) {
			e.printStackTrace();
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
	* Initialize this servlet by getting the logger:
	*
	* @exception ServletException if we cannot configure ourselves correctly
	*/
	public void init() throws ServletException {
		Logger zenologger =
			(Logger) getServletContext().getAttribute(Constants.LOGGER_KEY);
		log = zenologger.getLogger(ZenoVCALServlet.class.getName());
		log.info("Initializing " + getServletName() + " servlet");
	}

	private void marshallArticle(
		HttpServletRequest req,
		Article article,
		PrintWriter pw) {
		long before = System.currentTimeMillis();
		pw.print(VCARD_HEADER);
		
		// Timezone handling
		//TimeZone tz = TimeZone.getTimeZone(System.getProperty("user.timezone"));
		//pw.print("TZ:" + tz.getDisplayName() + CRLF);		
		//pw.print("TZ:+20" + CRLF); // time zone
		
		marshallArticlesAttributes(req, article, pw);
		pw.print(VCARD_FOOTER);
		long after = System.currentTimeMillis();
		log.debug("vcard generation took " + (after - before) + " ms");
	}

	private void marshallArticlesAttributes(
		HttpServletRequest req,
		Article article,
		PrintWriter pw) {
		try {
			// only articles which have begin and end dates
			// only vEvents, that do not span more than one day?
			//
			if ((article.getBeginDate() == null) || (article.getEndDate() == null)) {
				log.warn(
					"Article with id = "
						+ article.getId()
						+ " is missing Begin or End date. Skipped.");
				return;
			}
			pw.print(VEVENT_HEADER);
			/* UID
			This property is identified by the property name UID. 
			This property defines a persistent, globally unique identifier associated 
			with the vCalendar entity. Some examples of forms of unique identifiers 
			would include ISO 9070 formal public identifiers (FPI), X.500 distinguished 
			names, machine-generated "random" numbers with a statistically high likelihood 
			of being globally unique and Uniform Resource Locators (URL). 
			If an URL is specified, it is suggested that the URL reference a service 
			which can render an updated version of the vCalendar for the object. 
			*/
			pw.print("UID:" + getUID(req, article) + CRLF);
			/*
			STATUS:
			Indicates todo was accepted by attendee	ACCEPTED
			Indicates event or todo requires action by attendee	NEEDS ACTION
			Indicates event or todo was sent out to attendee	SENT
			Indicates event is tentatively accepted by attendee	TENTATIVE
			Indicates attendee has confirmed their attendance at the event	CONFIRMED
			Indicates event or todo has been rejected by attendee	DECLINED
			Indicates todo has been completed by attendee	COMPLETED
			Indicates event or todo has been delegated by the attendee to another	DELEGATED
			*/
			pw.print("STATUS:NEEDS ACTION" + CRLF);
			pw.print("CATEGORIES:" + article.getLabel() + CRLF);
			// DTSTART:19960401T073000Z
			//pw.print("DTSTART:" + "20020528T073000Z" + CRLF);
			pw.print("DTSTART:" + TransVCALDate.printDate(article.getBeginDate()) + CRLF);
			// DTEND:19960401T083000Z
			//pw.print("DTEND:" + "20020528T093000Z" + CRLF);
			pw.print("DTEND:" + TransVCALDate.printDate(article.getEndDate()) + CRLF);
			// SUMMARY:Steve’s Proposal Review
			pw.print("SUMMARY" + fold(article.getTitle()) + CRLF);
			// DESCRIPTION:Steve and John to review newest proposal material
			pw.print("DESCRIPTION" + fold(article.getNote()) + CRLF);
			// CLASS:PUBLIC | PRIVATE | CONFIDENTIAL
			pw.print("CLASS:PUBLIC" + CRLF);
			pw.print(VEVENT_FOOTER);
		} catch (Exception exc) {
			log.error("Exception:" + exc);
			exc.printStackTrace();
		}
	}

	private void marshallJournal(
		HttpServletRequest req,
		Journal journal,
		PrintWriter pw) {
		try {
			long before = System.currentTimeMillis();
			pw.print(VCARD_HEADER);
			
			// Timezone handling
			//TimeZone tz = TimeZone.getTimeZone(System.getProperty("user.timezone"));
			//pw.print("TZ:" + tz.getDisplayName() + CRLF);		
			//pw.print("TZ:+20" + CRLF); // time zone
			
			pw.print("UID:" + getUID(req, journal) + CRLF);
			Iterator members = journal.getMembers();
			while (members.hasNext()) {
				ZenoResource member = (ZenoResource) members.next();
				if (member instanceof Article)
					marshallArticlesAttributes(req, (Article) member, pw);
			};
			pw.print(VCARD_FOOTER);
			long after = System.currentTimeMillis();
			log.debug("vcard generation took " + (after - before) + " ms");
		} catch (Exception exc) {
			log.error("Exception:" + exc);
			exc.printStackTrace();
		}
	}

	private String getUID(HttpServletRequest req, ZenoResource resource) {
		String prot = req.getProtocol();
		prot = prot.substring(0, prot.indexOf("/")).toLowerCase();
		String uid =
			prot
				+ "://"
				+ req.getServerName()
				+ ":"
				+ req.getServerPort()
				+ req.getContextPath()
				+ req.getServletPath()
				+ "/"
				+ resource.getId()
				+ ".vcs";
		return uid;
	}

	private String fold(String str) {
		/*
		Individual lines within the vCalendar data stream are delimited 
		by the (RFC 822) line break, which is a CRLF sequence (ASCII decimal 13, 
		followed by ASCII decimal 10). Long lines of text can be split into 
		a multiple-line representation using the RFC 822 "folding" technique. 
		That is, wherever there may be linear white space (NOT simply LWSP-chars), 
		a CRLF immediately followed by at least one LWSP-char may instead be inserted. 
		For example the line:
			DESCRIPTION:This is a very long description that exists on a long line.
		Can be represented as:
			DESCRIPTION:This is a very long description 
					that exists on a long line.
		
		line length should be less than 76 characters.
		Base64 coding (B) or quoted-printable (Q)
		;ENCODING=BASE64: or ;ENCODING=QUOTED-PRINTABLE:
		*/
		//log.debug( "Index of CRNL is: " + str.indexOf("\r\n") ) ;
		int line_length = 76 - 32; // DESCRIPTION;CHARSET=UTF: 
		if (str.length() <= line_length) {
			return ";CHARSET=UTF-8:" + str;
		} else {
			StringBuffer folded = new StringBuffer((int) (str.length() * 1.1));
			int i = 0, j = 0, stop = str.length();
			while (i < stop) {
				char c = str.charAt(i++);
				switch (c) {
					case '\r' :
						if (str.charAt(i) == '\n') {
							i++;
							folded.append("\r\n\t");
						} else {
							log.debug("\r not followed by \n");
							folded.append("\r\n\t");
						}
						j = 0;
						break;
					case ' ' :
						j++;
						if (j > line_length) {
							folded.append("\r\n\t ");
							j = 0;
						} else {
							folded.append(c);
						}
						break;
					default :
						j++;
						folded.append(c);
				}
				//log.debug("i: " + i + " , j: " + j);
			}
			return ";CHARSET=UTF-8:" + folded.toString();
		}
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

/*
		try {
			String enc_str = MimeUtility.encodeText(str, null, "Q");
			if (str.equals(enc_str)){
				return ":" + str;}
			else {
				//return ";ENCODING=BASE64:" + enc_str;
				return ";ENCODING=QUOTED-PRINTABLE:" + enc_str;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return ":" + str;
		}

BEGIN:VCALENDAR
VERSION:1.0
BEGIN:VEVENT
CATEGORIES:MEETING
STATUS:NEEDS ACTION
DTSTART:19960401T073000Z
DTEND:19960401T083000Z
SUMMARY:Steve’s Proposal Review
DESCRIPTION:Steve and John to review newest proposal material
CLASS:PRIVATE
END:VEVENT
END:VCALENDAR
*/