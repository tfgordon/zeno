package zeno2.velocity.action;

/*
 *
 *
 *
 *
 */

import java.io.File;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.velocity.context.Context;
import org.apache.velocity.app.tools.*;
import org.apache.velocity.servlet.VelocityServlet;

import zeno2.kernel.Article;
import zeno2.kernel.Constants;
import zeno2.kernel.Factory;
import zeno2.kernel.Journal;
import zeno2.kernel.NoPermissionException;
import zeno2.kernel.OutlineNode;
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
 *@version    2.0.2, 2001-08-31
 */
public class EditJournalCommand extends Command {

	private Journal me = null;
	private Factory factory = null;

	/**
	 *  Description of the Field
	 */
	public static Properties templates = new Properties();
	/**
	 *  Description of the Field
	 */
	public final static String orderOptions =
			"modification_date,rank,title,label,qualifier,creator,modifier,"
			 + "creation_date,begin_date,topic,attachment";


	/**
	 *  Constructor for the ListCommand object
	 *
	 *@param  req   Description of Parameter
	 *@param  resp  Description of Parameter
	 *@since
	 */
	public EditJournalCommand(HttpServletRequest req, HttpServletResponse resp) {
		super(req, resp);
	}


	/**
	 *  Get id and view parameters, get the journal(id), put id and journal into
	 *  the context, and select the appropriate template
	 *
	 *@param  ctx            Velocity Context
	 *@return                the appropriate template file name
	 *@exception  Exception  Description of Exception
	 *@since
	 */
	public String exec(Context ctx) throws Exception {

		factory = (Factory) ctx.get(Constants.FACTORY_KEY);
		forumServlet = (String) ctx.get("servl");
		String id = request.getParameter("id");
		int idNr = (new Tools()).toInt(id);
		String view = request.getParameter("view");
		ZenoBundle msg = (ZenoBundle) ctx.get(Constants.RESOURCE_KEY);

		if ((view == null) || (view.length() == 0)) {
			view = "struct";
		}
		try {
			me = (Journal) factory.loadResource(idNr);
			ctx.put(Constants.JOURNAL_KEY, me);
			if (me == null) {
				return showMessage(ctx, "error.no_such_journal",
						"error.not_allowed_" + view + "_journal");
			}
			boolean foo = factory.hasRole("editor", me);//???????????????????????????????
			String dummy = me.getNote();// only to force "NoPermissionException
		}
		catch (NoPermissionException e) {
			return showMessage(ctx, "error.no_permission",
					"error.not_allowed_" + view + "_journal");
		}
		catch (Exception e) {
			return showMessage(ctx, "error.no_find",
					"error.not_allowed_" + view + "_journal");
		}

		String mailAlias = "-";
		// get Zeno Plugins for journal menu
		String zenoPlugins = "";
		List zenoPluginIdList = null;
		String styleSheet = "ss1";
		try {
			mailAlias = me.getMailAlias();
			if ((mailAlias == null) || (mailAlias.equals(""))) {
				mailAlias = "-";
			}
			zenoPlugins = me.getProperty("zenoExtensions");
			if (zenoPlugins != null) {
				List zExt = getZenoExtensions(ctx, zenoPlugins);
				if ((zExt != null) && !zExt.isEmpty()) {
					ctx.put("zenoextensions", zExt);
				}
//System.out.println("EditJournal.command.exec.zExt=" + zExt);
			}
			zenoPluginIdList = factory.getPluginIds();
//System.out.println("EditJournalCommand.exec.zenoPluginIdList=" + zenoPluginIdList);
			styleSheet = me.getStyleSheetUrl();
//System.out.println("EditJournalCommand.exec.styleSheet=" + styleSheet);
		}
		catch (Exception e) {
		}

		if ((styleSheet == null) || !styleSheet.startsWith("ss")) {
			styleSheet = "ss1";
		}
		ctx.put("zcss", "/zeno/css/" + styleSheet + ".css");
//System.out.println("EditJournalCommand.exec.css=" + ctx.get("zcss"));

		String ssList = getStyleSheetNames((String) ctx.get("csspath"));
//System.out.println("EditJournalCommand.exec.ssList=" + ssList);
		Iterator ssOptions = ZenoUtilities.getOptions(
				ssList, styleSheet, "style.", msg);
//System.out.println("EditJournalCommand.exec.ssOptions=" + ssOptions);
		ctx.put("id", id);
//System.out.println("EditJournalCommand.exec.id=" + id);
		if (!view.equals("new")) {
			ctx.put("navmode", view);
		}
		ctx.put("mode", view);
//System.out.println("EditJournalCommand.exec.mode=" + view);
		String title = me.getTitle();// Exception, when writer!!!!!!!!!!!!!!!!!!!!!!
		title = (new Tools()).filterQuote(title);
//System.out.println("EditJournalCommand.exec.title=" + title);
		ctx.put("title", title);// Exception, when writer!!!!!!!!!!!!!!!!!!!!!!

// to do:
//	get title even for onlyWriters
//	boolen notOnlyWriter = factory.hasRole("editor", me)
//		||factory.hasRole("editor", me)
//	if (notOnlyWriter) {

		ctx.put("note", me.getNote());
		ctx.put("rank", "0");
		ctx.put("stylesheet", styleSheet);
		ctx.put("articlelabels", me.getArticleLabels());
		ctx.put("qualifiers", me.getQualifiers());
		ctx.put("linklabels", me.getLinkLabels());
		ctx.put("mailalias", mailAlias);
		ctx.put(Constants.JOURNAL_KEY, me);
		ctx.put("emailparams", makeEmailParams(ctx, me));
		ctx.put("revisionperiod", Integer.toString(me.getRevisionPeriod()));

//}				end of to do

		// ************************** noteDown ***************************
		if (view.equals("noteDown")) {
			markMeForBag(ctx);
			expandTopics(ctx, me);
			view = "struct";
		}
		// ************************** bag ********************************
		else if (view.equals("bag")) {
//System.out.println("EditJournalCommand.exec.view=" + view);
			List bagArticles = new ArrayList();
			List bagTopics = new ArrayList();
			List bagJournals = new ArrayList();
			HttpSession sess = request.getSession();
			ArrayList bagList = (ArrayList) sess.getAttribute(Constants.BAG_KEY);
//System.out.println("EditJournalCommand.exec.bag=" + bagList);
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
		}
		// ************************** edit *******************************
		else if (view.equals("edit")) {
			ctx.put("rank", Integer.toString(me.getRank()));
			if ((zenoPluginIdList != null) && !zenoPluginIdList.isEmpty()) {
				String zenoPluginIds = ZenoUtilities.listToString(zenoPluginIdList);
//System.out.println("EditJournalCommand.exec.zenoPluginIds="+zenoPluginIds);
				Iterator zenoExtOptions = ZenoUtilities.getOptions(
						zenoPluginIds, zenoPlugins);
				ctx.put("zenoextoptions", zenoExtOptions);
				ctx.put("stylesheet", ssOptions);
				ctx.put("boundArticles", me.getArticlesByTopic().get(0));
				ctx.put("freeArticles", me.getArticlesByTopic().get(1));
			}
		}
		// ************************** new ********************************
		else if (view.equals("new")) {
			ctx.put("title", "");
			ctx.put("note", "");
			ctx.put("revisionperiod", "-1");
			ctx.put("mailalias", "-");
			if ((zenoPluginIdList != null) && !zenoPluginIdList.isEmpty()) {
				String zenoPluginIds = ZenoUtilities.listToString(zenoPluginIdList);
//System.out.println("EditJournalCommand.exec.zenoPluginIds="+zenoPluginIds);
				Iterator zenoExtOptions = ZenoUtilities.getOptions(
						zenoPluginIds, "");
//System.out.println("EditJournalCommand.exec.zenoExtOptions="+zenoExtOptions);
				ctx.put("zenoextoptions", zenoExtOptions);
				ctx.put("stylesheet", ssOptions);
			}
		}
		// *********************** search ********************************
		else if (view.equals("search")) {
//System.out.println("EditJournalCommand.exec.view=" + view
//+", orderOptions="+orderOptions);
			ctx.put("orderoptions", ZenoUtilities.getOptions(orderOptions, "",
					"article.", msg));
			ctx.put("pattern", "");
			ctx.put("orderby", "modification_date");
			ctx.put("articles", new ArrayList());
			ctx.put("articles",
					me.search(null, null, null, null, null, null, null,"modification_date desc"));
		}
		// ********************* timeline ********************************
		else if (view.equals("timeline")) {
			setTimeLineValues(ctx, request.getParameter("month"));
		}
		// *********************** trash *********************************
		else if (view.equals("trash")) {
			List trashArticles = new ArrayList();
			List trashJournals = new ArrayList();
			separateTrashArticlesAndTrashJournals(me.getTrash()
					, trashArticles
					, trashJournals);
			ctx.put("trasharticles", trashArticles);
			ctx.put("trashjournals", trashJournals);
		}
		// *********************** labels *** for diagnosis **************
		else if (view.equals("labels")) {
			Enumeration e = msg.getKeys();
			List allMsgs = new ArrayList();
			while (e.hasMoreElements()) {
				String key = (String) e.nextElement();
				String[] msgElement = {key, msg.getString(key)};
				allMsgs.add(msgElement);
			}
			//Collections.sort(allMsgs);
			ctx.put("allmsgs", allMsgs);
		}
		// *********************** structure *********************************
		else if ( view.equals("struct") ) {
			expandTopics(ctx, me);
		}
		String retval = templates.getProperty(view, "");
		if (!retval.equals("")) {
			return retval;
		}
		else {
			return showMessage(ctx, "error.no_such_view: " + view);
		}

	}


