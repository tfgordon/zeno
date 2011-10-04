package zeno2.velocity.action;

/*
 *
 *
 *
 *
 */

import java.net.URLEncoder;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.context.Context;
//import org.apache.velocity.app.tools.*;
import org.apache.velocity.servlet.VelocityServlet;

import zeno2.kernel.Constants;
import zeno2.kernel.Journal;
import zeno2.kernel.Factory;
import zeno2.kernel.Principal;
import zeno2.util.ZenoUtilities;
import zeno2.velocity.util.ZenoEncoder;

/**
 *  Handles zeno journal views
 *
 *@author     <a href="mailto:lothar.oppor@ais.fraunhofer.de">Lothar Oppor</a>
 *@version    2.0.2, 2001-09-07 
 */
public class PostConfirmCommand extends Command {

	/**
	 *  Constructor
	 *
	 *@param  req
	 *@param  resp
	 */
	public PostConfirmCommand(HttpServletRequest req, HttpServletResponse resp) {
		super(req, resp);
	}


	/**
	 *  Compact, if the resource is a journal and compact parameter is true
	 *  Otherwise mark the resource for deleted. The handeled resource has to be
	 *  saved!!!
	 *
	 *@param  ctx            Velocity Context
	 *@return                the appropriate template file name
	 *@exception  Exception  only because it is defined in the abstract Command
	 *      class
	 *@since
	 */
	public String exec(Context ctx) throws Exception {

		String id = request.getParameter("id");
		System.out.println("PostConfirmCommand.exec.id=" + id);
		forumServlet = (String) ctx.get("servl");

		int idNr = 0;
		try {
			idNr = Integer.parseInt(id);
		}
		catch (Exception e) {
			idNr = 0;
		}
		Journal journal = null;
		try {
			Factory factory = (Factory) ctx.get(Constants.FACTORY_KEY);
			// only called for journals
			Journal me = (Journal) factory.loadResource(idNr);
			journal = me;
			String styleSheet = me.getStyleSheetUrl();
			if ((styleSheet == null) || !styleSheet.startsWith("ss")) {
				styleSheet = "ss1";
			}
			ctx.put("zcss", "/zeno/css/" + styleSheet + ".css");
			ctx.put("emailparams", makeEmailParams(ctx, me));

			if (request.getParameter("ok") != null) {
				if ("compact".equals(request.getParameter("mode"))) {
					me.compact();
					System.out.println("PostConfirmCommand.exec: compacted!!!!!!!");
				}
				else if ("delete".equals(request.getParameter("mode"))) {
					me.markForDeletion();
					System.out.println("PostConfirmCommand.exec: marked for deletion");
				}
			}
		}
		catch (Exception e) {
			return showMessage(ctx, "error.no_permission",
					"error.not_allowed_delete");
		}

		ctx.put("id", id);
		ctx.put(Constants.JOURNAL_KEY, journal);
		ctx.put("navmode", "struct");

		return EditJournalCommand.templates.getProperty("struct");
	}


	/**
	 *  Creates a parameter String for zeno notification
	 *
	 *@param  ctx  Veleocity context
	 *@param  me   Article
	 *@return      String
	 */
	private String makeEmailParams(
			Context ctx,
			Journal me) {
		ZenoEncoder enc = new ZenoEncoder();
		try {
			StringBuffer strb = new StringBuffer("mailto:?subject=Zeno Mail from ");
			Principal user = (Principal) ctx.get("user");
			strb.append(user.getName());
			strb.append("&body=");
			strb.append(me.getTitle());
			strb.append(" ");
			strb.append(ZenoUtilities.getIsoString(me.getCreationDate()));
			strb.append(" ");
			strb.append(me.getCreator());
			strb.append("  ");
			strb.append("http://");
			strb.append(request.getServerName());
			int port = request.getServerPort();
			if (port != 80) {
				strb.append(":");
				strb.append(Integer.toString(port));
			}
			StringBuffer url = new StringBuffer(forumServlet);
			url.append("?action=editJournal&view=struct&id=");
			url.append(me.getId());
			strb.append(enc.encode(url.toString()));
			//System.out.println("editJournalCommand.makeEmailParams=" + strb.toString());
			return strb.toString();
		}
		catch (Exception e) {
			System.out.println("editArticleCommand.makeEmailParams.error="
					 + e.toString());
			return "";
		}
	}

}

