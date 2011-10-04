
package zeno2.db;

import java.util.*;
import java.sql.ResultSet;
import javax.naming.*;
import javax.naming.directory.*;
import zeno2.kernel.*;

public class DefaultChecker implements PermissionChecker {
	static boolean[] none = {false, false, false, true};
	String username;	
	Hashtable objPatterns = new Hashtable();
	MonitorImpl monitor;
	DBClient dbclient;
	//requires root permission
	//LdapClient ldapclient;
	PrincipalFactory prfactory;
	

	// 0 = readers;1 = writers; 2 = editors;  3 = locked
	
	
	public DefaultChecker(MonitorImpl monitor, String username) {
		this.monitor = monitor;
		this.dbclient = monitor.getDBClient();
		//this.ldapclient = monitor.getLdapClient();
		this.prfactory = monitor.getPrincipalFactory();
		this.username = username;
	}
	
	public String getCurrentRole(int jnid)
			throws ZenoException {
		Integer journalId = new Integer(jnid);
		boolean[] pattern = getObjPattern(journalId);
		if (pattern[2])
			return "editor";
		if (pattern[0] && pattern[1])
			return "writer/reader";
		if (pattern[1])
			return "writer";
		if (pattern[0])
			return "reader";
		return ""; 
	}
	
	public List getValidRoles(int jnid) throws ZenoException {
		boolean[] pattern = getUserRoles(jnid);
		List roles = new ArrayList();
		if (pattern[2]) { 
			roles.add("editor");
			roles.add("writer/reader");
			roles.add("writer");
			roles.add("reader");
			return roles;
		} if (pattern[1] && pattern[0]) {
			roles.add("writer/reader");
			roles.add("writer");
			roles.add("reader");
			return roles;
		} if (pattern[1]) {
			roles.add("writer");
			return roles;
		} if (pattern[0])  
			roles.add("reader");
		return roles;
	}
	
	public void setCurrentRole(int jnid, String role) 
			throws ZenoException {
		Integer journalId = new Integer(jnid);
		boolean[] pattern = getObjPattern(journalId);
		if ("editor".equals(role))
			pattern[2] = true;
		else if ("writer/reader".equals(role)) {
		System.out.println("now a writer/reader");
			pattern[0] = true;
			pattern[1] = true;
			pattern[2] = false;
		} else if ("writer".equals(role)) {
		System.out.println("now a writer");
			pattern[0] = false;
			pattern[1] = true;	
			pattern[2] = false;
		} if ("reader".equals(role)) {
		System.out.println("now a reader");
			pattern[0] = true;
			pattern[1] = false;
			pattern[2] = false;
		}
		System.out.println(pattern[0] + "  " + pattern[1] + "  " +pattern[2]);	
	}
	
	
	
	protected  List[]  loadAllRoleDefinitions (int journalId) 
			throws ZenoException {
			
		List readers = new ArrayList();
		List writers = new ArrayList();
		List editors = new ArrayList();	
		try {	
			String query = "select * from role where journal =" + journalId;
			ResultSet rs = dbclient.executeQuery(query);
			while(rs.next()) {
				String role = rs.getString("role_name");
				if ("reader".equals(role))
					readers.add(rs.getString("principal"));
				else if ("writer".equals(role))
					writers.add(rs.getString("principal"));
				else if ("editor".equals(role))
					editors.add(rs.getString("principal"));	
				else
					//ignored other roles
					;
			}
		} catch(java.sql.SQLException e) {
			monitor.reportError("DefaultChecker.loadAllRoleDefinitions"  + e);
			throw new ZenoException("DBException");
		}
		List[] result = new List[3];
		result[0] = readers;
		result[1] = writers;
		result[2] = editors;
		return result;
	}
	
