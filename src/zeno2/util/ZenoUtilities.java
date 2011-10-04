package zeno2.util;

import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import zeno2.velocity.util.ZenoBundle;
/**
 *  Description of the Class
 *
 *@author     oppor
 *@created    December 17, 2001
 */
public class ZenoUtilities {

	private static Properties weekDays = new Properties();


	/**
	 *  Converts the input Set to a "sep"-separated list String
	 *
	 *@param  set  The input Set
	 *@param  sep  The separator
	 *@return      "sep"-sparated list String
	 */
	public static String setToString(Set set, String sep) {
		StringBuffer buf = new StringBuffer();
		Iterator setit = set.iterator();
		while (setit.hasNext()) {
			buf.append(setit.next());
			if (setit.hasNext()) {
				buf.append(sep);
			}
		}
		return buf.toString();
	}


	/**
	 *  Converts the input Set to a komma-separated list String
	 *
	 *@param  set  The new ToString value
	 *@return      Description of the Returned Value
	 */
	public static String setToString(Set set) {
		return setToString(set, ",");
	}


	/**
	 *  Gets the Month attribute
	 *
	 *@param  d  Date
	 *@param  l  Locale
	 *@return    The Month value
	 */
	public static String getMonth(Date d, Locale l) {
		if (d == null) {
			return "";
		}
		SimpleDateFormat formatter = new SimpleDateFormat("MMMMMMMMM yyyy", l);
		return formatter.format(d);
	}


	/**
	 *  returns the Year attribute
	 *
	 *@param  d  Date
	 *@param  l  Locale
	 *@return    The Year value
	 */
	public static String getYear(Date d, Locale l) {
		if (d == null) {
			return "";
		}
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy", l);
		return formatter.format(d);
	}


	/**
	 *  parses the Locale String for valid locale date
	 *
	 *@param  s       input string
	 *@param  locale  Locale
	 *@return         The Date value
	 */
	public static Date getDateFromLocaleString(String s, Locale locale) {
		Date date = getDateFromIsoString(s);
		if (date != null) {
			return date;
		}
		DateFormat[] df = {
				DateFormat.getDateInstance(DateFormat.SHORT, locale),
				DateFormat.getDateInstance(DateFormat.MEDIUM, locale),
				};

		for (int i = 0; i < df.length; i++) {
			df[i].setLenient(false);
			try {
				date = df[i].parse(s);
				return date;
			}
			catch (Exception e) {
			}
		}
		return null;
	}


	/**
	 *  parses the input String for valid ISO date
	 *
	 *@param  s  Input string
	 *@return    The Date value
	 */
	public static Date getDateFromIsoString(String s) {
		Date date = null;
		SimpleDateFormat sdf[] = {
				new SimpleDateFormat("yyyy-M-d"),
				new SimpleDateFormat("yy-M-d")
				};
		for (int i = 0; i < sdf.length; i++) {
			sdf[i].setLenient(false);
			try {
				date = sdf[i].parse(s);
				return date;
			}
			catch (Exception e) {
			}
		}
		return null;
	}



	/**
	 *  Parses the input String for valid ISO date and time
	 *
	 *@param  s  iso date String
	 *@return    The DateAndTimeFromIsoString value
	 */
	public static Date getDateAndTimeFromIsoString(String s) {
		if (s.indexOf(" ") < 0) {
			return getDateFromIsoString(s);
		}
		Date date = null;
		SimpleDateFormat sdtf[] = {
				new SimpleDateFormat("yyyy-M-d H:mm:ss"),
				new SimpleDateFormat("yy-M-d H:mm:ss"),
				new SimpleDateFormat("yyyy-M-d H:mm"),
				new SimpleDateFormat("yy-M-d H:mm"),
				new SimpleDateFormat("yyyy-M-d H"),
				new SimpleDateFormat("yy-M-d H")
				};
		for (int i = 0; i < sdtf.length; i++) {
			sdtf[i].setLenient(false);
			try {
				date = sdtf[i].parse(s);
				return date;
			}
			catch (Exception e) {
			}
		}
		return null;
	}


