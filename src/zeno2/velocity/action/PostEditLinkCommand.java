package zeno2.velocity.action;

/*
 *
 *
 *
 *
 */

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.velocity.context.Context;
import org.apache.velocity.servlet.VelocityServlet;

import zeno2.kernel.Article;
import zeno2.kernel.Constants;
import zeno2.kernel.Factory;
import zeno2.kernel.Journal;
import zeno2.kernel.Principal;
import zeno2.kernel.Topic;
import zeno2.kernel.XLink;
import zeno2.kernel.ZenoResource;
import zeno2.util.ZenoUtilities;
import zeno2.velocity.util.Errors;
import zeno2.velocity.util.Tools;
import zeno2.velocity.util.ZenoEncoder;

/**
 *  Handles zeno links
 *
 *@author     <a href="mailto:lothar.oppor@ais.fraunhofer.de">Lothar Oppor</a>
 *@version    2.0.2, 2001-09-25
 */
public class PostEditLinkCommand extends Command {

	/**
	 *  Constructor for the PostEditLinkCommand object
	 *
	 *@param  req   Description of Parameter
	 *@param  resp  Description of Parameter
	 *@since
	 */
	public PostEditLinkCommand(HttpServletRequest req, HttpServletResponse resp) {
		super(req, resp);
	}


