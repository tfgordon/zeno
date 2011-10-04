package zeno2.velocity.util;

import java.net.URLDecoder;

/**
 *  URLDecoder object for use in velocity templates
 *
 *@author     oppor
 *@version    2002-12-05
 */
public class ZenoDecoder {

	/**
	 *  returns the input string URLDecoded
	 *
	 *@param  s  source string
	 *@return    URLEncoder.decode(s)
	 *@since
	 */
	static public String decode(String s) {
		//return URLDecoder.decode(s,"x-www-form-urldecoded");*** 1.4
		return URLDecoder.decode(s);
	}
}

