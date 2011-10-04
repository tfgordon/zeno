package zeno2.db;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;
import zeno2.kernel.Community;
import zeno2.kernel.ZenoException;
import zeno2.kernel.NoPermissionException;

public class DBCommunityImpl extends DBPrincipalImpl 
										implements Community {

	public DBCommunityImpl(String id, PrincipalFactory factory) {
		super(id, factory);
	}
	
	public String getAdmissionCriterion() throws ZenoException {
		String criterion = 
			factory.getProperty(this.id, "system", "admission");
		if (criterion == null)
			criterion = "";
		return criterion;
	}
	
	public void setAdmissionCriterion(String criterion) throws ZenoException {
		factory.setProperty(this.id, "system", "admission", criterion);
	}
	
	public String getRejectionCriterion() throws ZenoException {
		String criterion = 
			factory.getProperty(this.id, "system", "rejection");
		if (criterion == null)
			criterion = "";
		return criterion;
	}
	
	public void setRejectionCriterion(String criterion) throws ZenoException {
		factory.setProperty(this.id, "system", "rejection", criterion);
	}
	
	public int memberCount(String type)
			throws ZenoException {
		return factory.communityMemberCount(this.id, type);
	}			
	
	
	public List getMembers(boolean usersOnly)
			throws ZenoException {
		factory.checkPermission("Community.getMembers", this);
		String type = usersOnly ? "person" : "";
		List memberIds = factory.getCommunityMembers(this.id, type);
		return factory.loadPrincipals(memberIds);
	}
	
	public List getUsers() throws ZenoException {
		return getMembers(true);
	}
	
	public boolean isMember(String uid) throws ZenoException {
		return factory.isCommunityMember(this.id, uid);	
	}
	
	
	public void addMember(String uid) throws ZenoException {
		factory.checkPermission("Community.addMember", this);
		String type = factory.getPrincipalType(uid);
		if (! "person".equals(type) && ! "collective".equals(type))
			throw new NoPermissionException("noUser");
		else
			factory.addToCommunity(this.id, uid, false);
	}
	

	public void removeMember(String uid) throws ZenoException {
		factory.checkPermission("Community.removeMember", this);
		factory.removePrincipal(this.id, uid);
	}
		
	
	public List getAdmins() throws ZenoException {
		factory.checkPermission("Community.getAdmins", this);
		List adminIds = factory.getAdmins(this.id);
		return factory.loadPrincipals(adminIds);
		}
	
	
	public void addAdmin(String uid) throws ZenoException {			
		factory.checkPermission("Community.addAdmin", this);
		String type = factory.getPrincipalType(uid);
		if (! "person".equals(type))
			return;
		else if (isMember(uid))
			factory.setAdmin(this.id, uid, true);
		else 
			factory.addToCommunity(this.id, uid, true);
	}
	
	
	public void removeAdmin(String uid) throws ZenoException {
			
		factory.checkPermission("Community.removeAdmin", this);
		if (factory.hasOtherAdmins(this.id, uid))
			factory.setAdmin(this.id, uid, false);
		else
			throw new NoPermissionException("lastAdmin");
	}
	
	
	
	

}

