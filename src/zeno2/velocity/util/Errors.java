package zeno2.velocity.util;

import java.util.*;
/**
 *  A container for error messages collected during servlet processing
 *
 *@author     viviane wolff
 *@version    2001-10-22
 */
public class Errors {
	private ArrayList errors = new ArrayList();


	/**
	 *  Errors constructor
	 *
	 *@since
	 */
	public Errors() {
		super();
	}


	/**
	 *  Insert the method's description here. Creation date: (13.09.2001 11:24:45)
	 *
	 *@return    String array of errorMessages.
	 *@since
	 */
	public ArrayList getErrors() {
		return errors;
	}


	/**
	 *  checks if errors are present
	 *
	 *@return    boolean.
	 *@since
	 */
	public boolean isEmpty() {
		return errors.isEmpty();
	}


	/**
	 *  checks if errors are present
	 *
	 *@return    boolean.
	 *@since
	 */
	public boolean isNotEmpty() {
		return !errors.isEmpty();
	}

	public String toString() {
		String errStr = "";
		for (int i=0; i<errors.size(); i++) {
			errStr += errors.get(i);
		}
		return errStr;
	}


	/**
	 *@param  error  The feature to be added to the errors string array
	 *@since
	 */
	public void addError(String error) {
		errors.add(error);
	}
}

