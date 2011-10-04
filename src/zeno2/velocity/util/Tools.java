package zeno2.velocity.util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import zeno2.util.ZenoUtilities;
import zeno2.kernel.Principal;

/**
 *  Toolbox for Velocity Temoplates
 *
 *@author     oppor
 *@VERSION 2.0.2, 2001-11-15
 */
public class Tools {
	/**
	 *  Description of the Field
	 */
	public final Date TODAY = new Date();
	/**
	 *  Description of the Field
	 */
	public final String LASTYEAR = getLastYear();
	/**
	 *  Description of the Field
	 */
	public final String NEXTYEAR = getNextYear();
	/**
	 *  Description of the Field
	 */
	public final String THISYEAR = getThisYear();

	Object o = null;


	/**
	 *  Constructor for the Tools object
	 */
	public Tools() {
		super();
	}


	/**
	 *  Constructor for the Tools object
	 *
	 *@param  o  any object
	 */
	public Tools(Object o) {
		this.o = o;
	}


	/**
	 *  give any object to Tools
	 *
	 *@param  o  any object
	 */
	public void set(Object o) {
		this.o = o;
	}


	/**
	 *@return    a string containing the class name and the string value of the
	 *      object.
	 */
	public String getDiag() {
		return (o == null) ? "null" :
				"Class="
				 + o.getClass().getName()
				 + ",toString=" + o.toString();
	}


	/**
	 *@param  o  Description of Parameter
	 *@return    a string containing the class name and the string value of the
	 *      object.
	 */
	public String getDiag(Object o) {
		if (o == null) {
			return "Class=Object, Instance is null";
		}
		return "Class="
				 + o.getClass().getName()
				 + ",toString=" + o.toString();
	}


	/**
	 *  Gets the Iso attribute of the Tools object
	 *
	 *@param  date  Description of Parameter
	 *@return       The Iso value
	 */
	public String getIso(Date date) {
		if (date == null) {
			return "";
		}
		String result = ZenoUtilities.getIsoString(date);
		if (result.equals("0002-11-30")) {
			return "";
		}
		else {
			return result;
		}
	}


	/**
	 *  Gets the Year attribute of the Tools object
	 *
	 *@param  date  Description of Parameter
	 *@return       The Year value
	 */
	public String getYear(Date date) {
		DateFormat df = new SimpleDateFormat("yyyy");
		return df.format(date);
	}


	/**
	 *  Gets the ThisYear attribute of the Tools object
	 *
	 *@return    The ThisYear value
	 */
	public String getThisYear() {
		return getYear(TODAY);
	}


	/**
	 *  Gets the LastYear attribute of the Tools object
	 *
	 *@return    The LastYear value
	 */
	public String getLastYear() {
		String thisYear = getThisYear();
		try {
			int nextY = -1 + Integer.parseInt(thisYear);
			return Integer.toString(nextY);
		}
		catch (Exception e) {
			System.out.println("Tools.getNextYear.error=" + e.toString());
			return "00";
		}
	}


	/**
	 *  Gets the NextYear attribute of the Tools object
	 *
	 *@return    The NextYear value
	 */
	public String getNextYear() {
		String thisYear = getThisYear();
		try {
			int nextY = 1 + Integer.parseInt(thisYear);
			return Integer.toString(nextY);
		}
		catch (Exception e) {
			System.out.println("Tools.getNextYear.error=" + e.toString());
			return "00";
		}
	}


	/**
	 *  Gets the LastMonth attribute of the Tools object
	 *
	 *@param  isodate  Description of Parameter
	 *@return          The LastMonth value
	 */
	public String getLastMonth(String isodate) {
		Date d = ZenoUtilities.firstOfMonth(
				ZenoUtilities.getDateFromIsoString(isodate));
		d = ZenoUtilities.plusDays(d, -1);
		DateFormat df = new SimpleDateFormat("yyyy-MM");
		return df.format(d);
	}


	/**
	 *  Gets the NextMonth attribute of the Tools object
	 *
	 *@param  isodate  Description of Parameter
	 *@return          The NextMonth value
	 */
	public String getNextMonth(String isodate) {
		Date d = ZenoUtilities.lastOfMonth(
				ZenoUtilities.getDateFromIsoString(isodate));
		d = ZenoUtilities.plusDays(d, +1);
		DateFormat df = new SimpleDateFormat("yyyy-MM");
		return df.format(d);
	}


	/**
	 *  Gets the ThisMonth attribute of the Tools object
	 *
	 *@param  isodate  Description of Parameter
	 *@return          The ThisMonth value
	 */
	public String getThisMonth(String isodate) {
		Date d = (isodate.equals("")) ? new Date() :
				ZenoUtilities.firstOfMonth(ZenoUtilities.getDateFromIsoString(isodate));
		DateFormat df = new SimpleDateFormat("yyyy-MM");
		return df.format(d);
	}


