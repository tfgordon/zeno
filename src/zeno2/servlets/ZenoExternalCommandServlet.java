package zeno2.servlets;

import com.oreilly.servlet.MultipartRequest;
import com.oreilly.servlet.LocaleNegotiator;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.PrintWriter;

import java.util.Date;
import java.util.Iterator;
//import java.util.ArrayList;
//import java.util.Enumeration;
//import java.util.List;
//import java.util.Locale;


//import javax.servlet.RequestDispatcher;
//import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServlet;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.UnavailableException;

import zeno2.kernel.Constants;
import zeno2.kernel.Factory;
import zeno2.kernel.Monitor;
import zeno2.servlets.extcmdaction.Command;
import zeno2.servlets.extcmdaction.AddArticleCommand;
import zeno2.servlets.extcmdaction.AddAttachmentCommand;
import zeno2.servlets.extcmdaction.AddXLinkCommand;
import zeno2.servlets.extcmdaction.GetArticleIdCommand;
import zeno2.servlets.extcmdaction.GetAttachmentCommand;
import zeno2.servlets.extcmdaction.GetSectionIdCommand;
import zeno2.util.ZenoUtilities;
import zeno2.velocity.util.Tools;
//import zeno2.velocity.util.ZenoBundle;

/**
 *  Commandprocessor for ZENO commands outsite the ZENO html interface
 *
 *@author     <a href="mailto:lothar.oppor@ais.fhg.de">Lothar Oppor</a>
 *@version    $zenoVersion$ 2002-05-17
 */

public class ZenoExternalCommandServlet extends HttpServlet {

	HttpServletRequest request = null;
	MultipartRequest multi = null;
	HttpServletResponse response = null;
	HttpSession session = null;
	Monitor monitor = null;
	Factory factory = null;

	String user = "";
	String now = ZenoUtilities.getIsoString(new Date());
	PrintWriter out = null;

	/**
	 *  Description of the Field
	 */
	public final static String[] ERRORS = {
			"0 ok",
			"1 invalid authentication",
			"2 unknown action: ",
			"3 section id missing",
			"4 invalid section id",
			"5 section not found",
			"6 no permission",
			"7 unexpected exception",
			"8 article attribut errors",
			"9 title required",
			"10 file too big",
			"11 article id missing",
			"12 invalid article id",
			"13 article not found",
			"14 file is empty",
			"15 invalid title",
			"16 article not found",
			"17 attachmentId or title parameter required",
			"18 error: ",
			"19 invalid attachment id"
			};


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
		monitor = (Monitor)
				getServletContext().getAttribute(Constants.MONITOR_KEY);

