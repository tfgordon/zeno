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

import org.apache.velocity.context.Context;
import org.apache.velocity.app.tools.*;
import org.apache.velocity.servlet.VelocityServlet;

import zeno2.kernel.*;

/**
 *  Handles zeno journal views
 *
 *@author     <a href="mailto:lothar.oppor@ais.fraunhofer.de">Lothar Oppor</a>
 *@version    2.0.2, 2001-09-07
 */
public class EditAttachmentCommand extends Command {

	public static Properties templates = new Properties();


	/**
	 *  Constructor for the ListCommand object
	 *
	 *@param  req   Description of Parameter
	 *@param  resp  Description of Parameter
	 */
	public EditAttachmentCommand(HttpServletRequest req, HttpServletResponse resp) {
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
		int idNr = 0;
		try {
			idNr = Integer.parseInt(id);
		}
		catch (Exception e) {
			idNr = 0;
		}
		ctx.put("id", id);
		Article me = null;
		try {
			me = (Article) ((Factory) ctx.get(Constants.FACTORY_KEY)).loadResource(idNr);
			ctx.put(Constants.ARTICLE_KEY, me);
			Journal parent = (Journal) me.getParent();
			String styleSheet = parent.getStyleSheetUrl();
			if ((styleSheet == null) || !styleSheet.startsWith("ss")) {
				styleSheet = "ss1";
			}
			ctx.put("zcss", "/zeno/css/"+styleSheet+".css");
			ctx.put("parentid",Integer.toString(parent.getId()));
		}
		catch (Exception e) {
			return showMessage(ctx, "error.no_permission",
					"error.not_allowed_edit_article");
		}

		String view = request.getParameter("view");

		if ((view == null) || view.equals("")) {
			view = "edit";
		}
		if (view.equals("add")) {
			ctx.put("rename", "");
			String extension = request.getParameter("extension");
			if (extension != null) ctx.put("extension",extension);
		}
		else if (view.equals("edit")) {
			System.out.println("EditAttachmentCommand.exec.name="
					 + request.getParameter("attachment"));
			ctx.put("rename", request.getParameter("attachment"));
		}
		else {
			return showMessage(ctx);
		}
		//System.out.println("EditAttachmentCommand.exec.view=" + view + ", id=" + id);
		return templates.getProperty(view);
	}
	static {
		templates.put("add", "forum/article/editAttachment.vm");
		templates.put("edit", "forum/article/editAttachment.vm");
		//templates.put("delete", "editArticlePrint.vm");
	}

}

