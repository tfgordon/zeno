package zeno2.velocity.util;

import java.util.Enumeration;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
//import com.oreilly.servlet.*;

/**
 *  Wrapper for ResourceBundle, which avoids throwing Exceptions while getting
 *  Strings. Each of its getString methods allways returns a String.
 *
 *@author     oppor
 *@version    2001-09-19
 */
public class ZenoBundle {

	private ResourceBundle bundle = null;


	/**
	 *@param  bundleName
	 *@param  bundle
	 */

	public ZenoBundle(String bundleName, ResourceBundle bundle) {
		try {
			this.bundle = (bundle != null) ? bundle :
					ResourceBundle.getBundle(bundleName, Locale.getDefault());
		}
		catch (Exception e) {
			System.out.println("ZenoBundle.exception:" + e.toString());
		}

	}


	/**
	 *  Gets an enumeration of the keys of the ZenoBundle object. 
	 *  key
	 *
	 *@return    enumeration of keys
	 */
	public Enumeration getKeys() {
		return bundle.getKeys();
	}


	/**
	 *  Gets the Msg value of the ZenoBundle object. If the value is null (key does
	 *  not exist), a http error message is returned:  "missing: "+key
	 *
	 *@param  s  property key
	 *@return    The Msg value or a http error message
	 */
	public String getString(String s) {
		String result = null;
		try {
			result = bundle.getString(s);
		}
		catch (MissingResourceException e) {
			result = "<font color=\"coral\">missing:" + s +"</font>";
		}
		return result;
	}


	/**
	 *  Gets the Msg value of the ZenoBundle object. If the value is null (key does
	 *  not exist), the default value is returned.
	 *
	 *@param  s  property key
	 *@param  d  default property value
	 *@return    the value or the default value
	 */
	public String getString(String s, String d) {
		String result = null;
		try {
			result = bundle.getString(s);
		}
		catch (MissingResourceException e) {
			result = d;
		}
		return result;
	}

}

