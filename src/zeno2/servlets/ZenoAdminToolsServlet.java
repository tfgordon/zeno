package zeno2.servlets;

import com.oreilly.servlet.LocaleNegotiator;

import java.net.URLDecoder;
import java.io.IOException;
import java.io.PrintWriter;

//import java.util.ArrayList;
//import java.util.Enumeration;
//import java.util.List;
import java.util.Locale;

//import javax.servlet.RequestDispatcher;
//import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import zeno2.db.Upgrader;
import zeno2.kernel.Constants;
import zeno2.kernel.ZenoException;
import zeno2.velocity.util.ZenoBundle;

/**
 *  Toolbox Servlet to handle Zeno Session Plugin Attributes, above all the
 *  Session Queues for the Zeno Plugins.
 *
 *@author     <a href="mailto:lothar.oppor@ais.fhg.de">Lothar Oppor</a>
 *@version    $zenoVersion$ 2002-04-03
 */

public class ZenoAdminToolsServlet extends HttpServlet {

	HttpServletRequest request = null;
	HttpServletResponse response = null;
	HttpSession session = null;
	String message = "";
	String ok = "";
	ZenoBundle bundle = null;


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
		this.request = req;
		this.response = res;
		this.session = req.getSession();
		String action = req.getParameter("action");
		if (action == null || action.equals("")) {
			action = "noAction";
		}
		ok = " not yet realized";
		System.out.println("ZenoAdminToolsServlet.doGet.sessId=" + session.getId());
		PrintWriter out = null;
		try {
//********************* upgrade ********************************
			if (action.equals("upgrade")) {
				String url = "/zeno/upgrade.html";
				String urlEncode = response.encodeRedirectURL(url);
				response.sendRedirect(urlEncode);
				return;
			}
//********************* noAction ********************************
			else {
				response.setContentType("text/plain");
				out = new PrintWriter(response.getWriter());
				if (action.equals("noAction")) {
					ok = "no action parameter found";
				}
				else {
					ok = action + ok;
				}
				out.println(ok);
			}
		}
		catch (Exception e) {
			out.println(e);
			System.out.println("ZenoAdminToolsServlet.doGet.error=" + e);
			log("Forwarding exception: " + e);
			throw new UnavailableException(e.getMessage());
		}
	}


	/**
	 *  Description of the Method
	 *
	 *@param  req                   Description of Parameter
	 *@param  res                   Description of Parameter
	 *@exception  ServletException  Description of Exception
	 *@exception  IOException       Description of Exception
	 */
	public void doPost(HttpServletRequest req, HttpServletResponse res)
			 throws ServletException, IOException {
		this.request = req;
		this.response = res;
		this.session = req.getSession();
		String action = req.getParameter("action");
		if (action == null || action.equals("")) {
			action = "noAction";
		}
		ok = " not yet realized";
		System.out.println("ZenoAdminToolsServlet.doPost.sessId=" + session.getId());
		PrintWriter out = null;
		try {
			response.setContentType("text/plain");
			out = new PrintWriter(response.getWriter());
//********************* upgrade ********************************
			if (action.equals("upgrade")) {
				UpgradeZenoSystem(out);
				return;
			}
//********************* noAction ********************************
			else if (action.equals("noAction")) {
				ok = "no action parameter found";
			}
			else {
				ok = action + ok;
			}
			out.println(ok);
		}
		catch (Exception e) {
			out.println(e);
			System.out.println("ZenoAdminToolsServlet.doPost.error=" + e);
			log("Forwarding exception: " + e);
			throw new UnavailableException(e.getMessage());
		}
	}


	/**
	 *  instanciates and executes the ZenoUpgrader
	 *
	 *@param  out  Description of Parameter
	 */
	private void UpgradeZenoSystem(PrintWriter out) {

		String path = "/WEB-INF/conf/zeno.properties";
		path = getServletContext().getRealPath(path);
		String passwd = request.getParameter("passwd");
		String target = request.getParameter("target");
		try {
			Upgrader upgrader = new Upgrader(path, out);
			upgrader.upgrade(target, passwd);
		}
		catch (ZenoException e) {
			System.out.println("ZenoAdminToolsServlet.UpgradeZenoSystem.error=" + e);
			out.println("Upgrader:" + e);
		}
		//ok = " no upgrader present";
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
			System.out.println("ZenoAdminToolsServlet.loadBundle.error=" + e);
			return null;
		}
	}

}