	protected  boolean hasRole(List roleDefinition, String principal) 
			throws ZenoException {
		if (roleDefinition.contains("any"))
			//exclude guest???
			return true;	
		if (roleDefinition.contains(principal))
			return true;
		Iterator it = roleDefinition.iterator();
		while(it.hasNext()) {
			String cprincipal = (String)it.next();
			if (prfactory.isIndirectMember(cprincipal, principal))
				return true;
		}
		return false;
	}
	
	protected boolean isClosed(int collectionId)
			throws ZenoException {
		// column locked not yest renamed
		String request ="select locked from resource" +
									" where id = " + DBClient.format(collectionId); 
		Object[] result = dbclient.getRowArray(request);
		if (result != null) {
			String closed = (String)result[0];
			return closed.equals("true");
		} else
			return true;
	}
	
	
	protected  boolean[] getUserRoles(int journalId) 
			throws ZenoException {
		boolean[] userRoles = new boolean[4];
		List[] roleDefinitions = loadAllRoleDefinitions(journalId);
		for (int i=0; i<roleDefinitions.length; i++) {
			userRoles[i] = hasRole(roleDefinitions[i],username);
		}
		userRoles[3] = isClosed(journalId);
		return userRoles;
	}
	
	
	
	public boolean[] getObjPattern(Integer id) throws ZenoException {
		if (id == null)
			return none;
		else {
			boolean[] pattern = (boolean[])objPatterns.get(id);
			if (pattern == null) {
				pattern = getUserRoles(id.intValue());
				objPatterns.put(id, pattern);
			}
			return pattern;
		}
	}
	
	public void removeObjPattern(Object obj) {
		Integer id = getJournalId(obj);
		if (id != null)
			objPatterns.remove(id);
	}
	
	public Integer getJournalId(Object obj) {
		Integer journalId = null;
		if (obj instanceof Integer)
			journalId = (Integer)obj;
		else if (obj instanceof Journal) {
			int jid = ((JournalImpl)obj).id;
			journalId = new Integer(jid);
		} else if  (obj instanceof Article) {
			int jid = ((ArticleImpl)obj).parentId;
			journalId = new Integer(jid);
		} else if (obj instanceof Attachment) {
			int articleId = ((Attachment)obj).getArticleId();
			String request = "select parent from resource" +
								  " where id = " + DBClient.format(articleId);
			Object[] result = dbclient.getRowArray(request);
			if (result != null)
				journalId = (Integer)result[0];
		}	else if (obj instanceof Link) {
			int sourceId = ((LinkImpl)obj).source;
			String request ="select class, parent from resource" +
									" where id = " + DBClient.format(sourceId); 
			Object[] result = dbclient.getRowArray(request);
			if (result != null) {
				String zenoClass = (String)result[0];
				if (zenoClass.equals("article"))
					journalId = (Integer)result[1];
				else
					journalId = new Integer(sourceId);
			}
		}
		return journalId;
	}
	
	
	public boolean hasRole(String role, Object object)
			throws ZenoException {
	
		if (role.equals("reader"))
			return isReader(object);
		else if (role.equals("writer"))
			return isWriter(object);
		else if (role.equals("editor")) {
			//removeObjPattern(object);
			return isEditor(object);
		} else if (role.equals("creator"))
			return isCreator(object);
		else if (role.equals("any"))
			return isReader(object) || isWriter(object);	
		else
			return false;
	}
	
	/*	
	public boolean isReader(Object obj) throws ZenoException {
		Integer journalId = getJournalId(obj);
		boolean[] pattern = getObjPattern(journalId);
		return (pattern[0] | pattern[2]);
	}
	*/
	
	public boolean isReader(Object obj) throws ZenoException {
		Integer journalId = getJournalId(obj);
		boolean[] pattern = getObjPattern(journalId);
		if (pattern[2])
			return true;
		else if (obj instanceof ResourceImpl && ((ResourceImpl)obj).markedForDeletion) 
			return false;
		else if (obj instanceof ArticleImpl && !((ArticleImpl)obj).published) 
			return false;
		else
			return pattern[0];
	}
	
