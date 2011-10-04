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
import zeno2.velocity.util.Errors;
import zeno2.velocity.util.Tools;

/**
 *  Handles Zeno Principales management
 *
 *@author  <a href="mailto:hans-eckehard.grossr@ais.fraunhofer.de"> Hans-Eckehard  Gross</a>
 *@version    2.0.2, 2001-12-16
 */
public class PostEditPrincipalCommand extends AdrbookCommand {

	private final String adrbook = "adrbook/editAdrbook.vm";
	private final String presentCommunity = "adrbook/community/editCommunity.vm";
	private final String presentUser = "adrbook/principal/editPrincipal.vm";
	private final String newUser = "adrbook/principal/editPrincipalNew.vm";
	private final String enterPwd = "adrbook/enterPassword.vm";
	//private final String passwordSent = "adrbook/passwordSent.vm";


	/**
	 *  Constructor for the PostEditPrincipalCommand object
	 *
	 *@param  req   Description of Parameter
	 *@param  resp  Description of Parameter
	 */
	public PostEditPrincipalCommand(HttpServletRequest req, HttpServletResponse resp) {
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

		HttpSession sess = request.getSession();
		Factory factory = (Factory) ctx.get(Constants.FACTORY_KEY);
		Principal principal = null;
		List containers = Collections.EMPTY_LIST;
		forumServlet = (String) ctx.get("servl");
		String cid = null;
		Community community = null;
		String mode = request.getParameter("mode");
		String id = request.getParameter("id");
		String identifier = request.getParameter("identifier");
		if (identifier != null) {
			identifier = identifier.trim().toLowerCase();
		}
		String name = request.getParameter("name");
		name = (new Tools()).filterQuote(name);
		String email = request.getParameter("email");
		if (email != null) {
			email = email.trim();
		}

		String template = adrbook;
		ctx.put("zcss", "/zeno/css/ss1.css");
		ctx.put("id", id);
		//ctx.put("error", "");
		ctx.put("comment", "");
		ctx.put("containers", containers);

		ctx.put("id", identifier);
		ctx.put("name", name);
		ctx.put("email", email);
		ctx.put("mode", mode);

		if (id != null && !id.equals("") && !mode.equals("new_user")) {
			try {
				principal = (Principal) factory.loadPrincipal(id);
				ctx.put("principal", principal);
				ctx.put("id", id);
			}
			catch (NotFoundException e) {
				System.out.println(e);
				errors.addError("adrbook.not_found");
			}
		}

		if (request.getParameter("ok") != null) {

			if (mode.equals("new_user")) {
				//******************** new_user ***********************

				template = newUser;
				try {
					cid = request.getParameter("cid");
					community = (Community) factory.loadPrincipal(cid);
					ctx.put("principal", community);
					ctx.put("community", community);
					ctx.put("id", identifier);
					ctx.put("propnames", propnames);
				}
				catch (ZenoException e) {
					System.out.println(e);
					template = adrbook;
				}

				if (!checkIdentifier(identifier)) {
					//ctx.put("error", "adrbook.bad_identifier");
					errors.addError("adrbook.bad_identifier");
				}
				if (badEmail(email)) {
					errors.addError("adrbook.email_required");
				}
				if (errors.isEmpty()) {
					try {
						community = (Community) factory.loadPrincipal(cid);

						principal =
								factory.createUser(cid, identifier, name, email);
						Enumeration parnames = request.getParameterNames();
						while (parnames.hasMoreElements()) {
							String parname = (String) parnames.nextElement();
							if (parname.startsWith("prop.")) {
								String value = request.getParameter(parname);
								setProperty(principal, "", parname, value);
							}
						}

						String password = (new PassGen()).generate();
						String uri = request.getRequestURI();
						String serverName = (String) ctx.get(Constants.SERVER_NAME);
						String serverPort = (String) ctx.get(Constants.SERVER_PORT);
						if (!serverPort.equals("80")) {
							serverPort = ":" + serverPort;
						}
						else {
							serverPort = "";
						}
						String adrbookURL = "http://" + serverName + serverPort
								 + uri + "?action=editAdrbook&mode=present";
						String privacyURL =
								"http://ais.gmd.de/MS/results/zeno2/zeno2-privacy-policy.html";
						Monitor monitor = (Monitor) ctx.get(Constants.MONITOR_KEY);
						String mailhost = monitor.getProperty("zenoMailServer"
								, "No_zenoMailServer");
						String zenoHotline = monitor.getProperty("zenoHotlineEmailAddress",
								"No_zenoHotlineEmailAddress");
						Principal administrator = factory.getUser();
						String administratorName = administrator.getId() +" ("
								+ administrator.getName() +")";
						String administratorEmail = administrator.getEmail();
						String[] fillins = {
								administratorName, administratorEmail, identifier, password,
								adrbookURL, privacyURL, email
								};
						String noticePath = ctx.get("notipath") + "/newUser.txt";
						sendNotice(mailhost, noticePath, fillins);
						principal.setPassword(password);
						principal.save();
						ctx.put("principal", community);
						ctx.put("community", community);
						ctx.put("maxPresent", new Integer(maxPresent));
						ctx.put("id", cid);
						ctx.put("username", identifier + " (" + email + ")");
						ctx.put("passwdmsg", "adrbook.password_set");
						template = presentCommunity;
					}
					catch (NoPermissionException e) {
						errors.addError("adrbook.no_permission");
						template = adrbook;
					}
					catch (NameInUseException e) {
						errors.addError("adrbook.name_in_use");
					}
				}
				if (!errors.isEmpty()) {
					ctx.put(Constants.ERRORS, errors.getErrors());
					ctx.put("cid", cid);
				}
				return template;
			}
			else if (mode.equals("apply_as")) {
				//******************** apply_as ***********************

				template = newUser;

				cid = request.getParameter("cid");

				if (!checkIdentifier(identifier)) {
					errors.addError("adrbook.bad_identifier");
					ctx.put("identifier", "");
				}

				String pwd = request.getParameter("password_new");
				String pwdAgain = request.getParameter("password_again");
				if (pwd.equals("")) {
					errors.addError("adrbook.illegal_password");
				}
				if (!pwd.equals(pwdAgain)) {
					errors.addError("adrbook.wrong_repeated_password");
				}

				if (errors.isEmpty()) {

					try {
						principal =
								factory.registerAs(cid, identifier, name, pwd);
						System.out.println("postedit new prin " + principal);
						Enumeration parnames = request.getParameterNames();
						while (parnames.hasMoreElements()) {
							String parname = (String) parnames.nextElement();
							if (parname.startsWith("prop.")) {
								String value = request.getParameter(parname);
								setProperty(principal, cid, parname, value);
							}
						}

						//relogin with new identity

						Monitor monitor = (Monitor) ctx.get(Constants.MONITOR_KEY);
						String remadr = request.getRemoteAddr();
						String criteria = "ipaddr=" + remadr;
						factory = monitor.login(identifier, pwd, criteria);
						sess.setAttribute(Constants.FACTORY_KEY, factory);
						sess.setAttribute(Constants.USER_KEY, factory.getUser());
						String url = forumServlet + "?action=editPrincipal&id=" +
								identifier + "&mode=present";
						response.sendRedirect(url);
						return null;
					}
					catch (NoPermissionException e) {
						errors.addError("adrbook.no_permission");
					}
					catch (NameInUseException e) {
						errors.addError("adrbook.name_in_use");
					}
				}

				if (!errors.isEmpty()) {
					ctx.put(Constants.ERRORS, errors.getErrors());
					ctx.put("propnames", propnames);
				}
				return template;
			}
			else if (mode.equals("update_user")) {
				//******************** update_user ***********************
				ctx.put("propnames", propnames);
				if (!id.equals(principal.getCreator()) &&
						((email == null) || email.equals(""))) {
					errors.addError("adrbook.email_required");
					template = newUser;
				}
				else {
					try {
						template = presentUser;
						principal.setName(name);
						principal.setEmail(email);

						Enumeration parnames = request.getParameterNames();
						while (parnames.hasMoreElements()) {
							String parname = (String) parnames.nextElement();
							if (parname.startsWith("prop.")) {
								String value = request.getParameter(parname);
								setProperty(principal, "", parname, value);
							}
						}

						//new property
						String propname = request.getParameter("propname");
						String propvalue = request.getParameter("propvalue");
						cid = request.getParameter("cid");
						if (propvalue != null && !"".equals(propvalue)) {
							principal.setProperty(cid, propname, propvalue);
						}
						principal.save();
						ctx.put("principal", principal);

					}
					catch (NoPermissionException e) {
						//ctx.put("error", "adrbook.no_permission");
						errors.addError("adrbook.no_permission");
					}
					catch (ZenoException e) {
						//ctx.put("error", "adrbook.no_permission");
						errors.addError("error.just_deleted");
					}
				}
				if (!errors.isEmpty()) {
					ctx.put(Constants.ERRORS, errors.getErrors());
				}
				return template;
			}
			else if (mode.equals("change_pwd")) {
				//******************** change_pwd ***********************

				ctx.put("mode", mode);
				ctx.put("action", "posteditPrincipal");
				String oldpwd = request.getParameter("password_old");
				Principal user = factory.getUser();
				if (!user.checkPassword(oldpwd)) {
					errors.addError("adrbook.invalid_permission");
					ctx.put(Constants.ERRORS, errors.getErrors());
					return enterPwd;
				}

				String pwd = request.getParameter("password_new");
				String pwdAgain = request.getParameter("password_again");
				if (pwd.equals("")) {
					errors.addError("adrbook.illegal_password");
					ctx.put(Constants.ERRORS, errors.getErrors());
					return enterPwd;
				}

				if (!pwd.equals(pwdAgain)) {
					ctx.put("mode", mode);
					errors.addError("adrbook.wrong_repeated_password");
					ctx.put(Constants.ERRORS, errors.getErrors());
					return enterPwd;
				}

				try {
					principal.setPassword(pwd);
					//ctx.put("principal", principal);
					return presentUser;
				}
				catch (NoPermissionException e) {
					System.out.println(e);
					errors.addError("adrbook.no_permission");
					ctx.put(Constants.ERRORS, errors.getErrors());
					return presentUser;
				}
					catch (ZenoException e) {
						//ctx.put("error", "adrbook.no_permission");
						errors.addError("error.just_deleted");
					}
			}
			else if (mode.equals("apply")) {
				//******************** apply ***********************

				try {
					cid = request.getParameter("cid");
					community = (Community) factory.loadPrincipal(cid);
					principal = factory.getUser();
					ctx.put("id", principal.getId());
					ctx.put("principal", principal);
					ctx.put("propnames", propnames);

				}
				catch (ZenoException e) {
					System.out.println(e);
					template = adrbook;
				}

				factory.register(cid);
				Enumeration parnames = request.getParameterNames();
				while (parnames.hasMoreElements()) {
					String parname = (String) parnames.nextElement();
					if (parname.startsWith("prop.")) {
						String value = request.getParameter(parname);
						setProperty(principal, cid, parname, value);
					}
				}
				return presentUser;
			}
			else if (mode.equals("leave_community")) {
				//******************** leave_community ***********************
				String[] cids =
						request.getParameterValues("leave_community");
				if (cids != null) {
					for (int i = 0; i < cids.length; i++) {
						try {
							principal.leaveCommunity(cids[i]);
						}
						catch (NoPermissionException e) {
							if (e.getMessage().equals("lastAdmin")) {
								//errors.addError("last_admin_for" + cids[i]);
								errors.addError("adrbook.last_admin");
							}
						}
					}
				}
				if (!errors.isEmpty()) {
					ctx.put(Constants.ERRORS, errors.getErrors());
				}
				try {
					principal = factory.loadPrincipal(id);
				}
				catch (NotFoundException e) {
					//principal completely erased from system
					System.out.println("posteditPrincipal: erased " + id);

					if (!id.equals(factory.getUser().getId())) {
						ctx.put("id", "");
						return adrbook;
					}
					else {
						//relogin as guest
						Monitor monitor = (Monitor) ctx.get(Constants.MONITOR_KEY);
						String remadr = request.getRemoteAddr();
						String criteria = "ipadr=" + remadr;
						factory = monitor.login("guest", "", criteria);
						sess.setAttribute(Constants.FACTORY_KEY, factory);
						sess.setAttribute(Constants.USER_KEY, factory.getUser());
						String url = forumServlet + "?action=editAdrbook"
								 + "&mode=present";
						response.sendRedirect(url);
						return null;
					}

				}
				return presentUser;
			}
			else if (mode.equals("leave_group")) {
				//******************** leave_group ***********************

				String[] gids =
						request.getParameterValues("leave_group");
				if (gids != null) {
					for (int i = 0; i < gids.length; i++) {
						try {
							principal.leaveGroup(gids[i]);
						}
						catch (NoPermissionException e) {
							errors.addError(e.getMessage());
						}
					}
				}
				if (!errors.isEmpty()) {
					ctx.put(Constants.ERRORS, errors.getErrors());
				}
				return presentUser;
			}

		}
		else {
			//******************** cancel ***********************


			if (mode.equals("new_user")) {
				cid = request.getParameter("cid");
				community = (Community) factory.loadPrincipal(cid);
				ctx.put("principal", community);
				ctx.put("community", community);
				ctx.put("id", cid);
				ctx.put("cid", cid);
				ctx.put("maxPresent", new Integer(maxPresent));
				return presentCommunity;
			}
			else if (mode.startsWith("apply")) {
				return adrbook;
			}
			else {
				ctx.put("principal", principal);
				template = presentUser;
			}
		}
		return template;
	}

	//----------------------------------------------------------------

	/**
	 *  Sets the Property attribute of the PostEditPrincipalCommand object
	 *
	 *@param  principal  The new Property value
	 *@param  community  The new Property value
	 *@param  parname    The new Property value
	 *@param  parvalue   The new Property value
	 */
	protected void setProperty(Principal principal,
			String community, String parname, String parvalue) {
		try {
			int index = parname.indexOf(".", 5);
			String cid = community.equals("") ? parname.substring(5, index) : community;
			String propname = parname.substring(index + 1);
			System.out.println("setproperty " + cid + " " + propname + " " + parvalue);
			if (parvalue != null && !"".equals(parvalue.trim())) {
				principal.setProperty(cid, propname, parvalue);
			}
			else {
				principal.removeProperty(cid, propname);
			}
		}
		catch (ZenoException e) {
			System.out.println(e);
		}
	}


	/**
	 *  Description of the Method
	 *
	 *@param  email  Description of Parameter
	 *@return        Description of the Returned Value
	 */
	protected boolean badEmail(String email) {
		return ((email == null) || email.equals("")
				 || (email.indexOf("@") < 0));
	}

}

