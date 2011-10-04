package zeno2.util;

/**
 * <strong>TransVCALDate</strong> translates dates to/from vcard (ISO8601) format.
 * 
 * provides parseDate( String) and printDate( Date ) static methods.
 * using the following ISO8601 format: yyyyMMdd'T'HHmmss
 *  
 * @author Juergen Walther
 * @version $Revision: 1.0 $ $Date: 2001/05/15 01:14:37 $
 */

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.Logger;

public class TransVCALDate {
	static Logger log = Logger.getLogger(TransVCALDate.class.getName());
	private static SimpleDateFormat df =
		new SimpleDateFormat("yyyyMMdd'T'HHmmss");

	public static Date parseDate(String d) {
		try {
			log.debug( "parsing date string: " + d );
			Date date = df.parse(d);
			log.debug( "returning date: " + date );
			return date;
		} catch (java.text.ParseException pe) {
			//System.out.print(pe);
			log.error( "error parsing date string: " + d + ". Exception: " + pe );
			return new Date();
		}
	}

	public static String printDate(Date d) {
		log.debug( "printing date: " + d );
		String str = df.format(d);
		log.debug( "returning date string: " + str );
		return str;
	}

	public static void main(String[] args) {
		try {
			TransVCALDate d = new TransVCALDate();
			String current_date_str = d.printDate(new Date());
			//System.out.println("printDate of current date: " + current_date_str);
			log.debug("printDate of current date: " + current_date_str);
			Date current_date = d.parseDate(current_date_str);
			//System.out.println("parseDate of printDate of current date: " + current_date);
			log.debug("parseDate of printDate of current date: " + current_date);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}