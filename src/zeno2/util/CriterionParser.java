package zeno2.util;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
/**
 *  Description of the Class
 *
 *@author     gross
 *@created    Juni 27, 2002
 */
public class CriterionParser {
	
	public static boolean checkBrackets(String input) {
		int ob = 0;
		int cb = 0;
		for(int i=0; i < input.length(); i++) {
			char c = input.charAt(i);
			if (c == '(')
				ob++;
			else if (c == ')')
				cb++;
		}
		return ob == cb;
	}

	public static List getSubCriteria(String criterion) {
		//returns an empty list for a simple criterion
		List subCriteria = new ArrayList();
		if (criterion.startsWith("(and") ||
				criterion.startsWith("(or")) {
			int to = criterion.length();
			int cfrom = 1;
			int cto = 1;
			while(true) {
				cfrom = getSubCriterionStart(criterion, cto, to);
				if (cfrom == -1)
					break;
				else {
					cto = getSubCriterionEnd(criterion, cfrom, to);
					if (cto != -1) 
						subCriteria.add(criterion.substring(cfrom, cto));
				}
			}
		} 
		return subCriteria;
	}
	
	protected static int getSubCriterionStart(String input, int from, int to) {
		int cfrom = input.indexOf("(", from);
		if (cfrom ==  -1 || cfrom > to)
			return -1;
		else
			return cfrom;
	}
	
	protected static int getSubCriterionEnd(String input, int from, int to) {
		//return index behind subcriterion
		int clength = 0;
		int count = 0;
		for(int i= from; i < to; i++) {
			char c = input.charAt(i);
			if (c == '(') {
				count++;
			} else if (c == ')') {
				count--;
				if (count == 0)
					return i+1;
			}
		}
		return -1;
	}
	
	public static Hashtable parseValues(String values) {
		Hashtable result = new Hashtable();
		StringTokenizer tok = new StringTokenizer(values, "()=, ");
		while (tok.hasMoreTokens()) {
			String name = tok.nextToken().toLowerCase();
			String value = tok.nextToken();
			result.put(name,value);
		}
		return result;
	}
	
	public static boolean matchCriterion(String criterion, String values) {
		
		//System.out.println("matchCriterion " + criterion + " againt " + values);
		Hashtable table = parseValues(values);
		return matchCriterion(criterion, table);
	}
	
	public static boolean matchCriterion(String criterion, Hashtable values) {
	
		String operation = "match";
		if (criterion.startsWith("(and")) 
			operation = "and";
		else if 	(criterion.startsWith("(or")) 
			operation = "or";
		if (operation.equals("match")) 
			return matchSimpleCriterion(criterion, values);
		else {
			//Iterator it = getSubCriteria(criterion).iterator();
			List criteria = CriterionParser.getSubCriteria(criterion);
			Iterator it = criteria.iterator();
			while(it.hasNext()) {
				String subcriterion = (String)it.next();
				boolean partresult = matchCriterion(subcriterion, values);
				if (operation.equals("and") && ! partresult)
						return false;
				if (operation.equals("or") && partresult)
						return true;
			}
			if (operation.equals("and"))
				return true;
			else 
				return false;
		}
	}
	
	public static boolean matchSimpleCriterion(String criterion, Hashtable values) {
		//criterion (type = pattern) or type = pattern
		StringTokenizer ctok = new StringTokenizer(criterion, "()= ");
		String ctype = ctok.nextToken().toLowerCase();
		String pattern = ctok.nextToken();
		String value = (String)values.get(ctype);
		if (value == null)
			return false;
		else
			return matchPattern(pattern, value);
	}
	
	
	public static boolean matchPattern(String pattern, String value) {
		return matchPattern(pattern, 0, pattern.length(), value, 0, value.length());
	}
	
	public static boolean matchPattern(String pattern, int pfrom, int pto, 
											String value, int vfrom, int vto) {
	
		int index = pattern.indexOf("%", pfrom);
		int length = 0;
		if (index == -1 || index >= pto) {
			length = pto - pfrom;
			if (length != vto - vfrom)
				return false;
			else
				return value.regionMatches(vfrom, pattern, pfrom, length);
		} else  if (index > pfrom) {
			length = index - pfrom;
			if (!value.regionMatches(vfrom, pattern, pfrom, length))
				return false;
			else
				return matchPattern(pattern, index, pto, value, vfrom + length, vto);
		} else {
			int index2 = pattern.indexOf("%", index+1);
			if (index2 == -1 || index2 >= pto) {
				length = pto-index-1;
				return value.regionMatches(vto - length , pattern, index+1, length);
			} else {
				String subpattern = pattern.substring(index+1,index2);
				length = subpattern.length();
				int index3 = value.indexOf(subpattern,vfrom);
				int vfrom2 = index3 + length;
				if (index3 == -1 || vfrom2 >=vto)
					return false;
				else 
					return matchPattern(pattern, index2, pto, value, vfrom2, vto);
			}
		}
	}
	
	/*
	protected static String transformPattern(String pattern) {
		StringBuffer buf = new StringBuffer();
		 for(int i =0; i<pattern.length(); i++) {
	 		char c = pattern.charAt(i);
	 		if (c == '.') 
	 			buf.append("\\\\.");
			else if (c == '%')
				buf.append(".+");
			else
			buf.append(c);
		}
		return buf.toString();
	}
	
	
	protected staitc boolean matchPattern(String pattern, String value) {
		try {
			RE regexp = new RE(transformPattern(pattern));
			return regexp.isMatch(value);
		} catch(REException e) {
			System.out.println(e);
			return false;
		}
	}
	*/
	


}

