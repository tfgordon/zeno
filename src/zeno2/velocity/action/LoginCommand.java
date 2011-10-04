package zeno2.velocity.action;

import java.util.*;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.context.Context;
import org.apache.velocity.app.tools.*;
import org.apache.velocity.servlet.VelocityServlet;

import zeno2.kernel.*;

/**
 *  Handles zeno login. Creation date: (04.09.2001 17:12:43)
 *
 *@author    Lothar ppor
 *@author    Viviane Wolff
 *@version   2.0.2, 2001-09-19
 */
public class LoginCommand extends Command {
	/**
	 *  Description of the Field
	 *
	 */
	public final static java.lang.String LOGIN = "login.vm";


	/**
	 *  LoginCommand constructor comment.
	 *
	 *@param  req   Description of Parameter
	 *@param  resp  Description of Parameter
	 */

	public LoginCommand(HttpServletRequest req, HttpServletResponse resp) {
		super(req, resp);
	}


	/**
	 *  Insert the method's description here. Creation date: (04.09.2001 17:21:14)
	 *
	 *@param  ctx            Description of Parameter
	 *@return                java.lang.String
	 *@exception  Exception  Description of Exception
	 */
	public String exec(Context ctx) throws Exception {

		String id = request.getParameter("id");
		String subject = request.getParameter("subject");
		ctx.put("id", id);
		ctx.put("subject", subject);

		return LOGIN;
	}
}

