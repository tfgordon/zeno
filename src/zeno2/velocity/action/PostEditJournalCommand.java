package zeno2.velocity.action;

/*
 *
 *
 *
 *
 */

import java.io.File;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Hashtable;
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
import zeno2.kernel.NameInUseException;
import zeno2.kernel.OutlineNode;
import zeno2.kernel.PreviewElement;
import zeno2.kernel.Principal;
import zeno2.kernel.ZenoException;
import zeno2.kernel.ZenoResource;
import zeno2.util.ZenoUtilities;
import zeno2.velocity.util.Errors;
import zeno2.velocity.util.Tools;
import zeno2.velocity.util.ZenoBundle;
import zeno2.velocity.util.ZenoEncoder;

/**
 *  Handles zeno journal views
 *
 *@author     <a href="mailto:lothar.oppor@ais.fraunhofer.de">Lothar Oppor</a>
 *@version    2.0.2,  2001-09-18
 */
public class PostEditJournalCommand extends Command {

	private Factory factory = null;
	private Errors errors = new Errors();


	/**
	 *  Constructor for the PostEditJournalCommand object
	 *
	 *@param  req   javax.servlet.http.HttpServletRequest
	 *@param  resp  javax.servlet.http.HttpServletResponse
	 *@since
	 */
	public PostEditJournalCommand(HttpServletRequest req, HttpServletResponse resp) {
		super(req, resp);
	}


