package zeno2.velocity.action;

/*
 *
 *
 *
 *
 */
import com.oreilly.servlet.MultipartRequest;

import java.lang.reflect.Method;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.ServletInputStream;

import org.apache.velocity.context.Context;
import org.apache.velocity.servlet.VelocityServlet;

import zeno2.velocity.util.Errors;
import zeno2.kernel.Article;
import zeno2.kernel.Attachment;
import zeno2.kernel.Constants;
import zeno2.kernel.Factory;
import zeno2.kernel.Journal;
import zeno2.kernel.Monitor;
import zeno2.kernel.Plugin;
import zeno2.kernel.Principal;
import zeno2.kernel.Topic;
import zeno2.kernel.ZenoException;
import zeno2.kernel.ZenoResource;
import zeno2.util.ZenoUtilities;

import zeno2.velocity.util.Tools;
import zeno2.velocity.util.ZenoEncoder;

/**
 *  Handles zeno articles
 *
 *@author     <a href="mailto:lothar.oppor@ais.fraunhofer.de">Lothar Oppor</a>
 *@version   2.0.2,  2001-09-23
 */
public class PostEditAttachmentCommand extends Command {

	private Article me = null;
	private static Properties uglyMimeTypes = new Properties();
	private ZenoEncoder enc = new ZenoEncoder();

	/**
	 *  Constructor for the PostEditAttachmentCommand object
	 *
	 *@param  req   Description of Parameter
	 *@param  resp  Description of Parameter
	 */
	public PostEditAttachmentCommand(HttpServletRequest req, HttpServletResponse resp) {
		super(req, resp);
	}


