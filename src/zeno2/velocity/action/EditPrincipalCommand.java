package zeno2.velocity.action;


import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.velocity.context.Context;
import org.apache.velocity.app.tools.*;
import org.apache.velocity.servlet.VelocityServlet;

import zeno2.kernel.Collective;
import zeno2.kernel.Community;
import zeno2.kernel.Constants;
import zeno2.kernel.Factory;
import zeno2.kernel.Group;
import zeno2.kernel.Monitor;
import zeno2.kernel.NoPermissionException;
import zeno2.kernel.NotFoundException;
import zeno2.kernel.Principal;
import zeno2.kernel.ZenoException;
import zeno2.kernel.ZenoResource;
import zeno2.util.PassGen;
import zeno2.util.ZenoUtilities;
import zeno2.velocity.util.Errors;

/**
 *  Handles zeno principals 
 *
 *@author     <a href="mailto:eckehard.gross@ais.fraunhofer.de">Eckehard Gross</a>
 *@version    2.0.2, 2001-11-14
 */
public class EditPrincipalCommand extends AdrbookCommand {

	private final String adrbook = "adrbook/editAdrbook.vm";
	private final String presentUser = "adrbook/principal/editPrincipal.vm";
	private final String presentCollective = "adrbook/collective/editCollective.vm";
	private final String presentGroup = "adrbook/group/editGroup.vm";
	private final String presentCommunity = "adrbook/community/editCommunity.vm";
	private final String newUser = "adrbook/principal/editPrincipalNew.vm";
	private final String newGroup = "adrbook/group/editGroupNew.vm";	
	private final String newCommunity = "adrbook/community/editCommunityNew.vm";
	private final String enterPwd = "adrbook/enterPassword.vm";
	private final String membership = "adrbook/community/editMembership.vm";
	
	
	public EditPrincipalCommand(HttpServletRequest req, HttpServletResponse resp) {
		super(req, resp);
	}
	
	public String exec(Context ctx) throws Exception {

		Factory factory = (Factory) ctx.get(Constants.FACTORY_KEY);
		ZenoResource home = factory.loadResource(1);
		Principal principal = null;
		List containers = Collections.EMPTY_LIST;
		List members = Collections.EMPTY_LIST;
		ctx.put("zcss", "/zeno/css/ss1.css"); 
		
		String id = request.getParameter("id");
		if (id != null) {
			try {
				principal = factory.loadPrincipal(id);
				ctx.put("resource", home);
				ctx.put("id", id);
				ctx.put("principal", principal);
				ctx.put("containers", containers);
				ctx.put("comment", "");
			} catch(NotFoundException e) {
				errors.addError("adrbook.not_found");
			}
		}
		
		String remadr = request.getRemoteAddr();
		String remhost = request.getRemoteHost();
		
		String mode = request.getParameter("mode");
			
		if (mode.equals("present")) {
			ctx.put("mode", mode);
			if (principal == null) {
				if (!errors.isEmpty()) 
					ctx.put(Constants.ERRORS, errors.getErrors());
				return adrbook;
				
			} if (factory.hasRole("guest", principal)) {
				return showMessage(ctx, "error.no_permission",
						"adberror.not_allowed_principal");
				
			} if (id.equals("any") || id.equals("system") 
					|| !factory.hasRole("mate", principal)) {
				errors.addError("adrbook.no_permission");
				ctx.put(Constants.ERRORS, errors.getErrors());
				return adrbook;
			
			} if (principal instanceof Community) {
				ctx.put("community", principal);
				ctx.put("maxPresent", new Integer(maxPresent));
				return presentCommunity;
		
			} if (principal instanceof Group) {
				members = getMemberPrincipals(factory, (Group)principal);
				ctx.put("group", principal);
				ctx.put("members", members);
				return presentGroup;
				
			} if (principal instanceof Collective) {
				ctx.put("collective", principal);
				return presentCollective;
				
			} else {
				return presentUser;
			}
		
		} else if (mode.equals("update_user")) {
			ctx.put("propnames", propnames);
			ctx.put("id", principal.getId());
			ctx.put("name", principal.getName());
			ctx.put("email", principal.getEmail());
			ctx.put("mode", mode);
			return newUser;
			
		} else if (mode.equals("delete")) {
			containers = getContainers(factory, principal);
			ctx.put("containers", containers);
			ctx.put("action", "posteditPrincipal");
			if (containers.isEmpty())
				ctx.put("comment", "adrbook.no_member_of");
			return membership;
		
		} else if (mode.equals("change_pwd")) {
			ctx.put("mode", mode);
			ctx.put("action", "posteditPrincipal");
			return enterPwd;
		
		} else if (mode.equals("reset_pwd")) {
			ctx.put("mode", mode);
			try {
				String email = principal.getEmail();
				if (email == null || "".equals(email.trim())) {
					errors.addError("adrbook.email_required");
					ctx.put(Constants.ERRORS, errors.getErrors());
					return presentUser;
				} 
				String password = (new PassGen()).generate();
				String uri = request.getRequestURI();
				String serverName = (String) ctx.get(Constants.SERVER_NAME);
				String serverPort = (String) ctx.get(Constants.SERVER_PORT);
				if (!serverPort.equals("80")) {
					serverPort = ":"+serverPort;
				}
				else {
					serverPort = "";
				}
				String adrbookURL = "http://"+serverName+serverPort
					+uri+"?action=editAdrbook&mode=present";
				String privacyURL = 
					"http://ais.gmd.de/MS/results/zeno2/zeno2-privacy-policy.html";
				Monitor monitor = (Monitor) ctx.get(Constants.MONITOR_KEY);
				String mailhost = monitor.getProperty("zenoMailServer","no_zenoMailServer");
				String zenoHotline = monitor.getProperty("zenoHotlineEmailAddress",
						"No_zenoHotlineEmailAddress");
				Principal administrator = factory.getUser();
				String administratorName = administrator.getId() +" ("
						+ administrator.getName() +")";
				String administratorEmail = administrator.getEmail();
				String[] fillins = {
					administratorName,administratorEmail,id,password,
					adrbookURL,privacyURL,email
					};
				String noticePath = ctx.get("notipath")+"/resetPassword.txt";
				sendNotice(mailhost,noticePath,fillins);
				principal.setPassword(password);
				principal.save();
				ctx.put("username",id+" ("+email+")");
				ctx.put("passwdmsg","adrbook.password_reset");
				return presentUser;
			} catch(NoPermissionException e) {
				errors.addError("adrbook.no_permission");
				ctx.put(Constants.ERRORS, errors.getErrors());
				return presentUser;
			}
			
		} else if (mode.equals("member_of")) {
			ctx.put("mode", mode);
			containers = getContainers(factory, principal);
			ctx.put("containers", containers);
			if (containers.isEmpty())
				ctx.put("comment", "adrbook.no_member_of");
			 
			if (principal instanceof Group) {
				members = getMemberPrincipals(factory, (Group)principal);
				ctx.put("members", members);
				ctx.put("group", principal);
				return presentGroup;
			
			} else {
				return presentUser;
			}
			
					
		} else 
			return adrbook;
		
	}
	

	
}
			
