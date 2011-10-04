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
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

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
import zeno2.kernel.NoPermissionException;
import zeno2.kernel.NotFoundException;
import zeno2.kernel.Plugin;
import zeno2.kernel.Principal;
import zeno2.kernel.Topic;
import zeno2.kernel.ZenoException;
import zeno2.kernel.ZenoResource;
import zeno2.util.ZenoUtilities;
import zeno2.velocity.util.Tools;
import zeno2.velocity.util.ZenoEncoder;

/**
 *  Handles zeno journal views
 *
 *@author     <a href="mailto:lothar.oppor@ais.fraunhofer.de">Lothar Oppor</a>
 *@version    2.0.2, 2001-09-07
 */
public class EditArticleCommand extends Command {

	private ZenoResource me = null;

	private Factory factory = null;

	public static Properties templates = new Properties();

	/**
	 *  Constructor for the EditArticleCommand object
	 *
	 *@param  req   Description of Parameter
	 *@param  resp  Description of Parameter
	 *@since
	 */
	public EditArticleCommand(HttpServletRequest req, HttpServletResponse resp) {
		super(req, resp);
	}


	/**
	 *  Put id, title, note, keywords, rank expires and eventually georef into the
	 *  contextAdd view as modeto the contextIf view=new, add the journal to
	 *  the context, else add the articlefinaly return the filename of the
	 *  appropriate template.
	 *
	 *@param  ctx            Velocity Context
	 *@return                the appropriate template file name
	 *@exception  Exception  no exception is thrown
	 *@since
	 */
	public String exec(Context ctx) throws Exception {

		forumServlet = (String) ctx.get("servl");
		String id = request.getParameter("id");
		int idNr = (new Tools()).toInt(id);
		if (idNr < 0) {
			System.out.println("EditArticleCommand.exec.error(id)=" + idNr);
			return showMessage(ctx, "error.no_such_article");
		}
		//ZenoResource me = null;
		try {
			factory = (Factory) ctx.get(Constants.FACTORY_KEY);
			me = factory.loadResource(idNr);
			boolean foo = factory.hasRole("editor", me);
			int dummy = me.getRank();// to get exception if no permission
		}
		catch (Exception e) {
			System.out.println("EditArticleCommand.exec.error(me)=" + e.toString());
			return showMessage(ctx, "error.not_allowed_print_article");
		}
		// ********** get Zeno Plugins for article menu ***********
		String zenoPlugins = me.getProperty("zenoExtensions");
//System.out.println("EditArticleCommand.exec.zenoPlugins="+zenoPlugins);
		if (zenoPlugins != null) {
			List zExt = getZenoExtensions(ctx, zenoPlugins);
			if ((zExt != null) && !zExt.isEmpty()) {
				ctx.put("zenoextensions", zExt);
//System.out.println("EditArticleCommand.exec.zExt="+zExt);
			}
		}

		ctx.put("id", id);
		String view = request.getParameter("view");

		if ((view == null) || view.equals("")) {
			view = "print";
		}
		ctx.put("mode", view);
		ctx.put("title", "");
		ctx.put("note", "");
		ctx.put("keywords", "");
		ctx.put("rank", "0");
		ctx.put("expires", "");
		ctx.put("begindate", "");
		ctx.put("enddate", "");
		if (id.equals("1")) {
			ctx.put("parentid","1");
		}
		else{
			ctx.put("parentid",Integer.toString(me.getParent().getId()));
		}

//System.out.println("EditArticleCommand.exec.view=" + view + ", id=" + id);

		// ************************** noteDown ***************************
		if (view.equals("noteDown")) {
			markMeForBag(ctx);
			view = "print";
		}
		//*************************** edit *************************
		if (view.equals("edit")) {
			setNoCache();
			try {
				ctx.put(Constants.ARTICLE_KEY, (Article) me);
				String title = ((Article) me).getTitle();
				title = (new Tools()).filterQuote(title);
				ctx.put("title", title);
				ctx.put("note", ((Article) me).getNote().trim());
				ctx.put("keywords", ((Article) me).getKeywords());
				String rank = Integer.toString(((Article) me).getRank());
//System.out.println("EditArticleCommand.exec.rank=" + rank);
				if ((rank == null) || (rank.startsWith("-"))) {
					rank = "0";
				}
				ctx.put("rank", rank);
				String expires = ZenoUtilities.getIsoString(((Article)
						me).getExpirationDate());
				ctx.put("expires", expires);
				String beginDate = ZenoUtilities.getIsoString(((Article)
						me).getBeginDate());
				ctx.put("begindate", beginDate);
				String endDate = ZenoUtilities.getIsoString(((Article)
						me).getEndDate());
				ctx.put("enddate", endDate);
				ctx.put("articlelabeloptions", ZenoUtilities.getOptions(
						((Journal) me.getParent()).getArticleLabels()
						, ((Article) me).getLabel()));
				ctx.put("qualifieroptions", ZenoUtilities.getOptions(
						((Journal) me.getParent()).getQualifiers()
						, ((Article) me).getQualifier()));
//System.out.println("EditArticleCommand.exec.qualifier="
//+ ((Article) me).getQualifier());
				String notifycreator = ((Article) me).getProperty("notifyCreator");
				if ((notifycreator != null) && notifycreator.equals("true")) {
					ctx.put("notifycreator", notifycreator);
				}
				Journal parent = (Journal) me.getParent();
				String styleSheet = parent.getStyleSheetUrl();
				if ((styleSheet == null) || !styleSheet.startsWith("ss")) {
					styleSheet = "ss1";
				}
				ctx.put("zcss", "/zeno/css/" + styleSheet + ".css");
			}
			catch (NotFoundException e) {
				System.out.println("EditArticleCommand.exec.error(edit)=" + e.toString());
				return showMessage(ctx, "error.not_found",
						"error.not_found_zeno_resource");
			}
			catch (NoPermissionException e) {
				System.out.println("EditArticleCommand.exec.error(edit)=" + e.toString());
				return showMessage(ctx, "error.no_permission",
						"error.not_allowed_edit_article");
			}
			catch (Exception e) {
				System.out.println("EditArticleCommand.exec.error(edit)=" + e.toString());
				return showMessage(ctx, "error.unable",
						"error.unable_edit_article");
			}
		}
		else if (view.equals("new")) {
			//*************************** new *************************
			setNoCache();
			try {
				ctx.put("parentid", id);
				ctx.put("id", "");
				ctx.put("expires", "");
				String now = ZenoUtilities.getIsoString(new Date());
				ctx.put("begindate", now);
				ctx.put("enddate", "");
				ctx.put(Constants.JOURNAL_KEY, (Journal) me);
				ctx.put("articlelabeloptions",
						ZenoUtilities.getOptions(((Journal) me).getArticleLabels(), ""));
				ctx.put("qualifieroptions", ZenoUtilities.getOptions(
						((Journal) me).getQualifiers(), ""));
				String styleSheet = ((Journal) me).getStyleSheetUrl();
				if ((styleSheet == null) || !styleSheet.startsWith("ss")) {
					styleSheet = "ss1";
				}
				ctx.put("zcss", "/zeno/css/" + styleSheet + ".css");
			}
			catch (NoPermissionException e) {
				System.out.println("EditArticleCommand.exec.error(new)=" + e.toString());
				return showMessage(ctx, "error.no_permission",
						"error.not_adllowed_new_article");
			}
		}
		else if (view.equals("print")
				 || view.equals("newWindow")
				 || view.equals("flash")) {
			//*************flash *** newWindow *** print *************
			try {
				ctx.put("id", id);
//System.out.println("EditArticleCommand.exec.view=" + view + ", id=" + id);
				ctx.put("expires", null);
				ctx.put("begindate", null);
				ctx.put("enddate", null);
				ctx.put("mes", request.getParameter("mes"));
				if (me instanceof Journal) {
					String url = forumServlet + "?action=editJournal&view=struct&id=" + id;
					String urlEncode = response.encodeRedirectURL(url);
					response.sendRedirect(urlEncode);
					return null;
					/*
					 * ctx.put(Constants.JOURNAL_KEY, (Journal) me);
					 * String styleSheet = ((Journal) me).getStyleSheetUrl();
					 * if ((styleSheet == null) || !styleSheet.startsWith("ss")) {
					 * styleSheet = "ss1";
					 * }
					 * ctx.put("zcss", "/zeno/css/"+styleSheet+".css");
					 * return EditJournalCommand.templates.getProperty("struct");
					 */
				}
				ctx.put(Constants.ARTICLE_KEY, (Article) me);
				// to get a NoPermissionException if not permitted
				String dummy = ((Article) me).getLabel();
				ctx.put("emailparams", makeEmailParams(ctx, me));
				Journal parent = (Journal) me.getParent();
				String styleSheet = parent.getStyleSheetUrl();
				if ((styleSheet == null) || !styleSheet.startsWith("ss")) {
					styleSheet = "ss1";
				}
				ctx.put("zcss", "/zeno/css/" + styleSheet + ".css");
			}
			catch (NoPermissionException e) {
				System.out.println("EditArticleCommand.exec.error(print)=" + e.toString());
				return showMessage(ctx, "error.no_permission",
						"error.not_allowed_print_article");
			}
			catch (Exception e) {
				System.out.println("EditArticleCommand.exec.error(print)=" + e.toString());
				return showMessage(ctx, "error.not_such_article",
						"error.not_allowed_print_article");
			}
		}
		else if (view.equals("respond")) {
			//*************************** respond *************************
			setNoCache();
			try {
				ctx.put("id", id);
				ctx.put(Constants.ARTICLE_KEY, (Article) me);
				ctx.put("parentid", Integer.toString(me.getParent().getId()));
				ctx.put("articlelabeloptions",
						ZenoUtilities.getOptions(((Journal) me.getParent()).getArticleLabels(), ""));
				ctx.put("qualifieroptions",
						ZenoUtilities.getOptions(((Journal) me.getParent()).getQualifiers(), ""));
				ctx.put("linklabeloptions",
						ZenoUtilities.getOptions(((Journal) me.getParent()).getLinkLabels(), ""));
				String dummy = ((Article) me).getLabel();// permission ?
				Journal parent = (Journal) me.getParent();
				String styleSheet = parent.getStyleSheetUrl();
				if ((styleSheet == null) || !styleSheet.startsWith("ss")) {
					styleSheet = "ss1";
				}
				ctx.put("zcss", "/zeno/css/" + styleSheet + ".css");
			}
			catch (NotFoundException e) {
				System.out.println("EditArticleCommand.exec.error(edit)=" + e.toString());
				return showMessage(ctx, "error.not_found",
						"error.not_found_zeno_resource");
			}
			catch (Exception e) {
				System.out.println("EditArticleCommand.exec.error(respond)=" + e.toString());
				return showMessage(ctx, "error.no_permission",
						"error.not_allowed_respond_article");
			}
		}
		else if (view.equals("bag")) {
			//*************************** bag *****************************
			//return showMessage(ctx, "error.under_construction");
			List bagArticles = new ArrayList();
			List bagTopics = new ArrayList();
			List bagJournals = new ArrayList();
			HttpSession sess = request.getSession();
			ArrayList bagList = (ArrayList) sess.getAttribute(Constants.BAG_KEY);
			if (bagList == null) {
				bagList = new ArrayList();
			}
			separateBagArticlesAndBagJournals(
					ctx
					, bagList.iterator()
					, bagArticles
					, bagJournals
					, bagTopics);
			ctx.put("bagarticles", bagArticles);
			ctx.put("bagtopics", bagTopics);
			ctx.put("bagjournals", bagJournals);
			ctx.put(Constants.TOPIC_KEY, (Topic) me);
			ctx.put("mode", view);
			ctx.put(Constants.ARTICLE_KEY, (Article) me);
			ctx.put("parentid", Integer.toString(me.getParent().getId()));
		}
		else {
			return showMessage(ctx, "error.no_such_view");
		}
//System.out.println("EditArticleCommand.exec.view=" + view + ", id=" + id);
		return templates.getProperty(view);
	}


