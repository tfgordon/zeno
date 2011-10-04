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
 *@version    2.0.2 2001-09-07
 */
public class ConfirmCommand extends Command {
	
	String confirmReport = "forum/journal/editJournalConfirmReport.vm";

	/**
	 *  Constructor
	 *
	 *@param  req
	 *@param  resp
	 *@since
	 */
	public ConfirmCommand(HttpServletRequest req, HttpServletResponse resp) {
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
		String mode = request.getParameter("mode");
	
		int idNr = 0;
		try {
			idNr = Integer.parseInt(id);
		}
		catch (Exception e) {
			idNr = 0;
		}
		Journal journal = null;
		List preview = new ArrayList();
		try {
			Factory factory = (Factory) ctx.get(Constants.FACTORY_KEY);
			//only called for journals
			journal = (Journal)factory.loadResource(idNr);
			if ("delete".equals(mode)) {
	System.out.println("ConfirmCommand.exec: deletePreview");
				preview.add(journal.genDeletePreview());
			}
			else if ("compact".equals(mode)) {
	System.out.println("ConfirmCommand.exec:  compactPreview");
				preview.add(journal.genCompactPreview());
			}
			String styleSheet = journal.getStyleSheetUrl();
			if ((styleSheet == null) || !styleSheet.startsWith("ss")) {
				styleSheet = "ss1";
			}
			ctx.put("zcss", "/zeno/css/"+styleSheet+".css");
		}
		catch (Exception e) {
			return showMessage(ctx, "error.no_permission",
					"error.not_adllowed_delete");
		}

		ctx.put("id", id);
		//ctx.put(Constants.JOURNAL_KEY, journal);
		ctx.put("preview", preview);
		ctx.put("navmode", "struct");
		
		ctx.put("action", "postconfirm");
		ctx.put("mode", mode);

		return confirmReport;
	}
}