	/**
	 *  Sets the TimeLineValues attribute of the EditJournalCommand object
	 *
	 *@param  ctx    The new TimeLineValues value
	 *@param  month  The new TimeLineValues value
	 */
	private void setTimeLineValues(Context ctx, String month) {
		Date artBeginDate = null;
		Date artEndDate = null;
		Date logBeginDate = null;
		Date logEndDate = null;
		if (month == null || month.equals("")) {
			artBeginDate = ZenoUtilities.getDayBegin(new Date());
			artEndDate = ZenoUtilities.lastOfMonth(artBeginDate);
			logEndDate = ZenoUtilities.getDayEnd(new Date());
			logBeginDate = ZenoUtilities.firstOfMonth(logEndDate);
		}
		else {
			artBeginDate = ZenoUtilities.firstOfMonth(month);
			logBeginDate = ZenoUtilities.firstOfMonth(month);
			artEndDate = ZenoUtilities.lastOfMonth(month);
			logEndDate = ZenoUtilities.lastOfMonth(month);
		}
		ctx.put("artfrom", ZenoUtilities.getIsoString(artBeginDate));
		ctx.put("artto", ZenoUtilities.getIsoString(artEndDate));
		ctx.put("logfrom", ZenoUtilities.getIsoString(logBeginDate));
		ctx.put("logto", ZenoUtilities.getIsoString(logEndDate));

		ctx.put("articles", getArticleEntries(artBeginDate, artEndDate));
		ctx.put("log", getLogEntries(logBeginDate, logEndDate));
	}


