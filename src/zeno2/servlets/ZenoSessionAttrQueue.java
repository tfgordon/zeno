package zeno2.servlets;

import com.oreilly.servlet.LocaleNegotiator;

//import java.net.URLDecoder;
import java.io.IOException;
import java.io.PrintWriter;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;

//import javax.servlet.RequestDispatcher;
//import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import zeno2.kernel.Constants;
import zeno2.kernel.Principal;
import zeno2.velocity.util.ZenoBundle;
import zeno2.velocity.util.ZenoDecoder;

/**
 *  Toolbox Servlet to handle Zeno Session Plugin Attributes, above all the
 *  Session Queues for the Zeno Plugins.
 *
 *@author     <a href="mailto:lothar.oppor@@ais.fraunhofer.de">Lothar Oppor</a>
 *@version    2.0.2, 2002-08-28
 */

public class ZenoSessionAttrQueue extends HttpServlet {

	HttpServletRequest request = null;
	HttpServletResponse response = null;
	HttpSession session = null;
	String message = "";
	Object sessAttr = null;
	List queue = null;
	ZenoBundle bundle = null;
	final String sess_attr = "session_attribute";


	/**
	 *  Description of the Method
	 *
	 *@param  req                   Description of Parameter
	 *@param  res                   Description of Parameter
	 *@exception  ServletException  Description of Exception
	 *@exception  IOException       Description of Exception
	 */
	public void doGet(HttpServletRequest req, HttpServletResponse res)
				throws ServletException, IOException {
		ZenoDecoder dec = new ZenoDecoder();
		String ok = null;
		this.request = req;
		this.response = res;
		this.session = req.getSession();
//System.out.println("ZenoSessionAttrQueue.doget.sessId=" + session.getId());
		bundle = loadBundle();
		try {
			String action = req.getParameter("action");
			String id = req.getParameter("id");
			String attr = req.getParameter("attr");
			String val = req.getParameter("val");

			response.setContentType("text/plain");
			PrintWriter out = new PrintWriter(response.getWriter());

			//********************* get ********************************
			if (action.equals("get")) {
				sessAttr = session.getAttribute(attr);
				if (sessAttr == null) {
					//out.println(getString(sess_attr) + attr + getString("is_empty"));
					//ok = "failed";
				}
				else if (sessAttr instanceof List) {
					queue = (List) sessAttr;
// ***********************************************************
//System.out.println("<--QUEUE.get.sessID="+session.getId()
//+", user="+((Principal)session.getAttribute("user")).getName()
//+", IDENTIFIERS="+getIDENT(queue));
// ***********************************************************

					for (int i = 0; i < queue.size(); i++) {
						out.println((String) queue.get(i));
					}
					session.removeAttribute(attr);
					session.setAttribute(attr, new ArrayList());
				}
				else {
					out.println(sessAttr.toString());
				}
				ok = "<EOF>";
			}
			//****************** remove ********************************
			else if (action.equals("remove")) {
				session.removeAttribute(attr);
				out.println(getString(sess_attr) + attr + getString("removed"));
			}
			//********************* add ********************************
			else if (action.equals("add")) {
				ok = "ok";
				//out.println("trying to add " + val);
				String artId = extractId(val);
//System.out.println("ZenoSessionAttrQueue.doGet.id=" + id + ", action=" + action
//+ ", attr=" + attr + ", val=" + val + ", artId=" + artId + ", session=" + session.getId());
				val = dec.decode(val);
//System.out.println("ZenoSessionAttrQueue.doGet.decodedVal=" + val);

// ***********************************************************
//System.out.println("-->QUEUE.add.sessID="+session.getId()
//+", user="+((Principal)session.getAttribute("user")).getName()
//+", IDENTIFIER="+getIDENT(val));
// ***********************************************************

				String forumServlet = (String) session.getAttribute("forumServlet");
				String nextURL = forumServlet + "?action=editArticle&view=flash&id=" + artId;
//System.out.println("ZenoSessionAttrQueue.doGet.nextURL="+nextURL);
				sessAttr = session.getAttribute(attr);
				//out.println("---> "+ sessAttr);
				if (sessAttr == null) {
					List queue = new ArrayList();
					queue.add(val);
					session.setAttribute(attr, queue);
					//out.println("first element "+ val);
					nextURL += "&mes=map_entry_ok";
				}
				else if (sessAttr instanceof List) {
					List queue = (List) sessAttr;
					if (queue == null) {
						queue = new ArrayList();
					}
					queue.add(val);
					//out.println("added "+ val);
					//out.println(sessAttr+" is: "+ queue);
					session.setAttribute(attr, queue);
					nextURL += "&mes=map_entry_ok";
				}
				else {
					//out.println(getString(sess_attr) + attr + getString("not_list"));
					ok = "failed";
					nextURL += "&mes=queue.error1";
				}
				response.sendRedirect(nextURL);
				return;
			}
			//********************* set ********************************
			else if (action.equals("set")) {
				session.setAttribute(attr, val);
				ok = "ok";
			}
			else if (action.equals("")) {
				ok = "failed";
			}
			else {
				out.println(getString("unknown_action") + action);
				ok = "failed";
			}
			if (ok != null) {
				out.println(ok);
			}

			out.close();
		}
		catch (Exception e) {
			System.out.println("ZenoSessionAttrQueue.doGet.error=" + e);
			log("Forwarding exception: " + e);
			throw new UnavailableException(e.getMessage());
		}
	}


