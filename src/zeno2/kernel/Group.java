package zeno2.kernel;

import java.util.List;
import java.util.Enumeration;

/**
 * Zeno 2 Group.  A group is a principal which can have members.
 */

public interface Group extends Principal {

	/** Returns the principals which are members of this group. 
	If the recurse parameter is true, members of subgroups
	are included, recursively. If userOnly is true, 
	groups are filtered out of the result. */

	public List getMembers(boolean recurse, boolean usersOnly)
		throws ZenoException;

	/** Returns the users who are members of the group or, recursively, 
	subgroups. This is synonym for getMembers(true, true). */

	public List getUsers() throws ZenoException;
	
	/** Returns the ids of the principals which are members of 
	this principal. If the recurse parameter is true, members of subgroups
	are included, recursively. If userOnly is true, groups are filtered out
	of the result. */

	public Enumeration getMemberIds(boolean recurse, boolean usersOnly)
		throws ZenoException;

	/** Returns the ids of the users who are members of the group or, recursively, 
	subgroups. This is synonym for getMemberIds(true, true). */

	public Enumeration getUserIds() throws ZenoException;


	/** Adds a principal to the group if group and
	principal belong to the same community. 
	Throws a NoPermissionException otherwise.
	Use the save method to make these changes persistent. */

	public void addMember(String id) throws ZenoException;

	/** Deletes a principal from the group. This is a null op 
	if the prinicpal with this id is not a member of the group. 
	Use the save method to make these changes persistent.*/

	public void deleteMember(String id) throws ZenoException;

	/** Checks whether a principal is a member of the group or, if the recurse
	parameter is true, recursively a member of one of its subgroups. */

	public boolean isMember(String id, boolean recurse) throws ZenoException;

	/** Saves changes made to the membership list of the group to 
	the persistent store. Raises an exception if the user does not
	not have permission or if the transaction could not be completed. */

	public void save() throws ZenoException;

}