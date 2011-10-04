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
import zeno2.velocity.util.ZenoBundle;

/**
 *  Handles zeno start views
 *
 *@author     <a href="mailto:andreas.klotz@ais.fraunhofer.de">Andreas Klotz</a>
 *@version    2.0.2, 2001-09-07
 */
public class GlobalSearchCommand extends Command {

	/**
	 *  the template file names for all editArticle views
	 *
	 *@since
	 */
	public static Properties templates = new Properties();
	
	
	public final static String orderOptions =
			"modification_date,rank,title,label,qualifier,creator,modifier,"
			 + "creation_date,begin_date";


	/**
	 *  Constructor for the EditArticleCommand object
	 *
	 *@param  req   Description of Parameter
	 *@param  resp  Description of Parameter
	 *@since
	 */
	public GlobalSearchCommand(HttpServletRequest req, HttpServletResponse resp) {
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

		Factory factory = (Factory)ctx.get(Constants.FACTORY_KEY);
		
		ZenoBundle msg = (ZenoBundle) ctx.get(Constants.RESOURCE_KEY);
		
		String view = request.getParameter("view");
		String action = request.getParameter("action");
		String id = request.getParameter("id");
		int idNr = (new Tools()).toInt(id);
		
		if ((view == null) || view.equals("")) {
			view = "print";
		}
		
		
		Journal journal = null;
		try {
			journal = (Journal) ((Factory) ctx.get(Constants.FACTORY_KEY)).loadResource(idNr);
			ctx.put("thisjournal", journal);
			ctx.put("resource", journal);
			String styleSheet = journal.getStyleSheetUrl();
			if ((styleSheet == null) || !styleSheet.startsWith("ss")) {
				styleSheet = "ss1";
			}
			ctx.put("zcss", "/zeno/css/"+styleSheet+".css");
		}
		catch (NoPermissionException e) {
			journal = null;
		}
		catch (Exception e) {
			journal = null;
		}
		
		
		ctx.put("view", view);
		ctx.put("mode", "struct");
		ctx.put("action", action);
		ctx.put("date", "");
		ctx.put("id", id);
		ctx.put("orderoptions", ZenoUtilities.getOptions(orderOptions, "",
					"article.", msg));
				

//System.out.println("SubscriptionCommand.exec.zcss=" + ctx.get("zcss"));
		if (ctx.get("zcss") == null) {
			ctx.put("zcss","/zeno/css/ss1.css");
		}
//System.out.println("SubscriptionCommand.exec.view=" + view);
		if (view.equals("print")) {
			//*************************** print *************************
			
//			ctx.put("articleCollections", articleCollections);
//			ctx.put("subscribedJournals", subscribedJournals);
		}
/*		else if (view.equals("subscribe")) {
			//*************************** subscribe *************************
			String query = null;
			List unsubscribedJournals = new ArrayList();
			List subscribedJournals = factory.loadSubscribedJournals();
			ctx.put("unsubscribedJournals", unsubscribedJournals);
			ctx.put("subscribedJournals", subscribedJournals);
			ctx.put("query", "");
		}
*/
		else {
			
		}
		return templates.getProperty(view);
	}


	static {
		templates.put("print", "globalsearch/search.vm");
	}
}

