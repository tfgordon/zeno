package zeno2.velocity.action;

import java.util.Date;

// Java Servlet
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

// Velocity
import org.apache.velocity.context.Context;

import zeno2.velocity.util.Errors;
import zeno2.kernel.Constants;
import zeno2.kernel.Factory;
import zeno2.kernel.Monitor;
import zeno2.kernel.ZenoException;
import zeno2.util.ZenoUtilities;

/**
 *  Insert the type's description here. Creation date: (05.09.2001 14:24:53)
 *
 *@author     Viviane Wolff
 *@version    2.0.2, 2001-09-13
 */
public class PostLoginCommand extends Command {
	/**
	 *  PostLogin constructor comment.
	 *
	 *@param  req   javax.servlet.http.HttpServletRequest
	 *@param  resp  javax.servlet.http.HttpServletResponse
	 *@since
	 */
	public PostLoginCommand(HttpServletRequest req, HttpServletResponse resp) {
		super(req, resp);
	}


	/**
	 *  Implemented by classes that extends this class
	 *
	 *@param  ctx            Description of Parameter
	 *@return                the name of the template to execute
	 *@exception  Exception  Description of Exception
	 *@since
	 */
	public String exec(org.apache.velocity.context.Context ctx) throws Exception {

		String userid = request.getParameter("name");
		String password = request.getParameter("password");
		String template = "";
		String id = request.getParameter("id");
		String subject = request.getParameter("subject");
		HttpSession sess = request.getSession();
		String now = ZenoUtilities.getIsoString(new Date());
		forumServlet = (String) ctx.get("servl");

		System.out.println("*** " + now
				 + ", user=" + userid
				 + ", host=" + request.getRemoteHost() + ", IP=" + request.getRemoteAddr()
				 + ", session=" + request.getSession().getId()
				 + ", remoteUser=" + request.getRemoteUser()
				 + " (PostLoginCommand)");

		Factory factory = null;
		Monitor monitor = (Monitor) ctx.get(Constants.MONITOR_KEY);
		if (monitor == null) {
			System.out.println("exec POSTLOGIN Monitor is null");
			//ctx.put("errortitle","severe_error");
			//ctx.put("addon","no_database");
			//return "error.vm";
			return showMessage(ctx, "error.severe_error", "error.no_database");
		}

		String remadr = request.getRemoteAddr();
		String clientInfo = "ipaddr=" + remadr;

		try {
			factory = monitor.login(userid, password, clientInfo);
			sess.setAttribute(Constants.FACTORY_KEY, factory);
			sess.setAttribute(Constants.USER_KEY, factory.getUser());
		}
		catch (ZenoException e) {
			System.out.print("PostLoginCommand:catch ZenoException:");
			System.out.println(e.getMessage());

			// create Errors Object and put it into the context
			Errors errors = new Errors();
			errors.addError("error." + e.getMessage());
			ctx.put(Constants.ERRORS, errors.getErrors());

			return LoginCommand.LOGIN;
		}
		if (factory == null) {
			// create Errors Object and put it into the context
			template = LoginCommand.LOGIN;
			System.out.println("exec POSTLOGIN factory is null");
		}
		else {
			ctx.put(Constants.FACTORY_KEY, factory);
			ctx.put(Constants.USER_KEY, factory.getUser());

			// check what to do: FRONT_VIEW or another action
			// look into the session context

			String url = null;
			System.out.println("PostLoginCommand.exec.LAST_URL="
					 + (String) sess.getAttribute(Constants.LAST_URL));
			if ((url = (String) sess.getAttribute(Constants.LAST_URL)) != null) {
				//response.setCo MimeType
				sess.removeAttribute(Constants.LAST_URL);
			}
			else {
				url = forumServlet + "?action=subscription&view=print";
			}
			template = null;
			String urlEncode = response.encodeRedirectURL(url);
			System.out.println("PostLoginCommand.exec.urlEncode=" + urlEncode);
			response.sendRedirect(urlEncode);
		}
		System.out.println("PostLoginCommand.exec.template=" + template);
		return template;
	}
}

