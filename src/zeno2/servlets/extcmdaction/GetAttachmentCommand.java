package zeno2.servlets.extcmdaction;

/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999 The Apache Software Foundation.  All rights
 * reserved.
 *
 */

// Java Stuff
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.UnavailableException;

import zeno2.kernel.Article;
//import zeno2.kernel.ArticleCollection;
import zeno2.kernel.Attachment;
import zeno2.kernel.Factory;
import zeno2.kernel.Monitor;
//import zeno2.kernel.NotFoundException;
import zeno2.servlets.ZenoExternalCommandServlet;

//import zeno2.util.ZenoUtilities;
import zeno2.velocity.util.Tools;

/**
 *  Base class for external zeno commands
 *
 *@author     <a href="mailto:lothar.oppor@ais.fhg.de">Lothar Oppor</a>
 * 2002-12-03
 *@version    $zenoVersion $
 */
public class GetAttachmentCommand {
	
	HttpServletRequest req;
	HttpServletResponse res;
	Monitor mon;
	Factory factory;

	/**
	 *  Constructor
	 *
	 *@param  req   HttpRequest
	 *@param  res   HttpResponse
	 *@param  mon   zeno2.kernel.Monitor
	 */
	public GetAttachmentCommand(HttpServletRequest req, HttpServletResponse res
						, Monitor mon) {
		super();
		this.req = req;
		this.res = res;
		this.mon = mon;
	}


	/**
	 *  sends the attachment file or an error message as response
	 *
	 */
	public void exec() throws UnavailableException {
		List result = new ArrayList();
		result.add("***** getAttachment *****      not yet realized");
		String attachmentId = getParam("attachmentId");
		String title = getParam("title");
		String articleId = getParam("articleId");
		String user = getParam("userid");
		String passwd = getParam("passwd");
//System.out.println("GetAttachmentCommand.exec.id="+id+", title="+title
//+",articleId="+articleId);

		if (!authorized(user, passwd)) {
			signalError(ZenoExternalCommandServlet.ERRORS[1]);
			return;
		}
		if ((articleId == null) || articleId.equals("")) {
			signalError(ZenoExternalCommandServlet.ERRORS[11]);
		}
		else if (((title == null) || title.equals(""))
					&& ((attachmentId == null) || attachmentId.equals(""))) {
				signalError(ZenoExternalCommandServlet.ERRORS[17]);
			}
		else {
			int ArticleIdNr = (new Tools()).toInt(articleId);
			if (ArticleIdNr < 0) {
				signalError(ZenoExternalCommandServlet.ERRORS[12]);
				return;
			}
			Attachment attachment = null;
			try{
				if ((attachmentId != null) && !attachmentId.equals("")) {
					int attachmentIdNr = (new Tools()).toInt(attachmentId);
					if (attachmentIdNr < 0) {
						signalError(ZenoExternalCommandServlet.ERRORS[19]);
						return;
					}
					attachment =
						((Article) factory.loadResource(ArticleIdNr)).getAttachment(attachmentIdNr);
				}
				else {
					attachment =
						((Article) factory.loadResource(ArticleIdNr)).getAttachment(title);
				}
			}
			catch (Exception e) {
				signalError(ZenoExternalCommandServlet.ERRORS[18]+e.getMessage());
			}
			String ok = getContent(attachment);
			if (ok != null) {
				signalError(ZenoExternalCommandServlet.ERRORS[18]+ok);
			}
		}
	}

	/**
	 *  checks, if the candidate is a registered zeno user
	 *
	 *@param  user    String candidate with valid password
	 *@param  passwd  String password
	 *@return         true, if user and password are registered
	 */
	private boolean authorized(String user, String passwd) {
		String ipNr = req.getRemoteAddr();
		ipNr = "(ipaddr=" + ipNr + ")";
		if ((user.equals("")) || (passwd.equals(""))) {
			return false;
		}
		try {
			factory = mon.login(user, passwd, ipNr);
		}
		catch (Exception e) {
			System.out.println("ZenoExternalCommandServlet.authorized.error=" + e
					 + ", ipNr=" + ipNr);
		}
		return (factory != null);
	}

	/**
	 *  Gets the Param attribute of the Command object
	 *
	 *@param  key  String
	 *@return      The Param value
	 */
	private String getParam(String key) {
		String result = req.getParameter(key);
		if (result == null) {
			result = "";
		}
		return result;
	}


	private void signalError(String message) throws UnavailableException {
		res.setContentType("text/plain");
		try {
			PrintWriter out = new PrintWriter(res.getWriter());
			out.println(message);
			factory = null;
			out.close();
		}
		catch (Exception e) {
			System.out.println("ZenoExternalCommandServlet.doGet.error=" + e);
			throw new UnavailableException(e.getMessage());
		}
	}
	
	private String getContent(Attachment attachment) {
		try {
			String mimeType = attachment.getMimeType();
			res.setContentType(mimeType);
			BufferedOutputStream out = new BufferedOutputStream(res.getOutputStream());
			BufferedInputStream in = new BufferedInputStream(attachment.getContents());

			byte buffer[] = new byte[2048];
			int n = -1;
			while ((n = in.read(buffer)) > 0) {
				out.write(buffer, 0, n);
				out.flush();
			}
			in.close();
			return null;
		}
		catch (Exception e) {
			System.out.println("OpenAttachmentCommand.exec.error:"
					 + e.getMessage());
			return(ZenoExternalCommandServlet.ERRORS[18]+e.getMessage());
		}
	}


}

