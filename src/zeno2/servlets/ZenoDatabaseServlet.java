package zeno2.servlets;

import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServlet;
import org.apache.log4j.Logger;
import zeno2.db.MonitorImpl;
import zeno2.kernel.Constants;
import zeno2.kernel.Monitor;
import zeno2.notification.DailyTimer;
//import zeno2.notification.RSSModule;

/**
 *  <strong>ZenoDatabaseServlet</strong> initializes and finalizes the
 *  persistent storage of the Zeno Environment. Must be loaded as the <strong>
 *  second</strong> servlet (after Log4jInit) in the load on startup sequence of
 *  the Zeno webapplication. The following servlet initialization parameters are
 *  processed, with default values in square brackets:
 *  <ul>
 *    <li> <strong>pathname</strong> - Resource pathname to our persistent
 *    storage configuration file. ["/WEB-INF/conf/zeno.properties"]
 *  </ul>
 *  ServletException if we cannot configure ourselves correctly
 *
 *@author     Juergen Walther
 *@version    September 11, 2002
 */

public final class ZenoDatabaseServlet extends HttpServlet {

	// The resource path of our configuration file.
	private String pathname = "/WEB-INF/conf/zeno.properties";
	// the log4j Logger
	static Logger log;


	/**
	 *  Gracefully shut down this servlet, releasing any resources that were
	 *  allocated at initialization.
	 */
	public void destroy() {
		log.info("Finalizing " + getServletName() + " servlet");

		// NOTE:  Any need to finalize the database connection?
		// then call the method of the monitor object here!

		// Remove the monitor from our application attributes
		getServletContext().removeAttribute(Constants.MONITOR_KEY);
		getServletContext().removeAttribute(Constants.LOGGER_KEY);
	}


	/**
	 *  Initialize the connection to the zeno database.
	 *
	 *@exception  ServletException  if we cannot configure ourselves correctly
	 */
	public void init() throws ServletException {

		Logger zenologger =
				(Logger) getServletContext().getAttribute(Constants.LOGGER_KEY);
		log = zenologger.getLogger(ZenoDatabaseServlet.class.getName());
		log.info("Initializing " + getServletName() + " servlet");

		// Process our servlet initialization parameters
		String value;
		value = getServletConfig().getInitParameter("pathname");
		if (value != null) {
			pathname = value;
		}

		// Create the Zeno Monitor and set it to application context
		try {
			Monitor monitor = new MonitorImpl();
			monitor.setErrorWriter(new PrintWriter(System.out, true));
			value = getServletContext().getRealPath(pathname);
			log.debug("Trying to load config file from: " + value);
			monitor.configure(value, true);
			getServletContext().setAttribute(Constants.MONITOR_KEY, monitor);
			// Added code ZAK
			// Starts Notificatio and RSS module
			String startNotify = monitor.getProperty("startNotification","yes");
			String startRSS = monitor.getProperty("startRSS","yes");
			if(startNotify.equals("yes"))
				new DailyTimer(monitor);
			//if((startRSS == null) || (startRSS.equals("yes")))
				//new RSSModule(monitor);
			//End Added code ZAK
		}
		catch (Exception e) {
			log.fatal("Database connection exception", e);
			getServletContext().setAttribute(Constants.ERRORS, e.toString());
			throw new UnavailableException(
					"Cannot connect to database using configuration file '" + pathname + "':" + e);
		}
	}
}

