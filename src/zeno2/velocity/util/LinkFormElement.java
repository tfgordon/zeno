package zeno2.velocity.util;

import java.util.*;
import zeno2.kernel.*;

/**
 *  Alternative Zeno 2 Link Implementation for usage in Velocity Templates. This
 *  is necessary, because there is no zeno kernel method to create an empty link
 *  object due to the representaion of link objects in the link database table.
 *  <br>
 *  A link database entry has a unique key consisting of three table colums:
 *  <br>
 *  1. source <br>
 *  2. target <br>
 *  3. label <br>
 *  We handle "adding a new link" (as with journals and articles) as "updating
 *  an (mostly) empty link. Therefore an empty link object is needed for the
 *  Velocity Template, which produces the editLinkForm.
 *
 *@author     oppor
 *@version    2001-09-24
 */

public class LinkFormElement implements Link {
	Factory factory = null;
	String label = " ";
	String sourceAlias = "";
	String targetAlias = "";
	int source;
	int target = 0;


	/**
	 *  Constructor for the link object
	 *
	 *@param  source       int
	 *@param  sourceAlias  of the link
	 *@param  factory      Description of Parameter
	 *@param  target       int
	 */
	public LinkFormElement(Factory factory, int source, 
			int target, String sourceAlias) {
		this.factory = factory;
		this.source = source;
		this.target = target;
		this.sourceAlias = sourceAlias;
	}

	/**
	 *  Constructor for the link object
	 *
	 *@param  source       int
	 *@param  sourceAlias  of the link
	 *@param  factory      Description of Parameter
	 */
	public LinkFormElement(Factory factory, int source, String sourceAlias) {
		this(factory,source,0,sourceAlias);
	}


	/**
	 *  Change the source alias.
	 *
	 *@param  sourceAlias  The new SourceAlias value
	 */
	public void setSourceAlias(String sourceAlias) {
		this.sourceAlias = sourceAlias;
	}


	/**
	 *  Change the target alias.
	 *
	 *@param  targetAlias  The new TargetAlias value
	 */
	public void setTargetAlias(String targetAlias) {
		this.targetAlias = targetAlias;
	}


	/**
	 *  Get the label of the link.
	 *
	 *@return    The Label value
	 */

	public String getLabel() {
		return label;
	}


	/**
	 *  Get the source alias of the link.
	 *
	 *@return    The SourceAlias value
	 */
	public String getSourceAlias() {
		return sourceAlias;
	}

	//heg
	public String getUserSourceAlias() {
		return sourceAlias;
	}
	
	/**
	 *  Get the target alias of the link.
	 *
	 *@return    The TargetAlias value
	 */
	public String getTargetAlias() {
		return targetAlias;
	}

	public String getUserTargetAlias() {
		return targetAlias;
	}
	/**
	 *  Get the source resource of the link.
	 *
	 *@return                    The Source value
	 *@exception  ZenoException  Description of Exception
	 */

	public ZenoResource getSource() throws ZenoException {
		return factory.loadResource(source);
	}


	/**
	 *  Gets the SourceId attribute of the LinkFormElement object
	 *
	 *@return    The SourceId value
	 */
	public int getSourceId() {
		return source;
	}


	/**
	 *  Get the target resource of the link.
	 *
	 *@return                    The Target value
	 *@exception  ZenoException  Description of Exception
	 */

	public ZenoResource getTarget() throws ZenoException {
		return factory.loadResource(target);
	}


	/**
	 *  Gets the TargetId attribute of the LinkFormElement object
	 *
	 *@return    The TargetId value
	 */
	public int getTargetId() {
		return target;
	}


	/**
	 *  Save changes. Raises an exception if the user has no permission or the
	 *  transaction could not be completed.
	 *
	 *@exception  ZenoException  Description of Exception
	 */

	public void save() throws ZenoException {
		throw new ZenoException("LnkFormElement is not savable");
	}



	/**
	 *  overwrites the toString method of java.lang.object
	 *
	 *@return		a string containing a square brackets embraced list of the
	 *					properties: link,label,source,sourceAlias,target.
	 */
	public String toString() {
		return "[Link " + label + " " + source + " " + sourceAlias + "  " + target + "]";
	}

}

