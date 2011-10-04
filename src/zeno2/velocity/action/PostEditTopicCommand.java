package zeno2.velocity.action;

/*
 *
 *
 *
 *
 */

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.velocity.context.Context;
//import org.apache.velocity.app.tools.*;
import org.apache.velocity.servlet.VelocityServlet;

import zeno2.kernel.Article;
import zeno2.kernel.Constants;
import zeno2.kernel.Factory;
import zeno2.kernel.Journal;
import zeno2.kernel.PreviewElement;
import zeno2.kernel.Principal;
import zeno2.kernel.Topic;
import zeno2.kernel.ZenoException;
import zeno2.kernel.ZenoResource;
import zeno2.util.ZenoUtilities;
import zeno2.velocity.util.Errors;
import zeno2.velocity.util.Tools;
import zeno2.velocity.util.ZenoBundle;
import zeno2.velocity.util.ZenoEncoder;

/**
 *  Handles zeno topic form results
 *
 *@author     <a href="mailto:lothar.oppor@ais.fraunhofer.de">Lothar Oppor</a>
 *@version    2.0.2, 2001-09-18
 */
public class PostEditTopicCommand extends Command {

	private Errors errors = new Errors();
	private Factory factory = null;
	private Topic me = null;


	/**
	 *  Constructor for the PostEditTopicCommand object
	 *
	 *@param  req   javax.servlet.http.HttpServletRequest
	 *@param  resp  javax.servlet.http.HttpServletResponse
	 *@since
	 */
	public PostEditTopicCommand(HttpServletRequest req, HttpServletResponse resp) {
		super(req, resp);
	}