	/**
	 *  Gets the String attribute of the ZenoSessionAttrQueue object
	 *
	 *@param  what  Description of Parameter
	 *@return       The String value
	 */
	private String getString(String what) {
		System.out.println("ZenoSessionAttrQueue.getString.what=" + what);
		if (bundle == null) {
			return what;
		}
		System.out.println("ZenoSessionAttrQueue.getString.what=queue." + what);
		return bundle.getString("queue." + what);
	}


	/**
	 *  Description of the Method
	 *
	 *@return    Description of the Returned Value
	 */
	private ZenoBundle loadBundle() {
		try {
			String bundleName = Constants.RESOURCE_NAME;
			String acceptLanguage = request.getHeader("Accept-Language");
			String acceptCharset = request.getHeader("Accept-Charset");

			LocaleNegotiator negotiator =
					new LocaleNegotiator(bundleName, acceptLanguage, acceptCharset);

			Locale locale = negotiator.getLocale();
			String charset = negotiator.getCharset();
			return new ZenoBundle(bundleName, negotiator.getBundle());
		}
		catch (Exception e) {
			System.out.println("ZenoSessionAttrQueue.loadBundle.error=" + e);
			return null;
		}
	}


	/**
	 *  Description of the Method
	 *
	 *@param  what  Description of Parameter
	 *@return       Description of the Returned Value
	 */
	private String extractId(String what) {
		int p = what.indexOf("IDENTIFIER");
		p = what.indexOf("\"", p);
		int q = what.indexOf("\"", p + 1);
		if ((q < 0) || (p > q)) {
			return "";
		}
		return what.substring(p + 1, q);
	}


	/**
	 *  Description of the Method
	 *
	 *@param  what  Description of Parameter
	 *@return       Description of the Returned Value
	 */
	private String getIDENT(String what) {
		int p = what.indexOf("IDENTIFIER");
		int q = what.indexOf("\"", p + 12);
		if ((q < 0) || (p > q)) {
			return "";
		}
		return  what.substring(p + 11, q);
	}


	/**
	 *  Description of the Method
	 *
	 *@param  what  Description of Parameter
	 *@return       Description of the Returned Value
	 */
	private String getIDENT(List what) {
		String result = "";
		for (int i=0;i<what.size(); i++) {
			result += " "+ getIDENT((String) what.get(i));
		}
		return result;
	}
}

