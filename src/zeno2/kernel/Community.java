package zeno2.kernel;

import java.util.List;
/**
 * Zeno 2 Community.  
 */

public interface Community extends Principal {
	
	public String getAdmissionCriterion() throws ZenoException;
	

	public void setAdmissionCriterion(String criterion) 
		throws ZenoException;
	
	public String getRejectionCriterion() throws ZenoException;
	
	public void setRejectionCriterion(String criterion) 
		throws ZenoException;
	
	/** Returns the number of principals 
	of the specified type in the community */
	
	public int memberCount(String type) throws ZenoException;
			
	/** Returns the principals which belong to the community. 
	If userOnly is true, groups are filtered out of the result. */
	
	public List getMembers(boolean usersOnly)
			throws ZenoException;
	
	/** Checks whether principal pid is a member of the community */
		
	public boolean isMember(String pid) throws ZenoException;		
	
	/** Adds principal pid to the community if pid is a user
	or a collective. Throws a NoPermissionException otherwise. 
	method save is not required to make the change persistent*/
	
	public void addMember(String pid) throws ZenoException;
	
	/** Deletes principal pid from the community if pid is a member
	of the community. This is a null op otherwise.
	Principals not belonging to other communities (like groups)
	are irrecoverably removed from the zeno system. 
	method save is not required to make the change persistent*/
	
	public void removeMember(String pid) throws ZenoException;
	
	/** Returns the administrators of the the community. 
	 * A community has at least one administrator */

	public List getAdmins() throws ZenoException;
	
	/** Nominates uid as administrator of the the community
	adding uid to the community if necessary 
	method save is not required to make the change persistent*/
	
	public void addAdmin(String uid) throws ZenoException;
	
	/** Dismisses uid as administrator of the community
	without removing uid from the community, if uid 
	is not the only administrator of the community. 
	Throws a NoPermissionException otherwise.
	method save is not required to make the change persistent*/
	
	public void removeAdmin(String uid) throws ZenoException;
	
}