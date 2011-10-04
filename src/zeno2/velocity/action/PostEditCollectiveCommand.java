package zeno2.velocity.action;


import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

import javax.servlet.http.HttpServlet; 
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.velocity.context.Context;
import org.apache.velocity.app.tools.*;
import org.apache.velocity.servlet.VelocityServlet;

import zeno2.kernel.Constants;
import zeno2.kernel.Collective ;
import zeno2.kernel.Community;
import zeno2.kernel.Factory;
import zeno2.kernel.Monitor;
import zeno2.kernel.NoPermissionException;
import zeno2.kernel.Principal;
import zeno2.kernel.NameInUseException;
import zeno2.kernel.NotFoundException;
import zeno2.kernel.ZenoException;
import zeno2.util.PassGen;
import zeno2.util.ZenoUtilities;
import zeno2.util.CriterionParser;
import zeno2.velocity.util.Errors;
import zeno2.velocity.util.Tools;

/**
 *  Handles Zeno Principal management
 *
 *@author     <a href="mailto:hans-eckehard.grossr@ais.fraunhofer.de">Hans-Eckehard Gross</a>
 *@version    2.0.2, 2001-12-16
 */
public class PostEditCollectiveCommand extends AdrbookCommand {
	
	private final String adrbook = "adrbook/editAdrbook.vm";
	private final String presentCommunity = "adrbook/community/editCommunity.vm";
	private final String presentCollective = "adrbook/collective/editCollective.vm";
	private final String newCollective = "adrbook/collective/editCollectiveNew.vm";
	private final String enterPwd = "adrbook/enterPassword.vm";
	
	
	public PostEditCollectiveCommand(HttpServletRequest req, HttpServletResponse resp) {
		super(req, resp);
	}
	
