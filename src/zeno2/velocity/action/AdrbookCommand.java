package zeno2.velocity.action;

/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999 The Apache Software Foundation.  All rights
 * reserved.
 *
 */

// Java Stuff

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

// Velocity stuff
import org.apache.velocity.context.Context;
import org.apache.velocity.servlet.VelocityServlet;

// Zeno stuff

import zeno2.kernel.Community;
import zeno2.kernel.ZenoException;
import zeno2.kernel.Factory;
import zeno2.kernel.Group;

import zeno2.kernel.Principal;
import zeno2.util.ZenoUtilities;
import zeno2.velocity.util.Errors;


/**
 *  Base class for adrbook commands
 *
 *@author     <a href="mailto:gross@ais.fraunhofer.de">Hans Eckehard Gross</a>
 *@version 2.0.22002-08-31
 */
public abstract class AdrbookCommand extends Command {
	
	protected String idChars = "abcdefghijklmnopqrstuvwxyz0123456789_";
	protected List propnames = new ArrayList();
	protected int maxPresent = 60;
	protected Errors errors = new Errors();

	/**
	 *  Constructor
	 *
	 *@param  req   Description of Parameter
	 *@param  resp  Description of Parameter
	 */
	
	public AdrbookCommand(HttpServletRequest req, HttpServletResponse resp) {
		super(req, resp);
		propnames.add("organization");
		propnames.add("orgRole");
		propnames.add("description");
		propnames.add("homePage");
		
	}


	/**
	 *  Implemented by classes that extends this class
	 *
	 *@param  context        Description of Parameter
	 *@return                the name of the template to execute
	 *@exception  Exception  Description of Exception
	 */
	public abstract String exec(Context context) throws Exception;


	
	protected boolean checkIdentifier(String identifier) {
		if ((identifier ==null) || identifier.equals(""))
			return false;
		for (int i=0; i<identifier.length(); i++) {
			if (idChars.indexOf(identifier.charAt(i))<0) {
				System.out.println("bad " + i);
				return false;
			}
		}
		return true;
	}
	
	
	protected List getContainers(Factory factory, Principal principal) {
		try {
			return principal.getGroups(true);
		} catch(ZenoException e) {
			System.out.println(e);
			return Collections.EMPTY_LIST;
		}
	}
	
	protected List getAdminPrincipals(Factory factory, Community community) {
		try {
			return community.getAdmins();
		} catch(ZenoException e) {
			System.out.println(e);
			return Collections.EMPTY_LIST;
		}
	}
	
	
	protected List getMemberPrincipals(Factory factory, Community community) {
		try {
			return  community.getMembers(false);
		} catch(ZenoException e) {
			System.out.println(e);
			return Collections.EMPTY_LIST;
		}
	}
	
	
	protected List getMemberPrincipals(Factory factory, Group group) {
		try {
			return group.getMembers(false, false);
		} catch(ZenoException e) {
			System.out.println(e);
			return Collections.EMPTY_LIST;
		}
	}
	
	protected String getEmailAddresses (List principals) {
		
		Set addresses = new HashSet();
		Iterator mit = principals.iterator();
		while (mit.hasNext()) {
			Principal principal = (Principal)mit.next();
			try {
				String email = principal.getEmail();
				if (!email.equals("")) 
					addresses.add(email);
			} catch(ZenoException e) {
				System.out.println(e);
			}
		}
		StringBuffer buf = new StringBuffer();
		Iterator it = addresses.iterator();
		while(it.hasNext()) {
			if (buf.length() > 0) 
				buf.append(",");
			buf.append((String)it.next());
		}
		return buf.toString();
	}
	
	/**
	* 
	* Replaces the $ variables in the notice file
	* by the values of fillins and sends it via email
	* to the new user
	* 
	*@param  notice   String, path of the notice template file
	*@param  fillins  String[7] containing:<br>
	* 0. $adminCn      Name of the administrator <br>
	* 1. $adminEmail   email address of the zenoHotline <br>
	* 2. $uid          Name of the new user <br>
	* 3. $password     Password of the new user <br>
	* 4. $adrbookurl   URL of the zeno addressbook <br>
	* 5. $privacyurl   URL of zeno privacy statement <br>
	* 5. $uEmail       email address of the new user <br>
	*/
	protected void sendNotice(String mailhost, String notice, String[] fillins) {
		StringBuffer buf = new StringBuffer();
		try {
			BufferedReader f = new BufferedReader(new FileReader(notice));
			String line = f.readLine();
			while (line != null) {
				buf.append(line);
				buf.append("\n");
				line = f.readLine();
			}
			replace(buf, "$adminCn", fillins[0]);
			replace(buf, "$adminEmail", fillins[1]);
			replace(buf, "$uid", fillins[2]);
			replace(buf, "$password", fillins[3]);
			replace(buf, "$adrbookurl", fillins[4]);
			replace(buf, "$privacyurl", fillins[5]);
			String[] from = {fillins[1],"Zeno Support"};
			ZenoUtilities.sendNotice(mailhost,fillins[6],from,
			"Zeno Registration", buf.toString());
		}
		catch (Exception e) {
			System.out.println("PostEditPrincipal.sendNotice.error="+e);
		}
	}
	
	protected void replace(StringBuffer buf, String s1, String s2) {
		int i1 = buf.toString().indexOf(s1);
		if (i1 >=0) buf.replace(i1,i1+s1.length(),s2);
	}
	

	

}

