package zeno2.velocity.action;

/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999 The Apache Software Foundation.  All rights
 * reserved.
 *
 */

import java.util.*;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.context.Context;
import org.apache.velocity.app.tools.*;
import org.apache.velocity.servlet.VelocityServlet;

import zeno2.kernel.*;

/**
 *  Handles zeno journal views
 *
 *@author     <a href="mailto:lothar.oppor@ais.fhg.de">Lothar Oppor</a>
 *@version 2.0.2, 2001-09-02
 */
public class UnderConstructionCommand extends Command {

	/**
	 *  Constructor for the ListCommand object
	 *
	 *@param  req   Description of Parameter
	 *@param  resp  Description of Parameter
	 *@since
	 */
	public UnderConstructionCommand(HttpServletRequest req, HttpServletResponse resp) {
		super(req, resp);
	}


	/**
	 *  Get id and view parameters, get the journal(id), put id and journal into
	 *  the context, and select the appropriate template
	 *
	 *@param  ctx            Velocity Context
	 *@return                the appropriate template file name
	 *@exception  Exception  Description of Exception
	 *@since
	 */
	public String exec(Context ctx) throws Exception {

		String id = request.getParameter("id");
		if (id == null) {
			id = " ";
		}
		String action = request.getParameter("action");
		if (action == null) {
			action = " ";
		}
		String view = request.getParameter("view");
		if (view == null) {
			view = " ";
		}
		ctx.put("id", id);
		ctx.put("action", action);
		ctx.put("view", view);
		ctx.put("errortitle", "error.under_construction");
		ctx.put("addon", "error.no_such_action");
		return "error.vm";
	}
}