	/**
	 *  Sets the NoCache attribute of the EditJournalCommand object
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
	 *  Gets the article entries in this journal between begin date and end date
	 *
	 *@param  beginDate  Date
	 *@param  endDate    Date
	 *@return            articles Iterator
	 */

	private List getArticleEntries(Date beginDate, Date endDate) {
		List articles = null;
		try {
			//articles = me.search(null,null,null,null
			//		,beginDate, endDate,null,null);
			articles = me.getArticlesBetween(beginDate, endDate);
//System.out.println("EditJournalCommand.exec.beginDate="+beginDate
//	+", endDate="+endDate+", articles="+articles);
		}
		catch (Exception e) {
			System.out.println("EditJournalCommand.getLogEntries.error="
					 + e.toString());
		}
		return articles;
	}


	/**
	 *  Gets the log entries in this journal between begin date and end date
	 *
	 *@param  beginDate  Date
	 *@param  endDate    Date
	 *@return            events Iterator
	 *@since
	 */

	private Iterator getLogEntries(Date beginDate, Date endDate) {
		Iterator events = null;
		try {
			events = me.getEventsDuring(beginDate, endDate);
		}
		catch (Exception e) {
			System.out.println("EditJournalCommand.getLogEntries.error="
					 + e.toString());
		}
		return events;
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
//System.out.println("editJournalCommand.getZenoExtensions.result="+result);
		if (result.isEmpty()) {
			return null;
		}
		return result;
	}