	/**
	 *  Sets the NoCache attribute of the EditArticleCommand object
	 */
	private void setNoCache() {
		if (request.getProtocol().compareTo("HTTP/1.0") == 0) {
			response.setHeader("Pragma", "no-cache");
		}
		else
				if (request.getProtocol().compareTo("HTTP/1.1") == 0) {
			response.setHeader("Cache-Control", "no-cache");
		}
		response.setDateHeader("Expires", 0);
	}


	/**
	 *  Creates List of menu entries (name/href List) for zeno extensions
	 *
	 *@param  ctx           Velocity Context
	 *@param  extensionIds  Description of Parameter
	 *@return               List of Lists
	 */
	private List getZenoExtensions(Context ctx, String extensionIds) {
		List result = new ArrayList();
		StringTokenizer extIds = new StringTokenizer(extensionIds, ",");
		while (extIds.hasMoreTokens()) {
			String extId = extIds.nextToken();
			List zenoExtension = getZenoExtension(ctx, extId);
//System.out.println("editArticleCommand.getZenoExtensions.zenoExtension="+zenoExtension);
			if ((zenoExtension != null) && !zenoExtension.isEmpty()
			//*************** provisorium ***************************
					 && (!(userIsGuest(ctx) && extId.equals("wissnacht")))) {
				result.add(zenoExtension);
			}
		}
//System.out.println("editArticleCommand.getZenoExtensions.result="+result);
		if (result.isEmpty()) {
			return null;
		}
		return result;
	}