	/**
	 *  Get parameters of editJournal template and handle this template
	 *
	 *@param  ctx            Velocity Context
	 *@return                the appropriate template file name
	 *@exception  Exception  The original Java Exception
	 *@since
	 */
	public String exec(Context ctx) throws Exception {

		String id = request.getParameter("id");
		int idNr = (new Tools()).toInt(id);
//System.out.println("PostEditJournalCommand.exe.idNr=" + Integer.toString(idNr));
		forumServlet = (String) ctx.get("servl");
		try {
			// get parameters
			String mode = request.getParameter("mode");
			String modeTemplate = EditJournalCommand.templates.getProperty(mode);
			String title = request.getParameter("title");
			String rank = request.getParameter("rank");
			int rankNr = (new Tools()).toInt(rank);
			if ((rankNr < 0) && (!mode.equals("trash")) && (!mode.equals("bag"))
					 && (!mode.equals("exec"))) {
				errors.addError("error.invalid_rank");
				//rankNr = 0;
			}
			int revisionPeriod = (new Tools()).toInt(request.getParameter("revisionperiod"));
			String note = request.getParameter("note");
			ZenoBundle msg = (ZenoBundle) ctx.get(Constants.RESOURCE_KEY);
			if (note != null) {
				note = note.trim();
			}
			String styleSheet = request.getParameter("stylesheet");
			String ssList = getStyleSheetNames((String) ctx.get("csspath"));
			Iterator ssOptions = ZenoUtilities.getOptions(
					ssList, styleSheet, "style.", msg);
			String articleLabels = request.getParameter("articlelabels");
			String qualifiers = request.getParameter("qualifiers");
			String linkLabels = request.getParameter("linklabels");
//System.out.println("PostEditJournalCommand.exe.linkLabels=" + linkLabels);
			String mailer = (String) ctx.get("mailer");
//System.out.println("PostEditJournalCommand.exe.mailer=" + mailer);
			String mailAlias = request.getParameter("mailalias");
//System.out.println("PostEditJournalCommand.exe.mailAlias0=" + mailAlias + "|");
			if (mailAlias == null) {
				mailAlias = "-";
			}
			else {
				mailAlias = mailAlias.trim();
				ctx.put("mailalias", mailAlias);
			}
//System.out.println("PostEditJournalCommand.exe.mailAlias1=" + mailAlias + "|");
			String zenoExtensions = ZenoUtilities.stringArrayToString(
					request.getParameterValues("zext"));
			factory = (Factory) ctx.get(Constants.FACTORY_KEY);
			List zenoPluginIdList = factory.getPluginIds();
			if ((zenoPluginIdList != null) && zenoPluginIdList.isEmpty()) {
				String zenoPluginIds = ZenoUtilities.listToString(zenoPluginIdList);
				Iterator zenoExtOptions = ZenoUtilities.getOptions(
						zenoPluginIds, zenoExtensions);
				ctx.put("zenoextoptions", zenoExtOptions);
			}

			Journal me = (Journal) factory.loadResource(idNr);
//System.out.println("PostEditJournalCommand.exe.mailAlias2=" + mailAlias + "|");
			//if ((request.getParameter("ok") == null) && mailAlias.equals("-")) {
			//	mailAlias = me.getMailAlias();
			//	ctx.put("mailalias", mailAlias);
			//}
//System.out.println("PostEditJournalCommand.exe.mailAlias3=" + mailAlias + "|");
			if (styleSheet != null) {
				ctx.put("zcss", "/zeno/css/" + styleSheet + ".css");
			}
			else {
				String zCss = me.getStyleSheetUrl();
				if ((zCss == null) || !zCss.startsWith("ss")) {
					zCss = "ss1";
				}
				ctx.put("zcss", "/zeno/css/" + zCss + ".css");
			}
			if (title == null) {
				title = me.getTitle();
			}
//System.out.println("PostEditJournalCommand.exe.me="+me+", mode="+mode
//+", ok="+request.getParameter("ok")+", title="+title+", rank="+rank);

			ctx.put("id", id);
			ctx.put(Constants.JOURNAL_KEY, me);
			ctx.put("emailparams", makeEmailParams(ctx, me));
// ********************* OK button pressed *********************************
			if (request.getParameter("ok") != null) {
				if ((containsDoubleQuote(title)) || (title.equals(""))) {
					if (title.equals("")) {
						errors.addError("error.title_required");
					}
					else {
						title = (new Tools()).filterQuote(title);
						errors.addError("error.no_double_quote");
					}
					if (!zenoExtensions.equals("")) {
						ctx.put("zenoextensions", zenoExtensions);
					}
				}
				// **************** bag *************************
				if (mode.equals("bag")) {
					moveOrCopy(ctx, me);
					putOutlinesCtx(ctx, me);
					modeTemplate = EditJournalCommand.templates.getProperty("struct");
				}
				// **************** edit ************************
				else if (mode.equals("edit")) {
					try {
						if (!mailer.equals("")){
							if (mailAlias.equals("")){
								me.setMailAlias(id);
								me.addPrincipalToRole(mailer, "editor");
							}
							else if (mailAlias.equals("-")){
								me.setMailAlias("");
								me.deletePrincipalFromRole(mailer, "editor");
							}
							else {
								me.setMailAlias(mailAlias);
								me.addPrincipalToRole(mailer, "editor");
							}
							putOutlinesCtx(ctx, me);
							modeTemplate = EditJournalCommand.templates.getProperty("struct");
						}
					}
					catch (NameInUseException e) {
						System.out.println("PostEditJournalCommand.exec.error="
								 + e + ": " + mailAlias);
						errors.addError("error." + e.getMessage());
					}
					me.setRank(rankNr);
					me.setNote(note);
					me.setTitle(title);
					me.setRevisionPeriod(revisionPeriod);
					me.setStyleSheetUrl(styleSheet);
					me.setArticleLabels(articleLabels);
					me.setQualifiers(qualifiers);
					me.setLinkLabels(linkLabels);
					markForDeletion(ctx, request.getParameterValues("isdeleted"), me);
					setIsTopics(ctx, request.getParameterValues("istopic"), me);
					markForBag(ctx, request.getParameterValues("marked"), me);
					//setIsPublished(ctx, request.getParameterValues("ispublished"), me);
					setIsUnpublished(ctx, request.getParameterValues("isunpublished"), me);
					setIsClosed(ctx, request.getParameterValues("isclosed"), me);
					if ((zenoExtensions != null) && !zenoExtensions.equals("")) {
						me.setProperty("zenoExtensions", zenoExtensions);
					}
				}
				// **************** new *************************
				else if (mode.equals("new") && errors.isEmpty()) {
					Journal journal = (Journal) factory.createJournal(me);
					journal.replaceRoleDefinition(me, null, false);
					journal.setRank(rankNr);
					journal.setNote(request.getParameter("note"));
					journal.setTitle(title);
					journal.setRevisionPeriod(revisionPeriod);
					journal.setStyleSheetUrl(styleSheet);
					journal.setArticleLabels(request.getParameter("articlelabels"));
					journal.setQualifiers(request.getParameter("qualifiers"));
					journal.setLinkLabels(request.getParameter("linklabels"));
					journal.save();
					id = Integer.toString(journal.getId());
					me = journal;
					ctx.put("id", id);
					ctx.put(Constants.JOURNAL_KEY, me);
					if ((zenoExtensions != null) && !zenoExtensions.equals("")) {
						journal.setProperty("zenoExtensions", zenoExtensions);
					}
					try {
						if (!mailer.equals("")) {
							if (mailAlias.equals("")){
								journal.setMailAlias(id);
								journal.addPrincipalToRole(mailer, "editor");
								putOutlinesCtx(ctx, me);
								modeTemplate = EditJournalCommand.templates.getProperty("struct");
							}
							else if (!mailAlias.equals("-")){
								journal.setMailAlias(mailAlias);
								journal.addPrincipalToRole(mailer, "editor");
								putOutlinesCtx(ctx, me);
								modeTemplate = EditJournalCommand.templates.getProperty("struct");
							}
						}
					}
					catch (NameInUseException e) {
						System.out.println("PostEditJournalCommand.exec.error="
								 + e + ": " + mailAlias);
						errors.addError("error." + e.getMessage());
					}
				}
				// **************** trash ***********************
				else if (mode.equals("trash")) {
					String[] remove = request.getParameterValues("remove");
					String[] untrash = request.getParameterValues("untrash");
					String[] untrprop = request.getParameterValues("untrprop");
					List preview = new ArrayList();
					genPreview(untrash, "undelete", preview);
					genPreview(untrprop, "xundelete", preview);
					genPreview(remove, "remove", preview);

					if (!preview.isEmpty()) {
//System.out.println("PostEditJournalCommand.exec.preview=" + preview);

						ctx.put("preview", preview);
						ctx.put("action", "postEditJournal");
						ctx.put("mode", "exec");
						modeTemplate = EditJournalCommand.templates.getProperty("confirm");
						return modeTemplate;
					}
					else {
//System.out.println("PostEditJournalCommand.exec.preview(empty)=" + preview);
						putOutlinesCtx(ctx, me);
						modeTemplate = EditJournalCommand.templates.getProperty("struct");
					}
				}
				// **************** exec ***********************
				else if (mode.equals("exec")) {
//System.out.println("PostEditJournalCommand.exec.mode="
//+mode);
					String[] idsToUndelete = request.getParameterValues("undelete");
					String[] idsToXUndelete = request.getParameterValues("xundelete");
					String[] idsToRemove = request.getParameterValues("remove");
//System.out.println("PostEditJournalCommand.exec.idsToUndelete="
//+idsToUndelete);
//System.out.println("PostEditJournalCommand.exec.idsToXUndelete="
//+idsToXUndelete);
//System.out.println("PostEditJournalCommand.exec.idsToRemove="
//+idsToRemove);

					unmarkForDeletion(idsToUndelete, false);
					unmarkForDeletion(idsToXUndelete, true);
					remove(idsToRemove);
					putOutlinesCtx(ctx, me);
					modeTemplate = EditJournalCommand.templates.getProperty("struct");
					title = me.getTitle();
					note = me.getNote();
					rank = Integer.toString(me.getRank());
					articleLabels = me.getArticleLabels();
					qualifiers = me.getQualifiers();
					linkLabels = me.getLinkLabels();
				}
//System.out.println("PostEditJournalCommand.exe.me="+me);
				//me.save();
				if (errors.isNotEmpty()) {
					ctx.put("journal", me);
					ctx.put("id", id);
					ctx.put("title", title);
					ctx.put("mode", mode);
					if (!mode.equals("new")) {
						ctx.put("navmode", mode);
					}
					ctx.put("note", note);
					ctx.put("rank", rank);
					ctx.put("stylesheet", ssOptions);
					ctx.put("articlelabels", articleLabels);
					ctx.put("qualifiers", qualifiers);
					ctx.put("linklabels", linkLabels);
					ctx.put("mailalias", mailAlias);
					ctx.put("title", title);
					ctx.put("boundArticles", me.getArticlesByTopic().get(0));
					ctx.put("freeArticles", me.getArticlesByTopic().get(1));
					ctx.put(Constants.ERRORS, errors.getErrors());
				}
				else {
					me.save();
					putOutlinesCtx(ctx, me);
					modeTemplate = EditJournalCommand.templates.getProperty("struct");
				}
			}
			// ******************** search *******************
			else if ((request.getParameter("search") != null)
					 || mode.equals("search")) {
				String orderBy = request.getParameter("orderby");
//System.out.println("PostEditJournalCommand.exec.mode="+mode+", orderBy="+orderBy);
				if (orderBy == null) {
					orderBy = "modification_date";
				}
				String pattern = request.getParameter("pattern");
				ctx.put("pattern", pattern);
				ctx.put("orderoptions",
						ZenoUtilities.getOptions(EditJournalCommand.orderOptions, orderBy,
						"article.", msg));
				ctx.put("order", "rank,title");
System.out.println("PostEditJournalCommand.exec.order="+orderBy);
				ctx.put("mode", mode);
				ctx.put("navmode", mode);
//System.out.println("PostEditJournalCommand.exe.mailAlias5=" + mailAlias + "|");
				ctx.put("mailalias", mailAlias);
//System.out.println("PostEditJournalCommand.exe.template="+modeTemplate);
				if (orderBy.endsWith("date")) {
					orderBy += " desc";
				}
				if (orderBy.endsWith("topic")) {
					List searchByTopicResult = me.searchByTopic(null, null, null, null, null, null,
							"%" + pattern + "%");
					List topics = (List)searchByTopicResult.get(0);
					List freeArticles = (List)searchByTopicResult.get(1);

					ctx.put("topics",topics);
					ctx.put("freeArticles",freeArticles);
					modeTemplate = EditJournalCommand.templates.getProperty("searchtopic");
				}
				else if (orderBy.endsWith("attachment")) {
					Iterator searchResult =
						me.search(null, null, null, null, null, null,
							"%" + pattern + "%", "title");
					ctx.put("articles", filterAttachments(searchResult));
					modeTemplate = EditJournalCommand.templates.getProperty("searchattachment");
				}
				else {
					ctx.put("articles",
						me.search(null, null, null, null, null, null,
							"%" + pattern + "%", orderBy));
				}
				//return modeTemplate;
			}
			// ******************** cancel *******************
			else if ((request.getParameter("cancel") != null)) {
				putOutlinesCtx(ctx, me);
				modeTemplate = EditJournalCommand.templates.getProperty("struct");
			}
			// ******************** impossible *******************
			else {
				System.out.println("PostEditJournalCommand.exec.mode="
						 + mode + "!!!!!!!!! should be impossible!!!!!!!!!!!!!!");
			}
			return modeTemplate;
		}
		catch (Exception e) {
		//	throw e;
			return showMessage(ctx,"error.no_such_journal","error.just_deleted");
		}

	}


