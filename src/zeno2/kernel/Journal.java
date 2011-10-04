package zeno2.kernel;

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Zeno 2 Journals
 * @author T. Gordon
 */

public interface Journal extends ZenoCollection {

	/** Returns a comma separated list of labels to be used to label
	articles.  Example: "issue, position, pro, con". */

	public String getArticleLabels() throws ZenoException;

	/** Sets the article labels of the journal. The labels
	should be separated by commas.  Save to make persistent. */

	public void setArticleLabels(String labels) throws ZenoException;

	/** Returns a comma separated list of labels reserved for use
	by editors or moderators to label articles or authors/creators. 
	Examples: "warning, yellow card, red card, senior contributor". */

	public String getQualifiers() throws ZenoException;

	/** Sets the editor labels of the journal. The labels
	should be separated by commas. Save to make persistent. */

	public void setQualifiers(String labels) throws ZenoException;

	/** Returns a comma separated list of link labels. Examples: "reply,
	about, refines, distinguishes". */

	public String getLinkLabels() throws ZenoException;

	/** Sets the links labels of the journal. The labels should be separated
	by commas. Save to make persistent. */

	public void setLinkLabels(String labels) throws ZenoException;

	/** Returns an set containing the members of this journal which
	are also journals. */

	public Set getSubjournals() throws ZenoException;

	/** Returns a List of journal members sorted by rank and then title. */

	public List sortSubjournals() throws ZenoException;

	/** Gets the number of hours during which the creator of an article
	may modify it.  After expiration of this period the article is 
	published and locked. */

	public int getRevisionPeriod() throws ZenoException;

	/** Sets the revision  period property. Save to make persistent. */

	public void setRevisionPeriod(int hours) throws ZenoException;
	
	public List getArticlesByTopic() throws ZenoException;
	
	public List searchByTopic (
		String authorId,
		String title,
		String articleLabel,
		String qualifier,
		Date fromDate,
		Date toDate,
		String fullText)
			throws ZenoException;

	/** The "topics" of a journal are the "top-level" articles. Returns
	a list of topics. The topics are sorted by rank and then title. */

	public List getTopics() throws ZenoException;

	/** Returns the n most recently created topics in a journal,
	sorted by creation date.  Only published and not marked for deletion
	topics are included. */
	
	public List getRecentTopics(int n) throws ZenoException;
	
	/** Returns topics created in this journal after date
	sorted by creation date. Only published and nondeleted
	topics are included. Uses date of last login if date is null*/
	
	public List getRecentTopics(Date date) throws ZenoException;
	
	/** Returns the topic mode which signals whether articles which are 
	not replies to other articles should be created as topics; 
	for use by the gui only */
	
	public boolean getTopicMode() throws ZenoException;
	
	/** Sets the topic mode */
	
	public void setTopicMode(boolean on) throws ZenoException;
	
	/** Returns the maximal size (KB) of an attachment in the journal */
	
	public int getAttachmentSizeLimit() throws ZenoException;
	
	/** Sets the maximal size (KB) of an attachment in the journal */
	
	public void setAttachmentSizeLimit(int size) throws ZenoException;
	
	/** Gets the name to be used by email to add articles to the journal */
	
	public String getMailAlias() throws ZenoException;
	
	/** Sets the name to be used by email to add articles to the journal.
	Throws a NameInUseException if alias is already used for another journal
	The current alias is removed if the specified alias is empty. */
	
	public void setMailAlias(String alias) throws ZenoException;
	
	/** Gets the URL of the Style Sheet to be used, if one has been set 
	for this journal. If not, the empty string is returned. */

	public String getStyleSheetUrl() throws ZenoException;

	/** Sets the URL of the Style Sheet to be used for formatting
	this journal, overriding system defaults. */

	public void setStyleSheetUrl(String url) throws ZenoException;
	
	
	public void renumberArticles() throws ZenoException;
	
	
	public List getFullOutline() throws ZenoException;
	
	/** The members parameter denotes a set of member resources (subjournals, 
	articles) of this journal which are to be moved to the journal 
	denoted with the collection parameter. If the destination journal is 
	this journal, execution of this method has no effect (without 
	throwing an exception).
	
	To move a resource means to remove it from the place where it is and 
	add it to a given collection (journal). All attributes and all links 
	connecting this resource with others (moved or not) persist.
	obsolete use moveHere instead */

	public void move(Set members, ZenoCollection collection) 
		throws ZenoException;
		
	/** The resources parameter denotes a set of resources (journals, 
	topics, articles) which are to be moved to this journal. 
	
	To move a resource means to remove it from the place where it is and 
	add it to a given collection (journal). All attributes and all links 
	connecting this resource with others (moved or not) persist.*/
	