	public boolean isWriter(Object obj) throws ZenoException {
		Integer journalId = getJournalId(obj);
		boolean[] pattern = getObjPattern(journalId);
		if (pattern[2])
			return true;
		else if (pattern[3])
			//journal closed
			return false;
		else if (obj instanceof Topic && ((TopicImpl)obj).closed) 
				return false;
		else if (obj instanceof Article && hasClosedTopic(obj)) 
			return false;
		else
			return (pattern[1]);
	}
	
	public boolean hasClosedTopic(Object obj) throws  ZenoException {
		int part = ((ResourceImpl)obj).part;
		if (part == 0)
			return false;
		String request = "select locked from resource where id =" +
					 			DBClient.format(part);
		try {
			ResultSet rs = dbclient.executeQuery(request);
			if (rs.next())
				return "true".equals(rs.getString("locked"));
			else
				return false;
		} catch(java.sql.SQLException e) {
			monitor.reportError("DefaultChecker.hasClosedTopic"  + e);
			throw new ZenoException("DBException");
		}
	}
		
	public boolean isEditor(Object obj) throws ZenoException {
		Integer journalId = getJournalId(obj);
		boolean[] pattern = getObjPattern(journalId);
		return pattern[2];
	}
	
	public boolean isParentReader(Object obj) throws ZenoException {
	// for getTitle
		if (obj instanceof JournalImpl) {
			int parentId = ((JournalImpl)obj).parentId;
			return isReader(new Integer(parentId));
		} else
			return false;
	}
	
