package zeno2.velocity.action;

//import java.util.*;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

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
import zeno2.kernel.Principal;
import zeno2.kernel.ZenoException;
import zeno2.kernel.ZenoResource;
import zeno2.velocity.util.Errors;

/**
 *  Handles zeno groups
 *
 *@author     <a href="mailto:eckehard.gross@ais.fraunhofer.de">Eckehard Gross</a>
 *@version    2.0.2, 2001-11-14
 */

public class EditGroupCommand extends AdrbookCommand {

	private final String newGroup = "adrbook/group/editGroupNew.vm";
	private final String selectForGroup = "adrbook/group/selectForGroup.vm";
	private final String presentCommunity = "adrbook/community/editCommunity.vm";
	private final String adrbook = "adrbook/editAdrbook.vm";


	/**
	 *  Constructor for the EditGroupCommand object
	 *
	 *@param  req   Description of Parameter
	 *@param  resp  Description of Parameter
	 */
	public EditGroupCommand(HttpServletRequest req, HttpServletResponse resp) {
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

		String id = request.getParameter("id");
		String mode = request.getParameter("mode");

		Factory factory = (Factory) ctx.get(Constants.FACTORY_KEY);
		ZenoResource home = factory.loadResource(1);
		Principal principal = factory.loadPrincipal(id);
		Community community = (Community) principal.getCommunities().get(0);
		String cid = community.getId();
		ctx.put("group", principal);
		ctx.put("zcss", "/zeno/css/ss1.css");
		ctx.put("id", id);
		ctx.put("cid", cid);
		ctx.put("resource", home);
		ctx.put("comment", "");
		ctx.put("mode", mode);

		if (mode.equals("update_group")) {
			ctx.put("Id", principal.getId());
			ctx.put("name", principal.getName());
			ctx.put("description", principal.getDescription());
			ctx.put("mode", mode);
			return newGroup;
		}
		// ******************** delete *************************************
		else if (mode.equals("delete")) {
			ctx.put("principal", community);
			ctx.put("community", community);
			ctx.put("maxPresent", new Integer(maxPresent));
			ctx.put("id", cid);
			try {
				factory.removePrincipal(community.getId(), id);
				return presentCommunity;
			}
			catch (NoPermissionException e) {
				errors.addError("adrbook.no_permission");
				return presentCommunity;
			}

		}
		// **************** add_member *************************************
		else if (mode.equals("add_member")) {
			ctx.put("search", "true");
			ctx.put("principals", Collections.EMPTY_LIST);
			return selectForGroup;
		}
		// ****************** remove_member ********************************
		else if (mode.equals("remove_member")) {
			ctx.put("search", "false");
			List members = getMemberPrincipals(factory, (Group) principal);
			ctx.put("principals", members);
			return selectForGroup;
		}
		// ***************** mail_to ***************************************
		else if (mode.equals("mail_to")) {
			HttpServletResponse res =
					(HttpServletResponse) ctx.get(VelocityServlet.RESPONSE);
			String emailAddresses = getEmailAddresses(factory, (Group) principal);
			res.sendRedirect("mailto:" + emailAddresses);
			return null;
		}
		else {
			return adrbook;
		}
	}

	//----------------------------------------------------------------------

	/**
	 *  returns a list of email addresses, separated by commas, for all members and
	 *  indirect members of a group.
	 *
	 *@param  factory  Description of Parameter
	 *@param  group    Description of Parameter
	 *@return          The EmailAddresses value
	 */

	protected String getEmailAddresses(Factory factory, Group group) {
		List members = Collections.EMPTY_LIST;
		try {
			members = group.getMembers(true, true);
		}
		catch (ZenoException e) {
			System.out.println(e);
		}
		return getEmailAddresses(members);
	}


}