	/**
	 *  Eventually replaces a variable by the return value of the appropriate
	 *  Journal method and URLencodes the right hand part of the key/value pair.
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
			System.out.println("EditJournalCommand.getZenoExtension.error=" + e);
			return null;
		}
		if ((extension != null) && !extension.isEmpty()) {
			String menu = extension.getJournalMenu();
			if ((menu == null) || menu.trim().equals("")) {
				return null;
			}
			result.add(menu);
			result.add(extension.getJournalIcon());
			String journalURL = extension.getJournalURL();
			String journalParams = extension.getJournalParams();
//System.out.println("EditArticleCommand.getZenoExtension.journalParams="
//+extension.journalParams());
			StringBuffer url = new StringBuffer(journalURL);
			if (!journalParams.equals("")) {
				url.append("?");
				url.append(evaluate(ctx, journalParams));
			}
			result.add(url.toString());
		}
//System.out.println("EditJournalCommand.getZenoExtension.result.="+result);
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


	/**
	 *  Separtates journals and articles in the trash can
	 *
	 *@param  trash  Input Iterator
	 *@param  art    Output List of trash can articles
	 *@param  journ  Output List of trash can journals
	 *@since
	 */
	private void separateTrashArticlesAndTrashJournals(Iterator trash,
			List art, List journ) {

		while (trash.hasNext()) {
			ZenoResource resource = (ZenoResource) trash.next();
			if (resource instanceof Article) {
				art.add(resource);
			}
			else if (resource instanceof Journal) {
				journ.add(resource);
			}
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
//System.out.println("editJournalCommand.replaceDollarMethods.dollarMethod="
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
//System.out.println("editJournalCommand.replaceDollarMethods.varName=" + varName
//+ ", rest=" + rest);
				if (varName.equals("getURL")) {
					String varVal = ctx.get(Constants.SERVER_NAME) + ":"
							 + ctx.get(Constants.SERVER_PORT) + request.getRequestURI()
							 + "?action=editArticle&view=struct&id=" + me.getId();
					result.append(varVal);
				}
				else {
					HttpSession sess = request.getSession();
					int p = varName.indexOf(".");
//System.out.println("editJournalCommand.replaceDollarMethods.p="
//+ Integer.toString(p));
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

	private void expandTopics(Context ctx, Journal me){
		try {
			String expand = request.getParameter("expand");
			String collapse = request.getParameter("collapse");
			List nodeblocks = me.getFullOutline();
			List topicblocks = (List)nodeblocks.get(0);
			ctx.put("topicblocks", topicblocks);
			List freearticles = (List)nodeblocks.get(1);
			ctx.put("freearticles", freearticles);
			HttpSession sess = request.getSession();
			
			List topicsList = new ArrayList();
			Iterator topicblocksIt = topicblocks.iterator();
			while (topicblocksIt.hasNext()){
				List topicblock = (List)topicblocksIt.next();
				OutlineNode node = (OutlineNode)topicblock.get(0);
				topicsList.add(node.getId());
			}
			Hashtable expandLists = (Hashtable) sess.getAttribute("expandLists");
			if (expandLists == null) {
				expandLists = new Hashtable();
			}
			List expandList = (ArrayList)expandLists.get(new Integer(me.getId()));
			System.out.println(expandList);
			if (expandList == null || (expand != null && expand.equals("all"))) {
				expandList = new ArrayList();
			}
			if ((collapse != null || collapse != "") && topicsList.contains(collapse)
				 && !expandList.contains(collapse) ){
				expandList.add(collapse);
			}
			if ((expand != null || expand != "") && expandList.contains(expand)){
				expandList.remove(expandList.indexOf(expand));
			}
			if (collapse != null && collapse.equals("all")) {
				expandList = topicsList;
			}
			expandLists.remove(new Integer(me.getId()));
			if (!expandList.isEmpty()) {
				expandLists.put(new Integer(me.getId()),expandList);
			}
			sess.setAttribute("expandLists", expandLists);
			ctx.put("expandTopicsIds", expandList);
			ctx.put("expandLists", expandLists);

			return;
		}
		catch (Exception e) {
			
		}
	}

	static {
		templates.put("bag", "forum/journal/editJournalBag.vm");
		templates.put("confirm", "forum/journal/editJournalConfirmReport.vm");
		//templates.put("custom", "editJournalCustom.vm");
		templates.put("edit", "forum/journal/editJournalEdit.vm");
		//templates.put("struct", "forum/journal/editJournalFront.vm");
		templates.put("labels", "forum/journal/showAllLabels.vm");// for Diagnosis
		//templates.put("list", "forum/journal/editJournalList.vm");
		//templates.put("log", "editJournalLog.vm");
		templates.put("new", "forum/journal/editJournalNew.vm");
		templates.put("search", "forum/journal/editJournalSearch.vm");
		templates.put("searchattachment", "forum/journal/editJournalSearchAttachment.vm");
		templates.put("searchtopic", "forum/journal/editJournalSearchTopic.vm");
		//templates.put("sort", "editJournalSort.vm");
		//templates.put("struct", "editJournalStruct.vm");
		templates.put("struct", "forum/journal/editJournalXStruct.vm");
		templates.put("timeline", "forum/journal/editJournalTimeline.vm");
		//templates.put("topics", "forum/journal/editJournalTopics.vm");
		templates.put("tree", "forum/journal/editJournalTree.vm");
		templates.put("treeNewWindow", "forum/journal/editJournalTreeNewWindow.vm");
		templates.put("trash", "forum/journal/editJournalTrash.vm");
		templates.put("more", "forum/journal/editJournalMore.vm");
	}
}