	public boolean isParentEditor(Object obj) throws ZenoException {
	// for move copy
		if (obj instanceof JournalImpl) {
			int parentId = ((JournalImpl)obj).parentId;
			return isEditor(new Integer(parentId));
		} else
			return isEditor(obj);
	}
	
	
	public boolean isCreator(Object obj) throws ZenoException {
		// for topics, articles , attachements and links only
		if (obj instanceof Topic) {
			TopicImpl article = (TopicImpl)obj;
			if (article.closed)
				return false;
			else if (!article.creator.equals(username))
				return false;
			else 
				//is still a writer
				return isWriter(new Integer(article.parentId));
		} else if (obj instanceof Article) {
			ArticleImpl article = (ArticleImpl)obj;
			if (article.closed)
				return false;
			else if (!article.creator.equals(username))
				return false;
			else if (article.part != 0 && isClosed(article.part)) {
				// art belongs to a closed topic
				return false;
			}
			else 
				//is still a writer
				return isWriter(new Integer(article.parentId));
		} else if (obj instanceof Attachment) {
			int articleId = ((Attachment)obj).getArticleId();
			String request = "select locked, creator, parent, part from resource" +
									" where id = " + DBClient.format(articleId); 
			Object[] result = dbclient.getRowArray(request);
			if (result != null) {
				String closed = ((String)result[0]);
				String creator = (String)result[1];
				Integer journal = (Integer)result[2];
				Integer part = (Integer)result[3];
				if (closed.equals("true"))
					return false;
				else if (!creator.equals(username))
					return false;
				else if (part != null && isClosed(part.intValue()))
					//attachment of an article in a closed topic
					return false;
				else 
					return isWriter(journal);
			} else
				return false;
		} else if (obj instanceof Link) {
			int articleId = ((LinkImpl)obj).source;
			String request = "select locked, creator, parent, part from resource" +
									" where id = " + DBClient.format(articleId); 
			Object[] result = dbclient.getRowArray(request);
			if (result != null) {
				String closed = ((String)result[0]);
				String creator = (String)result[1];
				Integer journal = (Integer)result[2];
				Integer part = (Integer)result[3];
				if (closed.equals("true"))
					return false;
				else if (!creator.equals(username))
					return false;
				else if (part != null && isClosed(part.intValue()))
					//attachment of an article in a closed topic
					return false;
				else 
					return isWriter(journal);
			} else
				return false;
		} else
			return false;
	}
			
	
	//-----------------------------------------------------------	
	
	
	public void checkPermission(String operation, Object obj) 
			throws ZenoException {
		boolean result = false;
		if (operation.startsWith("ZenoResource.")) {
			result = checkResourcePermissions(operation.substring(13), obj);
		} else if (operation.startsWith("ZenoCollection.")) {
			result = checkCollectionPermissions(operation.substring(15), obj);
		} else if (operation.startsWith("Journal.")) {
			result = checkJournalPermissions(operation.substring(8), obj);
		} else if (operation.startsWith("Topic.")) {
			result = checkTopicPermissions(operation.substring(6), obj);		
		} else if (operation.startsWith("Article.")) {
			result = checkArticlePermissions(operation.substring(8), obj);	
		} else if (operation.startsWith("Attachment.")) {
			result = checkAttachmentPermissions(operation.substring(11), obj);	
		} else if (operation.startsWith("Factory.")) {
			result = checkFactoryPermissions(operation.substring(8), obj);
		} else if (operation.startsWith("Link.")) {
			result = checkLinkPermissions(operation.substring(5), obj);	
		} else {
			monitor.reportError("DefaultChecker.checkPermission unknown " + operation);
		}
		if (!result) {
			throw new zeno2.kernel.NoPermissionException("NoPermission " + operation + " " + obj);
		}
	}	
	
	
	public boolean checkResourcePermissions(String op, Object obj) 
			throws ZenoException {
		if (op.equals("getTitle")) 
			return (isReader(obj) | isParentReader(obj));
		if (op.startsWith("get"))
			return isReader(obj);
		if (op.equals("noteSize"))
			return isReader(obj);
		if (op.startsWith("isClosed"))
			return isReader(obj);	
		if (op.startsWith("locked"))
			return isReader(obj);	
		if (op.startsWith("hasParent"))
			return true;
		if (op.startsWith("set"))
			return (isEditor(obj) | isCreator(obj));
		if (op.startsWith("close"))
			return isEditor(obj);
		if (op.startsWith("open"))
			return isEditor(obj);	
		if (op.endsWith("Link"))
			return (isEditor(obj) | isCreator(obj));
		if (op.equals("markForDeletion"))
			return isEditor(obj);
		if (op.equals("unmarkForDeletion"))
			return isEditor(obj);
		if (op.equals("move"))
			return isEditor(obj);
		if (op.equals("copy"))
			return isReader(obj);	
		else {	
			monitor.reportError("DefaultChecker.checkResourcePermission unknown " + op);
			return false;
		}
	}
	
	public boolean checkCollectionPermissions(String op, Object obj)
			throws ZenoException {
		if (op.startsWith("getUnpublishedArticles"))
			return isEditor(obj);
		if (op.startsWith("get"))
			return isReader(obj);
		if (op.startsWith("articleCount"))
			return isReader(obj);
		if (op.equals("markedArticleCount"))
			return isEditor(obj);
		if (op.startsWith("search"))
			return isReader(obj);
		if (op.equals("compact"))
			return isEditor(obj);
		if (op.equals("paste"))
			return isEditor(obj);	
		if (op.equals("setRecursivleyMarkedForDeletion"))
			return isEditor(obj);	
		else {
			monitor.reportError("DefaultChecker.checkCollectionPermission unknown " + op);
			return false;
		}
	}
	
