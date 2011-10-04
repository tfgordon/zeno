package zeno2.velocity.action;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

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
//import zeno2.kernel.Group;
import zeno2.kernel.NoPermissionException;
import zeno2.kernel.Constants;
import zeno2.kernel.NameInUseException;
import zeno2.kernel.NotFoundException;
import zeno2.kernel.Principal;
import zeno2.kernel.ZenoException;
import zeno2.util.CriterionParser;
import zeno2.velocity.util.Errors;
import zeno2.velocity.util.Tools;


public class PostEditCommunityCommand extends AdrbookCommand {
	
	private final String adrbook = "adrbook/editAdrbook.vm";
	private final String presentCommunity = "adrbook/community/editCommunity.vm";
	private final String newCommunity = "adrbook/community/editCommunityNew.vm";
	private final String presentAdmins = "adrbook/editAdmins.vm";
	private final String selectForCommunity = "adrbook/community/selectForCommunity.vm";
		
	public PostEditCommunityCommand(HttpServletRequest req, HttpServletResponse resp) {
		super(req, resp);
	}
	
	public String exec(Context ctx) throws Exception {

		Factory factory = (Factory) ctx.get(Constants.FACTORY_KEY);
		List communities = null;
		Community community = null;
		List members = null;
		List admins = null;
		List options = null; ;
		
		String mode = request.getParameter("mode");
		String id = request.getParameter("id");
		String identifier = request.getParameter("identifier");
		if (identifier == null) 
			identifier = "";
		else
			identifier = identifier.trim().toLowerCase();
		String name = request.getParameter("name");
		name = (new Tools()).filterQuote(name).trim();
		String description = request.getParameter("description");
		String rejection = request.getParameter("rejection");
		if (rejection != null) rejection = rejection.trim();
		String admission = request.getParameter("admission");
		if (admission != null) admission = admission.trim();

		ctx.put("zcss", "/zeno/css/ss1.css"); 
		ctx.put("id", id);
		ctx.put("comment", "");
		
		if (id != null & !id.equals("")) {
			try {
				community = (Community)factory.loadPrincipal(id);
				ctx.put("community", community);
				ctx.put("principal", community);
				ctx.put("maxPresent", new Integer(maxPresent));
			} catch(NotFoundException e) {
				System.out.println(e);
				errors.addError("adrbook.not_found");
				ctx.put(Constants.ERRORS, errors.getErrors());
			}
		}
		
		if (request.getParameter("ok") != null) {
		
			if (mode.equals("new_community")) {
			//************************ new_community *********************
				if (!checkIdentifier(identifier)) {
					errors.addError("adrbook.bad_identifier");
					ctx.put("name", name);
					ctx.put("description", description);
					ctx.put("mode", mode);
					ctx.put(Constants.ERRORS, errors.getErrors());
					return newCommunity;
				}
				
				try {
					community = factory.createCommunity(identifier, name);
					community.setDescription(description);
					community.save();
					ctx.put("principal", community);
					ctx.put("community", community);
					ctx.put("maxPresent", new Integer(maxPresent));
					ctx.put("id", identifier);
					return presentCommunity;
				} catch(NoPermissionException e) {
					errors.addError("adrbook.no_permission");
					ctx.put(Constants.ERRORS, errors.getErrors());
					return adrbook;
				} catch(NameInUseException e) {
					errors.addError("adrbook.name_in_use");
					ctx.put("name", name);
					ctx.put("description", description);
					ctx.put("mode", mode);
					ctx.put(Constants.ERRORS, errors.getErrors());
					return newCommunity;
				}
					
			
			} else if (mode.equals("update_community")) {
			//************************ update_community *********************
				try {
					community.setName(name);
					community.setDescription(description);
					community.save();
					if (!CriterionParser.checkBrackets(rejection)) 
						errors.addError("adrbook.unbalanced_brackets_for_rejection");
					if (!CriterionParser.checkBrackets(admission)) 
						errors.addError("adrbook.unbalanced_brackets_for_admission");
					if (errors.isEmpty()) {
						community.setRejectionCriterion(rejection);
						community.setAdmissionCriterion(admission);
						ctx.put("community", community);
						return presentCommunity;
					} else {
						ctx.put(Constants.ERRORS, errors.getErrors());
						ctx.put("name", community.getName());
						ctx.put("email", community.getEmail());
						ctx.put("organization", community.getOrganization());
						ctx.put("orgRole", community.getOrgRole());
						ctx.put("description", community.getDescription());
						ctx.put("rejection", rejection);
						ctx.put("admission", admission);
						ctx.put("mode", mode);
						return newCommunity;
					}
				} catch(NoPermissionException e) {
					errors.addError("adrbook.no_permission");
					ctx.put(Constants.ERRORS, errors.getErrors());
					//ctx.put("error", "adrbook.no_permission");
					return presentCommunity;
				}
				
			} else if (mode.equals("delete_community")) {
				try {
					factory.removeCommunity(id);
				return adrbook;
				} catch(NoPermissionException e) {
					errors.addError("adrbook.no_permission");
					return adrbook;
				}
				
			
			} else if (mode.equals("add_principal")) {
			//************************ add_principal *********************	
				try {
					String[] prinselected = request.getParameterValues("prinselected");
					community = (Community)factory.loadPrincipal(id);
					addPrincipalsToCommunity(community, prinselected);
					members = getMemberPrincipals(factory, community);
					ctx.put("community", community);
					ctx.put("members", members);
					return presentCommunity;
				} catch(NoPermissionException e) {
					members = getMemberPrincipals(factory, community);
					errors.addError("adrbook.no_permission");
					//ctx.put("error", "adrbook.no_permission");
					ctx.put("community", community);
					ctx.put("members", members);
					ctx.put(Constants.ERRORS, errors.getErrors());
					return presentCommunity;
				}	
					
			} else if (mode.equals("remove_principal")) {
			//************************ remove_principal *********************
				
				try {
					String[] prinselected = request.getParameterValues("prinselected");
					community = (Community)factory.loadPrincipal(id);
					removePrincipalsFromCommunity(community, prinselected);
					members = getMemberPrincipals(factory, community);
					ctx.put("community", community);
					ctx.put("members", members);
					return presentCommunity;
				} catch(NoPermissionException e) {
					//members = getMemberPrincipals(factory, community);
					if (e.getMessage().equals("lastAdmin"))
						errors.addError("adrbook.last_admin");
					ctx.put("community", community);
					//ctx.put("members", members);
					ctx.put(Constants.ERRORS, errors.getErrors());
					return presentCommunity;
				}
				
			} else if (mode.equals("update_admins")) {
			//******** remove_admins && present admin candidates **************
			
				String[] idsToRemove = request.getParameterValues("remove");
				removeAdmins(community, idsToRemove);
				String idToAdd = request.getParameter("add");
				String cid = request.getParameter("cid");
				Iterator principalsToSelect = 
					getPrincipalsToSelect(factory, cid, idToAdd, "person");
				if (principalsToSelect != null) {
					ctx.put("principals", principalsToSelect);
					ctx.put("mode", "add_admin");
					ctx.put("search", "false");
					return selectForCommunity;
				} else {
					admins = getAdminPrincipals(factory, community); 
					ctx.put("admins", admins);
					String comment = idToAdd.equals("") ? "" : "no_matches";
					ctx.put("comment", comment);
					return presentAdmins;
				}
				
			} else if (mode.equals("add_admin")) {
			//************************ add_admin *********************	
				try {
					String[] prinselected = request.getParameterValues("prinselected");
					community = (Community)factory.loadPrincipal(id);
					addAdmins(community, prinselected);
					admins = getAdminPrincipals(factory, community);
					ctx.put("admins", admins);
					return presentAdmins;
				} catch(NoPermissionException e) {
					admins = getAdminPrincipals(factory, community);
					errors.addError("adrbook.no_permission");
					//ctx.put("error", "adrbook.no_permission");
					ctx.put("admins", admins);
					ctx.put("options", options);
					ctx.put(Constants.ERRORS, errors.getErrors());
					return presentAdmins;
				}		
					
			} else {
				return adrbook;
			}
		
		} else {
			if (mode.equals("new_community"))
				return adrbook;
			else if (id.equals("system"))
				return adrbook;
			else {
				community = (Community)factory.loadPrincipal(id);
				ctx.put("community", community);
				ctx.put("maxPresent", new Integer(maxPresent));
				return presentCommunity;
			}
		}
		
	}
	
