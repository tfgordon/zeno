package zeno2.velocity.action;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.context.Context;
import org.apache.velocity.servlet.VelocityServlet;
import javax.servlet.http.HttpSession;
import zeno2.kernel.Constants;

/**
 *  Handles logout command by invaidating the current session
 *
 *@author     <a href="mailto:lothar.oppor@ais.fraunhofer.de">Lothar Oppor</a>
 *@version    2.0.2, 2001-09-14
 */
public class LogoutCommand extends Command {

	/**
	 *  Description of the Field
	 *
	 *@param  req   Description of Parameter
	 *@param  resp  Description of Parameter
	 *@since
	 */

	/**
	 *  Description of the Field
	 *
	 *  Description of the Field Description of the Field Description of the Field
	 *  Description of the Field Description of the Field Description of the Field
	 *  Description of the Field Description of the Field Description of the Field
	 *  Constructor for the DeleteCommand object
	 *
	 *@param  req   Description of Parameter
	 *@param  resp  Description of Parameter
	 *@since
	 *@since
	 *@since
	 *@since
	 *@since
	 *@since
	 *@since
	 *@since
	 */
	public LogoutCommand(HttpServletRequest req, HttpServletResponse resp) {
		super(req, resp);
	}


	/**
	 *  Compact, if the resource is a journal and compact parameter is true
	 *  Otherwise mark the resource for deleted. The handeled resource has to be
	 *  saved!!!
	 *
	 *@param  ctx            Velocity Context
	 *@return                the appropriate template file name
	 *@exception  Exception  Description of Exception
	 *@since
	 */
	public String exec(Context ctx) throws Exception {

		HttpSession sess = request.getSession();
		forumServlet = (String) ctx.get("servl");
		try {
			String lastUrl = (String) sess.getAttribute(Constants.LAST_URL);
			if (lastUrl == null) {
				lastUrl = forumServlet + "?action=subscription&view=print";
			}
			sess = request.getSession();
			sess.invalidate();
			sess = request.getSession();
			sess.setAttribute(Constants.LAST_URL, lastUrl);
			String url = response.encodeRedirectURL(forumServlet);
			response.sendRedirect(url);
		}
		catch (Exception e) {
			throw e;
		}

		return null;
	}
}

