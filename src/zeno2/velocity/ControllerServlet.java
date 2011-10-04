package zeno2.velocity;

/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999 The Apache Software Foundation.  All rights
 * reserved.
 *
 */

// Negotiator stuff
import com.oreilly.servlet.LocaleNegotiator;

// Multipart stuff
import com.oreilly.servlet.MultipartRequest;

// Java stuff
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.FileInputStream;

// Servlet stuff
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.UnavailableException;

// Velocity stuff
import org.apache.velocity.context.Context;
import org.apache.velocity.Template;
import org.apache.velocity.servlet.VelocityServlet;

import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.exception.MethodInvocationException;

// Zeno stuff
import zeno2.kernel.Constants;
import zeno2.kernel.Factory;
import zeno2.kernel.Monitor;
import zeno2.kernel.NoPermissionException;
import zeno2.kernel.Principal;
import zeno2.kernel.ZenoException;
import zeno2.kernel.ZenoNotFoundException;
import zeno2.kernel.ZenoResource;
import zeno2.util.ZenoUtilities;
import zeno2.velocity.action.Command;
import zeno2.velocity.action.ConfirmCommand;
import zeno2.velocity.action.DeleteCommand;
import zeno2.velocity.action.EditAdrbookCommand;
import zeno2.velocity.action.EditArticleCommand;
import zeno2.velocity.action.EditAttachmentCommand;
import zeno2.velocity.action.EditCollectiveCommand;
import zeno2.velocity.action.EditCommunityCommand;
import zeno2.velocity.action.EditGroupCommand;
import zeno2.velocity.action.EditJournalCommand;
import zeno2.velocity.action.EditLinkCommand;
import zeno2.velocity.action.EditMarkedCommand;
import zeno2.velocity.action.EditPrincipalCommand;
import zeno2.velocity.action.EditRolesCommand;
import zeno2.velocity.action.EditTopicCommand;
import zeno2.velocity.action.LoginCommand;
import zeno2.velocity.action.LogoutCommand;
import zeno2.velocity.action.OpenAttachmentCommand;
import zeno2.velocity.action.PostConfirmCommand;
import zeno2.velocity.action.PostEditArticleCommand;
import zeno2.velocity.action.PostEditAttachmentCommand;
import zeno2.velocity.action.PostEditCollectiveCommand;
import zeno2.velocity.action.PostEditCommunityCommand;
import zeno2.velocity.action.PostEditGroupCommand;
import zeno2.velocity.action.PostEditJournalCommand;
import zeno2.velocity.action.PostEditLinkCommand;
import zeno2.velocity.action.PostEditMarkedCommand;
import zeno2.velocity.action.PostEditPrincipalCommand;
import zeno2.velocity.action.PostEditTopicCommand;
import zeno2.velocity.action.PostLoginCommand;
import zeno2.velocity.action.PostSubscriptionCommand;
import zeno2.velocity.action.ShowCommand;
import zeno2.velocity.action.SubscriptionCommand;
import zeno2.velocity.action.UnderConstructionCommand;
import zeno2.velocity.action.GlobalSearchCommand;
import zeno2.velocity.action.PostGlobalSearchCommand;
import zeno2.velocity.util.Tools;
import zeno2.velocity.util.ZenoBundle;
import zeno2.velocity.util.ZenoEncoder;

/**
 *  Main entry point into the forum application. All requests are made to this
 *  servlet.
 *
 *@author     <a href="mailto:daveb@miceda-data.com">Dave Bryson</a>
 *@created    August 31, 2001
 *@version    $Revision: 1.4 $ $Id: ControllerServlet.java,v 1.4 2001/05/19
 *      17:26:35 geirm Exp $
 */
public class ControllerServlet extends VelocityServlet {
	private static String ERR_MSG_TAG = "zeno_current_error_msg";


	/**
	 *  VelocityServlet handles most of the Servlet issues. By extending it, you
	 *  need to just implement the handleRequest method.
	 *
	 *@param  ctx   Description of Parameter
	 *@param  req   Description of Parameter
	 *@param  resp  Description of Parameter
	 *@return       the template
	 */

