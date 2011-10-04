package zeno2.velocity.action;

/*
 *
 *
 *
 *
 */

import java.util.*;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.velocity.context.Context;
import org.apache.velocity.app.tools.*;
import org.apache.velocity.servlet.VelocityServlet;

import zeno2.velocity.util.LinkFormElement;
import zeno2.velocity.util.Tools;
import zeno2.kernel.*;

/**
 *  Handles zeno journal views
 *
 *@author     <a href="mailto:lothar.oppor@ais.fraunhofer.de">Lothar Oppor</a>
 *@version    2.0.2, 2001-09-07
 */
public class EditLinkCommand extends Command {

	/**
	 *  Description of the Field
	 *
	 *@since
	 */
	public static Properties templates = new Properties();


	/**
	 *  Constructor for the ListCommand object
	 *
	 *@param  req   Description of Parameter
	 *@param  resp  Description of Parameter
	 *@since
	 */
	public EditLinkCommand(HttpServletRequest req, HttpServletResponse resp) {
		super(req, resp);
	}


	/**
	 *  Get id and view parameters, get the journal(id), put id and journal into
	 *  the context, and select the appropriate template
	 *
	 *@param  ctx            Velocity Context
	 *@return                the appropriate template file name
	 *@exception  Exception  Description of Exception
	 *@since
	 */
	public String exec(Context ctx) throws Exception {

		String id = request.getParameter("id");
		String targetId = request.getParameter("target");
		String label = request.getParameter("label");
		String xTarget = request.getParameter("xtarget");
		String xName = request.getParameter("xname");
		//System.out.println("EditLinkCommand.exec.id=" + id + ", targetId=" + targetId
		//		 + ", label=" + label);
		int idNr = (new Tools()).toInt(id);
		int targetNr = (new Tools()).toInt(targetId);
		ctx.put("id", id);
		String view = request.getParameter("view");

		if ((view == null) || view.equals("")) {
			view = "edit";
		}
		ctx.put("mode", view);
		ZenoResource me = null;
		ZenoResource target = null;
		try {
			me = ((Factory) ctx.get(Constants.FACTORY_KEY)).loadResource(idNr);
			ctx.put(Constants.ARTICLE_KEY, me);
			Journal parent = (Journal) me.getParent();
			String styleSheet = parent.getStyleSheetUrl();
			if ((styleSheet == null) || !styleSheet.startsWith("ss")) {
				styleSheet = "ss1";
			}
			ctx.put("zcss", "/zeno/css/"+styleSheet+".css");
			ctx.put("parentid",Integer.toString(parent.getId()));
		}
		catch (NoPermissionException e) {
			System.out.println("EditLinkCommand.exec.error=" + e.toString());
			return showMessage(ctx, "error.no_permission");
		}

		if (view.equals("add")) {// new link
			ctx.put("linklabeloptions",
					getLinkLabels(((Journal) me.getParent()).getLinkLabels(), ""));
			//System.out.println("EditLinkCommand.exec going to get LinkFormElement");
			HttpSession sess = request.getSession();
			List bagArticles = new ArrayList();
			List bagJournals = new ArrayList();
			ArrayList bag = (ArrayList) sess.getAttribute(Constants.BAG_KEY);
			//System.out.println("EditLinkCommand.exec.bag=" + bag);
			if (bag == null || bag.isEmpty()) {
				ctx.put("sourcetitle",me.getTitle());
			}
			else {
				targetId = (String) bag.get(0);
				int bagTopIdNr = (new Tools()).toInt(targetId);
				target = ((Factory) ctx.get(Constants.FACTORY_KEY)).loadResource(bagTopIdNr);
				ctx.put("targettitle",target.getTitle());
				ctx.put("sourcetitle",me.getTitle());
				
				
				separateBagArticlesAndBagJournals(ctx,
							bag.iterator(),
							bagArticles,
							bagJournals);
				ctx.put("bagarticles", bagArticles);
				ctx.put("bagjournals", bagJournals);
				
			}
		}
		else if (view.equals("edit")) {
			try {
				System.out.println("EditLinkCommand.exec.me="+me
					+ ", label=" + label
					+ ", targetNr=" + Integer.toString(targetNr));
				String linkLabel = "";
				if (xTarget == null) {
					Link link = me.getLink(label, targetNr);
					System.out.println("EditLinkCommand.exec.link=" + link);
					ctx.put("link", link);
					linkLabel = link.getLabel();
					target = ((Factory) ctx.get(Constants.FACTORY_KEY)).loadResource(targetNr);
					ctx.put("targettitle",target.getTitle());
				}
				else { // external link
					ctx.put("xtarget",xTarget);
					ctx.put("xname",xName);
				}
				Iterator ll = null;
				try {// for the case of top journal, which has got no parent
					ll = getLinkLabels(((Journal) me.getParent()).getLinkLabels(),
							linkLabel);
				}
				catch (Exception e) {
					ll = (new ArrayList()).iterator();
				}
				ctx.put("linklabeloptions", ll);
				ctx.put("sourcetitle",me.getTitle());
				
				//System.out.println("EditLinkCommand.exec.link=" + link);

			}
			catch (Exception e) {
				System.out.println("EditLinkCommand.exec.error=" + e.toString());
				return showMessage(ctx, "error.no_such_link");
			}
		}
		else {
			System.out.println("EditLinkCommand.exec.view=" + view);
			return showMessage(ctx, "error.no_such_view");
		}
		//System.out.println("EditLinkCommand.exec.view=" + view + ", id=" + id);
		return templates.getProperty(view);
	}


	/**
	 *  Gets the LinkLabels attribute of the EditLinkCommand object
	 *
	 *@param  labelsString  Description of Parameter
	 *@param  selected      Description of Parameter
	 *@return               The LinkLabels value
	 *@since
	 */
	private Iterator getLinkLabels(String labelsString, String selected) {
		List labels = new ArrayList();
		StringTokenizer tok = new StringTokenizer(labelsString, ",");
		if (!tok.hasMoreTokens()) {
			labels.add("<option>re</option>");
		}
		while (tok.hasMoreTokens()) {
			String optionSrc = tok.nextToken().trim();
			String optionElement = "<option"
					 + ((optionSrc.equals(selected)) ? " selected" : "")
					 + ">" + optionSrc + "</option>";
			labels.add(optionElement);
		}
		return labels.iterator();
	}

	/**
	 *  Separtates journals and articles in the bag
	 *
	 *@param  bag    input itIterator
	 *@param  ctx    Velocity Context
	 *@param  art    output List for bag articles
	 *@param  journ  output List for bag journals
	 *@since
	 */
	private void separateBagArticlesAndBagJournals(
			Context ctx, Iterator bag, List art, List journ) {

		//System.out.println("EditJournalCommand.separateBagArticlesAndBagJournals.bag="
		//		 + bag);

		while (bag.hasNext()) {
			int idNr = (new Tools()).toInt((String) bag.next());
			ZenoResource resource = null;
			try {
				resource = (ZenoResource) ((Factory) ctx.get(Constants.FACTORY_KEY)).loadResource(idNr);
			}
			catch (Exception e) {
				System.out.println("EditJournalCommand.separateBagArticlesAndBagJournals.error="
						 + e.toString());
			}
			if (resource instanceof Article) {
				art.add(resource);
			}
			else if (resource instanceof Journal) {
				journ.add(resource);
			}
		}
	}

	static {
		templates.put("add", "forum/editLink.vm");
		templates.put("edit", "forum/editLink.vm");
	}
}