		try {
			user = getParam("userid");
			String passwd = getParam("passwd");
			String action = getParam("action");
//System.out.println("ZenoExternalCommandServlet.doGet.user="+user
//+", passwd="+passwd+", action="+action
//+", sessId="+session.getId()+", factory="+factory);

			clientStamp();
			
			//********************* getAttachment ****************************
			if (action.equalsIgnoreCase("getAttachment")) {
//System.out.println("ZenoExternalCommandServlet.doGet.action="+action);
				GetAttachmentCommand c = new GetAttachmentCommand(request, response, monitor);
				c.exec();
				return;
			}
			
			response.setContentType("text/plain");
			out = new PrintWriter(response.getWriter());

			if (!authorized(user, passwd)) {
				out.println(ERRORS[1]);
				out.close();
				factory = null;
				return;
			}
			//********************* addArticle ********************************
			if (action.equalsIgnoreCase("addArticle")) {
				if (getParam("title").equals("")) {
					out.println(ERRORS[9]);
					out.println("title");
					out.close();
					return;
				}
				Command c = new AddArticleCommand(request, factory);
				Iterator result = c.exec();
				while (result.hasNext()) {
					out.println(result.next());
				}
			}
			//********************* addXLink ********************************
			else if (action.equalsIgnoreCase("addXLink")) {

				Command c = new AddXLinkCommand(request, factory);
				Iterator result = c.exec();
				while (result.hasNext()) {
					out.println(result.next());
				}
			}
			//********************* getArticleId ****************************
			else if (action.equalsIgnoreCase("getArticleId")) {

				Command c = new GetArticleIdCommand(request, factory);
				Iterator result = c.exec();
				while (result.hasNext()) {
					out.println(result.next());
				}
			}
			//********************* getSectionId ****************************
			else if (action.equalsIgnoreCase("getSectionId")) {

				Command c = new GetSectionIdCommand(request, factory);
				Iterator result = c.exec();
				while (result.hasNext()) {
					out.println(result.next());
				}
			}
			else {
				out.println(ERRORS[2] + action);
			}
			//out.println("ipNr=" + request.getRemoteAddr());
			out.close();
			factory = null;
		}
		catch (Exception e) {
			System.out.println("ZenoExternalCommandServlet.doGet.error=" + e);
			log("ZenoExternalCommandServlet Forwarding exception: " + e);
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
		monitor = (Monitor)
				getServletContext().getAttribute(Constants.MONITOR_KEY);
		if (!req.getContentType().startsWith("multipart")) {
			doGet(req, res);
		}
		this.request = req;
		this.response = res;
		this.session = req.getSession();
		response.setContentType("text/plain");
		out = new PrintWriter(response.getWriter());
		try {
			//*********************************************************************
			// in /etc/my.cnf (the mysql configuration file) in Section [mysqld]
			//max_allowed_packet has to be set to "mb"M !!!
			// for instance:
			// set-variable   = max_allowed_packet=16M
			//*********************************************************************
			String megaBytes = monitor.getProperty("zenoMaxUploadFileSize", "16");
			int mb = (new Tools()).toInt(megaBytes);
			multi = new MultipartRequest(request, System.getProperty("java.io.tmpdir"),
					mb * 1024 * 1024);
		}
		catch (Exception e) {
			// ***************************************************************
			// This does not work because supposedly the "new MultipartRequest"
			// corrupts the socket connection!!!
			// ***************************************************************
			out.println(ERRORS[10]);
			System.out.println("ZenoExternalCommandServlet.doPost.error="
					 + e.toString());
			return;
		}
		try {
			user = getMultiParam("userid");
			String passwd = getMultiParam("passwd");
			String action = getMultiParam("action");

			clientStamp();

			System.out.println("ZenoExternalCommandServlet.doPost.user=" + user
					 + ", passwd=" + passwd + ", action=" + action
					 + ", session=" + session.getId() + ", factory=" + factory);
			if (!authorized(user, passwd)) {
				out.println(ERRORS[1]);
				out.close();
				factory = null;
				System.out.println("ZenoExternalCommandServlet.doPost.error***=" + ERRORS[1]);
				return;
			}
			//********************* addAttachment ****************************
			if (action.equalsIgnoreCase("addAttachment")) {
				System.out.println("ZenoExternalCommandServlet.doPost.action=" + action);
				Command c = new AddAttachmentCommand(multi, factory);
				System.out.println("ZenoExternalCommandServlet.doPost.c=" + c);
				Iterator result = c.exec();
				System.out.println("ZenoExternalCommandServlet.doPost.result=" + result);
				int nr = 0;
				while (result.hasNext()) {
					System.out.println("ZenoExternalCommandServlet.doPost.result.nr="
							 + Integer.toString(nr++));
					out.println(result.next());
				}
			}
			else {
				out.println(ERRORS[2] + action);
				System.out.println("ZenoExternalCommandServlet.doPost.error***=" + ERRORS[2]);
			}
			out.close();
			factory = null;
		}
		catch (Exception e) {
			System.out.println("ZenoExternalCommandServlet.doPost.error=" + e);
			log("ZenoExternalCommandServlet Forwarding exception: " + e);
			throw new UnavailableException(e.getMessage());
		}
	}


	/**
	 *  Gets the MultiParam attribute of the ZenoExternalCommandServlet object
	 *
	 *@param  key  Description of Parameter
	 *@return      The MultiParam value
	 */
	private String getMultiParam(String key) {
		String result = multi.getParameter(key);
		if (result == null) {
			result = "";
		}
		return result;
	}


	/**
	 *  Gets the Param attribute of the ZenoExternalCommandServlet object
	 *
	 *@param  key  Description of Parameter
	 *@return      The Param value
	 */
	private String getParam(String key) {
		String result = request.getParameter(key);
		if (result == null) {
			result = "";
		}
		return result;
	}


	/**
	 *  Cecks, if user and password are registered
	 *
	 *@param  user    String candidate
	 *@param  passwd  String password 
	 *@return         true, if user and password are registered
	 */
	private boolean authorized(String user, String passwd) {
		//monitor = (Monitor)
		//	getServletContext().getAttribute(Constants.MONITOR_KEY);
		String ipNr = request.getRemoteAddr();
		ipNr = "(ipaddr=" + ipNr + ")";
		if ((user.equals("")) || (passwd.equals(""))) {
			return false;
		}
		try {
			factory = monitor.login(user, passwd, ipNr);
			//factory = monitor.login(user, passwd);
		}
		catch (Exception e) {
			System.out.println("ZenoExternalCommandServlet.authorized.error=" + e
					 + ", ipNr=" + ipNr);
			//out.println(e);
			//out.println(ipNr);
		}
		return (factory != null);
	}


	/**
	 *  Description of the Method
	 */
	private void clientStamp() {
		now = ZenoUtilities.getIsoString(new Date());
		System.out.println("--->" + now
				 + ", user=" + user
				 + ", host=" + request.getRemoteHost() + ", IP=" + request.getRemoteAddr()
				 + ", session=" + request.getSession().getId()
				 + ", remoteUser=" + request.getRemoteUser()
				 + " (ExternalCommand)");
	}

}