	/**
	 *  Get parameters of editTopic template and handle this template
	 *
	 *@param  ctx            Velocity Context
	 *@return                the appropriate template file name
	 *@exception  Exception  The original Java Exception
	 *@since
	 */
	public String exec(Context ctx) throws Exception {

		ZenoBundle msg = (ZenoBundle) ctx.get(Constants.RESOURCE_KEY);
		String id = request.getParameter("id");
//System.out.println("PostEditTopicCommand.exec.id=" + id);
		int idNr = (new Tools()).toInt(id);
//System.out.println("PostEditTopicCommand.exec.idNr=" + Integer.toString(idNr));
		String view = "";
		try {
			factory = (Factory) ctx.get(Constants.FACTORY_KEY);
			// get parameters
			String mode = request.getParameter("mode");
			view = mode;
			String modeTemplate = EditTopicCommand.templates.getProperty(mode);
			String title = request.getParameter("title");
			String rank = request.getParameter("rank");
			int rankNr = (new Tools()).toInt(rank);
			String expires = request.getParameter("expires");
			String beginDateParam = request.getParameter("begindate");
			String endDateParam = request.getParameter("enddate");
//System.out.println("PostEditTopicCommand.exec.title=" + title + ", factory=" + factory);
			me = (Topic) factory.loadResource(idNr);
//System.out.println("PostEditTopicCommand.exec.me=" + me);
			if (title == null) {
				title = me.getTitle();
			}
//System.out.println("PostEditTopicCommand.exec.me=" + me + ", mode=" + mode
//+ ", ok=" + request.getParameter("ok") + ", title=" + title);
			ctx.put("title", title);

			Journal parent = (Journal) me.getParent();
			String styleSheet = parent.getStyleSheetUrl();
			if ((styleSheet == null) || !styleSheet.startsWith("ss")) {
				styleSheet = "ss1";
			}
			ctx.put("zcss", "/zeno/css/" + styleSheet + ".css");
			ctx.put("id", id);
			ctx.put(Constants.TOPIC_KEY, me);
			ctx.put("emailparams", makeEmailParams(ctx, me));

			// ************************ OK pushed ******************
			if (request.getParameter("ok") != null) {
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
					ctx.put(Constants.TOPIC_KEY, me);
					ctx.put("parentid", Integer.toString(parent.getId()));
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
				// **************** bag mode **************************
				if (mode.equals("bag")) {
					moveOrCopy(ctx, me);
					view = "print";
				}
				// **************** edit mode *************************
				else if (mode.equals("edit")) {
					me.setKeywords(request.getParameter("keywords"));
					me.setLabel(request.getParameter("label"));
					String qualifier = request.getParameter("qualifier");
System.out.println("PostEditArticleCommand.exec.qualifier="+qualifier+"|");
					if (qualifier != null && !qualifier.equals("")) {
						me.setQualifier(qualifier);
					}
					me.setNote(request.getParameter("note"));
					me.setTitle(title);
					me.setRank(rankNr);
					me.setBeginDate(beginDate);
					me.setEndDate(endDate);
//System.out.println("PostEditArticleCommand.exec.(set)beginDate="+beginDate
//+", endDate="+endDate);
					me.setExpirationDate(expirationDate);
//System.out.println("PostEditArticleCommand.exec: all set");
					String notifyCreatorParam = request.getParameter("notifycreator");
//System.out.println("PostEditArticleCommand.exec.notifyCreatorParam="
//+notifyCreatorParam);
					if (notifyCreatorParam != null) {
						//art.setNotifyCreator(true);
						me.setProperty("notifyCreator", "true");
					}
					else {
						me.setProperty("notifyCreator", "false");
					}
					markForDeletion(ctx, request.getParameterValues("delete"));
					markForBag(ctx, request.getParameterValues("marked"), me);
					view = "print";

				}
				// ******************** trash *******************
				else if (mode.equals("trash")) {
					if (request.getParameter("remove") != null) {
						remove(ctx, request.getParameterValues("remove"));
					}

					if (request.getParameter("untrash") != null) {
						unmarkForDeletion(ctx, request.getParameterValues("untrash"));
					}
					view = "print";
				}
				// ******************** respond *******************
				else if (mode.equals("respond")) {
					// not used! Is handeled by  PostEditArticleCommand
				}
			}
			// ******************** search *******************
			else if ((request.getParameter("search") != null)
					 && mode.equals("search")) {
				String orderBy = request.getParameter("orderby");
//System.out.println("PostEditTopicCommand.exec.mode=" + mode + ", orderBy=" + orderBy);
				if (orderBy == null) {
					orderBy = "rank,title";
				}
				String pattern = request.getParameter("pattern");
//System.out.println("PostEditTopicCommand.exec.pattern=" + pattern);
				ctx.put("pattern", pattern);
				ctx.put("orderoptions",
						ZenoUtilities.getOptions(EditJournalCommand.orderOptions, orderBy,
						"article.", msg));
				ctx.put("order", "rank,title");
				System.out.println("PostEditTopicCommand.exec.order=" + orderBy);
				ctx.put("articles",
						me.search(null, null, null, null, null, null, "%" + pattern + "%", orderBy));
				ctx.put("mode", mode);
//System.out.println("PostEditTopicCommand.exe.template=" + modeTemplate);
				return modeTemplate;
			}
			else {
				view = "print";
			}
//System.out.println("PostEditTopicCommand.exec.me="+me);
			me.save();
		}
		catch (Exception e) {
			System.out.println("PostEditTopicCommand.exec.error="+e);
			//e.printStackTrace();
			//	throw e;
		return showMessage(ctx,"error.no_such_topic","error.just_deleted");
		}

//System.out.println("PostEditTopicCommand.exec: going to struct");
		return EditTopicCommand.templates.getProperty(view);
	}



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
	 *  Creates a parameter String for zeno notification
	 *
	 *@param  ctx  Veleocity context
	 *@param  me   Article
	 *@return      String
	 */
/*
	private String makeEmailParams(Context ctx,	Article me) {
		ZenoEncoder enc = new ZenoEncoder();
		try {
			StringBuffer strb = new StringBuffer("mailto:?subject=Zeno Mail from ");
			Principal user = (Principal) ctx.get("user");
			strb.append(user.getName());
			strb.append("&body=");
			strb.append(me.getLabel());
			strb.append(": ");
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
			url.append("?action=editTopic&view=print&id=");
			url.append(me.getId());
			strb.append(enc.encode(url.toString()));
//System.out.println("editTopicCommand.makeEmailParams=" + strb.toString());
			return strb.toString();
		}
		catch (Exception e) {
			System.out.println("editTopicCommand.makeEmailParams.error="
					 + e.toString());
			return "";
		}
	}
*/