	public String exec(Context ctx) throws Exception {

		Factory factory = (Factory) ctx.get(Constants.FACTORY_KEY);
		Principal principal = null;
		List containers = Collections.EMPTY_LIST;
		String cid = null;
		Community community = null;
		String mode = request.getParameter("mode");
		String id = request.getParameter("id");
		String identifier = request.getParameter("identifier");
		if (identifier != null) 
			identifier = identifier.trim().toLowerCase();
		String name = request.getParameter("name");
		name = (new Tools()).filterQuote(name);
		String email = request.getParameter("email");
		if (email != null) 
			email = email.trim();
		
		
		String template = adrbook;
		ctx.put("zcss", "/zeno/css/ss1.css"); 
		ctx.put("comment", "");
		ctx.put("containers", containers);
		
		ctx.put("id",identifier);
		ctx.put("name", name);
		ctx.put("email", email);
		ctx.put("mode", mode);
		
		if (id != null && !id.equals("") && !mode.equals("new_user")) {
			try {
				principal = (Principal)factory.loadPrincipal(id);
				ctx.put("principal", principal);
				ctx.put("id", id);
			} catch(NotFoundException e) {
				System.out.println(e);
				errors.addError("adrbook.not_found");
			}
		}
		
		if (request.getParameter("ok") != null) {
			
			if (mode.equals("new_collective")) {
			//******************** apply_as ***********************
				
				template = newCollective;
				
				cid = request.getParameter("cid");
				community = (Community)factory.loadPrincipal(cid);
				ctx.put("community", community);
				ctx.put("cid", cid);
		
				if (!checkIdentifier(identifier)) {
					errors.addError("adrbook.bad_identifier");
					ctx.put("identifier", "");
				}
				
				String pwd = request.getParameter("password_new");
				String pwdAgain = request.getParameter("password_again");
				if(pwd.equals(""))
					errors.addError("adrbook.invalid_password");
				if (!pwd.equals(pwdAgain)) {
					errors.addError("adrbook.wrong_repeated_password");
				} 
				
				if (errors.isEmpty()) {
					
					try {
						principal = 
							factory.createCollective(cid, identifier, name, pwd);
						Enumeration parnames = request.getParameterNames();
						while(parnames.hasMoreElements()){
							String parname = (String)parnames.nextElement();
							if (parname.startsWith("prop.")) {
								String value = request.getParameter(parname);
								setProperty(principal, cid, parname, value);
							}
						}
						
						ctx.put("principal", community);
						ctx.put("id", cid);
						ctx.put("maxPresent", new Integer(maxPresent));
						return presentCommunity;
								
					} catch(NoPermissionException e) {
						errors.addError("adrbook.no_permission");
					} catch(NameInUseException e) {
						errors.addError("adrbook.name_in_use");
						ctx.put("identifier", "");
					} 
				}
				
				if (!errors.isEmpty()) {
					ctx.put(Constants.ERRORS, errors.getErrors());
					ctx.put("propnames", propnames);
				}
				return template;
				
			} else if (mode.equals("update_collective")) {
			//******************** update_user ***********************
				
				try {
					template = presentCollective;
					principal.setName(name);
					principal.setEmail(email);
					String membership = request.getParameter("membership");
					if (!CriterionParser.checkBrackets(membership)) 
						errors.addError("adrbook.unbalanced_brackets_for_membership");
						
					Enumeration parnames = request.getParameterNames();
					while(parnames.hasMoreElements()){
						String parname = (String)parnames.nextElement();
						if (parname.startsWith("prop.")) {
							String value = request.getParameter(parname);
							 setProperty(principal, "", parname, value);
						}
					}
						
					//new property
					String propname = request.getParameter("propname");	
					String propvalue = request.getParameter("propvalue");
					cid = request.getParameter("cid");
					if (propvalue != null && !"".equals(propvalue)) 
						principal.setProperty(cid, propname, propvalue);
					principal.save();
					if (errors.isEmpty()) {
						((Collective)principal).setMembershipCriterion(membership);
						ctx.put("principal", principal);
					} else {
						ctx.put("propnames", propnames);
						ctx.put("name", principal.getName());
						ctx.put("email", principal.getEmail());
						ctx.put("membership", membership);
						ctx.put("mode", mode);
						template = newCollective;
					}
						
				} catch(NoPermissionException e) {
						//ctx.put("error", "adrbook.no_permission");
						errors.addError("adrbook.no_permission");
				}
			
				if (!errors.isEmpty()) {
					ctx.put(Constants.ERRORS, errors.getErrors());
				}
				return template;
				
			} else if (mode.equals("change_pwd")) {
			//******************** change_pwd ***********************
			
				ctx.put("mode", mode);
				ctx.put("action", "posteditCollective");
				String oldpwd = request.getParameter("password_old");
				if (!principal.checkPassword(oldpwd)) {
					errors.addError("adrbook.invalid_password");
					ctx.put(Constants.ERRORS, errors.getErrors());
					return enterPwd;
				}
			
				String pwd = request.getParameter("password_new");
				String pwdAgain = request.getParameter("password_again");
				if(pwd.equals("")) {
					errors.addError("adrbook.illegal_password");
					ctx.put(Constants.ERRORS, errors.getErrors());
					return enterPwd;
				}
					
				if (!pwd.equals(pwdAgain)) {
					errors.addError("adrbook.wrong_repeated_password");
					ctx.put(Constants.ERRORS, errors.getErrors());
					return enterPwd;
				}
				 
				try {
					principal.setPassword(pwd);
					return presentCollective;
				} catch(NoPermissionException e) {
					System.out.println(e);
					errors.addError("adrbook.no_permission");
					ctx.put(Constants.ERRORS, errors.getErrors());
					return presentCollective;
				}
				
			} else if (mode.equals("leave_community")) {
			//******************** leave_community ***********************
				String[] cids = 
						request.getParameterValues("leave_community");
				for (int i = 0; i< cids.length; i++) {
					try {
						principal.leaveCommunity(cids[i]);
					} catch(NoPermissionException e) {
						if (e.getMessage().equals("lastAdmin"))
							errors.addError("last_admin_for " + cids[i]);	
					}
				}
				if (!errors.isEmpty())
					ctx.put(Constants.ERRORS, errors.getErrors());
				try {	
					principal = factory.loadPrincipal(id);
				} catch(NotFoundException e) {
					//principal completely erased from system
					System.out.println("posteditPrincipal: erased " + id);
					
					ctx.put("id", "");
					return adrbook;
					
				}
				return presentCollective;
		
			} else if (mode.equals("leave_group")) {
			//******************** leave_group ***********************
				
				String[] gids = 
						request.getParameterValues("leave_group");
				for (int i = 0; i< gids.length; i++) {
					try {
						principal.leaveGroup(gids[i]);
					} catch(NoPermissionException e) {
						errors.addError(e.getMessage());	
					}
				}
				if (!errors.isEmpty())
					ctx.put(Constants.ERRORS, errors.getErrors());
				return presentCollective;
			}
		
		 
		} else {
		//******************** cancel ***********************
			
				
			if (mode.equals("new_collective")) {
				cid = request.getParameter("cid");
				community = (Community)factory.loadPrincipal(cid);
				ctx.put("principal", community);
				ctx.put("community", community);
				ctx.put("id", cid);
				ctx.put("cid", cid);
				ctx.put("maxPresent", new Integer(maxPresent));
				return presentCommunity;
			
			} else {
				ctx.put("principal", principal);
				template = presentCollective;
			}
		}
		return template;
	}
	
	//----------------------------------------------------------------
	
	
	protected void setProperty(Principal principal, 
								String community, String parname, String parvalue) {
		try {
			int index = parname.indexOf(".", 5);
			String cid = community.equals("") ? parname.substring(5, index) : community;
			String propname = parname.substring(index + 1);
			if (parvalue != null && !"".equals(parvalue.trim())) 
				principal.setProperty(cid, propname, parvalue);
			else 
				principal.removeProperty(cid, propname);
		} catch(ZenoException e) {
			System.out.println(e);
		}
	}
	

	protected boolean badEmail(String email) {
		return ((email == null) || email.equals("") 
			|| (email.indexOf("@") < 0));
	}
	
	
}
		
