package zeno2.kernel;

import java.util.Date;
import java.util.Enumeration;
import java.util.List;

/**
 * Zeno 2 Principal.  A principal is a user or group of users.
 */

public interface Principal {

	/** Gets the id of the principal.  */

	public String getId();

	/** Checks whether the given password is correct. The password
	given here should be in plain text, not encoded. */
	public boolean checkPassword(String password) throws ZenoException;

	/** Changes the password. The change is made persistent immediately,
	without having to execute the save method. */

	public void setPassword(String newPassword) throws ZenoException;

	/** Returns the common name of the principal. */

	public String getName() throws ZenoException;

	/** Changes the common name of the principal. */

	public void setName(String name) throws ZenoException;

	/** Gets the email address of the principal. */

	public String getEmail() throws ZenoException;

	/** Changes the email address of the principal. */

	public void setEmail(String email) throws ZenoException;

	/** Gets the organization of the principal 
	Obsolete for user principals */

	public String getOrganization() throws ZenoException;

	/** Modifies the organization of the principal. 
	Obsolete for user principals */

	public void setOrganization(String organization) throws ZenoException;

	/** Gets the role of this principal in the organization. 
	Obsolete for user principals */

	public String getOrgRole() throws ZenoException;

	/** Set the role of this principal in the organization .
	Obsolete for user principals */

	public void setOrgRole(String title) throws ZenoException;

	/** Gets the plain text description of this principal. 
	Obsolete for user principals */

	public String getDescription() throws ZenoException;

	/** Sets the plain text description of this principal.
	Obsolete for user principals */

	public void setDescription(String description) throws ZenoException;
	
	/** Gets the creator of this principal. */

	public String getCreator() throws ZenoException;
	
	/** Gets the creation date of this principal. */

	public Date getCreationDate() throws ZenoException;


	/** Saves all changes made to the principal back to the 
	persistent store.  Raises an exception if the user does not
	have permission or if some the transaction could not be
	completed. */

	public void save() throws ZenoException;
	
	/** Returns the names of community specific properties 
	which have a value. */
	
	public List getPropertyNamesUsed(String community) 
		throws ZenoException;
	
	/** Gets the user-defined community specific property with the specified name. 
	Returns null if there is no such property*/

	public String getProperty(String community, String propertyName) 
		throws ZenoException;

	/** Creates or updates the user-defined community specific property
	with the given name. This change is made persistent, without having
	to call the save method. */

	public void setProperty(String community, 
								String propertyName, String propertyValue)
		throws ZenoException;

	/** Removes the user-defined community specific property 
	with the specified name.*/

	public void removeProperty(String community,String propertyName) 
		throws ZenoException;


	/** Gets the user-defined property for community any with the specified name. 
	Returns null if there is no such property. Obsolete */

	public String getProperty(String propertyName) throws ZenoException;

	/** Creates or updates the property with the user-defined property
	with the given name for community any. This change is made persistent, 
	without having to call the save method. . Obsolete */

	public void setProperty(String propertyName, String propertyValue)
		throws ZenoException;

	/** Removes the user-defined property with the specified name
	for community any. Obsolete */

	public void removeProperty(String propertyName) throws ZenoException;
	
	/** Returns the communities to which the principal belongs
	and which are accessible by the user */

	public List getCommunities() throws ZenoException;

	/** Returns the groups where the principal is an direct 
	 * or undirect member and which are accessible by the user */

	public List getGroups(boolean directOnly) throws ZenoException;
	
	/** Leaves the specified community; throws a NoPermissionException
	 if the principal is the last administrator of the community
	 Principals not belonging to other communities are irrecoverably removed 
	 from the zeno system */ 
	
	public void leaveCommunity(String community) throws ZenoException;
	
	/** Leaves the specified group */ 
	 
	public void leaveGroup(String group) throws ZenoException;
	

}