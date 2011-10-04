package zeno2.velocity.action;

/*
 *
 *
 *
 *
 */

//import java.net.URLEncoder;
import java.util.ArrayList;
import java.sql.Date;
import java.util.List;
import java.util.Properties;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.context.Context;
//import org.apache.velocity.app.tools.*;
import org.apache.velocity.servlet.VelocityServlet;

import zeno2.kernel.Constants;
import zeno2.kernel.Factory;
import zeno2.kernel.Journal;
import zeno2.kernel.PreviewElement;
import zeno2.kernel.ZenoException;
import zeno2.db.FactoryImpl;
import zeno2.util.ZenoUtilities;
import zeno2.velocity.util.Errors;
import zeno2.velocity.util.Tools;

/**
 *  Handles zeno start views
 *
 *@author     <a href="mailto:andreas.klotz@ais.fraunhofer.de">Andreas Klotz</a>
 *@version    2.0.2, 2001-09-07
 */
public class PostEditMarkedCommand extends Command {

	private final String home = "home/subscribe.vm";
	private final String show = "home/editMarked.vm";
	private final String confirm = "forum/journal/editJournalConfirmReport.vm";

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
	public PostEditMarkedCommand(HttpServletRequest req, HttpServletResponse resp) {
		super(req, resp);
	}


	/**
	 *  Put id, title, note, keywords, rank expires and eventually georef into the
	 *  contextAdd view as modeto the contextIf view=new, add the journal to
	 *  the context, else add the articlefinaly return the filename of the
	 *  appropriate template.
	 *
	 *@param  ctx            Velocity Context
	 *@return                the appropriate template file name
	 *@exception  Exception  no exception is thrown
	 *@since
	 */
	public String exec(Context ctx) throws Exception {

		Factory factory = (Factory) ctx.get(Constants.FACTORY_KEY);

		String mode = request.getParameter("mode");
		//String action = request.getParameter("action");

		String navmode = "struct";

		ctx.put("navmode", navmode);
		ctx.put("zcss", "/zeno/css/ss1.css");
		forumServlet = (String) ctx.get("servl");

		if (request.getParameter("ok") != null) {

			if ("confirm".equals(mode)) {

				List preview = new ArrayList();

				String[] idsToUndelete = request.getParameterValues("undelete");
				String[] idsToXUndelete = request.getParameterValues("xundelete");
				String[] idsToRemove = request.getParameterValues("remove");

				genPreview(factory, idsToUndelete, "undelete", preview);
				genPreview(factory, idsToXUndelete, "xundelete", preview);
				genPreview(factory, idsToRemove, "remove", preview);

				if (!preview.isEmpty()) {

					ctx.put("preview", preview);
					ctx.put("action", "postEditMarked");
					ctx.put("mode", "exec");

					return confirm;
				}
			}

			if ("exec".equals(mode)) {

//System.out.println("execute confirmed operations");

				String[] idsToUndelete = request.getParameterValues("undelete");
				String[] idsToXUndelete = request.getParameterValues("xundelete");
				String[] idsToRemove = request.getParameterValues("remove");

				unmarkForDeletion(factory, idsToUndelete, false);
				unmarkForDeletion(factory, idsToXUndelete, true);
				remove(factory, idsToRemove, ctx);
			}
		}
		else if (request.getParameter("cancel") != null) {
			String url = forumServlet + "?action=subscription&view=print";
			String urlEncode = response.encodeRedirectURL(url);
			response.sendRedirect(urlEncode);
			return null;
		}

		List markedJournals = ((FactoryImpl) factory).loadMarkedJournals();
		ctx.put("markedJournals", markedJournals);

		List subscribedJournals = factory.loadSubscribedJournals();
		ctx.put("subscribedJournals", subscribedJournals);

		return show;
	}


	/**
	 *  generates a previewElement for each journal from ids;&nbsp;if it is null
	 *  i.eno
	 *  need to get confirmation the operation called for is immediately executed;
	 *  all other elements are added to preview
	 *
	 *@param  factory  Description of Parameter
	 *@param  ids      Description of Parameter
	 *@param  mode     Description of Parameter
	 *@param  preview  Description of Parameter
	 */

	private void genPreview(Factory factory,
			String[] ids,
			String mode,
			List preview) {

		Journal jn;
		PreviewElement element;
		int size = ids != null ? ids.length : 0;

		for (int i = 0; i < size; i++) {
			int id = Integer.parseInt(ids[i]);
			try {
				jn = (Journal) factory.loadResource(id);
				if (mode.equals("remove")) {
//System.out.println("removePreview called for " + jn);
					element = jn.genRemovePreview();
					preview.add(element);
				}
				else if (mode.equals("undelete")) {
//System.out.println("undeletePreview called for " + jn);
					element = jn.genUndeletePreview("undelete");
					if (element != null) {
						preview.add(element);
					}
					else {
//System.out.println("undelete immediately for " + jn);
						jn.unmarkForDeletion(false);
					}
				}
				else if (mode.equals("xundelete")) {
//System.out.println("xundeletePreview called for " + jn);
					element = jn.genUndeletePreview("xundelete");
					if (element != null) {
						preview.add(element);
					}
					else {
//System.out.println("xundelete immediately for " + jn);
						jn.unmarkForDeletion(true);
					}
				}

			}
			catch (ZenoException e) {
				System.out.println("PostEditMarked.genReport " + e);
			}
		}
	}


	/**
	 *  Description of the Method
	 *
	 *@param  factory    Description of Parameter
	 *@param  ids        Description of Parameter
	 *@param  propagate  Description of Parameter
	 */
	private void unmarkForDeletion(Factory factory, String[] ids,
			boolean propagate) {
		if (ids == null) {
			return;
		}

		for (int i = 0; i < ids.length; i++) {
			int idNo = 0;
			try {
				idNo = Integer.parseInt(ids[i]);
				Journal jn = (Journal) factory.loadResource(idNo);
				System.out.println("unmark " + jn);
				jn.unmarkWithAncestors();
				jn.unmarkForDeletion(propagate);
			}
			catch (Exception e) {
				// do nothing
			}
		}
	}


	/**
	 *  Description of the Method
	 *
	 *@param  factory  Description of Parameter
	 *@param  ids      Description of Parameter
	 *@param  ctx      Description of Parameter
	 */
	private void remove(Factory factory, String[] ids, Context ctx) {
		System.out.print("PostEditMarkedCommand.remove.ids=");
		if (ids == null) {
			return;
		}

		Errors errors = new Errors();
		for (int i = 0; i < ids.length; i++) {

			System.out.print("(" + ids[i] + "),");
			int idNo = 0;
			try {
				idNo = Integer.parseInt(ids[i]);
				Journal jn = (Journal) factory.loadResource(idNo);
				System.out.println("remove " + jn);
				try {
					jn.remove();
				}
				catch (Exception e) {
					errors.addError("error." + e.getMessage());

				}
			}
			catch (Exception e) {
				System.out.println("PostEditMarkedCommand.remove.error=" + e);
			}
		}
		System.out.println(".");
		if (errors.isNotEmpty()) {
			ctx.put(Constants.ERRORS, errors.getErrors());
		}
	}


}