	/**
	 *  Eventually replaces a variable by the return value of the appropriate
	 *  Article method and URLencodes the right hand part of the key/value pair.
	 *
	 *@param  ctx          Velocity Context
	 *@param  extensionId  Description of Parameter
	 *@return              String key/value pair, separate by'="
	 */
	private List getZenoExtension(Context ctx, String extensionId) {
		List result = new ArrayList();
		Plugin extension = null;
		try {
			extension = factory.loadPlugin(extensionId);
		}
		catch (ZenoException e) {
			System.out.println("EditArticleCommand.getZenoExtension.error=" + e);
			return null;
		}
//System.out.println("EditArticleCommand.getZenoExtension.extension="+extension);
		if ((extension != null) && !extension.isEmpty()) {
			String menu = extension.getArticleMenu();
			if ((menu == null) || menu.trim().equals("")) {
				return null;
			}
			result.add(menu);
			result.add(extension.getArticleIcon());
			String articleURL = extension.getArticleURL();
//System.out.println("EditArticleCommand.getZenoExtension.URL="
//+extension.getArticleURL());
			String articleParams = extension.getArticleParams();
//System.out.println("EditArticleCommand.getZenoExtension.articleParams="
//+extension.getArticleParams());
			StringBuffer url = new StringBuffer(articleURL);
			if (!articleParams.equals("")) {
				url.append("?");
				url.append(evaluate(ctx, articleParams));
			}
			result.add(url.toString());
		}
//System.out.println("EditArticleCommand.getZenoExtension.result.="+result);
		return result;
	}


