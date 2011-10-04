package zeno2.servlets.extcmdaction;

/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999 The Apache Software Foundation.  All rights
 * reserved.
 *
 */

// Java Stuff
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;

import zeno2.kernel.Article;
import zeno2.kernel.Factory;
import zeno2.kernel.Journal;
import zeno2.kernel.NoPermissionException;
import zeno2.kernel.NotFoundException;
import zeno2.kernel.ZenoException;
//import zeno2.kernel.;
import zeno2.servlets.ZenoExternalCommandServlet;

import zeno2.util.ZenoUtilities;
import zeno2.velocity.util.Tools;

/**
 *  Base class for external zeno commands
 *
 *@author     <a href="mailto:lothar.oppor@ais.fhg.de">Lothar Oppor</a>
 *@created    2002-05-27
 *@version    $zenoVersion $
 */
public class AddArticleCommand extends Command {

	String artId = "";


	/**
	 *  Constructor
	 *
	 *@param  req   Description of Parameter
	 *@param  fact  Description of Parameter
	 */
	public AddArticleCommand(HttpServletRequest req, Factory fact) {
		super(req, fact);
	}


	/**
	 *  Gets the Id attribute of the AddArticleCommand object
	 *
	 *@return    The Id value
	 */
	public String getId() {
		return artId;
	}


	/**
	 *  Implemented by classes that extends this class
	 *
	 *@return    the name of the template to execute
	 */
	public Iterator exec() {
		List result = new ArrayList();
		String id = getParam("id");
		if (id.equals("null")) {
			result.add(ZenoExternalCommandServlet.ERRORS[3]);
			result.add("id");
			return result.iterator();
		}
		int parent = (new Tools()).toInt(id);
		try {
			Journal journal = (Journal) factory.loadResource(parent);
			Article art = (Article) factory.createArticle(journal);
			setAttributes(art, journal, result);
			art.save();
			artId = Integer.toString(art.getId());
			result.add(artId);
			result.add(artUrl(artId));
		}
		catch (ClassCastException e) {
			result.add(ZenoExternalCommandServlet.ERRORS[4]);
			result.add("id -->" + e.toString());
		}
		catch (NotFoundException e) {
			result.add(ZenoExternalCommandServlet.ERRORS[5]);
			result.add("id -->" + e.toString());
		}
		catch (NoPermissionException e) {
			result.add(ZenoExternalCommandServlet.ERRORS[6]);
			result.add("id -->" + e.toString());
		}
		catch (Exception e) {
			result.add(ZenoExternalCommandServlet.ERRORS[7]);
			result.add("id -->" + e.toString());
		}

		return result.iterator();
	}


	/**
	 *  Sets the Attributes attribute of the AddArticleCommand object
	 *
	 *@param  art     The new Attributes value
	 *@param  journ   The new Attributes value
	 *@param  result  The new Attributes value
	 */
	private void setAttributes(Article art, Journal journ, List result) {
		try {
			art.setKeywords(getParam("keywords"));
			String labels = journ.getArticleLabels();
			String label = getParam("label");
			String title = (getParam("title"));
			title = (new Tools()).filterQuote(title);
			if (ZenoUtilities.contained(label, labels)) {
				art.setLabel(label);
			}
			String qualifiers = journ.getQualifiers();
			String qualifier = getParam("qualifier");
			if (ZenoUtilities.contained(qualifier, qualifiers)) {
				art.setQualifier(qualifier);
			}
			art.setNote(getParam("note"));
			art.setTitle(title);
			int rankNr = (new Tools()).toInt(getParam("rank"));
			art.setRank(rankNr);
			Date beginDate = makeDate(getParam("begindate"));
			if (beginDate != null) {
				art.setBeginDate(beginDate);
			}
			Date endDate = makeDate(getParam("enddate"));
			if (endDate != null) {
				art.setEndDate(endDate);
			}
			//art.set expirationDate ???
			if ("true".equals(getParam("notifyCreator"))) {
				art.setProperty("NotifCreator", "true");
			}
			//art.setplugins???
			result.add(ZenoExternalCommandServlet.ERRORS[0]);
		}
		catch (ZenoException e) {
			result.add(ZenoExternalCommandServlet.ERRORS[8]);
			result.add(e.toString());
		}
	}


	/**
	 *  Description of the Method
	 *
	 *@param  s  Description of Parameter
	 *@return    Description of the Returned Value
	 */
	private Date makeDate(String s) {
		Date date = null;
		if ((s != null) && !s.equals("")) {
			try {
				date = ZenoUtilities.getDateAndTimeFromIsoString(s.trim());
			}
			catch (Exception e) {
				date = null;
			}
		}
		return date;
	}


	/**
	 *  Description of the Method
	 *
	 *@param  id  Description of Parameter
	 *@return     Description of the Returned Value
	 */
	private String artUrl(String id) {
		StringBuffer strb = new StringBuffer("http://");
		strb.append(request.getServerName());
		int port = request.getServerPort();
		if (port != 80) {
			strb.append(":");
			strb.append(Integer.toString(port));
		}
		strb.append(forumServlet);
		strb.append("?action=editArticle&view=print&id=");
		strb.append(id);
		return strb.toString();
	}

}

