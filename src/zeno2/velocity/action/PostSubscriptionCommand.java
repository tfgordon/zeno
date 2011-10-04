package zeno2.velocity.action;

/*
 *
 *
 *
 *
 */

import java.net.URLEncoder;
import java.util.*;
//import java.sql.Date;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.context.Context;
import org.apache.velocity.app.tools.*;
import org.apache.velocity.servlet.VelocityServlet;

import zeno2.kernel.*;
import zeno2.util.ZenoUtilities;
import zeno2.velocity.util.Tools;
import zeno2.velocity.util.Errors;

/**
 *  Handles zeno start views
 *
 *@author     <a href="mailto:andreas.klotz@ais.fraunhofer.de">Andreas Klotz</a>
 *@version    2.0.2, 2001-09-07
 */
public class PostSubscriptionCommand extends Command {
	
	private Errors errors = new Errors();

	/**
	 *  the template file names for all editArticle views
	 *
	 *@since
	 */
	public static Properties templates = new Properties();


	/**
	 *  Constructor for the EditArticleCommand object
	 *
	 *@param  req   Description of Parameter
	 *@param  resp  Description of Parameter
	 *@since
	 */
	public PostSubscriptionCommand(HttpServletRequest req, HttpServletResponse resp) {
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
		
		String view = request.getParameter("view");
		String action = request.getParameter("action");
		String id = request.getParameter("id");
		String dateParam = null;
		String since = null;
		String queryParam = null;
		String query = null;
				
		Date date = null;
		
		int idNr = (new Tools()).toInt(id);
		Journal journal = null;
		try {
			journal = (Journal) ((Factory) ctx.get(Constants.FACTORY_KEY)).loadResource(idNr);
			ctx.put("thisjournal", journal);
			ctx.put("resource", journal);
			ctx.put("journal",journal);
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
		ctx.put("action", action);
		ctx.put("id", id);
		if (ctx.get("zcss") == null) {
			ctx.put("zcss","/zeno/css/ss1.css");
		}
				
//System.out.println("PostSubscriptionCommand.exec.view=" + view);
		
		
		if ((view == null) || view.equals("")) {
			view = "print";
		}
		
		if (view.equals("print")) {
			//*************************** print *******************************
			// String subscribedOnlyParam = request.getParameter("subscribed");
			//if (subscribedOnlyParam.equals("false")) {
			//	subscribedOnly = false;
			//} 
			
			since = request.getParameter("since");
			dateParam = request.getParameter("date");
			if ((dateParam != null) && !dateParam.equals("")) {
				date = makeDate(dateParam);
				if (date == null) {
					errors.addError("error.use_iso_date_for_begin_date");
					dateParam = "";
				}
				since = "date";
			}
			else if (since.equals("login")) {
				date = null;
				dateParam = "";
			}
			else if (since.equals("day")) {
				date = new Date();
				date = ZenoUtilities.plusDays(date, -1);
				dateParam = "";
			}
			else if (since.equals("week")) {
				date = new Date();
				date = ZenoUtilities.plusDays(date, -7);
				dateParam = "";
			}
			else { 
				date = null;
				dateParam = "";
				since = "login";
			}
			ctx.put("since", since);
			ctx.put("date", dateParam);
			//int subscribed = 0; // gets all new articles
			//int subscribed = 1; // gets all new articles from the subscribed journal
			int subscribed = 2;  // gets all new articles from the subscribed journal and its children
			List articleCollections = factory.getArticleCollections(date, subscribed, false);
			ctx.put("articleCollections", articleCollections);
			System.out.println("PostSubscriptionCommand.exec.collectionSize=" +
					articleCollections.size());
			List subscribedJournals = factory.loadSubscribedJournals();
			List subscribedForNotification = factory.loadSubscribedJournalsForNotification(1);
			ctx.put("subscribedJournals", subscribedJournals);
			ctx.put("subscribedForNotification", subscribedForNotification);
		}
		else if (view.equals("subscribe")) {

		//if ((view.equals("subscribe") && request.getParameter("ok") != null)) {
			//*************************** subscribe *************************
			subscribeJournals(ctx, request.getParameterValues("unsubscribedJournals"));
			unSubscribeJournals(ctx, request.getParameterValues("subscribedJournals"));
			//*************************** notify  ZAK code*************************
			setNotify(ctx, request.getParameterValues("notifyMe"));
			unNotify(ctx, request.getParameterValues("unnotifyMe"));
			//*************************** end notify  ZAK code*************************
			//List unsubscribedJournals = factory.searchJournals(query);
			List subscribedJournals = factory.loadSubscribedJournals();
			query = request.getParameter("query");
			if ( query == null ) {
				query = "";
			}
			//ctx.put("unsubscribedJournals", unsubscribedJournals);
			List subscribedForNotification = factory.loadSubscribedJournalsForNotification(1);
			ctx.put("subscribedForNotification", subscribedForNotification);
			ctx.put("subscribedJournals", subscribedJournals);
			ctx.put("query", query);
		}
		else if ((view.equals("search") && request.getParameter("ok") != null) ) {
			//*************************** search *************************
			queryParam = request.getParameter("query");
			query = "%" + queryParam + "%";
			System.out.println("PostSubscriptionCommand.exec.Query: " + query);
			List unsubscribedJournals = factory.searchJournals(query);
			List subscribedJournals = factory.loadSubscribedJournals();
			List subscribedForNotification = factory.loadSubscribedJournalsForNotification(1);
			ctx.put("subscribedForNotification", subscribedForNotification);
			ctx.put("unsubscribedJournals", unsubscribedJournals);
			ctx.put("subscribedJournals", subscribedJournals);
			ctx.put("query", queryParam);
		}
		else if ((request.getParameter("cancel") != null)  ){
			view = "struct";
		}
		else { //error but cannot happen
			view = "struct";
		}
		return templates.getProperty(view);
	}


	 /**
	 *  Sets the Subscribed attribute of the PostStartPageCommand object
	 *
	 *@param  ctx  Velocity Context
	 *@param  ids  String[]
	 *@since
	 */
	private void subscribeJournals(Context ctx, String[] ids) {
		try {
			for (int i = 0; i < ids.length; i++) {
				Factory factory = (Factory)ctx.get(Constants.FACTORY_KEY);
				int id = Integer.valueOf(ids[i]).intValue();
				factory.subscribeJournal(id);
			}
		}
		catch (Exception e) {
			System.out.println("PostStartPageCommand.setIsTopic.error1="+ e.toString());
		}
	}
	
	 /**
	 *  Sets the UnsSubscribed attribute of the PostStartPageCommand object
	 *
	 *@param  ctx  Velocity Context
	 *@param  ids  String[]
	 *@since
	 */
	private void unSubscribeJournals(Context ctx, String[] ids) {
		try {
			for (int i = 0; i < ids.length; i++) {
				Factory factory = (Factory)ctx.get(Constants.FACTORY_KEY);
				int id = Integer.valueOf(ids[i]).intValue();
				factory.unsubscribeJournal(id);
			}
		}
		catch (Exception e) {
			System.out.println("PostStartPageCommand.setIsTopic.error1="+ e.toString());
		}
	}
	
	 //************************* Added code ZAK *******************************

	 /**
	 *  Sets the notify attribute of the PostStartPageCommand object
	 *
	 *@param  ctx  Velocity Context
	 *@param  ids  String[]
	 *@since
	 */

	private void setNotify(Context ctx, String[] ids) {
		try {
			for (int i = 0; i < ids.length; i++) {
				Factory factory = (Factory)ctx.get(Constants.FACTORY_KEY);
				int id = Integer.valueOf(ids[i]).intValue();
				factory.setNotify(id,1);
			}
		}
		catch (Exception e) {
			System.out.println("PostStartPageCommand.setIsTopic.error1="+ e.toString());
		}
	}

	/**
	 *  Sets the unnotify attribute of the PostStartPageCommand object
	 *
	 *@param  ctx  Velocity Context
	 *@param  ids  String[]
	 *@since
	 */

	private void unNotify(Context ctx, String[] ids) {
		try {
			for (int i = 0; i < ids.length; i++) {
				Factory factory = (Factory)ctx.get(Constants.FACTORY_KEY);
				int id = Integer.valueOf(ids[i]).intValue();
				factory.unNotify(id,1);
			}
		}
		catch (Exception e) {
			System.out.println("PostStartPageCommand.setIsTopic.error1="+ e.toString());
		}
	}
		//***************************************************

	/**
	 *  Description of the Method
	 *
	 *@param  s  Description of Parameter
	 *@return    Description of the Returned Value
	 *@since
	 */
	
	private Date makeDate(String s) {
		Date date = null;
		if ((s != null) && !s.equals("")) {
			try {
				date = ZenoUtilities.getDateFromIsoString(s);
			}
			catch (Exception e) {
				date = null;
			}
		}

		return date;
	}

	static {
		templates.put("print", "home/startPage.vm");
		templates.put("subscribe", "forum/subscribe.vm");
		templates.put("search", "forum/subscribe.vm");
		templates.put("struct", "forum/journal/editJournalXStruct.vm");
	}
}

