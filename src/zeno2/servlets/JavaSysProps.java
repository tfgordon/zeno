package zeno2.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Properties;

import javax.servlet.GenericServlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

public class JavaSysProps extends GenericServlet {

	/**
	 * @see GenericServlet#service(ServletRequest, ServletResponse)
	 */
	public void service(ServletRequest req, ServletResponse res)
		throws ServletException, IOException {
		res.setContentType("text/plain");

		Properties props;
		props = System.getProperties();

		PrintWriter out = res.getWriter();
		out.println("---------- Server Java System Properties ----------");
		out.println("             Juergen Walther 22.04.2002            ");

		if (req.getParameter("long") != null) {
			out.println("                 long form listing                 ");
			Enumeration prop_enum = props.propertyNames();
			String key = "";
			while (prop_enum.hasMoreElements()) {
				key = (String) prop_enum.nextElement();
				out.println(key + "\t" + props.getProperty(key));
			}
			out.close();
			return;
		}
		props.list(out); // abbreviated listing is default.
		out.close();
	}
}