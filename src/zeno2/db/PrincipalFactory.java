package zeno2.db;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.naming.directory.Attributes;
import zeno2.kernel.Community;
import zeno2.kernel.Group;
import zeno2.kernel.Principal;
import zeno2.kernel.ZenoException;

public interface PrincipalFactory {
	
	public Principal loadPrincipal(String uid) throws ZenoException;

	public Iterator searchPrincipals(String community, 
										String query, String type) 
		throws ZenoException;
	
	public List getCommunitiesForRegistration(boolean isMember) throws ZenoException;
		
	public void register(String community) throws ZenoException;
		
	public Principal registerAs(String community, 
								String uid, String name, String password)
		throws ZenoException;
		
	public Community createCommunity(String cid, String name)
		throws ZenoException;

	public Principal createUser(String community, 
								String uid, String name, String email)
		throws ZenoException;
		
	public Principal createCollective(String community, 
								String uid, String name, String password)
		throws ZenoException;
		
	public Group createGroup(String community, String uid, String name) 
		throws ZenoException;

	public void removeCommunity(String uid) throws ZenoException;
	
	public void removePrincipal(String community, String uid) 
		throws ZenoException;

	
	public String getProperty(String id, String community, String propertyName) 
		throws ZenoException;

	public void setProperty(String id, String community, String propertyName, String propertyValue)
		throws ZenoException;

	public void removeProperty(String id, String community, String propertyName)
		throws ZenoException;

	public void addMembers(String id, List list) throws ZenoException;

	public void removeMembers(String id, List list) throws ZenoException;

	public boolean checkClientInfo(String name, String clientInfo)
		throws ZenoException;
	
	public boolean checkPassword(String name, String password)
		throws ZenoException;

	public void setPassword(String id, String oldPassword, String newPassword)
		throws ZenoException;

	public void setPassword(String id, String newPassword) throws ZenoException;

	public Set getDirectMembers(String id, boolean personsOnly)
		throws ZenoException;

	public Set getAllMembers(String rdn, boolean personsOnly) throws ZenoException;

	public boolean isDirectMember(String parentdn, String childdn)
		throws ZenoException;

	public boolean isIndirectMember(String parentdn, String childdn)
		throws ZenoException;

	public boolean hasRole(String role, Object obj) throws ZenoException;

}