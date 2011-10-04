package zeno2.kernel;

import java.io.InputStream;
import java.io.PrintWriter;

/** Zeno2 Monitor.  (Should) conform to Java Bean conventions. Intended for use
as a "application bean", i.e. an object of application scope in a web
application.  Used for event notification and propogation. */

public interface Monitor {

	public void addZenoEventListener(ZenoEventListener listener);

	public void removeZenoEventListener(ZenoEventListener listener);

	/** Configures the monitor by loading a Java property file with the
	given pathname from the local file system. */

	public void configure(String pathname) throws ZenoException;
	
	public void configure(String pathname, boolean startTasks) 
			throws ZenoException;
	
	/*
	 * 07/11/2002 Ahmet Ocakli
	 * introduced new public Method
	 * in order to use the Zeno Kernel API for
	 * mail the fascility of Zeno
	 *
	 */
	public void configure(InputStream stream) throws ZenoException;

	/** Sets a writer for error messages flushing any writer set before */

	public void setErrorWriter(PrintWriter prw);

	/** Returns a factory for an authorized user */

	public Factory login(String username, String password) 
		throws ZenoException;
		
	/** Returns a factory for an authorized user. clientInfo
	is a komma separated list of client properties of the form
	propertyName=value which should include ipaddr, the internet
	address of the client. clientInfo is matched against membership 
	criteria of collectives and admission or rejection criteria 
	of communities */ 

	public Factory login(String username, String password, String clientInfo) 
		throws ZenoException;

	/** Gets the specified property; returns defvalue 
	if the property is unavailable */
	
	public String getProperty(String key, String defvalue);
	
	/** Gets the domain name of this Zeno server */

	public String getDomainName();

	/** Gets the name of this zeno web application  */

	public String getWebApplication();
}