	//----------------------------------------------------------------
	
	
	private Iterator getPrincipalsToSelect(Factory factory, 
											String cid,
											String names,
											String type) {
		
		if (names == null || names.equals(""))
			return null;
		try {
			Iterator it = factory.searchPrincipals(cid, names, type);
			if (it.hasNext())
				return it;
			else
				return null;
		} catch(ZenoException e) {
			System.out.println(e);
			return null;
		}
	}	
	
	private void addPrincipalsToCommunity(Community community, String[] ids) 
			throws NoPermissionException {
		if (ids == null) return;
		try {
			for (int i=0; i<ids.length; i++) {
				community.addMember(ids[i]);
			}
			community.save();
		} catch(NoPermissionException e) {
			throw e;
		} catch(ZenoException e) {
			System.out.println(e);
		}
	}
	
	private void removePrincipalsFromCommunity(Community community, String[] ids)
			throws NoPermissionException {
		if (ids == null) return;
		try {
			for (int i=0; i<ids.length; i++) {
				community.removeMember(ids[i]);
			}
			community.save();
		} catch(NoPermissionException e) {
			throw e;	
		} catch(ZenoException e) {
			System.out.println(e);
		}
	}
	
	private void addAdmins(Community community, String[] ids) 
			throws NoPermissionException {
		if (ids == null) return;
		for (int i=0; i<ids.length; i++) {
			try {
				community.addAdmin(ids[i]);
			} catch(NoPermissionException e) {
				throw e;
			} catch(ZenoException e) {
				System.out.println(e);
			}
		}
	}
	
	
	private void removeAdmins(Community community, String[] ids) {
		
		if (ids == null) return;
		for(int i = 0; i < ids.length; i++) {
			try {
				community.removeAdmin(ids[i]);
			} catch(ZenoException e) {
				System.out.println(e);
			}
		}
	}
	
	
	
}
		
