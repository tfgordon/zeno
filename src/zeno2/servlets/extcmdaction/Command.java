package zeno2.servlets.extcmdaction;

/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999 The Apache Software Foundation.  All rights
 * reserved.
 *
 */
import com.oreilly.servlet.MultipartRequest;

// Java Stuff
import java.util.Iterator;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

//import zeno2.kernel.Article;
import zeno2.kernel.Factory;
//import zeno2.kernel.Journal;
//import zeno2.kernel.;
import zeno2.servlets.ZenoExternalCommandServlet;

/**
 *  Base class for external zeno commands
 *
 *@author     <a href="mailto:lothar.oppor@ais.fhg.de">Lothar Oppor</a>
 *@created    2002-05-27
 *@version    $zenoVersion $
 */
public abstract class Command {

	/**
	 *  Description of the Field
	 */
	protected MultipartRequest multi = null;
	/**
	 *  Description of the Field
	 */
	protected HttpServletRequest request = null;
	/**
	 *  Description of the Field
	 */
	protected Factory factory = null;
	/**
	 *  Description of the Field
	 */
	protected String forumServlet = "/zeno/forum";



	/**
	 *  Constructor
	 *
	 *@param  multi  Description of Parameter
	 *@param  fact   Description of Parameter
	 */
	public Command(MultipartRequest multi, Factory fact) {
		System.out.println("EXTCommand.exec  start");
		this.multi = multi;
		this.factory = fact;
		System.out.println("EXTCommand.exec.fact=" + fact);
		//this.request = (HttpServletRequest) multi;
		//this.forumServlet = (String) request.getSession().getAttribute("forumServlet");
	}


	/**
	 *  Constructor
	 *
	 *@param  req   Description of Parameter
	 *@param  fact  Description of Parameter
	 */
	public Command(HttpServletRequest req, Factory fact) {
		this.request = req;
		this.factory = fact;
		//this.forumServlet = (String) req.getSession().getAttribute("forumServlet");
	}


	/**
	 *  Implemented by classes that extends this class
	 *
	 *@return    the name of the template to execute
	 */
	public abstract Iterator exec();


	/**
	 *  Gets the Param attribute of the Command object
	 *
	 *@param  key  Description of Parameter
	 *@return      The Param value
	 */
	protected String getParam(String key) {
		String result = request.getParameter(key);
		if (result == null) {
			result = "";
		}
		return result;
	}


	/**
	 *  Gets the MultiParam attribute of the Command object
	 *
	 *@param  key  Description of Parameter
	 *@return      The MultiParam value
	 */
	protected String getMultiParam(String key) {
		String result = multi.getParameter(key);
		if (result == null) {
			result = "";
		}
		return result;
	}

}