	/**
	 *  Sets the Topics attribute of the PostEditJournalCommand object
	 *
	 *@param  ctx  Velocity Context
	 *@param  ids  String[]
	 *@param  me   Journal
	 *@since
	 */
	private void setIsTopics(Context ctx, String[] ids, Journal me) {
		// get all topics of this journal
		ArrayList idList = makeArrayList(ids);
		//System.out.println("PostEditJournalCommand.setIsTopic.idList=" + idList);
		try {
			List topics = me.getTopics();
			for (int i = 0; i < topics.size(); i++) {
				Article art = (Article) topics.get(i);
				String artId = Integer.toString(art.getId());
				//System.out.println("PostEditJournalCommand.setIsTopic.artId=" + artId);
				if (!idList.contains(artId)) {
					art.setIsTopic(false);
					art.save();
				}
				else {
					idList.remove(artId);//to avoid database access
					//System.out.println("PostEditJournalCommand.setIsTopic.removed=" + artId);
				}
			}
		}
		catch (Exception e) {
			System.out.println("PostEditJournalCommand.setIsTopic.error1="
					 + e.toString());
		}
		//System.out.println("PostEditJournalCommand.setIsTopic.idList1=" + idList);
		for (int i = 0; i < idList.size(); i++) {
			try {
				String artId = (String) idList.get(i);
				int artIdNr = Integer.parseInt(artId);
//System.out.println("PostEditJournalCommand.setIsTopic.artId=" + artId
//		 + ", artIdNr=" + Integer.toString(artIdNr));
				Article art = (Article) factory.loadResource(artIdNr);
				//System.out.println("PostEditJournalCommand.setIsTopic.art=" + art);
				art.setIsTopic(true);
				art.save();
			}
			catch (Exception e) {
				System.out.println("PostEditJournalCommand.setIsTopic.error2=" + e.toString());
				continue;
			}
		}
	}


