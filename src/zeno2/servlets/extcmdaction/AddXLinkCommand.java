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
import zeno2.kernel.ZenoResource;
import zeno2.servlets.ZenoExternalCommandServlet;

import zeno2.util.ZenoUtilities;
import zeno2.velocity.util.Tools;

/**
 *  Base class for external zeno commands
 *
 *@author     <a href="mailto:lothar.oppor@ais.fhg.de">Lothar Oppor</a>
 *@created    2002-08-12
 *@version    $zenoVersion $
 */
public class AddXLinkCommand extends Command {


	/**
	 *  Constructor
	 *
	 *@param  req   Description of Parameter
	 *@param  fact  Description of Parameter
	 */
	public AddXLinkCommand(HttpServletRequest req, Factory fact) {
		super(req, fact);
	}


	/**
	 *  Implemented by classes that extends this class
	 *
	 *@param  context        Description of Parameter
	 *@return                the name of the template to execute
	 */
	public Iterator exec() {
		List result = new ArrayList();
		String id = getParam("id");
//System.out.println("AddXLinkCommand.exec.id="+id);
		if (id.equals("null")) {
			result.add(ZenoExternalCommandServlet.ERRORS[3]);
			result.add("id");
			return result.iterator();
		}
		int idNr = (new Tools()).toInt(id);
		String reference = getParam("ref");
		String name = getParam("name");
//System.out.println("AddXLinkCommand.exec.reference="+reference
//+", name="+name);
		if (name.equals("")) name = reference;
		try {
			ZenoResource resource = factory.loadResource(idNr);
			int p = reference.indexOf(":");
			String xType = (p > 0) ? reference.substring(0,p) : "http";
//System.out.println("AddXLinkCommand.exec.xType="+xType
//+", reference="+reference+", name="+name);
			resource.addXLink(xType,reference,name);
			result.add(ZenoExternalCommandServlet.ERRORS[0]);
		}
		catch (ClassCastException e) {
			result.add(ZenoExternalCommandServlet.ERRORS[4]);
			result.add("id -->"+e.toString());
		}
		catch (NotFoundException e) {
			result.add(ZenoExternalCommandServlet.ERRORS[5]);
			result.add("id -->"+e.toString());
		}
		catch (NoPermissionException e) {
			result.add(ZenoExternalCommandServlet.ERRORS[6]);
			result.add("id -->"+e.toString());
		}
		catch (Exception e) {
			result.add(ZenoExternalCommandServlet.ERRORS[7]);
			result.add("id -->"+e.toString());
		}
		
		return result.iterator();
	}

}