	/**
	 *  returns the Type name of the object
	 *
	 *@param  o  object
	 *@return    a String
	 */
	public String getType(Object o) {
		String result = o.getClass().getName();
		return result.substring(result.lastIndexOf(".") + 1);
	}


	/**
	 *@param  i  Description of Parameter
	 *@return    a String
	 */
	public String toString(int i) {
		return Integer.toString(i);
	}


	/**
	 *@param  s  Description of Parameter
	 *@return    an int value
	 */
	public int toInt(String s) {
		//System.out.println("Tools.toInt.s=" + s);
		try {
			return Integer.parseInt(s.trim());
		}
		catch (Exception e) {
			//System.out.println("Tools.toInt.error=" + e.toString());
			return -1;
		}
	}


	/**
	 *  Encodes an URL
	 *
	 *@param  s  String URL
	 *@return    the encoded URL
	 */
	public String encodeZenoUrl(String s) {
		try {
			//return URLEncoder.encode(s,"x-www-form-urlencoded");*** 1.4
			return URLEncoder.encode(s);
		}
		catch (/*UnsupportedEncoding*/Exception e) {
			System.out.println("util.tools.encodeZenoUrl.error="+e.toString());
			return s;
		}
	}


	/**
	 *  Decodes an encoded URL
	 *
	 *@param  s  encoded URL
	 *@return    the decoded URL
	 */
	public String decodeZenoUrl(String s) {
		int p = s.indexOf("zenourl=");
		String zenoUrl = (p >= 0) ? s.substring(p + 8) : s;
		try {
			//return URLDecoder.decode(zenoUrl,"x-www-form-urlencoded"); ***1.4
			return URLDecoder.decode(zenoUrl);
		}
		catch (/*UnsupportedEncoding*/Exception e) {
			System.out.println("util.tools.encodeZenoUrl.error="+e.toString());
			return s;
		}		
	}


	/**
	 *  filters LineFeeds and URLs
	 *
	 *@param  note   String
	 *@param  count  int max lines
	 *@return        modified note String
	 */
	public String filterNote(String note, int count) {
		return filterURLs(filterLineFeeds(note, count));
	}


	/**
	 *  replaces " by \"
	 *
	 *@param  ipt  String
	 *@return      modified ipt
	 */
	public String filterQuote(String ipt) {
		if (ipt == null) {
			return "";
		}
		StringBuffer result = new StringBuffer();
		int from = 0;
		int to = ipt.indexOf("\"");
		while (to >= 0) {
			result.append(ipt.substring(from, to));
			result.append("'");
			from = to + 1;
			to = ipt.indexOf("\"", from);
		}
		from = 0;
		to = ipt.indexOf("\\");
		while (to >= 0) {
			result.append(ipt.substring(from, to));
			result.append(" ");
			from = to + 1;
			to = ipt.indexOf("\\", from);
		}
		result.append(ipt.substring(from));
		return result.toString();
	}


	/**
	 *  replaces less sign by &lt;
	 *
	 *@param  ipt  String
	 *@return      modified ipt
	 */
	public String filterLess(String ipt) {
		if (ipt == null) {
			return "";
		}
		StringBuffer result = new StringBuffer();
		int from = 0;
		int to = ipt.indexOf("<");
		while (to >= 0) {
			result.append(ipt.substring(from, to));
			result.append("&lt;");
			from = to + 1;
			to = ipt.indexOf("<", from);
		}
		result.append(ipt.substring(from));
		return result.toString();
	}


	/**
	 *  shortens the title to max characters;
	 *
	 *@param  title  String
	 *@param  max    int
	 *@return      shoretened title
	 */
	public String shortenTitle(String title,int max) {
		if (title == null) {
			return "";
		}
		if((title.length()<= max) || (max<1)) {
			return filterLess(title);
		}
		else {
			return filterLess(title.substring(0,max))+"...";
		}
	}


	/**
	 *  shortens the title to 10 characters;
	 *
	 *@param  title  String
	 *@return      shoretened title
	 */
	public String shortenTitle(String title) {
		return shortenTitle(title,10);
	}


	/**
	 *  filters LineFeeds and URLs
	 *
	 *@param  note  String
	 *@return       modified note String
	 */
	public String filterNote(String note) {
		return filterNote(note, 0);
	}


	/**
	 *  Description of the Method
	 *
	 *@param  src  String
	 *@return      String Hexadecimal representation of src
	 */
	public String hex(String src) {
		if (src == null || src.equals("")) {
			return "";
		}
		String res = "";
		byte[] b = src.getBytes();
		for (int i = 0; i < b.length; i++) {
			res += Integer.toHexString(b[i]);
		}
		return res;
	}