	/**
	 *  Get uploaded file and make it persistent in zenoThe max upload file size
	 *  is read from the zeno configuration file<br>
	 *  ATTENTION: the max upload file size also depenmds from the database
	 *  property "max_allowed_package" (in the mysql configuration filefile
	 *  etc/my.cnf).
	 *
	 *@param  ctx            Velocity Context
	 *@return                the appropriate template file name
	 *@exception  Exception  The original Java Exception
	 */
	public String exec(Context ctx) throws Exception {

		System.out.println("PostEditAttachmentCommand.exec.name=request=" + request
				 + ", response=" + response);

		MultipartRequest multi = null;
		forumServlet = (String) ctx.get("servl");
//System.out.println("PostEditAttachmentCommand.exec.OK="+multi.getParameter("ok")
//+ ", ok_further="+multi.getParameter("ok_further"));

		try {
			//*********************************************************************
			// in /etc/my.cnf (the mysql configuration file) in Section [mysqld]
			//max_allowed_packet has to be set to "mb"M !!!
			// for instance:
			// set-variable   = max_allowed_packet=16M
			//*********************************************************************
			Monitor monitor = (Monitor) ctx.get(Constants.MONITOR_KEY);
			String megaBytes = monitor.getProperty("zenoMaxUploadFileSize", "16");
			int mb = (new Tools()).toInt(megaBytes);
			multi = new MultipartRequest(request, System.getProperty("java.io.tmpdir"),
					mb * 1024 * 1024);
		}
		catch (Exception e) {
			// ***************************************************************
			// This does not work because supposedly the "new MultipartRequest"
			// corrupts the socket connection!!!
			// ***************************************************************
			ctx.put("id", "");
			ctx.put("action", "posteditattachment");
//System.out.println("PostEditAttachmentCommand.exec.action=posteditattachment");
			return showMessage(ctx, "error.file_to_big");
		}
		String id = multi.getParameter("id");
		String oldName = multi.getParameter("oldname");
		String rename = multi.getParameter("rename").trim();
		int idNr = (new Tools()).toInt(id);
		me = null;
		try {
			//get parameters
			me = (Article)
					((Factory) ctx.get(Constants.FACTORY_KEY)).loadResource(idNr);
			ctx.put("id", id);
			ctx.put(Constants.ARTICLE_KEY, me);
			Journal parent = (Journal) me.getParent();
			String styleSheet = parent.getStyleSheetUrl();
			if ((styleSheet == null) || !styleSheet.startsWith("ss")) {
				styleSheet = "ss1";
			}
			ctx.put("zcss", "/zeno/css/" + styleSheet + ".css");
			ctx.put("parentid",Integer.toString(parent.getId()));
		}
		catch (Exception e) {
			System.out.println("PostEditAttachmentCommand.exec.error=" + e.toString());
			//e.printStackTrace();
			return showMessage(ctx,"error.no_such_article","error.just_deleted");
		}

		ctx.put("emailparams", makeEmailParams(ctx, me));
		if (multi.getParameter("cancel") != null) {
			// *********************** cancel ***************************
			//return EditArticleCommand.templates.getProperty("print");
		}
		else if (multi.getParameter("delete") != null) {
			// *********************** delete ***************************
			try {
				me.deleteAttachment(oldName);
			}
			catch (Exception e) {

			}
		}
		else if (multi.getParameter("replace") != null) {
			// *********************** replace ***************************
			try {
				String fileName = multi.getFilesystemName("file");
				String type = multi.getContentType("file");
				System.out.print("PostEditAttachmentCommand.exec.replace.type=" + type);
				type = filterMimeType(type);
				System.out.println(", type=" + type);
				File f = multi.getFile("file");
				if ((f != null) && !oldName.equals("")) {
					Attachment att = me.getAttachment(oldName);
					att.setContents(f);
					att.setMimeType(type);
					att.save();
				}
				else {
					Errors err = new Errors();
					err.addError("error.empty_file");
					ctx.put(Constants.ERRORS, err.getErrors());
				}
			}
			catch (Exception e) {
				e.printStackTrace();
				Errors errors = new Errors();
				errors.addError("error.not_allowed_add_attachment");
				ctx.put(Constants.ERRORS, errors.getErrors());
			}
		}
		// ********** OK or OK + Attachment pushed ******************
		// *********************** add or edit ***********************
		else if ((multi.getParameter("ok") != null)
						 || (multi.getParameter("ok_further") != null)){
			try {
				String fileName = multi.getFilesystemName("file");
				String type = multi.getContentType("file");
				System.out.println("PostEditAttachmentCommand.exec.ok.type=" + type);
				// ** supposed bug in com.oreilly.servlet.MultipartRequest;
				if (type != null) {
					int p = type.indexOf(";");
					if (p > 0) {
						type = type.substring(0, p);
					}
				}
				// ** end of bug prevent
				System.out.println(", type=" + type);
				File f = multi.getFile("file");
				if (f != null) {
					if ((rename != null) && !rename.equals("")) {
						me.addAttachment(rename, type, f);
					}
					else {
						me.addAttachment(fileName, type, f);
					}
					String extensionId = multi.getParameter("extension");
					System.out.println("PostEditAttachmentCommand.exec.extension=" + extensionId);

					if (extensionId != null) {
						me.addXLink("http", genXLinkUrl(ctx, extensionId) + fileName
								, "view " + fileName);
					}
				}
				else if ((rename != null) && !rename.equals("")) {
					me.renameAttachment(oldName, rename);
				}
				else {
					Errors err = new Errors();
					err.addError("error.empty_file");
					ctx.put(Constants.ERRORS, err.getErrors());
				}
				if (multi.getParameter("ok_further") != null) {
					ctx.put("view","add");
					ctx.put("rename","");
					return EditAttachmentCommand.templates.getProperty("add");
				}
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
		String zenoPlugins = me.getProperty("zenoExtensions");
		if (zenoPlugins != null) {
			List zExt = getZenoExtensions(ctx, zenoPlugins);
			if ((zExt != null) && !zExt.isEmpty()) {
				ctx.put("zenoextensions", zExt);
			}
		}
/*
		if (me instanceof Topic) {
			ctx.put(Constants.TOPIC_KEY, me);
			return EditTopicCommand.templates.getProperty("print");
		}
		else {
			ctx.put(Constants.ARTICLE_KEY, me);
			return EditArticleCommand.templates.getProperty("print");
		}
*/
		return EditArticleCommand.templates.getProperty("print");
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
			extension = ((Factory)
					ctx.get(Constants.FACTORY_KEY)).loadPlugin(extensionId);
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
	 *  Description of the Method
	 *
	 *@param  ctx          Description of Parameter
	 *@param  extensionId  Description of Parameter
	 *@return              Description of the Returned Value
	 */
	private String genXLinkUrl(Context ctx, String extensionId) {
		Plugin extension = null;
		StringBuffer result = new StringBuffer();
		try {
			extension = ((Factory)
					ctx.get(Constants.FACTORY_KEY)).loadPlugin(extensionId);
		}
		catch (ZenoException e) {
			System.out.println("EditAttachmentCommand.addXLink.error=" + e);
		}
		if ((extension != null) && !extension.isEmpty()) {
			String addOn = extension.getAddOn();
			result.append(evaluate(ctx, addOn));
		}
		return result.toString();
	}


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
		System.out.println("PostEditAttachmentCommand.evaluate.what:" + what + ".");
		if (what.indexOf("$") < 0) {
			return what;
		}
		String result = replaceDollarMethods(ctx, what);
		result = replaceBrackets(ctx, result);
		System.out.println("PostEditAttachmentCommand.evaluate.result:" + result + ".");
		return result;
	}


	/**
	 *  Evaluates the String whatEvery $variable is replaced by the return value
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
			System.out.println("PostEditAttachmentCommand.replaceDollarMethods.dollarMethod="
					 + dollarMethod + ".");
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
				int nameEnd = dollarMethod.indexOf("&");
				if (nameEnd > 0) {
					varName = dollarMethod.substring(0, nameEnd);
					rest = dollarMethod.substring(nameEnd);
				}
				if (varName.equals("getURL")) {
					String varVal = ctx.get(Constants.SERVER_NAME) + ":"
							 + ctx.get(Constants.SERVER_PORT) + request.getRequestURI()
							 + "?action=editArticle&view=struct&id=" + me.getId();
					result.append(varVal);
				}
				else {
					HttpSession sess = request.getSession();
					Object o = sess.getAttribute(varName);
					if (o != null) {
						result.append(enc.encode(o.toString()));
					}
					else {
//System.out.println("PostEditAttachmentCommand.replaceDollarMethods.varName=" + varName+".");
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
//System.out.println("PostEditAttachmentCommand.replaceDollarMethods.result=" + result);
						}
						catch (Exception e) {
							System.out.println("PostEditAttachmentCommand.evaluate.error=" + e + ":" + what);
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
	 *  All parts embraced by "$(", "$)" are recursively replaced by ZenoEncoded
	 *  values.
	 *
	 *@param  ctx   Velocity Context
	 *@param  what  String
	 *@param  enc   Description of Parameter
	 *@return       String
	 */
	private String replaceBrackets(Context ctx, String what) {
//System.out.println("PostEditAttachmentCommand.replaceBrackets.what:"+what+".");
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
//System.out.println("PostEditAttachmentCommand.replaceBrackets.result:"+result+".");
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
			ZenoResource me) {
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

	private String filterMimeType(String what) {
		if (what == null) return "text/plain";
		String result = what;
		// ** supposed bug in com.oreilly.servlet.MultipartRequest;
		if (what != null) {
			int p = what.indexOf(";");
			if (p > 0) {
				result = what.substring(0, p);
			}
		}
		// ** end of bug prevent
		return uglyMimeTypes.getProperty(result,result);

	}

	static {
		uglyMimeTypes.put("application/x-sh", "text/plain");
		uglyMimeTypes.put("application/octet-stream", "text/plain");
		//uglyMimeTypes.put("?", "text/plain");
	}
}