	public Template handleRequest(HttpServletRequest req,
			HttpServletResponse resp, Context ctx) {
		Template template = null;
		String templateName = null;

		HttpSession sess = req.getSession();
		sess.setAttribute(ERR_MSG_TAG, "-");
//System.out.println("zeno.Controler.Servlet.sessId="+sess.getId());
//System.out.println("zeno.Controler.Servlet.rhost="+req.getRemoteHost());
		String fatalError = (String) getServletContext().getAttribute(Constants.ERRORS);
		if( (fatalError != null) && !fatalError.equals("")) {
			sess.setAttribute(ERR_MSG_TAG,fatalError);
			return sendError(req,resp, new UnavailableException("Bad configuration in zeno.properties"));
		}

		try {

			// handle internationalization/localization

			String bundleName = Constants.RESOURCE_NAME;
			String acceptLanguage = req.getHeader("Accept-Language");
			String acceptCharset = req.getHeader("Accept-Charset");

			LocaleNegotiator negotiator =
					new LocaleNegotiator(bundleName, acceptLanguage, acceptCharset);

			Locale locale = negotiator.getLocale();
			String charset = negotiator.getCharset();

			/*
			 * ResourceBundle bundle = negotiator.getBundle();// may be null
			 * if (bundle == null) {
			 * bundle = ResourceBundle.getBundle(bundleName, Locale.getDefault());
			 * }
			 */
			ZenoBundle bundle = new ZenoBundle(bundleName, negotiator.getBundle());

			resp.setContentType("text/html; charset=" + charset);
			resp.setHeader("Content-Language", locale.getLanguage());
			resp.setHeader("Vary", "Accept-Language");

			//populate the context
			ctx.put(Constants.ZENO_VERSION, "2.1.0");
			ctx.put(Constants.SERVER_NAME, req.getServerName());
			ctx.put(Constants.SERVER_PORT, Integer.toString(req.getServerPort()));
			ctx.put(Constants.RESOURCE_KEY, bundle);
			Monitor monitor = (Monitor)
					getServletContext().getAttribute(Constants.MONITOR_KEY);
			ctx.put(Constants.MONITOR_KEY, monitor);

			// if postlogin the keys are empty
			if ((Factory) sess.getAttribute(Constants.FACTORY_KEY) != null) {
				ctx.put(Constants.FACTORY_KEY, sess.getAttribute(Constants.FACTORY_KEY));
				ctx.put(Constants.USER_KEY, sess.getAttribute(Constants.USER_KEY));
			}

			// init errors object
			ctx.remove(Constants.ERRORS);
			ctx.put(Constants.ERRORS, new ArrayList());

			// path of notices directory
			String noticePath = getServletContext().getRealPath("notices");
			ctx.put("notipath", noticePath);
			// path of stylesheet directory
			String cssPath = getServletContext().getRealPath("css");
			ctx.put("csspath", cssPath);
			ctx.put("servl", req.getRequestURI());
			sess.setAttribute("forumServlet", req.getRequestURI());
			//System.out.println("ControllerServlet.handleRequest.noticePath="
			//	+noticePath);
			ctx.put("mailer", monitor.getProperty("zenoMailUser", ""));

			// Process the command
			templateName = processRequest(req, resp, ctx);

			// Get the template
			if (templateName != null) {
				template = getTemplate(templateName);
			}

			// ********** additional model parts and features *****************
			ctx.put("tools", new Tools());
			ArrayList bag = (ArrayList) sess.getAttribute(Constants.BAG_KEY);
			if (bag == null) {
				bag = new ArrayList();
			}
			ctx.put("bag", bag);
			ZenoEncoder ze = new ZenoEncoder();
			ctx.put("ze", ze);
			ctx.put("zenoHotline", monitor.getProperty("zenoHotlineEmailAddress"
					, "no_hotline"));
			ctx.put("aboutZeno", monitor.getProperty("aboutZeno"
					, "http://www.ais.fraunhofer.de/MS/results/zeno2"));
			ctx.put("resp", resp);
			String lastForumUrl = (String) sess.getAttribute("lastForumUrl");
			if (lastForumUrl != null) {
				ctx.put("lastForumUrl",lastForumUrl);
			}

		}
		catch (ZenoNotFoundException znfe) {
			String err =
					"Zeno -> ControllerServlet.handleRequest() : Cannot find User "
					 + ctx.get(Constants.USER_KEY);
			sess.setAttribute(ERR_MSG_TAG, err);
			System.out.println(err);
			znfe.printStackTrace(System.out);
			return sendError(req,resp,znfe);
		}
		catch (ZenoException ze) {
			String err = "Zeno -> ControllerServlet.handleRequest() : ZenoException " + ze;
			sess.setAttribute(ERR_MSG_TAG, err);
			System.out.println(err);
			ze.printStackTrace(System.out);
			return sendError(req,resp,ze);
		}
		catch (ResourceNotFoundException rnfe) {
			String err =
					"Zeno -> ControllerServlet.handleRequest() : Cannot find template "
					 + templateName;
			sess.setAttribute(ERR_MSG_TAG, err);
			System.out.println(err);
			rnfe.printStackTrace(System.out);
			return sendError(req,resp,rnfe);
		}
		catch (ParseErrorException pee) {
			String err =
					"Zeno -> ControllerServlet.handleRequest() : Syntax error in template "
					 + templateName
					 + ":"
					 + pee;
			sess.setAttribute(ERR_MSG_TAG, err);
			System.out.println(err);
			pee.printStackTrace(System.out);
			return sendError(req,resp,pee);
		}
		catch (Exception e) {
			String err = "Zeno --> ControllerServlet.handleRequest() : " + e;
			sess.setAttribute(ERR_MSG_TAG, err);
			System.out.println(err);
			e.printStackTrace(System.out);
			return sendError(req,resp,e);
		}

		return template;
	}