	/**
	 *  Description of the Method
	 *
	 *@param  principals  Description of Parameter
	 *@param  selected    Description of Parameter
	 *@return             Description of the Returned Value
	 */
	public List genSelectOptions(List principals, String selected) {
		List options = new ArrayList();
		Iterator it = principals.iterator();
		StringBuffer buf = new StringBuffer();
		while (it.hasNext()) {
			Principal pr = (Principal) it.next();
			buf.setLength(0);
			buf.append("<option");
			if (pr.getId().equals(selected)) {
				buf.append(" selected");
			}
			buf.append(" >");
			buf.append(pr.getId());
			buf.append("</option>");
			options.add(buf.toString());
		}
		return options;
	}


	/**
	 *  replaces upto "count" linefeed characters by the appropriate HTML tag
	 *
	 *@param  note   String
	 *@param  count  int Max lines
	 *@return        Modified note String
	 */
	private String filterLineFeeds(String note, int count) {
		int beginLine = note.indexOf("\n");
		int endLine = 0;
		int max = (count > 0) ? count : 200;
		int n = 0;
		if (beginLine < 0) {
			return note;
		}
		StringBuffer result = new StringBuffer(note.substring(0, beginLine));
		beginLine++;
		while (beginLine > 0) {
			result.append("<br>");
			endLine = note.indexOf("\n", beginLine);
			//System.out.println("Tools.filterLF.beginLine="
			//		 + Integer.toString(beginLine)
			//		 + "endLine="
			//		 + Integer.toString(beginLine)
			//		);
			if (endLine > 0) {
				result.append(note.substring(beginLine, endLine));
				beginLine = (n < max) ? endLine + 1 : -1;
			}
			else {
				result.append(note.substring(beginLine));
				beginLine = -1;
			}
			n++;
		}
		return result.toString();
	}


	/**
	 *  replaces any linefeed character by the appropriate HTML tag
	 *
	 *@param  note  String
	 *@return       Modified note String
	 */
	private String filterLineFeeds(String note) {
		return filterLineFeeds(note, 0);
	}



	/**
	 *  replaces every url by an anchor of that url
	 *
	 *@param  note  String
	 *@return       String
	 */
	private String filterURLs(String note) {
		//System.out.println("Tools.filterURLs.note=" + note);
		int urlBegin = minPos(note.indexOf("http:"), note.indexOf("ftp:"),
				note.indexOf("mailto:"));
		//System.out.println("Tools.filterURLs.urlBegin=" + Integer.toString(urlBegin));

		if (urlBegin < 0) {
			return note;
		}
		if ((urlBegin > 0) &&
				(note.substring(urlBegin - 1, urlBegin).equals("\""))) {
			StringBuffer result = new StringBuffer(note.substring(0, urlBegin + 1));
			result.append(filterURLs(note.substring(urlBegin + 1)));
			return result.toString();
		}
		//if ((urlBegin > 0) && (note.substring(urlBegin - 1, urlBegin).equals("\""))) {
		//	return note;
		//}
		int urlEnd = minPos(
				note.indexOf(" ", urlBegin),
				note.indexOf("<br>", urlBegin),
				note.indexOf("\n", urlBegin)
				);
		//System.out.println("Tools.filterURLs.urlEnd=" + Integer.toString(urlEnd));
		if (urlEnd < 0) {
			urlEnd = note.length();
		}
		StringBuffer result = new StringBuffer(note.substring(0, urlBegin));
		result.append(makeAnchor(note.substring(urlBegin, urlEnd)));
		result.append(filterURLs(note.substring(urlEnd)));
		return result.toString();
	}


	/**
	 *  makes an anchor of the inoput String
	 *
	 *@param  url  String
	 *@return      String anchor
	 */
	private String makeAnchor(String url) {
		return "<a href=\"" + url + "\">" + url + "</a>";
	}


	/**
	 *  givs minimal value contained in the input array
	 *
	 *@param  x  int array
	 *@return    minimum value of members of x
	 */
	private int minPos(int[] x) {
		int result = -1;
		for (int i = 0; i < x.length; i++) {
			if ((x[i] >= 0) && ((x[i] < result) || (result < 0))) {
				result = x[i];
			}
		}
		return result;
	}


	/**
	 *  gives the minimum of x,y,z and u
	 *
	 *@param  x  int
	 *@param  y  int
	 *@param  z  int
	 *@param  u  int
	 *@return    minimum of x, y,z and u
	 */
	private int minPos(int x, int y, int z, int u) {
		int[] xx = {x, y, z, u};
		return minPos(xx);
	}


	/**
	 *  gives the minimum of x,y and z
	 *
	 *@param  x  int
	 *@param  y  int
	 *@param  z  int
	 *@return    minimum of x, y and z
	 */
	private int minPos(int x, int y, int z) {
		int[] xx = {x, y, z};
		return minPos(xx);
	}


	/**
	 *  gives the minimum of l and r
	 *
	 *@param  l  int left value
	 *@param  r  int right value
	 *@return    the minimum of l and r
	 */
	private int minPos(int l, int r) {
		if (l > 0) {
			if (l < r) {
				return l;
			}
			else {
				return r;
			}
		}
		else {
			return r;
		}
	}

}