	/**
	 *  Sets the Published attribute of the PostEditJournalCommand object
	 *
	 *@param  ctx  Velocity Context
	 *@param  ids  String[]
	 *@param  me   Journal
	 *@since
	 */
	private void setIsPublished(Context ctx, String[] ids, Journal me) {
		ArrayList idList = makeArrayList(ids);
//System.out.println("PostEditJournalCommand.setIsPublished.idList=" + idList);
		try {
			// get all articles of this journal
			List artByTopic = me.getArticlesByTopic();
			List articles = (List) artByTopic.get(0);
			articles.addAll((List) artByTopic.get(1));
//System.out.println("PostEditJournalCommand.setIsPublished.articles=" + articles);

			for (int i = 0; i < articles.size(); i++) {
				Article art = (Article) articles.get(i);
				String artId = Integer.toString(art.getId());
//System.out.println("PostEditJournalCommand.setIsPublished.artId=" + artId);
//				if (idList.contains(artId)) {
//System.out.println("PostEditJournalCommand.setIsPublished: contained");
//					art.setPublished(true);
//				}
//				else {
//					art.setPublished(false);
//				}
				art.setPublished(idList.contains(artId));
//System.out.println("PostEditJournalCommand.setIsPublished: going to save");
				art.save();
//System.out.println("PostEditJournalCommand.setIsPublished: all saved");
			}
		}
		catch (Exception e) {
			System.out.println("PostEditJournalCommand.setIsPublished.error="
					 + e.toString());
		}
	}