	/**
	 *  Override the method from VelocityServlet to produce an intelligent message
	 *  to the browser
	 *
	 *@param  request
	 *@param  response
	 *@param  cause
	 *@exception  ServletException
	 *@exception  IOException
	 */
	protected void error(
			HttpServletRequest request,
			HttpServletResponse response,
			Exception cause)
			 throws ServletException, IOException {
		HttpSession sess = request.getSession();
		String err = (String) sess.getAttribute(ERR_MSG_TAG);

		StringBuffer html = new StringBuffer();
		html.append("<html>");
		html.append("<body bgcolor=\"#ffffff\">");
		html.append("<h2>Zeno : Error processing the request</h2>");
		html.append("<br><br>There was a problem in the request.");
		html.append("<br><br>The relevant error is :<br>");
		html.append(err);
		html.append("<br><br><br>");
		html.append("The error occurred at :<br><br>");

		StringWriter sw = new StringWriter();
		cause.printStackTrace(new PrintWriter(sw));

		html.append(sw.toString());
		html.append("</body>");
		html.append("</html>");
		response.getOutputStream().print(html.toString());
	}


	protected Template sendError(
			HttpServletRequest request,
			HttpServletResponse response,
			Exception cause) {
		try {
			error(request,response,cause);
		}
		catch (Exception e ) {}
		return null;
	}


	/**
	 *  lets override the loadConfiguration() so we can do some fancier setup of
	 *  the template path
	 *
	 *@param  config                     Description of Parameter
	 *@return                            Description of the Returned Value
	 *@exception  IOException            Description of Exception
	 *@exception  FileNotFoundException  Description of Exception
	 */

	protected Properties loadConfiguration(ServletConfig config)
			 throws IOException, FileNotFoundException {

		String propsFile = config.getInitParameter(INIT_PROPS_KEY);

		// now convert to an absolute path relative to the webapp root
		// This will work in a decently implemented servlet 2.2
		// container like Tomcat.

		if (propsFile != null) {
			String realPath = getServletContext().getRealPath(propsFile);

			if (realPath != null) {
				propsFile = realPath;
			}
		}

		Properties p = new Properties();
		p.load(new FileInputStream(propsFile));

		// now, lets get the two elements we care about, the
		// template path and the log, and fix those from relative
		// to the webapp root, to absolute on the filesystem, which is
		// what velocity needs

		String path = p.getProperty("file.resource.loader.path");

		if (path != null) {
			path = getServletContext().getRealPath(path);
			p.setProperty("file.resource.loader.path", path);
		}

		path = p.getProperty("runtime.log");

		if (path != null) {
			path = getServletContext().getRealPath(path);
			p.setProperty("runtime.log", path);
		}

		return p;
	}


