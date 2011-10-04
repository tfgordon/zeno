package zeno2.velocity.action;

/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999 The Apache Software Foundation.  All rights
 * reserved.
 *
 */

// Java Stuff
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

// Velocity stuff
import org.apache.velocity.context.Context;
import org.apache.velocity.servlet.VelocityServlet;

import zeno2.kernel.Article;
import zeno2.kernel.Principal;
import zeno2.kernel.ZenoResource;
import zeno2.util.ZenoUtilities;
import zeno2.velocity.util.ZenoEncoder;

/**
 *  Base class for commands
 *
 *@author     <a href="mailto:oppor@ais.fraunhofer.dem">Lothar Oppor</a>
 *@version    2.0.2 2001-08-31
 */
public abstract class Command {

	protected HttpServletRequest request = null;
	protected HttpServletResponse response = null;
	protected String forumServlet = "";
	protected ZenoEncoder enc = new ZenoEncoder();


	/**
	 *  Constructor
	 *
	 *@param  req   Description of Parameter
	 *@param  resp  Description of Parameter
	 */
	public Command(HttpServletRequest req, HttpServletResponse resp) {
		this.request = req;
		this.response = resp;
	}


	/**
	 *  Implemented by classes that extends this class
	 *
	 *@param  context        Description of Parameter
	 *@return                the name of the template to execute
	 *@exception  Exception  Description of Exception
	 */
	public abstract String exec(Context context) throws Exception;


	/**
	 *  MessageOutput common to all Extensions
	 *
	 *@param  ctx        Velocity Context
	 *@param  messages   String Array containing the title and the message lines
	 *@param  errorIcon  full filename of the error icon
	 *@return            Filename of the MessageOutput Template
	 */
	protected String showMessage(Context ctx, String[] messages, String errorIcon) {
		HttpServletRequest req = (HttpServletRequest) ctx.get(VelocityServlet.REQUEST);
		HttpServletResponse res = (HttpServletResponse) ctx.get(VelocityServlet.RESPONSE);
		if ((errorIcon != null) && (!errorIcon.equals(""))) {
			ctx.put("erroricon", errorIcon);
		}
		ctx.put("errortitle", messages[0]);
		List msgs = new ArrayList();
		if (messages.length > 1) {
			for (int i = 1; i < messages.length; i++) {
				msgs.add(messages[i]);
			}
			ctx.put("msgs", msgs);
		}
		return "error.vm";
	}


	/**
	 *  MessageOutput common to all Extensions
	 *
	 *@param  ctx        Velocity Context
	 *@param  title      Title of the Message Page
	 *@param  addon      Additional Text
	 *@param  errorIcon  full filename of the error icon
	 *@return            Filename of the MessageOutput Template
	 */
	protected String showMessage(Context ctx, String title, String addon,
			String errorIcon) {
		HttpServletRequest req = (HttpServletRequest) ctx.get(VelocityServlet.REQUEST);
		HttpServletResponse res = (HttpServletResponse) ctx.get(VelocityServlet.RESPONSE);
		ctx.put("errortitle", title);
		if ((errorIcon != null) && (!errorIcon.equals(""))) {
			ctx.put("erroricon", errorIcon);
		}
		if (addon.length() < 80) {
			ctx.put("addon", addon);
		}
		else {
			ctx.put("addon", makePretty(addon));
		}
		String id = "";
		String action = "";
		String view = "";
		//System.out.println("Command.showMessage.title=" + title);
		try {
			id = req.getParameter("id");
			action = req.getParameter("action");
			view = req.getParameter("view");
		}
		catch (Exception e) {
		}
		if ((id != null) && !id.equals("")) {
			ctx.put("id", id);
			ctx.put("action", action);
			ctx.put("view", view);
		}
		//System.out.println("Command.showMessage.id=" + id);
		return "error.vm";
	}


	/**
	 *  MessageOutput common to all Extensions
	 *
	 *@param  ctx    Description of Parameter
	 *@param  title  Description of Parameter
	 *@param  addon  Description of Parameter
	 *@return        Description of the Returned Value
	 */
	protected String showMessage(Context ctx, String title, String addon) {
		return showMessage(ctx, title, addon, null);
	}


	/**
	 *  MessageOutput common to all Extensions
	 *
	 *@param  ctx    Velocity Context
	 *@param  title  Title of the Message Page
	 *@return        Filename of the MessageOutput Template
	 */
	protected String showMessage(Context ctx, String title) {
		return showMessage(ctx, title, "");
	}


	/**
	 *  MessageOutput common to all Extensions
	 *
	 *@param  ctx  Velocity Context
	 *@return      Filename of the MessageOutput Template
	 */
	protected String showMessage(Context ctx) {
		return showMessage(ctx, "Under Construction", "");
	}


	/**
	 *  Does ipt contain double quote?
	 *
	 *@param  ipt  String
	 *@return      true if ipt contains double quote
	 */
	protected boolean containsDoubleQuote(String ipt) {
		return (ipt.indexOf("\"") >= 0);
	}


	/**
	 *  Inserts newline character before each ampersand character
	 *
	 *@param  what  String original
	 *@return       String whith additional newline characters
	 */
	private String makePretty(String what) {
		StringBuffer result = new StringBuffer();
		StringTokenizer st = new StringTokenizer(what, "&");
		result.append(st.nextToken());
		while (st.hasMoreTokens()) {
			result.append("\n&");
			result.append(st.nextToken());
		}
		return result.toString();
	}

	/**
	 *  Creates a parameter String for zeno notification
	 *
	 *@param  ctx  Veleocity context
	 *@param  me   Article
	 *@return      String
	 */
	protected String makeEmailParams(
			Context ctx,
			ZenoResource me) {
		try {
			StringBuffer strb = new StringBuffer("mailto:?subject=Zeno Mail from ");
			Principal user = (Principal) ctx.get("user");
			String userName = (user != null) ? user.getName() : "guest";
			strb.append(userName);
			strb.append(":");
			strb.append(me.getTitle());
			strb.append("&body=");
			if (me instanceof Article) {
				strb.append(((Article)me).getLabel());
				strb.append(": ");
			}
			strb.append(me.getTitle());
			strb.append(" ");
			strb.append(ZenoUtilities.getIsoString(me.getCreationDate()));
			strb.append(" ");
			strb.append(me.getCreator());
			strb.append("  ");
			strb.append("http://");
			strb.append(request.getServerName());
			int port = request.getServerPort();
			if (port != 80) {
				strb.append(":");
				strb.append(Integer.toString(port));
			}
			StringBuffer url = new StringBuffer(forumServlet);
			url.append("?action=editArticle&view=print&id=");
			url.append(me.getId());
			strb.append(enc.encode(url.toString()));
//System.out.println("editArticleCommand.makeEmailParams=" + strb.toString());
			return strb.toString();
		}
		catch (Exception e) {
			System.out.println("Command.makeEmailParams.error="
					 + e.toString());
			return "";
		}
	}


}

