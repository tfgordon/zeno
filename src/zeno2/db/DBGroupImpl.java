package zeno2.db;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import zeno2.kernel.Group;
import zeno2.kernel.ZenoException;
import zeno2.kernel.NoPermissionException;

public class DBGroupImpl extends DBPrincipalImpl implements Group {
	List membersToAdd = new ArrayList();
	List membersToDelete = new ArrayList();

	public DBGroupImpl(String id, PrincipalFactory factory) {
		super(id, factory);
	}

	public List getMembers(boolean recurse, boolean usersOnly)
			throws ZenoException {
		factory.checkPermission("Group.getMembers", this);	
		Set memberIds;
		if (recurse) {
			memberIds = factory.getAllMembers(this.id, usersOnly);
		} else {
			memberIds = factory.getDirectMembers(this.id, usersOnly);
		}
		List members = factory.loadPrincipals(memberIds);
		return members;
	}

	public List getUsers() throws ZenoException {
		return getMembers(true, true);
	}
	
	public Enumeration getMemberIds(boolean recurse, boolean usersOnly)
		throws ZenoException {
		Set members;
		if (recurse) {
			members = factory.getAllMembers(this.id, usersOnly);
		} else {
			members = factory.getDirectMembers(this.id, usersOnly);
		}
		Vector memberVector = new Vector(members);
		return memberVector.elements();
	}

	public Enumeration getUserIds() throws ZenoException {
		return getMemberIds(true, true);
	}

	public void addMember(String id) throws ZenoException {
		factory.checkPermission("Group.addMember", this);
		if (factory.areCoMembers(this.id, id))
			membersToAdd.add(id);
		else 
			throw new NoPermissionException("differentCommunities");
	}

	public void deleteMember(String id) throws ZenoException {
		factory.checkPermission("Group.addMember", this);
		membersToDelete.add(id);
	}

	public boolean isMember(String id, boolean recurse) throws ZenoException {
		if (recurse)
			return factory.isIndirectMember(this.id, id);
		else
			return factory.isDirectMember(this.id, id);
	}

	public void save() throws ZenoException {
		List members;
		if (membersToAdd.size() > 0) {
			members = membersToAdd;
			membersToAdd = new ArrayList();
			factory.addMembers(this.id, members);
		} 
		if (membersToDelete.size() > 0) {
			members = membersToDelete;
			membersToDelete = new ArrayList();
			factory.removeMembers(this.id, members);
		}
		super.save();
	}

}