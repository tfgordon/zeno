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
import zeno2.kernel.Constants;
import zeno2.kernel.Factory;
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
public class EditCollectiveCommand extends AdrbookCommand {

	private final String adrbook = "adrbook/editAdrbook.vm";
	private final String presentCollective = "adrbook/collective/editCollective.vm";
	private final String newCollective = "adrbook/collective/editCollectiveNew.vm";
	private final String enterPwd = "adrbook/enterPassword.vm";
	private final String membership = "adrbook/community/editMembership.vm";
	
	
	public EditCollectiveCommand(HttpServletRequest req, HttpServletResponse resp) {
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
		
		String mode = request.getParameter("mode");
			
		if (mode.equals("update_collective")) {
			ctx.put("propnames", propnames);
			ctx.put("id", principal.getId());
			ctx.put("name", principal.getName());
			ctx.put("email", principal.getEmail());
			ctx.put("membership", ((Collective)principal).getMembershipCriterion());
			ctx.put("mode", mode);
			return newCollective;
			
		} else if (mode.equals("delete")) {
			containers = getContainers(factory, principal);
			ctx.put("containers", containers);
			ctx.put("action", "posteditCollective");
			ctx.put("mode", mode);
			if (containers.isEmpty())
				ctx.put("comment", "adrbook.no_member_of");
			return membership;
		
		} else if (mode.equals("change_pwd")) {
			ctx.put("mode", mode);
			ctx.put("action", "posteditCollective");
			return enterPwd;
			
		} else if (mode.equals("member_of")) {
		
			containers = getContainers(factory, principal);
			ctx.put("containers", containers);
			ctx.put("mode", mode);
			if (containers.isEmpty())
				ctx.put("comment", "adrbook.no_member_of");
			return presentCollective;
			
		} else {
			ctx.put("mode", mode);
			return adrbook;
		}
		
	}
	

	
}
			
