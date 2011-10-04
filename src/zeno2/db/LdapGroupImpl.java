package zeno2.db;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import javax.naming.directory.Attributes;
import zeno2.kernel.Group;
import zeno2.kernel.ZenoException;

public class LdapGroupImpl extends LdapPrincipalImpl implements Group {
	List membersToAdd = new ArrayList();
	List membersToDelete = new ArrayList();

	public LdapGroupImpl(String id, PrincipalFactory factory, Attributes attrs) {
		super(id, factory, attrs);
	}

	public List getMembers(boolean recurse, boolean usersOnly)
		throws ZenoException {
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

	public void addMember(String id) {
		membersToAdd.add(id);
	}

	public void deleteMember(String id) {
		membersToDelete.add(id);
	}

	public boolean isMember(String id, boolean recurse) throws ZenoException {
		if (recurse)
			return factory.isIndirectMember(this.id, id);
		else
			return factory.isDirectMember(this.id, id);
	}

	public void save() throws ZenoException {
		if (membersToAdd.size() > 0)
			factory.addMembers(this.id, membersToAdd);
		if (membersToDelete.size() > 0)
			factory.removeMembers(this.id, membersToDelete);
	}

}