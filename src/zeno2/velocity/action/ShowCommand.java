package zeno2.velocity.action;

/*
 *
 *
 *
 *
 *///import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.util.List;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.velocity.context.Context;

import zeno2.kernel.Constants;
import zeno2.kernel.Factory;
import zeno2.kernel.XLink;
import zeno2.velocity.util.Errors;

/**
 *  Handles zeno journal views
 *
 *@author     <a href="mailto:lothar.oppor@ais.fraunhofer.de">Lothar Oppor</a>
 *@version    2.0.2,  2002-08-12
 */
public class ShowCommand extends Command {

	/**
	 *  Constructor for the ShowCommand object
	 *
	 *@param  req   Description of Parameter
	 *@param  resp  Description of Parameter
	 *@since
	 */
	public ShowCommand(HttpServletRequest req, HttpServletResponse resp) {
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

		String xId = request.getParameter("id");
		String id = xId;
		System.out.println("ShowCommand.exec.xId=" + xId);
		List xLinks = null;
		forumServlet = (String) ctx.get("servl");
		try {
			Factory factory = (Factory) ctx.get(Constants.FACTORY_KEY);
			xLinks = factory.findXLinks("%", xId, "%");
			System.out.println("ShowCommand.exec.xLinks=" + xLinks);
			if (!xLinks.isEmpty()) {
				XLink xl = (XLink) xLinks.get(0);
				System.out.println("ShowCommand.exec.xl=" + xl);
				id = Integer.toString(xl.getSourceId());
				System.out.println("ShowCommand.exec.id=" + id);
			}
			else {
				String errorIcon = "attntn.gif";
				String[] errors = {
						"error.not_found_article",
				//"information_not_available",
				//"inform_manager",
				//"..."
						};
				return showMessage(ctx, errors, errorIcon);
			}
		}
		catch (Exception e) {
			System.out.println("ShowCommand.exec.error(me)=" + e.toString());
		}

		String url = forumServlet + "?action=editArticle&view=print&id=" + id;
		String urlEncode = response.encodeRedirectURL(url);
		System.out.println("ShowCommand.exec.urlEncode=" + urlEncode);
		response.sendRedirect(urlEncode);
		return null;
	}

}

