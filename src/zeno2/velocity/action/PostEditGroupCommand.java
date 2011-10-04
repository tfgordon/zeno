 package zeno2.velocity.action;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServlet; 
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.velocity.context.Context;
import org.apache.velocity.app.tools.*;
import org.apache.velocity.servlet.VelocityServlet;

import zeno2.kernel.Community;
import zeno2.kernel.Constants;
import zeno2.kernel.Factory;
import zeno2.kernel.Group;
import zeno2.kernel.NoPermissionException;
import zeno2.kernel.Constants;
import zeno2.kernel.NameInUseException;
import zeno2.kernel.NotFoundException;
import zeno2.kernel.Principal;
import zeno2.kernel.ZenoException;
import zeno2.velocity.util.Errors;
import zeno2.velocity.util.Tools;


public class PostEditGroupCommand extends AdrbookCommand {
	//PostEditPrincipalCommand {
	
	private final String adrbook = "adrbook/editAdrbook.vm";
	private final String presentCommunity = "adrbook/community/editCommunity.vm";
	private final String presentGroup = "adrbook/group/editGroup.vm";
	private final String newGroup = "adrbook/group/editGroupNew.vm";
	
		
	public PostEditGroupCommand(HttpServletRequest req, HttpServletResponse resp) {
		super(req, resp);
	}
	
	public String exec(Context ctx) throws Exception {

		Factory factory = (Factory) ctx.get(Constants.FACTORY_KEY);
		Group group = null;
		List members = null;
		List containers = Collections.EMPTY_LIST;
		
		String mode = request.getParameter("mode");
		String cid = request.getParameter("cid");
		String id = request.getParameter("id");
		String identifier = request.getParameter("identifier");
		if (identifier == null) 
			identifier = "";
		else
			identifier = identifier.trim().toLowerCase();
		String name = request.getParameter("name");
		name = (new Tools()).filterQuote(name).trim();
		String description = request.getParameter("description");
		
		ctx.put("zcss", "/zeno/css/ss1.css"); 
		ctx.put("id", id);
		ctx.put("comment", "");
		ctx.put("containers", containers);
		
		if (id != null & !id.equals("")) {
			try {
				group = (Group)factory.loadPrincipal(id);
				ctx.put("principal", group);
				ctx.put("group", group);
			} catch(NotFoundException e) {
				System.out.println(e);
				errors.addError("adrbook.not_found");
				ctx.put(Constants.ERRORS, errors.getErrors());
			}
		}
		
		if (request.getParameter("ok") != null) {
		
			if (mode.equals("new_group")) {
			//************************ new_group *********************
				if (!checkIdentifier(identifier)) {
					errors.addError("adrbook.bad_identifier");
					ctx.put("cid",cid);
					ctx.put("name", name);
					ctx.put("description", description);
					ctx.put("mode", mode);
					ctx.put(Constants.ERRORS, errors.getErrors());
					return newGroup;
				}
				
				try {
					group = factory.createGroup(cid, identifier, name);
					group.setDescription(description);
					group.save();
					ctx.put("principal", group);
					ctx.put("group", group);
					ctx.put("id", identifier);
					return presentGroup;
				} catch(NoPermissionException e) {
					errors.addError("adrbook.no_permission");
					ctx.put(Constants.ERRORS, errors.getErrors());
					return adrbook;
				} catch(NameInUseException e) {
					errors.addError("adrbook.name_in_use");
					ctx.put("cid", cid);
					ctx.put("name", name);
					ctx.put("description", description);
					ctx.put("mode", mode);
					ctx.put(Constants.ERRORS, errors.getErrors());
					return newGroup;
				}
					
			
			} else if (mode.equals("update_group")) {
			//************************ update_group *********************
			
				try {
					group.setName(name);
					group.setDescription(description);
					group.save();
					members = getMemberPrincipals(factory, group); 
					ctx.put("group", group);
					ctx.put("members", members);
					return presentGroup;
				} catch(NoPermissionException e) {
					errors.addError("adrbook.no_permission");
					ctx.put(Constants.ERRORS, errors.getErrors());
					//ctx.put("error", "adrbook.no_permission");
					return presentGroup;
				}
				
			
			} else if (mode.equals("add_member")) {
				
				try {
					String[] prinselected = request.getParameterValues("prinselected");
					group = (Group)factory.loadPrincipal(id);
					addPrincipalsToGroup(group, prinselected);
					members = getMemberPrincipals(factory, group);
					ctx.put("group", group);
					ctx.put("members", members);
					return presentGroup;
				} catch(NoPermissionException e) {
					members = getMemberPrincipals(factory, group);
					errors.addError("adrbook.no_permission");
					ctx.put("group", group);
					ctx.put("members", members);
					ctx.put(Constants.ERRORS, errors.getErrors());
					return presentGroup;
				}	
					
			} else if (mode.equals("remove_member")) {
			//************************ remove_member *********************
				
				try {
					String[] prinselected = 
						request.getParameterValues("prinselected");
					group = (Group)factory.loadPrincipal(id);
					removePrincipalsFromGroup(group, prinselected);
					members = getMemberPrincipals(factory, group);
					ctx.put("group", group);
					ctx.put("members", members);
					return presentGroup;
				} catch(NoPermissionException e) {
					members = getMemberPrincipals(factory, group);
					errors.addError("adrbook.no_permission");
					ctx.put("group", group);
					ctx.put("members", members);
					ctx.put(Constants.ERRORS, errors.getErrors());
					return presentGroup;
				}	
							
					
			} else
				return adrbook;
		
		} else {
		//************************ cancel *********************
		
			if (mode.equals("new_group")) {
				Community community = (Community)factory.loadPrincipal(cid);
				ctx.put("principal", community);
				ctx.put("community", community);
				ctx.put("id", cid);
				ctx.put("cid", cid);
				ctx.put("maxPresent", new Integer(maxPresent));
				return presentCommunity;
			} else {
				group = (Group)factory.loadPrincipal(id);
				members = getMemberPrincipals(factory, group);
				ctx.put("principal", group); 
				ctx.put("group", group);
				ctx.put("members", members);
				return presentGroup;
			}
		}
		
	}
	
	//----------------------------------------------------------------
	
	
	
	private void addPrincipalsToGroup(Group group, String[] ids) 
			throws NoPermissionException {
		if (ids == null) return;
		for (int i=0; i<ids.length; i++) {
			try {
				group.addMember(ids[i]);
				group.save();
			} catch(NoPermissionException e) {
				throw e;
			} catch(ZenoException e) {
				System.out.println(e);
			}
		}
	}
	
	private void removePrincipalsFromGroup(Group group, String[] ids)
			throws NoPermissionException {
		if (ids == null) return;
		try {
			for (int i=0; i<ids.length; i++) {
				group.deleteMember(ids[i]);
			}
			group.save();
		} catch(NoPermissionException e) {
			throw e;	
		} catch(ZenoException e) {
			System.out.println(e);
		}
	}
	
}
		
