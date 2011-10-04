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
import zeno2.velocity.util.ZenoBundle;
import zeno2.velocity.util.ZenoEncoder;

/**
 *  Handles zeno journal views
 *
 *@author     <a href="mailto:lothar.oppor@ais.fraunhofer.de">Lothar Oppor</a>
 *@version    2.0.2, 2002-06-07
 */
public class EditTopicCommand extends Command {
	private ZenoResource me = null;
	private int myIdNr = 0;
	private Journal parent = null;

	/**
	 *  Description of the Field
	 */
	public static Properties templates = new Properties();


	/**
	 *  Constructor for the EditTopicCommand object
	 *
	 *@param  req   Description of Parameter
	 *@param  resp  Description of Parameter
	 *@since
	 */
	public EditTopicCommand(HttpServletRequest req, HttpServletResponse resp) {
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

		ZenoBundle msg = (ZenoBundle) ctx.get(Constants.RESOURCE_KEY);
		String id = request.getParameter("id");
		int idNr = (new Tools()).toInt(id);
		if (idNr < 0) {
			System.out.println("EditTopicCommand.exec.error(id)=" + idNr);
			return showMessage(ctx, "error.no_such_article");
		}
		myIdNr = idNr;
		forumServlet = (String) ctx.get("servl");
		try {
			me = ((Factory) ctx.get(Constants.FACTORY_KEY)).loadResource(idNr);
			boolean foo = ((Factory)
					ctx.get(Constants.FACTORY_KEY)).hasRole("editor", me);
			parent = (Journal) me.getParent();
			String styleSheet = parent.getStyleSheetUrl();
			if ((styleSheet == null) || !styleSheet.startsWith("ss")) {
				styleSheet = "ss1";
			}
			ctx.put("zcss", "/zeno/css/" + styleSheet + ".css");
		}
		catch (Exception e) {
			System.out.println("EditTopicCommand.exec.error(me)=" + e.toString());
			return showMessage(ctx, "error.not_allowed_print_article");
		}
		// ********** get Zeno Plugins for topic menu ***********
		String zenoPlugins = me.getProperty("zenoExtensions");
//System.out.println("EditTopicCommand.exec.zenoPlugins="+zenoPlugins);
		if (zenoPlugins != null) {
			List zExt = getZenoExtensions(ctx, zenoPlugins);
			if ((zExt != null) && !zExt.isEmpty()) {
				ctx.put("zenoextensions", zExt);
			}
//System.out.println("EditTopicCommand.exec.zExt="+zExt);
		}

		//************************ common ctx values *******************
		ctx.put("id", id);
		String view = request.getParameter("view");

		if ((view == null) || view.equals("")) {
			view = "edit";
		}
		ctx.put("navmode", "struct");
		ctx.put("mode", view);
		ctx.put("title", "");
		ctx.put("note", "");
		ctx.put("keywords", "");
		ctx.put("rank", "");
		ctx.put("expires", "");
		ctx.put("begindate", "");
		ctx.put("enddate", "");
		try {
			ctx.put(Constants.TOPIC_KEY, (Topic) me);
			ctx.put("title", ((Topic) me).getTitle());
			ctx.put("note", ((Topic) me).getNote().trim());
			ctx.put("keywords", ((Topic) me).getKeywords());
			String rank = Integer.toString(((Topic) me).getRank());
			if ((rank == null) || (rank.equals("-1"))) {
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
//System.out.println("EditTopicCommand.exec.qualifier="
//+ ((Article) me).getQualifier());
			String notifycreator = ((Article) me).getProperty("notifyCreator");
			if ((notifycreator != null) && notifycreator.equals("true")) {
				ctx.put("notifycreator", notifycreator);
			}
		}
		catch (NotFoundException e) {
			System.out.println("EditTopicCommand.exec.error=" + e.toString());
			return showMessage(ctx, "error.not_found",
					"error.not_found_zeno_resource");
		}
		catch (NoPermissionException e) {
			System.out.println("EditTopicCommand.exec.error(edit)=" + e.toString());
			return showMessage(ctx, "error.no_permission",
					"error.not_allowed_edit_article");
		}
		catch (Exception e) {
			System.out.println("EditTopicCommand.exec.error(edit)=" + e.toString());
			return showMessage(ctx, "error.unable",
					"error.unable_edit_article");
		}
		// ************************** noteDown ***************************
		if (view.equals("noteDown")) {
			markMeForBag(ctx);
			view = "print";
			ctx.put("mes", request.getParameter("mes"));
			ctx.put("emailparams", makeEmailParams(ctx, me));
		}
		// ************************** edit *******************************
		if (view.equals("edit")) {
			setNoCache();
		}
		else if (view.equals("print")) {
			//******************* print ********************************
//			try {
			ctx.put("mes", request.getParameter("mes"));
			ctx.put("emailparams", makeEmailParams(ctx, me));
		}
		// ************************** bag ********************************
		else if (view.equals("bag")) {
//System.out.println("EditTopicCommand.exec.view=" + view);
			HttpSession sess = request.getSession();
			ArrayList bagList = (ArrayList) sess.getAttribute(Constants.BAG_KEY);
			if (bagList == null) {
				bagList = new ArrayList();
			}
//System.out.println("EditTopicCommand.exec.bagList=" + bagList);
			List bagArticles = getBagArticles(ctx, bagList.iterator());
			ctx.put("bagarticles", bagArticles);
			ctx.put(Constants.TOPIC_KEY, (Topic) me);
			ctx.put("mode", view);
			ctx.put("emailparams", makeEmailParams(ctx, me));
		}
		// *********************** search ********************************
		else if (view.equals("search")) {
			ctx.put("orderoptions", ZenoUtilities.getOptions(
					EditJournalCommand.orderOptions, "", "article.", msg));
			ctx.put("pattern", "");
			ctx.put("orderby", "rank,title");
			ctx.put("articles", new ArrayList());
			//	((Topic) me).search(null, null, null, null, null, null, null, "rank,title"));
		}
		// *********************** struct ********************************
		else if (view.equals("struct")) {
			System.out.println("EditTopicCommand.exec.view=" + view);
		}

		// *********************** trash *********************************
		else if (view.equals("trash")) {
			List trashArticles = selectTrashArticles(parent.getTrash());
			ctx.put("trasharticles", trashArticles);
		}
		// *********************** respond *******************************
		else if (view.equals("respond")) {
			setNoCache();
			try {

				ctx.put("parentid", Integer.toString(parent.getId()));
				ctx.put("id", id);
				ctx.put("title", "");
				ctx.put("note", "");
				ctx.put("expires", "");
				ctx.put("begindate", "");
				ctx.put("enddate", "");
				ctx.put(Constants.JOURNAL_KEY, parent);
				ctx.put("articlelabeloptions",
						ZenoUtilities.getOptions(parent.getArticleLabels(), ""));
				ctx.put("qualifieroptions", ZenoUtilities.getOptions(
						parent.getQualifiers(), ""));
				ctx.put("linklabeloptions",
						ZenoUtilities.getOptions(parent.getLinkLabels(), ""));
				String styleSheet = parent.getStyleSheetUrl();
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
		else {
			return showMessage(ctx, "error.no_such_view");
		}
		//System.out.println("EditTopicCommand.exec.view=" + view + ", id=" + id);
		return templates.getProperty(view);
	}


	/**
	 *  Sets the NoCache attribute of the EditTopicCommand object
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
			List zenoExtension = getZenoExtension(ctx, extIds.nextToken());
			if ((zenoExtension != null) && !zenoExtension.isEmpty()) {
				result.add(zenoExtension);
			}
		}
//System.out.println("editTopicCommand.getZenoExtensions.result="+result);
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
			extension = ((Factory)
					ctx.get(Constants.FACTORY_KEY)).loadPlugin(extensionId);
		}
		catch (ZenoException e) {
			System.out.println("EditTopicCommand.getZenoExtension.error=" + e);
			return null;
		}
//System.out.println("EditTopicCommand.getZenoExtension.extension="+extension);
		if ((extension != null) && !extension.isEmpty()) {
			String menu = extension.getArticleMenu();
			if ((menu == null) || menu.trim().equals("")) {
				return null;
			}
			result.add(menu);
			result.add(extension.getArticleIcon());
			String articleURL = extension.getArticleURL();
//System.out.println("EditTopicCommand.getZenoExtension.URL="
//+extension.getArticleURL());
			String articleParams = extension.getArticleParams();
//System.out.println("EditTopicCommand.getZenoExtension.articleParams="
//+extension.getArticleParams());
			StringBuffer url = new StringBuffer(articleURL);
			if (!articleParams.equals("")) {
				url.append("?");
				url.append(evaluate(ctx, articleParams));
			}
			result.add(url.toString());
		}
//System.out.println("EditTopicCommand.getZenoExtension.result.="+result);
		return result;
	}


	/**
	 *  Gets the BagArticles attribute of the EditTopicCommand object
	 *
	 *@param  ctx  Description of Parameter
	 *@param  bag  Description of Parameter
	 *@return      The BagArticles value
	 */
	private List getBagArticles(Context ctx, Iterator bag) {
		ArrayList result = new ArrayList();
		if (bag == null) {
			return result;
		}
		while (bag.hasNext()) {
			int idNr = (new Tools()).toInt((String) bag.next());
			ZenoResource resource = null;
			try {
				resource = (ZenoResource) ((Factory) ctx.get(Constants.FACTORY_KEY)).loadResource(idNr);
			}
			catch (Exception e) {
				System.out.println("EditJournalCommand.separateBagArticlesAndBagJournals.error="
						 + e.toString());
			}
			if (resource instanceof Article) {
				result.add(resource);
			}
		}
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
	private String makeEmailParams(
			Context ctx,
			Article me) {
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
	 *  Evaluates the String what. Every $variable is replaced by the return value
	 *  of the appropriate Article method. The result is inserted into the result
	 *  String. Finaly all parts embraced by "$(", "$)" are recursively replaced by
	 *  ZenoEncoded values.
	 *
	 *@param  ctx   Velocity Context
	 *@param  what  String
	 *@return       String
	 */
	private String evaluate(Context ctx, String what) {
//System.out.println("editTopicCommand.evaluate.what:"+what+".");
		if (what.indexOf("$") < 0) {
			return what;
		}
		String result = replaceDollarMethods(ctx, what);
		result = replaceBrackets(ctx, result);
		return result;
	}


	/**
	 *  Evaluates the String what. Every $variable is replaced by the return value
	 *  of the appropriate Article method.
	 *
	 *@param  ctx   Velocity Context
	 *@param  what  String
	 *@param  enc   Description of Parameter
	 *@return       String
	 */
	private String replaceDollarMethods(Context ctx, String what) {
		StringTokenizer dollarMethods = new StringTokenizer(what, "$");
		StringBuffer result = new StringBuffer(dollarMethods.nextToken());
		while (dollarMethods.hasMoreTokens()) {
			String dollarMethod = dollarMethods.nextToken();
//System.out.println("editTopicCommand.replaceDollarMethods.dollarMethod="
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
//System.out.println("editTopicCommand.replaceDollarMethods.varName="+varName
//+", rest="+rest);
				if (varName.equals("getURL")) {
					String varVal = ctx.get(Constants.SERVER_NAME) + ":"
							 + ctx.get(Constants.SERVER_PORT) + request.getRequestURI()
							 + "?action=editTopic&view=print&id=" + me.getId();
					result.append(varVal);
				}
				else {
					HttpSession sess = request.getSession();
					Object o = sess.getAttribute(varName);
					if (o != null) {
						result.append(enc.encode(o.toString()));
					}
					else {
//System.out.println("editTopicCommand.replaceDollarMethods.varName(2)="+varName
//+", rest="+rest);
						Class myClass = me.getClass();
						Method method = null;
						Object articleMethodResult = null;
						try {
							method = myClass.getMethod(varName, null);
							articleMethodResult = method.invoke(me, null);
							if (articleMethodResult instanceof Date) {
								result.append(enc.encode(
										ZenoUtilities.getIsoString((Date) articleMethodResult)));
							}
							else {
								result.append(enc.encode(
										articleMethodResult.toString()));
							}
//System.out.println("editTopicCommand.replaceDollarMethods.result=" + result);
						}
						catch (Exception e) {
							System.out.println("editTopicCommand.evaluate.error=" + e + ":" + what);
							result.append("***error***(" + what + ")");
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
			if (!Character.isLetter(probe.charAt(i))) {
				break;
			}
			i++;
		}
//System.out.println("EditTopicCommand.dollarMethodEndPostion.probe="+probe
//+", i="+i);
		return i;
	}


	/**
	 *  All parts embraced by "$(", "$)" are recursively replaced by ZenoEncoded
	 *  values.
	 *
	 *@param  ctx   Velocity Context
	 *@param  what  String
	 *@param  enc   Description of Parameter
	 *@return       String
	 */
	private String replaceBrackets(Context ctx, String what) {
//System.out.println("editTopicCommand.replaceBrackets.what:"+what+".");
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
//System.out.println("editTopicCommand.replaceBrackets.result:"+result+".");
		return result;
	}


	/**
	 *  Separtates journals and articles in the trash can
	 *
	 *@param  trash  Input Iterator
	 *@return        Description of the Returned Value
	 *@since
	 */
	private List selectTrashArticles(Iterator trash) {
		List art = new ArrayList();
		while (trash.hasNext()) {
			ZenoResource resource = (ZenoResource) trash.next();
			if (resource instanceof Article) {
				Topic tpc = null;
				try {
					tpc = ((Article) resource).getTopic();
				}
				catch (Exception e) {
				}
				if ((tpc != null) && (myIdNr == tpc.getId())) {
					art.add(resource);
				}
			}
		}
		return art;
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

	static {
		templates.put("bag", "topic/editTopicBag.vm");
		templates.put("edit", "topic/editTopic.vm");
		//templates.put("new", "topic/editTopicNew.vm");
		templates.put("print", "topic/editTopicPrint.vm");
		templates.put("respond", "topic/editTopicRespond.vm");
		templates.put("search", "topic/editTopicSearch.vm");
		templates.put("struct", "topic/editTopicStruct.vm");
		templates.put("trash", "topic/editTopicTrash.vm");
	}
}