	/**
	 *  converts Date into a String in ISO format
	 *
	 *@param  d  Date
	 *@return    The String value ind ISO format
	 */
	public static String getIsoString(Date d) {
		if (d == null) {
			return "";
		}
		if (d.equals(mysqlNullDate())) {
			return "";
		}
		//System.out.println("ZenoUtilities.getIsoString.d="+d);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String result = sdf.format(d);
		if ((result.indexOf(" 00:00:00") > 0)
				 || (result.indexOf(" 23:59:59") > 0)) {
			result = result.substring(0, result.indexOf(" "));
		}
		else if (result.substring(result.lastIndexOf(":") + 1).equals("00")) {
			result = result.substring(0, result.lastIndexOf(":"));
		}
		if (result.equals("0002-11-30")) {
			result = "";
		}
		return result;
	}


	/**
	 *  Parses the input String for valid locale date and time
	 *
	 *@param  s       Input String
	 *@param  locale  Locale
	 *@return         The Date value
	 */
	public static Date getDateAndTimeFromLocaleString(String s, Locale locale) {
		Date date = getDateAndTimeFromIsoString(s);
		if (date != null) {
			return date;
		}
		DateFormat[] df = {
				DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM, locale),
				DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, locale),
				DateFormat.getDateInstance(DateFormat.SHORT, locale),
				DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, locale),
				DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, locale),
				DateFormat.getDateInstance(DateFormat.MEDIUM, locale),
				};
		for (int i = 0; i < df.length; i++) {
			df[i].setLenient(false);
			try {
				Date d = df[i].parse(s);
				return d;
			}
			catch (Exception e) {
			}
		}
		return null;
	}


	/**
	 *  Computes the DateTime of 0 o'clock of date
	 *
	 *@param  date  Date
	 *@return       DateTime of 0:0:0 o'clock
	 */
	public static Date getDayBegin(Date date) {
		return getDateFromIsoString(getIsoString(date));
	}


	/**
	 *  Computes the DateTime of 23:59:59 o'clock of date
	 *
	 *@param  date  Date
	 *@return       DateTime of 0 o'clock
	 */
	public static Date getDayEnd(Date date) {
		StringTokenizer dg = new StringTokenizer(getIsoString(date), "-");
		String y = dg.nextToken();
		String m = dg.nextToken();
		String d = dg.nextToken();
		if (d.indexOf(" ") > 0) {
			d = d.substring(0, d.indexOf(" "));
		}
		GregorianCalendar cal = new GregorianCalendar((new Integer(y)).intValue(),
				(new Integer(m)).intValue() - 1, (new Integer(d)).intValue());
		cal.set(Calendar.HOUR, 23);
		cal.set(Calendar.MINUTE, 59);
		cal.set(Calendar.SECOND, 59);
		return cal.getTime();
	}


	/**
	 *  Constructs an Iterator containing html option tags. prefix+'.'+ label is
	 *  translated into the language given by trans. The result is used as text
	 *  between the option tag and the end option tag.
	 *
	 *@param  labelsString  Komma separated list of subjects
	 *@param  selected      String: the selected subject
	 *@param  prefix        boolean, forces a traslated visible option
	 *@param  trans         ZenoBundle (language translation table)
	 *@return               Iterator containig html option tags
	 */
	public static Iterator getOptions(String labelsString,
			String selected, String prefix, ZenoBundle trans) {
		List labels = new ArrayList();
		StringTokenizer tok = new StringTokenizer(labelsString, ",");
		while (tok.hasMoreTokens()) {
			String optionVal = tok.nextToken().trim();
			String optionTxt = (trans != null)
					 ? trans.getString(prefix + optionVal) : optionVal;
//System.out.println("ZenoUtilities.getOptions.optionVal="
// + optionVal+", optionTxt="+optionTxt+", selected="+selected+"|");
			String optionElement = "<option"
					 + ((contained(optionVal, selected)) ? " selected" : "")
			//+ ((optionVal.equals(selected)) ? " selected" : "")
					 + ((trans != null) ? " value=\"" + optionVal + "\" " : "")
					 + ">" + optionTxt + "</option>";
			labels.add(optionElement);
		}
		return labels.iterator();
	}


	/**
	 *  Constructs an Iterator containing html option tags
	 *
	 *@param  labelsString  Komma separated list of subjects
	 *@param  selected      String: the selected subject
	 *@return               Iterator containig html option tags
	 */
	public static Iterator getOptions(String labelsString, String selected) {
		return getOptions(labelsString, selected, "", null);
	}


	/**
	 *  Converts the input List to a "sep"-separated list String
	 *
	 *@param  sep  The separator
	 *@param  l    Description of Parameter
	 *@return      "sep"-sparated list String
	 */
	public static String listToString(List l, String sep) {
		StringBuffer buf = new StringBuffer();
		Iterator lit = l.iterator();
		while (lit.hasNext()) {
			buf.append(lit.next());
			if (lit.hasNext()) {
				buf.append(sep);
			}
		}
		return buf.toString();
	}


	/**
	 *  Converts the input List to a komma-separated list String
	 *
	 *@param  l  Description of Parameter
	 *@return    Description of the Returned Value
	 */
	public static String listToString(List l) {
		return listToString(l, ",");
	}


	/**
	 *  Description of the Method
	 *
	 *@return    Description of the Returned Value
	 */
	public static Date mysqlNullDate() {
		return java.sql.Date.valueOf("0000-00-00");
	}


	/**
	 *  converts the "sep" sparated string to a set
	 *
	 *@param  str  "sep" separated String
	 *@param  sep  Separator
	 *@return      Set
	 */
	public static Set stringToSet(String str, String sep) {
		Set set = new HashSet();
		if ((str == null) || (str.equals(""))) {
			return set;
		}
		else {
			StringTokenizer tok = new StringTokenizer(str, sep);
			while (tok.hasMoreTokens()) {
				set.add(tok.nextToken());
			}
			return set;
		}
	}


	/**
	 *  converts the komma sparated string to a set
	 *
	 *@param  str  komma separated String
	 *@return      Set
	 */
	public static Set stringToSet(String str) {
		return stringToSet(str, ",");
	}


	/**
	 *  converts the "sep" sparated string to a List
	 *
	 *@param  str  "sep" separated String
	 *@param  sep  Separator
	 *@return      Set
	 */
	public static List stringToList(String str, String sep) {
		List l = new ArrayList();
		if ((str == null) || (str.equals(""))) {
			return l;
		}
		else {
			StringTokenizer tok = new StringTokenizer(str, sep);
			while (tok.hasMoreTokens()) {
				l.add(tok.nextToken());
			}
			return l;
		}
	}


	/**
	 *  converts the komma sparated string to a list
	 *
	 *@param  str  komma separated String
	 *@return      List
	 */
	public static List stringToList(String str) {
		return stringToList(str, ",");
	}


	/**
	 *  converts the String array to a "sep" sparated string
	 *
	 *@param  sta  String Array
	 *@param  sep  Separator
	 *@return      String
	 */
	public static String stringArrayToString(String[] sta, String sep) {
		if (sta == null || (sta.length == 0)) {
			return "";
		}
		StringBuffer result = new StringBuffer(sta[0]);
		for (int i = 1; i < sta.length; i++) {
			result.append(sep);
			result.append(sta[i]);
		}
		return result.toString();
	}


	/**
	 *  converts the String array to a komma sparated string
	 *
	 *@param  sta  String Array
	 *@return      String
	 */
	public static String stringArrayToString(String[] sta) {
		return stringArrayToString(sta, ",");
	}


	/**
	 *  Converts the String to a String of hex values
	 *
	 *@param  src  String
	 *@return      String containing the hexadecimal representation of src
	 */
	public static String hex(String src) {
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
	 *  Increments a Dat by an int amount of days
	 *
	 *@param  date  Date
	 *@param  days  int, number of days to increment the date (may be negatice)
	 *@return       Incremented Date
	 */
	public static Date plusDays(Date date, int days) {
		DateFormat df = new SimpleDateFormat("dd.MM.yyyy");
		StringTokenizer dg = new StringTokenizer(df.format(date), ".");
		String d = dg.nextToken();
		String m = dg.nextToken();
		String y = dg.nextToken();
		GregorianCalendar cal = new GregorianCalendar((new Integer(y)).intValue(),
				(new Integer(m)).intValue() - 1, (new Integer(d)).intValue());
		cal.add(GregorianCalendar.DATE, days);
		return cal.getTime();
	}


	/**
	 *  Increments the Date by an int amount of seconds
	 *
	 *@param  date     Date
	 *@param  seconds  int, number of secondss to increment the date (may be
	 *      negatice)
	 *@return          Incremented Date
	 */
	public static Date plusSeconds(Date date, int seconds) {
		DateFormat df = new SimpleDateFormat("dd.MM.yyyy");
		StringTokenizer dg = new StringTokenizer(df.format(date), ".");
		String d = dg.nextToken();
		String m = dg.nextToken();
		String y = dg.nextToken();
		GregorianCalendar cal = new GregorianCalendar((new Integer(y)).intValue(),
				(new Integer(m)).intValue() - 1, (new Integer(d)).intValue());
		cal.add(GregorianCalendar.SECOND, seconds);
		return cal.getTime();
	}


	/**
	 *  Computes the Date of the first day of the month
	 *
	 *@param  date  Date
	 *@return       Date of the first day of month
	 */
	public static Date firstOfMonth(Date date) {
		DateFormat df = new SimpleDateFormat("yyyy-MM");
		return firstOfMonth(df.format(date));
	}


	/*
	 * public static Date firstOfMonth(Date date) {
	 * DateFormat df = new SimpleDateFormat("yyyy-MM");
	 * StringTokenizer dg = new StringTokenizer(df.format(date), "-");
	 * String y = dg.nextToken();
	 * String m = dg.nextToken();
	 * GregorianCalendar cal = new GregorianCalendar((new Integer(y)).intValue(),
	 * (new Integer(m)).intValue() - 1, 1);
	 * return cal.getTime();
	 * }
	 */
	/**
	 *  Computes the Date of the first day of the month
	 *
	 *@param  month  Sting (ISO like notation: yyyy-mm)
	 *@return        Date of the first day of month
	 */
	public static Date firstOfMonth(String month) {
		StringTokenizer tok = new StringTokenizer(month, "-");
		String y = tok.nextToken();
		String m = tok.nextToken();
		GregorianCalendar cal = new GregorianCalendar((new Integer(y)).intValue(),
				(new Integer(m)).intValue() - 1, 1);
		return cal.getTime();
	}


	/**
	 *  Computes the Date of the last day of the month
	 *
	 *@param  date  Date
	 *@return       Date of the last day of month
	 */
	public static Date lastOfMonth(Date date) {
		DateFormat df = new SimpleDateFormat("yyyy-MM");
		return lastOfMonth(df.format(date));
	}


	/*
	 * public static Date lastOfMonth(Date date) {
	 * DateFormat df = new SimpleDateFormat("MM.yyyy");
	 * StringTokenizer dg = new StringTokenizer(df.format(date), ".");
	 * String m = dg.nextToken();
	 * String y = dg.nextToken();
	 * GregorianCalendar cal = new GregorianCalendar((new Integer(y)).intValue(),
	 * (new Integer(m)).intValue(), 1);
	 * cal.add(GregorianCalendar.SECOND, -1);
	 * return cal.getTime();
	 * }
	 */
	/**
	 *  Computes the Date of the last day of the month
	 *
	 *@param  month  String (ISO like notation: yyyy-mm)
	 *@return        Date of the last day of month
	 */
	public static Date lastOfMonth(String month) {
		StringTokenizer tok = new StringTokenizer(month, "-");
		String y = tok.nextToken();
		String m = tok.nextToken();
		GregorianCalendar cal = new GregorianCalendar((new Integer(y)).intValue(),
				(new Integer(m)).intValue(), 1);
		cal.add(GregorianCalendar.SECOND, -1);
		return cal.getTime();
	}


	/**
	 *  Computes the Date of the first day of the year
	 *
	 *@param  y       Description of Parameter
	 *@return         Date of the first day of year
	 */
	public static Date firstOfYear(String y) {
		GregorianCalendar cal = new GregorianCalendar((new Integer(y).intValue()), 0, 1);
		return cal.getTime();
	}


	/**
	 *  Computes the Date of the first day of the year
	 *
	 *@param  date  Date
	 *@return       Date of the first day of year
	 */
	public static Date firstOfYear(Date date) {
		DateFormat df = new SimpleDateFormat("yyyy");
		return firstOfYear(df.format(date));
	}


	/*
	 * public static Date firstOfYear(Date date) {
	 * DateFormat df = new SimpleDateFormat("yyyy");
	 * String y = df.format(date);
	 * GregorianCalendar cal = new GregorianCalendar((new Integer(y).intValue()), 0, 1);
	 * return cal.getTime();
	 * }
	 */

	/**
	 *  Computes the date of the last day of the year
	 *
	 *@param  y     Description of Parameter
	 *@return       Date value of the last day of year
	 */
	public static Date lastOfYear(String y) {
		GregorianCalendar cal = new GregorianCalendar((new Integer(y)).intValue() + 1, 0, 1);
		cal.add(Calendar.SECOND, -1);
		return cal.getTime();
	}


	/**
	 *  Computes the date of the last day of the year
	 *
	 *@param  date  Date
	 *@return       Date value of the last day of year
	 */
	public static Date lastOfYear(Date date) {
		DateFormat df = new SimpleDateFormat("yyyy");
		return lastOfYear(df.format(date));
	}


	/*
	 * public static Date lastOfYear(Date date) {
	 * DateFormat df = new SimpleDateFormat("yyyy");
	 * String y = df.format(date);
	 * GregorianCalendar cal = new GregorianCalendar( (new Integer(y)).intValue(),11,31);
	 * GregorianCalendar cal = new GregorianCalendar((new Integer(y)).intValue() + 1, 0, 1);
	 * cal.add(GregorianCalendar.SECOND, -1);
	 * return cal.getTime();
	 * }
	 */

	/**
	 *  Computes the Date of the first day of the week
	 *
	 *@param  date  Date
	 *@return       Date of the first day of week
	 */
	public static Date firstOfWeek(Date date) {
		DateFormat df = new SimpleDateFormat("dd.MM.yyyy.EE", Locale.GERMAN);
		StringTokenizer dg = new StringTokenizer(df.format(date), ".");
		String d = dg.nextToken();
		String m = dg.nextToken();
		String y = dg.nextToken();
		String w = dg.nextToken();
		int diff = (new Integer(weekDays.getProperty(w))).intValue();
		GregorianCalendar cal = new GregorianCalendar((new Integer(y)).intValue(),
				(new Integer(m)).intValue() - 1, (new Integer(d)).intValue() - diff);
		return cal.getTime();
	}


	/**
	 *  Computes the Date of the last day of the week
	 *
	 *@param  date  Date
	 *@return       Date of the last day of week
	 */
	public static Date lastOfWeek(Date date) {
		DateFormat df = new SimpleDateFormat("dd.MM.yyyy.EE", Locale.GERMAN);
		StringTokenizer dg = new StringTokenizer(df.format(date), ".");
		String d = dg.nextToken();
		String m = dg.nextToken();
		String y = dg.nextToken();
		String w = dg.nextToken();
		int diff = (new Integer(weekDays.getProperty(w))).intValue();
		GregorianCalendar cal = new GregorianCalendar((new Integer(y)).intValue(),
				(new Integer(m)).intValue() - 1, (new Integer(d)).intValue() - diff + 7);
		cal.add(Calendar.SECOND, -1);
		return cal.getTime();
	}


	/**
	 *  Send a zeno message
	 *
	 *@param  mailhost       String
	 *@param  to             String email address
	 *@param  from           String[2] email address, email personal
	 *@param  subject        String
	 *@param  replyTo        String[2] email address, email personal
	 *@param  body           String
	 *@exception  Exception  Description of Exception
	 */
	public static void sendNotice(String mailhost, String to, 	String[] from,
			String subject, InternetAddress[] replyTo, String body) throws Exception {
		Properties props = new Properties();
		props.put("mail.host", mailhost);
		Session session = Session.getInstance(props, null);
		Message msg = new MimeMessage(session);
		System.out.println("ZenoUtilities.getOptions.from=("
				 + from[0] + "," + from[1] + ")");
		msg.setFrom(new InternetAddress(from[0], from[1]));
		msg.setRecipients(Message.RecipientType.TO,
				InternetAddress.parse(to, false));
		msg.setSubject(subject);
		if (replyTo != null) {
			msg.setReplyTo(replyTo);
		}
		msg.setText(body);
		Transport.send(msg);
	}


		/**
	 *  Send a zeno message
	 *
	 *@param  mailhost       String
	 *@param  to             String email address
	 *@param  from           String[2] email address, email personal
	 *@param  subject        String
	 *@param  body           String
	 *@exception  Exception  Description of Exception
	 */
	public static void sendNotice(String mailhost, String to, String[] from,
			String subject, String body) throws Exception {
		sendNotice(mailhost, to, from, subject, null, body);
	}


	/**
	 *  Returns true, if "what" is contained in the "sep" separated list string
	 *  "where"
	 *
	 *@param  what   Description of Parameter
	 *@param  where  Description of Parameter
	 *@param  sep    Description of Parameter
	 *@return        Description of the Returned Value
	 */
	public static boolean contained(String what, String where, String sep) {
		if ((what == null) || what.equals("")
				 || (where == null) || where.equals("")
				 || (sep == null) || sep.equals("")) {
			return false;
		}
		if (where.indexOf(sep) < 0) {
			return (what.equals(where));
		}
		//int p = where.indexOf(what);
		//if (p < 0) return false;
		//return ((p == 0) || (where.indexOf(sep) == (p-1)));
		String target = sep + where + sep;
		return (target.indexOf(sep + what + sep) > -1);
	}


	/**
	 *  Returns true, if "what" is contained in the komma separated list string
	 *  "where"
	 *
	 *@param  what   Description of Parameter
	 *@param  where  Description of Parameter
	 *@return        Description of the Returned Value
	 */
	public static boolean contained(String what, String where) {
		return contained(what, where, ",");
	}


	/**
	 *  Invokes the method with the methodName of the Object. The value is
	 *  converted to string and returned. only methods with empty parameter lists
	 *  are used.
	 *
	 *@param  object      The object whose method result is computed
	 *@param  methodName  String, Name of the method
	 *@return             String
	 */
	private static String getMethodResult(Object object, String methodName) {
		Class probe = object.getClass();
		Method method = null;
		Object resultObject = null;
		try {
			method = probe.getMethod(methodName, null);
			resultObject = method.invoke(object, null);
		}
		catch (Exception e) {
			System.out.println(e.toString());
			return e.toString();
		}
		if (resultObject instanceof Date) {
			return getIsoString((Date) resultObject);
		}
		return resultObject.toString();
	}

	static {
		weekDays.put("Mo", "0");
		weekDays.put("Di", "1");
		weekDays.put("Mi", "2");
		weekDays.put("Do", "3");
		weekDays.put("Fr", "4");
		weekDays.put("Sa", "5");
		weekDays.put("So", "6");
	}

}

