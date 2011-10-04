package zeno2.velocity.action;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Iterator;

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
import zeno2.kernel.Principal;
import zeno2.kernel.ZenoException;
import zeno2.kernel.ZenoResource;

/**
 *  Handles zeno journal roles
 *
 *@author     <a href="mailto:eckehard.gross@ais.fraunhofer.de">Eckehard Gross</a>
 *@version    2.0.2 2001-11-14
 */
public class EditAdrbookCommand extends AdrbookCommand {

	private final String adrbook = "adrbook/editAdrbook.vm";
	private final String presentUser = "adrbook/principal/editPrincipal.vm";
	private final String presentCollective = "adrbook/collective/editCollective.vm";
	private final String presentGroup = "adrbook/group/editGroup.vm";
	private final String presentCommunity = "adrbook/community/editCommunity.vm";
	private final String newCommunity = "adrbook/community/editCommunityNew.vm";
	private final String newUser = "adrbook/principal/editPrincipalNew.vm";
	private final String selectForGroup = "adrbook/group/selectForGroup.vm";
	private final String selectForCommunity = "adrbook/community/selectForCommunity.vm";


	/**
	 *  Constructor for the EditAdrbookCommand object
	 *
	 *@param  req   Description of Parameter
	 *@param  resp  Description of Parameter
	 */
	public EditAdrbookCommand(HttpServletRequest req, HttpServletResponse resp) {
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

		ctx.put("resource", home);
		ctx.put("zcss", "/zeno/css/ss1.css");

		String id = req.getParameter("id");
		if (id != null && !id.equals("")) {
			//id represents the principal for which to search
			principal = factory.loadPrincipal(id);
			ctx.put("id", id);
			ctx.put("principal", principal);
		}

		String cid = req.getParameter("cid");
		if (cid != null) {
			//cid represents the community where to search
			ctx.put("cid", cid);
		}

		String mode = req.getParameter("mode");

		if (mode.equals("search")) {

			String searchFor = req.getParameter("search_for");
			String aim = req.getParameter("aim");
			ctx.put("mode", aim);
			if (aim.endsWith("principal") || aim.equals("community")) {
				ctx.put("community", principal);
			}
			if (aim.endsWith("member")) {
				ctx.put("group", principal);
			}

			if (request.getParameter("ok") != null &&
					searchFor != null && !searchFor.equals("")) {
				try {
					Iterator it =
							factory.searchPrincipals(cid, searchFor, "");
					if (it.hasNext()) {
						ctx.put("principals", it);
						ctx.put("comment", "");
					}
					else {
						ctx.put("principals", Collections.EMPTY_LIST);
						ctx.put("comment", "adrbook.no_matches");
						ctx.put("search", "true");
					}

					if (aim.equals("add_principal")) {
						return selectForCommunity;
					}
					else if (aim.equals("remove_principal")) {
						return selectForCommunity;
					}
					else if (aim.equals("add_member")) {
						return selectForGroup;
					}
					else if (aim.equals("remove_member")) {
						return selectForGroup;
					}
					else if (aim.equals("community")) {
						return presentCommunity;
					}
					else {
						return adrbook;
					}

				}
				catch (ZenoException e) {
					System.out.println(e);
					return adrbook;
				}
				catch (Exception e2) {
					e2.printStackTrace();
					return adrbook;
				}

			}
			else {
				if (aim.endsWith("principal")) {
					return presentCommunity;
				}
				if (aim.endsWith("member")) {
					List members =
							((Group) principal).getMembers(false, false);
					ctx.put("members", members);
					return presentGroup;
				}
				else {
					return adrbook;
				}
			}

		}
		else if (mode.equals("present")) {
			ctx.put("principals", Collections.EMPTY_LIST);
			ctx.put("comment", "");
			return adrbook;
		}
		else if (mode.equals("self")) {
			Principal self = factory.getUser();
			ctx.put("id", self.getId());
			ctx.put("containers", Collections.EMPTY_LIST);
			ctx.put("mode", mode);
			ctx.put("principal", self);
			if (self instanceof Collective) {
				ctx.put("collective", self);
				return presentCollective;
			}
			else {
				return presentUser;
			}

		}
		else if (mode.equals("new_community")) {
			ctx.put("id", "");
			ctx.put("name", "");
			ctx.put("description", "");
			ctx.put("mode", mode);
			return newCommunity;
		}
		else if (mode.equals("apply")) {
			Principal user = factory.getUser();
			ctx.put("propnames", propnames);
			ctx.put("id", user.getId());
			ctx.put("name", user.getName());
			ctx.put("mode", mode);
			return newUser;
		}
		else if (mode.equals("apply_as")) {
			ctx.put("propnames", propnames);
			ctx.put("id", "");
			ctx.put("name", "");
			ctx.put("mode", mode);
			return newUser;
		}
		else {
			return adrbook;
		}
	}


}