	/**
	 *  Process the request and execute the command. Uses a command pattern
	 *
	 *@param  req            HTTP request
	 *@param  resp           HTTP response
	 *@param  context        Velocity context
	 *@return                the name of the template to use
	 *@exception  Exception  No Exception is thrown explicitely,
	 *            inner Exceptions such as NullpointerException may happen
	 */
	private String processRequest(
			HttpServletRequest req,
			HttpServletResponse resp,
			Context context)
			 throws Exception {
		Command c = null;
		String template = null;
		String name = req.getParameter("action");
		String view = req.getParameter("view");
		String contentType = req.getContentType();
		//System.out.println("ControlerServlet.processRequest.name=" + name
		//		 + ", contentType=" + contentType);

		if (name == null || name.length() == 0) {
			if ((contentType != null) &&
					contentType.startsWith("multipart/form-data")) {
				//********************************************************************
				//MultipartRequest multi = new MultipartRequest(req, "/tmp", 50 * 1024);
				//name = multi.getParameter("action");
				// this does not work, because you cannot get MultipartRequest twice!!!
				//*********************************************************************
				name = "posteditattachment";
			}
			else {
				name = "login";
			}
		}

		// first login if not
		HttpSession sess = req.getSession();
//System.out.println("ControlerServlet.processRequest.sess=" + sess);
//System.out.println("ControlerServlet.processRequest.sess="
// + sess.getId() + ", factory="
//+ sess.getAttribute(Constants.FACTORY_KEY)
//+ ", action=" + name);
		if ((sess.getAttribute(Constants.FACTORY_KEY) == null) &&
		//!name.equalsIgnoreCase("login") &&
				!name.equalsIgnoreCase("postlogin")) {
			Monitor monitor =
					(Monitor) getServletContext().getAttribute(Constants.MONITOR_KEY);
			if (monitor != null) {
				context.put(Constants.MONITOR_KEY, monitor);

				// ************** Test here if guestable **************************
				if (!guest_is_allowed(req, context)) {
					c = new LoginCommand(req, resp);
					template = c.exec(context);
					if (urlRememberable(name, view)) {
						sess.setAttribute(Constants.LAST_URL,
								req.getRequestURI() + "?" + req.getQueryString());
						System.out.println("ControlerServlet.processRequest.last_url="
								 + sess.getAttribute(Constants.LAST_URL));
					}
					return template;
				}
			}
			else {
				context.put("errortitle", "error.severe_error");
				context.put("addon", "error.no_database");
				return "error.vm";
			}
			System.out.println("ControlerServlet.processRequest.factory="
					 + sess.getAttribute(Constants.FACTORY_KEY)
					 + ", action=" + name);

		}
		/*
		 * else
		 */
		if (name.equalsIgnoreCase("delete")) {// zeno,lo,01-09-06
			c = new DeleteCommand(req, resp);
		}
		else if (name.equalsIgnoreCase("login")) {
			c = new LoginCommand(req, resp);
		}
		else if (name.equalsIgnoreCase("logout")) {// zeno,lo,01-09-14
			c = new LogoutCommand(req, resp);
		}
		else if (name.equalsIgnoreCase("editarticle")) {// zeno,lo,01-09-06
			//System.out.println("ControllerServlet.processRequest.name=" + name);
			c = new EditArticleCommand(req, resp);
			//System.out.println("ControllerServlet.processRequest.c=" + c);
		}
		else if (name.equalsIgnoreCase("editattachment")) {// zeno,lo,01-09-23
			c = new EditAttachmentCommand(req, resp);
			template = c.exec(context);
		}
		else if (name.equalsIgnoreCase("editjournal")) {// zeno,lo,01-09-18
			c = new EditJournalCommand(req, resp);
		}
		else if (name.equalsIgnoreCase("editlink")) {// zeno,lo,01-09-25
			//System.out.println("ControllerServlet.processRequest.name=" + name);
			c = new EditLinkCommand(req, resp);
			//System.out.println("ControllerServlet.processRequest.c=" + c);
		}
		else if (name.equalsIgnoreCase("edittopic")) {// zeno,lo,02-06-10
			//System.out.println("ControllerServlet.processRequest.name=" + name);
			//c = new EditTopicCommand(req, resp);
			c = new EditArticleCommand(req, resp);
			//System.out.println("ControllerServlet.processRequest.c=" + c);
		}
		else if (name.equalsIgnoreCase("openattachment")) {
			c = new OpenAttachmentCommand(req, resp);
		}
		else if (name.equalsIgnoreCase("postlogin")) {
			c = new PostLoginCommand(req, resp);
		}
		else if (name.equalsIgnoreCase("posteditarticle")) {// zeno,lo,01-09-07
			c = new PostEditArticleCommand(req, resp);
		}
		else if (name.equalsIgnoreCase("posteditattachment")) {// zeno,lo,01-09-23
			c = new PostEditAttachmentCommand(req, resp);
		}
		else if (name.equalsIgnoreCase("posteditjournal")) {// zeno,lo,01-09-07
			c = new PostEditJournalCommand(req, resp);
		}
		else if (name.equalsIgnoreCase("posteditlink")) {// zeno,lo,01-09-25
			c = new PostEditLinkCommand(req, resp);
		}
		else if (name.equalsIgnoreCase("postEditTopic")) {// zeno,lo,02-06-10
			//c = new PostEditTopicCommand(req, resp);
			c = new PostEditArticleCommand(req, resp);
		}
		else if (name.equalsIgnoreCase("show")) {// zeno,lo,02-08-12
			//System.out.println("ControllerServlet.processRequest.name=" + name);
			c = new ShowCommand(req, resp);
			//System.out.println("ControllerServlet.processRequest.c=" + c);
		}
		//--------------------heg----------------------------------------
		else if (name.equalsIgnoreCase("editRoles")) {// zeno,lo,01-09-25
			c = new EditRolesCommand(req, resp);
		}
		else if (name.equalsIgnoreCase("editAdrbook")) {// zeno,lo,01-09-25
			c = new EditAdrbookCommand(req, resp);
		}
		else if (name.equalsIgnoreCase("editPrincipal")) {// zeno,lo,01-09-25
			c = new EditPrincipalCommand(req, resp);
		}
		else if (name.equalsIgnoreCase("editCollective")) {// zeno,lo,01-09-25
			c = new EditCollectiveCommand(req, resp);
		}
		else if (name.equalsIgnoreCase("editGroup")) {// zeno,lo,01-09-25
			c = new EditGroupCommand(req, resp);
		}
		else if (name.equalsIgnoreCase("editCommunity")) {// zeno,lo,01-09-25
			c = new EditCommunityCommand(req, resp);
		}
		else if (name.equalsIgnoreCase("posteditPrincipal")) {// zeno,lo,01-09-25
			c = new PostEditPrincipalCommand(req, resp);
		}
		else if (name.equalsIgnoreCase("posteditCollective")) {// zeno,lo,01-09-25
			c = new PostEditCollectiveCommand(req, resp);
		}
		else if (name.equalsIgnoreCase("posteditGroup")) {// zeno,lo,01-09-25
			c = new PostEditGroupCommand(req, resp);
		}
		else if (name.equalsIgnoreCase("posteditCommunity")) {// zeno,lo,01-09-25
			c = new PostEditCommunityCommand(req, resp);
		}
		else if (name.equalsIgnoreCase("editMarked")) {// zeno,lo,01-09-25
			c = new EditMarkedCommand(req, resp);
		}
		else if (name.equalsIgnoreCase("postEditMarked")) {// zeno,lo,01-09-25
			c = new PostEditMarkedCommand(req, resp);
		}
		else if (name.equalsIgnoreCase("confirm")) {// zeno,lo,01-09-25
			c = new ConfirmCommand(req, resp);
		}
		else if (name.equalsIgnoreCase("postconfirm")) {// zeno,lo,01-09-25
			c = new PostConfirmCommand(req, resp);
		}

		//--------------------end heg----------------------------------------
		//--------------------andreas----------------------------------------
		else if (name.equalsIgnoreCase("subscription")) {// zeno,lo,01-09-25
			c = new SubscriptionCommand(req, resp);
		}
		else if (name.equalsIgnoreCase("postSubscription")) {// zeno,lo,01-09-25
			c = new PostSubscriptionCommand(req, resp);
		}
		else if (name.equalsIgnoreCase("search")) {// zeno,lo,01-09-25
			c = new GlobalSearchCommand(req, resp);
		}
		else if (name.equalsIgnoreCase("postsearch")) {// zeno,lo,01-09-25
			c = new PostGlobalSearchCommand(req, resp);
		}
		//--------------------end andreas heg---------------------------------
		else {
			//System.out.println("ControlerServlet.processRequest.name(false!!)=" + name);
			c = new UnderConstructionCommand(req, resp);
		}
		if (urlRememberable(name, view)) {
			sess.setAttribute(Constants.LAST_URL,
					req.getRequestURI() + "?" + req.getQueryString());
//System.out.println("ControlerServlet.processRequest.last_url="
//	+ sess.getAttribute(Constants.LAST_URL));
		}
		if (forumUrlRememberable(name, view)) {
			sess.setAttribute("lastForumUrl",
					req.getRequestURI() + "?" + req.getQueryString());
		}
		//-------------------end test lothar ---------------------------------
		//System.out.println("ControlerServlet.processRequest.template=" + template
		//		 + ", c=" + c);
		template = c.exec(context);
//		String navmode = (String) context.get("navmode");
//		if ((navmode == null) || navmode.equals("")) {
//			context.put("navmode", "struct");
//		}
		return template;
	}