	public boolean checkJournalPermissions(String op, Object obj)
				throws ZenoException {
		if (op.startsWith("getUnpublishedArticles"))
			return isEditor(obj);	
		if (op.startsWith("get"))
			return isReader(obj);
		if (op.equals("sortSubjournals"))
			return isReader(obj);	
		if (op.equals("articleCount"))
			return isReader(obj);
		if (op.equals("markedArticleCount"))
			return isEditor(obj);	
		if (op.startsWith("search"))
			return isReader(obj);
		if (op.startsWith("hasRole"))
			return isReader(obj);		
		if (op.startsWith("set"))
			return isEditor(obj);
		if (op.equals("compact"))
			return isEditor(obj);
		if (op.equals("remove"))
			return isEditor(obj);	
		if (op.equals("save"))
			return isEditor(obj);
		if (op.equals("paste"))
			return isEditor(obj);
		if (op.equals("drop"))
			return isEditor(obj);
		if (op.equals("move"))
			return isEditor(obj);	
		if (op.equals("copy"))
			return isEditor(obj);	
		if (op.equals("addPrincipalToRole"))
			return isEditor(obj);
		if (op.equals("deletePrincipalFromRole"))
			return isEditor(obj);			
		else {
			monitor.reportError("DefaultChecker.checkJournalPermission unknown " + op);
			return false;
		}
	}
	
	public boolean checkTopicPermissions(String op, Object obj) 
				throws ZenoException {
		if (op.equals("dissolve"))
			return (isEditor(obj) | isCreator(obj));
		if (op.equals("copy"))
			return isEditor(obj);		
		if (op.equals("copyAsArticle"))
			return isReader(obj);
		else {
			monitor.reportError("DefaultChecker.checkTopicPermission unknown " + op);
			return false;
		}
	}
	
	public boolean checkArticlePermissions(String op, Object obj) 
				throws ZenoException {
		if (op.equals("getAttachments"))
			return (isReader(obj) | isCreator(obj));		
		if (op.startsWith("get"))
			return isReader(obj);
		if (op.startsWith("isTopic"))
			return isReader(obj);	
		if (op.equals("setQualifier"))
			return isEditor(obj);
		if (op.equals("setPublished"))
			return isEditor(obj);	
		if (op.startsWith("set"))
			return (isEditor(obj) | isCreator(obj));
		if (op.startsWith("transformArticle"))
			return (isEditor(obj) | isCreator(obj));	
		if (op.equals("save"))
			return (isEditor(obj) | isCreator(obj));
		if (op.equals("finish"))
			return (isEditor(obj) | isCreator(obj));	
		if (op.equals("addAttachment"))
			return (isEditor(obj) | isCreator(obj));
		if (op.equals("renameAttachment"))
			return (isEditor(obj) | isCreator(obj));	
		if (op.equals("deleteAttachment"))
			return (isEditor(obj) | isCreator(obj));
		if (op.equals("copy"))
			return isReader(obj);
		if (op.equals("generateReadEvent"))
			return isReader(obj);	
		else {
			monitor.reportError("DefaultChecker.checkArticlePermission unknown " + op);
			return false;
		}
	}
	
	
	public boolean checkAttachmentPermissions(String op, Object obj) 
				throws ZenoException {
		if (op.startsWith("get"))
			return (isReader(obj) | isCreator(obj));
		if (op.startsWith("size"))
			return (isReader(obj) | isCreator(obj));	
		if (op.startsWith("set"))
			return (isEditor(obj) | isCreator(obj));
		else {
			monitor.reportError("DefaultChecker.checkAtttachmentPermission unknown " + op);
			return false;
		}
	}
	
	public boolean checkLinkPermissions(String op, Object obj)
			throws ZenoException {
		if (op.equals("save"))
			return (isEditor(obj) | isCreator(obj));
		else {
			monitor.reportError("DefaultChecker.checkLinkPermission unknown " + op);
			return false;
		}
	}
	
	public boolean checkFactoryPermissions(String op, Object obj) 
				throws ZenoException {
		if (op.equals("createArticle"))
			return  isWriter(obj);
		if (op.equals("createTopic"))
			return  isWriter(obj);
		if (op.equals("createJournal")) {
			removeObjPattern(obj);
			return isEditor(obj);
		} else {
			monitor.reportError("DefaultChecker.checkFactoryePermission unknown " + op);
			return false;
		}
	}
	
	

}