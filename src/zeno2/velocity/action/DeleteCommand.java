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
 *@author     <a href="mailto:lothar.oppor@ais.fhg.de">Lothar Oppor</a>
 *@version    2.0.2, 2001-09-07
 */
public class DeleteCommand extends Command {

	/**
	 *  Constructor
	 *
	 *@param  req
	 *@param  resp
	 */
	public DeleteCommand(HttpServletRequest req, HttpServletResponse resp) {
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
	 */
	public String exec(Context ctx) throws Exception {

		HttpServletRequest req = (HttpServletRequest) ctx.get(VelocityServlet.REQUEST);
		String id = req.getParameter("id");
//System.out.println("DeleteCommand.exec.id=" + id);

		int idNr = 0;
		try {
			idNr = Integer.parseInt(id);
		}
		catch (Exception e) {
			idNr = 0;
		}
		Journal journal = null;
//System.out.println("DeleteCommand.exec.journal=" + journal);
		try {
			ZenoResource resource = (ZenoResource) ((Factory) ctx.get(Constants.FACTORY_KEY)).loadResource(idNr);
//System.out.println("DeleteCommand.exec.resource=" + resource);
			if ((resource instanceof Journal)
					 && (req.getParameter("mode") != null)
					 && req.getParameter("mode").equals("compact")) {
//System.out.println("DeleteCommand.exec: trying to compact");
				((ZenoCollection) resource).compact();
//System.out.println("DeleteCommand.exec: compacted!!!!!!!");
				journal = (Journal) resource;
//System.out.println("DeleteCommand.exec.journal=" + journal);
			}
			else {
				resource.markForDeletion();
//System.out.println("DeleteCommand.exec:marked for deletion");
				journal = (Journal) resource.getParent();
				id = Integer.toString(journal.getId());
			}
			String styleSheet = journal.getStyleSheetUrl();
			if ((styleSheet == null) || !styleSheet.startsWith("ss")) {
				styleSheet = "ss1";
			}
			ctx.put("zcss", "/zeno/css/"+styleSheet+".css");

//System.out.println("DeleteCommand.exec: trying to save");
			resource.save();
//System.out.println("DeleteCommand.exec: saved !!!!!!!");
		}
		catch (Exception e) {
			return showMessage(ctx, "error.no_permission",
					"error.not_adllowed_delete");
		}

		ctx.put("id", id);
//System.out.println("DeleteCommand.exec.id=" + id);
		ctx.put(Constants.JOURNAL_KEY, journal);
		ctx.put("navmode", "struct");

		return EditJournalCommand.templates.getProperty("struct");
	}
}