	/**
	 *  test if URL points to Article, Topic or Section
	 *
	 *@param  action  String
	 *@param  view    String
	 *@return         true or false
	 */
	private boolean urlRememberable(String action, String view) {
		return (action.startsWith("editA") || action.startsWith("editT")
				|| action.startsWith("editJ"))
				&& (view != null) && (!view.equals("")) && (!view.equals("edit"))
				&& (!view.equals("bag")) && (!view.equals("trash"))
				&& (!view.equals("add")) && (!view.equals("new"))
				&& (!view.equals("respond")) && (!view.equals("subscribe"))
				&& (!view.equals("noteDown"));
	}


	/**
	 *  tests if URL points to inner of forum
	 *
	 *@param  action  String
	 *@param  view    String
	 *@return         true or false
	 */
	private boolean forumUrlRememberable(String action, String view) {
		return (action.startsWith("editAr") || action.startsWith("editJ")
				 || (action.startsWith("subs")) || (action.startsWith("editrol")))
				 && ((view == null) || ((!view.equals("edit"))
				 && (!view.equals("bag")) && (!view.equals("trash"))
				 && (!view.equals("add")) && (!view.equals("new"))
				 && (!view.equals("newWindow")) && (!view.equals("noteDown"))
				 && (!view.equals("respond")) && (!view.equals("subscribe"))));
	}


