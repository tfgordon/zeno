package zeno2.servlets;

import java.util.Enumeration;
import java.util.Properties;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import zeno2.kernel.Constants;

/**
 * <strong>Log4jInit</strong> provides logging facilities for Zeno.
 * 
 * A Logger as configured by log4j.properties file specified as 
 * this servlets log4j-init-file init parameter, and is set 
 * in the servlet context with the key Constants.LOGGER_KEY.
 * 
 * should be first servlet on initialization sequence.
 * 
 * @author Juergen Walther
 * @version $Revision: 1.0 $ $Date: 2002/06/03 01:14:37 $
 */

public class Log4jInit extends HttpServlet {
	static Logger log;

	public void init() {
		try {
			String prefix = getServletContext().getRealPath("/");
			String file = getInitParameter("log4j-init-file");
			// if the log4j-init-file is not set, then no point in trying
			if (file != null) {
				PropertyConfigurator.configure(prefix + file);
				log = Logger.getLogger(Log4jInit.class.getName());
				getServletContext().setAttribute(Constants.LOGGER_KEY, log);
				log.info("Logging initialized");
				//sysprops();
			}
		} catch (Exception e) {
			log.fatal("Logging initialization exception", e);
		}
	}

	public void sysprops() {
		Properties prop = System.getProperties();
		Enumeration enum = prop.propertyNames();
		log.debug("***System Environment As Seen By Java***");
		log.debug("***Format: PROPERTY = VALUE***");
		while (enum.hasMoreElements()) {
			String key = (String) enum.nextElement();
			log.debug(key + " = " + System.getProperty(key));
		}
	}

	public void doGet(HttpServletRequest req, HttpServletResponse res) {
	}
}
