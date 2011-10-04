package zeno2.servlets;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.log4j.Logger;
import zeno2.kernel.Constants;
import zeno2.kernel.Factory;
import zeno2.kernel.Monitor;
import zeno2.kernel.ZenoEvent;
import zeno2.kernel.ZenoEventListener;

/**
 * <strong>ZenoEventListenerServlet</strong> an sample class
 * for EventListenerServlets in the Zeno environment.
 *
 * @author Juergen Walther
 * @version $Revision: 1.0 $ $Date: 2001/04/29 01:14:37 $
 */

public class ZenoEventListenerServlet extends HttpServlet {
	/**
	 * The log4j Logger for this servlet.
	 */
	static Logger log;

	/**
	 * A Factory is used to make instances of zeno2.kernel.ZenoResource 
	 * for a particular, authenticated user.  A factory is associated with 
	 * a session and reused to make all the resources needed by the user 
	 * during the session.
	 */
	private Factory factory = null;
	/**
	 * Gracefully shut down this servlet, releasing any resources
	 * that were allocated.
	 */
	public void destroy() {

		log.info("Finalizing " + getServletName() + " servlet");

		// NOTE:  We do not attempt to unload the database because there
		// is no portable way to do so.  Real applications will have used
		// a real database, with no need to unload it

		//getServletContext().removeAttribute(Constants.MONITOR_KEY);
		factory = null;
	}
	public void doGet(HttpServletRequest req, HttpServletResponse res)
		throws ServletException, IOException {
		try {
			String url = "/jsp/eventlistener.jsp";
			ServletContext sc = getServletContext();
			RequestDispatcher rd = sc.getRequestDispatcher(url);
			rd.forward(req, res);
		} catch (Exception e) {
			log.info("Forwarding exception: " + e);
			throw new UnavailableException(e.getMessage());
		}
	}

	/**
	 * Returns my zeno factory.
	 * Creation date: (7/18/2001 4:27:15 PM)
	 * @return zeno2.kernel.Factory
	 */
	public zeno2.kernel.Factory getFactory() {
		return factory;
	}

	/**
	 * Describes what this servlet does.
	 * Should be overwritten.
	 */
	public String getServletInfo() {
		return "ZenoBaseServlet";
	}

	/**
	 * Initialize this servlet, including loading our initial database from
	 * persistent storage.  
	 *
	 * @exception ServletException if we cannot configure ourselves correctly
	 */
	public void init() throws ServletException {

		Logger zenologger =
			(Logger) getServletContext().getAttribute(Constants.LOGGER_KEY);
		log = zenologger.getLogger(ZenoEventListenerServlet.class.getName());
		log.info("Initializing " + getServletName() + " servlet");

	}
	
	/**
	 * Assure the session and user authentification here.
	 * request authentification here.
	 * Creation date: (10/8/99 4:35:47 PM)
	 */
	public void service(HttpServletRequest req, HttpServletResponse res)
		throws ServletException, java.io.IOException {
		HttpSession session = req.getSession();
		if (session.isNew()) {
			// assure a factory for user in a session is created
			try {
				Monitor monitor =
					(Monitor) getServletContext().getAttribute(Constants.MONITOR_KEY);
				// handle login here to get userid and passwd
				// you may get them from the servlet parameters or hard code them here
				// for a non interactive servlet
				String userid = "zenoadmin";
				String passwd = "zeno%admin";
				factory = monitor.login(userid, passwd);
				// add your event listener here
				monitor.addZenoEventListener(new ZenoEventListener() {
					public void handleEvent(ZenoEvent e) {
						log.info(
							"Event handled: Date:"
								+ e.getDate()
								+ "; Principal: "
								+ e.getPrincipalId()
								+ "; Resource: "
								+ e.getSource());
					}
				});
				super.service(req, res);
			} catch (Exception e) {
				log.fatal("Authorization exception");
			}
		}
	}
}