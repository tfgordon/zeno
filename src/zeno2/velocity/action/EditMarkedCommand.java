package zeno2.velocity.action;

/*
 *
 *
 *
 *
 */

import java.net.URLEncoder;
import java.util.*;
import java.sql.Date;


import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.context.Context;
import org.apache.velocity.app.tools.*;
import org.apache.velocity.servlet.VelocityServlet;

import zeno2.kernel.*;
import zeno2.util.ZenoUtilities;
import zeno2.velocity.util.Tools;

/**
 *  Handles zeno start views
 *
 *@author     <a href="mailto:andreas.klotz@ais.fraunhofer.de">Andreas Klotz</a>
 *@version   2.0.2 2001-09-07
 */
public class EditMarkedCommand extends Command {

	/**
	 *  the template file names for all editArticle views
	 *
	 *@since
	 */
	public static Properties templates = new Properties();
	
	private final String show = "home/editMarked.vm";

	/**
	 *  Constructor for the EditArticleCommand object
	 *
	 *@param  req   Description of Parameter
	 *@param  resp  Description of Parameter
	 *@since
	 */
	public EditMarkedCommand(HttpServletRequest req, HttpServletResponse resp) {
		super(req, resp);
	}


	/**
	 *  Put id, title, note, keywords, rank expires and eventually georef into the
	 *  context. Add view as modeto the context. If view=new, add the journal to
	 *  the context, else add the article. finaly return the filename of the
	 *  appropriate template.
	 *
	 *@param  ctx            Velocity Context
	 *@return                the appropriate template file name
	 *@exception  Exception  no exception is thrown
	 *@since
	 */
	public String exec(Context ctx) throws Exception {

		HttpServletRequest req = (HttpServletRequest) ctx.get(VelocityServlet.REQUEST);
		HttpServletResponse res = (HttpServletResponse) ctx.get(VelocityServlet.RESPONSE);
		
		Factory factory = (Factory)ctx.get(Constants.FACTORY_KEY);
		
		String navmode = "struct";
		
		ctx.put("navmode", navmode);
		ctx.put("mode", "struct");
		ctx.put("zcss", "/zeno/css/ss1.css");
		
		List subscribedJournals = factory.loadSubscribedJournals();
		ctx.put("subscribedJournals", subscribedJournals);
		
		List markedJournals =factory.loadMarkedJournals();
		ctx.put("markedJournals", markedJournals);
				
		return show;
	}


	
}