	public void moveHere(Set members) throws ZenoException;


	/** The members parameter denotes a set of member resources (subjournals, 
	articles) of the journal. The collection parameter denotes the destination
	journal where the copied journals and articles are inserted as new members; 
	can be  the parent journal of the member resources.
	
	To copy a resource means to create a new resource derived from the 
	original and add it to the destination journal: the attributes 
	id, creator, creationDate, modifier, modificationDate are assigned 
	new values, but all other attributes are initialized from the original.
	
	The withLinks parameter specifies whether links pointing from copied
	nodes to other nodes are to be copied or not. To copy a link means to 
	create a new link that connects either the copies of the two endnodes 
	(in the destination journal) or the copy of one endnode with the other
	uncopied endnode. 
	
	Which endnodes are actually copied, depends on the permissions of the 
	requesting user. Resources are not copied (without throwing an exception) 
	if he has no permission. 
	 
	Recursively copies member collections (subjournals) . However, descending
	the tree structure (of nested journals) for selecting the resources to be 
	copied terminates where the user has no permission, even if the user 
	could copy  some deeper journal.  
	obsolete use copyHere instead*/
		
	public void copy(Set members, ZenoCollection collection, boolean withLinks)
		throws ZenoException;
	
	/** The resources parameter denotes a set of resources (journals, 
	topics, articles) which are to be copied to this journal..
	
	To copy a resource means to create a new resource derived from the 
	original and add it to the destination journal: all attributes but 
	the id are initialized from the original.
	
	The withLinks parameter specifies whether links pointing from copied
	nodes to other nodes are to be copied or not. To copy a link means to 
	create a new link that connects either the copies of the two endnodes 
	(in the destination journal) or the copy of one endnode with the other
	uncopied endnode. 
	
	Which endnodes are actually copied, depends on the permissions of the 
	requesting user. Resources are not copied (without throwing an exception) 
	if he has no permission. 
	 
	Recursively copies member collections (subjournals) . However, descending
	the tree structure (of nested journals) for selecting the resources to be 
	copied terminates where the user has no permission, even if the user 
	could copy  some deeper journal. */
		
	public void copyHere(Set resources, boolean withLinks)
		throws ZenoException;

	/** Returns a list of strings naming the supported roles, 
	e.g. "editor", "writer", and "reader". */

	public List getRoles() throws ZenoException;

	/** Gets the ids (strings) of principals with the given role.  The 
	members of groups in the role definition are included, recursively,
	in this set. If the userOnly parameter is true, the ids of groups are
	filtered out of the result. */

	public Set getPrincipalsInRole(String role, boolean usersOnly)
		throws ZenoException;

	/** Gets the definition of the role.  Each item in the list
	is the identifier of a principal, i.e. user or group. */

	public List getRoleDefinition(String role) throws ZenoException;

	/** Checks whether a principal with the given id has the given role.  Membership
	in groups used to define roles is checked. */

	public boolean hasRole(String id, String role) throws ZenoException;

	/** Adds a principal (i.e. user or group) to the role.  This change
	is made immediately.  There is no need to use the save method. */

	public void addPrincipalToRole(String id, String role) throws ZenoException;

	/** Deletes a principal from the given role. This change is made
	immediately.  There is no need to use the save method. */

	public void deletePrincipalFromRole(String id, String role)
		throws ZenoException;
		
	/** Replaces the role definition by the role definition of source 
	If role is null the definitions of all roles are replaced */
	
	public void replaceRoleDefinition(Journal source, String role, boolean recursively)
			throws ZenoException;


	/** Gets the set of ids of all principals (i.e. users and groups) 
	having any role in the journal.  The ids of members of groups and, recursively, 
	subgroups are included in the result. If the usersOnly parameter is true, 
	the ids of groups are filtered out of the result.
	Not called the "members" of the journal to avoid confusion with collection
	membership. */

	public Set getPrincipals(boolean usersOnly) throws ZenoException;

	
	

	public void register(String commonName, String email, String preferredUsername)
		throws ZenoException;
		
	public void remove() throws ZenoException;
		
	public PreviewElement genCompactPreview() throws ZenoException;
		
	public PreviewElement genRemovePreview() throws ZenoException;
		
	public PreviewElement genDeletePreview() throws ZenoException;	
	
	/** valis modes: undelete, xundelete */
	
	public PreviewElement genUndeletePreview(String mode) 
		throws ZenoException;
		
	public boolean unmarkWithAncestors() throws ZenoException;
		
}