	/**
	 *  Description of the Method
	 *
	 *@param  req  Description of Parameter
	 *@param  ctx  Description of Parameter
	 *@return      Description of the Returned Value
	 */
	private boolean guest_is_allowed(HttpServletRequest req, Context ctx) {

		Principal user = null;
		HttpSession sess = req.getSession();
		Monitor monitor = (Monitor)
				getServletContext().getAttribute(Constants.MONITOR_KEY);
		if (monitor == null) {
			System.out.println("ControllerServlet.guest_is_allowed: Monitor is null");
		}
		Factory factory = null;
		try {
			factory = monitor.login("", "");// guest
			sess.setAttribute(Constants.FACTORY_KEY, factory);
			ctx.put(Constants.FACTORY_KEY, factory);
//System.out.println("ControllerServlet.guest_is_allowed.factory=" + factory);
			user = factory.getUser();
			ctx.put(Constants.USER_KEY, user);
			sess.setAttribute(Constants.USER_KEY, user);
//System.out.println("ControllerServlet.guest_is_allowed.user=" + user);

			String id = req.getParameter("id");
//System.out.println("ControllerServlet.guest_is_allowed.id=" + id);
			int idNr = (new Tools()).toInt(id);
			if (idNr == -1) {//must be action=show!!!, Checking later on
				return true;
			}
//System.out.println("ControllerServlet.guest_is_allowed.idNr=" + Integer.toString(idNr));
			ZenoResource zr = factory.loadResource(idNr);
			String dummy = zr.getNote();// to force "NoPermissionException
		}
		catch (NoPermissionException e) {
			return false;
		}
		catch (Exception e) {
			System.out.println("ControlerServlet.guest_is_allowed.error=" + e);
			return false;
		}
		if (factory != null) {
			System.out.println("*+* " + ZenoUtilities.getIsoString(new Date())
					 + ", user=" + user.getId()
					 + ", host=" + req.getRemoteHost() + ", IP=" + req.getRemoteAddr()
					 + ", session=" + req.getSession().getId()
					 + ", remoteUser=" + req.getRemoteUser()
					 + " (Guest is allowed)");
			return true;
		}

		return false;
	}
}

