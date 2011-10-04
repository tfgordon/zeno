package zeno2.velocity.action;

/*
 *
 *
 *
 *
 */
import java.lang.reflect.Method;
import java.net.URLEncoder;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

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
import zeno2.kernel.Monitor;
import zeno2.kernel.Principal;
import zeno2.kernel.Topic;
import zeno2.kernel.ZenoResource;
import zeno2.util.ZenoUtilities;
import zeno2.velocity.util.Errors;
import zeno2.velocity.util.Tools;
import zeno2.velocity.util.ZenoEncoder;


/**
 *  Handles zeno articles
 *
 *@author     <a href="mailto:lothar.oppor@ais.fraunhofer.de">Lothar Oppor</a>
 *@version   2.0.2, 2001-09-1-07
 */
public class PostEditArticleCommand extends Command {

	private Errors errors = new Errors();
	private Factory factory = null;
	private Article me = null;


	/**
	 *  Constructor for the PostEditArticleCommand object
	 *
	 *@param  req   Description of Parameter
	 *@param  resp  Description of Parameter
	 *@since
	 */
	public PostEditArticleCommand(HttpServletRequest req,
			HttpServletResponse resp) {
		super(req, resp);
	}


	/**
	 *  Get parameters of editArticle template handle this template
	 *
	 *@param  ctx            Velocity Context
	 *@return                the appropriate template file name
	 *@exception  Exception  The original Java Exception
	 *@since
	 */
	public String exec(Context ctx) throws Exception {

		String parentId = request.getParameter("parentid");
		String id = request.getParameter("id");
		forumServlet = (String) ctx.get("servl");

		int parentIdNr = (new Tools()).toInt(parentId);
		int idNr = (new Tools()).toInt(id);
//System.out.println("PostEditArticleCommand.exec.id="+id
//+", parentId="+parentId);

		try {
			// get parameters
			factory = (Factory) ctx.get(Constants.FACTORY_KEY);
			String mode = request.getParameter("mode");
			String modeTemplate = EditArticleCommand.templates.getProperty(mode);
			String title = request.getParameter("title");
			String rank = request.getParameter("rank");
			String expires = request.getParameter("expires");
			String beginDateParam = request.getParameter("begindate");
			String endDateParam = request.getParameter("enddate");
			boolean isTopic = (request.getParameter("isTopic") != null);
			int rankNr = (new Tools()).toInt(rank);
			if (!mode.equals("bag") && (rankNr < 0)) {
				errors.addError("error.invalid_rank");
				//rankNr = 0;
			}
			Journal parent = null;
			if (mode.equals("new")) {
				parent = (Journal) ((Factory) ctx.get(Constants.FACTORY_KEY)).loadResource(parentIdNr);
				ctx.put("emailparams", makeEmailParams(ctx, parent));
			}
			else {
				me = (Article) ((Factory) ctx.get(Constants.FACTORY_KEY)).loadResource(idNr);
				parent = (Journal) me.getParent();
				parentId = Integer.toString(parent.getId());
				ctx.put("emailparams", makeEmailParams(ctx, me));
			}
			ctx.put("id", parentId);
			ctx.put(Constants.JOURNAL_KEY, parent);
			String styleSheet = parent.getStyleSheetUrl();
			ctx.put("mailalias", parent.getMailAlias());
			if ((styleSheet == null) || !styleSheet.startsWith("ss")) {
				styleSheet = "ss1";
			}
			ctx.put("zcss", "/zeno/css/" + styleSheet + ".css");
			String zenoPlugins = parent.getProperty("zenoExtensions");

			// ********** OK or OK + Attachment pushed ******************
			if ((request.getParameter("ok") != null)
						 || (request.getParameter("ok_attachment") != null)){
				Date expirationDate = null;
				Date beginDate = null;
				Date endDate = null;
				if (containsDoubleQuote(title)) {
					title = (new Tools()).filterQuote(title);
					errors.addError("error.no_double_quote");
				}
				else if (title.equals("")) {
					errors.addError("error.title_required");
				}

				if ((beginDateParam != null) && !beginDateParam.equals("")) {
					beginDate = makeDate(beginDateParam);
					if (beginDate == null) {
						errors.addError("error.use_iso_date_for_begin_date");
					}
				}
				if ((endDateParam != null) && !endDateParam.equals("")) {
					endDate = makeDate(endDateParam);
					if (endDate == null) {
						errors.addError("error.use_iso_date_for_end_date");
					}
				}
				else {
					endDate = beginDate;
				}
				if ((expires != null) && !expires.equals("")) {
					expirationDate = makeDate(expires);
					if (expirationDate == null) {
						errors.addError("error.use_iso_date_for_expiration_date");
					}
				}
				if (errors.isNotEmpty()) {
					ctx.put(Constants.ARTICLE_KEY, me);
					ctx.put("parentid", parentId);
					ctx.put("id", id);
					ctx.put("title", title);
					ctx.put("rank", rank);
					ctx.put("mode", mode);
					ctx.put("expires", expires);
					ctx.put("begindate", beginDateParam);
					ctx.put("enddate", endDateParam);
					ctx.put("keywords", request.getParameter("keywords"));
					ctx.put("note", request.getParameter("note").trim());
					ctx.put("articlelabeloptions", ZenoUtilities.getOptions(
							parent.getArticleLabels(), request.getParameter("label")));
					ctx.put("qualifieroptions", ZenoUtilities.getOptions(
							parent.getQualifiers(), request.getParameter("qualifier")));
					ctx.put("linklabeloptions", ZenoUtilities.getOptions(
							parent.getLinkLabels(), request.getParameter("linklabel")));
					ctx.put(Constants.ERRORS, errors.getErrors());
					return modeTemplate;
				}
//System.out.println("PostEditArticleCommand.exec.mode="+mode);
//******************* edit mode ******************************************
				if (mode.equals("edit")) {
System.out.println("PostEditArticleCommand.exec(edit).me="+me);
					me.setKeywords(request.getParameter("keywords"));
					me.setLabel(request.getParameter("label"));
					String qualifier = request.getParameter("qualifier");
					//if (qualifier != null && !qualifier.equals("")) {
					if (qualifier != null) {
						me.setQualifier(qualifier);
					}
					//System.out.println("PostEditArticleCommand.exec.qualifier=" +
					//		request.getParameter("qualifier"));
					me.setNote(request.getParameter("note"));
					me.setTitle(title);
					me.setRank(rankNr);
					me.setBeginDate(beginDate);
					me.setEndDate(endDate);
					me.setExpirationDate(expirationDate);
					String notifyCreatorParam = request.getParameter("notifycreator");
					if (notifyCreatorParam != null) {
						//art.setNotifyCreator(true);
						me.setProperty("notifyCreator", "true");
					}
					else {
						me.setProperty("notifyCreator", "false");
					}
					ctx.put(Constants.ARTICLE_KEY, me);
					ctx.put("parentid", parentId);
					ctx.put("id", id);
					if (request.getParameter("ok_attachment") != null) {
						ctx.put("view","add");
						ctx.put("rename","");
						return EditAttachmentCommand.templates.getProperty("add");
					}
				}
//******************* bag mode *******************************************
				else if (mode.equals("bag")) {
					if (!(me instanceof Topic)) {
						return showMessage(ctx, "error.unable");
					}
					ctx.put(Constants.ARTICLE_KEY, me);
					ctx.put("parentid", Integer.toString(parent.getId()));
					ctx.put("resource",me);
					ctx.put("id", Integer.toString(me.getId()));
System.out.println("PostEditArticleCommand.exec.me="+me
+", meAsTopic="+(Topic)me);
					moveOrCopy(ctx, (Topic) me);
					mode = "print";
				}

//******************* new mode or respond ********************************
				else {
					//create new article
					//heg
					Article art;
					if (mode.equals("respond")) {
						art = me.createReply(request.getParameter("linklabel"));
						String notifyCreator = me.getProperty("notifyCreator");
						if ((notifyCreator != null)
								 && notifyCreator.equals("true")) {
							String newId = Integer.toString(art.getId());
							notifyCreator(ctx, me, newId, title);
						}
					}
					else {
						art = factory.createTopic(parent);
					}

					art.setKeywords(request.getParameter("keywords"));
					String label = request.getParameter("label");
					if (label != null) {
						art.setLabel(label);
					}
					String qualifier = request.getParameter("qualifier");
					if (qualifier != null) {
						try {
						art.setQualifier(qualifier);
						}
						catch (Exception e) {
							// writers are not allowed to set qualifiers
						}
					}
					art.setNote(request.getParameter("note").trim());
					art.setTitle(title);
					art.setRank(rankNr);
					art.setBeginDate(beginDate);
					art.setEndDate(endDate);
					art.setExpirationDate(expirationDate);
					String notifyCreatorParam = request.getParameter("notifycreator");
					if (notifyCreatorParam != null) {
						//art.setNotifyCreator(true);
						art.setProperty("notifyCreator", "true");
					}
					else {
						art.setProperty("notifyCreator", "false");
					}
					me = art;
					if (zenoPlugins != null) {
						me.setProperty("zenoExtensions", zenoPlugins);
					}
					me.save();
					me.finish(false);
					ctx.put(Constants.ARTICLE_KEY, me);
					ctx.put("parentid", Integer.toString(parent.getId()));
					ctx.put("resource",me);
					ctx.put("id", Integer.toString(art.getId()));
//System.out.println("PostEditArticleCommand.exec.ok_attachment="
//+request.getParameter("ok_attachment"));
					if (request.getParameter("ok_attachment") != null) {
						ctx.put("view","add");
						ctx.put("rename","");
						return EditAttachmentCommand.templates.getProperty("add");
					}
				}
				me.save();
//System.out.println("PostEditArticleCommand.exec: all saved");
				//ctx.put("id", Integer.toString(me.getId()));
				//ctx.put(Constants.ARTICLE_KEY, me);
			}
			else if ((request.getParameter("cancel") != null) &&
					(mode.equals("respond") ||mode.equals("new"))){
				ctx.put(Constants.JOURNAL_KEY,parent);
				ctx.put("resource",parent);
				ctx.put("id", Integer.toString(parent.getId()));
				ctx.put("parentid", Integer.toString(parent.getParent().getId()));
				return EditJournalCommand.templates.getProperty("struct");

			}
		}
		catch (Exception e) {
			System.out.println("PostEditArticleCommand.exec.error=" + e.toString());
			//throw e;
			return showMessage(ctx,"error.no_such_article","error.just_deleted");
		}
		ctx.put(Constants.ARTICLE_KEY,me);
		ctx.put("resource",me);
		ctx.put("id", Integer.toString(me.getId()));
		ctx.put("parentid", Integer.toString(me.getParent().getId()));
		return EditArticleCommand.templates.getProperty("print");

	}