	/**
	 *  Get parameters of editLink template handle this template
	 *
	 *@param  ctx            Velocity Context
	 *@return                the appropriate template file name
	 *@exception  Exception  The original Java Exception
	 *@since
	 */
	public String exec(Context ctx) throws Exception {

		HttpServletResponse res = (HttpServletResponse) ctx.get(VelocityServlet.RESPONSE);
		HttpSession sess = request.getSession();
		ArrayList bag = (ArrayList) sess.getAttribute(Constants.BAG_KEY);

		String view = request.getParameter("mode");
		String id = request.getParameter("id");
		int idNr = (new Tools()).toInt(id);
		ZenoResource me = null;
		forumServlet = (String) ctx.get("servl");
		try {
			//get parameters
			me = ((Factory) ctx.get(Constants.FACTORY_KEY)).loadResource(idNr);
			ctx.put("id", id);
			String linkLabel = request.getParameter("linklabel");
			if (linkLabel == null) {
				linkLabel = "";
			}
			String oldLinkLabel = request.getParameter("oldlinklabel");
			String targetId = request.getParameter("targetid");
			int targetIdNr = (new Tools()).toInt(targetId);
			String targetTitle = request.getParameter("targettitle");
			ctx.put("targettitle", targetTitle);
			String xTarget = request.getParameter("xtarget");
			String oldXTarget = request.getParameter("oldxtarget");
			String xName = request.getParameter("xname");
			String probe = request.getParameter("probe");
			Journal parent = (Journal) me.getParent();
			String styleSheet = parent.getStyleSheetUrl();
			if ((styleSheet == null) || !styleSheet.startsWith("ss")) {
				styleSheet = "ss1";
			}
			ctx.put("zcss", "/zeno/css/" + styleSheet + ".css");
			ctx.put("emailparams", makeEmailParams(ctx, parent));
			ctx.put("parentid",Integer.toString(parent.getId()));

			if ((request.getParameter("delete") != null)) {
				if (xTarget != null) {
					int p = oldXTarget.indexOf(":");
					String xType = (p > 0) ? oldXTarget.substring(0, p) : "http";
					me.deleteXLink(xType, oldXTarget);
				}
				else {
					me.deleteLink(oldLinkLabel, targetIdNr);
				}
			}
			else if ((request.getParameter("ok") != null)) {
				// ****************** add ***************
				if (view.equals("add")) {
					//**************** add external link *******
					if ((xTarget != null) && !xTarget.equals("http://")
							 && !xTarget.equals("")) {
						if (xName.equals("")) {
							xName = xTarget;
						}
						int p = xTarget.indexOf(":");
						String xType = (p > 0) ? xTarget.substring(0, p) : "http";
						me.addXLink(xType, xTarget, xName);
					}
					//**************** add internal links ******
					if ((request.getParameter("target") != null)) {
						String[] targets = request.getParameterValues("target");
						if (probe.equals("source")) {
							for (int i = 0; i < targets.length; i++) {
								int tIdNr = (new Tools()).toInt(targets[i]);
								if (!bag.isEmpty() && (bag.indexOf(targets[i]) >= 0)) {
									me.addLink("", linkLabel, tIdNr, "");
									deleteFromBag(bag, targets[i]);
								}
								else {
									System.out.println("PostEditLinkCommand.exec.error="
											 + targets[i] + " not in bag");
								}
							}
						}
						else {
							for (int i = 0; i < targets.length; i++) {
								int tIdNr = (new Tools()).toInt(targets[i]);
								ZenoResource zres = null;
								try {
									zres =
											((Factory) ctx.get(Constants.FACTORY_KEY)).loadResource(tIdNr);
								}
								catch (Exception e) {
									System.out.println("PostEditLinkCommand.exec.error="
											 + e.toString());
								}
								zres.addLink("", linkLabel, idNr, "");
								deleteFromBag(bag, targets[i]);
							}
						}
					}
				}
				else if (view.equals("edit")) {
					if (xTarget != null) {
						if (!xTarget.equals(oldXTarget)) {
							int p = oldXTarget.indexOf(":");
							String xOType = (p > 0) ? oldXTarget.substring(0, p) : "http";
							me.deleteXLink(xOType, oldXTarget);
							p = xTarget.indexOf(":");
							String xType = (p > 0) ? xTarget.substring(0, p) : "http";
							me.addXLink(xType, xTarget, xName);
						}
						else {
							XLink xlink = me.getXLink("http", oldXTarget);
							xlink.setName(xName);
							xlink.save();
						}
					}
					else if (!linkLabel.equals(oldLinkLabel)) {
						me.deleteLink(oldLinkLabel, targetIdNr);
						me.addLink("", linkLabel, targetIdNr, "");
					}
				}
			}
			else {//cancel button was pressed
				// ?????????????????//
			}
		}
		catch (Exception e) {
			System.out.println("PostEditLinkCommand.exec.exception=-----"
					 + e.toString());
			e.printStackTrace();
			return showMessage(ctx, "error.unknownError");
		}
		if (me instanceof Journal) {
			ctx.put(Constants.JOURNAL_KEY, me);
			return EditJournalCommand.templates.getProperty("struct");
		}
		else if (me instanceof Topic) {
			//ctx.put(Constants.TOPIC_KEY, me);
			ctx.put(Constants.ARTICLE_KEY, me);
			return EditArticleCommand.templates.getProperty("print");
		}
		else if (me instanceof Article) {
			ctx.put(Constants.ARTICLE_KEY, me);
			return EditArticleCommand.templates.getProperty("print");
		}
		else {
			System.out.println("PostEditLinkCommand.exec.me=" + me);
			return showMessage(ctx, "error.unknownError");
		}
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
	 *  Description of the Method
	 *
	 *@param  bag  Description of Parameter
	 *@param  id   Description of Parameter
	 */
	private void deleteFromBag(ArrayList bag, String id) {
		if (bag.isEmpty()) {
			return;
		}
		int j = bag.indexOf(id);
		if (j >= 0) {
			bag.remove(j);
		}

	}


	/**
	 *  Creates a parameter String for zeno notification
	 *
	 *@param  ctx  Veleocity context
	 *@param  me   Article
	 *@return      String
	 */
/*
	private String makeEmailParams(
			Context ctx,
			ZenoResource me) {
		ZenoEncoder enc = new ZenoEncoder();
		try {
			StringBuffer strb = new StringBuffer("mailto:?subject=Zeno Mail from ");
			Principal user = (Principal) ctx.get("user");
			strb.append(user.getName());
			strb.append("&body=");
			if (me instanceof Article) {
				strb.append(((Article) me).getLabel());
				strb.append(": ");
			}
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
			url.append("?action=editArticle&view=print&id=");
			url.append(me.getId());
			strb.append(enc.encode(url.toString()));
			return strb.toString();
		}
		catch (Exception e) {
			System.out.println("PosteditArticleCommand.makeEmailParams.error="
					 + e.toString());
			return "";
		}
	}
*/

}

