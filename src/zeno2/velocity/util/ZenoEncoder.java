package zeno2.velocity.util;

import java.net.URLEncoder;

/**
 *  URLEncoder object for use in velocity templates
 *
 *@author     oppor
 *@version    2001-11-07
 */
public class ZenoEncoder {

	/**
	 *  returns the input string URLEncoded
	 *
	 *@param  s  source string
	 *@return    URLEncoder.encode(s)
	 *@since
	 */
	static public String encode(String s) {
		//return URLEncoder.encode(s,"x-www-form-urlencoded");*** 1.4
		return URLEncoder.encode(s);
	}
}