	/**
	 *  Makes a date from an iso date and time String
	 *
	 *@param  s  iso date and time String
	 *@return    the date if valid, else null
	 *@since
	 */
	private Date makeDate(String s) {
		Date date = null;
		if ((s != null) && !s.equals("")) {
			try {
				date = ZenoUtilities.getDateAndTimeFromIsoString(s.trim());
//System.out.println("PostEditArticleCommand.makeDate.s="+s+" date="+date);
			}
			catch (Exception e) {
				date = null;
			}
		}

		return date;
	}


	/**
	 *  Description of the Method
	 *
	 *@param  ctx    Description of Parameter
	 *@param  me     Description of Parameter
	 *@param  title  Description of Parameter
	 */
	private void notifyCreator(Context ctx, Article me,
			String newId, String title) {
		try {
			String creatorId = me.getCreator();
			String userId = ((Factory) ctx.get(Constants.FACTORY_KEY))
					.getUser().getId();
			if (userId.equals(creatorId)) {
				return;
			}
			Principal creator = ((Factory) ctx.get(Constants.FACTORY_KEY))
					.loadPrincipal(creatorId);
			Monitor monitor = (Monitor) ctx.get(Constants.MONITOR_KEY);
			String mailhost = monitor.getProperty("zenoMailServer", "no_zenoMailServer");
			String creatorEmailAddress = creator.getEmail();
			String myTitle = me.getTitle();
			String zenoHotline = monitor.getProperty("zenoHotlineEmailAddress",
					"No_zenoHotlineEmailAddress");
			String[] from = {zenoHotline, "Zeno Support"};
			String subject = "Response to Article:  " + myTitle;
			Principal answerer = ((Factory) ctx.get(Constants.FACTORY_KEY)).getUser();
			StringBuffer url = new StringBuffer("http://");
			url.append(request.getServerName());
			int port = request.getServerPort();
			if (port != 80) {
				url.append(":");
				url.append(Integer.toString(port));
			}
			url.append(forumServlet);
			url.append("?action=editArticle&view=print&id=");
			url.append(newId);
			String body = answerer.getName() + ": " + title + "\n" + url.toString();
//System.out.println("PostEditArticleCommand/notifyCreator.mailhost="
//+mailhost+", creatorEmailAddress="+creatorEmailAddress
//+", from="+from+", subject="+subject+", body="+body);

			ZenoUtilities.sendNotice(mailhost,
					creatorEmailAddress,
					from,
					subject,
					body);
		}
		catch (Exception e) {
			System.out.println("EditArticleCommandnotifyCreator.error=" + e.toString());
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

	/**
	 *  Description of the Method
	 *
	 *@param  ctx  Description of Parameter
	 *@param  me   Description of Parameter
	 *@since
	 */
	private void moveOrCopy(Context ctx, Topic me) {
//System.out.println("PostEditTopicCommand.moveOrCopy.me=" + me);
		HttpSession sess = request.getSession();
		ArrayList bag = (ArrayList) sess.getAttribute(Constants.BAG_KEY);
//System.out.println("PostEditTopicCommand.moveOrCopy.bag=" + bag);
		if (bag == null) {
			bag = new ArrayList();
		}
		try {
			String[] moveCandidates = filter(request.getParameterValues("move"), bag);
			String[] copyeCandidates = filter(request.getParameterValues("copy"), bag);
			ArrayList mResIds = makeArrayList(moveCandidates);
			ArrayList cResIds = makeArrayList(copyeCandidates, mResIds);
			Set mObj = makeObjectSet(ctx, mResIds);
//System.out.println("PostEditTopicCommand.moveOrCopy.mObj=" + mObj);
			Set cObj = makeObjectSet(ctx, cResIds);
//System.out.println("PostEditTopicCommand.moveOrCopy.cObj=" + cObj);
			if (!mObj.isEmpty()) {
				me.moveHereArticles(mObj);
			}
			if (!cObj.isEmpty()) {
				me.copyHereArticles(cObj, true);// copy with links
			}
			deleteFromBag(bag, mResIds);
			deleteFromBag(bag, cResIds);
		}
		catch (Exception e) {
			System.out.println("PostEditTopicCommand.moveOrCopy.error="
					 + e.toString());
		}
		sess.setAttribute(Constants.BAG_KEY, bag);
//System.out.println("PostEditTopicCommand.markForBag.bag+=" + bag);
	}


	/**
	 *  Description of the Method
	 *
	 *@param  ids      String[]
	 *@param  without  Description of Parameter
	 *@return          ArrayList
	 *@since
	 */
	private ArrayList makeArrayList(String[] ids, ArrayList without) {
		ArrayList result = new ArrayList();
		if (ids == null) {
			return result;
		}
		for (int i = 0; i < ids.length; i++) {
			if (without.isEmpty() || !without.contains(ids[i])) {
				result.add(ids[i]);
			}
		}
		return result;
	}


	/**
	 *  Description of the Method
	 *
	 *@param  ids  Description of Parameter
	 *@return      Description of the Returned Value
	 *@since
	 */
	private ArrayList makeArrayList(String[] ids) {
		return makeArrayList(ids, new ArrayList());
	}


	/**
	 *  removes all ZenoResources, which are contained in the bag List ids, from
	 *  the bag
	 *
	 *@param  bag  Array list
	 *@param  ids  ArrayList
	 *@since
	 */
	private void deleteFromBag(ArrayList bag, ArrayList ids) {
		if (bag.isEmpty()) {
			return;
		}
		for (int i = 0; i < ids.size(); i++) {
			String probe = (String) ids.get(i);
			int j = bag.indexOf(probe);
			if (j >= 0) {
				bag.remove(j);
			}
		}
	}


	/**
	 *  Filters html form checklist values. <br>
	 *  Only those, which are in the bag are passed.
	 *
	 *@param  bag  input itIterator
	 *@param  in   Description of Parameter
	 *@return      Description of the Returned Value
	 *@return      String[], members of in, which are also containe in bag
	 */
	private String[] filter(String[] in, ArrayList bag) {
		if ((in == null) || (in.length == 0)) {
			return null;
		}
		String[] pass = new String[in.length];
		int to = 0;
		for (int i = 0; i < in.length; i++) {
			if (bag.indexOf(in[i]) >= 0) {
				pass[to] = in[i];
				to++;
			}
		}
		String[] result = new String[to];
		System.arraycopy(pass, 0, result, 0, to);
		return result;
	}


	/**
	 *  Converts an Arraylist to a Set
	 *
	 *@param  ids  ArrayList
	 *@param  ctx  Velocity Context
	 *@return      Set of members contained in the ids list
	 *@since
	 */
	private Set makeObjectSet(Context ctx, ArrayList ids) {
		HashSet result = new HashSet();
		if (ids == null) {
			return result;
		}
		for (int i = 0; i < ids.size(); i++) {
			int idNo = (new Tools()).toInt((String) ids.get(i));
			try {
				ZenoResource zr = factory.loadResource(idNo);
				result.add(zr);
			}
			catch (Exception e) {
				System.out.println("PostEditTopicCommand.makeObjectSet.error="
						 + e.toString());
			}
		}
		return result;
	}


}