	/**
	 *  Sets the Published attribute of the PostEditJournalCommand object to the
	 *  value specified by the editJournal form.
	 *@param  ctx  Velocity Context
	 *@param  ids  String[]
	 *@param  me   Journal
	 *@since
	 */
	private void setIsUnpublished(Context ctx, String[] ids, Journal me) {
		ArrayList idList = makeArrayList(ids);
//System.out.println("PostEditJournalCommand.setIsPublished.idList=" + idList);
		try {
			// get all articles of this journal
			List artByTopic = me.getArticlesByTopic();
			List articles = (List) artByTopic.get(0);
			articles.addAll((List) artByTopic.get(1));
//System.out.println("PostEditJournalCommand.setIsPublished.articles=" + articles);

			for (int i = 0; i < articles.size(); i++) {
				Article art = (Article) articles.get(i);
				String artId = Integer.toString(art.getId());
//System.out.println("PostEditJournalCommand.setIsPublished.artId=" + artId);
//				if (idList.contains(artId)) {
//System.out.println("PostEditJournalCommand.setIsPublished: contained");
//					art.setPublished(true);
//				}
//				else {
//					art.setPublished(false);
//				}
				art.setPublished(!idList.contains(artId));
//System.out.println("PostEditJournalCommand.setIsPublished: going to save");
				art.save();
//System.out.println("PostEditJournalCommand.setIsPublished: all saved");
			}
		}
		catch (Exception e) {
			System.out.println("PostEditJournalCommand.setIsPublished.error="
					 + e.toString());
		}
	}