	/**
	 *  returns the string result of the method methName of the object o
	 *
	 *@param  o         Object
	 *@param  methName  name oft the method of o
	 *@param  what      string objectName+methodName
	 *@return           string result of the method
	 */
	private String getValue(Object o, String methName, String what) {
		Class probeClass = o.getClass();
		Method method = null;
		Object methodResult = null;
		try {
			method = probeClass.getMethod(methName, null);
			methodResult = method.invoke(o, null);
			if (methodResult instanceof Date) {
				return enc.encode(
						ZenoUtilities.getIsoString((Date) methodResult));
			}
			else {
				return enc.encode(
						methodResult.toString());
			}
//System.out.println("editArticleCommand.replaceDollarMethods.result=" + result);
		}
		catch (Exception e) {
			System.out.println("editJournalCommand.evaluate.error=" + e + ":" + what);
			return "***error***(" + what + ")";
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
			Article me) {
		try {
			StringBuffer strb = new StringBuffer("mailto:?subject=Zeno Mail from ");
			Principal user = (Principal) ctx.get("user");
			String userName = (user != null ? user.getName() : "guest";
			strb.append(userName);
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
			url.append("?action=editArticle&view=print&id=");
			url.append(me.getId());
			strb.append(enc.encode(url.toString()));
//System.out.println("editArticleCommand.makeEmailParams=" + strb.toString());
			return strb.toString();
		}
		catch (Exception e) {
			System.out.println("Command.makeEmailParams.error="
					 + e.toString());
			return "";
		}
	}
*/

	/**
	 *  Evaluates the String whatEvery $variable is replaced by the return value
	 *  of the appropriate Article methodThe result is inserted into the result
	 *  StringFinaly all parts embraced by "$(", "$)" are recursively replaced by
	 *  ZenoEncoded values.
	 *
	 *@param  ctx   Velocity Context
	 *@param  what  String
	 *@return       String
	 */
	private String evaluate(Context ctx, String what) {
//System.out.println("editArticleCommand.evaluate.what:"+what+".");
		if (what.indexOf("$") < 0) {
			return what;
		}
		String result = replaceDollarMethods(ctx, what);
		result = replaceBrackets(ctx, result);
		return result;
	}


	/**
	 *  Evaluates the String whatEvery $variable is replaced by the return value
	 *  of the appropriate Article method.
	 *
	 *@param  ctx   Velocity Context
	 *@param  what  String
	 *@return       String
	 */
	private String replaceDollarMethods(Context ctx, String what) {
		StringTokenizer dollarMethods = new StringTokenizer(what, "$");
		StringBuffer result = new StringBuffer(dollarMethods.nextToken());
		while (dollarMethods.hasMoreTokens()) {
			String dollarMethod = dollarMethods.nextToken();
//System.out.println("editArticleCommand.replaceDollarMethods.dollarMethod="
//+dollarMethod+".");
			if ((dollarMethod == null) || dollarMethod.equals("")) {
				// do nothing
			}
			else if (dollarMethod.startsWith("(") || dollarMethod.startsWith(")")) {
				result.append("$");
				result.append(dollarMethod);
			}
			else {
				String varName = dollarMethod;
				String rest = "";
				//int nameEnd = dollarMethod.indexOf("&");
				int nameEnd = dollarMethodEndPostion(varName);
				if (nameEnd > 0) {
					varName = dollarMethod.substring(0, nameEnd);
					rest = dollarMethod.substring(nameEnd);
				}
//System.out.println("editArticleCommand.replaceDollarMethods.varName="+varName
//+", rest="+rest);
				if (varName.equals("getURL")) {
					String varVal = ctx.get(Constants.SERVER_NAME) + ":"
							 + ctx.get(Constants.SERVER_PORT) + request.getRequestURI()
							 + "?action=editArticle&view=print&id=" + me.getId();
					result.append(varVal);
				}
				else {
					HttpSession sess = request.getSession();
					int p = varName.indexOf(".");
					if (p < 0) {
						Object o = sess.getAttribute(varName);
						if (o != null) {
							result.append(enc.encode(o.toString()));
						}
						else {
							result.append(getValue(me, varName, what));
						}
					}
					else {
						String objName = varName.substring(0, p);
						String methName = varName.substring(p + 1);
						Object o = sess.getAttribute(objName);
						if (o != null) {
							result.append(getValue(o, methName, what));
						}
					}
				}
				result.append(rest);
			}
		}
		return result.toString();
	}


	/**
	 *  The position of the first non-character following the dollar sign
	 *  is calculated.
	 *
	 *@param  probe  Description of Parameter
	 *@return        int, postion after the last character
	 */
	private int dollarMethodEndPostion(String probe) {
		int i = 0;
		while (i < probe.length()) {
			if (!Character.isLetter(probe.charAt(i))
					 && (probe.charAt(i) != '.')) {
				break;
			}
			i++;
		}
//System.out.println("EditArticleCommand.dollarMethodEndPostion.probe="+probe
//+", i="+i);
		return i;
	}


	/**
	 *  All parts embraced by "$(", "$)" are recursively replaced by ZenoEncoded
	 *  values.
	 *
	 *@param  ctx   Velocity Context
	 *@param  what  String
	 *@return       String
	 */
	private String replaceBrackets(Context ctx, String what) {
//System.out.println("editArticleCommand.replaceBrackets.what:"+what+".");
		if (what.indexOf("$") < 0) {
			return what;
		}
		int begin = what.indexOf("$(");
		int end = what.lastIndexOf("$)");
		if ((end < 0) && (begin < 0)) {
			return what;
		}
		if ((begin > end) || (begin < 1)) {
			return "error=***bad_brackets:" + what;
		}

		String result = what.substring(0, begin)
				 + enc.encode(replaceBrackets(ctx, what.substring(begin + 2, end)))
				 + what.substring(end + 2);
//System.out.println("editArticleCommand.replaceBrackets.result:"+result+".");
		return result;
	}


	/**
	 *  Description of the Method
	 *
	 *@param  ctx  Description of Parameter
	 *@return      Description of the Returned Value
	 */
	private boolean userIsGuest(Context ctx) {
		String userId = null;
		try {
			userId = factory.getUser().getId();
//System.out.println("EditArticleCommand.userIsGuest.userid=" + userId);
			return (userId.equals("guest"));
		}
		catch (Exception e) {
			return true;
		}

	}


	/**
	 *  Sets my id on the bag list session attribute
	 *
	 *@param  ctx  Velocity Context
	 */
	private void markMeForBag(Context ctx) {
		HttpSession sess = request.getSession();
		ArrayList bag = (ArrayList) sess.getAttribute(Constants.BAG_KEY);
		//System.out.println("PostEditJournalCommand.markMeForBag.bag=" + bag);
		if (bag == null) {
			bag = new ArrayList();
		}
		try {
			String myId = Integer.toString(me.getId());
			if (!bag.contains(myId)) {
				bag.add(myId);
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
	 *  Separtates journals and articles in the bag
	 *
	 *@param  ctx    velocity context
	 *@param  bag    Iterator
	 *@param  art    Artile List, transient parameter
	 *@param  journ  Journal List, transient parameter
	 *@param  tpc    Description of Parameter
	 *@since
	 */
	private void separateBagArticlesAndBagJournals(
			Context ctx, Iterator bag, List art, List journ, List tpc) {

		//System.out.println("EditArticleCommand.separateBagArticlesAndBagJournals.bag="
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
			if (resource instanceof Topic) {
				tpc.add(resource);
			}
			else if (resource instanceof Article) {
				art.add(resource);
			}
			else if (resource instanceof Journal) {
				journ.add(resource);
			}
		}
	}


	static {
		templates.put("bag", "forum/topic/editTopicBag.vm");
		templates.put("edit", "forum/article/editArticle.vm");
		templates.put("flash", "forum/article/editArticleFlash.vm");
		templates.put("new", "forum/article/editArticleNew.vm");
		templates.put("print", "forum/article/editArticlePrint.vm");
		templates.put("respond", "forum/article/editArticleRespond.vm");
		templates.put("newWindow", "forum/article/editArticleNewWindow.vm");
	}
}

