package zeno2.velocity.action;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
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
import zeno2.kernel.Group;
import zeno2.kernel.Monitor;
import zeno2.kernel.NoPermissionException;
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
public class EditCommunityCommand extends AdrbookCommand {

	private final String adrbook = "adrbook/editAdrbook.vm";
	private final String newUser = "adrbook/principal/editPrincipalNew.vm";
	private final String newCollective = "adrbook/collective/editCollectiveNew.vm";
	private final String newGroup = "adrbook/group/editGroupNew.vm";
	private final String presentCommunity = "adrbook/community/editCommunity.vm";
	private final String newCommunity = "adrbook/community/editCommunityNew.vm";
	private final String confirmDelete = "adrbook/confirmDelete.vm";
	private final String presentAdmins = "adrbook/editAdmins.vm";
	private final String selectForCommunity = "adrbook/community/selectForCommunity.vm";


	/**
	 *  Constructor for the EditCommunityCommand object
	 *
	 *@param  req   Description of Parameter
	 *@param  resp  Description of Parameter
	 */
	public EditCommunityCommand(HttpServletRequest req, HttpServletResponse resp) {
		super(req, resp);
	}


	/**
	 *  Description of the Method
	 *
	 *@param  ctx            Description of Parameter
	 *@return                Description of the Returned Value
	 *@exception  Exception  Description of Exception
	 */
	public String exec(Context ctx) throws Exception {

		HttpServletRequest req = (HttpServletRequest) ctx.get(VelocityServlet.REQUEST);

		Factory factory = (Factory) ctx.get(Constants.FACTORY_KEY);
		ZenoResource home = factory.loadResource(1);
		Principal principal = null;
		String cid = req.getParameter("cid");
		List admins;
		List members;
		ctx.put("zcss", "/zeno/css/ss1.css");

		String id = request.getParameter("id");
		if (id != null) {
			principal = factory.loadPrincipal(id);
			ctx.put("community", principal);
			ctx.put("resource", home);
			ctx.put("id", id);
			ctx.put("principal", principal);
			ctx.put("comment", "");
		}

		String mode = request.getParameter("mode");

		if (mode.equals("present")) {
			ctx.put("maxPresent", new Integer(maxPresent));
			return presentCommunity;
		}
		else if (mode.equals("update_community")) {
			ctx.put("id", principal.getId());
			ctx.put("name", principal.getName());
			ctx.put("email", principal.getEmail());
			ctx.put("organization", principal.getOrganization());
			ctx.put("orgRole", principal.getOrgRole());
			ctx.put("description", principal.getDescription());
			ctx.put("rejection", ((Community) principal).getRejectionCriterion());
			ctx.put("admission", ((Community) principal).getAdmissionCriterion());
			ctx.put("mode", mode);
			return newCommunity;
		}
		else if (mode.equals("delete_community")) {
			return confirmDelete;
		}
		else if (mode.equals("present_admins")) {
			admins = getAdminPrincipals(factory, (Community) principal);
			ctx.put("admins", admins);
			ctx.put("comment", "");
			ctx.put("mode", mode);
			return presentAdmins;
		}
		else if (mode.equals("mail_to_admins")) {
			HttpServletResponse res =
					(HttpServletResponse) ctx.get(VelocityServlet.RESPONSE);
			String emailAddresses =
					getEmailAddresses(factory, (Community) principal);
			res.sendRedirect("mailto:" + emailAddresses);
			return null;
		}
		else if (mode.equals("add_principal")) {
			ctx.put("search", "true");
			ctx.put("mode", "add_principal");
			ctx.put("principals", Collections.EMPTY_LIST);
			return selectForCommunity;
		}
		else if (mode.equals("remove_principal")) {
			int count = ((Community) principal).memberCount("");
			if (count < maxPresent) {
				members = getMemberPrincipals(factory, (Community) principal);
				ctx.put("principals", members);
				ctx.put("search", "false");
			}
			else {
				ctx.put("principals", Collections.EMPTY_LIST);
				ctx.put("search", "true");
			}
			ctx.put("mode", "remove_principal");
			return selectForCommunity;
		}
		else if (mode.equals("new_user")) {
			ctx.put("community", principal);
			ctx.put("cid", id);
			ctx.put("id", "");
			ctx.put("name", "");
			ctx.put("email", "");
			ctx.put("propnames", propnames);
			ctx.put("mode", mode);
			return newUser;
		}
		else if (mode.equals("new_collective")) {
			ctx.put("community", principal);
			ctx.put("cid", id);
			ctx.put("id", "");
			ctx.put("name", "");
			ctx.put("email", "");
			ctx.put("propnames", propnames);
			ctx.put("mode", mode);
			return newCollective;
		}
		else if (mode.equals("new_group")) {
			ctx.put("community", principal);
			ctx.put("cid", id);
			ctx.put("id", "");
			ctx.put("name", "");
			ctx.put("description", "");
			ctx.put("mode", mode);
			return newGroup;
		}
		else {
			return adrbook;
		}

	}

	//---------------------------------------------------------------

	/**
	 *  Gets the EmailAddresses attribute of the EditCommunityCommand object
	 *
	 *@param  factory    Description of Parameter
	 *@param  community  Description of Parameter
	 *@return            The EmailAddresses value
	 */
	protected String getEmailAddresses(Factory factory, Community community) {

		List admins = Collections.EMPTY_LIST;
		try {
			admins = community.getAdmins();
		}
		catch (ZenoException e) {
			System.out.println(e);
		}
		return getEmailAddresses(admins);
	}

}