	/**
	 *  Sets the Closed attribute of the PostEditJournalCommand object
	 *
	 *@param  ctx  Velocity Context
	 *@param  ids  String[]
	 *@param  me   Journal
	 */
	private void setIsClosed(Context ctx, String[] ids, Journal me) {
		ArrayList idList = makeArrayList(ids);
//System.out.println("PostEditJournalCommand.setIsClosed.idList=" + idList);
		try {
			// get all articles of this journal
			List artByTopic = me.getArticlesByTopic();
			List articles = (List) artByTopic.get(0);
			articles.addAll((List) artByTopic.get(1));

			for (int i = 0; i < articles.size(); i++) {
				Article art = (Article) articles.get(i);
				String artId = Integer.toString(art.getId());
//System.out.println("PostEditJournalCommand.setIsClosed.artId=" + artId);
				if (idList.contains(artId)) {
					art.close();
				}
				else {
					art.open();
				}
				art.save();
			}
		}
		catch (Exception e) {
			System.out.println("PostEditJournalCommand.setIsClosed.error="
					 + e.toString());
		}
	}



	/**
	 *  Looks up the names of all zeno stylesheets contained in the directory
	 *  http:/zeno/cssand puts them into a comma separated list.
	 *
	 *@param  cssPath  Description of Parameter
	 *@return          String containing a comma separated list of stylesheet names
	 */
	private String getStyleSheetNames(String cssPath) {
		StringBuffer result = new StringBuffer();
		File cssDir = new File(cssPath);
		String[] cssFiles = null;
		try {
			cssFiles = cssDir.list();
		}
		catch (Exception e) {
			System.out.println("editArticleCommand.getStyleSheetNames.error=" + e);
			return "";
		}
		for (int i = 0; i < cssFiles.length; i++) {
			String fName = cssFiles[i];
			if (fName.startsWith("ss")) {
				result.append(",");
				result.append(fName.substring(0, fName.indexOf(".")));
			}
		}
		return result.toString().substring(1);
	}


