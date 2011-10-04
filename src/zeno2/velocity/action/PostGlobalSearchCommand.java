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
import zeno2.velocity.util.ZenoBundle;

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
public class PostGlobalSearchCommand extends Command {
	
	private Errors errors = new Errors();

	/**
	 *  the template file names for all editArticle views
	 *
	 *@since
	 */
	public static Properties templates = new Properties();
	
	public final static String orderOptions =
			"modification_date,rank,title,label,qualifier,creator,modifier,"
				+"creation_date,begin_date";


	/**
	 *  Constructor for the EditArticleCommand object
	 *
	 *@param  req   Description of Parameter
	 *@param  resp  Description of Parameter
	 *@since
	 */
	public PostGlobalSearchCommand(HttpServletRequest req, HttpServletResponse resp) {
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
		String fullText = null;
		String title = null;
		String articleLabel = null;		
		String qualifier = null;
		String authorId = null;
		
		
		String fullTextParam = request.getParameter("fulltext");
		if (!fullTextParam.equals("")) {
			fullText = "%" + fullTextParam + "%";
		}
		String titleParam = request.getParameter("title");
		if (!titleParam.equals("")) {
			title = "%" + titleParam + "%";	
		}
		String articleLabelParam = request.getParameter("articlelabel");
		if (!articleLabelParam.equals("")) {
			articleLabel = "%" + articleLabelParam + "%";
		}
		String qualifierParam = request.getParameter("qualifier");
		if (!qualifierParam.equals("")) {
			qualifier = "%" + qualifierParam + "%";
		}
		String authorIdParam = request.getParameter("authorid");
		if (!authorIdParam.equals("")) {
			authorId = "%" + authorIdParam + "%";	
		}
		String fromDateParam = request.getParameter("fromdate");
		if (fromDateParam.equals("")) {
			fromDateParam = null;
		}
		String toDateParam = request.getParameter("todate");
		if (toDateParam.equals("")) {
			toDateParam = null;
		}
		String order = request.getParameter("order");
		if (order.equals("")) {
			order = null;
		}	
		
		Date fromDate = null;
		Date toDate = null;
		String checkForm = "ok";
		if (fullText == null && title == null && articleLabel == null
				&& qualifier == null && authorId == null) {
			checkForm = "error";
		}
		//System.out.println("PostGlobalSearchCommand.exec.checkForm=" + checkForm );
		
		int idNr = (new Tools()).toInt(id);
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
		ctx.put("action", action);
		ctx.put("id", id);
		if (ctx.get("zcss") == null) {
			ctx.put("zcss","/zeno/css/ss1.css");
		}
				
		System.out.println("PostGlobalSearchCommand.exec.view=" + view );
		
		
		if ((view == null) || view.equals("")) {
			view = "print";
		}
		
		if (view.equals("print") && checkForm.equals("ok")) {
			//*************************** subscribe *************************
			
			
			if (fromDateParam != null) {
				fromDate = makeDate(fromDateParam);
				if (fromDate == null) {
					errors.addError("error.use_iso_date_for_begin_date");
				}
			}
			if (toDateParam != null) {
				toDate = makeDate(toDateParam);
				if (toDate == null) {
					errors.addError("error.use_iso_date_for_begin_date");
				}
			}
/*
			System.out.println("PostGlobalSearchCommand.exec.authorId=" + authorId );
			System.out.println("PostGlobalSearchCommand.exec.title=" + title );
			System.out.println("PostGlobalSearchCommand.exec.articleLabel=" + articleLabel );
			System.out.println("PostGlobalSearchCommand.exec.qualifier=" + qualifier );
			System.out.println("PostGlobalSearchCommand.exec.fromDate=" + fromDate );
			System.out.println("PostGlobalSearchCommand.exec.toDate=" + toDate );
			System.out.println("PostGlobalSearchCommand.exec.fullText=" + fullText );
			System.out.println("PostGlobalSearchCommand.exec.order=" + order );
*/
			List articleCollections = factory.searchArticles(	authorId,
																title,
																articleLabel,
																qualifier,
																fromDate,
																toDate,
																fullText,
																order);
			ctx.put("articleCollections", articleCollections);
			ctx.put("authorid", authorIdParam);
			ctx.put("title", titleParam);
			ctx.put("articlelabel", articleLabelParam);
			ctx.put("qualifier", qualifierParam);
			ctx.put("fromdate", fromDateParam);
			ctx.put("todate", toDateParam);
			ctx.put("fulltext", fullTextParam);
			ctx.put("order", order);
			ctx.put("orderoptions", ZenoUtilities.getOptions(orderOptions, order,
					"article.", msg));
			ctx.put("checkform", checkForm);
			
		}
		else {
			ctx.put("authorid", authorIdParam);
			ctx.put("title", titleParam);
			ctx.put("articlelabel", articleLabelParam);
			ctx.put("qualifier", qualifierParam);
			ctx.put("fromdate", fromDateParam);
			ctx.put("todate", toDateParam);
			ctx.put("fulltext", fullTextParam);
			ctx.put("order", order);
			ctx.put("orderoptions", ZenoUtilities.getOptions(orderOptions, order,
					"article.", msg));
			ctx.put("checkform", checkForm);
		}
		//System.out.println("EditArticleCommand.exec.view=" + view + ", id=" + id);
		return templates.getProperty(view);
	}

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
		templates.put("print", "globalsearch/search.vm");
	}
}