	/**
	 *  Description of the Method
	 *
	 *@param  ctx  Velocity Context
	 *@param  ids  String[]
	 *@param  me   Journal
	 *@since
	 */
	private void markForBag(Context ctx, String[] ids, Topic me) {
		HttpSession sess = request.getSession();
		ArrayList bag = (ArrayList) sess.getAttribute(Constants.BAG_KEY);
		//System.out.println("PostEditJournalCommand.markForBag.bag=" + bag);
		if (bag == null) {
			bag = new ArrayList();
		}
		ArrayList idList = makeArrayList(ids);
		//System.out.println("PostEditJournalCommand.markForBag.idList=" + idList);
		try {
			Iterator members = me.getMembers();
			while (members.hasNext()) {
				ZenoResource resource = (ZenoResource) members.next();
				String resId = Integer.toString(resource.getId());
				//System.out.println("PostEditJournalCommand.markForBag.resId=" + resId);
				if (!idList.contains(resId) && bag.contains(resId)) {
					bag.remove(resId);
					//System.out.println("PostEditJournalCommand.markForBag.removed=" + resId);
				}
				else if (idList.contains(resId) && !bag.contains(resId)) {
					bag.add(resId);//to avoid database access
					//System.out.println("PostEditJournalCommand.markForBag.added=" + resId);
				}
			}
		}
		catch (Exception e) {
			System.out.println("PostEditJournalCommand.markForBag.error="
					 + e.toString());
		}
		sess.setAttribute(Constants.BAG_KEY, bag);
		//System.out.println("PostEditJournalCommand.markForBag.bag+=" + bag);
	}


	/**
	 *  marks the checked journals and/or articles for deletion
	 *
	 *@param  ids  String[]
	 *@param  ctx  Velocity Context
	 */
	private void markForDeletion(Context ctx, String[] ids) {
//System.out.println("PostEditJournalCommand.markForDeletion.ids=" + ids);
		if (ids == null) {
			return;
		}
		for (int i = 0; i < ids.length; i++) {
			//System.out.println("PostEditJournalCommand.markForDeletion.ids[i]=" + ids[i]);
			int idNo = 0;
			try {
				idNo = Integer.parseInt(ids[i]);
//System.out.println("PostEditJournalCommand.markForDeletion.idNo="
//+ Integer.toString(idNo));
				ZenoResource zr = factory.loadResource(idNo);
				//System.out.println("PostEditJournalCommand.markForDeletion.zr=" + zr);
				zr.markForDeletion();
				zr.save();
			}
			catch (Exception e) {
				// do nothing
			}
		}
	}


	/**
	 *  unmarks the checked journals and/or articles for deletion
	 *
	 *@param  ids  String[]
	 *@param  ctx  Velocity Context
	 */
	private void unmarkForDeletion(Context ctx, String[] ids) {
//System.out.println("PostEditJournalCommand.unmarkForDeletion.ids=" + ids);
		if (ids == null) {
			return;
		}
		for (int i = 0; i < ids.length; i++) {
//System.out.println("PostEditJournalCommand.unmarkForDeletion.ids[i]=" + ids[i]);
			int idNo = 0;
			try {
				idNo = Integer.parseInt(ids[i]);
				//System.out.println("PostEditJournalCommand.unmarkForDeletion.idNo="
				//		 + Integer.toString(idNo));
				ZenoResource zr = factory.loadResource(idNo);
				//System.out.println("PostEditJournalCommand.unmarkForDeletion.zr=" + zr);
				zr.unmarkForDeletion();
				zr.save();
			}
			catch (Exception e) {
				// do nothing
			}
		}
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
	 *  Irrecoverably removes the marked articles from the parent section
	 *
	 *@param  ctx  velocity context
	 *@param  ids  String array containing ids of articles to be removed
	 */
	private void remove(Context ctx, String[] ids) {
		if (ids == null) {
			return;
		}
		List artList = new ArrayList();
		for (int i = 0; i < ids.length; i++) {
			artList.add(ids[i]);
		}
		try {
			me.removeArticles(artList);// removes irrecoverably
		}
		catch (Exception e) {
		}
	}

}