	/**
	 *  marks the checked journals and/or articles for deletion
	 *
	 *@param  ids  String[]
	 *@param  ctx  Velocity Context
	 */
	private void markForDeletion(Context ctx, String[] ids, Journal me) {
		ArrayList idList = makeArrayList(ids);
		try {
			// get all articles of this journal
			List artByTopic = me.getArticlesByTopic();
			List articles = (List) artByTopic.get(0);
			articles.addAll((List) artByTopic.get(1));

			for (int i = 0; i < articles.size(); i++) {
				Article art = (Article) articles.get(i);
				String artId = Integer.toString(art.getId());
//System.out.println("PostEditJournalCommand.setIsClosed.artId=" + artId);
				if (idList.contains(artId)) {
					art.markForDeletion();
				}
				else {
					art.unmarkForDeletion();
				}
			}
		}
		catch (Exception e) {
			System.out.println("PostEditJournalCommand.setIsClosed.error="
					 + e.toString());
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
	 *  unmarks the checked journals and subjournals recursively
	 *
	 *@param  ids  String[]
	 *@param  ctx  Velocity Context
	 */
	private void propagateUnmarkForDeletion(Context ctx, String[] ids) {
		//System.out.println("PostEditJournalCommand.propagateUnmarkForDeletion.ids=" + ids);
		if (ids == null) {
			return;
		}
		for (int i = 0; i < ids.length; i++) {
			//System.out.println("PostEditJournalCommand.propagateUnmarkForDeletion.ids[i]=" + ids[i]);
			int idNo = 0;
			try {
				idNo = Integer.parseInt(ids[i]);
				//System.out.println("PostEditJournalCommand.propagateUnmarkForDeletion.idNo="
				//		 + Integer.toString(idNo));
				Journal jrnl = (Journal) factory.loadResource(idNo);
				//System.out.println("PostEditJournalCommand.propagateUnmarkForDeletion.jrnl="+jrnl);
				jrnl.unmarkForDeletion(true);
				jrnl.save();
			}
			catch (Exception e) {
				// do nothing
			}
		}
	}



	/**
	 *  Description of the Method
	 *
	 *@param  ctx  Velocity Context
	 *@param  ids  String[]
	 *@param  me   Journal
	 *@since
	 */
	private void markForBag(Context ctx, String[] ids, Journal me) {
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
	 *  Description of the Method
	 *
	 *@param  ctx  Description of Parameter
	 *@param  me   Description of Parameter
	 *@since
	 */
	private void moveOrCopy(Context ctx, Journal me) {
		HttpSession sess = request.getSession();
		ArrayList bag = (ArrayList) sess.getAttribute(Constants.BAG_KEY);
		//System.out.println("PostEditJournalCommand.moveOrCopy.bag=" + bag);
		if (bag == null) {
			bag = new ArrayList();
		}
		try {
			String[] moveCandidates = filter(request.getParameterValues("move"), bag);
			String[] copyCandidates = filter(request.getParameterValues("copy"), bag);
			ArrayList mResIds = makeArrayList(moveCandidates);
			ArrayList cResIds = makeArrayList(copyCandidates, mResIds);
			Set mObj = makeObjectSet(ctx, mResIds);
			//System.out.println("PostEditJournalCommand.moveOrCopy.mObj=" + mObj);
			Set cObj = makeObjectSet(ctx, cResIds);
			//System.out.println("PostEditJournalCommand.moveOrCopy.cObj=" + cObj);
			if (!mObj.isEmpty()) {
				//me.move(mObj, me);
				me.moveHere(mObj);
			}
			if (!cObj.isEmpty()) {
				//me.copy(cObj, me, true);// copy with links
				me.copyHere(cObj, true);// copy with links
			}
			deleteFromBag(bag, mResIds);
			deleteFromBag(bag, cResIds);
		}
		catch (Exception e) {
			System.out.println("PostEditJournalCommand.moveOrCopy.error="
					 + e.toString());
		}
		sess.setAttribute(Constants.BAG_KEY, bag);
		//System.out.println("PostEditJournalCommand.markForBag.bag+=" + bag);
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
				System.out.println("PostEditJournalCommand.makeObjectSet.error="
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
	 *  Separtates journals and articles in the bag
	 *
	 *@param  bag    input itIterator
	 *@param  ctx    Velocity Context
	 *@param  art    output List for bag articles
	 *@param  journ  output List for bag journals
	 */
	private void separateBagArticlesAndBagJournals(
			Context ctx, Iterator bag, List art, List journ) {

		//System.out.println("EditJournalCommand.separateBagArticlesAndBagJournals.bag="
		//		 + bag);

		while (bag.hasNext()) {
			int idNr = (new Tools()).toInt((String) bag.next());
			ZenoResource resource = null;
			try {
				resource = (ZenoResource) factory.loadResource(idNr);
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


	/**
	 *  Filters html form checklist values<br>
	 *  Only those, which are in the bag are passed.
	 *
	 *@param  in   String[] of results from a check list of a HTML form
	 *@param  bag  ArrayList
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
	 *  Filters artile listLets pass only articles with attachments.
	 *
	 *@param  in   String[] of results from a check list of a HTML form
	 *@param  bag  ArrayList
	 *@return      String[], members of in, which are also containe in bag
	 */
	private Iterator filterAttachments(Iterator in) {
		if ((in == null) || !in.hasNext()) {
			return null;
		}
		List result = new ArrayList();
		while (in.hasNext()) {
			Article art = (Article) in.next();
			try {
				if (!(art.getAttachments().isEmpty())) {
					result.add(art);
				}
			}
			catch (Exception e) {}
		}
		return result.iterator();
	}


	/**
	 *  generates a previewElement for each journal from ids; if it is null
	 *  i.eno
	 *  need to get confirmation the operation called for is immediately executed;
	 *  all other elements are added to preview
	 *
	 *@param  ids      Description of Parameter
	 *@param  mode     Description of Parameter
	 *@param  preview  Description of Parameter
	 */

	private void genPreview(String[] ids,
			String mode,
			List preview) {

		ZenoResource resource;
		PreviewElement element;
		int size = ids != null ? ids.length : 0;

		for (int i = 0; i < size; i++) {
			int id = Integer.parseInt(ids[i]);
			try {

				resource = (ZenoResource) factory.loadResource(id);
				if (mode.equals("remove") && (resource instanceof Journal)) {
					System.out.println("PostEditJournalCommand.genPreview(removePreview)resource="
							 + resource);
					element = ((Journal) resource).genRemovePreview();
					preview.add(element);
				}
				else if (mode.equals("undelete")) {
//System.out.println("PostEditJournalCommand.genPreview(undeletePreview)resource="
//+ resource);
					if (resource instanceof Journal) {
						element = ((Journal) resource).genUndeletePreview("undelete");
						if (element != null) {
							preview.add(element);
						}
						else {
//System.out.println("PostEditJournalCommand.genPreview(undelete immediately)resource="
//+ resource);
							((Journal) resource).unmarkForDeletion(false);
						}
					}
					else {// is article
						resource.unmarkForDeletion();
					}
				}
				else if (mode.equals("xundelete") && (resource instanceof Journal)) {
//System.out.println("PostEditJournalCommand.genPreview(xundeletePreview)resource="
//+ resource);
					element = ((Journal) resource).genUndeletePreview("xundelete");
					if (element != null) {
						preview.add(element);
					}
					else {
//System.out.println("PostEditJournalCommand.genPreview(xundelete immediately)resource="
//+ resource);
						((Journal) resource).unmarkForDeletion(true);
					}
				}

			}
			catch (ZenoException e) {
				System.out.println("PostEditJournal.genPreview.e=" + e);
			}
		}
	}


	/**
	 *  Description of the Method
	 *
	 *@param  ids        Description of Parameter
	 *@param  propagate  Description of Parameter
	 */
	private void unmarkForDeletion(String[] ids, boolean propagate) {
		if (ids == null) {
			return;
		}

		for (int i = 0; i < ids.length; i++) {
			int idNo = 0;
			try {
				idNo = Integer.parseInt(ids[i]);
				ZenoResource resource = (ZenoResource) factory.loadResource(idNo);
//System.out.println("PostEditJournalCommand.unmarkForDeletion.resource=" + resource);
				if (resource instanceof Journal) {
					((Journal) resource).unmarkWithAncestors();
					((Journal) resource).unmarkForDeletion(propagate);
				}
				else {
					resource.unmarkForDeletion();
				}
			}
			catch (Exception e) {
				// do nothing
			}
		}
	}


	/**
	 *  Description of the Method
	 *
	 *@param  ids  Description of Parameter
	 */
	private void remove(String[] ids) {
		if (ids == null) {
			return;
		}

		for (int i = 0; i < ids.length; i++) {
			int idNo = 0;
			try {
				idNo = Integer.parseInt(ids[i]);
				Journal jn = (Journal) factory.loadResource(idNo);
//System.out.println("PostEditJournalCommand.remove.jn=" + jn);
				jn.remove();
			}
			catch (Exception e) {
				// do nothing
			}
		}
	}
	
	/**
		 *  Puts the outline of the journal in the context for
		 *  for the structure view 
		 *
		 *@param  
		 */
		
		private void putOutlinesCtx(Context ctx, Journal me) {
			try {
				List nodeblocks = me.getFullOutline();
				List topicblocks = (List)nodeblocks.get(0);
				ctx.put("topicblocks", topicblocks);
				List freearticles = (List)nodeblocks.get(1);
				ctx.put("freearticles", freearticles);
				
				HttpSession sess = request.getSession();
				Hashtable expandLists = (Hashtable) sess.getAttribute("expandLists");
				if (expandLists == null) {
					expandLists = new Hashtable();
				}
				List expandList = (ArrayList)expandLists.get(new Integer(me.getId()));
				if (expandList == null ) {
					expandList = new ArrayList();
				}
				sess.setAttribute("expandLists", expandLists);
				ctx.put("expandTopicsIds", expandList);
				ctx.put("expandLists", expandLists);
				
				return;
			}
			catch (Exception e){

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
*/

}

